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

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

public class FindReferencedTypes {
    private FindReferencedTypes() {
    }

    public static Set<JavaType.Class> find(J j) {
        Set<JavaType.Class> fields = new HashSet<>();
        new FindReferencedTypesVisitor().visit(j, fields);
        return fields;
    }

    private static class FindReferencedTypesVisitor extends JavaIsoVisitor<Set<JavaType.Class>> {
        @Override
        public <N extends NameTree> N visitTypeName(N name, Set<JavaType.Class> ctx) {
            JavaType.Class asClass = TypeUtils.asClass(name.getType());
            if (asClass != null) {
                ctx.add(asClass);
            }
            return super.visitTypeName(name, ctx);
        }
    }
}
