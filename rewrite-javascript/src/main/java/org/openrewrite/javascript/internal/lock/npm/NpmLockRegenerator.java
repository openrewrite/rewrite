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
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
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
 * <p>
 * A project is installed as the npm release it names. One that names none is installed as every release the engine
 * reproduces that its package.json allows, once for the releases that answer alike whatever an install asks of its
 * release: the result is the lock they all write, or that the release which installs the project cannot be known.
 */
public final class NpmLockRegenerator {

    /**
     * What the engine is given for an npm project.
     */
    @Value
    @Builder
    public static class Inputs {
        /**
         * The npm release that installs the project, when the caller knows it; null to install it as each release its
         * package.json allows ({@link NpmReleases#candidates}), which must agree.
         */
        @Nullable
        @With
        NpmRelease release;

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
         * The files the project holds relative to the lock's directory, when the host says; an .npmrc among them the
         * host did not read is unknown.
         */
        @Nullable
        Set<String> paths;

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

    /**
     * One install of the project as a release, and the candidates it stands for: each of them answers what the install
     * asked of its release as that release did ({@link NpmRelease#answersAs}), and satisfies alike every range the
     * install could have held npm's version to, so it installs the project the same way.
     */
    @Getter
    static final class Install {
        /**
         * The release installed as, {@linkplain NpmRelease#followed() followed}.
         */
        private final NpmRelease release;

        private final Result result;

        /**
         * Whether npm rejects the project because of the release itself: {@code EBADDEVENGINES} for the package
         * manager, or {@code EBADENGINE} on the root package for its range of npm.
         */
        private final boolean refusing;

        /**
         * npm's code for the error that ended the install; null without one.
         */
        private final @Nullable String code;

        /**
         * The ranges the install could have held npm's version to: every {@code engines.npm} of the project's
         * manifests, of the lock it started from and of the registry documents it read. More than it compared, which
         * can only cost another install.
         */
        private final Set<String> npmRanges;

        /**
         * The {@code devEngines} of the package.json where npm installs, whose check reads npm's version too.
         */
        private final @Nullable Object devEngines;

        private final List<NpmRelease> candidates = new ArrayList<>();

        Install(NpmRelease release, Result result, boolean refusing, @Nullable String code, Set<String> npmRanges,
                @Nullable Object devEngines) {
            this.release = release;
            this.result = result;
            this.refusing = refusing;
            this.code = code;
            this.npmRanges = npmRanges;
            this.devEngines = devEngines;
        }

        boolean standsFor(NpmRelease candidate) {
            if (!candidate.answersAs(release)) {
                return false;
            }
            // the install is over: comparing its release from here on notes nothing more of it
            NpmRelease installed = NpmRelease.of(release.toString());
            for (String range : npmRanges) {
                if (NpmInstallChecks.npmSatisfies(candidate, range) !=
                    NpmInstallChecks.npmSatisfies(installed, range)) {
                    return false;
                }
            }
            return NpmReleases.devEnginesRefuse(devEngines, candidate) ==
                   NpmReleases.devEnginesRefuse(devEngines, installed);
        }
    }

    /**
     * The registry as one install reads it. Each document is the install's own copy, since an install normalizes what
     * it reads in place and the next one, as another release, reads the registry's own; the ranges of npm a document
     * names are noted as it is first read.
     */
    private static final class InstallRegistry implements PackumentSource {
        private final PackumentSource source;
        private final Set<String> npmRanges;
        private final Map<Map<String, Object>, Map<String, Object>> read = new IdentityHashMap<>();

        InstallRegistry(PackumentSource source, Set<String> npmRanges) {
            this.source = source;
            this.npmRanges = npmRanges;
        }

        @Override
        public Map<String, Object> packument(String registry, String name, boolean fullMetadata) {
            return own(source.packument(registry, name, fullMetadata));
        }

        @Override
        public Map<String, Object> manifest(String registry, String name, String version) {
            return own(source.manifest(registry, name, version));
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> own(Map<String, Object> document) {
            Map<String, Object> own = read.get(document);
            if (own == null) {
                own = (Map<String, Object>) Objects.requireNonNull(Js.jsonValue(document));
                read.put(document, own);
                noteNpmRanges(own, npmRanges);
            }
            return own;
        }
    }

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.NPM;

    private final Inputs inputs;
    private final PackumentSource registry;

    // of an install as one release: its first build and what ended it, which tell an npm refusing its own version
    private @Nullable IdealTreeBuilder built;
    private @Nullable RuntimeException ended;

    public NpmLockRegenerator(Inputs inputs, PackumentSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    /**
     * The lock npm writes for the project, or why there is none: installed as the release the inputs name, else as
     * each release its package.json allows that the engine reproduces, for one result ({@link #result}).
     */
    public Result regenerate() {
        List<NpmRelease> candidates;
        try {
            candidates = inputs.getRelease() != null ? Collections.singletonList(inputs.getRelease()) :
                    NpmReleases.candidates(localPackage(inputs.getManifests().get("")));
        } catch (CannotKnow e) {
            return Result.failure(failure(e));
        }
        return regenerate(candidates);
    }

    /**
     * {@link #regenerate()} for a project installed as {@code candidates}, newest first.
     */
    Result regenerate(List<NpmRelease> candidates) {
        return result(installs(candidates));
    }

    /**
     * Installs the project as each of {@code candidates} that no install already done stands for, in their order.
     * The installs share the registry's documents, each fetched once.
     */
    List<Install> installs(List<NpmRelease> candidates) {
        Set<String> projectRanges = new LinkedHashSet<>();
        for (String manifest : inputs.getManifests().values()) {
            noteNpmRanges(parsed(manifest), projectRanges);
        }
        noteNpmRanges(parsed(inputs.getLock()), projectRanges);
        Object devEngines = localPackage(inputs.getManifests().get("")).get("devEngines");

        List<Install> installs = new ArrayList<>();
        for (NpmRelease candidate : candidates) {
            Install standing = null;
            for (Install install : installs) {
                if (install.standsFor(candidate)) {
                    standing = install;
                    break;
                }
            }
            if (standing == null) {
                NpmRelease release = candidate.followed();
                Set<String> npmRanges = new LinkedHashSet<>(projectRanges);
                NpmLockRegenerator install = new NpmLockRegenerator(inputs.withRelease(release),
                        new InstallRegistry(registry, npmRanges));
                Result result = install.install();
                standing = new Install(release, result, install.refusesItsOwnVersion(), install.code(), npmRanges,
                        devEngines);
                installs.add(standing);
            }
            standing.getCandidates().add(candidate);
        }
        return installs;
    }

    // every range a document holds npm's version to: the engines.npm of each object in it
    private static void noteNpmRanges(@Nullable Object document, Set<String> npmRanges) {
        if (document instanceof Map) {
            Object engines = ((Map<?, ?>) document).get("engines");
            Object npm = engines instanceof Map ? ((Map<?, ?>) engines).get("npm") : null;
            if (npm instanceof String) {
                npmRanges.add((String) npm);
            }
            for (Object value : ((Map<?, ?>) document).values()) {
                noteNpmRanges(value, npmRanges);
            }
        } else if (document instanceof List) {
            for (Object element : (List<?>) document) {
                noteNpmRanges(element, npmRanges);
            }
        }
    }

    // a manifest or a lock as an install reads it; null where it does not parse, and names no range
    private static @Nullable Object parsed(@Nullable String json) {
        try {
            return json == null ? null : JsonParseEvenBetterErrors.parse(json).getValue();
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return null;
        }
    }

    // EBADDEVENGINES whose engine is the package manager, or EBADENGINE on the root package where the range of npm is
    // what the release does not satisfy
    private boolean refusesItsOwnVersion() {
        if (ended instanceof BadDevEngines) {
            return "packageManager".equals(((BadDevEngines) ended).getEngine());
        }
        Node root = built == null ? null : built.getIdealTree();
        NpmRelease release = inputs.getRelease();
        if (ended instanceof NpmInstallChecks.UnsupportedEngine && root != null && release != null) {
            Object engines = ((NpmInstallChecks.UnsupportedEngine) ended).getRequired();
            return engines == root.getPackage().get("engines") && truthy(Js.get(engines, "npm")) &&
                   !NpmInstallChecks.npmSatisfies(release, Js.get(engines, "npm"));
        }
        return false;
    }

    // npm's code for the error that ended the install
    private @Nullable String code() {
        RuntimeException outcome = ended == null ? null : outcome(ended);
        return outcome instanceof Rejected ? ((Rejected) outcome).getCode() : null;
    }

    /**
     * The one result of the installs, the newest release's first. An npm that refuses the project for its own version
     * is not the npm that installs it: those are left out, unless every one refuses, when the newest one's rejection
     * is the result. Of the rest, the result is the newest one's failure when every one fails and the lock when every
     * one writes the same; otherwise the release that installs the project cannot be known.
     */
    static Result result(List<Install> installs) {
        if (installs.isEmpty()) {
            throw new IllegalArgumentException("no npm release to install as");
        }
        List<Install> rest = new ArrayList<>();
        for (Install install : installs) {
            if (!install.isRefusing()) {
                rest.add(install);
            }
        }
        if (rest.isEmpty()) {
            return installs.get(0).getResult();
        }
        Result newest = rest.get(0).getResult();
        boolean anyInstalls = false;
        boolean same = true;
        for (Install install : rest) {
            anyInstalls |= install.getResult().isSuccess();
            same &= install.getResult().equals(newest);
        }
        if (!anyInstalls || same) {
            return newest;
        }
        return Result.failure(failure(new CannotKnow("the npm release that installs the project: " +
                differences(rest) + "; package.json names no release in packageManager or " +
                "devEngines.packageManager")));
    }

    // which releases come to which outcome, each outcome's releases as ranges: "npm 12.0.0 - 12.2.0 rejects it
    // (EALLOWGIT) and npm 11.17.0 installs it", or "npm 10.8.0 - 12.2.0 writes one lock and npm 10.0.0 - 10.7.0
    // another"
    private static String differences(List<Install> installs) {
        // the installs by outcome: the lock and files written, or a failure's kind and code
        Map<Object, List<Install>> outcomes = new LinkedHashMap<>();
        boolean anyFails = false;
        for (Install install : installs) {
            Failure failure = install.getResult().getFailure();
            anyFails |= failure != null;
            outcomes.computeIfAbsent(failure == null ? install.getResult() :
                    failure.getKind() + " " + install.getCode(), outcome -> new ArrayList<>()).add(install);
        }
        List<String> differences = new ArrayList<>();
        int locks = 0;
        for (List<Install> alike : outcomes.values()) {
            List<NpmRelease> releases = new ArrayList<>();
            for (Install install : alike) {
                releases.addAll(install.getCandidates());
            }
            String npm = "npm " + NpmReleases.ranges(releases);
            Failure failure = alike.get(0).getResult().getFailure();
            if (failure != null) {
                differences.add(failed(npm, failure.getKind(), alike.get(0).getCode()));
            } else if (anyFails) {
                differences.add(npm + (locks++ == 0 ? " installs it" : " installs it with another lock"));
            } else {
                differences.add(npm + (locks++ == 0 ? " writes one lock" : " another"));
            }
        }
        String last = differences.remove(differences.size() - 1);
        return String.join(", ", differences) + " and " + last;
    }

    // how a failure reads beside the other outcomes
    private static String failed(String npm, Kind kind, @Nullable String code) {
        switch (kind) {
            case PM_REJECTS:
                return npm + " rejects it" + (code == null ? "" : " (" + code + ")");
            case PM_FAILS:
                return npm + " fails on it";
            case SECOND_INSTALL:
                return npm + " writes a lock its second install does not keep";
            case CANNOT_KNOW:
            default:
                return "what " + npm + " writes cannot be known";
        }
    }

    // one install as the release of the inputs, which installs() names, and the second install over the lock it writes
    private Result install() {
        Objects.requireNonNull(inputs.getRelease(), "an install runs as a release");
        NpmOptions options;
        String path;
        String written;
        String yarnLock;
        try {
            INSTALL.requireInstallsAt(NpmOptions.loadLocalPrefix(inputs.getManifests().get(inputs.getRunsIn()),
                    inputs.getParentTrees()), inputs.getRunsIn(), "lock");
            if (inputs.getPaths() != null && Shrinkwrap.readsShrinkwrap(inputs.getPaths()::contains)) {
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
                new LockFile(inputs.getLock()) : null);
            path = projectPath(files);
            IdealTreeBuilder arb = arborist(path, files, options);
            arb.buildIdealTree();
            if (!options.isSave() || options.isDryRun()) {
                return Result.success(Objects.requireNonNull(inputs.getLock()));
            }
            written = saveIdealTree(arb, options);
            yarnLock = Objects.requireNonNull(arb.buildIdealTree().getMeta()).yarnLockText();
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            ended = e;
            return Result.failure(failure(e));
        }
        Result verified = INSTALL.verify(written, lock -> {
            IdealTreeBuilder again = arborist(path, projectFiles(new LockFile(lock)), options);
            again.buildIdealTree();
            return saveIdealTree(again, options);
        }, NpmLockRegenerator::failure);
        return yarnLock == null || !verified.isSuccess() ? verified :
                Result.success(written, Collections.singletonMap("yarn.lock", yarnLock));
    }

    /**
     * Where npm run in {@code runsIn} installs and writes its lock, by {@link Inputs#getParentTrees()}; a walk that
     * fails, which {@link #regenerate()} reports as it walks first, stops here.
     */
    public static InstallRoot installRoot(String runsIn, Map<String, String> manifests,
                                          Map<String, Map<String, @Nullable String>> parentTrees) {
        try {
            return InstallRoot.at(NpmOptions.loadLocalPrefix(manifests.get(runsIn), parentTrees));
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

        Object devEngines = Js.get(localPackage(manifest), "devEngines");

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
     * installs as npm's own commands read it, an empty one where there is none or it does not normalize.
     */
    static Map<String, Object> localPackage(@Nullable String manifest) {
        if (manifest != null) {
            try {
                return PackageJson.normalize(manifest).getContent();
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
                files).entrySet()) {
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

    private VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock) {
        return projectFiles(lock, inputs.getYarnLock(), inputs.getManifests());
    }

    /**
     * The project's files, the lock's own entries standing in for the root and for link targets outside node_modules
     * the host did not supply, as arborist reads a project without a package.json.
     */
    @SuppressWarnings("unchecked")
    static VirtualTreeLoader.ProjectFiles projectFiles(@Nullable LockFile lock, @Nullable String yarnLock,
                                                       Map<String, String> manifests) {
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
                .lock(lock)
                .yarnLock(yarnLock)
                .manifests(manifests);
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
                    if (!manifests.containsKey("")) {
                        files.parsedManifest("", (Map<String, Object>) meta);
                    }
                } else if (Js.strictEquals(Js.get(meta, "link"), true) && resolved instanceof String &&
                           !((String) resolved).contains("node_modules/") && !manifests.containsKey(resolved)) {
                    Object target = Js.get(packages, (String) resolved);
                    if (target instanceof Map) {
                        files.parsedManifest((String) resolved, (Map<String, Object>) target);
                    }
                }
            }
        }
        return files.build();
    }

    private IdealTreeBuilder arborist(String path, VirtualTreeLoader.ProjectFiles files, NpmOptions options) {
        IdealTreeBuilder arb = new IdealTreeBuilder(path, files, options, registry, inputs.getHost(), inputs.getNow());
        if (built == null) {
            built = arb;
        }
        return arb;
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
            return Rejected.coded(Reason.PEER_CONFLICT, conflict.getEdge().getName(), "ERESOLVE",
                    ExplainEresolve.explain(conflict.getExplanation(), 4));
        }
        if (e instanceof CannotKnow) {
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
