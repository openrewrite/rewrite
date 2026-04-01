/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon"})
class AssignmentTest implements RewriteTest {

    @Test
    void noKeyword() {
        rewriteRun(
          groovy(
            """
              x = "s"
              """
          )
        );
    }

    @Test
    void simple() {
        rewriteRun(
          groovy(
            """
              def x = "s"
              """
          )
        );
    }

    @Test
    void simpleWithFinal() {
        rewriteRun(
          groovy(
            """
              final def x = "x"
              def final y = "y"
              final z = "z"
              """
          )
        );
    }

    @Test
    void concat() {
        rewriteRun(
          groovy(
            """
              android {
                  // specify the artifactId as module-name for kotlin
                  kotlinOptions.freeCompilerArgs += ["-module-name", POM_ARTIFACT_ID]
              }
              """
          )
        );
    }

    @Test
    void assignment() {
        rewriteRun(
          groovy(
            """
              String s;
              s = "foo";
              """
          )
        );
    }

    @Test
    void classAssignment() {
        rewriteRun(
          groovy(
            """
              def s = String
              """
          )
        );
    }

    @Test
    void classAssignmentJavaStyle() {
        rewriteRun(
          groovy(
            """
              def s = String.class
              """
          )
        );
    }

    @Test
    void unaryMinus() {
        rewriteRun(
          groovy(
            """
              def i = -1
              def l = -1L
              def f = -1.0f
              def d = -1.0d
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1522")
    @Test
    void unaryPlus() {
        rewriteRun(
          groovy(
            """
              int k = +10
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1533")
    @Test
    void baseNConversions() {
        rewriteRun(
          groovy(
            """
              def a = 01
              def b = 001
              def c = 0001
              def d = 00001
              def e = 000001
              """
          )
        );
    }

    @Test
    void multipleAssignmentsAtOneLine() {
        rewriteRun(
          groovy(
            """
              def startItem = '|  ', endItem = '  |'
              def repeatLength = startItem.length() + output.length() + endItem.length()
              println("\\n" + ("-" * repeatLength) + "\\n|  " + startItem + output + endItem + "  |\\n" + ("-" * repeatLength))
              """
          )
        );
    }

    @Test
    void multipleAssignmentsAtOneLineSimple() {
        rewriteRun(
          groovy(
            """
              def a = '1', b = '2'
              """
          )
        );
    }

    @Test
    void multipleAssignmentsAtMultipleLineDynamicType() {
        rewriteRun(
          groovy(
            """
               def a = '1'    ,
                  b = '2'
              """
          )
        );
    }

    @Test
    void multipleAssignmentsAtMultipleLineStaticType() {
        rewriteRun(
          groovy(
            """
               String a = '1'    ,
                  b = '2'
              """
          )
        );
    }

    @Test
    void staticTypeWithTypeAsPrefix() {
        rewriteRun(
          groovy(
            """
            int a = 1    ,
            intB = 2
            """
          ));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void destructuringAssignment() {
        rewriteRun(
          groovy(
            """
              def (a, b, c) = [1, 2, 3]
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Issue("https://github.com/openrewrite/rewrite/issues/4756")
    @Test
    void destructuringAssignmentTwoVariables() {
        rewriteRun(
          groovy(
            """
              def (x, y) = [1, 2]
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Issue("https://github.com/openrewrite/rewrite/issues/5283")
    @Test
    void destructuringAssignmentWithMethodCall() {
        rewriteRun(
          groovy(
            """
              def tag = '1.2.3'
              def (major, minor, patch) = tag.tokenize('.')
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void destructuringAssignmentWithExtraSpaces() {
        rewriteRun(
          groovy(
            """
              def ( a , b , c ) = [1, 2, 3]
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void destructuringAssignmentNoSpaces() {
        rewriteRun(
          groovy(
            """
              def (a,b,c) = [1,2,3]
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void destructuringAssignmentNoSpaceBeforeParen() {
        rewriteRun(
          groovy(
            """
              def(key,value) = "a1:b2".split(":")
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void destructuringAssignmentWithTypedVariables() {
        rewriteRun(
          groovy(
            """
              def (String key, String value) = "a:b".split(":")
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1478")
    @Test
    void destructuringAssignmentWithMixedTypes() {
        rewriteRun(
          groovy(
            """
              def (String name, Integer age) = ["Alice", 30]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5293")
    @Test
    void destructuringAssignmentWithFullyQualifiedType() {
        rewriteRun(
          groovy(
            """
              def (String key, java.lang.String value) = "a1:b2".split(":")
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5293")
    @Test
    void destructuringAssignmentWithMixedTypedAndUntyped() {
        rewriteRun(
          groovy(
            """
              def  (first,  int           second,   third)   =     [1, 2, 3]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/5293")
    @Test
    void destructuringAssignmentNoSpacesAroundEquals() {
        rewriteRun(
          groovy(
            """
              def(key,value)="a1:b2".split(":")
              """
          )
        );
    }

}
