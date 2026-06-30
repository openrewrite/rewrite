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
package org.openrewrite.yaml.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

/**
 * Direct unit tests for {@link BlockScalarUtils}, isolating {@code getBody}/{@code withBody}
 * from the recipe round-trip. The recipe path calls {@code getBody} then {@code withBody}, so a
 * stray {@code \r} leaked by {@code getBody} can be re-absorbed by {@code withBody} and mask a
 * line-ending bug end-to-end; testing each method in isolation prevents that masking.
 */
class BlockScalarUtilsTest {

    private static Yaml.Scalar literal(String value) {
        return new Yaml.Scalar(randomId(), "", Markers.EMPTY, Yaml.Scalar.Style.LITERAL, null, null, value);
    }

    @Test
    void getBodyStripsCrFromLfBody() {
        // header "\n", two indented lines, trailing "\n"
        Yaml.Scalar s = literal("\n  line one\n  line two\n");
        assertThat(BlockScalarUtils.getBody(s)).isEqualTo("line one\nline two");
    }

    @Test
    void getBodyStripsCrFromCrlfBody() {
        // A CRLF-authored block scalar must not leak '\r' into the returned body.
        Yaml.Scalar s = literal("\r\n  line one\r\n  line two\r\n");
        assertThat(BlockScalarUtils.getBody(s)).isEqualTo("line one\nline two");
    }

    @Test
    void withBodyKeepsLfForLfScalar() {
        Yaml.Scalar s = literal("\n  line one\n  line two\n");
        Yaml.Scalar updated = BlockScalarUtils.withBody(s, "new one\nnew two");
        assertThat(updated.getValue()).isEqualTo("\n  new one\n  new two\n");
    }

    @Test
    void withBodyEmitsCrlfForCrlfScalar() {
        // Interior breaks of the new body must follow the existing CRLF convention, and the
        // preserved header/trailing must stay CRLF as well — no mixed endings.
        Yaml.Scalar s = literal("\r\n  line one\r\n  line two\r\n");
        Yaml.Scalar updated = BlockScalarUtils.withBody(s, "new one\nnew two");
        assertThat(updated.getValue()).isEqualTo("\r\n  new one\r\n  new two\r\n");
    }
}
