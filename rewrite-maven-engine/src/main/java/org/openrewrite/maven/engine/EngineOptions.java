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
package org.openrewrite.maven.engine;

import lombok.Value;
import lombok.With;

/**
 * Immutable engine-wide options. Distinct from per-session {@link SessionConfig}: these are set once when the
 * {@link MavenEngine} is created and shared across every session it serves.
 */
@Value
@With
public class EngineOptions {

    public static final EngineOptions DEFAULT = new EngineOptions(false);

    /**
     * When {@code true}, Gradle Module Metadata ({@code .module}) platform injection is honored on the parsing path.
     * Inert in Phase 1 (no code path reads it yet); the flag exists so rewrite-gradle's approximation path can opt in
     * later without an API change. Off by default because {@code .module} injection is a parity bug on the Maven path
     * (DESIGN §0 "Gradle metadata").
     */
    boolean gradleMetadataInjection;
}
