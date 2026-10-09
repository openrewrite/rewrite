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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.Spec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;

/**
 * Port of @npmcli/arborist test/spec-from-lock.js; each snapshot is what arborist answers from the
 * working directory {@code /} (npm-arborist/generate/spec-from-lock-cases.js).
 */
class SpecFromLockTest {

    static Stream<Arguments> cases() {
        List<Arguments> cases = new ArrayList<>();
        for (JsonNode c : fixture("spec-from-lock-cases")) {
            cases.add(Arguments.of(c.get("about").asText(), c.get("name").asText(), c.get("lock"), c.get("expected")));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void specFromLock(String about, String name, JsonNode lock, JsonNode expected) {
        Spec spec = SpecFromLock.specFromLock(name, json(lock), null);
        if (expected.isNull()) {
            assertThat(spec).isNull();
            return;
        }
        assertThat(spec).isNotNull();
        assertThat(String.valueOf(spec.getType())).as("type").isEqualTo(expected.get("type").asText());
        assertThat(spec.isRegistry()).as("registry").isEqualTo(expected.get("registry").asBoolean());
        assertThat(spec.getWhere()).as("where").isEqualTo(text(expected, "where"));
        assertThat(spec.getRaw()).as("raw").isEqualTo(text(expected, "raw"));
        assertThat(spec.getName()).as("name").isEqualTo(text(expected, "name"));
        assertThat(spec.getEscapedName()).as("escapedName").isEqualTo(text(expected, "escapedName"));
        assertThat(spec.getScope()).as("scope").isEqualTo(text(expected, "scope"));
        assertThat(spec.getRawSpec()).as("rawSpec").isEqualTo(text(expected, "rawSpec"));
        assertThat(spec.getSaveSpec()).as("saveSpec").isEqualTo(text(expected, "saveSpec"));
        assertThat(spec.getFetchSpec()).as("fetchSpec").isEqualTo(text(expected, "fetchSpec"));
        assertThat(spec.getGitRange()).as("gitRange").isEqualTo(text(expected, "gitRange"));
        assertThat(spec.getGitCommittish()).as("gitCommittish").isEqualTo(text(expected, "gitCommittish"));
        assertThat(spec.getGitSubdir()).as("gitSubdir").isEqualTo(text(expected, "gitSubdir"));
        assertThat(spec.getHosted() == null ? null : spec.getHosted().toString()).as("hosted")
          .isEqualTo(text(expected, "hosted"));
    }

    private static @Nullable String text(JsonNode expected, String field) {
        return expected.get(field).isNull() ? null : expected.get(field).asText();
    }
}
