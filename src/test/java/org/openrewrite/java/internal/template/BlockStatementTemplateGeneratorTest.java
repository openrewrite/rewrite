package org.openrewrite.java.internal.template;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class BlockStatementTemplateGeneratorTest {

    private final BlockStatementTemplateGenerator templateGenerator = new BlockStatementTemplateGenerator();
    private final JavaParser parser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build();

    private J.CompilationUnit parseSingle(String source) {
        return parser.parse(source).get(0);
    }

    private Cursor getCursorToStatement(J.CompilationUnit cu, String statementTextFragment) {
        final Cursor[] cursor = {null};
        new org.openrewrite.java.JavaIsoVisitor<Integer>() {
            @Override
            public Statement visitStatement(Statement statement, Integer p) {
                if (statement.print(getCursor()).contains(statementTextFragment)) {
                    if (cursor[0] == null) { // Take the first match
                        cursor[0] = getCursor();
                    }
                }
                return super.visitStatement(statement, p);
            }
        }.visit(cu, 0);
        assertThat(cursor[0]).as("Cursor to statement containing: " + statementTextFragment).isNotNull();
        return cursor[0];
    }
    
    private Cursor getCursorToComment(J.CompilationUnit cu, String commentText) {
        final Cursor[] cursor = {null};
        new org.openrewrite.java.JavaIsoVisitor<Integer>() {
            @Override
            public J.Comment visitComment(J.Comment comment, Integer p) {
                if (comment.getText().equals(commentText)) {
                    cursor[0] = getCursor();
                }
                return super.visitComment(comment, p);
            }
        }.visit(cu, 0);
         assertThat(cursor[0]).as("Cursor to comment: " + commentText).isNotNull();
        return cursor[0];
    }

    @Test
    void contextTemplateBeforeStatement() {
        J.CompilationUnit cu = parseSingle(
                "package com.example;\n" +
                "class A {\n" +
                "    void method() {\n" +
                "        int x = 1;\n" +
                "        int target = 2; // Insertion point is 'int target = 2;'\n" +
                "        int y = 3;\n" +
                "    }\n" +
                "}"
        );
        Cursor insertionPoint = getCursorToStatement(cu, "int target = 2;");
        String codeToInsert = "System.out.println(\"Inserted Code\");";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.BEFORE_STATEMENT);

        // AstPruner should keep: package, class A, method, 'int x = 1;', 'int target = 2;'
        // It should prune 'int y = 3;'
        // Template markers should be before 'int target = 2;'
        assertThat(result)
                .contains("package com.example;")
                .contains("class A {")
                .contains("void method() {")
                .contains("int x = 1;")
                .contains("//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__\nint target = 2;")
                .doesNotContain("int y = 3;");
    }

    @Test
    void contextTemplateAfterStatement() {
        J.CompilationUnit cu = parseSingle(
                "package com.example;\n" +
                "class A {\n" +
                "    void method() {\n" +
                "        int x = 1;\n" +
                "        int target = 2; // Insertion point is 'int target = 2;'\n" +
                "        int y = 3;\n" +
                "    }\n" +
                "}"
        );
        Cursor insertionPoint = getCursorToStatement(cu, "int target = 2;");
        String codeToInsert = "System.out.println(\"Inserted Code\");";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.AFTER_STATEMENT);
        
        // AstPruner: keeps up to and including 'int target = 2;'. Prunes 'int y = 3;'.
        // Template markers after 'int target = 2;'
        assertThat(result)
                .contains("package com.example;")
                .contains("class A {")
                .contains("void method() {")
                .contains("int x = 1;")
                .contains("int target = 2;\n//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__")
                .doesNotContain("int y = 3;");
    }

    @Test
    void contextTemplateReplaceStatement() {
        J.CompilationUnit cu = parseSingle(
                "package com.example;\n" +
                "class A {\n" +
                "    void method() {\n" +
                "        int x = 1;\n" +
                "        int target = 2; // Insertion point is 'int target = 2;'\n" +
                "        int y = 3;\n" +
                "    }\n" +
                "}"
        );
        Cursor insertionPoint = getCursorToStatement(cu, "int target = 2;");
        String codeToInsert = "System.out.println(\"Inserted Code\");";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.REPLACE_STATEMENT);

        // AstPruner: keeps 'int x = 1;'. 'int target = 2;' is the element being replaced. 'int y = 3;' pruned.
        // Template markers replace 'int target = 2;'
        assertThat(result)
                .contains("package com.example;")
                .contains("class A {")
                .contains("void method() {")
                .contains("int x = 1;")
                .contains("//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__")
                .doesNotContain("int target = 2;") // Original statement is replaced
                .doesNotContain("int y = 3;");
    }
    
    @Test
    void contextTemplateReplaceStatementInEmptyBlock() {
        J.CompilationUnit cu = parseSingle(
            "package com.example;\n" +
            "class A {\n" +
            "    void method() {\n" +
            "        //__INSERTION_POINT__\n" +
            "    }\n" + // Block is empty except for the comment
            "    void otherMethod() {}\n" +
            "}"
        );
        // The insertion point is the comment, which is effectively the block itself if we consider it as the target.
        // More accurately, the insertion point is the J.Block containing the comment.
        // Let's get the cursor to the J.Block of method().
        
        final Cursor[] methodBlockCursor = {null};
        new org.openrewrite.java.JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                if ("method".equals(method.getSimpleName())) {
                    methodBlockCursor[0] = new Cursor(getCursor(), method.getBody());
                }
                return super.visitMethodDeclaration(method, p);
            }
        }.visit(cu, 0);
        assertThat(methodBlockCursor[0]).isNotNull();
        
        String codeToInsert = "System.out.println(\"Hello\");";
        // Mode for inserting into an empty block is typically REPLACE_STATEMENT,
        // where the "statement" is notionally the block's content.
        String result = templateGenerator.contextTemplate(methodBlockCursor[0], codeToInsert, TemplateInsertionMode.REPLACE_STATEMENT);

        // AstPruner: class A, method signature. otherMethod body emptied.
        // Template is inserted inside method's block.
        assertThat(result)
            .contains("package com.example;")
            .contains("class A {")
            .contains("void method() {")
            // The BlockStatementTemplateGenerator's logic for empty block / J.Block target needs to be robust.
            // Current logic tries to find "{", then inserts after it.
            .contains("{\n//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__\n")
            .contains("void otherMethod() {")
            .matches(source -> !source.substring(source.indexOf("void otherMethod() {") + "void otherMethod() {".length()).trim().startsWith("}")); // otherMethod body pruned
    }

    // TODO: Test with insertion point being a J.Comment that causes the target element to be the comment itself (if that's how cursor works).
    // TODO: Test interaction with AstPruner: ensure other methods/classes are correctly pruned from context.


    @Test
    void contextTemplateWithMultiLineCode() {
        J.CompilationUnit cu = parseSingle(
                "package com.example;\n" +
                "class A {\n" +
                "    void method() {\n" +
                "        int target = 1;\n" +
                "    }\n" +
                "}"
        );
        Cursor insertionPoint = getCursorToStatement(cu, "int target = 1;");
        String codeToInsert = "System.out.println(\"Line 1\");\nSystem.out.println(\"Line 2\");";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.REPLACE_STATEMENT);
        assertThat(result)
                .contains("package com.example;")
                .contains("class A {")
                .contains("void method() {")
                .contains("//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__")
                .doesNotContain("int target = 1;");
    }

    @Test
    void contextTemplateAtBeginningOfBlock() {
        J.CompilationUnit cu = parseSingle(
                "package com.example;\n" +
                "class A {\n" +
                "    void method() {\n" +
                "        int first = 1; // Target for insertion: BEFORE this\n" +
                "        int second = 2;\n" +
                "    }\n" +
                "}"
        );
        Cursor insertionPoint = getCursorToStatement(cu, "int first = 1;");
        String codeToInsert = "System.out.println(\"Inserted At Start\");";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.BEFORE_STATEMENT);
        
        // AstPruner: keeps 'int first = 1;'. Prunes 'int second = 2;'.
        // Template inserted before 'int first = 1;'
        assertThat(result)
                .contains("package com.example;")
                .contains("class A {")
                .contains("void method() {")
                .contains("//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__\nint first = 1;")
                .doesNotContain("int second = 2;");
    }
    
    @Test
    void contextTemplateAtEndOfBlock() {
         J.CompilationUnit cu = parseSingle(
                "package com.example;\n" +
                "class A {\n" +
                "    void method() {\n" +
                "        int first = 1;\n" +
                "        int last = 2; // Target for insertion: AFTER this\n" +
                "    }\n" + // No statements after 'last = 2;' in this block
                "}"
        );
        Cursor insertionPoint = getCursorToStatement(cu, "int last = 2;");
        String codeToInsert = "System.out.println(\"Inserted At End\");";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.AFTER_STATEMENT);

        // AstPruner: keeps 'int first = 1;' and 'int last = 2;'.
        // Template inserted after 'int last = 2;'
        assertThat(result)
                .contains("package com.example;")
                .contains("class A {")
                .contains("void method() {")
                .contains("int first = 1;")
                .contains("int last = 2;\n//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__");
    }

    @Test
    void contextTemplateInDeeplyNestedStructure() {
        J.CompilationUnit cu = parseSingle(
            "package com.example;\n" +
            "class Outer {\n" +
            "    void outerMethod() {\n" +
            "        System.out.println(\"Outer before if\");\n" + // Kept - before relevant if
            "        if (true) { // Kept - ancestor of IP
            "            System.out.println(\"Outer in if, before inner class\");\n" + // Kept
            "            class Inner {\n" + // Kept - ancestor
            "                void innerMethod() {\n" + // Kept - ancestor
            "                    System.out.println(\"Inner before target\");\n" + // Kept
            "                    int target = 123; // IP is BEFORE this\n" +
            "                    System.out.println(\"Inner after target\");\n" + // Pruned by block logic
            "                }\n" +
            "                void anotherInnerMethod() {} // Pruned - body emptied by AstPruner\n" +
            "            }\n" +
            "            System.out.println(\"Outer in if, after inner class\");\n" + // Pruned by block logic (after relevant inner class)
            "        }\n" +
            "        System.out.println(\"Outer after if\");\n" + // Pruned by block logic
            "    }\n" +
            "    void anotherOuterMethod() {} // Pruned - body emptied by AstPruner\n" +
            "}"
        );

        Cursor insertionPoint = getCursorToStatement(cu, "int target = 123;");
        String codeToInsert = "/* TEMPLATE_CODE */";

        String result = templateGenerator.contextTemplate(insertionPoint, codeToInsert, TemplateInsertionMode.BEFORE_STATEMENT);

        // Expected: Only the direct path to 'int target = 123;' and statements immediately before it in its block are kept.
        // Outer methods/statements not on this path are pruned or have bodies emptied.
        assertThat(result)
            .contains("package com.example;")
            .contains("class Outer {")
            .contains("void outerMethod()")
            .contains("System.out.println(\"Outer before if\");")
            .contains("if (true)") // Condition might be simplified if not IP ancestor
            .contains("System.out.println(\"Outer in if, before inner class\");")
            .contains("class Inner {")
            .contains("void innerMethod() {")
            .contains("System.out.println(\"Inner before target\");")
            .contains("//__TEMPLATE__\n" + codeToInsert + "\n//__TEMPLATE_STOP__\nint target = 123;")
            .doesNotContain("System.out.println(\"Inner after target\");")
            .contains("void anotherInnerMethod() {") // Signature kept
            .matches(source -> !source.substring(source.indexOf("void anotherInnerMethod() {") + "void anotherInnerMethod() {".length()).trim().startsWith("}")) // Body check (empty)
            .doesNotContain("System.out.println(\"Outer in if, after inner class\");")
            .doesNotContain("System.out.println(\"Outer after if\");")
            .contains("void anotherOuterMethod() {") // Signature kept
            .matches(source -> !source.substring(source.indexOf("void anotherOuterMethod() {") + "void anotherOuterMethod() {".length()).trim().startsWith("}")); // Body check (empty)
            
        // Refined check for `if(true)` condition simplification:
        // The `if` statement is an ancestor of the IP. So `isIpOrAncestor(mIf)` is true.
        // Its condition `true` is not an IP/ancestor. So `conditionIsRelevant` is false.
        // `simplifyExpression(condition)` should be called. Current `simplifyExpression` makes it `true`. So this is fine.
    }
}
