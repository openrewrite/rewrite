/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class PrimitiveWrapperClassConstructorToValueOf extends Recipe {

    private static final ThreadLocal<JavaParser> JAVA_PARSER_THREAD_LOCAL = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    @Override
    public String getDisplayName() {
        return "Use primitive wrapper `valueOf` method";
    }

    @Override
    public String getDescription() {
        return "The constructor of all primitive types has been deprecated in favor of using the static factory method `valueOf` available for each of the primitive type wrappers.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>("java.lang.Boolean"));
                doAfterVisit(new UsesType<>("java.lang.Byte"));
                doAfterVisit(new UsesType<>("java.lang.Character"));
                doAfterVisit(new UsesType<>("java.lang.Double"));
                doAfterVisit(new UsesType<>("java.lang.Float"));
                doAfterVisit(new UsesType<>("java.lang.Integer"));
                doAfterVisit(new UsesType<>("java.lang.Long"));
                doAfterVisit(new UsesType<>("java.lang.Short"));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                J j = super.visitNewClass(newClass, executionContext);
                J.NewClass nc = (J.NewClass) j;
                if (nc.getType() instanceof JavaType.FullyQualified && nc.getArguments() != null && nc.getArguments().size() == 1) {
                    JavaType.FullyQualified fqn = (JavaType.FullyQualified) nc.getType();
                    switch (fqn.getFullyQualifiedName()) {
                        case "java.lang.Boolean":
                        case "java.lang.Byte":
                        case "java.lang.Character":
                        case "java.lang.Double":
                        case "java.lang.Float":
                        case "java.lang.Integer":
                        case "java.lang.Long":
                        case "java.lang.Short":
                            j = nc.withTemplate(template(fqn.getClassName() + ".valueOf(#{});").javaParser(JAVA_PARSER_THREAD_LOCAL.get()).build(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                            break;
                        default:
                            break;
                    }
                }
                return j;
            }
        };
    }
}
