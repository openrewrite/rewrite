package org.openrewrite.java.upgrade;

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
