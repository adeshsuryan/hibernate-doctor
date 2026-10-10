# Security Policy

## Guarantees this library makes

- **Read-only.** hibernate-doctor observes and reports. It never mutates
  SQL, never rewrites entities, never touches application data.
- **No bind values, ever.** The capture layer reads SQL at the JDBC
  `PreparedStatement` boundary, which only ever contains `?` placeholders.
  Raw literals in `Statement.execute(sql)` calls are stripped during
  normalization before they reach the evidence model. There is no flag to
  enable parameter capture — it is structurally absent.
- **No side effects when disabled.** With `hibernate-doctor.enabled=false`
  (or when no operation boundary is open) the proxy records nothing and
  adds a near-zero check per execution.
- **No network calls.** The library makes no outbound connections of its
  own. Report surfaces added in later versions are opt-in and documented
  here when they land.

## Reporting surfaces

- The request-scope filter writes findings to the application log
  (`hibernate-doctor.report-mode`). Statement *shapes* reveal schema
  table/column names — keep reports out of public logs if schema secrecy
  matters.

## Supported versions

| Version | Supported |
| ------- | --------- |
| 0.1.x   | yes       |

Tested against Spring Boot 3.5.x (Hibernate ORM 6.6) with JDK 17+.
Other combinations may work but are not covered by this policy yet.

## Reporting a vulnerability

Open a private security advisory on
[GitHub](https://github.com/adeshsuryan/hibernate-doctor/security/advisories/new)
rather than a public issue.
