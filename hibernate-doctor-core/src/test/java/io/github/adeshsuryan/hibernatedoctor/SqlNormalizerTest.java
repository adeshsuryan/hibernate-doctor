package io.github.adeshsuryan.hibernatedoctor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlNormalizerTest {

    @Test
    void replacesStringLiterals() {
        assertEquals(
                "select * from orders where status = ?",
                SqlNormalizer.normalize(
                        "SELECT * FROM orders WHERE status = 'SHIPPED'"));
    }

    @Test
    void replacesNumericLiterals() {
        assertEquals(
                "select * from orders where id = ?",
                SqlNormalizer.normalize("SELECT * FROM orders WHERE id = 42"));
    }

    @Test
    void collapsesWhitespaceAndCase() {
        assertEquals(
                "select * from a join b on a.id = b.a_id",
                SqlNormalizer.normalize("  SELECT  * FROM   a  JOIN\nb ON a.id = b.a_id  "));
    }

    @Test
    void keepsIdentifiersContainingDigits() {
        assertEquals(
                "select * from order2 where c1 = ?",
                SqlNormalizer.normalize("SELECT * FROM order2 WHERE c1 = 7"));
    }

    @Test
    void keepsPlaceholders() {
        assertEquals(
                "select * from customer where id = ?",
                SqlNormalizer.normalize("SELECT * FROM customer WHERE id = ?"));
    }

    @Test
    void sameShapeSameOutputRegardlessOfValues() {
        String a = SqlNormalizer.normalize(
                "SELECT * FROM customer WHERE id = 101");
        String b = SqlNormalizer.normalize(
                "SELECT * FROM customer WHERE id = 99999");
        assertEquals(a, b);
    }
}
