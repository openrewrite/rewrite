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
package org.openrewrite.scala;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.scala.internal.*;
import org.openrewrite.scala.tree.S;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

/**
 * Converts Scala AST to OpenRewrite's LST model.
 * This visitor delegates to ScalaTreeVisitor for the actual AST traversal.
 */
public class ScalaParserVisitor {
    private final Path sourcePath;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;
    private final JavaTypeCache typeCache;
    private final ExecutionContext context;

    public ScalaParserVisitor(Path sourcePath,
                              @Nullable FileAttributes fileAttributes,
                              String source,
                              Charset charset,
                              boolean charsetBomMarked,
                              JavaTypeCache typeCache,
                              ExecutionContext context) {
        this.sourcePath = sourcePath;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
        this.typeCache = typeCache;
        this.context = context;
    }

    /**
     * Entry point for converting a Scala AST to an S.CompilationUnit.
     */
    public S.CompilationUnit visitCompilationUnit(ScalaParseResult parseResult) {
        // Use the Scala AST converter to convert the parsed tree
        ScalaASTConverter converter = new ScalaASTConverter();
        CompilationUnitResult result = converter.convertToCompilationUnit(parseResult, source);
        
        J.Package packageDecl = result.getPackageDecl();
        List<J.Import> imports = result.getImports();
        List<Statement> statements = result.getStatements();
        
        
        // Filter out any Unknown statements that contain the entire source with package
        if (packageDecl != null) {
            final String packageName = packageDecl.getPackageName();
            statements = statements.stream()
                .filter(stmt -> {
                    if (stmt instanceof J.Unknown) {
                        String text = ((J.Unknown) stmt).getSource().getText().trim();
                        // Skip if this Unknown contains the same package declaration
                        boolean shouldFilter = text.startsWith("package " + packageName);
                        return !shouldFilter;
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        // Don't include empty package declarations
        if (packageDecl != null && packageDecl.getExpression() instanceof J.Identifier) {
            J.Identifier id = (J.Identifier) packageDecl.getExpression();
            if (id.getSimpleName().isEmpty() || id.getSimpleName().equals("<empty>")) {
                packageDecl = null;
            }
        }
        
        // If we didn't get any statements and have source content, create an Unknown node
        // But skip if we already have a package declaration (to avoid duplication)
        if (statements.isEmpty() && !source.trim().isEmpty() && packageDecl == null) {
            J.Unknown.Source unknownSource = new J.Unknown.Source(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                source
            );
            
            J.Unknown unknown = new J.Unknown(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                unknownSource
            );
            
            statements.add(unknown);
        }
        
        // Get remaining source for EOF
        String remainingSource = converter.getRemainingSource(parseResult, source);
        Space eof = remainingSource.isEmpty() ? EMPTY : Space.build(remainingSource, Collections.emptyList());

        return new S.CompilationUnit(
            randomId(),
            EMPTY,
            Markers.EMPTY,
            charset.name(),
            charsetBomMarked,
            null,
            sourcePath,
            null, // checksum
            packageDecl,
            imports,
            statements,
            eof
        );
    }
}