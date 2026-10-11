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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * The data each Yarn 4.x release ships that reaches a lock, from {@code yarn-releases.json.gz}.
 */
@Getter
final class YarnRelease {

    private static final String RESOURCE = "yarn-releases.json.gz";

    private static volatile @Nullable Map<String, @Nullable Object> data;

    private final String version;

    /**
     * Project.ts's LOCKFILE_VERSION: 8, 9 from 4.14.0, 10 from 4.15.0. Recordings with 4.11.0 through 4.18.1 write
     * the entries 4.10.3, whose code this package ports, writes.
     */
    private final int lockfileVersion;

    /**
     * {@code [descriptor, extension data]} in the table's order.
     */
    private final List<Map.Entry<String, Map<String, @Nullable Object>>> packageExtensions;

    private final List<String> patches;
    private final Pattern nodeGypMatch;
    private final String nodeGypRange;

    /**
     * The plugins the CLI bundles, in the order it activates them.
     */
    private final List<String> plugins;

    /**
     * Whether the npm plugin sends the {@code jsr} scope to npm.jsr.io when {@code npmScopes} does not name it.
     */
    private final boolean jsrScope;

    /**
     * Whether {@code generateLockfile} writes a checksum without its cache key when the key is the lockfile's.
     */
    private final boolean checksumWithoutCacheKey;

    private final Set<String> settings;

    /**
     * The command the usage errors for an unknown setting or shape key name ({@code yarn config -v} before 4.9.3).
     */
    private final String unknownSettingHint;

    private YarnRelease(String version, List<Map.Entry<String, Map<String, @Nullable Object>>> packageExtensions,
                        List<String> patches, Pattern nodeGypMatch, String nodeGypRange, List<String> plugins,
                        boolean jsrScope, boolean checksumWithoutCacheKey, Set<String> settings,
                        String unknownSettingHint) {
        this.version = version;
        this.lockfileVersion = compareVersions(version, "4.15.0") >= 0 ? 10 :
                compareVersions(version, "4.14.0") >= 0 ? 9 : 8;
        this.packageExtensions = packageExtensions;
        this.patches = patches;
        this.nodeGypMatch = nodeGypMatch;
        this.nodeGypRange = nodeGypRange;
        this.plugins = plugins;
        this.jsrScope = jsrScope;
        this.checksumWithoutCacheKey = checksumWithoutCacheKey;
        this.settings = settings;
        this.unknownSettingHint = unknownSettingHint;
    }

    boolean hasPlugin(String name) {
        return plugins.contains(name);
    }

    /**
     * The release, or null when the data has none of that version.
     */
    @SuppressWarnings("unchecked")
    static @Nullable YarnRelease of(String version) {
        Map<String, @Nullable Object> all = data();
        Object release = Js.get(all.get("releases"), version);
        if (!(release instanceof Map)) {
            return null;
        }
        Map<String, Object> tables = (Map<String, Object>) all.get("tables");
        List<Map.Entry<String, Map<String, @Nullable Object>>> extensions = new ArrayList<>();
        for (Object entry : (List<Object>) tables.get((String) Js.get(release, "packageExtensions"))) {
            List<Object> pair = (List<Object>) entry;
            extensions.add(new AbstractMap.SimpleImmutableEntry<>((String) pair.get(0),
                    (Map<String, @Nullable Object>) pair.get(1)));
        }
        Map<String, Object> nodeGyp = (Map<String, Object>) Js.get(release, "nodeGyp");
        Set<String> settings = new LinkedHashSet<>((List<String>) tables.get((String) Js.get(release, "settings")));
        return new YarnRelease(version, Collections.unmodifiableList(extensions),
                Collections.unmodifiableList((List<String>) Js.get(release, "patches")),
                V8RegExp.of((String) nodeGyp.get("pattern")), (String) nodeGyp.get("range"),
                Collections.unmodifiableList((List<String>) tables.get((String) Js.get(release, "plugins"))),
                Boolean.TRUE.equals(Js.get(release, "jsrScope")),
                Boolean.TRUE.equals(Js.get(release, "checksumWithoutCacheKey")), Collections.unmodifiableSet(settings),
                (String) Js.get(release, "unknownSettingHint"));
    }

    /**
     * Two release versions ({@code 4.10.3}) by major, minor and patch.
     */
    static int compareVersions(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> data() {
        Map<String, @Nullable Object> loaded = data;
        if (loaded == null) {
            InputStream resource = YarnRelease.class.getResourceAsStream(RESOURCE);
            if (resource == null) {
                throw new IllegalStateException(RESOURCE + " is missing");
            }
            try (InputStream in = new GZIPInputStream(resource)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[65536];
                int n;
                while ((n = in.read(buffer)) > 0) {
                    out.write(buffer, 0, n);
                }
                loaded = (Map<String, @Nullable Object>) Js.jsonParse(new String(out.toByteArray(),
                        StandardCharsets.UTF_8));
                data = loaded;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return loaded;
    }
}
