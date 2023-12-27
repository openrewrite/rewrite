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
package org.openrewrite.java.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class TokenCount extends DataTable<TokenCount.Row> {

    public TokenCount(Recipe recipe) {
        super(recipe,
                "Token count",
                "The number of tokens from a code snippet");
    }

    @Value
    public static class Row {

        @Column(displayName = "Name of Class or Method",
                description = "The name of the class or method.")
        String codeSnippet;

        @Column(displayName = "Tokens",
                description = "The number of tokens estimated in the code snippet.")
        int tokens;
    }
}
