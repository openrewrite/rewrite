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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.yaml.Assertions.yaml;

class UpdateStaticAnalysisPackageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
          spec.recipe(
              Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes("org.openrewrite.java.upgrade.UpdateStaticAnalysisPackage")
            )
            .typeValidationOptions(TypeValidation.none());
    }

    @SuppressWarnings("all")
    @DocumentExample("Update referencing places in java file.")
    @Test
    void changeCleanUpToStaticanalysisForSpecificClassOnly() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate;

              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.TreeVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.cleanup.UnnecessaryCatch;
              import org.openrewrite.java.tree.J;

              public class UseJavaUtilBase64 extends Recipe {

                  @Override
                  public String getDisplayName() {return "Prefer `java.util.Base64` instead of `sun.misc`";}

                  @Override
                  public String getDescription() {return "Prefer `java.util.Base64` instead of `sun.misc`.";}

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaVisitor<ExecutionContext>() {
                          @Override
                          public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                              J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                              // expect to change
                              doAfterVisit(new UnnecessaryCatch(false).getVisitor());
                              var v1 = new org.openrewrite.java.cleanup.UnnecessaryCatch(true);
                              return m;
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.java.migrate;

              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.TreeVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;
              import org.openrewrite.staticanalysis.UnnecessaryCatch;

              public class UseJavaUtilBase64 extends Recipe {

                  @Override
                  public String getDisplayName() {return "Prefer `java.util.Base64` instead of `sun.misc`";}

                  @Override
                  public String getDescription() {return "Prefer `java.util.Base64` instead of `sun.misc`.";}

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaVisitor<ExecutionContext>() {
                          @Override
                          public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                              J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                              // expect to change
                              doAfterVisit(new UnnecessaryCatch(false).getVisitor());
                              var v1 = new org.openrewrite.staticanalysis.UnnecessaryCatch(true);
                              return m;
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @DocumentExample("Update referencing places in yaml file.")
    @Test
    void changeYamlRecipeList() {
        rewriteRun(
          yaml("""
            type: specs.openrewrite.org/v1beta/recipe
            name: org.example.bank.Internal
            displayName: org.example.bank.Internal
            description: org.example.bank.Internal
            recipeList:
              - org.openrewrite.java.cleanup.AddSerialVersionUidToSerializable
            """,
            """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.example.bank.Internal
            displayName: org.example.bank.Internal
            description: org.example.bank.Internal
            recipeList:
              - org.openrewrite.staticanalysis.AddSerialVersionUidToSerializable
            """));
    }

    @DocumentExample("Update referencing places in pom.xml.")
    @Test
    void changePomXmlConfiguration() {
        rewriteRun(
          pomXml("""
            <project>
              <groupId>org.example</groupId>
              <artifactId>foo</artifactId>
              <version>1.0</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.45.0</version>
                    <configuration>
                      <activeRecipes>
                        <recipe>org.openrewrite.java.cleanup.AddSerialVersionUidToSerializable</recipe>
                      </activeRecipes>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """, """
            <project>
              <groupId>org.example</groupId>
              <artifactId>foo</artifactId>
              <version>1.0</version>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>4.45.0</version>
                    <configuration>
                      <activeRecipes>
                        <recipe>org.openrewrite.staticanalysis.AddSerialVersionUidToSerializable</recipe>
                      </activeRecipes>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>"""));
    }
}
