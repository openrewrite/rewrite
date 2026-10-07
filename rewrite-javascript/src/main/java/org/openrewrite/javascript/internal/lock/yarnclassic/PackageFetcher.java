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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of yarn src/package-fetcher.js {@code fetch} and config.js {@code generateUniquePackageSlug} (yarn 1.22.22).
 */
final class PackageFetcher {

    private static final Pattern SLUG_CHARS = Pattern.compile("[^@a-z0-9]+");
    private static final Pattern SLUG_EDGES = Pattern.compile("^-+|-+\\z");

    /**
     * The package.json a fetch reads for a reference, normalized: from the tarball, the git checkout or the
     * directory.
     */
    interface Fetcher {
        Manifest fetch(PackageReference ref);
    }

    private PackageFetcher() {
    }

    static List<Manifest> fetch(List<Manifest> pkgs, Fetcher fetcher, List<String> warnings) {
        Map<String, PackageReference> pkgsPerDest = new HashMap<>();
        List<Manifest> kept = new ArrayList<>();
        for (Manifest pkg : pkgs) {
            PackageReference ref = pkg.reference;
            if (ref == null) {
                continue;
            }
            String dest = generateModuleCachePath(ref);
            PackageReference otherPkg = pkgsPerDest.get(dest);
            if (otherPkg != null) {
                warnings.add(Lang.format("Pattern $0 is trying to unpack in the same destination $1 as pattern $2. " +
                                         "This could result in non-deterministic behavior, skipping.",
                        ref.patterns, dest, otherPkg.patterns));
                continue;
            }
            pkgsPerDest.put(dest, ref);
            kept.add(pkg);
        }

        List<Manifest> out = new ArrayList<>();
        for (Manifest pkg : kept) {
            PackageReference ref = pkg.reference;
            Manifest newPkg = "link".equals(ref.remote.type) ? linkMock() : fetcher.fetch(ref);
            newPkg.reference = ref;
            newPkg.remote = ref.remote;
            newPkg.put("name", pkg.getName());
            newPkg.fresh = pkg.fresh;
            out.add(newPkg);
        }
        return out;
    }

    // fetchOneRemote's mock metadata for a symlinked dependency
    static Manifest linkMock() {
        Manifest mock = new Manifest();
        mock.put("_uid", "");
        mock.put("name", "");
        mock.put("version", "0.0.0");
        return mock;
    }

    static String generateModuleCachePath(PackageReference pkg) {
        return generateUniquePackageSlug(pkg) + "/node_modules/" + Js.string(pkg.name);
    }

    static String generateUniquePackageSlug(PackageReference pkg) {
        String slug = Js.string(pkg.name);
        slug = SLUG_CHARS.matcher(slug).replaceAll("-");
        slug = SLUG_EDGES.matcher(slug).replaceAll("");
        slug = (Js.truthy(pkg.registry) ? pkg.registry : "unknown") + "-" + slug;
        String hash = pkg.remote.hash;
        if (Js.truthy(pkg.version)) {
            slug += "-" + Js.string(pkg.version);
        }
        if (Js.truthy(pkg.uid) && !equal(pkg.version, pkg.uid)) {
            slug += "-" + Js.string(pkg.uid);
        } else if (Js.truthy(hash)) {
            slug += "-" + hash;
        }
        if (Js.truthy(pkg.remote.integrity)) {
            slug += "-integrity";
        }
        return slug;
    }

    private static boolean equal(@Nullable Object a, @Nullable Object b) {
        return a == null ? b == null : a.equals(b);
    }
}
