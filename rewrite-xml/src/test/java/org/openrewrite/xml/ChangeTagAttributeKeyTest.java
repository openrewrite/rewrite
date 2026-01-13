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
package org.openrewrite.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class ChangeTagAttributeKeyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeTagAttributeKey(
          "/html/body/a4j:ajax",
          "reRender",
          "render"
        ));
    }

    @DocumentExample
    @Test
    void renameReRenderToRender() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <html xmlns="http://www.w3.org/1999/xhtml"
                    xmlns:a4j="http://richfaces.org/a4j">
                  <body>
                      <a4j:ajax reRender="form:output" event="change"/>
                  </body>
              </html>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <html xmlns="http://www.w3.org/1999/xhtml"
                    xmlns:a4j="http://richfaces.org/a4j">
                  <body>
                      <a4j:ajax render="form:output" event="change"/>
                  </body>
              </html>
              """
          )
        );
    }

    @Test
    void applyRepeatedly() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <html xmlns="http://www.w3.org/1999/xhtml"
                    xmlns:a4j="http://richfaces.org/a4j">
                  <body>
                      <a4j:ajax reRender="form:output" event="change"/>
                      <a4j:ajax reRender="form:output" event="change1"/>
                      <a4j:ajax reRender="form:output" event="change2"/>
                  </body>
              </html>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <html xmlns="http://www.w3.org/1999/xhtml"
                    xmlns:a4j="http://richfaces.org/a4j">
                  <body>
                      <a4j:ajax render="form:output" event="change"/>
                      <a4j:ajax render="form:output" event="change1"/>
                      <a4j:ajax render="form:output" event="change2"/>
                  </body>
              </html>
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void whenAttributeNotPresent() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <html xmlns="http://www.w3.org/1999/xhtml"
                        xmlns:a4j="http://richfaces.org/a4j">
                      <body>
                          <a4j:ajax render="form:output" event="change"/>
                      </body>
                  </html>
                  """
              )
            );
        }

        @Test
        void whenOnOtherXPath() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <html xmlns="http://www.w3.org/1999/xhtml"
                        xmlns:a4j="http://richfaces.org/a4j">
                      <body>
                          <div>
                              <a4j:ajax render="form:output" event="change"/>
                          </div>
                      </body>
                  </html>
                  """
              )
            );
        }
    }
}
