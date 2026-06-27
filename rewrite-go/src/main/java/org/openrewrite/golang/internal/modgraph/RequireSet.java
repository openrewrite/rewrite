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
package org.openrewrite.golang.internal.modgraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The set of modules {@code go mod tidy} would write to go.mod, classified the
 * way tidy classifies them. Computed from the package import graph (not just the
 * module graph), so it matches go.mod exactly.
 */
public final class RequireSet {

    /** Module path -&gt; version for modules providing a package the main module imports directly. */
    public final Map<String, String> direct = new LinkedHashMap<>();
    /** Module path -&gt; version for modules needed transitively but not imported directly. */
    public final Map<String, String> indirect = new LinkedHashMap<>();
    /** False if any package directory could not be read, making the set best-effort. */
    public boolean complete = true;
    /** Import paths that mapped to no build-list module (diagnostic only). */
    public final List<String> unresolved = new ArrayList<>();
    /** Package directories that could not be read (diagnostic only). */
    public final List<String> missingDirs = new ArrayList<>();

    public Map<String, String> direct() {
        return direct;
    }

    public Map<String, String> indirect() {
        return indirect;
    }

    public boolean complete() {
        return complete;
    }
}
