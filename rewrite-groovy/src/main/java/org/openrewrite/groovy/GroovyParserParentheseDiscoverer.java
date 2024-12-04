package org.openrewrite.groovy;

import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyParserParentheseDiscoverer {
    // Matches the first method invocation arguments including leading and trailing parenthesis
    // Eg: ((((a.invoke~~>("arg")))))<~~
    private static final Pattern PARENTHESES_GROUP = Pattern.compile(".*?(\\(+[^()]+\\)+).*", Pattern.DOTALL);
    // Matches when string consist of multiple parenthese blocks
    // Eg: `((a.invoke())` does not match, `((a.invoke() && b.invoke())` does match
    private static final Pattern HAS_SUB_PARENTHESIS = Pattern.compile("\\(+[^)]+\\)+[^)]+\\).*", Pattern.DOTALL);

    public static @Nullable Integer getInsideParenthesesLevel(MethodCallExpression node, String source) {
        if (node.getObjectExpression() instanceof CastExpression) {
            return null;
        }

        // start with a (` character with optional whitespace
        if (source.matches("(?s)^\\s*\\(.*")) {
            // grab the source code until method and closing parenthesis
            Matcher m = Pattern.compile("(?s)(.*" + node.getMethodAsString() + "[^)]*\\)+).*").matcher(source);
            if (m.matches()) {
                // Replace all string literals with `<s>` to prevent weird matches with the PARENTHESES_GROUP regex
                String s = m.group(1).replaceAll("\"[^\"]*\"", "<s>");
                return GroovyParserParentheseDiscoverer.getInsideParenthesesLevelForMethodCalls(s);
            }
        }

        return null;
    }

    private static int getInsideParenthesesLevelForMethodCalls(String source) {
        String s = source;
        Matcher m = PARENTHESES_GROUP.matcher(s);
        while (m.matches() && HAS_SUB_PARENTHESIS.matcher(s).find()) {
            int parenthesis = lowestParentheseLevel(m.group(1));
            String part = m.group(1).replaceAll("\\(", "").replaceAll("\\)", "");
            String regex = StringUtils.repeat("\\(", parenthesis) + part + StringUtils.repeat("\\)", parenthesis);
            s = s.replaceAll(regex, "");
            m = PARENTHESES_GROUP.matcher(s);
        }

        return lowestParentheseLevel(s);
    }

    private static int lowestParentheseLevel(String s) {
        int leadingParenthesis = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
            else if (c == '(') leadingParenthesis++;
            else break;
        }
        int trailingParenthesis = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
            else if (c == ')') trailingParenthesis++;
            else break;
        }

        return Math.min(leadingParenthesis, trailingParenthesis);
    }
}
