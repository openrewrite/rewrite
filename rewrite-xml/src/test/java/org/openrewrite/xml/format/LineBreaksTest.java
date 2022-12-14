/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.xml.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class LineBreaksTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LineBreaks());
    }

    @SuppressWarnings("CheckTagEmptyBody")
    @Test
    void tags() {
        rewriteRun(
          xml(
            """
              <project>
                <dependencies><dependency></dependency><dependency/>
                </dependencies>
              </project>
              """,
            """
              <project>
                <dependencies>
              <dependency></dependency>
              <dependency/>
                </dependencies>
              </project>
              """
          )
        );
    }

    @SuppressWarnings("CheckTagEmptyBody")
    @Test
    void comments() {
        rewriteRun(
          xml(
            """
              <project>
                <dependencies><!--comment-->
                </dependencies>
              </project>
              """,
            """
              <project>
                <dependencies>
              <!--comment-->
                </dependencies>
              </project>
              """
          )
        );
    }

    @SuppressWarnings("CheckTagEmptyBody")
    @Test
    void docTypeDecl() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?><!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "https://www.springframework.org/dtd/spring-beans-2.0.dtd"><beans/>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <!DOCTYPE beans PUBLIC "-//SPRING//DTD BEAN 2.0//EN" "https://www.springframework.org/dtd/spring-beans-2.0.dtd">
              <beans/>
              """
          )
        );
    }

    @SuppressWarnings("CheckTagEmptyBody")
    @Test
    void prolog() {
        rewriteRun(
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?><?xml-stylesheet href="mystyle.css" type="text/css"?><beans/>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <?xml-stylesheet href="mystyle.css" type="text/css"?>
              <beans/>
              """
          )
        );
    }
}
