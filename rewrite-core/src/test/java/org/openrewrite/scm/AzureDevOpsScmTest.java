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
package org.openrewrite.scm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.internal.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class AzureDevOpsScmTest {
    Scm scm = new AzureDevOpsScm();

    @ParameterizedTest
    @CsvSource(textBlock = """
      https://dev.azure.com/org/project/_git/repo, dev.azure.com, org/project/repo, org/project
      git@ssh.dev.azure.com:v3/org/project/repo, dev.azure.com, org/project/repo, org/project
      """)
    void splitOriginPathWithValidUrls(String cloneUrl,
                                      @Nullable String expectedOrigin,
                                      @Nullable String expectedPath,
                                      @Nullable String expectedOrganization) {
        assertThat(scm.belongsToScm(cloneUrl)).isTrue();
        assertThat(scm.getOrigin()).isEqualTo(expectedOrigin);
        CloneUrl parsed = scm.parseCloneUrl(cloneUrl);
        assertThat(parsed).isInstanceOf(AzureDevOpsCloneUrl.class);
        AzureDevOpsCloneUrl azureDevopsCloneUrl = (AzureDevOpsCloneUrl) parsed;
        assertThat(azureDevopsCloneUrl.getOrigin()).isEqualTo(expectedOrigin);
        assertThat(azureDevopsCloneUrl.getPath()).isEqualTo(expectedPath);
        assertThat(azureDevopsCloneUrl.getOrganization()).isEqualTo(expectedOrganization);
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "https://github.com/org/repo",
      "https://gitlab.com/org/repo",
      "https://scm.company.com/scm/project/repo.git",
      "git@scm.company.com:context/path/scm/project/repo.git"
    })
    void splitOriginPathWithInvalidUrls(String cloneUrl) {
        assertThat(scm.belongsToScm(cloneUrl)).isFalse();
    }
}
