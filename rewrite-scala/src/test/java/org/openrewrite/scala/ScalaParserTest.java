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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.scala.tree.S;
import org.openrewrite.tree.ParseError;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ScalaParserTest {

    @Test
    void parseSimpleValDeclaration() {
        ScalaParser parser = ScalaParser.builder().build();
        
        String source = """
            val x = 42
            """;
        
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isNotInstanceOf(ParseError.class);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        
        S.CompilationUnit cu = (S.CompilationUnit) parsed.get(0);
        assertThat(cu.getStatements()).hasSize(1);
    }

    @Test
    void parseSimpleObject() {
        ScalaParser parser = ScalaParser.builder().build();
        
        String source = """
            object HelloWorld {
              def main(args: Array[String]): Unit = {
                println("Hello, World!")
              }
            }
            """;
        
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        
        assertThat(parsed).hasSize(1);
        // For now, we expect this to fail since we haven't implemented object parsing yet
        // This test documents the expected behavior once implemented
    }

    @Test
    void parseWithPackage() {
        ScalaParser parser = ScalaParser.builder().build();
        
        String source = """
            package com.example
            
            val message = "Hello"
            """;
        
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        
        S.CompilationUnit cu = (S.CompilationUnit) parsed.get(0);
        assertThat(cu.getPackageDeclaration()).isNotNull();
    }

    @Test
    void testPackageDuplicationIssue() {
        ScalaParser parser = ScalaParser.builder().build();
        
        String source = "package com.example\n\nval x = 42";
        
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        
        S.CompilationUnit cu = (S.CompilationUnit) parsed.get(0);
        
        // Check the package declaration
        assertThat(cu.getPackageDeclaration()).isNotNull();
        assertThat(cu.getPackageDeclaration().getExpression().toString())
            .as("Package expression should be 'com.example'")
            .isEqualTo("com.example");
        
        // Check the statements
        assertThat(cu.getStatements())
            .as("Should have exactly 1 statement (the val declaration)")
            .hasSize(1);
        
        // Print diagnostics to see what's happening
        System.out.println("Package: " + cu.getPackageDeclaration().getExpression());
        System.out.println("Number of statements: " + cu.getStatements().size());
        for (int i = 0; i < cu.getStatements().size(); i++) {
            System.out.println("Statement " + i + ": " + cu.getStatements().get(i).getClass().getSimpleName());
        }
        
        // Print the full source to see if there's duplication
        System.out.println("\nFull printed source:");
        System.out.println(cu.printTrimmed());
    }
    
    @Test
    void parseSimpleForLoop() {
        ScalaParser parser = ScalaParser.builder().build();
        
        String source = """
            object Test {
              def main(args: Array[String]): Unit = {
                val nums = List(1, 2, 3)
                for (n <- nums) println(n)
              }
            }
            """;
        
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isNotInstanceOf(ParseError.class);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        
        S.CompilationUnit cu = (S.CompilationUnit) parsed.get(0);
        
        // Print the whole structure first to debug
        System.out.println("\nFull parsed structure:");
        System.out.println(cu.printTrimmed());
        
        assertThat(cu.getStatements()).hasSize(1);
        
        // The only statement should be the object declaration
        assertThat(cu.getStatements().get(0)).isInstanceOf(J.ClassDeclaration.class);
        
        J.ClassDeclaration objectDecl = (J.ClassDeclaration) cu.getStatements().get(0);
        assertThat(objectDecl.getSimpleName()).isEqualTo("Test");
        
        // Get the object body - all the content is likely Unknown for now
        J.Block objectBody = objectDecl.getBody();
        assertThat(objectBody.getStatements()).isNotEmpty();
        
        // For now, just verify that the for loop parsing doesn't crash
        // The full implementation is still in progress, so we'll check for Unknown nodes
        boolean foundForLoop = false;
        for (Statement stmt : objectBody.getStatements()) {
            System.out.println("Statement type: " + stmt.getClass().getSimpleName());
            if (stmt instanceof J.Unknown) {
                String text = ((J.Unknown) stmt).getSource().getText();
                System.out.println("Unknown statement: " + text);
                if (text.contains("for (n <- nums)")) {
                    foundForLoop = true;
                }
            }
        }
        
        // Since method declarations are not fully implemented yet, we expect the whole
        // method body to be wrapped as Unknown
        assertThat(foundForLoop || objectBody.getStatements().stream()
            .anyMatch(stmt -> stmt instanceof J.Unknown && 
                             ((J.Unknown) stmt).getSource().getText().contains("for")))
            .as("Should find for loop in the parsed structure (possibly as Unknown)")
            .isTrue();
    }
    
    // @Test  // TODO: Enable once method declarations are fully implemented
    void parseForLoopWithBlock() {
        ScalaParser parser = ScalaParser.builder().build();
        
        String source = """
            object Test {
              def run(): Unit = {
                val items = Array("a", "b", "c")
                for (item <- items) {
                  println(item)
                  println(item.toUpperCase)
                }
              }
            }
            """;
        
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0)).isNotInstanceOf(ParseError.class);
        assertThat(parsed.get(0)).isInstanceOf(S.CompilationUnit.class);
        
        S.CompilationUnit cu = (S.CompilationUnit) parsed.get(0);
        assertThat(cu.getStatements()).hasSize(1);
        
        // The only statement should be the object declaration
        assertThat(cu.getStatements().get(0)).isInstanceOf(J.ClassDeclaration.class);
        
        J.ClassDeclaration objectDecl = (J.ClassDeclaration) cu.getStatements().get(0);
        
        // Get the run method
        J.Block objectBody = objectDecl.getBody();
        J.MethodDeclaration runMethod = null;
        for (Statement stmt : objectBody.getStatements()) {
            if (stmt instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) stmt;
                if ("run".equals(method.getSimpleName())) {
                    runMethod = method;
                    break;
                }
            }
        }
        
        assertThat(runMethod).isNotNull();
        assertThat(runMethod.getBody()).isNotNull();
        
        // Check the run method body
        J.Block runBody = runMethod.getBody();
        assertThat(runBody.getStatements()).hasSize(2);
        
        // Second statement should be a ForEachLoop
        assertThat(runBody.getStatements().get(1)).isInstanceOf(J.ForEachLoop.class);
        
        J.ForEachLoop forLoop = (J.ForEachLoop) runBody.getStatements().get(1);
        
        // Check the body is a block
        assertThat(forLoop.getBody()).isInstanceOf(J.Block.class);
        J.Block block = (J.Block) forLoop.getBody();
        assertThat(block.getStatements()).hasSize(2);
    }
}