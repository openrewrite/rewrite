/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.RecipeList;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.maven.Assertions.pomXml;

/**
 * A faithful miniature of the {@code DependencyVulnerabilityCheck} redesign ("DVC2"): a
 * {@link ScanningRecipe} that makes <em>no edits of its own</em>, discovers an upgrade target from the
 * resolved Maven model in one stage, and schedules {@link UpgradeDependencyVersion} via
 * {@link ScanningRecipe#nextStage} to perform it in the next stage.
 * <p>
 * The fixture is the exact case that broke the original DVC: a version held in a {@code <properties>}
 * entry and consumed through {@code <dependencyManagement>} in the same POM. Maven's UDV performs that
 * edit through deferred {@code doAfterVisit} sub-visitors, which the original DVC dropped by driving
 * UDV's visitor manually via {@code getVisitor(acc).visitNonNull(...)}. Run as a genuine downstream
 * stage, UDV's full lifecycle executes and the property is upgraded — so any change to the POM here can
 * only have come from the scheduled sub-recipe, since the recipe under test never edits.
 */
class NextStageUpgradeTest implements RewriteTest {

    @Test
    void scheduledUpgradeLandsPropertyManagedEdit() {
        rewriteRun(
          spec -> spec.recipe(new AutoUpgradeManagedBom()),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                      <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                      <quarkus.platform.version>1.11.7.Final</quarkus.platform.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>${quarkus.platform.group-id}</groupId>
                              <artifactId>${quarkus.platform.artifact-id}</artifactId>
                              <version>${quarkus.platform.version}</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                      <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                      <quarkus.platform.version>1.13.7.Final</quarkus.platform.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>${quarkus.platform.group-id}</groupId>
                              <artifactId>${quarkus.platform.artifact-id}</artifactId>
                              <version>${quarkus.platform.version}</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    /**
     * Stage one: find the managed BOM to upgrade. Stage two (scheduled, not run inline): upgrade it.
     * The accumulator holds {@code groupId:artifactId:newVersion} targets discovered from the model.
     */
    static class AutoUpgradeManagedBom extends ScanningRecipe<Set<String>> {
        @Override
        public String getDisplayName() {
            return "Schedule a managed-BOM upgrade";
        }

        @Override
        public String getDescription() {
            return "Discovers a managed dependency to upgrade and schedules UpgradeDependencyVersion as a downstream stage.";
        }

        @Override
        public Set<String> getInitialValue(ExecutionContext ctx) {
            return new LinkedHashSet<>();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Set<String> acc) {
            return new MavenIsoVisitor<ExecutionContext>() {
                @Override
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    if ("1.11.7.Final".equals(getResolutionResult().getPom().getProperties().get("quarkus.platform.version"))) {
                        acc.add("io.quarkus:quarkus-universe-bom:1.13.7.Final");
                    }
                    return super.visitDocument(document, ctx);
                }
            };
        }

        @Override
        public void nextStage(RecipeList stage, ExecutionContext ctx, Set<String> acc) {
            for (String target : acc) {
                String[] ga = target.split(":");
                stage.recipe(new UpgradeDependencyVersion(ga[0], ga[1], ga[2], null, null, null));
            }
        }
    }
}
