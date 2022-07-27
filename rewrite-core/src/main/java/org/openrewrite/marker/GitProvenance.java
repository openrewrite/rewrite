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
package org.openrewrite.marker;


import lombok.Value;
import lombok.With;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.RemoteConfig;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.ci.BuildEnvironment;
import org.openrewrite.marker.ci.JenkinsBuildEnvironment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

@Value
@With
public class GitProvenance implements Marker {
    UUID id;

    @Nullable
    String origin;

    @Nullable
    String branch;

    String change;

    @Nullable
    public String getOrganizationName() {
        if (origin == null) {
            return null;
        }
        String path;
        if (origin.startsWith("git")) {
            path = origin.substring(origin.indexOf(':') + 1);
        } else {
            path = URI.create(origin).getPath().substring(1);
        }
        int firstSlashPos = path.lastIndexOf('/');
        int secondSlashPos = path.lastIndexOf('/', firstSlashPos - 1);

        if (secondSlashPos > -1) {
            return path.substring(secondSlashPos + 1, firstSlashPos);
        } else if (firstSlashPos > -1) {
            return path.substring(0, firstSlashPos);
        } else {
            return "";
        }
    }

    @Nullable
    public String getRepositoryName() {
        if (origin == null) {
            return null;
        }
        if (origin.startsWith("git")) {
            return origin.substring(origin.lastIndexOf('/') + 1).replaceAll("\\.git$", "");
        } else {
            String path = URI.create(origin).getPath();
            return path.substring(path.lastIndexOf('/') + 1).replaceAll("\\.git$", "");
        }
    }

    /**
     * @param projectDir The project directory.
     * @return A marker containing git provenance information.
     * @deprecated Use {@link #fromProjectDirectory(Path, BuildEnvironment) instead}.
     */
    @Deprecated
    public static @Nullable GitProvenance fromProjectDirectory(Path projectDir) {
        return fromProjectDirectory(projectDir, null);
    }

    /**
     * @param projectDir The project directory.
     * @param buildEnvironment In detached head scenarios, the branch is best
     *                determined from a {@link BuildEnvironment} marker if possible.
     * @return A marker containing git provenance information.
     */
    public static @Nullable GitProvenance fromProjectDirectory(Path projectDir, @Nullable BuildEnvironment buildEnvironment) {
        try (Repository repository = new RepositoryBuilder().findGitDir(projectDir.toFile()).build()) {
            String branch = null;
            String changeset = getChangeset(repository);

            if (!repository.getBranch().equals(changeset)) {
                branch = repository.getBranch();
            } else if (buildEnvironment instanceof JenkinsBuildEnvironment) {
                JenkinsBuildEnvironment jenkins = (JenkinsBuildEnvironment) buildEnvironment;
                branch = jenkins.getLocalBranch() != null ?
                        jenkins.getLocalBranch() :
                        localBranchName(repository, jenkins.getBranch());
            }

            if (branch == null) {
                Git git = Git.open(repository.getDirectory());
                ObjectId commit = repository.resolve(Constants.HEAD);
                Map<ObjectId, String> branchesByCommit = git.nameRev().addPrefix("refs/heads/").add(commit).call();
                if (branchesByCommit.containsKey(commit)) {
                    // detached head but _not_ a shallow clone
                    branch = branchesByCommit.get(commit);
                    if (branch.contains("^")) {
                        branch = branch.substring(0, branch.indexOf('^'));
                    } else if (branch.contains("~")) {
                        branch = branch.substring(0, branch.indexOf('~'));
                    }
                } else {
                    // detached head and _also_ a shallow clone
                    branchesByCommit = git.nameRev().add(commit).call();
                    branch = localBranchName(repository, branchesByCommit.get(commit));
                }
            }

            return new GitProvenance(randomId(), getOrigin(repository), branch, changeset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalArgumentException | GitAPIException e) {
            return null;
        }
    }

    @Nullable
    private static String localBranchName(Repository repository, @Nullable String remoteBranch) throws IOException, GitAPIException {
        if (remoteBranch == null) {
            return null;
        }

        String branch = null;
        try {
            Git git = Git.open(repository.getDirectory());
            /*~~>*/List<RemoteConfig> remotes = git.remoteList().call();
            for (RemoteConfig remote : remotes) {
                if (remoteBranch.startsWith(remote.getName()) &&
                        (branch == null || branch.length() > remoteBranch.length() - remote.getName().length() - 1)) {
                    branch = remoteBranch.substring(remote.getName().length() + 1); // +1 for the forward slash
                }
            }
        } catch (GitAPIException ignored) {
        }
        return branch;
    }

    @Nullable
    private static String getOrigin(Repository repository) {
        Config storedConfig = repository.getConfig();
        String url = storedConfig.getString("remote", "origin", "url");
        if (url == null) {
            return null;
        }
        if (url.startsWith("https://") || url.startsWith("http://")) {
            url = hideSensitiveInformation(url);
        }
        return url;
    }

    @Nullable
    private static String getChangeset(Repository repository) throws IOException {
        ObjectId head = repository.resolve(Constants.HEAD);
        if (head == null) {
            return null;
        }
        return head.getName();
    }

    private static String hideSensitiveInformation(String url) {
        try {
            String credentials = URI.create(url).toURL().getUserInfo();
            if (credentials != null) {
                return url.replaceFirst(credentials, credentials.replaceFirst(":.*", ""));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to remove credentials from repository URL. {0}", e);
        }
        return url;
    }
}
