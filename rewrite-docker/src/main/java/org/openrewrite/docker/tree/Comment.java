/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.docker.tree;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Markers;

/**
 * Represents a comment in a Dockerfile.
 * Comments in Dockerfile start with # and continue to the end of the line.
 */
@Value
@With
public class Comment {
    /**
     * The text of the comment, including the leading #
     */
    String text;

    /**
     * Whitespace that precedes this comment
     */
    String prefix;

    Markers markers;
}
