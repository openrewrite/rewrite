package org.openrewrite.scm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.internal.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class BitbucketServerScmTest {
    @CsvSource(textBlock = """
            scm.company.com, https://scm.company.com/scm/org/repo.git, true, scm.company.com, org/repo
            https://scm.company.com/context/path, https://scm.company.com/context/path/scm/org/repo.git, true, scm.company.com/context/path, org/repo
            ssh://git@scm.company.com:8443/context/path, ssh://git@scm.company.com:8443/context/path/scm/org/repo.git, true, scm.company.com:8443/context/path, org/repo
            scm.company.com/context/path, git@scm.company.com:scm/context/path/project/repo.git, true, scm.company.com/context/path, project/repo
            """)
    @ParameterizedTest
    void splitOriginPath(String origin, String cloneUrl, boolean matchesScm, @Nullable String expectedOrigin, @Nullable String expectedPath) {
        Scm scm = new BitbucketServerScm(origin);
        assertThat(scm.belongsToScm(cloneUrl)).isEqualTo(matchesScm);
        if (matchesScm) {
            assertThat(scm.getOrigin()).isEqualTo(expectedOrigin);
            assertThat(scm.determineScmUrlComponents(cloneUrl).getOrigin()).isEqualTo(expectedOrigin);
            assertThat(scm.determineScmUrlComponents(cloneUrl).getPath()).isEqualTo(expectedPath);
        }
    }

}