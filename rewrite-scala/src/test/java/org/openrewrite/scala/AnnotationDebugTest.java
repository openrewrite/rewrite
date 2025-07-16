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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.util.List;

import static org.openrewrite.scala.Assertions.scala;

public class AnnotationDebugTest implements RewriteTest {
    
    @Test
    void debugAnnotation() {
        rewriteRun(
            spec -> spec.afterRecipe(run -> {
                var cu = run.getChangeset().getAllResults().get(0).getAfter();
                if (cu instanceof ParseError) {
                    ParseError pe = (ParseError) cu;
                    System.out.println("Parse error: " + pe.getText());
                } else if (cu instanceof S.CompilationUnit) {
                    S.CompilationUnit scu = (S.CompilationUnit) cu;
                    System.out.println("\nScala compilation unit statements:");
                    for (int i = 0; i < scu.getStatements().size(); i++) {
                        var stmt = scu.getStatements().get(i);
                        System.out.println("Statement " + i + ": " + stmt.getClass().getSimpleName());
                        if (stmt instanceof J.ClassDeclaration) {
                            J.ClassDeclaration cd = (J.ClassDeclaration) stmt;
                            System.out.println("  - Name: " + cd.getSimpleName());
                            System.out.println("  - Leading annotations: " + cd.getLeadingAnnotations().size());
                            System.out.println("  - All annotations: " + cd.getAllAnnotations().size());
                            System.out.println("  - Prefix: '" + cd.getPrefix().getWhitespace() + "'");
                            System.out.println("  - Modifiers: " + cd.getModifiers());
                            for (J.Modifier mod : cd.getModifiers()) {
                                System.out.println("    - Modifier: " + mod.getType() + " keyword: " + mod.getKeyword());
                            }
                        } else if (stmt instanceof J.Unknown) {
                            J.Unknown unknown = (J.Unknown) stmt;
                            System.out.println("  - Source: " + unknown.getSource().getText());
                        }
                    }
                }
            }),
            scala(
                """
                @deprecated
                class OldClass {
                }
                """
            )
        );
    }
    
    @Test
    void testScalaCompilerParsing() {
        // This test is to investigate what the Scala compiler produces
        String source = "@deprecated\nclass OldClass {\n}";
        
        // Let's see what the Scala compiler produces
        System.out.println("Testing Scala compiler parsing of:\n" + source);
        
        // We'll add a custom parser visitor to inspect the untpd tree
        rewriteRun(
            scala(source)
        );
    }
}