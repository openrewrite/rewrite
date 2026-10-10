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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Port of yarn src/fetchers/tarball-fetcher.js with base-fetcher.js {@code fetch} (yarn 1.22.22) for a version the
 * registry names anew: the package.json of the tarball the registry serves.
 */
final class TarballFetcher {

    private static final Pattern DRIVE = Pattern.compile("^[a-z]:\\z", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATIVE_PATH = Pattern.compile("^(?:\\.{1,2})?[\\\\/]");
    private static final List<String> DEFAULT_PRIORITY = Arrays.asList("md5", "whirlpool", "sha1", "sha224",
            "sha256", "sha384", "sha512", "sha3", "sha3-256", "sha3-384", "sha3-512", "sha3_256", "sha3_384",
            "sha3_512");

    private final PackageReference ref;
    private final PackageRemote remote;
    private final RegistrySource registry;
    private final YarnClassicOptions config;
    private final List<String> warnings;

    TarballFetcher(PackageReference ref, RegistrySource registry, YarnClassicOptions config, List<String> warnings) {
        this.ref = ref;
        this.remote = ref.remote;
        this.registry = registry;
        this.config = config;
        this.warnings = warnings;
    }

    /**
     * base-fetcher.js {@code fetch}: the manifest of what {@code _fetch} extracted, or one made of the name and
     * version when it holds none; the .yarn-metadata.json a package ships is read as it is.
     */
    @Nullable
    Object fetch() {
        return manifest(fetchTarball(), PackageFetcher.generateModuleCachePath(ref), Js.string(remote.registry),
                ref.name, ref.version, config.isLooseSemver(), warnings);
    }

    /**
     * The package base-fetcher.js reads from an extracted directory, its bins' targets checked as it links them.
     */
    static @Nullable Object manifest(TarFs extracted, String dest, String registry, @Nullable Object name,
                                     @Nullable Object version, boolean looseSemver, List<String> warnings) {
        Object pkg = Config.readManifest(extracted, dest, registry, looseSemver, warnings);
        if (pkg == Config.NO_MANIFEST) {
            Manifest defaultManifest = new Manifest();
            defaultManifest.put("name", name);
            defaultManifest.put("version", version);
            pkg = NormalizeManifest.normalizeManifest(defaultManifest, null, "", false, looseSemver, warnings);
        }
        Object data = pkg instanceof Manifest ? ((Manifest) pkg).data : pkg;
        Object bin = Js.get(data, "bin");
        if (Js.truthy(bin)) {
            for (String binName : Js.keys(bin)) {
                Object target = Js.get(bin, binName);
                if (!(target instanceof String)) {
                    throw JsError.invalidArgType("The \"paths[1]\" argument must be of type string", target);
                }
            }
        }
        return pkg;
    }

    // _fetch: a local path is the filesystem's; any other reference is the registry's, verified, gunzipped, untarred
    private TarFs fetchTarball() {
        String reference = String.valueOf(remote.reference);
        boolean isFilePath = reference.startsWith("file:");
        if (isFilePath) {
            reference = reference.substring("file:".length());
        }
        ExoticResolvers.LegacyUrl url = ExoticResolvers.LegacyUrl.parse(reference);
        String pathname = url.path == null ? null : url.path.replaceFirst("\\?.*", "");
        boolean isRelativePath = url.protocol != null ? DRIVE.matcher(url.protocol).find() :
                pathname != null && RELATIVE_PATH.matcher(pathname).find();
        String pkg = Js.string(ref.name) + "@" + Js.string(ref.version);
        if (isFilePath || isRelativePath) {
            throw new CannotKnow("the tarball " + reference + " of " + pkg + " (a file on disk)");
        }
        byte[] tarball = registry.tarball(reference, Js.string(Js.truthy(remote.packageName) ? remote.packageName :
                ref.name), Js.string(ref.version));
        if (tarball == null) {
            throw new CannotKnow("whether yarn's cache holds " + pkg + ", whose tarball " + reference +
                                 " answers 404 (yarn fails on a cold cache and installs from a warm one)");
        }
        Ssri.Integrity integrity = Js.truthy(remote.integrity) ? Ssri.parse(remote.integrity) : null;
        if (Js.truthy(remote.hash) && !matches(tarball, Ssri.fromHex(remote.hash, "sha1")) ||
            integrity != null && !integrity.hashes.isEmpty() && !matches(tarball, integrity)) {
            throw new CannotKnow("whether yarn's cache holds " + pkg + ", whose tarball " + reference +
                                 " does not match the integrity the registry gives it");
        }
        TarFs extracted = untar(reference, tarball);
        if (integrity != null && integrity.hashes.isEmpty()) {
            throw new MessageError(reference + ": " + Lang.format("Integrity checked failed for $0 (none of the " +
                                                                  "specified algorithms are supported)",
                    remote.packageName));
        }
        return extracted;
    }

    /**
     * {@code createExtractor}'s gunzip and tar-fs extract, their errors as request-manager rejects them, the url first.
     */
    static TarFs untar(String url, byte[] tarball) {
        byte[] tar;
        try {
            tar = GunzipMaybe.gunzip(tarball);
        } catch (JsError e) {
            throw new JsError(e.getName(), url + ": " + e.getMessage());
        }
        try {
            return TarFs.extract(tar);
        } catch (JsError e) {
            throw new MessageError(url + ": " + Lang.format("Extracting tar content of $1 failed, the file appears " +
                                                            "to be corrupt: $0", e.getMessage(), null));
        }
    }

    // ssri's integrityStream check: the strongest algorithm the expected integrity names, one of its digests
    private boolean matches(byte[] data, Ssri.Integrity expected) {
        if (expected.hashes.isEmpty()) {
            return false;
        }
        String algorithm = null;
        for (String candidate : Js.keys(expected.hashes)) {
            String name = candidate.toLowerCase(Locale.ROOT);
            if (!digestible(name)) {
                throw new CannotKnow("whether the tarball of " + Js.string(ref.name) + "@" + Js.string(ref.version) +
                                     " matches its " + candidate + " integrity");
            }
            if (algorithm == null ||
                DEFAULT_PRIORITY.indexOf(algorithm.toLowerCase(Locale.ROOT)) < DEFAULT_PRIORITY.indexOf(name)) {
                algorithm = candidate;
            }
        }
        String digest = digest(data, algorithm);
        for (Ssri.Hash hash : expected.hashes.get(algorithm)) {
            if (digest.equals(hash.digest)) {
                return true;
            }
        }
        return false;
    }

    private static boolean digestible(String algorithm) {
        return Arrays.asList("md5", "sha1", "sha224", "sha256", "sha384", "sha512").contains(algorithm);
    }

    private static String digest(byte[] data, String algorithm) {
        String name = algorithm.toLowerCase(Locale.ROOT);
        try {
            MessageDigest md = MessageDigest.getInstance("md5".equals(name) ? "MD5" :
                    "SHA-" + name.substring("sha".length()));
            return Base64.getEncoder().encodeToString(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
