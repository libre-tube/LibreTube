# Roadmap

This represents the larger, higher-impact work planned for this fork. Items
below track where the project has been and where it is heading. Features are
planned, do not represent a commitment to develop, and can change at any time.

## Done (recent)

- **v32.0** — livestream fix: empty DASH manifest no longer selected for live
  streams; playback-source selection moved to a pure, unit-tested selector
  (`SABR / DASH / HLS / NONE`) with HLS fallback for lives. Validated on real
  hardware (2 lives + VOD).
- **Playback engine hardening** — SABR client leaks, chunk-source error
  handling, retry interceptor (13 tests), instance failover (7 tests),
  `runBlocking` removal in bottom sheets, fast DB cleanups, Room migrations
  23→26 with instrumented tests.
- **Quality/CI** — CodeQL, per-workflow CI, signed release pipeline,
  accessibility (content descriptions), theme and splash fixes, dead-code
  removal, docs (`ARCHITECTURE.md` living report + wiki).

## Planned

- Room migration instrumentation on an emulator covering API < 31 (runtime
  **BLOCKED** on this machine — needs a device/emulator).
- Translation gap reduction via Weblate (community debt: ~11k missing strings
  across 77 locales — upstream contribution).
- Evaluate replacing the deprecated `ilharp/sign-android-release@v2` (runs on
  legacy Node 20) with a maintained alternative.
- Housekeeping: clear the remaining legacy lint debt (mostly
  `MissingTranslation`) without altering upstream translations.

## Not planned

- Google/MicroG login.
- Platforms other than Android (iOS, desktop, Android TV).
- Downloading and muxing media for playback outside LibreTube.
- Recommendation algorithms to pull users into a rabbit hole.