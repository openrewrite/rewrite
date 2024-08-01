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