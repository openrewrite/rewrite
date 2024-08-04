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


import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.api.errors.GitAPIException;
import org.openrewrite.jgit.lib.*;
import org.openrewrite.jgit.revwalk.RevCommit;
import org.openrewrite.jgit.transport.RemoteConfig;
import org.openrewrite.jgit.transport.URIish;
import org.openrewrite.jgit.treewalk.WorkingTreeOptions;
import org.openrewrite.marker.ci.BuildEnvironment;
import org.openrewrite.marker.ci.IncompleteGitConfigException;
import org.openrewrite.marker.ci.JenkinsBuildEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

@Value
@With
@RequiredArgsConstructor
@AllArgsConstructor
public class GitProvenance implements Marker {
    UUID id;

    /**
     * The URL of the origin remote, not to be confused with the SCM origin
     * which can be found under scmUrlComponents.getOrigin() which contains the base url to the SCM server
     */
    @Nullable
    String origin;

    @Nullable
    String branch;

    @Nullable
    String change;

    @Nullable
    AutoCRLF autocrlf;

    @Nullable
    EOL eol;

    @Nullable
    @Incubating(since = "8.9.0")
    List<Committer> committers;

    @Incubating(since = "8.33.0")
    @Nullable
    @NonFinal
    transient GitRemote gitRemote = null;

    private @Nullable GitRemote determineGitRemote(GitRemote.Parser parser) {
        return parser.parse(origin);
    }

    private @Nullable GitRemote getGitRemote() {
        if (gitRemote == null) {
            gitRemote = determineGitRemote(new GitRemote.Parser());
        }
        return gitRemote;
    }

    /**
     * Extract the organization name, including sub-organizations for git hosting services which support such a concept,
     * from the origin URL. Needs to be supplied with the
     *
     * @param baseUrl the portion of the URL which precedes the organization
     * @return the portion of the git origin URL which corresponds to the organization the git repository is organized under
     * @deprecated use {@link #getOrganizationName()} instead }
     */
    @Deprecated
    public @Nullable String getOrganizationName(String baseUrl) {
        GitRemote gitRemote = getGitRemote();
        if (gitRemote != null) {
            return gitRemote.getOrganization();
        }
        if (origin == null) {
            return null;
        }
        int schemeEndIndex = baseUrl.indexOf("://");
        if (schemeEndIndex != -1) {
            baseUrl = baseUrl.substring(schemeEndIndex + 3);
        }
        if (baseUrl.startsWith("git@")) {
            baseUrl = baseUrl.substring(4);
        }
        String remainder = origin.substring(origin.indexOf(baseUrl) + baseUrl.length());
        if (remainder.startsWith(":")) {
            remainder = remainder.substring(1);
        }
        if (remainder.startsWith("/")) {
            remainder = remainder.substring(1);
        }
        return remainder.substring(0, remainder.lastIndexOf('/'));
    }

    public @Nullable String getOrganizationName() {
        return Optional.ofNullable(getGitRemote())
                .map(GitRemote::getOrganization)
                .orElse(null);
    }

    public @Nullable String getRepositoryName() {
        return Optional.ofNullable(getGitRemote())
                .map(GitRemote::getRepositoryName)
                .orElse(null);
    }

    public static String getRepositoryPath(String origin) {
        return Optional.ofNullable(new GitRemote.Parser().parse(origin))
                .map(GitRemote::getPath)
                .orElse("");
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
     * @param projectDir  The project directory.
     * @param environment In detached head scenarios, the branch is best
     *                    determined from a {@link BuildEnvironment} marker if possible.
     * @return A marker containing git provenance information.
     */
    public static @Nullable GitProvenance fromProjectDirectory(Path projectDir, @Nullable BuildEnvironment environment) {
        if (environment != null) {
            if (environment instanceof JenkinsBuildEnvironment) {
                JenkinsBuildEnvironment jenkinsBuildEnvironment = (JenkinsBuildEnvironment) environment;
                try (Repository repository = new RepositoryBuilder().findGitDir(projectDir.toFile()).build()) {
                    String branch = jenkinsBuildEnvironment.getLocalBranch() != null
                            ? jenkinsBuildEnvironment.getLocalBranch()
                            : localBranchName(repository, jenkinsBuildEnvironment.getBranch());
                    return fromGitConfig(repository, branch, getChangeset(repository));
                } catch (IllegalArgumentException | GitAPIException e) {
                    // Silently ignore if the project directory is not a git repository
                    printRequireGitDirOrWorkTreeException(e);
                    return null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                File gitDir = new RepositoryBuilder().findGitDir(projectDir.toFile()).getGitDir();
                if (gitDir != null && gitDir.exists()) {
                    //it has been cloned with --depth > 0
                    return fromGitConfig(projectDir);
                } else {
                    //there is not .git config
                    try {
                        return environment.buildGitProvenance();
                    } catch (IncompleteGitConfigException e) {
                        return fromGitConfig(projectDir);
                    }
                }
            }
        } else {
            return fromGitConfig(projectDir);
        }
    }

    private static void printRequireGitDirOrWorkTreeException(Exception e) {
        if (!"requireGitDirOrWorkTree".equals(e.getStackTrace()[0].getMethodName())) {
            e.printStackTrace();
        }
    }

    private static @Nullable GitProvenance fromGitConfig(Path projectDir) {
        String branch = null;
        try (Repository repository = new RepositoryBuilder().findGitDir(projectDir.toFile()).build()) {
            String changeset = getChangeset(repository);
            if (!repository.getBranch().equals(changeset)) {
                branch = repository.getBranch();
            }
            return fromGitConfig(repository, branch, changeset);
        } catch (IllegalArgumentException e) {
            printRequireGitDirOrWorkTreeException(e);
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static GitProvenance fromGitConfig(Repository repository, @Nullable String branch, @Nullable String changeset) {
        if (branch == null) {
            branch = resolveBranchFromGitConfig(repository);
        }
        String remoteOriginUrl = getRemoteOriginUrl(repository);
        return new GitProvenance(randomId(), remoteOriginUrl, branch, changeset,
                getAutocrlf(repository), getEOF(repository),
                getCommitters(repository));
    }

    static @Nullable String resolveBranchFromGitConfig(Repository repository) {
        String branch;
        try {
            try (Git git = Git.open(repository.getDirectory())) {
                ObjectId commit = repository.resolve(Constants.HEAD);
                Map<ObjectId, String> branchesByCommit = git.nameRev().addPrefix("refs/heads/").add(commit).call();
                if (branchesByCommit.containsKey(commit)) {
                    // detached head but _not_ a shallow clone
                    branch = branchesByCommit.get(commit);
                } else {
                    // detached head and _also_ a shallow clone
                    branchesByCommit = git.nameRev().add(commit).call();
                    branch = localBranchName(repository, branchesByCommit.get(commit));
                }
                if (branch != null) {
                    if (branch.contains("^")) {
                        branch = branch.substring(0, branch.indexOf('^'));
                    } else if (branch.contains("~")) {
                        branch = branch.substring(0, branch.indexOf('~'));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalArgumentException | GitAPIException e) {
            // Silently ignore if the project directory is not a git repository
            if (!"requireGitDirOrWorkTree".equals(e.getStackTrace()[0].getMethodName())) {
                e.printStackTrace();
            }
            return null;
        }
        return branch;

    }

    private static @Nullable String localBranchName(Repository repository, @Nullable String remoteBranch) throws IOException, GitAPIException {
        if (remoteBranch == null) {
            return null;
        } else if (remoteBranch.startsWith("remotes/")) {
            // Remote branch names retrieved from git are prefixed with "remotes/"
            remoteBranch = remoteBranch.substring(8);
        }

        String branch = null;
        try (Git git = Git.open(repository.getDirectory())) {
            List<RemoteConfig> remotes = git.remoteList().call();
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

    private static @Nullable String getRemoteOriginUrl(Repository repository) {
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

    private static @Nullable AutoCRLF getAutocrlf(Repository repository) {
        WorkingTreeOptions opt = repository.getConfig().get(WorkingTreeOptions.KEY);
        switch (opt.getAutoCRLF()) {
            case FALSE:
                return AutoCRLF.False;
            case TRUE:
                return AutoCRLF.True;
            case INPUT:
                return AutoCRLF.Input;
            default:
                return null;
        }
    }

    private static @Nullable EOL getEOF(Repository repository) {
        WorkingTreeOptions opt = repository.getConfig().get(WorkingTreeOptions.KEY);
        switch (opt.getEOL()) {
            case CRLF:
                return EOL.CRLF;
            case LF:
                return EOL.LF;
            case NATIVE:
                return EOL.Native;
            default:
                return null;
        }
    }

    private static List<Committer> getCommitters(Repository repository) {
        try (Git git = Git.open(repository.getDirectory())) {
            ObjectId head = repository.readOrigHead();
            if (head == null) {
                Ref headRef = repository.getRefDatabase().findRef("HEAD");
                if (headRef == null || headRef.getObjectId() == null) {
                    return emptyList();
                }
                head = headRef.getObjectId();
            }

            Map<String, Committer> committers = new TreeMap<>();
            for (RevCommit commit : git.log().add(head).call()) {
                PersonIdent who = commit.getAuthorIdent();
                Committer committer = committers.computeIfAbsent(who.getEmailAddress(),
                        email -> new Committer(who.getName(), email, new TreeMap<>()));
                committer.getCommitsByDay().compute(who.getWhen().toInstant().atZone(who.getTimeZone().toZoneId())
                                .toLocalDate(),
                        (day, count) -> count == null ? 1 : count + 1);
            }
            return new ArrayList<>(committers.values());
        } catch (IOException | GitAPIException e) {
            return emptyList();
        }
    }

    private static @Nullable String getChangeset(Repository repository) throws IOException {
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

    public enum AutoCRLF {
        False,
        True,
        Input
    }

    public enum EOL {
        CRLF,
        LF,
        Native
    }

    @Value
    @With
    public static class Committer {
        String name;
        String email;
        NavigableMap<LocalDate, Integer> commitsByDay;
    }

    @Value
    public static class GitRemote {
        Service service;
        String url;
        String origin;
        String path;

        @Nullable
        String organization;

        String repositoryName;

        public enum Service {
            GitHub,
            GitLab,
            Bitbucket,
            BitbucketCloud,
            AzureDevOps,
            Unknown
        }

        public static class Parser {
            private final Map<String, GitRemote.Service> origins;

            public Parser() {
                origins = new LinkedHashMap<>();
                origins.put("github.com", Service.GitHub);
                origins.put("gitlab.com", Service.GitLab);
                origins.put("bitbucket.org", Service.BitbucketCloud);
                origins.put("dev.azure.com", Service.AzureDevOps);
                origins.put("ssh.dev.azure.com", Service.AzureDevOps);
            }

            public Parser registerRemote(Service service, String origin) {
                if (origin.startsWith("https://") || origin.startsWith("http://") || origin.startsWith("ssh://")) {
                    origin = new HostAndPath(origin).concat();
                }
                origins.put(origin, service);
                return this;
            }

            public @Nullable GitRemote parse(@Nullable String url) {
                if (url == null) {
                    return null;
                }
                HostAndPath hostAndPath = new HostAndPath(url);

                String origin = hostAndPath.host;
                Service service = origins.get(origin);
                if (service == null) {
                    for (String maybeOrigin : origins.keySet()) {
                        if (hostAndPath.concat().startsWith(maybeOrigin)) {
                            service = origins.get(maybeOrigin);
                            origin = maybeOrigin;
                            break;
                        }
                    }
                }

                if (service == null) {
                    // If we cannot find a service, we assume the last 2 path segments are the organization and repository name
                    service = Service.Unknown;
                    String hostPath = hostAndPath.concat();
                    origin = hostPath.substring(0, hostPath.lastIndexOf("/"))
                            .substring(0, hostPath.lastIndexOf("/"));
                }

                String repositoryPath = hostAndPath.remainder(origin)
                        .replaceFirst("/$", "")
                        .replaceFirst(".git$", "")
                        .replaceFirst("^/", "");


                switch (service) {
                    case AzureDevOps:
                        if (origin.equals("ssh.dev.azure.com")) {
                            origin = "dev.azure.com";
                            repositoryPath = repositoryPath.replaceFirst("v3/", "");
                        } else {
                            repositoryPath = repositoryPath.replaceFirst("/_git/", "/");
                        }

                    case Bitbucket:
                        if (url.startsWith("http")) {
                            repositoryPath = repositoryPath.replaceFirst("scm/", "");
                        }
                        break;
                }
                String organization = null;
                String repositoryName;
                if (repositoryPath.contains("/")) {
                    organization = repositoryPath.substring(0, repositoryPath.lastIndexOf("/"));
                    repositoryName = repositoryPath.substring(repositoryPath.lastIndexOf("/") + 1);
                } else {
                    repositoryName = repositoryPath;
                }
                return new GitRemote(service, url, origin, repositoryPath, organization, repositoryName);
            }

            private static class HostAndPath {
                String host;
                String path;

                public HostAndPath(String url) {
                    try {
                        URIish uri = new URIish(url);
                        host = uri.getHost();
                        path = uri.getPath().replaceFirst("/$", "");
                    } catch (URISyntaxException e) {
                        throw new IllegalStateException("Unable to parse origin", e);
                    }
                }

                private String concat() {
                    String hostAndPath = host;
                    if (!path.isEmpty()) {
                        hostAndPath = hostAndPath + "/" + path;
                    }
                    return hostAndPath;
                }

                private String remainder(String prefix) {
                    String hostAndPath = concat();
                    if (!hostAndPath.startsWith(prefix)) {
                        throw new IllegalArgumentException("Unable to find prefix '" + prefix + "' in '" + hostAndPath + "'");
                    }
                    return hostAndPath.substring(prefix.length());
                }
            }
        }
    }
}
