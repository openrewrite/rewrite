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
package org.openrewrite.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class AddTagAttributeTest implements RewriteTest {

    @DocumentExample
    @Test
    void addsAttributeWhenMissing() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "scope", "singleton")),
          xml(
            """
              <beans>
                  <bean id="myBean"/>
                  <bean id="anotherBean" scope="prototype"/>
              </beans>
              """,
            """
              <beans>
                  <bean id="myBean" scope="singleton"/>
                  <bean id="anotherBean" scope="prototype"/>
              </beans>
              """
          )
        );
    }

    @Test
    void doesNothingIfAttributeExists() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "id", "newId")),
          xml(
            """
              <beans>
                  <bean id="myBean"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addsAttributeToMultipleMatchingTags() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "type", "service")),
          xml(
            """
              <beans>
                  <bean id="one"/>
                  <bean id="two" type="repository"/>
                  <bean id="three"/>
              </beans>
              """,
            """
              <beans>
                  <bean id="one" type="service"/>
                  <bean id="two" type="repository"/>
                  <bean id="three" type="service"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addAttributeToTagWithNoAttributes() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "scope", "singleton")),
          xml(
            """
              <beans>
                  <bean/>
              </beans>
              """,
            """
              <beans>
                  <bean scope="singleton"/>
              </beans>
              """
          )
        );
    }

    @Test
    void addAttributeToNonSelfClosingEmptyTagWithoutAttributes() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "scope", "singleton")),
          xml(
            """
              <beans>
                  <bean></bean>
              </beans>
              """,
            """
              <beans>
                  <bean scope="singleton"></bean>
              </beans>
              """
          )
        );
    }

    @Test
    void addAttributeToTagWithNewlineBetweenOpenAndClose() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "scope", "singleton")),
          xml(
            """
              <beans>
                  <bean>
                  </bean>
              </beans>
              """,
            """
              <beans>
                  <bean scope="singleton">
                  </bean>
              </beans>
              """
          )
        );
    }

    @Test
    void addAttributeWhenSimilarAttributeExists() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("bean", "scope", "singleton")),
          xml(
            """
              <beans>
                  <bean scope1="singleton">
                  </bean>
              </beans>
              """,
            """
              <beans>
                  <bean scope1="singleton" scope="singleton">
                  </bean>
              </beans>
              """
          )
        );
    }

    @Test
    void addAttributeUsingXPathExpression() {
        rewriteRun(
          spec -> spec.recipe(new AddTagAttribute("//bean/property", "scope", "singleton")),
          xml(
            // before
            """
              <beans>
                  <bean>
                      <property name="myProperty" />
                  </bean>
                  <notbean>
                      <property name="shouldNotChange"/>
                  </notbean>
              </beans>
              """,
            // after
            """
              <beans>
                  <bean>
                      <property name="myProperty" scope="singleton" />
                  </bean>
                  <notbean>
                      <property name="shouldNotChange"/>
                  </notbean>
              </beans>
              """
          )
        );
    }

}
