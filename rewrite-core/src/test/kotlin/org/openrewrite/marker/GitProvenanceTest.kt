/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.marker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.Tree.randomId

class GitProvenanceTest {
    private val sshRepo = GitProvenance(randomId(), "ssh://git@github.com/openrewrite/rewrite.git", "main", "123")
    private val httpsRepo = GitProvenance(randomId(), "https://github.com/openrewrite/rewrite.git", "main", "123")
    private val sshAlternateFormRepo = GitProvenance(randomId(), "git@github.com:openrewrite/rewrite.git", "main", "123")

    @Test
    fun getOrganizationName() {
        assertThat(sshRepo.organizationName).isEqualTo("openrewrite")
        assertThat(httpsRepo.organizationName).isEqualTo("openrewrite")
        assertThat(sshAlternateFormRepo.organizationName).isEqualTo("openrewrite")
    }

    @Test
    fun getRepositoryName() {
        assertThat(sshRepo.repositoryName).isEqualTo("rewrite")
        assertThat(httpsRepo.repositoryName).isEqualTo("rewrite")
        assertThat(sshAlternateFormRepo.repositoryName).isEqualTo("rewrite")
    }
}
