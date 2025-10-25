/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;

/**
 * @deprecated Use {@link DependencyNotation#parse(String)} instead.
 * This class is retained for backward compatibility.
 */
@Deprecated
public class DependencyStringNotationConverter {

    /**
     * @param notation a String in the format group:artifact:version:classifier@extension
     * @return A corresponding Dependency or null if the notation could not be parsed
     * @deprecated Use {@link DependencyNotation#parse(String)} instead
     */
    @Deprecated
    public static @Nullable Dependency parse(@Nullable String notation) {
        return DependencyNotation.parse(notation);
    }
}
