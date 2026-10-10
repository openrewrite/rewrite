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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of yarn src/package-fetcher.js and config.js {@code generateUniquePackageSlug} (yarn 1.22.22).
 */
final class PackageFetcher {

    private static final Pattern SLUG_CHARS = Pattern.compile("[^@a-z0-9]+");
    private static final Pattern SLUG_EDGES = Pattern.compile("^-+|-+\\z");

    /**
     * {@code fetchOneRemote}'s package for a reference: the package.json of its tarball, git checkout or directory;
     * a .yarn-metadata.json a tarball ships hands over its manifest, of whatever type.
     */
    interface Fetcher {
        @Nullable
        Object fetch(PackageReference ref);
    }

    private PackageFetcher() {
    }

    /**
     * {@code fetchOneRemote} with what stands for each remote's package.json: a version picked anew reads its
     * tarball's, a reused lock entry is what it holds, a directory reads its own.
     */
    static Fetcher remote(PackageResolver resolver, RegistrySource registry, YarnClassicOptions config) {
        return ref -> {
            Manifest pkg = resolver.patterns.get(ref.patterns.get(0));
            if ("workspace".equals(ref.remote.type)) {
                return pkg;
            }
            if ("copy".equals(ref.remote.type)) {
                Manifest copy = pkg.copy();
                copy.data.remove("_uid");
                return copy;
            }
            Map<String, @Nullable Object> entry = pkg.locked;
            if (entry == null && "tarball".equals(ref.remote.type)) {
                return new TarballFetcher(ref, registry, config, resolver.warnings).fetch();
            }
            Manifest locked = new Manifest();
            locked.put("name", entry == null ? pkg.getName() : entry.get("name"));
            locked.put("version", entry == null ? pkg.getVersion() : entry.get("version"));
            locked.put("dependencies", entry == null ? pkg.get("dependencies") : entry.get("dependencies"));
            locked.put("optionalDependencies", entry == null ? pkg.get("optionalDependencies") :
                    entry.get("optionalDependencies"));
            return locked;
        };
    }

    /**
     * {@code fetch}: each package's fetched manifest in place of the resolver's; an optional one whose fetch fails
     * keeps the resolver's, and its cache directory, which yarn removes, goes into {@code unfetched}.
     */
    static List<Manifest> fetch(List<Manifest> pkgs, Fetcher fetcher, List<String> warnings, Set<String> unfetched) {
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
            Object fetched;
            try {
                fetched = "link".equals(ref.remote.type) ? linkMock() : fetcher.fetch(ref);
            } catch (JsError | MessageError e) {
                if (!Js.truthy(ref.optional)) {
                    throw e;
                }
                warnings.add(String.valueOf(e.getMessage()));
                unfetched.add(generateModuleCachePath(ref));
                fetched = null;
            }
            if (!Js.truthy(fetched)) {
                out.add(pkg);
                continue;
            }
            Manifest newPkg = fetched instanceof Manifest ? (Manifest) fetched : Config.manifest(fetched, "_reference");
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
        if (!(pkg.name instanceof String)) {
            throw new JsError(JsError.TYPE_ERROR, "slug.replace is not a function");
        }
        String slug = (String) pkg.name;
        slug = SLUG_CHARS.matcher(slug).replaceAll("-");
        slug = SLUG_EDGES.matcher(slug).replaceAll("");
        slug = (Js.truthy(pkg.registry) ? Js.templateString(pkg.registry) : "unknown") + "-" + slug;
        String hash = pkg.remote.hash;
        if (Js.truthy(pkg.version)) {
            slug += "-" + Js.templateString(pkg.version);
        }
        if (Js.truthy(pkg.uid) && !equal(pkg.version, pkg.uid)) {
            slug += "-" + Js.templateString(pkg.uid);
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
