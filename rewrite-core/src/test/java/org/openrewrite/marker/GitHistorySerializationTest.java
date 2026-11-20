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
package org.openrewrite.marker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.ObjectMappers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class GitHistorySerializationTest {

    private final ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null).registerModule(new JavaTimeModule());

    private final GitHistory.Author sam = new GitHistory.Author("Sam", "sam@example.com");
    private final GitHistory.Author alex = new GitHistory.Author("Alex", "alex@example.com");

    @Test
    void authorsAreWrittenOnce() throws Exception {
        GitHistory original = new GitHistory(randomId(), List.of(sam, alex), List.of(
          new GitHistory.Commit("2627cb410b4d7f0b0f4d2a7e2a3c0e9b5a1d6c3e", sam, Instant.ofEpochSecond(1791417600)),
          new GitHistory.Commit("6937ae529c0a1b2c3d4e5f60718293a4b5c6d7e8", alex, Instant.ofEpochSecond(1791331200)),
          new GitHistory.Commit("f2b780f8041a2b3c4d5e6f708192a3b4c5d6e7f8", sam, Instant.ofEpochSecond(1467676800))));

        String json = mapper.writeValueAsString(original);
        GitHistory restored = mapper.readValue(json, GitHistory.class);

        assertThat(restored).isEqualTo(original);
        assertThat(json).containsOnlyOnce("sam@example.com");
        assertThat(restored.getCommits().get(0).getAuthor())
          .isSameAs(restored.getCommits().get(2).getAuthor())
          .isSameAs(restored.getAuthors().get(0));
    }

    @Test
    void fileTouchedByOneAuthorTwice() throws Exception {
        GitFileHistory original = new GitFileHistory(randomId(),
          new GitHistory.Commit("f2b780f8041a2b3c4d5e6f708192a3b4c5d6e7f8", sam, Instant.ofEpochSecond(1467676800)),
          new GitHistory.Commit("2627cb410b4d7f0b0f4d2a7e2a3c0e9b5a1d6c3e", sam, Instant.ofEpochSecond(1791417600)), 2, 1, null);

        String json = mapper.writeValueAsString(original);
        GitFileHistory restored = mapper.readValue(json, GitFileHistory.class);

        assertThat(restored).isEqualTo(original);
        assertThat(json).containsOnlyOnce("sam@example.com");
        assertThat(restored.getLastCommit().getAuthor()).isSameAs(restored.getFirstCommit().getAuthor());
    }

    @Test
    void linesLastChangedByEachCommit() throws Exception {
        GitHistory.Commit first = new GitHistory.Commit("f2b780f8041a2b3c4d5e6f708192a3b4c5d6e7f8", sam, Instant.ofEpochSecond(1467676800));
        GitHistory.Commit last = new GitHistory.Commit("6937ae529c0a1b2c3d4e5f60718293a4b5c6d7e8", alex, Instant.ofEpochSecond(1791331200));
        GitFileHistory original = new GitFileHistory(randomId(), first, last, 2, 2, List.of(
          new GitFileHistory.Lines(3, first),
          new GitFileHistory.Lines(1, last),
          new GitFileHistory.Lines(5, first)));

        String json = mapper.writeValueAsString(original);
        GitFileHistory restored = mapper.readValue(json, GitFileHistory.class);

        assertThat(restored).isEqualTo(original);
        assertThat(json).containsOnlyOnce("alex@example.com").containsOnlyOnce(first.getId());
        assertThat(restored.getBlame()).extracting(GitFileHistory.Lines::getCount).containsExactly(3, 1, 5);
        assertThat(restored.getBlame().get(2).getCommit()).isSameAs(restored.getFirstCommit());
    }
}
