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

public class SourcesFiles extends DataTable<SourcesFiles.Row> {

    public SourcesFiles(Recipe recipe) {
        super(recipe, "Source files that matched",
                "Source files that matched some criteria.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path before the run",
                description = "The source path of the file before the run.")
        String sourcePath;

        @Column(displayName = "LST type",
                description = "The LST model type that the file is parsed as.")
        String type;
    }
}
