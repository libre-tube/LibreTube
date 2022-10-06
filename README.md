<div align="center">
  <img src="https://libre-tube.github.io/images/gh-banner.png" width="auto" height="auto" alt="LibreTube">

[![GPL-v3](https://libre-tube.github.io/images/license-widget.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![Matrix](https://libre-tube.github.io/images/mat-widget.svg)](https://matrix.to/#/#LibreTube:matrix.org)
[![Telegram](https://libre-tube.github.io/images/tg-widget.svg)](https://t.me/libretube)
[![Twitter](https://libre-tube.github.io/images/tw-widget.svg)](https://twitter.com/libretube)
[![Reddit](https://libre-tube.github.io/images/rd-widget.svg)](https://www.reddit.com/r/Libretube/)

</div><div align="center" style="width:100%; display:flex; justify-content:space-between;">

[<img src="https://libre-tube.github.io/images/fdrload.png" alt="Get it on F-Droid" width="30%">](https://f-droid.org/en/packages/com.github.libretube/)
[<img src="https://libre-tube.github.io/images/izzyload.png" alt="Get it on IzzyOnDroid" width="30%">](https://apt.izzysoft.de/fdroid/index/apk/com.github.libretube)<br/>
[<img src="https://libre-tube.github.io/images/ghload.png" alt="Get it on GitHub" width="30%">](https://github.com/libre-tube/LibreTube/releases/latest)

</div>

## 📔 About

YouTube has an extremely invasive privacy policy which relies on using user data in unethical ways. They store a lot of your personal data - ranging from ideas, music taste, content, political opinions, and much more than you think.

The project is aimed to improve the users privacy by being independent from Google and bypassing their data collection. Therefore the app is using the [Piped API](https://github.com/TeamPiped/Piped), which uses proxies to circumvent Googles data collection and includes some other additional features.

> **Warning**
<br>The project is still in beta, therefore you may encounter bugs. If you do so, please open an issue via our GitHub repository.

<table><td>
<a href="https://github.com/libre-tube/LibreTube/issues/new?assignees=&labels=bug&template=report_bug.yml">🐞 Report Issue</a>
</td>
<td><a href="https://github.com/libre-tube/LibreTube/issues/new?assignees=&labels=enhancement&template=feature-request.yml">🤩 Request Feature</a>
</td></table>

## 📱 Screenshots

<div style="width:100%; display:flex; justify-content:space-between;">

[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_1.png" width=30% alt="Home">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_1.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_2.png" width=30% alt="Search">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_2.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_3.png" width=30% alt="Player">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_3.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_4.png" width=30% alt="Channel">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_4.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_5.png" width=30% alt="Settings">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_5.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_6.png" width=30% alt="Subscriptions">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_6.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_7.png" width=30% alt="Subscriptions List">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_7.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_8.png" width=30% alt="Library">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_8.png)
[<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot_9.png" width=30% alt="Playlist">](fastlane/metadata/android/en-US/images/phoneScreenshots/shot_9.png)

</div>

## ⭐ Features

| Feature           |     |
| ----------------- | --- |
| User Accounts     | ✅  |
| Subscriptions     | ✅  |
| User Playlists    | ✅  |
| Channel Playlists | ✅  |
| Search Filters    | ✅  |
| SponsorBlock      | ✅  |
| Subtitles         | ✅  |
| Comments          | ✅  |

## 😇 Contributing

Whether you have ideas, translations, design changes, code cleaning, or real heavy code changes, help is always welcome. The more is done, the better it gets!

If creating a pull request, please make sure to format your code (preferred ktlint) before.

If opening an issue without following the issue template, we will ignore the issue and force close it.

### 📜️ Credits

- Readme Design and Banners by [XelXen](https://github.com/XelXen)

#### Icons

- [Default App Icon](https://github.com/libre-tube/LibreTube/blob/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png) by [XelXen](https://github.com/XelXen)
- [Boosted Bird](https://github.com/libre-tube/LibreTube/blob/master/app/src/main/res/mipmap-xxxhdpi/ic_bird_round.png) by [Margot Albert-Heuzey](https://margotdesign.ovh)

### 📝 Translation

<a href="https://hosted.weblate.org/projects/libretube/#languages">
<img src="https://hosted.weblate.org/widgets/libretube/-/287x66-grey.png" alt="Translation status" />
</a>

## Differences from NewPipe

With NewPipe, the extraction is done locally on your phone and all the requests sent towards YouTube/Google are done directly from the network you're connected to and doesn't use a middleman server in between. Therefore Google can still access information such as the users IP address, besides that subscriptions can only be stored locally.

LibreTube takes this one step further and proxies all requests via Piped (which uses the NewPipeExtractor). This prevents Google servers from accessing your IP address or any other personal data. Apart from that Piped allows syncing your subscriptions between LibreTube and Piped which can be used on desktop too. If the NewPipeExtractor breaks, it only requires an update of Piped and not LibreTube itself, therefore fixes usually arrive faster than in NewPipe.

While LibreTube only supports YouTube, NewPipe also allows using other platforms like SoundCloud and media.ccc.de. Both are great clients for watching YouTube videos, it depends on the individuals use case which one fits the own needs better.

## 🔒 Privacy Policy and Disclaimer

The LibreTube project aims to provide a private, anonymous experience for using web-based media services. Therefore, the app does not collect any data without your consent. 

The LibreTube project and its contents are not affiliated with, funded, authorized, endorsed by, or in any way accociated with YouTube, Google LLC or any of its affiliates and subsidaries.
Any trademark, service mark, trade name, or other intellectual property rights used are owned by the respective owners.

LibreTube is an open source software built for learning and research purposes.

## 🪞 Mirrors (read-only)

<a href="https://gitlab.com/libretube/LibreTube">GitLab</a></p>
<a href="https://notabug.org/LibreTube/LibreTube">NotABug</a></p>
<div align="right">
<table><td>
<a href="start-of-content">↥ Scroll to top</a>
</td></table>
</div>
