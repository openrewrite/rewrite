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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class NodeDependencyScanTest {

    @Test
    void everyVisitedPathIsHeldAndOnlyAReadPackageManagerFileHasContents() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        Path read = Paths.get("app/patches/ms@2.1.3.patch");
        Path unread = Paths.get("app/patches/debug@4.4.3.patch");
        Path source = Paths.get("app/src/index.js");

        boolean readKept = NodeDependencyScan.capturePackageManagerFile(acc,
          PlainText.builder().sourcePath(read).text("--- a/index.js\n+++ b/index.js\n").build());
        boolean unreadKept = NodeDependencyScan.capturePackageManagerFile(acc,
          new Quark(Tree.randomId(), unread, Markers.EMPTY, null, null));
        boolean sourceKept = NodeDependencyScan.capturePackageManagerFile(acc,
          PlainText.builder().sourcePath(source).text("x").build());

        assertThat(readKept).isTrue();
        assertThat(unreadKept).isTrue();
        assertThat(sourceKept).isFalse();
        assertThat(acc.paths).containsExactlyInAnyOrder(read, unread, source);
        assertThat(acc.packageManagerFiles).containsOnlyKeys(read);
    }

    @Test
    void theEnvFilesAnInstallLoadsAreKept() {
        NodeDependencyScan.Accumulator acc = new NodeDependencyScan.Accumulator();
        for (String name : new String[]{".env", ".env.local", ".env.production", ".env.production.local",
          ".env.development", ".env.test"}) {
            NodeDependencyScan.capturePackageManagerFile(acc,
              PlainText.builder().sourcePath(Paths.get("app/" + name)).text("NPM_CONFIG_REGISTRY=https://r/").build());
        }

        assertThat(acc.packageManagerFiles).containsOnlyKeys(Paths.get("app/.env"), Paths.get("app/.env.local"),
          Paths.get("app/.env.production"), Paths.get("app/.env.production.local"));
    }
}
