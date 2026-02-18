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

import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.openrewrite.java.style.Checkstyle.defaultBlockPolicy;
import static org.openrewrite.java.style.Checkstyle.defaultOperatorWrapStyleOption;

public class CheckstyleConfigLoader {
    private CheckstyleConfigLoader() {
    }

    public static Checkstyle loadCheckstyleConfig(Path checkstyleConf, Map<String, Object> properties) throws CheckstyleException {
        try (InputStream is = Files.newInputStream(checkstyleConf)) {
            return loadCheckstyleConfig(is, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Checkstyle loadCheckstyleConfig(@Language("XML") String checkstyleConf, Map<String, Object> properties) throws CheckstyleException {
        try (InputStream is = new ByteArrayInputStream(checkstyleConf.getBytes(StandardCharsets.UTF_8))) {
            return loadCheckstyleConfig(is, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Checkstyle loadCheckstyleConfig(InputStream checkstyleConf, Map<String, Object> properties) throws CheckstyleException {
        Map<String, List<Module>> conf = loadConfiguration(checkstyleConf, properties);

        return new Checkstyle(
                Stream.of(
                                defaultComesLast(conf),
                                emptyBlock(conf),
                                emptyForInitializerPadStyle(conf),
                                emptyForIteratorPadStyle(conf),
                                equalsAvoidsNull(conf),
                                explicitInitialization(conf),
                                fallThrough(conf),
                                hiddenFieldStyle(conf),
                                hideUtilityClassConstructorStyle(conf),
                                methodParamPadStyle(conf),
                                needBracesStyle(conf),
                                noWhitespaceAfterStyle(conf),
                                noWhitespaceBeforeStyle(conf),
                                operatorWrapStyle(conf),
                                typecastParenPadStyle(conf),
                                unnecessaryParentheses(conf),
                                customImportOrderStyle(conf),
                                unusedImportsStyles(conf),
                                avoidStarImportStyle(conf),
                                lineLengthStyle(conf),
                                tabCharacterStyle(conf),
                                indentationStyle(conf),
                                rightCurlyStyle(conf),
                                leftCurlyStyle(conf),
                                whitespaceAroundStyle(conf),
                                whitespaceAfterStyle(conf),
                                importOrderStyle(conf),
                                parenPadStyle(conf))
                        .filter(Objects::nonNull)
                        .flatMap(Set::stream)
                        .collect(toSet()));
    }

    private static @Nullable Set<DefaultComesLastStyle> defaultComesLast(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("DefaultComesLast");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new DefaultComesLastStyle(parseBoolean(module.properties.get("skipIfLastAndSharedWithCase"))))
                .collect(toSet());
    }

    private static @Nullable Set<EmptyBlockStyle> emptyBlock(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("EmptyBlock");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawOption = module.properties.get("option");
                    EmptyBlockStyle.BlockPolicy blockPolicy = defaultBlockPolicy;
                    if (rawOption != null) {
                        blockPolicy = Enum.valueOf(EmptyBlockStyle.BlockPolicy.class, rawOption.toUpperCase());
                    }
                    String rawTokens = module.properties.get("tokens");
                    boolean instanceInit = true;
                    boolean literalCatch = true;
                    boolean literalDo = true;
                    boolean literalElse = true;
                    boolean literalFinally = true;
                    boolean literalFor = true;
                    boolean literalIf = true;
                    boolean literalSwitch = true;
                    boolean literalSynchronized = true;
                    boolean literalTry = true;
                    boolean literalWhile = true;
                    boolean staticInit = true;
                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        instanceInit = tokens.contains("INSTANCE_INIT");
                        literalCatch = tokens.contains("LITERAL_CATCH");
                        literalDo = tokens.contains("LITERAL_DO");
                        literalElse = tokens.contains("LITERAL_ELSE");
                        literalFinally = tokens.contains("LITERAL_FINALLY");
                        literalFor = tokens.contains("LITERAL_FOR");
                        literalIf = tokens.contains("LITERAL_IF");
                        literalSwitch = tokens.contains("LITERAL_SWITCH");
                        literalSynchronized = tokens.contains("LITERAL_SYNCHRONIZED");
                        literalTry = tokens.contains("LITERAL_TRY");
                        literalWhile = tokens.contains("LITERAL_WHILE");
                        staticInit = tokens.contains("STATIC_INIT");
                    }

                    return new EmptyBlockStyle(blockPolicy, instanceInit, literalCatch, literalDo,
                            literalElse, literalFinally, literalFor, literalIf, literalSwitch, literalSynchronized, literalTry,
                            literalWhile, staticInit);
                })
                .collect(toSet());
    }

    private static @Nullable Set<EmptyForInitializerPadStyle> emptyForInitializerPadStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("EmptyForInitializerPad");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String option = module.properties.get("option");
                    // nospace is default, so no option means pad is false
                    boolean pad = option != null && "space".equals(option.trim());
                    return new EmptyForInitializerPadStyle(pad);
                })
                .collect(toSet());
    }

    private static @Nullable Set<EmptyForIteratorPadStyle> emptyForIteratorPadStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("EmptyForIteratorPad");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String option = module.properties.get("option");
                    // nospace is default, so no option means pad is false
                    boolean pad = option != null && "space".equals(option.trim());
                    return new EmptyForIteratorPadStyle(pad);
                })
                .collect(toSet());
    }

    private static @Nullable Set<EqualsAvoidsNullStyle> equalsAvoidsNull(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("EqualsAvoidNull");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new EqualsAvoidsNullStyle(module.prop("ignoreEqualsIgnoreCase", false)))
                .collect(toSet());
    }

    private static @Nullable Set<FallThroughStyle> fallThrough(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("FallThrough");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new FallThroughStyle(module.prop("checkLastCaseGroup", false)))
                .collect(toSet());
    }

    private static @Nullable Set<HiddenFieldStyle> hiddenFieldStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("HiddenField");
        if (moduleList == null) {
            return null;
        }
        // https://checkstyle.sourceforge.io/checks/coding/hiddenfield.html
        return moduleList.stream()
                .map(module -> new HiddenFieldStyle(
                        module.prop("ignoreConstructorParameter", false),
                        module.prop("ignoreSetter", false),
                        module.prop("setterCanReturnItsClass", false),
                        module.prop("ignoreAbstractMethods", false)
                ))
                .collect(toSet());
    }

    private static @Nullable Set<MethodParamPadStyle> methodParamPadStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("MethodParamPad");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String option = module.properties.get("option");
                    // nospace is default, so no option means pad is false
                    boolean pad = option != null && "space".equals(option.trim());
                    return new MethodParamPadStyle(
                            pad,
                            module.prop("allowLineBreaks", false)
                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<NeedBracesStyle> needBracesStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("NeedBraces");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new NeedBracesStyle(
                        module.prop("allowSingleLineStatement", false),
                        module.prop("allowEmptyLoopBody", false)
                ))
                .collect(toSet());
    }

    private static @Nullable Set<NoWhitespaceAfterStyle> noWhitespaceAfterStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("NoWhitespaceAfter");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawTokens = module.properties.get("tokens");
                    boolean typecast = true;
                    boolean methodRef = false;
                    boolean arrayDeclarator = true;
                    boolean annotation = true;
                    boolean arrayInitializer = true;
                    boolean indexOperation = true;
                    boolean dot = false;
                    boolean inc = true;
                    boolean dec = true;
                    boolean bnoc = true;
                    boolean lnot = true;
                    boolean unaryPlus = true;
                    boolean unaryMinus = true;
                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        typecast = tokens.contains("TYPECAST");
                        methodRef = tokens.contains("METHOD_REF");
                        arrayDeclarator = tokens.contains("ARRAY_DECLARATOR");
                        arrayInitializer = tokens.contains("ARRAY_INIT");
                        annotation = tokens.contains("AT");
                        indexOperation = tokens.contains("INDEX_OP");
                        dot = tokens.contains("DOT");
                        inc = tokens.contains("INC");
                        dec = tokens.contains("DEC");
                        bnoc = tokens.contains("BNOT");
                        lnot = tokens.contains("LNOT");
                        unaryPlus = tokens.contains("UNARY_PLUS");
                        unaryMinus = tokens.contains("UNARY_MINUS");
                    }
                    return new NoWhitespaceAfterStyle(
                            module.prop("allowLineBreaks", true),
                            typecast,
                            methodRef,
                            arrayDeclarator,
                            annotation,
                            arrayInitializer,
                            indexOperation,
                            dot,
                            inc,
                            dec,
                            bnoc,
                            lnot,
                            unaryPlus,
                            unaryMinus
                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<NoWhitespaceBeforeStyle> noWhitespaceBeforeStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("NoWhitespaceBefore");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawTokens = module.properties.get("tokens");
                    boolean dot = false;
                    boolean comma = true;
                    boolean semi = true;
                    boolean genericStart = false;
                    boolean genericEnd = false;
                    boolean methodRef = false;
                    boolean postInc = true;
                    boolean postDec = true;
                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        dot = tokens.contains("DOT");
                        comma = tokens.contains("COMMA");
                        semi = tokens.contains("SEMI");
                        genericStart = tokens.contains("GENERIC_START");
                        genericEnd = tokens.contains("GENERIC_END");
                        methodRef = tokens.contains("METHOD_REF");
                        postInc = tokens.contains("POST_INC");
                        postDec = tokens.contains("POST_DEC");
                    }
                    return new NoWhitespaceBeforeStyle(
                            module.prop("allowLineBreaks", false),
                            dot,
                            comma,
                            semi,
                            genericStart,
                            genericEnd,
                            methodRef,
                            postInc,
                            postDec

                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<OperatorWrapStyle> operatorWrapStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("OperatorWrap");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawOption = module.properties.get("option");
                    OperatorWrapStyle.WrapOption wrapOption = defaultOperatorWrapStyleOption;
                    if (rawOption != null) {
                        wrapOption = Enum.valueOf(OperatorWrapStyle.WrapOption.class, rawOption.toUpperCase());
                    }
                    String rawTokens = module.properties.get("tokens");
                    boolean question = true;
                    boolean colon = true;
                    boolean equal = true;
                    boolean notEqual = true;
                    boolean div = true;
                    boolean plus = true;
                    boolean minus = true;
                    boolean star = true;
                    boolean mod = true;
                    boolean sr = true;
                    boolean bsr = true;
                    boolean ge = true;
                    boolean gt = true;
                    boolean sl = true;
                    boolean le = true;
                    boolean lt = true;
                    boolean bxor = true;
                    boolean bor = true;
                    boolean lor = true;
                    boolean band = true;
                    boolean land = true;
                    boolean typeExtensionAnd = true;
                    boolean literalInstanceof = true;
                    boolean methodRef = false;
                    boolean assign = false;
                    boolean plusAssign = false;
                    boolean minusAssign = false;
                    boolean starAssign = false;
                    boolean divAssign = false;
                    boolean modAssign = false;
                    boolean srAssign = false;
                    boolean bsrAssign = false;
                    boolean slAssign = false;
                    boolean bandAssign = false;
                    boolean bxorAssign = false;
                    boolean borAssign = false;
                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        question = tokens.contains("QUESTION");
                        colon = tokens.contains("COLON");
                        equal = tokens.contains("EQUAL");
                        notEqual = tokens.contains("NOT_EQUAL");
                        div = tokens.contains("DIV");
                        plus = tokens.contains("PLUS");
                        minus = tokens.contains("MINUS");
                        star = tokens.contains("STAR");
                        mod = tokens.contains("MOD");
                        sr = tokens.contains("SR");
                        bsr = tokens.contains("BSR");
                        ge = tokens.contains("GE");
                        gt = tokens.contains("GT");
                        sl = tokens.contains("SL");
                        le = tokens.contains("LE");
                        lt = tokens.contains("LT");
                        bxor = tokens.contains("BXOR");
                        bor = tokens.contains("BOR");
                        lor = tokens.contains("LOR");
                        band = tokens.contains("BAND");
                        land = tokens.contains("LAND");
                        typeExtensionAnd = tokens.contains("TYPE_EXTENSION_AND");
                        literalInstanceof = tokens.contains("LITERAL_INSTANCEOF");
                        methodRef = tokens.contains("METHOD_REF");
                        assign = tokens.contains("ASSIGN");
                        plusAssign = tokens.contains("PLUS_ASSIGN");
                        minusAssign = tokens.contains("MINUS_ASSIGN");
                        starAssign = tokens.contains("STAR_ASSIGN");
                        divAssign = tokens.contains("DIV_ASSIGN");
                        modAssign = tokens.contains("MOD_ASSIGN");
                        srAssign = tokens.contains("SR_ASSIGN");
                        bsrAssign = tokens.contains("BSR_ASSIGN");
                        slAssign = tokens.contains("SL_ASSIGN");
                        bandAssign = tokens.contains("BAND_ASSIGN");
                        bxorAssign = tokens.contains("BXOR_ASSIGN");
                        borAssign = tokens.contains("BOR_ASSIGN");
                    }
                    return new OperatorWrapStyle(wrapOption, question, colon, equal, notEqual, div, plus,
                            minus, star, mod, sr, bsr, ge, gt, sl, le, lt, bxor, bor, lor, band, land,
                            typeExtensionAnd, literalInstanceof, methodRef, assign,
                            plusAssign, minusAssign, starAssign, divAssign, modAssign, srAssign,
                            bsrAssign, slAssign, bandAssign, bxorAssign, borAssign);
                })
                .collect(toSet());
    }

    private static @Nullable Set<TypecastParenPadStyle> typecastParenPadStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("TypecastParenPad");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String option = module.properties.get("option");
                    // nospace is default, so no option means pad is false
                    boolean pad = option != null && "space".equals(option.trim());
                    return new TypecastParenPadStyle(pad);
                })
                .collect(toSet());
    }

    @SuppressWarnings("DuplicatedCode")
    private static @Nullable Set<UnnecessaryParenthesesStyle> unnecessaryParentheses(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("UnnecessaryParentheses");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawTokens = module.properties.get("tokens");
                    boolean expr = true;
                    boolean ident = true;
                    boolean numDouble = true;
                    boolean numFloat = true;
                    boolean numInt = true;
                    boolean numLong = true;
                    boolean stringLiteral = true;
                    boolean literalNull = true;
                    boolean literalFalse = true;
                    boolean literalTrue = true;
                    boolean assign = true;
                    boolean bitAndAssign = true;
                    boolean bitOrAssign = true;
                    boolean bitShiftRightAssign = true;
                    boolean bitXorAssign = true;
                    boolean divAssign = true;
                    boolean minusAssign = true;
                    boolean modAssign = true;
                    boolean plusAssign = true;
                    boolean shiftLeftAssign = true;
                    boolean shiftRightAssign = true;
                    boolean starAssign = true;
                    boolean lambda = true;
                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        expr = tokens.contains("EXPR");
                        ident = tokens.contains("IDENT");
                        numDouble = tokens.contains("NUM_DOUBLE");
                        numFloat = tokens.contains("NUM_FLOAT");
                        numInt = tokens.contains("NUM_INT");
                        numLong = tokens.contains("NUM_LONG");
                        stringLiteral = tokens.contains("STRING_LITERAL");
                        literalNull = tokens.contains("LITERAL_NULL");
                        literalFalse = tokens.contains("LITERAL_FALSE");
                        literalTrue = tokens.contains("LITERAL_TRUE");
                        assign = tokens.contains("ASSIGN");
                        bitAndAssign = tokens.contains("BIT_AND_ASSIGN");
                        bitOrAssign = tokens.contains("BIT_OR_ASSIGN");
                        bitShiftRightAssign = tokens.contains("BIT_SHIFT_RIGHT_ASSIGN");
                        bitXorAssign = tokens.contains("BIT_XOR_ASSIGN");
                        divAssign = tokens.contains("DIV_ASSIGN");
                        minusAssign = tokens.contains("MINUS_ASSIGN");
                        modAssign = tokens.contains("MOD_ASSIGN");
                        plusAssign = tokens.contains("PLUS_ASSIGN");
                        shiftLeftAssign = tokens.contains("SHIFT_LEFT_ASSIGN");
                        shiftRightAssign = tokens.contains("SHIFT_RIGHT_ASSIGN");
                        starAssign = tokens.contains("STAR_ASSIGN");
                        lambda = tokens.contains("LAMBDA");
                    }
                    return new UnnecessaryParenthesesStyle(expr, ident, numDouble, numFloat, numInt, numLong, stringLiteral,
                            literalNull, literalFalse, literalTrue, assign, bitAndAssign, bitOrAssign, bitShiftRightAssign,
                            bitXorAssign, divAssign, minusAssign, modAssign, plusAssign, shiftLeftAssign, shiftRightAssign,
                            starAssign, lambda
                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<HideUtilityClassConstructorStyle> hideUtilityClassConstructorStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("HideUtilityClassConstructor");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> Checkstyle.hideUtilityClassConstructorStyle())
                .collect(toSet());
    }

    private static @Nullable Set<UnusedImportsStyle> unusedImportsStyles(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("UnusedImports");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new UnusedImportsStyle(parseBoolean(module.properties.get("processJavadoc"))))
                .collect(toSet());
    }

    protected static class Module {
        @Getter
        private final String name;
        private final Map<String, String> properties;

        public Module(String name, Map<String, String> properties) {
            // Checkstyle allows both "EqualsAvoidNull" and "EqualsAvoidsNullCheck"
            // For consistency, remove the "Check" suffix if it exists
            if (name.endsWith("Check")) {
                this.name = name.substring(0, name.lastIndexOf("Check"));
            } else {
                this.name = name;
            }
            this.properties = properties;
        }

        public boolean prop(String key, boolean defaultValue) {
            return properties.containsKey(key) ? parseBoolean(properties.get(key)) :
                    defaultValue;
        }

        @Override
        public String toString() {
            return "Module{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    private static @Nullable Set<ExplicitInitializationStyle> explicitInitialization(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("ExplicitInitialization");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new ExplicitInitializationStyle(module.prop("onlyObjectReferences", false)))
                .collect(toSet());
    }

    private static @Nullable Set<CustomImportOrderStyle> customImportOrderStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("CustomImportOrder");
        if (moduleList == null) {
            return null;
        }

        return moduleList.stream()
                .map(module -> {
                    boolean separateLineBetweenGroups = parseBoolean(module.properties.get("separateLineBetweenGroups"));
                    boolean sortImportsInGroupAlphabetically = parseBoolean(module.properties.get("sortImportsInGroupAlphabetically"));
                    String specialImportsRegExp = module.properties.get("specialImportsRegExp");
                    String standardPackageRegExp = module.properties.get("standardPackageRegExp");
                    String thirdPartyPackageRegExp = module.properties.get("thirdPartyPackageRegExp");
                    List<CustomImportOrderStyle.GroupWithDepth> customImportOrderRules = CustomImportOrderStyle
                            .parseImportOrder(module.properties.get("customImportOrderRules"));
                    return new CustomImportOrderStyle(customImportOrderRules, separateLineBetweenGroups, sortImportsInGroupAlphabetically,
                            specialImportsRegExp, standardPackageRegExp, thirdPartyPackageRegExp);
                })
                .collect(toSet());
    }

    // https://checkstyle.sourceforge.io/checks/imports/avoidstarimport.html
    private static @Nullable Set<ImportLayoutStyle> avoidStarImportStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("AvoidStarImport");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> new ImportLayoutStyle(
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        emptyList(),
                        emptyList()
                ))
                .collect(toSet());
    }

    private static @Nullable Set<WrappingAndBracesStyle> lineLengthStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("LineLength");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String max = module.properties.get("max");
                    int hardWrapAt = max != null ? Integer.parseInt(max) : 80;
                    return new WrappingAndBracesStyle(
                            hardWrapAt,       // hardWrapAt
                            null, null, null, // keepWhenFormatting, extendsImplementsPermitsList, extendsImplementsPermitsKeyword
                            null, null, null, // throwsList, throwsKeyword, methodDeclarationParameters
                            null, null, null, // methodCallArguments, methodParentheses, chainedMethodCalls
                            null, null, null, // ifStatement, forStatement, whileStatement
                            null, null, null, // doWhileStatement, switchStatement, tryWithResources
                            null, null, null, // tryStatement, binaryExpressions, assignmentStatement
                            null, null, null, // groupDeclarations, ternaryOperation, arrayInitializer
                            null, null, null, // modifierList, assertStatement, enumConstants
                            null, null, null, // classAnnotations, methodAnnotations, fieldAnnotations
                            null, null, null, // parameterAnnotations, localVariableAnnotations, enumFieldAnnotations
                            null, null, null, // annotationParameters, textBlocks, recordComponents
                            null              // deconstructionPatterns
                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<TabsAndIndentsStyle> tabCharacterStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("FileTabCharacter");
        if (moduleList == null) {
            return null;
        }
        // OK to return a style with nullable non-null fields, if Autodetections doesn't provide Style.applyDefaults() will
        //noinspection DataFlowIssue
        return moduleList.stream()
                .map(module -> new TabsAndIndentsStyle(false, null, null, null, null))
                .collect(toSet());
    }

    private static @Nullable Set<TabsAndIndentsStyle> indentationStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("Indentation");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String basicOffset = module.properties.get("basicOffset");
                    String tabWidth = module.properties.get("tabWidth");
                    String lineWrappingIndentation = module.properties.get("lineWrappingIndentation");
                    //noinspection DataFlowIssue
                    return new TabsAndIndentsStyle(
                            null,
                            tabWidth != null ? Integer.parseInt(tabWidth) : null,
                            basicOffset != null ? Integer.parseInt(basicOffset) : null,
                            lineWrappingIndentation != null ? Integer.parseInt(lineWrappingIndentation) : null,
                            null
                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<WrappingAndBracesStyle> rightCurlyStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("RightCurly");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawOption = module.properties.get("option");
                    // "same" is the default
                    boolean alone = rawOption != null && "alone".equalsIgnoreCase(rawOption.trim());

                    String rawTokens = module.properties.get("tokens");
                    // Default tokens for RightCurly: LITERAL_TRY, LITERAL_CATCH, LITERAL_FINALLY, LITERAL_IF, LITERAL_ELSE
                    boolean affectsIf = true;
                    boolean affectsTry = true;
                    boolean affectsCatch = true;
                    boolean affectsFinally = true;
                    boolean affectsDoWhile = false;
                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        affectsIf = tokens.contains("LITERAL_IF") || tokens.contains("LITERAL_ELSE");
                        affectsTry = tokens.contains("LITERAL_TRY");
                        affectsCatch = tokens.contains("LITERAL_CATCH");
                        affectsFinally = tokens.contains("LITERAL_FINALLY");
                        affectsDoWhile = tokens.contains("LITERAL_DO");
                    }

                    return new WrappingAndBracesStyle(
                            null,
                            null, null, null,
                            null, null, null,
                            null, null, null,
                            affectsIf ? new WrappingAndBracesStyle.IfStatement(null, alone, null) : null,
                            null, null,
                            affectsDoWhile ? new WrappingAndBracesStyle.DoWhileStatement(null, alone) : null,
                            null, null,
                            (affectsTry || affectsCatch || affectsFinally) ?
                                    new WrappingAndBracesStyle.TryStatement(
                                            affectsCatch ? alone : null,
                                            affectsFinally ? alone : null,
                                            null, null) : null,
                            null, null, null,
                            null, null, null,
                            null, null, null,
                            null, null, null,
                            null, null, null,
                            null, null, null
                    );
                })
                .collect(toSet());
    }

    private static @Nullable Set<SpacesStyle> leftCurlyStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("LeftCurly");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawOption = module.properties.get("option");
                    // "eol" is the default - left curly brace on end of line with space before
                    // "nl" and "nlow" yet to be implemented
                    boolean eol = rawOption == null || "eol".equalsIgnoreCase(rawOption.trim());

                    String rawTokens = module.properties.get("tokens");
                    boolean classLeftBrace = eol;
                    boolean methodLeftBrace = eol;
                    boolean ifLeftBrace = eol;
                    boolean elseLeftBrace = eol;
                    boolean forLeftBrace = eol;
                    boolean whileLeftBrace = eol;
                    boolean doLeftBrace = eol;
                    boolean switchLeftBrace = eol;
                    boolean tryLeftBrace = eol;
                    boolean catchLeftBrace = eol;
                    boolean finallyLeftBrace = eol;
                    boolean synchronizedLeftBrace = eol;

                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        classLeftBrace = tokens.contains("CLASS_DEF") || tokens.contains("INTERFACE_DEF") ||
                                tokens.contains("ENUM_DEF") || tokens.contains("RECORD_DEF") ||
                                tokens.contains("ANNOTATION_DEF");
                        methodLeftBrace = tokens.contains("METHOD_DEF") || tokens.contains("CTOR_DEF") ||
                                tokens.contains("COMPACT_CTOR_DEF");
                        ifLeftBrace = tokens.contains("LITERAL_IF");
                        elseLeftBrace = tokens.contains("LITERAL_ELSE");
                        forLeftBrace = tokens.contains("LITERAL_FOR");
                        whileLeftBrace = tokens.contains("LITERAL_WHILE");
                        doLeftBrace = tokens.contains("LITERAL_DO");
                        switchLeftBrace = tokens.contains("LITERAL_SWITCH");
                        tryLeftBrace = tokens.contains("LITERAL_TRY");
                        catchLeftBrace = tokens.contains("LITERAL_CATCH");
                        finallyLeftBrace = tokens.contains("LITERAL_FINALLY");
                        synchronizedLeftBrace = tokens.contains("LITERAL_SYNCHRONIZED");
                    }

                    //noinspection DataFlowIssue
                    return new SpacesStyle(null, null,
                            new SpacesStyle.BeforeLeftBrace(
                                    classLeftBrace, methodLeftBrace, ifLeftBrace, elseLeftBrace,
                                    forLeftBrace, whileLeftBrace, doLeftBrace, switchLeftBrace,
                                    tryLeftBrace, catchLeftBrace, finallyLeftBrace, synchronizedLeftBrace,
                                    null,
                                    null
                            ),
                            null, null, null, null, null, null);
                })
                .collect(toSet());
    }

    private static @Nullable Set<SpacesStyle> whitespaceAroundStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("WhitespaceAround");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawTokens = module.properties.get("tokens");

                    // WhitespaceAround default tokens include most operators and braces
                    boolean assignment = true;
                    boolean logical = true;
                    boolean equality = true;
                    boolean relational = true;
                    boolean bitwise = true;
                    boolean additive = true;
                    boolean multiplicative = true;
                    boolean shift = true;
                    boolean lambdaArrow = true;

                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        assignment = tokens.contains("ASSIGN") || tokens.contains("PLUS_ASSIGN") ||
                                tokens.contains("MINUS_ASSIGN") || tokens.contains("STAR_ASSIGN") ||
                                tokens.contains("DIV_ASSIGN") || tokens.contains("MOD_ASSIGN");
                        logical = tokens.contains("LOR") || tokens.contains("LAND");
                        equality = tokens.contains("EQUAL") || tokens.contains("NOT_EQUAL");
                        relational = tokens.contains("LT") || tokens.contains("GT") ||
                                tokens.contains("LE") || tokens.contains("GE");
                        bitwise = tokens.contains("BAND") || tokens.contains("BOR") || tokens.contains("BXOR");
                        additive = tokens.contains("PLUS") || tokens.contains("MINUS");
                        multiplicative = tokens.contains("STAR") || tokens.contains("DIV") || tokens.contains("MOD");
                        shift = tokens.contains("SL") || tokens.contains("SR") || tokens.contains("BSR");
                        lambdaArrow = tokens.contains("LAMBDA");
                    }

                    //noinspection DataFlowIssue
                    return new SpacesStyle(null, new SpacesStyle.AroundOperators(
                            assignment, logical, equality, relational, bitwise,
                            additive, multiplicative, shift,
                            null,
                            lambdaArrow,
                            null
                    ), null, null, null, null, null, null, null);
                })
                .collect(toSet());
    }

    private static @Nullable Set<SpacesStyle> whitespaceAfterStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("WhitespaceAfter");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String rawTokens = module.properties.get("tokens");

                    // WhitespaceAfter default tokens: COMMA, SEMI, TYPECAST, LITERAL_IF, LITERAL_ELSE,
                    // LITERAL_WHILE, LITERAL_DO, LITERAL_FOR, DO_WHILE
                    boolean afterComma = true;
                    boolean afterForSemicolon = true;
                    boolean afterTypeCast = true;

                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        afterComma = tokens.contains("COMMA");
                        afterForSemicolon = tokens.contains("SEMI");
                        afterTypeCast = tokens.contains("TYPECAST");
                    }

                    //noinspection DataFlowIssue
                    return new SpacesStyle(null, null, null, null, null, null, null,
                            new SpacesStyle.Other(
                                    null,
                                    afterComma,
                                    null,
                                    afterForSemicolon,
                                    afterTypeCast,
                                    null,
                                    null,
                                    null),
                            null);
                })
                .collect(toSet());
    }

    private static @Nullable Set<SpacesStyle> parenPadStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("ParenPad");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String option = module.properties.get("option");
                    // "nospace" is the default
                    boolean pad = option != null && "space".equalsIgnoreCase(option.trim());

                    String rawTokens = module.properties.get("tokens");

                    boolean methodCall = pad;
                    boolean methodDeclaration = pad;
                    boolean ifParens = pad;
                    boolean forParens = pad;
                    boolean whileParens = pad;
                    boolean switchParens = pad;
                    boolean catchParens = pad;
                    boolean synchronizedParens = pad;
                    boolean typeCast = pad;
                    boolean annotation = pad;

                    if (rawTokens != null) {
                        Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                                .collect(toSet());
                        methodCall = pad && (tokens.contains("METHOD_CALL") || tokens.contains("CTOR_CALL") || tokens.contains("SUPER_CTOR_CALL"));
                        methodDeclaration = pad && (tokens.contains("METHOD_DEF") || tokens.contains("CTOR_DEF") || tokens.contains("RECORD_DEF"));
                        ifParens = pad && tokens.contains("LITERAL_IF");
                        forParens = pad && tokens.contains("LITERAL_FOR");
                        whileParens = pad && (tokens.contains("LITERAL_WHILE") || tokens.contains("LITERAL_DO"));
                        switchParens = pad && tokens.contains("LITERAL_SWITCH");
                        catchParens = pad && tokens.contains("LITERAL_CATCH");
                        synchronizedParens = pad && tokens.contains("LITERAL_SYNCHRONIZED");
                        typeCast = pad && tokens.contains("EXPR");
                        annotation = pad && (tokens.contains("ANNOTATION") || tokens.contains("ANNOTATION_FIELD_DEF"));
                    }

                    //noinspection DataFlowIssue
                    return new SpacesStyle(null, null, null, null,
                            new SpacesStyle.Within(
                                    null, null, null, null, null,
                                    methodDeclaration, null,
                                    methodCall, null,
                                    ifParens, forParens, whileParens, switchParens,
                                    null,
                                    catchParens, synchronizedParens,
                                    typeCast, annotation,
                                    null, null
                            ),
                            null, null, null, null);
                })
                .collect(toSet());
    }

    private static @Nullable Set<ImportLayoutStyle> importOrderStyle(Map<String, List<Module>> conf) {
        List<Module> moduleList = conf.get("ImportOrder");
        if (moduleList == null) {
            return null;
        }
        return moduleList.stream()
                .map(module -> {
                    String groups = module.properties.get("groups");
                    String option = module.properties.get("option");
                    boolean separated = module.prop("separated", false);
                    String staticGroups = module.properties.get("staticGroups");

                    // option: "top" or "above" = statics first, "bottom" or "under" = statics last, "inflow" = mixed
                    // Default is "under" per checkstyle docs
                    String trimmedOption = option != null ? option.trim() : "under";
                    boolean staticsOnTop = "top".equalsIgnoreCase(trimmedOption) || "above".equalsIgnoreCase(trimmedOption);
                    boolean staticsOnBottom = "bottom".equalsIgnoreCase(trimmedOption) || "under".equalsIgnoreCase(trimmedOption);

                    ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();

                    if (staticsOnTop) {
                        // Add static import groups or catch-all
                        if (staticGroups != null && !staticGroups.isEmpty()) {
                            addGroupBlocks(builder, staticGroups, separated, true);
                            builder.blankLine();
                        } else {
                            builder.importStaticAllOthers();
                            builder.blankLine();
                        }
                    }

                    // Add non-static import groups
                    if (groups != null && !groups.isEmpty()) {
                        addGroupBlocks(builder, groups, separated, false);
                        // Add catch-all for any imports not matching specified groups
                        if (separated) {
                            builder.blankLine();
                        }
                        builder.importAllOthers();
                    } else {
                        builder.importAllOthers();
                    }

                    if (staticsOnBottom) {
                        builder.blankLine();
                        if (staticGroups != null && !staticGroups.isEmpty()) {
                            addGroupBlocks(builder, staticGroups, separated, true);
                        } else {
                            builder.importStaticAllOthers();
                        }
                    }

                    // If "inflow", statics are mixed with non-statics — use catch-all
                    if (!staticsOnTop && !staticsOnBottom) {
                        builder.importStaticAllOthers();
                    }

                    return builder.build();
                })
                .collect(toSet());
    }

    private static void addGroupBlocks(ImportLayoutStyle.Builder builder, String groups, boolean separated, boolean isStatic) {
        String[] groupArray = groups.split("\\s*,\\s*");
        for (int i = 0; i < groupArray.length; i++) {
            String group = groupArray[i].trim();
            if (group.isEmpty()) {
                continue;
            }
            if (i > 0 && separated) {
                builder.blankLine();
            }
            // Convert checkstyle group pattern to ImportLayoutStyle wildcard
            // Checkstyle uses "java." to mean "java and all subpackages"
            // ImportLayoutStyle uses "java.*" with withSubpackages=true
            String packageWildcard;
            if (group.endsWith(".")) {
                packageWildcard = group + "*";
            } else if (!group.contains("*")) {
                packageWildcard = group + ".*";
            } else {
                packageWildcard = group;
            }
            if (isStatic) {
                builder.staticImportPackage(packageWildcard);
            } else {
                builder.importPackage(packageWildcard);
            }
        }
    }

    private static Map<String, List<Module>> loadConfiguration(InputStream inputStream, Map<String, Object> properties) throws CheckstyleException {
        Configuration checkstyleConfig = ConfigurationLoader.loadConfiguration(new InputSource(inputStream),
                name -> {
                    Object prop = properties.get(name);
                    return prop == null ?
                            "config_loc".equals(name) ? "config/checkstyle" : null :
                            prop.toString();
                },
                ConfigurationLoader.IgnoredModulesOptions.OMIT);

        List<Module> modules = new ArrayList<>();
        for (Configuration firstLevelChild : checkstyleConfig.getChildren()) {
            if ("TreeWalker".equals(firstLevelChild.getName())) {
                modules.addAll(Arrays.stream(firstLevelChild.getChildren())
                        .map(child -> {
                            try {
                                Map<String, String> props = new HashMap<>();
                                for (String propertyName : child.getPropertyNames()) {
                                    props.put(propertyName, child.getProperty(propertyName));
                                }
                                return new Module(child.getName(), props);
                            } catch (CheckstyleException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(toList())
                );
            } else if (!"SuppressionFilter".equals(firstLevelChild.getName())) {
                Map<String, String> props = new HashMap<>();
                for (String propertyName : firstLevelChild.getPropertyNames()) {
                    props.put(propertyName, firstLevelChild.getProperty(propertyName));
                }
                modules.add(new Module(firstLevelChild.getName(), props));
            }
        }
        return modules.stream()
                .collect(toMap(Module::getName, Collections::singletonList, CheckstyleConfigLoader::mergeModules));
    }

    private static List<Module> mergeModules(List<Module> list, List<Module> anotherList) {
        List<Module> result = new ArrayList<>(list);
        result.addAll(anotherList);

        return result;
    }
}
