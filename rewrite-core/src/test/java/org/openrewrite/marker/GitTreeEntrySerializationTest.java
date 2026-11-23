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
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.ObjectMappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class GitTreeEntrySerializationTest {

    private final ObjectMapper mapper = ObjectMappers.propertyBasedMapper(null);

    @Test
    void roundTrip() throws Exception {
        GitTreeEntry original = new GitTreeEntry(randomId(), "06085efc592f6851a3f54f502f1a270db233ebf0", 0100644,
          GitTreeEntry.WorkingTreeMatch.CRLF);

        String json = mapper.writeValueAsString(original);

        assertThat(mapper.readValue(json, GitTreeEntry.class)).isEqualTo(original);
    }

    /**
     * An entry written before {@link GitTreeEntry#workingTreeMatch} existed says nothing about how the bytes
     * compared, which is what null means.
     */
    @Test
    void anEntryWrittenBeforeWorkingTreeMatchExisted() throws Exception {
        String json = "{" +
                      "\"@c\":\"org.openrewrite.marker.GitTreeEntry\"," +
                      "\"@ref\":1," +
                      "\"id\":\"11111111-1111-1111-1111-111111111111\"," +
                      "\"objectId\":\"06085efc592f6851a3f54f502f1a270db233ebf0\"," +
                      "\"fileMode\":33188" +
                      "}";

        GitTreeEntry restored = mapper.readValue(json, GitTreeEntry.class);

        assertThat(restored.getWorkingTreeMatch()).isNull();
        assertThat(restored.getId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(restored.getObjectId()).isEqualTo("06085efc592f6851a3f54f502f1a270db233ebf0");
        assertThat(restored.getFileMode()).isEqualTo(0100644);
    }
}
