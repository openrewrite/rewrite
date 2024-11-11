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
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.Assertions.*;

class ExtendInterfaceTest implements RewriteTest {

    @Test
    void interfaceExtendsInterface() {
        @Language("java")
        String parentInterface = """
          package parent;
          public interface parentInterface {}
          """;

        @Language("java")
        String childInterface = """
          package child;
          import parent.parentInterface;
          public interface childInterface extends parentInterface {}
          """;

        J.CompilationUnit sourceJavaFile = (J.CompilationUnit) JavaParser.fromJavaVersion()
          .dependsOn(parentInterface)
          .build()
          .parse(childInterface).findFirst().get();

        J.ClassDeclaration cd = sourceJavaFile.getClasses().get(0);

        // Check that childInterface does not implement any interfaces
        assertNull(cd.getImplements());

        // Check that childInterface extends an interface
        assertNotNull(cd.getExtends());
    }
}
