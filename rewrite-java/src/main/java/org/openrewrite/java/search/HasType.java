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

import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class HasType extends JavaSourceVisitor<Boolean> {
    private final String clazz;

    public HasType(String clazz) {
        super("java.HasType", "type", clazz);
        this.clazz = clazz;
    }

    @Override
    public Boolean defaultTo(Tree t) {
        return false;
    }

    @Override
    public Boolean visitIdentifier(J.Ident ident) {
        JavaType.Class asClass = TypeUtils.asClass(ident.getType());
        return asClass != null && asClass.getFullyQualifiedName().equals(clazz);
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation method) {
        if(firstMethodInChain(method).getSelect() == null) {
            // either a same-class instance method or a statically imported method
            return method.getType() != null && method.getType().getDeclaringType().getFullyQualifiedName().equals(clazz);
        }
        return super.visitMethodInvocation(method);
    }

    private J.MethodInvocation firstMethodInChain(J.MethodInvocation method) {
        return method.getSelect() instanceof J.MethodInvocation ?
                firstMethodInChain((J.MethodInvocation) method.getSelect()) :
                method;
    }
}
