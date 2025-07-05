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
package org.openrewrite.java;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.ImportAnalyzer.ImportStatus;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportAnalyzerTest {

    private ImportAnalyzer importManager;
    private J.CompilationUnit compilationUnit;

    @BeforeAll
    void init() {
        // language=java
        compilationUnit = parse(
          """
          import static java.util.Collections.emptyList; // used static import
          import static java.util.Collections.*; // used wildcard static import
          import java.util.*; // used wildcard import
          import java.util.stream.Stream; // used explicit import
          import foo.bar.Record; // class name same as Record class from java.lang
          import java.util.function.Supplier; // unused type import
          import static java.util.stream.Collectors.toList; // unused member import
          import static java.time.LocalDateTime.*; // unused wildcard member import

          class A {
              static final String MSG = "foo";
              List<String> list = emptyList();
              List<String> list2 = new B().list;
          
              void bar() {
                  foo();
                  Stream<String> stream = list.stream();
                  sort(list);
                  Record record = new Record();
                  Map<String, String> map = new java.util.concurrent.ConcurrentHashMap<>(); // fully-qualified, import not needed.
              }
          
              static class B {
                  List<String> list = new ArrayList<>();
          
                  void foo() {
                      list.add(MSG);
                  }
              }
          }
          """, """
          package foo.bar;
          
          public class Record {}
          """);

        importManager = ImportAnalyzer.init(compilationUnit);
    }

    @Test
    void basicVerification() {
        // Always empty for code with no compilation error.
        assertThat(importManager.getAmbiguousImports()).isEmpty();
        assertThat(importManager.getMissingTypeImports()).isEmpty();
        assertThat(importManager.getMissingMemberImports()).isEmpty();

        // Assertions for unused imports
        assertThat(importManager.getUnusedImports()).hasSize(3);
        assertThat(importManager.getUnusedImports())
          .extracting(ImportAnalyzer.ImportEntry::getImportName)
          .containsExactlyInAnyOrder("java.util.function.Supplier", "java.util.stream.Collectors.toList", "java.time.LocalDateTime.*");
        assertThat(importManager.getUnusedImports())
          .filteredOn(ImportAnalyzer.ImportEntry::isMemberImport)
          .hasSize(2);
    }

    @ParameterizedTest
    @CsvSource({
      "java.util.Map, IMPLICITLY_IMPORTED",
      "java.util.stream.Stream, EXPLICITLY_IMPORTED",
      "java.lang.Record, IMPORT_AMBIGUITY",
      "foo.bar.List, IMPORT_AMBIGUITY", // import ambiguity due to a wildcard import
      "java.util.stream.IntStream, NOT_IMPORTED",
    })
    void checkImportForType(String typeName, ImportStatus status) {
        assertThat(importManager.checkTypeForImport(typeName)).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource({
      "java.time.LocalDateTime, MIN, IMPLICITLY_IMPORTED",
      "java.util.Collections, emptyList, EXPLICITLY_IMPORTED",
      "foo.bar.List, emptyList, IMPORT_AMBIGUITY",
      "foo.bar.List, MIN, NOT_IMPORTED", // a wildcard import declares, but not used so safe to import.
      "java.util.Comparator, comparing, NOT_IMPORTED",
    })
    void checkImportForMember(String typeName, String memberName, ImportStatus status) {
        assertThat(importManager.checkMemberForImport(typeName, memberName)).isEqualTo(status);
    }

    @Test
    void missingTypeImport() {
        JavaTemplate template = JavaTemplate.builder("Map<String, String> concurrentMap = new ConcurrentHashMap<>()")
          .imports("java.util.concurrent.ConcurrentHashMap", "java.util.Map")
          .contextSensitive()
          .build();
        J j = new AddStatement(template).visit(compilationUnit, new InMemoryExecutionContext());
        assert j instanceof J.CompilationUnit;
        J.CompilationUnit cu = (J.CompilationUnit) j;
        ImportAnalyzer importManager = ImportAnalyzer.init(cu);
        assertThat(importManager.getMissingTypeImports())
          .extracting(JavaType.FullyQualified::getFullyQualifiedName)
          .containsOnly("java.util.concurrent.ConcurrentHashMap");
    }

    @Test
    void missingMemberImport() {
        JavaTemplate template = JavaTemplate.builder("Stream<Integer> s = of(1, 2, 3);")
          .imports("java.util.stream.Stream")
          .staticImports("java.util.stream.Stream.of")
          .contextSensitive()
          .build();
        J j = new AddStatement(template).visit(compilationUnit, new InMemoryExecutionContext());
        assert j instanceof J.CompilationUnit;
        J.CompilationUnit cu = (J.CompilationUnit) j;
        ImportAnalyzer importManager = ImportAnalyzer.init(cu);
        assertThat(importManager.getMissingMemberImports())
          .extracting(ImportAnalyzer::toMemberName)
          .containsOnly("java.util.stream.Stream.of");
    }

    @Test
    void ambiguousImport() {
        JavaTemplate template = JavaTemplate.builder("sort(new int[]{1, 2, 3})")
          .staticImports("java.util.Arrays.sort")
          .contextSensitive()
          .build();
        J j = new AddStatement(template).visit(compilationUnit, new InMemoryExecutionContext());
        assert j instanceof J.CompilationUnit;
        J.CompilationUnit cu = (J.CompilationUnit) j;
        ImportAnalyzer importManager = ImportAnalyzer.init(cu);
        assertThat(importManager.getMissingMemberImports())
          .extracting(ImportAnalyzer::toMemberName)
          .containsOnly("java.util.Arrays.sort");
    }

    private static J.CompilationUnit parse(String source, String dependsOn) {
        JavaParser parser = JavaParser.fromJavaVersion()
          .dependsOn(dependsOn)
          .build();
        List<SourceFile> sourceFiles = parser
          .parseInputs(List.of(Parser.Input.fromString(source)), null, new InMemoryExecutionContext()).toList();
        assert sourceFiles.size() == 1;
        assert sourceFiles.get(0) instanceof J.CompilationUnit;
        return (J.CompilationUnit) sourceFiles.get(0);
    }

    @AllArgsConstructor
    private static class AddStatement extends JavaIsoVisitor<ExecutionContext> {

        private final JavaTemplate statement;

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDecl, ExecutionContext ctx) {
            return statement.apply(getCursor(), methodDecl.getBody().getCoordinates().firstStatement());
        }
    }
}
