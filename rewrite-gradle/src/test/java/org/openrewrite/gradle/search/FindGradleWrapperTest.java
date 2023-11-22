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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

class FindGradleWrapperTest implements RewriteTest {
    @DocumentExample
    @Test
    void findGradleWrapperVersion() {
        rewriteRun(
          spec -> spec.recipe(new FindGradleWrapper("[6,)", null, null)),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              ~~>distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          )
        );
    }

    @Test
    void findGradleWrapperDistribution() {
        rewriteRun(
          spec -> spec.recipe(new FindGradleWrapper(null, null, "all")),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              ~~>distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          )
        );
    }

    @Test
    void findGradleWrapperVersionAndDistribution() {
        rewriteRun(
          spec -> spec.recipe(new FindGradleWrapper("[6,)", null, "all")),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              ~~>distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          )
        );
    }

    @Test
    void findWrapperDefaults() {
        rewriteRun(
          spec -> spec.recipe(new FindGradleWrapper(null, null, null)),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              ~~>distributionUrl=https\\\\://services.gradle.org/distributions/gradle-7.4-all.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
          )
        );
    }
}
