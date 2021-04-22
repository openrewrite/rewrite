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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class PrimitiveWrapperClassConstructorToValueOf extends Recipe {

    private static final ThreadLocal<JavaParser> JAVA_PARSER_THREAD_LOCAL = ThreadLocal.withInitial(() -> JavaParser.fromJavaVersion().build());

    @Override
    public String getDisplayName() {
        return "Convert Primitive Wrapper Class Constructors to valueOf Method";
    }

    @Override
    public String getDescription() {
        return "The constructor of all primitive types has been deprecated in favor of using the static factory method valueOf available of each of the primitive types. This is a recipe to convert these constructors to their valueOf counterparts";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                J j = super.visitNewClass(newClass, executionContext);
                J.NewClass nc = (J.NewClass) j;
                if (nc.getType() instanceof JavaType.FullyQualified && nc.getArguments() != null && nc.getArguments().getElements().size() == 1) {
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
                            j = nc.withTemplate(template(fqn.getClassName() + ".valueOf(#{});").javaParser(JAVA_PARSER_THREAD_LOCAL.get()).build(), nc.getCoordinates().replace(), nc.getArguments().getElements().get(0));
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
