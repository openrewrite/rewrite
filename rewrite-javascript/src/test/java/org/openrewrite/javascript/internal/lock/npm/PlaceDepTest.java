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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.CanPlaceDep.Result.KEEP;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.PlacementCases.describe;
import static org.openrewrite.javascript.internal.lock.npm.PlacementCases.plain;

/**
 * Port of @npmcli/arborist test/place-dep.js over the place-dep-cases fixture (the audit-report case names y in
 * updateNames instead, which places the same way).
 */
class PlaceDepTest {

    private static final PlacementCases CASES = new PlacementCases("place-dep-cases.gz");

    private static final Map<String, BiConsumer<Node, PlaceDep>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("nest a transitive dependency", (tree, pd) -> {
            Node foobar = tree.getChildren().get("foo").resolve("bar");
            assertThat(foobar.getLocation()).isEqualTo("node_modules/bar");
            assertThat(foobar.getVersion()).isEqualTo("1.0.1");
            Node bazbar = tree.getChildren().get("baz").resolve("bar");
            assertThat(bazbar.getLocation()).isEqualTo("node_modules/baz/node_modules/bar");
            assertThat(bazbar.getVersion()).isEqualTo("1.0.0");
        });
        TESTS.put("accept an older transitive dependency", (tree, pd) -> {
            Node foobar = tree.getChildren().get("foo").resolve("bar");
            assertThat(foobar.getLocation()).isEqualTo("node_modules/bar");
            assertThat(foobar.getVersion()).isEqualTo("1.0.0");
            Node bazbar = tree.getChildren().get("baz").resolve("bar");
            assertThat(bazbar.getLocation()).isEqualTo("node_modules/bar");
            assertThat(bazbar.getVersion()).isEqualTo("1.0.0");
        });
        TESTS.put("nest even though unnecessary, because legacy bundling", (tree, pd) -> {
            Node foobar = tree.getChildren().get("foo").resolve("bar");
            assertThat(foobar.getLocation()).isEqualTo("node_modules/foo/node_modules/bar");
            assertThat(foobar.getVersion()).isEqualTo("1.0.0");
            assertThat(tree.getChildren().get("baz").resolve("bar")).isNull();
        });
        TESTS.put("nest because globalStyle", (tree, pd) ->
          assertThat(tree.getChildren().get("foo").resolve("bar").getLocation())
            .isEqualTo("node_modules/foo/node_modules/bar"));
        TESTS.put("nest only 1 level due to globalStyle", (tree, pd) -> {
            Node foobar = tree.getChildren().get("foo").resolve("bar");
            Node foobarbaz = foobar.resolve("baz");
            assertThat(foobar.getLocation()).isEqualTo("node_modules/foo/node_modules/bar");
            assertThat(foobarbaz.getLocation()).isEqualTo("node_modules/foo/node_modules/baz");
        });
        TESTS.put("prefer to dedupe rather than nest", (tree, pd) -> {
            Node foobar = tree.getChildren().get("foo").resolve("bar");
            assertThat(foobar.getLocation()).isEqualTo("node_modules/bar");
            assertThat(foobar.getVersion()).isEqualTo("1.0.0");
            Node bazbar = tree.getChildren().get("baz").resolve("bar");
            assertThat(bazbar.getLocation()).isEqualTo("node_modules/bar");
            assertThat(bazbar.getVersion()).isEqualTo("1.0.0");
        });
        TESTS.put("keep, but dedupe", (tree, pd) -> {
            Node y = tree.getChildren().get("x").resolve("y");
            assertThat(y.getLocation()).isEqualTo("node_modules/y");
            assertThat(y.getVersion()).isEqualTo("1.1.2");
            Node zy = tree.getInventory().get("z").resolve("y");
            assertThat(zy.getLocation()).as("y bundled under z is removed").isEqualTo(y.getLocation());
        });
        TESTS.put("replace higher up, and dedupe descendants", (tree, pd) -> {
            Node y = tree.getChildren().get("x").resolve("y");
            assertThat(y.getLocation()).isEqualTo("node_modules/y");
            assertThat(y.getVersion()).isEqualTo("1.2.2");
            assertThat(tree.resolve("f")).isNull();
            assertThat(tree.resolve("g")).isNull();
            Node z = tree.resolve("z");
            assertThat(z.getLocation()).isEqualTo("node_modules/z");
            assertThat(z.getVersion()).isEqualTo("2.1.0");
            assertThat(tree.getInventory().get("k").getChildren().size())
              .as("children of fsChild all deduped out").isZero();
        });
        TESTS.put("replace higher up, and dedupe descendants (2)", (tree, pd) -> {
            List<String> a = new ArrayList<>();
            for (Node n : tree.getInventory().query("name", "a")) {
                a.add(n.getLocation());
            }
            assertThat(a).as("should be left with one a").containsExactly("node_modules/a");
        });
        TESTS.put("skip over peer dependents in the ancestry walkup", (tree, pd) ->
          assertThat(tree.getChildren().get("v")).isNotNull());
        TESTS.put("do not shadow inappropriately", (tree, pd) -> {
            Node c = tree.getChildren().get("c");
            Node x = tree.getChildren().get("x");
            Node xa = x.resolve("a");
            Node xab = xa.resolve("b");
            Node xabc = xab.resolve("c");
            assertThat(c.getVersion()).isEqualTo("1.0.0");
            assertThat(xabc.getParent()).isSameAs(xa);
            assertThat(xab.getParent()).isSameAs(xa);
            assertThat(xa.getParent()).isSameAs(x);
        });
        BiConsumer<Node, PlaceDep> nestedUnderK = (tree, pd) -> {
            Node x = tree.getChildren().get("k").resolve("x");
            Node y = x.resolve("y");
            assertThat(y.getLocation()).isEqualTo("node_modules/k/node_modules/y");
            assertThat(x.getLocation()).isEqualTo("node_modules/k/node_modules/x");
        };
        TESTS.put("nest peer set of non-root dep", nestedUnderK);
        TESTS.put("nest peer set of non-root dep, step 2", nestedUnderK);
        TESTS.put("replace peer set of non-root dep already in root, step 2", (tree, pd) -> {
            Node x = tree.getChildren().get("k").resolve("x");
            Node y = x.resolve("y");
            assertThat(y.getLocation()).isEqualTo("node_modules/y");
            assertThat(x.getLocation()).isEqualTo("node_modules/x");
            assertThat(y.getVersion()).isEqualTo("2.0.0");
        });
        TESTS.put("prune competing peerSet that can be nested, 2", (tree, pd) -> {
            assertThat(tree.getChildren().get("x")).isNull();
            assertThat(tree.getChildren().get("j").getVersion()).isEqualTo("2.0.0");
            assertThat(pd.getNeedEvaluation().iterator().next()).isSameAs(tree.getChildren().get("a"));
        });
    }

    @Test
    void everyUpstreamCase() {
        assertThat(CASES.caseCount()).isEqualTo(74);
    }

    @TestFactory
    List<DynamicTest> placementTests() {
        return CASES.tests(p -> {
            assertThat(describe(p.tree)).as("before").isEqualTo(p.answer.get("before").asText());

            NpmOptions options = NpmOptions.builder()
              .preferDedupe(p.flag("preferDedupe"))
              .force(p.flag("force"))
              .strictPeerDeps(p.flag("strictPeerDeps"))
              .legacyPeerDeps(p.flag("legacyPeerDeps"))
              .installStrategy(installStrategy(p.declared.path("installStrategy").asText("hoisted")))
              .build();
            List<String> updateNames = p.flag("auditReport") ? Collections.singletonList(p.edge.getName()) :
              p.declared.has("updateNames") ? json(p.declared.get("updateNames")) : Collections.emptyList();

            if (p.flag("error")) {
                assertThatThrownBy(() -> new PlaceDep(p.edge, p.dep, options, p.flag("explicitRequest"), updateNames))
                  .isInstanceOfSatisfying(PlaceDep.CouldNotResolve.class, e -> {
                      assertThat(e.getCode()).isEqualTo("ERESOLVE");
                      Map<String, Object> thrown = new LinkedHashMap<>(e.getConflict().getExplanation());
                      thrown.put("message", e.getMessage());
                      assertThat(plain(thrown)).as("thrown error").isEqualTo(json(p.answer.get("thrown")));
                  });
                assertThat(describe(p.tree)).as("tree should not change").isEqualTo(p.answer.get("after").asText())
                  .isEqualTo(p.answer.get("before").asText());
                return;
            }

            PlaceDep pd = new PlaceDep(p.edge, p.dep, options, p.flag("explicitRequest"), updateNames);

            if (p.flag("test")) {
                TESTS.get(p.name()).accept(p.tree, pd);
            }

            List<String> needEvaluation = new ArrayList<>();
            for (Node n : pd.getNeedEvaluation()) {
                needEvaluation.add(n.getLocation() + " " + Js.string(n.getVersion()));
            }
            assertThat(needEvaluation).isEqualTo(json(p.answer.get("needEvaluation")));
            assertThat(describe(p.tree)).as("changes to tree").isEqualTo(p.answer.get("after").asText());
            List<Object> warnings = new ArrayList<>();
            for (PlaceDep.PeerConflict w : pd.getWarnings()) {
                warnings.add(asList("ERESOLVE", "overriding peer dependency", w.getExplanation()));
            }
            assertThat(plain(warnings)).as("warnings").isEqualTo(json(p.answer.get("warnings")));
            assertThat(plain(placements(pd))).as("placements").isEqualTo(json(p.answer.get("placements")));
        });
    }

    @Test
    void everyCaseWithItsOwnAssertionsIsPorted() {
        assertThat(TESTS.keySet()).containsExactlyElementsOf(CASES.names("test"));
    }

    private static List<Object> placements(PlaceDep pd) {
        // a Map by location, as the test builds it: a target pruned since it was checked is at "" with the root
        Map<String, Object> byLocation = new LinkedHashMap<>();
        for (Map.Entry<Node, CanPlaceDep> check : pd.getChecks().entrySet()) {
            byLocation.put(check.getKey().getLocation(),
              asList(check.getValue().getCanPlace(), check.getValue().getCanPlaceSelf()));
        }
        List<Object> checks = new ArrayList<>();
        for (Map.Entry<String, Object> check : byLocation.entrySet()) {
            checks.add(asList(check.getKey(), check.getValue()));
        }
        List<PlaceDep> all = new ArrayList<>();
        all.add(pd);
        all.addAll(pd.getAllChildren());
        List<Object> placements = new ArrayList<>();
        for (PlaceDep c : all) {
            if (c.getCanPlace() != null && c.getCanPlace().getCanPlace() == KEEP) {
                assertThat(c.getPlaced()).as("should not place if result is KEEP").isNull();
            }
            Map<String, Object> placement = new LinkedHashMap<>();
            if (c.getParent() != null) {
                placement.put("parent", c.getParent().getName());
            }
            String from = c.getEdge().getFrom().getLocation();
            placement.put("edge", "{ " + (from.isEmpty() ? "ROOT" : from) + " " + c.getEdge().getType() + " " +
                                  c.getEdge().getName() + "@" + c.getEdge().getSpec() + " }");
            placement.put("dep", c.getDep().getName() + "@" + Js.string(c.getDep().getVersion()));
            placement.put("canPlace", c.getCanPlace() == null ? null : c.getCanPlace().getCanPlace());
            placement.put("canPlaceSelf", c.getCanPlaceSelf() == null ? null : c.getCanPlaceSelf().getCanPlaceSelf());
            placement.put("placed", c.getPlaced() == null ? null : c.getPlaced().getLocation());
            placement.put("checks", checks);
            placements.add(placement);
        }
        return placements;
    }

    private static NpmOptions.InstallStrategy installStrategy(String name) {
        return Arrays.stream(NpmOptions.InstallStrategy.values())
          .filter(s -> s.toString().equals(name))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(name));
    }
}
