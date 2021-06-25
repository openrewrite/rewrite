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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class PrimitiveWrapperClassConstructorToValueOf extends Recipe {

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
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(nc.getType());
                if (type != null && nc.getArguments() != null && nc.getArguments().size() == 1) {
                    JavaTemplate.Builder valueOf = null;
                    switch (type.getFullyQualifiedName()) {
                        case "java.lang.Boolean":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(boolean)});");
                            break;
                        case "java.lang.Byte":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(byte)});");
                            break;
                        case "java.lang.Character":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(char)});");
                            break;
                        case "java.lang.Double":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(double)});");
                            break;
                        case "java.lang.Float":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(float)});");
                            break;
                        case "java.lang.Integer":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(int)});");
                            break;
                        case "java.lang.Long":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(long)});");
                            break;
                        case "java.lang.Short":
                            valueOf = JavaTemplate.builder(this::getCursor, "#{}.valueOf(#{any(short)});");
                            break;
                        default:
                            break;
                    }

                    if(valueOf != null) {
                        j = nc.withTemplate(valueOf.build(), nc.getCoordinates().replace(),
                                type.getClassName(), nc.getArguments().get(0));
                    }
                }
                return j;
            }
        };
    }
}
