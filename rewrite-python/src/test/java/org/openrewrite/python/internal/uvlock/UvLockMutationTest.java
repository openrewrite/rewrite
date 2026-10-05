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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.uvlock.UvLockFixtures.resource;

/**
 * Proves the model supports the engine's surgical edits without format drift:
 * applying the scenario-i pin bump (six 1.16.0 → 1.17.0) to the before-lock
 * reproduces uv's own after-lock byte for byte.
 */
class UvLockMutationTest {

    @Test
    void pinBumpMatchesRealUvOutputByteForByte() {
        UvLock lock = UvLockReader.parse(resource("i-minimal-update/uv.lock.v1"));

        List<UvLockPackage> packages = new ArrayList<>();
        for (UvLockPackage pkg : lock.getPackages()) {
            if ("fixture-i".equals(pkg.getName())) {
                UvLockMetadata metadata = Objects.requireNonNull(pkg.getMetadata());
                List<UvLockRequirement> requiresDist = new ArrayList<>();
                for (UvLockRequirement req : Objects.requireNonNull(metadata.getRequiresDist())) {
                    requiresDist.add("six".equals(req.getName()) ? req.withSpecifier("==1.17.0") : req);
                }
                packages.add(pkg.withMetadata(metadata.withRequiresDist(requiresDist)));
            } else if ("six".equals(pkg.getName())) {
                packages.add(pkg
                  .withVersion("1.17.0")
                  .withSdist(UvLockArtifact.remote(
                    "https://files.pythonhosted.org/packages/94/e7/b2c673351809dca68a0e064b6af791aa332cf192da575fd474ed7d6f16a2/six-1.17.0.tar.gz",
                    "sha256:ff70335d468e7eb6ec65b95b99d3a2836546063f63acc5171de367e834932a81",
                    34031L,
                    "2024-12-04T17:35:28.174Z"))
                  .withWheels(singletonList(UvLockArtifact.remote(
                    "https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl",
                    "sha256:4721f391ed90541fddacab5acf947aa0d3dc7d27b2e1e8eda2be8970586c3274",
                    11050L,
                    "2024-12-04T17:35:26.475Z"))));
            } else {
                packages.add(pkg);
            }
        }

        assertThat(UvLockWriter.write(lock.withPackages(packages)))
          .isEqualTo(resource("i-minimal-update/uv.lock.v2"));
    }
}
