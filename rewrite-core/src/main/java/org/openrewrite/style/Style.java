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
package org.openrewrite.style;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;

import java.util.function.Supplier;

/**
 * Styles represent project-level standards that each source file is expected to follow, e.g.
 * import ordering. They are provided to parser implementations and expected to be stored on
 * {@link SourceFile} instances so that any modifications to those source files can conform
 * to the source's expected styles.
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c")
public interface Style {
    @JsonProperty("@c")
    default String getJacksonPolymorphicTypeTag() {
        return getClass().getName();
    }

    default Style merge(Style lowerPrecedence) {
        return StyleHelper.merge(lowerPrecedence, this);
    }

    default Style applyDefaults() { return this; }

    static <S extends Style> @Nullable S from(Class<S> styleClass, SourceFile sf) {
        return NamedStyles.merge(styleClass, sf.getMarkers().findAll(NamedStyles.class));
    }

    static <S extends Style> S from(Class<S> styleClass, SourceFile sf, Supplier<S> defaultStyle) {
        S s = from(styleClass, sf);
        return s == null ? defaultStyle.get() : s;
    }
}
