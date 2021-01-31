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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodTargetToStatic extends Recipe {

    /**
     * A method pattern, expressed as a pointcut expression, that is used to find matching method invocations.
     * See {@link  MethodMatcher} for details on the expression's syntax.
     */
    private final String methodPattern;

    /**
     * A fully-qualified class name of the type upon which the static method is defined.
     */
    private final String fullyQualifiedTargetTypeName;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeMethodTargetToStaticVisitor(new MethodMatcher(methodPattern));
    }

    private class ChangeMethodTargetToStaticVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        public ChangeMethodTargetToStaticVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(method)) {
                JavaType.FullyQualified classType = JavaType.Class.build(fullyQualifiedTargetTypeName);

                m = method.withSelect(J.Ident.build(randomId(),
                        method.getSelect() == null ?
                                Space.EMPTY :
                                method.getSelect().getPrefix(),
                        Markers.EMPTY,
                        classType.getClassName(),
                        classType
                        )
                );

                maybeAddImport(fullyQualifiedTargetTypeName);

                JavaType.Method transformedType = null;
                if (method.getType() != null) {
                    maybeRemoveImport(method.getType().getDeclaringType());
                    transformedType = method.getType().withDeclaringType(classType);
                    if (!method.getType().hasFlags(Flag.Static)) {
                        Set<Flag> flags = new LinkedHashSet<>(method.getType().getFlags());
                        flags.add(Flag.Static);
                        transformedType = transformedType.withFlags(flags);
                    }
                }
                m = m.withType(transformedType);
            }
            return m;
        }
    }
}
