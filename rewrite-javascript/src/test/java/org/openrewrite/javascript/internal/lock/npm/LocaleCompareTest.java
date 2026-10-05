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
package org.openrewrite.javascript.internal.lock.npm;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * Parity with {@code Intl.Collator('en').compare} in Node v24.19.0 over ASCII, as dumped by
 * npm-arborist/generate/locale-compare-cases.js.
 */
class LocaleCompareTest {

    private static final JsonNode CASES = fixture("locale-compare-cases");

    @Test
    void characterOrder() {
        List<List<String>> order = json(CASES.get("order"));
        for (int i = 0; i < order.size(); i++) {
            for (int j = 0; j < order.size(); j++) {
                for (String a : order.get(i)) {
                    for (String b : order.get(j)) {
                        assertThat(Integer.signum(LocaleCompare.en(a, b)))
                          .as("U+%04x against U+%04x", (int) a.charAt(0), (int) b.charAt(0))
                          .isEqualTo(Integer.compare(i, j));
                    }
                }
            }
        }
    }

    @Test
    void strings() {
        for (JsonNode pair : CASES.get("pairs")) {
            String a = pair.get(0).asText();
            String b = pair.get(1).asText();
            assertThat(Integer.signum(LocaleCompare.en(a, b))).as("%s against %s", pair.get(0), pair.get(1))
              .isEqualTo(pair.get(2).asInt());
        }
    }

    @Test
    void nonAsciiIsRefused() {
        assertThatThrownBy(() -> LocaleCompare.en("café", "cafe")).isInstanceOf(IllegalArgumentException.class);
    }
}
