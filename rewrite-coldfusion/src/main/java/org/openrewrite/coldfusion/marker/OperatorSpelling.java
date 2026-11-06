/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * An operator as written when it is not the symbol its type prints: {@code EQ}, {@code is not},
 * {@code GREATER THAN OR EQUAL TO}, {@code <>} or {@code NOT} on a {@code J.Binary} or {@code J.Unary},
 * and every spelled-out {@code Cf.Binary} operator.
 */
@Value
@With
public class OperatorSpelling implements Marker {
    UUID id;
    String text;
}
