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
import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * A single entry from the AGP {@code android.productFlavors {}} block. Each flavor
 * belongs to one flavor dimension; the cartesian product of flavor dimensions plus
 * build types yields the module's variants.
 */
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class AndroidProductFlavor implements Serializable {

    String name;

    @Nullable
    String dimension;

    @Nullable
    String applicationIdSuffix;

    @Nullable
    String versionNameSuffix;
}
