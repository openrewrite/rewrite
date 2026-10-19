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
package org.openrewrite.scala.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * Marks an import, or a selector of an import, that imports an implicit or {@code given}
 * definition by name, such as {@code import scala.concurrent.ExecutionContext.Implicits.global}.
 * Such an import may be in use without any reference to the imported name in the source;
 * see {@link ImplicitArguments} and {@link ImplicitConversion}.
 */
@Value
@With
public class ImplicitImport implements Marker {
    UUID id;

    public ImplicitImport(UUID id) {
        this.id = id;
    }
}
