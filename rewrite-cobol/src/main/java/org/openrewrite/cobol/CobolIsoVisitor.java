/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.cobol;

import org.openrewrite.cobol.tree.Cobol;

public class CobolIsoVisitor<P> extends CobolVisitor<P> {

    @Override
    public Cobol.CompilationUnit visitDocument(Cobol.CompilationUnit compilationUnit, P p) {
        return (Cobol.CompilationUnit) super.visitDocument(compilationUnit, p);
    }

    @Override
    public Cobol.Empty visitEmpty(Cobol.Empty empty, P p) {
        return (Cobol.Empty) super.visitEmpty(empty, p);
    }
}
