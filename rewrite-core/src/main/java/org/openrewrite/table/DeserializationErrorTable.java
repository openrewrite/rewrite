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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

@JsonIgnoreType
public class DeserializationErrorTable extends DataTable<DeserializationErrorTable.Row> {

    public DeserializationErrorTable(Recipe recipe) {
        super(recipe,
                "Deserialization errors",
                "Table collecting any LST deserialization errors.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source path", description = "The source that failed to be deserialized.")
        String sourcePath;

        @Column(displayName = "Error message", description = "The error message of the exception that produced the error.")
        @Nullable
        String errorMessage;

        @Column(displayName = "Stack trace", description = "The stack trace of the error.")
        String stackTrace;

        @Column(displayName = "Language",
                description = "Language of source file in case it requires a newer CLI version for the ingestion.")
        @Nullable
        String language;

        @Column(displayName = "Minimum CLI version required by language",
                description = "Minimum CLI version required to be able to successfully ingest sources of this language.")
        @Nullable
        String minimumVersion;

        @Column(displayName = "Actual CLI version used for ingestion",
                description = "Actual CLI version used to ingest this source.")
        @Nullable
        String actualVersion;

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
