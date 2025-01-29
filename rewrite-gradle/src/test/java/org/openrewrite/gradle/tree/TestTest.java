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

class TestTest implements RewriteTest {

    @Test
    void dsl() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              test {
                 useJUnit()
                 useTestNG()
                 useJUnitPlatform()
                 systemProperty 'some.prop', 'value'
                 include 'org/foo/**'
                 exclude 'org/boo/**'
                 testLogging.showStandardStreams = true
                 minHeapSize = "128m"
                 maxHeapSize = "512m"
                 jvmArgs '-XX:MaxPermSize=256m'
                 failFast = true
              }
              """
          )
        );
    }
}
