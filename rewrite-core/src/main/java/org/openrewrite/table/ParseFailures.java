/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

@JsonIgnoreType
public class ParseFailures extends DataTable<ParseFailures.Row> {
    public ParseFailures(Recipe recipe) {
        super(recipe,
                "Parser failures",
                "A list of files that failed to parse along with stack traces of their failures.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Parser", description = "The parser implementation that failed.")
        String parser;

        @Column(displayName = "Source path", description = "The file that failed to parse.")
        String sourcePath;

        @Column(displayName = "Exception type", description = "The class name of the exception that produce the parse failure.")
        @Nullable
        String exceptionType;

        @Column(displayName = "Tree type", description = "The type of the tree element that was being parsed " +
                                                         "when the failure occurred. This can refer either to the intended " +
                                                         "target OpenRewrite Tree type or a parser or compiler internal tree " +
                                                         "type that we couldn't determine how to map.")
        @Nullable
        String treeType;

        @Column(displayName = "Snippet",
                description = "The code snippet that the failure occurred on. Omitted when the parser fails on the whole file.")
        @Nullable
        String snippet;

        @Column(displayName = "Stack trace", description = "The stack trace of the failure.")
        String stackTrace;
    }
}
