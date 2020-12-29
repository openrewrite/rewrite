package org.openrewrite.java;

import org.openrewrite.ParserTest;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JavaParserTest extends ParserTest {
    public void assertParseAndPrint(JavaParser parser, Parseable before, String... imports) {
        String source = Arrays.stream(imports).map(i -> "import " + i + ";").collect(joining(""));

        switch(before.level) {
            case Block:
                source = source + "class A" + System.nanoTime() + "{{" + before.code + "}}";
                break;
            case Class:
                source = source + "class A" + System.nanoTime() + "{" + before.code + "}";
                break;
        }

//        assertThat(printed).isEqualTo(StringUtils.trimIndent(before.code));
    }

    public static Parseable blockLevel(String code) {
        return new Parseable(code, NestingLevel.Block);
    }

    public static Parseable classLevel(String code) {
        return new Parseable(code, NestingLevel.Class);
    }

    public static Parseable outerLevel(String code) {
        return new Parseable(code, NestingLevel.CompilationUnit);
    }

    public static class Parseable {
        private final String code;
        private final NestingLevel level;

        public Parseable(String code, NestingLevel level) {
            this.code = code;
            this.level = level;
        }
    }

    enum NestingLevel {
        Block,
        Class,
        CompilationUnit
    }
}
