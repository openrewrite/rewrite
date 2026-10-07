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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of pacote's {@code manifest()} (npm 11.17.0) for a registry spec ({@code lib/registry.js}: pick the
 * version, normalize it as {@code @npmcli/package-json} does for a manifest, add {@code _resolved},
 * {@code _integrity} and {@code _from} from {@code dist}) and for a directory spec ({@code lib/dir.js} over
 * the project's files). A git, file or remote spec is a {@link CannotKnow}: its manifest is not in the
 * registry.
 */
final class Pacote {

    @Value
    @Builder
    static class Options {
        boolean fullMetadata;

        /**
         * The time value a version must have been published by, or null for no filter.
         */
        @Nullable
        Double before;

        @Nullable
        String defaultTag;

        @Nullable
        String nodeVersion;

        @Nullable
        String npmVersion;

        /**
         * The integrity the caller already holds for the package, checked against the registry's.
         */
        @Nullable
        Object integrity;
    }

    private final PackumentSource registry;
    private final VirtualTreeLoader.ProjectFiles files;
    private final String path;

    /**
     * @param path the project directory a directory spec's realpath is read relative to
     */
    Pacote(PackumentSource registry, VirtualTreeLoader.ProjectFiles files, String path) {
        this.registry = registry;
        this.files = files;
        this.path = path;
    }

    // every fetcher renders --before in its constructor: an invalid date is a RangeError there
    static void checkBefore(Options opts) {
        if (opts.getBefore() != null && opts.getBefore().isNaN()) {
            throw new NpmRejection(null, "Invalid time value", null);
        }
    }

    Map<String, Object> manifest(Spec spec, Options opts) {
        checkBefore(opts);
        Spec fetched = spec.getType() == Spec.Type.ALIAS && spec.getSubSpec() != null ? spec.getSubSpec() : spec;
        switch (Objects.requireNonNull(fetched.getType())) {
            case GIT:
                throw new CannotKnow("git manifest " + (truthy(fetched.getSaveSpec()) ? fetched.getSaveSpec() : fetched.getRaw()));
            case FILE:
                throw new CannotKnow("local tarball " + fetched.getRawSpec());
            case DIRECTORY:
                return directory(fetched, opts);
            case REMOTE:
                throw new CannotKnow("remote tarball " + fetched.getFetchSpec());
            default:
                break;
        }
        String name = Objects.requireNonNull(fetched.getName());
        boolean full = opts.getBefore() != null;
        Map<String, Object> packument = packument(name, full);
        Map<String, Object> picked = PickManifest.pickManifest(packument, fetched.getFetchSpec(),
                PickManifest.Options.builder()
                        .defaultTag(truthy(opts.getDefaultTag()) ? opts.getDefaultTag() : "latest")
                        .before(opts.getBefore())
                        .nodeVersion(opts.getNodeVersion())
                        .npmVersion(opts.getNpmVersion())
                        .build());
        Map<String, Object> mani = full || !opts.isFullMetadata() ? picked :
                withVersionDocument(picked, registry.manifest(name, versionKey(packument, picked)));
        PackageJson.syncNormalize(mani);
        PackageJson.normalizeBin(mani);

        Object time = truthy(packument.get("time")) ?
                Js.get(packument.get("time"), Js.string(mani.get("version"))) : null;
        if (truthy(time)) {
            mani.put("_time", time);
        }

        // the fetcher's constructor parses a string integrity; a value of another type is kept as it is
        Object held = opts.getIntegrity();
        String integrity = held instanceof String ? Ssri.parse((String) held) : null;
        boolean heldRaw = !(held instanceof String) && truthy(held);
        Object dist = mani.get("dist");
        if (truthy(dist)) {
            Object tarball = Js.get(dist, "tarball");
            if (tarball != Js.UNDEFINED) {
                mani.put("_resolved", tarball);
            }
            mani.put("_from", fetched.isRegistry() ? fetched.getName() + "@" + fetched.getRawSpec() :
                    fetched.getSaveSpec());
            Object distIntegrity = Js.get(dist, "integrity");
            Object shasum = Js.get(dist, "shasum");
            String parsed = truthy(distIntegrity) ? Ssri.parse(Js.string(distIntegrity)) :
                    truthy(shasum) ? Ssri.fromHex(Js.string(shasum), "sha1") : null;
            if (parsed != null) {
                if (heldRaw) {
                    throw new IllegalArgumentException("this.integrity.match is not a function");
                }
                if (integrity != null && !Ssri.match(integrity, parsed)) {
                    for (String algorithm : Ssri.algorithms(integrity)) {
                        if (Ssri.algorithms(parsed).contains(algorithm)) {
                            throw new NpmRejection("EINTEGRITY", "Integrity checksum failed when using " + algorithm +
                                                                 ": wanted " + integrity + " but got " + parsed + ".", null);
                        }
                    }
                }
                integrity = integrity == null ? parsed : Ssri.merge(integrity, parsed);
            }
        }
        if (integrity != null || heldRaw) {
            mani.put("_integrity", integrity != null ? integrity : Js.string(held));
            if (!truthy(dist)) {
                // dist.signatures is read whether or not the manifest has a dist
                throw new IllegalArgumentException("Cannot read properties of undefined (reading 'signatures')");
            }
        }
        return mani;
    }

    // DirFetcher.manifest(): the directory's package.json, with what the fetcher adds to it
    private Map<String, Object> directory(Spec spec, Options opts) {
        String realpath = Objects.requireNonNull(spec.getFetchSpec());
        String location = Relpath.relpath(path, realpath);
        PackageJson manifest = files.manifest(location);
        if (manifest == null) {
            throw new CannotKnow("directory manifest " + spec.getRawSpec() + " (" + location + ")");
        }
        Map<String, Object> mani = new LinkedHashMap<>(manifest.getContent());
        Object integrity = opts.getIntegrity();
        mani.put("_integrity", truthy(integrity) ? Js.string(integrity) : integrity);
        mani.put("_resolved", realpath);
        mani.put("_from", spec.getSaveSpec());
        return mani;
    }

    // npm reads the full packument's entry, whose key order the abbreviated entry shares and the version
    // document does not: the document only supplies what the abbreviated form omits (license, libc)
    private static Map<String, Object> withVersionDocument(Map<String, Object> entry, Map<String, Object> document) {
        Map<String, Object> mani = new LinkedHashMap<>(entry);
        for (Map.Entry<String, Object> extra : document.entrySet()) {
            if (!mani.containsKey(extra.getKey())) {
                mani.put(extra.getKey(), extra.getValue());
            }
        }
        return mani;
    }

    // the key the manifest was picked under names the version document, whatever the entry itself says
    private static String versionKey(Map<String, Object> packument, Map<String, Object> picked) {
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(packument.get("versions"))) {
            if (entry.getValue() == picked) {
                return entry.getKey();
            }
        }
        return Js.string(picked.get("version"));
    }

    // a registry that has no abbreviated packument is asked for the full one before the 404 counts
    private Map<String, Object> packument(String name, boolean full) {
        try {
            return registry.packument(name, full);
        } catch (NpmRejection e) {
            if (!"E404".equals(e.getCode()) || full) {
                throw e;
            }
            return registry.packument(name, true);
        }
    }
}
