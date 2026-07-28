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
package org.openrewrite.javascript.rpc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JavaScriptRewriteRpcBuilderTest {

    @Test
    void plainSnapshotIsLocallyLinked() {
        // given a local build SNAPSHOT version
        // when checked
        // then it resolves via npm link (no --package)
        assertThat(JavaScriptRewriteRpc.Builder.isLocallyLinkedVersion("0.1.0-SNAPSHOT")).isTrue();
    }

    @Test
    void datedSnapshotIsLocallyLinked() {
        // given the CI dated-snapshot version (SNAPSHOT replaced by a commit timestamp)
        // when checked
        // then it resolves via npm link, rather than npx --package which would fail with ETARGET
        assertThat(JavaScriptRewriteRpc.Builder.isLocallyLinkedVersion("0.1.0-20260624-090742")).isTrue();
    }

    @Test
    void publishedReleaseUsesPackage() {
        // given published release versions
        // when checked
        // then they are fetched from the registry via --package
        assertThat(JavaScriptRewriteRpc.Builder.isLocallyLinkedVersion("8.83.0")).isFalse();
        assertThat(JavaScriptRewriteRpc.Builder.isLocallyLinkedVersion("8.84.0-rc.1")).isFalse();
    }
}
