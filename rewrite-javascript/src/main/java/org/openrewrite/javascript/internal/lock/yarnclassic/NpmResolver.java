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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of yarn src/resolvers/registries/npm-resolver.js (yarn 1.22.22).
 */
final class NpmResolver {

    static final String NPM_REGISTRY_ID = "npm";

    private static final Pattern NPM_REGISTRY_RE = Pattern.compile("https?://registry\\.npmjs\\.org");
    private static final Object NOT_A_TAG = new Object();

    private NpmResolver() {
    }

    /**
     * @throws MessageError when no version matches, as yarn fails a non-interactive install.
     */
    static @Nullable Object findVersionInRegistryResponse(YarnClassicOptions config, String name, String range,
                                                          Map<String, @Nullable Object> body) {
        Object versions = body.get("versions");
        Object distTags = body.get("dist-tags");
        if (Js.truthy(versions) && Js.keys(versions).isEmpty()) {
            throw new MessageError(Lang.format("No valid versions found for $0. The package may be unpublished.",
                    body.get("name")));
        }
        if (!Js.truthy(distTags) || !Js.truthy(versions)) {
            throw new MessageError(Lang.format("Received malformed response from registry for $0. " +
                                               "The registry may be down.", name));
        }
        Object wanted = range;
        Object tag = distTag(distTags, range);
        if (tag != NOT_A_TAG) {
            wanted = tag;
        }

        Object latestVersion = Js.get(distTags, "latest");
        if (Js.truthy(latestVersion) && wanted instanceof String && satisfies(latestVersion, (String) wanted)) {
            return Js.get(versions, Js.string(latestVersion));
        }

        String satisfied = wanted instanceof String ?
                config.resolveConstraints(new ArrayList<>(Js.keys(versions)), (String) wanted) : null;
        if (satisfied != null) {
            return Js.get(versions, satisfied);
        }
        throw new MessageError(Lang.format("Couldn't find any versions for $0 that matches $1", body.get("name"),
                wanted));
    }

    private static boolean satisfies(Object version, String range) {
        try {
            return NodeSemver.satisfies(version, range, YarnClassicOptions.SEMVER);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.TYPE_ERROR, e.getMessage());
        }
    }

    // `range in tags` and `tags[range]` over the registry's JSON, whose objects inherit Object.prototype
    private static @Nullable Object distTag(Object tags, String range) {
        if (tags instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) tags;
            if (map.containsKey(range)) {
                return map.get(range);
            }
            Object function = PlainObject.objectFunction(range);
            return function != null ? function : PlainObject.PROTO.equals(range) ? new PlainObject() : NOT_A_TAG;
        }
        if (tags instanceof List) {
            List<?> list = (List<?>) tags;
            long index = Js.arrayIndex(range);
            if (index >= 0 && index < list.size()) {
                return list.get((int) index);
            }
            if ("length".equals(range)) {
                return (double) list.size();
            }
            Object function = PlainObject.arrayFunction(range);
            return function != null ? function : PlainObject.PROTO.equals(range) ? new ArrayList<>() : NOT_A_TAG;
        }
        throw new JsError(JsError.TYPE_ERROR, "Cannot use 'in' operator to search for '" + range + "' in " +
                                              Js.string(tags));
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
                    Ssri.fromHex(shasum, "sha1");
            remote.registry = NPM_REGISTRY_ID;
            remote.packageName = info.get("name");
            manifest.remote = remote;
        }
        manifest.put("_uid", info.get("version"));
        return manifest;
    }
}
