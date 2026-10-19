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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.lock.Ssri;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String RACED_FETCH = "the fetch step downloads the entry they share from the first one's " +
                                              "remote, and ";

    /**
     * {@code fetchOneRemote}'s package for a reference: the package.json of its tarball, git checkout or directory,
     * a link's mock; a .yarn-metadata.json a tarball ships hands over its manifest, of whatever type.
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
    static Fetcher remote(PackageResolver resolver, RegistrySource registry, Config config) {
        return ref -> {
            for (PackageResolver.Race race : resolver.races) {
                if (race.getFirst().reference == ref) {
                    requireOneTarball(race, ref, registry, config);
                }
            }
            if ("link".equals(ref.remote.type)) {
                return linkMock();
            }
            Manifest pkg = resolver.patterns.get(ref.patterns.get(0));
            String dest = generateModuleCachePath(ref, config.getRelease());
            if ("workspace".equals(ref.remote.type)) {
                PackageRemote registryRemote = ref.remote.registryRemote;
                if (registryRemote == null) {
                    return pkg;
                }
                // workspace-fetcher.js reads the workspace's own manifest again after fetching the registry's tarball
                String loc = String.valueOf(ref.remote.reference);
                Manifest local = Config.tryManifest(loc, String.valueOf(resolver.files.manifest(loc)),
                        config.isLooseSemver(), false, "", config.getRelease());
                new TarballFetcher(registryRemote, local.getName(), local.getVersion(), dest, registry, config)
                        .fetch();
                local.put("_uid", local.getVersion());
                return local;
            }
            if ("copy".equals(ref.remote.type)) {
                Manifest copy = pkg.copy();
                copy.data.remove("_uid");
                // copy-fetcher.js is a base-fetcher.js, which reads the copied directory's manifest again
                if (config.getRelease().linksBinsWhenFetched()) {
                    TarballFetcher.linkBins(copy.data);
                }
                return copy;
            }
            Map<String, @Nullable Object> entry = pkg.locked;
            if (entry == null && "tarball".equals(ref.remote.type)) {
                return new TarballFetcher(ref.remote, ref.name, ref.version, dest, registry, config).fetch();
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
     * The tarball a raced reference is fetched from, whichever entry yarn resolves first: a reused entry stands for
     * the one it names by its sha1 or integrity, one picked anew is downloaded.
     */
    private static void requireOneTarball(PackageResolver.Race race, PackageReference ref, RegistrySource registry,
                                          Config config) {
        TarballFetcher first = tarballFetcher(race, race.getFirst(), ref, registry, config);
        TarballFetcher other = tarballFetcher(race, race.getOther(), ref, registry, config);
        byte[] firstTarball = download(race, race.getFirst(), first);
        byte[] otherTarball = download(race, race.getOther(), other);
        boolean same;
        if (firstTarball != null && otherTarball != null) {
            same = Arrays.equals(firstTarball, otherTarball);
        } else if (firstTarball != null) {
            same = first.isNamedBy(firstTarball, other.remote);
        } else if (otherTarball != null) {
            same = other.isNamedBy(otherTarball, first.remote);
        } else {
            same = nameOneTarball(first.remote, other.remote);
        }
        if (!same) {
            throw race.cannotKnow(RACED_FETCH + "the two remotes do not name one tarball");
        }
    }

    private static TarballFetcher tarballFetcher(PackageResolver.Race race, Manifest side, PackageReference ref,
                                                 RegistrySource registry, Config config) {
        PackageRemote remote = side.remote;
        if (remote == null || side.locked == null && !"tarball".equals(remote.type)) {
            throw race.cannotKnow(RACED_FETCH + "one of them is not a registry tarball");
        }
        return new TarballFetcher(remote, ref.name, ref.version, generateModuleCachePath(ref, config.getRelease()),
                registry, config);
    }

    private static byte @Nullable [] download(PackageResolver.Race race, Manifest side, TarballFetcher fetcher) {
        if (side.locked != null) {
            return null;
        }
        try {
            return fetcher.download();
        } catch (CannotKnow e) {
            throw race.cannotKnow(RACED_FETCH + e.getMessage());
        }
    }

    // two lock entries' remotes, by every sha1 and integrity both carry
    private static boolean nameOneTarball(PackageRemote a, PackageRemote b) {
        boolean hashes = Js.truthy(a.hash) && Js.truthy(b.hash);
        boolean integrities = Js.truthy(a.integrity) && Js.truthy(b.integrity);
        return (hashes || integrities) &&
               (!hashes || String.valueOf(a.hash).equals(b.hash)) &&
               (!integrities || Ssri.stringify(a.integrity, Ssri.Release.V5_3_0)
                       .equals(Ssri.stringify(b.integrity, Ssri.Release.V5_3_0)));
    }

    /**
     * {@code fetch}: each package's fetched manifest in place of the resolver's; an optional one whose fetch fails
     * keeps the resolver's, and its cache directory, which yarn removes, goes into {@code unfetched}.
     */
    static List<Manifest> fetch(List<Manifest> pkgs, Fetcher fetcher, Set<String> unfetched) {
        return fetch(pkgs, fetcher, unfetched, YarnClassicRelease.PORTED);
    }

    /**
     * {@code fetch} as {@code release} names each package's cache folder.
     */
    static List<Manifest> fetch(List<Manifest> pkgs, Fetcher fetcher, Set<String> unfetched,
                                YarnClassicRelease release) {
        Map<String, PackageReference> pkgsPerDest = new HashMap<>();
        List<Manifest> kept = new ArrayList<>();
        for (Manifest pkg : pkgs) {
            PackageReference ref = pkg.reference;
            if (ref == null) {
                continue;
            }
            String dest = generateModuleCachePath(ref, release);
            PackageReference otherPkg = pkgsPerDest.get(dest);
            if (otherPkg != null) {
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
                fetched = fetcher.fetch(ref);
            } catch (JsError | MessageError e) {
                if (!Js.truthy(ref.optional)) {
                    throw e;
                }
                unfetched.add(generateModuleCachePath(ref, release));
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
        return generateModuleCachePath(pkg, YarnClassicRelease.PORTED);
    }

    /**
     * {@code generateModuleCachePath} of {@code release}, relative to yarn's cache: before 1.11.0, the registry, name,
     * version and uid of the package as a template literal writes them.
     */
    static String generateModuleCachePath(PackageReference pkg, YarnClassicRelease release) {
        if (!release.slugsCacheFolders()) {
            String name = Js.templateString(pkg.name);
            if (Js.truthy(pkg.registry)) {
                name = Js.templateString(pkg.registry) + "-" + name;
            }
            String uid = Js.templateString(pkg.uid);
            String hash = pkg.remote.hash;
            if (Js.truthy(pkg.version) && !equal(pkg.version, pkg.uid)) {
                uid = Js.templateString(pkg.version) + "-" + uid;
            } else if (Js.truthy(hash)) {
                uid += "-" + hash;
            }
            return name + "-" + uid;
        }
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
