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
package org.openrewrite.python.internal.poetrylock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.internal.poetrylock.PoetryLockFixtures.resource;

class PoetryContentHashTest {

    @ParameterizedTest
    @ValueSource(strings = {"a-minimal", "b-graph", "c-modern", "d-extras", "e-groups", "f-markers", "g-git"})
    void reproducesPoetryContentHash(String scenario) {
        assertHashMatches(scenario + "/pyproject.toml", scenario + "/poetry.lock");
    }

    @Test
    void upgradePairHashes() {
        assertHashMatches("i-upgrade/pyproject.toml.before", "i-upgrade/poetry.lock.before");
        assertHashMatches("i-upgrade/pyproject.toml.after", "i-upgrade/poetry.lock.after");
    }

    private void assertHashMatches(String pyprojectResource, String lockResource) {
        Toml.Document pyproject = parseToml(resource(pyprojectResource));
        String expected = PoetryLockReader.parse(resource(lockResource)).getContentHash();
        assertThat(PoetryContentHash.hash(pyproject)).as(pyprojectResource).isEqualTo(expected);
    }

    private static Toml.Document parseToml(String content) {
        SourceFile parsed = new TomlParser().parse(new InMemoryExecutionContext(), content)
                .findFirst().orElseThrow(() -> new IllegalStateException("not TOML"));
        return (Toml.Document) parsed;
    }
}
