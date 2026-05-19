  # Security Policy

  ## Supported Versions

  This project is maintained as a reference application. Security
  fixes are applied to the latest version on `master` and to the currently deployed
  demo version.

  | Version | Supported |
  | ------- | --------- |
  | Latest  | Yes       |
  | Older releases/tags | No |

  ## Reporting a Vulnerability

  Please do not open a public GitHub issue for security vulnerabilities.

  Report vulnerabilities through GitHub private vulnerability reporting for this
  repository. Include:

  - a short description of the issue,
  - affected area, for example backend, frontend, Kubernetes, CI/CD, or seed data,
  - reproduction steps or proof of concept,
  - potential impact,
  - suggested fix, if known.

  I will try to acknowledge valid reports within 7 days.

  If the issue is accepted, I will prioritize a fix based on impact and may publish
  a security advisory or release note after the fix is available. If the report is
  declined, I will explain why when possible.

  ## Scope

  In scope:

  - authentication and authorization issues,
  - exposed secrets or credentials,
  - dependency vulnerabilities with practical impact,
  - unsafe file or object-storage access,
  - deployment configuration that exposes private services or data.

  Out of scope:

  - denial-of-service against the public demo instance,
  - automated scanner output without a reproducible impact,
  - vulnerabilities in unsupported old tags,
  - issues that require access to private local configuration or secrets,
  - social engineering or physical attacks.

  ## Public Demo

  The public deployment is a demo environment. Please do not attempt destructive
  testing, data exfiltration, privilege escalation beyond a minimal proof of
  concept, or attacks against infrastructure outside this repository.
