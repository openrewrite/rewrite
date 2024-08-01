package org.openrewrite.scm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.internal.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class AzureDevOpsScmTest {

    @CsvSource(textBlock = """
            https://dev.azure.com/org/project/_git/repo.git, true, dev.azure.com, org/project/repo
            git@ssh.dev.azure.com:v3/org/project/repo.git, true, dev.azure.com, org/project/repo
            
            https://github.com/org/repo, false,,
            https://gitlab.com/org/repo, false,,
            https://scm.company.com/scm/project/repo.git, false,,
            git@scm.company.com:context/path/scm/project/repo.git, false,,
            """)
    @ParameterizedTest
    void splitOriginPath(String cloneUrl, boolean matchesScm, @Nullable String expectedOrigin, @Nullable String expectedPath) {
        Scm scm = new AzureDevOpsScm();
        assertThat(scm.belongsToScm(cloneUrl)).isEqualTo(matchesScm);
        if(matchesScm) {
            assertThat(scm.getOrigin()).isEqualTo(expectedOrigin);
            ScmUrlComponents urlParts = scm.determineScmUrlComponents(cloneUrl);
            assertThat(urlParts.getOrigin()).isEqualTo(expectedOrigin);
            assertThat(urlParts.getPath()).isEqualTo(expectedPath);
        }
    }

}