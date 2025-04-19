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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddExplicitImport extends Recipe {
    @Option(displayName = "List of imports to add",
            description = "The list of imports, this field is multiline.",
            example = "foo.bar\nbiz.baz")
    String imports;

    @Override
    public String getDisplayName() {
        return "Add explicit imports";
    }

    @Override
    public int maxCycles() {
        return 1;
    }
    @Override
    public String getDescription() {
        return "This recipe adds an explicit import to a single Java file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(true, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (javaSourceFile != null) {
                    addExplicitImport(imports);
                }
                return super.visitCompilationUnit(cu, executionContext);
            }
        });
    }
}
