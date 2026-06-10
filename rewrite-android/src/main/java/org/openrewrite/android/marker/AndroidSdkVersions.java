/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
 * The SDK version triple declared by an Android module's {@code android {}} block.
 * All three fields are nullable: legacy or partially-configured projects may omit
 * any of {@code compileSdk}, {@code minSdk}, or {@code targetSdk}.
 */
@Value
@With
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class AndroidSdkVersions implements Serializable {

    @Nullable
    Integer compileSdk;

    @Nullable
    Integer minSdk;

    @Nullable
    Integer targetSdk;
}
