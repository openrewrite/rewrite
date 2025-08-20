/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Tree;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class HasBuildToolVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new HasBuildToolVersion(BuildTool.Type.Maven, "3.6.0-3.8.0"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "3.6.0",
      "3.6.1",
      "3.8.0"
    })
    void detectMavenVersionWithinRange(String version) {
        rewriteRun(
          java("class A {}", "/*~~>*/class A {}",
            spec -> spec.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, version)))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "3.5.4",
      "3.8.1",
      "4.0.0"
    })
    void doNotDetectVersionsOutsideRange() {
        rewriteRun(
          java("class A {}",
            spec -> spec.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Maven, "3.5.4")))
        );
    }

    @Test
    void doNotDetectGradleVersion() {
        rewriteRun(
          java("class A {}",
            spec -> spec.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "4.10.0")))
        );
    }
}
