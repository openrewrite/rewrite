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
package org.openrewrite.javascript.internal.lock.yarnclassic;

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
 * The Yarn 1 releases there are, the ones the engine reproduces, and the one a project is installed as.
 */
public final class YarnClassicReleases {

    /**
     * The releases the engine reproduces, a node-semver range: every recorded scenario replays as each of them and
     * gives what that release wrote ({@code YarnClassicScenarioTest} with {@code REWRITE_LOCK_EVERY_RELEASE}). That is
     * every stable release of Yarn 1 but 1.22.12, which the registry publishes without its bundle, so it was never run.
     */
    public static final String PROVEN = "1.0.0 - 1.22.11 || 1.22.13 - 1.22.22";

    private static final List<YarnClassicRelease> ALL = load();

    private YarnClassicReleases() {
    }

    /**
     * Every stable release of Yarn 1 the registry publishes, in ascending order
     * ({@code yarn-classic/generate/releases.js}).
     */
    public static List<YarnClassicRelease> all() {
        return ALL;
    }

    /**
     * Whether the engine reproduces {@code release}.
     */
    public static boolean isProven(YarnClassicRelease release) {
        return NodeSemver.satisfies(release.toString(), PROVEN, NodeSemver.Options.DEFAULT);
    }

    /**
     * The releases the engine reproduces, in ascending order.
     */
    static List<YarnClassicRelease> proven() {
        List<YarnClassicRelease> proven = new ArrayList<>();
        for (YarnClassicRelease release : ALL) {
            if (isProven(release)) {
                proven.add(release);
            }
        }
        return proven;
    }

    /**
     * The release a project is installed as, the one that built its LST: Yarn {@code version}, the version the
     * parser's yarn answered ({@code NodeRuntime}). Where the parser recorded none, yarn 1.22.22 installs.
     *
     * @throws CannotKnow for a version that is no release of {@link #all()} (a prerelease, Yarn 0, one newer than the
     *                    list) or one the engine does not reproduce
     */
    public static YarnClassicRelease installedAs(String version) {
        for (YarnClassicRelease release : ALL) {
            if (release.toString().equals(version)) {
                if (isProven(release)) {
                    return release;
                }
                break;
            }
        }
        throw new CannotKnow("what Yarn " + version + " writes, since the engine reproduces Yarn " +
                             ranges(proven()));
    }

    /**
     * {@code releases} as the runs of {@link #all()} they fill, in ascending order: {@code 1.19.1 - 1.22.11, 1.22.13}.
     */
    static String ranges(Collection<YarnClassicRelease> releases) {
        List<YarnClassicRelease> sorted = new ArrayList<>(new TreeSet<>(releases));
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

    private static boolean isNext(YarnClassicRelease release, YarnClassicRelease next) {
        int i = ALL.indexOf(release);
        return i >= 0 && i + 1 < ALL.size() && ALL.get(i + 1).equals(next);
    }

    private static List<YarnClassicRelease> load() {
        String resource = "yarn-classic-releases.txt";
        try (InputStream in = YarnClassicReleases.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is missing");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            List<YarnClassicRelease> releases = new ArrayList<>();
            for (String version : new String(out.toByteArray(), StandardCharsets.UTF_8).split("[\r\n]+")) {
                if (!version.isEmpty()) {
                    releases.add(YarnClassicRelease.of(version));
                }
            }
            return Collections.unmodifiableList(releases);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
