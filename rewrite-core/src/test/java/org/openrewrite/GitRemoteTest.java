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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

public class GitRemoteTest {

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://github.com/org/repo, github.com, org/repo, org, repo
      https://github.com/1org/repo, github.com, 1org/repo, 1org, repo
      https://github.com/1234/repo, github.com, 1234/repo, 1234, repo
      git@github.com:org/repo.git, github.com, org/repo, org, repo
      git@github.com:1org/1repo.git, github.com, 1org/1repo, 1org, 1repo
      git@github.com:1234/1repo.git, github.com, 1234/1repo, 1234, 1repo
      ssh://github.com/org/repo.git, github.com, org/repo, org, repo

      https://gitlab.com/group/repo.git, gitlab.com, group/repo, group, repo
      https://gitlab.com/group/subgroup/subergroup/subestgroup/repo.git, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@gitlab.com:group/subgroup/subergroup/subestgroup/repo.git, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://git@gitlab.com:22/group/subgroup/subergroup/subestgroup/repo.git, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo

      https://bitbucket.org/PRJ/repo, bitbucket.org, PRJ/repo, PRJ, repo
      git@bitbucket.org:PRJ/repo.git, bitbucket.org, PRJ/repo, PRJ, repo
      git@bitbucket.org:1PRJ/repo.git, bitbucket.org, 1PRJ/repo, 1PRJ, repo
      ssh://bitbucket.org/PRJ/repo.git, bitbucket.org, PRJ/repo, PRJ, repo

      https://org@dev.azure.com/org/project/_git/repo, dev.azure.com, org/project/repo, org/project, repo
      https://dev.azure.com/org/project/_git/repo, dev.azure.com, org/project/repo, org/project, repo
      git@ssh.dev.azure.com:v3/org/project/repo, dev.azure.com, org/project/repo, org/project, repo

      https://github.com/group/repo with spaces, github.com, group/repo with spaces, group, repo with spaces
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
      https://scm.company.com/stash/scm/org/repo.git, scm.company.com/stash, Bitbucket, scm.company.com/stash, org/repo, org, repo
      http://scm.company.com:80/stash/scm/org/repo.git, http://scm.company.com/stash, Bitbucket, scm.company.com/stash, org/repo, org, repo
      http://scm.company.com:8080/stash/scm/org/repo.git, http://scm.company.com:8080/stash, Bitbucket, scm.company.com:8080/stash, org/repo, org, repo
      https://scm.company.com:443/stash/scm/org/repo.git, scm.company.com/stash, Bitbucket, scm.company.com/stash, org/repo,  org, repo
      https://scm.company.com:1234/stash/scm/org/repo.git, https://scm.company.com:1234/stash, Bitbucket, scm.company.com:1234/stash, org/repo, org, repo
      git@scm.company.com:stash/org/repo.git, scm.company.com/stash, Bitbucket, scm.company.com/stash, org/repo, org, repo
      ssh://scm.company.com/stash/org/repo, scm.company.com/stash, Bitbucket, scm.company.com/stash, org/repo, org, repo
      ssh://scm.company.com:22/stash/org/repo, scm.company.com/stash, Bitbucket, scm.company.com/stash, org/repo, org, repo
      ssh://scm.company.com:7999/stash/org/repo, ssh://scm.company.com:7999/stash, Bitbucket, scm.company.com:7999/stash, org/repo, org, repo

      https://scm.company.com/very/long/context/path/org/repo.git, scm.company.com/very/long/context/path, Bitbucket, scm.company.com/very/long/context/path, org/repo, org, repo
      https://scm.company.com:1234/very/long/context/path/org/repo.git, https://scm.company.com:1234/very/long/context/path, Bitbucket, scm.company.com:1234/very/long/context/path, org/repo, org, repo
      git@scm.company.com:very/long/context/path/org/repo.git, scm.company.com/very/long/context/path, Bitbucket, scm.company.com/very/long/context/path, org/repo, org, repo

      https://scm.company.com/group/subgroup/subergroup/subestgroup/repo, scm.company.com, GitLab, scm.company.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      https://scm.company.com:1234/group/subgroup/subergroup/subestgroup/repo, https://scm.company.com:1234, GitLab, scm.company.com:1234, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@scm.company.com/group/subgroup/subergroup/subestgroup/repo.git, scm.company.com, GitLab, scm.company.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      git@scm.company.com:group/subgroup/subergroup/subestgroup/repo.git, ssh://scm.company.com, GitLab, scm.company.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      https://scm.company.com:443/group/subgroup/subergroup/subestgroup/repo.git, scm.company.com, GitLab, scm.company.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://scm.company.com:22/group/subgroup/subergroup/subestgroup/repo.git, ssh://scm.company.com, GitLab, scm.company.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      ssh://scm.company.com:222/group/subgroup/subergroup/subestgroup/repo.git, ssh://scm.company.com:222, GitLab, scm.company.com:222, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo

      https://scm.company.com/very/long/context/path/group/subgroup/subergroup/subestgroup/repo, scm.company.com/very/long/context/path, GitLab, scm.company.com/very/long/context/path, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, repo
      """)
    void parseRegisteredRemote(String cloneUrl, String originToRegister, GitRemote.Service service, String expectedOrigin, String expectedPath, String expectedOrganization, String expectedRepositoryName) {
        GitRemote.Parser parser = new GitRemote.Parser().registerRemote(service, originToRegister);
        GitRemote remote = parser.parse(cloneUrl);
        assertThat(remote.getOrigin()).isEqualTo(expectedOrigin);
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
          .registerRemote(GitRemote.Service.Bitbucket, URI.create("https://scm.company.com/very/long/context/path/"), List.of(
            URI.create("https://scm.company.com:8443/very/long/context/path/"),
            URI.create("http://scm.company.com/very/long/context/path/"),
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
        assertThat(ex).isNotNull().hasMessageContaining("Unable to normalize URL: Specifying a port without a scheme is not supported for URL");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      GitHub, github.com, org/repo, https, https://github.com/org/repo.git
      GitHub, github.com, org/repo, ssh, ssh://git@github.com/org/repo.git
      GitHub, https://github.com, org/repo, https, https://github.com/org/repo.git
      GitHub, https://github.com, org/repo, ssh, ssh://git@github.com/org/repo.git

      GitLab, gitlab.com, group/subgroup/repo, https, https://gitlab.com/group/subgroup/repo.git
      GitLab, gitlab.com, group/subgroup/repo, ssh, ssh://git@gitlab.com/group/subgroup/repo.git
      AzureDevOps, dev.azure.com, org/project/repo, https, https://dev.azure.com/org/project/_git/repo
      AzureDevOps, dev.azure.com, org/project/repo, ssh, ssh://git@ssh.dev.azure.com/v3/org/project/repo
      BitbucketCloud, bitbucket.org, org/repo, https, https://bitbucket.org/org/repo.git
      BitbucketCloud, bitbucket.org, org/repo, ssh, ssh://git@bitbucket.org/org/repo.git

      Bitbucket, scm.company.com/context/bitbucket, org/repo, https, https://scm.company.com/context/bitbucket/scm/org/repo.git
      Bitbucket, scm.company.com/context/bitbucket, org/repo, http, http://scm.company.com/context/bitbucket/scm/org/repo.git
      Bitbucket, scm.company.com/context/bitbucket, org/repo, ssh, ssh://git@scm.company.com:7999/context/bitbucket/org/repo.git
      GitHub, scm.company.com/context/github, org/repo, https, https://scm.company.com/context/github/org/repo.git
      GitHub, scm.company.com/context/github, org/repo, ssh, ssh://git@scm.company.com/context/github/org/repo.git
      GitLab, scm.company.com/context/gitlab, group/subgroup/repo, https, https://scm.company.com/context/gitlab/group/subgroup/repo.git
      GitLab, scm.company.com:8443/context/gitlab, group/subgroup/repo, https, https://scm.company.com:8443/context/gitlab/group/subgroup/repo.git
      GitLab, scm.company.com/context/gitlab, group/subgroup/repo, ssh, ssh://git@scm.company.com:8022/context/gitlab/group/subgroup/repo.git

      Bitbucket, scm.company.com:12345/context/bitbucket, org/repo, https, https://scm.company.com:12345/context/bitbucket/scm/org/repo.git
      Bitbucket, scm.company.com:12346/context/bitbucket, org/repo, https, https://scm.company.com:12345/context/bitbucket/scm/org/repo.git

      Unknown, scm.unregistered.com/context/path/, org/repo, https, https://scm.unregistered.com/context/path/org/repo.git
      Unknown, scm.unregistered.com/context/path/, org/repo, ssh, ssh://scm.unregistered.com/context/path/org/repo.git
      """)
    void buildUri(GitRemote.Service service, String origin, String path, String protocol, String expectedUri) {
        GitRemote remote = new GitRemote(service, null, origin, path, null, null);
        URI uri = new GitRemote.Parser()
          .registerRemote(GitRemote.Service.Bitbucket, URI.create("https://scm.company.com/context/bitbucket/"), List.of(URI.create("http://scm.company.com/context/bitbucket/"), URI.create("ssh://git@scm.company.com:7999/context/bitbucket")))
          .registerRemote(GitRemote.Service.GitHub, URI.create("https://scm.company.com/context/github"), List.of(URI.create("ssh://git@scm.company.com/context/github/")))
          .registerRemote(GitRemote.Service.GitLab, URI.create("https://scm.company.com/context/gitlab/"), List.of(URI.create("ssh://git@scm.company.com:8022/context/gitlab")))
          .registerRemote(GitRemote.Service.GitLab, URI.create("https://scm.company.com:8443/context/gitlab"), List.of(URI.create("https://scm.company.com/context/gitlab/")))
          .registerRemote(GitRemote.Service.Bitbucket, URI.create("https://scm.company.com:12345/context/bitbucket"), List.of(URI.create("https://scm.company.com:12346/context/bitbucket/")))
          .toUri(remote, protocol);
        assertThat(uri).isEqualTo(URI.create(expectedUri));
    }

    @Test
    void buildUriUnregisteredOriginWithPortNotSupported() {
        GitRemote remote = new GitRemote(GitRemote.Service.Unknown, null, "scm.unregistered.com:8443/context/path", "org/repo", null, null);
        IllegalArgumentException ex = catchThrowableOfType(IllegalArgumentException.class, () -> new GitRemote.Parser().toUri(remote, "ssh"));
        assertThat(ex).isNotNull().hasMessageContaining("Unable to determine protocol/port combination for an unregistered origin with a port");
    }

    @Test
    void shouldNotStripJgit() {
        GitRemote.Parser parser = new GitRemote.Parser();
        GitRemote remote = parser.parse("https://github.com/openrewrite/jgit");
        assertThat(remote.getPath()).isEqualTo("openrewrite/jgit");
    }

    @Test
    void shouldNotReplaceExistingWellKnownServer(){
        GitRemote.Parser parser = new GitRemote.Parser()
          .registerRemote(GitRemote.Service.GitHub, URI.create("https://github.com"), List.of(URI.create("ssh://notgithub.com")));

        assertThat(parser.findRemoteServer("github.com").getUris())
          .containsExactlyInAnyOrder(URI.create("https://github.com"), URI.create("ssh://git@github.com"));
    }

    @Test
    void findRemote() {
        GitRemote.Parser parser = new GitRemote.Parser()
          .registerRemote(GitRemote.Service.Bitbucket, URI.create("scm.company.com/stash"), emptyList());
        assertThat(parser.findRemoteServer("github.com").getService()).isEqualTo(GitRemote.Service.GitHub);
        assertThat(parser.findRemoteServer("https://github.com").getService()).isEqualTo(GitRemote.Service.GitHub);
        assertThat(parser.findRemoteServer("gitlab.com").getService()).isEqualTo(GitRemote.Service.GitLab);
        assertThat(parser.findRemoteServer("bitbucket.org").getService()).isEqualTo(GitRemote.Service.BitbucketCloud);
        assertThat(parser.findRemoteServer("dev.azure.com").getService()).isEqualTo(GitRemote.Service.AzureDevOps);
        assertThat(parser.findRemoteServer("scm.company.com/stash").getService()).isEqualTo(GitRemote.Service.Bitbucket);
        assertThat(parser.findRemoteServer("scm.unregistered.com").getService()).isEqualTo(GitRemote.Service.Unknown);
        assertThat(parser.findRemoteServer("scm.unregistered.com").getOrigin()).isEqualTo("scm.unregistered.com");
        assertThat(parser.findRemoteServer("https://scm.unregistered.com").getOrigin()).isEqualTo("scm.unregistered.com");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://github.com/org/repo, github.com, org/repo, org, repo
      https://GITHUB.COM/ORG/REPO, github.com, ORG/REPO, ORG, REPO
      ssh://GITHUB.COM/ORG/REPO.GIT, github.com, ORG/REPO, ORG, REPO
      https://DEV.AZURE.COM/ORG/PROJECT/_GIT/REPO, dev.azure.com, ORG/PROJECT/REPO, ORG/PROJECT, REPO
      GIT@SSH.DEV.AZURE.COM:V3/ORG/PROJECT/REPO, dev.azure.com, ORG/PROJECT/REPO, ORG/PROJECT, REPO
      """)
    void parseOriginCaseInsensitive(String cloneUrl, String expectedOrigin, String expectedPath, String expectedOrganization, String expectedRepositoryName) {
        GitRemote.Parser parser = new GitRemote.Parser();
        GitRemote remote = parser.parse(cloneUrl);
        assertThat(remote.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(remote.getPath()).isEqualTo(expectedPath);
        assertThat(remote.getOrganization()).isEqualTo(expectedOrganization);
        assertThat(remote.getRepositoryName()).isEqualTo(expectedRepositoryName);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        GitHub, GitHub
        GITLAB, GitLab
        bitbucket, Bitbucket
        BitbucketCloud, BitbucketCloud
        Bitbucket Cloud, BitbucketCloud
        BITBUCKET_CLOUD, BitbucketCloud
        AzureDevOps, AzureDevOps
        AZURE_DEVOPS, AzureDevOps
        Azure DevOps, AzureDevOps
        idontknow, Unknown
      """)
    void findServiceForName(String name, GitRemote.Service service) {
        assertThat(GitRemote.Service.forName(name)).isEqualTo(service);
    }

    @Test
    void equalsIgnoresCase() {
        assertThat(new GitRemote(GitRemote.Service.GitHub, "https://github.com/org/repo", "github.com", "org/repo", "org", "repo"))
          .isEqualTo(new GitRemote(GitRemote.Service.GitHub, "https://GITHUB.COM/ORG/REPO", "GITHUB.COM", "ORG/REPO", "ORG", "REPO"));
    }

    @Test
    void hashCodeIgnoresCase() {
        assertThat(new GitRemote(GitRemote.Service.GitHub, "https://github.com/org/repo", "github.com", "org/repo", "org", "repo"))
          .hasSameHashCodeAs(new GitRemote(GitRemote.Service.GitHub, "https://GITHUB.COM/ORG/REPO", "GITHUB.COM", "ORG/REPO", "ORG", "REPO"));
    }
}
