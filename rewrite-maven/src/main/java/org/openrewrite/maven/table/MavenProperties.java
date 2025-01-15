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
package org.openrewrite.maven.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class MavenProperties extends DataTable<MavenProperties.Row> {

    public MavenProperties(Recipe recipe) {
        super(recipe, Row.class,
                MavenProperties.class.getName(),
                "Maven properties", "Property and value.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Property",
                description = "The Maven property that was found.")
        String property;

        @Column(displayName = "Value",
                description = "The value associated with the property.")
        @Nullable
        String value;
    }
}
