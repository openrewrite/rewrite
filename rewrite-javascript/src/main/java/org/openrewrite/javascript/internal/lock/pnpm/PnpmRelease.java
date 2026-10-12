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

import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // @pnpm/lockfile.fs src/yamlDocuments.ts

    /**
     * A lock with CRLF line ends reads its env document and its main document apart, from 11.1.3
     * (pnpm/pnpm#11612); before, the two documents are read as one, which no YAML reader accepts.
     */
    boolean readsACrLfLockWithAnEnvDocument() {
        return atLeast(11, 1, 3);
    }

    // @pnpm/config.reader src/index.ts, env.ts, getOptionsFromRootManifest.ts

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
     * {@code trustLockfile} skips the verification of the lock's resolutions, from 11.3.0; before, it is no setting.
     */
    boolean readsTrustLockfile() {
        return atLeast(11, 3, 0);
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

    // pnpm/src main.ts, syncEnvLockfile.ts

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
     * Peers are resolved a second time from the peer contexts the lock records, where their providers are still in
     * the graph, satisfy the ranges and no current manifest choice must win, from 11.5.2 (pnpm/pnpm#12083); before,
     * once, from the current graph alone.
     */
    boolean prefersThePeerContextsTheLockRecords() {
        return atLeast(11, 5, 2);
    }

    // @pnpm/installing.package-requester src/packageRequester.ts

    /**
     * A resolution the resolver gives without an integrity, for a package it did not update, takes the locked one,
     * from 11.5.0 (pnpm/pnpm#12001); before, it has none.
     */
    boolean keepsALockedIntegrityTheResolverDoesNotGive() {
        return atLeast(11, 5, 0);
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
     * none does, the range itself under autoInstallPeers or nothing; the workspace root's dependencies it may be
     * hoisted as include those wanted but not resolved, by their wanted specifier, from 11.12.0; before, a range takes
     * the highest preferred version of any.
     */
    boolean hoistsAPeerInRange() {
        return atLeast(11, 12, 0);
    }

    /**
     * Nodes whose peer paths wait on each other in a cycle are given their {@code name@version}, from 11.12.0
     * (breakDepPathAwaitCycles); before, pnpm waits for them forever.
     */
    boolean breaksPeerPathAwaitCycles() {
        return atLeast(11, 12, 0);
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
