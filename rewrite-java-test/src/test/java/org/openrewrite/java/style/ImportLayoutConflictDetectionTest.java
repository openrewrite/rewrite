/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class ImportLayoutConflictDetectionTest {

    private static final ImportLayoutStyle LAYOUT = ImportLayoutStyle.builder()
      .classCountToUseStarImport(3)
      .nameCountToUseStarImport(3)
      .importAllOthers()
      .importStaticAllOthers()
      .build();

    @Test
    void classpathIsNotReadWhenNoImportCanFold() {
        ImportLayoutStyle neverFolds = ImportLayoutStyle.builder()
          .classCountToUseStarImport(999)
          .nameCountToUseStarImport(999)
          .importAllOthers()
          .importStaticAllOthers()
          .build();
        List<J.CompilationUnit> cus = parse(
          "import java.util.ArrayList; import java.util.HashMap; import java.util.HashSet; class T {}",
          "import java.util.List; class U {}"
        );
        List<JRightPadded<J.Import>> imports = cus.get(0).getPadding().getImports();
        J.Import toAdd = cus.get(1).getImports().get(0);

        assertThat(neverFolds.orderImports(imports, UNREADABLE)).hasSize(3);
        assertThat(neverFolds.addImport(imports, toAdd, null, UNREADABLE)).hasSize(4);
    }

    @Test
    void classNameConflictsDecideFoldingOnEveryUseOfAClasspath() {
        List<JavaType.FullyQualified> classpath = JavaSourceSet.build("main", emptyList()).getClasspath();
        J.CompilationUnit cu = parse(
          "import java.awt.List; import java.util.ArrayList; import java.util.HashMap; import java.util.HashSet; class T {}"
        ).get(0);
        J.CompilationUnit noConflict = parse(
          "import java.util.ArrayList; import java.util.HashMap; import java.util.HashSet; class V {}"
        ).get(0);

        // java.util.* would make List ambiguous with java.awt.List. The second use reads the classpath's index.
        String first = print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        String second = print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        assertThat(first).doesNotContain("java.util.*");
        assertThat(second).isEqualTo(first);
        assertThat(print(noConflict, LAYOUT.orderImports(noConflict.getPadding().getImports(), classpath)))
          .contains("java.util.*");

        // a.b.* would make String ambiguous with java.lang.String
        J.CompilationUnit shadowsJavaLang = parse("import a.b.String; import a.b.C1; import a.b.C2; class U {}").get(0);
        assertThat(print(shadowsJavaLang, LAYOUT.orderImports(shadowsJavaLang.getPadding().getImports(), classpath)))
          .doesNotContain("a.b.*");
    }

    @Test
    void staticMemberConflictPreventsFoldingOnEveryUseOfAClasspath() {
        List<J.CompilationUnit> cus = parse(
          "package x.y; public class Util { public static void a() {} public static void b() {} public static void c() {} public static void d() {} }",
          "package p.q; public class Other { public static void d() {} }",
          "import static x.y.Util.a; import static x.y.Util.b; import static x.y.Util.c; import static p.q.Other.d; class T {}"
        );
        List<JavaType.FullyQualified> classpath = cus.subList(0, 2).stream()
          .map(cu -> cu.getClasses().get(0).getType())
          .collect(toList());
        J.CompilationUnit cu = cus.get(2);

        // x.y.Util.* would import Util.d, which clashes with Other.d
        String first = print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        String second = print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        assertThat(first).doesNotContain("x.y.Util.*");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void staticMembersOfATypeWhosePackageIsImportedDoNotConflict() {
        List<J.CompilationUnit> cus = parse(
          "package x.y; public class Util { public static void a() {} public static void b() {} public static void c() {} public static void d() {} }",
          "package p.q; public class Other { public static void d() {} }",
          "import x.y.Util; import static x.y.Util.a; import static x.y.Util.b; import static x.y.Util.c; import static p.q.Other.d; class T {}"
        );
        List<JavaType.FullyQualified> classpath = cus.subList(0, 2).stream()
          .map(cu -> cu.getClasses().get(0).getType())
          .collect(toList());
        J.CompilationUnit cu = cus.get(2);

        String first = print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        String second = print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        assertThat(first).contains("x.y.Util.*");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void classpathChangedInPlaceIsReadAgain() {
        List<J.CompilationUnit> cus = parse(
          "package d.e; public class X { }",
          "package d.e; public class C2 { }",
          "import a.b.C1; import a.b.C2; import a.b.C3; import d.e.X; class T { }"
        );
        List<JavaType.FullyQualified> classpath = new ArrayList<>(singletonList(cus.get(0).getClasses().get(0).getType()));
        J.CompilationUnit cu = cus.get(2);
        print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath));
        assertThat(print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath))).contains("a.b.*");

        // d.e.C2 now makes a.b.* ambiguous with the import of d.e.X
        classpath.add(cus.get(1).getClasses().get(0).getType());
        assertThat(print(cu, LAYOUT.orderImports(cu.getPadding().getImports(), classpath))).doesNotContain("a.b.*");
    }

    private static final Collection<JavaType.FullyQualified> UNREADABLE = new AbstractCollection<JavaType.FullyQualified>() {
        @Override
        public Iterator<JavaType.FullyQualified> iterator() {
            throw new AssertionError("The conflict check read the classpath");
        }

        @Override
        public int size() {
            return 1;
        }
    };

    private static List<J.CompilationUnit> parse(String... sources) {
        return JavaParser.fromJavaVersion().build().parse(sources)
          .map(J.CompilationUnit.class::cast)
          .collect(toList());
    }

    private static String print(J.CompilationUnit cu, List<JRightPadded<J.Import>> imports) {
        Cursor cuCursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), cu);
        return imports.stream()
          .map(i -> i.getElement().printTrimmed(cuCursor))
          .collect(joining("\n"));
    }
}
