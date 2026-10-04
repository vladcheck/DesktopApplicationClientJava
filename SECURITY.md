# Security Policy

## Supported Versions

This project follows a rolling release model. Security fixes are applied to the latest released version only.

| Version        | Supported          |
| -------------- | ------------------ |
| 1.0.x (latest) | Yes |
| < 1.0          | No                |

As the project matures, this table will be updated to reflect long-term support (LTS) branches if applicable.

## Reporting a Vulnerability

We take the security of **DesktopApplicationClientJava** seriously. If you believe you have found a security vulnerability, please help us address it responsibly.

### How to Report

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Instead, use one of the following private channels:

1. **GitHub Private Vulnerability Reporting** (preferred)
   Navigate to the repository's [Security tab](https://github.com/vladcheck/DesktopApplicationClientJava/security) and click **"Report a vulnerability"**.

### What to Include

To help us triage and resolve the issue quickly, please include:

- A clear description of the vulnerability and its potential impact.
- The affected component (e.g., module, class, or dependency).
- Step-by-step reproduction instructions or a proof-of-concept (PoC).
- The affected version(s) and environment (OS, JDK version, JavaFX version).
- Any suggested mitigation or fix, if known.
- Whether you wish to be credited in the advisory.

### What to Expect

| Stage                                 | Target Timeframe                 |
| ------------------------------------- | -------------------------------- |
| Acknowledgement of report             | within **3 business days**       |
| Initial assessment / triage           | within **7 business days**       |
| Status update                         | every **14 days** until resolved |
| Fix or mitigation for critical issues | as soon as reasonably possible   |

If the vulnerability is accepted, we will:

- Work with you to understand and validate the issue.
- Prepare and release a patched version.
- Publish a security advisory crediting you (unless you prefer to remain anonymous).

If the vulnerability is declined, we will explain our reasoning.

## Scope

### In Scope

- Source code in this repository (Java/JavaFX application code).
- Configuration files, build scripts (`pom.xml`), and CI/CD pipelines maintained in this repository.
- Dependency vulnerabilities that are exploitable through this application.

### Out of Scope

- Vulnerabilities in third-party dependencies that are already publicly disclosed and have no exploitable path in this application (please report those upstream).
- Issues requiring physical access to an unlocked device.
- Social engineering attacks against contributors or users.
- Denial-of-service via resource exhaustion on the local machine.
- Findings from automated scanners submitted without a demonstrated impact.

## Security Practices

This project aims to follow these practices:

- **Dependency hygiene** — Dependencies are declared with pinned versions in `pom.xml`. We monitor advisories (e.g., via GitHub Dependabot) and update where practical.
- **Build reproducibility** — The Maven build uses fixed plugin and dependency versions, and the compiler targets Java 25.
- **Static analysis** — Code formatting and analysis are enforced via Spotless (`google-java-format`) during the `verify` phase.
- **Least privilege** — The application is a desktop client and does not require elevated privileges to run.
- **Secrets management** — No credentials, tokens, or private keys are committed to the repository. Secrets must be supplied via environment variables or a secure secret store.

## Handling of Sensitive Data

If the application processes user data, tokens, or credentials, the following apply:

- Sensitive values must not be logged.
- Data at rest should be stored using OS-appropriate secure storage.
- Network communication should use TLS.
- Jackson is used for (de)serialization; deserialization of untrusted input is restricted to known-safe types where possible.

## Dependency Vulnerability Disclosure

If you discover a vulnerability in a dependency used by this project:

1. Report it to the upstream maintainer first.
2. Notify us if the issue is exploitable through this application so we can pin, patch, or mitigate.

## Security Updates

Security fixes will be released as patch versions. Users are encouraged to:

- Watch the repository for releases.
- Subscribe to GitHub security advisories for this project.
- Rebuild from the latest tagged release.

## Acknowledgements

We thank the security researchers and community members who responsibly disclose issues and help keep this project and its users safe.

---

**Adopted:** 04.10.2026
**Last Reviewed:** 04.10.2026
**Approved By:** Vladimir Valekzhanin / vladcheck