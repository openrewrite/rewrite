/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://host.example:443/npm/",
            "http://host.example:80/npm/",
            "https://host.example:0443/npm/",
            "http://host.example:080/npm/",
            "HTTPS://host.example:443/npm/"
    })
    void nerfDartOmitsDefaultPorts(String url) {
        assertThat(Urls.nerfDart(url)).isEqualTo("//host.example/npm/");
    }

    @ParameterizedTest
    @CsvSource({
            "https://host.example:8443/npm/, //host.example:8443/npm/",
            "http://host.example:8080/npm/, //host.example:8080/npm/",
            "https://host.example:80/npm/, //host.example:80/npm/",
            "http://host.example:443/npm/, //host.example:443/npm/"
    })
    void nerfDartRetainsNonDefaultPorts(String url, String expected) {
        assertThat(Urls.nerfDart(url)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "https://[::1]:443/npm/, //[::1]/npm/",
            "http://[::1]:80/npm/, //[::1]/npm/",
            "https://[::443]/npm/, //[::443]/npm/",
            "https://[::1]:8443/npm/, //[::1]:8443/npm/"
    })
    void nerfDartHandlesIpv6Ports(String url, String expected) {
        assertThat(Urls.nerfDart(url)).isEqualTo(expected);
    }

    @Test
    void unscopedNameUnchanged() {
        assertThat(Urls.encodeName("lodash")).isEqualTo("lodash");
        assertThat(Urls.encodeName("is-number")).isEqualTo("is-number");
    }

    @Test
    void scopeSeparatorEncoded() {
        assertThat(Urls.encodeName("@angular/core")).isEqualTo("@angular%2Fcore");
    }

    @Test
    void pathSignificantCharactersPercentEncoded() {
        // A space (or any non-unreserved char) must be encoded so it cannot break the URL path.
        assertThat(Urls.encodeName("weird name")).isEqualTo("weird%20name");
        assertThat(Urls.encodeName("a?b")).isEqualTo("a%3Fb");
    }

    @Test
    void pathTraversalRejected() {
        assertThatThrownBy(() -> Urls.encodeName("..")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Urls.encodeName("../etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Urls.encodeName("@scope/../evil")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rawSlashInRemainderRejected() {
        assertThatThrownBy(() -> Urls.encodeName("a/b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Urls.encodeName("@scope/a/b")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Urls.encodeName("/leading")).isInstanceOf(IllegalArgumentException.class);
    }
}
