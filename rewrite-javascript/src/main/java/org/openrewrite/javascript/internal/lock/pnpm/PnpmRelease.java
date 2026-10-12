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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The pnpm release an install is reproduced as. The port follows pnpm 11.2.2; what another release does differently is
 * asked here, one question per upstream change, named for what pnpm does and answered by the release that changed it
 * (tag {@code v<release>} of pnpm/pnpm, as the release's bundle holds the module). The ported modules read as
 * 11.2.2's source with each other release's branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version but where pnpm names its own
 * ({@link #version()}). Where the inputs do not say which release of a range installs (a range the project names
 * whose pick the inputs do not hold, corepack and pnpm run directly starting different ones), the release is every
 * release of {@link PnpmReleases#PROVEN} it may be, and a question their answers to differ on is cannot-know.
 */
public final class PnpmRelease {

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    /**
     * The release the port follows.
     */
    public static final PnpmRelease PORTED = of("11.2.2");

    /**
     * The version, or the range the inputs leave it at.
     */
    private final String name;

    /**
     * The releases it may be, ascending, as major, minor and patch.
     */
    private final List<int[]> releases;

    private PnpmRelease(String name, List<int[]> releases) {
        this.name = name;
        this.releases = releases;
    }

    /**
     * The release of a version such as {@code 11.5.1}; a prerelease or a range names none.
     */
    public static PnpmRelease of(String version) {
        return new PnpmRelease(version, Collections.singletonList(parse(version)));
    }

    /**
     * One of {@code releases}, ascending, which {@code range} names where the inputs do not say which installs.
     */
    static PnpmRelease oneOf(String range, List<String> releases) {
        if (releases.isEmpty()) {
            throw new IllegalArgumentException("no release of " + range);
        }
        if (releases.size() == 1) {
            return of(releases.get(0));
        }
        List<int[]> parsed = new ArrayList<>();
        for (String release : releases) {
            parsed.add(parse(release));
        }
        return new PnpmRelease(range, Collections.unmodifiableList(parsed));
    }

    private static int[] parse(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not a pnpm release: " + version);
        }
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     *
     * @throws CannotKnow where this is one of several releases, some of them before that release and some not
     */
    public boolean atLeast(int major, int minor, int patch) {
        int at = 0;
        for (int[] release : releases) {
            int c = Integer.compare(release[0], major);
            if (c == 0) {
                c = Integer.compare(release[1], minor);
            }
            if (c == 0) {
                c = Integer.compare(release[2], patch);
            }
            if (c >= 0) {
                at++;
            }
        }
        if (at == releases.size()) {
            return true;
        }
        if (at == 0) {
            return false;
        }
        throw new CannotKnow("the pnpm release that installs the project, one of " + name + ", where the releases " +
                             "from " + major + "." + minor + "." + patch + " install it otherwise than those before");
    }

    // @pnpm/lockfile.utils src/toLockfileResolution.ts and pkgSnapshotToResolution.ts, @pnpm/lockfile.fs
    // src/lockfileFormatConverters.ts

    /**
     * A git-hosted tarball's resolution (codeload.github.com, gitlab.com, bitbucket.org) carries
     * {@code gitHosted: true}, written by toLockfileResolution and added to a lock read without it, from 11.0.7
     * ("Pin the integrity of git-hosted tarballs"); before, there is no such field, and a lock's is not written back.
     */
    boolean marksGitHostedTarballs() {
        return atLeast(11, 0, 7);
    }

    /**
     * Under {@code lockfileIncludeTarballUrl: false}, the default, a tarball URL the registry, name and version do not
     * give is kept in the lock, from 11.0.8 (pnpm/pnpm#11276); before, the resolution is the integrity alone.
     */
    boolean keepsATarballUrlTheRegistryCannotGive() {
        return atLeast(11, 0, 8);
    }

    /**
     * A snapshot without a resolution whose dep path is a {@code file:} directory reads as that directory's
     * resolution, from 11.2.0 (convertToLockfileObject, for a pruned lock); before, it has none.
     */
    boolean readsADirectoryResolutionFromItsDepPath() {
        return atLeast(11, 2, 0);
    }

    /**
     * A locked registry tarball without an integrity is {@code ERR_PNPM_MISSING_TARBALL_INTEGRITY}, from 11.4.0
     * (pkgSnapshotToResolution); before, it is resolved as it is.
     */
    boolean requiresALockedTarballsIntegrity() {
        return atLeast(11, 4, 0);
    }

    /**
     * A tarball URL is git-hosted in any case ({@code https://CodeLoad.GitHub.com/...}, {@code TAR.GZ}), and
     * lockfile.fs asks lockfile.utils, from 11.5.3; before, only as written in lower case.
     */
    boolean matchesGitHostedTarballUrlsInAnyCase() {
        return atLeast(11, 5, 3);
    }

    /**
     * A resolution written with its tarball URL keeps its {@code path} (a git repository's subdirectory), from 11.7.0;
     * before, the path is left out.
     */
    boolean keepsATarballResolutionsPath() {
        return atLeast(11, 7, 0);
    }

    /**
     * A registry tarball URL with an escaped slash in any case ({@code @s%2Fp}) is the registry's own and left out
     * of the lock, only an http(s) protocol removed before comparing, from 11.9.0 (tarball-url's
     * isCanonicalRegistryTarballUrl); before, only {@code %2f} is unescaped, and the URL is kept.
     */
    boolean unescapesAnyCaseSlashInATarballUrl() {
        return atLeast(11, 9, 0);
    }

    /**
     * A tarball URL is git-hosted only as an https URL of a commit's archive ({@code codeload.github.com/<o>/<r>/
     * tar.gz/<sha>}, bitbucket.org's {@code <o>/<r>/get/<sha>.tar.gz}, gitlab.com's {@code api/v4/projects/<id>/
     * repository/archive.tar.gz?ref=<sha>} or {@code <o>/<r>/-/archive/<sha>/<name>.tar.gz}), a tarball that is no
     * string none, from 11.9.0 (resolver-base's isGitHostedTarballUrl, which toLockfileResolution, lockfile.fs, both
     * verifiers and the fetcher's classifyResolution share, the npm verifier in place of the {@code gitHosted} flag);
     * before, any URL of those hosts with {@code tar.gz} in it, and lockfile.fs fails on a tarball that is no string.
     */
    boolean recognizesOnlyCommitArchivesAsGitHosted() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/lockfile.fs src/yamlDocuments.ts

    /**
     * A lock with CRLF line ends reads its env document and its main document apart, from 11.1.3
     * (pnpm/pnpm#11612); before, the two documents are read as one, which no YAML reader accepts.
     */
    boolean readsACrLfLockWithAnEnvDocument() {
        return atLeast(11, 1, 3);
    }

    // @pnpm/lockfile.fs src/write.ts (EnvLockfile.OnDisk)

    /**
     * A lock file that already reads as the lock pnpm writes, its BOM stripped and its CRLF line ends read as LF, is
     * left as it is, from 11.14.0 (writeLockfileDoc); before, it is written over, LF and without a BOM.
     */
    boolean leavesAnUnchangedLockAsItIs() {
        return atLeast(11, 14, 0);
    }

    // @pnpm/lockfile.verification src/satisfiesPackageManifest.ts, allCatalogsAreUpToDate.ts,
    // gitSpecifiersAreEquivalent.ts

    /**
     * A git specifier the lock records is the manifest's where both name one repository and committish
     * ({@code github:o/r#x}, {@code o/r#x}, {@code git+https://github.com/o/r.git#x}; gitlab and bitbucket alike, and
     * {@code git://}, {@code git+https://} and {@code https://} URLs of those hosts or ending in {@code .git}), so
     * that the project and the catalogs are up to date and the lock is kept, from 11.14.0
     * (dependencySpecifiersAreEqual); before, only the same specifier is.
     */
    boolean takesEquivalentGitSpecifiersAsEqual() {
        return atLeast(11, 14, 0);
    }

    /**
     * minimumReleaseAge set explicitly makes minimumReleaseAgeStrict default to true, from 11.0.4; before, it stays
     * unset, and the picker takes the lowest immature version.
     */
    boolean defaultsMinimumReleaseAgeStrict() {
        return atLeast(11, 0, 4);
    }

    /**
     * A {@code PNPM_CONFIG_} environment variable sets a setting as its {@code pnpm_config_} form does, from 11.0.6;
     * before, only the lower-case form is read.
     */
    boolean readsUpperCaseConfigVariables() {
        return atLeast(11, 0, 6);
    }

    /**
     * The .npmrc the environment's {@code pnpm_config_npmrc_auth_file}, else {@code pnpm_config_userconfig} (or its
     * {@code PNPM_CONFIG_} form), names is read as the user's in place of ~/.npmrc, an empty value unset, from 11.0.6
     * (readEnvVar); before, the environment names none.
     */
    boolean readsTheUserNpmrcTheEnvironmentNames() {
        return atLeast(11, 0, 6);
    }

    /**
     * The .npmrc {@code npm_config_userconfig} (or {@code NPM_CONFIG_USERCONFIG}, which actions/setup-node sets) names
     * is read as the user's where neither pnpm variable nor the global config.yaml's npmrcAuthFile names one, from
     * 11.0.9 (readNpmEnvVar); before, pnpm reads ~/.npmrc all the same.
     */
    boolean readsTheUserNpmrcNpmConfigUserconfigNames() {
        return atLeast(11, 0, 9);
    }

    /**
     * An {@code overrides} setting that is no object of strings is {@code ERR_PNPM_INVALID_OVERRIDES}, from 11.0.6;
     * before, it is read as it is.
     */
    boolean rejectsOverridesThatAreNoStrings() {
        return atLeast(11, 0, 6);
    }

    /**
     * {@code updateWorkspaceManifest} keeps the order of a pnpm-workspace.yaml level that is not sorted, a new key
     * last, and the blank lines between keys, from 11.0.6 (reorderRecursive, propagateBlankLinesToNewPairs); before,
     * it sorts every level, packages first, as pnpm 10 does.
     */
    public boolean keepsTheWorkspaceManifestKeyOrder() {
        return atLeast(11, 0, 6);
    }

    /**
     * {@code namedRegistries} (and the built-in {@code gh}) name registries an {@code <alias>:} spec resolves from,
     * whose URLs and those of {@code registries} take the environment's {@code ${VAR}}, from 11.1.0
     * (pnpm/pnpm#11324); before, no resolver claims such a spec, and neither setting is read.
     */
    boolean readsNamedRegistries() {
        return atLeast(11, 1, 0);
    }

    /**
     * devEngines.packageManager as one object without {@code onFail} takes pmOnFail's default ({@code download}),
     * from 11.1.3 (pnpm/pnpm#11676); before, {@code error}.
     */
    boolean takesPmOnFailForDevEnginesWithoutOnFail() {
        return atLeast(11, 1, 3);
    }

    /**
     * A node runtime whose version is no valid range gives no {@code nodeVersion}, from 11.4.0; before, its
     * {@code semver.minVersion} throws.
     */
    boolean skipsAnInvalidNodeRuntimeRange() {
        return atLeast(11, 4, 0);
    }

    /**
     * pnpm-workspace.yaml's {@code update} and {@code audit} settings are objects, read into updateConfig, auditConfig
     * and auditLevel ({@code ERR_PNPM_INVALID_SETTING} otherwise), and {@code update} no longer sets the install's
     * update, from 11.16.0; before, both are read as they are.
     */
    boolean translatesTheUpdateAndAuditSettings() {
        return atLeast(11, 16, 0);
    }

    /**
     * The {@code packageManager} field is split at the first {@code @} after a scope's, everything after it the
     * reference ({@code @pnpm/exe@11.2.2} names @pnpm/exe), from 11.8.0; before, at each {@code @}, the first two
     * parts the name and the reference.
     */
    boolean splitsThePackageManagerAtItsVersionsAt() {
        return atLeast(11, 8, 0);
    }

    /**
     * {@code trustLockfile} skips the verification of the lock's resolutions, from 11.3.0; before, it is no setting.
     */
    boolean readsTrustLockfile() {
        return atLeast(11, 3, 0);
    }

    /**
     * An explicit {@code registry} setting (pnpm-workspace.yaml's) is the default registry the install resolves from,
     * over the workspace's {@code registries} and pnpm_config__auth's, from 11.10.0; before, it is the setting alone,
     * and the default registry the .npmrc's.
     */
    boolean takesTheRegistrySettingAsTheDefault() {
        return atLeast(11, 10, 0);
    }

    // @pnpm/config.reader src/loadNpmrcFiles.ts, getNetworkConfigs.ts

    /**
     * An .npmrc's unscoped credentials ({@code _authToken}, {@code _auth}, {@code username}, {@code _password},
     * {@code tokenHelper}, {@code cert}, {@code key}) are pinned to the registry that .npmrc names, else npm's, and
     * checked with that registry's own, from 11.4.0; before, the merged unscoped ones are checked as the default
     * registry's.
     */
    boolean rescopesUnscopedCredentials() {
        return atLeast(11, 4, 0);
    }

    /**
     * An .npmrc value with a placeholder the environment does not set loses that placeholder, the others substituted
     * (envReplaceLossy), from 11.1.3; before, the value is kept as written, every placeholder in it.
     */
    boolean dropsAnUnresolvedNpmrcPlaceholder() {
        return atLeast(11, 1, 3);
    }

    /**
     * The environment's {@code npm_config_//…} and {@code pnpm_config_//…} variables set their {@code //…} key (a
     * registry's credentials, certificates) after the project's .npmrc, the pnpm one winning, from 11.6.0; before,
     * they set nothing.
     */
    boolean readsUrlScopedConfigFromTheEnvironment() {
        return atLeast(11, 6, 0);
    }

    /**
     * A registry's credentials are checked together by registry and scope, a scope written after the URL
     * ({@code //h/@s/:}) or after a colon ({@code //h/:@s:}) alike, from 11.7.0; before, by the key as written.
     */
    boolean groupsCredentialsByRegistryAndScope() {
        return atLeast(11, 7, 0);
    }

    /**
     * The environment's {@code pnpm_config__auth} (else {@code PNPM_CONFIG__AUTH}), a JSON object of registry URLs and
     * the {@code authToken} of each scope ({@code @} the default), sets those tokens after the URL-scoped config and
     * routes each scope to its registry, over the workspace's {@code registries} and for pnpm's own bootstrap too,
     * {@code ERR_PNPM_INVALID_AUTH_SETTING} where it is no such object, from 11.10.0 (readJsonAuthEnv); before, pnpm
     * reads no such setting.
     */
    boolean readsTheAuthSetting() {
        return atLeast(11, 10, 0);
    }

    // @pnpm/store.connection-manager src/createNewStoreController.ts, @pnpm/store.index (ReadOnlyStoreIndex)

    /**
     * {@code frozenStore} opens the store index read-only, which needs a Node.js that opens a SQLite "immutable" URI
     * ({@code ERR_PNPM_FROZEN_STORE_UNSUPPORTED_NODE}) and a seeded store, from 11.7.0; before, it is no setting.
     */
    boolean opensAFrozenStoreReadOnly() {
        return atLeast(11, 7, 0);
    }

    /**
     * The resolver fetches full packuments, unless the config's fetchFullMetadata says otherwise, wherever
     * supportedArchitectures sets libc, under trustPolicy no-downgrade, and under time-based resolution where the
     * registry has no time field, the install command passing nothing over it, from 11.13.0 (shouldFetchFullMetadata);
     * before, the install command passes {@code supportedArchitectures.libc && true} (getFetchFullMetadata), and
     * otherwise time-based resolution and no-downgrade fetch them only where the registry has no time field.
     */
    boolean fetchesFullMetadataForLibcAndNoDowngrade() {
        return atLeast(11, 13, 0);
    }

    // @pnpm/hooks.read-package-hook src/createReadPackageHook.ts

    /**
     * The compatibility database's and the project's packageExtensions are one extender whose entries of one selector
     * are merged, the first one's keys winning, from 11.7.0; before, the database's last entry of a selector wins,
     * and the project's are a second extender.
     */
    boolean mergesThePackageExtensionsOfASelector() {
        return atLeast(11, 7, 0);
    }

    // @pnpm/config.parse-overrides src/index.ts, @pnpm/hooks.read-package-hook src/createVersionsOverrider.ts

    /**
     * An override of a package with an empty range ({@code foo@}) converges: its value must be an exact version, and
     * it replaces only a declared range (a peer's too) that version satisfies, where no other override matches; an
     * empty range in a parent>child selector, and a convergence value that is no version, are
     * {@code ERR_PNPM_INVALID_CONVERGENCE_OVERRIDE} (in allowedVersions
     * {@code ERR_PNPM_INVALID_ALLOWED_VERSION_SELECTOR}), from 11.13.0 (markConvergeOverride, convergeDep); before, an
     * empty range is any, and the value replaces every range.
     */
    boolean convergesAnEmptyRangeOverride() {
        return atLeast(11, 13, 0);
    }

    /**
     * An {@code _auth} or {@code _password} that is no base64 is decoded again with its padding mended, else is
     * {@code ERR_PNPM_AUTH_INVALID_BASE64}, from 11.1.3; before, Node's atob throws its InvalidCharacterError.
     */
    boolean decodesACredentialForgivingItsPadding() {
        return atLeast(11, 1, 3);
    }

    /**
     * The project's own .npmrc expands no environment variable in a request destination (a registry, a {@code //}
     * key, a proxy) or a credential, and its pnpm-workspace.yaml none in {@code registry}, {@code pnprServer},
     * {@code registries} or {@code namedRegistries}: such an entry is left out with a warning, from 11.5.3; before,
     * each is expanded as any other.
     */
    boolean ignoresTheEnvironmentInRepositoryRequestDestinations() {
        return atLeast(11, 5, 3);
    }

    /**
     * pnpm resolves and fetches itself (the package manager check and the switch) from {@code
     * packageManagerRegistries}, those of its built-in config, the user's .npmrc and the environment alone, from
     * 11.5.3 (getPackageManagerBootstrapConfig); before, from the install's registries, the project's among them.
     */
    boolean resolvesThePackageManagerFromTrustedConfig() {
        return atLeast(11, 5, 3);
    }

    // pnpm/src main.ts, syncEnvLockfile.ts, switchCliVersion.ts, packageManagerLockfile.ts

    /**
     * devEngines.runtime and engines.runtime with {@code onFail} error or warn check the runtime's version, error
     * failing with {@code ERR_PNPM_BAD_RUNTIME_VERSION}, from 11.4.0 (pnpm/pnpm#11818); before, only
     * {@code download} does anything.
     */
    boolean checksTheRuntimeVersion() {
        return atLeast(11, 4, 0);
    }

    /**
     * The package manager check records the release that runs in an env document that has no pnpm entry, from 11.1.3
     * (pnpm/pnpm#11674); before, it refreshes only an entry the wanted range does not satisfy.
     */
    boolean recordsTheReleaseAnEnvDocumentLacks() {
        return atLeast(11, 1, 3);
    }

    /**
     * Before it hands the install to another release, pnpm checks that each package the env document's
     * packageManagerDependencies reach is a registry package locked by an integrity alone
     * ({@code ERR_PNPM_INVALID_PACKAGE_MANAGER_LOCKFILE}), ahead of its store controller, from 11.5.3; before, it
     * checks nothing.
     */
    boolean checksTheEnvDocumentBeforeSwitching() {
        return atLeast(11, 5, 3);
    }

    /**
     * pnpm refuses to hand the install to a broken release, 11.12.0 or 11.13.0, which it resolved and recorded in the
     * env document, with {@code ERR_PNPM_BROKEN_PNPM_RELEASE}, ahead of the env document's check, from 11.14.0
     * (assertReleaseIsInstallable); before, it hands the install over.
     */
    boolean refusesToSwitchToABrokenRelease() {
        return atLeast(11, 14, 0);
    }

    // @pnpm/installing.commands src/installDeps.ts, verifyPacquetIdentity.ts

    /**
     * A pacquet the configDependencies name, locked with its binary for the host by integrity, has its registry
     * signatures checked before every install, a lock-only one too, from 11.5.3; before, a lock-only install never
     * reads it.
     */
    boolean verifiesPacquetBeforeInstalling() {
        return atLeast(11, 5, 3);
    }

    // @pnpm/engine.runtime.node-resolver src/index.ts, @pnpm/crypto.shasums-file src/verifyNodeShasums.ts

    /**
     * A Node.js runtime of the release channel is resolved from a SHASUMS256.txt whose OpenPGP signature
     * ({@code SHASUMS256.txt.sig}) a Node.js release key must have made, from 11.5.3; before, the file is read
     * unsigned.
     */
    boolean verifiesTheNodeShasumsSignature() {
        return atLeast(11, 5, 3);
    }

    /**
     * A Node.js runtime's saved specifier keeps the {@code ^} or {@code ~} of the one it replaces, a prerelease or
     * another specifier pinning the version, from 11.8.0; before, {@code ^} unless the specifier was the version.
     */
    boolean keepsARuntimeSpecifiersRangeOperator() {
        return atLeast(11, 8, 0);
    }

    /**
     * A runtime's version is trimmed, both where engines.runtime gives it (pkg-manifest.utils'
     * convertEnginesRuntimeToDependencies, which skips a version that is no string) and where the node, bun and deno
     * resolvers read a {@code runtime:} specifier, a blank one {@code latest} (normalizeRuntimeSpec), from 11.9.0;
     * before, any truthy version is taken as written ({@code 22} a {@code runtime:22}, an empty one none).
     */
    boolean trimsRuntimeVersions() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/pkg-manifest.utils src/convertEnginesRuntimeToDependencies.ts

    /**
     * A {@code runtimeOnFail} other than {@code download} removes from an engines field's dependencies field only the
     * {@code runtime:} dependencies of the runtimes that engines field names, from 11.15.1
     * (applyRuntimeOnFailOverride); before, those of node, deno and bun alike.
     */
    boolean dropsOnlyTheRuntimesTheEnginesFieldNames() {
        return atLeast(11, 15, 1);
    }

    // @pnpm/config.version-policy src/index.ts

    /**
     * A version policy (minimumReleaseAgeExclude, trustPolicyExclude) merges the exact versions of every rule that
     * matches a name until a rule of the bare name, from 11.9.0; before, the first rule that matches decides
     * ({@code foo@1.0.0, foo@2.0.0} exempts 1.0.0 alone).
     */
    boolean mergesExactVersionsAcrossPolicyRules() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/lockfile.preferred-versions src/index.ts

    /**
     * The preferred versions are seeded without a prototype, so that a lock package named after an Object.prototype
     * member ({@code constructor}) is a package as any other, from 11.10.0; before, pnpm finds that member and throws
     * {@code TypeError: uniqueNameVersions[name].add is not a function}.
     */
    boolean seedsPreferredVersionsWithoutAPrototype() {
        return atLeast(11, 10, 0);
    }

    // @pnpm/config.package-is-installable src/index.ts, inferPlatformFromPackageName.ts

    /**
     * An optional package that does not declare all of os, cpu and libc takes the rest from the tokens of its name
     * ({@code foo-win32-x64} is win32 and x64), from 11.6.0; before, an undeclared one is any.
     */
    boolean infersAnOptionalPackagesPlatformFromItsName() {
        return atLeast(11, 6, 0);
    }

    // @pnpm/workspace.workspace-manifest-reader src/index.ts, versioning.ts

    /**
     * pnpm-workspace.yaml's {@code versioning} must be an object whose fixed, epics, ignore, maxBump, lanes and
     * changelog are well formed ({@code ERR_PNPM_INVALID_WORKSPACE_CONFIGURATION}), from 11.13.0
     * (assertValidWorkspaceManifestVersioning); before, the key is not read.
     */
    boolean validatesTheVersioningSettings() {
        return atLeast(11, 13, 0);
    }

    // @pnpm/workspace.project-manifest-reader src/index.ts

    /**
     * A project's or {@code file:} directory's manifest whose dependency field is {@code null} is read as any other,
     * from 11.3.0; before, the manifest writer's normalize throws {@code Cannot convert undefined or null to object}.
     */
    boolean readsANullDependencyField() {
        return atLeast(11, 3, 0);
    }

    /**
     * The manifest writer, which a recursive install runs over each project it installs once the lock is written,
     * fails on a devDependencies, then dependencies field that is null, an array or no object
     * ({@code ERR_PNPM_INVALID_DEPENDENCIES_FIELD}), from 11.9.0 (readDependenciesField); before, it reads no such
     * field.
     */
    boolean rejectsANonObjectDependenciesFieldOnWrite() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/config.pick-registry-for-package src/index.ts

    /**
     * An {@code npm:} alias whose target is unscoped is fetched from the default registry, from 11.2.1; before, from
     * the registry of the alias's own scope.
     */
    boolean fetchesAnUnscopedAliasTargetFromTheDefaultRegistry() {
        return atLeast(11, 2, 1);
    }

    // @pnpm/resolving.default-resolver src/index.ts

    /**
     * The local resolver's path shapes (a {@code /}, a tarball name) come after the named-registry resolver, so that
     * {@code <alias>:@scope/pkg} reaches its registry, from 11.1.1; before, the local resolver claims them first.
     */
    boolean resolvesNamedRegistrySpecsBeforeLocalPaths() {
        return atLeast(11, 1, 1);
    }

    // @pnpm/resolving.npm-resolver src/pickPackage.ts, pickPackageFromMeta.ts

    /**
     * A packument the run already holds, abbreviated and changed after the release-age cutoff, is fetched again in
     * full, from 11.1.2 (maybeUpgradeAbbreviatedMetaForReleaseAge); before, it is picked from as it is.
     */
    boolean upgradesAHeldPackumentForTheReleaseAge() {
        return atLeast(11, 1, 2);
    }

    /**
     * The packuments a run holds are keyed by registry and name, a filtered one apart, from 11.9.0
     * (getPkgMetaCacheKey); before, by name, so that a name from a second registry takes the first one's packument.
     */
    boolean keysThePackumentCacheByRegistry() {
        return atLeast(11, 9, 0);
    }

    /**
     * Under a release-age cutoff, a resolution that picks the lowest version (resolution-mode lowest-direct or
     * time-based) picks the lowest mature one, from 11.22.0; before, the highest mature one.
     */
    boolean picksTheLowestMatureVersion() {
        return atLeast(11, 22, 0);
    }

    /**
     * A {@code catalog:} specifier pins nothing, its catalog entry's specifier deciding, from 11.8.0
     * (whichVersionIsPinned); before, a catalog name that reads as a version ({@code catalog:react18}) pins.
     */
    boolean takesNoPinFromACatalogSpecifier() {
        return atLeast(11, 8, 0);
    }

    /**
     * A version prefixed with {@code =} ({@code =1.2.3}) pins as the version does, so that a specifier the resolver
     * saves for it, or a peer hoisted as it, keeps it exact, from 11.14.0 (whichVersionIsPinned); before, it pins
     * nothing, and the default pin ({@code ^}) applies.
     */
    boolean pinsAVersionPrefixedWithEqualsAsTheVersion() {
        return atLeast(11, 14, 0);
    }

    /**
     * An abbreviated packument modified exactly at the release-age cutoff counts as mature, from 11.2.0; before, it
     * is fetched in full (and without a time field is {@code ERR_PNPM_MISSING_TIME}).
     */
    boolean takesAPackumentModifiedAtTheCutoffAsMature() {
        return atLeast(11, 2, 0);
    }

    /**
     * The release-age policy is checked as a policy: the lock's resolutions are verified against minimumReleaseAge
     * and trustPolicy before resolving, and an immature pick is a violation the install command handles (loose mode
     * keeps it, minimumReleaseAgeStrict fails after resolution), from 11.1.3 (pnpm/pnpm#10438, #10488, #11687).
     * Before, nothing verifies the lock, and under minimumReleaseAgeStrict the picker finds no version and the
     * resolver throws {@code ERR_PNPM_NO_MATURE_MATCHING_VERSION}, which skips no optional dependency; otherwise it
     * takes the lowest immature version unreported.
     */
    boolean checksTheReleaseAgeAsAPolicy() {
        return atLeast(11, 1, 3);
    }

    // @pnpm/resolving.npm-resolver src/index.ts, parseBareSpecifier.ts

    /**
     * The release age ERR_PNPM_NO_MATURE_MATCHING_VERSION tells is the largest whole unit of years, months of 30 days,
     * days, hours and minutes ({@code 1 day ago}), else {@code a few seconds ago}, and {@code just now} for a date
     * ahead or none, from 11.1.0 (formatTimeAgo, pnpm/pnpm#11532); before, minutes under 90 of them, hours under 48 and
     * days after ({@code 30 hours ago}), {@code just now} under a minute and for a date ahead, and {@code NaN minutes
     * ago} for none.
     */
    boolean tellsAReleaseAgeInItsLargestWholeUnit() {
        return atLeast(11, 1, 0);
    }

    /**
     * An {@code npm:} specifier that is a range alone ({@code foo: npm:^1.0.0}) is a range of the dependency's alias,
     * which the locked version replaces as {@code npm:1.2.3}, from 11.1.0 (pnpm/pnpm#11324, parseBareSpecifier and
     * replaceVersionInBareSpecifier); before, it names a package {@code ^1.0.0} at the default tag, which the registry
     * does not have, and the locked version is appended to it ({@code npm:^1.0.0@1.2.3}).
     */
    boolean takesTheAliasAsTheNameOfAnNpmRange() {
        return atLeast(11, 1, 0);
    }

    /**
     * A jsr package is picked as an npm one is, under the release-age exclusions (minimumReleaseAgeExclude) and, for a
     * dependency that is optional, from the full packument, from 11.1.0 (pickFromSimpleRegistry, pnpm/pnpm#11324);
     * before, an excluded jsr package is held to the cutoff all the same, and an optional one is picked from the
     * abbreviated packument.
     */
    boolean picksAJsrPackageWithItsExclusionsAndOptionality() {
        return atLeast(11, 1, 0);
    }

    // @pnpm/resolving.npm-resolver src/index.ts, parseBareSpecifier.ts, @pnpm/resolving.jsr-specifier-parser
    // src/index.ts

    /**
     * A JSR package name validate-npm-package-name rejects ({@code @s/}, {@code @/x}) is
     * {@code ERR_PNPM_INVALID_JSR_PACKAGE_NAME}, from 11.10.0 (jsrToNpmPackageName's validForOldPackages); before, only
     * one without a slash, the rest named {@code @jsr/<scope>__<name>} as they are.
     */
    boolean validatesAJsrPackageName() {
        return atLeast(11, 10, 0);
    }

    /**
     * A named registry spec's package name validate-npm-package-name rejects, in whatever form the spec gives it, is
     * {@code ERR_PNPM_INVALID_NAMED_REGISTRY_PACKAGE_NAME}, from 11.10.0
     * (parseNamedRegistrySpecifierToRegistryPackageSpec); before, only a scope without a name ({@code gh:@s},
     * {@code gh:@s/}), so that {@code gh:@/bar} resolves.
     */
    boolean validatesANamedRegistryPackageName() {
        return atLeast(11, 10, 0);
    }

    /**
     * Where the registry has no version of a package (its 404, or no version in range) and the workspace's are tried
     * (linkWorkspacePackages), a workspace without a matching version fails the resolution
     * ({@code ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE}), from 11.10.0 (resolveNpm); before, the registry's error
     * is reported.
     */
    boolean reportsTheWorkspaceVersionsTheRegistryLacks() {
        return atLeast(11, 10, 0);
    }

    // @pnpm/resolving.npm-resolver src/trustChecks.ts, createNpmResolutionVerifier.ts

    /**
     * A version's trusted publisher counts as trust evidence only with a provenance attestation, from 11.4.0;
     * before, alone.
     */
    boolean requiresProvenanceOfATrustedPublisher() {
        return atLeast(11, 4, 0);
    }

    /**
     * A staged publish (the version's {@code _npmUser.approver}) is the strongest trust evidence, from 11.5.0
     * (pnpm/pnpm#11887); before, it is none.
     */
    boolean ranksAStagedPublish() {
        return atLeast(11, 5, 0);
    }

    /**
     * The lock's resolutions are verified on every install, and a registry entry's explicit tarball URL must be the
     * one the registry's metadata lists for that version ({@code ERR_PNPM_TARBALL_URL_MISMATCH}), a version with a
     * non-semver dep path not checked, from 11.5.2; before, only under minimumReleaseAge or trustPolicy, and the URL
     * not at all.
     */
    boolean bindsALockedTarballUrlToTheRegistry() {
        return atLeast(11, 5, 2);
    }

    /**
     * A tarball whose URL is neither http nor https ({@code file:}) is no registry resolution to verify, from 11.5.2;
     * before, it is checked against the registry's metadata.
     */
    boolean verifiesOnlyHttpTarballs() {
        return atLeast(11, 5, 2);
    }

    /**
     * Every untyped resolution but a {@code file:}, non-http or git-hosted tarball's is a registry tarball to verify,
     * and one without an integrity fails the lock's verification ({@code ERR_PNPM_MISSING_TARBALL_INTEGRITY}) ahead of
     * a non-semver dep path's pass, a non-semver version or a tarball that is no string a
     * {@code TARBALL_URL_MISMATCH}; pkgSnapshotToResolution fails on such a tarball
     * ({@code ERR_PNPM_INVALID_TARBALL_RESOLUTION}) and no longer on the missing integrity, from 11.9.0; before, a
     * resolution with neither a tarball nor an integrity is none, a non-semver version passes, and from 11.4.0
     * pkgSnapshotToResolution fails on the missing integrity ({@link #requiresALockedTarballsIntegrity}).
     */
    boolean verifiesALockedTarballsIntegrity() {
        return atLeast(11, 9, 0);
    }

    /**
     * A registry fetch error the verification meets (a 404 of the metadata its age, trust or tarball URL check
     * reads) fails the install, from 11.9.0; before, it is a violation the check could not decide (or for the URL
     * check, a URL that could not be verified), but in the abbreviated metadata's "modified" shortcut, which ignores
     * it either way.
     */
    boolean failsVerificationOnARegistryFetchError() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/error src/index.ts (FetchError)

    /**
     * A failed request's message names its URL without the credentials in it ({@code GET https://r.test/foo: …} for
     * {@code https://u:p@r.test/foo}), from 11.9.0 (redactUrlCredentials); before, as requested.
     */
    boolean redactsUrlCredentialsInFetchErrors() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/installing.deps-installer src/install/verifyLockfileResolutions.ts

    /**
     * A lock entry whose dep path is a registry version ({@code name@version}) but whose resolution is no registry
     * tarball (typed, git-hosted, or a tarball URL neither http nor https) fails the lock's verification with
     * {@code ERR_PNPM_RESOLUTION_SHAPE_MISMATCH}, ahead of any verifier and with none active, from 11.5.3; before, it
     * is read as it is.
     */
    boolean rejectsARegistryDepPathWithANonRegistryResolution() {
        return atLeast(11, 5, 3);
    }

    /**
     * A dependency alias of the lock's importers or snapshots that is no package name fails the lock's verification
     * first ({@code ERR_PNPM_INVALID_DEPENDENCY_NAME}), from 11.7.0; before, it is read as it is.
     */
    boolean rejectsAnInvalidDependencyAliasInTheLock() {
        return atLeast(11, 7, 0);
    }

    // @pnpm/installing.deps-installer src/install/index.ts (mutateModules)

    /**
     * The lock's verification runs beside the install, so that a project without a package.json fails with that
     * first, from 11.7.0; before, the verification comes first.
     */
    boolean verifiesTheLockBesideTheInstall() {
        return atLeast(11, 7, 0);
    }

    /**
     * {@code updateChecksums} makes the install resolve in full, from 11.4.0; before, a lock-only install does not
     * read it.
     */
    boolean resolvesInFullToUpdateChecksums() {
        return atLeast(11, 4, 0);
    }

    /**
     * A {@code pnprServer} resolves the install ({@code ERR_PNPM_TRUST_POLICY_INCOMPATIBLE_WITH_PNPR} under trustPolicy
     * no-downgrade), from 11.5.2; before, an {@code agent} does ({@code ..._WITH_AGENT}).
     */
    boolean resolvesThroughAPnprServer() {
        return atLeast(11, 5, 2);
    }

    // @pnpm/installing.commands src/install.ts (dryRunInstall)

    /**
     * A {@code dryRun} setting resolves in full and writes no lock ({@code ERR_PNPM_CONFIG_CONFLICT_DRY_RUN_WITH_PNPR
     * _SERVER} beside a pnpr server), from 11.8.0; before, pnpm install does not read it.
     */
    boolean stopsTheLockWriteOnADryRun() {
        return atLeast(11, 8, 0);
    }

    // @pnpm/installing.env-installer src/resolveOptionalSubdeps.ts, resolveAndInstallConfigDeps.ts,
    // installConfigDeps.ts

    /**
     * A config dependency's optionalDependencies are resolved into the env document and checked, from 11.2.0; before,
     * its snapshot is empty and they are not read.
     */
    boolean resolvesTheOptionalDependenciesOfAConfigDependency() {
        return atLeast(11, 2, 0);
    }

    /**
     * A config dependency's optional dependency's snapshot is {@code optional: true}, from 11.2.1; before, empty.
     */
    boolean marksAConfigDependencysOptionalDependenciesOptional() {
        return atLeast(11, 2, 1);
    }

    /**
     * The env document's config dependencies, and their optional dependencies, must have package names and exact
     * versions ({@code ERR_PNPM_INVALID_DEPENDENCY_NAME}, {@code ERR_PNPM_INVALID_CONFIG_DEP_VERSION}) before each
     * write of the document and each install of them, from 11.8.0 (verifyEnvLockfile); before, nothing checks them.
     */
    boolean verifiesTheEnvDocumentsConfigDependencies() {
        return atLeast(11, 8, 0);
    }

    // @pnpm/installing.deps-resolver src/resolveDependencies.ts, getWantedDependencies.ts,
    // getNonDevWantedDependencies.ts, updateLockfile.ts

    /**
     * The children of one package are taken in the order they are wanted, from 11.3.0 (pnpm/pnpm#8155); before, in
     * the order their resolutions end.
     */
    boolean takesChildrenInTheOrderWanted() {
        return atLeast(11, 3, 0);
    }

    /**
     * A dependency name no npm package can have is {@code ERR_PNPM_INVALID_DEPENDENCY_NAME}, in a project's manifest
     * and a package's, from 11.4.0; before, it is taken as a name.
     */
    boolean rejectsAnInvalidDependencyName() {
        return atLeast(11, 4, 0);
    }

    /**
     * A remote tarball's resolution without an integrity keeps the one its locked entry had for the same tarball, from
     * 11.5.1 (pnpm/pnpm#12067); before, it is written without.
     */
    boolean keepsTheIntegrityOfALockedRemoteTarball() {
        return atLeast(11, 5, 1);
    }

    /**
     * A locked package the install does not update, whose snapshot records no peers, is read from its snapshot under
     * the manifest: {@code hasBin} as the lock records it, no git {@code prepare}, from 11.5.2, whose
     * getInfoFromLockfile names the package's dep path; before, the manifest decides, the lock's {@code hasBin} only
     * where the manifest has no {@code bin}.
     */
    boolean readsALockedPackageFromItsSnapshot() {
        return atLeast(11, 5, 2);
    }

    /**
     * The children of a package several occurrences reach are resolved in the context of the occurrence lowest by
     * depth, importer and the ids of its parents, a lower one that resolves later taking them over from the one
     * resolving them, and no package is requested while a shallower one's resolution is active, from 11.7.0
     * (pnpm/pnpm#12358); before, in the context of the first occurrence to resolve.
     */
    boolean resolvesSharedChildrenInADeterministicContext() {
        return atLeast(11, 7, 0);
    }

    /**
     * A child the lock pins below a version a direct dependency names, which the child's range admits, is resolved
     * again to that version, from 11.8.0 (getDepsToResolve's findHigherDirectDepVersion); before, the pin is kept.
     */
    boolean movesAStalePinUpToADirectVersion() {
        return atLeast(11, 8, 0);
    }

    /**
     * An occurrence of a package that does not resolve its children shares their missing peers only with an owner as
     * deep as it, from 11.9.0 (claimChildrenResolution); before, also with a shallower owner whose children are
     * resolved already, so that a deeper occurrence reports their missing peers to its own parents.
     */
    boolean sharesMissingPeersOfChildrenOnlyWithAnOwnerAsDeep() {
        return atLeast(11, 9, 0);
    }

    /**
     * An optional dependency whose resolution fails (but by a trust downgrade) is not skipped where the lock holds a
     * package of its name, an npm: alias's real one, in its range, the error rethrown with a hint where it has none,
     * from 11.11.0 (wantedLockfileContainsSatisfyingEntry, pnpm/pnpm#12853); before, it is skipped, and its locked
     * entries are left out.
     */
    boolean keepsAFailingOptionalDependencyTheLockResolves() {
        return atLeast(11, 11, 0);
    }

    /**
     * The peers an occurrence of a package reports missing are those of the package as its first occurrence resolved
     * it (peerDependenciesWithoutOwn): without its own dependencies and name, with its optional peers named only in
     * peerDependenciesMeta (range {@code *}), so that autoInstallPeers hoists such a peer the install holds and no
     * longer one that is also an optional dependency, from 11.15.0 (getMissingPeers of resolvedPkg); before, every
     * peer of this occurrence's manifest.
     */
    boolean takesMissingPeersFromTheResolvedPackage() {
        return atLeast(11, 15, 0);
    }

    /**
     * Peers are resolved a second time from the peer contexts the lock records, where their providers are still in
     * the graph, satisfy the ranges and no current manifest choice must win, from 11.5.2 (pnpm/pnpm#12083); before,
     * once, from the current graph alone.
     */
    boolean prefersThePeerContextsTheLockRecords() {
        return atLeast(11, 5, 2);
    }

    // @pnpm/installing.package-requester src/packageRequester.ts

    /**
     * A git-hosted tarball the install fetches for its manifest gives a resolution without an integrity the one it
     * was fetched with, under --lockfile-only too (a store that holds the tarball gives none), from 11.0.7
     * (gitHostedTarballFetcher, the change that marks git-hosted tarballs, {@link #marksGitHostedTarballs}); before,
     * it is written without one.
     */
    boolean pinsTheIntegrityOfAFetchedGitHostedTarball() {
        return atLeast(11, 0, 7);
    }

    /**
     * A resolution the resolver gives without an integrity, for a package it did not update, takes the locked one,
     * from 11.5.0 (pnpm/pnpm#12001); before, it has none.
     */
    boolean keepsALockedIntegrityTheResolverDoesNotGive() {
        return atLeast(11, 5, 0);
    }

    /**
     * A remote tarball whose resolution has no integrity is downloaded to fill it, under --lockfile-only too, from
     * 11.9.0 (the tarball fetcher's resolutionNeedsFetch); before, it is written without one.
     */
    boolean fetchesATarballToFillAMissingIntegrity() {
        return atLeast(11, 9, 0);
    }

    // @pnpm/installing.deps-resolver src/resolvePeers.ts

    /**
     * A cycle of peers one of the pending peers is in counts as cyclic, so that cached path promises do not wait on
     * each other, from 11.5.0 (pnpm/pnpm#11999); before, only a cycle the current package is in.
     */
    boolean cutsCyclesOfPendingPeers() {
        return atLeast(11, 5, 0);
    }

    /**
     * A parent package a node inherits is replaced by its own child's where it was resolved against another version
     * of a peer it shares with that child, from 11.5.1 (pnpm/pnpm#12079); before, only where the two do not match.
     */
    boolean replacesAnInheritedParentThatBreaksAPeerDiamond() {
        return atLeast(11, 5, 1);
    }

    /**
     * Variants of a package with as many dependencies are deduplicated in the order of their dep paths, from 11.5.3
     * (deduplicateDepPaths); before, in the order the variants were made.
     */
    boolean breaksPeerDedupeTiesByDepPath() {
        return atLeast(11, 5, 3);
    }

    /**
     * A peer provider a childless package keeps from the lock goes into a copy of its parents' scope, from 11.7.0;
     * before, into the scope it shares with its siblings, who take it as theirs.
     */
    boolean keepsALockedPeerInItsOwnScope() {
        return atLeast(11, 7, 0);
    }

    /**
     * A package reached again through a cycle, its children cut there, is neither pure nor cached for its later
     * occurrences, from 11.8.0 (pnpm/pnpm#5108); before, it is, and those occurrences lose their transitive peers.
     */
    boolean leavesACycleReentryUncached() {
        return atLeast(11, 8, 0);
    }

    /**
     * An npm-aliased child ({@code ts4: npm:typescript@4}) provides a peer of its real name to its siblings, from
     * 11.8.0; before, only of its alias.
     */
    boolean providesPeersUnderAnAliasedChildsName() {
        return atLeast(11, 8, 0);
    }

    /**
     * The peer providers autoInstallPeers attaches to a project for reuse (peers resolved inside a dependency's own
     * closure) are peer-resolved after the project's own direct dependencies, and only those that walk did not reach,
     * from 11.11.0 (pnpm/pnpm#4993); before, with them at once, so that a provider inside such a closure takes its
     * peers from the project.
     */
    boolean resolvesHoistedPeerProvidersAfterOwnDependencies() {
        return atLeast(11, 11, 0);
    }

    /**
     * A package's directory in the virtual store is safeJoinModulesDir's, which refuses a name
     * validate-npm-package-name rejects ({@code ERR_PNPM_INVALID_DEPENDENCY_NAME}, naming the directory's absolute
     * path), from 11.11.0; before, any name is joined as it is.
     */
    boolean placesAPackageOnlyUnderAValidName() {
        return atLeast(11, 11, 0);
    }

    // @pnpm/installing.deps-resolver src/dedupeInjectedDeps.ts

    /**
     * An injected workspace project whose own project the install did not resolve is a link where its dep path has no
     * peers, children or not, from 11.8.0; before, where it has no children.
     */
    boolean dedupesAnUnresolvedInjectedTargetByItsPeers() {
        return atLeast(11, 8, 0);
    }

    /**
     * An injected workspace project is a link where each of its children is its own project's, or a node of the same
     * package that its own project's child of that alias is compatible with and has as many dependencies as (a peer
     * the injecting project does not give it), from 11.13.0 (getDedupeMap with depPathCompatibility.ts'
     * isCompatibleAndHasMoreDeps); before, only where each child is its own project's.
     */
    boolean dedupesAnInjectedChildTheTargetCovers() {
        return atLeast(11, 13, 0);
    }

    // @pnpm/installing.deps-installer src/install/index.ts (installSome's catalogMode)

    /**
     * A {@code runtime:} dependency is never saved to a catalog under catalogMode strict or prefer, from 11.5.2;
     * before, it is checked against the catalog as any other.
     */
    boolean keepsARuntimeDependencyOutOfTheCatalog() {
        return atLeast(11, 5, 2);
    }

    /**
     * catalogMode compares a wanted specifier with the catalog's as versions only, so a range is a mismatch, from
     * 11.5.2 (pnpm/pnpm#11570); before, a range passes to semver.eq, which throws {@code Invalid Version}.
     */
    boolean comparesCatalogSpecifiersAsVersionsOnly() {
        return atLeast(11, 5, 2);
    }

    /**
     * A missing peer's range, not only an exact version, takes the highest preferred version it satisfies, and where
     * none does, the range itself under autoInstallPeers or nothing, from 11.12.0 (hoistPeers); before, a range takes
     * the highest preferred version of any.
     */
    boolean hoistsAPeerInRange() {
        return atLeast(11, 12, 0);
    }

    /**
     * The workspace root's dependencies a missing peer may be hoisted as are a copy taken before any peer is hoisted,
     * each by the specifier it saves, else its wanted one, with those wanted but not resolved, from 11.12.0
     * (resolveDependencies.ts' getHoistableRootDeps); before, the root's own list as it grows, the peers hoisted into
     * it and the providers attached to it among them, a provider saving no specifier and standing before the root's
     * own dependency on its package where its alias sorts first.
     */
    boolean copiesTheRootDependenciesPeersAreHoistedAs() {
        return atLeast(11, 12, 0);
    }

    /**
     * Nodes whose peer paths wait on each other in a cycle are given their {@code name@version}, from 11.12.0
     * (breakDepPathAwaitCycles); before, pnpm waits for them forever.
     */
    boolean breaksPeerPathAwaitCycles() {
        return atLeast(11, 12, 0);
    }

    // @pnpm/deps.peer-range src/index.ts (ReadPackageHook), as validatePeerDependencies.ts, resolvePeers.ts and
    // hoistPeers.ts read it

    /**
     * A peer spec that is a dependency specifier ({@code npm:react@^18}, {@code <registry>:^1}, {@code file:} and
     * git ones) is no invalid project peer ({@code ERR_PNPM_INVALID_PEER_DEPENDENCY_SPECIFICATION}, whose hint names
     * them), and the range a peer is checked against, a locked one too, and a missing one hoisted by, is the one
     * getPeerVersionRange reads out of the spec, each {@code ||} part apart ({@code workspace:^1} gives {@code ^1},
     * {@code npm:x@^1} {@code ^1}, anything else any), from 11.14.0; before, the spec less a leading
     * {@code workspace:}, a hoisted peer's as it is.
     */
    boolean readsAPeerRangeOutOfItsSpecifier() {
        return atLeast(11, 14, 0);
    }

    // @pnpm/installing.deps-resolver src/hoistPeers.ts (ResolveDependencies.LOCKED_OPTIONAL_PEER_CANDIDATES)

    /**
     * A version only the lock holds is a candidate for a missing optional peer, from 11.5.1 (pnpm/pnpm#12075). The
     * resolver asks it of the release or range the install runs as through
     * {@link ResolveDependencies#LOCKED_OPTIONAL_PEER_CANDIDATES}, computing both sides of a range that straddles it.
     */
    boolean countsLockedVersionsAsOptionalPeerCandidates() {
        return atLeast(11, 5, 1);
    }

    // yaml, whose document @pnpm/workspace.workspace-manifest-writer writes pnpm-workspace.yaml as
    // (WorkspaceManifestWriter, internal.lock.YamlDocument)

    /**
     * updateWorkspaceManifest writes a negative number with the fraction digits it was read with ({@code -1.50}), and
     * one whose form has an exponent as that form ({@code 0.00000010} as {@code 1e-7}), from 11.0.7 (yaml 2.8.4's
     * stringifyNumber, 2.9.0's from 11.2.0); before, yaml 2.8.3 pads only a number that starts with a digit, after an
     * exponent too ({@code -1.5}, {@code 1e-7.00000000}).
     */
    boolean padsTheFractionDigitsOfANegativeNumber() {
        return atLeast(11, 0, 7);
    }

    // semver

    /**
     * The node-semver pnpm 11 bundles: 7.8.5 from 11.9.0, whose {@code ~1.2} reaches down to {@code 1.2.0-0} under
     * {@code includePrerelease}; 7.8.4 from 11.7.0, which takes an x-range with a number after a wildcard
     * ({@code 1.x.3}) for no range and leaves a caret range below 1.0.0 without the {@code -0} includePrerelease adds;
     * 7.8.1 from 11.3.0, which strips build metadata from a whole range; 7.8.0 from 11.2.0, and before, 7.7.4, which
     * answers as 7.8.0 does (7.8.0 adds only {@code truncate}).
     */
    NodeSemver.Release semver() {
        return atLeast(11, 9, 0) ? NodeSemver.Release.V7_8_5 :
                atLeast(11, 7, 0) ? NodeSemver.Release.V7_8_4 :
                        atLeast(11, 3, 0) ? NodeSemver.Release.V7_8_1 : NodeSemver.Release.V7_8_0;
    }

    /**
     * What {@code answer} gives under the node-semver this release bundles ({@link #semver()}): where this is one of
     * several releases, their node-semvers' answers where they agree, the question where they do not.
     */
    <T> T underSemver(Function<NodeSemver.Release, T> answer) {
        if (releases.size() == 1) {
            return answer.apply(semver());
        }
        Set<NodeSemver.Release> asked = EnumSet.noneOf(NodeSemver.Release.class);
        Answer<T> first = null;
        for (int[] release : releases) {
            NodeSemver.Release semver = new PnpmRelease(name, Collections.singletonList(release)).semver();
            if (!asked.add(semver)) {
                continue;
            }
            Answer<T> next = Answer.of(() -> answer.apply(semver));
            if (first == null) {
                first = next;
            } else if (!first.same(next)) {
                return answer.apply(semver());
            }
        }
        return Objects.requireNonNull(first).get();
    }

    // a value or the error that came instead
    private static final class Answer<T> {
        private final @Nullable T value;
        private final @Nullable RuntimeException error;

        private Answer(@Nullable T value, @Nullable RuntimeException error) {
            this.value = value;
            this.error = error;
        }

        static <T> Answer<T> of(Supplier<T> compute) {
            try {
                return new Answer<>(compute.get(), null);
            } catch (RuntimeException e) {
                return new Answer<>(null, e);
            }
        }

        boolean same(Answer<T> other) {
            if (error != null || other.error != null) {
                return error != null && other.error != null && error.getClass() == other.error.getClass() &&
                       Objects.equals(error.getMessage(), other.error.getMessage());
            }
            return Objects.equals(value, other.value);
        }

        T get() {
            if (error != null) {
                throw error;
            }
            //noinspection DataFlowIssue
            return value;
        }
    }

    /**
     * The release's version, where pnpm names its own (its {@code packageManager.version}).
     *
     * @throws CannotKnow where this is one of several releases
     */
    public String version() {
        if (releases.size() > 1) {
            throw new CannotKnow("the pnpm release that installs the project, one of " + name + ", which pnpm names " +
                                 "here");
        }
        int[] release = releases.get(0);
        return release[0] + "." + release[1] + "." + release[2];
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PnpmRelease && name.equals(((PnpmRelease) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * The version, or the range of releases it may be.
     */
    @Override
    public String toString() {
        return name;
    }
}
