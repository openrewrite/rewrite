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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

@JsonIgnoreType
public class ParseToPrintInequalities extends DataTable<ParseToPrintInequalities.Row> {
    public ParseToPrintInequalities(Recipe recipe) {
        super(recipe,
                "Parser to print inequalities",
                "A list of files that parsers produced `SourceFile` which, when printed, " +
                "didn't match the original source code.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path", description = "The file that failed to parse.")
        String sourcePath;

        @Column(displayName = "Diff",
                description = "The diff between the original source code and the printed `SourceFile`.")
        @Nullable
        String diff;
    }
}
