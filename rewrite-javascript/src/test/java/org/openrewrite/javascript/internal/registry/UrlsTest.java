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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlsTest {

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
