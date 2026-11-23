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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import static org.junit.jupiter.api.Assertions.*;

class SpreadMapTest {
    @Test
    void spreadMapVisitsItsValueOnce() {
        G.MapLiteral map = initializer(parse("def copy = [*: run.environment]"), 0);
        G.MapEntry entry = map.getElements().get(0);
        G.Unary spread = assertInstanceOf(G.Unary.class, entry.getKey());
        assertEquals(G.Unary.Type.Spread, spread.getOperator());
        assertInstanceOf(J.Empty.class, spread.getExpression());
        J.FieldAccess value = assertInstanceOf(J.FieldAccess.class, entry.getValue());
        assertEquals("environment", value.getSimpleName());
    }

    @Test
    void spreadValueRetainsItsType() {
        G.CompilationUnit cu = parse("Map<String, String> env = [:]\ndef copy = [*: env]");
        G.MapLiteral map = initializer(cu, 1);
        J.Identifier value = assertInstanceOf(J.Identifier.class, map.getElements().get(0).getValue());
        assertTrue(TypeUtils.isAssignableTo("java.util.Map", value.getType()), () -> String.valueOf(value.getType()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "def copy = [ * : env ]",
      "def copy = [/*before*/ * /*key*/ : /*value*/ env /*after*/]",
      "def copy = [*:\n env]",
      "def copy = [*:env,]"
    })
    void spreadMapFormatting(String source) {
        parse(source);
    }

    @Test
    void multipleSpreadEntriesAmongOrdinaryEntries() {
        G.MapLiteral map = initializer(parse("def copy = [first: 1, *: a, second: 2, *: b]"), 0);
        assertEquals(4, map.getElements().size());
        assertInstanceOf(J.Literal.class, map.getElements().get(0).getKey());
        assertInstanceOf(G.Unary.class, map.getElements().get(1).getKey());
        assertInstanceOf(J.Literal.class, map.getElements().get(2).getKey());
        assertInstanceOf(G.Unary.class, map.getElements().get(3).getKey());
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "def copy = [*: [*: env]]",
      "def copy = [*: load()]",
      "accept(*: env)",
      "def copy = [*: (env + other)]"
    })
    void spreadMapOperandsAndNamedArguments(String source) {
        parse(source);
    }

    @Test
    void anetSpreadMapDoesNotConsumeLaterCallsOrWildcardCast() {
        parse("""
          tasks.register('dbLoad') {
              def dbLoadEnvironment = [*: run.environment]
              dbLoadEnvironment["ANET_DB_DROP"] = "false"
              dbLoadEnvironment["ANET_DB_MIGRATE"] = "false"
              environment(dbLoadEnvironment)
          }
          tasks.register('dockerCreateDB') {
              hostConfig.binds.set([projectDir: "/hostdata"] + run.environment.get("DOCKER_MOUNTS", [:]) as Map<? extends String, ? extends String>)
          }
          """);
    }

    @Test
    void quotedStarRemainsAnOrdinaryKey() {
        G.MapLiteral map = initializer(parse("def copy = ['*': env]"), 0);
        assertEquals("*", assertInstanceOf(J.Literal.class, map.getElements().get(0).getKey()).getValue());
    }

    @Test
    void listSpreadRetainsItsOperand() {
        G.CompilationUnit cu = parse("def copy = [*env]");
        J.VariableDeclarations variable = (J.VariableDeclarations) cu.getStatements().get(0);
        G.ListLiteral list = assertInstanceOf(G.ListLiteral.class, variable.getVariables().get(0).getInitializer());
        G.Unary spread = assertInstanceOf(G.Unary.class, list.getElements().get(0));
        assertInstanceOf(J.Identifier.class, spread.getExpression());
    }

    @Test
    void parenthesizedMapKeyRemainsAnExpression() {
        G.MapLiteral map = initializer(parse("def copy = [(key): env]"), 0);
        assertInstanceOf(J.Parentheses.class, map.getElements().get(0).getKey());
    }

    @Test
    void mapAdditionCastHasRichWildcardTypes() {
        G.CompilationUnit cu = parse("def copy = [:] + [:] as Map<? extends String, ? super String>");
        J.VariableDeclarations variable = (J.VariableDeclarations) cu.getStatements().get(0);
        J.TypeCast cast = assertInstanceOf(J.TypeCast.class, variable.getVariables().get(0).getInitializer());
        assertInstanceOf(J.Binary.class, cast.getExpression());
        J.ParameterizedType type = assertInstanceOf(J.ParameterizedType.class, cast.getClazz().getTree());
        assertNotNull(type.getTypeParameters());
        assertEquals(J.Wildcard.Bound.Extends, assertInstanceOf(J.Wildcard.class, type.getTypeParameters().get(0)).getBound());
        assertEquals(J.Wildcard.Bound.Super, assertInstanceOf(J.Wildcard.class, type.getTypeParameters().get(1)).getBound());
    }

    private static G.MapLiteral initializer(G.CompilationUnit cu, int statement) {
        J.VariableDeclarations variable = (J.VariableDeclarations) cu.getStatements().get(statement);
        return assertInstanceOf(G.MapLiteral.class, variable.getVariables().get(0).getInitializer());
    }

    private static G.CompilationUnit parse(String source) {
        SourceFile parsed = GroovyParser.builder().build().parse(source).findFirst().orElseThrow();
        G.CompilationUnit cu = assertInstanceOf(G.CompilationUnit.class, parsed, () -> parsed.getMarkers().toString());
        assertEquals(source, cu.printAll());
        new GroovyIsoVisitor<Void>() {
            @Override
            public J.Unknown visitUnknown(J.Unknown unknown, Void unused) {
                fail("Expected rich Groovy syntax, not J.Unknown");
                return unknown;
            }
        }.visit(cu, null);
        return cu;
    }
}
