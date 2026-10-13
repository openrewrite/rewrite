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

import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleEntry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Port of @npmcli/arborist test/case-insensitive-map.js. The map's keys are package names, so the
 * upstream null, object and NaN keys are not representable; the string-keyed half of each case is.
 */
class CaseInsensitiveMapTest {

    @Test
    void setValuesInCtor() {
        CaseInsensitiveMap<String> cmap = new CaseInsensitiveMap<>();
        cmap.set("a", "a");
        cmap.set("A", "A");
        assertThat(cmap.entries()).containsExactly(new SimpleEntry<>("A", "A"));
        assertThat(cmap.has("a")).isTrue();
        assertThat(cmap.has("A")).isTrue();
        assertThat(cmap.get("a")).isEqualTo("A");
        cmap.delete("a");
        assertThat(cmap.has("a")).isFalse();
        assertThat(cmap.has("A")).isFalse();
        assertThat(cmap.get("A")).isNull();
    }

    @Test
    void setValuesAfterCtor() {
        CaseInsensitiveMap<String> cmap = new CaseInsensitiveMap<>();
        cmap.set("a", "a");
        assertThat(cmap.has("a")).isTrue();
        assertThat(cmap.has("A")).isTrue();
        cmap.set("A", "A");
        assertThat(cmap.entries()).containsExactly(new SimpleEntry<>("A", "A"));
        cmap.delete("a");
        assertThat(cmap.has("a")).isFalse();
        assertThat(cmap.has("A")).isFalse();
        assertThat(cmap.get("A")).isNull();
    }

    // before npm 7.21.0 a node's children and edges are a plain Map: a name of another case is another key, and a key
    // set again keeps its place
    @Test
    void plainMapBefore7_21_0() {
        CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(false);
        map.set("a", "a");
        map.set("A", "A");
        map.set("b", "b");
        map.set("a", "again");
        assertThat(map.entries()).containsExactly(new SimpleEntry<>("a", "again"), new SimpleEntry<>("A", "A"),
          new SimpleEntry<>("b", "b"));
        map.delete("A");
        assertThat(map.has("A")).isFalse();
        assertThat(map.get("a")).isEqualTo("again");

        Node root = NpmTreeSupport.node(NpmTreeSupport.opts().path("/root").release(NpmRelease.of("7.20.6"))
          .pkg(NpmTreeSupport.obj("dependencies", NpmTreeSupport.obj("JSONStream", "1", "jsonstream", "1"))));
        assertThat(root.getEdgesOut().keys()).containsExactly("JSONStream", "jsonstream");
        Node later = NpmTreeSupport.node(NpmTreeSupport.opts().path("/root").release(NpmRelease.of("7.21.0"))
          .pkg(NpmTreeSupport.obj("dependencies", NpmTreeSupport.obj("JSONStream", "1", "jsonstream", "1"))));
        assertThat(later.getEdgesOut().keys()).containsExactly("jsonstream");
    }

    @Test
    void doNotGetConfusedWithWeirdValues() {
        CaseInsensitiveMap<String> cmap = new CaseInsensitiveMap<>();
        cmap.set("NaN", "this is a string");
        cmap.set("nan", "this is a quieter string");
        cmap.delete("foo");
        cmap.delete("NAN");
        assertThat(cmap.entries()).isEmpty();
    }
}
