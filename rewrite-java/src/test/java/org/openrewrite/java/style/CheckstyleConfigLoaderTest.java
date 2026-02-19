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

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.style.CheckstyleConfigLoader.loadCheckstyleConfig;

class CheckstyleConfigLoaderTest {

    @Test
    void basicSingleStyle() throws Exception {
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
    void singleStyleWithProperty() throws Exception {
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
    void emptyBlockStyle() throws Exception {
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
    void equalsAvoidsNull() throws Exception {
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
    void insideTreeWalker() throws Exception {
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
    void moduleNameEndsWithCheck() throws Exception {
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
    void emptyForPadInitializer() throws Exception {
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
    void methodParamPadStyle() throws Exception {
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
    void noWhitespaceAfterStyle() throws Exception {
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
    void noWhitespaceBeforeStyle() throws Exception {
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
    void needBraces() throws Exception {
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
    void operatorWrap() throws Exception {
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
    void typecastParenPad() throws Exception {
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
    void duplicatedModuleNames() throws Exception {
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

    @Test
    void customImportOrderStyle() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="CustomImportOrder">
                        <property name="specialImportsRegExp" value="^org\\."/>
                        <property name="thirdPartyPackageRegExp" value="^com\\."/>
                        <property name="standardPackageRegExp" value="^(java\\.|javax\\.)"/>
                        <property name="customImportOrderRules"
                       value="STANDARD_JAVA_PACKAGE###THIRD_PARTY_PACKAGE###SPECIAL_IMPORTS###STATIC"/>
                </module>
            </module>
        """, emptyMap());

        // CustomImportOrder is converted to ImportLayoutStyle
        assertThat(checkstyle.getStyles()).hasSize(1);
        assertThat(checkstyle.getStyles()).allMatch(s -> s instanceof ImportLayoutStyle);

        ImportLayoutStyle importLayout = (ImportLayoutStyle) checkstyle.getStyles().iterator().next();
        // Rules: STANDARD_JAVA_PACKAGE, THIRD_PARTY_PACKAGE, SPECIAL_IMPORTS, STATIC
        // with separateLineBetweenGroups=true (default)
        // Should produce: java.*, javax.*, blank, all-others, blank, org.*, blank, static-all-others
        assertThat(importLayout.getLayout().stream()
                .filter(b -> b.getClass().equals(ImportLayoutStyle.Block.ImportPackage.class))
                .count()).isGreaterThanOrEqualTo(2); // java.*, javax.*, org.*

        assertThat(importLayout.getLayout().stream()
                .filter(b -> b instanceof ImportLayoutStyle.Block.AllOthers)
                .count()).isEqualTo(2); // non-static catch-all + static catch-all
    }

    @Test
    void unusedImportsWithJavadocProcessing() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="UnusedImports">
                        <property name="processJavadoc" value="true"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1).allSatisfy(style -> {
            assertThat(style).isInstanceOf(UnusedImportsStyle.class);
            assertThat(((UnusedImportsStyle) style).isProcessJavadoc()).isTrue();
        });
    }

    @Test
    void unusedImportsWithoutExpliciteJavadocProcessing() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="UnusedImports">
                        <property name="processJavadoc" value="false"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1).allSatisfy(style -> {
            assertThat(style).isInstanceOf(UnusedImportsStyle.class);
            assertThat(((UnusedImportsStyle) style).isProcessJavadoc()).isFalse();
        });
    }

    @Test
    void unusedImportsWithoutJavadocProcessing() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="UnusedImports"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).hasSize(1).allSatisfy(style -> {
            assertThat(style).isInstanceOf(UnusedImportsStyle.class);
            assertThat(((UnusedImportsStyle) style).isProcessJavadoc()).isFalse();
        });
    }

    @Test
    void avoidStarImport() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="AvoidStarImport"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(ImportLayoutStyle.class);
            ImportLayoutStyle importLayout = (ImportLayoutStyle) style;
            assertThat(importLayout.getClassCountToUseStarImport()).isEqualTo(Integer.MAX_VALUE);
            assertThat(importLayout.getNameCountToUseStarImport()).isEqualTo(Integer.MAX_VALUE);
        });
    }

    @Test
    void lineLengthDefault() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="LineLength"/>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(WrappingAndBracesStyle.class);
            WrappingAndBracesStyle wrapping = (WrappingAndBracesStyle) style;
            assertThat(wrapping.getHardWrapAt()).isEqualTo(80);
        });
    }

    @Test
    void lineLengthCustom() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="LineLength">
                    <property name="max" value="145"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(WrappingAndBracesStyle.class);
            WrappingAndBracesStyle wrapping = (WrappingAndBracesStyle) style;
            assertThat(wrapping.getHardWrapAt()).isEqualTo(145);
        });
    }

    @Test
    void fileTabCharacter() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="FileTabCharacter">
                    <property name="eachLine" value="true"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(TabsAndIndentsStyle.class);
            TabsAndIndentsStyle tabs = (TabsAndIndentsStyle) style;
            assertThat(tabs.getUseTabCharacter()).isFalse();
        });
    }

    @Test
    void indentation() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="Indentation">
                        <property name="basicOffset" value="2"/>
                        <property name="tabWidth" value="2"/>
                        <property name="lineWrappingIndentation" value="4"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(TabsAndIndentsStyle.class);
            TabsAndIndentsStyle tabs = (TabsAndIndentsStyle) style;
            assertThat(tabs.getIndentSize()).isEqualTo(2);
            assertThat(tabs.getTabSize()).isEqualTo(2);
            assertThat(tabs.getContinuationIndent()).isEqualTo(4);
        });
    }

    @Test
    void rightCurlySame() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="RightCurly">
                        <property name="option" value="same"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(WrappingAndBracesStyle.class);
            WrappingAndBracesStyle wrapping = (WrappingAndBracesStyle) style;
            assertThat(wrapping.getIfStatement()).isNotNull();
            assertThat(wrapping.getIfStatement().getElseOnNewLine()).isFalse();
            assertThat(wrapping.getTryStatement()).isNotNull();
            assertThat(wrapping.getTryStatement().getCatchOnNewLine()).isFalse();
            assertThat(wrapping.getTryStatement().getFinallyOnNewLine()).isFalse();
        });
    }

    @Test
    void rightCurlyAlone() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="RightCurly">
                        <property name="option" value="alone"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(WrappingAndBracesStyle.class);
            WrappingAndBracesStyle wrapping = (WrappingAndBracesStyle) style;
            assertThat(wrapping.getIfStatement()).isNotNull();
            assertThat(wrapping.getIfStatement().getElseOnNewLine()).isTrue();
            assertThat(wrapping.getTryStatement()).isNotNull();
            assertThat(wrapping.getTryStatement().getCatchOnNewLine()).isTrue();
            assertThat(wrapping.getTryStatement().getFinallyOnNewLine()).isTrue();
        });
    }

    @Test
    void leftCurlyEol() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="LeftCurly">
                        <property name="option" value="eol"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            assertThat(spaces.getBeforeLeftBrace().getClassLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getMethodLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getIfLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getElseLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getForLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getWhileLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getTryLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getCatchLeftBrace()).isTrue();
            assertThat(spaces.getBeforeLeftBrace().getFinallyLeftBrace()).isTrue();
        });
    }

    @Test
    void whitespaceAround() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="WhitespaceAround">
                        <property name="tokens" value="ASSIGN, PLUS, MINUS, EQUAL, NOT_EQUAL, LAMBDA"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            assertThat(spaces.getAroundOperators().getAssignment()).isTrue();
            assertThat(spaces.getAroundOperators().getAdditive()).isTrue();
            assertThat(spaces.getAroundOperators().getEquality()).isTrue();
            assertThat(spaces.getAroundOperators().getLambdaArrow()).isTrue();
            // Tokens not listed should be false
            assertThat(spaces.getAroundOperators().getLogical()).isFalse();
            assertThat(spaces.getAroundOperators().getMultiplicative()).isFalse();
        });
    }

    @Test
    void whitespaceAfter() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="WhitespaceAfter">
                        <property name="tokens" value="COMMA, SEMI, TYPECAST"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            assertThat(spaces.getOther().getAfterComma()).isTrue();
            assertThat(spaces.getOther().getAfterForSemicolon()).isTrue();
            assertThat(spaces.getOther().getAfterTypeCast()).isTrue();
        });
    }

    @Test
    void whitespaceAfterDefaults() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="WhitespaceAfter"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            // Default tokens include COMMA, SEMI, TYPECAST
            assertThat(spaces.getOther().getAfterComma()).isTrue();
            assertThat(spaces.getOther().getAfterForSemicolon()).isTrue();
            assertThat(spaces.getOther().getAfterTypeCast()).isTrue();
        });
    }

    @Test
    void fullSwissquoteConfig() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="FileTabCharacter">
                    <property name="eachLine" value="true"/>
                </module>
                <module name="LineLength">
                    <property name="max" value="145"/>
                </module>
                <module name="TreeWalker">
                    <module name="AvoidStarImport"/>
                    <module name="ImportOrder">
                        <property name="groups" value="java.,javax.,org.,com."/>
                        <property name="option" value="top"/>
                        <property name="separated" value="true"/>
                        <property name="ordered" value="true"/>
                    </module>
                    <module name="NeedBraces"/>
                    <module name="LeftCurly">
                        <property name="option" value="eol"/>
                    </module>
                    <module name="RightCurly">
                        <property name="option" value="same"/>
                    </module>
                    <module name="WhitespaceAfter"/>
                    <module name="WhitespaceAround"/>
                    <module name="NoWhitespaceAfter"/>
                    <module name="NoWhitespaceBefore"/>
                    <module name="OperatorWrap"/>
                    <module name="UnusedImports"/>
                </module>
            </module>
        """, emptyMap());

        // Verify we got styles for all the key rules
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(ImportLayoutStyle.class));
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(NeedBracesStyle.class));
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(NoWhitespaceAfterStyle.class));
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(NoWhitespaceBeforeStyle.class));
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(OperatorWrapStyle.class));
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(UnusedImportsStyle.class));

        // Verify formatting styles were extracted
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(TabsAndIndentsStyle.class));

        // Verify WrappingAndBracesStyle was extracted (from LineLength and RightCurly)
        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            if (style instanceof WrappingAndBracesStyle wrapping) {
                if (wrapping.getHardWrapAt() != null) {
                    assertThat(wrapping.getHardWrapAt()).isEqualTo(145);
                }
            }
        });

        // Verify SpacesStyle was extracted (from LeftCurly, WhitespaceAfter, WhitespaceAround)
        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(SpacesStyle.class));
    }

    @Test
    void importOrderWithGroupsStaticsOnTop() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ImportOrder">
                        <property name="groups" value="java.,javax.,org.,com."/>
                        <property name="option" value="top"/>
                        <property name="separated" value="true"/>
                        <property name="ordered" value="true"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(ImportLayoutStyle.class);
            ImportLayoutStyle importLayout = (ImportLayoutStyle) style;
            // Should have blocks for: static-all, blank, java.*, blank, javax.*, blank, org.*, blank, com.*, blank, all-others
            assertThat(importLayout.getLayout()).hasSizeGreaterThanOrEqualTo(7);

            // First block should be static catch-all (option=top)
            assertThat(importLayout.getLayout().getFirst())
                    .isInstanceOf(ImportLayoutStyle.Block.AllOthers.class);
            assertThat(((ImportLayoutStyle.Block.AllOthers) importLayout.getLayout().getFirst()).isStatic()).isTrue();

            // Should have java, javax, org, com groups (excluding AllOthers which extends ImportPackage)
            long importPackageCount = importLayout.getLayout().stream()
                    .filter(b -> b.getClass().equals(ImportLayoutStyle.Block.ImportPackage.class))
                    .count();
            assertThat(importPackageCount).isEqualTo(4);

            // Last non-blank block should be non-static catch-all
            assertThat(importLayout.getLayout().stream()
                    .filter(b -> b instanceof ImportLayoutStyle.Block.AllOthers && !((ImportLayoutStyle.Block.AllOthers) b).isStatic())
                    .count()).isEqualTo(1);
        });
    }

    @Test
    void importOrderStaticsOnBottom() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ImportOrder">
                        <property name="groups" value="java.,javax."/>
                        <property name="option" value="bottom"/>
                        <property name="separated" value="true"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(ImportLayoutStyle.class);
            ImportLayoutStyle importLayout = (ImportLayoutStyle) style;

            // First block should be non-static package (java.*)
            assertThat(importLayout.getLayout().getFirst())
                    .isInstanceOf(ImportLayoutStyle.Block.ImportPackage.class);
            assertThat(((ImportLayoutStyle.Block.ImportPackage) importLayout.getLayout().getFirst()).isStatic()).isFalse();

            // Last non-blank block should be static catch-all (option=bottom)
            ImportLayoutStyle.Block lastNonBlank = null;
            for (ImportLayoutStyle.Block block : importLayout.getLayout()) {
                if (!(block instanceof ImportLayoutStyle.Block.BlankLines)) {
                    lastNonBlank = block;
                }
            }
            assertThat(lastNonBlank)
              .extracting(ImportLayoutStyle.Block.AllOthers.class::cast)
              .extracting(ImportLayoutStyle.Block.AllOthers::isStatic)
              .isEqualTo(true);
        });
    }

    @Test
    void importOrderDefaultsNoGroups() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ImportOrder"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(ImportLayoutStyle.class);
            ImportLayoutStyle importLayout = (ImportLayoutStyle) style;
            // Default option is "under" — statics after non-statics
            // Should have: non-static catch-all, blank, static catch-all
            assertThat(importLayout.getLayout().stream()
                    .filter(b -> b instanceof ImportLayoutStyle.Block.AllOthers)
                    .count()).isEqualTo(2);

            // First AllOthers should be non-static (under = statics after)
            ImportLayoutStyle.Block.AllOthers firstAllOthers = importLayout.getLayout().stream()
                    .filter(b -> b instanceof ImportLayoutStyle.Block.AllOthers)
                    .map(b -> (ImportLayoutStyle.Block.AllOthers) b)
                    .findFirst().orElseThrow();
            assertThat(firstAllOthers.isStatic()).isFalse();
        });
    }

    @Test
    void importOrderNotSeparated() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ImportOrder">
                        <property name="groups" value="java.,javax."/>
                        <property name="option" value="top"/>
                        <property name="separated" value="false"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(ImportLayoutStyle.class);
            ImportLayoutStyle importLayout = (ImportLayoutStyle) style;
            // With separated=false, blank lines should only appear between static and non-static sections
            long blankLineCount = importLayout.getLayout().stream()
                    .filter(b -> b instanceof ImportLayoutStyle.Block.BlankLines)
                    .count();
            // At least one blank line between statics and non-statics
            assertThat(blankLineCount).isGreaterThanOrEqualTo(1);
            // But should be fewer than with separated=true
            assertThat(blankLineCount).isLessThanOrEqualTo(2);
        });
    }

    @Test
    void parenPadNospace() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ParenPad"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            // Default is nospace — no padding inside parentheses
            assertThat(spaces.getWithin().getMethodCallParentheses()).isFalse();
            assertThat(spaces.getWithin().getMethodDeclarationParentheses()).isFalse();
            assertThat(spaces.getWithin().getIfParentheses()).isFalse();
            assertThat(spaces.getWithin().getForParentheses()).isFalse();
            assertThat(spaces.getWithin().getWhileParentheses()).isFalse();
            assertThat(spaces.getWithin().getSwitchParentheses()).isFalse();
            assertThat(spaces.getWithin().getCatchParentheses()).isFalse();
            assertThat(spaces.getWithin().getSynchronizedParentheses()).isFalse();
        });
    }

    @Test
    void parenPadSpace() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ParenPad">
                        <property name="option" value="space"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            assertThat(spaces.getWithin().getMethodCallParentheses()).isTrue();
            assertThat(spaces.getWithin().getMethodDeclarationParentheses()).isTrue();
            assertThat(spaces.getWithin().getIfParentheses()).isTrue();
            assertThat(spaces.getWithin().getForParentheses()).isTrue();
            assertThat(spaces.getWithin().getWhileParentheses()).isTrue();
            assertThat(spaces.getWithin().getSwitchParentheses()).isTrue();
            assertThat(spaces.getWithin().getCatchParentheses()).isTrue();
            assertThat(spaces.getWithin().getSynchronizedParentheses()).isTrue();
        });
    }

    @Test
    void parenPadSpaceWithTokens() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="ParenPad">
                        <property name="option" value="space"/>
                        <property name="tokens" value="LITERAL_IF, LITERAL_FOR"/>
                    </module>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style -> {
            assertThat(style).isInstanceOf(SpacesStyle.class);
            SpacesStyle spaces = (SpacesStyle) style;
            // Only listed tokens get padding
            assertThat(spaces.getWithin().getIfParentheses()).isTrue();
            assertThat(spaces.getWithin().getForParentheses()).isTrue();
            // Non-listed tokens should not get padding
            assertThat(spaces.getWithin().getMethodCallParentheses()).isFalse();
            assertThat(spaces.getWithin().getWhileParentheses()).isFalse();
        });
    }

    @Test
    void hideUtilityClassConstructor() throws Exception {
        var checkstyle = loadCheckstyleConfig("""
            <!DOCTYPE module PUBLIC
                "-//Checkstyle//DTD Checkstyle Configuration 1.2//EN"
                "https://checkstyle.org/dtds/configuration_1_2.dtd">
            <module name="Checker">
                <module name="TreeWalker">
                    <module name="HideUtilityClassConstructor"/>
                </module>
            </module>
        """, emptyMap());

        assertThat(checkstyle.getStyles()).anySatisfy(style ->
                assertThat(style).isInstanceOf(HideUtilityClassConstructorStyle.class));
    }
}
