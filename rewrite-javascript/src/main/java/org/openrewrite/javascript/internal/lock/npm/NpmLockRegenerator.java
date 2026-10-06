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
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.marker.NodeResolutionResult;

import java.nio.file.Path;
import java.nio.file.Paths;
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

        /**
         * The project's own .npmrc; the host's user and global npmrc are not an input.
         */
        @Nullable
        String npmrc;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The name of the directory the project is in, when the host knows it: a root manifest without a name
         * is named after it.
         */
        @Nullable
        String directoryName;

        /**
         * A recipe's request: the manifests by path, placed relative to the lock's directory, which is that of
         * the topmost manifest given, else the edited manifest's own. At the repository root the directory's
         * name is not known.
         */
        public static Inputs from(Request request, Host host) {
            Path packageJsonPath = request.getPackageJsonPath() == null ? Paths.get("package.json") :
                    request.getPackageJsonPath();
            String dir = directory(packageJsonPath);
            String rootDir = dir;
            for (Path path : request.getManifests().keySet()) {
                String candidate = directory(path);
                if (contains(candidate, dir) && candidate.length() < rootDir.length()) {
                    rootDir = candidate;
                }
            }
            InputsBuilder inputs = builder()
                    .lock(request.getLock())
                    .npmrc(projectNpmrc(request.getMarker()))
                    .host(host)
                    .directoryName(rootDir.isEmpty() ? null : PosixPath.basename(rootDir));
            for (Map.Entry<Path, String> manifest : request.getManifests().entrySet()) {
                inputs.manifest(location(rootDir, directory(manifest.getKey())), manifest.getValue());
            }
            inputs.manifest(location(rootDir, dir), request.getPackageJson());
            return inputs.build();
        }

        private static String directory(Path manifest) {
            Path dir = manifest.normalize().getParent();
            return dir == null ? "" : dir.toString().replace('\\', '/');
        }

        private static boolean contains(String dir, String other) {
            return dir.isEmpty() || other.equals(dir) || other.startsWith(dir + "/");
        }

        private static String location(String rootDir, String dir) {
            String location = Paths.get(rootDir).relativize(Paths.get(dir)).toString().replace('\\', '/');
            return ".".equals(location) ? "" : location;
        }

        private static @Nullable String projectNpmrc(@Nullable NodeResolutionResult marker) {
            if (marker == null || marker.getNpmrcConfigs() == null) {
                return null;
            }
            for (NodeResolutionResult.Npmrc npmrc : marker.getNpmrcConfigs()) {
                if (npmrc.getScope() == NodeResolutionResult.NpmrcScope.Project) {
                    return Ini.encode(npmrc.getProperties());
                }
            }
            return null;
        }
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
        NpmOptions options;
        Map<String, String> manifests;
        String path;
        String written;
        try {
            options = NpmOptions.fromNpmrc(inputs.getNpmrc());
            LockFile lock = truthy(inputs.getLock()) ? new LockFile(inputs.getLock()) : null;
            manifests = manifests(lock);
            path = projectPath(lock, manifests.get(""));
            IdealTreeBuilder arb = arborist(path, lock, manifests, options);
            arb.buildIdealTree();
            if (!options.isSave()) {
                return Result.success(Objects.requireNonNull(inputs.getLock()));
            }
            written = saveIdealTree(arb, options);
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            return Result.failure(failure(e, NPM_REJECTS, NPM_FAILS));
        }
        try {
            IdealTreeBuilder again = arborist(path, new LockFile(written), manifests, options);
            again.buildIdealTree();
            String rewritten = saveIdealTree(again, options);
            if (!rewritten.equals(written)) {
                return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null,
                        SECOND_INSTALL + " would rewrite it: " + firstDifference(written, rewritten)));
            }
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            return Result.failure(failure(e, SECOND_INSTALL + " would reject it", SECOND_INSTALL + " would fail"));
        }
        return Result.success(written);
    }

    private IdealTreeBuilder arborist(String path, @Nullable LockFile lock, Map<String, String> manifests,
                                      NpmOptions options) {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
                .lock(lock)
                .yarnLock(inputs.getYarnLock())
                .manifests(manifests)
                .build();
        return new IdealTreeBuilder(path, files, options, registry, inputs.getHost());
    }

    /**
     * The manifests given, and for the root and the link targets outside node_modules the host did not
     * supply, the lock's own entries, as arborist reads a project without a package.json from its lock.
     */
    private Map<String, String> manifests(@Nullable LockFile lock) {
        Map<String, String> manifests = new LinkedHashMap<>();
        Object data = lockData(lock);
        Object packages = data instanceof Map ? Js.get(data, "packages") : null;
        if (packages instanceof Map) {
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(packages)) {
                Object meta = entry.getValue();
                if (!(meta instanceof Map)) {
                    continue;
                }
                Object resolved = Js.get(meta, "resolved");
                if (entry.getKey().isEmpty()) {
                    manifests.put("", JsonStringifyNice.stringify(meta));
                } else if (Js.strictEquals(Js.get(meta, "link"), true) && resolved instanceof String &&
                           !((String) resolved).contains("node_modules/")) {
                    Object target = Js.get(packages, (String) resolved);
                    if (target instanceof Map) {
                        manifests.put((String) resolved, JsonStringifyNice.stringify(target));
                    }
                }
            }
        }
        manifests.putAll(inputs.getManifests());
        return manifests;
    }

    // a lock that does not parse is the builder's to refuse
    private static @Nullable Object lockData(@Nullable LockFile lock) {
        if (lock == null) {
            return null;
        }
        try {
            return lock.parsed().getValue();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // reify.js #saveIdealTree: the lock takes the indent of the root package.json, two spaces when it has none
    private static String saveIdealTree(IdealTreeBuilder arb, NpmOptions options) {
        PackageJson rootManifest = arb.getRootManifest();
        Object format = rootManifest == null || rootManifest.getIndent() == null ? "  " : rootManifest.getIndent();
        boolean formatPackageLock = options.isFormatPackageLock();
        return Objects.requireNonNull(arb.buildIdealTree().getMeta())
                .toString(formatPackageLock && truthy(format) ? format : formatPackageLock);
    }

    /**
     * The root takes the directory's name when its manifest has none (node.js: nameFromFolder). Without the
     * host's word, the lock's own top-level name stands in: npm wrote it from that directory.
     */
    private String projectPath(@Nullable LockFile lock, @Nullable String rootText) {
        String directoryName = inputs.getDirectoryName();
        if (directoryName == null) {
            Object data = lockData(lock);
            Object lockName = data instanceof Map ? Js.get(data, "name") : null;
            if (lockName instanceof String && !((String) lockName).isEmpty()) {
                directoryName = (String) lockName;
            } else if (data instanceof Map && !truthy(rootName(rootText))) {
                throw new CannotKnow("the project directory name (package.json has no name; npm names the lock " +
                                     "after the directory it runs in)");
            } else {
                // a named root never reads it; a lock that is missing or malformed is refused before any name is written
                directoryName = "project";
            }
        }
        return "/" + directoryName;
    }

    // a manifest that does not parse is the builder's to reject
    private static Object rootName(@Nullable String rootText) {
        if (rootText == null) {
            return Js.UNDEFINED;
        }
        try {
            Object value = JsonParseEvenBetterErrors.parse(rootText).getValue();
            return value instanceof Map ? Js.get(value, "name") : Js.UNDEFINED;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static Failure failure(RuntimeException e, String rejects, String fails) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (e instanceof PlaceDep.CouldNotResolve) {
            PlaceDep.PeerConflict conflict = ((PlaceDep.CouldNotResolve) e).getConflict();
            return new Failure(Reason.PEER_CONFLICT, conflict.getEdge().getName(),
                    rejects + " (ERESOLVE):\n" + ExplainEresolve.explain(conflict.getExplanation(), 4));
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
            return new Failure(Reason.MALFORMED_MANIFEST, null, rejects + " (EJSONPARSE): " + message);
        }
        String code = e instanceof NpmError ? ((NpmError) e).getCode() : null;
        if (code == null) {
            return new Failure(Reason.RESOLUTION_REQUIRED, packageOf(e), fails + ": " + message);
        }
        return new Failure(reasonFor(code), packageOf(e), rejects + " (" + code + "): " + message);
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
