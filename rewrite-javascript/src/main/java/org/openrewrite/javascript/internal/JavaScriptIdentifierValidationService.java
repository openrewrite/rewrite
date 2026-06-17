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
package org.openrewrite.javascript.internal;

import org.openrewrite.java.internal.JavaIdentifierValidationService;

public class JavaScriptIdentifierValidationService extends JavaIdentifierValidationService {

    @Override
    protected boolean isValidChar(char c) {
        // ECMAScript identifiers permit letters, digits, '_', '$' and (for private class members) a leading '#'.
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '#';
    }
}
