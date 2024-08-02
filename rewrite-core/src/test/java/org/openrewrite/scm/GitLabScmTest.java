package org.openrewrite.scm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.internal.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabScmTest {

    @CsvSource(textBlock = """
      https://gitlab.com/group/repo.git, true, gitlab.com, group/repo, group, group
      https://gitlab.com/group/subgroup/repo.git, true, gitlab.com, group/subgroup/repo, group/subgroup, group
            
      https://dev.azure.com/org/project/_git/repo.git, false,,,
      git@ssh.dev.azure.com:v3/org/project/repo.git, false,,,
      https://github.com/org/repo, false,,,,
      https://scm.company.com/scm/project/repo.git, false,,,,
      git@scm.company.com:context/path/scm/project/repo.git, false,,,,
      """)
    @ParameterizedTest
    void splitOriginPath(String cloneUrl, boolean matchesScm, @Nullable String expectedOrigin, @Nullable String expectedPath, @Nullable String expectedGroupPath) {
        Scm scm = new GitLabScm();
        assertThat(scm.belongsToScm(cloneUrl)).isEqualTo(matchesScm);
        if (matchesScm) {
            assertThat(scm.getOrigin()).isEqualTo(expectedOrigin);
            CloneUrl parsed = scm.parseCloneUrl(cloneUrl);
            assertThat(parsed).isInstanceOf(GitLabCloneUrl.class);
            GitLabCloneUrl gitLabCloneUrl = (GitLabCloneUrl) parsed;
            assertThat(gitLabCloneUrl.getOrigin()).isEqualTo(expectedOrigin);
            assertThat(gitLabCloneUrl.getPath()).isEqualTo(expectedPath);
            assertThat(gitLabCloneUrl.getGroupPath()).isEqualTo(expectedGroupPath);
            assertThat(gitLabCloneUrl.getOrganization()).isEqualTo(expectedGroupPath);
        }
    }
}