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
package org.openrewrite.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.binary.Binary
import org.openrewrite.properties.PropertiesParser
import org.openrewrite.properties.tree.Properties
import org.openrewrite.text.PlainText
import java.nio.file.Paths

class UpdateGradleWrapperTest {

    @Test
    fun updateVersionAndDistribution() {
        val gradleWrapperProps = PropertiesParser().parse(
            """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\://services.gradle.org/distributions/gradle-7.4-all.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
            """.trimIndent())
            .first()
            .withSourcePath(Paths.get("gradle", "wrapper", "gradle-wrapper.properties"))

        val result = UpdateGradleWrapper("7.4.2", "bin")
            .run(listOf(gradleWrapperProps))
            .map { it.after }
        assertThat(result.size).isEqualTo(4)

        assertThat(result.filterIsInstance<Properties.File>().first().printAll()).isEqualTo(
            //language=properties
            """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\://services.gradle.org/distributions/gradle-7.4.2-bin.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
            """.trimIndent()
        )

        val gradleSh = result.filterIsInstance<PlainText>().first { it.sourcePath.endsWith("gradlew") }
        assertThat(gradleSh).isNotNull
        assertThat(gradleSh.text).isNotBlank

        val gradleBat = result.filterIsInstance<PlainText>().first { it.sourcePath.endsWith("gradlew.bat") }
        assertThat(gradleBat).isNotNull
        assertThat(gradleBat.text).isNotBlank

        val gradleWrapperJar = result.filterIsInstance<Binary>().first { it.sourcePath.endsWith("gradle-wrapper.jar") }
        assertThat(gradleWrapperJar).isNotNull
        assertThat(gradleWrapperJar.bytes).isNotEmpty
    }

    @Test
    fun updateVersionPreserveDistribution() {
        val gradleWrapperProps = PropertiesParser().parse(
            """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\://services.gradle.org/distributions/gradle-7.4-all.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
            """.trimIndent())
            .first()
            .withSourcePath(Paths.get("gradle", "wrapper", "gradle-wrapper.properties"))

        val result = UpdateGradleWrapper("7.4.2", null)
            .run(listOf(gradleWrapperProps))
            .map { it.after }
        assertThat(result.size).isEqualTo(4)

        assertThat(result.filterIsInstance<Properties.File>().first().printAll()).isEqualTo(
            //language=properties
            """
                distributionBase=GRADLE_USER_HOME
                distributionPath=wrapper/dists
                distributionUrl=https\://services.gradle.org/distributions/gradle-7.4.2-all.zip
                zipStoreBase=GRADLE_USER_HOME
                zipStorePath=wrapper/dists
            """.trimIndent()
        )
    }
}
