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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The commits reachable from HEAD when the LST was built, and who authored them.
 */
@Value
@With
public class GitHistory implements Marker {
    UUID id;

    List<Author> authors;

    /**
     * Newest first, in the order a walk from HEAD yields them.
     */
    List<Commit> commits;

    /**
     * Written once per marker and referred to after that, since a file's line history names the
     * same commits many times.
     */
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    public static class Commit {
        String id;

        Author author;

        Instant authorTime;
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    public static class Author {
        String name;
        String email;
    }
}
