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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class CreateXmlFileTest implements RewriteTest {

    @DocumentExample
    @Test
    void hasCreatedFile() {
        String fileContents = """
          <?xml version="1.0" encoding="UTF-8"?>
          <library>
              <book id="1">
                  <title>The Great Gatsby</title>
                  <author>F. Scott Fitzgerald</author>
                  <year>1925</year>
              </book>
              <book id="2">
                  <title>To Kill a Mockingbird</title>
                  <author>Harper Lee</author>
                  <year>1960</year>
              </book>
          </library>
          """;
        rewriteRun(
          spec -> spec.recipe(new CreateXmlFile(
            "test/test.xml",
            fileContents,
            null
          )),
          xml(
            null,
            fileContents,
            spec -> spec.path("test/test.xml")
          )
        );
    }

    @DocumentExample
    @Test
    void hasOverwrittenFile() {
        String fileContents = """
          <?xml version="1.1" encoding="UTF-8"?>
          <after/>
          """;
        rewriteRun(
          spec -> spec.recipe(new CreateXmlFile(
            "test/test.xml",
            fileContents,
            true
          )).cycles(1).expectedCyclesThatMakeChanges(1),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <before/>
              """,
            fileContents,
            spec -> spec.path("test/test.xml")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateXmlFile(
            "test/test.xml",
            null,
            false
          )),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <test/>
              """,
            spec -> spec.path("test/test.xml")
          )
        );
    }

    @Test
    void shouldNotChangeExistingFileWhenOverwriteNull() {
        rewriteRun(
          spec -> spec.recipe(new CreateXmlFile(
            "test/test.xml",
            null,
            null
          )),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <test/>
              """,
            spec -> spec.path("test/test.xml")
          )
        );
    }

    @Test
    void shouldAddAnotherFile() {
        rewriteRun(
          spec -> spec.recipe(new CreateXmlFile(
            "test/test-file-2.xml",
            null,
            true
          )),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <test/>
              """,
            spec -> spec.path("test/test-file-1.xml")
          ),
          xml(
            null,
            "",
            spec -> spec.path("test/test-file-2.xml")
          )
        );
    }
}
