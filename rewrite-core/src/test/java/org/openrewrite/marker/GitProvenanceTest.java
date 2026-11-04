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
package org.openrewrite.marker;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.GitRemote;
import org.openrewrite.marker.GitProvenance.CommitHistory;
import org.openrewrite.jgit.api.Git;
import org.openrewrite.jgit.api.errors.GitAPIException;
import org.openrewrite.jgit.lib.Constants;
import org.openrewrite.jgit.lib.RepositoryCache;
import org.openrewrite.jgit.transport.TagOpt;
import org.openrewrite.jgit.transport.URIish;
import org.openrewrite.jgit.util.FS;
import org.openrewrite.marker.ci.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.fasterxml.jackson.core.JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.jgit.lib.ConfigConstants.CONFIG_BRANCH_SECTION;

@SuppressWarnings({"ConstantConditions", "HttpUrlsUsage"})
class GitProvenanceTest {

    private static Stream<Arguments> remotes() {
        return Stream.of(
          Arguments.of("ssh://git@github.com/openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("https://github.com/openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("file:///openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("http://localhost:7990/scm/openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("http://localhost:7990/scm/some/openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("git@github.com:openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("org-12345678@github.com:openrewrite/rewrite.git", "openrewrite", "rewrite"),
          Arguments.of("https://dev.azure.com/openrewrite/rewrite/_git/rewrite", "openrewrite/rewrite", "rewrite"),
          Arguments.of("https://openrewrite@dev.azure.com/openrewrite/rewrite/_git/rewrite",
            "openrewrite/rewrite",
            "rewrite"),
          Arguments.of("git@ssh.dev.azure.com:v3/openrewrite/rewrite/rewrite", "openrewrite/rewrite", "rewrite"),
          Arguments.of("ssh://git@ssh.dev.azure.com/v3/openrewrite/rewrite/rewrite", "openrewrite/rewrite", "rewrite")
        );
    }

    @ParameterizedTest
    @MethodSource("remotes")
    void getRepositoryPath(String origin, String expectedOrg, String expectedRepo) {
        GitProvenance gitProvenance = new GitProvenance(randomId(), origin, "main", "123", null, null, List.of());
        assertThat(gitProvenance.getOrganizationName()).isEqualTo(expectedOrg);
        assertThat(gitProvenance.getRepositoryName()).isEqualTo(expectedRepo);
        String expectedPath = expectedOrg + '/' + expectedRepo;
        assertThat(gitProvenance.getRepositoryPath()).isEqualTo(expectedPath);
    }

    @ParameterizedTest
    @MethodSource("remotes")
    void getRepositoryOrigin(String origin, String expectedOrg, String expectedRepo) {
        GitProvenance gitProvenance = new GitProvenance(randomId(), origin, "main", "123", null, null, List.of());
        assertThat(gitProvenance.getOrganizationName()).isEqualTo(expectedOrg);
        assertThat(gitProvenance.getRepositoryName()).isEqualTo(expectedRepo);
        assertThat(gitProvenance.getOrigin()).isEqualTo(origin);
    }

    @ParameterizedTest
    @CsvSource({
      "https://github.com/organization/repository, https://github.com, GitHub, organization",
      "git@gitlab.acme.com/organization/subgroup/repository.git, https://gitlab.acme.com, GitLab, organization/subgroup",
      "git@gitlab.acme.com/organization/subgroup/repository.git, git@gitlab.acme.com, GitLab, organization/subgroup",
      "git@gitlab.acme.com:organization/subgroup/repository.git, ssh://git@gitlab.acme.com, GitLab, organization/subgroup",
      "https://dev.azure.com/organization/project/_git/repository, https://dev.azure.com, AzureDevOps, organization/project",
      "https://organization@dev.azure.com/organization/project/_git/repository, https://dev.azure.com, AzureDevOps, organization/project",
      "git@ssh.dev.azure.com:v3/organization/project/repository, git@ssh.dev.azure.com, AzureDevOps, organization/project"
    })
    void getOrganizationName(String gitOrigin, String baseUrl, GitRemote.Service service, String organizationName) {
        GitRemote.Parser parser = new GitRemote.Parser();
        parser.registerRemote(service, baseUrl);
        assertThat(new GitProvenance(randomId(), gitOrigin, "main", "123", null, null, emptyList(), parser.parse(gitOrigin)).getOrganizationName())
          .isEqualTo(organizationName);
    }

    @ParameterizedTest
    @MethodSource("remotes")
    void getRepositoryName(String remote) {
        assertThat(new GitProvenance(randomId(), remote, "main", "123", null, null, emptyList(), null).getRepositoryName())
          .isEqualTo("rewrite");
    }

    @Test
    void localBranchPresent(@TempDir Path projectDir) throws Exception {
        try (Git ignored = Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call()) {
            GitProvenance git = GitProvenance.fromProjectDirectory(projectDir, null);
            assertThat(git).isNotNull();
            assertThat(git.getBranch()).isEqualTo("main");
        }
    }

    @Test
    void nonGitNoStacktrace(@TempDir Path projectDir) {
        PrintStream standardErr = System.err;
        ByteArrayOutputStream captor = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(captor));
            assertThat(GitProvenance.fromProjectDirectory(projectDir, null)).isNull();
            assertThat(captor.toString()).doesNotContain("jgit");
        } finally {
            System.setErr(standardErr);
        }
    }

    @Test
    void noneSkipsWalkButMatchesCheapFields(@TempDir Path projectDir) throws Exception {
        try (Git ignored = initGitWithOneCommit(projectDir)) {
            GitProvenance withCommitters = GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.full());
            GitProvenance withoutCommitters = GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.none());

            assertThat(withCommitters).isNotNull();
            assertThat(withoutCommitters).isNotNull();

            // origin/branch/change are identical whether or not committers are walked
            assertThat(withoutCommitters.getOrigin()).isEqualTo(withCommitters.getOrigin());
            assertThat(withoutCommitters.getBranch()).isEqualTo(withCommitters.getBranch());
            assertThat(withoutCommitters.getChange()).isEqualTo(withCommitters.getChange());

            // the full path actually walks history and finds the commit author
            assertThat(withCommitters.getCommitters()).isNotEmpty();
            // none() skips the walk; committers is null ("not computed"), not an empty list
            assertThat(withoutCommitters.getCommitters()).isNull();
        }
    }

    @Test
    void commitHistoryBoundsScopeAndDetail(@TempDir Path projectDir) throws Exception {
        try (Git ignored = initGitWithOneCommit(projectDir)) {
            // lastCommits keeps the walk bounded but still yields the committer with per-day detail
            GitProvenance lastCommit = GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.lastCommits(1));
            assertThat(lastCommit.getCommitters()).isNotEmpty();
            assertThat(lastCommit.getCommitters().get(0).getCommitsByDay()).isNotEmpty();

            // identities() walks but retains only name/email, with no per-day breakdown
            GitProvenance identities = GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.identities());
            assertThat(identities.getCommitters()).isNotEmpty();
            assertThat(identities.getCommitters().get(0).getCommitsByDay()).isEmpty();

            // since() prunes the walk at the cutoff: a future cutoff yields a walked-but-empty list (not null)
            GitProvenance sinceFuture = GitProvenance.fromProjectDirectory(projectDir, null, null,
              CommitHistory.since(LocalDate.now().plusDays(1)));
            assertThat(sinceFuture.getCommitters()).isEmpty();

            // a past cutoff still includes the commit
            GitProvenance sincePast = GitProvenance.fromProjectDirectory(projectDir, null, null,
              CommitHistory.since(LocalDate.now().minusDays(1)));
            assertThat(sincePast.getCommitters()).isNotEmpty();
        }
    }

    @Test
    void linkedWorktree(@TempDir Path projectDir) throws Exception {
        // The shaded JGit cannot open a linked worktree's private gitdir, so GitProvenance opens the
        // shared common repository and recovers the worktree's branch/HEAD from its own gitdir.
        Path mainDir = projectDir.resolve("main");
        Files.createDirectories(mainDir);
        String commit;
        try (Git git = Git.init().setDirectory(mainDir.toFile()).setInitialBranch("main").call()) {
            Files.writeString(mainDir.resolve("test.txt"), "hi");
            git.add().addFilepattern("*").call();
            commit = git.commit().setMessage("init").setSign(false).call().getName();
            git.branchCreate().setName("feature").call();
            git.remoteAdd().setName("origin").setUri(new URIish("git@github.com:openrewrite/doesnotexist.git")).call();
        }

        // Hand-build the linked-worktree layout that `git worktree add` would create.
        Path wtPrivate = mainDir.resolve(".git").resolve("worktrees").resolve("wt");
        Files.createDirectories(wtPrivate);
        Path worktreeDir = projectDir.resolve("wt");
        Files.createDirectories(worktreeDir);
        Files.writeString(worktreeDir.resolve(".git"), "gitdir: " + wtPrivate.toAbsolutePath() + "\n");
        Files.writeString(wtPrivate.resolve("HEAD"), "ref: refs/heads/feature\n");
        Files.writeString(wtPrivate.resolve("commondir"), "../..\n");
        Files.writeString(wtPrivate.resolve("gitdir"), worktreeDir.resolve(".git").toAbsolutePath() + "\n");

        GitProvenance withCommitters = GitProvenance.fromProjectDirectory(worktreeDir, null, null, CommitHistory.full());
        assertThat(withCommitters).isNotNull();
        // the worktree's own branch and HEAD, not the main checkout's
        assertThat(withCommitters.getBranch()).isEqualTo("feature");
        assertThat(withCommitters.getChange()).isEqualTo(commit);
        // origin comes from the shared config, and the walk reaches the shared objects
        assertThat(withCommitters.getOrigin()).isEqualTo("git@github.com:openrewrite/doesnotexist.git");
        assertThat(withCommitters.getCommitters()).isNotEmpty();

        // none() still resolves the cheap fields on a worktree, skipping only the walk
        GitProvenance none = GitProvenance.fromProjectDirectory(worktreeDir, null, null, CommitHistory.none());
        assertThat(none.getBranch()).isEqualTo("feature");
        assertThat(none.getChange()).isEqualTo(commit);
        assertThat(none.getCommitters()).isNull();
    }

    @Test
    void detachedHeadLinkedWorktree(@TempDir Path projectDir) throws Exception {
        Path mainDir = projectDir.resolve("main");
        Files.createDirectories(mainDir);
        String commit;
        try (Git git = Git.init().setDirectory(mainDir.toFile()).setInitialBranch("main").call()) {
            Files.writeString(mainDir.resolve("test.txt"), "hi");
            git.add().addFilepattern("*").call();
            commit = git.commit().setMessage("init").setSign(false).call().getName();
        }

        Path wtPrivate = mainDir.resolve(".git").resolve("worktrees").resolve("wt");
        Files.createDirectories(wtPrivate);
        Path worktreeDir = projectDir.resolve("wt");
        Files.createDirectories(worktreeDir);
        Files.writeString(worktreeDir.resolve(".git"), "gitdir: " + wtPrivate.toAbsolutePath() + "\n");
        Files.writeString(wtPrivate.resolve("HEAD"), commit + "\n"); // detached: a raw sha, not a ref
        Files.writeString(wtPrivate.resolve("commondir"), "../..\n");
        Files.writeString(wtPrivate.resolve("gitdir"), worktreeDir.resolve(".git").toAbsolutePath() + "\n");

        GitProvenance gp = GitProvenance.fromProjectDirectory(worktreeDir, null, null, CommitHistory.full());
        assertThat(gp).isNotNull();
        // detached HEAD has no branch, and crucially must NOT report the common checkout's "main"
        assertThat(gp.getBranch()).isNull();
        assertThat(gp.getChange()).isEqualTo(commit);
        assertThat(gp.getCommitters()).isNotEmpty();
    }

    @Test
    void detachedHead(@TempDir Path projectDir) throws Exception {
        try (Git git = initGitWithOneCommit(projectDir)) {
            git.checkout().setName(git.getRepository().resolve(Constants.HEAD).getName()).call();
            assertThat(GitProvenance.fromProjectDirectory(projectDir, null).getBranch())
              .isEqualTo("main");
        }
    }

    @Test
    void detachedHeadJenkinsLocalBranch(@TempDir Path projectDir) throws Exception {
        try (Git git = initGitWithOneCommit(projectDir)) {
            git.checkout().setName(git.getRepository().resolve(Constants.HEAD).getName()).call();
            assertThat(
              GitProvenance.fromProjectDirectory(
                projectDir,
                new JenkinsBuildEnvironment(
                  randomId(), "1", "1", "https://jenkins/job/1",
                  "https://jenkins", "job", "main", "origin/main"
                )
              ).getBranch()
            ).isEqualTo("main");
        }
    }

    @Test
    void detachedHeadJenkinsNoLocalBranch(@TempDir Path projectDir) throws Exception {
        try (Git git = initGitWithOneCommit(projectDir)) {
            git.checkout().setName(git.getRepository().resolve(Constants.HEAD).getName()).call();
            assertThat(
              GitProvenance.fromProjectDirectory(
                projectDir,
                new JenkinsBuildEnvironment(randomId(), "1", "1", "https://jenkins/job/1",
                  "https://jenkins", "job", null, "origin/main")
              ).getBranch()
            ).isEqualTo("main");
        }
    }

    private Git initGitWithOneCommit(Path projectDir) throws GitAPIException, IOException, URISyntaxException {
        var git = Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call();
        Files.writeString(projectDir.resolve("test.txt"), "hi");
        git.add().addFilepattern("*").call();
        git.commit().setMessage("init").setSign(false).call();
        git.remoteAdd().setName("origin").setUri(new URIish("git@github.com:openrewrite/doesnotexist.git")).call();

        // origins are still present in .git/config on Jenkins
        var config = git.getRepository().getConfig();
        config.setString(CONFIG_BRANCH_SECTION, "main", "remote", "origin");
        config.setString(CONFIG_BRANCH_SECTION, "main", "merge", "refs/heads/main");
        config.save();

        return git;
    }

    @Test
    void detachedHeadBehindBranchHead(@TempDir Path projectDir) throws Exception {
        try (Git git = Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call()) {
            Files.writeString(projectDir.resolve("test.txt"), "hi");
            git.add().addFilepattern("*").call();
            git.commit().setMessage("init").setSign(false).call();
            var commit1 = git.getRepository().resolve(Constants.HEAD).getName();

            Files.writeString(projectDir.resolve("test.txt"), "hi");
            git.add().addFilepattern("*").call();
            git.commit().setMessage("init").setSign(false).call();

            assertThat(git.getRepository().resolve(Constants.HEAD).getName()).isNotEqualTo(commit1);

            git.checkout().setName(commit1).call();

            assertThat(GitProvenance.fromProjectDirectory(projectDir, null).getBranch())
              .isEqualTo("main");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<String> baseUrls() {
        return Stream.of(
          "ssh://git@gitlab.com",
          "http://gitlab.com/",
          "gitlab.com"
        );
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @MethodSource("baseUrls")
    void multiplePathSegments(String baseUrl) {
        GitProvenance provenance = new GitProvenance(randomId(),
          "http://gitlab.com/group/subgroup1/subgroup2/repo.git",
          "master",
          "1234567890abcdef1234567890abcdef12345678",
          null,
          null,
          emptyList(),
          null);

        assertThat(provenance.getOrganizationName(baseUrl)).isEqualTo("group/subgroup1/subgroup2");
        assertThat(provenance.getRepositoryName()).isEqualTo("repo");
    }

    @Disabled("Does not work the same way in CI")
    @Test
    void shallowCloneDetachedHead(@TempDir Path projectDir) throws Exception {
        var remoteDir = projectDir.resolve("remote");
        var fileKey = RepositoryCache.FileKey.exact(remoteDir.toFile(), FS.DETECTED);
        // push an initial commit to the remote
        var cloneDir = projectDir.resolve("clone1");
        try (var remoteRepo = fileKey.open(false)) {
            remoteRepo.create(true);
            try (Git git = Git.cloneRepository().setURI(remoteRepo.getDirectory().getAbsolutePath()).setDirectory(cloneDir.toFile()).call()) {
                Files.writeString(projectDir.resolve("test.txt"), "hi");
                git.add().addFilepattern("*").call();
                git.commit().setMessage("init").setSign(false).call();

                runCommand(cloneDir, "git push -u origin main");
                var commit = git.getRepository().resolve(Constants.HEAD).getName();

                // shallow clone the remote to another directory
                runCommand(projectDir, "git clone file:///%s shallowClone --depth 1 --branch main"
                  .formatted(remoteRepo.getDirectory().getAbsolutePath()));
                try (var git2 = Git.open(projectDir.resolve("shallowClone").toFile())) {
                    // creates detached head
                    git2.checkout().setName(commit).call();

                    assumeTrue(GitProvenance.fromProjectDirectory(projectDir.resolve("shallowClone"), null) != null);
                    assertThat(GitProvenance.fromProjectDirectory(projectDir.resolve("shallowClone"), null).getBranch())
                      .isEqualTo("main");
                }
            }
        }
    }

    @Disabled("Does not work the same way in CI")
    @Test
    void noLocalBranchDeriveFromRemote(@TempDir Path projectDir) throws Exception {
        var remoteDir = projectDir.resolve("remote");
        var fileKey = RepositoryCache.FileKey.exact(remoteDir.toFile(), FS.DETECTED);
        try (var remoteRepo = fileKey.open(false)) {
            remoteRepo.create(true);

            // push an initial commit to the remote
            var cloneDir = projectDir.resolve("clone1");
            try (Git gitSetup = Git.cloneRepository().setURI(remoteRepo.getDirectory().getAbsolutePath()).setDirectory(cloneDir.toFile()).call()) {
                Files.writeString(projectDir.resolve("test.txt"), "hi");
                gitSetup.add().addFilepattern("*").call();
                var commit = gitSetup.commit().setMessage("init").setSign(false).call();
                gitSetup.push().add("master").setRemote("origin").call();

                //Now create new workspace directory, git init and then fetch from remote.
                var workspaceDir = projectDir.resolve("workspace");
                try (Git git = Git.init().setDirectory(workspaceDir.toFile()).call()) {
                    git.remoteAdd().setName("origin").setUri(new URIish(remoteRepo.getDirectory().getAbsolutePath())).call();
                    git.fetch().setRemote("origin").setForceUpdate(true).setTagOpt(TagOpt.FETCH_TAGS).setRefSpecs("+refs/heads/*:refs/remotes/origin/*").call();
                    git.checkout().setName(commit.getName()).call();
                }
            }
        }

        assumeTrue(GitProvenance.fromProjectDirectory(projectDir.resolve("workspace"), null) != null);
        assertThat(GitProvenance.fromProjectDirectory(projectDir.resolve("workspace"), null).getBranch())
          .isEqualTo("main");
    }

    @Test
    void supportsGitHubActions(@TempDir Path projectDir) {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("GITHUB_API_URL", "https://api.github.com");
        envVars.put("GITHUB_REPOSITORY", "octocat/Hello-World");
        envVars.put("GITHUB_REF", "refs/heads/main");
        envVars.put("GITHUB_SHA", "287364287357");
        envVars.put("GITHUB_HEAD_REF", "");

        GitProvenance prov = GitProvenance.fromProjectDirectory(projectDir,
          GithubActionsBuildEnvironment.build(envVars::get));
        assertThat(prov).isNotNull();
        assertThat(prov.getOrigin()).isEqualTo("https://github.com/octocat/Hello-World.git");
        assertThat(prov.getBranch()).isEqualTo("main");
        assertThat(prov.getChange()).isEqualTo("287364287357");
    }

    @Test
    void ignoresBuildEnvironmentIfThereIsGitConfig(@TempDir Path projectDir) throws Exception {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("GITHUB_API_URL", "https://api.github.com");
        envVars.put("GITHUB_REPOSITORY", "octocat/Hello-World");
        envVars.put("GITHUB_REF", "refs/heads/foo");
        envVars.put("GITHUB_SHA", "287364287357");
        envVars.put("GITHUB_HEAD_REF", "");
        try (Git ignored = Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call()) {
            GitProvenance prov = GitProvenance.fromProjectDirectory(projectDir,
              GithubActionsBuildEnvironment.build(envVars::get));
            assertThat(prov).isNotNull();
            assertThat(prov.getOrigin()).isNotEqualTo("https://github.com/octocat/Hello-World.git");
            assertThat(prov.getBranch()).isEqualTo("main");
            assertThat(prov.getChange()).isNotEqualTo("287364287357");
        }
    }

    @Test
    void supportsCustomBuildEnvironment(@TempDir Path projectDir) {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("CUSTOM_GIT_CLONE_URL", "https://github.com/octocat/Hello-World.git");
        envVars.put("CUSTOM_GIT_REF", "main");
        envVars.put("CUSTOM_GIT_SHA", "287364287357");

        GitProvenance prov = GitProvenance.fromProjectDirectory(projectDir,
          CustomBuildEnvironment.build(envVars::get));

        assertThat(prov).isNotNull();
        assertThat(prov.getOrigin()).isEqualTo("https://github.com/octocat/Hello-World.git");
        assertThat(prov.getBranch()).isEqualTo("main");
        assertThat(prov.getChange()).isEqualTo("287364287357");
    }

    @Test
    void supportsGitLab(@TempDir Path projectDir) {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("CI_REPOSITORY_URL", "https://github.com/octocat/Hello-World.git");
        envVars.put("CI_COMMIT_REF_NAME", "main");
        envVars.put("CI_COMMIT_SHA", "287364287357");

        GitProvenance prov = GitProvenance.fromProjectDirectory(projectDir,
          GitlabBuildEnvironment.build(envVars::get));

        assertThat(prov).isNotNull();
        assertThat(prov.getOrigin()).isEqualTo("https://github.com/octocat/Hello-World.git");
        assertThat(prov.getBranch()).isEqualTo("main");
        assertThat(prov.getChange()).isEqualTo("287364287357");
    }

    @Test
    void supportsDrone(@TempDir Path projectDir) {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("DRONE_BRANCH", "main");
        envVars.put("DRONE_TAG", "");
        envVars.put("DRONE_REMOTE_URL", "https://github.com/octocat/Hello-World.git");
        envVars.put("DRONE_COMMIT_SHA", "287364287357");

        GitProvenance prov = GitProvenance.fromProjectDirectory(projectDir,
          DroneBuildEnvironment.build(envVars::get));

        assertThat(prov).isNotNull();
        assertThat(prov.getOrigin()).isEqualTo("https://github.com/octocat/Hello-World.git");
        assertThat(prov.getBranch()).isEqualTo("main");
        assertThat(prov.getChange()).isEqualTo("287364287357");
    }

    @Test
    void supportsTravis(@TempDir Path projectDir) throws Exception {
        try (Git ignored = Git.init().setDirectory(projectDir.toFile()).setInitialBranch("main").call()) {
            TravisBuildEnvironment buildEnvironment = TravisBuildEnvironment.build(s -> null);
            GitProvenance git = GitProvenance.fromProjectDirectory(projectDir, buildEnvironment);
            assertThat(git).isNotNull();
            assertThat(git.getBranch()).isEqualTo("main");
        }
    }

    @Test
    void serialization() throws Exception {
        GitProvenance gitProvenance = new GitProvenance(randomId(), "https://github.com/octocat/Hello-World.git", "main", "123", null, null, List.of());
        ObjectMapper mapper = new ObjectMapper()
          .findAndRegisterModules()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(INCLUDE_SOURCE_IN_LOCATION);
        String json = mapper.writeValueAsString(gitProvenance);
        GitProvenance read = mapper.readValue(json, GitProvenance.class);
        assertThat(read).isEqualTo(gitProvenance);
    }

    void runCommand(Path workingDir, String command) {
        //noinspection ResultOfMethodCallIgnored
        workingDir.toFile().mkdirs();
        try {
            new ProcessBuilder(command.split(" "))
              .directory(workingDir.toFile())
              .redirectOutput(ProcessBuilder.Redirect.INHERIT)
              .redirectError(ProcessBuilder.Redirect.INHERIT)
              .start()
              .waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
