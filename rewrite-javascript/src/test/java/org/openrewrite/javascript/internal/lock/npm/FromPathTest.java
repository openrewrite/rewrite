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
package org.openrewrite.javascript.internal.lock.npm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/from-path.js, with nodes built as trees rather than duck-typed objects.
 */
class FromPathTest {

    @Test
    void usePathIfNoResolvedSpec() {
        assertThat(FromPath.fromPath(node(opts().path("/some/path")), null)).isEqualTo("/some/path");
    }

    @Test
    void useTargetPathIfDirType() {
        Node node = node(opts().path("/some/path/to/directory").resolved("file:/dont/use/this"));
        assertThat(FromPath.fromPath(node, null)).isEqualTo("/some/path/to/directory");
    }

    @Test
    void useDirnameOfResolvedIfFileType() {
        Node node = node(opts().path("/some/path/to/install/target").resolved("file:/some/path/to/file.tgz"));
        assertThat(FromPath.fromPath(node, null)).isEqualTo("/some/path/to");
    }

    @Test
    void useDirnameIfNotDirOrFileType() {
        Node node = node(opts().path("/some/path/to/install/target").resolved("https://registry.com/package.tgz"));
        assertThat(FromPath.fromPath(node, null)).isEqualTo("/some/path/to/install/target");
    }

    @Test
    void usesRootRealpathForOverriddenEdges() {
        Node root = node(opts().path("/some/root"));
        Node node = node(opts().pkg(obj("name", "x")).parent(root));
        Edge edge = new Edge(node, Edge.Type.PROD, "foo", "*");
        edge.setOverrides(new OverrideSet(obj("foo", "foo@2")).getChildren().get("foo"));
        assertThat(FromPath.fromPath(node, edge)).isEqualTo("/some/root");
    }

    @Test
    void usesSourceReferencesRootRealpathForOverriddenEdges() {
        Node root = node(opts().path("/some/root"));
        Node reference = node(opts().pkg(obj("name", "x")).parent(root));
        Node node = node(opts().path("/some/red/herring").sourceReference(reference));
        Edge edge = new Edge(node, Edge.Type.PROD, "foo", "*");
        edge.setOverrides(new OverrideSet(obj("foo", "foo@2")).getChildren().get("foo"));
        assertThat(FromPath.fromPath(node, edge)).isEqualTo("/some/root");
    }
}
