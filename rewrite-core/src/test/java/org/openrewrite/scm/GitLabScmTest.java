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
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabScmTest {

    @CsvSource(textBlock = """
      https://gitlab.com/group/repo.git, true, gitlab.com, group/repo, group, group
      https://gitlab.com/group/subgroup/subergroup/subestgroup/repo.git, true, gitlab.com, group/subgroup/subergroup/subestgroup/repo, group/subgroup/subergroup/subestgroup, group
            
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
            assertThat(expectedGroupPath).isNotNull();
            assertThat(gitLabCloneUrl.getGroups()).containsExactlyElementsOf(List.of(expectedGroupPath.split("/")));
            assertThat(gitLabCloneUrl.getOrganization()).isEqualTo(expectedGroupPath);
        }
    }
}