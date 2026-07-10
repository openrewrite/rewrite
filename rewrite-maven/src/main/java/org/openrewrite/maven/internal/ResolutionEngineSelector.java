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
package org.openrewrite.maven.internal;

import org.openrewrite.ExecutionContext;

/**
 * Selects the dependency resolution engine for dev/CI purposes only. Deliberately not part of
 * {@link org.openrewrite.maven.MavenExecutionContextView}; this never becomes public API.
 * <p>
 * The default is the Maven engine (Phase 5 cutover). The legacy resolver remains in-tree for one
 * release as revert-by-release insurance, reachable only through this internal dev/CI key.
 */
public class ResolutionEngineSelector {
    public static final String ENGINE_KEY = "org.openrewrite.maven.resolution.engine";

    public enum Engine {
        LEGACY,
        MAVEN,
        SHADOW
    }

    private ResolutionEngineSelector() {
    }

    public static Engine select(ExecutionContext ctx) {
        String engine = ctx.getMessage(ENGINE_KEY, System.getProperty(ENGINE_KEY));
        if ("legacy".equalsIgnoreCase(engine)) {
            return Engine.LEGACY;
        }
        if ("shadow".equalsIgnoreCase(engine)) {
            return Engine.SHADOW;
        }
        return Engine.MAVEN;
    }
}
