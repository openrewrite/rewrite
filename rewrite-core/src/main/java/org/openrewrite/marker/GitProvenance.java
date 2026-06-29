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


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.GitRemote;
import org.openrewrite.Incubating;
import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.api.LogCommand;
import org.openrewrite.jgit.api.errors.GitAPIException;
import org.openrewrite.jgit.lib.*;
import org.openrewrite.jgit.revwalk.RevCommit;
import org.openrewrite.jgit.revwalk.filter.CommitTimeRevFilter;
import org.openrewrite.jgit.transport.RemoteConfig;
import org.openrewrite.jgit.treewalk.WorkingTreeOptions;
import org.openrewrite.marker.ci.BuildEnvironment;
import org.openrewrite.marker.ci.IncompleteGitConfigException;
import org.openrewrite.marker.ci.JenkinsBuildEnvironment;
import org.openrewrite.rpc.Reference;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyNavigableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PACKAGE) // required for @With and tests
@With
public class GitProvenance extends Reference implements Marker {
    UUID id;

    /**
     * The URL of the origin remote, not to be confused with the SCM origin
     * which can be found under gitRemote.getOrigin() which contains the base url of the remote SCM server
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
    @NonFinal
    @Nullable
    GitRemote gitRemote;

    // javadoc does not like @RequiredArgsConstructor(onConstructor_ = { @JsonCreator })
    @JsonCreator
    public GitProvenance(UUID id,
                         @Nullable String origin,
                         @Nullable String branch,
                         @Nullable String change,
                         @Nullable AutoCRLF autocrlf,
                         @Nullable EOL eol,
                         @Nullable List<Committer> committers) {
        this.id = id;
        this.origin = origin;
        this.branch = branch;
        this.change = change;
        this.autocrlf = autocrlf;
        this.eol = eol;
        this.committers = committers;
    }

    @Override
    public Object getValue() {
        return this;
    }

    public @Nullable GitRemote getGitRemote() {
        if (gitRemote == null && origin != null) {
            gitRemote = new GitRemote.Parser().parse(origin);
        }
        return gitRemote;
    }

    /**
     * Extract the organization name, including sub-organizations for git hosting services which support such a concept,
     * from the origin URL. Needs to be supplied with the
     *
     * @param baseUrl the portion of the URL which precedes the organization
     * @return the portion of the git origin URL which corresponds to the organization the git repository is organized under
     */
    public @Nullable String getOrganizationName(String baseUrl) {
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
        String remainder = origin.substring(origin.indexOf(baseUrl) + baseUrl.length())
                .replaceFirst("^:", "")
                .replaceFirst("^/", "");
        return remainder.substring(0, remainder.lastIndexOf('/'));
    }

    public @Nullable String getOrganizationName() {
        if (getGitRemote() == null) {
            return null;
        }
        return getGitRemote().getOrganization();
    }

    public @Nullable String getRepositoryName() {
        if (getGitRemote() == null) {
            return null;
        }
        return getGitRemote().getRepositoryName();
    }

    public @Nullable String getRepositoryPath() {
        if (getGitRemote() == null) {
            return null;
        }
        return getGitRemote().getPath();
    }

    public @Nullable String getRepositoryOrigin() {
        if (getGitRemote() == null) {
            return null;
        }
        return getGitRemote().getOrigin();
    }

    /**
     * @param projectDir The project directory.
     * @return A marker containing git provenance information.
     * @deprecated Use {@link #fromProjectDirectory(Path, BuildEnvironment, GitRemote.Parser) instead}.
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
        return fromProjectDirectory(projectDir, environment, null);
    }

    /**
     * @param projectDir      The project directory.
     * @param environment     In detached head scenarios, the branch is best
     *                        determined from a {@link BuildEnvironment} marker if possible.
     * @param gitRemoteParser Parses the remote url into a {@link GitRemote}. Custom remotes can be registered to it,
     *                        or a default with known git hosting services can be used.
     * @return A marker containing git provenance information.
     */
    public static @Nullable GitProvenance fromProjectDirectory(Path projectDir,
                                                               @Nullable BuildEnvironment environment,
                                                               GitRemote.@Nullable Parser gitRemoteParser) {
        return fromProjectDirectory(projectDir, environment, gitRemoteParser, CommitHistory.full());
    }

    /**
     * The cheap provenance fields (origin, branch, change, autocrlf, eol, remote) are always computed.
     * Only the optional, expensive commit-history walk that populates {@link #getCommitters()} is
     * configurable, via {@link CommitHistory}.
     *
     * @param projectDir      The project directory.
     * @param environment     In detached head scenarios, the branch is best
     *                        determined from a {@link BuildEnvironment} marker if possible.
     * @param gitRemoteParser Parses the remote url into a {@link GitRemote}. Custom remotes can be registered to it,
     *                        or a default with known git hosting services can be used.
     * @param commitHistory   Controls how much of the commit history to walk and how much per-committer
     *                        detail to retain. Use {@link CommitHistory#none()} to skip the walk entirely
     *                        (in which case {@link #getCommitters()} is {@code null}), {@link CommitHistory#full()}
     *                        for the entire history, or a bounded preset such as {@link CommitHistory#since(LocalDate)}
     *                        or {@link CommitHistory#lastCommits(int)}.
     * @return A marker containing git provenance information.
     */
    public static @Nullable GitProvenance fromProjectDirectory(Path projectDir,
                                                               @Nullable BuildEnvironment environment,
                                                               GitRemote.@Nullable Parser gitRemoteParser,
                                                               CommitHistory commitHistory) {
        if (gitRemoteParser == null) {
            gitRemoteParser = new GitRemote.Parser();
        }
        if (environment != null) {
            if (environment instanceof JenkinsBuildEnvironment) {
                JenkinsBuildEnvironment jenkinsBuildEnvironment = (JenkinsBuildEnvironment) environment;
                try {
                    RepositoryContext ctx = openRepository(projectDir.toFile());
                    if (ctx == null) {
                        return null;
                    }
                    try (Repository repository = ctx.repository) {
                        String branch = jenkinsBuildEnvironment.getLocalBranch() != null ?
                                jenkinsBuildEnvironment.getLocalBranch() :
                                ctx.branch != null ? ctx.branch :
                                        localBranchName(repository, jenkinsBuildEnvironment.getBranch());
                        String changeset = ctx.changeset != null ? ctx.changeset : getChangeset(repository);
                        return fromGitConfig(repository, branch, changeset, gitRemoteParser, commitHistory, ctx.committerStart, ctx.linkedWorktree);
                    }
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
                    return fromGitConfig(projectDir, gitRemoteParser, commitHistory);
                } else {
                    //there is not .git config
                    try {
                        return environment.buildGitProvenance();
                    } catch (IncompleteGitConfigException e) {
                        return fromGitConfig(projectDir, gitRemoteParser, commitHistory);
                    }
                }
            }
        } else {
            return fromGitConfig(projectDir, gitRemoteParser, commitHistory);
        }
    }

    private static void printRequireGitDirOrWorkTreeException(Exception e) {
        if (!"requireGitDirOrWorkTree".equals(e.getStackTrace()[0].getMethodName())) {
            e.printStackTrace();
        }
    }

    private static @Nullable GitProvenance fromGitConfig(Path projectDir, GitRemote.Parser gitRemoteParser, CommitHistory commitHistory) {
        try {
            RepositoryContext ctx = openRepository(projectDir.toFile());
            if (ctx == null) {
                return null;
            }
            try (Repository repository = ctx.repository) {
                String branch = ctx.branch;
                String changeset = ctx.changeset != null ? ctx.changeset : getChangeset(repository);
                if (branch == null && !ctx.linkedWorktree && !repository.getBranch().equals(changeset)) {
                    branch = repository.getBranch();
                }
                return fromGitConfig(repository, branch, changeset, gitRemoteParser, commitHistory, ctx.committerStart, ctx.linkedWorktree);
            }
        } catch (IllegalArgumentException e) {
            printRequireGitDirOrWorkTreeException(e);
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens the git repository for {@code projectDir}, transparently handling linked git worktrees. The
     * shaded JGit predates {@code commondir} support and so cannot open a worktree's private gitdir
     * (it would report the repository as bare with no refs or objects). For a worktree we instead open
     * the shared common repository, which owns the objects, refs, and config, and recover this
     * worktree's branch and HEAD from its own gitdir.
     */
    private static @Nullable RepositoryContext openRepository(File projectDir) throws IOException {
        File gitDir = new RepositoryBuilder().findGitDir(projectDir).getGitDir();
        File commonDir = gitDir == null ? null : commonDir(gitDir);
        if (commonDir == null) {
            // A normal repository (or a shallow clone). Build exactly as before; a missing git dir
            // throws IllegalArgumentException, which callers translate to a null provenance.
            return new RepositoryContext(new RepositoryBuilder().findGitDir(projectDir).build(), null, null, null, false);
        }
        // A linked worktree: open the shared common repository and derive HEAD from the worktree gitdir.
        Repository repository = new RepositoryBuilder().setGitDir(commonDir).build();
        try {
            String[] head = readWorktreeHead(gitDir);
            ObjectId headId = head[1] == null ? null : repository.resolve(head[1]);
            String changeset = headId == null ? null : headId.getName();
            return new RepositoryContext(repository, head[0], changeset, headId, true);
        } catch (IOException | RuntimeException e) {
            // Don't leak the open repository if reading the worktree's HEAD fails (e.g. a corrupt worktree).
            repository.close();
            throw e;
        }
    }

    /**
     * @return the shared common git directory if {@code gitDir} is a linked worktree's private gitdir
     * (i.e. it contains a {@code commondir} file), or {@code null} for a normal repository.
     */
    private static @Nullable File commonDir(File gitDir) {
        File commonDirFile = new File(gitDir, "commondir");
        if (!commonDirFile.isFile()) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(commonDirFile.toPath()), StandardCharsets.UTF_8).trim();
            File common = new File(content);
            if (!common.isAbsolute()) {
                common = new File(gitDir, content);
            }
            return common.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads a worktree's own {@code HEAD}.
     *
     * @return a two element array of {@code [branchName, refOrSha]}; {@code branchName} is {@code null}
     * for a detached HEAD, and {@code refOrSha} is the symbolic ref (e.g. {@code refs/heads/main}) or the
     * commit sha to resolve against the shared common repository.
     */
    private static String[] readWorktreeHead(File gitDir) throws IOException {
        String head = new String(Files.readAllBytes(new File(gitDir, "HEAD").toPath()), StandardCharsets.UTF_8).trim();
        if (head.startsWith("ref:")) {
            String ref = head.substring(4).trim();
            String branch = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : null;
            return new String[]{branch, ref};
        }
        return new String[]{null, head.isEmpty() ? null : head};
    }

    /**
     * The opened repository together with the branch, changeset, and committer-walk start that a linked
     * worktree's own HEAD implies. For a normal repository the latter three are {@code null}, so callers
     * fall back to reading them from the repository's HEAD as before.
     */
    private static class RepositoryContext {
        final Repository repository;

        @Nullable
        final String branch;

        @Nullable
        final String changeset;

        @Nullable
        final ObjectId committerStart;

        /** Whether {@link #repository} is the shared common repository of a linked worktree, in which
         * case its own HEAD belongs to a different checkout and must not be consulted for branch/changeset. */
        final boolean linkedWorktree;

        RepositoryContext(Repository repository, @Nullable String branch, @Nullable String changeset,
                          @Nullable ObjectId committerStart, boolean linkedWorktree) {
            this.repository = repository;
            this.branch = branch;
            this.changeset = changeset;
            this.committerStart = committerStart;
            this.linkedWorktree = linkedWorktree;
        }
    }

    private static @Nullable GitProvenance fromGitConfig(Repository repository, @Nullable String branch, @Nullable String changeset, GitRemote.Parser gitRemoteParser, CommitHistory commitHistory, @Nullable ObjectId committerStart, boolean linkedWorktree) {
        if (repository.isBare()) {
            return null;
        }
        if (branch == null && !linkedWorktree) {
            // A linked worktree's branch is authoritative from its own HEAD (null means genuinely
            // detached); resolving from the shared repository's HEAD would report a different checkout's branch.
            branch = resolveBranchFromGitConfig(repository);
        }
        String remoteOriginUrl = getRemoteOriginUrl(repository);
        GitRemote remote;
        if (remoteOriginUrl == null) {
            remote = null;
        } else {
            remote = gitRemoteParser.parse(remoteOriginUrl);
        }
        // null (not emptyList) when the walk is skipped, so consumers can distinguish
        // "committers were not computed" from "walked the history and found nobody". For a linked
        // worktree whose own HEAD couldn't be resolved, leave committers null rather than walking from
        // the shared repository's HEAD, which belongs to a different checkout.
        List<Committer> committers = commitHistory.isEnabled() && !(linkedWorktree && committerStart == null) ?
                getCommitters(repository, committerStart, commitHistory) : null;
        return new GitProvenance(randomId(), remoteOriginUrl, branch, changeset,
                getAutocrlf(repository), getEOF(repository), committers, remote);
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

    private static List<Committer> getCommitters(Repository repository, @Nullable ObjectId from, CommitHistory commitHistory) {
        try (Git git = Git.open(repository.getDirectory())) {
            // A linked worktree supplies its own HEAD (the repository is the shared common one, whose
            // HEAD/ORIG_HEAD belong to a different checkout). Otherwise prefer ORIG_HEAD, then HEAD.
            ObjectId head = from;
            if (head == null) {
                head = repository.readOrigHead();
            }
            if (head == null) {
                Ref headRef = repository.getRefDatabase().findRef("HEAD");
                if (headRef == null || headRef.getObjectId() == null) {
                    return emptyList();
                }
                head = headRef.getObjectId();
            }

            LogCommand log = git.log().add(head);
            CommitHistory.Scope scope = commitHistory.getScope();
            switch (scope.getKind()) {
                case SINCE:
                    // CommitTimeRevFilter.after throws StopWalkException on the first commit older than the
                    // cutoff, which prunes the walk (real CPU savings). Because git timestamps are
                    // non-monotonic, this is an approximation, exactly like `git log --since`.
                    log.setRevFilter(CommitTimeRevFilter.after(
                            Date.from(requireNonNull(scope.getSince()).atStartOfDay(ZoneId.systemDefault()).toInstant())));
                    break;
                case LAST_COMMITS:
                    log.setMaxCount(scope.getMaxCommits());
                    break;
                case NONE:
                case FULL:
                default:
                    break;
            }

            boolean perDay = commitHistory.getDetail() == CommitHistory.Detail.COMMITS_BY_DAY;
            Map<String, String> committerName = new HashMap<>();
            Map<String, NavigableMap<LocalDate, Integer>> commitMap = new HashMap<>();
            for (RevCommit commit : log.call()) {
                PersonIdent who = commit.getAuthorIdent();
                committerName.putIfAbsent(who.getEmailAddress(), who.getName());
                if (perDay) {
                    commitMap.computeIfAbsent(who.getEmailAddress(),
                            email -> new TreeMap<>()).compute(who.getWhen().toInstant().atZone(who.getTimeZone().toZoneId())
                                    .toLocalDate(),
                            (day, count) -> count == null ? 1 : count + 1);
                }
            }
            return committerName.entrySet().stream()
                    .map(c -> {
                        NavigableMap<LocalDate, Integer> byDay = perDay ? commitMap.get(c.getKey()) : emptyNavigableMap();
                        return new Committer(c.getValue(), c.getKey(), byDay);
                    }).collect(toList());
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

    /**
     * Controls the optional, expensive commit-history walk that {@link GitProvenance} performs to
     * populate {@link GitProvenance#getCommitters()}. The cheap provenance fields (origin, branch,
     * change, autocrlf, eol, remote) are always computed and are not affected by this type.
     * <p>
     * Two axes are bounded here, but only through named presets, so the combination space stays small
     * and every reachable state is meaningful:
     * <ul>
     *   <li><b>Scope</b> &mdash; how far back the walk goes: {@link #none()} (don't walk),
     *       {@link #full()} (entire history), {@link #since(LocalDate)} / {@link #sinceDaysAgo(int)}
     *       (pruned at a date), or {@link #lastCommits(int)} (a hard commit cap).</li>
     *   <li><b>Detail</b> &mdash; how much per-committer information is retained, and only applies when we
     *       walk: {@link Detail#COMMITTERS} (identities only) or {@link Detail#COMMITS_BY_DAY} (the full
     *       per-day breakdown). Override it with {@link #withDetail(Detail)}.</li>
     * </ul>
     * The off-switch lives on the scope axis, so {@code since(date).withDetail(...)} can never collapse
     * to "walk nothing." This is a factory <em>input</em> only; it is never stored on the marker and
     * never serialized.
     */
    @Value
    @Incubating(since = "8.85.0")
    public static class CommitHistory {
        Scope scope;
        Detail detail;

        private CommitHistory(Scope scope, Detail detail) {
            this.scope = scope;
            this.detail = detail;
        }

        /** Skip the walk entirely. {@link GitProvenance#getCommitters()} will be {@code null}. */
        public static CommitHistory none() {
            return new CommitHistory(Scope.none(), Detail.COMMITS_BY_DAY);
        }

        /** Walk the entire reachable history, retaining the full per-day commit breakdown. */
        public static CommitHistory full() {
            return new CommitHistory(Scope.full(), Detail.COMMITS_BY_DAY);
        }

        /**
         * Walk commits committed on or after {@code since}, retaining the full per-day breakdown. The walk
         * is pruned at the cutoff by commit time (like {@code git log --since}); because git timestamps are
         * non-monotonic this is an approximation. Note the per-day breakdown is keyed by author date, which
         * for rebased or cherry-picked commits can differ from the commit date the cutoff is applied to.
         */
        public static CommitHistory since(LocalDate since) {
            return new CommitHistory(Scope.since(since), Detail.COMMITS_BY_DAY);
        }

        /** Walk commits committed within the last {@code days} days, retaining the full per-day breakdown. */
        public static CommitHistory sinceDaysAgo(int days) {
            return since(LocalDate.now().minusDays(days));
        }

        /** Walk at most {@code maxCommits} commits (an exact cap), retaining the full per-day breakdown. */
        public static CommitHistory lastCommits(int maxCommits) {
            if (maxCommits < 0) {
                throw new IllegalArgumentException("maxCommits must be non-negative, but was " + maxCommits);
            }
            return new CommitHistory(Scope.lastCommits(maxCommits), Detail.COMMITS_BY_DAY);
        }

        /**
         * Walk the entire reachable history but retain only the distinct committer identities (name and
         * email), discarding the per-day breakdown. Cheaper to hold and serialize; the resulting
         * {@link Committer}s have an empty {@link Committer#getCommitsByDay()}.
         */
        public static CommitHistory identities() {
            return new CommitHistory(Scope.full(), Detail.COMMITTERS);
        }

        /**
         * Override the detail axis on any scope, e.g. {@code CommitHistory.since(date).withDetail(COMMITTERS)}.
         * Has no observable effect when the scope is {@link #none()} (the walk is skipped regardless).
         */
        public CommitHistory withDetail(Detail detail) {
            return new CommitHistory(scope, detail);
        }

        boolean isEnabled() {
            return scope.getKind() != Scope.Kind.NONE;
        }

        /** How far back the commit walk extends. Constructed only through {@link CommitHistory}'s presets. */
        @Value
        static class Scope {
            enum Kind {
                NONE, FULL, SINCE, LAST_COMMITS
            }

            Kind kind;

            @Nullable
            LocalDate since;

            int maxCommits;

            static Scope none() {
                return new Scope(Kind.NONE, null, -1);
            }

            static Scope full() {
                return new Scope(Kind.FULL, null, -1);
            }

            static Scope since(LocalDate since) {
                return new Scope(Kind.SINCE, since, -1);
            }

            static Scope lastCommits(int maxCommits) {
                return new Scope(Kind.LAST_COMMITS, null, maxCommits);
            }
        }

        /** How much per-committer detail to retain. Applies only when the history is walked. */
        public enum Detail {
            /** Distinct committer identities only (name + email); {@code commitsByDay} is empty. */
            COMMITTERS,
            /** The full per-day commit breakdown (the historical default). */
            COMMITS_BY_DAY
        }
    }

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Committer {

        @With
        String name;

        @With
        String email;

        @Getter(AccessLevel.PRIVATE)
        int[] data;

        @JsonCreator
        static Committer from(
                @JsonProperty("name") String name, @JsonProperty("email") String email,
                @JsonProperty("dates") int @Nullable [] data,
                @Nullable @JsonProperty("commitsByDay") NavigableMap<LocalDate, Integer> commitsByDay) {
            if (commitsByDay != null) {
                return new Committer(name, email, commitsByDay);
            } else {
                return new Committer(name, email, requireNonNull(data));
            }
        }

        public Committer(String name, String email, NavigableMap<LocalDate, Integer> commitsByDay) {
            this.name = name;
            this.email = email;
            this.data = new int[commitsByDay.size() * 2];
            int i = 0;
            for (Map.Entry<LocalDate, Integer> entry : commitsByDay.entrySet()) {
                data[i++] = (int) entry.getKey().toEpochDay();
                data[i++] = entry.getValue();
            }
        }

        public NavigableMap<LocalDate, Integer> getCommitsByDay() {
            TreeMap<LocalDate, Integer> commitsByDay = new TreeMap<>();
            for (int i = 0; i < data.length; ) {
                commitsByDay.put(LocalDate.ofEpochDay(data[i++]), data[i++]);
            }
            return commitsByDay;
        }
    }
}
