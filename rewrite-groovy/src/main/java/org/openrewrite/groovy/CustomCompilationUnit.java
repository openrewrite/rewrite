/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.groovy;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.jspecify.annotations.Nullable;

import java.security.CodeSource;

// Override `addPhaseOperation()` so that we can skip the `StaticImportVisitor` operation
public class CustomCompilationUnit extends CompilationUnit {

    private static final int STATIC_IMPORT_OPERATION = 0;

    private int semanticAnalysisOperation = 0;

    public CustomCompilationUnit(CompilerConfiguration configuration, @Nullable CodeSource codeSource,
                                 GroovyClassLoader loader, GroovyClassLoader transformLoader) {
        super(configuration, codeSource, loader, transformLoader);
    }

    @Override
    public void addPhaseOperation(IPrimaryClassNodeOperation op, int phase) {
        if (phase == Phases.SEMANTIC_ANALYSIS) {
            if (semanticAnalysisOperation++ == STATIC_IMPORT_OPERATION) {
                // we don't want to register the `StaticImportVisitor` operation
                return;
            }
        }
        super.addPhaseOperation(op, phase);
    }
}
