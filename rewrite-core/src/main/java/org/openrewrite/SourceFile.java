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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.internal.lang.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

public interface SourceFile extends Tree {
    URI getSourcePath();

    /**
     * {@link SourceVisitor} may respond to metadata to determine whether to act on
     * a source file or not.
     *
     * @return A metadata map containing any additional context about this source file.
     */
    Collection<Metadata> getMetadata();

    /**
     * Overrides metadata of the same type if it exists, otherwise adds
     * a new metadata element to the collection.
     *
     * @param metadata The metadata to set
     * @param <S>      The type of {@link SourceFile}.
     * @return A new {@link SourceFile} with updated metadata.
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    default <S extends SourceFile> S setMetadata(Metadata metadata) {
        List<Metadata> updatedMetadata = new ArrayList<>(getMetadata().size());
        boolean updated = false;
        for (Metadata metadatum : getMetadata()) {
            if (metadatum.getClass().equals(metadata.getClass())) {
                updatedMetadata.add(metadata);
                updated = true;
            } else {
                updatedMetadata.add(metadatum);
            }
        }
        if (!updated) {
            updatedMetadata.add(metadata);
        }
        return (S) withMetadata(updatedMetadata);
    }

    SourceFile withMetadata(Collection<Metadata> metadata);

    @Nullable
    default <M extends Metadata> M getMetadata(Class<M> metadataType) {
        for (Metadata metadata : getMetadata()) {
            if (metadataType.isInstance(metadata)) {
                //noinspection unchecked
                return (M) metadata;
            }
        }

        return null;
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

    @Nullable
    default <S extends Style> S getStyle(Class<S> styleType) {
        for (Style style : getStyles()) {
            if (styleType.isInstance(style)) {
                //noinspection unchecked
                return (S) style;
            }
        }

        return null;
    }

    @JsonProperty("@c")
    default String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }
}
