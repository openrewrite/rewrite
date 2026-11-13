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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;

/**
 * Port of @npmcli/arborist test/override-set.js, plus the OverrideSet comparison case of test/edge.js.
 */
class OverrideSetTest {

    @Nested
    class Constructor {

        @Test
        void throwsWhenAddingAChildRuleWithNoName() {
            assertThatThrownBy(() -> new OverrideSet(obj("github:npm/cli", "1.0.0")))
              .hasMessage("Override without name: github:npm/cli");
        }

        @Test
        void loadsOverrides() {
            assertRules(new OverrideSet(obj(
              "a@1", obj(".", "1.0.2"),
              "b", obj(".", "1.0.0"),
              "c@1.0.1", obj(".", "1.0.2"))));
        }

        @Test
        void loadsChildOverridesAsString() {
            assertRules(new OverrideSet(obj("a@1", "1.0.2", "b", "1.0.0", "c@1.0.1", "1.0.2")));
        }

        private void assertRules(OverrideSet overrides) {
            assertThat(overrides.getChildren()).containsKey("a@1").containsKey("b");

            assertThat(overrides.getEdgeRule("a", "^2.0.0")).isSameAs(overrides);

            OverrideSet edgeRuleA = overrides.getEdgeRule("a", "^1.0.1");
            assertThat(edgeRuleA).isNotNull();
            OverrideSet edgeRuleB = overrides.getEdgeRule("b", "^1.1.0");
            assertThat(edgeRuleB).isNotNull();
            assertThat(overrides.getEdgeRule("c", "^1.0.0")).isNotNull();

            assertThat(overrides.getNodeRule("c", "1.0.0")).isSameAs(overrides);
            assertThat(overrides.getMatchingRule("a", "1.0.2")).isSameAs(overrides.getChildren().get("a@1"));
            assertThat(overrides.getMatchingRule("a", "2.0.0")).isNull();

            OverrideSet keySpecRuleA = overrides.getNodeRule("a", "1.0.1");
            OverrideSet keySpecRuleB = overrides.getNodeRule("b", "1.1.1");
            OverrideSet valueRuleA = overrides.getNodeRule("a", "1.0.2");
            OverrideSet valueRuleB = overrides.getNodeRule("b", "1.0.0");

            assertThat(edgeRuleA).isSameAs(keySpecRuleA);
            assertThat(keySpecRuleA).isSameAs(valueRuleA);
            assertThat(edgeRuleB).isSameAs(keySpecRuleB);
            assertThat(keySpecRuleB).isSameAs(valueRuleB);
        }

        @Test
        void childRulesTakePriority() {
            OverrideSet overrides = new OverrideSet(obj(
              "foo", obj("bar", obj(".", "2.0.0", "baz", "3.0.0"), "baz", "2.0.0"),
              "bar", "1.0.0",
              "baz", "1.0.0"));

            OverrideSet edgeRule = overrides.getEdgeRule("foo", "^1");
            assertThat(edgeRule.getName()).isEqualTo("foo");
            Map<String, OverrideSet> ruleset = edgeRule.getRuleset();
            OverrideSet fooRule = ruleset.get("foo");
            assertThat(fooRule.getKeySpec()).isEqualTo("*");
            assertThat(fooRule.getValue()).isEqualTo("*");
            OverrideSet barRule = ruleset.get("bar");
            assertThat(barRule.getKeySpec()).isEqualTo("*");
            assertThat(barRule.getValue()).isEqualTo("2.0.0");
            OverrideSet bazRule = ruleset.get("baz");
            assertThat(bazRule.getKeySpec()).isEqualTo("*");
            assertThat(bazRule.getValue()).isEqualTo("2.0.0");

            OverrideSet childRule = edgeRule.getEdgeRule("bar", "^1.0.0");
            Map<String, OverrideSet> childRuleSet = childRule.getRuleset();
            assertThat(childRuleSet.get("baz").getKeySpec()).isEqualTo("*");
            assertThat(childRuleSet.get("baz").getValue()).isEqualTo("3.0.0");
            assertThat(childRuleSet.get("bar").getKeySpec()).isEqualTo("*");
            assertThat(childRuleSet.get("bar").getValue()).isEqualTo("2.0.0");
            assertThat(childRuleSet.get("foo").getKeySpec()).isEqualTo("*");
            assertThat(childRuleSet.get("foo").getValue()).isEqualTo("*");
        }

        @Test
        void coercesEmptyStringToStar() {
            OverrideSet overrides = new OverrideSet(obj("foo", obj(".", ""), "bar", obj()));
            assertThat(overrides.getEdgeRule("foo", "^1").getValue()).isEqualTo("*");
            assertThat(overrides.getEdgeRule("bar", "^1").getValue()).isEqualTo("*");
        }

        private OverrideSet referenceOverrides() {
            return new OverrideSet(obj(
              "foo", obj("bar", "$bar"),
              "baz@^1.0.0", obj("buzz@^1.0.0", "$buzz")));
        }

        @Test
        void versionSpecsWork() {
            assertReferenceRules("1.0.0", "1.0.0");
        }

        @Test
        void directorySpecsWork() {
            assertReferenceRules("file:../bar", "file:../buzz");
        }

        @Test
        void fileSpecsWork() {
            assertReferenceRules("file:../bar.tgz", "file:../buzz.tgz");
        }

        @Test
        void aliasSpecsWork() {
            assertReferenceRules("npm:bar2@^1.0.0", "npm:buzz2@^1.0.0");
        }

        private void assertReferenceRules(String barSpec, String buzzSpec) {
            OverrideSet overrides = referenceOverrides();
            OverrideSet fooEdgeRule = overrides.getEdgeRule("foo", "^1.0.0");
            assertThat(fooEdgeRule.getEdgeRule("bar", barSpec).getValue()).isEqualTo("$bar");
            OverrideSet bazEdgeRule = overrides.getEdgeRule("baz", "^1.0.0");
            assertThat(bazEdgeRule.getEdgeRule("buzz", buzzSpec).getValue()).isEqualTo("$buzz");
        }

        // arborist before npm 9.3.0 intersects the edge's spec with the rule's key as two ranges: a range still takes
        // the rule, a spec that is no range throws node-semver's TypeError
        @Test
        void edgeRuleIsARangeIntersectionBefore9_3_0() {
            OverrideSet overrides = new OverrideSet(obj("foo", "1.0.0", "bar@^1.0.0", "2.0.0"), NpmRelease.of("9.2.0"));
            assertThat(overrides.getEdgeRule("foo", "^1.0.0").getValue()).isEqualTo("1.0.0");
            assertThat(overrides.getEdgeRule("bar", "^1.5.0").getValue()).isEqualTo("2.0.0");
            assertThat(overrides.getEdgeRule("bar", "^2.0.0").isRoot()).isTrue();
            assertThatThrownBy(() -> overrides.getEdgeRule("foo", "npm:foo2@^1.0.0"))
              .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> overrides.getEdgeRule("foo", "latest")).isInstanceOf(IllegalArgumentException.class);

            OverrideSet from930 = new OverrideSet(obj("foo", "1.0.0"), NpmRelease.of("9.3.0"));
            assertThat(from930.getEdgeRule("foo", "npm:foo2@^1.0.0").getValue()).isEqualTo("1.0.0");
            assertThat(from930.getEdgeRule("foo", "latest").getValue()).isEqualTo("1.0.0");
        }

        @Test
        void gitSpecsWork() {
            OverrideSet overrides = referenceOverrides();
            OverrideSet fooEdgeRule = overrides.getEdgeRule("foo", "^1.0.0");
            assertThat(fooEdgeRule.getEdgeRule("bar", "github:foo/bar").getValue()).isEqualTo("$bar");
            OverrideSet bazEdgeRule = overrides.getEdgeRule("baz", "^1.0.0");
            assertThat(bazEdgeRule.getEdgeRule("buzz", "github:baz/buzz#semver:^1.0.0").getValue()).isEqualTo("$buzz");
            assertThat(bazEdgeRule.getEdgeRule("buzz", "github:baz/buzz#semver:^2.0.0").getName()).isEqualTo("baz");
        }

        @Test
        void isEqualAndFindSpecificOverrideSet() {
            OverrideSet overrides1 = new OverrideSet(obj(
              "foo", obj("bar", obj(".", "2.0.0", "baz", "3.0.0"), "baz", "2.0.0"), "bar", "1.0.0", "baz", "1.0.0"));
            OverrideSet overrides2 = new OverrideSet(obj(
              "foo", obj("bar", obj(".", "2.0.0", "baz", "3.0.0"), "baz", "2.0.0"), "bar", "1.0.0", "baz", "1.0.0"));
            OverrideSet overrides3 = new OverrideSet(obj(
              "foo", obj("bar", obj(".", "2.0.0", "baz", "3.1.0"), "baz", "2.0.0"), "bar", "1.0.0", "baz", "1.0.0"));
            OverrideSet overrides4 = new OverrideSet(obj(
              "foo", obj("bar", obj(".", "2.0.0"), "baz", "2.0.0"), "bar", "1.0.0", "baz", "1.0.0"));
            OverrideSet overrides5 = new OverrideSet(obj(
              "foo", obj("bar", obj(".", "2.0.0"), "bat", "2.0.0"), "bar", "1.0.0", "baz", "1.0.0"));
            OverrideSet overrides6 = new OverrideSet(obj("bar", obj(".", "2.0.0"), "bat", "2.0.0"));
            overrides6.setParent(overrides5);
            OverrideSet overrides7 = new OverrideSet(obj("bat", "2.0.0"));
            OverrideSet overrides8 = new OverrideSet(obj("bat", "1.2.0"));
            OverrideSet overrides9 = new OverrideSet(obj("bat@3.0.0", "1.2.0"));

            assertThat(overrides1.isEqual(overrides1)).isTrue();
            assertThat(overrides1.isEqual(overrides2)).isTrue();
            assertThat(overrides1.isEqual(overrides3)).isFalse();
            assertThat(overrides2.isEqual(overrides3)).isFalse();
            assertThat(overrides3.isEqual(overrides1)).isFalse();
            assertThat(overrides3.isEqual(overrides2)).isFalse();
            assertThat(overrides4.isEqual(overrides1)).isFalse();
            assertThat(overrides4.isEqual(overrides2)).isFalse();
            assertThat(overrides4.isEqual(overrides3)).isFalse();
            assertThat(overrides4.isEqual(overrides5)).isFalse();
            assertThat(overrides5.isEqual(overrides4)).isFalse();
            assertThat(OverrideSet.findSpecificOverrideSet(overrides5, overrides5)).isSameAs(overrides5);
            assertThat(OverrideSet.findSpecificOverrideSet(overrides5, overrides6)).isSameAs(overrides6);
            assertThat(OverrideSet.findSpecificOverrideSet(overrides6, overrides5)).isSameAs(overrides6);
            assertThat(OverrideSet.doOverrideSetsConflict(overrides1, overrides2)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides5, overrides5)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides5, overrides6)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides6, overrides5)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides5, overrides7)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides7, overrides5)).isFalse();
            assertThat(overrides7.isEqual(overrides8)).isFalse();
            assertThat(overrides8.isEqual(overrides9)).isFalse();
            assertThat(overrides7.isEqual(overrides9)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides7, overrides8)).isTrue();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides7, overrides9)).isFalse();
            assertThat(OverrideSet.doOverrideSetsConflict(overrides8, overrides9)).isFalse();
        }

        @Nested
        class SemanticConflictDetection {

            @Test
            void noConflictWhenPackagesAreDifferent() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("foo", "1.0.0")), new OverrideSet(obj("bar", "1.0.0")))).isFalse();
            }

            @Test
            void noConflictWhenVersionRangesIntersect() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("lodash", "^4.0.0")), new OverrideSet(obj("lodash", "4.17.0")))).isFalse();
            }

            @Test
            void conflictWhenVersionRangesDoNotIntersect() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("lodash", "1.x")), new OverrideSet(obj("lodash", "4.x")))).isTrue();
            }

            @Test
            void noConflictWhenUsingReferenceOverrides() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("lodash", "$ref1")), new OverrideSet(obj("lodash", "$ref2")))).isFalse();
            }

            @Test
            void noConflictWhenOneIsAReferenceOverride() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("lodash", "$ref1")), new OverrideSet(obj("lodash", "4.17.21")))).isFalse();
            }

            @Test
            void noConflictWhenRulesAreEqual() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("lodash", "4.17.21")), new OverrideSet(obj("lodash", "4.17.21")))).isFalse();
            }

            @Test
            void handlesNonSemverSpecTypesGracefully() {
                assertThat(OverrideSet.haveConflictingRules(
                  new OverrideSet(obj("foo", "github:user/repo")), new OverrideSet(obj("foo", "file:./local"))))
                    .isFalse();
            }

            @Test
            void regressionVaadinCase() {
                assertThat(OverrideSet.doOverrideSetsConflict(
                  new OverrideSet(obj("@vaadin/react-components", "24.9.2")),
                  new OverrideSet(obj("@vaadin/react-components-pro", "24.9.2")))).isFalse();
            }
        }
    }

    @Test
    void findSpecificOverrideSetReturnsCommonAncestorForCompatibleSiblings() {
        OverrideSet root = new OverrideSet(obj("react", "18.3.1", "react-dom", "18.3.1"));
        OverrideSet reactChild = root.getChildren().get("react");
        OverrideSet reactDomChild = root.getChildren().get("react-dom");
        assertThat(reactChild).isNotNull();
        assertThat(reactDomChild).isNotNull();

        OverrideSet result = OverrideSet.findSpecificOverrideSet(reactChild, reactDomChild);
        assertThat(result).isNotNull();
        assertThat(result.isEqual(root)).isTrue();

        OverrideSet reverse = OverrideSet.findSpecificOverrideSet(reactDomChild, reactChild);
        assertThat(reverse).isNotNull();
        assertThat(reverse.isEqual(root)).isTrue();
    }

    @Test
    void findCommonAncestor() {
        OverrideSet root = new OverrideSet(obj("foo", "1.0.0", "bar", "2.0.0"));
        OverrideSet fooChild = root.getChildren().get("foo");
        OverrideSet barChild = root.getChildren().get("bar");

        OverrideSet ancestor = OverrideSet.findCommonAncestor(fooChild, barChild);
        assertThat(ancestor).isNotNull();
        assertThat(ancestor.isEqual(root)).isTrue();

        OverrideSet other = new OverrideSet(obj("baz", "3.0.0"));
        assertThat(OverrideSet.findCommonAncestor(fooChild, other)).isNull();
    }

    @Test
    void findSpecificOverrideSetReturnsNothingForTrulyConflictingSiblings() {
        OverrideSet root = new OverrideSet(obj(
          "foo", obj(".", "1.0.0", "shared", "1.0.0"),
          "bar", obj(".", "2.0.0", "shared", "99.0.0")));
        OverrideSet fooChild = root.getChildren().get("foo");
        OverrideSet barChild = root.getChildren().get("bar");
        assertThat(fooChild).isNotNull();
        assertThat(barChild).isNotNull();
        assertThat(OverrideSet.findSpecificOverrideSet(fooChild, barChild)).isNull();
    }

    @Nested
    class IsEqualEdgeCases {

        @Test
        void isEqualWithNullOther() {
            assertThat(new OverrideSet(obj("foo", "1.0.0")).isEqual(null)).isFalse();
        }

        @Test
        void isEqualWhenParentIsSet() {
            OverrideSet parentA = new OverrideSet(obj("foo", "1.0.0"));
            OverrideSet parentB = new OverrideSet(obj("foo", "1.0.0"));
            OverrideSet childA = new OverrideSet(obj("bar", "2.0.0"), "bar", parentA);
            OverrideSet childB = new OverrideSet(obj("bar", "2.0.0"), "bar", parentB);
            assertThat(childA.isEqual(childB)).isTrue();

            OverrideSet parentC = new OverrideSet(obj("foo", "1.0.0"));
            OverrideSet parentD = new OverrideSet(obj("foo", "1.0.1"));
            OverrideSet childC = new OverrideSet(obj("bar", "2.0.0"), "bar", parentC);
            OverrideSet childD = new OverrideSet(obj("bar", "2.0.0"), "bar", parentD);
            assertThat(childC.isEqual(childD)).isFalse();
        }
    }

    // test/edge.js: overrideset comparison logic
    @Test
    void overrideSetComparisonLogic() {
        OverrideSet overrides1 = new OverrideSet(obj("bar", "^2.0.0"));
        OverrideSet overrides2 = new OverrideSet(obj("bar", "^2.0.0"));
        OverrideSet overrides3 = new OverrideSet(obj("foo", "^2.0.0"));
        OverrideSet overrides4 = new OverrideSet(obj("foo", "^1.0.0"));
        OverrideSet overrides5 = new OverrideSet(obj("bar", "^2.0.0", "foo", "^2.0.0"));
        OverrideSet overrides6 = new OverrideSet(obj());
        OverrideSet overrides7 = new OverrideSet(obj("bar", obj(".", "^2.0.0", "baz", "1.2.3")));

        assertThat(overrides1.isEqual(overrides1)).isTrue();
        assertThat(overrides1.isEqual(overrides2)).isTrue();
        assertThat(overrides1.isEqual(overrides3)).isFalse();
        assertThat(overrides1.isEqual(overrides5)).isFalse();
        assertThat(overrides1.isEqual(overrides6)).isFalse();
        assertThat(overrides1.isEqual(overrides7)).isFalse();
        assertThat(overrides3.isEqual(overrides1)).isFalse();
        assertThat(overrides3.isEqual(overrides4)).isFalse();
        assertThat(overrides3.isEqual(overrides5)).isFalse();
        assertThat(overrides4.isEqual(overrides5)).isFalse();
        assertThat(overrides5.isEqual(overrides1)).isFalse();
        assertThat(overrides5.isEqual(overrides3)).isFalse();
        assertThat(overrides5.isEqual(overrides6)).isFalse();
        assertThat(overrides6.isEqual(overrides1)).isFalse();
        assertThat(overrides6.isEqual(overrides3)).isFalse();
        assertThat(overrides6.isEqual(overrides6)).isTrue();
        assertThat(overrides7.isEqual(overrides1)).isFalse();
    }
}
