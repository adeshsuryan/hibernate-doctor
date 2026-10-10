package io.github.adeshsuryan.hibernatedoctor;

/**
 * Normalizes SQL into a stable comparison shape. Replaces string and
 * numeric literals with {@code ?}, collapses whitespace, lowercases.
 * This is also the privacy guarantee: literal values never enter the
 * evidence model.
 */
public final class SqlNormalizer {

    private SqlNormalizer() {
    }

    public static String normalize(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(sql.length());
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                    out.append('?');
                }
                continue;
            }
            if (inDouble) {
                if (c == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (c == '\'') {
                inSingle = true;
                continue;
            }
            if (c == '"') {
                inDouble = true;
                out.append(c);
                continue;
            }
            if (Character.isDigit(c)
                    && (out.isEmpty() || !isIdentChar(out.charAt(out.length() - 1)))) {
                int j = i;
                while (j < sql.length() && (Character.isDigit(sql.charAt(j))
                        || sql.charAt(j) == '.')) {
                    j++;
                }
                if (j < sql.length() && isIdentChar(sql.charAt(j))) {
                    out.append(c);
                    continue;
                }
                out.append('?');
                i = j - 1;
                continue;
            }
            out.append(Character.isWhitespace(c) ? ' ' : c);
        }
        return out.toString().trim().replaceAll(" +", " ").toLowerCase();
    }

    private static boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
