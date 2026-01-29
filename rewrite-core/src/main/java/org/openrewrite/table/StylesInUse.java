/*
 * Copyright 2025 the original author or authors.
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

public class StylesInUse extends DataTable<StylesInUse.Row> {

    public StylesInUse(Recipe recipe) {
        super(recipe, "Styles in use",
                "Styles detected on each source file.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path",
                description = "The source path of the file.")
        String sourcePath;

        @Column(displayName = "Style names",
                description = "The names of the styles attached to the file.")
        String styleNames;

        @Column(displayName = "Styles YAML",
                description = "The complete style configuration as OpenRewrite style YAML.")
        String stylesYaml;
    }
}
