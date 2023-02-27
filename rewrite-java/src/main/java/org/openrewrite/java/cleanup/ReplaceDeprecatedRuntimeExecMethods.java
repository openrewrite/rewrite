/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ReplaceDeprecatedRuntimeExecMethods extends Recipe {
    private static final MethodMatcher RUNTIME_EXEC = new MethodMatcher("java.lang.Runtime exec(String)");
    private static final MethodMatcher RUNTIME_EXEC_ENVP = new MethodMatcher("java.lang.Runtime exec(String, String[])");

    @Override
    public String getDisplayName() {
        return "Replace Deprecated Runtime.Exec() methods";
    }

    @Override
    public String getDescription() {
        return "Replace Runtime.exec(String) methods to use exec(String[]) instead because the former is deprecated " +
               "after java 18 and no longer recommended for use by the Java documentation.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesJavaVersion<>(18);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                            ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                if (RUNTIME_EXEC.matches(m)) {
                    Expression command = m.getArguments().get(0);
                    List<Expression> commands = new ArrayList<>();
                    boolean flattenAble= ChainStringBuilderAppendCalls.flatAdditiveExpressions(command, commands);

                    StringBuilder sb = new StringBuilder();
                    if (flattenAble) {
                        for (Expression e : commands) {
                            if (e instanceof J.Literal && ((J.Literal) e).getType() == JavaType.Primitive.String) {
                                sb.append(((J.Literal) e).getValue());
                            } else {
                                flattenAble = false;
                                break;
                            }
                        }
                    }

                    if (flattenAble) {
                        String[] cmds = sb.toString().split(" ");
                        String templateCode = String.format("#{any(java.lang.Runtime)}.exec(new String[] {%s})", toStringArguments(cmds));
                        JavaTemplate template = JavaTemplate.builder(
                            this::getCursor, templateCode).build();
                        return m.withTemplate(template, m.getCoordinates().replace(), m.getSelect());
                    } else {
                        // replace argument to 'command.split(" ")'
                        JavaTemplate template = JavaTemplate.builder(
                            this::getCursor, "#{any(java.lang.Runtime)}.exec(#{any(java.lang.String)}.split(\" \"))").build();
                        Expression splitSelect = m.getArguments().get(0);

                        if (!(splitSelect instanceof J.Identifier)
                            && !(splitSelect instanceof J.Literal)
                            && !(splitSelect instanceof J.MethodInvocation)) {
                            splitSelect = ReplaceStringBuilderWithString.wrapExpression(splitSelect);
                        }

                        return m.withTemplate(template, m.getCoordinates().replace(), m.getSelect(), splitSelect);
                    }
                } else if (RUNTIME_EXEC_ENVP.matches(m)) {
                    Expression command = m.getArguments().get(0);
                    List<Expression> commands = new ArrayList<>();
                    boolean flattenAble= ChainStringBuilderAppendCalls.flatAdditiveExpressions(command, commands);

                    StringBuilder sb = new StringBuilder();
                    if (flattenAble) {
                        for (Expression e : commands) {
                            if (e instanceof J.Literal && ((J.Literal) e).getType() == JavaType.Primitive.String) {
                                sb.append(((J.Literal) e).getValue());
                            } else {
                                flattenAble = false;
                                break;
                            }
                        }
                    }

                    if (flattenAble) {
                        String[] cmds = sb.toString().split(" ");
                        String templateCode = String.format("#{any(java.lang.Runtime)}.exec(new String[] {%s}, #{anyArray(java.lang.String)})", toStringArguments(cmds));
                        JavaTemplate template = JavaTemplate.builder(
                            this::getCursor, templateCode).build();
                        return m.withTemplate(template, m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(1));
                    } else {
                        // replace argument to 'command.split(" ")'
                        JavaTemplate template = JavaTemplate.builder(
                            this::getCursor, "#{any(java.lang.Runtime)}.exec(#{any(java.lang.String)}.split(\" \"), #{anyArray(java.lang.String)})").build();
                        Expression splitSelect = m.getArguments().get(0);

                        if (!(splitSelect instanceof J.Identifier)
                            && !(splitSelect instanceof J.Literal)
                            && !(splitSelect instanceof J.MethodInvocation)) {
                            splitSelect = ReplaceStringBuilderWithString.wrapExpression(splitSelect);
                        }

                        return m.withTemplate(template, m.getCoordinates().replace(), m.getSelect(), splitSelect, m.getArguments().get(1));
                    }

                }

                return m;
            }
        };
    }

    private static String toStringArguments(String[] cmds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cmds.length; i++) {
            String token = cmds[i];
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("\"")
                .append(token)
                .append("\"");
        }
        return sb.toString();
    }
}
