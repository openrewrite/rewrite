/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven;

import lombok.Getter;
import lombok.experimental.NonFinal;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;

@Getter
public class MavenDownloadingException extends Exception {
    /**
     * The direct dependency, parent, or dependency management dependency whose resolution failed.
     */
    @NonFinal
    @Nullable
    private GroupArtifactVersion root;

    /**
     * The potentially transitive dependency, parent ancestor, or transitive dependency management
     * dependency that resolution failed on. If the resolution failed on a direct dependency, this
     * will match {@link #root}.
     */
    private final GroupArtifactVersion failedOn;

    /**
     * The result of each repository that was tried. This will be either an HTTP status code
     * or a reason why the downloaded artifact was invalid from a given repository.
     */
    @NonFinal
    private Map<MavenRepository, String> repositoryResponses = Collections.emptyMap();

    public MavenDownloadingException setRoot(GroupArtifactVersion root) {
        this.root = root;
        return this;
    }

    public GroupArtifactVersion getRoot() {
        return root == null ? failedOn : root;
    }

    /**
     * @param repositoryResponses The HTTP response codes of the repositories that were tried, as an
     *                            instance of {@link Map} that iterates the responses in the order in
     *                            which the repositories were tried.
     * @return This exception instance.
     */
    public MavenDownloadingException setRepositoryResponses(Map<MavenRepository, String> repositoryResponses) {
        this.repositoryResponses = repositoryResponses;
        return this;
    }

    public MavenDownloadingException(String message, @Nullable Throwable cause, GroupArtifactVersion failedOn) {
        super(message, cause);
        this.failedOn = failedOn;
    }

    @Override
    public String getMessage() {
        String message = "";
        if (!failedOn.equals(root)) {
            message += failedOn + " failed. ";
        }
        message += super.getMessage();

        if (!repositoryResponses.isEmpty()) {
            StringJoiner repos = new StringJoiner("\n");
            for (Map.Entry<MavenRepository, String> repoResponse : repositoryResponses.entrySet()) {
                repos.add(repoResponse.getKey().getUri() + ": " + repoResponse.getValue());
            }
            return message + " Tried repositories:\n" + repos;
        }
        return message;
    }

    public <T extends Tree> T warn(T t) {
        return Markup.warn(t, this);
    }
}
