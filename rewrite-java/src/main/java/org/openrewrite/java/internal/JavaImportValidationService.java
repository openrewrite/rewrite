/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.ImportValidationService;
import org.openrewrite.java.internal.template.UndeclaredTemplateReferences;
import org.openrewrite.java.tree.J;

import java.util.List;

import static java.util.Collections.emptyList;

public class JavaImportValidationService implements ImportValidationService {

    @Override
    public List<String> unimported(SourceFile sourceFile) {
        // Kotlin and Groovy import more by default, and alias imports
        return sourceFile instanceof J.CompilationUnit ?
                UnimportedReferences.of((J.CompilationUnit) sourceFile) : emptyList();
    }

    @Override
    public Recording recordUndeclared() {
        return UndeclaredTemplateReferences.record();
    }
}
