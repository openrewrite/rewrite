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
package org.openrewrite.java.style

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.cleanup.DefaultComesLastStyle
import org.openrewrite.java.cleanup.EmptyBlockStyle
import org.openrewrite.java.cleanup.EmptyForInitializerPadStyle
import org.openrewrite.java.cleanup.EqualsAvoidsNullStyle
import org.openrewrite.java.style.CheckstyleConfigLoader.loadCheckstyleConfig

class CheckstyleConfigLoaderTest {

    @Test
    fun basicSingleStyle() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="DefaultComesLast"/>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first())
                .isExactlyInstanceOf(DefaultComesLastStyle::class.java)
                .matches { (it as DefaultComesLastStyle).skipIfLastAndSharedWithCase == false }
    }

    @Test
    fun singleStyleWithProperty() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="DefaultComesLast">
                    <property name="skipIfLastAndSharedWithCase" value="true"/>
                </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first())
                .isExactlyInstanceOf(DefaultComesLastStyle::class.java)
                .matches { (it as DefaultComesLastStyle).skipIfLastAndSharedWithCase }
    }

    @Test
    fun emptyBlockStyle() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="EmptyBlock">
                    <property name="option" value="text" />
                    <property name="tokens" value="LITERAL_WHILE, LITERAL_TRY"/>
                </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(EmptyBlockStyle::class.java)
        val blockStyle = checkstyle.styles.first() as EmptyBlockStyle

        assertThat(blockStyle.blockPolicy).isEqualTo(EmptyBlockStyle.BlockPolicy.TEXT)
        assertThat(blockStyle.literalWhile).isTrue
        assertThat(blockStyle.literalTry).isTrue
        assertThat(blockStyle.literalCatch).isFalse
    }

    @Test
    fun equalsAvoidsNull() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="EqualsAvoidNull">
                    <property name="ignoreEqualsIgnoreCase" value="true" />
                </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(EqualsAvoidsNullStyle::class.java)
        val equalsAvoidsNullStyle = checkstyle.styles.first() as EqualsAvoidsNullStyle

        assertThat(equalsAvoidsNullStyle.ignoreEqualsIgnoreCase).isTrue
    }

    @Test
    fun insideTreeWalker() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="EqualsAvoidNull">
                        <property name="ignoreEqualsIgnoreCase" value="true" />
                    </module>
                </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(EqualsAvoidsNullStyle::class.java)
        val equalsAvoidsNullStyle = checkstyle.styles.first() as EqualsAvoidsNullStyle

        assertThat(equalsAvoidsNullStyle.ignoreEqualsIgnoreCase).isTrue
    }

    @Test
    fun moduleNameEndsWithCheck() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="EqualsAvoidNullCheck">
                    <property name="ignoreEqualsIgnoreCase" value="true" />
                </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(EqualsAvoidsNullStyle::class.java)
        val equalsAvoidsNullStyle = checkstyle.styles.first() as EqualsAvoidsNullStyle

        assertThat(equalsAvoidsNullStyle.ignoreEqualsIgnoreCase).isTrue
    }

    @Test
    fun emptyForPadInitializer() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="EmptyForInitializerPad">
                <property name="option" value=" space"/>
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
                .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(EmptyForInitializerPadStyle::class.java)
        val emptyForPadInitializerStyle = checkstyle.styles.first() as EmptyForInitializerPadStyle

        assertThat(emptyForPadInitializerStyle.space).isTrue
    }
}
