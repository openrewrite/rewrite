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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of yarn src/package-request.js (yarn 1.22.22): one request for one pattern, and the manifest a lock entry
 * stands for when the request reuses it.
 */
final class PackageRequest {

    private static final Pattern GIT_PROTOCOL = Pattern.compile("^git(\\+[a-z0-9]+)?://");

    private static final List<String> REQUIRED_PACKAGE_KEYS = Arrays.asList("name", "version", "_uid");

    final @Nullable PackageRequest parentRequest;
    final List<String> parentNames;
    final Lockfile lockfile;
    final String registry;
    final PackageResolver resolver;
    final boolean optional;
    final @Nullable String hint;
    final String pattern;
    @Nullable
    Manifest foundInfo;

    PackageRequest(DependencyRequestPattern req, PackageResolver resolver) {
        this.parentRequest = req.getParentRequest();
        this.parentNames = req.getParentNames() == null ? Collections.emptyList() : req.getParentNames();
        this.lockfile = resolver.lockfile;
        this.registry = req.getRegistry();
        this.resolver = resolver;
        this.optional = req.isOptional();
        this.hint = req.getHint();
        this.pattern = req.getPattern();
    }

    void init() {
        resolver.usedRegistries.add(registry);
    }

    /**
     * The manifest the pattern's lock entry stands for, when the entry has a {@code resolved}: a git URL makes
     * the remote a git one whatever the caller asks for.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    Manifest getLocked(String remoteType) {
        Map<String, @Nullable Object> shrunk = lockfile.getLocked(pattern);
        if (shrunk == null || !Js.truthy(shrunk.get("resolved"))) {
            return null;
        }
        String resolved = (String) shrunk.get("resolved");
        String[] parts = resolved.split("#", -1);
        String url = parts[0];
        String hash = parts.length > 1 ? parts[1] : "";

        String preferredRemoteType = GIT_PROTOCOL.matcher(url).find() ? "git" : remoteType;

        Manifest manifest = new Manifest();
        manifest.put("name", shrunk.get("name"));
        manifest.put("version", shrunk.get("version"));
        manifest.put("_uid", shrunk.get("uid"));
        PackageRemote remote = new PackageRemote();
        remote.resolved = resolved;
        remote.type = preferredRemoteType;
        remote.reference = url;
        remote.hash = hash;
        remote.integrity = shrunk.get("integrity");
        remote.registry = (String) shrunk.get("registry");
        remote.packageName = shrunk.get("name") instanceof String ? (String) shrunk.get("name") : null;
        manifest.remote = remote;
        manifest.put("optionalDependencies", or(shrunk.get("optionalDependencies")));
        manifest.put("dependencies", or(shrunk.get("dependencies")));
        manifest.put("prebuiltVariants", or(shrunk.get("prebuiltVariants")));
        return manifest;
    }

    private static Object or(@Nullable Object value) {
        return Js.truthy(value) ? value : new LinkedHashMap<String, Object>();
    }

    /**
     * Fills a resolved manifest's version from its uid and requires the name, version and uid.
     */
    static void validateVersionInfo(Manifest info) {
        String human = Js.string(info.getName()) + "@" + Js.string(info.getVersion());
        info.put("version", getPackageVersion(info));
        for (String key : REQUIRED_PACKAGE_KEYS) {
            if (!Js.truthy(info.get(key))) {
                throw new MessageError("Package \"" + human + "\" doesn't have a \"" + key + "\".");
            }
        }
    }

    static @Nullable Object getPackageVersion(Manifest info) {
        return info.data.containsKey("version") && info.getVersion() != null ? info.getVersion() : info.getUid();
    }
}
