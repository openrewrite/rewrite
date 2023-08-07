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
import org.openrewrite.xml.ChangeNamespace;

import static org.openrewrite.xml.Assertions.xml;

public class ChangeNamespaceTest  implements RewriteTest {

    @DocumentExample
    @Test
    void namespaceWithPrefixMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespace("http://old.namespace", "https://new.namespace")),
          xml(
            """
                    <ns0:parent
                        xmlns:ns0="http://old.namespace"
                        xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                            <ns0:child>value</ns0:child>
                    </ns0:parent>
                    """,
                        """
                    <ns0:parent
                        xmlns:ns0="https://new.namespace"
                        xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                            <ns0:child>value</ns0:child>
                    </ns0:parent>
                    """
          )
        );
    }

    @DocumentExample
    @Test
    void namespaceWithoutPrefixMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespace("http://old.namespace", "https://new.namespace")),
          xml(
            """
                    <parent
                        xmlns="http://old.namespace"
                        xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                            <child>value</child>
                    </parent>
                    """,
                        """
                    <parent
                        xmlns="https://new.namespace"
                        xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                            <child>value</child>
                    </parent>
                    """
          )
        );
    }

    @Test
    void namespaceNotMatched() {
        rewriteRun(
          spec -> spec.recipe(new ChangeNamespace("http://non.existant.namespace", "https://new.namespace")),
          xml(
            """
                    <ns0:parent
                        xmlns:ns0="http://old.namespace"
                        xmlns:xs="http://www.w3.org/2000/10/XMLSchema-instance">
                            <ns0:child>value</ns0:child>
                    </ns0:parent>
                    """
          )
        );
    }
}
