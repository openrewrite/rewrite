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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

public class ChangeMethodTargetToStatic extends Recipe {
    private String method;
    private String targetType;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new ChangeMethodTargetToStaticProcessor(new MethodMatcher(method), targetType);
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Override
    public Validated validate() {
        return required("method", method)
                .and(required("target.type", targetType));
    }

    private static class ChangeMethodTargetToStaticProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher methodMatcher;
        private final String targetType;

        public ChangeMethodTargetToStaticProcessor(MethodMatcher methodMatcher, String targetType) {
            this.methodMatcher = methodMatcher;
            this.targetType = targetType;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(method)) {
                JavaType.FullyQualified classType = JavaType.Class.build(targetType);

                m = method.withSelect(
                        new JRightPadded<>(
                                J.Ident.build(randomId(),
                                        method.getSelect() == null ?
                                                Space.EMPTY :
                                                method.getSelect().getElem().getPrefix(),
                                        Markers.EMPTY,
                                        classType.getClassName(),
                                        classType),
                                Space.EMPTY
                        )
                );

                maybeAddImport(targetType);

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
