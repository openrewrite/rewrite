/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.trait.member;

import lombok.AllArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.trait.Top;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;
import org.openrewrite.java.trait.variable.Parameter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * A compiler-generated initializer method (could be static or
 * non-static), which is used to hold (static or non-static) field
 * initializers, as well as explicit initializer blocks.
 */
public interface InitializerMethod extends Callable {

    @Override
    default @Nullable JavaType getReturnType() {
        return JavaType.Primitive.Void;
    }

    /**
     * Initializer methods have no parameters.
     */
    @Override
    default List<Parameter> getParameters() {
        return Collections.emptyList();
    }
}

@AllArgsConstructor
abstract class InitializerMethodBase implements InitializerMethod {
    /**
     * IMPORTANT: This cursor points to the {@link J.Block} that is the body of the class, NOT the initializer block.
     * This is because the static/object initializer block may not be explicitly declared, and so the cursor would be null.
     */
    private final Cursor cursor;

    @Override
    public UUID getId() {
        return cursor.<Tree>getValue().getId();
    }

    @Override
    public boolean equals(Object obj) {
        return Top.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Top.hashCode(this);
    }

    static <V extends InitializerMethodBase> Validation<TraitErrors, V> genericViewOf(Cursor cursor, Function<Cursor, V> initializer, Class<V> clazz) {
        Objects.requireNonNull(cursor, "cursor must not be null");
        if (cursor.getValue() instanceof J.Block) {
            Cursor parent = cursor.getParentTreeCursor();
            if (parent.getValue() instanceof J.ClassDeclaration) {
                J.ClassDeclaration classDecl = parent.getValue();
                if (classDecl.getBody() != null && classDecl.getBody() == cursor.getValue()) {
                    return Validation.success(initializer.apply(cursor));
                }
            }
        }
        return TraitErrors.invalidTraitCreationType(clazz, cursor, J.Block.class);
    }
}

