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
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * Parity with {@code Intl.Collator('en').compare} in Node v24.19.0, as dumped by
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
    void unicodeCharacterOrder() {
        List<List<Integer>> order = json(CASES.get("unicodeOrder"));
        String previous = null;
        for (List<Integer> equal : order) {
            String first = new String(Character.toChars(equal.get(0)));
            if (previous != null) {
                assertThat(Integer.signum(LocaleCompare.en(previous, first)))
                  .as("U+%04x against U+%04x", previous.codePointAt(0), equal.get(0))
                  .isEqualTo(-1);
                assertThat(Integer.signum(LocaleCompare.en(first, previous))).isEqualTo(1);
            }
            for (int codePoint : equal) {
                assertThat(LocaleCompare.en(first, new String(Character.toChars(codePoint))))
                  .as("U+%04x against U+%04x", equal.get(0), codePoint)
                  .isEqualTo(0);
            }
            previous = first;
        }
    }

    @Test
    void unicodeStrings() {
        for (JsonNode pair : CASES.get("unicodePairs")) {
            String a = pair.get(0).asText();
            String b = pair.get(1).asText();
            assertThat(Integer.signum(LocaleCompare.en(a, b))).as("%s against %s", pair.get(0), pair.get(1))
              .isEqualTo(pair.get(2).asInt());
        }
    }

    /**
     * Node answers -1 for both: ICU tells each first string from its normalized form, the second. One has
     * a combining character from beyond the Basic Multilingual Plane before one of a lower combining
     * class; in the other a vowel sign from there decomposes into a contraction with the sign before it.
     */
    @Test
    void canonicallyEquivalentStringsAreEqual() {
        assertThat(LocaleCompare.en("\u0f71\ud800\udf78\u309a", "\u309a\u0f71\ud800\udf78")).isEqualTo(0);
        assertThat(LocaleCompare.en("\ud818\udd1e\ud818\udd22", "\ud818\udd1e\ud818\udd1e\ud818\udd29")).isEqualTo(0);
    }
}
