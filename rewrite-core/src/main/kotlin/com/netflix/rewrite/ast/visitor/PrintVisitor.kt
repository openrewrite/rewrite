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
package com.netflix.rewrite.ast.visitor

import com.netflix.rewrite.ast.*

class PrintVisitor : AstVisitor<String>("") {
    override fun reduce(r1: String, r2: String): String = r1 + r2

    fun visit(nodes: Collection<Tree>, suffixBetween: String, suffixEnd: String = ""): String {
        return nodes.foldIndexed("") { i, s, node ->
            reduce(s, visit(node) + (if(i == nodes.size - 1) suffixEnd else suffixBetween))
        }
    }

    fun visitStatements(statements: Collection<Tree>): String =
        statements.fold("") { s, stat -> s + visitStatement(stat) }

    fun visitStatement(statement: Tree): String {
        return visit(statement) + when(statement) {
            is Tr.Assign, is Tr.AssignOp, is Tr.Break, is Tr.Continue, is Tr.MethodInvocation -> ";"
            is Tr.NewClass, is Tr.Return, is Tr.Throw, is Tr.Unary, is Tr.VariableDecls -> ";"
            is Tr.DoWhileLoop, is Tr.Empty, is Tr.Assert -> ";"
            is Tr.MethodDecl -> if(statement.body == null) ";" else ""
            else -> ""
        }
    }

    fun visit(t: Tree?, suffix: String): String {
        return if(t is Tree)
            visit(t) + suffix
        else ""
    }

    override fun visitAnnotation(annotation: Tr.Annotation): String {
        val args = annotation.args?.let {
            it.fmt("(${visit(it.args, ",")})")
        } ?: ""
        return annotation.fmt("@${visit(annotation.annotationType)}$args")
    }

    override fun visitArrayAccess(arrayAccess: Tr.ArrayAccess): String {
        val dimension = arrayAccess.dimension.fmt("[${visit(arrayAccess.dimension.index)}]")
        return arrayAccess.fmt("${visit(arrayAccess.indexed)}$dimension")
    }

    override fun visitArrayType(arrayType: Tr.ArrayType): String {
        val dim = arrayType.dimensions.fold("") { s, d -> s + d.fmt("[${visit(d.inner)}]") }
        return arrayType.fmt("${visit(arrayType.elementType)}$dim")
    }

    override fun visitAssert(assert: Tr.Assert): String {
        return assert.fmt("assert${visit(assert.condition)}")
    }

    override fun visitAssign(assign: Tr.Assign): String {
        return assign.fmt("${visit(assign.variable)}=${visit(assign.assignment)}")
    }

    override fun visitAssignOp(assign: Tr.AssignOp): String {
        val keyword = when(assign.operator) {
            is Tr.AssignOp.Operator.Addition -> "+="
            is Tr.AssignOp.Operator.Subtraction -> "-="
            is Tr.AssignOp.Operator.Multiplication -> "*="
            is Tr.AssignOp.Operator.Division -> "/="
            is Tr.AssignOp.Operator.Modulo -> "%="
            is Tr.AssignOp.Operator.BitAnd -> "&="
            is Tr.AssignOp.Operator.BitOr -> "|="
            is Tr.AssignOp.Operator.BitXor -> "^="
            is Tr.AssignOp.Operator.LeftShift -> "<<="
            is Tr.AssignOp.Operator.RightShift -> ">>="
            is Tr.AssignOp.Operator.UnsignedRightShift -> ">>>="
        }

        return assign.fmt("${visit(assign.variable)}${assign.operator.fmt(keyword)}${visit(assign.assignment)}")
    }

    override fun visitBinary(binary: Tr.Binary): String {
        val keyword = when(binary.operator) {
            is Tr.Binary.Operator.Addition -> "+"
            is Tr.Binary.Operator.Subtraction -> "-"
            is Tr.Binary.Operator.Multiplication -> "*"
            is Tr.Binary.Operator.Division -> "/"
            is Tr.Binary.Operator.Modulo -> "%"
            is Tr.Binary.Operator.LessThan -> "<"
            is Tr.Binary.Operator.GreaterThan -> ">"
            is Tr.Binary.Operator.LessThanOrEqual -> "<="
            is Tr.Binary.Operator.GreaterThanOrEqual -> ">="
            is Tr.Binary.Operator.Equal -> "=="
            is Tr.Binary.Operator.NotEqual -> "!="
            is Tr.Binary.Operator.BitAnd -> "&"
            is Tr.Binary.Operator.BitOr -> "|"
            is Tr.Binary.Operator.BitXor -> "^"
            is Tr.Binary.Operator.LeftShift -> "<<"
            is Tr.Binary.Operator.RightShift -> ">>"
            is Tr.Binary.Operator.UnsignedRightShift -> ">>>"
            is Tr.Binary.Operator.Or -> "||"
            is Tr.Binary.Operator.And -> "&&"
        }

        return binary.fmt("${visit(binary.left)}${binary.operator.fmt(keyword)}${visit(binary.right)}")
    }

    override fun visitBlock(block: Tr.Block<Tree>): String {
        val static = block.static.fmt("static")
        return block.fmt("$static{${visitStatements(block.statements)}${block.endOfBlockSuffix}}")
    }

    override fun visitBreak(breakStatement: Tr.Break): String {
        return breakStatement.fmt("break${visit(breakStatement.label)}")
    }

    override fun visitCase(case: Tr.Case): String {
        return case.fmt("${visit(case.pattern)}:${visitStatements(case.statements)}")
    }

    override fun visitCatch(catch: Tr.Catch): String {
        return catch.fmt("catch${visit(catch.param)}${visit(catch.body)}")
    }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): String {
        val typeParams = classDecl.typeParams?.let { it.fmt("<${visit(it.params, ",")}>") } ?: ""
        val modifiers = visitModifiers(classDecl.modifiers)

        val kind = classDecl.kind.fmt(when(classDecl.kind) {
            is Tr.ClassDecl.Kind.Class -> "class"
            is Tr.ClassDecl.Kind.Enum -> "enum"
            is Tr.ClassDecl.Kind.Interface -> "interface"
            is Tr.ClassDecl.Kind.Annotation -> "@interface"
        })

        return classDecl.fmt("${visit(classDecl.annotations)}$modifiers$kind${visit(classDecl.name)}$typeParams" +
                "${visit(classDecl.extends)}${visit(classDecl.implements, ",")}${visit(classDecl.body)}")
    }

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): String {
        return cu.fmt("${visit(cu.packageDecl, ";")}${visit(cu.imports, ";", ";")}${visit(cu.classes)}")
    }

    override fun visitContinue(continueStatement: Tr.Continue): String {
        return continueStatement.fmt("continue${visit(continueStatement.label)}")
    }

    override fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop): String {
        return doWhileLoop.fmt("do${visit(doWhileLoop.body)}while${visit(doWhileLoop.condition)}")
    }

    override fun visitEmpty(empty: Tr.Empty): String = empty.fmt("")

    override fun visitEnumValue(enum: Tr.EnumValue): String {
        val initializer = if(enum.initializer != null) {
            enum.initializer.fmt("(${visit(enum.initializer.args, ",")})")
        } else ""

        return enum.fmt("${visit(enum.name)}$initializer")
    }

    override fun visitEnumValueSet(enums: Tr.EnumValueSet): String {
        return enums.fmt(visit(enums.enums, ",")) + if(enums.terminatedWithSemicolon) ";" else ""
    }

    override fun visitFieldAccess(field: Tr.FieldAccess): String {
        return field.fmt("${visit(field.target)}.${visit(field.name)}")
    }

    override fun visitForLoop(forLoop: Tr.ForLoop): String {
        val expr = forLoop.control.let { it.fmt("(${visit(it.init)};${visit(it.condition)};${visit(it.update)})") }
        return forLoop.fmt("for$expr${visitStatement(forLoop.body)}")
    }

    override fun visitForEachLoop(forEachLoop: Tr.ForEachLoop): String {
        val control = forEachLoop.control.let { it.fmt("(${visit(it.variable)}:${visit(it.iterable)})") }
        return forEachLoop.fmt("for$control${visitStatement(forEachLoop.body)}")
    }

    override fun visitIdentifier(ident: Tr.Ident): String {
        return ident.fmt(ident.simpleName)
    }

    override fun visitIf(iff: Tr.If): String {
        val elsePart = iff.elsePart?.let { it.fmt("else${visitStatement(iff.elsePart.statement)}") } ?: ""
        return iff.fmt("if${visit(iff.ifCondition)}${visitStatement(iff.thenPart)}$elsePart")
    }

    override fun visitImport(import: Tr.Import): String {
        return if (import.static)
            import.fmt("import static${visit(import.qualid)}")
        else
            import.fmt("import${visit(import.qualid)}")
    }

    override fun visitInstanceOf(instanceOf: Tr.InstanceOf): String {
        return instanceOf.fmt("${visit(instanceOf.expr)}instanceof${visit(instanceOf.clazz)}")
    }

    override fun visitLabel(label: Tr.Label): String {
        return label.fmt("${visit(label.label)}:${visit(label.statement)}")
    }

    override fun visitLambda(lambda: Tr.Lambda): String {
        val params = visit(lambda.paramSet.params, ",")
        val paramSet = lambda.paramSet.fmt(if(lambda.paramSet.parenthesized) "($params)" else params)
        return lambda.fmt("$paramSet${lambda.arrow.fmt("->")}${visit(lambda.body)}")
    }

    override fun visitLiteral(literal: Tr.Literal): String {
        return literal.fmt(literal.valueSource)
    }

    override fun visitMemberReference(memberRef: Tr.MemberReference): String {
        return memberRef.fmt("${visit(memberRef.containing)}::${visit(memberRef.reference)}")
    }

    override fun visitMethod(method: Tr.MethodDecl): String {
        val modifiers = visitModifiers(method.modifiers)
        val typeParams = method.typeParameters?.let { it.fmt("<${visit(it.params, ",")}>") } ?: ""
        val params = method.params.fmt("(${visit(method.params.params, ",")}") + ")"
        val default = method.defaultValue?.let { it.fmt("default${visit(it.value)}") } ?: ""

        val throws = method.throws?.let {
            it.fmt("throws${visit(method.throws.exceptions, ",")}")
        } ?: ""

        return method.fmt("${visit(method.annotations)}$modifiers$typeParams${visit(method.returnTypeExpr)}${visit(method.name)}$params$throws${visit(method.body)}$default")
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): String {
        val args = meth.args.fmt("(${visit(meth.args.args, ",")})")
        val typeParams = meth.typeParameters?.let { it.fmt("<${visit(it.params, ",")}>") } ?: ""
        val selectSeparator = if(meth.select != null) "." else ""
        return meth.fmt("${visit(meth.select)}$selectSeparator$typeParams${visit(meth.name)}$args")
    }

    private fun visitModifiers(modifiers: Iterable<Tr.Modifier>): String = modifiers.fold("") { s, mod ->
        s + mod.fmt(when(mod) {
            is Tr.Modifier.Public -> "public"
            is Tr.Modifier.Protected -> "protected"
            is Tr.Modifier.Private -> "private"
            is Tr.Modifier.Abstract -> "abstract"
            is Tr.Modifier.Native -> "native"
            is Tr.Modifier.Static -> "static"
            is Tr.Modifier.Strictfp -> "strictfp"
            is Tr.Modifier.Final -> "final"
            is Tr.Modifier.Transient -> "transient"
            is Tr.Modifier.Volatile -> "volatile"
            is Tr.Modifier.Default -> "default"
            is Tr.Modifier.Synchronized -> "synchronized"
        })
    }

    override fun visitMultiCatch(multiCatch: Tr.MultiCatch): String {
        return multiCatch.fmt(visit(multiCatch.alternatives, "|"))
    }

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): String {
        val modifiers = visitModifiers(multiVariable.modifiers)

        val varargs = when(multiVariable.varArgs) {
            is Tr.VariableDecls.Varargs -> multiVariable.varArgs.fmt("...")
            else -> ""
        }

        return multiVariable.fmt("${visit(multiVariable.annotations)}$modifiers" +
                "${visit(multiVariable.typeExpr)}${visitDims(multiVariable.dimensionsBeforeName)}" +
                "$varargs${visit(multiVariable.vars, ",")}")
    }

    override fun visitNewArray(newArray: Tr.NewArray): String {
        val typeExpr = newArray.typeExpr?.let { "new${visit(newArray.typeExpr)}" } ?: ""
        val dimensions = newArray.dimensions.fold("") { s, dim -> s + dim.fmt("[${visit(dim.size)}]") }
        val init = newArray.initializer?.let { it.fmt("{${visit(it.elements, ",")}}") } ?: ""
        return newArray.fmt("$typeExpr$dimensions$init")
    }

    override fun visitNewClass(newClass: Tr.NewClass): String {
        val args = newClass.args.fmt("(${visit(newClass.args.args, ",")})")
        return newClass.fmt("new${visit(newClass.clazz)}$args${visit(newClass.classBody)}")
    }

    override fun visitPackage(pkg: Tr.Package): String {
        return pkg.fmt("package${visit(pkg.expr)}")
    }

    override fun visitParameterizedType(type: Tr.ParameterizedType): String {
        val typeParams = type.typeArguments?.let {
            it.fmt("<${visit(it.args, ",")}>")
        } ?: ""
        return type.fmt("${visit(type.clazz)}$typeParams")
    }

    override fun visitPrimitive(primitive: Tr.Primitive): String {
        return primitive.fmt(when (primitive.type) {
            Type.Primitive.Boolean -> "boolean"
            Type.Primitive.Byte -> "byte"
            Type.Primitive.Char -> "char"
            Type.Primitive.Double -> "double"
            Type.Primitive.Float -> "float"
            Type.Primitive.Int -> "int"
            Type.Primitive.Long -> "long"
            Type.Primitive.Short -> "short"
            Type.Primitive.Void -> "void"
            Type.Primitive.String -> "String"
            Type.Primitive.Wildcard -> "*"
            Type.Primitive.None -> error("Unable to print None primitive")
            Type.Primitive.Null -> error("Unable to print Null primitive")
            else -> error("Unable to print non-primitive type")
        })
    }

    override fun <T: Tree> visitParentheses(parens: Tr.Parentheses<T>): String {
        return parens.fmt("(${visit(parens.tree)})")
    }

    override fun visitReturn(retrn: Tr.Return): String {
        return retrn.fmt("return${visit(retrn.expr)}")
    }

    override fun visitSwitch(switch: Tr.Switch): String {
        return switch.fmt("switch${visit(switch.selector)}${visit(switch.cases)}")
    }

    override fun visitSynchronized(synch: Tr.Synchronized): String {
        return synch.fmt("synchronized${visit(synch.lock)}${visit(synch.body)}")
    }

    override fun visitTernary(ternary: Tr.Ternary): String {
        return ternary.fmt("${visit(ternary.condition)}?${visit(ternary.truePart)}:${visit(ternary.falsePart)}")
    }

    override fun visitThrow(thrown: Tr.Throw): String {
        return thrown.fmt("throw${visit(thrown.exception)}")
    }

    override fun visitTry(tryable: Tr.Try): String {
        val resources = tryable.resources?.let {
            it.fmt("(${visit(it.decls, ";")})")
        } ?: ""

        val finally = tryable.finally?.let {
            it.fmt("finally${visit(it.block)}")
        } ?: ""

        return tryable.fmt("try$resources${visit(tryable.body)}${visit(tryable.catches)}$finally")
    }

    override fun visitTypeCast(typeCast: Tr.TypeCast): String {
        return typeCast.fmt("${visit(typeCast.clazz)}${visit(typeCast.expr)}")
    }

    override fun visitTypeParameter(typeParameter: Tr.TypeParameter): String {
        val bounds = if(typeParameter.bounds != null) {
            typeParameter.bounds.fmt("extends${visit(typeParameter.bounds.types, "&")}")
        } else ""

        return typeParameter.fmt("${visit(typeParameter.annotations, "")}${visit(typeParameter.name)}$bounds")
    }

    override fun visitUnary(unary: Tr.Unary): String {
        return unary.fmt(when (unary.operator) {
            is Tr.Unary.Operator.PreIncrement -> "++${visit(unary.expr)}"
            is Tr.Unary.Operator.PreDecrement -> "--${visit(unary.expr)}"
            is Tr.Unary.Operator.PostIncrement -> "${visit(unary.expr)}${unary.operator.fmt("++")}"
            is Tr.Unary.Operator.PostDecrement -> "${visit(unary.expr)}${unary.operator.fmt("--")}"
            is Tr.Unary.Operator.Positive -> "+${visit(unary.expr)}"
            is Tr.Unary.Operator.Negative -> "-${visit(unary.expr)}"
            is Tr.Unary.Operator.Complement -> "~${visit(unary.expr)}"
            is Tr.Unary.Operator.Not -> "!${visit(unary.expr)}"
        })
    }

    override fun visitUnparsedSource(unparsed: Tr.UnparsedSource): String {
        return unparsed.fmt(unparsed.source)
    }

    override fun visitVariable(variable: Tr.VariableDecls.NamedVar): String {
        val init = when (variable.initializer) {
            is Expression -> "=${visit(variable.initializer)}"
            else -> ""
        }

        return variable.fmt("${visit(variable.name)}${visitDims(variable.dimensionsAfterName)}$init")
    }

    override fun visitWhileLoop(whileLoop: Tr.WhileLoop): String {
        return whileLoop.fmt("while${visit(whileLoop.condition)}${visitStatement(whileLoop.body)}")
    }

    override fun visitWildcard(wildcard: Tr.Wildcard): String {
        val bound = when(wildcard.bound) {
            is Tr.Wildcard.Bound.Super -> wildcard.bound.fmt("super")
            is Tr.Wildcard.Bound.Extends -> wildcard.bound.fmt("extends")
            null -> ""
        }

        return wildcard.fmt("?$bound${visit(wildcard.boundedType)}")
    }

    private fun Tree?.fmt(code: String?): String {
        return if (this == null || code == null)
            ""
        else {
            formatting.prefix() + code + formatting.suffix()
        }
    }

    private fun Formatting.prefix(): String = when(this) {
        is Formatting.Reified -> prefix
        is Formatting.Infer -> ""
        is Formatting.None -> ""
    }

    private fun Formatting.suffix(): String = when(this) {
        is Formatting.Reified -> suffix
        is Formatting.Infer -> ""
        is Formatting.None -> ""
    }

    private fun visitDims(dims: List<Tr.VariableDecls.Dimension>): String =
            dims.fold("") { s, d -> s + d.fmt("[${visit(d.whitespace)}]") }
}
