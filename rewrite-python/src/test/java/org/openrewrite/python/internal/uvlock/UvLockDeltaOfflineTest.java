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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.LockFileRegeneration.Reason;
import org.openrewrite.python.internal.LockFileRegeneration.Result;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline replay of the greedy-forward delta resolver (ADR 0010 T2 adds and T1 cascades)
 * against tiny packages so CI has a network-free regression test. Each scenario's index and
 * metadata responses under its {@code /uvlock/offN-.../http/} directory were recorded from
 * pypi.org while the engine's output was verified byte-identical to real {@code uv lock} 0.10.11; the
 * network-hitting counterparts live in {@link UvLockCascadeLiveTest}.
 */
class UvLockDeltaOfflineTest {

    RoutedHttp http;
    ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        http = new RoutedHttp();
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));
    }

    /** T2: adding a leaf package (iniconfig, no dependencies) inserts one entry and a root edge. */
    @Test
    void addLeafMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/iniconfig/", "off1-add-leaf/http/iniconfig-listing-json");
        http.route("https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl.metadata",
          "off1-add-leaf/http/iniconfig-2.3.0-py3-none-any.whl.metadata");
        assertScenario("off1-add-leaf");
    }

    /** T2: adding python-dateutil resolves it and its transitive closure (six). */
    @Test
    void addClosureMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/python-dateutil/", "off2-add-closure/http/python-dateutil-listing-json");
        http.route("https://pypi.org/simple/six/", "off2-add-closure/http/six-listing-json");
        http.route("https://files.pythonhosted.org/packages/ec/57/56b9bcc3c9c6a792fcbaf139543cee77261f3651ca9da0c93f5c1221264b/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/six-1.17.0-py2.py3-none-any.whl.metadata");
        assertScenario("off2-add-closure");
    }

    /** T1: bumping flake8 6.0.0 -> 6.1.0 forces its lockstep pins pycodestyle and pyflakes to move. */
    @Test
    void cascadeMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/flake8/", "off3-cascade-flake8/http/flake8-listing-json");
        http.route("https://pypi.org/simple/pycodestyle/", "off3-cascade-flake8/http/pycodestyle-listing-json");
        http.route("https://pypi.org/simple/pyflakes/", "off3-cascade-flake8/http/pyflakes-listing-json");
        http.route("https://files.pythonhosted.org/packages/b0/24/bbf7175ffc47cb3d3e1eb523ddb23272968359dfcf2e1294707a2bf12fc4/flake8-6.1.0-py2.py3-none-any.whl.metadata",
          "off3-cascade-flake8/http/flake8-6.1.0-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/b1/90/a998c550d0ddd07e38605bb5c455d00fcc177a800ff9cc3dafdcb3dd7b56/pycodestyle-2.11.1-py2.py3-none-any.whl.metadata",
          "off3-cascade-flake8/http/pycodestyle-2.11.1-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/00/e9/1e1fd7fae559bfd07704991e9a59dd1349b72423c904256c073ce88a9940/pyflakes-3.1.0-py2.py3-none-any.whl.metadata",
          "off3-cascade-flake8/http/pyflakes-3.1.0-py2.py3-none-any.whl.metadata");
        assertScenario("off3-cascade-flake8");
    }

    /**
     * Increment 3: adding a leaf gated on a non-version marker records the marker on the root
     * edge and requires-dist only; the [[package]] entry is identical to an unmarkered add, so
     * off1's iniconfig fixtures serve both.
     */
    @Test
    void addMarkeredLeafMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/iniconfig/", "off1-add-leaf/http/iniconfig-listing-json");
        http.route("https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl.metadata",
          "off1-add-leaf/http/iniconfig-2.3.0-py3-none-any.whl.metadata");
        assertScenario("off4-add-markered");
    }

    /**
     * Increment 3: the marker on a markered add stays on the root edge only; the pulled-in
     * closure (python-dateutil -> six, all unconditional) is byte-identical to the unmarkered
     * closure add, so off2's fixtures serve both.
     */
    @Test
    void addMarkeredClosureMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/python-dateutil/", "off2-add-closure/http/python-dateutil-listing-json");
        http.route("https://pypi.org/simple/six/", "off2-add-closure/http/six-listing-json");
        http.route("https://files.pythonhosted.org/packages/ec/57/56b9bcc3c9c6a792fcbaf139543cee77261f3651ca9da0c93f5c1221264b/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/python_dateutil-2.9.0.post0-py2.py3-none-any.whl.metadata");
        http.route("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          "off2-add-closure/http/six-1.17.0-py2.py3-none-any.whl.metadata");
        assertScenario("off5-add-markered-closure");
    }

    /**
     * uv rewrites {@code platform_system == 'Windows'} to {@code sys_platform == 'win32'} on both
     * the root edge and requires-dist; the engine reproduces that normalization (off1's iniconfig
     * fixtures serve the unchanged [[package]] entry).
     */
    @Test
    void addMarkeredPlatformSystemMatchesRealUvByteForByte() {
        http.route("https://pypi.org/simple/iniconfig/", "off1-add-leaf/http/iniconfig-listing-json");
        http.route("https://files.pythonhosted.org/packages/cb/b1/3846dd7f199d53cb17f49cba7e651e9ce294d8497c8c150530ed11865bb8/iniconfig-2.3.0-py3-none-any.whl.metadata",
          "off1-add-leaf/http/iniconfig-2.3.0-py3-none-any.whl.metadata");
        assertScenario("off7-add-markered-platform");
    }

    /**
     * A dependent's requirement on the cascade target only constrains it when its marker can
     * fire under requires-python: botolike's {@code urlish<2.1; python_version < '3.10'} is dead
     * under {@code >=3.10}, so uv moves urlish to 2.5.0, not the 2.0.0 the dead bound would allow.
     */
    @Test
    void cascadeIgnoresConstraintsUnderDeadMarkers() {
        routeLocalIndex("off10-dead-marker-cascade");
        assertScenario("off10-dead-marker-cascade");
    }

    /**
     * An extra-gated requirement ({@code urlish<2; extra == 'old'}) constrains the cascade target
     * only when some edge activates that extra on the dependent; here none does, so uv moves urlish
     * to 2.5.0 where the inactive bound would have made the cascade unsatisfiable.
     */
    @Test
    void cascadeIgnoresConstraintsUnderInactiveExtras() {
        routeLocalIndex("off12-inactive-extra-constraint");
        assertScenario("off12-inactive-extra-constraint");
    }

    /**
     * A {@code [dependency-groups]} declaration bounds a cascade target like any other root
     * requirement: {@code dev = ["urlish<2.4"]} keeps uv at 2.0.0 although 2.5.0 is listed.
     */
    @Test
    void cascadeHonorsDependencyGroupConstraints() {
        routeLocalIndex("off13-dev-group-constraint");
        assertScenario("off13-dev-group-constraint");
    }

    @Test
    void cascadeIgnoresConstraintsUnderExtrasTheSameEditDeactivates() {
        // dropping reqlike[socks] while keeper still needs reqlike releases its urlish<2 bound, so uv picks 2.5
        routeLocalIndex("off18-removed-extra-unbinds-cascade");
        assertScenario("off18-removed-extra-unbinds-cascade");
    }

    /**
     * Removing {@code reqlike[socks]} while {@code other} still depends on reqlike keeps the
     * package but drops its now-unactivated {@code socks} optional-dependencies group and the
     * socklike entry only that group reached.
     */
    @Test
    void removingExtrasDeclarationDropsOrphanedExtraGroup() {
        Result result = UvLockEngine.regenerate(
          resource("off11-extras-removal-keeps-package/pyproject.toml"),
          resource("off11-extras-removal-keeps-package/uv.lock.before"),
          ctx);
        assertThat(result.getFailure())
          .withFailMessage("engine failed: %s", result.getErrorMessage())
          .isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("off11-extras-removal-keeps-package/uv.lock.after"));
        // the recorded graph decides a removal; nothing is fetched
        assertThat(http.requests).isEmpty();
    }

    /**
     * Scenarios recorded against a local PEP 503 index (see the fixture README): the manifest
     * declares {@code https://index.example/simple/} as its default index, and every simple page
     * and metadata sidecar under the scenario's {@code http/} directory is served by name.
     */
    private void routeLocalIndex(String scenario) {
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("local", "https://index.example/simple/", true, null, null, false)));
        http.routeDirectory("https://index.example/simple/", scenario + "/http/");
    }

    /**
     * Upgrading {@code reqlike[socks]} to a version that no longer provides the extra makes uv
     * drop the extra from the root edge, the optional-dependencies group and its closure;
     * rebuilding groups is outside the engine, so it fails loud instead of keeping them.
     */
    @Test
    void upgradeToVersionWithoutActivatedExtraFailsLoud() {
        routeLocalIndex("off14-upgrade-drops-extra");
        assertFailsLoud("off14-upgrade-drops-extra", "reqlike");
    }

    @Test
    void upgradeRebuildsActivatedExtraGroupAndCascadesThroughIt() {
        // alpha[c] 2.0 needs alphac==2.0, which needs alpha==2.0: uv moves both and rebuilds group c
        routeLocalIndex("off19-extra-group-lockstep-upgrade");
        assertScenario("off19-extra-group-lockstep-upgrade");
    }

    @Test
    void transitiveAddWithExtrasBuildsOnlyItsActivatedGroups() {
        // beta pulls in gamma[fast]: group fast and delta appear, the unactivated slow extra does not
        routeLocalIndex("off20-transitive-add-with-extras");
        assertScenario("off20-transitive-add-with-extras");
        // the slow extra's closure (epsilon) is not looked up
        assertThat(http.requests).noneMatch(url -> url.contains("epsilon"));
    }

    @Test
    void upgradeActivatingAnExtraOnLockedPackageAddsItsGroup() {
        // reqlike 2.0 asks for urlish[socks]: urlish keeps its version and gains the socks group
        routeLocalIndex("off21-upgrade-activates-extra");
        assertScenario("off21-upgrade-activates-extra");
    }

    @Test
    void extraDisjunctionLandsInEachActivatedGroup() {
        // zeta; extra == 'fast' or extra == 'slow' joins group fast next to the plain extra == 'fast' entry
        routeLocalIndex("off22-or-extras-marker");
        assertScenario("off22-or-extras-marker");
    }

    @Test
    void parenthesizedExtraClausesAndUnactivatedDisjunctsRegenerate() {
        // (sys_platform == 'win32') and extra == 'fast' keeps its platform clause; the all-gated DNF drops
        routeLocalIndex("off23-extra-dnf-markers");
        assertScenario("off23-extra-dnf-markers");
        assertThat(http.requests).noneMatch(url -> url.contains("epsilon"));
    }

    @Test
    void extraOrEnvironmentMarkerFailsLoud() {
        // uv splits this marker into a base edge and a group entry with complementary platform markers
        routeLocalIndex("off24-extra-or-platform-marker");
        assertFailsLoud("off24-extra-or-platform-marker", "gamma");
    }

    @Test
    void directAddWithExtrasBuildsTheActivatedGroup() {
        routeLocalIndex("off25-add-with-extras");
        assertScenario("off25-add-with-extras");
    }

    @Test
    void addingExtrasToDeclarationAddsGroupAndRootEdgeExtra() {
        routeLocalIndex("off26-declared-extras-added");
        assertScenario("off26-declared-extras-added");
    }

    @Test
    void removingExtrasFromDeclarationDropsGroupAndClosureWithoutNetwork() {
        routeLocalIndex("off27-declared-extras-removed");
        Result result = UvLockEngine.regenerate(
          resource("off27-declared-extras-removed/pyproject.toml"),
          resource("off27-declared-extras-removed/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("off27-declared-extras-removed/uv.lock.after"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void addWithPinnedIndexResolvesFromThatIndexOnly() {
        // iniconfig 0.9 sits on the default index and 1.0 on alt; the [tool.uv.sources] pin selects alt
        routeLocalIndex("off28-add-pinned-index");
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("local", "https://index.example/simple/", true, null, null, false),
          new PythonPackageIndex("alt", "https://index.example/alt/simple/", true, null, null, false)));
        http.routeDirectory("https://index.example/alt/simple/", "off28-add-pinned-index/http-alt/");
        assertScenario("off28-add-pinned-index");
        assertThat(http.requests).noneMatch(url -> url.equals("https://index.example/simple/iniconfig/"));
    }

    @Test
    void constraintDependencyMovesTransitiveAndRecordsManifest() {
        // the UpgradeTransitiveDependencyVersion edit: [tool.uv] constraint-dependencies = ["urlish==2.5"]
        routeLocalIndex("off29-transitive-constraint");
        assertScenario("off29-transitive-constraint");
    }

    @Test
    void constraintsAreRecordedSortedAndBoundKeptPins() {
        // reqlike<1.5 keeps reqlike at its pin although 1.5 is listed; urlish>=2 moves urlish
        routeLocalIndex("off30-two-constraints");
        assertScenario("off30-two-constraints");
    }

    @Test
    void constraintOnPackageAbsentFromLockLeavesItUntouched() {
        routeLocalIndex("off29-transitive-constraint");
        String pyproject = resource("off29-transitive-constraint/pyproject.toml")
          .replace("constraint-dependencies = [\"urlish==2.5\"]", "constraint-dependencies = [\"nothere>=3\"]");
        Result result = UvLockEngine.regenerate(pyproject, resource("off29-transitive-constraint/uv.lock.before"), ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("off29-transitive-constraint/uv.lock.before"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void declarationConflictingWithRecordedConstraintFailsAsConflict() {
        // uv refuses reqlike>=1.5 against the recorded reqlike<1.5, so the listed 1.5 must not be picked
        routeLocalIndex("off31-declaration-vs-constraint-conflict");
        Result result = UvLockEngine.regenerate(
          resource("off31-declaration-vs-constraint-conflict/pyproject.toml"),
          resource("off31-declaration-vs-constraint-conflict/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_CONFLICT);
        assertThat(result.getFailure().getPackageName()).isEqualTo("reqlike");
    }

    @Test
    void upgradePastADependentsBoundFailsAsConflict() {
        // other==1.0 requires reqlike<1.5, so uv calls reqlike==1.5 unsatisfiable rather than moving it
        routeLocalIndex("off32-upgrade-past-dependent-bound");
        Result result = UvLockEngine.regenerate(
          resource("off32-upgrade-past-dependent-bound/pyproject.toml"),
          resource("off32-upgrade-past-dependent-bound/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("other 1.0 requires reqlike<1.5");
    }

    @Test
    void rangeUpgradeLandsUnderTheDependentsBound() {
        // other==1.0 requires reqlike<1.5, so reqlike>=1.2 resolves to 1.4 although 2.0 is listed
        routeLocalIndex("off33-dependent-bound-shapes-selection");
        assertScenario("off33-dependent-bound-shapes-selection");
    }

    @Test
    void excludeNewerPackageAndBuildConstraintsPassThroughAnUnrelatedRemoval() {
        Result result = UvLockEngine.regenerate(
          resource("off34-exclude-newer-package-and-build-constraints/pyproject.toml"),
          resource("off34-exclude-newer-package-and-build-constraints/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("off34-exclude-newer-package-and-build-constraints/uv.lock.after"));
        assertThat(http.requests).isEmpty();
    }

    @Test
    void selectingAVersionOfAnExcludeNewerPackageFailsLoud() {
        String pyproject = resource("off34-exclude-newer-package-and-build-constraints/pyproject.toml")
          .replace("six==1.17.0", "six==1.16.0");
        Result result = UvLockEngine.regenerate(pyproject,
          resource("off34-exclude-newer-package-and-build-constraints/uv.lock.before"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getDetail()).contains("exclude-newer-package");
        assertThat(http.requests).isEmpty();
    }

    @Test
    void exactMinorRequiresPythonDropsWheelsTaggedOutsideIt() {
        // ==3.12.* keeps cp312, the abi3 and py3 wheels, and drops cp311 and cp313
        routeLocalIndex("off35-exact-minor-wheel-filter");
        assertScenario("off35-exact-minor-wheel-filter");
    }

    @Test
    void exactMinorRequiresPythonDropsMarkersItDecides() {
        // under ==3.12.*, python_version < '3.14' is always true and python_version < '3.12' never fires
        routeLocalIndex("off36-exact-minor-marker-drop");
        assertScenario("off36-exact-minor-marker-drop");
    }

    @Test
    void anyChangeDropsEdgeMarkersTheParentsReachabilityImplies() {
        // a uv 0.10.11 lock: 0.13.4 drops caioish's fork markers and zippish's inherited python bound on any relock
        PythonExecutionContextView.view(ctx).setUvVersion("0.13.4");
        routeLocalIndex("off37-parent-reachability-markers");
        assertScenario("off37-parent-reachability-markers");
    }

    @Test
    void markerVersionsRecordAsUvRendersThem() {
        // prerelease and trailing-zero segments go, python_version <= '3.11' becomes python_full_version < '3.12'
        routeLocalIndex("off38-marker-version-forms");
        assertScenario("off38-marker-version-forms");
    }

    @Test
    void markeredConstraintFailsLoud() {
        routeLocalIndex("off29-transitive-constraint");
        String pyproject = resource("off29-transitive-constraint/pyproject.toml")
          .replace("constraint-dependencies = [\"urlish==2.5\"]", "constraint-dependencies = [\"urlish==2.5; sys_platform == 'win32'\"]");
        Result result = UvLockEngine.regenerate(pyproject, resource("off29-transitive-constraint/uv.lock.before"), ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo("urlish");
    }

    @Test
    void configuredUvReleaseStampsChangedLocksWithItsRevision() {
        PythonExecutionContextView.view(ctx).setUvVersion("0.13.4");
        routeLocalIndex("off18-removed-extra-unbinds-cascade");
        Result result = UvLockEngine.regenerate(
          resource("off18-removed-extra-unbinds-cascade/pyproject.toml"),
          resource("off18-removed-extra-unbinds-cascade/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("off18-removed-extra-unbinds-cascade/uv.lock.after.uv-0.13.4"));
    }

    @Test
    void configuredUvReleaseBelow0_12_22RestampsAtRevision3() {
        PythonExecutionContextView.view(ctx).setUvVersion("0.12.21");
        routeLocalIndex("off21-upgrade-activates-extra");
        Result result = UvLockEngine.regenerate(
          resource("off21-upgrade-activates-extra/pyproject.toml"),
          resource("off21-upgrade-activates-extra/uv.lock.before"),
          ctx);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource("off21-upgrade-activates-extra/uv.lock.after.uv-0.12.21"));
    }

    @Test
    void configuredUvReleaseLeavesUnchangedLockAlone() {
        PythonExecutionContextView.view(ctx).setUvVersion("0.13.4");
        assertUnchangedWithoutNetwork("off16-tool-uv-dev-dependencies");
    }

    @Test
    void upgradeRaisingPythonLowerBoundOnAnEdgeFailsLoud() {
        // uv forks at python_full_version >= '3.11' although every package resolves to one version
        routeLocalIndex("off15-python-lower-bound-marker");
        assertFailsLoud("off15-python-lower-bound-marker", "reqlike");
    }

    @Test
    void upgradeBoundingPythonFromAboveOnAnEdgeRegenerates() {
        // a patch-level upper bound (python_full_version < '3.10.2') does not fork
        routeLocalIndex("off17-python-upper-bound-marker");
        assertScenario("off17-python-upper-bound-marker");
    }

    /**
     * uv folds the legacy {@code [tool.uv] dev-dependencies} list into the {@code dev} group
     * (its lock is identical to the same list under {@code [dependency-groups]}), so a manifest
     * declaring groups that way is not a removal of the group.
     */
    @Test
    void toolUvDevDependenciesAreTheDevGroup() {
        assertUnchangedWithoutNetwork("off16-tool-uv-dev-dependencies");
    }

    /** {@code requires-python = "~=3.10"} denotes the recorded {@code >=3.10, <4}; the header stays. */
    @Test
    void equivalentRequiresPythonSpellingKeepsHeader() {
        assertUnchangedWithoutNetwork("z3-compatible-release-python");
    }

    /** A root with {@code dynamic = ["version"]} is locked without a version key. */
    @Test
    void dynamicVersionRootRegenerates() {
        assertUnchangedWithoutNetwork("z2-dynamic-version-root");
    }

    /** The manifest already matches the lock: the engine returns it byte-identical and fetches nothing. */
    private void assertUnchangedWithoutNetwork(String scenario) {
        Result result = UvLockEngine.regenerate(
          resource(scenario + "/pyproject.toml"),
          resource(scenario + "/uv.lock"),
          ctx);
        assertThat(result.getFailure())
          .withFailMessage("engine failed: %s", result.getErrorMessage())
          .isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "/uv.lock"));
        assertThat(http.requests).isEmpty();
    }

    private void assertFailsLoud(String scenario, String packageName) {
        Result result = UvLockEngine.regenerate(
          resource(scenario + "/pyproject.toml"),
          resource(scenario + "/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(result.getFailure().getPackageName()).isEqualTo(packageName);
    }

    /**
     * A python-version-gated add makes real uv record lock-level resolution-markers (a fork
     * boundary at 3.11) even though iniconfig resolves to one version; that is marker-space
     * resolution, so the engine fails loud before any network fetch.
     */
    @Test
    void markeredVersionAddFailsLoud() {
        Result result = UvLockEngine.regenerate(
          resource("off6-add-markered-version/pyproject.toml"),
          resource("off6-add-markered-version/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    /**
     * A lock restricted to a subset of environments ({@code [tool.uv] environments}, recorded as
     * {@code supported-markers}) is resolved per environment: uv drops edges gated on an unsupported
     * platform (this base lock's linux-only supported-markers already dropped click's win32-only
     * {@code colorama}). Resolving a new dependency into it needs marker-space resolution, so the
     * engine fails loud before any network fetch -- even when the manifest itself does not repeat the
     * {@code [tool.uv]} block (environments can come from {@code uv.toml} or the CLI).
     */
    @Test
    void restrictedEnvironmentsLockFailsLoud() {
        Result result = UvLockEngine.regenerate(
          resource("off8-restricted-environments/pyproject.toml"),
          resource("off8-restricted-environments/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    /**
     * A manifest declaring {@code [tool.uv] environments} restricts resolution to those environments;
     * the engine rejects any resolution under it (here an unforked base lock, so the manifest is the
     * only signal) rather than resolve universally and emit a lock uv would not.
     */
    @Test
    void manifestEnvironmentsFailsLoud() {
        Result result = UvLockEngine.regenerate(
          resource("off9-manifest-environments/pyproject.toml"),
          resource("off1-add-leaf/uv.lock.before"),
          ctx);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getReason()).isEqualTo(Reason.RESOLUTION_REQUIRED);
        assertThat(http.requests).isEmpty();
    }

    private void assertScenario(String scenario) {
        Result result = UvLockEngine.regenerate(
          resource(scenario + "/pyproject.toml"),
          resource(scenario + "/uv.lock.before"),
          ctx);
        assertThat(result.getFailure())
          .withFailMessage("engine failed: %s", result.getErrorMessage())
          .isNull();
        assertThat(result.getLockFileContent()).isEqualTo(resource(scenario + "/uv.lock.after"));
        // every URL the engine hit was recorded (returned 200); nothing fell through to the network
        assertThat(http.requests).isNotEmpty();
        assertThat(http.routes.keySet()).containsAll(http.requests);
    }

    static String resource(String name) {
        return new String(resourceBytes(name), StandardCharsets.UTF_8);
    }

    static byte[] resourceBytes(String name) {
        try (InputStream is = UvLockDeltaOfflineTest.class.getResourceAsStream("/uvlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Serves recorded responses by exact URL; unrouted URLs return 404 and are recorded. */
    static final class RoutedHttp implements HttpSender {
        final Map<String, byte[]> routes = new LinkedHashMap<>();
        final List<String> requests = new ArrayList<>();

        void route(String url, String fixtureName) {
            routes.put(url, resourceBytes(fixtureName));
        }

        /**
         * Serves a whole simple index from a fixture directory: {@code <prefix><pkg>/} maps to
         * {@code <dir><pkg>-listing-html} and {@code <prefix><pkg>/<file>} to {@code <dir><file>}.
         */
        void routeDirectory(String urlPrefix, String fixtureDir) {
            directories.put(urlPrefix, fixtureDir);
        }

        private final Map<String, String> directories = new LinkedHashMap<>();

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            requests.add(url);
            byte[] body = routes.get(url);
            if (body == null) {
                body = fromDirectory(url);
                if (body != null) {
                    routes.put(url, body);
                }
            }
            if (body == null) {
                return new Response(404, new ByteArrayInputStream(new byte[0]), () -> {
                });
            }
            return new Response(200, new ByteArrayInputStream(body), () -> {
            });
        }

        private byte @Nullable [] fromDirectory(String url) {
            for (Map.Entry<String, String> directory : directories.entrySet()) {
                if (!url.startsWith(directory.getKey())) {
                    continue;
                }
                String rest = url.substring(directory.getKey().length());
                String fixture = rest.endsWith("/") ?
                  rest.substring(0, rest.length() - 1) + "-listing-html" :
                  rest.substring(rest.lastIndexOf('/') + 1);
                try (InputStream is = UvLockDeltaOfflineTest.class.getResourceAsStream("/uvlock/" + directory.getValue() + fixture)) {
                    return is == null ? null : is.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return null;
        }
    }
}
