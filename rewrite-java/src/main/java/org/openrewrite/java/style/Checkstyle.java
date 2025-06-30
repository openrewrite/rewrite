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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class Checkstyle extends NamedStyles {
    private static final Checkstyle INSTANCE = new Checkstyle();
    private static final String NAME = "org.openrewrite.java.Checkstyle";
    private static final String DISPLAY_NAME = "Checkstyle";
    private static final String DESCRIPTION = "Checkstyle defaults for styles";

    private Checkstyle() {
        super(randomId(),
                NAME,
                DISPLAY_NAME,
                DESCRIPTION,
                emptySet(),
                Arrays.asList(
                        defaultComesLast(),
                        emptyBlock(),
                        emptyForInitializerPadStyle(),
                        emptyForIteratorPadStyle(),
                        equalsAvoidsNull(),
                        explicitInitialization(),
                        fallThrough(),
                        hiddenFieldStyle(),
                        hideUtilityClassConstructorStyle(),
                        methodParamPadStyle(),
                        needBracesStyle(),
                        noWhitespaceAfterStyle(),
                        noWhitespaceBeforeStyle(),
                        operatorWrapStyle(),
                        typecastParenPadStyle(),
                        unnecessaryParentheses(),
                        customImportOrderStyle(),
                        unusedImportsStyle()
                ));
    }

    Checkstyle(Collection<Style> styles) {
        super(randomId(), NAME, DISPLAY_NAME, DESCRIPTION, emptySet(), styles);
    }

    @JsonCreator
    public static Checkstyle defaults() {
        return INSTANCE;
    }

    public static DefaultComesLastStyle defaultComesLast() {
        return new DefaultComesLastStyle(false);
    }

    public static final EmptyBlockStyle.BlockPolicy defaultBlockPolicy = EmptyBlockStyle.BlockPolicy.TEXT;

    public static EmptyBlockStyle emptyBlock() {
        return new EmptyBlockStyle(EmptyBlockStyle.BlockPolicy.TEXT, true, true, true, true,
                true, true, true, true, true, true, true, true);
    }

    public static EqualsAvoidsNullStyle equalsAvoidsNull() {
        return new EqualsAvoidsNullStyle(false);
    }

    public static ExplicitInitializationStyle explicitInitialization() {
        return new ExplicitInitializationStyle(false);
    }

    public static FallThroughStyle fallThrough() {
        return new FallThroughStyle(false);
    }

    public static HiddenFieldStyle hiddenFieldStyle() {
        return new HiddenFieldStyle(true, true, true, false);
    }

    public static HideUtilityClassConstructorStyle hideUtilityClassConstructorStyle() {
        return new HideUtilityClassConstructorStyle(Arrays.asList(
                "@lombok.experimental.UtilityClass",
                "@lombok.NoArgsConstructor",
                "@lombok.Data"
        ));
    }

    public static NeedBracesStyle needBracesStyle() {
        return new NeedBracesStyle(false, false);
    }

    public static NoWhitespaceAfterStyle noWhitespaceAfterStyle() {
        return new NoWhitespaceAfterStyle(true, false, false, true, true, true,
                true, false, true, true, true, true, true, true);
    }

    public static NoWhitespaceBeforeStyle noWhitespaceBeforeStyle() {
        return new NoWhitespaceBeforeStyle(false, false, true, true, false, false,
                false, true, true);
    }

    public static final OperatorWrapStyle.WrapOption defaultOperatorWrapStyleOption = OperatorWrapStyle.WrapOption.NL;

    public static OperatorWrapStyle operatorWrapStyle() {
        return new OperatorWrapStyle(OperatorWrapStyle.WrapOption.NL, true, true, true, true, true,
                true, true, true, true, true, true, true, true, true, true,
                true, true, true, true, true, true, true, true,
                false, false, false, false, false, false,
                false, false, false, false, false, false, false);
    }

    public static TypecastParenPadStyle typecastParenPadStyle() {
        return new TypecastParenPadStyle(false);
    }

    public static UnnecessaryParenthesesStyle unnecessaryParentheses() {
        return new UnnecessaryParenthesesStyle(true, true, true, true, true,
                true, true, true, true, true, true, true,
                true, true, true, true, true, true,
                true, true, true, true, true);
    }

    public static EmptyForInitializerPadStyle emptyForInitializerPadStyle() {
        return new EmptyForInitializerPadStyle(false);
    }

    public static EmptyForIteratorPadStyle emptyForIteratorPadStyle() {
        return new EmptyForIteratorPadStyle(false);
    }

    public static MethodParamPadStyle methodParamPadStyle() {
        return new MethodParamPadStyle(false, false);
    }

    public static CustomImportOrderStyle customImportOrderStyle() {
        return new CustomImportOrderStyle(Arrays.asList(new CustomImportOrderStyle.GroupWithDepth(CustomImportOrderStyle.CustomImportOrderGroup.STATIC, null),
                new CustomImportOrderStyle.GroupWithDepth(CustomImportOrderStyle.CustomImportOrderGroup.STANDARD_JAVA_PACKAGE, null),
                new CustomImportOrderStyle.GroupWithDepth(CustomImportOrderStyle.CustomImportOrderGroup.THIRD_PARTY_PACKAGE, null)),
                true, false, "^$", "^(java|javax)\\.", ".*");
    }

    private static UnusedImportsStyle unusedImportsStyle() {
        return new UnusedImportsStyle(false);
    }
}
