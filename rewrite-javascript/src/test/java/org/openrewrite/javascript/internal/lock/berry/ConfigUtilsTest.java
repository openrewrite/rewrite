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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yarn's packages/yarnpkg-core/tests/configUtils.test.ts, every expectation of its one case.
 */
class ConfigUtilsTest {

    @Test
    void resolvesAllRcFilesAccordingToTheOnConflictOption() {
        assertThat(resolve(rc("a", list("foo")), rc("b", list("bar")), rc("c", list("baz"))))
                .isEqualTo(r("a, b, c", list(r("a", "foo"), r("b", "bar"), r("c", "baz"))));

        assertThat(resolve(rc("a", list("foo")), rc("b", map("onConflict", "reset", "value", list("bar"))),
                rc("c", list("baz"))))
                .isEqualTo(r("b, c", list(r("b", "bar"), r("c", "baz"))));

        assertThat(resolve(rc("a", list(map("foo", "bar")))))
                .isEqualTo(r("a", list(r("a", map("foo", r("a", "bar"))))));

        assertThat(resolve(rc("a", list("foo")), rc("b", "42")))
                .isEqualTo(r("b", "42"));

        assertThat(resolve(rc("a", map("foo", "foo")), rc("b", map("bar", "bar")), rc("c", map("baz", "baz"))))
                .isEqualTo(r("a, b, c", map("foo", r("a", "foo"), "bar", r("b", "bar"), "baz", r("c", "baz"))));

        assertThat(resolve(rc("a", map("foo", "foo")), rc("b", map("onConflict", "reset", "bar", "bar")),
                rc("c", map("baz", "baz"))))
                .isEqualTo(r("b, c", map("bar", r("b", "bar"), "baz", r("c", "baz"))));

        assertThat(resolve(rc("a", map("foo", "foo")), rc("b", map("onConflict", "reset", "value", map("bar", "bar"))),
                rc("c", map("baz", "baz"))))
                .isEqualTo(r("b, c", map("bar", r("b", "bar"), "baz", r("c", "baz"))));

        assertThat(resolve(rc("a", map("foo", map("hello", "hello"), "bar", "42")),
                rc("b", map("onConflict", "reset", "foo", map("onConflict", "extend", "world", "world")))))
                .isEqualTo(r("b", map("foo", r("a, b", map("hello", r("a", "hello"), "world", r("b", "world"))))));

        assertThat(resolve(rc("a", map("foo", map("hello", "hello"), "bar", "42")), rc("b", "42"),
                rc("c", map("onConflict", "reset", "foo", map("onConflict", "extend", "world", "world")))))
                .isEqualTo(r("c", map("foo", r("c", map("world", r("c", "world"))))));

        assertThat(resolve(rc("a", map("foo", map("hello", "hello"), "bar", "42")), rc("b", Syml.UNDEFINED),
                rc("c", map("onConflict", "reset", "foo", map("onConflict", "extend", "world", "world")))))
                .isEqualTo(r("c", map("foo", r("a, c", map("hello", r("a", "hello"), "world", r("c", "world"))))));
    }

    private static ConfigUtils.Resolved resolve(Object[]... rcFiles) {
        return ConfigUtils.resolveRcFiles(new ArrayList<>(Arrays.asList(rcFiles)));
    }

    private static Object[] rc(String id, Object data) {
        return new Object[]{id, data};
    }

    private static ConfigUtils.Resolved r(String source, Object value) {
        return new ConfigUtils.Resolved(source, value);
    }

    private static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            out.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return out;
    }
}
