/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GroovyUnusedAssignment")
class MapEntryTest implements RewriteTest {

    @Test
    void mapEntryMethodArguments() {
        rewriteRun(
          groovy("apply plugin: 'java'")
        );
    }

    @Test
    void multipleEntries() {
        rewriteRun(
          groovy("exclude(group: 'g', module: 'm')")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3853")
    @Test
    void genericMaps() {
        rewriteRun(
          groovy("Map<String, String> m = new HashMap<String, String>()"),
          groovy("Map<String, String> n = new HashMap<>()")
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2071")
    @Test
    void emptyMapLiteral() {
        rewriteRun(
          groovy("Map m =  [  :  ]")
        );
    }

    @Test
    void mapAccess() {
        rewriteRun(
          groovy("def a = someMap /*[*/ [ /*'*/ 'someKey' /*]*/ ]")
        );
    }
}
