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
package org.openrewrite.kotlin.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class KotlinSourceFile extends DataTable<KotlinSourceFile.Row> {

    public KotlinSourceFile(Recipe recipe) {
        super(recipe, "Kotlin source files",
                "Kotlin sources present in LSTs on the SAAS.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path before the run",
                description = "The source path of the file before the run.")
        String sourcePath;

        @Column(displayName = "Source file type", description = "The source file type that was created.")
        SourceFileType sourceFileType;
    }

    public enum SourceFileType {
        Kotlin,
        Quark,
        PlainText
    }
}
