/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree.visitor.search;

import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.TypeUtils;
import com.netflix.rewrite.tree.visitor.AstVisitor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HasType extends AstVisitor<Boolean> {
    private final String clazz;

    @Override
    public Boolean defaultTo(Tree t) {
        return false;
    }

    @Override
    public Boolean visitIdentifier(Tr.Ident ident) {
        Type.Class asClass = TypeUtils.asClass(ident.getType());
        return asClass != null && asClass.getFullyQualifiedName().equals(clazz);
    }

    @Override
    public Boolean visitMethodInvocation(Tr.MethodInvocation method) {
        if(firstMethodInChain(method).getSelect() == null) {
            // either a same-class instance method or a statically imported method
            return method.getType() != null && method.getType().getDeclaringType().getFullyQualifiedName().equals(clazz);
        }
        return super.visitMethodInvocation(method);
    }

    private Tr.MethodInvocation firstMethodInChain(Tr.MethodInvocation method) {
        return method.getSelect() instanceof Tr.MethodInvocation ?
                firstMethodInChain((Tr.MethodInvocation) method.getSelect()) :
                method;
    }
}
