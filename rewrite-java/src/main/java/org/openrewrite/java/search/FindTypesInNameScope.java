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
package org.openrewrite.java.search;

import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashSet;
import java.util.Set;

public final class FindTypesInNameScope {
    private FindTypesInNameScope() {
    }

    public static Set<JavaType> find(J j) {
        Set<JavaType> types = new HashSet<>();
        new FindTypesInNameScopeVisitor().visit(j, types);
        return types;
    }

    private static class FindTypesInNameScopeVisitor extends JavaVisitor<Set<JavaType>> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, Set<JavaType> ctx) {
            if (method.getType() != null) {
                ctx.add(method.getType());
            }
            return super.visitMethodInvocation(method, ctx);
        }

        @Override
        public J visitNewClass(J.NewClass newClass, Set<JavaType> ctx) {
            if (newClass.getType() != null) {
                ctx.add(newClass.getType());
            }
            return super.visitNewClass(newClass, ctx);
        }

        @Override
        public J visitVariable(J.VariableDeclarations.NamedVariable variable, Set<JavaType> ctx) {
            if (variable.getType() != null) {
                ctx.add(variable.getType());
            }
            return super.visitVariable(variable, ctx);
        }
    }
}
