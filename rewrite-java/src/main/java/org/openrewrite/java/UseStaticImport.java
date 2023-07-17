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
package org.openrewrite.java;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@ToString
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UseStaticImport extends Recipe {

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
        return "Use static import";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary receiver types from static method invocations. For example, `Collections.emptyList()` becomes `emptyList()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new UseStaticImportVisitor());
    }

    private class UseStaticImportVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(m)) {
                if (m.getMethodType() != null) {
                    JavaType.FullyQualified receiverType = m.getMethodType().getDeclaringType();
                    maybeRemoveImport(receiverType);

                    AddImport<ExecutionContext> addStatic = new AddImport<>(
                            receiverType.getFullyQualifiedName(),
                            m.getSimpleName(),
                            false);

                    if (!getAfterVisit().contains(addStatic)) {
                        doAfterVisit(addStatic);
                    }
                }

                if (m.getSelect() != null) {
                    m = m.withSelect(null).withName(m.getName().withPrefix(m.getSelect().getPrefix()));
                }
            }
            return m;
        }
    }
}
