/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keys are String's own bytes on Java 17+ with java.lang open, as in rewrite-java-21's compatibility tests, and UTF-8
 * elsewhere.
 */
class JavaTypeCacheTest {

    @Test
    void lookAlikeKeysKeepTheirOwnValues() {
        String[] keys = {
            "java.util.List", "java.util.List<java.lang.String>",
            // é in Latin-1, and é's UTF-8 bytes read as Latin-1
            "Café", "CafÃ©",
            // Ł is UTF-16, and its two bytes, 0x41 and 0x01 in the platform's byte order, also make a Latin-1 key
            "Ł", "A\u0001", "\u0001A",
            "中文"
        };
        JavaTypeCache cache = new JavaTypeCache();
        for (String key : keys) {
            cache.put(key, key);
        }
        for (String key : keys) {
            assertThat(cache.<String>get(new String(key.toCharArray()))).isEqualTo(key);
        }
    }

    @Test
    void aHashMapCacheGivesEachCloneAMapOfItsOwn() {
        JavaTypeCache cache = new HashMapJavaTypeCache();
        cache.put("base", "base");

        JavaTypeCache overlay = cache.clone();
        overlay.put("overlay", "overlay");
        assertThat(overlay.<String>get("base")).isEqualTo("base");
        assertThat(cache.<String>get("overlay")).isNull();
    }
}
