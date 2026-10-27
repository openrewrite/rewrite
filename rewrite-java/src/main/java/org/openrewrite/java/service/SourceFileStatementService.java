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
package org.openrewrite.java.service;

import org.openrewrite.Incubating;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * The statements a source file holds directly, for languages whose scripts have no enclosing {@link
 * org.openrewrite.java.tree.J.Block}. Everything that edits a statement list otherwise works through the block that
 * contains it, so this is the only way to reach a Groovy script's top-level statements from the Java layer.
 */
@Incubating(since = "8.92.0")
public class SourceFileStatementService {

    /**
     * Empty when the file keeps its statements inside a block, which is the shape the rest of the Java family uses.
     */
    public List<Statement> getStatements(JavaSourceFile cu) {
        return emptyList();
    }

    public JavaSourceFile withStatements(JavaSourceFile cu, List<Statement> statements) {
        throw new UnsupportedOperationException(cu.getClass().getName() + " does not hold statements directly.");
    }
}
