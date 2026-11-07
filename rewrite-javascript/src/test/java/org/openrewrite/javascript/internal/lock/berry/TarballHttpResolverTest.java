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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * plugin-http tests/urlUtils.test.ts {@code isTgzUrl} (Yarn 4.10.3).
 */
class TarballHttpResolverTest {

    @ParameterizedTest(name = "isTgzUrl({0}) = {1}")
    @CsvSource(delimiter = '|', value = {
            "https://example.org/package.tar.gz|true",
            "https://example.org/package.tgz|true",
            "https://example.org/package|true",
            "https://example.org/package.git|false",
            "https://example.org/package.tgz.git|false",
            "ftp://example.org/package.tgz|false",
            "1.2.3|false",
    })
    void detectsATarballUrl(String url, boolean expected) {
        assertThat(TarballHttpResolver.isTgzUrl(url)).isEqualTo(expected);
    }
}
