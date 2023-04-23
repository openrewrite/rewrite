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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class ClasspathReport extends DataTable<ClasspathReport.Row> {

    public ClasspathReport(Recipe recipe) {
        super(recipe,
                "Classpath report",
                "Contains a report of the runtime classpath and any other jars found inside each classpath entry.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Classpath entry URI",
                description = "The URI where a particular classpath entry can be found. " +
                        "May point to a jar or a directory containing class files. " +
                        "Local to the system running the recipe.")
        String uri;

        @Column(displayName = "Classpath entry resource",
                description = "Path within a classpath entry to a jar file it contains. Resources not ending in \"jar\" are ignored.")
        String resource;
    }
}
