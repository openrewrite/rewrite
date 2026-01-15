/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.tree;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

class ImportTest implements RewriteTest {

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "import math",
      "import  math",
    })
    void simpleImport(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "import math as math2",
      "import  math as math2",
      "import math  as math2",
      "import math as  math2",
    })
    void simpleImportAlias(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }


    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "from . import foo",
      "from  . import foo",
      "from .  import foo",
      "from . import  foo",
      "from .mod import foo",
      "from  .mod import foo",
      "from .mod  import foo",
      "from .mod import  foo",
      "from ...mod import  foo",
      "from ....mod import  foo",
    })
    void localImport(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "from math import ceil",
      "from  math import ceil",
      "from math  import ceil",
      "from math import  ceil",
    })
    void qualifiedImport(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }

    @ParameterizedTest
    //language=py
    @ValueSource(strings = {
      "from . import foo as foo2",
      "from  . import foo as foo2",
      "from .  import foo as foo2",
      "from . import  foo as foo2",
      "from . import foo  as foo2",
      "from . import foo as  foo2",
    })
    void localImportAlias(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-python/issues/35")
    @Test
    void multipleImports() {
        rewriteRun(
          python(
            """
             import sys
             
             import math
             """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-python/issues/35")
    @Test
    void enclosedInParens() {
        rewriteRun(
          python(
            """
             from math import (
                 sin,
                 cos
             )
             """
          )
        );
    }

    @ParameterizedTest
    @SuppressWarnings("TrailingWhitespacesInTextBlock")
    //language=py
    @ValueSource(strings = {
      "from math import sin, cos # stuff\n\n",
      "from math import sin as sin2, cos\n",
      "from math import sin as sin2, cos as cos2\n",
      """
        from math import (
            sin, 
            cos
        )
        """,
    })
    void multipleImport(@Language("py") String arg) {
        rewriteRun(
          python(arg)
        );
    }

}
