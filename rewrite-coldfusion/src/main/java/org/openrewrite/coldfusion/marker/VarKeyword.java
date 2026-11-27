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
 * A {@code var} written where a {@code J.VariableDeclarations} cannot hold it: before an assignment
 * to a member, {@code var local.x = 1}, or inside an expression, {@code while ((var n = next()) > 0)}, on
 * a {@code J.Assignment}; between the type and the name of a catch variable, {@code catch (any var e)},
 * on its {@code NamedVariable}.
 */
@Value
@With
public class VarKeyword implements Marker {
    UUID id;
    String keyword;
}
