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
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Recipe to use {@link String#replace(CharSequence, CharSequence)} when the fist argument is not a regular expression.
 * <p>
 * The underlying implementation of {@link String#replaceAll(String, String)} calls the {@link Pattern#compile(String)}
 * method each time it is called even if the first argument is not a regular expression. This has a significant
 * performance cost and therefore should be used with care.
 *
 * @see <a href="https://rules.sonarsource.com/java/RSPEC-5361"></a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/essential/regex/index.html"></a>
 */
public class UseStringReplace extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `String::replace()` when fist parameter is not a real regular expression";
    }

    @Override
    public String getDescription() {
        return "When `String::replaceAll` is used, the first argument should be a real regular expression. " +
                "If it’s not the case, `String::replace` does exactly the same thing as `String::replaceAll` without the performance drawback of the regex.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-5361");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UseStringReplaceVisitor();
    }

    private static class UseStringReplaceVisitor extends JavaVisitor<ExecutionContext> {

        private static final MethodMatcher REPLACE_ALL = new MethodMatcher("java.lang.String replaceAll(..)");
        private static final Pattern ESCAPED_CHARACTER = Pattern.compile("\\\\\\.");
        private static final Pattern METACHARACTERS = Pattern.compile("[(\\[{\\\\^\\-=$!|\\]})?*+.]");
        private static final Pattern CHARACTER_CLASSES = Pattern.compile("\\\\d|\\\\D|\\\\s|\\\\S|\\\\w|\\\\W");

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
            J.MethodInvocation invocation = (J.MethodInvocation) super.visitMethodInvocation(method, context);

            //Checks if method invocation matches with String#replaceAll
            if (REPLACE_ALL.matches(invocation)) {
                Expression firstArgument = invocation.getArguments().get(0);

                //Checks if the first argument is a String literal
                if (isStringLiteral(firstArgument)) {
                    J.Literal literal = (J.Literal) firstArgument;
                    String value = (String) literal.getValue();

                    //Checks if the String literal may not be a regular expression,
                    //if so, then change the method invocation name
                    if (Objects.nonNull(value) && !mayBeRegExp(value)) {
                        String unEscapedLiteral = unEscapeCharacters(value);
                        invocation = invocation.withTemplate(JavaTemplate.builder(this::getCursor,
                                "#{any(java.lang.String)}.replace(\"#{}\", #{any(java.lang.String)})")
                                .doBeforeParseTemplate(System.out::println).build(),
                                invocation.getCoordinates().replace(), invocation.getSelect(), unEscapedLiteral, invocation.getArguments().get(1));
                    }
                }
            }

            return invocation;
        }

        private boolean isStringLiteral(Expression expression) {
            return expression instanceof J.Literal && TypeUtils.isString(((J.Literal) expression).getType());
        }

        private boolean mayBeRegExp(String argument) {
            //Remove all escaped characters and then checks if argument contains any metacharacter or any character class
            String cleanedValue = ESCAPED_CHARACTER.matcher(argument).replaceAll("");
            return METACHARACTERS.matcher(cleanedValue).find() || CHARACTER_CLASSES.matcher(cleanedValue).find();
        }

        private String unEscapeCharacters(String argument) {
            return argument.replace("\\\\", "\\")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
                    .replace("\\", "")
                    .replace("\"", "\\\"");
        }
    }
}
