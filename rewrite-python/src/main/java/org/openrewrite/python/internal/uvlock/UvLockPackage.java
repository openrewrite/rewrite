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
 * A {@code [[package]]} entry. List and map fields preserve file order on read
 * (uv sorts packages alphabetically, wheels in index order, groups alphabetically);
 * a {@code null} collection means the key was absent in the file.
 */
@Value
@With
@Builder(toBuilder = true)
public class UvLockPackage {

    String name;
    String version;
    UvLockSource source;

    /**
     * Present only on fork-duplicated packages; emitted multiline even for one element.
     */
    @Nullable
    List<String> resolutionMarkers;

    @Nullable
    List<UvLockDependency> dependencies;

    @Nullable
    UvLockArtifact sdist;

    @Nullable
    List<UvLockArtifact> wheels;

    @Nullable
    Map<String, List<UvLockDependency>> optionalDependencies;

    @Nullable
    Map<String, List<UvLockDependency>> devDependencies;

    @Nullable
    UvLockMetadata metadata;
}
