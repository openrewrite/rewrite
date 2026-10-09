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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code Date.parse} in Node v24.19.0 over the ISO form, NaN where V8's ISO parser refuses.
 */
class V8DateTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
      "1970-01-01|0",
      "1970-02-29|5097600000",
      "0301-02-30|-52663392000000",
      "1970-04-31|10368000000",
      "0865-06-06T24:00+00:00|-34856784000000",
      "1970-01-01T24:00:00Z|86400000",
      "2020-01-01T00:00+19:00|1577768400000",
      "2020-01-01T00:00+23:59|1577750460000",
      "2020-01-01T00:00+24:00|NaN",
      "1970-13-01|NaN",
      "1970-01-32|NaN",
      "2020-01-01T25:00Z|NaN",
      "+275760-09-13T00:00:00.001Z|NaN",
      "2026-10-08T12:34:56.789Z|1791462896789"
    })
    void parsesAsNode(String text, double expected) {
        assertThat(V8Date.parse(text)).isEqualTo(Double.valueOf(expected));
    }
}
