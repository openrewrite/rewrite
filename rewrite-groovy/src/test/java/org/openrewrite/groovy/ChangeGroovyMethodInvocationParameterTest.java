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

class ChangeGroovyMethodInvocationParameterTest implements RewriteTest {

    @Test
    void givenGroovyFile_whenParamSet_thenChangeToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("method", "param", "newValue")),
          //language=groovy
          Assertions.groovy(
                """
            method(
                            param: 'oldValue'
            )
            method(
                            param: 'newValue'
            )
                    )
            """));
    }

    @Test
    void givenGroovyFile_whenNewValueEqualsOldValue_thenChangeNothing() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("method", "param", "value")),
          //language=groovy
          Assertions.groovy(
                """
            method(
                            param: 'value'
            )
            """));
    }

    @Test
    void givenGroovyFile_whenSingleParamSetWithGString_thenChangeToNewValue() {
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("method", "param", "newValue")),
          //language=groovy
          Assertions.groovy(
                """
            method(
                            param: "oldValue"
            )
            method(
                            param: "newValue"
            )
        rewriteRun(spec -> spec.recipe(new ChangeGroovyMethodInvocationParameter("method2", "param", "newValue")), Assertions.groovy(
                """
          method1(
                          param: "oldValue"
          )
          method2(
                          param: "oldValue"
          )
          method1(
                          param: "oldValue"
          )
          method2(
                          param: "newValue"
          )
}
                  method1(
                                  param: "oldValue"
                  )
                  method2(
                                  param: "newValue"
                  )
          """));
    }
}