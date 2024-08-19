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
package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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
      
      https://org@dev.azure.com/org/project/_git/repo, dev.azure.com, org/project/repo, org/project, repo
      https://dev.azure.com/org/project/_git/repo, dev.azure.com, org/project/repo, org/project, repo
      git@ssh.dev.azure.com:v3/org/project/repo, dev.azure.com, org/project/repo, org/project, repo
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
      https://scm.company.com:1234/stash/scm/org/repo.git, scm.company.com:1234/stash/scm, org/repo, org, repo
      git@scm.company.com:stash/org/repo.git, scm.company.com/stash, org/repo, org, repo
      ssh://scm.company.com/stash/org/repo, scm.company.com/stash, org/repo, org, repo
      
      https://scm.company.com:1234/very/long/context/path/org/repo.git, scm.company.com:1234/very/long/context/path, org/repo, org, repo
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
      http://scm.company.com:80/stash/scm/org/repo.git, http://scm.company.com/stash, Bitbucket, org/repo, org, repo
      http://scm.company.com:8080/stash/scm/org/repo.git, http://scm.company.com:8080/stash, Bitbucket, org/repo, org, repo
      https://scm.company.com:443/stash/scm/org/repo.git, scm.company.com/stash, Bitbucket, org/repo, org, repo
      https://scm.company.com:1234/stash/scm/org/repo.git, https://scm.company.com:1234/stash, Bitbucket, org/repo, org, repo
      git@scm.company.com:stash/org/repo.git, scm.company.com/stash, Bitbucket, org/repo, org, repo
      ssh://scm.company.com/stash/org/repo, scm.company.com/stash, Bitbucket, org/repo, org, repo
      ssh://scm.company.com:22/stash/org/repo, scm.company.com/stash, Bitbucket, org/repo, org, repo
      ssh://scm.company.com:7999/stash/org/repo, ssh://scm.company.com:7999/stash, Bitbucket, org/repo, org, repo
      
      https://scm.company.com/very/long/context/path/org/repo.git, scm.company.com/very/long/context/path, Bitbucket, org/repo, org, repo
      https://scm.company.com:1234/very/long/context/path/org/repo.git, https://scm.company.com:1234/very/long/context/path, Bitbucket, org/repo, org, repo
      git@scm.company.com:very/long/context/path/org/repo.git, scm.company.com/very/long/context/path, Bitbucket, org/repo, org, repo
      
      https://scm.company.com/group/subgroup/subergroup/subestgroup/repo, scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      https://scm.company.com:1234/group/subgroup/subergroup/subestgroup/repo, https://scm.company.com:1234, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@scm.company.com/group/subgroup/subergroup/subestgroup/repo.git, scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@scm.company.com:group/subgroup/subergroup/subestgroup/repo.git, ssh://scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      https://scm.company.com:443/group/subgroup/subergroup/subestgroup/repo.git, scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://scm.company.com:22/group/subgroup/subergroup/subestgroup/repo.git, ssh://scm.company.com, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://scm.company.com:222/group/subgroup/subergroup/subestgroup/repo.git, ssh://scm.company.com:222, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      
      https://scm.company.com/very/long/context/path/group/subgroup/subergroup/subestgroup/repo, scm.company.com/very/long/context/path, GitLab, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      """)
    void parseRegisteredRemote(String cloneUrl, String origin, GitRemote.Service service, String expectedPath, String expectedOrganization, String expectedRepositoryName) {
        GitRemote.Parser parser = new GitRemote.Parser().registerRemote(service, origin);
        GitRemote remote = parser.parse(cloneUrl);
        assertThat(remote.getOrigin()).isEqualTo(origin);
        assertThat(remote.getPath()).isEqualTo(expectedPath);
        assertThat(remote.getOrganization()).isEqualTo(expectedOrganization);
        assertThat(remote.getRepositoryName()).isEqualTo(expectedRepositoryName);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://scm.company.com/very/long/context/path/org/repo.git,
      https://scm.company.com:8443/very/long/context/path/org/repo.git
      http://scm.company.com/very/long/context/path/org/repo.git
      ssh://scm.company.com:7999/very/long/context/path/org/repo.git
      ssh://scm.company.com:222/very/long/context/path/org/repo.git
      ssh://scm.company.com/very/long/context/path/org/repo.git
      scm.company.com:very/long/context/path/org/repo.git
      """)
    void parseRegisteredRemoteServer(String cloneUrl) {
        GitRemote.Parser parser = new GitRemote.Parser()
          .registerRemote(GitRemote.Service.Bitbucket, URI.create("https://scm.company.com/very/long/context/path"), List.of(
            URI.create("https://scm.company.com:8443/very/long/context/path"),
            URI.create("http://scm.company.com/very/long/context/path"),
            URI.create("ssh://scm.company.com:7999/very/long/context/path"),
            URI.create("ssh://scm.company.com:222/very/long/context/path"),
            URI.create("ssh://scm.company.com/very/long/context/path")
          ));
        GitRemote remote = parser.parse(cloneUrl);
        String origin = "scm.company.com/very/long/context/path";
        String expectedPath = "org/repo";
        String expectedOrganization = "org";
        String expectedRepositoryName = "repo";
        assertThat(remote.getOrigin()).isEqualTo(origin);
        assertThat(remote.getPath()).isEqualTo(expectedPath);
        assertThat(remote.getOrganization()).isEqualTo(expectedOrganization);
        assertThat(remote.getRepositoryName()).isEqualTo(expectedRepositoryName);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://github.com/org/repo.git, https://github.com/org/repo
      ssh://github.com/org/repo.git, ssh://github.com/org/repo
      github.com:org/repo.git, ssh://github.com/org/repo
      github.com/org/repo.git, https://github.com/org/repo
      
      https://github.com/org/repo/, https://github.com/org/repo
      ssh://github.com/org/repo/, ssh://github.com/org/repo
      
      https://github.com:443/org/repo.git, https://github.com/org/repo
      ssh://github.com:22/org/repo.git, ssh://github.com/org/repo
      github.com:org/repo.git, ssh://github.com/org/repo
      
      https://github.com:8443/org/repo, https://github.com:8443/org/repo
      ssh://github.com:8022/org/repo, ssh://github.com:8022/org/repo
      
      https://scm.company.com/very/long/context/path/org/repo.git, https://scm.company.com/very/long/context/path/org/repo
      ssh://scm.company.com/very/long/context/path/org/repo.git, ssh://scm.company.com/very/long/context/path/org/repo
      scm.company.com:very/long/context/path/org/repo.git, ssh://scm.company.com/very/long/context/path/org/repo
      """)
    void normalizeUri(String input, String expected) {
        URI result = GitRemote.Parser.normalize(input);
        assertThat(result).isEqualTo(URI.create(expected));
    }

    @Test
    void normalizePortWithoutSchemaNotSupported() {
        IllegalArgumentException ex = catchThrowableOfType(IllegalArgumentException.class, () -> GitRemote.Parser.normalize("github.com:443/org/repo.git"));
        assertThat(ex).isNotNull().hasMessageContaining("Unable to normalize URI. Port without a scheme is not supported");
    }
}
