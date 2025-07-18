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
package org.openrewrite.graphql.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.With;
import org.openrewrite.marker.Markers;

@Getter
@With
@EqualsAndHashCode
public class Comment {
    private final boolean multiline;
    private final String text;
    private final String suffix;
    private final Markers markers;

    @JsonCreator
    public Comment(boolean multiline, String text, String suffix, Markers markers) {
        this.multiline = multiline;
        this.text = text;
        this.suffix = suffix;
        this.markers = markers;
    }

    public String printComment() {
        return text;
    }

    @Override
    public String toString() {
        return printComment() + suffix;
    }
}