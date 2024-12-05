/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.groovy;

import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GroovyParserParentheseDiscoverer {
    // Matches a code block including leading and trailing parenthesis
    // Eg: ((((a.invoke~~>("arg")))))<~~ or ~~>(((((( "" as String )))))<~~.toString())
    private static final Pattern PARENTHESES_GROUP = Pattern.compile(".*?(\\(+[^()]+\\)+).*", Pattern.DOTALL);
    // Matches when string consist of multiple parenthese blocks
    // Eg: `((a.invoke())` does not match, `((a.invoke() && b.invoke())` does match
    private static final Pattern HAS_SUB_PARENTHESIS = Pattern.compile("\\(+[^)]+\\)+[^)]+\\).*", Pattern.DOTALL);

    /**
     * Calculates the insideParenthesesLevel for method calls, because the compiler does not set the _INSIDE_PARENTHESES_LEVEL flag for method calls.
     */
    public static @Nullable Integer getInsideParenthesesLevel(MethodCallExpression node, String source, int cursor) {
        String sourceFromCursor = source.substring(cursor);

        // start with a (` character with optional whitespace
        if (sourceFromCursor.matches("(?s)^\\s*\\(.*")) {
            String sourceNoWhitespace = sourceFromCursor.replaceAll("\\s+", "");
            // grab the source code until method and closing parenthesis
            Matcher m = Pattern.compile("(?s)(.*?" + node.getMethodAsString() + "[^)]*\\)+).*").matcher(sourceNoWhitespace);
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
        // lookup for first code block with parenthesis
        Matcher m = PARENTHESES_GROUP.matcher(s);
        // check if source matches any code block with parenthesis + check if there are still inner parenthesis
        while (m.matches() && HAS_SUB_PARENTHESIS.matcher(s).find()) {
            int parenthesis = lowestParentheseLevel(m.group(1));
            String part = m.group(1).replaceAll("\\(", "").replaceAll("\\)", "");
            String regex = StringUtils.repeat("\\(", parenthesis) + part + StringUtils.repeat("\\)", parenthesis);
            // remove parentheses and arguments in source code
            s = s.replaceAll(regex, "");
            // move to next possible code block with parenthesis
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
