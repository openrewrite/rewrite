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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class BuildscriptTest implements RewriteTest {

    @Test
    void buildscriptWithDependencies() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      maven {
                          url "https://plugins.gradle.org/m2/"
                      }
                      mavenCentral()
                  }
                  dependencies {
                      classpath 'com.netflix.nebula:nebula-dependency-recommender:9.1.1'
                      classpath 'com.netflix.nebula:gradle-netflixoss-project-plugin:9.1.0'
                  }
              }
              """
          )
        );
    }
}
