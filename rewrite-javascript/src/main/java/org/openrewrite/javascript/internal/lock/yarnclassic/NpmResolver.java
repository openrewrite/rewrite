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
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of yarn src/resolvers/registries/npm-resolver.js (yarn 1.22.22): the version a range picks from a registry
 * response, and the manifest and remote a picked version becomes. A pattern with a lock entry that has an integrity
 * reuses the entry ({@link PackageRequest#getLocked}).
 */
final class NpmResolver {

    static final String NPM_REGISTRY_ID = "npm";

    private static final Pattern NPM_REGISTRY_RE = Pattern.compile("https?://registry\\.npmjs\\.org");

    private NpmResolver() {
    }

    /**
     * The version manifest a range picks: a dist-tag's version, the {@code latest} tag's when it satisfies the range
     * (strictly), else the highest version that satisfies it loosely.
     *
     * @throws MessageError when no version matches, as yarn fails a non-interactive install.
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> findVersionInRegistryResponse(YarnClassicOptions config, String name,
                                                                       String range, Map<String, @Nullable Object> body) {
        Object versions = body.get("versions");
        Object distTags = body.get("dist-tags");
        if (versions instanceof Map && ((Map<?, ?>) versions).isEmpty()) {
            throw new MessageError(Lang.format("No valid versions found for $0. The package may be unpublished.",
                    body.get("name")));
        }
        if (!Js.truthy(distTags) || !Js.truthy(versions)) {
            throw new MessageError(Lang.format("Received malformed response from registry for $0. The registry may be " +
                                               "down.", name));
        }
        Map<String, @Nullable Object> tags = (Map<String, @Nullable Object>) distTags;
        Map<String, @Nullable Object> byVersion = (Map<String, @Nullable Object>) versions;
        if (tags.containsKey(range)) {
            range = Js.string(tags.get(range));
        }

        Object latestVersion = tags.get("latest");
        if (Js.truthy(latestVersion) && NodeSemver.satisfies(latestVersion, range, YarnClassicOptions.SEMVER)) {
            return (Map<String, @Nullable Object>) byVersion.get(Js.string(latestVersion));
        }

        String satisfied = NodeSemver.maxSatisfying(new ArrayList<>(Js.keys(byVersion)), range,
                config.isLooseSemver() ? YarnClassicOptions.LOOSE_SEMVER : YarnClassicOptions.SEMVER);
        if (satisfied != null) {
            return (Map<String, @Nullable Object>) byVersion.get(satisfied);
        }
        throw new MessageError(Lang.format("Couldn't find any versions for $0 that matches $1", body.get("name"),
                range));
    }

    static String cleanRegistry(YarnClassicOptions config, String url) {
        if (YarnClassicOptions.YARN_REGISTRY.equals(config.getOption("registry"))) {
            return NPM_REGISTRY_RE.matcher(url).replaceAll(YarnClassicOptions.YARN_REGISTRY);
        }
        return url;
    }

    /**
     * The remote and uid a version manifest picked from the registry carries: its tarball, on the yarn registry
     * while that is the configured one, with the integrity the registry gives or one from the shasum.
     */
    @SuppressWarnings("unchecked")
    static Manifest fromRegistry(YarnClassicOptions config, Map<String, @Nullable Object> info) {
        Manifest manifest = new Manifest(info);
        Object dist = info.get("dist");
        if (dist instanceof Map && Js.truthy(((Map<String, Object>) dist).get("tarball"))) {
            Map<String, @Nullable Object> d = (Map<String, @Nullable Object>) dist;
            String tarball = Js.string(d.get("tarball"));
            Object shasum = d.get("shasum");
            PackageRemote remote = new PackageRemote();
            remote.resolved = cleanRegistry(config, tarball) + "#" + Js.string(shasum);
            remote.type = "tarball";
            remote.reference = cleanRegistry(config, tarball);
            remote.hash = shasum instanceof String ? (String) shasum : null;
            remote.integrity = Js.truthy(d.get("integrity")) ? Ssri.parse(Js.string(d.get("integrity"))) :
                    Ssri.fromHex(Js.string(shasum), "sha1");
            remote.registry = NPM_REGISTRY_ID;
            remote.packageName = Js.string(info.get("name"));
            manifest.remote = remote;
        }
        manifest.put("_uid", info.get("version"));
        return manifest;
    }
}
