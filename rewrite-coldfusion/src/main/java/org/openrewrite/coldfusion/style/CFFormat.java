/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.style.NamedStyles;

import java.util.Arrays;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

/**
 * The defaults of CFFormat, the CommandBox module {@code commandbox-cfformat}, which a {@code .cfformat.json}
 * configures. CFML has no IDE style that everyone uses, so this is the formatter its community shares. It indents
 * with four spaces, pads operators but not parentheses, brackets or braces, and writes {@code key: value}. A blank
 * line or two between statements stays, as CFFormat keeps them in script. Where CFFormat has no setting, as for blank
 * lines around functions, the default is IntelliJ's.
 */
public class CFFormat extends NamedStyles {
    private static final CFFormat INSTANCE = new CFFormat();

    // Public so that Environment can find and activate this style. From code, use CFFormat.defaults().
    public CFFormat() {
        super(randomId(),
                "org.openrewrite.coldfusion.style.CFFormat",
                "CFFormat",
                "The defaults of CFFormat, the formatter CommandBox runs on CFML.",
                emptySet(),
                Arrays.asList(
                        tabsAndIndents(),
                        spaces(),
                        blankLines(),
                        wrappingAndBraces()
                )
        );
    }

    @JsonCreator
    public static CFFormat defaults() {
        return INSTANCE;
    }

    /**
     * {@code indent_size} 4 and {@code tab_indent} false. CFFormat indents a wrapped line one level, aligns a closing
     * bracket with the line that opens it, writes a {@code <cfscript>} body at its tag's level and every other tag's
     * body a level in, and leaves blank lines empty.
     */
    public static TabsAndIndentsStyle tabsAndIndents() {
        return new TabsAndIndentsStyle(false, 4, 4, 4, false, true, false,
                new TabsAndIndentsStyle.Tags(true, false, true, true, true));
    }

    /**
     * {@code binary_operators.padding} true, {@code keywords.spacing_to_group} true, every {@code padding} and
     * {@code empty_padding} false, {@code struct.separator} {@code ": "}, {@code for_loop_semicolons.padding} true,
     * the {@code spaced} block spacings, {@code key_value.padding} false for attributes, and no {@code alignment}.
     */
    public static SpacesStyle spaces() {
        return new SpacesStyle(
                new SpacesStyle.BeforeParentheses(true, false, false, false),
                new SpacesStyle.AroundOperators(true, true, true, true),
                new SpacesStyle.Within(false, false, false, false, false, false, false, false, false, false, false, false),
                new SpacesStyle.Other(false, true, false, true, false, true, true, false, true, true, true, true, true, true, false),
                new SpacesStyle.Tags(false, false, false, false)
        );
    }

    /**
     * CFFormat trims the start and end of a block and keeps the blank lines between script statements. Tags get at
     * most one between them, but the most this keeps is IntelliJ's two, for both.
     */
    public static BlankLinesStyle blankLines() {
        return new BlankLinesStyle(
                new BlankLinesStyle.KeepMaximum(2, 2, 0),
                new BlankLinesStyle.Minimum(1)
        );
    }

    /**
     * CFFormat writes every block over lines, and its {@code group_to_block_spacing}, {@code spacing_to_block} and
     * {@code block_to_keyword_spacing} default to {@code spaced}, which keeps braces and keywords on the line before.
     */
    public static WrappingAndBracesStyle wrappingAndBraces() {
        return new WrappingAndBracesStyle(false,
                new WrappingAndBracesStyle.Braces(false, false, false),
                new WrappingAndBracesStyle.KeywordsAfterBlock(false, false, false));
    }
}
