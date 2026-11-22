# Security policy

Only the latest release receives security fixes. Report vulnerabilities privately through GitHub's **Security → Report a vulnerability** form. Never attach real photos, passwords, cookies or private server addresses.

The app requests only network access, network state, notifications and a foreground data-sync service. Camera and photo selection use Android system intents and temporary URI grants; the app does not request broad camera or media-library permissions. Android private storage keeps server profile names and addresses, excludes them from backups, and stores no album or admin password. Authentication cookies remain in the system WebView cookie store.

HTTPS uses Android's normal certificate and hostname validation. Certificate errors are not bypassed. Static certificate pinning is unsuitable because every installation uses its own LAN, Let's Encrypt, reverse-proxy or Funnel certificate. HTTP remains available for trusted LAN installations and triggers a warning when saved; do not use it over the Internet.

The GitHub APK and Play AAB are built by CI. The Play release requires repository signing secrets and is verified with `jarsigner`. Dependabot monitors Gradle and GitHub Actions dependencies.
