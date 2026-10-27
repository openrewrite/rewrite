/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * TODO is there a more general purpose need for this?
 */
@Incubating(since = "7.22.0")
public class ChangeStringLiteral {

    /**
     * Opening to closing delimiter, ordered longest opening first so that {@code '''} matches before {@code '}.
     */
    private static final String[][] DELIMITERS = {
            {"~\"\"\"", "\"\"\""}, {"~'''", "'''"}, {"\"\"\"", "\"\"\""}, {"'''", "'''"},
            {"~\"", "\""}, {"~'", "'"}, {"~/", "/"}, {"$/", "/$"},
            {"\"", "\""}, {"'", "'"}, {"/", "/"}
    };

    public static Expression withStringValue(Expression e, String newValue) {
        return (Expression) new JavaVisitor<Integer>() {
            @Override
            public J visitLiteral(J.Literal literal, Integer integer) {
                return withStringValue(literal, newValue);
            }
        }.visitNonNull(e, 0);
    }

    public static J.Literal withStringValue(J.Literal l, String newValue) {
        String oldValue = (String) l.getValue();
        if (oldValue == null || oldValue.equals(newValue)) {
            return l;
        }
        String[] delimiters = delimitersOf(l.getValueSource(), oldValue);
        return l.withValue(newValue).withValueSource(delimiters[0] + newValue + delimiters[1]);
    }

    /**
     * What encloses {@code value} in {@code valueSource} — nothing at all for a string template fragment.
     */
    private static String[] delimitersOf(@Nullable String valueSource, String value) {
        if (valueSource == null) {
            return new String[]{"'", "'"};
        }
        for (String[] delimiter : DELIMITERS) {
            int enclosed = valueSource.length() - delimiter[0].length() - delimiter[1].length();
            // An escape sequence is longer than the character it spells, so a delimited literal always has at
            // least as many characters between its delimiters as its value has
            if (enclosed >= value.length() && valueSource.startsWith(delimiter[0]) && valueSource.endsWith(delimiter[1])) {
                return delimiter;
            }
        }
        return new String[]{"", ""};
    }
}
