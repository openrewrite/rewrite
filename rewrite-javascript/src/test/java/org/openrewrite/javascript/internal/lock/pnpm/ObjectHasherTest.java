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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.pnpm.PnpmTestSupport.obj;

/**
 * crypto/object-hasher/test/index.ts of pnpm 11.2.2 for {@code hashObjectNullableWithPrefix}, less the {@code Set}
 * cases (pnpm-workspace.yaml gives no {@code Set}).
 */
class ObjectHasherTest {

    @Test
    void createsAHash() {
        assertThat(ObjectHasher.hashObjectNullableWithPrefix(obj("{'b':1,'a':2}")))
          .isEqualTo("sha256-48AVoXIXcTKcnHt8qVKp5vNw4gyOB5VfztHwtYBRcAQ=");
        assertThat(ObjectHasher.hashObjectNullableWithPrefix(obj("{}"))).isNull();
        assertThat(ObjectHasher.hashObjectNullableWithPrefix(null)).isNull();
    }

    @Test
    void sorts() {
        assertThat(ObjectHasher.hashObjectNullableWithPrefix(obj("{'b':1,'a':2}")))
          .isEqualTo(ObjectHasher.hashObjectNullableWithPrefix(obj("{'a':2,'b':1}")));
        assertThat(ObjectHasher.hashObjectNullableWithPrefix(obj("{'b':[1,2,3],'a':[1,2,3]}")))
          .isEqualTo(ObjectHasher.hashObjectNullableWithPrefix(obj("{'a':[2,3,1],'b':[3,2,1]}")));
    }

    /**
     * The checksum {@code small-settings-package-extensions} records for its packageExtensions.
     */
    @Test
    void theRecordedPackageExtensionsChecksum() {
        String workspace = "packageExtensions:\n  \"@isaacs/testing-peer-deps-c@2\":\n    dependencies:\n" +
                           "      wrappy: 1.0.2\npackages:\n  - .\n";
        Object extensions = WorkspaceManifest.readWorkspaceManifest(workspace).get("packageExtensions");
        assertThat(ObjectHasher.hashObjectNullableWithPrefix(extensions))
          .isEqualTo("sha256-evGr1O6/1ZX3Wo+O7hsPE8kZGElq8HlB0mkT7YBjU/k=");
    }
}
