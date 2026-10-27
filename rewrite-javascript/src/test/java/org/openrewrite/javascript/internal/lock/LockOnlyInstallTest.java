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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockOnlyInstallTest {

    @Test
    void eachOutcomeHasTheKindTheRuleNames() {
        assertThat(LockOnlyInstall.kindOf(new CannotKnow("x"))).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(LockOnlyInstall.kindOf(Rejected.coded(Reason.PEER_CONFLICT, "a", "ERESOLVE", "x")))
          .isEqualTo(Kind.PM_REJECTS);
        assertThat(LockOnlyInstall.kindOf(Rejected.rejects(Reason.RESOLUTION_REQUIRED, "Couldn't find package")))
          .isEqualTo(Kind.PM_REJECTS);
        assertThat(LockOnlyInstall.kindOf(Rejected.rejects(SharedError.PACKAGE_NOT_FOUND, "a", "Not found")))
          .isEqualTo(Kind.PM_REJECTS);
        assertThat(LockOnlyInstall.kindOf(Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "TypeError: x")))
          .isEqualTo(Kind.PM_FAILS);
        assertThat(LockOnlyInstall.kindOf(Rejected.discardsLock("x"))).isEqualTo(Kind.PM_REJECTS);
        assertThat(LockOnlyInstall.kindOf(Rejected.deletesLock("x"))).isEqualTo(Kind.PM_REJECTS);
        assertThat(LockOnlyInstall.kindOf(new NodeRegistryException(NodeRegistryException.Reason.PACKAGE_NOT_FOUND,
          "a", "404"))).isEqualTo(Kind.PM_REJECTS);
        assertThat(LockOnlyInstall.kindOf(new NodeRegistryException(NodeRegistryException.Reason.UNREACHABLE,
          "a", "timeout"))).isEqualTo(Kind.CANNOT_KNOW);
    }

    @Test
    void anythingElseIsNotAnOutcome() {
        IllegalStateException bug = new IllegalStateException("bug");
        assertThatThrownBy(() -> LockOnlyInstall.NPM.failure(bug)).isSameAs(bug);
    }

    @Test
    void aFailureIsWordedWithTheCommandAndItsCode() {
        assertThat(LockOnlyInstall.NPM.failure(Rejected.coded(Reason.VERSION_NOT_FOUND, "a", "ETARGET", "No match"))
          .getDetail()).isEqualTo("npm rejects this edit (ETARGET): No match");
        assertThat(LockOnlyInstall.YARN.failure(Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "TypeError: x"))
          .getDetail()).isEqualTo("yarn fails on this edit: TypeError: x");
        assertThat(LockOnlyInstall.PNPM.failure(new CannotKnow("the environment")).getDetail())
          .isEqualTo("the engine cannot know the environment");
    }

    @Test
    void whatTheSecondInstallCannotKnowStaysCannotKnow() {
        Result result = LockOnlyInstall.BUN.verify("lock", lock -> {
            throw new CannotKnow(".npmrc (its contents were not read)");
        }, LockOnlyInstall.BUN::failure);

        Failure failure = result.getFailure();
        assertThat(failure).isNotNull();
        assertThat(failure.getKind()).isEqualTo(Kind.CANNOT_KNOW);
        assertThat(failure.getDetail()).isEqualTo("the engine cannot know .npmrc (its contents were not read) (in a " +
                                                  "second bun install over the lock written for this edit)");
    }

    @Test
    void aSecondInstallThatRejectsFailsOrRewritesTheLockIsTheSecondInstalls() {
        Result rejects = LockOnlyInstall.PNPM.verify("lock", lock -> {
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "[ERR_PNPM_X] x");
        }, LockOnlyInstall.PNPM::failure);
        Result fails = LockOnlyInstall.YARN.verify("lock", lock -> {
            throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "TypeError: x");
        }, LockOnlyInstall.YARN::failure);
        Result rewrites = LockOnlyInstall.NPM.verify("a\nb\n", lock -> "a\nc\n", LockOnlyInstall.NPM::failure);

        assertThat(rejects.getFailure().getKind()).isEqualTo(Kind.SECOND_INSTALL);
        assertThat(rejects.getFailure().getDetail()).isEqualTo("a second pnpm install over the lock written for " +
                                                               "this edit would reject it: pnpm rejects this " +
                                                               "edit: [ERR_PNPM_X] x");
        assertThat(fails.getFailure().getKind()).isEqualTo(Kind.SECOND_INSTALL);
        assertThat(fails.getFailure().getDetail()).contains(" would fail: yarn fails on this edit: TypeError: x");
        assertThat(rewrites.getFailure().getKind()).isEqualTo(Kind.SECOND_INSTALL);
        assertThat(rewrites.getFailure().getDetail()).endsWith("would rewrite it: line 2 \"b\" becomes \"c\"");
        assertThat(LockOnlyInstall.NPM.verify("a\n", lock -> lock, LockOnlyInstall.NPM::failure).isSuccess()).isTrue();
    }
}
