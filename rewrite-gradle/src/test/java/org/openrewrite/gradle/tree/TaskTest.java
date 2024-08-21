/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.gradle.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.gradle.Assertions.buildGradle;

class TaskTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/1236")
    @Test
    void declareTaskOldStyle() {
        rewriteRun(
          spec -> spec.afterTypeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              task(testWithCloud, type: Test) {
                useTestNG()
                options.excludeGroups = [] as Set
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1929")
    @Test
    void dsl() {
        rewriteRun(
          spec -> spec.afterTypeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              test {
                  // Ex: -PexcludeTests=com/google/cloud/healthcare/etl/runner/hl7v2tofhir/integ/*
                  if (project.hasProperty('excludeTests')) {
                      exclude project.property('excludeTests') as String
                  }
                  dependsOn('buildDeps')
              }
              """
          )
        );
    }
}
