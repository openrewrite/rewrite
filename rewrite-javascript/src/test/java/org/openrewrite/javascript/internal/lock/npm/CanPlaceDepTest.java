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

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.PlacementCases.describe;
import static org.openrewrite.javascript.internal.lock.npm.PlacementCases.plain;

/**
 * Port of @npmcli/arborist test/can-place-dep.js, its runTest table with arborist's answers in can-place-dep-cases; the
 * "constructor debug throws" test checks the omitted debug.js assertions.
 */
class CanPlaceDepTest {

    private static final PlacementCases CASES = new PlacementCases("can-place-dep-cases.gz");

    @Test
    void everyUpstreamCase() {
        assertThat(CASES.caseCount()).isEqualTo(46);
    }

    @TestFactory
    List<DynamicTest> basicPlacementCheckTests() {
        return CASES.tests(p -> {
            CanPlaceDep cpd = new CanPlaceDep(p.dep, p.target, p.edge, p.flag("preferDedupe"),
              p.flag("explicitRequest"));

            assertThat(cpd.getCanPlace().name()).isEqualTo(p.declared.get("expect").asText())
              .isEqualTo(p.answer.get("canPlace").asText());
            if (p.declared.has("expectSelf")) {
                assertThat(cpd.getCanPlaceSelf().name()).isEqualTo(p.declared.get("expectSelf").asText());
            }
            assertThat(cpd.getCanPlaceSelf().name()).isEqualTo(p.answer.get("canPlaceSelf").asText());
            assertThat(cpd.getDescription()).isEqualTo(cpd.getCanPlace().name())
              .isEqualTo(p.answer.get("description").asText());

            List<Object> conflictChildren = new ArrayList<>();
            for (CanPlaceDep c : cpd.getConflictChildren()) {
                Map<String, Object> child = obj(
                  "dep", asList(c.getDep().getName(), Js.string(c.getDep().getVersion())),
                  "edge", asList(c.getEdge().getFrom().getLocation(), c.getEdge().getType().toString(),
                    c.getEdge().getName(), c.getEdge().getSpec()),
                  "canPlace", c.getCanPlace(),
                  "canPlaceSelf", c.getCanPlaceSelf());
                conflictChildren.add(child);
            }
            assertThat(plain(conflictChildren)).as("conflict children")
              .isEqualTo(json(p.answer.get("conflictChildren")));

            assertThat(describe(p.tree)).isEqualTo(p.answer.get("tree").asText());
        });
    }
}
