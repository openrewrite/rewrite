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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

class RemoveUnusedImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveUnusedImports());
    }

    @DocumentExample
    @Test
    void removeNamedImport() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class A {}
              """,
            "class A {}")
        );
    }

    @Test
    void enumsFromInnerClass() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Outer {
                  public enum E {
                      A, B, C, D
                  }
              }
              """
          ),
          java(
            """
              import static org.openrewrite.Outer.E.*;
              public class Test {
                  Object a = A;
                  Object b = B;
                  Object c = C;
              }
              """
          )
        );
    }

    @Test
    void enums() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public enum E {
                  A, B, C
              }
              """
          ),
          java(
            """
              import org.openrewrite.E;
              import static org.openrewrite.E.A;
              import static org.openrewrite.E.B;
              public class Test {
                  E a = A;
                  E b = B;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/969")
    @Test
    void doNotRemoveInnerClassImport() {
        rewriteRun(
          java(
            """
              import java.util.Map.Entry;

              public abstract class MyMapEntry<K, V> implements Entry<K, V> {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1798")
    @Test
    void doNotRemoveInnerClassInSamePackage() {
        rewriteRun(
          java(
            """
              package java.util;

              import java.util.Map.Entry;

              public abstract class MyMapEntry<K, V> implements Entry<K, V> {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1052")
    @Test
    void usedInJavadocWithThrows() {
        rewriteRun(
          java(
            """
              import java.time.DateTimeException;

              class A {
                  /**
                   * @throws DateTimeException when ...
                   */
                  void foo() {}
              }
              """
          )
        );
    }

    @Test
    void usedInJavadoc() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Collection;

              /** {@link List} */
              class A {
                  /** {@link Collection} */
                  void foo() {}
              }
              """
          )
        );
    }

    @Test
    void leaveImportIfRemovedTypeIsStillReferredTo() {
        rewriteRun(
          java(
            """
              import java.util.List;
              class A {
                 List<Integer> list;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1251")
    @Test
    void leaveImportIfAnnotationOnEnum() {
        rewriteRun(
          java(
            """
              package com.google.gson.annotations;

              import java.lang.annotation.Documented;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Documented
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.FIELD, ElementType.METHOD})
              public @interface SerializedName {
                  String value();
              }
              """
          ),
          java(
            """
              import com.google.gson.annotations.SerializedName;

              public enum PKIState {
                  @SerializedName("active") ACTIVE,
                  @SerializedName("dismissed") DISMISSED
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/617")
    @Test
    void leaveImportForStaticImportEnumInAnnotation() {
        rewriteRun(
          java(
            """
              package org.openrewrite.test;

              public @interface YesOrNo {
                  Status status();
                  enum Status {
                      YES, NO
                  }
              }
              """
          ),
          java(
            """
              package org.openrewrite.test;

              import static org.openrewrite.test.YesOrNo.Status.YES;

              @YesOrNo(status = YES)
              public class Foo {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2722")
    @Test
    void removeAllImportsInFileWithHeader() {
        rewriteRun(
          java(
            """
              /*
               * header
               */
              package x;
              import java.util.List;
              class A {}
              """,
            """
              /*
               * header
               */
              package x;

              class A {}
              """
          )
        );
    }

    @Test
    void removeStarImportIfNoTypesReferredTo() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class A {}
              """,
            "class A {}"
          )
        );
    }

    @Test
    void replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains() {
        rewriteRun(
          java(
            """
              import java.util.*;

              class A {
                  Collection<Integer> c;
              }
              """,
            """
              import java.util.Collection;

              class A {
                  Collection<Integer> c;
              }
              """
          )
        );
    }

    @Test
    void unfoldIfLessThanStarCount() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class A {
                 Collection<Integer> c;
                 Set<Integer> s = new HashSet<>();
              }
              """,
            """
              import java.util.Collection;
              import java.util.HashSet;
              import java.util.Set;
              class A {
                 Collection<Integer> c;
                 Set<Integer> s = new HashSet<>();
              }
              """
          )
        );
    }

    @Test
    void leaveStarImportInPlaceIfMoreThanStarCount() {
        rewriteRun(
          java(
            """
              import java.util.*;
              class A {
                 Collection<Integer> c;
                 Set<Integer> s = new HashSet<>();
                 Map<String, String> m = new HashMap<>();
                 List<String> l = new ArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void removeStarStaticImport() {
        rewriteRun(
          java(
            """
              import static java.util.Collections.*;
              class A {}
              """,
            "class A {}"
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/687")
    @Test
    void leaveStarStaticImportIfReferenceStillExists() {
        rewriteRun(
          java(
            """
              import static java.util.Collections.*;
              class A {
                 Object o = emptyList();
              }
              """,
            """
              import static java.util.Collections.emptyList;
              class A {
                 Object o = emptyList();
              }
              """
          )
        );
    }

    @Test
    void removeStaticImportIfNotReferenced() {
        rewriteRun(
          java(
            """
              import java.time.DayOfWeek;

              import static java.time.DayOfWeek.MONDAY;
              import static java.time.DayOfWeek.TUESDAY;

              class WorkWeek {
                  DayOfWeek shortWeekStarts(){
                      return TUESDAY;
                  }
              }
              """,
            """
              import java.time.DayOfWeek;

              import static java.time.DayOfWeek.TUESDAY;

              class WorkWeek {
                  DayOfWeek shortWeekStarts(){
                      return TUESDAY;
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveNamedStaticImportIfReferenceStillExists() {
        rewriteRun(

          java(
            """
              import static java.util.Collections.emptyList;
              import static java.util.Collections.emptySet;

              class A {
                 Object o = emptyList();
              }
              """,
            """
              import static java.util.Collections.emptyList;

              class A {
                 Object o = emptyList();
              }
              """
          )
        );
    }

    @Test
    void leaveNamedStaticImportOnFieldIfReferenceStillExists() {
        rewriteRun(
          java(
            """
              package foo;
              public class B {
                  public static final String STRING = "string";
                  public static final String STRING2 = "string2";
              }
              """
          ),
          java(
            """
              package foo;
              public class C {
                  public static final String ANOTHER = "string";
              }
              """
          ),
          java(
            """
              import static foo.B.STRING;
              import static foo.B.STRING2;
              import static foo.C.*;

              public class A {
                  String a = STRING;
              }
              """,
            """
              import static foo.B.STRING;

              public class A {
                  String a = STRING;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/429")
    @Test
    void removePackageInfoImports() {
        rewriteRun(
          java(
            """
              package foo;
              public @interface FooAnnotation {}
              public @interface Foo {}
              public @interface Bar {}
              """
          ),
          java(
            """
              @Foo
              @Bar
              package foo.bar.baz;

              import foo.Bar;
              import foo.Foo;
              import foo.FooAnnotation;
              """,
            """
              @Foo
              @Bar
              package foo.bar.baz;

              import foo.Bar;
              import foo.Foo;
              """
          )
        );
    }

    @Test
    void removePackageInfoStarImports() {
        rewriteRun(
          java(
            """
              package foo;
              public @interface FooAnnotation {}
              public @interface Foo {}
              public @interface Bar {}
              """
          ),
          java(
            """
              @Foo
              @Bar
              package foo.bar.baz;

              import foo.*;
              """,
            """
              @Foo
              @Bar
              package foo.bar.baz;

              import foo.Bar;
              import foo.Foo;
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/594")
    @Test
    void dontRemoveStaticReferenceToPrimitiveField() {
        rewriteRun(
          java(
            """
              import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
              public class A {
                  int t = TYPE_FORWARD_ONLY;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/580")
    @Test
    void resultSetType() {
        rewriteRun(
          java(
            """
              import java.sql.ResultSet;
              public class A {
                  int t = ResultSet.TYPE_FORWARD_ONLY;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/701")
    @Test
    void ensuresWhitespaceAfterPackageDeclarationNoImportsRemain() {
        rewriteRun(
          java(
            """
              package com.example.foo;
              import java.util.List;
              public class A {
              }
              """,
            """
              package com.example.foo;

              public class A {
              }
              """
          )
        );
    }

    @Test
    void doesNotAffectClassBodyFormatting() {
        rewriteRun(

          java(
            """
              package com.example.foo;

              import java.util.List;
              import java.util.ArrayList;

              public class A {
              // Intentionally misaligned to ensure formatting is not overzealous
              ArrayList<String> foo = new ArrayList<>();
              }
              """,
            """
              package com.example.foo;

              import java.util.ArrayList;

              public class A {
              // Intentionally misaligned to ensure formatting is not overzealous
              ArrayList<String> foo = new ArrayList<>();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/845")
    @Test
    void doesNotRemoveStaticReferenceToNewClass() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Bar {
                  public static final class Buz {
                      public Buz() {}
                  }
              }
              """
          ),
          java(
            """
              package foo.test;

              import static org.openrewrite.Bar.Buz;

              public class Test {
                  private void method() {
                      new Buz();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    void doNotUnfoldStaticValidWildCard() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Foo {
                  public static final int FOO_CONSTANT = 10;
                  public static final class Bar {
                      private Bar() {}
                      public static void helper() {}
                  }
                  public static void fooMethod() {}
              }
              """
          ),
          java(
            """
              package foo.test;

              import static org.openrewrite.Foo.*;

              public class Test {
                  int var = FOO_CONSTANT;
                  private void method() {
                      fooMethod();
                      Bar.helper();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/877")
    @Test
    void unfoldStaticUses() {
        rewriteRun(
          java(
            """
              package org.openrewrite;
              public class Foo {
                  public static final int FOO_CONSTANT = 10;
                  public static final class Bar {
                      private Bar(){}
                      public static void helper() {}
                  }
                  public static void fooMethod() {}
              }
              """
          ),
          java(
            """
              package foo.test;

              import static org.openrewrite.Foo.*;

              public class Test {
                  int var = FOO_CONSTANT;
                  private void method() {
                      Bar.helper();
                  }
              }
              """,
            """
              package foo.test;

              import static org.openrewrite.Foo.FOO_CONSTANT;
              import static org.openrewrite.Foo.Bar;

              public class Test {
                  int var = FOO_CONSTANT;
                  private void method() {
                      Bar.helper();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotUnfoldPackage() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), singletonList(
                ImportLayoutStyle.builder()
                  .packageToFold("java.util.*")
                  .staticPackageToFold("java.util.Collections.*")
                  .importAllOthers()
                  .importStaticAllOthers()
                  .build()
              )
              )
            )
          )),
          java(
            """
              import java.util.*;

              import static java.util.Collections.*;

              class Test {
                  List<String> l = emptyList();
              }
              """
          )
        );
    }

    @Test
    void unfoldSubpackage() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), singletonList(
                ImportLayoutStyle.builder()
                  .packageToFold("java.util.*", false)
                  .staticPackageToFold("java.util.*", false)
                  .importAllOthers()
                  .importStaticAllOthers()
                  .build()
              )
              )
            )
          )),
          java(
            """
              import java.util.concurrent.*;

              import static java.util.Collections.*;

              class Test {
                  Object o = emptyMap();
                  ConcurrentHashMap<String, String> m;
              }
              """,
            """
              import java.util.concurrent.ConcurrentHashMap;

              import static java.util.Collections.emptyMap;

              class Test {
                  Object o = emptyMap();
                  ConcurrentHashMap<String, String> m;
              }
              """
          )
        );
    }

    @Test
    void doNotUnfoldSubpackage() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(
            singletonList(
              new NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), singletonList(
                ImportLayoutStyle.builder()
                  .packageToFold("java.util.*", true)
                  .staticPackageToFold("java.util.*", true)
                  .importAllOthers()
                  .importStaticAllOthers()
                  .build()
              )
              )
            )
          )),
          java(
            """
              import java.util.concurrent.*;

              import static java.util.Collections.*;

              class Test {
                  ConcurrentHashMap<String, String> m = new ConcurrentHashMap<>(emptyMap());
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1509")
    @Test
    void removeImportsForSamePackage() {
        rewriteRun(
          java(
            """
              package com.google.gson.annotations;

              import java.lang.annotation.Documented;
              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              @Documented
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.FIELD, ElementType.METHOD})
              public @interface SerializedName {
                  String value();
              }
              """
          ),
          java(
            """
              package com.google.gson.annotations;

              import com.google.gson.annotations.SerializedName;

              public enum PKIState {
                  @SerializedName("active") ACTIVE,
                  @SerializedName("dismissed") DISMISSED
              }
              """,
            """
              package com.google.gson.annotations;

              public enum PKIState {
                  @SerializedName("active") ACTIVE,
                  @SerializedName("dismissed") DISMISSED
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1698")
    @Test
    void correctlyRemoveImportsFromLowerCaseClassNames() {
        rewriteRun(
          java(
            """
              package com.source;

              public class a {
                  public static final short SHORT1 = (short)1;
                  public static final short SHORT2 = (short)2;
              }
              """
          ),
          java(
            """
              package com.test;

              import static com.source.a.SHORT1;
              import static com.source.a.SHORT2;

              class Test {
                  short uniqueCount = SHORT1;
              }
              """,
            """
              package com.test;

              import static com.source.a.SHORT1;

              class Test {
                  short uniqueCount = SHORT1;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1698")
    @Test
    void correctlyRemoveImportsFromUpperCasedPackages() {
        rewriteRun(
          java(
            """
              package com.Source.$;

              public class A {
                  public static final short SHORT1 = (short)1;
                  public static final short SHORT2 = (short)2;
              }
              """
          ),
          java(
            """
              package com.test;

              import static com.Source.$.A.SHORT1;
              import static com.Source.$.A.SHORT2;

              class Test {
                  short uniqueCount = SHORT1;
              }
              """,
            """
              package com.test;

              import static com.Source.$.A.SHORT1;

              class Test {
                  short uniqueCount = SHORT1;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1698")
    @Test
    void correctlyRemoveImportsFromInternalClasses() {
        rewriteRun(
          java(
            """
              package com.Source.A;

              public class B {
                public enum Enums {
                    B1, B2
                }

                public static void helloWorld() {
                    System.out.println("hello world!");
                }
              }
              """
          ),
          java(
            """
              package com.test;

              import static com.Source.A.B.Enums.B1;
              import static com.Source.A.B.Enums.B2;
              import static com.Source.A.B.helloWorld;

              class Test {
                  public static void main(String[] args) {
                    var uniqueCount = B1;
                    helloWorld();
                  }
              }
              """,
            """
              package com.test;

              import static com.Source.A.B.Enums.B1;
              import static com.Source.A.B.helloWorld;

              class Test {
                  public static void main(String[] args) {
                    var uniqueCount = B1;
                    helloWorld();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1698")
    @Test
    void correctlyRemoveImportsFromNestedInternalClasses() {
        rewriteRun(
          java(
            """
              package com.Source.A;

              public class B {
                public enum Enums {
                    B1, B2
                }

                public static class C {
                    public enum Enums {
                        C1, C2
                    }
                }

                public static void helloWorld() {
                    System.out.println("hello world!");
                }
              }
              """
          ),
          java(
            """
              package com.test;

              import static com.Source.A.B.Enums.B1;
              import static com.Source.A.B.Enums.B2;
              import static com.Source.A.B.C.Enums.C1;
              import static com.Source.A.B.C.Enums.C2;
              import static com.Source.A.B.helloWorld;

              class Test {
                  public static void main(String[] args) {
                    var uniqueCount = B1;
                    var uniqueCountNested = C1;
                    helloWorld();
                  }
              }
              """,
            """
              package com.test;

              import static com.Source.A.B.Enums.B1;
              import static com.Source.A.B.C.Enums.C1;
              import static com.Source.A.B.helloWorld;

              class Test {
                  public static void main(String[] args) {
                    var uniqueCount = B1;
                    var uniqueCountNested = C1;
                    helloWorld();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3275")
    @Test
    void doesNotRemoveReferencedClassesBeingUsedAsParameters() {
        rewriteRun(
          java(
            """
              package com.Source.mine;

              public class A {
                  public static final short SHORT1 = (short)1;

                  public short getShort1() {
                    return SHORT1;
                  }
              }
              """
          ),
          java(
            """
              package com.test;

              import com.Source.mine.A;

              class Test {
                  void f(A classA) {
                    classA.getShort1();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3275")
    @Test
    void doesNotRemoveReferencedClassesBeingUsedAsParametersUnusualPackageName() {
        rewriteRun(
          java(
            """
              package com.Source.$;

              public class A {
                  public static final short SHORT1 = (short)1;

                  public short getShort1() {
                    return SHORT1;
                  }
              }
              """
          ),
          java(
            """
              package com.test;

              import com.Source.$.A;

              class Test {
                  void f(A classA) {
                    classA.getShort1();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3275")
    @Test
    void doesNotRemoveWildCardImport() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              package com.Source.mine;

              public class A {
                  public void f() {}
              }
              """
          ),
          java(
            """
              package com.Source.mine;

              public class B {
                  public void f() {}
              }
              """
          ),
          java(
            """
              package com.Source.mine;

              public class C {
                  public void f() {}
              }
              """
          ),
          java(
            """
              package com.Source.mine;

              public class Record {
                  public A theOne() { return new A(); }
                  public B theOther1() { return new B(); }
                  public C theOther2() { return new C(); }
              }
              """
          ),
          java(
            """
              package com.test;

              import com.Source.mine.Record;
              import com.Source.mine.*;

              class Test {
                  void f(Record r) {
                    A a = r.theOne();
                    B b = r.theOther1();
                    C c = r.theOther2();
                  }
              }
              """,
            """
              package com.test;

              import com.Source.mine.Record;
              import com.Source.mine.A;
              import com.Source.mine.B;
              import com.Source.mine.C;

              class Test {
                  void f(Record r) {
                    A a = r.theOne();
                    B b = r.theOther1();
                    C c = r.theOther2();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3275")
    @Test
    void doesNotUnfoldWildCardImport() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnusedImports()),
          java(
            """
              package com.Source.mine;

              public class A {
                  public void f() {}
              }
              """
          ),
          java(
            """
              package com.Source.mine;

              public class B {
                  public void f() {}
              }
              """
          ),
          java(
            """
              package com.Source.mine;

              public class C {
                  public void f() {}
              }
              """
          ),
          java(
            """
              package com.Source.mine;

              public class Record {
                  public A theOne() { return new A(); }
                  public B theOther1() { return new B(); }
                  public C theOther2() { return new C(); }
              }
              """
          ),
          java(
            """
              package com.test;

              import com.Source.mine.Record;
              import com.Source.mine.A;
              import com.Source.mine.B;
              import com.Source.mine.C;

              class Test {
                  void f(Record r) {
                    A a = r.theOne();
                    B b = r.theOther1();
                    C c = r.theOther2();
                  }
              }
              """
          )
        );
    }

    @Test
    void removeImportUsedAsLambdaParameter() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.function.Function;

              public class Test {
                  public static void foo(){
                      final HashMap<Integer,String> map = new HashMap<>();
                      map.computeIfAbsent(3, integer -> String.valueOf(integer + 1));
                  }
              }
              """,
            """
              import java.util.HashMap;

              public class Test {
                  public static void foo(){
                      final HashMap<Integer,String> map = new HashMap<>();
                      map.computeIfAbsent(3, integer -> String.valueOf(integer + 1));
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2920")
    @Test
    void removeImportUsedAsImpliedLambdaParameter() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnusedImports()),
          java(
            """              
              import java.awt.KeyEventDispatcher;
              import java.awt.event.KeyEvent;
              
              class Test {
                  KeyEventDispatcher foo() {
                      return ked -> true;
                  }
              }
              """,
            """              
              import java.awt.KeyEventDispatcher;
              
              class Test {
                  KeyEventDispatcher foo() {
                      return ked -> true;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2920")
    @Test
    void retainImportUsedAsExplicitLambdaParameter() {
        rewriteRun(
          spec -> spec.recipe(new RemoveUnusedImports()),
          java(
            """              
              import java.awt.KeyEventDispatcher;
              import java.awt.event.KeyEvent;
              
              class Test {
                  KeyEventDispatcher foo() {
                      return (KeyEvent ked) -> true;
                  }
              }
              """
          )
        );
    }

    @Test
    void removeImportUsedAsMethodParameter() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.ArrayList;
              import java.util.Set;

              public class Test {
                  public static void foo(){
                      new ArrayList<>(new HashMap<Integer, String>().keySet());
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.ArrayList;

              public class Test {
                  public static void foo(){
                      new ArrayList<>(new HashMap<Integer, String>().keySet());
                  }
              }
              """
          )
        );
    }

    @Test
    void removeMultipleDirectImports() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.Collection;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.emptyList;

              class A {
                 Collection<Integer> c = emptyList();
              }
              """,
            """
              import java.util.Collection;

              import static java.util.Collections.emptyList;

              class A {
                 Collection<Integer> c = emptyList();
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void removeMultipleWildcardImports() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, false);
        rewriteRun(
          spec -> spec.executionContext(ctx),
          java(
            """
              import java.util.*;
              import java.util.*;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.singletonList;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = new HashSet<>();
                 List<String> l = singletonList("a");
              }
              """,
            """
              import java.util.*;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.singletonList;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = new HashSet<>();
                 List<String> l = singletonList("a");
              }
              """
          )
        );
    }

    @Test
    void removeWildcardImportWithDirectImport() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              import java.util.*;
              import java.util.Collection;

              import static java.util.Collections.*;
              import static java.util.Collections.emptyList;

              class A {
                 Collection<Integer> c = emptyList();
              }
              """,
            """
              import java.util.Collection;

              import static java.util.Collections.emptyList;

              class A {
                 Collection<Integer> c = emptyList();
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void removeDirectImportWithWildcardImport() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, false);
        rewriteRun(
          spec -> spec.executionContext(ctx),
          java(
            """
              import java.util.Set;
              import java.util.*;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.*;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = emptySet();
                 List<String> l = singletonList("c");
                 Iterator<Short> i = emptyIterator();
              }
              """,
            """
              import java.util.*;

              import static java.util.Collections.*;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = emptySet();
                 List<String> l = singletonList("c");
                 Iterator<Short> i = emptyIterator();
              }
              """
          )
        );
    }

    @Test
    void removeMultipleImportsWhileUnfoldingWildcard() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              import java.util.Set;
              import java.util.*;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.*;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = emptySet();
              }
              """,
            """
              import java.util.Set;
              import java.util.Collection;

              import static java.util.Collections.emptyList;
              import static java.util.Collections.emptySet;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = emptySet();
              }
              """
          )
        );
    }

    @Test
    void keepInnerClassImports() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.Map.Entry;

              import static java.util.Collections.*;

              class A {
                 Collection<Integer> c = emptyList();
                 Set<Integer> s = emptySet();
                 List<String> l = List.of("c","b","a");
                 Iterator<Short> i = emptyIterator();
                 Entry<String, Integer> entry;
              }
              """
          )
        );
    }

    // If multiple packages have conflicting class names,
    // so have to explicitly import the specific class to avoid ambiguity.
    @Test
    void multiplePackagesHaveConflictingClassNames() {
        rewriteRun(
          java(
            """
              package org.b;
              public class Pair<K, V> {
                  K key;
                  V value;
              }
              """
          ),
          java(
            """
              package org.b;
              public class Table<T> { }
              """
          ),
          java(
            """
              package org.c;
              public class Pair<K, V> {
                  K key;
                  V value;
              }
              """
          ),
          java(
            """
              package org.c;
              public class Table<T> { }
              """
          ),
          java(
            """
              package org.b;
              public class B1 { }
              """
          ),
          java(
            """
              package org.b;
              public class B2 { }
              """
          ),
          java(
            """
              package org.b;
              public class B3 { }
              """
          ),
          java(
            """
              package org.b;
              public class B4 { }
              """
          ),
          java(
            """
              package org.c;
              public class C1 {}
              """
          ),
          java(
            """
              package org.c;
              public class C2 {}
              """
          ),
          java(
            """
              package org.c;
              public class C3 {}
              """
          ),
          java(
            """
              package org.c;
              public class C4 {}
              """
          ),
          java(
            """
              package org.a;

              import org.b.*;
              import org.b.Pair;
              import org.c.*;
              import org.c.Table;

              class A {
                  void method() {
                      Pair<String, String > pair = new Pair<>();
                      Table<String> table = new Table<>();
                      B1 b1 = new B1();
                      B2 b2 = new B2();
                      B3 b3 = new B3();
                      B4 b4 = new B4();
                      C1 c1 = new C1();
                      C2 c2 = new C2();
                      C3 c3 = new C3();
                      C4 c4 = new C4();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3607")
    @Test
    void conflictWithRecord() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package pzrep.p1;

              public class Record {
                public A theOne() { return new A(); }
                public B0 theOther0() { return new B0(); }
                public B1 theOther1() { return new B1(); }
                public B2 theOther2() { return new B2(); }
                public B3 theOther3() { return new B3(); }
                public B4 theOther4() { return new B4(); }
                public B5 theOther5() { return new B5(); }
                public B6 theOther6() { return new B6(); }
                public B7 theOther7() { return new B7(); }
                public B8 theOther8() { return new B8(); }
                public B9 theOther9() { return new B9(); }
              }
              """, """
              package pzrep.p1;

              public class A {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B0 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B1 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B2 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B3 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B4 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B5 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B6 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B7 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B8 {
                public void f() {}
              }
              """, """
              package pzrep.p1;

              public class B9 {
                public void f() {}
              }
              """
          )),
          java(
            """
              package pzrep.p2;

              import pzrep.p1.Record;
              import pzrep.p1.*;

              class Client2 {
                  void f(Record r) {
                    A a = r.theOne();
                    B0 b0 = r.theOther0();
                    B1 b1 = r.theOther1();
                    B2 b2 = r.theOther2();
                    B3 b3 = r.theOther3();
                    B4 b4 = r.theOther4();
                    B5 b5 = r.theOther5();
                    B6 b6 = r.theOther6();
                    B7 b7 = r.theOther7();
                    B8 b8 = r.theOther8();
                    B9 b9 = r.theOther9();
                  }
              }
              """
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3909")
    @Test
    void importUsedOnlyInReturnType() {
        // language=java
        rewriteRun(
          java(
            """
              import javax.xml.datatype.XMLGregorianCalendar;
              import java.time.LocalDateTime;
              class LocalDateTimeUtil {
                public LocalDateTime toLocalDateTime(XMLGregorianCalendar cal) {
                    if (cal == null) {
                        return null;
                    }
                    return cal.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4405")
    @Test
    void missingTypeReturnValueOfMethodWithArguments() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          // method with argument should still remove unused import
          java(
            """
              import java.util.List;

              public abstract class Foo1 {
                  public abstract String m(String a);
              }
              """,
            """
              public abstract class Foo1 {
                  public abstract String m(String a);
              }
              """
          ),
          // case with missing type should not remove the import
          java(
            """
              import java.util.List;
              import java.util.GenClass1;

              public abstract class Foo3 {
                  public abstract GenClass1 m();
              }
              """
          ),
          // case with missing type and method argument should not remove the import
          java(
            """
              import java.util.GenClass1;

              public abstract class Foo2 {
                  public abstract GenClass1 m(String a);
              }
              """
          )
        );
    }

    @Test
    void staticAmbiguousImport() {
        // language=java
        rewriteRun(
          java(
            """
              package org.a;
              public class Abc {
                public static String A = "A";
                public static String B = "B";
                public static String C = "C";
                public static String ALL = "%s%s%s".formatted(A, B, C);
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package org.b;
              public class Def {
                public static String D = "D";
                public static String E = "E";
                public static String F = "F";
                public static String ALL = "%s%s%s".formatted(D, E, F);
              }
              """,
            SourceSpec::skip
          ),
          // No change when removal would cause ambiguity
          java(
            """
              package org.test;

              import static org.a.Abc.*;
              import static org.a.Abc.ALL;
              import static org.b.Def.*;

              public class Foo {
                private String abc = ALL;
                private String a = A;
                private String b = B;
                private String c = C;
                private String d = D;
                private String e = E;
                private String f = F;
              }
              """
          ),
          // Do still remove unambiguous imports
          java(
            """
              package org.test;

              import static org.a.Abc.*;
              import static org.a.Abc.A;
              import static org.a.Abc.ALL;
              import static org.b.Def.*;

              public class Bar {
                private String abc = ALL;
                private String a = A;
                private String b = B;
                private String c = C;
                private String d = D;
                private String e = E;
                private String f = F;
              }
              """,
            """
              package org.test;

              import static org.a.Abc.*;
              import static org.a.Abc.ALL;
              import static org.b.Def.*;

              public class Bar {
                private String abc = ALL;
                private String a = A;
                private String b = B;
                private String c = C;
                private String d = D;
                private String e = E;
                private String f = F;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3283")
    @Test
    void nestedImport() {
        rewriteRun(
          java(
            """
              package foo;

              public interface J {
                  final class One implements J {}
                  final class Two implements J {}
                  final class Three implements J {}
                  final class Four implements J {}
                  final class Five implements J {}
                  final class Six implements J {}
              }
              """
          ),
          java(
            """
              package bar;

              import foo.J;
              import foo.J.*;

              class Quz {
                void test() {
                  J j = null;
                  One one = null;
                  Two two = null;
                  Three three = null;
                  Four four = null;
                  Five five = null;
                  Six six = null;
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3283")
    @Test
    void nestedImportStaticInnerClass() {
        rewriteRun(
          java(
            """
              package com.a.b.c;
              public final class ParentBaseClass {
                public static abstract class BaseImplClass {
                   void foo() {
                    }
                }
              }
              """
          ),
          java(
            """
              import com.a.b.c.ParentBaseClass.*;
              public class MyClass extends BaseImplClass {
                 @Override
                 public void foo() {
                 }
              }
              """,
            """
              import com.a.b.c.ParentBaseClass.BaseImplClass;
              public class MyClass extends BaseImplClass {
                 @Override
                 public void foo() {
                 }
              }
              """
          )
        );
    }

    @Test
    void retainExplicitImportWhenConflictingClassInSamePackage() {
        rewriteRun(
          java(
            """
              package com.a;

              class ConflictingClass {
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package com.b;

              public class ConflictingClass {
                  public ConflictingClass() {
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package com.c;

              import com.b.ConflictingClass;

              public class ImplProvider {
                  static ConflictingClass getImpl(){
                      return new ConflictingClass();
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              package com.a;

              import com.b.ConflictingClass;
              import com.c.ImplProvider;

              class CImpl {
                  ConflictingClass impl = ImplProvider.getImpl();
              }
              """
          )
        );
    }

    @Test
    void staticNestedInnerClass() {
        // language=java
        rewriteRun(
          java(
            """
              package a;

              public class A {
                  public final class B {}
                  public static class C {}
              }
              """,
            SourceSpec::skip),
          java(
            """
              import a.*;

              public class Foo {
                  A method(A.B ab, A.C ac) {}
              }
              """,
            """
              import a.A;

              public class Foo {
                  A method(A.B ab, A.C ac) {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5498")
    @Test
    void javaUtilMapEntry() {
        // language=java
        rewriteRun(
          java(
            """
              import java.util.*;

              public class Usages {
                  HashMap<String, String> hashMap;
                  Map<String, String> map;
                  Optional<String> optional;
                  Map.Entry<String, String> favoriteEntry;
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;
              import java.util.Optional;

              public class Usages {
                  HashMap<String, String> hashMap;
                  Map<String, String> map;
                  Optional<String> optional;
                  Map.Entry<String, String> favoriteEntry;
              }
              """
          ),
          java(
            """
              import java.util.*;

              public class WithoutMap {
                  Optional<String> optional;
                  Map.Entry<String, String> favoriteEntry;
              }
              """,
            """
              import java.util.Map;
              import java.util.Optional;

              public class WithoutMap {
                  Optional<String> optional;
                  Map.Entry<String, String> favoriteEntry;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5703")
    @Test
    void wildcardImportsWithConflictingNames() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              import java.sql.*;
              import java.sql.Date;
              import java.util.*;

              class Main {
                final Date date = new Date(123);
                final java.util.Date date2 = new java.util.Date(123);
              }
              """,
            """
              import java.sql.Date;

              class Main {
                final Date date = new Date(123);
                final java.util.Date date2 = new java.util.Date(123);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5703")
    @Test
    void wildcardImportsWithSamePackageConflict() {
        rewriteRun(
          java(
            """
              package com.helloworld;

              public class Date {}
              """
          ),
          java(
            """
              package com.helloworld;

              import java.util.*;

              class Main {
                final Date date = new Date();
                final java.util.Date date2 = new java.util.Date(123);
              }
              """,
            """
              package com.helloworld;

              class Main {
                final Date date = new Date();
                final java.util.Date date2 = new java.util.Date(123);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5703")
    @Test
    void onlyFullyQualifiedUsageShouldRemoveWildcard() {
        rewriteRun(
          java(
            """
              import java.util.*;

              class Main {
                final java.util.Date date2 = new java.util.Date(123);
              }
              """,
            """
              class Main {
                final java.util.Date date2 = new java.util.Date(123);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5703")
    @Test
    void keepImportWhenBothQualifiedAndUnqualifiedUsageExists() {
        rewriteRun(
          java(
            """
            import java.util.Date;

            class Test {
                Date date1 = new Date();                    // unqualified usage
                java.util.Date date2 = new java.util.Date();  // qualified usage
            }
            """
          )
        );
    }
}
