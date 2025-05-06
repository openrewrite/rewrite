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
package org.openrewrite.java.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class FieldsOfTypeUses extends DataTable<FieldsOfTypeUses.Row> {
    public FieldsOfTypeUses(Recipe recipe) {
        super(recipe,
                "Fields of type uses",
                "Information about fields that match a specific type.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file that contains the field declaration.")
        String sourceFile;

        @Column(displayName = "Field name",
                description = "The name of the field.")
        String fieldName;

        @Column(displayName = "Field type",
                description = "The declared type of the field.")
        String fieldType;

        @Column(displayName = "Concrete field type",
                description = "The concrete type of the field, which may be a subtype of a searched type.")
        String concreteFieldType;

        @Column(displayName = "Field modifiers",
                description = "The modifiers of the field.")
        String fieldModifiers;

        @Column(displayName = "Field declaration",
                description = "The source code of the field declaration.")
        String fieldDeclaration;
    }
}
