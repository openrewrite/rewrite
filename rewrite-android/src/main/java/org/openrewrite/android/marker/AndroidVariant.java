/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.android.marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * One realized build variant — the cross product of a build type and zero or more
 * product flavors (e.g., {@code paidDebug}, {@code freeRelease}). The variant
 * {@code name} maps recipes to per-variant Gradle configurations such as
 * {@code paidDebugImplementation}.
 */
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class AndroidVariant implements Serializable {

    String name;

    String buildTypeName;

    List<String> flavorNames;

    /**
     * Names of the source sets that contribute to this variant, ordered from most
     * general (e.g., {@code main}) to most specific (e.g., {@code paidDebug}).
     */
    List<String> sourceSetNames;

    public List<String> getFlavorNames() {
        return flavorNames == null ? emptyList() : flavorNames;
    }

    public List<String> getSourceSetNames() {
        return sourceSetNames == null ? emptyList() : sourceSetNames;
    }
}
