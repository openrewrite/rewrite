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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MigrateJavaTemplateToRewrite8 extends Recipe {
    private static final String CONTEXT_SENSITIVE_COMMENT = "[Rewrite8 migration] contextSensitive() could be unnecessary, please follow the migration guide";
    private static final String GET_CURSOR_COMMENT = "[Rewrite8 migration] getCursor() could be updateCursor() if the J instance is updated, or it should be updated to point to the correct cursor, please follow the migration guide";

    private static final MethodMatcher templateBuilderMethodMatcher = new MethodMatcher("org.openrewrite.java.JavaTemplate builder(java.lang.String)", true);
    @Nullable private static J.MethodInvocation builderTemplate = null;
    @Nullable private static J.MethodInvocation applyTemplate = null;

    @Override
    public String getDisplayName() {
        return "Migrate `JavaTemplate` to accommodate Rewrite 8";
    }

    @Override
    public String getDescription() {
        return "Migrate `JavaTemplate` to accommodate Rewrite 8, due to wide open-ended usage of JavaTemplate, " +
               "this recipe just apply most of common changes to pass compile and will leave some comments to require human's review.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("Rewrite8 migration");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>(){
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                            ExecutionContext ctx) {
                method = super.visitMethodInvocation(method, ctx);

                // Change `JavaTemplate.builder(P1, P2)` to `JavaTemplate.builder(P2).contextSensitive()`
                if (method.getSelect() != null &&
                    method.getSelect().getType() != null &&
                    TypeUtils.isOfClassType(method.getSelect().getType(), "org.openrewrite.java.JavaTemplate") &&
                    method.getArguments().size() == 2 &&
                    method.getSimpleName().equals("builder")) {
                    return createTemplateBuilderMethodInvocation(method.getArguments().get(1));
                }

                // Change `X.withTemplate(t, coordinate, params)` to `t.apply(cursor, coordinate, params)`
                if (method.getSimpleName().equals("withTemplate") &&
                    method.getSelect() != null &&
                    method.getArguments().size() >= 2 &&
                    TypeUtils.isAssignableTo("org.openrewrite.java.tree.J", method.getSelect().getType())
                ) {
                    List<Expression> args = method.getArguments();
                    J.MethodInvocation applyMethodCall = buildApplyMethodInvocation();
                    applyMethodCall = applyMethodCall.withSelect(args.get(0).withPrefix(Space.EMPTY));
                    List<Expression> newArgs = applyMethodCall.getArguments();
                    // replace the 2nd parameter and append following parameters if any
                    newArgs.set(1, args.get(1));
                    for (int i = 2; i < args.size(); i++) {
                        newArgs.add(args.get(i));
                    }
                    applyMethodCall = applyMethodCall.withArguments(newArgs);
                    return autoFormat(applyMethodCall, ctx) ;
                }
                return method;
            }
        };
    }

    private static J.MethodInvocation createTemplateBuilderMethodInvocation(Expression templateString) {
        J.MethodInvocation builderTemplate = buildBuilderMethodInvocation();
        builderTemplate = new JavaIsoVisitor<Expression>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Expression param) {
                if (templateBuilderMethodMatcher.matches(method.getMethodType())) {
                    return method.withArguments(Collections.singletonList(param));
                }
                return super.visitMethodInvocation(method, param);
            }
        }.visitMethodInvocation(builderTemplate, templateString);
        return builderTemplate;
    }

    @SuppressWarnings("all")
    private static J.MethodInvocation buildBuilderMethodInvocation() {
        if (builderTemplate == null) {
            //noinspection OptionalGetWithoutIsPresent
            J.CompilationUnit cu = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build()
                .parse("import org.openrewrite.java.JavaTemplate;\n" +
                       "public class Demo {void method() {JavaTemplate.builder(\"\")" +
                       "/*" + CONTEXT_SENSITIVE_COMMENT + "*/" +
                       ".contextSensitive();}}")
                .map(J.CompilationUnit.class::cast)
                .findFirst()
                .get();

            builderTemplate = (J.MethodInvocation)((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getBody().getStatements().get(0);
        }
        return builderTemplate;
    }

    @SuppressWarnings("all")
    private static J.MethodInvocation buildApplyMethodInvocation() {
        if (applyTemplate == null) {
            //noinspection OptionalGetWithoutIsPresent
            J.CompilationUnit cu = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build()
                .parse("import org.openrewrite.java.JavaTemplate;\n" +
                       "import org.openrewrite.java.JavaVisitor;\n" +
                       "import org.openrewrite.java.tree.J;\n" +
                       "public class DemoVisitor extends JavaVisitor {\n" +
                       "    @Override\n" +
                       "    public J visitMethodInvocation(J.MethodInvocation method, Object o) {\n" +
                       "        JavaTemplate t = JavaTemplate.builder(\"\").build();\n" +
                       "        return t.apply(" +
                       "/*" + GET_CURSOR_COMMENT + "*/" +
                       "getCursor(), null);\n" +
                       "    }\n" +
                       "}")
                .map(J.CompilationUnit.class::cast)
                .findFirst()
                .get();

            applyTemplate = (J.MethodInvocation)((J.Return)((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getBody().getStatements().get(1)).getExpression();
        }

        return applyTemplate;
    }
}
