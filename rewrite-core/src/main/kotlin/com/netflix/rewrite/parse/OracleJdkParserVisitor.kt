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
package com.netflix.rewrite.parse

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.ast.Tree
import com.sun.source.tree.*
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.DocCommentTable
import com.sun.tools.javac.tree.EndPosTable
import com.sun.tools.javac.tree.JCTree
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.nio.file.Path
import java.util.*
import java.util.regex.Pattern
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeKind

private typealias JdkTree = com.sun.source.tree.Tree

@Suppress("UNUSED_PARAMETER")
class OracleJdkParserVisitor(val path: Path, val source: String): TreePathScanner<Tree, Formatting.Reified>() {
    private lateinit var endPosTable: EndPosTable
    private lateinit var docTable: DocCommentTable
    private var cursor: Int = 0

    companion object {
        private val logger = LoggerFactory.getLogger(OracleJdkParserVisitor::class.java)
    }

    override fun visitAnnotation(node: AnnotationTree, fmt: Formatting.Reified): Tree {
        skip("@")
        val name = node.annotationType.convert<NameTree>()

        val args = if(node.arguments.size > 0) {
            val argsPrefix = sourceBefore("(")
            val args: List<Expression> = if (node.arguments.size == 1) {
                val arg = node.arguments[0]
                listOf(when(arg) {
                    is JCTree.JCAssign -> {
                        if (arg.endPos() < 0) {
                            // this is the "value" argument, but without an explicit "value = ..."
                            arg.rhs.convert { sourceBefore(")") }
                        } else {
                            // this is either an explicit "value" argument or is assigning some other property
                            arg.convert { sourceBefore(")") }
                        }
                    }
                    is JCTree.JCFieldAccess -> {
                        arg.convert { sourceBefore(")") }
                    }
                    else -> arg.convert { sourceBefore(")") }
                })
             } else {
                node.arguments.convertAll(COMMA_DELIM, { sourceBefore(")") })
            }

            Tr.Annotation.Arguments(args, format(argsPrefix))
        } else {
            val remaining = source.substring(cursor, node.endPos())

            // NOTE: technically, if there is code like this, we have a bug, but seems exceedingly unlikely:
            // @MyAnnotation /* Comment () that contains parentheses */ ()

            if(remaining.contains("(") && remaining.contains(")")) {
                val parenPrefix = sourceBefore("(")
                Tr.Annotation.Arguments(listOf(Tr.Empty(format(sourceBefore(")")))), format(parenPrefix))
            } else null
        }

        return Tr.Annotation(name, args, node.type(), fmt)
    }

    override fun visitArrayAccess(node: ArrayAccessTree, fmt: Formatting.Reified): Tree {
        val indexed = node.expression.convert<Expression>()

        val dimensionPrefix = sourceBefore("[")
        val dimension = Tr.ArrayAccess.Dimension(node.index.convert<Expression> { sourceBefore("]") },
                format(dimensionPrefix))

        return Tr.ArrayAccess(indexed, dimension, node.type(), fmt)
    }

    override fun visitArrayType(node: ArrayTypeTree, fmt: Formatting.Reified): Tree {
        var typeIdent = node.type
        var dimCount = 1
        while(typeIdent is ArrayTypeTree) {
            dimCount++
            typeIdent = typeIdent.type
        }

        val elemType = typeIdent.convert<TypeTree>()

        val dimensions = (1..dimCount).map {
            val dimPrefix = sourceBefore("[")
            Tr.ArrayType.Dimension(Tr.Empty(format(sourceBefore("]"))), format(dimPrefix))
        }

        return Tr.ArrayType(elemType, dimensions, fmt)
    }

    override fun visitAssert(node: AssertTree, fmt: Formatting.Reified): Tree {
        skip("assert")
        return Tr.Assert((node as JCTree.JCAssert).cond.convert(), fmt)
    }

    override fun visitAssignment(node: AssignmentTree, fmt: Formatting.Reified): Tree {
        val variable = node.variable.convert<Expression> { sourceBefore("=") }
        val assignment = node.expression.convert<Expression>()
        return Tr.Assign(variable, assignment, node.type(), fmt)
    }

    override fun visitBinary(node: BinaryTree, fmt: Formatting.Reified): Tree {
        val left = node.leftOperand.convert<Expression>()

        val opPrefix = format(whitespace())
        val op = when ((node as JCTree.JCBinary).tag) {
            JCTree.Tag.PLUS -> { skip("+"); Tr.Binary.Operator.Addition(opPrefix) }
            JCTree.Tag.MINUS -> { skip("-"); Tr.Binary.Operator.Subtraction(opPrefix) }
            JCTree.Tag.DIV -> { skip("/"); Tr.Binary.Operator.Division(opPrefix) }
            JCTree.Tag.MUL -> { skip("*"); Tr.Binary.Operator.Multiplication(opPrefix) }
            JCTree.Tag.MOD -> { skip("%"); Tr.Binary.Operator.Modulo(opPrefix) }
            JCTree.Tag.AND -> { skip("&&"); Tr.Binary.Operator.And(opPrefix) }
            JCTree.Tag.OR -> { skip("||"); Tr.Binary.Operator.Or(opPrefix) }
            JCTree.Tag.BITAND -> { skip("&"); Tr.Binary.Operator.BitAnd(opPrefix) }
            JCTree.Tag.BITOR -> { skip("|"); Tr.Binary.Operator.BitOr(opPrefix) }
            JCTree.Tag.BITXOR -> { skip("^"); Tr.Binary.Operator.BitXor(opPrefix) }
            JCTree.Tag.SL -> { skip("<<"); Tr.Binary.Operator.LeftShift(opPrefix) }
            JCTree.Tag.SR -> { skip(">>"); Tr.Binary.Operator.RightShift(opPrefix) }
            JCTree.Tag.USR -> { skip(">>>"); Tr.Binary.Operator.UnsignedRightShift(opPrefix) }
            JCTree.Tag.LT -> { skip("<"); Tr.Binary.Operator.LessThan(opPrefix) }
            JCTree.Tag.GT -> { skip(">"); Tr.Binary.Operator.GreaterThan(opPrefix) }
            JCTree.Tag.LE -> { skip("<="); Tr.Binary.Operator.LessThanOrEqual(opPrefix) }
            JCTree.Tag.GE -> { skip(">="); Tr.Binary.Operator.GreaterThanOrEqual(opPrefix) }
            JCTree.Tag.EQ -> { skip("=="); Tr.Binary.Operator.Equal(opPrefix) }
            JCTree.Tag.NE -> { skip("!="); Tr.Binary.Operator.NotEqual(opPrefix) }
            else -> throw IllegalArgumentException("Unexpected binary tag ${node.tag}")
        }

        val right = node.rightOperand.convert<Expression>()

        return Tr.Binary(left, op, right, node.type(), fmt)
    }

    override fun visitBlock(node: BlockTree, fmt: Formatting.Reified): Tree {
        val static = if((node as JCTree.JCBlock).flags and Flags.STATIC.toLong() != 0L) {
            skip("static")
            Tr.Empty(format("", sourceBefore("{")))
        } else {
            skip("{")
            null
        }

        @Suppress("UNCHECKED_CAST")
        val statements = node
                .statements
                .filter {
                    // filter out synthetic super() invocations and the like
                    it.endPos() > 0
                }
                .convertPossibleMultiVariable() as List<Statement>

        return Tr.Block<Statement>(static, statements, fmt, sourceBefore("}"))
    }

    override fun visitBreak(node: BreakTree, fmt: Formatting.Reified): Tree {
        skip("break")
        val label = node.label?.toString()?.let { name ->
            val label = Tr.Ident.build(name, null, format(sourceBefore(name)))
            skip(name)
            label
        }
        return Tr.Break(label, fmt)
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitCase(node: CaseTree, fmt: Formatting.Reified): Tree {
        val pattern = node.expression.convertOrNull<Expression> { sourceBefore(":") } ?:
            Tr.Ident.build(skip("default")!!, null, format(sourceBefore(":")))
        return Tr.Case(
                pattern,
                node.statements.convertPossibleMultiVariable() as List<Statement>,
                fmt
        )
    }

    override fun visitCatch(node: CatchTree, fmt: Formatting.Reified): Tree {
        skip("catch")

        val paramPrefix = sourceBefore("(")
        val paramDecl = node.parameter.convert<Tr.VariableDecls> { sourceBefore(")") }
        val param = Tr.Parentheses(paramDecl, format(paramPrefix))

        return Tr.Catch(param, node.block.convert(), fmt)
    }

    override fun visitClass(node: ClassTree, fmt: Formatting.Reified): Tree {
        val annotations = node.modifiers.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)

        val modifiers = node.modifiers.sortedFlags().map { mod ->
            val modPrefix = whitespace()
            cursor += mod.name.length
            val modFormat = format(modPrefix)
            when (mod) {
                Modifier.PUBLIC -> Tr.Modifier.Public(modFormat)
                Modifier.PROTECTED -> Tr.Modifier.Protected(modFormat)
                Modifier.PRIVATE -> Tr.Modifier.Private(modFormat)
                Modifier.ABSTRACT -> Tr.Modifier.Abstract(modFormat)
                Modifier.STATIC -> Tr.Modifier.Static(modFormat)
                Modifier.FINAL -> Tr.Modifier.Final(modFormat)
                Modifier.STRICTFP -> Tr.Modifier.Strictfp(modFormat)
                else -> throw IllegalArgumentException("Unexpected modifier $mod")
            }
        }

        val kind = if(node.modifiers.hasFlag(Flags.ENUM)) {
            Tr.ClassDecl.Kind.Enum(format(sourceBefore("enum")))
        } else if(node.modifiers.hasFlag(Flags.ANNOTATION)) {
            // note that annotations ALSO have the INTERFACE flag
            Tr.ClassDecl.Kind.Annotation(format(sourceBefore("@interface")))
        } else if(node.modifiers.hasFlag(Flags.INTERFACE)) {
            Tr.ClassDecl.Kind.Interface(format(sourceBefore("interface")))
        } else {
            Tr.ClassDecl.Kind.Class(format(sourceBefore("class")))
        }

        val name = Tr.Ident.build((node as JCTree.JCClassDecl).simpleName.toString(), node.type(),
                format(sourceBefore(node.simpleName.toString())))

        val typeParams = if(node.typeParameters.isNotEmpty()) {
            val genericPrefix = sourceBefore("<")
            Tr.TypeParameters(node.typeParameters.convertAll(COMMA_DELIM, { sourceBefore(">") }),
                    format(genericPrefix))
        } else null

        val extends = node.extendsClause.convertOrNull<TypeTree>()
        val implements = node.implementsClause.convertAll<TypeTree>(COMMA_DELIM, NO_DELIM)

        val bodyPrefix = sourceBefore("{")

        // enum values are required by the grammar to occur before any ordinary field, constructor, or method members

        val jcEnums = node.members
                .filterIsInstance<JCTree.JCVariableDecl>()
                .filter { it.modifiers.hasFlag(Flags.ENUM) }

        val enumSet = if(jcEnums.isNotEmpty()) {
            var semicolonPresent = false

            val enumValues = jcEnums
                .convertAll<Tr.EnumValue>(COMMA_DELIM, {
                    // this semicolon is required when there are non-value members, but can still
                    // be present when there are not
                    semicolonPresent = positionOfNext(";", stop = '}') > 0
                    if (semicolonPresent) sourceBefore(";", stop = '}') else ""
                })

            Tr.EnumValueSet(enumValues, semicolonPresent, Formatting.Empty)
        } else null

        val members = listOf(enumSet).filterNotNull() + node.members
                // we don't care about the compiler-inserted default constructor,
                // since it will never be subject to refactoring
                .filter {
                    when(it) {
                        is JCTree.JCMethodDecl -> !it.modifiers.hasFlag(Flags.GENERATEDCONSTR)
                        is JCTree.JCVariableDecl -> !it.modifiers.hasFlag(Flags.ENUM)
                        else -> true
                    }
                }
                .convertPossibleMultiVariable()

        val body = Tr.Block<Tree>(null, members, format(bodyPrefix), sourceBefore("}"))

        return Tr.ClassDecl(annotations, modifiers, kind, name, typeParams, extends, implements, body, node.type(), fmt)
    }

    override fun visitCompilationUnit(node: CompilationUnitTree, fmt: Formatting.Reified): Tree {
        logger.debug("$path: building Rewrite AST from Oracle AST")

        endPosTable = (node as JCTree.JCCompilationUnit).endPositions
        docTable = node.docComments // TODO when we want to implement refactoring into doc comments as well, refer to this table by JCTree node
        val prefix = source.substring(0, node.startPosition)
        cursor(node.startPosition)

        val packageDecl = if (node.packageName != null) {
            skip("package")
            val pkg = Tr.Package(node.packageName.convert(), Formatting.Empty)
            skip(";")
            pkg
        } else null

        return Tr.CompilationUnit(
                path.toString(),
                packageDecl,
                node.imports.convertAll(SEMI_DELIM, SEMI_DELIM),
                node.typeDecls.filterIsInstance<JCTree.JCClassDecl>().convertAll(this::whitespace, NO_DELIM),
                format(prefix, source.substring(cursor))
        )
    }

    override fun visitCompoundAssignment(node: CompoundAssignmentTree, fmt: Formatting.Reified): Tree {
        val left = (node as JCTree.JCAssignOp).lhs.convert<Expression>()

        val opPrefix = format(whitespace())
        val op = when (node.tag) {
            JCTree.Tag.PLUS_ASG -> { skip("+="); Tr.AssignOp.Operator.Addition(opPrefix) }
            JCTree.Tag.MINUS_ASG -> { skip("-="); Tr.AssignOp.Operator.Subtraction(opPrefix) }
            JCTree.Tag.DIV_ASG -> { skip("/="); Tr.AssignOp.Operator.Division(opPrefix) }
            JCTree.Tag.MUL_ASG -> { skip("*="); Tr.AssignOp.Operator.Multiplication(opPrefix) }
            JCTree.Tag.MOD_ASG -> { skip("%="); Tr.AssignOp.Operator.Modulo(opPrefix) }
            JCTree.Tag.BITAND_ASG -> { skip("&="); Tr.AssignOp.Operator.BitAnd(opPrefix) }
            JCTree.Tag.BITOR_ASG -> { skip("|="); Tr.AssignOp.Operator.BitOr(opPrefix) }
            JCTree.Tag.BITXOR_ASG -> { skip("^="); Tr.AssignOp.Operator.BitXor(opPrefix) }
            JCTree.Tag.SL_ASG -> { skip("<<="); Tr.AssignOp.Operator.LeftShift(opPrefix) }
            JCTree.Tag.SR_ASG -> { skip(">>="); Tr.AssignOp.Operator.RightShift(opPrefix) }
            JCTree.Tag.USR_ASG -> { skip(">>>="); Tr.AssignOp.Operator.UnsignedRightShift(opPrefix) }
            else -> throw IllegalArgumentException("Unexpected compound assignment tag ${node.tag}")
        }

        return Tr.AssignOp(
                left,
                op,
                node.rhs.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitConditionalExpression(node: ConditionalExpressionTree, fmt: Formatting.Reified): Tree {
        return Tr.Ternary(
                node.condition.convert { sourceBefore("?") },
                node.trueExpression.convert { sourceBefore(":") },
                node.falseExpression.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitContinue(node: ContinueTree, fmt: Formatting.Reified): Tree {
        skip("continue")
        return Tr.Continue(
                node.label?.toString()?.let { lbl -> Tr.Ident.build(lbl, null, format(sourceBefore(lbl))) },
                fmt
        )
    }

    override fun visitDoWhileLoop(node: DoWhileLoopTree, fmt: Formatting.Reified): Tree {
        skip("do")
        val stat = node.statement.convert<Statement> { sourceBefore("while") }
        return Tr.DoWhileLoop(
                stat,
                node.condition.convert(),
                fmt
        )
    }

    override fun visitEmptyStatement(node: EmptyStatementTree, fmt: Formatting.Reified): Tree {
        return Tr.Empty(fmt)
    }

    override fun visitEnhancedForLoop(node: EnhancedForLoopTree, fmt: Formatting.Reified): Tree {
        skip("for")
        val ctrlPrefix = sourceBefore("(")
        val variable = node.variable.convert<Tr.VariableDecls> { sourceBefore(":") }
        val expression = node.expression.convert<Expression> { sourceBefore(")") }

        return Tr.ForEachLoop(
                Tr.ForEachLoop.Control(variable, expression, format(ctrlPrefix)),
                node.statement.convert(statementDelim),
                fmt
        )
    }

    fun visitEnumVariable(node: VariableTree, fmt: Formatting.Reified): Tree {
        skip(node.name.toString())
        val name = Tr.Ident.build(node.name.toString(), node.type(), Formatting.Empty)

        val initializer = if(source[node.endPos()-1] == ')') {
            val initPrefix = sourceBefore("(")
            var args = (node.initializer as JCTree.JCNewClass).args.convertAll<Expression>(COMMA_DELIM, { sourceBefore(")") })
            if((node.initializer as JCTree.JCNewClass).args.isEmpty())
                args = listOf(Tr.Empty(format(sourceBefore(")"))))
            Tr.EnumValue.Arguments(args, format(initPrefix))
        } else null

        return Tr.EnumValue(name, initializer, fmt)
    }

    override fun visitForLoop(node: ForLoopTree, fmt: Formatting.Reified): Tree {
        skip("for")
        val ctrlPrefix = sourceBefore("(")

        fun List<JdkTree>.convertAllOrEmpty(innerSuffix: (JdkTree) -> String = { "" },
                                                    suffix: (JdkTree) -> String = { "" }): List<Statement> {
            return when (size) {
                0 -> listOf(Tr.Empty(format("", suffix(object : JCTree.JCSkip() {}))))
                else -> mapIndexed { i, tree ->
                    tree.convert<Statement>(if (i == size - 1) suffix else innerSuffix)
                }
            }
        }

        val init: Statement = node.initializer.convertPossibleMultiVariable().filterIsInstance<Statement>().firstOrNull() ?:
                Tr.Empty(format("", sourceBefore(";")))

        val condition = node.condition.convertOrNull<Expression>(SEMI_DELIM) ?:
                Tr.Empty(format("", sourceBefore(";")))
        val update = node.update.convertAllOrEmpty(COMMA_DELIM, { sourceBefore(")") })

        return Tr.ForLoop(
                Tr.ForLoop.Control(init, condition, update, format(ctrlPrefix)),
                node.statement.convert(statementDelim),
                fmt
        )
    }

    override fun visitIdentifier(node: IdentifierTree, fmt: Formatting.Reified): Tree {
        cursor += node.name.toString().length
        return Tr.Ident.build(node.name.toString(), node.type(), fmt)
    }

    override fun visitIf(node: IfTree, fmt: Formatting.Reified): Tree {
        skip("if")

        val ifPart = node.condition.convert<Tr.Parentheses<Expression>>()
        val then = node.thenStatement.convert<Statement>(statementDelim)

        val elsePart = if(node.elseStatement is JCTree.JCStatement) {
            val elsePrefix = sourceBefore("else")
            Tr.If.Else(node.elseStatement.convert<Statement>(statementDelim), format(elsePrefix))
        } else null

        return Tr.If(ifPart, then, elsePart, fmt)
    }

    override fun visitImport(node: ImportTree, fmt: Formatting.Reified): Tree {
        skip("import")
        skipPattern("\\s+static")
        return Tr.Import(node.qualifiedIdentifier.convert(), node.isStatic, fmt)
    }

    override fun visitInstanceOf(node: InstanceOfTree, fmt: Formatting.Reified): Tree {
        return Tr.InstanceOf(
                node.expression.convert<Expression> { sourceBefore("instanceof") },
                node.type.convert(),
                node.type(),
                fmt
        )
    }

    override fun visitLabeledStatement(node: LabeledStatementTree, fmt: Formatting.Reified): Tree {
        skip(node.label.toString())
        return Tr.Label(
                Tr.Ident.build(node.label.toString(), null, format("", sourceBefore(":"))),
                node.statement.convert(),
                fmt
        )
    }

    override fun visitLambdaExpression(node: LambdaExpressionTree, fmt: Formatting.Reified): Tree {
        val parenthesized = source[cursor] == '('
        skip("(")

        val paramList = if(parenthesized && node.parameters.isEmpty()) {
            listOf(Tr.Empty(format(sourceBefore(")"))))
        } else {
            node.parameters.convertAll(COMMA_DELIM, { if (parenthesized) sourceBefore(")") else "" })
        }

        val params = Tr.Lambda.Parameters(parenthesized, paramList)
        val arrow = Tr.Lambda.Arrow(format(sourceBefore("->")))

        val body = when(node.body) {
            is JCTree.JCBlock -> {
                // This compensates for a bug in the Oracle AST in which the startPos of JCBlock statements
                // that are on the right side of lambda expressions evaluates at the start of the lambda expression.
                // All other AST elements that can occur at the right side of lambda expressions correctly evluate startPos
                // after the arrow.
                val prefix = sourceBefore("{")
                cursor--
                val blockBody = node.body.convert<Tr.Block<*>>()
                blockBody.changeFormatting(blockBody.formatting.withPrefix(prefix))
            }
            else -> node.body.convert<Tree>()
        }

        return Tr.Lambda(
                params,
                arrow,
                body,
                node.type(),
                fmt
        )
    }

    override fun visitLiteral(node: LiteralTree, fmt: Formatting.Reified): Tree {
        cursor(node.endPos())
        val type = (node as JCTree.JCLiteral).typetag.primitive()

        return Tr.Literal(
                when(type) {
                    Type.Primitive.Char -> (node.value as Int).toChar()
                    Type.Primitive.Boolean -> node.value as Int != 0
                    else -> node.value
                },
                source.substring(node.startPosition, node.endPos()),
                type,
                fmt
        )
    }

    override fun visitMemberReference(node: MemberReferenceTree, fmt: Formatting.Reified): Tree {
        val expr = (node as JCTree.JCMemberReference).expr.convert<Expression> { sourceBefore("::") }
        val referenceName = when(node.mode!!) {
            MemberReferenceTree.ReferenceMode.NEW -> "new"
            MemberReferenceTree.ReferenceMode.INVOKE -> node.name.toString()
        }
        val reference = Tr.Ident.build(referenceName, null, format(sourceBefore(referenceName)))

        return Tr.MemberReference(expr, reference, node.type(), fmt)
    }

    override fun visitMemberSelect(node: MemberSelectTree, fmt: Formatting.Reified): Tree {
        val target = (node as JCTree.JCFieldAccess).selected.convert<Expression> { sourceBefore(".") }
        val name = Tr.Ident.build(node.name.toString(), null, format(sourceBefore(node.name.toString())))
        return Tr.FieldAccess(target, name, node.type(), fmt)
    }

    override fun visitMethodInvocation(node: MethodInvocationTree, fmt: Formatting.Reified): Tree {
        val jcSelect = (node as JCTree.JCMethodInvocation).methodSelect

        val select = when(jcSelect) {
            is JCTree.JCFieldAccess -> jcSelect.selected.convert<Expression> { sourceBefore(".") }
            is JCTree.JCIdent -> null
            else -> error("Unexpected method select type ${jcSelect::class.java}")
        }

        // generic type parameters can only exist on qualified targets
        val typeParams = if(node.typeargs.isNotEmpty()) {
            val genericPrefix = sourceBefore("<")
            val genericParams = node.typeargs.convertAll<NameTree>(COMMA_DELIM, { sourceBefore(">") })
            Tr.MethodInvocation.TypeParameters(genericParams, format(genericPrefix))
        } else null

        val name = when(jcSelect) {
            is JCTree.JCFieldAccess ->  Tr.Ident.build(jcSelect.name.toString(), null, format(sourceBefore(jcSelect.name.toString())))
            is JCTree.JCIdent -> jcSelect.convert<Tr.Ident>()
            else -> error("Unexpected method select type ${jcSelect::class.java}")
        }

        val argsPrefix = sourceBefore("(")
        val args = Tr.MethodInvocation.Arguments(
                if(node.args.isEmpty()) {
                    listOf(Tr.Empty(format(sourceBefore(")"))))
                } else {
                    node.args.convertAll<Expression>(COMMA_DELIM, { sourceBefore(")") })
                },
                format(argsPrefix))

        val genericSymbol = when (jcSelect) {
            null -> null
            is JCTree.JCIdent -> jcSelect.sym
            is JCTree.JCFieldAccess -> jcSelect.sym
            else -> throw IllegalArgumentException("Unexpected method select type $this")
        }.let {
            // if the symbol is not a method symbol, there is a parser error in play
            if(it is Symbol.MethodSymbol) it else null
        }

        val type = if(genericSymbol != null && jcSelect.type != null) {
            fun signature(t: com.sun.tools.javac.code.Type): Type.Method.Signature? = when(t) {
                is com.sun.tools.javac.code.Type.MethodType ->
                    Type.Method.Signature(type(t.restype), t.argtypes.map { type(it) }.filterNotNull())
                else -> null
            }

            val genericSignature = when (genericSymbol.type) {
                is com.sun.tools.javac.code.Type.ForAll ->
                    signature((genericSymbol.type as com.sun.tools.javac.code.Type.ForAll).qtype)
                else -> signature(genericSymbol.type)
            }

            Type.Method.build(
                    type(genericSymbol.owner).asClass()!!,
                    name.simpleName,
                    genericSignature,
                    signature(jcSelect.type),
                    genericSymbol.params().map { it.name.toString() },
                    genericSymbol.filteredFlags())
        } else null

        return Tr.MethodInvocation(select, typeParams, name, args, type, fmt)
    }

    override fun visitMethod(node: MethodTree, fmt: Formatting.Reified): Tree {
        logger.trace("Visiting method {}", node.name)

        val annotations = node.modifiers.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)

        val modifiers = node.modifiers.sortedFlags()
                .map { mod ->
                    val modFormat = format(whitespace())
                    cursor += mod.name.length
                    when(mod) {
                        Modifier.DEFAULT -> Tr.Modifier.Default(modFormat)
                        Modifier.PUBLIC -> Tr.Modifier.Public(modFormat)
                        Modifier.PROTECTED -> Tr.Modifier.Protected(modFormat)
                        Modifier.PRIVATE -> Tr.Modifier.Private(modFormat)
                        Modifier.ABSTRACT -> Tr.Modifier.Abstract(modFormat)
                        Modifier.STATIC -> Tr.Modifier.Static(modFormat)
                        Modifier.FINAL -> Tr.Modifier.Final(modFormat)
                        Modifier.NATIVE -> Tr.Modifier.Native(modFormat)
                        Modifier.SYNCHRONIZED -> Tr.Modifier.Synchronized(modFormat)
                        else -> throw IllegalArgumentException("Unexpected modifier $mod")
                    }
                }

        // see https://docs.oracle.com/javase/tutorial/java/generics/methods.html
        val typeParams = if(node.typeParameters.isNotEmpty()) {
            val genericPrefix = sourceBefore("<")
            Tr.TypeParameters(node.typeParameters.convertAll(COMMA_DELIM, { sourceBefore(">") }),
                    format(genericPrefix))
        } else null

        val returnType = node.returnType.convertOrNull<TypeTree>()

        val name = if(node.name.toString() == "<init>") {
            val owner = when((node as JCTree.JCMethodDecl).sym) {
                null -> currentPath.filterIsInstance<JCTree.JCClassDecl>().first().simpleName.toString()
                else -> (node.sym.owner as Symbol.ClassSymbol).name.toString()
            }
            Tr.Ident.build(owner, null, format(sourceBefore(owner)))
        } else {
            Tr.Ident.build(node.name.toString(), null, format(sourceBefore(node.name.toString())))
        }

        val paramFmt = format(sourceBefore("("))
        val params = if(node.parameters.isNotEmpty()) {
            Tr.MethodDecl.Parameters(node.parameters.convertAll<Tr.VariableDecls>(COMMA_DELIM, { sourceBefore(")") }), paramFmt)
        } else {
            Tr.MethodDecl.Parameters(listOf(Tr.Empty(format(sourceBefore(")")))), paramFmt)
        }

        val throws = if(node.throws.isNotEmpty()) {
            val throwsPrefix = sourceBefore("throws")
            Tr.MethodDecl.Throws(node.throws.convertAll<NameTree>(COMMA_DELIM, NO_DELIM), format(throwsPrefix))
        } else null

        val body = node.body.convertOrNull<Tr.Block<Statement>>()

        val defaultValue = if(node.defaultValue != null) {
            val defaultFmt = format(sourceBefore("default"))
            Tr.MethodDecl.Default(node.defaultValue.convert<Expression>(), defaultFmt)
        } else null

        return Tr.MethodDecl(annotations, modifiers, typeParams, returnType, name, params, throws, body, defaultValue, fmt)
    }

    override fun visitNewArray(node: NewArrayTree, fmt: Formatting.Reified): Tree {
        skip("new")

        val jcVarType = (node as JCTree.JCNewArray).elemtype
        val typeExpr = when(jcVarType) {
            is JCTree.JCArrayTypeTree -> {
                // we'll capture the array dimensions in a bit, just convert the element type
                var elementType = jcVarType.elemtype
                while(elementType is JCTree.JCArrayTypeTree) {
                    elementType = elementType.elemtype
                }
                elementType.convertOrNull<TypeTree>()
            }
            else -> jcVarType.convertOrNull<TypeTree>()
        }

        val dimensions = if(node.dimensions.isNotEmpty()) {
            node.dimensions.mapIndexed { i, dim ->
                val dimensionPrefix = sourceBefore("[")
                Tr.NewArray.Dimension(dim.convert { sourceBefore("]") }, format(dimensionPrefix,
                        if(i == node.dimensions.size - 1 && node.initializers != null) sourceBefore("}") else ""))
            }.toMutableList()
        } else {
            mutableListOf()
        }

        val matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)\\]").matcher(source)
        while(matcher.find(cursor)) {
            cursor(matcher.end())
            val ws = Tr.Empty(format(matcher.group(2)))
            dimensions.add(Tr.NewArray.Dimension(ws, format(matcher.group(1))))
        }

        val initializer = if(node.initializers != null) {
            val initPrefix = sourceBefore("{")
            val initializers = if(node.initializers.isEmpty()) {
                listOf(Tr.Empty(format(sourceBefore("}"))))
            } else {
                node.initializers.convertAll<Expression>(COMMA_DELIM, { sourceBefore("}") })
            }
            Tr.NewArray.Initializer(initializers, format(initPrefix))
        } else null

        return Tr.NewArray(typeExpr, dimensions, initializer, node.type(), fmt)
    }

    override fun visitNewClass(node: NewClassTree, fmt: Formatting.Reified): Tree {
        skip("new")
        val clazz = node.identifier.convert<TypeTree>()

        val argPrefix = sourceBefore("(")
        val args = Tr.NewClass.Arguments(
                if(node.arguments.isEmpty()) {
                    listOf(Tr.Empty(format(sourceBefore(")"))))
                } else {
                    node.arguments.convertAll<Expression>(COMMA_DELIM, { sourceBefore(")") })
                },
                format(argPrefix))

        val body = node.classBody?.let {
            val bodyPrefix = sourceBefore("{")

            val members = it.members
                    // we don't care about the compiler-inserted default constructor,
                    // since it will never be subject to refactoring
                    .filter { it !is JCTree.JCMethodDecl || it.modifiers.flags and Flags.GENERATEDCONSTR == 0L }
                    .convertAll<Tree>(NO_DELIM, NO_DELIM)

            Tr.Block(null, members, format(bodyPrefix), sourceBefore("}"))
        }

        return Tr.NewClass(clazz, args, body, type((node as JCTree.JCNewClass).type), fmt)
    }

    override fun visitParameterizedType(node: ParameterizedTypeTree, fmt: Formatting.Reified): Tree {
        val clazz = node.type.convert<NameTree>()

        val typeArgPrefix = sourceBefore("<")
        val typeArgs = if(node.typeArguments.isEmpty()) {
            // raw type, see http://docs.oracle.com/javase/tutorial/java/generics/rawTypes.html
            listOf(Tr.Empty(format(sourceBefore(">"))))
        } else {
            node.typeArguments.convertAll<Expression>(COMMA_DELIM, { sourceBefore(">") })
        }

        return Tr.ParameterizedType(
                clazz,
                Tr.ParameterizedType.TypeArguments(typeArgs, format(typeArgPrefix)),
                fmt
        )
    }

    override fun visitParenthesized(node: ParenthesizedTree, fmt: Formatting.Reified): Tree {
        skip("(")
        return Tr.Parentheses<Expression>(node.expression.convert { sourceBefore(")") }, fmt)
    }

    override fun visitPrimitiveType(node: PrimitiveTypeTree, fmt: Formatting.Reified): Tree {
        cursor(node.endPos())
        return Tr.Primitive(when (node.primitiveTypeKind) {
            TypeKind.BOOLEAN -> Type.Primitive.Boolean
            TypeKind.BYTE -> Type.Primitive.Byte
            TypeKind.CHAR -> Type.Primitive.Char
            TypeKind.DOUBLE -> Type.Primitive.Double
            TypeKind.FLOAT -> Type.Primitive.Float
            TypeKind.INT -> Type.Primitive.Int
            TypeKind.LONG -> Type.Primitive.Long
            TypeKind.SHORT -> Type.Primitive.Short
            TypeKind.VOID -> Type.Primitive.Void
            else -> throw IllegalArgumentException("Unknown primitive type $this")
        }, fmt)
    }

    override fun visitReturn(node: ReturnTree, fmt: Formatting.Reified): Tree {
        skip("return")
        return Tr.Return(node.expression.convertOrNull(), fmt)
    }

    override fun visitSwitch(node: SwitchTree, fmt: Formatting.Reified): Tree {
        skip("switch")
        val selector = node.expression.convert<Tr.Parentheses<Expression>>()

        val casePrefix = sourceBefore("{")
        val cases = node.cases.convertAll<Tr.Case>(NO_DELIM, NO_DELIM)

        return Tr.Switch(selector, Tr.Block(null, cases, format(casePrefix), sourceBefore("}")), fmt)
    }

    override fun visitSynchronized(node: SynchronizedTree, fmt: Formatting.Reified): Tree {
        skip("synchronized")
        return Tr.Synchronized(
                node.expression.convert(),
                node.block.convert(),
                fmt
        )
    }

    override fun visitThrow(node: ThrowTree, fmt: Formatting.Reified): Tree {
        skip("throw")
        return Tr.Throw(node.expression.convert(), fmt)
    }

    override fun visitTry(node: TryTree, fmt: Formatting.Reified): Tree {
        skip("try")
        val resources = if(node.resources.isNotEmpty()) {
            val resourcesPrefix = sourceBefore("(")
            val decls = node.resources.convertAll<Tr.VariableDecls>(SEMI_DELIM, { sourceBefore(")") })
            Tr.Try.Resources(decls, format(resourcesPrefix))
        } else null

        val block = node.block.convert<Tr.Block<Statement>>()
        val catches = node.catches.convertAll<Tr.Catch>(NO_DELIM, NO_DELIM)

        val finally = if(node.finallyBlock != null) {
            val finallyPrefix = sourceBefore("finally")
            Tr.Try.Finally(node.finallyBlock.convert<Tr.Block<Statement>>(),
                    format(finallyPrefix))
        } else null

        return Tr.Try(resources, block, catches, finally, fmt)
    }

    override fun visitTypeCast(node: TypeCastTree, fmt: Formatting.Reified): Tree {
        val clazzPrefix = sourceBefore("(")
        val clazz = Tr.Parentheses(node.type.convert<TypeTree> { sourceBefore(")") },
                format(clazzPrefix))

        return Tr.TypeCast(clazz, node.expression.convert(), fmt)
    }

    override fun visitTypeParameter(node: TypeParameterTree, fmt: Formatting.Reified): Tree {
        val annotations = node.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)

        val name = TreeBuilder.buildName(node.name.toString(), format(sourceBefore(node.name.toString())))

        val bounds = if(node.bounds.isNotEmpty()) {
            val boundPrefix = if(node.bounds.isNotEmpty()) sourceBefore("extends") else ""
            // see https://docs.oracle.com/javase/tutorial/java/generics/bounded.html
            Tr.TypeParameter.Bounds(node.bounds.convertAll<TypeTree>({ sourceBefore("&") }, NO_DELIM),
                    format(boundPrefix))
        } else null

        return Tr.TypeParameter(annotations, name, bounds, fmt)
    }

    override fun visitUnionType(node: UnionTypeTree, fmt: Formatting.Reified): Tree {
        return Tr.MultiCatch(node.typeAlternatives.convertAll({ sourceBefore("|") }, NO_DELIM), fmt)
    }

    override fun visitUnary(node: UnaryTree, fmt: Formatting.Reified): Tree {
        val (op: Tr.Unary.Operator, expr: Expression) = when((node as JCTree.JCUnary).tag) {
            JCTree.Tag.POS -> {
                skip("+")
                Tr.Unary.Operator.Positive() to node.arg.convert<Expression>()
            }
            JCTree.Tag.NEG -> {
                skip("-")
                Tr.Unary.Operator.Negative() to node.arg.convert<Expression>()
            }
            JCTree.Tag.PREDEC -> {
                skip("--")
                Tr.Unary.Operator.PreDecrement() to node.arg.convert<Expression>()
            }
            JCTree.Tag.PREINC -> {
                skip("++")
                Tr.Unary.Operator.PreIncrement() to node.arg.convert<Expression>()
            }
            JCTree.Tag.POSTDEC -> {
                val expr = node.arg.convert<Expression>()
                Tr.Unary.Operator.PostDecrement(format(sourceBefore("--"))) to expr
            }
            JCTree.Tag.POSTINC -> {
                val expr = node.arg.convert<Expression>()
                Tr.Unary.Operator.PostIncrement(format(sourceBefore("++"))) to expr
            }
            JCTree.Tag.COMPL -> {
                skip("~")
                Tr.Unary.Operator.Complement(Formatting.Empty) to node.arg.convert<Expression>()
            }
            JCTree.Tag.NOT -> {
                skip("!")
                Tr.Unary.Operator.Not(Formatting.Empty) to node.arg.convert<Expression>()
            }
            else -> throw IllegalArgumentException("Unexpected unary tag ${node.tag}")
        }

        return Tr.Unary(op, expr, node.type(), fmt)
    }

    override fun visitVariable(node: VariableTree, fmt: Formatting.Reified): Tree {
        return if(node.modifiers.hasFlag(Flags.ENUM)) {
            visitEnumVariable(node, fmt)
        } else {
            visitVariables(listOf(node), fmt) // method arguments cannot be multi-declarations
        }
    }

    fun visitVariables(nodes: List<VariableTree>, fmt: Formatting.Reified): Tr.VariableDecls {
        val node = nodes[0] as JCTree.JCVariableDecl
        val annotations = node.modifiers.annotations.convertAll<Tr.Annotation>(NO_DELIM, NO_DELIM)

        val vartype = node.vartype

        val modifiers = if((node.modifiers as JCTree.JCModifiers).pos >= 0) {
            node.modifiers.sortedFlags().map { mod ->
                val modFormat = format(whitespace())
                cursor += mod.name.length
                when (mod) {
                    Modifier.PUBLIC -> Tr.Modifier.Public(modFormat)
                    Modifier.PROTECTED -> Tr.Modifier.Protected(modFormat)
                    Modifier.PRIVATE -> Tr.Modifier.Private(modFormat)
                    Modifier.ABSTRACT -> Tr.Modifier.Abstract(modFormat)
                    Modifier.STATIC -> Tr.Modifier.Static(modFormat)
                    Modifier.FINAL -> Tr.Modifier.Final(modFormat)
                    Modifier.TRANSIENT -> Tr.Modifier.Transient(modFormat)
                    Modifier.VOLATILE -> Tr.Modifier.Volatile(modFormat)
                    else -> throw IllegalArgumentException("Unexpected modifier $mod")
                }
            }
        } else {
            emptyList() // these are implicit modifiers, like "final" on try-with-resources variable declarations
        }

        val typeExpr = if(vartype == null || vartype.endPos() < 0) {
            null // this is a lambda parameter with an inferred type expression
        }
        else {
            when (vartype) {
                is JCTree.JCArrayTypeTree -> {
                    // we'll capture the array dimensions in a bit, just convert the element type
                    var elementType = vartype.elemtype
                    while (elementType is JCTree.JCArrayTypeTree) {
                        elementType = elementType.elemtype
                    }
                    elementType.convert<TypeTree>()
                }
                else -> vartype.convert<TypeTree>()
            }
        }

        fun dimensions(): List<Tr.VariableDecls.Dimension> {
            val matcher = Pattern.compile("\\G(\\s*)\\[(\\s*)\\]").matcher(source)
            val dimensions = ArrayList<Tr.VariableDecls.Dimension>()
            while(matcher.find(cursor)) {
                cursor(matcher.end())
                val ws = Tr.Empty(format(matcher.group(2)))
                dimensions.add(Tr.VariableDecls.Dimension(ws, format(matcher.group(1))))
            }
            return dimensions
        }

        val beforeDimensions = dimensions()

        val vartypeString = if(typeExpr != null) source.substring(vartype.startPosition, vartype.endPos()) else ""
        val varargMatcher = Pattern.compile("(\\s*)\\.{3}").matcher(vartypeString)
        val varargs = if(varargMatcher.find()) {
            skipPattern("(\\s*)\\.{3}")
            Tr.VariableDecls.Varargs(format(varargMatcher.group(1)))
        } else null

        val vars = nodes.mapIndexed { i, n ->
            val namedVarPrefix = sourceBefore(n.name.toString())
            val name = Tr.Ident.build(n.name.toString(), node.type(),
                    format("", if ((n as JCTree.JCVariableDecl).init is JCTree.JCExpression) sourceBefore("=") else ""))
            Tr.VariableDecls.NamedVar(
                    name,
                    dimensions(),
                    n.init.convertOrNull(),
                    n.type(),
                    if(i == nodes.size - 1) format(namedVarPrefix) else format(namedVarPrefix, sourceBefore(","))
            )
        }

        return Tr.VariableDecls(annotations, modifiers, typeExpr, varargs, beforeDimensions, vars, fmt)
    }

    override fun visitWhileLoop(node: WhileLoopTree, fmt: Formatting.Reified): Tree {
        skip("while")
        return Tr.WhileLoop(
                node.condition.convert(),
                node.statement.convert(statementDelim),
                fmt
        )
    }

    override fun visitWildcard(node: WildcardTree, fmt: Formatting.Reified): Tree {
        skip("?")

        val bound = when((node as JCTree.JCWildcard).kind.kind!!) {
            BoundKind.EXTENDS -> Tr.Wildcard.Bound.Extends(format(sourceBefore("extends")))
            BoundKind.SUPER -> Tr.Wildcard.Bound.Super(format(sourceBefore("super")))
            BoundKind.UNBOUND -> null
        }

        return Tr.Wildcard(bound, node.inner.convertOrNull<NameTree>(), fmt)
    }

    /**
     * --------------
     * Conversion utilities
     * --------------
     */

    @Suppress("UNCHECKED_CAST")
    private fun <T : Tree> JdkTree.convert(suffix: (JdkTree) -> String = { "" }): T {
        try {
            val prefix = source.substring(cursor, Math.max((this as JCTree).startPosition, cursor))
            cursor += prefix.length
            var t = scan(this, format(prefix)) as T
            t = t.changeFormatting<T>(t.formatting.withSuffix(suffix(this)))
            cursor(Math.max(this.endPos(), cursor)) // if there is a non-empty suffix, the cursor may have already moved past it
            return t
        } catch(t: Throwable) {
            // this SHOULD never happen, but is here simply as a diagnostic measure in the event of unexpected exceptions
            logger.error("Failed to convert ${this::class.java.simpleName} for the following cursor stack:")
            logCurrentPathAsError()
            throw t
        }
    }

    private fun logCurrentPathAsError() {
        logger.error("--- BEGIN PATH ---")
        currentPath.reversed().forEach {
            val lineNumber by lazy { source.substring(0, (it as JCTree).startPosition).count { it == '\n' } + 1 }
            logger.error(when(it) {
                is JCTree.JCCompilationUnit -> "JCCompilationUnit(sourceFile = ${it.sourcefile.name})"
                is JCTree.JCClassDecl -> "JCClassDecl(name = ${it.name})"
                is JCTree.JCMethodDecl -> "JCMethodDecl(name = ${it.name}, line = $lineNumber)"
                is JCTree.JCVariableDecl -> "JCVariableDecl(name = ${it.name}, line = $lineNumber)"
                else -> "${it::class.java.simpleName}(line = $lineNumber)"
            })
        }
        logger.error("--- END PATH ---")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Tree> JdkTree.convertOrNull(suffix: (JdkTree) -> String = { "" }): T? =
            if (this is JdkTree) convert<T>(suffix) else null

    private fun <T: Tree> List<JdkTree>.convertAll(innerSuffix: (JdkTree) -> String, suffix: (JdkTree) -> String): List<T> =
            mapIndexed { i, tree -> tree.convert<T>(if (i == size - 1) suffix else innerSuffix) }

    val statementDelim = { t: JdkTree ->
        sourceBefore(when (t) {
            is JCTree.JCThrow, is JCTree.JCBreak, is JCTree.JCAssert, is JCTree.JCContinue -> ";"
            is JCTree.JCExpressionStatement, is JCTree.JCReturn, is JCTree.JCVariableDecl -> ";"
            is JCTree.JCDoWhileLoop, is JCTree.JCSkip -> ";"
            is JCTree.JCCase -> ":"
            is JCTree.JCMethodDecl -> if (t.body == null) ";" else ""
            else -> ""
        })
    }

    private fun List<JdkTree>?.convertPossibleMultiVariable(): List<Tree> {
        if(this == null)
            return emptyList()

        val groups = this.groupBy {
            // group multi-variable declarations together, other types of members will never have the same starting position
            (it as JCTree).startPosition
        }.values

        return groups.map { treeGroup ->
            if(treeGroup.size == 1) {
                treeGroup[0].convert<Tree>(statementDelim)
            } else {
                // multi-variable declarations are split into independent overlapping JCVariableDecl's by the Oracle AST
                val prefix = source.substring(cursor, Math.max((treeGroup[0] as JCTree).startPosition, cursor))
                cursor += prefix.length
                @Suppress("UNCHECKED_CAST") var vars = visitVariables(treeGroup as List<VariableTree>, format(prefix))
                vars = vars.copy(formatting = vars.formatting.withSuffix(SEMI_DELIM(treeGroup.last())))
                cursor(Math.max(treeGroup.last().endPos(), cursor))
                vars
            }
        }
    }

    /**
     * --------------
     * Type conversion
     * --------------
     */

    private val flagMasks = mapOf(
        1L to Flag.Public,
        1L shl 1 to Flag.Private,
        1L shl 2 to Flag.Protected,
        1L shl 3 to Flag.Static,
        1L shl 4 to Flag.Final,
        1L shl 5 to Flag.Synchronized,
        1L shl 6 to Flag.Volatile,
        1L shl 7 to Flag.Transient,
        1L shl 10 to Flag.Abstract
    )

    private fun Symbol.filteredFlags(): List<Flag> =
            flagMasks.filter { flags() and it.key != 0L }.map { it.value }

    private fun type(symbol: Symbol?): Type? {
        return when (symbol) {
            is Symbol.ClassSymbol -> type(symbol.type)
            is Symbol.VarSymbol -> Type.GenericTypeVariable(symbol.name.toString(), null)
            is Symbol.TypeVariableSymbol -> type(symbol.type)
            else -> null
        }
    }

    private fun type(type: com.sun.tools.javac.code.Type?, stack: List<Symbol?> = emptyList(), shallow: Boolean = false): Type? {
        return when (type) {
            is com.sun.tools.javac.code.Type.ClassType -> {
                val sym = type.tsym as Symbol.ClassSymbol

                if (stack.contains(sym))
                    Type.Cyclic(sym.className())
                else {
                    if(shallow) {
                        Type.ShallowClass(sym.className())
                    } else {
                        val fields = (sym.members_field?.elements ?: emptyList())
                                .filterIsInstance<Symbol.VarSymbol>()
                                .map {
                                    Type.Var(
                                            it.name.toString(),
                                            type(it.type, stack.plus(sym)),
                                            it.filteredFlags()
                                    )
                                }

                        val symType = sym.type as com.sun.tools.javac.code.Type.ClassType
                        Type.Class.build(sym.className(), fields, type(type.supertype_field, stack.plus(sym)).asClass(),
                                type.typarams_field?.mapNotNull { tParam -> type(tParam, stack.plus(sym), shallow = true) } ?: emptyList(),
                                symType.interfaces_field?.mapNotNull { i -> type(i, stack.plus(sym), shallow = true) } ?: emptyList())
                    }
                }
            }
            is com.sun.tools.javac.code.Type.TypeVar -> Type.GenericTypeVariable(type.tsym.name.toString(), type(type.bound, stack).asClass())
            is com.sun.tools.javac.code.Type.JCPrimitiveType -> type.tag.primitive()
            is com.sun.tools.javac.code.Type.ArrayType -> Type.Array(type(type.elemtype, stack)!!)
            com.sun.tools.javac.code.Type.noType -> null
            else -> null
        }
    }

    private fun JdkTree.type(): Type? = type((this as JCTree).type)

    private fun TypeTag.primitive(): Type.Primitive {
        return when (this) {
            TypeTag.BOOLEAN -> Type.Primitive.Boolean
            TypeTag.BYTE -> Type.Primitive.Byte
            TypeTag.CHAR -> Type.Primitive.Char
            TypeTag.DOUBLE -> Type.Primitive.Double
            TypeTag.FLOAT -> Type.Primitive.Float
            TypeTag.INT -> Type.Primitive.Int
            TypeTag.LONG -> Type.Primitive.Long
            TypeTag.SHORT -> Type.Primitive.Short
            TypeTag.VOID -> Type.Primitive.Void
            TypeTag.NONE -> Type.Primitive.None
            TypeTag.CLASS -> Type.Primitive.String
            TypeTag.BOT -> Type.Primitive.Null
            else -> throw IllegalArgumentException("Unknown type tag $this")
        }
    }

    /**
     * --------------
     * Other convenience utilities
     * --------------
     */

    private fun JdkTree.endPos(): Int = (this as JCTree).getEndPosition(endPosTable)

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private fun sourceBefore(untilDelim: String, stop: Char? = null): String {
        val delimIndex = positionOfNext(untilDelim, stop)
        if(delimIndex < 0) {
            return "" // unable to find this delimiter
        }

        val prefix = source.substring(cursor, delimIndex)
        cursor += prefix.length + untilDelim.length // advance past the delimiter
        return prefix
    }

    private fun positionOfNext(untilDelim: String, stop: Char? = null): Int {
        var delimIndex = cursor
        var inMultiLineComment = false
        var inSingleLineComment = false
        while(delimIndex < source.length - untilDelim.length + 1) {
            if(inSingleLineComment && source[delimIndex] == '\n') {
                inSingleLineComment = false
            }
            else {
                if(source.length - untilDelim.length > delimIndex + 1) {
                    when(source.substring(delimIndex, delimIndex + 2)) {
                        "//" -> { inSingleLineComment = true; delimIndex++ }
                        "/*" -> { inMultiLineComment = true; delimIndex++ }
                        "*/" -> { inMultiLineComment = false; delimIndex++ }
                    }
                }

                if(!inMultiLineComment && !inSingleLineComment) {
                    if(source[delimIndex] == stop)
                        return -1 // reached stop word before finding the delimiter

                    if(source.substring(delimIndex, delimIndex + untilDelim.length) == untilDelim)
                        break // found it!
                }
            }
            delimIndex++
        }

        return if(delimIndex > source.length - untilDelim.length) -1 else delimIndex
    }

    private val SEMI_DELIM = { ignored: JdkTree -> sourceBefore(";") }
    private val COMMA_DELIM = { ignored: JdkTree -> sourceBefore(",") }
    private val NO_DELIM = { ignored: JdkTree -> "" }

    @Suppress("UNUSED_PARAMETER")
    private fun whitespace(t: JdkTree? = null): String {
        var delimIndex = cursor
        var inMultiLineComment = false
        var inSingleLineComment = false
        loop@while(delimIndex < source.length) {
            if(inSingleLineComment && source[delimIndex] == '\n') {
                inSingleLineComment = false
            }
            else {
                if(source.length > delimIndex + 1) {
                    when(source.substring(delimIndex, delimIndex + 2)) {
                        "//" -> {
                            inSingleLineComment = true
                            delimIndex += 2
                            continue@loop
                        }
                        "/*" -> {
                            inMultiLineComment = true
                            delimIndex += 2
                            continue@loop
                        }
                        "*/" -> {
                            inMultiLineComment = false
                            delimIndex += 2
                            continue@loop
                        }
                    }
                }

                if(!inMultiLineComment && !inSingleLineComment) {
                    if(!source.substring(delimIndex, delimIndex + 1)[0].isWhitespace())
                        break // found it!
                }
            }
            delimIndex++
        }

        val prefix = source.substring(cursor, delimIndex)
        cursor += prefix.length
        return prefix
    }

    private fun skip(token: String?): String? {
        if(token == null)
            return null
        if(source.substring(cursor, cursor + token.length) == token)
            cursor += token.length
        return token
    }

    private fun skipPattern(pattern: String) {
        val matcher = Pattern.compile("\\G$pattern").matcher(source)
        if(matcher.find(cursor)) {
            cursor(matcher.end())
        }
    }

    // Only exists as a function to make it easier to debug unexpected cursor shifts
    private fun cursor(n: Int) {
        cursor = n
    }

    private fun ModifiersTree.hasFlag(flag: Number): Boolean =
            (this as JCTree.JCModifiers).flags and flag.toLong() != 0L

    /**
     * Because Flags.asModifierSet() only matches on certain flags... (debugging utility only)
     */
    @Suppress("unused")
    private fun ModifiersTree.listFlags(): List<String> = (this as JCTree.JCModifiers).flags.listFlags()

    private fun Number.listFlags(): List<String> {
        val allFlags = Flags::class.java.declaredFields
                .filter {
                    it.isAccessible = true
                    it.get(null) is Number && it.name.matches("[A-Z_]+".toRegex())
                }
                .map { it.name to it.get(null) as Number }

        return allFlags.fold(emptyList<String>()) { all, f ->
            if(f.second.toLong() and this.toLong() != 0L)
                all + f.first
            else all
        }
    }

    /**
     * Modifiers in the order they appear in the source, which is not necessarily the same as the order in
     * which they appear in the Oracle AST
     */
    private fun ModifiersTree.sortedFlags(): List<Modifier> {
        if(flags.isEmpty())
            return emptyList()

        val modifiers = mutableListOf<Modifier>()

        var i = cursor
        var inComment = false
        var word = ""
        while(i < source.length) {
            val c = source[i]
            if(c == '/' && source.length > i + 1 && source[i + 1] == '*') {
                inComment = true
            }

            if(inComment && c == '/' && source[i - 1] == '*') {
                inComment = false
            }
            else if(!inComment) {
                if(c.isWhitespace()) {
                    if(word.isNotEmpty()) {
                        this.flags.find { it.name.toLowerCase() == word }?.let { modifiers.add(it); word = "" } ?:
                                break // this is the first non-modifier word we have encountered
                    }
                }
                else {
                    word += c
                }
            }
            i++
        }

        return modifiers
    }
}
