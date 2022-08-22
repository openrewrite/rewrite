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
package org.openrewrite.maven

import org.junit.jupiter.api.Test
import org.openrewrite.maven.Assertions.pomXml
import org.openrewrite.test.RewriteTest

class UpgradeVersionPropertyTest : RewriteTest {
    @Test
    fun `upgrades old version`() = rewriteRun(
        { spec -> spec.recipe(UpgradeVersionProperty("jacoco.version", "0.8.8", null, false)) },
        pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <jacoco.version>0.8.7</jacoco.version>
              </properties>
            </project>
        """, """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <jacoco.version>0.8.8</jacoco.version>
              </properties>
            </project>
        """)
    )

    @Test
    fun `keeps newer version`() = rewriteRun(
        { spec -> spec.recipe(UpgradeVersionProperty("jacoco.version", "0.8.8", null, false)) },
        pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <jacoco.version>1.2.3</jacoco.version>
              </properties>
            </project>
        """)
    )

    @Test
    fun `missing property with addIfMissing false`() = rewriteRun(
        { spec -> spec.recipe(UpgradeVersionProperty("jacoco.version", "0.8.8", null, false)) },
        pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <log4j.version>1.2.3</log4j.version>
              </properties>
            </project>
        """)
    )

    @Test
    fun `missing property with addIfMissing true`() = rewriteRun(
        { spec -> spec.recipe(UpgradeVersionProperty("jacoco.version", "0.8.8", null, true)) },
        pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <log4j.version>1.2.3</log4j.version>
              </properties>
            </project>
        """, """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <jacoco.version>0.8.8</jacoco.version>
                <log4j.version>1.2.3</log4j.version>
              </properties>
            </project>
        """)
    )

    @Test
    fun `no properties with addIfMissing true`() = rewriteRun(
        { spec -> spec.recipe(UpgradeVersionProperty("jacoco.version", "0.8.8", null, true)) },
        pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
            </project>
        """, """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>hello.maven</groupId>
              <artifactId>hello-maven</artifactId>
              <version>0.1.0</version>
              <properties>
                <jacoco.version>0.8.8</jacoco.version>
              </properties>
            </project>
        """)
    )
}