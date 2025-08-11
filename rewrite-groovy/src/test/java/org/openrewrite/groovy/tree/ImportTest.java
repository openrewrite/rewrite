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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class ImportTest implements RewriteTest {
    @Test
    void classImport() {
        rewriteRun(
          groovy(
            """
              import java.util.List
              """
          )
        );
    }

    @Test
    void multipleImportsOnOneLine() {
        rewriteRun(
          groovy(
            """
              import java.util.List;import java.util.Set
              """
          )
        );
    }

    @Test
    void starImport() {
        rewriteRun(
          groovy(
            """
              import java.util.*
              """
          )
        );
    }

    @Test
    void staticImport() {
        rewriteRun(
          groovy(
            """
              import static java.util.Collections.singletonList
              """
          )
        );
    }

    @Test
    void staticStarImport() {
        rewriteRun(
          groovy(
            """
              import static java.util.Collections.*
              """
          )
        );
    }

    @Test
    void classImportAlias() {
        rewriteRun(
          groovy(
            """
              import java.util.List as L
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getEof()).isEqualTo(Space.EMPTY))
          )
        );
    }

    @Test
    void staticImportAlias() {
        rewriteRun(
          groovy(
            """
              import static java.util.Collections.singletonList as listOf
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getEof()).isEqualTo(Space.EMPTY))
          )
        );
    }

    @Test
    void duplicateImportsAndComments() {
        rewriteRun(
          groovy(
            """
                    import static      java.util.Collections.*    ;     import               static      java.util.Collections.*
                    //import static      java.util.Collections.*    ;     import               static      java.util.Collections.*
              import java.util.Collections.*  ; import static java.util.Collections.*
              import java.util.Collections.*
              import static java.util.Collections.singletonList as listOf
              import static java.util.Collections.singletonList as listOf
              import /*static java.util.Collections.singletonList;import static*/ java.util.Collections.*
              import /*static java.util.Collections.singletonList;


              import static java.util.Collections.**/java.util.Collections.*
              import /*static java.util.Collections.singletonList;


              import static java.util.Collections.**/static          java.util.Collections.*
              import static java.util.Collections.singletonList;import static java.util.Collections.*
              import java.util.Collections.*
              """
          )
        );
    }

    @Test
    void multiLineCommentBeforeStarStaticImport() {
        rewriteRun(
          groovy(
            """
            /*
             * Hello
             */
            import java.io.File
            import static java.lang.Math.*
            import java.nio.file.Path
            final String s = new String("s")
            """
          )
        );
    }

    @Test
    void twoSameImportStarStatements() {
        rewriteRun(
          groovy(
            """
            import static java.lang.Math.*
            import static java.lang.Math.*
            """
          )
        );
    }

    @Test
    void oneImportStaticStarSplitIntoTwoLines() {
        rewriteRun(
          groovy(
            """
            import/* export
            */static java.lang.Math.*
            """
          )
        );
    }
}
