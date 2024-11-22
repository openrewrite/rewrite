/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.CompileErrors;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

public class FindCompileErrors extends Recipe {

    transient CompileErrors report = new CompileErrors(this);

    @Override
    public String getDisplayName() {
        return "Find compile errors";
    }

    @Override
    public String getDescription() {
        return "Compile errors result in a particular LST structure that can be searched for.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Erroneous visitErroneous(J.Erroneous erroneous, ExecutionContext ctx) {

                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                String sourceFile = cu != null ? cu.getSourcePath().toString() : "Unknown source file";

                String code = erroneous.print();

                report.insertRow(ctx, new CompileErrors.Row(
                        sourceFile,
                        code
                ));

                return SearchResult.found(erroneous);
            }
        };
    }
}
