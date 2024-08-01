package org.openrewrite.scm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.internal.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleScmTest {

    @CsvSource(textBlock = """
            github.com,https://github.com/org/repo.git, true, github.com, org/repo
            https://github.com,https://github.com/org/repo.git, true, github.com, org/repo
            ssh://git@github.com,https://github.com/org/repo.git, true, github.com, org/repo,

            gitlab.com,https://gitlab.com/group/repo.git, true, gitlab.com, group/repo
            https://gitlab.com,https://gitlab.com/group/repo.git, true, gitlab.com, group/repo
            ssh://git@gitlab.com,ssh://git@gitlab.com:group/repo.git, true, gitlab.com, group/repo

            gitlab.com,https://gitlab.com/group/subgroup/subsubgroup/repo.git, true, gitlab.com, group/subgroup/subsubgroup/repo
            https://gitlab.com,https://gitlab.com/group/subgroup/subsubgroup/repo.git, true, gitlab.com, group/subgroup/subsubgroup/repo
            ssh://git@gitlab.com,ssh://git@gitlab.com:group/subgroup/subsubgroup/repo.git, true, gitlab.com, group/subgroup/subsubgroup/repo
            
            bitbucket.org,https://bitbucket.org/org/repo.git, true, bitbucket.org, org/repo
            https://bitbucket.org,https://bitbucket.org/org/repo.git, true, bitbucket.org, org/repo
            ssh://git@bitbucket.org,ssh://git@bitbucket.org:org/repo.git, true, bitbucket.org, org/repo
            
            scm.mycompany.com/context/path/,https://scm.mycompany.com/context/path/group/subgroup/repo.git, true, scm.mycompany.com/context/path, group/subgroup/repo
            """)
    @ParameterizedTest
    void splitOriginPath(String origin, String cloneUrl, boolean matchesScm, @Nullable String expectedOrigin, @Nullable String expectedPath) {
        Scm scm = new SimpleScm(origin);
        assertThat(scm.belongsToScm(cloneUrl)).isEqualTo(matchesScm);
        if (matchesScm) {
            assertThat(scm.getOrigin()).isEqualTo(expectedOrigin);
            assertThat(scm.determineScmUrlComponents(cloneUrl).getOrigin()).isEqualTo(expectedOrigin);
            assertThat(scm.determineScmUrlComponents(cloneUrl).getPath()).isEqualTo(expectedPath);
        }
    }

}