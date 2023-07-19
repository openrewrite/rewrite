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
import org.openrewrite.marker.LstProvenance;

@JsonIgnoreType
public class LstProvenanceTable extends DataTable<LstProvenanceTable.Row> {

    public LstProvenanceTable(Recipe recipe) {
        super(recipe,
                "Parser failures",
                "A list of files that failed to parse along with stack traces of their failures.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Build tool type",
                description = "The type of tool which produced the LST.")
        LstProvenance.Type buildToolType;

        @Column(displayName = "Build tool version",
                description = "The version of the build tool which produced the LST.")
        String buildToolVersion;

        @Column(displayName = "LST serializer version",
                description = "The version of LST serializer which produced the LST.")
        String lstSerializerVersion;
    }
}
