# hibernate-doctor

<p align="center">
  <img src="docs/assets/banner.svg" alt="hibernate-doctor" width="900">
</p>

Your Spring Boot + Hibernate app works fine on small data. In production, one
request fires 101 SQL statements — the classic N+1 — and nobody noticed until
the database melted.

**hibernate-doctor** is a small, zero-dependency library that scopes every
statement to the operation that fired it, detects the shapes that hurt
(N+1, unbounded fetches, repeated writes, slow queries), and reports findings
with evidence and fix guidance. Same doctor shape as
[next-cache-doctor](https://github.com/adeshsuryan/next-cache-doctor) and
[next-build-doctor](https://github.com/adeshsuryan/next-build-doctor), for the JVM.

```text
Hibernate doctor

OPERATION  GET /api/orders (request scope)
Statements executed: 51   wall time: 412 ms   db time: 287 ms

FINDINGS
HIGH  n-plus-one-select       select * from customer where id = ?  x50
      confidence: heuristic   One statement shape fired 50 times with a
                              single-key WHERE clause — the classic lazy-load N+1 signature.

INVESTIGATE
1. Find the lazy association loaded inside the loop.
2. Try JOIN FETCH, an entity graph, a DTO projection, or @BatchSize.
3. Pin this operation with a query-budget test.
```

## Install

Maven:

```xml
<dependency>
    <groupId>io.github.adeshsuryan</groupId>
    <artifactId>hibernate-doctor-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("io.github.adeshsuryan:hibernate-doctor-spring-boot-starter:0.1.0")
```

That's it on Spring Boot 3.x — the starter wraps your `DataSource`, scopes
every HTTP request, and logs findings automatically.

## Quickstart — pin a query budget in a test

```java
@RegisterExtension
HibernateDoctorExtension doctor = new HibernateDoctorExtension();

@Test
void orderLookupStaysInBudget() {
    doctor.assertBudget("order lookup",
            () -> orderService.findOrders(customerId),
            Budget.maxQueries(5));
}
```

When an N+1 slips in, the test fails with the full report embedded — the
assertion error **is** the diagnosis.

## What it detects (v1 rules)

| Rule | Severity | Confidence | Fires when |
| ---- | -------- | ---------- | ---------- |
| `n-plus-one-select` | high | heuristic | Same single-key SELECT shape repeated ≥3× in one operation |
| `excessive-query-count` | high | exact | Statements in one operation exceed the budget (default 50) |
| `unbounded-result-set` | medium | exact | One statement returns >1000 rows with no LIMIT |
| `duplicate-statement` | medium | exact | Identical non-N+1 SELECT fired ≥3× |
| `slow-query-observation` | medium | exact | A statement exceeds the ms threshold (default 500) |
| `write-inside-read-flow` | low | exact | Writes inside an operation marked read-only |
| `flush-storm` | low | exact | Flush count exceeds threshold (needs Hibernate stats) |
| `batch-ignored` | low | heuristic | Repeated same-shape writes with `hibernate.jdbc.batch_size` unset |

`exact` = measured fact. `heuristic` = the pattern strongly suggests it —
verify before calling it the cause. The report never claims a proven root
cause.

## Configuration

```properties
hibernate-doctor.enabled=true            # master switch
hibernate-doctor.n-plus-one-threshold=3
hibernate-doctor.query-count-budget=50
hibernate-doctor.slow-query-ms=500
hibernate-doctor.unbounded-rows=1000
hibernate-doctor.report-mode=log-on-findings   # log-on-findings | log-always | off
```

## Without Spring Boot

```java
DataSource ds = DoctorDataSource.wrap(realDataSource);

Doctor.Report report = Doctor.get().check("order lookup", () -> {
    // any JDBC / Hibernate / JPA work through ds
});
System.out.println(report.text());   // or report.json()
```

## Privacy & safety

- **Bind values are never captured.** The capture layer only ever sees `?`
  placeholders; raw literals are normalized out before evidence is built.
  There is no flag to enable it — it's structurally absent.
- **Read-only.** It never mutates queries or entities.
- **Zero required dependencies** in `hibernate-doctor-core`.
- Disabled = no wrapping, no capture, near-zero overhead.

## Modules

| Artifact | Purpose |
| -------- | ------- |
| `hibernate-doctor-core` | Evidence model, JDBC capture, rules, renderers |
| `hibernate-doctor-spring-boot-starter` | Boot 3.x auto-config: DataSource wrap, request filter, properties |
| `hibernate-doctor-junit` | `HibernateDoctorExtension` + `Budget` assertions |

## Compatibility

| Tested | |
| ------ | --- |
| Java | 17+ |
| Spring Boot | 3.5.x |
| Hibernate ORM | 6.6.x (Boot-managed) |
| Database | Any JDBC — proof suite runs H2; PostgreSQL/MySQL matrix planned |

## Roadmap

- **0.2** — entity/association attribution (`suspect: Order.customer`)
- **0.3** — live diagnose endpoint for staging
- **0.4** — CI gate semantics, documented Actions usage
- **0.5** — Boot 4.x / Hibernate 7.x matrix, MySQL coverage
- **1.0** — stable rule IDs + JSON schema across two Boot minors

## License

[Apache-2.0](LICENSE)
