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
package org.openrewrite.gradle.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class GradleWrappersInUse extends DataTable<GradleWrappersInUse.Row> {
    public GradleWrappersInUse(Recipe recipe) {
        super(recipe, Row.class,
                GradleWrappersInUse.class.getName(),
                "Gradle wrappers in use",
                "Gradle wrappers in use.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Wrapper version",
                description = "The version of the Gradle wrapper in use.")
        String version;

        @Column(displayName = "Wrapper distribution",
                description = "The distribution type of the Gradle wrapper in use.")
        String distribution;
    }
}
