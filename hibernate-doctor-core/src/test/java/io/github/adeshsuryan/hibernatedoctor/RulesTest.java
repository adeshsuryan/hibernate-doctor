package io.github.adeshsuryan.hibernatedoctor;

import io.github.adeshsuryan.hibernatedoctor.rules.RuleRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RulesTest {

    private final RuleRegistry rules = new RuleRegistry();
    private final DoctorConfig config = DoctorConfig.defaults();

    private static OperationEvidence evidence(StatementEvidence... statements) {
        return new OperationEvidence(Scope.TEST, "test-op", false,
                List.of(statements), 1_000_000, null);
    }

    private static StatementEvidence selectN(int times) {
        StatementEvidence s = new StatementEvidence(
                "select * from customer where id = ?");
        for (int i = 0; i < times; i++) {
            s.record(5_000_000);
        }
        return s;
    }

    @Test
    void nPlusOneFiresOnRepeatedSingleKeySelect() {
        List<Finding> findings = rules.evaluate(evidence(selectN(100)), config);
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals("n-plus-one-select")));
        Finding f = findings.stream()
                .filter(x -> x.ruleId().equals("n-plus-one-select")).findFirst().get();
        assertEquals(Severity.HIGH, f.severity());
        assertEquals(Confidence.HEURISTIC, f.confidence());
    }

    @Test
    void nPlusOneSilentBelowThreshold() {
        List<Finding> findings = rules.evaluate(evidence(selectN(2)), config);
        assertTrue(findings.stream().noneMatch(f -> f.ruleId().equals("n-plus-one-select")));
    }

    @Test
    void nPlusOneSilentOnNonSelect() {
        StatementEvidence s = new StatementEvidence("insert into t (a) values (?)");
        for (int i = 0; i < 100; i++) {
            s.record(1_000_000);
        }
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.stream().noneMatch(f -> f.ruleId().equals("n-plus-one-select")));
    }

    @Test
    void excessiveCountFiresOverBudget() {
        List<StatementEvidence> stmts = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            StatementEvidence s = new StatementEvidence(
                    "select * from t" + i + " where x = ?");
            s.record(100_000);
            stmts.add(s);
        }
        List<Finding> findings = rules.evaluate(
                evidence(stmts.toArray(new StatementEvidence[0])), config);
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals("excessive-query-count")));
    }

    @Test
    void unboundedFiresOnLargeResultNoLimit() {
        StatementEvidence s = new StatementEvidence("select * from orders");
        s.record(10_000_000);
        s.addRows(5000);
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals("unbounded-result-set")));
    }

    @Test
    void unboundedSilentWhenLimited() {
        StatementEvidence s = new StatementEvidence(
                "select * from orders limit ?");
        s.record(10_000_000);
        s.addRows(5000);
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.stream().noneMatch(f -> f.ruleId().equals("unbounded-result-set")));
    }

    @Test
    void slowQueryFiresOverThreshold() {
        StatementEvidence s = new StatementEvidence("select * from big_table");
        s.record(2_000_000_000L); // 2s
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals("slow-query-observation")));
    }

    @Test
    void writeInsideReadFlowFiresWhenMarkedReadOnly() {
        StatementEvidence s = new StatementEvidence("update t set a = ?");
        s.record(100_000);
        OperationEvidence ev = new OperationEvidence(Scope.TEST, "read-op",
                true, List.of(s), 1_000_000, null);
        List<Finding> findings = rules.evaluate(ev, config);
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals("write-inside-read-flow")));
    }

    @Test
    void writeInsideReadFlowSilentWhenNotMarked() {
        StatementEvidence s = new StatementEvidence("update t set a = ?");
        s.record(100_000);
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.stream().noneMatch(f -> f.ruleId().equals("write-inside-read-flow")));
    }

    @Test
    void flushStormSilentWithoutStats() {
        List<Finding> findings = rules.evaluate(evidence(selectN(1)), config);
        assertTrue(findings.stream().noneMatch(f -> f.ruleId().equals("flush-storm")));
    }

    @Test
    void batchIgnoredFiresOnRepeatedWritesWithoutBatching() {
        StatementEvidence s = new StatementEvidence("insert into audit (a) values (?)");
        for (int i = 0; i < 20; i++) {
            s.record(500_000);
        }
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.stream().anyMatch(f -> f.ruleId().equals("batch-ignored")));
    }

    @Test
    void batchIgnoredSilentWhenBatchConfigured() {
        StatementEvidence s = new StatementEvidence("insert into audit (a) values (?)");
        for (int i = 0; i < 20; i++) {
            s.record(500_000);
        }
        DoctorConfig batched = DoctorConfig.builder().jdbcBatchConfigured(true).build();
        List<Finding> findings = rules.evaluate(evidence(s), batched);
        assertTrue(findings.stream().noneMatch(f -> f.ruleId().equals("batch-ignored")));
    }

    @Test
    void cleanEvidenceProducesNoFindings() {
        StatementEvidence s = new StatementEvidence(
                "select * from customer where id = ? limit ?");
        s.record(2_000_000);
        s.addRows(10);
        List<Finding> findings = rules.evaluate(evidence(s), config);
        assertTrue(findings.isEmpty(), "expected no findings, got " + findings);
    }
}
