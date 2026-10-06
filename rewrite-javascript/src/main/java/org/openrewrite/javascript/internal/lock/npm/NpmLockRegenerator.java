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
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/index.js and reify.js (npm 11.17.0) for {@code npm install
 * --package-lock-only}: the ideal tree built over the lock, written back as the lock. The lock written is then
 * loaded and built again, as the next install would, and must come back unchanged: a second install that
 * rejects or rewrites it is reported instead.
 */
public final class NpmLockRegenerator {

    /**
     * What the engine is given for an npm project.
     */
    @Value
    @Builder
    public static class Inputs {
        /**
         * The package-lock.json; null when the project has none.
         */
        @Nullable
        String lock;

        @Nullable
        String yarnLock;

        /**
         * The project's manifests as edited, by location: {@code ""} the root, {@code packages/a} a workspace
         * member. A member the host did not supply keeps its lock entry.
         */
        @Singular
        Map<String, String> manifests;

        @Builder.Default
        NpmOptions options = NpmOptions.DEFAULT;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The name of the directory the project is in, when the host knows it: a root manifest without a name
         * is named after it.
         */
        @Nullable
        String directoryName;
    }

    static final String NPM_REJECTS = "npm rejects this edit";
    static final String NPM_FAILS = "npm fails on this edit";
    static final String CANNOT_KNOW = "the engine cannot know";
    static final String SECOND_INSTALL = "a second npm install over the lock written for this edit";

    private final Inputs inputs;
    private final PackumentSource registry;

    public NpmLockRegenerator(Inputs inputs, PackumentSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    public Result regenerate() {
        String path;
        String written;
        try {
            path = projectPath();
            Node tree = build(path, inputs.getLock());
            if (!inputs.getOptions().isSave()) {
                return Result.success(Objects.requireNonNull(inputs.getLock()));
            }
            written = saveIdealTree(tree);
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            return Result.failure(failure(e, false));
        }
        try {
            String again = saveIdealTree(build(path, written));
            if (!again.equals(written)) {
                return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null,
                        SECOND_INSTALL + " would rewrite it: " + firstDifference(written, again)));
            }
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            return Result.failure(failure(e, true));
        }
        return Result.success(written);
    }

    private Node build(String path, @Nullable String lock) {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
                .lock(lock)
                .yarnLock(inputs.getYarnLock())
                .manifests(manifests())
                .build();
        return new IdealTreeBuilder(path, files, inputs.getOptions(), registry, inputs.getHost()).buildIdealTree();
    }

    /**
     * The manifests given, and for the root and the link targets outside node_modules the host did not
     * supply, the lock's own entries, as arborist reads a project without a package.json from its lock.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> manifests() {
        Map<String, String> manifests = new LinkedHashMap<>();
        Map<String, Object> packages = lockPackages();
        if (packages != null) {
            for (Map.Entry<String, Object> entry : packages.entrySet()) {
                Object meta = entry.getValue();
                if (!(meta instanceof Map)) {
                    continue;
                }
                Object resolved = ((Map<String, Object>) meta).get("resolved");
                if (entry.getKey().isEmpty()) {
                    manifests.put("", JsonStringifyNice.stringify(meta));
                } else if (Boolean.TRUE.equals(((Map<String, Object>) meta).get("link")) && resolved instanceof String &&
                           !((String) resolved).contains("node_modules/") && packages.get(resolved) instanceof Map) {
                    manifests.put((String) resolved, JsonStringifyNice.stringify(packages.get(resolved)));
                }
            }
        }
        manifests.putAll(inputs.getManifests());
        return manifests;
    }

    @SuppressWarnings("unchecked")
    private @Nullable Map<String, Object> lockPackages() {
        Map<String, Object> lock = lockData();
        Object packages = lock == null ? null : lock.get("packages");
        return packages instanceof Map ? (Map<String, Object>) packages : null;
    }

    @SuppressWarnings("unchecked")
    private @Nullable Map<String, Object> lockData() {
        if (inputs.getLock() == null) {
            return null;
        }
        try {
            Object value = JsonParseEvenBetterErrors.parse(inputs.getLock()).getValue();
            return value instanceof Map ? (Map<String, Object>) value : null;
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return null;
        }
    }

    private String saveIdealTree(Node tree) {
        String rootText = manifests().get("");
        Object format = rootText == null ? null : PackageJson.normalize(rootText).getIndent();
        if (format == null) {
            format = "  ";
        }
        boolean formatPackageLock = inputs.getOptions().isFormatPackageLock();
        return Objects.requireNonNull(tree.getMeta()).toString(formatPackageLock && truthy(format) ? format : formatPackageLock);
    }

    /**
     * The root takes the directory's name when its manifest has none (node.js: nameFromFolder). Without the
     * host's word, the lock's own top-level name stands in: npm wrote it from that directory.
     */
    private String projectPath() {
        String directoryName = inputs.getDirectoryName();
        if (directoryName == null && inputs.getLock() == null) {
            // no lock: the builder refuses the project before any name is written
            directoryName = "project";
        }
        if (directoryName == null) {
            if (truthy(rootName())) {
                directoryName = "project";
            } else {
                Map<String, Object> lock = lockData();
                Object lockName = lock == null ? null : lock.get("name");
                if (!(lockName instanceof String) || ((String) lockName).isEmpty()) {
                    throw new CannotKnow("the project directory name (package.json has no name; npm names the lock " +
                                         "after the directory it runs in)");
                }
                directoryName = (String) lockName;
            }
        }
        return "/" + directoryName;
    }

    // a manifest that does not parse is the builder's to reject
    @SuppressWarnings("unchecked")
    private Object rootName() {
        String rootText = manifests().get("");
        if (rootText == null) {
            return Js.UNDEFINED;
        }
        try {
            Object value = JsonParseEvenBetterErrors.parse(rootText).getValue();
            return value instanceof Map ? ((Map<String, Object>) value).get("name") : Js.UNDEFINED;
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return true;
        }
    }

    private static Failure failure(RuntimeException e, boolean secondInstall) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (e instanceof PlaceDep.CouldNotResolve) {
            PlaceDep.PeerConflict conflict = ((PlaceDep.CouldNotResolve) e).getConflict();
            return new Failure(Reason.PEER_CONFLICT, conflict.getEdge().getName(),
                    (secondInstall ? SECOND_INSTALL + " would reject it" : NPM_REJECTS) + " (ERESOLVE):\n" +
                    ExplainEresolve.explain(conflict.getExplanation(), 4));
        }
        if (e instanceof CannotKnow) {
            return new Failure(Reason.RESOLUTION_REQUIRED, null, CANNOT_KNOW + " " + ((CannotKnow) e).getSubject());
        }
        if (e instanceof NpmOptions.NeedsEnvironment) {
            return new Failure(Reason.RESOLUTION_REQUIRED, null, CANNOT_KNOW + " the environment: " + message);
        }
        if (e instanceof PackageJson.NeedsPackageFiles) {
            return new Failure(Reason.RESOLUTION_REQUIRED, null, CANNOT_KNOW + " the package's files: " + message);
        }
        if (e instanceof MalformedLock) {
            return new Failure(Reason.MALFORMED_LOCK, null,
                    "npm would discard the lock and resolve everything from scratch: " + message);
        }
        if (e instanceof MalformedManifest) {
            return new Failure(Reason.MALFORMED_MANIFEST, null, "npm cannot apply the manifest: " + message);
        }
        if (e instanceof JsonParseEvenBetterErrors.JsonParseException) {
            return new Failure(Reason.MALFORMED_MANIFEST, null, NPM_REJECTS + " (EJSONPARSE): " + message);
        }
        String code = e instanceof NpmError ? ((NpmError) e).getCode() : null;
        String prefix = secondInstall ? SECOND_INSTALL + " would fail" : code != null ? NPM_REJECTS : NPM_FAILS;
        if (code == null) {
            return new Failure(Reason.RESOLUTION_REQUIRED, packageOf(e), prefix + ": " + message);
        }
        return new Failure(reasonFor(code), packageOf(e), prefix + " (" + code + "): " + message);
    }

    private static Reason reasonFor(String code) {
        switch (code) {
            case "E404":
                return Reason.PACKAGE_NOT_FOUND;
            case "ETARGET":
            case "ENOVERSIONS":
            case "E403":
                return Reason.VERSION_NOT_FOUND;
            case "EUNSUPPORTEDPROTOCOL":
            case "EBADPLATFORM":
            case "EBADENGINE":
            case "EALLOWGIT":
            case "EALLOWFILE":
            case "EALLOWDIRECTORY":
            case "EALLOWREMOTE":
                return Reason.UNSUPPORTED_ENTRY_TYPE;
            case "EOVERRIDE":
            case "EJSONPARSE":
            case "EDUPLICATEWORKSPACE":
            case "EINVALIDTAGNAME":
            case "EINCOMPLETEMANIFEST":
                return Reason.MALFORMED_MANIFEST;
            default:
                return Reason.RESOLUTION_REQUIRED;
        }
    }

    private static @Nullable String packageOf(RuntimeException e) {
        if (e instanceof NpmRejection) {
            return ((NpmRejection) e).getPkg();
        }
        if (e instanceof PickManifest.PickFailure) {
            Object name = ((PickManifest.PickFailure) e).getName();
            return name == null ? null : Js.string(name);
        }
        return null;
    }

    private static String firstDifference(String a, String b) {
        String[] as = a.split("\n", -1);
        String[] bs = b.split("\n", -1);
        for (int i = 0; i < Math.max(as.length, bs.length); i++) {
            String x = i < as.length ? as[i] : "";
            String y = i < bs.length ? bs[i] : "";
            if (!x.equals(y)) {
                return "line " + (i + 1) + " \"" + x.trim() + "\" becomes \"" + y.trim() + "\"";
            }
        }
        return "the files differ";
    }
}
