/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.Map;

@JsonIgnoreType
public class MavenMetadataFailures extends DataTable<MavenMetadataFailures.Row> {
    public MavenMetadataFailures(Recipe recipe) {
        super(recipe, Row.class, MavenMetadataFailures.class.getName(),
                "Maven metadata failures",
                "Attempts to resolve maven metadata that failed.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Group id",
                description = "The groupId of the artifact for which the metadata download failed.")
        String group;
        @Column(displayName = "Artifact id",
                description = "The artifactId of the artifact for which the metadata download failed.")
        String artifactId;
        @Column(displayName = "Version",
                description = "The version of the artifact for which the metadata download failed.")
        String version;
        @Column(displayName = "Maven repository",
                description = "The URL of the Maven repository that the metadata download failed on.")
        String mavenRepositoryUri;
        @Column(displayName = "Snapshots",
                description = "Does the repository support snapshots.")
        String snapshots;
        @Column(displayName = "Releases",
                description = "Does the repository support releases.")
        String releases;
        @Column(displayName = "Failure",
                description = "The reason the metadata download failed.")
        String failure;
    }

    public interface MavenMetadataDownloader {
        MavenMetadata download() throws MavenDownloadingException;
    }

    public MavenMetadata insertRows(ExecutionContext ctx, MavenMetadataDownloader download) throws MavenDownloadingException {
        try {
            return download.download();
        } catch (MavenDownloadingException e) {
            GroupArtifactVersion failedOn = e.getFailedOn();
            for (Map.Entry<MavenRepository, String> repositoryResponse : e.getRepositoryResponses().entrySet()) {
                insertRow(ctx, new Row(
                        failedOn.getGroupId(),
                        failedOn.getArtifactId(),
                        failedOn.getVersion(),
                        repositoryResponse.getKey().getUri(),
                        repositoryResponse.getKey().getSnapshots(),
                        repositoryResponse.getKey().getReleases(),
                        repositoryResponse.getValue()
                ));
            }
            throw e;
        }
    }
}
