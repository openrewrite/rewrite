/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.Assertions.buildGradle;

public class DependencyInsightTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DependencyInsight("com.example", "bing-*", null));
    }

    @Test
    void findTransitiveDependency() {
        rewriteRun(
          buildGradle("""
              dependencies {
                  implementation 'org.whatever:test:1.0'
                  implementation 'com.example:foo-bar:latest.release'
                  implementation group: 'com.example', name: 'foo-bar', version: '1.0'
              }
              """,
            """
              dependencies {
                  implementation 'org.whatever:test:1.0'
                  /*~~(com.example:bing-baz:1.0)~~>*/implementation 'com.example:foo-bar:latest.release'
                  /*~~(com.example:bing-baz:1.0)~~>*/implementation group: 'com.example', name: 'foo-bar', version: '1.0'
              }
              """,
            spec -> {
                // This is really awkward, need a DSL or some other tooling support
                Dependency requestedOrgWhatever = Dependency.builder()
                  .gav(new GroupArtifactVersion("org.whatever", "test", "1.0"))
                  .type("jar")
                  .scope("implementation")
                  .build();
                ResolvedDependency resolvedOrgWhatever = ResolvedDependency.builder()
                  .gav(new ResolvedGroupArtifactVersion(null, "org.whatever", "test", "1.0", null))
                  .requested(requestedOrgWhatever)
                  .dependencies(emptyList())
                  .licenses(emptyList())
                  .build();

                Dependency requestedBingBaz = Dependency.builder()
                  .gav(new GroupArtifactVersion("com.example", "bing-baz", "latest.release"))
                  .type("jar")
                  .scope("implementation")
                  .build();
                ResolvedDependency resolvedBingBaz = ResolvedDependency.builder()
                  .gav(new ResolvedGroupArtifactVersion(null, "com.example", "bing-baz", "1.0", null))
                  .requested(requestedBingBaz)
                  .dependencies(emptyList())
                  .depth(1)
                  .build();

                Dependency requestedComExample = Dependency.builder()
                  .gav(new GroupArtifactVersion("com.example", "foo-bar", "latest.release"))
                  .type("jar")
                  .scope("implementation")
                  .build();
                ResolvedDependency resolvedComExample = ResolvedDependency.builder()
                  .gav(new ResolvedGroupArtifactVersion(null, "com.example", "foo-bar", "1.0", null))
                  .requested(requestedComExample)
                  .dependencies(List.of(resolvedBingBaz))
                  .depth(0)
                  .build();

                spec.markers(new GradleProject(
                  randomId(),
                  "project",
                  ":",
                  emptyList(),
                  emptyList(),
                  Map.of("implementation", new GradleDependencyConfiguration(
                    "implementation", "implementation configuration", true, true, emptyList(),
                    List.of(requestedOrgWhatever, requestedComExample),
                    List.of(resolvedOrgWhatever, resolvedComExample)
                  ))
                ));
            })
        );
    }
}
