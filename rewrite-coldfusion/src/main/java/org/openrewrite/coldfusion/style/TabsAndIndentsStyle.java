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

import lombok.Value;
import lombok.With;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

/**
 * How CFML is indented. A level is {@link #indentSize} columns, written with tabs when {@link #useTabCharacter} is
 * set. A line that continues a statement is indented {@link #continuationIndent} columns from the line the statement
 * starts on.
 */
@Value
@With
public class TabsAndIndentsStyle implements Style {
    Boolean useTabCharacter;
    Integer tabSize;
    Integer indentSize;
    Integer continuationIndent;

    /**
     * Whether a blank line keeps the spaces or tabs it is indented with, as some editors leave them.
     */
    Boolean keepIndentsOnEmptyLines;

    /**
     * Whether a script {@code case} is indented from its {@code switch}.
     */
    Boolean indentCaseFromSwitch;

    /**
     * Whether a {@code )}, {@code ]} or <code>}</code> that closes a list on a line of its own is indented like the list's
     * elements, rather than aligned with the line that opens the list.
     */
    Boolean indentClosingBrackets;

    Tags tags;

    /**
     * How the bodies of tags are indented. A tag's body is indented from the tag, and a {@code <cfelse>} or
     * {@code <cfelseif>} is aligned with its {@code <cfif>}. These say where tags differ.
     */
    @Value
    @With
    public static class Tags {
        /**
         * Whether the functions and properties of a {@code <cfcomponent>} are indented from it.
         */
        Boolean indentComponentBody;

        /**
         * Whether the script in a {@code <cfscript>} is indented from it.
         */
        Boolean indentScriptBody;

        /**
         * Whether the tags in a {@code <cfsilent>} are indented from it. A {@code <cfsilent>} often wraps a whole
         * template, which is then written as if it were not there.
         */
        Boolean indentSilentBody;

        /**
         * Whether a {@code <cfcase>} or {@code <cfdefaultcase>} is indented from its {@code <cfswitch>}.
         */
        Boolean indentCaseFromSwitch;

        /**
         * Whether a {@code <cfcatch>} or {@code <cffinally>} is indented from its {@code <cftry>}.
         */
        Boolean indentCatchFromTry;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(CFFormat.tabsAndIndents(), this);
    }
}
