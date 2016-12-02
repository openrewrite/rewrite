/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.java.refactor.ast

import com.netflix.java.refactor.ast.visitor.AstVisitor
import com.netflix.java.refactor.ast.visitor.PrintVisitor
import com.netflix.java.refactor.ast.visitor.RetrieveCursorVisitor
import com.netflix.java.refactor.refactor.Refactor
import com.netflix.java.refactor.search.*
import groovy.lang.Closure
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern
import kotlin.reflect.KClass

interface Tree {
    val formatting: Formatting

    /**
     * An overload that allows us to create a copy of any Tree element, optionally
     * changing formatting
     */
    fun copy(fmt: Formatting = formatting): Tree
    
    /**
     * An id that can be used to identify a particular AST element, even after transformations have taken place on it
     */
    val id: String

    fun <R> accept(v: AstVisitor<R>): R = v.default(null)
    fun format(): Tree = throw NotImplementedError()
    fun printTrimmed() = print().trimIndent().trim()
    fun print() = PrintVisitor().visit(this)
}

interface Statement : Tree

interface Expression : Tree {
    val type: Type?
}

/**
 * A tree representing a simple or fully qualified name
 */
interface NameTree : Tree {
    val type: Type?
}

/**
 * A tree identifying a type (e.g. a simple or fully qualified class name, a primitive, array, or parameterized type)
 */
interface TypeTree: NameTree

/**
 * The stylistic surroundings of a tree element
 */
sealed class Formatting {

    open fun withPrefix(prefix: String): Formatting = when(this) {
        is Infer -> Reified(prefix, "")
        is Reified -> Reified(prefix, suffix)
        is None -> Reified(prefix, "")
    }

    /**
     * Formatting should be inferred and reified from surrounding context
     */
    object Infer : Formatting()

    data class Reified(val prefix: String, val suffix: String = "") : Formatting() {
        companion object {
            val Empty = Reified("")
        }
    }

    object None : Formatting()
}

sealed class Tr : Serializable, Tree {
    
    companion object {
        val random = Random()

        // probabilities of collisions are governed by this approximation: https://en.wikipedia.org/wiki/Universally_unique_identifier#Random_UUID_probability_of_duplicates
        // 1-e^(-((2^28)^2)/(2*2^80)) = 3E-8 for 260 million unique elements
        fun id(): String {
            val buffer = ByteArray(10)
            random.nextBytes(buffer)
            return Base64.getEncoder().encodeToString(buffer)
        }
    }
    
    data class Annotation(var annotationType: NameTree,
                          var args: Arguments?,
                          override val type: Type?,
                          override val formatting: Formatting = Formatting.Reified.Empty,
                          override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
            v.reduce(v.visitAnnotation(this), v.visitExpression(this))

        data class Arguments(val args: List<Expression>, override val formatting: Formatting = Formatting.Reified.Empty, 
                             override val id: String = id()): Tr()
    }

    data class ArrayAccess(val indexed: Expression,
                           val dimension: Dimension,
                           override val type: Type?,
                           override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitArrayAccess(this), v.visitExpression(this))

        data class Dimension(val index: Expression, override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()): Tr()
    }

    data class ArrayType(val elementType: TypeTree,
                         val dimensions: List<Dimension>,
                         override val formatting: Formatting = Formatting.Reified.Empty,
                         override val id: String = id()): TypeTree, Expression, Tr() {

        @Transient
        override val type = elementType.type

        override fun <R> accept(v: AstVisitor<R>): R = v.visitArrayType(this)

        data class Dimension(val inner: Empty, override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()): Tr()
    }

    data class Assign(val variable: Expression,
                      val assignment: Expression,
                      override val type: Type?,
                      override val formatting: Formatting = Formatting.Reified.Empty,
                      override val id: String = id()) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitAssign(this), v.visitExpression(this))
    }

    data class AssignOp(val variable: Expression,
                        val operator: Operator,
                        val assignment: Expression,
                        override val type: Type?,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitAssignOp(this), v.visitExpression(this))

        sealed class Operator: Tr() {
            // Arithmetic
            data class Addition(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Subtraction(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Multiplication(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Division(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Modulo(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()

            // Bitwise
            data class BitAnd(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class BitOr(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class BitXor(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class LeftShift(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class RightShift(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class UnsignedRightShift(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
        }
    }

    data class Binary(val left: Expression,
                      val operator: Operator,
                      val right: Expression,
                      override val type: Type?,
                      override val formatting: Formatting = Formatting.Reified.Empty,
                      override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitBinary(this), v.visitExpression(this))

        sealed class Operator: Tr() {
            // Arithmetic
            data class Addition(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Subtraction(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Multiplication(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Division(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Modulo(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()

            // Relational
            data class LessThan(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class GreaterThan(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class LessThanOrEqual(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class GreaterThanOrEqual(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class Equal(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class NotEqual(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()

            // Bitwise
            data class BitAnd(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class BitOr(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class BitXor(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class LeftShift(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class RightShift(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class UnsignedRightShift(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()

            // Boolean
            data class Or(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
            data class And(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Operator()
        }
    }

    data class Block<out T: Tree>(val static: Tr.Empty?,
                                  val statements: List<T>,
                                  override val formatting: Formatting = Formatting.Reified.Empty,
                                  val endOfBlockSuffix: String,
                                  override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBlock(this)
    }

    data class Break(val label: Ident?,
                     override val formatting: Formatting = Formatting.Reified.Empty,
                     override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitBreak(this)
    }

    data class Case(val pattern: Expression?, // null for the default case
                    val statements: List<Statement>,
                    override val formatting: Formatting = Formatting.Reified.Empty,
                    override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCase(this)
    }

    data class Catch(val param: Parentheses<VariableDecls>,
                     val body: Block<Statement>,
                     override val formatting: Formatting = Formatting.Reified.Empty,
                     override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCatch(this)
    }

    data class ClassDecl(val annotations: List<Annotation>,
                         val modifiers: List<Modifier>,
                         val kind: Kind,
                         val name: Ident,
                         val typeParams: TypeParameters?,
                         val extends: TypeTree?,
                         val implements: List<TypeTree>,
                         val body: Block<Tree>,
                         val type: Type?,
                         override val formatting: Formatting = Formatting.Reified.Empty,
                         override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitClassDecl(this)

        /**
         * Values will always occur before any fields, constructors, or methods
         */
        fun enumValues(): EnumValueSet? = body.statements.find { it is EnumValueSet } as EnumValueSet?

        fun fields(): List<VariableDecls> = body.statements.filterIsInstance<VariableDecls>()
        fun methods(): List<MethodDecl> = body.statements.filterIsInstance<MethodDecl>()

        sealed class Modifier : Tr() {
            data class Public(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Protected(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Private(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Abstract(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Static(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Final(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
        }

        sealed class Kind: Tr() {
            data class Class(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Kind()
            data class Enum(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Kind()
            data class Interface(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Kind()
            data class Annotation(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Kind()
        }

        /**
         * Find fields defined on this class, but do not include inherited fields up the type hierarchy
         */
        fun findFields(clazz: Class<*>): List<Tr.VariableDecls> = FindFields(clazz.name).visit(this)

        fun findFields(clazz: String): List<Tr.VariableDecls> = FindFields(clazz).visit(this)

        /**
         * Find fields defined up the type hierarchy, but do not include fields defined directly on this class
         */
        fun findInheritedFields(clazz: Class<*>): List<Type.Var> = FindInheritedFields(clazz.name).visit(this)

        fun findInheritedFields(clazz: String): List<Type.Var> = FindInheritedFields(clazz).visit(this)

        fun findMethodCalls(signature: String): List<Tr.MethodInvocation> = FindMethods(signature).visit(this)

        fun findType(clazz: Class<*>): List<NameTree> = FindType(clazz.name).visit(this)
        fun findType(clazz: String): List<NameTree> = FindType(clazz).visit(this)

        fun findAnnotations(signature: String): List<Tr.Annotation> = FindAnnotations(signature).visit(this)

        fun hasType(clazz: Class<*>): Boolean = HasType(clazz.name).visit(this)
        fun hasType(clazz: String): Boolean = HasType(clazz).visit(this)

        fun <M: Modifier> hasModifier(modifier: Class<M>) = modifiers.any { it.javaClass == modifier }

        fun hasModifier(modifier: String) = Modifier::class.nestedClasses
                .filter { it.simpleName?.toLowerCase() == modifier.toLowerCase() }
                .filterIsInstance<KClass<Modifier>>()
                .filter { hasModifier(it.java) }
                .any()

        fun isEnum() = kind is Kind.Enum
        fun isClass() = kind is Kind.Class
        fun isInterface() = kind is Kind.Interface
        fun isAnnotation() = kind is Kind.Annotation

        @Transient val simpleName: String = name.simpleName
    }

    data class CompilationUnit(val sourcePath: Path,
                               val packageDecl: Package?,
                               val imports: List<Import>,
                               val classes: List<ClassDecl>,
                               private val cacheId: String,
                               override val formatting: Formatting = Formatting.Reified.Empty,
                               override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitCompilationUnit(this)

        fun hasImport(clazz: Class<*>): Boolean = HasImport(clazz.name).visit(this)
        fun hasImport(clazz: String): Boolean = HasImport(clazz).visit(this)

        fun hasType(clazz: Class<*>): Boolean = HasType(clazz.name).visit(this)
        fun hasType(clazz: String): Boolean = HasType(clazz).visit(this)

        fun findMethodCalls(signature: String): List<Tr.MethodInvocation> = FindMethods(signature).visit(this)

        fun findType(clazz: Class<*>): List<NameTree> = FindType(clazz.name).visit(this)
        fun findType(clazz: String): List<NameTree> = FindType(clazz).visit(this)

        fun refactor() = Refactor(this)

        fun refactor(ops: Refactor.() -> Unit): Refactor {
            val r = refactor()
            ops(r)
            return r
        }

        fun refactor(ops: Consumer<Refactor>): Refactor {
            val r = refactor()
            ops.accept(r)
            return r
        }

        fun refactor(ops: Closure<Refactor>): Refactor {
            val r = refactor()
            ops.delegate = r
            ops.call(r)
            return r
        }

        fun typeCache() = TypeCache.of(cacheId)

        fun firstClass() = classes.firstOrNull()

        fun cursor(t: Tree?) = RetrieveCursorVisitor(t).visit(this)
    }

    data class Continue(val label: Ident?,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitContinue(this)
    }

    data class DoWhileLoop(val body: Statement,
                           val condition: Parentheses<Expression>,
                           override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitDoWhileLoop(this)
    }

    data class Empty(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Statement, Expression, TypeTree, NameTree, Tr() {
        override val type: Type? = null
        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitEmpty(this), v.visitExpression(this))
    }

    data class EnumValue(val name: Ident,
                         val initializer: Arguments?,
                         override val formatting: Formatting = Formatting.Reified.Empty,
                         override val id: String = id()): Statement, Tr() {
        override fun <R> accept(v: AstVisitor<R>): R = v.visitEnumValue(this)

        data class Arguments(val args: List<Expression>, override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Tr()

        @Transient val simpleName: String = name.simpleName
    }

    data class EnumValueSet(val enums: List<EnumValue>,
                            val terminatedWithSemicolon: Boolean,
                            override val formatting: Formatting = Formatting.Reified.Empty,
                            override val id: String = id()): Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitEnumValueSet(this)
    }

    data class FieldAccess(val target: Expression,
                           val name: Ident,
                           override val type: Type?,
                           override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()) : Expression, NameTree, TypeTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitFieldAccess(this), v.visitExpression(this))

        /**
         * Make debugging a bit easier
         */
        override fun toString(): String = "FieldAccess(${printTrimmed()})"

        @Transient val simpleName: String = name.simpleName
    }

    data class ForEachLoop(val control: Control,
                           val body: Statement,
                           override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitForEachLoop(this)

        data class Control(val variable: VariableDecls,
                           val iterable: Expression,
                           override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()): Tr()
    }

    data class ForLoop(val control: Control,
                       val body: Statement,
                       override val formatting: Formatting = Formatting.Reified.Empty,
                       override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitForLoop(this)

        data class Control(val init: Statement, // either Tr.Empty or Tr.VariableDecls
                           val condition: Expression,
                           val update: List<Statement>,
                           override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()): Tr()
    }

    data class Ident(val simpleName: String,
                     override val type: Type?,
                     override val formatting: Formatting = Formatting.Reified.Empty,
                     override val id: String = id()) : Expression, NameTree, TypeTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitIdentifier(this), v.visitExpression(this))

        /**
         * Make debugging a bit easier
         */
        override fun toString(): String = "Ident(${printTrimmed()})"
    }

    data class If(val ifCondition: Parentheses<Expression>,
                  val thenPart: Statement,
                  val elsePart: Else?,
                  override val formatting: Formatting = Formatting.Reified.Empty,
                  override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitIf(this)

        data class Else(val statement: Statement,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()): Tr()
    }

    data class Import(val qualid: FieldAccess,
                      val static: Boolean,
                      override val formatting: Formatting = Formatting.Reified.Empty,
                      override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitImport(this)

        fun matches(clazz: String): Boolean = when (qualid.simpleName) {
            "*" -> qualid.target.printTrimmed() == clazz.split('.').takeWhile { it[0].isLowerCase() }.joinToString(".")
            else -> qualid.printTrimmed() == clazz
        }
    }

    data class InstanceOf(val expr: Expression,
                          val clazz: Tree,
                          override val type: Type?,
                          override val formatting: Formatting = Formatting.Reified.Empty,
                          override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitInstanceOf(this), v.visitExpression(this))
    }

    data class Label(val label: Ident,
                     val statement: Statement,
                     override val formatting: Formatting = Formatting.Reified.Empty,
                     override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitLabel(this)
    }

    data class Lambda(val params: List<VariableDecls>,
                      val arrow: Arrow,
                      val body: Tree,
                      override val type: Type?,
                      override val formatting: Formatting = Formatting.Reified.Empty,
                      override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitLambda(this), v.visitExpression(this))

        data class Arrow(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Tr()
    }

    data class Literal(val typeTag: TypeTag,
                       val value: Any?,
                       val valueSource: String,
                       override val type: Type?,
                       override val formatting: Formatting = Formatting.Reified.Empty,
                       override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitLiteral(this), v.visitExpression(this))

        /**
         * Primitive values sometimes contain a prefix and suffix that hold the special characters,
         * e.g. the "" around String, the L at the end of a long, etc.
         */
        fun <T> transformValue(transform: (T) -> Any): String {
            val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(this.printTrimmed().replace("\\", ""))
            @Suppress("UNREACHABLE_CODE")
            return when (valueMatcher) {
                is MatchResult -> {
                    val (prefix, suffix) = valueMatcher.groupValues.drop(1)
                    @Suppress("UNCHECKED_CAST")
                    return "$prefix${transform(value as T)}$suffix"
                }
                else -> {
                    throw IllegalStateException("Encountered a literal `$this` that could not be transformed")
                }
            }
        }
    }

    data class MethodDecl(val annotations: List<Annotation>,
                          val modifiers: List<Modifier>,
                          val typeParameters: TypeParameters?,
                          val returnTypeExpr: TypeTree?, // null for constructors
                          val name: Ident,
                          val params: Parameters,
                          val throws: Throws?,
                          val body: Block<Statement>?,
                          val defaultValue: Expression?,
                          override val formatting: Formatting = Formatting.Reified.Empty,
                          override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMethod(this)

        sealed class Modifier: Tr() {
            data class Default(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Public(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
            data class Protected(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
            data class Private(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
            data class Abstract(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
            data class Static(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
            data class Final(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
            data class Synchronized(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Modifier()
        }

        fun hasType(clazz: Class<*>): Boolean = HasType(clazz.name).visit(this)
        fun hasType(clazz: String): Boolean = HasType(clazz).visit(this)

        data class Parameters(val params: List<Statement>, override val formatting: Formatting = Formatting.Reified.Empty,
                              override val id: String = id()): Tr()

        data class Throws(val exceptions: List<NameTree>,
                          override val formatting: Formatting = Formatting.Reified.Empty,
                          override val id: String = id()): Tr()

        fun <M: Modifier> hasModifier(modifier: Class<M>) = modifiers.any { it.javaClass == modifier }

        fun hasModifier(modifier: String) = Modifier::class.nestedClasses
                .filter { it.simpleName?.toLowerCase() == modifier.toLowerCase() }
                .filterIsInstance<KClass<Modifier>>()
                .filter { hasModifier(it.java) }
                .any()

        fun findAnnotations(signature: String): List<Tr.Annotation> = FindAnnotations(signature).visit(this)

        @Transient val simpleName: String = name.simpleName
    }

    data class MethodInvocation(val select: Expression?,
                                val typeParameters: TypeParameters?,
                                val name: Ident,
                                val args: Arguments,
                                val declaringType: Type.Class?,
                                override val type: Type.Method?,
                                override val formatting: Formatting = Formatting.Reified.Empty,
                                override val id: String = id()) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitMethodInvocation(this), v.visitExpression(this))

        fun returnType(): Type? = type?.resolvedSignature?.returnType

        fun firstMethodInChain(): MethodInvocation =
            if(select is MethodInvocation)
                select.firstMethodInChain()
            else this

        fun argExpressions() = args.args.filter { it !is Tr.Empty }

        data class Arguments(val args: List<Expression>, override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Tr()
        data class TypeParameters(val params: List<NameTree>, override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Tr()

        @Transient val simpleName: String = name.simpleName
    }

    data class MultiCatch(val alternatives: List<NameTree>,
                          override val formatting: Formatting = Formatting.Reified.Empty,
                          override val id: String = id()): TypeTree, Tr() {
        override val type: Type by lazy { throw IllegalArgumentException("Multi-catch does not represent a single type") }

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMultiCatch(this)
    }

    data class NewArray(val typeExpr: TypeTree?, // null in the case of an array as an annotation parameter
                        val dimensions: List<Dimension>,
                        val initializer: Initializer?,
                        override val type: Type?,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitNewArray(this), v.visitExpression(this))

        data class Dimension(val size: Expression, override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()): Tr()

        data class Initializer(val elements: List<Expression>, override val formatting: Formatting = Formatting.Reified.Empty,
                               override val id: String = id()): Tr()
    }

    data class NewClass(val clazz: TypeTree,
                        val args: Arguments,
                        val classBody: Block<Tree>?, // non-null for anonymous classes
                        override val type: Type?,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitNewClass(this), v.visitExpression(this))

        data class Arguments(val args: List<Expression>, override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()): Tr()
    }

    data class Package(val expr: Expression, override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitPackage(this)
    }

    data class ParameterizedType(val clazz: NameTree,
                                 val typeArguments: TypeArguments?,
                                 override val formatting: Formatting = Formatting.Reified.Empty,
                                 override val id: String = id()): TypeTree, Expression, Tr() {

        @Transient override val type = clazz.type

        override fun <R> accept(v: AstVisitor<R>): R = v.visitParameterizedType(this)

        data class TypeArguments(val args: List<Expression>, /* TypeTree or Wildcard */
                                 override val formatting: Formatting = Formatting.Reified.Empty,
                                 override val id: String = id()): Tr()
    }

    data class Parentheses<out T: Tree>(val tree: T,
                                        override val formatting: Formatting = Formatting.Reified.Empty,
                                        override val id: String = id()) : Expression, Tr() {

        override val type = when(tree) {
            is Expression -> tree.type
            else -> null
        }

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitParentheses(this), v.visitExpression(this))
    }

    data class Primitive(val typeTag: TypeTag,
                         override val type: Type?,
                         override val formatting: Formatting = Formatting.Reified.Empty,
                         override val id: String = id()) : Expression, NameTree, TypeTree, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitPrimitive(this), v.visitExpression(this))
    }

    data class Return(val expr: Expression?,
                      override val formatting: Formatting = Formatting.Reified.Empty,
                      override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitReturn(this)
    }

    data class Switch(val selector: Parentheses<Expression>,
                      val cases: Block<Case>,
                      override val formatting: Formatting = Formatting.Reified.Empty,
                      override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitSwitch(this)
    }

    data class Synchronized(val lock: Parentheses<Expression>,
                            val body: Block<Statement>,
                            override val formatting: Formatting = Formatting.Reified.Empty,
                            override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitSynchronized(this)
    }

    data class Ternary(val condition: Expression,
                       val truePart: Expression,
                       val falsePart: Expression,
                       override val type: Type?,
                       override val formatting: Formatting = Formatting.Reified.Empty,
                       override val id: String = id()) : Expression, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitTernary(this), v.visitExpression(this))
    }

    data class Throw(val exception: Expression,
                     override val formatting: Formatting = Formatting.Reified.Empty,
                     override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitThrow(this)
    }

    data class Try(val resources: Resources?,
                   val body: Block<Statement>,
                   val catches: List<Catch>,
                   val finally: Finally?,
                   override val formatting: Formatting = Formatting.Reified.Empty,
                   override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTry(this)

        data class Resources(val decls: List<VariableDecls>, override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()): Tr()

        data class Finally(val block: Block<Statement>, override val formatting: Formatting = Formatting.Reified.Empty,
                           override val id: String = id()): Tr()
    }

    data class TypeCast(val clazz: Parentheses<TypeTree>,
                        val expr: Expression,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()): Expression, Tr() {

        override val type = clazz.type

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitTypeCast(this), v.visitExpression(this))
    }

    data class TypeParameter(val annotations: List<Annotation>,
                             val name: NameTree,
                             val bounds: Bounds?,
                             override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()) : Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTypeParameter(this)

        data class Bounds(val types: List<TypeTree>,
                          override val formatting: Formatting = Formatting.Reified.Empty,
                          override val id: String = id()) : Tr()
    }

    data class TypeParameters(val params: List<TypeParameter>,
                              override val formatting: Formatting = Formatting.Reified.Empty,
                              override val id: String = id()): Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitTypeParameters(this)
    }

    /**
     * Increment and decrement operations are valid statements, other operations are not
     */
    data class Unary(val operator: Operator,
                     val expr: Expression,
                     override val type: Type?,
                     override val formatting: Formatting = Formatting.Reified.Empty,
                     override val id: String = id()) : Expression, Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.reduce(v.visitUnary(this), v.visitExpression(this))

        sealed class Operator: Tr() {
            // Arithmetic
            data class PreIncrement(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()
            data class PreDecrement(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()
            data class PostIncrement(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()
            data class PostDecrement(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()
            data class Positive(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()
            data class Negative(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()

            // Bitwise
            data class Complement(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()

            // Boolean
            data class Not(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Operator()
        }
    }

    data class UnparsedSource(val source: String, override val formatting: Formatting = Formatting.Reified.Empty,
                              override val id: String = id()): Expression, Statement, Tr() {
        override val type: Type? = null

        override fun <R> accept(v: AstVisitor<R>): R =
                v.reduce(v.visitUnparsedSource(this), v.visitExpression(this))
    }

    data class VariableDecls(
            val annotations: List<Annotation>,
            val modifiers: List<Modifier>,
            val typeExpr: TypeTree,
            val varArgs: Varargs?,
            val dimensionsBeforeName: List<Dimension>,
            val vars: List<NamedVar>,
            override val formatting: Formatting = Formatting.Reified.Empty,
            override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitMultiVariable(this)

        sealed class Modifier: Tr() {
            data class Public(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Protected(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Private(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Abstract(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Static(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Final(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Transient(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
            data class Volatile(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Modifier()
        }

        data class Varargs(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Tr()

        data class Dimension(val whitespace: Tr.Empty, override val formatting: Formatting = Formatting.Reified.Empty,
                             override val id: String = id()): Tr()

        data class NamedVar(val name: Ident,
                            val dimensionsAfterName: List<Dimension>, // thanks for making it hard, Java
                            val initializer: Expression?,
                            val type: Type?,
                            override val formatting: Formatting = Formatting.Reified.Empty,
                            override val id: String = id()): Tr() {

            override fun <R> accept(v: AstVisitor<R>): R = v.visitVariable(this)

            @Transient val simpleName: String = name.simpleName
        }

        fun <M: Modifier> hasModifier(modifier: Class<M>) = modifiers.any { it.javaClass == modifier }

        fun hasModifier(modifier: String) = Modifier::class.nestedClasses
                .filter { it.simpleName?.toLowerCase() == modifier.toLowerCase() }
                .filterIsInstance<KClass<Modifier>>()
                .filter { hasModifier(it.java) }
                .any()

        fun findAnnotations(signature: String): List<Tr.Annotation> = FindAnnotations(signature).visit(this)
    }

    data class WhileLoop(val condition: Parentheses<Expression>,
                         val body: Statement,
                         override val formatting: Formatting = Formatting.Reified.Empty,
                         override val id: String = id()) : Statement, Tr() {

        override fun <R> accept(v: AstVisitor<R>): R = v.visitWhileLoop(this)
    }

    data class Wildcard(val bound: Bound?,
                        val boundedType: NameTree?,
                        override val formatting: Formatting = Formatting.Reified.Empty,
                        override val id: String = id()): Tr(), Expression {

        override val type = null

        override fun <R> accept(v: AstVisitor<R>): R = v.visitWildcard(this)

        sealed class Bound: Tr() {
            data class Super(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Bound()
            data class Extends(override val formatting: Formatting = Formatting.Reified.Empty, override val id: String = id()): Bound()
        }
    }

    /**
     * An overload that allows us to create a copy of any Tree element, optionally
     * changing formatting
     */
    override fun copy(fmt: Formatting): Tr = when(this) {
        is Tr.Annotation -> copy(formatting = fmt)
        is Tr.Annotation.Arguments -> copy(formatting = fmt)
        is Tr.ArrayAccess -> copy(formatting = fmt)
        is Tr.ArrayAccess.Dimension -> copy(formatting = fmt)
        is Tr.ArrayType -> copy(formatting = fmt)
        is Tr.ArrayType.Dimension -> copy(formatting = fmt)
        is Tr.Assign -> copy(formatting = fmt)
        is Tr.AssignOp -> copy(formatting = fmt)
        is Tr.AssignOp.Operator -> copy()
        is Tr.Binary -> copy(formatting = fmt)
        is Tr.Binary.Operator -> copy()
        is Tr.Block<*> -> copy(formatting = fmt)
        is Tr.Break -> copy(formatting = fmt)
        is Tr.Case -> copy(formatting = fmt)
        is Tr.Catch -> copy(formatting = fmt)
        is Tr.ClassDecl -> copy(formatting = fmt)
        is Tr.ClassDecl.Modifier -> copy()
        is Tr.ClassDecl.Kind -> copy()
        is Tr.CompilationUnit -> copy(formatting = fmt)
        is Tr.Continue -> copy(formatting = fmt)
        is Tr.DoWhileLoop -> copy(formatting = fmt)
        is Tr.Empty -> copy(formatting = fmt)
        is Tr.EnumValue -> copy(formatting = fmt)
        is Tr.EnumValue.Arguments -> copy(formatting = fmt)
        is Tr.EnumValueSet -> copy(formatting = fmt)
        is Tr.FieldAccess -> copy(formatting = fmt)
        is Tr.ForEachLoop -> copy(formatting = fmt)
        is Tr.ForEachLoop.Control -> copy(formatting = fmt)
        is Tr.ForLoop -> copy(formatting = fmt)
        is Tr.ForLoop.Control -> copy(formatting = fmt)
        is Tr.Ident -> copy(formatting = fmt)
        is Tr.If -> copy(formatting = fmt)
        is Tr.If.Else -> copy(formatting = fmt)
        is Tr.Import -> copy(formatting = fmt)
        is Tr.InstanceOf -> copy(formatting = fmt)
        is Tr.Label -> copy(formatting = fmt)
        is Tr.Lambda -> copy(formatting = fmt)
        is Tr.Lambda.Arrow -> copy(formatting = fmt)
        is Tr.Literal -> copy(formatting = fmt)
        is Tr.MethodDecl -> copy(formatting = fmt)
        is Tr.MethodDecl.Modifier -> copy()
        is Tr.MethodDecl.Parameters -> copy(formatting = fmt)
        is Tr.MethodDecl.Throws -> copy(formatting = fmt)
        is Tr.MethodInvocation -> copy(formatting = fmt)
        is Tr.MethodInvocation.Arguments -> copy(formatting = fmt)
        is Tr.MethodInvocation.TypeParameters -> copy(formatting = fmt)
        is Tr.MultiCatch -> copy(formatting = fmt)
        is Tr.NewArray -> copy(formatting = fmt)
        is Tr.NewArray.Dimension -> copy(formatting = fmt)
        is Tr.NewArray.Initializer -> copy(formatting = fmt)
        is Tr.NewClass -> copy(formatting = fmt)
        is Tr.NewClass.Arguments -> copy(formatting = fmt)
        is Tr.Package -> copy(formatting = fmt)
        is Tr.ParameterizedType -> copy(formatting = fmt)
        is Tr.ParameterizedType.TypeArguments -> copy(formatting = fmt)
        is Tr.Parentheses<*> -> copy(formatting = fmt)
        is Tr.Primitive -> copy(formatting = fmt)
        is Tr.Return -> copy(formatting = fmt)
        is Tr.Switch -> copy(formatting = fmt)
        is Tr.Synchronized -> copy(formatting = fmt)
        is Tr.Ternary -> copy(formatting = fmt)
        is Tr.Throw -> copy(formatting = fmt)
        is Tr.Try -> copy(formatting = fmt)
        is Tr.Try.Resources -> copy(formatting = fmt)
        is Tr.Try.Finally -> copy(formatting = fmt)
        is Tr.TypeCast -> copy(formatting = fmt)
        is Tr.TypeParameter -> copy(formatting = fmt)
        is Tr.TypeParameters -> copy(formatting = fmt)
        is Tr.Unary -> copy(formatting = fmt)
        is Tr.Unary.Operator -> copy()
        is Tr.UnparsedSource -> copy(formatting = fmt)
        is Tr.VariableDecls -> copy(formatting = fmt)
        is Tr.VariableDecls.Modifier -> copy()
        is Tr.VariableDecls.Varargs -> copy(formatting = fmt)
        is Tr.VariableDecls.Dimension -> copy(formatting = fmt)
        is Tr.VariableDecls.NamedVar -> copy(formatting = fmt)
        is Tr.WhileLoop -> copy(formatting = fmt)
        is Tr.Wildcard -> copy(formatting = fmt)
        is Tr.Wildcard.Bound -> copy()
        is Tr.TypeParameter.Bounds -> copy(formatting = fmt)
    }

}
