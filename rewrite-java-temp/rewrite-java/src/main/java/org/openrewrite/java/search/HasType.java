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
import org.openrewrite.java.AbstractJavaSourceVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

public class HasType extends AbstractJavaSourceVisitor<Boolean> {
    private final String clazz;

    public HasType(String clazz) {
        this.clazz = clazz;
        setCursoringOn();
    }

    @Override
    public Iterable<Tag> getTags() {
        return Tags.of("type", clazz);
    }

    @Override
    public Boolean defaultTo(Tree t) {
        return false;
    }

    @Override
    public Boolean visitTypeName(NameTree name) {
        JavaType.Class asClass = TypeUtils.asClass(name.getType());
        if (asClass != null && asClass.getFullyQualifiedName().equals(clazz) &&
                getCursor().firstEnclosing(J.Import.class) == null) {
            return true;
        }
        return super.visitTypeName(name);
    }
}
