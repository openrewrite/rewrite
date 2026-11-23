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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Parity with {@code Intl.Collator('en').compare} in Node v24.19.0, as dumped by
 * npm-arborist/generate/locale-compare-cases.js.
 */
class LocaleCompareTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode CASES = cases();

    private static JsonNode cases() {
        try (InputStream is = LocaleCompareTest.class.getResourceAsStream("/npm-arborist/locale-compare-cases")) {
            return MAPPER.readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T json(JsonNode json) {
        return (T) MAPPER.convertValue(json, Object.class);
    }

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
     * Node answers -1 for both, ICU telling each first string from its normalized form; the port refuses the first
     * rather than answer 0.
     */
    @Test
    void stringsBeyondTheBmpNotInNormalizationFormDAreRefused() {
        String[][] pairs = {
          {"\u0f71\ud800\udf78\u309a", "\u309a\u0f71\ud800\udf78"},
          {"\ud818\udd1e\ud818\udd22", "\ud818\udd1e\ud818\udd1e\ud818\udd29"},
        };
        for (String[] pair : pairs) {
            assertThatThrownBy(() -> LocaleCompare.en(pair[0], pair[1])).isInstanceOf(LocaleCompare.UnknownOrder.class)
              .extracting("string").isEqualTo(pair[0]);
            assertThatThrownBy(() -> LocaleCompare.en(pair[1], pair[0])).isInstanceOf(LocaleCompare.UnknownOrder.class);
            assertThatThrownBy(() -> LocaleCompare.en(pair[0], "a")).isInstanceOf(LocaleCompare.UnknownOrder.class);
            assertThat(LocaleCompare.en(pair[1], pair[1])).isEqualTo(0);
        }
    }

    /**
     * Node answers 0 for each, as the port does: strings not in normalization form D that hold no contraction meeting a
     * supplementary-plane character.
     */
    @Test
    void otherUnnormalizedStringsAreOrderedAsTheirNormalizedForm() {
        assertThat(LocaleCompare.en("caf\u00e9", "cafe\u0301")).isEqualTo(0);
        assertThat(LocaleCompare.en("\u0f71\u0301\u309a", "\u309a\u0f71\u0301")).isEqualTo(0);
        assertThat(LocaleCompare.en("\uac01", "\u1100\u1161\u11a8")).isEqualTo(0);
        assertThat(LocaleCompare.en("\ud87e\udc00", "\u4e3d")).isEqualTo(0);
        assertThat(LocaleCompare.en("\u00e9\udbff\udfff", "e\u0301\udbff\udfff")).isEqualTo(0);
        assertThat(LocaleCompare.en("\u00e9\ud803\udd25", "e\u0301\ud803\udd25")).isEqualTo(0);
        assertThat(LocaleCompare.en("a\u0301\ud803\udd25\u0327", "a\u0327\u0301\ud803\udd25")).isEqualTo(0);
        assertThat(LocaleCompare.en("a\ud818\udd21", "a\ud818\udd1e\ud818\udd1e")).isEqualTo(0);
        assertThat(LocaleCompare.en("\u0ddc\u0dca", "\u0ddd")).isEqualTo(0);
        assertThat(LocaleCompare.en("\ud818\udd1e\ud818\udd1e", "\ud818\udd1e\ud818\udd1e\ud818\udd29")).isEqualTo(-1);
    }

    /**
     * Node orders each apart from its normalized form: a contraction starter followed by a supplementary-plane mark and
     * a mark of a lower class, or U+1138B before a vowel sign whose decomposition continues a contraction.
     */
    @Test
    void contractionsMeetingSupplementaryCharactersAreRefused() {
        String[] refused = {
          "\u0418\ud800\udf78\u0327",
          "\u04e4\ud805\udeb6\u0327",
          "\u0f71\u0f73\ud800\udf78\u0344",
          "\u0ddc\ud803\udf85\u09bc",
          "\u1e1d\u030a\ud804\udf8b\ud804\udfc8",
        };
        for (String s : refused) {
            assertThatThrownBy(() -> LocaleCompare.en(s, "a")).as(s).isInstanceOf(LocaleCompare.UnknownOrder.class);
        }
    }
}
