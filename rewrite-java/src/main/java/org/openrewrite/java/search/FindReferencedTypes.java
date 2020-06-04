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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class FindReferencedTypes extends JavaSourceVisitor<Set<JavaType.Class>> {
    @Override
    public Set<JavaType.Class> defaultTo(Tree t) {
        return emptySet();
    }

    @Override
    public Set<JavaType.Class> visitTypeName(NameTree name) {
        Set<JavaType.Class> referenced = new HashSet<>(super.visitTypeName(name));
        JavaType.Class asClass = TypeUtils.asClass(name.getType());
        if (asClass != null) {
            referenced.add(asClass);
        }
        return referenced;
    }
}
