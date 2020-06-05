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
package org.openrewrite.xml.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.assertRefactored

class SetParentPomVersionTest : XmlParser() {
    val setBoot23 = SetParentPomVersion().apply {
        setWhenGroupId("org.springframework.boot")
        setWhenArtifactId("spring-boot-starter-parent")
        setVersion("2.3.0.RELEASE")
    }

    @Test
    fun changeParentPomVersionWhenMatchingGroupAndArtifact() {
        val x = parse("""
            <project>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.0.0.RELEASE</version>
              </parent>
            </project>
        """.trimIndent())

        val fixed = x.refactor().visit(setBoot23).fix().fixed

        assertRefactored(fixed, """
            <project>
              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.3.0.RELEASE</version>
              </parent>
            </project>
        """.trimIndent())
    }

    @Test
    fun dontChangeParentPomVersionWhenGroupDoesntMatch() {
        val x = parse("""
            <project>
              <parent>
                <groupId>org.springframework</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>2.0.0.RELEASE</version>
              </parent>
            </project>
        """.trimIndent())

        assertThat(x.refactor()
                .visit(setBoot23)
                .fix()
                .rulesThatMadeChanges).isEmpty()
    }
}
