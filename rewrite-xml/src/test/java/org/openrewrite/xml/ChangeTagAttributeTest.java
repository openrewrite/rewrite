/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class ChangeTagAttributeTest implements RewriteTest {

    @DocumentExample
    @Test
    void alterAttributeWhenElementAndAttributeMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagAttribute("bean", "id", "myBean2.subpackage", "myBean.subpackage", null)),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <bean id='myBean2.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }

    @Test
    void alterAttributeWithNullOldValue() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagAttribute("bean", "id", "myBean2.subpackage", null, null))
            .expectedCyclesThatMakeChanges(2),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <bean id='myBean2.subpackage'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }

    @Test
    void removeAttribute() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagAttribute("bean", "id", null, "myBean.subpackage", null)),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <bean/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }

    @Test
    void attributeNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagAttribute("bean", "id", "myBean2.subpackage", "not.matched", null)),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }


    @Test
    void alterAttributeWithRegex() {
        rewriteRun(
          spec -> spec.recipe(new ChangeTagAttribute("bean", "id", "myBean2.$1", "myBean\\.(.*)", true)),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <bean id='myBean2.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }
}
