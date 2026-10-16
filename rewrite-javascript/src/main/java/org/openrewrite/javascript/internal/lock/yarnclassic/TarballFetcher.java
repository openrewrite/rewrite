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

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Port of yarn src/fetchers/tarball-fetcher.js with base-fetcher.js {@code fetch} (yarn 1.22.22) for a version the
 * registry names anew: the package.json of the tarball the registry serves.
 */
final class TarballFetcher {

    private static final Pattern DRIVE = Pattern.compile("^[a-z]:\\z", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATIVE_PATH = Pattern.compile("^(?:\\.{1,2})?[\\\\/]");

    final PackageRemote remote;
    private final @Nullable Object name;
    private final @Nullable Object version;
    private final String dest;
    private final RegistrySource registry;
    private final Config config;

    /**
     * What package-fetcher.js {@code fetchOneRemote} hands the fetcher: the remote, the name and version a package
     * without a manifest gets, the cache directory.
     */
    TarballFetcher(PackageRemote remote, @Nullable Object name, @Nullable Object version, String dest,
                   RegistrySource registry, Config config) {
        this.remote = remote;
        this.name = name;
        this.version = version;
        this.dest = dest;
        this.registry = registry;
        this.config = config;
    }

    /**
     * base-fetcher.js {@code fetch}: the manifest of what {@code _fetch} extracted, or one made of the name and
     * version when it holds none; the .yarn-metadata.json a package ships is read as it is.
     */
    @Nullable
    Object fetch() {
        return manifest(fetchTarball(), dest, Js.string(remote.registry), name, version, config.isLooseSemver(),
                config.getRelease());
    }

    /**
     * The package base-fetcher.js reads from an extracted directory, its bins' targets checked as it links them.
     */
    static @Nullable Object manifest(TarFs extracted, String dest, String registry, @Nullable Object name,
                                     @Nullable Object version, boolean looseSemver) {
        return manifest(extracted, dest, registry, name, version, looseSemver, YarnClassicRelease.PORTED);
    }

    /**
     * The package base-fetcher.js of {@code release} reads from an extracted directory: from 1.11.0 its bins' targets
     * are checked as it links them.
     */
    static @Nullable Object manifest(TarFs extracted, String dest, String registry, @Nullable Object name,
                                     @Nullable Object version, boolean looseSemver, YarnClassicRelease release) {
        Object pkg = Config.readManifest(extracted, dest, registry, looseSemver, release);
        if (pkg == Config.NO_MANIFEST) {
            Manifest defaultManifest = new Manifest();
            defaultManifest.put("name", name);
            defaultManifest.put("version", version);
            pkg = NormalizeManifest.normalizeManifest(defaultManifest, null, "", false, looseSemver, release);
        }
        if (release.linksBinsWhenFetched()) {
            linkBins(pkg instanceof Manifest ? ((Manifest) pkg).data : pkg);
        }
        return pkg;
    }

    /**
     * base-fetcher.js {@code fetch} from 1.11.0: each bin of the package fetched is linked in its cache folder, where
     * {@code path.resolve} throws for a target that is no string.
     */
    static void linkBins(@Nullable Object pkg) {
        Object bin = Js.get(pkg, "bin");
        if (Js.truthy(bin)) {
            for (String binName : Js.keys(bin)) {
                Object target = Js.get(bin, binName);
                if (!(target instanceof String)) {
                    throw Js.invalidArgType("The \"paths[1]\" argument must be of type string", target);
                }
            }
        }
    }

    // _fetch: a local path is the filesystem's; any other reference is the registry's, verified, gunzipped, untarred
    private TarFs fetchTarball() {
        byte[] tarball = download();
        TarFs extracted = untar(reference(), tarball, config.getRelease());
        Ssri.Integrity integrity = integrity(remote);
        if (integrity != null && integrity.getHashes().isEmpty()) {
            throw new MessageError(reference() + ": " + Lang.format(config.getRelease(), "Integrity checked failed for $0 (none of " +
                                                                    "the specified algorithms are supported)",
                    remote.packageName));
        }
        return extracted;
    }

    /**
     * {@code _fetch}'s request: the registry's tarball, checked against the remote's sha1 and integrity.
     */
    byte[] download() {
        String reference = reference();
        boolean isFilePath = String.valueOf(remote.reference).startsWith("file:");
        ExoticResolvers.LegacyUrl url = ExoticResolvers.LegacyUrl.parse(reference);
        String pathname = url.path == null ? null : url.path.replaceFirst("\\?.*", "");
        boolean isRelativePath = url.protocol != null ? DRIVE.matcher(url.protocol).find() :
                pathname != null && RELATIVE_PATH.matcher(pathname).find();
        String pkg = Js.string(name) + "@" + Js.string(version);
        if (isFilePath || isRelativePath) {
            throw new CannotKnow("the tarball " + reference + " of " + pkg + " (a file on disk)");
        }
        String packageIdent = Js.truthy(remote.packageName) ? NpmRegistry.escapeName(Js.string(remote.packageName)) :
                reference;
        byte[] tarball = registry.tarball(reference, Js.string(Js.truthy(remote.packageName) ? remote.packageName :
                name), Js.string(version), config.getAuthorization(packageIdent, reference));
        if (tarball == null) {
            throw new CannotKnow("whether yarn's cache holds " + pkg + ", whose tarball " + reference +
                                 " answers 404 (yarn fails on a cold cache and installs from a warm one)");
        }
        if (mismatches(tarball, remote)) {
            throw new CannotKnow("whether yarn's cache holds " + pkg + ", whose tarball " + reference +
                                 " does not match the integrity the registry gives it");
        }
        return tarball;
    }

    /**
     * Whether a tarball is the one another remote of this package names by its sha1 or its integrity.
     */
    boolean isNamedBy(byte[] tarball, PackageRemote other) {
        Ssri.Integrity integrity = integrity(other);
        return (Js.truthy(other.hash) || integrity != null && !integrity.getHashes().isEmpty()) &&
               !mismatches(tarball, other);
    }

    // the sha1 hash and the integrity both; before 1.19.0 the integrity, else the hash (_findIntegrity)
    private boolean mismatches(byte[] tarball, PackageRemote expected) {
        Ssri.Integrity integrity = integrity(expected);
        boolean checksHash = Js.truthy(expected.hash) &&
                             (config.getRelease().checksTheHashBesideTheIntegrity() || !Js.truthy(expected.integrity));
        return checksHash &&
               !matches(tarball, Objects.requireNonNull(Ssri.fromHex(expected.hash, "sha1", Ssri.Release.V5_3_0))) ||
               integrity != null && !integrity.getHashes().isEmpty() && !matches(tarball, integrity);
    }

    private static Ssri.@Nullable Integrity integrity(PackageRemote of) {
        return Js.truthy(of.integrity) ? Ssri.parse(of.integrity, Ssri.Release.V5_3_0, Js::templateString) : null;
    }

    // the reference _fetch requests, without its file: prefix
    private String reference() {
        String reference = String.valueOf(remote.reference);
        return reference.startsWith("file:") ? reference.substring("file:".length()) : reference;
    }

    /**
     * {@code createExtractor}'s gunzip and tar-fs extract, their errors as request-manager rejects them, the url first.
     */
    static TarFs untar(String url, byte[] tarball) {
        return untar(url, tarball, YarnClassicRelease.PORTED);
    }

    /**
     * {@code untar} as {@code release} extracts.
     */
    static TarFs untar(String url, byte[] tarball, YarnClassicRelease release) {
        byte[] tar;
        try {
            tar = GunzipMaybe.gunzip(tarball);
        } catch (JsError e) {
            throw new JsError(e.getName(), url + ": " + e.getMessage());
        }
        try {
            return TarFs.extract(tar, release.jailsTarballLinks(), release.tarFs(), release.tarStream(),
                    release.failsAnArchiveThatEndsEarly());
        } catch (JsError e) {
            if (!release.reportsCorruptArchives()) {
                throw new JsError(e.getName(), url + ": " + e.getMessage());
            }
            throw new MessageError(url + ": " + Lang.format(release, "Extracting tar content of $1 failed, the file appears " +
                                                            "to be corrupt: $0", e.getMessage(), null));
        }
    }

    // isHashAlgorithmSupported over every algorithm named, then ssri's integrityStream check
    private boolean matches(byte[] data, Ssri.Integrity expected) {
        if (expected.getHashes().isEmpty()) {
            return false;
        }
        for (String candidate : Js.keys(expected.getHashes())) {
            if (!digestible(candidate.toLowerCase(Locale.ROOT))) {
                throw new CannotKnow("whether the Node yarn runs under has the " + candidate + " digest the " +
                                     "integrity of " + Js.string(name) + "@" + Js.string(version) + " names (its " +
                                     "OpenSSL build decides, and yarn checks every algorithm named)");
            }
        }
        return Ssri.checkData(data, expected, Ssri.Release.V5_3_0) != null;
    }

    private static boolean digestible(String algorithm) {
        return Arrays.asList("md5", "sha1", "sha224", "sha256", "sha384", "sha512").contains(algorithm);
    }
}
