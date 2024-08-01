package org.openrewrite.scm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownScmTest {

    @CsvSource(textBlock = """
            https://gitlab.company.com/group/repo.git, gitlab.company.com, group/repo
            https://gitlab.company.com/group/subgroup/subsubgroup/repo, gitlab.company.com/group/subgroup, subsubgroup/repo
            https://bitbucket.company.com/org/repo.git, bitbucket.company.com, org/repo
            https://scm.company.com/org/repo.git, scm.company.com, org/repo
            ssh://git@scm.company.com:context/path/org/repo, scm.company.com/context/path, org/repo
            https://scm.company.com:8080/org/repo.git, scm.company.com:8080, org/repo
            https://scm.company.com:8080/context/path/org/repo.git, scm.company.com:8080/context/path, org/repo
            """)
    @ParameterizedTest
    void splitOriginPath(String cloneUrl, String expectedOrigin, String expectedPath) {
        Scm scm = new UnknownScm(cloneUrl);
        assertThat(scm.belongsToScm(cloneUrl)).isTrue();
        assertThat(scm.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(scm.determineScmUrlComponents(cloneUrl).getOrigin()).isEqualTo(expectedOrigin);
        assertThat(scm.determineScmUrlComponents(cloneUrl).getPath()).isEqualTo(expectedPath);
    }
}