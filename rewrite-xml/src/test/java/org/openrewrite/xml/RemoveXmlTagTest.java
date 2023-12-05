/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


import static org.openrewrite.xml.Assertions.xml;

class RemoveXmlTagTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {spec.recipe(new RemoveXmlTag("//bean", "**/beans.xml"));}


    @DocumentExample
    @Test
    void removeMatchingElementInMatchingFile() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//bean", "**/beans.xml")),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            documentSourceSpec -> documentSourceSpec.path("my/project/beans.xml")

          )
        );
    }

    @Test
    void elementNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//notBean", null)),
          xml(
            """
              <beans>
                  <bean notBean='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """
          )
        );
    }

    @Test
    void fileNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//bean", "**/beans.xml")),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            documentSourceSpec -> documentSourceSpec.path("my/project/notBeans.xml")
          )
        );
    }

    @Test
    void fileMatcherEmpty() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//bean", null)),
          xml(
            """
              <beans>
                  <bean id='myBean.subpackage.subpackage2'/>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            """
              <beans>
                  <other id='myBean.subpackage.subpackage2'/>
              </beans>
              """,
            documentSourceSpec -> documentSourceSpec.path("my/project/notBeans.xml")
          )
        );
    }

    @Test
    void removeEmptyParentTag() {
        rewriteRun(
          spec -> spec.recipe(new RemoveXmlTag("//project/build/pluginManagement/plugins/plugin", null)),
          xml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <release>11</release>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>sample-app</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <release>11</release>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
