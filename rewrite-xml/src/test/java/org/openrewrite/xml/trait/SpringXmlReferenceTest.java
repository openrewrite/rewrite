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
package org.openrewrite.xml.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class SpringXmlReferenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new SpringXmlReference.Provider().getMatcher()
          .asVisitor(springJavaTypeReference -> SearchResult.found(springJavaTypeReference.getTree(), springJavaTypeReference.getValue()))));
    }

    @DocumentExample
    @SuppressWarnings("SpringXmlModelInspection")
    @Test
    void xmlConfiguration() {
        rewriteRun(
          xml(
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="http://www.springframework.org/schema/beans"
                  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
                <bean id="testBean" class="org.springframework.beans.TestBean" scope="prototype">
                  <property name="age" value="10"/>
                  <property name="sibling">
                      <bean class="org.springframework.beans.TestBean">
                          <property name="age" value="11" class="java.lang.Integer"/>
                          <property name="someName">
                              <value>java.lang.String</value>
                          </property>
                          <property name="someOtherName">
                              <value>java.lang</value>
                          </property>
                          <property name="nameMap">
                              <map key-type="java.lang.String" value-type="java.lang.String"/>
                          </property>
                      </bean>
                  </property>
                </bean>
              </beans>
              """,
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="http://www.springframework.org/schema/beans"
                  xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
                <bean id="testBean" <!--~~(org.springframework.beans.TestBean)~~>-->class="org.springframework.beans.TestBean" scope="prototype">
                  <property name="age" value="10"/>
                  <property name="sibling">
                      <bean <!--~~(org.springframework.beans.TestBean)~~>-->class="org.springframework.beans.TestBean">
                          <property name="age" value="11" <!--~~(java.lang.Integer)~~>-->class="java.lang.Integer"/>
                          <property name="someName">
                              <!--~~(java.lang.String)~~>--><value>java.lang.String</value>
                          </property>
                          <property name="someOtherName">
                              <!--~~(java.lang)~~>--><value>java.lang</value>
                          </property>
                          <property name="nameMap">
                              <map <!--~~(java.lang.String)~~>-->key-type="java.lang.String" <!--~~(java.lang.String)~~>-->value-type="java.lang.String"/>
                          </property>
                      </bean>
                  </property>
                </bean>
              </beans>
              """
          )
        );
    }
}
