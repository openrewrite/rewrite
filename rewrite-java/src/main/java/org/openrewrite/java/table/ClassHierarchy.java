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
import org.openrewrite.internal.lang.Nullable;

public class ClassHierarchy extends DataTable<ClassHierarchy.Row> {

    public ClassHierarchy(Recipe recipe) {
        super(recipe, "Class hierarchy", "Record the classes");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source Path",
                description = "The path to the source file which declares the subclass.")
        String sourcePath;

        @Column(displayName = "Class name",
                description = "The class or interface which extends or implements another class or interface.")
        String className;

        @Column(displayName = "Superclass",
                description = "The class extended by the subclass.")
        String superclass;

        @Nullable
        @Column(displayName = "Interfaces",
                description = "Comma-separated list of interfaces implemented by the class.")
        String interfaces;
    }
}
