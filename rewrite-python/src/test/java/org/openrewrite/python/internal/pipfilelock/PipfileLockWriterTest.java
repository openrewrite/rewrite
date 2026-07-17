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
package org.openrewrite.python.internal.pipfilelock;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.python.internal.pipfilelock.Fixtures.resource;
import static org.openrewrite.python.internal.pipfilelock.PipfileLockWriter.read;
import static org.openrewrite.python.internal.pipfilelock.PipfileLockWriter.write;

/**
 * Lock fixtures under {@code src/test/resources/pipfilelock/locks/} are byte-exact
 * reference outputs: {@code real-pipenv-2026.6.2.Pipfile.lock} was produced by a real
 * {@code pipenv lock} run, the others by {@code oracle.py emit} (Python 3.14), i.e.
 * {@code json.dumps(obj, indent=4, separators=(",", ": "), sort_keys=True) + "\n"}.
 */
class PipfileLockWriterTest {

    @Test
    void roundTripRealPipenvLockByteIdentical() {
        String lock = resource("locks/real-pipenv-2026.6.2.Pipfile.lock");
        assertThat(write(read(lock), "\n")).isEqualTo(lock);
    }

    @Test
    void roundTripFullLockByteIdentical() {
        String lock = resource("locks/full.Pipfile.lock");
        assertThat(write(read(lock), "\n")).isEqualTo(lock);
    }

    @Test
    void roundTripEmptyCategories() {
        String lock = resource("locks/empty.Pipfile.lock");
        assertThat(write(read(lock), "\n")).isEqualTo(lock);
        assertThat(lock).contains("\"default\": {}").contains("\"develop\": {}");
    }

    @Test
    void crlfNewlineStylePreserved() {
        String lock = resource("locks/full.Pipfile.lock");
        assertThat(write(read(lock), "\r\n")).isEqualTo(lock.replace("\n", "\r\n"));
    }

    @Test
    void tortureCorpusMatchesPythonJsonEncoder() {
        assertThat(write(read(resource("torture.input")), "\n"))
          .isEqualTo(resource("torture.expected.lock"));
    }

    @Test
    void emptyRootObject() {
        assertThat(write(new LinkedHashMap<>(), "\n")).isEqualTo("{}\n");
    }

    @Test
    void readProducesLongsAndBigIntegersInDocumentOrder() {
        Map<String, Object> root = read("""
          {"z": 1, "a": {"pipfile-spec": 6}, "big": 12345678901234567890123456789, "flag": true, "nil": null}
          """);
        assertThat(root.keySet()).containsExactly("z", "a", "big", "flag", "nil");
        assertThat(root.get("z")).isEqualTo(1L);
        assertThat(((Map<?, ?>) root.get("a")).get("pipfile-spec")).isEqualTo(6L);
        assertThat(root.get("big")).isEqualTo(new BigInteger("12345678901234567890123456789"));
        assertThat(root.get("flag")).isEqualTo(true);
        assertThat(root.get("nil")).isNull();
    }

    @Test
    void rejectsUnsupportedNewline() {
        assertThatThrownBy(() -> write(new LinkedHashMap<>(), "\r"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonObjectRoot() {
        assertThatThrownBy(() -> read("[1, 2]"))
          .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> read("not json"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFloatValues() {
        assertThatThrownBy(() -> read("{\"f\": 1.5}"))
          .isInstanceOf(IllegalArgumentException.class);
    }
}
