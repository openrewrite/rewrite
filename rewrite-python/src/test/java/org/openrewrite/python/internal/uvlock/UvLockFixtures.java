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
 * Fixtures under {@code src/test/resources/uvlock/} were generated with uv 0.10.11
 * (plus uv 0.5.0/0.7.0 for the {@code o-old-uv} variants); see the README.md there
 * for provenance. Contents are byte-exact reference outputs.
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
      "y-editable-marker/uv.lock"
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
