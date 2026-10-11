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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    final Config config;
    final boolean optional;
    final @Nullable String hint;
    final String pattern;
    @Nullable
    Manifest foundInfo;

    /**
     * The I/O this request waited on, its parents' included, issued since its root request began.
     */
    final Set<Integer> io;

    /**
     * The root request this one belongs to, in the order the install runs them.
     */
    final int root;

    PackageRequest(DependencyRequestPattern req, PackageResolver resolver) {
        this.parentRequest = req.getParentRequest();
        this.parentNames = req.getParentNames() == null ? Collections.emptyList() : req.getParentNames();
        this.lockfile = resolver.lockfile;
        this.registry = req.getRegistry();
        this.resolver = resolver;
        this.config = resolver.config;
        this.optional = req.isOptional();
        this.hint = req.getHint();
        this.pattern = req.getPattern();
        this.io = parentRequest == null ? new LinkedHashSet<>() : new LinkedHashSet<>(parentRequest.io);
        this.root = resolver.rootIndex;
    }

    void init() {
        resolver.usedRegistries.add(registry);
    }

    void awaited(Io<?> op) {
        if (op.id >= resolver.rootMark) {
            io.add(op.id);
        }
        for (int causal : op.causal) {
            if (causal >= resolver.rootMark) {
                io.add(causal);
            }
        }
    }

    @Nullable
    Manifest getLocked(String remoteType) {
        Map<String, @Nullable Object> shrunk = lockfile.getLocked(pattern);
        if (shrunk == null || !Js.truthy(shrunk.get("resolved"))) {
            return null;
        }
        if (!(shrunk.get("resolved") instanceof String)) {
            throw new JsError(JsError.TYPE_ERROR, "url.split is not a function");
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
        remote.registry = shrunk.get("registry");
        remote.packageName = shrunk.get("name");
        manifest.remote = remote;
        manifest.put("optionalDependencies", or(shrunk.get("optionalDependencies")));
        manifest.put("dependencies", or(shrunk.get("dependencies")));
        manifest.put("prebuiltVariants", or(shrunk.get("prebuiltVariants")));
        manifest.locked = shrunk;
        return manifest;
    }

    private static Object or(@Nullable Object value) {
        return Js.truthy(value) ? value : new PlainObject();
    }

    Promise<Manifest> findVersionOnRegistry(String pattern) {
        return normalize(pattern).then(normalized -> {
            ExoticResolvers.Kind exoticResolver = ExoticResolvers.getExoticResolver(normalized.getRange());
            if (exoticResolver != null) {
                return findExoticVersionInfo(exoticResolver, normalized.getRange()).then(found -> {
                    Manifest data = found.copy();
                    data.put("name", normalized.getName());
                    return Promise.resolved(resolver.loop, data);
                });
            }
            NpmResolver npmResolver = getRegistryResolver(normalized.getName(), normalized.getRange());
            return Promise.async(resolver.loop, npmResolver::resolve).recover(err -> {
                // only JavaScript's errors are yarn's; the host's failure to fetch stays the host's
                if (err instanceof JsError && parentRequest != null && Js.truthy(parentRequest.pattern)) {
                    throw new MessageError(Lang.format("Couldn't find package $0 required by $1 on the $2 registry.",
                            pattern, parentRequest.pattern, registry));
                }
                throw err;
            });
        });
    }

    private NpmResolver getRegistryResolver(String name, String range) {
        if ("npm".equals(registry) || "yarn".equals(registry)) {
            return new NpmResolver(this, name, range);
        }
        throw new MessageError(Lang.format("Unknown registry resolver $0", registry));
    }

    Promise<String> normalizeRange(String pattern) {
        EventLoop loop = resolver.loop;
        if (pattern.indexOf(':') > -1 || pattern.indexOf('@') > -1 ||
            ExoticResolvers.getExoticResolver(pattern) != null) {
            return Promise.resolved(loop, pattern);
        }
        if (NodeSemver.validRange(pattern, Config.SEMVER) == null) {
            String location = ProjectFiles.location(resolver.files.cwd(), pattern);
            if (location == null) {
                throw new CannotKnow("whether " + pattern + "/package.json exists outside the project, which makes " +
                                     "the range " + pattern + " a file: dependency");
            }
            Io<Boolean> exists = Io.issue(loop, io, () -> resolver.files.hasManifest(location) ||
                                                              resolver.files.holds(PosixPath.join(location,
                                                                      "package.json")));
            awaited(exists);
            return exists.promise.then(found -> {
                if (Boolean.TRUE.equals(found)) {
                    return Promise.resolved(loop, "file:" + pattern);
                }
                return Promise.resolved(loop, pattern);
            });
        }
        return Promise.resolved(loop, pattern);
    }

    Promise<NormalizedPattern> normalize(String pattern) {
        NormalizedPattern normalized = NormalizedPattern.normalizePattern(pattern);
        return normalizeRange(normalized.getRange()).then(range -> Promise.resolved(resolver.loop,
                new NormalizedPattern(normalized.isHasVersion(), normalized.getName(), range)));
    }

    Promise<Manifest> findExoticVersionInfo(ExoticResolvers.Kind kind, String range) {
        return Promise.async(resolver.loop, () -> ExoticResolvers.resolve(kind, this, range));
    }

    Promise<Manifest> findVersionInfo() {
        ExoticResolvers.Kind exoticResolver = ExoticResolvers.getExoticResolver(pattern);
        if (exoticResolver != null) {
            return findExoticVersionInfo(exoticResolver, pattern);
        }
        WorkspaceLayout workspaceLayout = resolver.workspaceLayout;
        if (WorkspaceResolver.isWorkspace(pattern, workspaceLayout)) {
            WorkspaceResolver workspaceResolver = new WorkspaceResolver(this, workspaceLayout);
            if (resolver.focus && !pattern.contains(String.valueOf(workspaceLayout.virtualManifestName)) &&
                !pattern.startsWith(Js.templateString(resolver.focusedWorkspaceName) + "@")) {
                WorkspaceLayout.Workspace localInfo = workspaceLayout.getManifestByPattern(pattern);
                if (localInfo == null) {
                    throw new JsError(Js.INVARIANT_VIOLATION, "expected local info for " + pattern);
                }
                Manifest localManifest = localInfo.getManifest();
                String requestPattern = Js.templateString(localManifest.getName()) + "@" +
                                        Js.templateString(localManifest.getVersion());
                return findVersionOnRegistry(requestPattern).then(manifest ->
                        Promise.resolved(resolver.loop, workspaceResolver.resolve(manifest)));
            }
            return Promise.resolved(resolver.loop, workspaceResolver.resolve(null));
        }
        return findVersionOnRegistry(pattern);
    }

    void resolveToExistingVersion(Manifest info) {
        NormalizedPattern normalized = NormalizedPattern.normalizePattern(pattern);
        String solvedRange = NodeSemver.validRange(normalized.getRange(), Config.SEMVER) != null ?
                Js.templateString(info.getVersion()) : normalized.getRange();
        Manifest resolved = resolver.getHighestRangeVersionMatch(normalized.getName(), solvedRange, info);
        if (resolved == null) {
            throw new JsError(Js.INVARIANT_VIOLATION, "should have a resolved reference");
        }
        PackageReference ref = resolved.reference;
        if (ref == null) {
            throw new JsError(Js.INVARIANT_VIOLATION, "Resolved package info has no package reference");
        }
        ref.addRequest(this);
        ref.addPattern(pattern, resolved);
        ref.addOptional(optional);
    }

    Promise<Void> find(boolean fresh, boolean frozen) {
        return Promise.async(resolver.loop, this::findVersionInfo).then(info -> {
            Object version = info.getVersion();
            if (!(version instanceof String) || NodeSemver.valid((String) version, Config.SEMVER) == null) {
                throw new MessageError(Lang.format("Can't add $0: invalid package version $1.", info.getName(),
                        info.getVersion()));
            }
            info.fresh = fresh;
            NormalizeManifest.cleanDependencies(info);

            NormalizedPattern normalized = NormalizedPattern.normalizePattern(pattern);
            String name = normalized.getName();
            String solvedRange = NodeSemver.validRange(normalized.getRange(), Config.SEMVER) != null ?
                    (String) info.getVersion() : normalized.getRange();
            Manifest resolved = !info.fresh || frozen ?
                    resolver.getExactVersionMatch(name, solvedRange, info) :
                    resolver.getHighestRangeVersionMatch(name, solvedRange, info);

            if (resolved != null) {
                resolver.joined(this, info, resolved);
                resolver.reportPackageWithExistingVersion(this, info);
                return Promise.resolved(resolver.loop, null);
            }

            if (Js.truthy(info.get("flat")) && !resolver.flat) {
                throw new MessageError(Lang.format("The package $0 requested to be flat, but the project is not " +
                                                   "flat. Please use the --flat flag.",
                        Js.templateString(info.getName()) + "@" + Js.templateString(info.getVersion())));
            }

            validateVersionInfo(info);

            PackageRemote remote = info.remote;
            if (remote == null) {
                throw new JsError(Js.INVARIANT_VIOLATION, "Missing remote");
            }

            PackageReference ref = new PackageReference(this, info, remote);
            ref.addPattern(pattern, info);
            ref.addOptional(optional);
            ref.setFresh(fresh);
            info.reference = ref;
            info.remote = remote;

            List<Promise<Void>> promises = new ArrayList<>();
            List<String> deps = new ArrayList<>();
            List<String> parentNames = new ArrayList<>(this.parentNames);
            parentNames.add(name);
            Object dependencies = info.get("dependencies");
            for (String depName : Js.keys(dependencies)) {
                String depPattern = depName + "@" + Js.templateString(Js.get(dependencies, depName));
                deps.add(depPattern);
                promises.add(resolver.find(DependencyRequestPattern.builder()
                        .pattern(depPattern)
                        .registry(Js.templateString(remote.registry))
                        .optional(optional)
                        .parentRequest(this)
                        .parentNames(parentNames)
                        .build()));
            }
            Object optionalDependencies = info.get("optionalDependencies");
            for (String depName : Js.keys(optionalDependencies)) {
                String depPattern = depName + "@" + Js.templateString(Js.get(optionalDependencies, depName));
                deps.add(depPattern);
                promises.add(resolver.find(DependencyRequestPattern.builder()
                        .hint("optional")
                        .pattern(depPattern)
                        .registry(Js.templateString(remote.registry))
                        .optional(true)
                        .parentRequest(this)
                        .parentNames(parentNames)
                        .build()));
            }
            if ("workspace".equals(remote.type) && !config.isProduction()) {
                Object devDependencies = info.get("devDependencies");
                for (String depName : Js.keys(devDependencies)) {
                    String depPattern = depName + "@" + Js.templateString(Js.get(devDependencies, depName));
                    deps.add(depPattern);
                    promises.add(resolver.find(DependencyRequestPattern.builder()
                            .hint("dev")
                            .pattern(depPattern)
                            .registry(Js.templateString(remote.registry))
                            .optional(false)
                            .parentRequest(this)
                            .parentNames(parentNames)
                            .build()));
                }
            }

            return Promise.awaitInOrder(resolver.loop, promises).then(v -> {
                ref.addDependencies(deps);
                for (PackageRequest otherRequest : ref.requests.subList(1, ref.requests.size())) {
                    ref.addOptional(otherRequest.optional);
                }
                return Promise.resolved(resolver.loop, null);
            });
        });
    }

    static void validateVersionInfo(Manifest info) {
        String human = Js.templateString(info.getName()) + "@" + Js.templateString(info.getVersion());
        info.put("version", getPackageVersion(info));
        for (String key : REQUIRED_PACKAGE_KEYS) {
            if (!Js.truthy(info.get(key))) {
                throw new MessageError(Lang.format("Package $0 doesn't have a $1.", human, key));
            }
        }
    }

    static @Nullable Object getPackageVersion(Manifest info) {
        return info.data.containsKey("version") && info.getVersion() != null ? info.getVersion() : info.getUid();
    }

    @Override
    public String toString() {
        return pattern;
    }
}
