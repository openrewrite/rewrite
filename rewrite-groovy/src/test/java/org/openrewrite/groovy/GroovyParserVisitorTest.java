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

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GroovyParserVisitorTest {

    @Test
    void eraseCommentsSimple() {
        assertEquals("a       b", GroovyParserVisitor.eraseComments("a /* */ b"));
    }

    @Test
    void eraseCommentsMultiLine() {
        assertEquals(
          """
            a ____________
            ____
            ____________ b
            """.replaceAll("_", " "), // this is to prevent IntelliJ from removing trailing whitespace
          GroovyParserVisitor.eraseComments(
            """
              a /* something
              else
              then this */ b
              """)
        );
    }

    @Test
    void eraseCommentsTwoMultiLinesInALine() {
        assertEquals("1       2       3", GroovyParserVisitor.eraseComments("1 /* */ 2 /* */ 3"));
    }

    @Test
    void eraseCommentsJustAnEmptyLine() {
        assertEquals("\n", GroovyParserVisitor.eraseComments("\n"));
    }

    @Test
    void destructuringAssignmentUsesTupleExpressionDeclarator() {
        List<G.CompilationUnit> cus = GroovyParser.builder().build()
          .parse(new InMemoryExecutionContext(), "def (a, b, c) = [1, 2, 3]")
          .map(G.CompilationUnit.class::cast)
          .toList();
        assertEquals(1, cus.size());

        List<? extends Statement> statements = cus.get(0).getStatements();
        assertEquals(1, statements.size());
        assertInstanceOf(J.VariableDeclarations.class, statements.get(0));

        J.VariableDeclarations varDecls = (J.VariableDeclarations) statements.get(0);
        assertEquals(1, varDecls.getVariables().size(),
          "Should have a single NamedVariable with TupleExpression declarator");

        J.VariableDeclarations.NamedVariable namedVar = varDecls.getVariables().get(0);
        assertInstanceOf(G.TupleExpression.class, namedVar.getDeclarator(),
          "Declarator should be a TupleExpression");

        G.TupleExpression tuple = (G.TupleExpression) namedVar.getDeclarator();
        assertEquals(3, tuple.getVariables().size());
        assertInstanceOf(J.VariableDeclarations.class, tuple.getVariables().get(0));
        assertEquals("a", tuple.getVariables().get(0).getVariables().get(0).getSimpleName());
        assertEquals("b", tuple.getVariables().get(1).getVariables().get(0).getSimpleName());
        assertEquals("c", tuple.getVariables().get(2).getVariables().get(0).getSimpleName());

        assertNotNull(namedVar.getInitializer(),
          "Initializer should be on the NamedVariable");
    }

}
