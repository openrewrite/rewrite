/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import org.openrewrite.scala.tree.S;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

/**
 * Tests for Scala 2 syntax patterns that must round-trip correctly
 * through the Scala 3 parser running in -source:3.0-migration mode.
 */
class Scala2CompatTest implements RewriteTest {

    @Test
    void procedureSyntax() {
        rewriteRun(
            scala(
                """
                object Test {
                  def hello() {
                    println("hello")
                  }
                }
                """
            )
        );
    }

    @Test
    void procedureSyntaxWithParams() {
        rewriteRun(
            scala(
                """
                object Test {
                  def greet(name: String) {
                    println(name)
                  }
                }
                """
            )
        );
    }

    @Test
    void uninitializedVar() {
        rewriteRun(
            scala(
                """
                class Test {
                  var x: Int = _
                }
                """
            )
        );
    }

    @Test
    void uninitializedVarMultiple() {
        rewriteRun(
            scala(
                """
                class Test {
                  var x: String = _
                  var y: Int = _
                  var z: Boolean = _
                }
                """
            )
        );
    }

    @Test
    void wildcardTypeArg() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list: List[_] = List(1, 2, 3)
                }
                """
            )
        );
    }

    @Test
    void blockArgumentToMethod() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  list.foreach { x =>
                    println(x)
                  }
                }
                """
            )
        );
    }

    @Test
    void blockArgumentWithTypedParam() {
        // In Scala 3, typed lambda params in block args need parentheses: (x: Int) =>
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  list.foreach { (x: Int) =>
                    println(x)
                  }
                }
                """
            )
        );
    }

    @Test
    void debugBlockArgParsing() {
        ScalaParser parser = ScalaParser.builder().build();
        String source = """
                object Test {
                  List(1).foreach { x =>
                    println(x)
                  }
                }
                """;
        List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
        assertThat(parsed).hasSize(1);
        SourceFile sf = parsed.get(0);
        System.out.println("TYPE: " + sf.getClass().getSimpleName());
        System.out.println("IS PARSE ERROR: " + (sf instanceof ParseError));
        System.out.println("OUTPUT:\n" + sf.printAll());
    }

    @Test
    void blockArgumentMap() {
        rewriteRun(
            scala(
                """
                object Test {
                  val nums = List(1, 2, 3)
                  val doubled = nums.map { x =>
                    x * 2
                  }
                }
                """
            )
        );
    }

    @Test
    void blockArgumentMultiStatement() {
        rewriteRun(
            scala(
                """
                object Test {
                  val list = List(1, 2, 3)
                  list.foreach { x =>
                    val y = x + 1
                    println(y)
                  }
                }
                """
            )
        );
    }

    @Test
    void blockArgumentChained() {
        rewriteRun(
            scala(
                """
                object Test {
                  val result = List(1, 2, 3).map { x =>
                    x * 2
                  }
                }
                """
            )
        );
    }

    @Test
    void twoStatementsInObject() {
        rewriteRun(
            scala(
                """
                object Test {
                  val a = 1
                  val b = 2
                }
                """
            )
        );
    }

    @Test
    void traitDefinition() {
        rewriteRun(
            scala(
                """
                trait MyTrait {
                  def foo(): Int
                }
                """
            )
        );
    }

    @Test
    void traitWithExtends() {
        rewriteRun(
            scala(
                """
                trait DatasetSuiteBaseLike extends Serializable {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void privateWithScope() {
        rewriteRun(
            scala(
                """
                object Test {
                  private[Test] val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void classExtendsWithSpace() {
        rewriteRun(
            scala(
                """
                class Child extends Parent {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void classWithMultipleParents() {
        rewriteRun(
            scala(
                """
                class MyClass extends Base with Serializable {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void importWithBraces() {
        rewriteRun(
            scala(
                """
                import java.io.{File, InputStream}

                object Test {
                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void multipleImportsPreserveOrder() {
        rewriteRun(
            scala(
                """
                import java.io.File
                import scala.collection.mutable
                import java.util.UUID

                object Test {
                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void importWithBracesAndOtherImports() {
        rewriteRun(
            scala(
                """
                package com.example

                import org.apache.spark.rdd.RDD
                import org.apache.spark.sql.{DataFrame, Dataset}
                import org.scalacheck.util.Pretty

                trait Prettify {
                  val x: Int = 100
                }
                """
            )
        );
    }

    @Test
    void importsInterleavedJavaScala() {
        rewriteRun(
            scala(
                """
                package com.example

                import java.io.File
                import scala.reflect.ClassTag
                import java.util.UUID

                object Test {
                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void annotationWithPrivateModifier() {
        rewriteRun(
            scala(
                """
                class Test {
                  @transient private var x: Int = _
                }
                """
            )
        );
    }

    @Test
    void classWithTypeParamExtends() {
        rewriteRun(
            scala(
                """
                class Wrapper[T] extends Base {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void classExtendsParameterizedType() {
        rewriteRun(
            scala(
                """
                class Wrapper extends Base[Int] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void privateClassWithExtendsTypeParam() {
        rewriteRun(
            scala(
                """
                private[testing] class Wrapper[T] extends Base[T] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void caseClassExtendsWithArgs() {
        rewriteRun(
            scala(
                """
                case class MyError(msg: String) extends Exception(msg)
                """
            )
        );
    }

    @Test
    void transientLazy() {
        rewriteRun(
            scala(
                """
                class Test {
                  @transient lazy val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void multiLineConstructorParams() {
        rewriteRun(
            scala(
                """
                class Wrapper[T]
                  (size: Int, name: String)
                  extends Base {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void contextBound() {
        rewriteRun(
            scala(
                """
                class Container[T: Ordering] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void valWithIfElseInitializer() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x: Int = if (true) {
                    1
                  } else {
                    2
                  }
                }
                """
            )
        );
    }

    @Test
    void annotationWithClassOf() {
        rewriteRun(
            scala(
                """
                @deprecated(message = "use other") class Test {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void annotationClassOfTypeArg() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = classOf[String]
                }
                """
            )
        );
    }

    @Test
    void typeProjection() {
        rewriteRun(
            scala(
                """
                object Test {
                  type Id[A] = A
                  type Pair[A] = (A, A)
                }
                """
            )
        );
    }

    @Test
    void intersectionType() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x: Serializable & Comparable[String] = "hello"
                }
                """
            )
        );
    }

    @Test
    void valWithNewAnonClassBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x: Runnable = new Runnable {
                    def run(): Unit = println("hi")
                  }
                }
                """
            )
        );
    }

    @Test
    void byNameParameter() {
        rewriteRun(
            scala(
                """
                object Test {
                  def foo(x: => Int): Int = x
                }
                """
            )
        );
    }

    @Test
    void byNameParameterInClass() {
        rewriteRun(
            scala(
                """
                class Lazy[A](value: => A) {
                  lazy val get: A = value
                }
                """
            )
        );
    }

    @Test
    void givenDeclaration() {
        rewriteRun(
            scala(
                """
                object Test {
                  given x: Int = 42
                }
                """
            )
        );
    }

    @Test
    void scala3Enum() {
        rewriteRun(
            scala(
                """
                enum Color:
                  case Red, Green, Blue
                """
            )
        );
    }

    @Test
    void scala3BracelessObject() {
        rewriteRun(
            scala(
                """
                object Test:
                  val x: Int = 1
                  val y: Int = 2
                """
            )
        );
    }

    @Test
    void scala3BracelessDef() {
        rewriteRun(
            scala(
                """
                object Test:
                  def hello(): Unit =
                    println("hello")
                """
            )
        );
    }

    @Test
    void higherKindedTypeParam() {
        rewriteRun(
            scala(
                """
                trait Functor[F[_]] {
                  def map[A, B](fa: F[A])(f: A => B): F[B]
                }
                """
            )
        );
    }

    @Test
    void traitExtendsWithKeyword() {
        rewriteRun(
            scala(
                """
                trait Applicative[F[_]] extends Apply[F] with Pointed[F] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void multipleHigherKindedParams() {
        rewriteRun(
            scala(
                """
                trait Iso[F[_], G[_]] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void stringInfixInBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = "hello" + "world"
                }
                """
            )
        );
    }

    @Test
    void infixMethodCallWithBlock() {
        rewriteRun(
            scala(
                """
                object Test {

                  "test name" should {
                    val x = 1
                  }
                }
                """
            )
        );
    }

    @Test
    void bareExpressionInBody() {
        rewriteRun(
            scala(
                """
                object Test {
                  println("hello")

                  println("world")
                }
                """
            )
        );
    }

    @Test
    void objectExtendsBodyBlankLine() {
        rewriteRun(
            scala(
                """
                trait SpecLite
                object MyTest extends SpecLite {

                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void objectBodyWithBlankLineBeforeFirstStatement() {
        rewriteRun(
            scala(
                """
                object Test extends App {

                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void commaSeparatedImport() {
        rewriteRun(
            scala(
                """
                object Test {
                  import scala.collection.mutable._, scala.util._
                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void importsWithExtraBlankLines() {
        rewriteRun(
            scala(
                """
                object Test {
                  import scala.collection.mutable

                  import java.util.UUID

                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void multiLineArgs() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = Seq(
                    1,
                    2,
                    3
                  )
                }
                """
            )
        );
    }

    @Test
    void newWithEmptyBody() {
        rewriteRun(
            scala(
                """
                trait Foo
                object Test {
                  val x: Foo = new Foo {}
                }
                """
            )
        );
    }

    @Test
    void namedGiven() {
        rewriteRun(
            scala(
                """
                trait SchemaFor[T]
                object Schemas {
                  given IntSchema: SchemaFor[Int] = new SchemaFor[Int] {}
                }
                """
            )
        );
    }

    @Test
    void namedGivenSpaceBeforeColon() {
        rewriteRun(
            scala(
                """
                trait SchemaFor[T]
                object Schemas {
                  given IntSchema : SchemaFor[Int] = new SchemaFor[Int] {}
                }
                """
            )
        );
    }

    @Test
    void anonymousGivenBraceless() {
        rewriteRun(
            scala(
                """
                trait Foo {
                  def bar(): Int
                }
                object Test {
                  given TypeGuardedDecoding[String] = new TypeGuardedDecoding[String] :
                    override def guard(): Boolean = true
                }
                trait TypeGuardedDecoding[T]
                """
            )
        );
    }

    @Test
    void enumCaseExtends() {
        rewriteRun(
            scala(
                """
                enum Color:
                  case Red extends Color
                  case Green extends Color
                  case Blue extends Color
                """
            )
        );
    }

    @Test
    void enumCaseWithParams() {
        rewriteRun(
            scala(
                """
                enum Planet(mass: Double):
                  case Earth extends Planet(5.976e+24)
                  case Mars extends Planet(6.421e+23)
                """
            )
        );
    }

    @Test
    void anonymousGiven() {
        rewriteRun(
            scala(
                """
                trait Foo {
                  def bar(): Int
                }
                object Test {
                  given Foo = new Foo {
                    def bar(): Int = 42
                  }
                }
                """
            )
        );
    }

    @Test
    void specializedAnnotationOnTypeParam() {
        rewriteRun(
            scala(
                """
                class Memo[@specialized(Int) K] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void typeProjectionHash() {
        rewriteRun(
            scala(
                """
                object Test {
                  type Foo = ({type L[A] = List[A]})#L
                }
                """
            )
        );
    }

    @Test
    void upperBound() {
        rewriteRun(
            scala(
                """
                class Box[T <: Comparable[T]] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void lowerBound() {
        rewriteRun(
            scala(
                """
                class Container[T >: Null] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void covariantWithUpperBound() {
        rewriteRun(
            scala(
                """
                class Box[+A <: AnyRef] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void contravariantWithLowerBound() {
        rewriteRun(
            scala(
                """
                class Box[-A >: Null] {
                  val x: Int = 1
                }
                """
            )
        );
    }

    @Test
    void applyWithBraces() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = Seq {
                    1
                  }
                }
                """
            )
        );
    }

    @Test
    void methodCallNewlineBeforeArgs() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = foo(
                    1,
                    2
                  )
                }
                """
            )
        );
    }

    @Test
    void functionApplicationNewlineArgs() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = Seq(
                    1,
                    2
                  )
                }
                """
            )
        );
    }

    @Test
    void applyWithBracesAndSpace() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = Seq {
                    "hello"
                  }
                }
                """
            )
        );
    }

    @Test
    void fieldAccessNewline() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = Map(1 -> 2)
                    .toList
                }
                """
            )
        );
    }

    @Test
    void dotMethodCallNewlineAfterParen() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = List.fill(
                    10
                  )(0)
                }
                """
            )
        );
    }

    @Test
    void dotChainNewline() {
        rewriteRun(
            scala(
                """
                object Test {
                  val x = List(1, 2, 3)
                    .map(_ + 1)
                    .filter(_ > 2)
                }
                """
            )
        );
    }

    @Test
    void importSingleSegmentPackage() {
        rewriteRun(
            scala(
                """
                package com.example

                import java.util

                object Test {
                  val x = 1
                }
                """
            )
        );
    }

    @Test
    void diagnosticRealFiles() throws Exception {
        ScalaParser parser = ScalaParser.builder().build();
        String base = "/tmp/scala-test-org/spark-testing-base/";
        String[] files = {
            base + "core/src/test/2.4/scala/com/holdenkarau/spark/testing/PrettifyTest.scala",
            base + "core/src/test/2.4/scala/com/holdenkarau/spark/testing/DatasetGeneratorSizeSpecial.scala",
            base + "core/src/main/3.0/scala/com/holdenkarau/spark/testing/JavaSuiteBase.scala",
            base + "core/src/main/2.4/scala/com/holdenkarau/spark/testing/receiver/InputStreamTestingContext.scala",
            base + "kafka-0.8/src/test/scala/com/holdenkarau/spark/testing/kafka/KafkaTestUtilsTest.scala",
        };
        for (String filePath : files) {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) { System.out.println("SKIP: " + filePath); continue; }
            String source = Files.readString(path);
            List<SourceFile> parsed = parser.parse(source).collect(Collectors.toList());
            if (parsed.isEmpty()) { System.out.println("FILE: " + path.getFileName() + " NO_OUTPUT"); continue; }
            SourceFile sf = parsed.get(0);
            String name = path.getFileName().toString();
            if (sf instanceof ParseError pe) {
                System.out.println("FILE: " + name + " PARSE_ERROR");
                System.out.println("  EXCEPTION: " + pe.getMarkers());
                // Try to get the actual compilation unit that failed print-equals-input
                // ParseError wraps the original source - check erroneous field
                String errText = pe.printAll();
                System.out.println("  SRC_LEN: " + source.length() + " ERR_LEN: " + errText.length());
                System.out.println("  MATCH: " + source.equals(errText));
                if (!source.equals(errText)) {
                    // Find first difference
                    int minLen = Math.min(source.length(), errText.length());
                    for (int c = 0; c < minLen; c++) {
                        if (source.charAt(c) != errText.charAt(c)) {
                            int ctx = Math.min(40, minLen - c);
                            System.out.println("  FIRST_DIFF at char " + c);
                            System.out.println("  EXPECT: >" + source.substring(c, c + ctx).replace("\n", "\\n") + "<");
                            System.out.println("  GOT:    >" + errText.substring(c, c + ctx).replace("\n", "\\n") + "<");
                            break;
                        }
                    }
                    if (source.length() != errText.length()) {
                        System.out.println("  LENGTH_DIFF: src=" + source.length() + " err=" + errText.length());
                    }
                }
            } else {
                System.out.println("FILE: " + name + " OK");
            }
        }
    }
}
