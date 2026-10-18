# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.1] - 2026-10-17

### Fixed

- JDBC proxies now unwrap `InvocationTargetException`, so `SQLException`
  reaches the caller unchanged — restoring Spring exception translation and
  `@Transactional` rollback matching on SQL error paths.
- `equals`/`hashCode`/`toString`/`unwrap`/`isWrapperFor` on proxied
  connections, statements, and result sets now preserve proxy identity and
  JDBC `Wrapper` semantics (a proxy previously compared unequal to itself).
- `executeBatch`/`executeLargeBatch` are now captured — prepared batches
  record one execution of the prepared SQL with the summed row count, and
  plain-statement batches record each distinct added SQL once.
- Result-set row counts are now also reported when the owning statement
  closes without an explicit `ResultSet.close()`.
- `Capture` no longer binds a `ThreadLocal` deque on threads that merely
  check `isActive()`/`record()` — near-zero cost on the cold path as
  documented.

### Security

- Default Spring Boot baseline 3.5.6 → 3.5.16 (October 2026 Spring
  Framework advisories; consumers' own Boot BOM governs runtime versions).
- Added `-Paudit` profile: SpotBugs, OWASP dependency-check (NVD), and
  CycloneDX SBOM generation.

### Added

- GitHub Actions CI: JDK 17/21 unit matrix, Spring Boot 3.2–3.5 matrix,
  audit job with SBOM artifact.
- `config/owasp-suppressions.xml` and `config/spotbugs-exclude.xml`.
- `JdbcProxyContractTest`: exception fidelity, proxy identity, batch
  capture, statement-close row reporting, cold-path ThreadLocal check.

### Changed

- JUnit version is now inherited from the Spring Boot BOM (5.12.x line)
  instead of a separate pinned property.

No public API changes.

## [0.1.0] - 2026-10-10

### Added

- `hibernate-doctor-core`: zero-dependency JDBC capture layer
  (`DoctorDataSource`), operation boundaries (TEST / REQUEST / MANUAL),
  evidence model, v1 rule set, text + JSON (`schemaVersion: 1`) reports.
- `hibernate-doctor-spring-boot-starter`: auto-configuration for Spring
  Boot 3.x — DataSource wrapping, request-scope filter, `hibernate-doctor.*`
  properties, Hibernate `Statistics` bridge.
- `hibernate-doctor-junit`: `HibernateDoctorExtension` with `Budget`
  assertions (`maxQueries`, `maxDbTimeMs`, `forbidRules`).
- Rules: `n-plus-one-select`, `excessive-query-count`,
  `duplicate-statement`, `unbounded-result-set`, `slow-query-observation`,
  `write-inside-read-flow`, `flush-storm`, `batch-ignored`.
- Privacy guarantee: bind parameter values are never captured — the capture
  layer only ever sees `?` placeholders, and literals are normalized out.

[0.1.1]: https://github.com/adeshsuryan/hibernate-doctor/releases/tag/v0.1.1
[0.1.0]: https://github.com/adeshsuryan/hibernate-doctor/releases/tag/v0.1.0
