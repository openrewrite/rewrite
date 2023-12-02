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
public class TypeMappings extends DataTable<TypeMappings.Row> {

    public TypeMappings(Recipe recipe) {
        super(recipe,
                "Type mapping",
                "The types mapped to J trees.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file that the method call occurred in.")
        String sourceFile;

        @Column(displayName = "Tree class name",
                description = "The class name of the tree.")
        String treeName;

        @Column(displayName = "Java type class name",
                description = "The class name of the java type.")
        String typeName;

        @Column(displayName = "Java type class name",
                description = "The class name of the java type.")
        Integer count;
    }
}
