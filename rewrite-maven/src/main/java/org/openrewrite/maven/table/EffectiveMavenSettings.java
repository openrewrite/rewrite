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

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class EffectiveMavenSettings extends DataTable<EffectiveMavenSettings.Row> {

    public EffectiveMavenSettings(Recipe recipe) {
        super(recipe, EffectiveMavenSettings.Row.class,
                EffectiveMavenSettings.class.getName(),
                "Effective maven settings",
                "The maven settings file used by each pom.");
    }

    @Value
    public static class Row {
        @Column(displayName = "POM",
                description = "The location of the `pom.xml`.")
        String pom;

        @Column(displayName = "Maven settings",
                description = "Effective maven settings.")
        String mavenSettings;
    }
}
