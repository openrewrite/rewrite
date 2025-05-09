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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

class ChangeNamedStringParameterValueOfMethodInvocationTest implements RewriteTest {

    @Test
    void whenParamSet_thenChangeToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method", "param", "newValue")),
          //language=groovy
          groovy(
            """
              method(
                  param: 'oldValue'
              )
              """, """
              method(
                  param: 'newValue'
              )
              """
          )
        );
    }

    @Test
    void noChangeWhenNewValueEqualsOldValue() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method", "param", "value")),
          //language=groovy
          groovy(
            """
              method(
                  param: 'value'
              )
              """
          )
        );
    }

    @Test
    void whenSingleParamSetWithGString_thenChangeToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method", "param", "newValue")),
          //language=groovy
          groovy(
            """
              method(
                  param: "oldValue"
              )
              """, """
              method(
                  param: "newValue"
              )
              """
          )
        );
    }

    @Test
    void whenMethodWithMultipleParameters_thenChangeOnlySelectedToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method", "param2", "newValue")),
          //language=groovy
          groovy(
            """
              method(
                  param1: "oldValue",
                  param2: "oldValue",
                  param3: "oldValue"
              )
              """, """
              method(
                  param1: "oldValue",
                  param2: "newValue",
                  param3: "oldValue"
              )
              """
          )
        );
    }

    @Test
    void whenTwoMethods_thenOnlyChangeOne() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method2", "param", "newValue")),
          groovy(
            """
              method1(
                param: "oldValue"
              )
              method2(
                param: "oldValue"
              )
              """, """
              method1(
                param: "oldValue"
              )
              method2(
                param: "newValue"
              )
              """
          )
        );
    }

    @Test
    void noChangeWhenParameterWithOtherDatatype() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method", "param", "newValue")),
          groovy(
            """
              method(
                param: 1
              )
              """
          )
        );
    }

    @Test
    void noChangeWhenMethodWithoutParameters() {
        rewriteRun(spec -> spec.recipe(new ChangeStringValueOfNamedParameterInMethodInvocation("method2", "param", "newValue")),
          groovy(
            """
              method()
              """
          )
        );
    }
}
