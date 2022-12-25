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
package org.openrewrite.java.style;

import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.style.CheckstyleConfigLoader.loadCheckstyleConfig;

class CheckstyleConfigLoaderTest {

    @Test
    void basicSingleStyle() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="DefaultComesLast"/>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next())
                .isExactlyInstanceOf(DefaultComesLastStyle.class)
                .matches(s -> !((DefaultComesLastStyle) s).getSkipIfLastAndSharedWithCase());
    }

    @Test
    void singleStyleWithProperty() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="DefaultComesLast">
                    <property name="skipIfLastAndSharedWithCase" value="true"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles())
                .hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next())
                .isExactlyInstanceOf(DefaultComesLastStyle.class)
                .matches(s -> ((DefaultComesLastStyle) s).getSkipIfLastAndSharedWithCase());
    }

    @Test
    void emptyBlockStyle() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="EmptyBlock">
                    <property name="option" value="text" />
                    <property name="tokens" value="LITERAL_WHILE, LITERAL_TRY"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(EmptyBlockStyle.class);
        EmptyBlockStyle blockStyle = (EmptyBlockStyle) checkstyle.getStyles().iterator().next();

        assertThat(blockStyle.getBlockPolicy()).isEqualTo(EmptyBlockStyle.BlockPolicy.TEXT);
        assertThat(blockStyle.getLiteralWhile()).isTrue();
        assertThat(blockStyle.getLiteralTry()).isTrue();
        assertThat(blockStyle.getLiteralCatch()).isFalse();
    }

    @Test
    void equalsAvoidsNull() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="EqualsAvoidNull">
                    <property name="ignoreEqualsIgnoreCase" value="true" />
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles())
                .hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(EqualsAvoidsNullStyle.class);
        EqualsAvoidsNullStyle equalsAvoidsNullStyle = (EqualsAvoidsNullStyle) checkstyle.getStyles().iterator().next();

        assertThat(equalsAvoidsNullStyle.getIgnoreEqualsIgnoreCase()).isTrue();
    }

    @Test
    void insideTreeWalker() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
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
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(EqualsAvoidsNullStyle.class);
        EqualsAvoidsNullStyle equalsAvoidsNullStyle = (EqualsAvoidsNullStyle) checkstyle.getStyles().iterator().next();

        assertThat(equalsAvoidsNullStyle.getIgnoreEqualsIgnoreCase()).isTrue();
    }

    @Test
    void moduleNameEndsWithCheck() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="EqualsAvoidNullCheck">
                    <property name="ignoreEqualsIgnoreCase" value="true" />
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(EqualsAvoidsNullStyle.class);
        EqualsAvoidsNullStyle equalsAvoidsNullStyle = (EqualsAvoidsNullStyle) checkstyle.getStyles().iterator().next();

        assertThat(equalsAvoidsNullStyle.getIgnoreEqualsIgnoreCase()).isTrue();
    }

    @Test
    void emptyForPadInitializer() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="EmptyForInitializerPad">
                <property name="option" value=" space"/>
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(EmptyForInitializerPadStyle.class);
        EmptyForInitializerPadStyle emptyForPadInitializerStyle = (EmptyForInitializerPadStyle) checkstyle.getStyles().iterator().next();

        assertThat(emptyForPadInitializerStyle.getSpace()).isTrue();
    }

    @Test
    void methodParamPadStyle() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="MethodParamPad">
                <property name="option" value=" space"/>
                <property name="allowLineBreaks" value="true" />
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(MethodParamPadStyle.class);
        MethodParamPadStyle methodParamPadStyle = (MethodParamPadStyle) checkstyle.getStyles().iterator().next();

        assertThat(methodParamPadStyle.getSpace()).isTrue();
        assertThat(methodParamPadStyle.getAllowLineBreaks()).isTrue();
    }

    @Test
    void noWhitespaceAfterStyle() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="NoWhitespaceAfter">
                <property name="allowLineBreaks" value="true"/>
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(NoWhitespaceAfterStyle.class);
        NoWhitespaceAfterStyle noWhitespaceAfterStyle = (NoWhitespaceAfterStyle) checkstyle.getStyles().iterator().next();

        assertThat(noWhitespaceAfterStyle.getAllowLineBreaks()).isTrue();
    }

    @Test
    void noWhitespaceBeforeStyle() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="NoWhitespaceBefore">
                <property name="allowLineBreaks" value="true"/>
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(NoWhitespaceBeforeStyle.class);
        NoWhitespaceBeforeStyle noWhitespaceBeforeStyle = (NoWhitespaceBeforeStyle) checkstyle.getStyles().iterator().next();

        assertThat(noWhitespaceBeforeStyle.getAllowLineBreaks()).isTrue();
    }

    @Test
    void needBraces() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="NeedBraces">
                <property name="allowSingleLineStatement" value="true"/>
                <property name="allowEmptyLoopBody" value="true"/>
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(NeedBracesStyle.class);
        NeedBracesStyle needBracesStyle = (NeedBracesStyle) checkstyle.getStyles().iterator().next();

        assertThat(needBracesStyle.getAllowSingleLineStatement()).isTrue();
        assertThat(needBracesStyle.getAllowEmptyLoopBody()).isTrue();
    }

    @Test
    void operatorWrap() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="OperatorWrap">
                <property name="option" value="EOL"/>
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(OperatorWrapStyle.class);
        OperatorWrapStyle operatorWrapStyle = (OperatorWrapStyle) checkstyle.getStyles().iterator().next();

        assertThat(operatorWrapStyle.getWrapOption()).isEqualTo(OperatorWrapStyle.WrapOption.EOL);
    }

    @Test
    void typecastParenPad() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
              <module name="TypecastParenPad">
                <property name="option" value=" space"/>
              </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1);

        assertThat(checkstyle.getStyles().iterator().next()).isExactlyInstanceOf(TypecastParenPadStyle.class);
        TypecastParenPadStyle typecastParenPadStyle = (TypecastParenPadStyle) checkstyle.getStyles().iterator().next();

        assertThat(typecastParenPadStyle.getSpace()).isTrue();
    }


    @Test
    void duplicatedModuleNames() throws CheckstyleException {
        var checkstyle = loadCheckstyleConfig("""
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
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(2);
        assertThat(checkstyle.getStyles()).allMatch(s -> s instanceof UnnecessaryParenthesesStyle);

        UnnecessaryParenthesesStyle unnecessaryParenthesesStyle = (UnnecessaryParenthesesStyle) checkstyle.getStyles().iterator().next();
        assertThat(unnecessaryParenthesesStyle.getStringLiteral()).isTrue();
    }
}
