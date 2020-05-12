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
package org.openrewrite;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openrewrite.internal.StringUtils;

import java.util.Optional;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@c")
public interface Tree {
    static UUID randomId() {
        return UUID.randomUUID();
    }

    Formatting getFormatting();

    /**
     * An id that can be used to identify a particular AST element, even after transformations have taken place on it
     *
     * @return A unique identifier
     */
    UUID getId();

    /**
     * An overload that allows us to create a copy of any Tree element, optionally
     * changing formatting
     *
     * @param <T> The type of this tree.
     * @param fmt The formatting to apply to this tree.
     * @return A copy of this tree, with formatting changed.
     */
    <T extends Tree> T withFormatting(Formatting fmt);

    default <T extends Tree> T withPrefix(String prefix) {
        return withFormatting(getFormatting().withPrefix(prefix));
    }

    default <T extends Tree> T withSuffix(String suffix) {
        return withFormatting(getFormatting().withSuffix(suffix));
    }

    default <R> R accept(SourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    default String printTrimmed() {
        return StringUtils.trimIndent(print().stripLeading());
    }

    String print();

    @SuppressWarnings("unchecked")
    default <T extends Tree> Optional<T> whenType(Class<T> treeType) {
        return treeType.isAssignableFrom(this.getClass()) ? Optional.of((T) this) : Optional.empty();
    }
}
