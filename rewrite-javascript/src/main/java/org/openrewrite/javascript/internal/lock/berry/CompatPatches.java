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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/** Port of plugin-compat's reduceDependency and its release-specific builtin patch hook. */
final class CompatPatches {
    private static final Map<String, String> SOURCES = new ConcurrentHashMap<>();

    private CompatPatches() {
    }

    private static final class Index {
        @SuppressWarnings("unchecked")
        static final Map<String, Object> RELEASES = (Map<String, Object>) Js.jsonParse(read("compat-patches.json.gz"));
    }

    static @Nullable String source(YarnRelease release, String name) {
        Object hash = Js.get(Index.RELEASES.get(release.getVersion()), name);
        if (!(hash instanceof String)) return null;
        return SOURCES.computeIfAbsent((String) hash, h -> read("compat/" + h + ".patch.gz"));
    }

    private static String read(String path) {
        InputStream resource = CompatPatches.class.getResourceAsStream(path);
        if (resource == null) throw new IllegalStateException("Missing builtin patch resource " + path);
        try (InputStream in = new GZIPInputStream(resource)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Descriptor reduceDependency(Descriptor dependency, YarnRelease release) {
        for (String name : release.getPatches()) {
            if (StructUtils.makeIdent(null, name).identHash.equals(dependency.identHash)) {
                return StructUtils.makeDescriptor(dependency, StructUtils.makeRange("patch:",
                        StructUtils.stringifyDescriptor(dependency),
                        "optional!builtin<compat/" + StructUtils.stringifyIdent(dependency) + ">", null));
            }
        }
        return dependency;
    }
}
