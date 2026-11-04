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
 * A {@link J.Case} that destructures a pattern rather than comparing with {@code ===}, which is
 * what {@code when} does. The operator says how it is spelled; a case with no {@code PatternCase}
 * is a {@code when}.
 */
@Value
@With
public class PatternCase implements Marker {
    UUID id;

    Operator operator;

    public enum Operator {
        /**
         * {@code in [x, y]}, both as a {@code case} clause and as a standalone boolean check.
         */
        In,

        /**
         * {@code config => {db:}}, the rightward assignment that spells the same match inline.
         */
        Rightward
    }
}
