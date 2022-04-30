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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class UseStandardCharset extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `StandardCharset` constants";
    }

    @Override
    public String getDescription() {
        return "Replaces `Charset.forName(java.lang.String)` with the equivalent `StandardCharset` constant.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.nio.charset.Charset");
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new UseStandardCharsetVisitor();
    }

    private static class UseStandardCharsetVisitor extends JavaVisitor<ExecutionContext> {
        MethodMatcher CHARSET_FOR_NAME = new MethodMatcher("java.nio.charset.Charset forName(java.lang.String)");
        Map<String, String> charsetValueToCode = new HashMap<>();

        public UseStandardCharsetVisitor() {
            charsetValueToCode.put("US-ASCII", "StandardCharsets.US_ASCII");
            charsetValueToCode.put("ISO-8859-1", "StandardCharsets.ISO_8859_1");
            charsetValueToCode.put("UTF-8", "StandardCharsets.UTF_8");
            charsetValueToCode.put("UTF-16", "StandardCharsets.UTF_16");
            charsetValueToCode.put("UTF-16BE", "StandardCharsets.UTF_16BE");
            charsetValueToCode.put("UTF-16LE", "StandardCharsets.UTF_16LE");
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if (CHARSET_FOR_NAME.matches(m)) {
                String maybeReplace = (String) ((J.Literal) m.getArguments().get(0)).getValue();
                if (charsetValueToCode.containsKey(maybeReplace)) {
                    maybeAddImport("java.nio.charset.StandardCharsets");
                    return m.withTemplate(JavaTemplate.builder(this::getCursor, charsetValueToCode.get(maybeReplace))
                            .imports("java.nio.charset.StandardCharsets").build(), m.getCoordinates().replace());
                }
            }
            return m;
        }
    }
}
