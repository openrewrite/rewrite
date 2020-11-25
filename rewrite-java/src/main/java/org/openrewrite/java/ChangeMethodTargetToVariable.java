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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Formatting;
import org.openrewrite.marker.Markers;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

public class ChangeMethodTargetToVariable extends JavaIsoRefactorVisitor {
    private MethodMatcher methodMatcher;
    private String variable;
    private JavaType.Class variableType;

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public void setVariableType(String variableType) {
        this.variableType = JavaType.Class.build(variableType);
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("variable", variable))
                .and(required("variable.type", variableType.getFullyQualifiedName()));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if (methodMatcher.matches(method)) {
            andThen(new Scoped(method, variable, variableType));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.MethodInvocation scope;
        private final String variable;

        @Nullable
        private final JavaType.Class type;

        public Scoped(J.MethodInvocation scope, J.VariableDecls.NamedVar namedVar) {
            this(scope, namedVar.getSimpleName(), TypeUtils.asClass(namedVar.getType()));
        }

        public Scoped(J.MethodInvocation scope, String variable, @Nullable JavaType.Class type) {
            this.scope = scope;
            this.variable = variable;
            this.type = type;
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("to", variable);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            if (scope.isScope(method)) {
                Expression select = method.getSelect();

                JavaType.Method methodType = null;
                if (method.getType() != null) {
                    Set<Flag> flags = new LinkedHashSet<>(method.getType().getFlags());
                    flags.remove(Flag.Static);
                    methodType = method.getType().withDeclaringType(this.type).withFlags(flags);
                }

                andThen(new OrderImports());

                return method
                        .withSelect(J.Ident.build(randomId(), variable, type,
                                select == null ? Formatting.EMPTY : select.getFormatting(),
                                Markers.EMPTY))
                        .withType(methodType);
            }

            return super.visitMethodInvocation(method);
        }
    }
}
