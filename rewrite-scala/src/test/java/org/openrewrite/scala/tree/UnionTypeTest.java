/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

/**
 * Union (`A | B`) and operator-form intersection (`A & B`) types are parsed as an
 * {@code untpd.InfixOp}. These must round-trip in every type position rather than being
 * silently dropped (the original report: a union literal type on a method parameter).
 */
class UnionTypeTest implements RewriteTest {

    @Test
    void methodParameterUnionLiteralType() {
        rewriteRun(
          scala("def display(operation: \"resize\" | \"thumbnail\") = 1\n")
        );
    }

    @Test
    void methodReturnType() {
        rewriteRun(
          scala("def f: Int | String = ???\n")
        );
    }

    @Test
    void valType() {
        rewriteRun(
          scala("val x: Int | String = ???\n")
        );
    }

    @Test
    void constructorParameter() {
        rewriteRun(
          scala("class C(x: Int | String)\n")
        );
    }

    @Test
    void caseClassParameter() {
        rewriteRun(
          scala("case class C(x: Int | String)\n")
        );
    }

    @Test
    void typeAlias() {
        rewriteRun(
          scala("type T = Int | String\n")
        );
    }

    @Test
    void typeAscription() {
        rewriteRun(
          scala("val y = (1: Int | String)\n")
        );
    }

    @Test
    void typeParameterBound() {
        rewriteRun(
          scala("def f[T <: Int | String](x: T) = x\n")
        );
    }

    @Test
    void lambdaParameter() {
        rewriteRun(
          scala("val f = (x: Int | String) => x\n")
        );
    }

    @Test
    void functionTypeComponent() {
        rewriteRun(
          scala("val f: (Int | String) => Int = ???\n")
        );
    }

    @Test
    void tupleTypeElement() {
        rewriteRun(
          scala("val t: (Int | String, Long) = ???\n")
        );
    }

    @Test
    void genericTypeArgument() {
        rewriteRun(
          scala("val l: List[Int | String] = ???\n")
        );
    }

    @Test
    void multiWayUnion() {
        rewriteRun(
          scala("def f(x: Int | String | Long) = 1\n")
        );
    }

    @Test
    void intersectionOperatorParameter() {
        rewriteRun(
          scala("def f(x: Int & String) = 1\n")
        );
    }
}
