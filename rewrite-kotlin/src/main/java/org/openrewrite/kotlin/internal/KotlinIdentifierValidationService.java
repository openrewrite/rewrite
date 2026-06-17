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
package org.openrewrite.kotlin.internal;

import org.openrewrite.java.internal.JavaIdentifierValidationService;
import org.openrewrite.java.marker.Quoted;
import org.openrewrite.java.tree.J;

public class KotlinIdentifierValidationService extends JavaIdentifierValidationService {

    @Override
    protected boolean isInvalid(J.Identifier identifier) {
        // Kotlin stores backtick-quoted identifiers without the backticks and marks them with Quoted;
        // such names may contain any character.
        if (identifier.getMarkers().findFirst(Quoted.class).isPresent()) {
            return false;
        }
        return super.isInvalid(identifier);
    }
}
