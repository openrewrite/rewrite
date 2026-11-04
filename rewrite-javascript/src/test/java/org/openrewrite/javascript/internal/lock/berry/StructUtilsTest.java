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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.JsError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * structUtils.ts {@code parseRange}: the source and the selector are read through {@code decodeURIComponent}.
 */
class StructUtilsTest {

    @Test
    void aMalformedPercentEscapeIsJavaScriptsUriError() {
        String range = "patch:ms@npm%3A2.1.3#~/.yarn/patches/x%zz.patch";
        assertThatThrownBy(() -> StructUtils.parseRange(range))
                .isInstanceOf(JsError.class)
                .hasToString("URIError: URI malformed");
        assertThat(StructUtils.tryParseRange(range, StructUtils.RangeOptions.NONE)).isNull();
        assertThat(StructUtils.parseRange("patch:ms@npm%3A2.1.3#~/.yarn/patches/x%25zz.patch").getSelector())
                .isEqualTo("~/.yarn/patches/x%zz.patch");
    }
}
