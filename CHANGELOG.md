# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

[0.1.0]: https://github.com/adeshsuryan/hibernate-doctor/releases/tag/v0.1.0
