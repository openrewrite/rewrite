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
package org.openrewrite.python.internal.uvlock;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The {@code [package.metadata]} block (declared inputs, uv's {@code --check} oracle).
 * May be present with all fields null: uv emits an empty {@code [package.metadata]} header
 * when only {@code [package.metadata.requires-dev]} follows.
 */
@Value
@With
@Builder(toBuilder = true)
public class UvLockMetadata {

    @Nullable
    List<UvLockRequirement> requiresDist;

    /**
     * Extra names in declaration order (uv does not sort this array).
     */
    @Nullable
    List<String> providesExtras;

    @Nullable
    Map<String, List<UvLockRequirement>> requiresDev;
}
