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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class RandomizeIdTest {

    @Test
    void assignNewIdsWithoutChangingPrintedSource() {
        // given
        String source = """
          class Test {
              int a = 1;
          }
          """;
        J.CompilationUnit before = JavaParser.fromJavaVersion().build()
          .parse(source)
          .map(J.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();
        Set<UUID> beforeIds = ids(before);

        // when
        List<Result> results = new RandomizeId()
          .run(new InMemoryLargeSourceSet(List.of(before)), new InMemoryExecutionContext(t -> {
              throw new AssertionError(t);
          }))
          .getChangeset()
          .getAllResults();

        // then
        assertThat(results).hasSize(1);
        SourceFile after = results.getFirst().getAfter();
        assertThat(after).isNotNull();
        assertThat(after.printAll()).isEqualTo(source);
        assertThat(ids(after)).hasSameSizeAs(beforeIds).doesNotContainAnyElementsOf(beforeIds);
    }

    private static Set<UUID> ids(SourceFile sourceFile) {
        Set<UUID> ids = ConcurrentHashMap.newKeySet();
        new JavaIsoVisitor<Set<UUID>>() {
            @Override
            public J preVisit(J tree, Set<UUID> acc) {
                acc.add(tree.getId());
                return tree;
            }
        }.visit(sourceFile, ids);
        return ids;
    }
}
