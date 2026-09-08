# Arquitetura do LibreTube (fork)

Este documento descreve a organização, o fluxo de dados e os componentes principais
deste fork, destacando também as melhorias aplicadas durante a auditoria.

## Visão geral

O app é um cliente Android para instâncias [Piped](https://github.com/TeamPiped/Piped)
que também suporta extração direta via NewPipe Extractor (modo offline/local),
bem como reprodução de downloads em SABR e modo clássico.

- Linguagem: 100% Kotlin
- UI: Jetpack Compose **não** é usado — a UI é baseada em Fragments + ViewBinding + Material 3
- Banco de dados: Room (KSP), versão de esquema **26**
- Player: Media3 ExoPlayer com fonte de mídia SABR própria (`player/`)
- Biblioteca de extração: NewPipe Extractor (`org.schabi`)

## Módulos (Gradle)

| Caminho | Papel |
| --- | --- |
| `app` | Aplicação principal |
| `baselineprofile` | Baseline Profile para arranque/scroll |
| `build.gradle.kts` (raiz) | Plugins compartilhados |
| `settings.gradle.kts` | Repositórios (Google, Maven Central, JitPack, mavenLocal) |

## Camadas principais

```
ui/               -> Activities, Fragments, Sheets, Dialogs, Adapters, ViewModels
api/              -> Retrofit (PipedApi/PipedAuthApi/ExternalApi) + fallback de instância
repo/             -> Repositórios de feed (conta / local / Piped)
player/           -> SabrMediaSource + SabrClient (streaming adaptativo SABR)
services/         -> PlayerServices (MediaLibraryService), DownloadService, workers
db/               -> Room entities/DAO/migrations (DatabaseMigrations, DatabaseHelper)
helpers/          -> PreferenceHelper (SharedPreferences), ImageHelper, PlayerHelper, etc.
workers/          -> NotificationWorker (verificação de inscrições em segundo plano)
```

## Fluxo de reprodução online

1. `PlayerFragment` monta um `PlayerData` (videoId, playlistId, channelId, timestamp).
2. `MainActivity` despacha uma ação `start_service_action` via `MediaSession` →
   `AbstractPlayerService.onCustomCommand` insere o serviço e dispara `onServiceCreated` +
   `startPlayback` dentro de `serviceScope` (corrotina scoped à vida do serviço).
3. `OnlinePlayerService.startPlayback` busca `Streams` (`MediaServiceRepository.instance.getStreams`)
   com fallback automático de instância (ver "Failover de instância").
4. `setStreamSource()` escolhe entre HLS, progressive e SABR (`SabrMediaSource`).
5. `configurePlayer()` aplica posição salva e `prepare()`.

### Failover de instância

A cadeia de recuperação é em camadas, porém limitada:

1. `RetryInterceptor` (OkHttp): repetições na mesma instância para `IOException`
   (qualquer método); para HTTP 5xx/429 **somente métodos idempotentes**
   (GET/HEAD/OPTIONS/PUT/DELETE/TRACE — POST não é repetido). O `Retry-After` é
   respeitado até o teto de `maxDelayMs`, não há sleep desperdida no último
   attempt e o último status HTTP real é devolvido quando o limite é atingido.
2. `PipedMediaServiceRepository.apiCallWithFallback`: exceções de rede/5xx/429
   invocam `InstanceFallbackManager.onInstanceFailed`, que marca a instância como
   falha (cooldown de 60 s) e procura alternativas entre as instâncias customizadas
   (até 3 átomos). **A checagem de health-check foi paralelizada** (era sequencial,
   até 8 s por instância; agora todas rodam concorrentemente com limite de 8 s),
   então a troca de instância é bounded.
3. `OnlinePlayerService`: ao receber `InstanceSwitchedException`, repete a chamada
   uma única vez na instância nova (o fallback interno volta a valer).

Resultado: no pior caso há no máximo **1 troca de instância + as repetições internas**;
não há loop infinito. A saúde por instância é mantida em `InstanceFallbackManager`
(tabela em memória com contagem de falhas + cooldown); a falha é marcada de forma
atômica (`instanceHealthMap.compute`) e a seleção de candidatas (exclui a falida,
preserva ordem, respeita cooldown, máximo 3) é feita por funções puras testadas.

## Player SABR

- `SabrMediaSource` → `SabrMediaPeriod` → `SabrChunkSource`/`DefaultSabrChunkSource`
  organizam o download dos segmentos.
- `SabrDataSource` baixa chunk a chunk via `SabrClient` usando o `PlaybackRequest`
  embutido no `DataSpec.customData` (nenhum cabeçalho de streaming é exposto).
- `CompositeBuffer` remonta os segmentos da API para o timestamp desejado.
- `BaseUrlExclusionList` evita repetir hosts que falharam.
- `DownloadService` reusa essa mesma fonte para baixar vídeos SABR.

## Banco de dados (Room, versão 26)

Auditoria aplicou neste fork:

- **Migração 23→24 corrigida**: `ALTER TABLE ... DROP COLUMN` só existe no SQLite 3.35+
  (Android 12+). Abaixo disso, a tabela `downloadItem` é **reconstruída** (create-new,
  copia de dados, drop, rename, recriação do índice) — o código antigo quebrava em
  dispositivos API < 31.
- **Índices adicionados** (via AutoMigration 25→26): `downloadItem(videoId)`,
  `downloadSponsorBlockSegment(videoId)`, `feedItem(uploaded)`,
  `localPlaylistItem(playlistId)` — eliminam full table scans em FKs e ordenação.
- `WatchHistoryDao.findById` agora usa `=` (igualdade) em vez de `LIKE`.
- Chamadas de banco **fora da main thread**: `setWatchProgressLength` virou suspend; as
  leituras `runBlocking` do caminho de bind (`PlayerFragment`, adapters) foram movidas
  para `lifecycleScope` / corrotinas.

`DatabaseHelper` centraliza acesso e conversão. As funções **suspend** já usam
`withContext(Dispatchers.IO)`; o `getWatchPositionBlocking` obsoleto foi removido.

## Segurança aplicada

- **Backup**: novo `data_extraction_rules.xml` exclui do backup em nuvem/transferência
  o SharedPreferences `auth` (token/usuário do Piped), além de arquivos derivados.
  O `PickerActivity`/`parseActivity` segue o mesmo comportamento dos itens excluídos.
- **Instâncias novas**: `InstancesModel` rejeita `http://` para instâncias customizadas,
  exceto endereços loopback (localhost / 127.x / ::1) — ideal para testes locais.
- `network_security_config.xml` mantém `cleartextTrafficPermitted=true` e trust de CAs
  do usuário **intencionalmente** (muitas instâncias Piped são http/self-signed); isso
  é uma decisão de compatibilidade documentada, não um descuido.

## Acessibilidade

- `exo_styled_player_control_view.xml`: todos os botões de ícone agora possuem
  `contentDescription` (minimize, lock, queue, options, sponsorblock, dearrow,
  play/pause, previous/next, fullscreen) — antes só havia `tooltipText`, invisível
  para TalkBack.

## Temas

- Temas material 3 (light/dark) por cor; tema monochrome teve `onSecondary`
  corrigido para atingir contraste ≥ 4.5:1 (#FFFFFF no claro, #161616 no escuro).
- Bug de splash corrigido: o valor-night definia `StartupTheme` (nunca referenciado);
  agora está renomeado para `SplashScreenTheme`, casando com o Manifesto.
- `DialogActivity` usa novo drawable `dialog_material_background` theme-aware
  (antes apontava para `abc_dialog_material_background`, branco fixo — quebrava
  o modo escuro).

## Serviços e corrotinas

- `AbstractPlayerService` agora expõe `serviceScope = SupervisorJob() + Dispatchers.IO`,
  cancelado em `onDestroy()`; todos os launchs adversos viraram escopados
  (`START_SERVICE_ACTION`, `navigateVideo`, fetch de vídeo, playback offline).
- Removida leitura bloqueante `getWatchPositionBlocking` da configuração do player
  (o valor é pré-buscado em IO antes de configurar o ExoPlayer na main thread).

## CI/CD

- `softprops/action-gh-release` atualizado **v2 → v3** (v2 roda em Node 20, removido
  pelo GitHub em 2026-09-16).
- `build-release.yml`: `buildToolsVersion` 35.0.0 → 36.0.0 (alinhado ao compileSdk 36).
- `build-debug-apk.yml`: versão dinâmica do APK (lida de `versionName`), sem string
  hardcoded "v31.4".
- `.github/uploader.py`: corrigida injeção de comando (o nome do autor — dado de fora —
  era interpolado em `os.system` para o `git commit`; agora sanitizado e chamado via
  `subprocess.run` com argumentos).
- `gradle.properties`: propriedade de configuration-cache renomeada para o novo nome
  (`configuration-cache.problems=warn`).

## Verificação final

- `./gradlew assembleDebug lintDebug testDebugUnitTest` — **BUILD SUCCESSFUL**
  (APK em `app/build/outputs/apk/debug/app-debug.apk`; 29 testes unitários passando).
- Lint: removidos 36 avisos `InvalidManifestAttribute` (atributos ignorados nos
  `activity-alias` dos ícones do launcher). Resíduos restantes são majoritariamente
  `MissingTranslation` (578, herança de estados parciais das 77 traduções), mais
  `ContentDescription`/`UnusedResources`/avisos de versão — sem regressões introduzidas
  por esta tarefa; `lint { abortOnError = false }` mantido por esse estado legado.
- Código morto removido: `PlayerPiPHelper` (130 linhas, nunca instanciado),
  `File.formatSize()` (sem chamadores), `ExternalApi.USER_AGENT` (substituído por
  `ApiConstants.USER_AGENT`) e `getWatchPositionBlocking`.

## Fase 2 — segunda auditoria (validação independente)

Levada a cabo em iterações de 5–7 tarefas ("waves"); a lista oficial de resultados
(fase 1 + fase 2) é publicada no relatório final (matriz de 17 linhas PASS/FAIL/BLOCKED).

### Waves 1–2: motor de repetição e failover — testes

- `app/src/test/.../api/interceptor/RetryInterceptorTest.kt` (13 testes): sucesso sem
  repetição, 500 transitório repetido, múltiplos erros transitórios, exaustão devolve
  o 500 real (3 chamadas), 404/501 não repetidos, 429 repetido, `Retry-After` tetado
  (assert temporal <2 s), POST 500/429 **não** repetido, `IOException` repetido mesmo em
  POST, `IOException` relançado após exaustão. `FakeChain` implementa `Interceptor.Chain`.
- `app/src/test/.../api/InstanceFallbackManagerTest.kt` (7 testes): exclui a falida,
  preserva ordem, cooldown ativo/vencido, limite de 3, instância sem falha fora do
  cooldown.

### Wave 3: endurecimento do player SABR

- `SabrClient.currentCall` agora é `@Volatile` e a limpeza `retainAll` ocorre dentro de
  `withContext(dispatcher)` (fora da thread do OkHttp).
- `Segment.length()` para `Long` (soma dos pacotes), comparado como `Long` ao
  `contentLength` (evita overflow em arquivos >2 GiB).
- `partialSegments` é limpo após parse bem-sucedido (com log de aviso).
- `RELOAD_PLAYER_RESPONSE` não desce mais como carga útil — seta
  `playerReloadRequested = true` e lança `SabrFatalException` (nova classe de erro).
- Retry de `fetchStreamData` re-cria o request com `&rn=${requestNumber++}` (novo
  request por tentativa). `SabrDataSource` propaga `SabrFatalException` sem embrulhar;
  `DefaultSabrChunkSource.onChunkLoadError` a consome (marca `fatalError`, retorna
  verdadeiro) — sem loop de repeats de loader.
- Guarda anti-livelock de inicialização: `MAX_INIT_CHUNK_ATTEMPTS = 5` por
  `RepresentationHolder`, reiniciada a cada sucesso.
- `SabrMediaSource.releaseSourceInternal` chama `SabrClient.release()` (cancela call,
  `dispatcher.executorService.shutdown()`, `connectionPool.evictAll()`,
  `poTokenGenerator.close()`); `PoTokenGenerator.close()` sincronizado no lock global.

### Wave 5: remoção de `runBlocking` nas sheets

- `EditChannelGroupSheet`: validação de nome por corrotina `lifecycleScope` cancelável
  (job por toque), com DAO em `Dispatchers.IO`.
- `PlaylistOptionsBottomSheet`: bookmark checado em `withContext(IO)` antes de montar
  as opções. `VideoOptionsBottomSheet`: opções base síncronas + opções de watch-status
  carregadas em corrotina, inseridas antes de "add to playlist".
- Restam apenas `runBlocking` de ponte Verde (loader do ExoPlayer) e Amarelo
  (interop NewPipe — `PoTokenGenerator`), ambos documentados.

### Wave 6: banco rápido

- `SearchHistoryDao.deleteOldest(keep)`: `DELETE ... rowid IN (SELECT rowid ORDER BY
  rowid DESC LIMIT -1 OFFSET :keep)` — trim em uma única instrução (tablea não tem
  coluna `id`). `DatabaseHelper.trimSearchHistoryIfNeeded` passou a usá-la.
- `SabrDownloadProvider`: persistência de progresso limitada a cada 25 segmentos
  (ou no último) — menos escritas de DB por streaming.

### Wave 6b: testes de migração Room (CI-ready, runtime BLOCKED)

- `app/src/androidTest/.../db/MigrationInstrumentedTest.kt`: 4 testes com
  `MigrationTestHelper` (construtor por classe) validando 23→24 (drop de
  `downloadItem.url`, preservando linhas), 24→25 (coluna nullable
  `currentDownloadPositionMillis`), 25→26 e a cadeia completa 23→26.
- Esquemas em `app/schemas/.../AppDatabase/*.json`; `assembleDebugAndroidTest`
  **BUILD SUCCESSFUL**. Execução exige device/emulador — **BLOCKED** nesta máquina.
  O caminho de rebuild para API < 31 da migração 23→24 só é exercitável em emulador
  antigo.

### Wave 7–8: lint e traduções

- `ContentDescription`: 43 → 0. Imagens decorativas recebem `contentDescription="@null"`;
  controles reais recebem strings novas (`play`, `close`, `minimize`, `decrease`,
  `increase`, `delete_history`) — 6 strings-base adicionadas.
- Corrigidas as 4 classes reais sinalizadas como erro: formato inválido
  `bn videoCount` (`%1$টি` → `%1$dটি`, crash para usuários bengali), 3 broadcasts
  internos do `DownloadService` agora com `setPackage(packageName)`, `history_empty`
  sem constraints verticais em `fragment_watch_history`, e o
  `Dialog.onBackPressed` deprecado removido (o `OnBackPressedDispatcher` do fragmento
  já trata o fullscreen).
- Auditoria de traduções: 77 arquivos de locale, 583 strings-base, soma de
  traduções faltantes = 10 961 — dívida de comunidade (crowdin), classificada como
  UPSTREAM, sem inventar traduções. Gap real corrigido: o formato inválido de `bn`.
- Lint final: **899 → 855** (após esta fase). Classes restantes
  (MissingQuantity/ImpliedQuantity em cs/sk/lt, UnsafeOptInUsageError do Media3,
  StringFormatCount, Overdraw, UnusedResources, versões) são dívida legada/eff-i.

### Wave 10–11: release e CI

- `./gradlew assembleRelease bundleRelease lintRelease testDebugUnitTest` com R8:
  **BUILD SUCCESSFUL** (APK-release-unsigned e AAB gerados).
- Workflows GHA: `ci.yml` (assembleDebug + assinatura via secrets + upload),
  `build-release.yml`, `build-debug-apk.yml`, `codeql-analysis.yml`. Localmente só é
  possível simular a parte de compile/teste/lint (executada). Assinatura/publicação
  exigem secrets — **BLOCKED** aqui.
- Dependências: 39 diretas, todas de geração atual (OkHttp 5.3.2, Retrofit 3.0.0,
  Room 2.8.4, Media3 1.9.2, Coil 3.4.0, protobuf 4.33.5); sem segredos hardcoded;
  `exported=true` restrito a launcher, share-receivers, router de deep links e
  aliases de ícone (verificado no manifesto).

### Decisões mantidas

- Sem testes artificiais; bloqueios documentados com precisão (device/emulador,
  keystore, secrets do GHA).
- `BaseUrlExclusionList`, rede em cleartext e token do Piped na query são contratos
  intencionais do upstream (documentados em "Segurança aplicada").
- Dívida aceita e anotada: aviso de schema KSP sobre o índice de jurisdição
  `DownloadPlaylistVideosCrossRef.videoId`, e `lint { abortOnError = false }`.

## Próxima leitura

- `README.md` — instruções de build e uso.
- `PRIVACY_POLICY.md` — política de privacidade.
- `RELEASE.md` — processo de release.