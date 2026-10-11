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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/inventory.js. The inventoried objects are nodes pinned to the
 * upstream locations; a nameless one sits at the filesystem root, the one place arborist allows it.
 */
class InventoryTest {

    private static Node at(String location, @Nullable String name, Map<String, Object> pkg) {
        Node.Options options = (name != null ? opts().name(name) : opts().path("/")).pkg(pkg).build();
        return new Node(options) {
            @Override
            public String getLocation() {
                return location;
            }
        };
    }

    @Test
    void basicOperations() {
        Inventory i = new Inventory();
        assertThat(i.getPrimaryKey()).isEqualTo("location");
        assertThat(i.getIndexes()).containsExactly("name", "license", "funding", "realpath", "packageName");

        Node x = at("x", "x", obj("licence", "MIT", "funding", "foo"));
        i.add(x);
        Node y = at("y", "x", obj("licenses", arr(obj("type", "ISC")), "funding", obj("url", "foo")));
        i.add(y);
        Node z = at("z", "z", obj("license", obj("type", "MIT"), "funding", "bar"));
        i.add(z);
        Node a = at("a", "a", obj());
        i.add(a);

        assertThat(i.filter(node -> node.getName() != null && node.getName().matches(".*[xy].*")))
          .containsExactly(i.get("x"), i.get("y"));

        assertThat(i.query("license")).containsExactlyInAnyOrder("ISC", "MIT", null);
        assertThat(i.query("license", "MIT")).containsExactly(x, z);
        assertThat(i.query("license", "blerg")).isEmpty();
        assertThat(i.query("name", "x")).containsExactly(x, y);
        assertThat(i.query("funding")).containsExactlyInAnyOrder("bar", "foo", null);
        assertThat(i.query("funding", "foo")).containsExactly(x, y);

        assertThat(i.get("x")).isSameAs(x);
        i.add(x);
        assertThat(i.get("x")).isSameAs(x);
        assertThat(i.has(x)).isTrue();
        Node x2 = at("x", "a", obj("licences", arr(obj("type", "ABC"))));
        i.add(x2);
        assertThat(i.get("x")).isSameAs(x2);
        assertThat(i.has(x)).isFalse();

        assertThat(i.query("license", null)).containsExactly(a);

        i.delete(at("y", "other", obj()));
        assertThat(i.get("y")).isSameAs(y);
        i.delete(y);
        assertThat(i.has(y)).isFalse();
        assertThat(i.get("y")).isNull();

        Node z2 = at("z", null, obj());
        i.add(z2);
        assertThat(i.get("z")).isSameAs(z2);
        z2.setPackage(obj("name", "z"));
        assertThatCode(() -> i.delete(z2)).doesNotThrowAnyException();
        assertThat(i.get("z")).isNull();
        assertThat(i.has(z2)).isFalse();

        assertThatCode(() -> i.add(at("f", "f", obj("license", "MIT", "funding", null)))).doesNotThrowAnyException();
        assertThatCode(() -> i.add(at("l", "l", obj("license", null)))).doesNotThrowAnyException();

        Node n = at("n", "n", obj("name", "n"));
        i.add(n);
        assertThat(i.get("n")).isSameAs(n);
        assertThat(i.query("packageName", "n")).containsExactly(n);
    }

    @Test
    void addingExternalNodesIsNoOpOutsideDebugMode() {
        Inventory i = new Inventory();
        Node root = at("", "root", obj());
        i.add(root);
        Node other = at("adsf", "other", obj());
        i.add(other);
        assertThat(i.has(other)).isFalse();
    }
}
