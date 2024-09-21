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
package org.openrewrite.java.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class CompileErrors extends DataTable<CompileErrors.Row> {
    public CompileErrors(Recipe recipe) {
        super(recipe,
                "Compile errors",
                "The source code of compile errors.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file that the method call occurred in.")
        String sourceFile;

        @Column(displayName = "Source",
                description = "The source code of the type use.")
        String code;
    }
}
