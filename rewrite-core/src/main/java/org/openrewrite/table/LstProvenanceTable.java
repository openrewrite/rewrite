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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.LstProvenance;

@JsonIgnoreType
public class LstProvenanceTable extends DataTable<LstProvenanceTable.Row> {

    public LstProvenanceTable(Recipe recipe) {
        super(recipe,
                "LST provenance",
                "Table showing which tools were used to produce LSTs.");
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

        @Column(displayName = "Timestamp (epoch millis)",
                description = "UTC timestamp describing when the LST was produced, in milliseconds since the unix epoch.")
        @Nullable
        Long timestampEpochMillis;

        @Column(displayName = "Timestamp",
                description = "UTC timestamp describing when the LST was produced, in ISO-8601 format. e.g.: \"2023‐08‐07T22:24:06+00:00 UTC+00:00\"")
        @Nullable
        String timestampUtc;
    }
}
