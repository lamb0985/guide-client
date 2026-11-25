# Guide Client

Unofficial Android and Android TV wrapper for Dispatcharr. Credit to the [Dispatcharr contributors](https://github.com/Dispatcharr/Dispatcharr). Not affiliated with or endorsed by Dispatcharr. No upstream logo or source bundled. Wrapper offered under [AGPL-3.0-only](https://www.gnu.org/licenses/agpl-3.0.html).

Uses the original login, guide and popups on the configured server, without server modifications. Default server: http://192.168.1.15:9191. HTTP is unencrypted; use HTTPS remotely. Native Media3 playback is attempted for direct same-origin stream navigations; embedded browser playback retains WebView fullscreen support. Exact guide playback handoff, D-pad navigation and login behavior need testing against a live instance.

Build with JDK 17, Android SDK 35, Gradle 8.9: gradle assembleDebug. Debug APK available from successful GitHub Actions build artifacts. Prototype, not yet device-tested.