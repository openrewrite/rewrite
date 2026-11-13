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
package org.openrewrite.scala.marker

import org.openrewrite.marker.Marker
import java.util.UUID

/**
 * Marks elements that are implicit in Scala code.
 * For example, objects are implicitly final in Scala.
 */
case class Implicit(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Marks blocks where braces have been omitted in Scala code.
 * For example, object declarations without a body: "object MySingleton"
 */
case class OmitBraces(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Marks a J.ClassDeclaration as a Scala object (singleton).
 * In Scala, object declarations create singleton instances.
 *
 * This marker distinguishes between:
 * - Regular classes: class Foo
 * - Singleton objects: object Foo
 * - Case objects: case object Foo (would have this marker + case modifier)
 *
 * @param id The marker ID
 * @param companion Whether this is a companion object (has the same name as a class in the same scope)
 */
case class SObject(id: UUID, companion: Boolean) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

object SObject {
  def create(): SObject = SObject(UUID.randomUUID(), false)
  def companion(): SObject = SObject(UUID.randomUUID(), true)
}

/**
 * Marks a J.ClassDeclaration as a Scala `package object X { ... }`.
 * The printer uses this to emit `package` before the `object` keyword.
 * Attached in addition to SObject — a package object is still a singleton object.
 */
case class PackageObject(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Marks a J.MethodDeclaration or J.Lambda whose body lambda represents a curried
 * parameter list rather than an actual lambda expression.
 *
 * For `def map(fa: F[A])(f: A => B): F[B] = body`, the method declaration carries
 * this marker. Its body contains a J.Lambda with params `(f: A => B)` and the actual body.
 * The printer uses this to emit `(f: A => B): F[B] = body` instead of treating the
 * lambda as a regular body expression.
 *
 * For 3+ param lists, intermediate J.Lambda nodes also carry this marker.
 */
case class Curried(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Carries the source text between the last annotation/modifier and the
 * `val`/`var`/`given` keyword for Scala variable declarations.
 */
case class ValVarKeyword(id: UUID, beforeKeyword: String) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Marks a package declaration followed by an explicit semicolon:
 * `package foo;class Bar`.
 */
case class PackageSemicolon(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Marks a Scala 3 `given` declaration. Applied to:
 * - `J.VariableDeclarations` for named given aliases (`given x: Int = 42`)
 *   and named given instances with `with` bodies (`given x: Foo with { ... }`).
 * - `J.MethodDeclaration` for method-shaped given aliases
 *   (`given listOrd[T](using Ord[T]): Ord[List[T]] = ...`).
 *
 * The printer uses this to emit `given` in place of `val`/`def`.
 */
case class Given(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Marks a `J.VariableDeclarations.NamedVariable` whose name is synthesized by the
 * Scala compiler and does not appear in source. The printer omits the name.
 * Used for anonymous `using` parameters (`def f(using Ord[T])`).
 */
case class OmitName(id: UUID) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}

/**
 * Captures additional constructor parameter lists for a Scala 3 class declared
 * with curried constructor params, e.g. `class C(a: Int)(using Executor)`.
 * The J.ClassDeclaration's `primaryConstructor` JContainer only models the first
 * list; this marker holds the verbatim source text of the remaining lists so the
 * printer can re-emit them after the first `)`. Includes the surrounding parens.
 */
case class ExtraConstructorParamLists(id: UUID, text: String) extends Marker {
  override def getId(): UUID = id
  override def withId[M <: Marker](newId: UUID): M = copy(id = newId).asInstanceOf[M]
}
