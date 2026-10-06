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

import java.util.Map;
import java.util.Objects;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of pacote's {@code manifest()} for a registry spec (npm 11.17.0, {@code lib/registry.js}): pick the
 * version, normalize it as {@code @npmcli/package-json} does for a manifest, and add {@code _resolved},
 * {@code _integrity} and {@code _from} from {@code dist}. A git, file, directory or remote spec is a
 * {@link CannotKnow}: its manifest is not in the registry.
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

        @Builder.Default
        String defaultTag = "latest";

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

    private Pacote() {
    }

    // every fetcher renders --before in its constructor: an invalid date is a RangeError there
    static void checkBefore(Options opts) {
        if (opts.getBefore() != null && opts.getBefore().isNaN()) {
            throw new NpmRejection(null, "Invalid time value", null);
        }
    }

    static Map<String, Object> manifest(Spec spec, Options opts, PackumentSource source) {
        checkBefore(opts);
        Spec fetched = spec.getType() == Spec.Type.ALIAS && spec.getSubSpec() != null ? spec.getSubSpec() : spec;
        switch (Objects.requireNonNull(fetched.getType())) {
            case GIT:
                throw new CannotKnow("git manifest " + (truthy(fetched.getSaveSpec()) ? fetched.getSaveSpec() : fetched.getRaw()));
            case FILE:
                throw new CannotKnow("local tarball " + fetched.getRawSpec());
            case DIRECTORY:
                throw new CannotKnow("directory manifest " + fetched.getRawSpec());
            case REMOTE:
                throw new CannotKnow("remote tarball " + fetched.getFetchSpec());
            default:
                break;
        }
        String name = Objects.requireNonNull(fetched.getName());
        boolean full = opts.getBefore() != null;
        Map<String, Object> packument = packument(source, name, full);
        Map<String, Object> picked = PickManifest.pickManifest(packument, fetched.getFetchSpec(),
                PickManifest.Options.builder()
                        .defaultTag(opts.getDefaultTag())
                        .before(opts.getBefore())
                        .nodeVersion(opts.getNodeVersion())
                        .npmVersion(opts.getNpmVersion())
                        .build());
        Map<String, Object> mani = full || !opts.isFullMetadata() ? picked :
                source.manifest(name, versionKey(packument, picked));
        PackageJson.syncNormalize(mani);
        PackageJson.normalizeBin(mani);

        Object time = truthy(packument.get("time")) ?
                Js.get(packument.get("time"), Js.string(mani.get("version"))) : null;
        if (truthy(time)) {
            mani.put("_time", time);
        }

        Object dist = mani.get("dist");
        String integrity = null;
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
                if (truthy(opts.getIntegrity()) && !Ssri.match(Js.string(opts.getIntegrity()), parsed)) {
                    throw new NpmRejection("EINTEGRITY", "Integrity checksum failed: wanted " +
                                                         Js.string(opts.getIntegrity()) + " but got " + parsed + ".", null);
                }
                integrity = parsed;
            }
        }
        if (integrity == null && truthy(opts.getIntegrity())) {
            integrity = Js.string(opts.getIntegrity());
        }
        if (truthy(integrity)) {
            mani.put("_integrity", integrity);
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
    private static Map<String, Object> packument(PackumentSource source, String name, boolean full) {
        try {
            return source.packument(name, full);
        } catch (NpmRejection e) {
            if (!"E404".equals(e.getCode()) || full) {
                throw e;
            }
            return source.packument(name, true);
        }
    }
}
