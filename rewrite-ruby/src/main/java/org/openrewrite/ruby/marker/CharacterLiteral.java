/*
 * Copyright 2025 the original author or authors.
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
 * A {@link J.Literal} written in the delimiter-less character form, {@code ?A}. Since Ruby 1.9 it
 * is an ordinary one-character string, and only this marker tells it apart from any other literal
 * whose source happens to begin with a {@code ?}.
 */
@Value
@With
public class CharacterLiteral implements Marker {
    UUID id;
}
