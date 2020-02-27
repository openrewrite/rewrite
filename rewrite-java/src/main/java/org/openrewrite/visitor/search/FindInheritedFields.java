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
package org.openrewrite.visitor.search;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.tree.*;
import org.openrewrite.tree.*;
import org.openrewrite.visitor.AstVisitor;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class FindInheritedFields extends AstVisitor<List<Type.Var>> {
    private final String fullyQualifiedClassName;

    @Override
    public List<Type.Var> defaultTo(Tree t) {
        return emptyList();
    }

    private List<Type.Var> superFields(@Nullable Type.Class type) {
        if(type == null || type.getSupertype() == null) {
            return emptyList();
        }
        List<Type.Var> types = new ArrayList<>();
        type.getMembers().stream()
                .filter(m -> !m.hasFlags(Flag.Private) && TypeUtils.hasElementType(m.getType(), fullyQualifiedClassName))
                .forEach(types::add);
        types.addAll(superFields(type.getSupertype()));
        return types;
    }

    @Override
    public List<Type.Var> visitClassDecl(J.ClassDecl classDecl) {
        Type.Class asClass = TypeUtils.asClass(classDecl.getType());
        return superFields(asClass == null ? null : asClass.getSupertype());
    }
}
