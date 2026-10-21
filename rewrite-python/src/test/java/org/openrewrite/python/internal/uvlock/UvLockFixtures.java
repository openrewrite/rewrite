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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixtures under {@code src/test/resources/uvlock/} are real uv output; the README.md there
 * records which release produced each. Contents are byte-exact reference outputs.
 */
final class UvLockFixtures {

    /**
     * Every uv-emitted lock in the corpus, including old-revision and variant states,
     * plus the one labeled engine-defined expectation ({@code uv.lock.engine-edited}).
     */
    static final String[] ALL_LOCKS = {
      "a-multi-package/uv.lock",
      "b-many-wheels/uv.lock",
      "c-sdist-only/uv.lock",
      "d-extras/uv.lock",
      "e-markers-forks/uv.lock",
      "e2-true-fork/uv.lock",
      "f-dep-groups/uv.lock",
      "g-optional-deps/uv.lock",
      "g2-extras-order/uv.lock",
      "g3-multi-extras/uv.lock",
      "h-requires-python/uv.lock",
      "h3-requires-python-order/uv.lock",
      "h-requires-python/uv.lock.after",
      "h-requires-python/uv.lock.before",
      "h2-bump-wheels/uv.lock",
      "h2-bump-wheels/uv.lock.after",
      "h2-bump-wheels/uv.lock.before",
      "i-minimal-update/uv.lock",
      "i-minimal-update/uv.lock.v1",
      "i-minimal-update/uv.lock.v2",
      "i2-upgrade-package/uv.lock",
      "i2-upgrade-package/uv.lock.pinned",
      "i2-upgrade-package/uv.lock.relaxed-plain",
      "i2-upgrade-package/uv.lock.upgraded",
      "j-check/uv.lock",
      "j-check/uv.lock.baseline",
      "j-check/uv.lock.handbumped",
      "j-check/uv.lock.pristine",
      "j-check/uv.lock.tampered-metadata",
      "k-multi-index/uv.lock",
      "l-flat-index/uv.lock",
      "m2-options/uv.lock",
      "m3-lexical/uv.lock",
      "m4-exclude-newer-span/uv.lock",
      "n-normalization/uv.lock",
      "n2-inline-width/uv.lock",
      "o-old-uv/proj-0.5.0/uv.lock",
      "o-old-uv/proj-0.5.0/uv.lock.after-change-current-uv",
      "o-old-uv/proj-0.5.0/uv.lock.as-0.5.0",
      "o-old-uv/proj-0.5.0/uv.lock.engine-edited",
      "o-old-uv/proj-0.7.0-flat/uv.lock",
      "o-old-uv/proj-0.7.0/uv.lock",
      "o-old-uv/proj-0.7.0/uv.lock.as-0.7.0",
      "p-editable-root/uv.lock",
      "q-workspace/uv.lock",
      "z-revision-5/uv.lock",
      "z2-dynamic-version-root/uv.lock",
      "r-removal/uv.lock",
      "r-removal/uv.lock.before",
      "s-remove-last-dep/uv.lock.after",
      "s-remove-last-dep/uv.lock.before",
      "t-url-source/uv.lock",
      "u-git-source/uv.lock",
      "v-directory/uv.lock",
      "w-conflicts/uv.lock",
      "w2-conflicts-groups/uv.lock",
      "x-supported-required-markers/uv.lock",
      "x1-cascade-boto3/uv.lock.before",
      "x1-cascade-boto3/uv.lock.after",
      "x2-add-leaf/uv.lock.after",
      "x3-add-closure/uv.lock.after",
      "off4-add-markered/uv.lock.after",
      "off5-add-markered-closure/uv.lock.after",
      "off7-add-markered-platform/uv.lock.after",
      "y-editable-marker/uv.lock",
      "h4-fork-collapse/uv.lock.after",
      "h4-fork-collapse/uv.lock.before",
      "h5-fork-partial/uv.lock.before",
      "off1-add-leaf/uv.lock.after",
      "off1-add-leaf/uv.lock.before",
      "off10-dead-marker-cascade/uv.lock.after",
      "off10-dead-marker-cascade/uv.lock.before",
      "off11-extras-removal-keeps-package/uv.lock.after",
      "off11-extras-removal-keeps-package/uv.lock.before",
      "off12-inactive-extra-constraint/uv.lock.after",
      "off12-inactive-extra-constraint/uv.lock.before",
      "off13-dev-group-constraint/uv.lock.after",
      "off13-dev-group-constraint/uv.lock.before",
      "off14-upgrade-drops-extra/uv.lock.before",
      "off15-python-lower-bound-marker/uv.lock.before",
      "off16-tool-uv-dev-dependencies/uv.lock",
      "off17-python-upper-bound-marker/uv.lock.after",
      "off17-python-upper-bound-marker/uv.lock.before",
      "off18-removed-extra-unbinds-cascade/uv.lock.after",
      "off18-removed-extra-unbinds-cascade/uv.lock.before",
      "off19-extra-group-lockstep-upgrade/uv.lock.after",
      "off19-extra-group-lockstep-upgrade/uv.lock.before",
      "off20-transitive-add-with-extras/uv.lock.after",
      "off20-transitive-add-with-extras/uv.lock.before",
      "off21-upgrade-activates-extra/uv.lock.after",
      "off21-upgrade-activates-extra/uv.lock.before",
      "off22-or-extras-marker/uv.lock.before",
      "off23-extra-dnf-markers/uv.lock.after",
      "off23-extra-dnf-markers/uv.lock.before",
      "off24-extra-or-platform-marker/uv.lock.after",
      "off24-extra-or-platform-marker/uv.lock.before",
      "off25-add-with-extras/uv.lock.after",
      "off25-add-with-extras/uv.lock.before",
      "off26-declared-extras-added/uv.lock.after",
      "off26-declared-extras-added/uv.lock.before",
      "off27-declared-extras-removed/uv.lock.after",
      "off27-declared-extras-removed/uv.lock.before",
      "off28-add-pinned-index/uv.lock.after",
      "off28-add-pinned-index/uv.lock.before",
      "off29-transitive-constraint/uv.lock.after",
      "off29-transitive-constraint/uv.lock.before",
      "off30-two-constraints/uv.lock.after",
      "off30-two-constraints/uv.lock.before",
      "off31-declaration-vs-constraint-conflict/uv.lock.before",
      "off32-upgrade-past-dependent-bound/uv.lock.before",
      "off33-dependent-bound-shapes-selection/uv.lock.after",
      "off33-dependent-bound-shapes-selection/uv.lock.before",
      "off34-exclude-newer-package-and-build-constraints/uv.lock.after",
      "off34-exclude-newer-package-and-build-constraints/uv.lock.before",
      "off35-exact-minor-wheel-filter/uv.lock.after",
      "off35-exact-minor-wheel-filter/uv.lock.before",
      "off36-exact-minor-marker-drop/uv.lock.after",
      "off36-exact-minor-marker-drop/uv.lock.before",
      "off37-parent-reachability-markers/uv.lock.after",
      "off37-parent-reachability-markers/uv.lock.before",
      "off38-marker-version-forms/uv.lock.after",
      "off38-marker-version-forms/uv.lock.before",
      "off2-add-closure/uv.lock.after",
      "off2-add-closure/uv.lock.before",
      "off3-cascade-flake8/uv.lock.after",
      "off3-cascade-flake8/uv.lock.before",
      "off4-add-markered/uv.lock.before",
      "off5-add-markered-closure/uv.lock.before",
      "off6-add-markered-version/uv.lock.before",
      "off7-add-markered-platform/uv.lock.before",
      "off8-restricted-environments/uv.lock.before",
      "r-removal/uv.lock.after",
      "x2-add-leaf/uv.lock.before",
      "x3-add-closure/uv.lock.before",
      "x5-add-forks/uv.lock.before",
      "x7-add-markered-conditional/uv.lock.before",
      "x8-add-portalocker/uv.lock.after",
      "x8-add-portalocker/uv.lock.before",
      "z3-compatible-release-python/uv.lock"
    };

    private UvLockFixtures() {
    }

    static String resource(String name) {
        try (InputStream is = UvLockFixtures.class.getResourceAsStream("/uvlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
