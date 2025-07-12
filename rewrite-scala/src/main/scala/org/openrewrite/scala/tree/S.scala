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
package org.openrewrite.scala.tree

import java.nio.charset.Charset
import java.nio.file.Path
import java.util.{Collections, List, UUID}
import scala.beans.BeanProperty

import org.jspecify.annotations.Nullable
import org.openrewrite._
import org.openrewrite.java.internal.TypesInUse
import org.openrewrite.java.tree._
import org.openrewrite.marker.Markers

/**
 * The Scala language-specific AST types extend the J interface and its sub-types.
 * S types represent Scala-specific constructs that have no direct equivalent in Java.
 * When a Scala construct can be represented using Java's AST, we compose J types.
 */
trait S extends J {

  override def accept[R <: Tree, P](v: TreeVisitor[R, P], p: P): R = {
    acceptScala(v.adapt(classOf[org.openrewrite.scala.ScalaVisitor[P]]), p).asInstanceOf[R]
  }

  override def isAcceptable[P](v: TreeVisitor[?, P], p: P): Boolean = {
    v.isAdaptableTo(classOf[org.openrewrite.scala.ScalaVisitor[?]])
  }

  def acceptScala[P](v: org.openrewrite.scala.ScalaVisitor[P], p: P): J
}

object S {

  /**
   * Represents a Scala compilation unit (.scala file).
   * Extends J.CompilationUnit to reuse package, imports, and type declarations.
   */
  class CompilationUnit(
    @BeanProperty val id: UUID,
    @BeanProperty val prefix: Space,
    @BeanProperty val markers: Markers,
    @Nullable @BeanProperty val charsetName: String,
    @BeanProperty val charsetBomMarked: Boolean,
    @Nullable @BeanProperty val fileAttributes: FileAttributes,
    private var _sourcePath: Path,
    @Nullable @BeanProperty val checksum: Checksum,
    @Nullable @BeanProperty val packageDeclaration: J.Package,
    @BeanProperty val imports: List[J.Import],
    val statements: List[Statement],
    @BeanProperty val eof: Space
  ) extends S with JavaSourceFile with SourceFile {

    def getSourcePath: Path = _sourcePath
    def getStatements: List[Statement] = statements

    override def acceptScala[P](v: org.openrewrite.scala.ScalaVisitor[P], p: P): J = {
      v.visitCompilationUnit(this, p)
    }

    override def withImports(imports: List[J.Import]): S.CompilationUnit = {
      if (imports `eq` this.imports) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )
    }

    override def printer[P](cursor: Cursor): TreeVisitor[?, PrintOutputCapture[P]] = {
      new org.openrewrite.scala.ScalaPrinter[P]()
    }

    override def getTypesInUse: TypesInUse = {
      // TODO: Implement type usage tracking for Scala
      TypesInUse.build(this)
    }

    override def getCharset: Charset = {
      if (charsetName == null) Charset.defaultCharset()
      else Charset.forName(charsetName)
    }

    override def isCharsetBomMarked(): Boolean = charsetBomMarked

    override def getClasses: List[J.ClassDeclaration] = {
      // TODO: Extract class declarations from statements
      Collections.emptyList()
    }

    override def withClasses(classes: List[J.ClassDeclaration]): S.CompilationUnit = {
      // TODO: Handle class updates
      this
    }

    override def withPackageDeclaration(pkg: J.Package): S.CompilationUnit = {
      if (pkg `eq` this.packageDeclaration) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, pkg, imports, statements, eof
      )
    }

    override def withEof(eof: Space): S.CompilationUnit = {
      if (eof `eq` this.eof) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )
    }

    // Implement the generic signature from SourceFile
    override def withSourcePath[T <: SourceFile](sourcePath: Path): T = {
      (if (_sourcePath.equals(sourcePath)) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }
    
    // Also provide the non-generic signature expected by JavaSourceFile
    def withSourcePath(sourcePath: Path): S.CompilationUnit = {
      if (_sourcePath.equals(sourcePath)) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        sourcePath, checksum, packageDeclaration, imports, statements, eof
      )
    }

    override def withChecksum[T <: SourceFile](@Nullable checksum: Checksum): T = {
      (if (checksum `eq` this.checksum) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }

    override def withCharset[T <: SourceFile](charset: Charset): T = {
      val newCharsetName = charset.name()
      (if (newCharsetName == this.charsetName) this
      else new S.CompilationUnit(
        id, prefix, markers, newCharsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }

    // With methods for each field to support the @With pattern from Lombok
    override def withId[T <: Tree](id: UUID): T = {
      (if (id `eq` this.id) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }

    override def withPrefix[J2 <: J](prefix: Space): J2 = {
      (if (prefix `eq` this.prefix) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[J2]
    }

    override def withMarkers[T <: Tree](markers: Markers): T = {
      (if (markers `eq` this.markers) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }

    def withCharsetName(@Nullable charsetName: String): S.CompilationUnit = {
      if (charsetName == this.charsetName) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )
    }

    override def withCharsetBomMarked[T <: SourceFile](charsetBomMarked: Boolean): T = {
      (if (charsetBomMarked == this.charsetBomMarked) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }

    override def withFileAttributes[T <: SourceFile](@Nullable fileAttributes: FileAttributes): T = {
      (if (fileAttributes `eq` this.fileAttributes) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )).asInstanceOf[T]
    }

    def withStatements(statements: List[Statement]): S.CompilationUnit = {
      if (statements `eq` this.statements) this
      else new S.CompilationUnit(
        id, prefix, markers, charsetName, charsetBomMarked, fileAttributes,
        _sourcePath, checksum, packageDeclaration, imports, statements, eof
      )
    }

    override def getPadding: JavaSourceFile.Padding = new Padding(this)

    override def equals(obj: Any): Boolean = obj match {
      case other: S.CompilationUnit => id == other.id
      case _ => false
    }

    override def hashCode(): Int = id.hashCode()

    class Padding(t: S.CompilationUnit) extends JavaSourceFile.Padding {
      def getPackageDeclaration: JRightPadded[J.Package] = {
        if (t.packageDeclaration == null) null
        else JRightPadded.build(t.packageDeclaration.asInstanceOf[J.Package])
      }

      override def getImports: List[JRightPadded[J.Import]] = {
        // TODO: Return properly padded imports
        Collections.emptyList()
      }

      override def withImports(imports: List[JRightPadded[J.Import]]): JavaSourceFile = {
        // TODO: Handle import updates
        t
      }
    }
  }
}