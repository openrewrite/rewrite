package org.openrewrite;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class GitRemoteTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://github.com/org/repo, github.com, org/repo, org, repo
      git@github.com:org/repo.git, github.com, org/repo, org, repo
      ssh://github.com/org/repo.git, github.com, org/repo, org, repo
                
      https://gitlab.com/group/repo.git, gitlab.com, group/repo, group, repo
      https://gitlab.com/group/subgroup/subergroup/subestgroup/repo.git, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@gitlab.com:group/subgroup/subergroup/subestgroup/repo.git, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://git@gitlab.com:22/group/subgroup/subergroup/subestgroup/repo.git, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
                
      https://bitbucket.org/PRJ/repo, bitbucket.org, PRJ/repo, PRJ, repo
      git@bitbucket.org:PRJ/repo.git, bitbucket.org, PRJ/repo, PRJ, repo
      ssh://bitbucket.org/PRJ/repo.git, bitbucket.org, PRJ/repo, PRJ, repo
                
      https://dev.azure.com/org/project/_git/repo, dev.azure.com, org/project/repo, org/project, repo
      git@ssh.dev.azure.com:v3/org/project/repo, dev.azure.com, org/project/repo, org/project, repo
      ssh://ssh.dev.azure.com:22/v3/org/project/repo, dev.azure.com, org/project/repo, org/project, repo
      """)
    void parseKnownRemotes(String cloneUrl, String expectedOrigin, String expectedPath, String expectedOrganization, String expectedRepositoryName) {
        GitRemote.Parser parser = new GitRemote.Parser();
        GitRemote remote = parser.parse(cloneUrl);
        assertThat(remote.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(remote.getPath()).isEqualTo(expectedPath);
        assertThat(remote.getOrganization()).isEqualTo(expectedOrganization);
        assertThat(remote.getRepositoryName()).isEqualTo(expectedRepositoryName);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://scm.company.com/stash/scm/org/repo.git, scm.company.com/stash/scm, org/repo, org, repo
      git@scm.company.com:stash/org/repo.git, scm.company.com/stash, org/repo, org, repo
      ssh://scm.company.com/stash/org/repo, scm.company.com/stash, org/repo, org, repo
                
      git@scm.company.com:very/long/context/path/org/repo.git, scm.company.com/very/long/context/path, org/repo, org, repo
      """)
    void parseUnknownRemote(String cloneUrl, String expectedOrigin, String expectedPath, String expectedOrganization, String expectedRepositoryName) {
        GitRemote.Parser parser = new GitRemote.Parser();
        GitRemote remote = parser.parse(cloneUrl);
        assertThat(remote.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(remote.getPath()).isEqualTo(expectedPath);
        assertThat(remote.getOrganization()).isEqualTo(expectedOrganization);
        assertThat(remote.getRepositoryName()).isEqualTo(expectedRepositoryName);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://scm.company.com/stash/scm/org/repo.git, scm.company.com/stash, Bitbucket, org/repo, org, repo
      git@scm.company.com:stash/org/repo.git, scm.company.com/stash, Bitbucket, org/repo, org, repo
      ssh://scm.company.com/stash/org/repo, scm.company.com/stash, Bitbucket, org/repo, org, repo
                
      git@scm.company.com:very/long/context/path/org/repo.git, scm.company.com/very/long/context/path, Bitbucket, org/repo, org, repo
                
      https://scm.company.com/group/subgroup/subergroup/subestgroup/repo, scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@scm.company.com:group/subgroup/subergroup/subestgroup/repo.git, scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://scm.company.com:22/group/subgroup/subergroup/subestgroup/repo.git, scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
                
      https://scm.company.com/very/long/context/path/group/subgroup/subergroup/subestgroup/repo, scm.company.com/very/long/context/path, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      """)
    void parseRegisteredRemote(String cloneUrl, String origin, GitRemote.Service service, String expectedPath, String expectedOrganization, String expectedRepositoryName) {
        GitRemote.Parser parser = new GitRemote.Parser();
        parser.registerRemote(service, origin);
        GitRemote remote = parser.parse(cloneUrl);
        assertThat(remote.getOrigin()).isEqualTo(origin);
        assertThat(remote.getPath()).isEqualTo(expectedPath);
        assertThat(remote.getOrganization()).isEqualTo(expectedOrganization);
        assertThat(remote.getRepositoryName()).isEqualTo(expectedRepositoryName);
    }
}
