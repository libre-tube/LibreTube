# Release instructions
The following sections describe how to create a new release.

## Prerequirements
Please note that for the process to work, the following requirements must be met:
- All commit messages should follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification (anything else is ignored).
- The following actions secrets have to be set (see [ilharp/sign-android-release](https://github.com/ilharp/sign-android-release)):
  - `ANDROID_RELEASE_SIGNING_KEY`
  - `ANDROID_RELEASE_KEY_ALIAS`
  - `ANDROID_RELEASE_KEYSTORE_PASSWORD`
  - `ANDROID_RELEASE_KEY_PASSWORD`
- Additionally, GitHub actions workflows must be configured to have "Read and write permissions" in the repository's settings.

## Release process
1. Increment the `versionNumber` and `versionCode` in `build.gradle.kts`.
2. Create a file `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` and write a short summary about the release into this file.
3. Push the changes you made to the `master` branch.
4. Create a new tag from the `master` branch, e.g. via `git tag v0.0 && git push --tags`.
