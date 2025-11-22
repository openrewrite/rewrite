package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.tree.ParseError;
import java.util.List;
import java.util.stream.Collectors;
import org.openrewrite.java.TreeVisitingPrinter;


class MyParserDebug {

    @Test
    void debugDirectly() {
        JavaParser parser = JavaParser.fromJavaVersion().build();

        String sourceCode = """
        import java.util.*;;
        import java.io.*;

        public class Main {
            public static void main(String[] args) {}
        }
        """;

        List<SourceFile> sourceFiles = parser.parse(sourceCode)
          .collect(Collectors.toList());

        SourceFile result = sourceFiles.get(0);

        // --- CHECK FOR PARSE ERROR ---
        if (result instanceof ParseError) {
            ParseError error = (ParseError) result;
            System.err.println("\n\n========================================");
            System.err.println("CRITICAL: PARSER FAILED");
            System.err.println("The parser could not understand the code.");
            System.err.println("Error Message: " + error.getText()); // This usually holds the source
            System.err.println("To String: " + error.toString());
            System.err.println("========================================\n\n");

            // Test should fail if parsing fails
            throw new RuntimeException("Parser failed: " + error.toString());
        }
        else if (result instanceof J.CompilationUnit) {
            // This part runs if parsing SUCCEEDS (what we were trying before)
            J.CompilationUnit cu = (J.CompilationUnit) result;
            var imports = cu.getImports();

            System.err.println("========================================");
            System.err.println("SUCCESS: Parser succeeded!");
            System.err.println("Number of imports: " + imports.size());

            if (imports.size() > 1) {
                String prefix = imports.get(1).getPrefix().getWhitespace();
                System.err.println("Second import prefix hex: " + stringToHex(prefix));
                System.err.println("Second import prefix text: '" + prefix.replace("\n", "\\n").replace("\r", "\\r") + "'");
            }

            String printedSource = cu.print();
            System.err.println("Original source length: " + sourceCode.length());
            System.err.println("Printed source length: " + printedSource.length());
            System.err.println("Sources match: " + sourceCode.equals(printedSource));

            System.err.println("LST Structure:");
            System.err.println(TreeVisitingPrinter.printTree((cu)));

            System.err.println("========================================");
        }
    }

    private String stringToHex(String input) {
        if (input == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(String.format("\\u%04x ", (int) c));
        }
        return sb.toString();
    }
}