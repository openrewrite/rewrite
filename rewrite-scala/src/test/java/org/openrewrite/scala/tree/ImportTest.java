/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

class ImportTest implements RewriteTest {

    @Test
    void singleImport() {
        rewriteRun(
          scala(
            """
            import scala.collection.mutable
            """
          )
        );
    }

    @Test
    void javaImport() {
        rewriteRun(
          scala(
            """
            import java.util.List
            """
          )
        );
    }

    @Test
    void wildcardImport() {
        rewriteRun(
          scala(
            """
            import java.util._
            """
          )
        );
    }

    @Test
    void multipleSelectImport() {
        rewriteRun(
          scala(
            """
            import java.util.{List, Map}
            """
          )
        );
    }

    @Test
    void aliasedImport() {
        rewriteRun(
          scala(
            """
            import java.io.{File => JFile}
            """
          )
        );
    }

    @Test
    void multipleImports() {
        rewriteRun(
          scala(
            """
            import scala.collection.mutable
            import java.util.List
            import java.io._
            """
          )
        );
    }

    @Test
    void complexMultiSelectImport() {
        rewriteRun(
          scala(
            """
            import a.b.{c, d => D, _}
            """
          )
        );
    }

    @Test
    void importWithPackage() {
        rewriteRun(
          scala(
            """
            package com.example
            
            import scala.collection.mutable
            import java.util.List
            
            val x = 42
            """
          )
        );
    }
}