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

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryCredentials;
import org.openrewrite.maven.tree.MavenRepositoryMirror;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

public class MavenExecutionContextView extends DelegatingExecutionContext {
    private static final String MAVEN_MIRRORS = "org.openrewrite.maven.mirrors";
    private static final String MAVEN_CREDENTIALS = "org.openrewrite.maven.auth";
    private static final String MAVEN_REPOSITORIES = "org.openrewrite.maven.repos";
    private static final String MAVEN_PINNED_SNAPSHOT_VERSIONS = "org.openrewrite.maven.pinnedSnapshotVersions";

    public MavenExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public void setMirrors(Collection<MavenRepositoryMirror> mirrors) {
        putMessage(MAVEN_MIRRORS, mirrors);
    }

    public Collection<MavenRepositoryMirror> getMirrors() {
        return getMessage(MAVEN_MIRRORS, emptyList());
    }

    public void setCredentials(Collection<MavenRepositoryCredentials> credentials) {
        putMessage(MAVEN_CREDENTIALS, credentials);
    }

    public Collection<MavenRepositoryCredentials> getCredentials() {
        return getMessage(MAVEN_CREDENTIALS, emptyList());
    }

    public void setRepositories(List<MavenRepository> repositories) {
        putMessage(MAVEN_REPOSITORIES, repositories);
    }

    public List<MavenRepository> getRepositories() {
        return getMessage(MAVEN_REPOSITORIES, emptyList());
    }

    /**
     * Require dependency resolution that encounters a matching group:artifact:version coordinate to resolve to a
     * particular dated snapshot version, effectively making snapshot resolution deterministic.
     *
     * @param pinnedSnapshotVersions A set of group:artiact:version and the dated snapshot version to pin them to.
     */
    public void setPinnedSnapshotVersions(Collection<GroupArtifactVersion> pinnedSnapshotVersions) {
        putMessage(MAVEN_PINNED_SNAPSHOT_VERSIONS, pinnedSnapshotVersions);
    }

    public Collection<GroupArtifactVersion> getPinnedSnapshotVersions() {
        return getMessage(MAVEN_PINNED_SNAPSHOT_VERSIONS, emptyList());
    }
}
