/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.table.DistinctCommitters;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.text;

class FindCommittersTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindCommitters(null));
    }

    @Test
    void findCommitters() {
        GitProvenance git = new GitProvenance(
          randomId(), "github.com", "main", "123", null, null,
          List.of(new GitProvenance.Committer("Jon", "jkschneider@gmail.com",
            new TreeMap<>() {{
                put(LocalDate.now().minusDays(5), 5);
                put(LocalDate.now(), 5);
            }}))
        );

        rewriteRun(
          spec -> spec.dataTable(DistinctCommitters.Row.class, rows -> {
              assertThat(rows).satisfiesExactly(
                r -> assertThat(r.getEmail()).isEqualTo("jkschneider@gmail.com")
              );
          }),
          text(
            "hi",
            spec -> spec.mapBeforeRecipe(pt -> pt.withMarkers(pt.getMarkers().add(git)))
          )
        );
    }

    @Test
    void findCommittersFromDate() {
        GitProvenance git = new GitProvenance(
          randomId(), "github.com", "main", "123", null, null,
          List.of(new GitProvenance.Committer("Jon", "jkschneider@gmail.com",
              new TreeMap<>() {{
                  put(LocalDate.of(2023, 1, 9), 5);
                  put(LocalDate.of(2023, 1, 1), 5);
              }}),
            new GitProvenance.Committer("Peter", "p.streef@gmail.com",
              new TreeMap<>() {{
                  put(LocalDate.of(2023, 1, 10), 5);
              }}))
        );

        rewriteRun(
          spec -> spec.recipe(new FindCommitters("2023-01-10"))
            .dataTable(DistinctCommitters.Row.class, rows -> {
                assertThat(rows).satisfiesExactly(
                  r -> assertThat(r.getEmail()).isEqualTo("p.streef@gmail.com")
                );
            }),
          text(
            "hi",
            spec -> spec.mapBeforeRecipe(pt -> pt.withMarkers(pt.getMarkers().add(git)))
          )
        );
    }

    @Test
    void findCommittersFromDateEmpty() {
        GitProvenance git = new GitProvenance(
          randomId(), "github.com", "main", "123", null, null,
          List.of(new GitProvenance.Committer("Jon", "jkschneider@gmail.com",
              new TreeMap<>() {{
                  put(LocalDate.of(2023, 1, 9), 5);
                  put(LocalDate.of(2023, 1, 1), 5);
              }}),
            new GitProvenance.Committer("Peter", "p.streef@gmail.com",
              new TreeMap<>() {{
                  put(LocalDate.of(2023, 1, 10), 5);
              }}))
        );

        rewriteRun(
          spec -> spec.recipe(new FindCommitters(""))
            .dataTable(DistinctCommitters.Row.class, rows -> {
                assertThat(rows).satisfiesExactly(
                  r -> assertThat(r.getEmail()).isEqualTo("jkschneider@gmail.com"),
                  r -> assertThat(r.getEmail()).isEqualTo("p.streef@gmail.com")
                );
            }),
          text(
            "hi",
            spec -> spec.mapBeforeRecipe(pt -> pt.withMarkers(pt.getMarkers().add(git)))
          )
        );
    }
}
