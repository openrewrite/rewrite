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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.emptyList;

public interface SourceFile extends Tree {
    String getSourcePath();

    /**
     * {@link SourceVisitor} may respond to metadata to determine whether to act on
     * a source file or not.
     *
     * @return A metadata map containing any additional context about this source file.
     */
    Collection<Metadata> getMetadata();

    default <M extends Metadata> Optional<M> getMetadata(Class<M> metadataType) {
        return getStyles().stream().filter(metadataType::isInstance).map(metadataType::cast).findFirst();
    }

    /**
     * Styles encode the surrounding project's expectation of formatting, etc.
     * For example, the project's expected Java import ordering is a style.
     *
     * @return A list of styles.
     */
    default Collection<? extends Style> getStyles() {
        return emptyList();
    }

    default <S extends Style> Optional<S> getStyle(Class<S> styleType) {
        return getStyles().stream().filter(styleType::isInstance).map(styleType::cast).findFirst();
    }

    @JsonProperty("@c")
    default String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }
}
