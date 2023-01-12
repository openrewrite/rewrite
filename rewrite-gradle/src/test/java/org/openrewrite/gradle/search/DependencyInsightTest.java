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
                Dependency requestedOrgWhatever = new Dependency(
                  new GroupArtifactVersion("org.whatever", "test", "1.0"),
                  null, "jar", "implementation", emptyList(), false);
                ResolvedDependency resolvedOrgWhatever = new ResolvedDependency(null,
                  new ResolvedGroupArtifactVersion(null, "org.whatever", "test", "1.0", null),
                  requestedOrgWhatever, emptyList(), emptyList(), 0);


                Dependency requestedBingBaz = new Dependency(
                  new GroupArtifactVersion("com.example", "bing-baz", "latest.release"),
                  null, "jar", "implementation", emptyList(), false);
                ResolvedDependency resolvedBingBaz = new ResolvedDependency(null,
                  new ResolvedGroupArtifactVersion(null, "com.example", "bing-baz", "1.0", null),
                  requestedBingBaz, emptyList(), emptyList(), 1);
                Dependency requestedComExample = new Dependency(
                  new GroupArtifactVersion("com.example", "foo-bar", "latest.release"),
                  null, "jar", "implementation", emptyList(), false);
                ResolvedDependency resolvedComExample = new ResolvedDependency(null,
                  new ResolvedGroupArtifactVersion(null, "com.example", "foo-bar", "1.0", null),
                  requestedComExample, List.of(resolvedBingBaz), emptyList(), 0);

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
