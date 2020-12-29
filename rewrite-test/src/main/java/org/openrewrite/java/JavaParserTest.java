package org.openrewrite.java;

import org.apache.commons.lang3.StringUtils;
import org.openrewrite.java.tree.J;

import java.util.Arrays;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public interface JavaParserTest {
    default void assertParseAndPrint(JavaParser parser, NestingLevel nestingLevel, String code, String... imports) {
        String source = Arrays.stream(imports).map(i -> "import " + i + ";").collect(joining(""));

        switch(nestingLevel) {
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

        String printed;
        switch(nestingLevel) {
            case Block:
                printed = cu.getClasses().iterator().next().getBody().getStatements().iterator().next().getElem().printTrimmed();
                printed = printed.substring(0, printed.length() - 1);
                break;
            case Class:
                printed = cu.getClasses().iterator().next().getBody().printTrimmed();
                printed = printed.substring(0, printed.length() - 1);
                break;
            case CompilationUnit:
            default:
                printed = cu.printTrimmed();
                printed = printed.substring(printed.indexOf("/*<START>*/") + "/*<START>*/".length());
                break;
        }

        assertThat(printed).isEqualTo(StringUtils.trim(code));
    }

    enum NestingLevel {
        Block,
        Class,
        CompilationUnit
    }
}
