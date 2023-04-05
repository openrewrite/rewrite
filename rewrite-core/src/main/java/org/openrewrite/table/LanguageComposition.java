/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class LanguageComposition extends DataTable<LanguageComposition.Row> {

    public LanguageComposition(Recipe recipe) {
        super(recipe, "Per-file language composition report",
                "A list of individual files and their language composition.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The path to the source file.")
        String sourcePath;

        @Column(displayName = "Language",
                description = "The language of the source file.")
        String language;

        @Column(displayName = "Weight",
                description = "The weight of the source file, in terms of " +
                              "total number of AST nodes, markers, and type " +
                              "attribution nodes.")
        Long weight;

        @Column(displayName = "Lines of text",
                description = "The number of lines of text in the source file. " +
                        "No language-specific knowledge to skip comments, blank lines, or any other non-code line.")
        Integer linesOfText;
    }
}
