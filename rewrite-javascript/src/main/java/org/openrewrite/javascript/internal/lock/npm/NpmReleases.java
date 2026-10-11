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
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SpecUtils;
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
import java.util.Map;
import java.util.TreeSet;

/**
 * The npm releases there are, the ones the engine reproduces, and the ones a project is installed as.
 */
public final class NpmReleases {

    /**
     * The releases the engine reproduces, a node-semver range: every recorded scenario replays as each of them and
     * gives what that release wrote. It widens as the releases around the ported one are ported and replayed.
     */
    public static final String PROVEN = "11.17.0";

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
     * The releases a project is installed as, newest first: of those its package.json allows ({@link #allowed}), the
     * ones the engine reproduces, and {@link #undeclared()} where it names none.
     *
     * @param pkg the package.json where npm installs, as npm reads it
     * @throws CannotKnow when the engine reproduces none of the releases the project names
     */
    static List<NpmRelease> candidates(Map<String, Object> pkg) {
        List<NpmRelease> allowed = allowed(pkg);
        if (allowed == null) {
            return undeclared();
        }
        List<NpmRelease> candidates = new ArrayList<>();
        for (int i = allowed.size() - 1; i >= 0; i--) {
            if (isProven(allowed.get(i))) {
                candidates.add(allowed.get(i));
            }
        }
        if (candidates.isEmpty()) {
            throw notReproduced(ranges(allowed));
        }
        return candidates;
    }

    /**
     * The releases a project is installed as when its package.json names none: every release the engine reproduces,
     * newest first, so that the result is the lock each of them writes or, where they differ, cannot be known. The
     * policy is this method's alone.
     */
    static List<NpmRelease> undeclared() {
        List<NpmRelease> releases = proven();
        Collections.reverse(releases);
        return releases;
    }

    /**
     * The releases of {@link #all()} a package.json allows, in ascending order; null where it names none:
     * <ul>
     * <li>a {@code packageManager} that names {@code npm@<version>}, as corepack reads the field, allows that
     * release alone. npm itself does not read it, so one that names another package manager, a range, a URL or what
     * corepack refuses allows every release.</li>
     * <li>else {@code devEngines.packageManager} allows the releases npm's check passes or only warns for, also those
     * too old to run it. One no release satisfies, as for another package manager, allows every release: each
     * install reports what npm does.</li>
     * </ul>
     *
     * @throws CannotKnow when {@code packageManager} names an npm that is none of these releases
     */
    static @Nullable List<NpmRelease> allowed(Map<String, Object> pkg) {
        String named = namedByPackageManager(pkg.get("packageManager"));
        if (named != null) {
            for (NpmRelease release : ALL) {
                if (release.toString().equals(named)) {
                    return Collections.singletonList(release);
                }
            }
            throw notReproduced(named);
        }
        List<NpmRelease> allowed = new ArrayList<>();
        for (NpmRelease release : ALL) {
            if (!devEnginesRefuse(pkg.get("devEngines"), release)) {
                allowed.add(release);
            }
        }
        return allowed.isEmpty() || allowed.size() == ALL.size() ? null : allowed;
    }

    /**
     * Whether {@code devEngines.packageManager} does not match npm {@code release} in a way that is an error for npm's
     * check; never for a shape the check fails on.
     */
    static boolean devEnginesRefuse(@Nullable Object devEngines, NpmRelease release) {
        if (!(devEngines instanceof Map) || !((Map<?, ?>) devEngines).containsKey("packageManager")) {
            return false;
        }
        Map<String, Object> wanted = Collections.singletonMap("packageManager",
                ((Map<?, ?>) devEngines).get("packageManager"));
        try {
            // whatever the release, as the ported one words what it finds: nothing is asked of it here
            for (NpmInstallChecks.InvalidDevEngine failure : NpmInstallChecks.checkDevEngines(wanted,
                    NpmInstallChecks.devEngines(Host.UNKNOWN, release.toString()), NpmRelease.PORTED)) {
                if (failure.isError()) {
                    return true;
                }
            }
        } catch (JsError e) {
            return false;
        }
        return false;
    }

    // corepack's parseSpec of a packageManager field where it must name a version: the npm version it names
    private static @Nullable String namedByPackageManager(@Nullable Object packageManager) {
        try {
            SpecUtils.Descriptor spec = SpecUtils.parseSpec(packageManager, "package.json", true,
                    HostEnvironment.UNKNOWN, null);
            return "npm".equals(spec.getName()) ? SpecUtils.installVersion(spec) : null;
        } catch (Rejected e) {
            return null;
        }
    }

    private static CannotKnow notReproduced(String releases) {
        return new CannotKnow("what npm " + releases + " writes, since the engine reproduces npm " +
                              ranges(proven()));
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
