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

import org.openrewrite.java.JavaProcessor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Find places where a type is mentioned explicitly, excluding imports.
 */
public final class FindType {
    private FindType() {
    }

    public static Set<NameTree> find(J j, String clazz) {
        Set<NameTree> nameTrees = new HashSet<>();
        new FindTypeProcessor(clazz).visit(j, nameTrees);
        return nameTrees;
    }

    private static class FindTypeProcessor extends JavaProcessor<Set<NameTree>> {
        private final String clazz;

        public FindTypeProcessor(String clazz) {
            this.clazz = clazz;
            setCursoringOn();
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, Set<NameTree> ctx) {
            JavaType.Class asClass = TypeUtils.asClass(name.getType());
            if (asClass != null && asClass.getFullyQualifiedName().equals(clazz) &&
                    getCursor().firstEnclosing(J.Import.class) == null) {
                ctx.add(name);
            }
            return super.visitTypeName(name, ctx);
        }
    }
}
