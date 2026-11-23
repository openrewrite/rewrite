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
package org.openrewrite.yaml.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class BlockScalarTest {

    private static BlockScalar of(String value) {
        Yaml.Scalar scalar = new Yaml.Scalar(randomId(), "", Markers.EMPTY, Yaml.Scalar.Style.LITERAL, null, null, value);
        return new BlockScalar.Matcher().get(new Cursor(null, scalar)).orElseThrow();
    }

    @Test
    void matcherRejectsPlainScalar() {
        Yaml.Scalar plain = new Yaml.Scalar(randomId(), "", Markers.EMPTY, Yaml.Scalar.Style.PLAIN, null, null, "hello");
        assertThat(new BlockScalar.Matcher().get(new Cursor(null, plain))).isEmpty();
    }

    @Test
    void getBodyStripsCrFromLfBody() {
        assertThat(of("\n  line one\n  line two\n").getBody()).isEqualTo("line one\nline two");
    }

    @Test
    void getBodyStripsCrFromCrlfBody() {
        assertThat(of("\r\n  line one\r\n  line two\r\n").getBody()).isEqualTo("line one\nline two");
    }

    @Test
    void withBodyKeepsLfForLfScalar() {
        assertThat(of("\n  line one\n  line two\n").withBody("new one\nnew two").getValue())
                .isEqualTo("\n  new one\n  new two\n");
    }

    @Test
    void withBodyEmitsCrlfForCrlfScalar() {
        assertThat(of("\r\n  line one\r\n  line two\r\n").withBody("new one\nnew two").getValue())
                .isEqualTo("\r\n  new one\r\n  new two\r\n");
    }
}
