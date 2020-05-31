/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml.search.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser

class ReadPomTest : XmlParser() {
    @Test
    fun properties() {
        val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <properties>
                    <java.version>11</java.version>
            
                    <!-- Plugins -->
                    <maven-surefire-plugin.version>2.22.2</maven-surefire-plugin.version>
                </properties>
            </project>
        """.trimIndent())

        val pom = ReadPom().visit(x)

        assertThat(pom.properties).containsExactlyInAnyOrderEntriesOf(
                mapOf(
                        "java.version" to "11",
                        "maven-surefire-plugin.version" to "2.22.2"
                )
        )
    }

    @Test
    fun dependencies() {
        val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.openrewrite.plan</groupId>
                        <artifactId>rewrite-checkstyle</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent())

        val pom = ReadPom().visit(x)

        assertThat(pom.dependencies).containsExactly(
                Dependency("org.openrewrite.plan", "rewrite-checkstyle", "1.0.0", null))
    }
}
