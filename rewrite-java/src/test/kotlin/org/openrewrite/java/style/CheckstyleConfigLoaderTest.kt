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
import org.openrewrite.java.cleanup.*
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

    @Test
    fun methodParamPadStyle() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="MethodParamPad">
                <property name="option" value=" space"/>
                <property name="allowLineBreaks" value="true" />
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
            .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(MethodParamPadStyle::class.java)
        val methodParamPadStyle = checkstyle.styles.first() as MethodParamPadStyle

        assertThat(methodParamPadStyle.space).isTrue
        assertThat(methodParamPadStyle.allowLineBreaks).isTrue
    }

    @Test
    fun noWhitespaceAfterStyle() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="NoWhitespaceAfter">
                <property name="allowLineBreaks" value="true"/>
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
            .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(NoWhitespaceAfterStyle::class.java)
        val noWhitespaceAfterStyle = checkstyle.styles.first() as NoWhitespaceAfterStyle

        assertThat(noWhitespaceAfterStyle.allowLineBreaks).isTrue
    }

    @Test
    fun noWhitespaceBeforeStyle() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="NoWhitespaceBefore">
                <property name="allowLineBreaks" value="true"/>
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
            .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(NoWhitespaceBeforeStyle::class.java)
        val noWhitespaceBeforeStyle = checkstyle.styles.first() as NoWhitespaceBeforeStyle

        assertThat(noWhitespaceBeforeStyle.allowLineBreaks).isTrue
    }

    @Test
    fun needBraces() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="NeedBraces">
                <property name="allowSingleLineStatement" value="true"/>
                <property name="allowEmptyLoopBody" value="true"/>
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
            .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(NeedBracesStyle::class.java)
        val needBracesStyle = checkstyle.styles.first() as NeedBracesStyle

        assertThat(needBracesStyle.allowSingleLineStatement).isTrue
        assertThat(needBracesStyle.allowEmptyLoopBody).isTrue
    }

    @Test
    fun operatorWrap() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="OperatorWrap">
                <property name="option" value="EOL"/>
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
            .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(OperatorWrapStyle::class.java)
        val operatorWrapStyle = checkstyle.styles.first() as OperatorWrapStyle

        assertThat(operatorWrapStyle.wrapOption).isEqualTo(OperatorWrapStyle.WrapOption.EOL)
    }

    @Test
    fun typecastParenPad() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="TypecastParenPad">
                <property name="option" value=" space"/>
              </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles)
            .hasSize(1)

        assertThat(checkstyle.styles.first()).isExactlyInstanceOf(TypecastParenPadStyle::class.java)
        val needBracesStyle = checkstyle.styles.first() as TypecastParenPadStyle

        assertThat(needBracesStyle.space).isTrue
    }


    @Test
    fun duplicatedModuleNames() {
        val checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="UnnecessaryParentheses">
                    <property name="id" value="expr"/>
                    <property name="tokens" value="EXPR"/>
                </module>
                <module name="UnnecessaryParentheses">
                    <property name="id" value="stringLiteral"/>
                    <property name="tokens" value="STRING_LITERAL"/>
                </module>
            </module>
        """.trimIndent(), emptyMap())

        assertThat(checkstyle.styles).hasSize(2)
        assertThat(checkstyle.styles).allMatch { it is UnnecessaryParenthesesStyle }

        val unnecessaryParenthesesStyle = checkstyle.styles.first() as UnnecessaryParenthesesStyle
        assertThat(unnecessaryParenthesesStyle.stringLiteral).isTrue
    }
}
