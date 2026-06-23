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

import org.jspecify.annotations.Nullable;

import java.util.Map;

/** Tries each source in order, returning the first hit — local cache, then proxy. */
public final class TieredSource implements ModSource {

    private final ModSource[] sources;

    public TieredSource(ModSource... sources) {
        this.sources = sources;
    }

    @Override
    public byte @Nullable [] goMod(String path, String version) {
        for (ModSource s : sources) {
            byte[] b = s.goMod(path, version);
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    @Override
    public @Nullable Map<String, byte[]> packageGoFiles(String modPath, String version, String importPath) {
        for (ModSource s : sources) {
            Map<String, byte[]> files = s.packageGoFiles(modPath, version, importPath);
            if (files != null) {
                return files;
            }
        }
        return null;
    }
}
