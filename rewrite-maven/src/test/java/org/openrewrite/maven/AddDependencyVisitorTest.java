/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.semver.LatestRelease;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class AddDependencyVisitorTest {

    @Test
    void shouldHonorMetadataLastRelease() throws MavenDownloadingException {
        // given
        AddDependencyVisitor.MavenMetadataSupplier metadataSupplier = () -> new MavenMetadata(new MavenMetadata.Versioning(
                  "1.0.1RC",
                  "1.0.0",
                  null,null, null, null
                ));
        LatestRelease comparator = new LatestRelease(null);

        // when
        String version = AddDependencyVisitor.findVersionToUse(comparator, "latest.release", metadataSupplier, null, null);

        // test
        assertThat(version).isEqualTo("1.0.0");
    }
}
