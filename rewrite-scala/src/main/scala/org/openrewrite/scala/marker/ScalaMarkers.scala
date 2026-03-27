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