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
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/arborist/index.js and reify.js (npm 11.17.0) for {@code npm install
 * --package-lock-only}, after the {@code devEngines} check of npm's lib/base-cmd.js; the lock written must come back
 * unchanged from a second install, or that install is reported.
 */
public final class NpmLockRegenerator {

    /**
     * What the engine is given for an npm project.
     */
    @Value
    @Builder
    public static class Inputs {
        /**
         * The npm release that installs the project: the one that built its LST ({@link NpmReleases#installedAs}).
         */
        @Builder.Default
        NpmRelease release = NpmRelease.PORTED;

        /**
         * The package-lock.json; null when the project has none.
         */
        @Nullable
        String lock;

        @Nullable
        String yarnLock;

        /**
         * The package.json files the host read, as edited, by location from the lock's directory: {@code ""} the
         * root, {@code packages/a} a workspace member, {@code ../lib} a link target above it. A location where the
         * host holds no package.json keeps its lock entry.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The project's own .npmrc.
         */
        @Nullable
        String npmrc;

        /**
         * The user's .npmrc, the host's registries, which npm reads under the project's .npmrc; null for none.
         */
        @Nullable
        String userNpmrc;

        /**
         * The files the host holds, by path from the lock's directory ({@code ../lib/package.json} above it), when it
         * says; an .npmrc or a package.json among them that it did not read is unknown.
         */
        @Nullable
        Set<String> paths;

        /**
         * The lock's directory from the root of what the host holds ({@code apps/web}, {@code ""} at the root), when
         * it says: a link target above that root is one the engine is not given.
         */
        @Nullable
        String directory;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The clock {@code min-release-age} counts from.
         */
        @NonNull
        Instant now;

        /**
         * The name of the directory the project is in, when the host knows it: a root manifest without a name
         * is named after it.
         */
        @Nullable
        String directoryName;

        /**
         * The directory npm runs in, the edited manifest's, relative to the lock's.
         */
        @Builder.Default
        String runsIn = "";

        /**
         * The package.json files under each directory above the one npm runs in that holds one, nearest first, keyed
         * by where npm runs as seen from there ({@code packages/a}); each by location, its own at "", null unread.
         */
        @Singular
        Map<String, Map<String, @Nullable String>> parentTrees;
    }

    /**
     * {@code EBADDEVENGINES}: the {@code devEngines} of the package npm installs exclude the environment it runs in.
     */
    @Getter
    static final class BadDevEngines extends IllegalStateException implements NpmError {
        private final String engine;
        private final transient Object current;
        private final transient @Nullable Object required;

        BadDevEngines(String message, NpmInstallChecks.InvalidDevEngine failure) {
            super(message);
            this.engine = failure.getEngine();
            this.current = failure.getCurrent();
            this.required = failure.getRequired();
        }

        @Override
        public String getCode() {
            return "EBADDEVENGINES";
        }
    }

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.NPM;

    // the repository's own directory, whose name the host does not know: one that names no package
    private static final String REPOSITORY = ". repository";

    private final Inputs inputs;
    private final PackumentSource registry;

    public NpmLockRegenerator(Inputs inputs, PackumentSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    public Result regenerate() {
        NpmOptions options;
        String path;
        String written;
        String yarnLock;
        Map<String, String> manifestsAgain;
        try {
            INSTALL.requireInstallsAt(NpmOptions.loadLocalPrefix(inputs.getRelease(),
                    inputs.getManifests().get(inputs.getRunsIn()), inputs.getParentTrees()), inputs.getRunsIn(),
                    "lock");
            if (inputs.getPaths() != null &&
                Shrinkwrap.readsShrinkwrap(inputs.getRelease(), inputs.getPaths()::contains)) {
                throw new CannotKnow("the lock npm writes: it reads and writes the npm-shrinkwrap.json beside this " +
                                     "lock in its place");
            }
            CannotKnow.requireRead(inputs.getNpmrc(), inputs.getPaths(), ".npmrc");
            CannotKnow.requireRead(inputs.getYarnLock(), inputs.getPaths(), "yarn.lock");
            options = NpmOptions.fromNpmrc(inputs.getRelease(), inputs.getUserNpmrc(), inputs.getNpmrc());
            if (!options.isGlobal()) {
                checkDevEngines(inputs.getManifests().get(""), options, inputs.getHost());
            }
            if (options.isGlobal()) {
                throw new NpmRejection("ESHRINKWRAPGLOBAL", "cannot generate lockfile for global packages", null);
            }
            VirtualTreeLoader.ProjectFiles files = projectFiles(truthy(inputs.getLock()) ?
                new LockFile(inputs.getLock()) : null, inputs.getManifests());
            path = projectPath(files);
            IdealTreeBuilder arb = arborist(path, files, options);
            arb.buildIdealTree();
            if (!options.isSave() || options.isDryRun()) {
                return Result.success(Objects.requireNonNull(inputs.getLock()));
            }
            Object rootName = Objects.requireNonNull(Objects.requireNonNull(arb.getIdealTree()).getTarget())
                    .getPackageName();
            if (inputs.getDirectoryName() == null && !inputs.getRelease().writesRootPackageName() &&
                rootName instanceof String && !((String) rootName).isEmpty()) {
                // the root entry names the package only where the directory npm runs in is named otherwise, and the
                // host knows no name of the repository's root
                throw new CannotKnow("the project directory name (npm before 7.20.6 writes the package's name in " +
                                     "the lock's root entry only when the directory it runs in has another)");
            }
            written = saveIdealTree(arb, options);
            yarnLock = Objects.requireNonNull(arb.buildIdealTree().getMeta()).yarnLockText();
            manifestsAgain = manifestsAfterSave(arb);
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        Result verified = INSTALL.verify(written, lock -> {
            IdealTreeBuilder again = arborist(path, projectFiles(new LockFile(lock), manifestsAgain), options);
            again.buildIdealTree();
            return saveIdealTree(again, options);
        }, NpmLockRegenerator::failure);
        return yarnLock == null || !verified.isSuccess() ? verified :
                Result.success(written, Collections.singletonMap("yarn.lock", yarnLock));
    }

    /**
     * Where npm {@code release} run in {@code runsIn} installs and writes its lock, by {@link
     * Inputs#getParentTrees()}; a walk that fails, which {@link #regenerate()} reports as it walks first, stops here.
     */
    public static InstallRoot installRoot(NpmRelease release, String runsIn, Map<String, String> manifests,
                                          Map<String, Map<String, @Nullable String>> parentTrees) {
        try {
            return InstallRoot.at(NpmOptions.loadLocalPrefix(release, manifests.get(runsIn), parentTrees));
        } catch (RuntimeException e) {
            return InstallRoot.STOPS_HERE;
        }
    }

    /**
     * base-cmd.js {@code checkDevEngines}, which npm.js runs before a command that is not global: the
     * {@code devEngines} of the package.json where npm installs against the environment npm runs in. Of the engines it
     * does not satisfy, the first whose mismatch is an error is npm's {@code EBADDEVENGINES}; npm logs those that warn
     * and, under {@code force}, the errors too.
     *
     * @param manifest that package.json; null without one
     * @return what npm logs, each warning as it words it
     */
    static List<String> checkDevEngines(@Nullable String manifest, NpmOptions options, Host host) {
        boolean force = options.isForce();

        Object devEngines = Js.get(localPackage(manifest, options.getRelease()), "devEngines");

        // a release without the check never reads the field
        if (devEngines == Js.UNDEFINED || !options.getRelease().checksDevEngines()) {
            return Collections.emptyList();
        }

        Map<String, Object> current = NpmInstallChecks.devEngines(host, options.getNpmVersion());

        List<NpmInstallChecks.InvalidDevEngine> warnings = new ArrayList<>();
        List<NpmInstallChecks.InvalidDevEngine> errors = new ArrayList<>();
        for (NpmInstallChecks.InvalidDevEngine failure : NpmInstallChecks.checkDevEngines(devEngines, current,
                options.getRelease())) {
            if (failure.isWarn()) {
                warnings.add(failure);
            }
            if (failure.isError()) {
                errors.add(failure);
            }
        }

        List<String> logged = new ArrayList<>();
        for (NpmInstallChecks.InvalidDevEngine failure : warnings) {
            logged.add(genMsg(failure, logged.size()));
        }
        if (force) {
            for (NpmInstallChecks.InvalidDevEngine failure : errors) {
                logged.add(genMsg(failure, logged.size()));
            }
            return logged;
        }

        if (!errors.isEmpty()) {
            NpmInstallChecks.InvalidDevEngine failure = errors.get(0);
            throw new BadDevEngines(genMsg(failure, 0), failure);
        }
        return logged;
    }

    private static String genMsg(NpmInstallChecks.InvalidDevEngine failure, int i) {
        Set<String> lines = new LinkedHashSet<>();
        lines.add(i == 0 ? "The developer of this package has specified the following through devEngines" : "");
        lines.add(failure.getMessage());
        lines.add(String.join("\n", failure.getErrors()));
        lines.remove("");
        return String.join("\n", lines);
    }

    /**
     * {@code PackageJson.normalize(localPrefix).then(p => p.content).catch(() => ({}))}: the package.json where npm
     * {@code release} installs as its own commands read it, an empty one where there is none or it does not normalize.
     */
    static Map<String, Object> localPackage(@Nullable String manifest, NpmRelease release) {
        if (manifest != null) {
            try {
                return PackageJson.normalize(manifest, release).getContent();
            } catch (PackageJson.NeedsPackageFiles e) {
                // normalize fills bin from the package's files; every other field is as the manifest has it
                return e.getContent();
            } catch (RuntimeException e) {
                return new LinkedHashMap<>();
            }
        }
        return new LinkedHashMap<>();
    }

    /**
     * index.js {@code #setWorkspaces}: the members the node's {@code workspaces} patterns name among the
     * project's directories become its workspace edges.
     */
    static Node setWorkspaces(Node node, VirtualTreeLoader.ProjectFiles files) {
        Map<String, String> workspaces = new LinkedHashMap<>();
        for (Map.Entry<Object, String> workspace : MapWorkspaces.mapWorkspaces(node.getPath(), node.getPackage(),
                files, node.getRelease()).entrySet()) {
            // the workspace edge the name becomes is what refuses it
            if (!(workspace.getKey() instanceof String)) {
                throw new IllegalArgumentException("must provide dependency name");
            }
            workspaces.put((String) workspace.getKey(), workspace.getValue());
        }
        if (!workspaces.isEmpty()) {
            node.setWorkspaces(workspaces);
        }
        return node;
    }

    private VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock, Map<String, String> manifests) {
        return projectFiles(lock, inputs.getYarnLock(), manifests, inputs.getPaths(), inputs.getDirectory())
                .readBy(inputs.getRelease(), inputs.getPaths());
    }

    /**
     * The files of a project whose host has not said what it holds.
     */
    static VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock, @Nullable String yarnLock,
                                                       Map<String, String> manifests) {
        return projectFiles(lock, yarnLock, manifests, null, null);
    }

    /**
     * The project's files. A package.json among {@code paths} that the host did not read is unread. Where the host
     * holds none, or has not said what it holds, the lock's own entry stands in for the root and for a link target
     * outside node_modules, as arborist reads a project without that package.json. The package.json above
     * {@code directory}'s root is one the engine is not given, and the one at that root is read only where the lock
     * shows how npm named it.
     */
    @SuppressWarnings("unchecked")
    static VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock, @Nullable String yarnLock,
                                                       Map<String, String> manifests, @Nullable Set<String> paths,
                                                       @Nullable String directory) {
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
                .lock(lock)
                .yarnLock(yarnLock)
                .manifests(manifests)
                .depth(directory == null ? null : directory.isEmpty() ? 0 : directory.split("/").length);
        Set<String> unread = new LinkedHashSet<>();
        if (paths != null) {
            for (String path : paths) {
                String location = "package.json".equals(path) ? "" : path.endsWith("/package.json") ?
                        path.substring(0, path.length() - "/package.json".length()) : null;
                if (location != null && !manifests.containsKey(location) && unread.add(location)) {
                    files.unread(location);
                }
            }
        }
        String top = directory == null || directory.isEmpty() ? null : directory.replaceAll("[^/]+", "..");
        boolean topNamedAsLocked = false;
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
                    if (!manifests.containsKey("") && !unread.contains("")) {
                        files.parsedManifest("", (Map<String, Object>) meta);
                    }
                } else if (Js.strictEquals(Js.get(meta, "link"), true) && resolved instanceof String &&
                           !((String) resolved).contains("node_modules/")) {
                    String location = RootRelativePath.normalize((String) resolved);
                    Object target = Js.get(packages, (String) resolved);
                    if (location.equals(top) && target instanceof Map) {
                        topNamedAsLocked = namedAsLocked((Map<String, Object>) target, manifests.get(location));
                    }
                    if (target instanceof Map && !manifests.containsKey(resolved) &&
                        !manifests.containsKey(location) && !unread.contains(location)) {
                        files.parsedManifest((String) resolved, (Map<String, Object>) target);
                    }
                }
            }
        }
        if (top != null && manifests.containsKey(top) && !topNamedAsLocked) {
            files.unknown(top, new CannotKnow("the name of the repository's directory, which npm compares with " +
                                              "the name of the package there (" + top + ") to write its lock entry"));
        }
        return files.build();
    }

    /**
     * Whether the lock shows how npm writes the name of the package in the repository's own directory, whose name
     * the host does not know: npm writes a package's name only where its directory is named otherwise, so an entry
     * that holds the name the package.json still declares was written from a directory named otherwise.
     */
    private static boolean namedAsLocked(Map<String, Object> entry, @Nullable String manifest) {
        if (manifest == null) {
            return true;
        }
        Object declared;
        try {
            declared = PackageJson.normalize(manifest, NpmRelease.PORTED).getContent().get("name");
        } catch (RuntimeException e) {
            // a package.json npm does not read is refused where it is read
            return true;
        }
        return declared instanceof String && declared.equals(entry.get("name"));
    }

    /**
     * The project's manifests as the install leaves them for the next one: before npm 7.14.0 every install writes the
     * root package.json back (update-root-package-json.js). Where the host holds no root package.json, the lock's
     * root entry stands in for it on both installs alike.
     */
    private Map<String, String> manifestsAfterSave(IdealTreeBuilder arb) {
        Map<String, String> manifests = inputs.getManifests();
        String rootJson = manifests.get("");
        PackageJson rootManifest = arb.getRootManifest();
        if (!inputs.getRelease().rewritesRootPackageJson() || rootJson == null || rootManifest == null) {
            return manifests;
        }
        Map<String, Object> pkg = Objects.requireNonNull(arb.getIdealTree()).getPackage();
        Map<String, String> after = new LinkedHashMap<>(manifests);
        after.put("", UpdateRootPackageJson.updateRootPackageJson(pkg, rootJson, rootManifest.getIndent(),
                rootManifest.getNewline(), inputs.getRelease()));
        return after;
    }

    private IdealTreeBuilder arborist(String path, VirtualTreeLoader.ProjectFiles files, NpmOptions options) {
        return new IdealTreeBuilder(path, files, options, registry, inputs.getHost(), inputs.getNow());
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
    private String projectPath(VirtualTreeLoader.ProjectFiles files) {
        String directory = inputs.getDirectory();
        if (directory != null && !directory.isEmpty()) {
            // under the directories above it, which a link target that climbs out of the project is read through
            return "/" + REPOSITORY + "/" + directory;
        }
        String directoryName = inputs.getDirectoryName();
        if (directoryName == null) {
            Object data = lockData(files.getLock());
            Object lockName = data instanceof Map ? Js.get(data, "name") : null;
            if (lockName instanceof String && !((String) lockName).isEmpty()) {
                directoryName = (String) lockName;
            } else if (data instanceof Map && !truthy(rootName(files))) {
                throw new CannotKnow("the project directory name (package.json has no name; npm names the lock " +
                                     "after the directory it runs in)");
            } else {
                // a named root never reads it; a lock that is missing or malformed is refused before any name is
                // written
                directoryName = "project";
            }
        }
        return "/" + directoryName;
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

    // a manifest that does not parse is the builder's to reject
    private static Object rootName(VirtualTreeLoader.ProjectFiles files) {
        try {
            PackageJson manifest = files.manifest("");
            return manifest == null ? Js.UNDEFINED : Js.get(manifest.getContent(), "name");
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    private static RuntimeException outcome(RuntimeException e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (e instanceof PlaceDep.CouldNotResolve) {
            PlaceDep.PeerConflict conflict = ((PlaceDep.CouldNotResolve) e).getConflict();
            NpmRelease release = conflict.getEdge().getRelease();
            String report;
            try {
                report = ExplainEresolve.explain(conflict.getExplanation(), 4, release);
            } catch (JsError er) {
                // before npm 7.0.11 the report can fail on the root project's name, after npm logged the code
                if (release.namesRootProjectAmongMoreDependents()) {
                    throw er;
                }
                report = String.valueOf(er.getMessage());
            }
            return Rejected.coded(Reason.PEER_CONFLICT, conflict.getEdge().getName(), "ERESOLVE", report);
        }
        if (e instanceof CannotKnow || e instanceof Rejected) {
            return e;
        }
        if (e instanceof PackageJson.NeedsPackageFiles) {
            return new CannotKnow("the package's files: " + message);
        }
        if (e instanceof LocaleCompare.UnknownOrder) {
            return new CannotKnow("the order npm writes the keys in: " + message);
        }
        if (e instanceof MalformedLock) {
            return Rejected.discardsLock(message);
        }
        if (e instanceof MalformedManifest) {
            return Rejected.crashes(Reason.MALFORMED_MANIFEST, null, "it cannot apply the manifest: " + message);
        }
        if (e instanceof JsonParseEvenBetterErrors.JsonParseException) {
            return Rejected.coded(Reason.MALFORMED_MANIFEST, null, "EJSONPARSE", message);
        }
        String code = e instanceof NpmError ? ((NpmError) e).getCode() :
                e instanceof Spec.InvalidSpecException ? ((Spec.InvalidSpecException) e).getCode() : null;
        if (code == null) {
            // JavaScript's errors, and its TypeError as the port and NodeSemver raise it; any other is the port's fault
            if (!(e instanceof JsError) && e.getClass() != IllegalArgumentException.class) {
                throw e;
            }
            return Rejected.crashes(Reason.RESOLUTION_REQUIRED, packageOf(e), message);
        }
        return Rejected.coded(reasonFor(code), packageOf(e), code, message);
    }

    private static Reason reasonFor(String code) {
        // npm 12's verdicts on the root's packageExtensions and patchedDependencies
        if (code.startsWith("EEXTENSION") || code.startsWith("EPATCH")) {
            return Reason.MALFORMED_MANIFEST;
        }
        switch (code) {
            case "E404":
                return SharedError.PACKAGE_NOT_FOUND.getReason();
            case "ETARGET":
                return SharedError.NO_MATCHING_VERSION.getReason();
            case "EBADPLATFORM":
                return SharedError.UNSUPPORTED_PLATFORM.getReason();
            case "EBADENGINE":
                return SharedError.UNSUPPORTED_ENGINE.getReason();
            case "ENOVERSIONS":
            case "E403":
                return Reason.VERSION_NOT_FOUND;
            case "ELOCKFILEVERSION":
                return Reason.UNSUPPORTED_LOCKFILE_VERSION;
            case "EUNSUPPORTEDPROTOCOL":
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
}
