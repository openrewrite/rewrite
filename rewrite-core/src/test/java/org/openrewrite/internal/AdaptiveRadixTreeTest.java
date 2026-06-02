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
package org.openrewrite.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptiveRadixTreeTest {

    @Test
    void insertAndSearch_SingleKey() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("cat", 1);

        assertThat(tree.search("cat")).isEqualTo(1);
        assertThat(tree.search("ca")).isNull();
        assertThat(tree.search("c")).isNull();
        assertThat(tree.search("dog")).isNull();
    }

    @Test
    void surviveDeepChainOnSmallStack() throws Exception {
        // Inserting a family of strict-extension keys ("a", "aa", "aaa", ...) builds
        // a chain of keyLength=0 internal nodes — one per extension. A recursive
        // insert/search would walk one frame per byte; with the iterative
        // implementation, even much longer keys are bounded by memory rather than the
        // stack. Run on a small-stack thread so that the regression catches the bug
        // regardless of JVM/platform defaults.
        Throwable[] error = new Throwable[1];
        Thread t = new Thread(null, () -> {
            AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
            int depth = 20_000;
            StringBuilder sb = new StringBuilder(depth);
            for (int i = 1; i <= depth; i++) {
                sb.append('a');
                tree.insert(sb.toString(), i);
            }
            assertThat(tree.search(sb.toString())).isEqualTo(depth);
        }, "AdaptiveRadixTreeTest-smallStack", 128 * 1024);
        t.setUncaughtExceptionHandler((thr, ex) -> error[0] = ex);
        t.start();
        t.join();
        if (error[0] != null) {
            throw new AssertionError("Failed on 128KB stack", error[0]);
        }
    }

    @Test
    void fullInternaNode() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("cat", 1);
        tree.insert("child", 1);
        tree.insert("circle", 1);
        tree.insert("cod", 1);
        tree.insert("cute", 1);
    }

    @Test
    void copy() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("cat", 1);
        AdaptiveRadixTree<Integer> copy = tree.copy();
        assertThat(copy.search("cat")).isEqualTo(1);
        assertThat(copy.search("ca")).isNull();
        assertThat(copy.search("c")).isNull();
        assertThat(copy.search("dog")).isNull();
    }

    @Test
    void insertAndSearch_MultipleKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("cat", 1);
        tree.insert("car", 2);
        tree.insert("cart", 3);
        tree.insert("dog", 4);

        assertThat(tree.search("cat")).isEqualTo(1);
        assertThat(tree.search("car")).isEqualTo(2);
        assertThat(tree.search("cart")).isEqualTo(3);
        assertThat(tree.search("dog")).isEqualTo(4);

        assertThat(tree.search("ca")).isNull();
        assertThat(tree.search("c")).isNull();
        assertThat(tree.search("do")).isNull();
        assertThat(tree.search("dogs")).isNull();
    }

    @Test
    void insertAndSearch_OverlappingKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("test", 1);
        tree.insert("testing", 2);
        tree.insert("tester", 3);

        assertThat(tree.search("test")).isEqualTo(1);
        assertThat(tree.search("testing")).isEqualTo(2);
        assertThat(tree.search("tester")).isEqualTo(3);

        assertThat(tree.search("tes")).isNull();
        assertThat(tree.search("testers")).isNull();
    }

    @Test
    void insertAndSearch_PrefixKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("a", 1);
        tree.insert("ab", 2);
        tree.insert("abc", 3);
        tree.insert("abcd", 4);

        assertThat(tree.search("a")).isEqualTo(1);
        assertThat(tree.search("ab")).isEqualTo(2);
        assertThat(tree.search("abc")).isEqualTo(3);
        assertThat(tree.search("abcd")).isEqualTo(4);

        assertThat(tree.search("abcde")).isNull();
        assertThat(tree.search("abce")).isNull();
    }

    @Test
    void insertAndSearch_EmptyString() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("", 1);

        assertThat(tree.search("")).isEqualTo(1);
        assertThat(tree.search(" ")).isNull();
        assertThat(tree.search("a")).isNull();
    }

    @Test
    void insertAndSearch_SpecialCharacters() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("hello-world", 1);
        tree.insert("hello_world", 2);
        tree.insert("hello world", 3);

        assertThat(tree.search("hello-world")).isEqualTo(1);
        assertThat(tree.search("hello_world")).isEqualTo(2);
        assertThat(tree.search("hello world")).isEqualTo(3);

        assertThat(tree.search("hello")).isNull();
        assertThat(tree.search("world")).isNull();
    }

    @Test
    void insertAndSearch_CaseSensitivity() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("Apple", 1);
        tree.insert("apple", 2);

        assertThat(tree.search("Apple")).isEqualTo(1);
        assertThat(tree.search("apple")).isEqualTo(2);

        assertThat(tree.search("Appl")).isNull();
        assertThat(tree.search("APPLE")).isNull();
    }

    @Test
    void insertAndSearch_NumericKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("123", 1);
        tree.insert("1234", 2);
        tree.insert("12345", 3);

        assertThat(tree.search("123")).isEqualTo(1);
        assertThat(tree.search("1234")).isEqualTo(2);
        assertThat(tree.search("12345")).isEqualTo(3);

        assertThat(tree.search("12")).isNull();
        assertThat(tree.search("123456")).isNull();
    }

    @Test
    void insertAndSearch_MixedCharacterKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("user1", 1);
        tree.insert("user2", 2);
        tree.insert("user10", 3);
        tree.insert("user20", 4);

        assertThat(tree.search("user1")).isEqualTo(1);
        assertThat(tree.search("user2")).isEqualTo(2);
        assertThat(tree.search("user10")).isEqualTo(3);
        assertThat(tree.search("user20")).isEqualTo(4);

        assertThat(tree.search("user")).isNull();
        assertThat(tree.search("user3")).isNull();
    }

    @Test
    void insertAndSearch_LongKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        String longKey1 = "a".repeat(1000);
        String longKey2 = "a".repeat(999) + "b";

        tree.insert(longKey1, 1);
        tree.insert(longKey2, 2);

        assertThat(tree.search(longKey1)).isEqualTo(1);
        assertThat(tree.search(longKey2)).isEqualTo(2);

        assertThat(tree.search("a".repeat(500))).isNull();
    }

    @Test
    void insertAndSearch_NullValue() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("key", null);

        assertThat(tree.search("key")).isNull();
        assertThat(tree.search("ke")).isNull();
    }

    @Test
    void insertAndSearch_UnicodeKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("こんにちは", 1); // "Hello" in Japanese
        tree.insert("こんばんは", 2); // "Good evening" in Japanese
        tree.insert("你好", 3);       // "Hello" in Chinese

        assertThat(tree.search("こんにちは")).isEqualTo(1);
        assertThat(tree.search("こんばんは")).isEqualTo(2);
        assertThat(tree.search("你好")).isEqualTo(3);

        assertThat(tree.search("こん")).isNull();
        assertThat(tree.search("你好嗎")).isNull();
    }

    @Test
    void insertAndSearch_EmptyTree() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        assertThat(tree.search("anykey")).isNull();
    }

    @Test
    void insert_DuplicateKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("duplicate", 1);
        tree.insert("duplicate", 2);

        assertThat(tree.search("duplicate")).isEqualTo(2);
    }

    @Test
    void Search_NonExistentKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("exist", 1);

        assertThat(tree.search("nonexist")).isNull();
        assertThat(tree.search("exis")).isNull();
        assertThat(tree.search("exists")).isNull();
    }

    @Test
    void insertAndSearch_SimilarKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("ab", 1);
        tree.insert("abc", 2);
        tree.insert("abcd", 3);
        tree.insert("abcde", 4);
        tree.insert("abcdef", 5);

        assertThat(tree.search("ab")).isEqualTo(1);
        assertThat(tree.search("abc")).isEqualTo(2);
        assertThat(tree.search("abcd")).isEqualTo(3);
        assertThat(tree.search("abcde")).isEqualTo(4);
        assertThat(tree.search("abcdef")).isEqualTo(5);
    }

    @Test
    void insertAndSearch_CommonPrefixes() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("prefix", 1);
        tree.insert("preface", 2);
        tree.insert("preform", 3);
        tree.insert("preposition", 4);
        tree.insert("presentation", 5);

        assertThat(tree.search("prefix")).isEqualTo(1);
        assertThat(tree.search("preface")).isEqualTo(2);
        assertThat(tree.search("preform")).isEqualTo(3);
        assertThat(tree.search("preposition")).isEqualTo(4);
        assertThat(tree.search("presentation")).isEqualTo(5);

        assertThat(tree.search("pre")).isNull();
        assertThat(tree.search("president")).isNull();
    }

    @Test
    void insertAndSearch_SingleCharacterKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("a", 1);
        tree.insert("b", 2);
        tree.insert("c", 3);

        assertThat(tree.search("a")).isEqualTo(1);
        assertThat(tree.search("b")).isEqualTo(2);
        assertThat(tree.search("c")).isEqualTo(3);

        assertThat(tree.search("d")).isNull();
    }

    @Test
    void insertAndSearch_NullKey() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        assertThatThrownBy(() -> tree.insert((String) null, 1))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    void insertAndSearch_SpecialCaseKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("user_name", 1);
        tree.insert("user-name", 2);
        tree.insert("user.name", 3);
        tree.insert("user@name", 4);
        tree.insert("user#name", 5);

        assertThat(tree.search("user_name")).isEqualTo(1);
        assertThat(tree.search("user-name")).isEqualTo(2);
        assertThat(tree.search("user.name")).isEqualTo(3);
        assertThat(tree.search("user@name")).isEqualTo(4);
        assertThat(tree.search("user#name")).isEqualTo(5);

        assertThat(tree.search("user%name")).isNull();
    }

    @Test
    void insertAndSearch_CyrillicKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("привет", 1); // "Hello" in Russian
        tree.insert("проект", 2); // "Project" in Russian

        assertThat(tree.search("привет")).isEqualTo(1);
        assertThat(tree.search("проект")).isEqualTo(2);

        assertThat(tree.search("про")).isNull();
        assertThat(tree.search("прив")).isNull();
    }

    @Test
    void insertAndSearch_EmojiKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("😀", 1);
        tree.insert("😀😁", 2);
        tree.insert("😀😁😂", 3);

        assertThat(tree.search("😀")).isEqualTo(1);
        assertThat(tree.search("😀😁")).isEqualTo(2);
        assertThat(tree.search("😀😁😂")).isEqualTo(3);

        assertThat(tree.search("😀😁😂🤣")).isNull();
    }

    @Test
    void insertAndSearch_MixedLanguageKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("hello世界", 1); // "hello world" mixing English and Chinese
        tree.insert("こんにちはworld", 2); // "hello world" mixing Japanese and English

        assertThat(tree.search("hello世界")).isEqualTo(1);
        assertThat(tree.search("こんにちはworld")).isEqualTo(2);

        assertThat(tree.search("hello")).isNull();
        assertThat(tree.search("world")).isNull();
    }

    @Test
    void insertAndSearch_SpacesInKeys() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("key with spaces", 1);
        tree.insert("another key with spaces", 2);

        assertThat(tree.search("key with spaces")).isEqualTo(1);
        assertThat(tree.search("another key with spaces")).isEqualTo(2);

        assertThat(tree.search("key with")).isNull();
        assertThat(tree.search("another key")).isNull();
    }

    @Test
    void insertAndSearch_SpecialUnicodeCharacters() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("naïve", 1);
        tree.insert("café", 2);
        tree.insert("résumé", 3);

        assertThat(tree.search("naïve")).isEqualTo(1);
        assertThat(tree.search("café")).isEqualTo(2);
        assertThat(tree.search("résumé")).isEqualTo(3);

        assertThat(tree.search("naive")).isNull();
        assertThat(tree.search("cafe")).isNull();
    }

    @Test
    void insertAndSearch_ControlCharacters() {
        AdaptiveRadixTree<Integer> tree = new AdaptiveRadixTree<>();
        tree.insert("line1\nline2", 1);
        tree.insert("tab\tcharacter", 2);

        assertThat(tree.search("line1\nline2")).isEqualTo(1);
        assertThat(tree.search("tab\tcharacter")).isEqualTo(2);

        assertThat(tree.search("line1")).isNull();
        assertThat(tree.search("tab")).isNull();
    }
}
