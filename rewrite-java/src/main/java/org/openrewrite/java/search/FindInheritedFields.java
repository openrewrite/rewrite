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

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class FindInheritedFields extends AbstractJavaSourceVisitor<List<JavaType.Var>> {
    private final String fullyQualifiedClassName;

    public FindInheritedFields(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("type", fullyQualifiedClassName);
    }

    @Override
    public List<JavaType.Var> defaultTo(Tree t) {
        return emptyList();
    }

    private List<JavaType.Var> superFields(@Nullable JavaType.Class type) {
        if(type == null || type.getSupertype() == null) {
            return emptyList();
        }
        List<JavaType.Var> types = new ArrayList<>();
        type.getMembers().stream()
                .filter(m -> !m.hasFlags(Flag.Private) && TypeUtils.hasElementType(m.getType(), fullyQualifiedClassName))
                .forEach(types::add);
        types.addAll(superFields(type.getSupertype()));
        return types;
    }

    @Override
    public List<JavaType.Var> visitClassDecl(J.ClassDecl classDecl) {
        JavaType.Class asClass = TypeUtils.asClass(classDecl.getType());
        return superFields(asClass == null ? null : asClass.getSupertype());
    }
}
