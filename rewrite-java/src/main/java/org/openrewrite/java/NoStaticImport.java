/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NoStaticImport extends Recipe {

    /**
     * A method pattern that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.Collections emptyList()")
    String methodPattern;

    @Override
    public String getDisplayName() {
        return "Remove static import";
    }

    @Override
    public String getDescription() {
        return "Removes static imports and replaces them with qualified references. For example, `emptyList()` becomes `Collections.emptyList()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (methodMatcher.matches(m) && m.getSelect() == null && m.getMethodType() != null) {
                    // Do not replace calls to super() in constructors
                    if (m.getName().getSimpleName().equals("super")) {
                        return m;
                    }

                    JavaType.FullyQualified receiverType = m.getMethodType().getDeclaringType();
                    RemoveImport<ExecutionContext> op = new RemoveImport<>(receiverType.getFullyQualifiedName() + "." + method.getSimpleName(), true);
                    if (!getAfterVisit().contains(op)) {
                        doAfterVisit(op);
                    }

                    J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    // Do not replace if method is in java.lang.Object
                    if ("java.lang.Object".equals(receiverType.getFullyQualifiedName()) ||
                            // Do not replace when we can not determine the enclosing class, such as in build.gradle files
                            enclosingClass == null || enclosingClass.getType() == null ||
                            // Do not replace if receiverType is the same as surrounding class
                            enclosingClass.getType().equals(receiverType) ||
                            // Do not replace if method is in a wrapping outer class; not looking up more than one level
                            enclosingClass.getType().getOwningClass() != null && enclosingClass.getType().getOwningClass().equals(receiverType) ||
                            // Do not replace if class extends receiverType
                            enclosingClass.getType().getSupertype() != null && enclosingClass.getType().getSupertype().equals(receiverType)

                    ) {
                        return m;
                    }

                    // Change method invocation to use fully qualified name
                    maybeAddImport(receiverType.getFullyQualifiedName());
                    m = m.withSelect(new J.Identifier(Tree.randomId(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            emptyList(),
                            receiverType.getClassName(),
                            receiverType,
                            null));
                }
                return m;
            }
        });
    }
}
