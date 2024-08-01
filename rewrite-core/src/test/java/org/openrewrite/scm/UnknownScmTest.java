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