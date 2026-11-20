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
package org.openrewrite.javascript.internal.lock.npm;

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
import java.util.TreeSet;

/**
 * The npm releases there are, the ones the engine reproduces, and the one a project is installed as.
 */
public final class NpmReleases {

    /**
     * The releases the engine reproduces, a node-semver range: every recorded scenario replays as each of them and
     * gives what that release wrote ({@code REWRITE_LOCK_EVERY_RELEASE} replays every release of a range, not only
     * its first and last). It widens as the releases around the ported one are ported and replayed.
     */
    public static final String PROVEN = "7.0.4 - 12.2.1";

    private static final List<NpmRelease> ALL = load();

    private NpmReleases() {
    }

    /**
     * Every stable release of npm from 7.0.0, the first with arborist, in ascending order
     * ({@code npm-arborist/generate/npm-releases.js}).
     */
    public static List<NpmRelease> all() {
        return ALL;
    }

    /**
     * Whether the engine reproduces {@code release}.
     */
    public static boolean isProven(NpmRelease release) {
        return NodeSemver.satisfies(release.toString(), PROVEN, NodeSemver.Options.DEFAULT);
    }

    /**
     * The releases the engine reproduces, in ascending order.
     */
    static List<NpmRelease> proven() {
        List<NpmRelease> proven = new ArrayList<>();
        for (NpmRelease release : ALL) {
            if (isProven(release)) {
                proven.add(release);
            }
        }
        return proven;
    }

    /**
     * The release a project is installed as, the one that built its LST: npm {@code version}, the version the
     * parser's npm answered ({@code NodeRuntime}), or {@link NpmRelease#PORTED} where it recorded none.
     *
     * @throws CannotKnow for a version that is no release of {@link #all()} (a prerelease, one before npm 7, one newer
     *                    than the list) or one the engine does not reproduce
     */
    public static NpmRelease installedAs(@Nullable String version) {
        if (version == null) {
            return NpmRelease.PORTED;
        }
        for (NpmRelease release : ALL) {
            if (release.toString().equals(version)) {
                if (isProven(release)) {
                    return release;
                }
                break;
            }
        }
        throw new CannotKnow("what npm " + version + " writes, since the engine reproduces npm " + ranges(proven()));
    }

    /**
     * {@code releases} as the runs of {@link #all()} they fill, in ascending order: {@code 10.8.0 - 10.9.9, 11.17.0}.
     */
    static String ranges(Collection<NpmRelease> releases) {
        List<NpmRelease> sorted = new ArrayList<>(new TreeSet<>(releases));
        List<String> ranges = new ArrayList<>();
        for (int first = 0, last; first < sorted.size(); first = last + 1) {
            last = first;
            while (last + 1 < sorted.size() && isNext(sorted.get(last), sorted.get(last + 1))) {
                last++;
            }
            ranges.add(first == last ? sorted.get(first).toString() : sorted.get(first) + " - " + sorted.get(last));
        }
        return String.join(", ", ranges);
    }

    private static boolean isNext(NpmRelease release, NpmRelease next) {
        int i = ALL.indexOf(release);
        return i >= 0 && i + 1 < ALL.size() && ALL.get(i + 1).equals(next);
    }

    private static List<NpmRelease> load() {
        String resource = "npm-releases.txt";
        try (InputStream in = NpmReleases.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is missing");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            List<NpmRelease> releases = new ArrayList<>();
            for (String version : new String(out.toByteArray(), StandardCharsets.UTF_8).split("[\r\n]+")) {
                if (!version.isEmpty()) {
                    releases.add(NpmRelease.of(version));
                }
            }
            return Collections.unmodifiableList(releases);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
