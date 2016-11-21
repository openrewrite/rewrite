package com.netflix.java.refactor.ast.visitor

import com.netflix.java.refactor.ast.*

class TransformVisitor(val transformations: Iterable<AstTransform<*>>) : AstVisitor<Tree?>({ it }) {
    private fun <T : Tree> T.transformIfNecessary(cursor: Cursor): T {
        return transformations
                .filterIsInstance<AstTransform<T>>()
                .filter { it.cursor == cursor }
                .map { it.mutation }
                .fold(this) { mutated, mut -> mut(mutated) }
    }

    private fun <T> List<T>.mapIfNecessary(transform: (T) -> T): List<T> {
        var changed = false
        val mapped = this.map {
            val mappedElem = transform(it)
            if(it !== mappedElem)
                changed = true
            mappedElem
        }
        
        return if(changed) mapped else this
    }

    override fun visitAnnotation(annotation: Tr.Annotation): Tree? {
        val annotationType = visit(annotation.annotationType) as NameTree
        val args = annotation.args?.let {
            val args = it.args.mapIfNecessary { visit(it) as Expression }
            if(it.args !== args) it.copy(args) else it
        }

        return (if(annotationType !== annotation.annotationType || args !== annotation.args) {
            annotation.copy(annotationType = annotationType, args = args)
        } else annotation).transformIfNecessary(cursor())
    }

    override fun visitArrayAccess(arrayAccess: Tr.ArrayAccess): Tree {
        val indexed = visit(arrayAccess.indexed) as Expression

        val dimension = arrayAccess.dimension.let {
            val index = visit(arrayAccess.dimension.index) as Expression
            if(it.index !== index) it.copy(index) else it
        }

        return (if(indexed !== arrayAccess.indexed || dimension !== arrayAccess.dimension) {
            arrayAccess.copy(indexed = indexed, dimension = dimension)
        } else arrayAccess).transformIfNecessary(cursor())
    }

    override fun visitArrayType(arrayType: Tr.ArrayType): Tree? {
        val elementType = visit(arrayType.elementType) as TypeTree

        return (if(elementType !== arrayType.elementType) {
            arrayType.copy(elementType = elementType)
        } else arrayType).transformIfNecessary(cursor())
    }

    override fun visitAssign(assign: Tr.Assign): Tree {
        val variable = visit(assign.variable) as Expression
        val assignment = visit(assign.assignment) as Expression

        return (if(variable !== assign.variable || assignment !== assign.assignment) {
            assign.copy(variable = variable, assignment = assignment)
        } else assign).transformIfNecessary(cursor())
    }

    override fun visitAssignOp(assign: Tr.AssignOp): Tree {
        val variable = visit(assign.variable) as Expression
        val assignment = visit(assign.assignment) as Expression

        return (if(variable !== assign.variable || assignment !== assign.assignment) {
            assign.copy(variable = variable, assignment = assignment)
        } else assign).transformIfNecessary(cursor())
    }

    override fun visitBinary(binary: Tr.Binary): Tree {
        val left = visit(binary.left) as Expression
        val right = visit(binary.right) as Expression

        return (if(left !== binary.left || right !== binary.right) {
            binary.copy(left = left, right = right, formatting = binary.formatting)
        } else binary).transformIfNecessary(cursor())
    }

    override fun visitBlock(block: Tr.Block<Tree>): Tree {
        val statements = block.statements.mapIfNecessary { visit(it) as Tree }

        return (if(statements !== block.statements) {
            block.copy(statements = statements)
        } else block).transformIfNecessary(cursor())
    }

    override fun visitBreak(breakStatement: Tr.Break): Tree = breakStatement.transformIfNecessary(cursor())

    override fun visitCase(case: Tr.Case): Tree {
        val pattern = visit(case.pattern) as Expression?
        val statements = case.statements.mapIfNecessary { visit(it) as Statement }

        return (if(pattern !== case.pattern || statements !== case.statements) {
            case.copy(pattern = pattern, statements = statements)
        } else case).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitCatch(catch: Tr.Catch): Tree {
        val param = visit(catch.param) as Tr.Parentheses<Tr.VariableDecls>
        val body = visit(catch.body) as Tr.Block<Statement>

        return (if(param !== catch.param || body !== catch.body) {
            catch.copy(param = param, body = body)
        } else catch).transformIfNecessary(cursor())
    }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): Tree {
        val annotations = classDecl.annotations.mapIfNecessary { visit(it) as Tr.Annotation }
        val name = visit(classDecl.name) as Tr.Ident
        val extends = visit(classDecl.extends)
        val implements = classDecl.implements.mapIfNecessary { visit(it) as Tree }
        val body = visit(classDecl.body) as Tr.Block<Tree>

        return (if(annotations !== classDecl.annotations || name !== classDecl.name || body !== classDecl.body ||
                implements !== classDecl.implements || extends !== classDecl.extends) {
            classDecl.copy(annotations = annotations, name = name, body = body, implements = implements, extends = extends)
        } else classDecl).transformIfNecessary(cursor())
    }

    override fun visitCompilationUnit(cu: Tr.CompilationUnit): Tree {
        val imports = cu.imports.mapIfNecessary { visit(it) as Tr.Import }
        val packageDecl = visit(cu.packageDecl) as Tr.Package?
        val classDecls = cu.classes.mapIfNecessary { visit(it) as Tr.ClassDecl }

        return (if(imports !== cu.imports || packageDecl !== cu.packageDecl || classDecls !== cu.classes) {
            cu.copy(imports = imports, packageDecl = packageDecl, classes = classDecls)
        } else cu).transformIfNecessary(cursor())
    }

    override fun visitContinue(continueStatement: Tr.Continue): Tree = continueStatement.transformIfNecessary(cursor())

    @Suppress("UNCHECKED_CAST")
    override fun visitDoWhileLoop(doWhileLoop: Tr.DoWhileLoop): Tree {
        val condition = visit(doWhileLoop.condition) as Tr.Parentheses<Expression>
        val body = visit(doWhileLoop.body) as Statement

        return (if(condition !== doWhileLoop.condition || body !== doWhileLoop.body) {
            doWhileLoop.copy(condition = condition, body = body)
        } else doWhileLoop).transformIfNecessary(cursor())
    }

    override fun visitEmpty(empty: Tr.Empty): Tree = empty.transformIfNecessary(cursor())

    override fun visitFieldAccess(field: Tr.FieldAccess): Tree {
        val target = visit(field.target) as Expression
        return (if(target !== field.target) {
            field.copy(target = target)
        }
        else field).transformIfNecessary(cursor())
    }

    override fun visitForLoop(forLoop: Tr.ForLoop): Tree {
        val control = forLoop.control.let {
            val init = visit(it.init) as Statement
            val condition = visit(it.condition) as Expression
            val update = it.update.mapIfNecessary { visit(it) as Statement }

            if(init != it.init || condition != it.condition || update != it.update) {
                forLoop.control.copy(init = init, condition = condition, update = update)
            } else forLoop.control
        }

        val body = visit(forLoop.body) as Statement

        return (if(control !== forLoop.control || body !== forLoop.body) {
            forLoop.copy(control = control, body = body)
        } else forLoop).transformIfNecessary(cursor())
    }

    override fun visitForEachLoop(forEachLoop: Tr.ForEachLoop): Tree {
        val control = forEachLoop.control.let {
            val variable = visit(it.variable) as Tr.VariableDecls
            val iterable = visit(it.iterable) as Expression

            if(variable !== it.variable || iterable !== it.iterable) {
                it.copy(variable, iterable)
            } else it
        }

        val body = visit(forEachLoop.body) as Statement

        return (if(control !== forEachLoop.control || body !== forEachLoop.body) {
            forEachLoop.copy(control = control, body = body)
        } else forEachLoop).transformIfNecessary(cursor())
    }

    override fun visitIdentifier(ident: Tr.Ident): Tree = ident.transformIfNecessary(cursor())

    @Suppress("UNCHECKED_CAST")
    override fun visitIf(iff: Tr.If): Tree {
        val ifCondition = visit(iff.ifCondition) as Tr.Parentheses<Expression>
        val thenPart = visit(iff.thenPart) as Statement

        val elsePart = iff.elsePart?.let {
            val statement = visit(it.statement) as Statement
            if(it.statement !== statement) it.copy(statement) else it
        }

        return (if(ifCondition !== iff.ifCondition || thenPart !== iff.thenPart || elsePart !== iff.elsePart) {
            iff.copy(ifCondition = ifCondition, thenPart = thenPart, elsePart = elsePart)
        } else iff).transformIfNecessary(cursor())
    }

    override fun visitImport(import: Tr.Import): Tree {
        val qualid = visit(import.qualid) as Tr.FieldAccess
        return (if(qualid !== import.qualid) {
            import.copy(qualid = qualid)
        }
        else import).transformIfNecessary(cursor())
    }

    override fun visitInstanceOf(instanceOf: Tr.InstanceOf): Tree {
        val expr = visit(instanceOf.expr) as Expression
        val clazz = visit(instanceOf.clazz) as Tree

        return (if(expr !== instanceOf.expr || clazz !== instanceOf.clazz) {
            instanceOf.copy(expr = expr, clazz = clazz)
        } else instanceOf).transformIfNecessary(cursor())
    }

    override fun visitLabel(label: Tr.Label): Tree {
        val statement = visit(label.statement) as Statement

        return (if(statement !== label.statement) {
            label.copy(statement = statement)
        } else label).transformIfNecessary(cursor())
    }

    override fun visitLambda(lambda: Tr.Lambda): Tree {
        val params = lambda.params.mapIfNecessary { visit(it) as Tr.VariableDecls }
        val body = visit(lambda.body) as Tree

        return (if(params !== lambda.params || body !== lambda.body) {
            lambda.copy(params = params, body = body)
        } else lambda).transformIfNecessary(cursor())
    }

    override fun visitLiteral(literal: Tr.Literal): Tree = literal.transformIfNecessary(cursor())

    override fun visitMethod(method: Tr.MethodDecl): Tree {
        val params = method.params.params.mapIfNecessary { visit(it) as Statement }

        val throws = method.throws?.let {
            val exceptions = it.exceptions.mapIfNecessary { visit(it) as NameTree }
            if(it.exceptions !== exceptions) it.copy(exceptions) else it
        }

        val defaultValue = visit(method.defaultValue) as Expression?

        val typeParams = method.typeParameters?.let {
            val generics = it.params.mapIfNecessary { visit(it) as Tr.TypeParameter }
            if(it.params !== generics) it.copy(generics) else it
        }

        @Suppress("UNCHECKED_CAST")
        val body = visit(method.body) as Tr.Block<Statement>

        return (if(params !== method.params.params || throws !== method.throws || defaultValue !== method.defaultValue ||
                body !== method.body || typeParams !== method.typeParameters) {
            method.copy(params = method.params.copy(params), throws = throws, defaultValue = defaultValue, body = body,
                    typeParameters = typeParams)
        } else method).transformIfNecessary(cursor())
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): Tree {
        val methodSelect = visit(meth.select) as Expression?

        val args = meth.args.let {
            val args = it.args.mapIfNecessary { visit(it) as Expression }
            if(it.args !== args) it.copy(args) else it
        }

        return (if(methodSelect !== meth.select || args !== meth.args) {
            meth.copy(select = methodSelect, args = args)
        } else meth).transformIfNecessary(cursor())
    }

    override fun visitMultiCatch(multiCatch: Tr.MultiCatch): Tree? {
        val alternatives = multiCatch.alternatives.mapIfNecessary { visit(it) as NameTree }

        return (if(alternatives !== multiCatch.alternatives) {
            multiCatch.copy(alternatives = alternatives)
        } else multiCatch).transformIfNecessary(cursor())
    }

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): Tree {
        val typeExpr = visit(multiVariable.typeExpr) as TypeTree
        val vars = multiVariable.vars.mapIfNecessary { visit(it) as Tr.VariableDecls.NamedVar }

        return (if(typeExpr !== multiVariable.typeExpr || vars !== multiVariable.vars) {
            multiVariable.copy(typeExpr = typeExpr, vars = vars)
        } else multiVariable).transformIfNecessary(cursor())
    }

    override fun visitNewArray(newArray: Tr.NewArray): Tree {
        val typeExpr = visit(newArray.typeExpr) as TypeTree

        val dimensions = newArray.dimensions.mapIfNecessary {
            val size = visit(it.size) as Expression
            if(it.size !== size) it.copy(size = size) else it
        }

        val initializer = if(newArray.initializer != null) {
            val elements = newArray.initializer.elements.mapIfNecessary { visit(it) as Expression }
            if(elements != newArray.initializer.elements) {
                newArray.initializer.copy(elements)
            } else newArray.initializer
        } else null

        return (if(typeExpr !== newArray.typeExpr || dimensions !== newArray.dimensions || initializer !== newArray.initializer) {
            newArray.copy(typeExpr = typeExpr, dimensions = dimensions, initializer = initializer)
        } else newArray).transformIfNecessary(cursor())
    }

    override fun visitNewClass(newClass: Tr.NewClass): Tree {
        val clazz = newClass.clazz
        val args = newClass.args.let {
            val params = it.args.mapIfNecessary { visit(it) as Expression }
            if(it.args !== params) it.copy(params) else it
        }
        val classBody = visit(newClass.classBody) as Tr.Block<*>?

        return (if(clazz !== newClass.clazz || args !== newClass.args || classBody !== newClass.classBody) {
            newClass.copy(clazz = clazz, args = args, classBody = classBody)
        } else newClass).transformIfNecessary(cursor())
    }

    override fun visitPackage(pkg: Tr.Package): Tree? {
        val expr = visit(pkg.expr) as Expression
        return (if(expr !== pkg.expr) {
            pkg.copy(expr = expr)
        } else pkg).transformIfNecessary(cursor())
    }

    override fun visitParameterizedType(type: Tr.ParameterizedType): Tree? {
        val clazz = visit(type.clazz) as NameTree

        val typeArguments = type.typeArguments?.let {
            val typeArgs = it.args.mapIfNecessary { visit(it) as NameTree }
            if(it.args !== typeArgs) it.copy(typeArgs) else it
        }

        return (if(typeArguments !== type.typeArguments || clazz !== type.clazz) {
            type.copy(clazz = clazz, typeArguments = typeArguments)
        } else type).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T: Tree> visitParentheses(parens: Tr.Parentheses<T>): Tree {
        val tree = visit(parens.tree) as T

        return (if(tree !== parens.tree) {
            parens.copy(tree = tree)
        } else parens).transformIfNecessary(cursor())
    }

    override fun visitPrimitive(primitive: Tr.Primitive): Tree = primitive.transformIfNecessary(cursor())

    override fun visitReturn(retrn: Tr.Return): Tree {
        val expr = visit(retrn.expr) as Expression?

        return (if(expr !== retrn.expr) {
            retrn.copy(expr = expr)
        } else retrn).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitSwitch(switch: Tr.Switch): Tree {
        val selector = visit(switch.selector) as Tr.Parentheses<Expression>
        val caseBlock = switch.cases.let {
            val cases = it.statements.mapIfNecessary { visit(it) as Tr.Case }
            if(it.statements !== cases) it.copy(statements = cases) else it
        }

        return (if(selector !== switch.selector || caseBlock !== switch.cases) {
            switch.copy(selector = selector, cases = caseBlock)
        } else switch).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitSynchronized(synch: Tr.Synchronized): Tree {
        val lock = visit(synch.lock) as Tr.Parentheses<Expression>
        val body = visit(synch.body) as Tr.Block<Statement>

        return (if(lock !== synch.lock || body !== synch.body) {
            synch.copy(lock = lock, body = body)
        } else synch).transformIfNecessary(cursor())
    }

    override fun visitTernary(ternary: Tr.Ternary): Tree {
        val condition = visit(ternary.condition) as Expression
        val truePart = visit(ternary.truePart) as Expression
        val falsePart = visit(ternary.falsePart) as Expression

        return (if(condition !== ternary.condition || truePart !== ternary.truePart || falsePart !== ternary.falsePart) {
            ternary.copy(condition = condition, truePart = truePart, falsePart = falsePart)
        } else ternary).transformIfNecessary(cursor())
    }

    override fun visitThrow(thrown: Tr.Throw): Tree {
        val exception = visit(thrown.exception) as Expression
        return (if(exception !== thrown.exception) {
            thrown.copy(exception = exception)
        } else exception).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitTry(tryable: Tr.Try): Tree {
        val resources = tryable.resources?.let {
            val decls = it.decls.mapIfNecessary { visit(it) as Tr.VariableDecls }
            if(it.decls !== decls) it.copy(decls) else it
        }

        val body = visit(tryable.body) as Tr.Block<Statement>
        val catches = tryable.catches.mapIfNecessary { visit(it) as Tr.Catch }

        val finally = tryable.finally?.let {
            val block = visit(tryable.finally) as Tr.Block<Statement>
            if(it.block !== block) it.copy(block) else it
        }

        return (if(resources !== tryable.resources || body !== tryable.body || catches !== tryable.catches ||
                finally !== tryable.finally) {
            tryable.copy(resources = resources, body = body, catches = catches, finally = finally)
        } else tryable).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitTypeCast(typeCast: Tr.TypeCast): Tree {
        val clazz = visit(typeCast.clazz) as Tr.Parentheses<TypeTree>
        val expr = visit(typeCast.expr) as Expression

        return (if(clazz !== typeCast.clazz || expr !== typeCast.expr) {
            typeCast.copy(clazz = clazz, expr = expr)
        } else typeCast).transformIfNecessary(cursor())
    }

    override fun visitUnary(unary: Tr.Unary): Tree {
        val expr = visit(unary.expr) as Expression

        return (if(expr !== unary.expr) {
            unary.copy(expr = expr)
        } else unary).transformIfNecessary(cursor())
    }

    override fun visitUnparsedSource(unparsed: Tr.UnparsedSource): Tree? {
        return unparsed
    }

    override fun visitVariable(variable: Tr.VariableDecls.NamedVar): Tree? {
        val name = visit(variable.name) as Tr.Ident
        val initializer = visit(variable.initializer) as Expression?

        return (if(name !== variable.name || initializer !== variable.initializer) {
            variable.copy(name = name, initializer = initializer)
        } else variable).transformIfNecessary(cursor())
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitWhileLoop(whileLoop: Tr.WhileLoop): Tree {
        val condition = visit(whileLoop.condition) as Tr.Parentheses<Expression>
        val body = visit(whileLoop.body) as Statement

        return (if(condition !== whileLoop.condition || body !== whileLoop.body) {
            whileLoop.copy(condition = condition, body = body)
        } else whileLoop).transformIfNecessary(cursor())
    }

    override fun visitWildcard(wildcard: Tr.Wildcard): Tree? {
        val boundedType = visit(wildcard.boundedType) as NameTree

        return (if(boundedType !== wildcard.boundedType) {
            wildcard.copy(boundedType = boundedType)
        } else wildcard).transformIfNecessary(cursor())
    }
}
