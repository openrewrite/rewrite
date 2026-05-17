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
package org.openrewrite.android.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.xml.Assertions.xml;

/**
 * End-to-end exercise of the canonical Trait + ScanningRecipe composition pattern
 * documented in {@code package-info.java}. Build an accumulator of declared
 * {@code <string name="...">} entries from XML resources, then flag any
 * {@code R.string.<name>} reference in Java source whose name is not present in
 * the accumulator.
 */
class AndroidTraitScanningRecipeIntegrationTest implements RewriteTest {

    public static class FindMissingStringResources extends ScanningRecipe<Set<String>> {
        @Override
        public String getDisplayName() {
            return "Find missing R.string.* references";
        }

        @Override
        public String getDescription() {
            return "Flag every `R.string.<name>` reference whose `<string name=\"<name>\">` is not declared " +
                    "in any scanned XML resource file.";
        }

        @Override
        public Set<String> getInitialValue(ExecutionContext ctx) {
            return new HashSet<>();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Set<String> known) {
            return new XmlIsoVisitor<ExecutionContext>() {
                @Override
                public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                    if ("string".equals(tag.getName())) {
                        for (Xml.Attribute a : tag.getAttributes()) {
                            if ("name".equals(a.getKeyAsString())) {
                                known.add(a.getValueAsString());
                            }
                        }
                    }
                    return super.visitTag(tag, ctx);
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> known) {
            return new AndroidResourceReference.Matcher().asVisitor((ref, ctx) -> {
                if ("string".equals(ref.getResourceType()) && !known.contains(ref.getResourceName())) {
                    return SearchResult.found(ref.getTree(),
                            "missing string resource: " + ref.getResourceName());
                }
                return ref.getTree();
            });
        }
    }

    @Test
    void flagsOnlyMissingStringResources() {
        rewriteRun(
          spec -> spec.recipe(new FindMissingStringResources()),
          xml(
            """
              <resources>
                  <string name="app_name">Demo</string>
                  <string name="known_label">Known</string>
              </resources>
              """,
            spec -> spec.path("res/values/strings.xml")
          ),
          java(
            """
              class R {
                  static class string {
                      static int app_name = 0;
                      static int known_label = 0;
                      static int missing_label = 0;
                  }
              }
              class Test {
                  int a() { return R.string.app_name; }
                  int b() { return R.string.known_label; }
                  int c() { return R.string.missing_label; }
              }
              """,
            """
              class R {
                  static class string {
                      static int app_name = 0;
                      static int known_label = 0;
                      static int missing_label = 0;
                  }
              }
              class Test {
                  int a() { return R.string.app_name; }
                  int b() { return R.string.known_label; }
                  int c() { return /*~~(missing string resource: missing_label)~~>*/R.string.missing_label; }
              }
              """
          )
        );
    }
}
