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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.trait.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.xml.Assertions.xml;

class SpringXmlReferenceTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings("SpringXmlModelInspection")
    @Test
    void xmlConfiguration() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new SpringXmlReference.Provider().getMatcher()
            .asVisitor(ref -> SearchResult.found(ref.getTree(), ref.getValue())))),
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

    @Test
    void springTypeAndPackageReferences() {
        rewriteRun(xml(
          """
            <!DOCTYPE beans SYSTEM "spring-beans.dtd">
            <beans>
              <bean class="example.Bean">
                <constructor-arg value="example.Implementation"/>
                <property name="package"><value>example.scan</value></property>
                <property name="ignored" value="example.scan"/>
              </bean>
            </beans>
            """,
          spec -> spec.beforeRecipe(doc -> assertThat(doc.getReferences().getReferences())
            .extracting(Reference::getKind, Reference::getValue)
            .containsExactlyInAnyOrder(tuple(Reference.Kind.TYPE, "example.Bean"),
              tuple(Reference.Kind.TYPE, "example.Implementation"), tuple(Reference.Kind.PACKAGE, "example.scan")))
        ));
    }

    @Test
    void unrelatedSpringValues() {
        rewriteRun(xml(
          """
            <beans xsi:schemaLocation="www.springframework.org/schema/beans" xmlns:other="urn:other">
              <other:property value="example.Old"/>
              <other:value>example.Old</other:value>
              <other:props><prop>example.Old</prop></other:props>
              <other:properties><prop>example.Old</prop></other:properties>
              <bean value="example.Old">
                <constructor-arg value="example.key"/>
                <property value="https://example.Old"/>
                <map><entry key="hibernate.dialect" value="example.key"/></map>
                <property value="SimpleName"/>
                <props>
                  <prop key="number">42</prop>
                  <prop key="url">https://example.Old</prop>
                  <prop key="hibernate.dialect">example.setting</prop>
                </props>
                <prop key="outside">example.Old</prop>
              </bean>
            </beans>
            """,
          spec -> spec.beforeRecipe(doc -> assertThat(doc.getReferences().getReferences()).isEmpty())
        ));
    }

    @Test
    void classNamesInKeysAndUtilProperties() {
        rewriteRun(xml(
          """
            <beans xmlns="http://www.springframework.org/schema/beans"
                   xmlns:u="http://www.springframework.org/schema/util">
              <bean>
                <property name="customEditors"><map><entry key="com.foo.Money" value="editor"/></map></property>
              </bean>
              <u:properties>
                <prop key="com.foo.NotFoundException">error</prop>
                <prop key="implementation">com.foo.Implementation</prop>
              </u:properties>
            </beans>
            """,
          spec -> spec.beforeRecipe(doc -> assertThat(doc.getReferences().getReferences())
            .extracting(Reference::getValue)
            .containsExactlyInAnyOrder("com.foo.Money", "com.foo.NotFoundException", "com.foo.Implementation"))
        ));
    }

    @Test
    void foreignDefaultNamespaceOnBeansRoot() {
        rewriteRun(xml(
          """
            <b:beans xmlns:b="http://www.springframework.org/schema/beans"
                     xmlns="http://www.springframework.org/schema/security">
              <property value="example.Security"/><entry key="example.Key" value="example.Security"/>
              <props><prop key="example.Error">example.Security</prop></props><value>example.Security</value>
              <b:property value="example.Spring"/>
            </b:beans>
            """,
          spec -> spec.beforeRecipe(doc -> assertThat(doc.getReferences().getReferences())
            .extracting(Reference::getValue).containsExactly("example.Spring"))
        ));
    }

    @Test
    void nestedForeignDefaultNamespace() {
        rewriteRun(xml(
          """
            <beans xmlns="http://www.springframework.org/schema/beans">
              <camelContext xmlns="http://camel.apache.org/schema/spring">
                <property value="example.Camel"/><props><prop>example.Camel</prop></props><value>example.Camel</value>
              </camelContext>
              <property value="example.Spring"/>
            </beans>
            """,
          spec -> spec.beforeRecipe(doc -> assertThat(doc.getReferences().getReferences())
            .extracting(Reference::getValue).containsExactly("example.Spring"))
        ));
    }
}
