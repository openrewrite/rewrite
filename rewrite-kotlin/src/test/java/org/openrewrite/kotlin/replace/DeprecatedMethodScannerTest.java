/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.replace;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.kotlin.replace.DeprecatedMethodScanner.DeprecatedMethod;
import org.openrewrite.kotlin.replace.DeprecatedMethodScanner.ScanResult;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeprecatedMethodScannerTest {

    private final DeprecatedMethodScanner scanner = new DeprecatedMethodScanner();

    @Nested
    class ScanKotlinxCoroutines {
        @Test
        void findsDeprecatedMethods() throws IOException {
            ScanResult result = scanner.scan("kotlinx-coroutines-core");

            assertThat(result).isNotNull();
            assertThat(result.groupId()).isEqualTo("org.jetbrains.kotlinx");
            assertThat(result.artifactId()).isEqualTo("kotlinx-coroutines-core");
            assertThat(result.majorVersion()).isEqualTo("1");
            assertThat(result.deprecatedMethods()).hasSizeGreaterThan(50);
        }

        @Test
        void extractsChannelMethods() throws IOException {
            ScanResult result = scanner.scan("kotlinx-coroutines-core");

            assertThat(result).isNotNull();
            List<DeprecatedMethod> channelMethods = result.deprecatedMethods().stream()
                    .filter(m -> m.methodPattern().contains("Channel"))
                    .toList();

            assertThat(channelMethods).isNotEmpty();
            assertThat(channelMethods)
                    .extracting(DeprecatedMethod::replacement)
                    .anyMatch(r -> r.contains("trySend") || r.contains("tryReceive"));
        }
    }

    @Nested
    class ScanKotlinxSerialization {
        @Test
        void findsDeprecatedMethods() throws IOException {
            ScanResult result = scanner.scan("kotlinx-serialization-core");

            assertThat(result).isNotNull();
            assertThat(result.groupId()).isEqualTo("org.jetbrains.kotlinx");
            assertThat(result.artifactId()).isEqualTo("kotlinx-serialization-core");
            assertThat(result.majorVersion()).isEqualTo("1");
            assertThat(result.deprecatedMethods()).isNotEmpty();
        }
    }

    @Nested
    class ScanNonExistentArtifact {
        @Test
        void returnsNullForMissingArtifact() throws IOException {
            ScanResult result = scanner.scan("non-existent-artifact");

            assertThat(result).isNull();
        }
    }
}
