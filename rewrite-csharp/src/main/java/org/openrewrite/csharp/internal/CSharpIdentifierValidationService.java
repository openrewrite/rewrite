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
package org.openrewrite.csharp.internal;

import org.openrewrite.java.internal.JavaIdentifierValidationService;

public class CSharpIdentifierValidationService extends JavaIdentifierValidationService {

    @Override
    protected boolean isValidChar(char c) {
        // C# identifiers permit letters, digits and '_'. A verbatim identifier may be prefixed with '@' (e.g. @class).
        return Character.isLetterOrDigit(c) || c == '_' || c == '@';
    }
}
