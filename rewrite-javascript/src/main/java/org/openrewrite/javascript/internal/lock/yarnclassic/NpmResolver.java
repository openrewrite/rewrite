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
import org.openrewrite.javascript.internal.lock.EventLoop;
import org.openrewrite.javascript.internal.lock.EventLoop.Promise;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Ssri;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of yarn src/resolvers/registries/npm-resolver.js (yarn 1.22.22), and yarn-resolver.js, its subclass for the
 * {@code yarn:} protocol.
 */
final class NpmResolver {

    static final String NPM_REGISTRY_ID = "npm";

    private static final Pattern NPM_REGISTRY_RE = Pattern.compile("https?://registry\\.npmjs\\.org");

    // before 1.10.0: /http[s]:\/\/registry.npmjs.org/g, an https url only, whose dots match any character
    private static final Pattern HTTPS_NPM_REGISTRY = Pattern.compile("https://registry.npmjs.org");
    private static final Object NOT_A_TAG = new Object();

    private final PackageRequest request;
    private final Config config;
    final String name;
    final String range;

    NpmResolver(PackageRequest request, String name, String range) {
        this.request = request;
        this.config = request.config;
        this.name = name;
        this.range = range;
    }

    /**
     * @throws MessageError when no version matches, as yarn fails a non-interactive install.
     */
    static @Nullable Object findVersionInRegistryResponse(Config config, String name, String range,
                                                          @Nullable Object body) {
        Object versions = Js.get(body, "versions");
        Object distTags = Js.get(body, "dist-tags");
        YarnClassicRelease release = config.getRelease();
        if (release.rejectsAResponseWithoutVersions() && Js.truthy(versions) && Js.keys(versions).isEmpty()) {
            throw new MessageError(Lang.format(config.getRelease(), "No valid versions found for $0. The package may be unpublished.",
                    Js.get(body, "name")));
        }
        if (!Js.truthy(distTags) || release.rejectsAResponseWithoutVersions() && !Js.truthy(versions)) {
            throw new MessageError(Lang.format(config.getRelease(), "Received malformed response from registry for $0. " +
                                               "The registry may be down.",
                    release.namesTheRequestedPackageOfAMalformedResponse() ? name : Js.get(body, "name")));
        }
        Object wanted = range;
        Object tag = distTag(distTags, range);
        if (tag != NOT_A_TAG) {
            wanted = tag;
        }

        Object latestVersion = Js.get(distTags, "latest");
        if (release.prefersTheLatestTag() && Js.truthy(latestVersion) && wanted instanceof String &&
            satisfies(latestVersion, (String) wanted)) {
            return Js.get(readable(body, versions, Js.string(latestVersion)), Js.string(latestVersion));
        }

        String satisfied = wanted instanceof String ?
                config.resolveConstraints(new ArrayList<>(Js.keys(readable(body, versions, null))), (String) wanted) :
                null;
        if (satisfied != null) {
            return Js.get(versions, satisfied);
        }
        throw new MessageError(Lang.format(config.getRelease(), "Couldn't find any versions for $0 that matches $1",
                Js.get(body, "name"), wanted));
    }

    /**
     * {@code body.versions} as JavaScript reads it where 1.5.0's check has not refused a response without versions:
     * reading one of its versions ({@code key}), or {@code Object.keys} of it ({@code key} null), throws where it is
     * undefined or null.
     */
    private static @Nullable Object readable(@Nullable Object body, @Nullable Object versions, @Nullable String key) {
        if (versions != null) {
            return versions;
        }
        if (key == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot convert undefined or null to object");
        }
        boolean isNull = body instanceof Map && ((Map<?, ?>) body).containsKey("versions");
        throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " + (isNull ? "null" : "undefined") +
                                              " (reading '" + key + "')");
    }

    private static boolean satisfies(Object version, String range) {
        try {
            return NodeSemver.satisfies(version, range, Config.SEMVER);
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

    static String cleanRegistry(Config config, String url) {
        if (Config.YARN_REGISTRY.equals(config.getOption("registry"))) {
            return (config.getRelease().cleansHttpNpmRegistryUrls() ? NPM_REGISTRY_RE : HTTPS_NPM_REGISTRY)
                    .matcher(url).replaceAll(Config.YARN_REGISTRY);
        }
        return url;
    }

    /**
     * The entry's {@code prebuiltVariants} key util/package-name-utils.js {@code getPlatformSpecificPackageFilename}
     * gives on the host, when the entry has it; any of the entry's when the host's platform is not known.
     */
    private @Nullable String prebuiltVariant(Manifest shrunk) {
        Object prebuiltVariants = shrunk.get("prebuiltVariants");
        String name = Js.string(shrunk.getName());
        String prefix = (name.startsWith("@") ? name.substring(1).replaceFirst("/", "-") : name) + "-v" +
                        Js.string(shrunk.getVersion()) + "-";
        Host host = request.resolver.host;
        if (host.getOs() != null && host.getCpu() != null) {
            prefix += host.getOs() + "-" + host.getCpu() + "-";
        }
        for (String key : Js.keys(prebuiltVariants)) {
            if (key.startsWith(prefix) && Js.truthy(Js.get(prebuiltVariants, key))) {
                return key;
            }
        }
        return null;
    }

    Promise<@Nullable Object> resolveRequest(@Nullable Object desiredVersion) {
        if (config.isOffline()) {
            throw config.offlineCache();
        }
        if (name.isEmpty()) {
            // before 1.5.0 a scoped alias reads as the name "": its request is the registry's root
            throw new CannotKnow("what the registry answers at its root, which yarn asks for the package \"\" of " +
                                 request.pattern);
        }
        String escapedName = NpmRegistry.escapeName(name);
        String desiredRange = Js.truthy(desiredVersion) ? Js.string(desiredVersion) : range;
        Io<Object> io = request.resolver.npmRegistry.request(escapedName, request.io);
        request.awaited(io);
        return io.promise.then(body -> Promise.resolved(request.resolver.loop, Js.truthy(body) ?
                findVersionInRegistryResponse(config, escapedName, desiredRange, body) : null));
    }

    @SuppressWarnings("unchecked")
    Promise<Manifest> resolve() {
        EventLoop loop = request.resolver.loop;
        Manifest shrunk = request.getLocked("tarball");
        if (shrunk != null && config.isPackBuiltPackages() && Js.truthy(shrunk.get("prebuiltVariants")) &&
            shrunk.remote != null) {
            String prebuiltName = prebuiltVariant(shrunk);
            if (prebuiltName != null && Js.truthy(config.getOfflineMirrorPath())) {
                throw new CannotKnow("whether the offline mirror holds the prebuilt package " + prebuiltName +
                                     " for " + request.pattern);
            }
        }
        // before 1.10.0 no entry lacks the integrity yarn does not record
        if (shrunk != null && shrunk.remote != null && (!config.getRelease().recordsIntegrity() ||
                                                         Js.truthy(shrunk.remote.integrity) ||
                                                         !config.isAutoAddIntegrity())) {
            return Promise.resolved(loop, shrunk);
        }

        Object desiredVersion = shrunk != null && Js.truthy(shrunk.getVersion()) ? shrunk.getVersion() : null;
        return resolveRequest(desiredVersion).then(found -> {
            if (found == null) {
                throw new MessageError(Lang.format(config.getRelease(), "Couldn't find package $0 on the $1 registry.", name,
                        NPM_REGISTRY_ID));
            }
            Map<String, @Nullable Object> data = found instanceof Map ? (Map<String, @Nullable Object>) found :
                    new LinkedHashMap<>();
            Object dist = data.get("dist");
            if (shrunk != null && shrunk.remote != null) {
                Object integrity = Js.get(dist, "integrity");
                shrunk.remote.integrity = Js.truthy(dist) && Js.truthy(integrity) ?
                        Ssri.parse(integrity, Ssri.Release.V5_3_0, Js::templateString) :
                        Ssri.fromHex(Js.truthy(dist) && Js.truthy(Js.get(dist, "shasum")) ?
                                Js.get(dist, "shasum") : "", "sha1", Ssri.Release.V5_3_0);
                return Promise.resolved(loop, shrunk);
            }

            Manifest info = new Manifest(data);
            if (dist != null && Js.truthy(Js.get(dist, "tarball"))) {
                String tarball = cleanRegistry(config, Js.templateString(Js.get(dist, "tarball")));
                Object shasum = Js.get(dist, "shasum");
                Object integrity = Js.get(dist, "integrity");
                PackageRemote remote = new PackageRemote();
                remote.resolved = tarball + "#" + Js.templateString(shasum);
                remote.type = "tarball";
                remote.reference = tarball;
                remote.hash = shasum instanceof String ? (String) shasum : null;
                remote.integrity = !config.getRelease().recordsIntegrity() ? null : Js.truthy(integrity) ?
                        Ssri.parse(integrity, Ssri.Release.V5_3_0, Js::templateString) :
                        Ssri.fromHex(shasum, "sha1", Ssri.Release.V5_3_0);
                remote.registry = NPM_REGISTRY_ID;
                remote.packageName = data.get("name");
                info.remote = remote;
            }
            info.put("_uid", data.get("version"));
            return Promise.resolved(loop, info);
        });
    }
}
