/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.TreeSerializer;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public interface JavaTreeTest {
    default void assertParsePrintAndProcess(JavaParser parser, NestingLevel nestingLevel, String code,
                                            String... imports) {
        String source = Arrays.stream(imports).map(i -> "import " + i + ";").collect(joining(""));

        switch (nestingLevel) {
            case Block:
                source = source + "class A" + System.nanoTime() + "{{\n" + code + "\n}}";
                break;
            case Class:
                source = source + "class A" + System.nanoTime() + "{\n" + code + "\n}";
                break;
            case CompilationUnit:
                source = source + "/*<START>*/\n" + code;
                break;
        }

        J.CompilationUnit cu = parser.parse(source).iterator().next();

        J processed = new JavaProcessor<Void>().visit(cu, null);
        assertThat(processed).as("Processing is idempotent").isSameAs(cu);

        TreeSerializer<J.CompilationUnit> treeSerializer = new TreeSerializer<>();
        J.CompilationUnit roundTripCu = treeSerializer.read(treeSerializer.write(cu));

        assertThat(JavaParserTestUtil.print(nestingLevel, cu))
                .as("Source code is printed the same after parsing")
                .isEqualTo(StringUtils.trimIndent(code));

        assertThat(JavaParserTestUtil.print(nestingLevel, roundTripCu))
                .as("Source code is printed the same after round trip serialization")
                .isEqualTo(StringUtils.trimIndent(code));
    }

    enum NestingLevel {
        Block,
        Class,
        CompilationUnit
    }
}

class JavaParserTestUtil {
    static String print(JavaTreeTest.NestingLevel nestingLevel, J.CompilationUnit cu) {
        String printed;
        switch (nestingLevel) {
            case Block:
                printed = cu.getClasses().iterator().next().getBody().getStatements().iterator().next().getElem().printTrimmed();
                printed = printed.substring(1, printed.length() - 1);
                break;
            case Class:
                printed = cu.getClasses().iterator().next().getBody().printTrimmed();
                printed = printed.substring(1, printed.length() - 1);
                break;
            case CompilationUnit:
            default:
                printed = cu.printTrimmed();
                printed = printed.substring(printed.indexOf("/*<START>*/") + "/*<START>*/".length());
                break;
        }
        return StringUtils.trimIndent(printed);
    }
}
