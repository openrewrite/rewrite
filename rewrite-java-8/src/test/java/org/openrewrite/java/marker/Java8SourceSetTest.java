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
package org.openrewrite.java.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class Java8SourceSetTest {

    @Test
    void javaStandardLibraryTypesFromRtJar() {
        JavaSourceSet jss = JavaSourceSet.build("main", emptyList());
        assertThat(jss.getClasspath())
          .extracting(JavaType.FullyQualified::getFullyQualifiedName)
          .contains(
            "java.lang.Object",
            "java.util.Map$Entry",
            "java.util.logging.Logger",
            "java.sql.Connection",
            "java.beans.PropertyChangeListener"
          )
          .doesNotContain("java.util.HashMap$Node")
          .allSatisfy(fqn -> assertThat(fqn).startsWith("java."));
    }
}
