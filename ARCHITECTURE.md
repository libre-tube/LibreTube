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

1. `RetryInterceptor` (OkHttp): até **2** repetições na mesma instância para
   `IOException`, HTTP 5xx (exceto 501) e 429, com backoff exponencial + jitter.
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
(tabela em memória com contagem de falhas + cooldown).

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
`withContext(Dispatchers.IO)`; `getWatchPositionBlocking` está deprecated.

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

## Próxima leitura

- `README.md` — instruções de build e uso.
- `PRIVACY_POLICY.md` — política de privacidade.
- `RELEASE.md` — processo de release.