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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class EmptyNewlineAtEndOfFileTest implements RewriteTest {

    private static Consumer<RecipeSpec> generalFormat(@Nullable Boolean useCRLF) {
        return spec -> {
            spec.recipe(new EmptyNewlineAtEndOfFile());
            if(useCRLF != null) {
                spec.parser(JavaParser.fromJavaVersion().styles(singletonList(
                    new NamedStyles(
                      randomId(), "test", "test", "test", emptySet(),
                      singletonList(new GeneralFormatStyle(useCRLF)))
                  )
                ));
            }
        };
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/1045")
    @Test
    void usesCRLF() {
        rewriteRun(
          generalFormat(true),
          java(
            "class Test {}",
            "class Test {}\r\n",
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void autodetectCRLF() {
        rewriteRun(
          generalFormat(null),
          java(
            "class Test {\r\n}",
            "class Test {\r\n}\r\n",
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void autodetectLF() {
        rewriteRun(
          generalFormat(null),
          java(
            "class Test {\n}",
            "class Test {\n}\n",
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void noComments() {
        rewriteRun(
          generalFormat(false),
          java(
            "class Test {}",
            "class Test {}\n",
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void comments() {
        rewriteRun(
          generalFormat(false),
          java(
            "class Test {}\n/*comment*/",
            "class Test {}\n/*comment*/\n",
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void multipleLinesToOne() {
        rewriteRun(
          generalFormat(false),
          java(
            "class Test {}\n\n",
            "class Test {}\n",
            SourceSpec::noTrim
          )
        );
    }
}
