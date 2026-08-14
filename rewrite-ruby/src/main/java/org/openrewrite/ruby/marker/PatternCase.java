/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * In Ruby, "in" conditions in case statements are mutually exclusive
 * "when" conditions. This marker is used to indicate that a
 * {@link J.Switch.Case} node is a case-in statement.
 */
@Value
@With
public class PatternCase implements Marker {
    UUID id;
}
