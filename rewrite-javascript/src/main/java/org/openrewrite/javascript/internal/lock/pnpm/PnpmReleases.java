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
package org.openrewrite.javascript.internal.lock.pnpm;

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
 * The pnpm releases there are, the ones the engine reproduces, and the one a project is installed as.
 */
public final class PnpmReleases {

    /**
     * The releases the engine reproduces, a node-semver range: every recorded scenario replays as each of them and
     * gives what that release wrote ({@code PnpmScenarioTest} with {@code REWRITE_LOCK_EVERY_RELEASE}), and every
     * upstream change between them and 11.2.2 in the modules the port names is a question of {@link PnpmRelease}.
     */
    public static final String PROVEN = "11.0.3 - 11.16.0";

    private static final List<String> ALL = load();

    private PnpmReleases() {
    }

    /**
     * Every stable release of pnpm 9 to 12, in ascending order ({@code pnpm/generate/pnpm-releases.js}).
     */
    public static List<String> all() {
        return ALL;
    }

    /**
     * Whether the engine reproduces every release {@code version} names, a release or a range: a release of
     * {@link #all()} in {@link #PROVEN}, or a range all of whose releases, published or to come, are.
     */
    public static boolean isProven(String version) {
        String exact = NodeSemver.valid(version, NodeSemver.Options.DEFAULT);
        if (exact != null) {
            return ALL.contains(exact) && NodeSemver.satisfies(exact, PROVEN, NodeSemver.Options.DEFAULT);
        }
        return NodeSemver.validRange(version, NodeSemver.Options.DEFAULT) != null &&
               NodeSemver.subset(version, PROVEN, NodeSemver.Options.DEFAULT);
    }

    /**
     * The release that runs the install, the one that built the LST: pnpm {@code version}, what the parser's pnpm
     * answered to {@code --version} in the project's root ({@code NodeRuntime}), which is the release on the path or
     * the one corepack starts, never one pnpm switches to. Null where the parser recorded none: pnpm 11.2.2 then runs
     * and hands the install to the release the project names ({@link Main#release}).
     *
     * @throws CannotKnow for a version that is no release of {@link #all()} (a prerelease, a release before 9, one
     *                    newer than the list) or one the engine does not reproduce
     */
    public static @Nullable PnpmRelease runs(@Nullable String version) {
        if (version == null) {
            return null;
        }
        if (ALL.contains(version) && isProven(version)) {
            return PnpmRelease.of(version);
        }
        throw new CannotKnow("what pnpm " + version + " writes, since the engine reproduces pnpm " +
                             ranges(proven()));
    }

    /**
     * The release that installs where pnpm hands the install to {@code version}, a release or the range of them the
     * inputs leave it at: the releases of {@link #all()} it names.
     *
     * @throws CannotKnow where it names a release the engine does not reproduce
     */
    public static PnpmRelease installs(String version) {
        if (!isProven(version)) {
            throw new CannotKnow("what pnpm " + version + " writes, since the engine reproduces pnpm " +
                                 ranges(proven()));
        }
        String exact = NodeSemver.valid(version, NodeSemver.Options.DEFAULT);
        if (exact != null) {
            return PnpmRelease.of(exact);
        }
        List<String> releases = new ArrayList<>();
        for (String release : ALL) {
            if (NodeSemver.satisfies(release, version, NodeSemver.Options.DEFAULT)) {
                releases.add(release);
            }
        }
        return PnpmRelease.oneOf(version, releases);
    }

    /**
     * The releases the engine reproduces, in ascending order.
     */
    static List<String> proven() {
        List<String> proven = new ArrayList<>();
        for (String release : ALL) {
            if (NodeSemver.satisfies(release, PROVEN, NodeSemver.Options.DEFAULT)) {
                proven.add(release);
            }
        }
        return proven;
    }

    /**
     * {@code releases} as the runs of {@link #all()} they fill, in ascending order: {@code 11.0.6 - 11.5.1, 11.8.0}.
     */
    public static String ranges(Collection<String> releases) {
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
        String resource = "pnpm-releases.txt";
        try (InputStream in = PnpmReleases.class.getResourceAsStream(resource)) {
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
                    PnpmRelease.of(version);
                    releases.add(version);
                }
            }
            return Collections.unmodifiableList(releases);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
