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
import org.openrewrite.Preconditions;
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
    private static final MethodMatcher RUNTIME_EXEC_CMD = new MethodMatcher("java.lang.Runtime exec(String)");
    private static final MethodMatcher RUNTIME_EXEC_CMD_ENVP = new MethodMatcher("java.lang.Runtime exec(String, String[])");
    private static final MethodMatcher RUNTIME_EXEC_CMD_ENVP_FILE = new MethodMatcher("java.lang.Runtime exec(String, String[], java.io.File)");

    @Override
    public String getDisplayName() {
        return "Replace deprecated Runtime.Exec() methods";
    }

    @Override
    public String getDescription() {
        return "Replace `Runtime.exec(String)` methods to use `exec(String[])` instead because the former is deprecated " +
               "after Java 18 and is no longer recommended for use by the Java documentation.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(18), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                if (RUNTIME_EXEC_CMD.matches(m) || RUNTIME_EXEC_CMD_ENVP.matches(m) || RUNTIME_EXEC_CMD_ENVP_FILE.matches(m)) {
                    Expression command = m.getArguments().get(0);
                    List<Expression> commands = new ArrayList<>();
                    boolean flattenAble = ChainStringBuilderAppendCalls.flatAdditiveExpressions(command, commands);

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
                        String templateCode = String.format("new String[] {%s}", toStringArguments(cmds));
                        JavaTemplate template = JavaTemplate.builder(
                                this::getCursor, templateCode).build();

                        List<Expression> args = m.getArguments();
                        args.set(0, args.get(0).withTemplate(template, args.get(0).getCoordinates().replace()));

                        if (m.getMethodType() != null) {
                            List<JavaType> parameterTypes = m.getMethodType().getParameterTypes();
                            parameterTypes.set(0, JavaType.ShallowClass.build("java.lang.String[]"));

                            return m.withArguments(args)
                                    .withMethodType(m.getMethodType().withParameterTypes(parameterTypes));
                        }
                    } else {
                        // replace argument to 'command.split(" ")'
                        List<Expression> args = m.getArguments();
                        boolean needWrap = false;
                        Expression arg0 = args.get(0);
                        if (!(arg0 instanceof J.Identifier) &&
                            !(arg0 instanceof J.Literal) &&
                            !(arg0 instanceof J.MethodInvocation)) {
                            needWrap = true;
                        }

                        String code = needWrap ? "(#{any()}).split(\" \")" : "#{any()}.split(\" \")";
                        JavaTemplate template = JavaTemplate.builder(
                                this::getCursor, code).build();
                        arg0 = args.get(0).withTemplate(template, args.get(0).getCoordinates().replace(), args.get(0));
                        args.set(0, arg0);

                        if (m.getMethodType() != null) {
                            List<JavaType> parameterTypes = m.getMethodType().getParameterTypes();
                            parameterTypes.set(0, JavaType.ShallowClass.build("java.lang.String[]"));

                            return m.withArguments(args).withMethodType(m.getMethodType().withParameterTypes(parameterTypes));
                        }
                        return m;
                    }
                }

                return m;
            }
        });
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
