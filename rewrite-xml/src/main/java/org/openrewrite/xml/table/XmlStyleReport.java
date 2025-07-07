/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.xml.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class XmlStyleReport extends DataTable<XmlStyleReport.Row> {

    public XmlStyleReport(Recipe recipe) {
        super(recipe, "XML style report",
                "Records style information about XML documents. Used for debugging style auto-detection issues.");
    }

    @Value
    public static class Row {

        @Column(displayName = "File name",
                description = "The name of the file that was analyzed.")
        String name;

        @Column(displayName = "Use tabs",
                description = "When 'true', tabs are used for indentation. When 'false', spaces are used.")
        boolean useTabCharacter;

        @Column(displayName = "Indent size",
                description = "The number of spaces that are used for each level of indentation.")
        int indentSize;

        @Column(displayName = "Tab size",
                description = "The number of spaces that a tab character represents.")
        int tabSize;

        @Column(displayName = "Continuation indent size",
                description = "The number of spaces that are used to indent an attribute that is on its own line.")
        int continuationIndentSize;

        @Column(displayName = "Indent count",
                description = "Count of tags in the file whose prefixes were evaluated.")
        int indentCount;

        @Column(displayName = "Indents matching own style",
                description = "Count of tags in the file whose prefix match the style of the file itself.")
        int indentsMatchingOwnStyle;

        @Column(displayName = "Indents matching project style",
                description = "Count of tags in the file whose prefix match the overall style of the project.")
        int indentsMatchingProjectStyle;

        @Column(displayName = "Continuation indent count",
                description = "Count of attributes in the file whose prefixes were evaluated.")
        int continuationIndentCount;

        @Column(displayName = "Continuation indents matching own style",
                description = "CCount of attributes in the file whose prefix matches  the style of the file itself.")
        int continuationIndentsMatchingOwnStyle;

        @Column(displayName = "Continuation indents matching project style",
                description = "Count of attributes in the file whose prefix matches the overall style of the project.")
        int continuationIndentsMatchingProjectStyle;
    }
}
