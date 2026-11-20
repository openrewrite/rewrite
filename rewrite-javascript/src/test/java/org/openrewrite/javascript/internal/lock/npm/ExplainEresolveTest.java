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
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;

/**
 * npm lib/utils/explain-eresolve.js and explain-dep.js, as npm renders an {@code ERESOLVE} warning (depth 2) and
 * failure (depth 4).
 */
class ExplainEresolveTest {

    // p@1.0.0, held by x and by the root project, where d's peer wants ^1.1.0; at depth 2 one dependent is explained
    // and the rest named
    private static final Map<String, Object> PEER_CONFLICT = obj(
      "edge", obj("type", "peer", "name", "p", "spec", "^1.1.0",
        "from", obj("name", "d", "version", "1.0.0", "location", "node_modules/d", "dependents", arr())),
      "current", obj("name", "p", "version", "1.0.0", "location", "node_modules/p", "dependents", arr(
        obj("type", "prod", "name", "p", "spec", "1.0.0",
          "from", obj("name", "x", "version", "1.0.0", "location", "node_modules/x", "dependents", arr())),
        obj("type", "prod", "name", "p", "spec", "^1.0.0", "from", obj("location", "/project")))));

    // explain-dep.js from npm 7.0.11 (npm/cli#2134) names the root project among the dependents past the first;
    // before, reading the length of its name fails
    @Test
    void namesTheRootProjectAmongMoreDependentsFrom7_0_11() {
        assertThat(ExplainEresolve.explain(PEER_CONFLICT, 2, NpmRelease.of("7.0.11"))).isEqualTo(
          "Found: p@1.0.0\n" +
          "node_modules/p\n" +
          "  p@\"1.0.0\" from x@1.0.0\n" +
          "  node_modules/x\n" +
          "  1 more (the root project)\n" +
          "\n" +
          "Could not resolve dependency:\n" +
          "peer p@\"^1.1.0\" from d@1.0.0\n" +
          "node_modules/d");
        assertThatThrownBy(() -> ExplainEresolve.explain(PEER_CONFLICT, 2, NpmRelease.of("7.0.10")))
          .isInstanceOf(JsError.class)
          .hasMessageContaining("reading 'length'");
    }
}
