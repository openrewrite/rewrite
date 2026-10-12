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
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.semver.NodeSemver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Bun releases that write bun.lock, the ones the engine reproduces, and the one a project is installed as.
 */
public final class BunReleases {

    /**
     * The releases the engine reproduces, a node-semver range: every recorded scenario replays as each of them and
     * gives what that release wrote ({@code BunScenarioTest} with {@code REWRITE_LOCK_EVERY_RELEASE}), and every
     * upstream change between them and 1.3.10 that reaches a lock is a question of {@link BunRelease}: the Zig sources
     * from 1.2.0 to 1.3.14, with the Zig standard library and the Highway string search they are built with, and the
     * Rust translation of 1.4.0 to 1.4.3 (lockfileVersion 2 and 3, scoped overrides, the new JSON and TOML parsers,
     * bunfig over .npmrc, and the rest). The releases before 1.2.0 that write bun.lock (1.1.39 to 1.1.45) write and
     * read its lockfileVersion 0 alone, which no recorded lock is.
     */
    public static final String PROVEN = "1.2.0 - 1.4.3";

    private static final List<String> ALL = load();

    private BunReleases() {
    }

    /**
     * Every stable release of Bun from 1.1.39, the first that writes bun.lock, in ascending order
     * ({@code bun/generate/bun-releases.js}).
     */
    public static List<String> all() {
        return ALL;
    }

    /**
     * Whether the engine reproduces {@code version}.
     */
    public static boolean isProven(String version) {
        return NodeSemver.satisfies(version, PROVEN, NodeSemver.Options.DEFAULT);
    }

    /**
     * The release a project is installed as, the one that built its LST: Bun {@code version}, what the parser's bun
     * answered to {@code --version} ({@code NodeRuntime}), or {@link BunRelease#PORTED} where it recorded none.
     *
     * @throws CannotKnow for a version that is no release of {@link #all()} (a prerelease or canary, one that writes
     *                    no bun.lock, one newer than the list) or one the engine does not reproduce
     */
    public static BunRelease installedAs(@Nullable String version) {
        if (version == null) {
            return BunRelease.PORTED;
        }
        if (ALL.contains(version) && isProven(version)) {
            return BunRelease.of(version);
        }
        throw new CannotKnow("what bun " + version + " writes, since the engine reproduces bun " + ranges(proven()));
    }

    /**
     * The releases the engine reproduces, in ascending order.
     */
    static List<String> proven() {
        List<String> proven = new ArrayList<>();
        for (String release : ALL) {
            if (isProven(release)) {
                proven.add(release);
            }
        }
        return proven;
    }

    /**
     * {@code releases} as the runs of {@link #all()} they fill, in ascending order: {@code 1.3.0 - 1.3.9, 1.3.14}.
     */
    static String ranges(Collection<String> releases) {
        List<String> sorted = new ArrayList<>();
        for (String release : ALL) {
            if (releases.contains(release)) {
                sorted.add(release);
            }
        }
        List<String> ranges = new ArrayList<>();
        for (int first = 0, last; first < sorted.size(); first = last + 1) {
            last = first;
            while (last + 1 < sorted.size() &&
                   ALL.indexOf(sorted.get(last + 1)) == ALL.indexOf(sorted.get(last)) + 1) {
                last++;
            }
            ranges.add(first == last ? sorted.get(first) : sorted.get(first) + " - " + sorted.get(last));
        }
        return String.join(", ", ranges);
    }

    private static List<String> load() {
        String resource = "bun-releases.txt";
        try (InputStream in = BunReleases.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is missing");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            List<String> releases = new ArrayList<>();
            for (String version : new String(out.toByteArray(), StandardCharsets.UTF_8).split("[\r\n]+")) {
                if (!version.isEmpty()) {
                    BunRelease.of(version);
                    releases.add(version);
                }
            }
            return Collections.unmodifiableList(releases);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
