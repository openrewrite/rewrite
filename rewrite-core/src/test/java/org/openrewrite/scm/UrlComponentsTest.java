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

class UrlComponentsTest {
    @CsvSource(textBlock = """
            https://github.com/organization/repository, null, null, https, github.com, organization/repository, null
            http://github.com/organization/repository, null, null, http, github.com, organization/repository, null
            https://user:pass@github.com/organization/repository, user, pass, https, github.com, organization/repository, null
            http://user:pass@github.com/organization/repository, user, pass, http, github.com, organization/repository, null
            https://github.com:443/organization/repository, null, null, https, github.com, organization/repository, 443
            http://github.com:8080/organization/repository, null, null, http, github.com, organization/repository, 8080
            ssh://git@github.com:organization/repository, git, null, ssh, github.com, organization/repository, null
            git@github.com:organization/repository, git, null, ssh, github.com, organization/repository, null
            ssh://git@github.com:443/organization/repository, git, null, ssh, github.com, organization/repository, 443
            git@github.com:8080/organization/repository, git, null, null, github.com, organization/repository, 8080
            """, nullValues = {"null"})
    @ParameterizedTest
    void parse(String url, String username, String password, String scheme, String host, String path, Integer port) {
        UrlComponents result = UrlComponents.parse(url);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo(password);
        assertThat(result.getScheme()).isEqualTo(scheme);
        assertThat(result.getHost()).isEqualTo(host);
        assertThat(result.getPath()).isEqualTo(path);
        assertThat(result.getPort()).isEqualTo(port);
    }
}