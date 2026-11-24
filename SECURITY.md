# Security policy

Only the latest release receives security fixes. Report vulnerabilities privately through GitHub's **Security → Report a vulnerability** form. Never attach real photos, passwords, cookies or private server addresses.

The app requests only network access, network state, notifications and a foreground data-sync service. Camera and photo selection use Android system intents and temporary URI grants; the app does not request broad camera or media-library permissions. Android private storage keeps server profile names and addresses, excludes them from backups, and stores no album or admin password. Authentication cookies remain in the system WebView cookie store.

HTTPS uses Android's normal certificate and hostname validation. Certificate errors are not bypassed. Static certificate pinning is unsuitable because every installation uses its own LAN, Let's Encrypt, reverse-proxy or Funnel certificate. HTTP is restricted to localhost, private LAN, `.local`/`.lan` and Tailscale address ranges and triggers a warning when saved. Public server addresses require HTTPS. Mixed content and third-party WebView cookies are disabled, Safe Browsing is enabled, and the native upload bridge is removed outside the configured server origin.

GitHub releases and Play bundles require protected repository signing secrets and are verified with `jarsigner`; debug APKs are never published as releases. Releases include a SHA-256 checksum. Dependabot, CodeQL and Trivy monitor source and dependencies.
