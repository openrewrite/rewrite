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

import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Yarn releases the release data holds, the ones the engine reproduces, and the one a project is installed as.
 */
public final class YarnReleases {

    /**
     * The releases the engine reproduces, a node-semver range: every recorded scenario replays as each of them and
     * gives what that release wrote ({@code YarnBerryScenarioTest} with {@code REWRITE_LOCK_EVERY_RELEASE}).
     */
    public static final String PROVEN = "4.0.0 - 4.18.1";

    private YarnReleases() {
    }

    /**
     * Every release the release data holds, Yarn 4.0.0 on, in ascending order.
     */
    public static List<String> all() {
        return YarnRelease.all();
    }

    /**
     * Whether the engine reproduces {@code version}.
     */
    public static boolean isProven(String version) {
        return NodeSemver.satisfies(version, PROVEN, NodeSemver.Options.DEFAULT);
    }

    /**
     * The release a project is installed as.
     *
     * @throws CannotKnow for a version the data does not hold (Yarn 2 and 3, a release after the data's) or one the
     *                    engine does not reproduce
     */
    static YarnRelease installedAs(String version) {
        YarnRelease release = YarnRelease.of(version);
        if (release == null) {
            throw new CannotKnow("the data of Yarn " + version);
        }
        if (!isProven(version)) {
            throw new CannotKnow("what Yarn " + version + " writes, since the engine reproduces Yarn " +
                                 ranges(proven()));
        }
        return release;
    }

    /**
     * The releases the engine reproduces, in ascending order.
     */
    static List<String> proven() {
        List<String> proven = new ArrayList<>();
        for (String release : all()) {
            if (isProven(release)) {
                proven.add(release);
            }
        }
        return proven;
    }

    /**
     * {@code releases} as the runs of {@link #all()} they fill, in ascending order: {@code 4.0.0 - 4.9.4, 4.10.3}.
     */
    static String ranges(Collection<String> releases) {
        List<String> all = all();
        List<String> sorted = new ArrayList<>();
        for (String release : all) {
            if (releases.contains(release)) {
                sorted.add(release);
            }
        }
        List<String> ranges = new ArrayList<>();
        for (int first = 0, last; first < sorted.size(); first = last + 1) {
            last = first;
            while (last + 1 < sorted.size() &&
                   all.indexOf(sorted.get(last + 1)) == all.indexOf(sorted.get(last)) + 1) {
                last++;
            }
            ranges.add(first == last ? sorted.get(first) : sorted.get(first) + " - " + sorted.get(last));
        }
        return String.join(", ", ranges);
    }
}
