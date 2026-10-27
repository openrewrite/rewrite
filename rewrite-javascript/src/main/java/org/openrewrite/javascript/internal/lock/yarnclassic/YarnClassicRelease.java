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
import org.openrewrite.javascript.internal.lock.Ini;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Yarn 1 release an install is reproduced as. The port follows yarn 1.22.22; what another release does
 * differently is asked here, one question per upstream change, named for what yarn does and answered by the release
 * that changed it (its tag {@code v1.x.y} of yarnpkg/yarn). The ported modules read as 1.22.22's source with each other
 * release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version but yarn's own version
 * ({@link #toString()}), which the lock's header, {@code engines.yarn} and yarn's messages name.
 */
public final class YarnClassicRelease implements Comparable<YarnClassicRelease> {

    /**
     * The release the port follows, and the one an install runs as when nothing names another.
     */
    public static final YarnClassicRelease PORTED = new YarnClassicRelease(1, 22, 22);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    private YarnClassicRelease(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * The release of a version such as {@code 1.22.19}; a prerelease or a range names none.
     */
    public static YarnClassicRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not a Yarn release: " + version);
        }
        return new YarnClassicRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                Integer.parseInt(m.group(3)));
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        return compareTo(new YarnClassicRelease(major, minor, patch)) >= 0;
    }

    // yarn .babelrc

    /**
     * An error yarn throws as one of its own classes ({@code MessageError} and those extending it) is an
     * {@code instanceof} that class before 1.4.0 (the es2015-node4 and node5 presets keep classes native) and from
     * 1.6.0 ({@code transform-builtin-extend}). From 1.4.0 to 1.5.1, babel-preset-env (loose, node 4) compiles
     * {@code class MessageError extends Error} to a constructor that returns the plain Error it calls, so that
     * cli/index.js reports every such error as unexpected ({@code An unexpected error occurred: "<message>".}) and
     * package-request.js (1.5.x) rewraps a resolver's own rejection.
     */
    boolean tellsItsErrorsByClass() {
        return !atLeast(1, 4, 0) || atLeast(1, 6, 0);
    }

    // yarn src/cli/index.js

    /**
     * {@code main} stops a project whose package.json names another {@code packageManager} than yarn 0 or 1, unless
     * {@code COREPACK_ROOT} is set: from 1.22.20.
     */
    boolean checksPackageManager() {
        return atLeast(1, 22, 20);
    }

    /**
     * {@code findPackageManager} finds that field in the nearest package.json from where yarn runs up, past one
     * JSON.parse rejects, and {@code SKIP_YARN_COREPACK_CHECK} skips the check, from 1.22.21; 1.22.20 reads the
     * manifest of the directory yarn runs in ({@code readRootManifest}), and its errors end yarn before the install
     * runs.
     */
    boolean findsPackageManagerAbove() {
        return atLeast(1, 22, 21);
    }

    /**
     * {@code onUnexpectedError} reports an error that is no MessageError as {@code An unexpected error occurred:
     * "<message>".}, before 1.22.20. 1.22.21 reports it by its stack, whose first line names the error; 1.22.20 reports
     * none, and the engine names the error as 1.22.21 does.
     */
    boolean reportsUnexpectedErrors() {
        return !atLeast(1, 22, 20);
    }

    /**
     * {@code start} hands the install to the release a .yarnrc's {@code yarnPath} names, as it does its
     * {@code yarn-path}, from 1.16.0; before, its {@code yarn-path} alone.
     */
    boolean followsAYarnrcsYarnPath() {
        return atLeast(1, 16, 0);
    }

    /**
     * {@code start} ends with the exit code of the yarn-path it ran through {@code forkp}, from 1.2.0 (1.2.1 also of
     * one {@code spawnp} ran); before, it awaits the yarn-path and ends with 0 whatever it did, and the project is as
     * the yarn-path left it: where node cannot load the file, as it was.
     */
    boolean exitsWithTheYarnPathsCode() {
        return atLeast(1, 2, 0);
    }

    /**
     * {@code start} reads {@code YARN_IGNORE_PATH} with {@code boolifyWithDefault} ({@code true}, {@code yes} ...
     * ignore the yarn-path, {@code 0}, {@code false} ... do not), from 1.3.1; before, only {@code 1} ignores it.
     */
    boolean boolifiesYarnIgnorePath() {
        return atLeast(1, 3, 1);
    }

    /**
     * {@code main} coerces the value of {@code --prod, --production [prod]} with its boolify, from 1.1.0; before,
     * commander keeps the value as it is given (a string, or true for the bare option).
     */
    boolean boolifiesTheProductionFlag() {
        return atLeast(1, 1, 0);
    }

    /**
     * {@code main} declares {@code --registry <url>}, which config.js passes to the registries over their config,
     * from 1.2.0; before, a .yarnrc's {@code --registry} is an option commander does not know, which it drops with
     * the value after it.
     */
    boolean declaresRegistry() {
        return atLeast(1, 2, 0);
    }

    /**
     * {@code main} declares {@code --update-checksums}, from 1.4.0; before, a .yarnrc's {@code --update-checksums} is
     * an option commander does not know, which it drops with the value after it.
     */
    boolean declaresUpdateChecksums() {
        return atLeast(1, 4, 0);
    }

    // yarn src/cli/commands/install.js

    /**
     * {@code setFlags} declares {@code --focus}, from 1.7.0; before, a .yarnrc's {@code --focus} is an option commander
     * does not know, which it drops with the value after it.
     */
    boolean declaresFocus() {
        return atLeast(1, 7, 0);
    }

    /**
     * {@code fetchRequestFromCwd} makes the resolution of a package the root's optionalDependencies name optional
     * under {@code --ignore-optional}, from 1.17.0; before, every resolution is required.
     */
    boolean resolvesOptionalDependenciesOptionally() {
        return atLeast(1, 17, 0);
    }

    /**
     * {@code fetchRequestFromCwd} asks for every workspace the root's own dependencies do not name as a dependency of
     * the root, beside the workspace aggregator, from 1.11.0; before, the aggregator alone asks for them.
     */
    boolean requestsWorkspacesFromTheRoot() {
        return atLeast(1, 11, 0);
    }

    /**
     * {@code init} checks the root's compatibility wherever its os, cpu or engines would be checked
     * ({@code shouldCheck}), from 1.15.0; before, only where its engines is an object, null among them, and
     * {@code --ignore-engines} is not given.
     */
    boolean checksTheRootsPlatform() {
        return atLeast(1, 15, 0);
    }

    /**
     * {@code saveLockfileAndIntegrity} writes no lock under {@code --frozen-lockfile} (#4385), from 1.0.2; before, a
     * frozen install that passes {@code bailout}'s check writes the lock as any install does.
     */
    boolean keepsTheLockUnderFrozenLockfile() {
        return atLeast(1, 0, 2);
    }

    /**
     * {@code fetchRequestFromCwd}'s workspace aggregator depends on the root's own dependencies, devDependencies and
     * optionalDependencies beside the workspaces, from 1.2.0; before, on the workspaces alone. The aggregator's
     * request of a root dependency carries parent names, so that a resolution reaches it.
     */
    boolean requestsTheRootsDependenciesFromTheAggregator() {
        return atLeast(1, 2, 0);
    }

    // yarn src/config.js

    /**
     * {@code init} holds a focused install's workspace by its package name ({@code focusedWorkspaceName}) and stops a
     * focus at the workspace root before it loads the registries, from 1.8.0; 1.7.0 takes the workspace whose package
     * the directory yarn runs in names, and stops a focus at the root in install's {@code init}.
     */
    boolean focusesByPackageName() {
        return atLeast(1, 8, 0);
    }

    /**
     * {@code unsafe-disable-integrity-migration} is a boolean that defaults to true, so that a lock entry without
     * integrity stays without it, from 1.12.0; before, the migration adds the integrity unless the option holds a
     * truthy value.
     */
    boolean keepsEntriesWithoutIntegrity() {
        return atLeast(1, 12, 0);
    }

    /**
     * {@code resolveWorkspaces} refuses a root's workspaces that are no array
     * ({@code workspacesSettingMustBeArray}), from 1.4.0; before, {@code patterns.map} of a string or an object throws.
     */
    boolean requiresAnArrayOfWorkspaces() {
        return atLeast(1, 4, 0);
    }

    /**
     * {@code extractWorkspaces} reads a manifest's workspaces as an array of member globs or an object with a
     * {@code packages} or {@code nohoist} array, which {@code findWorkspaceRoot}, {@code getWorkspaces} and
     * {@code resolveWorkspaces} take the packages of, from 1.5.0 (nohoist with it); before, any truthy
     * {@code workspaces} makes a workspace root, its value the member globs as it is (micromatch's {@code matcher}
     * throws for an object, {@code resolveWorkspaces} refuses what is no array).
     */
    boolean readsTheWorkspacesObject() {
        return atLeast(1, 5, 0);
    }

    /**
     * {@code getWorkspaces} throws its errors only, a nohoist the config disables being a warning, from 1.6.0; 1.5.x
     * throws every warning, {@code workspacesNohoistDisabled} among them.
     */
    boolean throwsOnlyWorkspaceErrors() {
        return atLeast(1, 6, 0);
    }

    /**
     * {@code resolveWorkspaces} ignores every manifest under a member glob's node_modules
     * ({@code /node_modules/**}{@code /+(package.json|yarn.json)}), from 1.6.0; before, those one level down only.
     */
    boolean ignoresEveryManifestUnderNodeModules() {
        return atLeast(1, 6, 0);
    }

    /**
     * {@code init} takes the {@code --production} flag wherever it is given, from 1.2.0; before, the flag's string
     * {@code false} makes no production install, the config's {@code production} or {@code NODE_ENV=production} (unless
     * {@code NPM_CONFIG_PRODUCTION} or {@code YARN_PRODUCTION} is {@code false}) makes one, and only then does the
     * flag's truth decide.
     */
    boolean letsTheProductionFlagWin() {
        return atLeast(1, 2, 0);
    }

    /**
     * {@code generateModuleCachePath} names a package's cache folder by {@code generateUniquePackageSlug}, whose
     * {@code replace} needs a string name, from 1.11.0; before, by its registry, name, version and uid, whatever their
     * type.
     */
    boolean slugsCacheFolders() {
        return atLeast(1, 11, 0);
    }

    // yarn src/rc.js, src/registries/yarn-registry.js

    /**
     * {@code offline-cache-folder} is a path an rc file's directory resolves, among rc.js's {@code PATH_KEYS} and
     * yarn-registry.js's {@code RELATIVE_KEYS}, from 1.11.0.
     */
    boolean resolvesOfflineCacheFolder() {
        return atLeast(1, 11, 0);
    }

    // yarn src/rc.js

    /**
     * {@code buildRcArgs} turns a .yarnrc's {@code --<arg>} into command-line arguments by the commander option it
     * names: {@code --<arg> <value>} for an option commander does not know or one that takes a value, {@code --<arg>}
     * for a flag whose value is {@code true}, from 1.1.0. Before, by the value's type: {@code --<arg> <value>} for a
     * string, {@code --<arg>} for {@code true}, {@code --no-<arg>} for {@code false}, nothing for any other, so that a
     * flag's string value stays behind as an argument of install, which refuses one.
     */
    boolean buildsRcArgsByTheirOption() {
        return atLeast(1, 1, 0);
    }

    /**
     * {@code loadRcFile} parses an rc file as its path names it, a .yarnrc.yml as YAML, from 1.22.1; before, every rc
     * file as a .yarnrc.
     */
    boolean parsesRcFilesByTheirPath() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code loadRcFile} keeps a .yarnrc.yml's {@code yarnPath} as {@code yarn-path}, resolved against the file's
     * directory, from 1.17.2; 1.17.1 keeps it as {@code yarnPath}, unresolved, which {@code start} runs from where
     * yarn runs.
     */
    boolean resolvesYarnrcYmlYarnPath() {
        return atLeast(1, 17, 2);
    }

    /**
     * {@code loadRcFile} keeps a .yarnrc.yml's settings unless its {@code yarnPath} is a string, from 1.22.1; before,
     * it keeps its {@code yarnPath} alone, as {@code yarn-path}.
     */
    boolean keepsYarnrcYmlSettings() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code loadRcFile} reads the {@code yarnPath} of a .yarnrc.yml only where the file holds a value, from 1.22.20;
     * before, a file of no value throws.
     */
    boolean readsYarnPathOfAValue() {
        return atLeast(1, 22, 20);
    }

    // yarn src/util/rc.js

    /**
     * {@code parseRcPaths} throws what reading an rc file throws but a missing file, from 1.22.1; before, a file that
     * throws counts as empty.
     */
    boolean throwsRcFileErrors() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code getRcPaths} reads a .yarnrc.yml beside every .yarnrc, from 1.17.1.
     */
    boolean readsYarnrcYml() {
        return atLeast(1, 17, 1);
    }

    /**
     * {@code getRcPaths} reads each directory's .yarnrc.yml after its .yarnrc, from 1.17.2; 1.17.1 reads it before.
     */
    boolean readsYarnrcYmlAfterYarnrc() {
        return atLeast(1, 17, 2);
    }

    // yarn src/registries/yarn-registry.js

    /**
     * {@code loadConfig} resolves a .yarnrc's {@code cache-folder} against the file's directory, as it does its
     * {@code yarn-offline-mirror}, from 1.9.0 ({@code RELATIVE_KEYS}).
     */
    boolean resolvesCacheFolder() {
        return atLeast(1, 9, 0);
    }

    /**
     * {@code loadConfig} resolves a .yarnrc's {@code yarn-path} too, from 1.13.0.
     */
    boolean resolvesYarnPath() {
        return atLeast(1, 13, 0);
    }

    /**
     * {@code loadConfig} resolves a .yarnrc's {@code global-folder} too, from 1.15.0.
     */
    boolean resolvesGlobalFolder() {
        return atLeast(1, 15, 0);
    }

    // yarn src/registries/base-registry.js

    /**
     * {@code mergeEnv} takes the variables whose name starts with its prefix, from 1.12.1; before, every one whose
     * name holds it, a name that does not start with it kept whole.
     */
    boolean mergesEnvOfItsPrefixOnly() {
        return atLeast(1, 12, 1);
    }

    /**
     * {@code mergeEnv} keeps the underscores a key starts with ({@code _auth}) and turns the others into dashes, from
     * 1.15.0; before, every underscore.
     */
    boolean keepsTheLeadingUnderscoresOfEnvKeys() {
        return atLeast(1, 15, 0);
    }

    // yarn src/lockfile/parse.js

    /**
     * {@code tokenise} ends a line at {@code \r} as at {@code \n}, a {@code \r\n} or {@code \n\n} being one line end
     * of two characters (a blank line counts no line of its own), and ends a word at {@code \r}, from 1.1.0; before,
     * {@code \r} is an invalid token, so that a .yarnrc of CRLF lines, which rc.js reads raw, is no rc file at all
     * (util/rc.js reads what throws as empty), and each {@code \n} is a line end of one character.
     */
    boolean readsCarriageReturnsAsLineEnds() {
        return atLeast(1, 1, 0);
    }

    /**
     * {@code parse}'s {@code Unknown token} names the token ({@code util.inspect}), from 1.1.0.
     */
    boolean inspectsTheUnknownToken() {
        return atLeast(1, 1, 0);
    }

    /**
     * {@code tokenise} ends a comment or a string at the end of the input, from 1.12.1; 1.12.0 reads on past it,
     * adding {@code undefined} to it until V8 refuses the string's length.
     */
    boolean endsTokensAtTheEndOfInput() {
        return atLeast(1, 12, 1);
    }

    /**
     * {@code parse} reads the plain value after a key and its colon, and {@code tokenise} a word that starts with a
     * dot, from 1.16.0; before, a colon always opens an object and a dot starts no token.
     */
    boolean readsAValueAfterAColon() {
        return atLeast(1, 16, 0);
    }

    /**
     * {@code parse} reads with js-yaml's failsafe schema what the lockfile parser cannot read, from 1.17.0.
     */
    boolean readsWithYamlWhatTheLockfileParserCannot() {
        return atLeast(1, 17, 0);
    }

    /**
     * {@code parse} reads a {@code .yml} file with js-yaml alone, from 1.22.1; before, as a lock first.
     */
    boolean parsesYmlAsYaml() {
        return atLeast(1, 22, 1);
    }

    /**
     * {@code parse} reads a {@code .yml} file whose document is no object as an empty object, from 1.22.2.
     */
    boolean readsYmlOfNoObjectAsEmpty() {
        return atLeast(1, 22, 2);
    }

    // yarn src/lockfile/index.js

    /**
     * {@code getLockfile} shares an entry among the patterns of one name and remote only, and no longer gives a shared
     * entry the {@code name} of the package where a pattern's name is another, from 1.22.22.
     */
    boolean sharesEntriesByName() {
        return atLeast(1, 22, 22);
    }

    /**
     * {@code fromDirectory} sets a Yarn Berry lock ({@code __metadata}) aside and resolves from scratch, from 1.19.0;
     * before, it reads the lock's entries as its own.
     */
    boolean setsBerryLocksAside() {
        return atLeast(1, 19, 0);
    }

    // yarn src/fetchers/tarball-fetcher.js

    /**
     * {@code createExtractor}'s map keeps every link an archive holds inside the package's directory, from 1.22.0.
     */
    boolean jailsTarballLinks() {
        return atLeast(1, 22, 0);
    }

    /**
     * {@code createExtractor} reports an archive tar cannot extract as corrupt ({@code errorExtractingTarball}), a
     * MessageError, from 1.8.0; before, by the extraction's own error.
     */
    boolean reportsCorruptArchives() {
        return atLeast(1, 8, 0);
    }

    /**
     * {@code createExtractor} checks a tarball against the remote's sha1 hash and its integrity both, from 1.19.0;
     * before, against its integrity, or its hash where it has none.
     */
    boolean checksTheHashBesideTheIntegrity() {
        return atLeast(1, 19, 0);
    }

    // yarn src/fetchers/base-fetcher.js

    /**
     * {@code fetch} links each bin of a package in its cache folder, where {@code path.resolve} throws for a target
     * that is no string, from 1.11.0; before, the link step links them, where {@code path.join} throws for it.
     */
    boolean linksBinsWhenFetched() {
        return atLeast(1, 11, 0);
    }

    // yarn src/util/normalize-manifest/fix.js

    /**
     * {@code fix} turns a dependency's falsy value into {@code ''} (#4461), from 1.1.0; before, the value stays, and
     * resolve-relative.js's {@code path.isAbsolute} throws for one that is no string ({@code null}, {@code 0},
     * {@code false}).
     */
    boolean emptiesFalsyDependencies() {
        return atLeast(1, 1, 0);
    }

    /**
     * {@code fix} cleans a manifest's {@code resolutions} as its dependencies (constants.js {@code MANIFEST_FIELDS}),
     * from 1.3.1: their {@code //} key goes and a falsy value becomes {@code ''}; before, a falsy resolution reaches
     * resolution-map.js as it is, where the git resolver's {@code url.parse} throws.
     */
    boolean cleansTheResolutions() {
        return atLeast(1, 3, 1);
    }

    /**
     * {@code fix} drops a bin whose name or target is invalid, and {@code path.isAbsolute} throws for a target that is
     * no string, from 1.21.1 ({@code isValidBin}).
     */
    boolean validatesBins() {
        return atLeast(1, 21, 1);
    }

    /**
     * {@code VALID_BIN_KEYS} takes a name with dots but {@code .} and {@code ..}, from 1.22.0; 1.21.1 takes letters,
     * digits, {@code _} and {@code -} only.
     */
    boolean takesBinNamesWithDots() {
        return atLeast(1, 22, 0);
    }

    // yarn src/registries/npm-registry.js

    /**
     * {@code getRequestUrl} resolves a package's path against the registry with a slash added to it (#4350), from
     * 1.0.2; before, {@code url.resolve(registry, pathname)}, which replaces the last segment of a registry path
     * that does not end with one ({@code https://host/npm/remote} asks {@code https://host/npm/<name>}).
     */
    boolean addsASlashToTheRegistry() {
        return atLeast(1, 0, 2);
    }

    /**
     * {@code isRequestToRegistry} takes a request to registry.yarnpkg.com for one to a registry of registry.npmjs.org
     * ({@code requestToYarn}), from 1.4.0; before, their hosts must be the same.
     */
    boolean takesYarnsRegistryForNpms() {
        return atLeast(1, 4, 0);
    }

    /**
     * {@code request} sends credentials to a host and path an {@code _authToken} key names ({@code requestNeedsAuth}),
     * from 1.4.0; before, only where {@code isRequestToRegistry} takes the request for one to the package's registry.
     */
    boolean authenticatesToARegistryOfAnAuthToken() {
        return atLeast(1, 4, 0);
    }

    /**
     * {@code getRequestUrl} asks registry.yarnpkg.com and registry.npmjs.org over https whatever the url says, from
     * 1.17.3.
     */
    boolean requestsPublicRegistriesOverHttps() {
        return atLeast(1, 17, 3);
    }

    /**
     * {@code requestNeedsAuth} sends credentials to a registry an rc file gives a {@code _password}, as to one it
     * gives an {@code _authToken}, from 1.12.1.
     */
    boolean authenticatesToARegistryOfAPassword() {
        return atLeast(1, 12, 1);
    }

    // yarn src/lockfile/index.js, src/package-request.js, src/resolvers/registries/npm-resolver.js (the integrity)

    /**
     * Yarn records each package's integrity, checks a tarball and a lock's entries against it, adds it to an entry
     * that has none and writes it to the lock, from 1.10.0; before, a lock holds no integrity and yarn reads none.
     */
    boolean recordsIntegrity() {
        return atLeast(1, 10, 0);
    }

    // yarn src/package-request.js

    /**
     * A request keeps the parent names its dependency request carries ({@code req.parentNames || []}), from 1.2.0;
     * before, every request starts from none, so that its dependencies carry its own name alone and a resolution's
     * {@code <parent>/<name>} matches under that parent at any depth, a longer path nowhere.
     */
    boolean passesTheParentNamesDown() {
        return atLeast(1, 2, 0);
    }

    /**
     * {@code findVersionOnRegistry} rewraps an error of a request that has a parent and is no MessageError as
     * {@code requiredPackageNotFoundRegistry} ({@code Couldn't find package $0 required by $1 on the $2 registry.}),
     * from 1.5.0; before, the resolver's error goes out as it is.
     */
    boolean namesTheParentOfAPackageNotFound() {
        return atLeast(1, 5, 0);
    }

    /**
     * {@code find} rejects a version semver does not take ({@code invalidPackageVersion}: {@code Can't add $0: invalid
     * package version $1.}), from 1.5.0; before, the install goes on with it.
     */
    boolean rejectsAnInvalidVersion() {
        return atLeast(1, 5, 0);
    }

    // yarn src/lockfile/index.js, src/package-request.js, src/package-resolver.js, src/cli/commands/install.js

    /**
     * Yarn reads a lock entry's {@code prebuiltVariants}, keeps them over a fetch, writes them and rewrites a lock whose
     * variants differ from the resolver's, from 1.5.0; before, it writes none.
     */
    boolean keepsPrebuiltVariants() {
        return atLeast(1, 5, 0);
    }

    // yarn src/resolvers/exotics/registry-resolver.js

    /**
     * The fragment of an {@code npm:} alias is {@code ^(\S+):(@?.*?)(@(.*?)|)$}, so that a scoped name keeps its
     * {@code @}, from 1.5.0; before, {@code ^(\S+):(.*?)(@(.*?)|)$}, which reads {@code npm:@s/p@1.0.0} as the name
     * {@code ""} and the range {@code s/p@1.0.0}.
     */
    boolean readsScopedAliases() {
        return atLeast(1, 5, 0);
    }

    /**
     * {@code findVersionInfo}'s {@code flatGlobalError} names the package that asks for a flat graph, from 1.12.0;
     * before, it passes none, and the message's {@code $0@$1} read {@code undefined@undefined}.
     */
    boolean namesTheFlatPackage() {
        return atLeast(1, 12, 0);
    }

    // yarn src/package-resolver.js, src/package-request.js

    /**
     * {@code find} keys a request by its registry, pattern and optional flag, and {@code resolveToExistingVersion}
     * adds the flag of a request that joins a version already resolved to its reference (#4448), from 1.1.0; before,
     * by registry and pattern, so that a pattern requested once as optional and once as required is requested once,
     * and the reference keeps the flag of the request that made it.
     */
    boolean tellsOptionalRequestsApart() {
        return atLeast(1, 1, 0);
    }

    // yarn src/package-resolver.js

    /**
     * {@code find} runs a request whose key was requested before, as one that is not fresh, which then joins a version
     * already resolved or resolves anew, from 1.4.0; before, it returns at once.
     */
    boolean findsARepeatedRequest() {
        return atLeast(1, 4, 0);
    }

    /**
     * {@code resolveToResolution} delays a pattern whose resolution's target is not resolved yet, and resolves it after
     * every request (#4372), from 1.0.2; before, {@code getStrictResolvedPattern} throws {@code expected manifest}.
     */
    boolean waitsForTheTargetOfAResolution() {
        return atLeast(1, 0, 2);
    }

    /**
     * {@code resolveToResolution} applies no resolution under {@code --flat} (#4372), from 1.0.2.
     */
    boolean appliesNoResolutionUnderFlat() {
        return atLeast(1, 0, 2);
    }

    /**
     * {@code init} collapses two or more unlocked patterns of a root request's name to one version of theirs
     * ({@code optimizeResolutions}, #4488), from 1.1.0; before, no such pass.
     */
    boolean optimizesResolutions() {
        return atLeast(1, 1, 0);
    }

    /**
     * {@code init} optimizes resolutions under {@code --flat} only (#4613), from 1.2.0; 1.1.0 for every install.
     */
    boolean optimizesOnlyFlatResolutions() {
        return atLeast(1, 2, 0);
    }

    /**
     * {@code optimizeResolutions} collapses to the highest version that satisfies each pattern's range (#4562), from
     * 1.2.0; 1.1.0 to {@code semver.maxSatisfying} of the ranges joined by spaces, one range whose {@code ||} splits
     * the text, not the ranges.
     */
    boolean collapsesToAVersionEveryRangeTakes() {
        return atLeast(1, 2, 0);
    }

    // yarn src/package-resolver.js, src/resolution-map.js

    /**
     * {@code resolveToResolution} keeps the lock entry of a pattern a resolution resolves where the pattern is one of
     * the root's ({@code topLevelPatterns}), from 1.3.1; before, it removes the entry of every such pattern.
     */
    boolean keepsTheLockEntryOfATopLevelPattern() {
        return atLeast(1, 3, 1);
    }

    /**
     * {@code resolveToResolution} keeps the lock entry of every pattern a resolution resolves, from 1.6.0.
     */
    boolean keepsTheLockEntryOfAResolvedPattern() {
        return atLeast(1, 6, 0);
    }

    /**
     * {@code resolveToResolution} removes the lock entry of a pattern a resolution resolves where the entry's
     * {@code resolved} is not the resolution's ({@code shouldUpdateLockfile}), from 1.7.0.
     */
    boolean removesTheLockEntryOfAPatternResolvedElsewhere() {
        return atLeast(1, 7, 0);
    }

    // yarn src/resolvers/registries/npm-resolver.js

    /**
     * {@code findVersionInRegistryResponse} takes {@code dist-tags.latest} where it satisfies the range (#3560), from
     * 1.3.1; before, the highest version that does.
     */
    boolean prefersTheLatestTag() {
        return atLeast(1, 3, 1);
    }

    /**
     * {@code findVersionInRegistryResponse} rejects a response whose {@code versions} are empty
     * ({@code registryNoVersions}) or missing ({@code malformedRegistryResponse}), from 1.5.0; before, only one without
     * {@code dist-tags} is malformed, empty versions match nothing, and missing ones throw where they are read.
     */
    boolean rejectsAResponseWithoutVersions() {
        return atLeast(1, 5, 0);
    }

    /**
     * {@code findVersionInRegistryResponse}'s {@code malformedRegistryResponse} names the package asked for, escaped,
     * from 1.12.0; before, the name the response gives.
     */
    boolean namesTheRequestedPackageOfAMalformedResponse() {
        return atLeast(1, 12, 0);
    }

    /**
     * {@code cleanRegistry} rewrites an http registry.npmjs.org url to registry.yarnpkg.com as an https one, from
     * 1.10.0 ({@code NPM_REGISTRY_RE}); before, an https one only.
     */
    boolean cleansHttpNpmRegistryUrls() {
        return atLeast(1, 10, 0);
    }

    // yarn src/resolvers/exotics/git-resolver.js

    /**
     * {@code isVersion} takes a pattern for git where it matches {@code GIT_PATTERN_MATCHERS} ({@code ^git:},
     * {@code ^git\+.+:}, {@code ^ssh:}, {@code ^https?:.+\.git$}, {@code ^https?:.+\.git#.+}) or names a git host, a user
     * and a repository, from 1.6.0; before, where url.parse finds a protocol and then a pathname ending in {@code .git}
     * ({@code file:../x.git}, {@code github:a/b.git}, {@code https://h/x.git?q}), a {@code git+} protocol, {@code git:}
     * or {@code ssh:}, or a git host, a user and a repository. Git is the first exotic resolver yarn tries.
     */
    boolean tellsGitByItsUrlPatterns() {
        return atLeast(1, 6, 0);
    }

    // yarn src/resolvers/exotics/file-resolver.js

    /**
     * {@code prefixMatcher} takes one or two dots before a slash ({@code ./}, {@code ../}), from 1.0.2 (#4338); before,
     * any one or two characters, so that a package of a one-character scope ({@code @a/pkg@^1.0.0}) is a file: path.
     */
    boolean takesOnlyDotsBeforeAFileSlash() {
        return atLeast(1, 0, 2);
    }

    /**
     * {@code resolve} names the package and the file of a file: pattern whose directory does not exist
     * ({@code doesntExist}: {@code Package $1 refers to a non-existing file '$0'.}), from 1.4.0; before, the file
     * alone ({@code $0 doesn't exist.}).
     */
    boolean namesThePackageOfAMissingFile() {
        return atLeast(1, 4, 0);
    }

    // yarn src/resolvers/exotics/link-resolver.js

    /**
     * {@code resolve} makes a link to a directory without a package.json a package of the directory's name at 0.0.0,
     * from 1.17.0; before, only a link to a path that does not exist, and a directory without one fails.
     */
    boolean linksADirectoryWithoutManifest() {
        return atLeast(1, 17, 0);
    }

    /**
     * {@code resolve} makes a link to the lockfile's directory such a package too, from 1.18.0; before, it is the
     * root's own package.
     */
    boolean linksTheLockfileFolderAsADirectory() {
        return atLeast(1, 18, 0);
    }

    // yarn src/util/normalize-manifest/resolve-relative.js

    /**
     * A file: or link: dependency on the lockfile's directory is written {@code .}, from 1.18.0; before, one written
     * with a {@code ./} prefix becomes {@code ./.}.
     */
    boolean writesTheLockfileFolderAsDot() {
        return atLeast(1, 18, 0);
    }

    // yarn src/package-compatibility.js, src/reporters/lang/en.js

    /**
     * {@code checkOne}'s {@code incompatibleEngine} names the version the engine has ({@code Got $2}), from 1.10.0.
     */
    boolean namesTheEngineVersion() {
        return atLeast(1, 10, 0);
    }

    /**
     * {@code foundIncompatible} ends with a period, from 1.16.0.
     */
    boolean endsFoundIncompatibleWithAPeriod() {
        return atLeast(1, 16, 0);
    }

    /**
     * {@code workspacesRequirePrivateProjects} ends with a period, from 1.4.0.
     */
    boolean endsTheWorkspacesPrivacyMessageWithAPeriod() {
        return atLeast(1, 4, 0);
    }

    // yarn src/reporters/base-reporter.js

    /**
     * {@code stringifyLangArgs} turns each line break an argument's JSON escapes ({@code \r\n}, {@code \n}) into
     * {@code os.EOL}, from 1.5.0; before, a message prints the escape.
     */
    boolean breaksTheLinesOfAnArgument() {
        return atLeast(1, 5, 0);
    }

    /**
     * {@code stringifyLangArgs} keeps a {@code \n} that a backslash leads in an argument's JSON (an argument's
     * backslash followed by an {@code n}), from 1.7.0; 1.5.0 and 1.6.0 turn its escape into {@code os.EOL} as well.
     */
    boolean keepsAnEscapedBackslashBeforeAnN() {
        return atLeast(1, 7, 0);
    }

    // yarn src/util/conversion.js

    /**
     * {@code boolify} takes the number 0 as false, from 1.6.0; before, cli/index.js's own boolify is false only for
     * a value whose string is {@code false} or the string {@code 0} (a .yarnrc's {@code --production 0} is true).
     */
    boolean readsZeroAsFalse() {
        return atLeast(1, 6, 0);
    }

    // ini

    /**
     * The ini yarn reads an .npmrc with: 1.3.5 from 1.10.0, which trims a value cut at an inline comment; before,
     * 1.3.4, which keeps the whitespace before the comment.
     */
    Ini.Release ini() {
        return atLeast(1, 10, 0) ? Ini.Release.V1_3_5 : Ini.Release.V1_3_4;
    }

    // tar-fs

    /**
     * The tar-fs yarn extracts a tarball with: 1.16.3 from 1.10.0; before, 1.16.0, which writes through a directory
     * that is a link of the archive and resolves a hard link's target from the package's directory, out of it too.
     */
    TarFs.Release tarFs() {
        return atLeast(1, 10, 0) ? TarFs.Release.V1_16_3 : TarFs.Release.V1_16_0;
    }

    // tar-stream

    /**
     * The tar-stream yarn extracts a tarball with: 1.6.0 from 1.7.0 (1.6.1 from 1.8.0, the same extract), whose
     * {@code mixinPax} takes a pax header's {@code size}; before, 1.5.4, which keeps the entry header's size.
     */
    TarStream.Release tarStream() {
        return atLeast(1, 7, 0) ? TarStream.Release.V1_6_1 : TarStream.Release.V1_5_4;
    }

    // readable-stream

    /**
     * tar-stream's extract fails an archive that ends inside an entry, its padding or a header ({@code _final}'s
     * {@code Unexpected end of data}), from 1.8.0, whose readable-stream 2.3.6 calls a Writable's {@code _final};
     * before, readable-stream 2.2.9 finishes the extract where the archive ends.
     */
    boolean failsAnArchiveThatEndsEarly() {
        return atLeast(1, 8, 0);
    }

    @Override
    public int compareTo(YarnClassicRelease o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) {
            c = Integer.compare(minor, o.minor);
        }
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof YarnClassicRelease && compareTo((YarnClassicRelease) o) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    /**
     * The version, as {@code yarn --version} prints it.
     */
    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
