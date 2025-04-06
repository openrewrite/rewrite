/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;

import java.time.Duration;

public class MavenDownloadEvents extends DataTable<MavenDownloadEvents.Row> {

    public MavenDownloadEvents(Recipe recipe) {
        super(recipe, "Maven download events",
                "Starts and ends of attempts to make requests to Maven repositories. The starts and ends " +
                "are recorded separately so that we do not end up with coordinated omission if some requests do not finish.");
    }

    @Value
    public static class Row {

        @Column(displayName = "URL",
                description = "The URL of the Maven POM or metadata file being downloaded.")
        String url;

        @Column(displayName = "Status",
                description = "The status of the download event. Valid: STARTED, SUCCESSFUL, FAILURE.")
        String outcome;

        @Column(displayName = "Status code",
                description = "The status code of the download attempt. If the download hasn't finished yet, this will be null.")
        @Nullable String statusCode;

        @Column(displayName = "Failure reason",
                description = "The reason the download failed. If the download was successful or hasn't finished yet, this will be null.")
        @Nullable String failureReason;

        @Column(displayName = "Duration Milliseconds",
                description = "The duration of the download attempt in milliseconds. If the download hasn't finished yet, this will be null.")
        @Nullable String durationMs;
    }

    public void insertStartedRow(ExecutionContext ctx, String url) {
        insertRow(ctx, new Row(url, "STARTED", null, null, null));
    }

    public void insertFinishedRow(ExecutionContext ctx, String url, @Nullable String statusCode, @Nullable Duration duration) {
        insertRow(ctx, new Row(url, "FINISHED", statusCode, null, duration != null ? String.valueOf(duration.toMillis()) : null));
    }

    public void insertErrorRow(ExecutionContext ctx, String url, @Nullable String statusCode, @Nullable String failureReason, @Nullable Duration duration) {
        insertRow(ctx, new Row(url, "ERROR", statusCode, failureReason, duration != null ? String.valueOf(duration.toMillis()) : null));
    }
}
