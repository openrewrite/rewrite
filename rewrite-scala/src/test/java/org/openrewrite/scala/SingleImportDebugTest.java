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
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.scala.tree.S;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class SingleImportDebugTest {

    @Test
    void debugSingleImport() {
        String source = "import scala.collection.mutable";
        System.out.println("=== Parsing: " + source + " ===");
        
        ScalaParser parser = ScalaParser.builder().build();
        List<? extends SourceFile> trees = parser.parse(source).collect(Collectors.toList());
        
        assertThat(trees).hasSize(1);
        assertThat(trees.get(0)).isInstanceOf(S.CompilationUnit.class);
        
        if (trees.get(0) instanceof S.CompilationUnit) {
            S.CompilationUnit cu = (S.CompilationUnit) trees.get(0);
            System.out.println("Package: " + cu.getPackageDeclaration());
            System.out.println("Number of imports: " + cu.getImports().size());
            System.out.println("Number of statements: " + cu.getStatements().size());
            
            for (int i = 0; i < cu.getImports().size(); i++) {
                J.Import imp = cu.getImports().get(i);
                System.out.println("Import " + i + ": " + imp);
                System.out.println("  Prefix: '" + imp.getPrefix().getWhitespace() + "'");
                System.out.println("  Qualid: " + imp.getQualid());
                System.out.println("  Printed: '" + imp.printTrimmed() + "'");
            }
            
            for (int i = 0; i < cu.getStatements().size(); i++) {
                System.out.println("Statement " + i + ": " + cu.getStatements().get(i).getClass().getSimpleName());
                if (cu.getStatements().get(i) instanceof J.Unknown) {
                    J.Unknown unk = (J.Unknown) cu.getStatements().get(i);
                    System.out.println("  Unknown text: '" + unk.getSource().getText() + "'");
                }
            }
            
            String printed = cu.printTrimmed();
            System.out.println("\nPrinted output:\n'" + printed + "'");
            
            assertThat(printed).isEqualTo(source);
        } else {
            System.out.println("ERROR: Not a compilation unit: " + trees.get(0).getClass());
            fail("Expected S.CompilationUnit but got " + trees.get(0).getClass());
        }
    }
}