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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * installing/deps-installer/test/install/verifyLockfileResolutions.ts of pnpm 11.2.2; its two cache cases are not
 * ported (the verification cache in pnpm's cache directory is the host's filesystem).
 */
class VerifyLockfileResolutionsTest {

    private static final ResolutionVerifier OK_VERIFIER = (resolution, name, version) -> ResolutionVerifier.Result.OK;

    private static LockfileObject makeLockfile(Map<String, @Nullable Object> resolutions) {
        LockfileObject lockfile = new LockfileObject();
        lockfile.set("lockfileVersion", "9.0");
        Map<String, PackageSnapshot> packages = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : resolutions.entrySet()) {
            PackageSnapshot snapshot = new PackageSnapshot();
            @SuppressWarnings("unchecked")
            Map<String, @Nullable Object> resolution = (Map<String, @Nullable Object>) entry.getValue();
            snapshot.setResolution(resolution);
            packages.put(entry.getKey(), snapshot);
        }
        lockfile.setPackages(packages);
        return lockfile;
    }

    private static Map<String, @Nullable Object> tarballResolution(String integrity) {
        return obj("{'integrity':'" + integrity + "','tarball':''}");
    }

    private static ResolutionVerifier failing(String code, String reason) {
        return (resolution, name, version) -> new ResolutionVerifier.Result(false, code, reason);
    }

    @Test
    void noOpWhenTheVerifierListIsEmpty() {
        LockfileObject lockfile = makeLockfile(Collections.singletonMap("fresh@1.0.0",
          tarballResolution("sha512-deadbeef")));
        VerifyLockfileResolutions.verifyLockfileResolutions(lockfile, Collections.emptyList());
    }

    @Test
    void noOpWhenLockfileHasNoPackages() {
        VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(new LinkedHashMap<>()),
          singletonList(OK_VERIFIER));
    }

    @Test
    void passesWhenEveryEntryIsVerifiedOk() {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("lodash@4.17.21", tarballResolution("sha512-deadbeef"));
        packages.put("is-odd@0.1.0", tarballResolution("sha512-deadbeef"));
        VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages), singletonList(OK_VERIFIER));
    }

    @Test
    void throwsWithTheVerifierSuppliedCodeAndReasonOnASingleFailure() {
        LockfileObject lockfile = makeLockfile(Collections.singletonMap("is-odd@0.1.2",
          tarballResolution("sha512-deadbeef")));
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(lockfile,
          singletonList(failing("MINIMUM_RELEASE_AGE_VIOLATION", "was published yesterday"))))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION");
              assertThat(e.getMessage()).containsPattern("is-odd@0\\.1\\.2 was published yesterday");
          });
    }

    @Test
    void throwsAGenericCodeWithPerEntryCodesInTheBreakdownWhenViolationsSpanPolicies() {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("is-odd@0.1.2", tarballResolution("sha512-a"));
        packages.put("untrusted@1.0.0", tarballResolution("sha512-b"));
        ResolutionVerifier verifier = (resolution, name, version) -> "is-odd".equals(name) ?
          new ResolutionVerifier.Result(false, "MINIMUM_RELEASE_AGE_VIOLATION", "too fresh") :
          new ResolutionVerifier.Result(false, "TRUST_DOWNGRADE", "trust weakened");
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages),
          singletonList(verifier)))
          .isInstanceOfSatisfying(PnpmError.class, e -> {
              assertThat(e.getCode()).isEqualTo("ERR_PNPM_LOCKFILE_RESOLUTION_VERIFICATION");
              assertThat(e.getMessage()).containsPattern("is-odd@0\\.1\\.2 \\[MINIMUM_RELEASE_AGE_VIOLATION\\]" +
                                                         "[\\s\\S]*untrusted@1\\.0\\.0 \\[TRUST_DOWNGRADE\\]");
          });
    }

    @Test
    void listsViolationsInStableOrderAcrossMultipleFailures() {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("fresh-b@2.0.0", tarballResolution("sha512-b"));
        packages.put("fresh-a@1.0.0", tarballResolution("sha512-a"));
        ResolutionVerifier verifier = (resolution, name, version) ->
          new ResolutionVerifier.Result(false, "POLICY_X", name + "@" + version + " failed");
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages),
          singletonList(verifier)))
          .hasMessageMatching("(?s).*fresh-a@1\\.0\\.0.*fresh-b@2\\.0\\.0.*");
    }

    @Test
    void capsPrintedViolationsAt20WithAnAndNMoreSummary() {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        for (int i = 0; i < 25; i++) {
            packages.put(String.format("pkg-%02d@1.0.0", i), tarballResolution("sha512-" + i));
        }
        ResolutionVerifier verifier = (resolution, name, version) ->
          new ResolutionVerifier.Result(false, "POLICY_X", name + "@" + version);
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages),
          singletonList(verifier)))
          .hasMessageMatching("(?s)25 lockfile entries failed verification.*…and 5 more");
    }

    @Test
    void dedupesPeerAndPatchSuffixVariantsAndInvokesTheVerifierOncePerNameVersion() {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("react@18.0.0", tarballResolution("sha512-a"));
        packages.put("react@18.0.0(peer-x)", tarballResolution("sha512-a"));
        packages.put("react@18.0.0(patch_hash=abc)(peer-x)", tarballResolution("sha512-a"));
        List<String> seen = new ArrayList<>();
        VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages), singletonList(
          (resolution, name, version) -> {
              seen.add(name + "@" + version);
              return ResolutionVerifier.Result.OK;
          }));
        assertThat(seen).containsExactly("react@18.0.0");
    }

    @Test
    void doesNotCollapseSameNameVersionWithDifferentResolutions() {
        Map<String, @Nullable Object> npmResolution = tarballResolution("sha512-a");
        Map<String, @Nullable Object> gitResolution = obj("{'type':'git','repo':'x','commit':'abc'}");
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("foo@1.0.0", npmResolution);
        packages.put("foo@1.0.0(peer-x)", gitResolution);
        List<@Nullable Object> seenResolutions = new ArrayList<>();
        VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages), singletonList(
          (resolution, name, version) -> {
              seenResolutions.add(resolution);
              return ResolutionVerifier.Result.OK;
          }));
        assertThat(seenResolutions).containsExactlyInAnyOrder(npmResolution, gitResolution);
    }

    @Test
    void theVerifierSeesTheResolutionShapeVerbatim() {
        Map<String, @Nullable Object> npmResolution = tarballResolution("sha512-deadbeef");
        Map<String, @Nullable Object> gitResolution = obj("{'type':'git','repo':'x','commit':'abc'}");
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("npm-pkg@1.0.0", npmResolution);
        packages.put("git-pkg@1.0.0", gitResolution);
        List<@Nullable Object> received = new ArrayList<>();
        VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages), singletonList(
          (resolution, name, version) -> {
              received.add(resolution);
              return ResolutionVerifier.Result.OK;
          }));
        assertThat(received).containsExactlyInAnyOrder(npmResolution, gitResolution);
    }

    @Test
    void keepsThePerPolicyCodeWhenEveryViolationInTheBatchSharesIt() {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        packages.put("a@1.0.0", tarballResolution("sha512-a"));
        packages.put("b@1.0.0", tarballResolution("sha512-b"));
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(makeLockfile(packages),
          singletonList(failing("POLICY_A", "failed"))))
          .isInstanceOfSatisfying(PnpmError.class, e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_POLICY_A"));
    }

    @Test
    void runsEveryActiveVerifierPerEntryAndStopsAtTheFirstFailure() {
        LockfileObject lockfile = makeLockfile(Collections.singletonMap("a@1.0.0", tarballResolution("sha512-a")));
        List<String> calls = new ArrayList<>();
        ResolutionVerifier firstOk = (resolution, name, version) -> {
            calls.add("first");
            return ResolutionVerifier.Result.OK;
        };
        ResolutionVerifier secondFail = (resolution, name, version) -> {
            calls.add("second");
            return new ResolutionVerifier.Result(false, "SECOND_POLICY", "nope");
        };
        assertThatThrownBy(() -> VerifyLockfileResolutions.verifyLockfileResolutions(lockfile,
          asList(firstOk, secondFail)))
          .isInstanceOfSatisfying(PnpmError.class,
            e -> assertThat(e.getCode()).isEqualTo("ERR_PNPM_SECOND_POLICY"));
        assertThat(calls).containsExactly("first", "second");
    }
}
