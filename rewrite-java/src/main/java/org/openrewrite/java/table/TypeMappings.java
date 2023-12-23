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
import org.openrewrite.java.tree.J;

@JsonIgnoreType
public class TypeMappings extends DataTable<TypeMappings.Row> {

    public TypeMappings(Recipe recipe) {
        super(recipe,
                "Type mapping",
                "The types mapped to `J` trees.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Compilation unit class name",
                description = "The root compilation unit class name containing the mapping.")
        String compilationUnitName;

        @Column(displayName = "Tree class name",
                description = "The simple class name of the `J` element.")
        String treeName;

        @Column(displayName = "Java type class name",
                description = "The simple class name of the `JavaType`.")
        String typeName;

        @Column(displayName = "Count",
                description = "The number of times this tree and type pair occurred in a repository.")
        Integer count;

        /**
         * When {@link #typeName} is null, this is the nearest non-null {@link J} type
         * in the cursor stack.
         */
        @Column(displayName = "Nearest non-null tree class name",
                description = "The simple class name of the nearest non-null `J` element when " +
                              "`typeName` is null.")
        String nearestNonNullTreeName;
    }
}
