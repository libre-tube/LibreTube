# Security Policy

## Supported versions

Only the latest stable release is supported with security updates.

| Version | Supported          |
| ------- | ------------------ |
| latest  | :white_check_mark: |
| older   | :x:                |

## Reporting a vulnerability

Please **do not** open a public issue for sensitive security problems.

- Prefer a private report through GitHub's
  [Security Advisories](https://github.com/brunlx/LibreTube----New-Version/security/advisories/new).
- Alternatively, contact the maintainer privately via GitHub (@brunlx).

You can expect an acknowledgment within 48 hours and triage with a fix
version / workaround as soon as possible. Responsible disclosures may be
credited in the release notes and [CONTRIBUTORS.md](CONTRIBUTORS.md).

## Security practices

- Static analysis via CodeQL runs on every push and PR
  (`.github/workflows/codeql-analysis.yml`).
- Release builds are signed in CI with a keystore held in encrypted secrets —
  local builds never contain signing material.
- The app only stores the minimum data needed and never tracks users. See
  the [Privacy Policy](PRIVACY_POLICY.md).
- Cleartext traffic to Piped/NewLeaf instances and exported Piped tokens in
  query strings are deliberate upstream compatibility contracts, documented in
  [`ARCHITECTURE.md`](ARCHITECTURE.md#seguran%C3%A7a-aplicada).