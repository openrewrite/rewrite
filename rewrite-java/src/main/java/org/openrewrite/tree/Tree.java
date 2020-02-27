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
package org.openrewrite.tree;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.visitor.AstVisitor;
import org.openrewrite.visitor.PrintVisitor;
import org.openrewrite.visitor.RetrieveCursorVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.visitor.AstVisitor;
import org.openrewrite.visitor.PrintVisitor;
import org.openrewrite.visitor.RetrieveCursorVisitor;

import java.util.Optional;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public interface Tree {
    Formatting getFormatting();

    /**
     * An id that can be used to identify a particular AST element, even after transformations have taken place on it
     */
    UUID getId();

    /**
     * An overload that allows us to create a copy of any Tree element, optionally
     * changing formatting
     */
    <T extends Tree> T withFormatting(Formatting fmt);

    default <T extends Tree> T withPrefix(String prefix) {
        return withFormatting(getFormatting().withPrefix(prefix));
    }

    default <T extends Tree> T withSuffix(String suffix) {
        return withFormatting(getFormatting().withSuffix(suffix));
    }

    default <R> R accept(AstVisitor<R> v)  {
        return v.defaultTo(null);
    }

    default String printTrimmed() {
        return StringUtils.trimIndent(print().stripLeading());
    }

    default String print() {
        return new PrintVisitor().visit(this);
    }

    @Nullable
    default Cursor cursor(Tree t) {
        return new RetrieveCursorVisitor(t.getId()).visit(this);
    }

    @SuppressWarnings("unchecked")
    default <T extends Tree> Optional<T> whenType(Class<T> treeType) {
        return treeType.isAssignableFrom(this.getClass()) ? Optional.of((T) this) : Optional.empty();
    }
}