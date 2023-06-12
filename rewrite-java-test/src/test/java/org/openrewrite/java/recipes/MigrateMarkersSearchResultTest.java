/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


import static org.openrewrite.java.Assertions.java;

class MigrateMarkersSearchResultTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateMarkersSearchResult())
          .parser(JavaParser.fromJavaVersion()
            .classpath(JavaParser.runtimeClasspath())
          );
    }

    @SuppressWarnings("all")
    @Test
    void migrate() {
        rewriteRun(
          java(
            """
              package org.openrewrite.kubernetes.resource;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.*;
              import org.openrewrite.yaml.YamlIsoVisitor;
              import org.openrewrite.yaml.tree.Yaml;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class FindExceedsResourceRatio extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Find exceeds resource ratio";
                  }

                  @Override
                  protected TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new YamlIsoVisitor<ExecutionContext>() {
                          @Override
                          public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                              Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                              return e.withMarkers(e.getMarkers().searchResult("foo"));
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.kubernetes.resource;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.*;
              import org.openrewrite.marker.SearchResult;
              import org.openrewrite.yaml.YamlIsoVisitor;
              import org.openrewrite.yaml.tree.Yaml;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class FindExceedsResourceRatio extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Find exceeds resource ratio";
                  }

                  @Override
                  protected TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new YamlIsoVisitor<ExecutionContext>() {
                          @Override
                          public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                              Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                              return SearchResult.found(e, "foo");
                          }
                      };
                  }
              }
              """
          )
        );
    }
}
