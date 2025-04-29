/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class TypesTest implements RewriteTest {

    @Test
    @SuppressWarnings("rawtypes")
    void isOfType() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.Map;
              
              class Test<T extends Number, U extends List<String>, V extends U, X> {
                  Integer integer;
                  int[] intArray;
                  Integer[] integerArray;
                  String[] stringArray;
                  List<String>[] genericArray;
                  Integer[][] nestedArray;
                  T[] tArray;
                  U[] uArray;
                  V[] vArray;
                  X[] xArray;
              
                  T numberType;
                  U listType;
                  V nestedListType;
                  X generic;
              
                  List<T> numberList;
                  List<String> listString;
                  Map<String, T> stringToNumberMap;
                  Map<String, X> stringToGenericMap;
              
                  List<? extends Number> extendsNumberList;
                  List<? super Integer> superIntegerList;
              
                  Map<String, List<Map<Integer, String>>> complexNested;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // 1. Primitive exact matches
                      assertions.isOfType("int", "int").isTrue();
                      assertions.isOfType("int", "Integer").isFalse();
                      assertions.isOfType("Integer", "int").isFalse();

                      // 2. Array matches
                      assertions.isOfType("int[]", "int[]").isTrue();
                      assertions.isOfType("Integer[]", "Integer[]").isTrue();
                      assertions.isOfType("Integer[]", "int[]").isFalse();
                      assertions.isOfType("int[]", "Integer[]").isFalse();
                      assertions.isOfType("Integer[][]", "Integer[][]").isTrue();
                      assertions.isOfType("List<String>[]", "List<String>[]").isTrue();
                      assertions.isOfType("List<String>[]", "String[]").isFalse();
                      assertions.isOfType("int[]", "String[]").isFalse();
                      assertions.isOfType("List<String>[]", "String[]").isFalse();

                      // 3. Generic array matches
                      assertions.isOfType("T[]", "T[]").isTrue();
                      assertions.isOfType("U[]", "U[]").isTrue();
                      assertions.isOfType("T[]", "Integer[]").isFalse();
                      assertions.isOfType("U[]", "List<String>[]").isFalse();
                      assertions.isOfType("Integer[][]", "T[]").isFalse();
                      assertions.isOfType("T[]", "Integer[][]").isFalse();
                      assertions.isOfType("U[]", "Integer[][]").isFalse();
                      assertions.isOfType("U[]", "V[]").isFalse();
                      assertions.isOfType("V[]", "U[]").isFalse();
                      assertions.isOfType("Integer[][]", "int[]").isFalse();

                      // 4. Type variable matches <T extends Number, U extends List<String>, V extends U>
                      assertions.isOfType("T", "T").isTrue();
                      assertions.isOfType("U", "U").isTrue();
                      assertions.isOfType("V", "V").isTrue();
                      assertions.isOfType("T", "Integer").isFalse();
                      assertions.isOfType("T", "Integer").isFalse();
                      assertions.isOfType("U", "V").isFalse();
                      assertions.isOfType("T", "U").isFalse();

                      // 5. Parameterized types
                      assertions.isOfType("List<T>", "List<T>").isTrue();
                      assertions.isOfType("List<? extends Number>", "List<? extends Number>").isTrue();
                      assertions.isOfType("Map<String, List<Map<Integer, String>>>", "Map<String, List<Map<Integer, String>>>").isTrue();
                      assertions.isOfType("List<T>", "List<? extends Number>").isFalse();
                      assertions.isOfType("List<? extends Number>", "List<T>").isFalse();

                      // 6. With INFER mode <T extends Number, U extends List<String>, V extends U>
                      assertions.isOfType("T", "Integer", true).isTrue();
                      assertions.isOfType("U", "Integer", true).isFalse();
                      assertions.isOfType("U", "List<String>", true).isTrue();
                      assertions.isOfType("V", "List<String>", true).isTrue();
                      assertions.isOfType("T", "Integer[]", true).isFalse();
                      assertions.isOfType("X", "Integer[]", true).isTrue();
                      assertions.isOfType("T", "int[]", true).isFalse();
                      assertions.isOfType("X", "int[]", true).isTrue();
                      assertions.isOfType("T[]", "int[]", true).isFalse();
                      assertions.isOfType("X[]", "int[]", true).isFalse();
                      assertions.isOfType("T[]", "Integer[]", true).isTrue();
                      assertions.isOfType("X[]", "Integer[]", true).isTrue();
                      assertions.isOfType("U[]", "List<String>[]", true).isTrue();
                      assertions.isOfType("V[]", "List<String>[]", true).isTrue();
                      assertions.isOfType("Integer[][]", "T[]", true).isFalse();
                      assertions.isOfType("X[]", "Integer[][]", true).isTrue();
                      assertions.isOfType("T[]", "Integer[][]", true).isFalse();
                      assertions.isOfType("U[]", "V[]", true).isTrue();
                      assertions.isOfType("V[]", "U[]", true).isTrue();
                      assertions.isOfType("Integer[][]", "int[]", true).isFalse();
                      assertions.isOfType("Map<String, T>", "Map<String, List<Map<Integer, String>>>", true).isFalse();
                      assertions.isOfType("Map<String, X>", "Map<String, List<Map<Integer, String>>>", true).isTrue();
                      assertions.isOfType("Map<String, List<Map<Integer, String>>>", "Map<String, T>", true).isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void isClassAssignableTo() {
        rewriteRun(
          java(
            """
              import java.io.Serializable;
              import java.util.ArrayList;
              import java.util.Collection;
              import java.util.List;
              
              @SuppressWarnings("all")
              class Test<T extends Number & Serializable, U> {
                  Integer integer;
                  Boolean bool;
                  Double bool;
                  Number number;
                  Cloneable cloneable;
                  Serializable serializable;
                  String[] array;
              
                  Object obj;
                  String str;
                  List listRaw;
                  Collection collectionRaw;
                  ArrayList arrayListRaw;
                  List<String> listString;
                  T genericBounded;
                  U generic;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // Boxed from primitives
                      assertions.isAssignableTo("Integer", "int").isTrue();
                      assertions.isAssignableTo("Number", "int").isTrue();
                      assertions.isAssignableTo("Serializable", "int").isTrue();
                      assertions.isAssignableTo("Boolean", "boolean").isTrue();
                      assertions.isAssignableTo("Number", "boolean").isFalse();
                      assertions.isAssignableTo("Serializable", "boolean").isTrue();
                      assertions.isAssignableTo("Double", "double").isTrue();
                      assertions.isAssignableTo("Number", "double").isTrue();
                      assertions.isAssignableTo("Serializable", "double").isTrue();
                      assertions.isAssignableTo("String", "int").isFalse();

                      // FullyQualified direct
                      assertions.isAssignableTo("Object", "String").isTrue();
                      assertions.isAssignableTo("String", "Object").isFalse();
                      assertions.isAssignableTo("List", "String").isFalse();

                      // Null type (assignable to any reference type)
                      assertions.isAssignableTo("String", "null").isTrue();
                      assertions.isAssignableTo("List", "null").isTrue();

                      // Parameterized type to raw type
                      assertions.isAssignableTo("List", "List<String>").isTrue();

                      // Class to interface
                      assertions.isAssignableTo("Serializable", "String").isTrue();
                      assertions.isAssignableTo("Collection", "ArrayList").isTrue();

                      // Interface to class
                      assertions.isAssignableTo("String", "Serializable").isFalse();

                      // Array assignability
                      assertions.isAssignableTo("Object", "String[]").isTrue();
                      assertions.isAssignableTo("Cloneable", "String[]").isTrue();
                      assertions.isAssignableTo("Serializable", "String[]").isTrue();

                      // Generic type <T extends Number & Serializable, U>
                      assertions.isAssignableTo("Serializable", "T").isTrue();
                      assertions.isAssignableTo("Number", "T").isTrue();
                      assertions.isAssignableTo("String", "T").isFalse();
                      assertions.isAssignableTo("Object", "T").isTrue();
                      assertions.isAssignableTo("Number", "U").isFalse();
                      assertions.isAssignableTo("Number", "U").isFalse();
                      assertions.isAssignableTo("Object", "U").isTrue();
                  }
              }
            )
          )
        );
    }

    @Test
    @SuppressWarnings("rawtypes")
    void isParameterizedAssignableTo() {
        rewriteRun(
          java(
            """
              import java.util.*;
              import java.util.function.Supplier;
              
              class Test<T, U extends T, N extends Number, CS extends CharSequence> {
                  ArrayList v1;
                  Comparable<?> v2;
                  Comparable<ImplementsComparable> v3;
                  Comparable<Number> v4;
                  Comparable<String> v5;
                  ComparableSupplier<String, Number> v6;
                  ExtendsComparable v7;
                  List v8;
                  List<? extends CharSequence> v9;
                  List<? extends List<? extends CharSequence>> v10;
                  List<? super String> v11;
                  List<? super CharSequence> v25;
                  List<? super T> v26;
                  List<? super U> v27;
                  List<?> v12;
                  List<CS> v13;
                  List<CharSequence> v14;
                  List<List<? extends CharSequence>> v15;
                  List<List<String>> v16;
                  List<N> v17;
                  List<String> v18;
                  List<T> v19;
                  List<U> v20;
                  MySupplier<Number> v21;
                  Supplier<Number> v22;
                  Supplier<String> v23;
                  ImplementsComparable v24;
                  Map<N, N> mapNN;
                  Map<String, String> mapSS;
                  Map<Integer, Integer> mapII;
                  Map<Long, Integer> mapLI;
              
                  static abstract class ImplementsComparable implements Comparable<ImplementsComparable> {}
                  static abstract class ExtendsComparable extends ImplementsComparable {}
                  static abstract class MySupplier<T> implements Supplier<T> {}
                  static abstract class ComparableSupplier<T, U> extends MySupplier<U> implements Comparable<T> {}
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // 1. Generic Variance
                      assertions.isAssignableTo("List<? extends CharSequence>", "List<String>").isTrue();
                      assertions.isAssignableTo("List<String>", "List<? extends CharSequence>").isFalse();
                      assertions.isAssignableTo("List<? super String>", "List<CharSequence>").isTrue();

                      // 2. Wildcards and Raw Types
                      assertions.isAssignableTo("List<?>", "List<String>").isTrue();
                      assertions.isAssignableTo("List<?>", "ArrayList").isTrue();
                      assertions.isAssignableTo("List<String>", "List").isFalse(); // We don't allow unsafe assignments
                      assertions.isAssignableTo("List<?>", "List").isTrue(); // Except for wildcards

                      // 3. Type Hierarchy with Generics (String, Number)
                      assertions.isAssignableTo("Comparable<?>", "ImplementsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<ImplementsComparable>", "ImplementsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<ImplementsComparable>", "ExtendsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<?>", "ExtendsComparable").isTrue();
                      assertions.isAssignableTo("Comparable<String>", "ExtendsComparable").isFalse();

                      assertions.isAssignableTo("Comparable<String>", "ComparableSupplier<String, Number>").isTrue();
                      assertions.isAssignableTo("Comparable<Number>", "ComparableSupplier<String, Number>").isFalse();
                      assertions.isAssignableTo("Supplier<Number>", "ComparableSupplier<String, Number>").isTrue();
                      assertions.isAssignableTo("Supplier<String>", "ComparableSupplier<String, Number>").isFalse();
                      assertions.isAssignableTo("MySupplier<Number>", "ComparableSupplier<String, Number>").isTrue();
                      assertions.isAssignableTo("Comparable<?>", "ComparableSupplier<String, Number>").isTrue();

                      // 4. Type Variables
                      assertions.isAssignableTo("List<T>", "List<String>").isFalse();
                      assertions.isAssignableTo("List<T>", "List<U>").isFalse();
                      assertions.isAssignableTo("List<? extends CharSequence>", "List<CS>").isTrue();
                      assertions.isAssignableTo("List<? super U>", "List<? super T>").isTrue();
                      assertions.isAssignableTo("List<? super String>", "List<? super CharSequence>").isTrue();
                      assertions.isAssignableTo("List<? super T>", "List<? super U>").isFalse();
                      assertions.isAssignableTo("List<? super CharSequence>", "List<? super String>").isFalse();

                      // 5. Edge Cases
                      assertions.isAssignableTo("List<? extends List<? extends CharSequence>>", "List<List<String>>").isTrue();
                      assertions.isAssignableTo("List<List<? extends CharSequence>>", "List<List<String>>").isFalse();

                      // 6. Inference Mode
                      assertions.isAssignableTo("List<T>", "List<String>", true).isTrue();
                      assertions.isAssignableTo("List<CS>", "List<String>", true).isTrue();
                      assertions.isAssignableTo("List<N>", "List<String>", true).isFalse();
                      assertions.isAssignableTo("List<? super T>", "List<? super String>", true).isTrue();
                      assertions.isAssignableTo("Map<N, N>", "Map<String, String>", true).isFalse();
                      assertions.isAssignableTo("Map<N, N>", "Map<Integer, Integer>", true).isTrue();
                      assertions.isAssignableTo("Map<N, N>", "Map<Long, Integer>", true).isTrue(); // This should be false
                  }
              }
            )
          )
        );
    }

    @Test
    void isAssignableToArray() {
        rewriteRun(
          java(
            """
              class Test<T extends CharSequence, U, V extends Number> {
                  Object[] objectArray;
                  String[] stringArray;
                  CharSequence[] charSequenceArray;
                  int[] intArray;
                  double[] doubleArray;
                  Integer[] integerArray;
                  Double[][] double2DArray;
                  Number[][] number2DArray;
                  Object[][] object2DArray;
                  String[][] string2DArray;
                  T[] genericCsArray;
                  U[] genericArray;
                  V[] genericNumericArray;
                  U generic;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // Identity and exact match
                      assertions.isAssignableTo("String[]", "String[]").isTrue();

                      // Covariant assignability of reference types
                      assertions.isAssignableTo("Object[]", "String[]").isTrue();
                      assertions.isAssignableTo("CharSequence[]", "String[]").isTrue();

                      // Reverse should fail
                      assertions.isAssignableTo("String[]", "Object[]").isFalse();
                      assertions.isAssignableTo("String[]", "CharSequence[]").isFalse();

                      // Primitive arrays are not assignable to Object[]
                      assertions.isAssignableTo("Object[]", "int[]").isFalse();
                      assertions.isAssignableTo("Object[]", "Integer[]").isTrue();

                      // Primitive identity
                      assertions.isAssignableTo("int[]", "int[]").isTrue();
                      assertions.isAssignableTo("int[]", "Integer[]").isFalse();
                      assertions.isAssignableTo("Integer[]", "int[]").isFalse();

                      // Different primitives are not assignable
                      assertions.isAssignableTo("int[]", "double[]").isFalse();

                      // 2D array covariance
                      assertions.isAssignableTo("Object[][]", "String[][]").isTrue();
                      assertions.isAssignableTo("Number[][]", "Double[][]").isTrue();
                      assertions.isAssignableTo("Double[][]", "Number[][]").isFalse();

                      // Incompatible inner dimension
                      assertions.isAssignableTo("Number[][]", "Integer[]").isFalse();

                      // Generics: <T extends CharSequence, U>
                      assertions.isAssignableTo("T[]", "String[]").isFalse();
                      assertions.isAssignableTo("T[]", "CharSequence[]").isFalse();
                      assertions.isAssignableTo("Object[]", "T[]").isTrue();
                      assertions.isAssignableTo("CharSequence[]", "T[]").isTrue();
                      assertions.isAssignableTo("U[]", "CharSequence[]").isFalse();
                      assertions.isAssignableTo("Object[]", "U[]").isTrue();

                      // Infer mode: <T extends CharSequence, U>
                      assertions.isAssignableTo("T[]", "String[]", true).isTrue();
                      assertions.isAssignableTo("T[]", "CharSequence[]", true).isTrue();
                      assertions.isAssignableTo("T[]", "String[][]", true).isFalse();

                      assertions.isAssignableTo("U", "String[]", true).isTrue();
                      assertions.isAssignableTo("U", "CharSequence[]", true).isTrue();
                      assertions.isAssignableTo("U", "String[][]", true).isTrue();
                      assertions.isAssignableTo("U", "int[]", true).isTrue();
                      assertions.isAssignableTo("U", "double[]", true).isTrue();

                      assertions.isAssignableTo("U[]", "String[]", true).isTrue();
                      assertions.isAssignableTo("U[]", "CharSequence[]", true).isTrue();
                      assertions.isAssignableTo("U[]", "String[][]", true).isTrue();
                      assertions.isAssignableTo("U[]", "int[]", true).isFalse();
                      assertions.isAssignableTo("V[]", "int[]", true).isFalse();
                      assertions.isAssignableTo("V[]", "Integer[]", true).isTrue();
                      assertions.isAssignableTo("U[]", "double[]", true).isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void isAssignableToPrimitive() {
        rewriteRun(
          java(
            """
              class Test<T, U extends Number> {
                  Byte boxedByte;
                  Character boxedChar;
                  Short boxedShort;
                  Integer boxedInt;
                  Long boxedLong;
                  Float boxedFloat;
                  Double boxedDouble;
                  Boolean boxedBoolean;
              
                  T genericT;
                  U genericU;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // Direct primitive assignability
                      assertions.isAssignableTo("int", "byte").isTrue();
                      assertions.isAssignableTo("int", "char").isTrue();
                      assertions.isAssignableTo("int", "short").isTrue();
                      assertions.isAssignableTo("int", "int").isTrue();
                      assertions.isAssignableTo("int", "long").isFalse();
                      assertions.isAssignableTo("float", "int").isTrue();
                      assertions.isAssignableTo("double", "float").isTrue();
                      assertions.isAssignableTo("float", "double").isFalse();

                      // Boolean isn't compatible with numeric types
                      assertions.isAssignableTo("int", "boolean").isFalse();
                      assertions.isAssignableTo("boolean", "boolean").isTrue();
                      assertions.isAssignableTo("boolean", "int").isFalse();

                      // Auto-unboxing
                      assertions.isAssignableTo("int", "Integer").isTrue();
                      assertions.isAssignableTo("double", "Double").isTrue();
                      assertions.isAssignableTo("boolean", "Boolean").isTrue();
                      assertions.isAssignableTo("Integer", "int").isTrue();
                      assertions.isAssignableTo("Double", "double").isTrue();
                      assertions.isAssignableTo("Boolean", "boolean").isTrue();

                      // Mismatched boxed types
                      assertions.isAssignableTo("int", "Boolean").isFalse();
                      assertions.isAssignableTo("boolean", "Integer").isFalse();
                      assertions.isAssignableTo("Boolean", "int").isFalse();
                      assertions.isAssignableTo("Integer", "boolean").isFalse();

                      // Generics <T, U extends Number>
                      assertions.isAssignableTo("T", "byte", true).isTrue();
                      assertions.isAssignableTo("T", "short", true).isTrue();
                      assertions.isAssignableTo("T", "char", true).isTrue();
                      assertions.isAssignableTo("T", "int", true).isTrue();
                      assertions.isAssignableTo("T", "long", true).isTrue();
                      assertions.isAssignableTo("T", "float", true).isTrue();
                      assertions.isAssignableTo("T", "double", true).isTrue();
                      assertions.isAssignableTo("T", "boolean", true).isTrue();

                      assertions.isAssignableTo("U", "byte", true).isTrue();
                      assertions.isAssignableTo("U", "short", true).isTrue();
                      assertions.isAssignableTo("U", "int", true).isTrue();
                      assertions.isAssignableTo("U", "long", true).isTrue();
                      assertions.isAssignableTo("U", "float", true).isTrue();
                      assertions.isAssignableTo("U", "double", true).isTrue();
                      assertions.isAssignableTo("U", "char", true).isFalse();
                      assertions.isAssignableTo("U", "boolean", true).isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void isAssignableToGenericTypeVariable() {
        rewriteRun(
          java(
            """
              class Test {
                  class A<T, U extends T, V extends U, X> {
                      T t;
                      U u;
                      V v;
                      X x;
                  }
              
                  class B<T, U extends T, V extends U, X> {
                      T t;
                      U u;
                      V v;
                      X x;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      // 1. Same variable name
                      assertions.isAssignableTo("T", "T").isTrue();
                      assertions.isAssignableTo("U", "U").isTrue();
                      assertions.isOfType("T", "T").isTrue();

                      // 2. Different variables with compatible bounds
                      // class <T, U extends T, V extends U>
                      assertions.isAssignableTo("T", "U").isTrue(); // U is assignable to T
                      assertions.isAssignableTo("U", "T").isFalse(); // T not assignable to U (U more specific)
                      assertions.isAssignableTo("T", "V").isTrue(); // V -> U -> T
                      assertions.isAssignableTo("U", "V").isTrue(); // V -> U
                      assertions.isAssignableTo("V", "T").isFalse(); // T is more general

                      // 3. Unrelated variables
                      // class <T, X>
                      assertions.isAssignableTo("T", "X").isFalse();
                      assertions.isAssignableTo("X", "T").isFalse();

                      // 4. isOfType tests for completeness
                      assertions.isOfType("T", "T").isTrue();
                      assertions.isOfType("U", "U").isTrue();
                      assertions.isOfType("T", "U").isFalse();
                      assertions.isOfType("U", "T").isFalse();
                  }
              }
            )
          )
        );
    }

    @Test
    void recursiveTypes() {
        rewriteRun(
          java(
            """
              abstract class Comp implements Comparable<Comp> {}
              abstract class Ext extends Comp {}
              enum EnumType { A, B, C }
              
              class Test<E extends Enum<E>, C extends Comparable<? super C>, T> {
                  E e;
                  C c;
                  T free;
                  Comp comp;
                  Ext ext;
                  EnumType enumType;
                  Comparable<Comp> comparable;
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      assertions.isOfType("Comp", "Comp").isTrue();
                      assertions.isOfType("Ext", "Ext").isTrue();
                      assertions.isOfType("EnumType", "EnumType").isTrue();

                      assertions.isAssignableTo("E", "EnumType", false).isFalse();
                      assertions.isAssignableTo("E", "EnumType", true).isTrue();

                      assertions.isAssignableTo("C", "Comp", false).isFalse();
                      assertions.isAssignableTo("C", "Ext", false).isFalse();

                      assertions.isAssignableTo("C", "Comp", true).isTrue();
                      assertions.isAssignableTo("C", "Ext", true).isTrue();

                      assertions.isAssignableTo("C", "Comparable<Comp>", false).isFalse();
                      assertions.isAssignableTo("C", "Comparable<Comp>", true).isTrue();

                      assertions.isAssignableTo("Comparable<Comp>", "Comp").isTrue();
                      assertions.isAssignableTo("Comparable<Comp>", "Ext").isTrue();
                  }
              }
            )
          )
        );
    }

    @Test
    @MinimumJava11
    void intersectionTypes() {
        rewriteRun(
          java(
            """
              import java.io.*;
              import java.util.*;
              
              @SuppressWarnings("all")
              public class Test {
                  void test() {
                      var intersection1 = (Cloneable & Serializable) null;
                      var intersection2 = (Serializable & Cloneable) null;
                      Serializable serializable;
                      Cloneable cloneable;
                      int[] arrayPrimitive;
                      DuplicateFormatFlagsException extendIllegal;
                      RuntimeException exception;
                      try {} catch (NullPointerException | IllegalArgumentException exception1) {}
                      try {} catch (IllegalArgumentException | NullPointerException exception2) {}
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                  try (TypeUtilsAssertions assertions = new TypeUtilsAssertions(cu)) {
                      assertions.isOfType("intersection1", "intersection2").isTrue();
                      assertions.isAssignableTo("intersection1", "int[]").isTrue();
                      assertions.isAssignableTo("int[]", "intersection1").isFalse();
                      assertions.isAssignableTo("Serializable", "intersection1").isTrue();
                      assertions.isAssignableTo("Cloneable", "intersection1").isTrue();

                      assertions.isOfType("NullPointerException | IllegalArgumentException", "IllegalArgumentException | NullPointerException").isTrue();
                      assertions.isAssignableTo("NullPointerException | IllegalArgumentException", "DuplicateFormatFlagsException").isTrue();
                      assertions.isAssignableTo("DuplicateFormatFlagsException", "NullPointerException | IllegalArgumentException").isFalse();
                      assertions.isAssignableTo("NullPointerException | IllegalArgumentException", "RuntimeException").isFalse();
                      assertions.isAssignableTo("RuntimeException", "NullPointerException | IllegalArgumentException").isTrue();
                      assertions.isAssignableTo("exception2", "NullPointerException | IllegalArgumentException").isTrue();
                  }
              }
            )
          )
        );
    }
}
