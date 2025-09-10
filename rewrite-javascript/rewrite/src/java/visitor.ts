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
import {isTree, SourceFile} from "../tree";
import {mapAsync} from "../util";
import {produceAsync, TreeVisitor, ValidImmerRecipeReturnType} from "../visitor";
import {
    Expression,
    isJava,
    isSpace,
    J,
    NameTree,
    Statement, TypedTree, TypeTree
} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";
import {JavaType} from "./type";

export class JavaVisitor<P> extends TreeVisitor<J, P> {
    // protected javadocVisitor: any | null = null;

    async isAcceptable(sourceFile: SourceFile): Promise<boolean> {
        return isJava(sourceFile);
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    protected async visitExpression(expression: Expression, p: P): Promise<J | undefined> {
        return expression;
    }

    // noinspection JSUnusedGlobalSymbols,JSUnusedLocalSymbols
    protected async visitStatement(statement: Statement, p: P): Promise<J | undefined> {
        return statement;
    }

    // noinspection JSUnusedLocalSymbols
    protected async visitSpace(space: J.Space, p: P): Promise<J.Space> {
        return space;
    }

    // noinspection JSUnusedLocalSymbols
    protected async visitType(javaType: JavaType | undefined, p: P): Promise<JavaType | undefined> {
        return javaType;
    }

    // noinspection JSUnusedLocalSymbols
    protected async visitTypeName<N extends NameTree>(nameTree: N, p: P): Promise<N> {
        return nameTree;
    }

    protected async visitAnnotatedType(annotatedType: J.AnnotatedType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(annotatedType, p);
        if (!expression?.kind || expression.kind !== J.Kind.AnnotatedType) {
            return expression;
        }
        annotatedType = expression as J.AnnotatedType;

        return this.produceJava<J.AnnotatedType>(annotatedType, p, async draft => {
            draft.annotations = await mapAsync(annotatedType.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.typeExpression = await this.visitDefined(annotatedType.typeExpression, p) as TypedTree;
        });
    }

    protected async visitAnnotation(annotation: J.Annotation, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(annotation, p);
        if (!expression?.kind || expression.kind !== J.Kind.Annotation) {
            return expression;
        }
        annotation = expression as J.Annotation;

        return this.produceJava<J.Annotation>(annotation, p, async draft => {
            draft.annotationType = await this.visitTypeName(annotation.annotationType, p);
            draft.arguments = await this.visitOptionalContainer(annotation.arguments, p);
        });
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(arrayAccess, p);
        if (!expression?.kind || expression.kind !== J.Kind.ArrayAccess) {
            return expression;
        }
        arrayAccess = expression as J.ArrayAccess;

        return this.produceJava<J.ArrayAccess>(arrayAccess, p, async draft => {
            draft.indexed = await this.visitDefined(arrayAccess.indexed, p) as Expression;
            draft.dimension = await this.visitDefined(arrayAccess.dimension, p) as J.ArrayDimension;
        });
    }

    protected async visitArrayDimension(arrayDimension: J.ArrayDimension, p: P): Promise<J | undefined> {
        return this.produceJava<J.ArrayDimension>(arrayDimension, p, async draft => {
            draft.index = await this.visitRightPadded(arrayDimension.index, p);
        });
    }

    protected async visitArrayType(arrayType: J.ArrayType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(arrayType, p);
        if (!expression?.kind || expression.kind !== J.Kind.ArrayType) {
            return expression;
        }
        arrayType = expression as J.ArrayType;

        return this.produceJava<J.ArrayType>(arrayType, p, async draft => {
            draft.elementType = await this.visitDefined(arrayType.elementType, p) as TypedTree;
            if (arrayType.annotations) {
                draft.annotations = await mapAsync(arrayType.annotations, a => this.visitDefined<J.Annotation>(a, p));
            }
            draft.dimension = await this.visitLeftPadded(arrayType.dimension, p);
            draft.type = await this.visitType(arrayType.type, p);
        });
    }

    protected async visitAssert(anAssert: J.Assert, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(anAssert, p);
        if (!statement?.kind || statement.kind !== J.Kind.Assert) {
            return statement;
        }
        anAssert = statement as J.Assert;

        return this.produceJava<J.Assert>(anAssert, p, async draft => {
            draft.condition = await this.visitDefined(anAssert.condition, p) as Expression;
            draft.detail = await this.visitOptionalLeftPadded(anAssert.detail, p);
        });
    }

    protected async visitAssignment(assignment: J.Assignment, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(assignment, p);
        if (!expression?.kind || expression.kind !== J.Kind.Assignment) {
            return expression;
        }
        assignment = expression as J.Assignment;

        const statement = await this.visitStatement(assignment, p);
        if (!statement?.kind || statement.kind !== J.Kind.Assignment) {
            return statement;
        }
        assignment = statement as J.Assignment;

        return this.produceJava<J.Assignment>(assignment, p, async draft => {
            draft.variable = await this.visitDefined(assignment.variable, p) as Expression;
            draft.assignment = await this.visitLeftPadded(assignment.assignment, p);
            draft.type = await this.visitType(assignment.type, p);
        });
    }

    protected async visitAssignmentOperation(assignOp: J.AssignmentOperation, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(assignOp, p);
        if (!expression?.kind || expression.kind !== J.Kind.AssignmentOperation) {
            return expression;
        }
        assignOp = expression as J.AssignmentOperation;

        return this.produceJava<J.AssignmentOperation>(assignOp, p, async draft => {
            draft.variable = await this.visitDefined(assignOp.variable, p) as Expression;
            draft.operator = await this.visitLeftPadded(assignOp.operator, p);
            draft.assignment = await this.visitDefined(assignOp.assignment, p) as Expression;
            draft.type = await this.visitType(assignOp.type, p);
        });
    }

    protected async visitBinary(binary: J.Binary, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(binary, p);
        if (!expression?.kind || expression.kind !== J.Kind.Binary) {
            return expression;
        }
        binary = expression as J.Binary;

        return this.produceJava<J.Binary>(binary, p, async draft => {
            draft.left = await this.visitDefined(binary.left, p) as Expression;
            draft.operator = await this.visitLeftPadded(binary.operator, p);
            draft.right = await this.visitDefined(binary.right, p) as Expression;
            draft.type = await this.visitType(binary.type, p);
        });
    }

    protected async visitBlock(block: J.Block, p: P): Promise<J | undefined> {
        return this.produceJava<J.Block>(block, p, async draft => {
            draft.static = await this.visitRightPadded(block.static, p);
            draft.statements = await mapAsync(block.statements, stmt => this.visitRightPadded(stmt, p));
            draft.end = await this.visitSpace(block.end, p);
        });
    }

    protected async visitBreak(breakStatement: J.Break, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(breakStatement, p);
        if (!statement?.kind || statement.kind !== J.Kind.Break) {
            return statement;
        }
        breakStatement = statement as J.Break;

        return this.produceJava<J.Break>(breakStatement, p, async draft => {
            if (breakStatement.label) {
                draft.label = await this.visitDefined(breakStatement.label, p) as J.Identifier;
            }
        });
    }

    protected async visitCase(aCase: J.Case, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(aCase, p);
        if (!statement?.kind || statement.kind !== J.Kind.Case) {
            return statement;
        }
        aCase = statement as J.Case;

        return this.produceJava<J.Case>(aCase, p, async draft => {
            draft.caseLabels = await this.visitContainer(aCase.caseLabels, p);
            draft.statements = await this.visitContainer(aCase.statements, p);
            draft.body = await this.visitOptionalRightPadded(aCase.body, p);
            if (aCase.guard) {
                draft.guard = await this.visitDefined(aCase.guard, p) as Expression;
            }
        });
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(classDecl, p);
        if (!statement?.kind || statement.kind !== J.Kind.ClassDeclaration) {
            return statement;
        }
        classDecl = statement as J.ClassDeclaration;

        return this.produceJava<J.ClassDeclaration>(classDecl, p, async draft => {
            draft.leadingAnnotations = await mapAsync(classDecl.leadingAnnotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.modifiers = await mapAsync(classDecl.modifiers, m => this.visitDefined<J.Modifier>(m, p));
            draft.classKind = await this.visitDefined(classDecl.classKind, p) as J.ClassDeclaration.Kind;
            draft.name = await this.visitDefined(classDecl.name, p) as J.Identifier;
            draft.typeParameters = await this.visitOptionalContainer(classDecl.typeParameters, p);
            draft.primaryConstructor = await this.visitOptionalContainer(classDecl.primaryConstructor, p);
            draft.extends = await this.visitOptionalLeftPadded(classDecl.extends, p);
            draft.implements = await this.visitOptionalContainer(classDecl.implements, p);
            draft.permitting = await this.visitOptionalContainer(classDecl.permitting, p);
            draft.body = await this.visitDefined(classDecl.body, p) as J.Block;
            draft.type = await this.visitType(classDecl.type, p) as JavaType.Class | undefined;
        });
    }

    protected async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, p: P): Promise<J | undefined> {
        return this.produceJava<J.ClassDeclaration.Kind>(kind, p, async draft => {
            draft.annotations = await mapAsync(kind.annotations, a => this.visitDefined<J.Annotation>(a, p));
        });
    }

    protected async visitCompilationUnit(cu: J.CompilationUnit, p: P): Promise<J | undefined> {
        return this.produceJava<J.CompilationUnit>(cu, p, async draft => {
            draft.packageDeclaration = await this.visitRightPadded(cu.packageDeclaration, p) as J.RightPadded<J.Package>;
            draft.imports = await mapAsync(cu.imports, imp => this.visitRightPadded(imp, p));
            draft.classes = await mapAsync(cu.classes, cls => this.visitDefined(cls, p) as Promise<J.ClassDeclaration>);
            draft.eof = await this.visitSpace(cu.eof, p);
        });
    }

    protected async visitContinue(continueStatement: J.Continue, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(continueStatement, p);
        if (!statement?.kind || statement.kind !== J.Kind.Continue) {
            return statement;
        }
        continueStatement = statement as J.Continue;

        return this.produceJava<J.Continue>(continueStatement, p, async draft => {
            if (continueStatement.label) {
                draft.label = await this.visitDefined(continueStatement.label, p) as J.Identifier;
            }
        });
    }

    protected async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, p: P): Promise<J | undefined> {
        return this.produceJava<J.ControlParentheses<T>>(controlParens, p, async draft => {
            (draft.tree as J.RightPadded<J>) = await this.visitRightPadded(controlParens.tree, p);
        });
    }

    protected async visitDeconstructionPattern(pattern: J.DeconstructionPattern, p: P): Promise<J | undefined> {
        return this.produceJava<J.DeconstructionPattern>(pattern, p, async draft => {
            draft.deconstructor = await this.visitDefined(pattern.deconstructor, p) as Expression;
            draft.nested = await this.visitContainer(pattern.nested, p);
            draft.type = await this.visitType(pattern.type, p);
        });
    }

    protected async visitDoWhileLoop(doWhileLoop: J.DoWhileLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(doWhileLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.DoWhileLoop) {
            return statement;
        }
        doWhileLoop = statement as J.DoWhileLoop;

        return this.produceJava<J.DoWhileLoop>(doWhileLoop, p, async draft => {
            draft.body = await this.visitRightPadded(doWhileLoop.body, p);
            draft.whileCondition = await this.visitLeftPadded(doWhileLoop.whileCondition, p);
        });
    }

    protected async visitEmpty(empty: J.Empty, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(empty, p);
        if (!expression?.kind || expression.kind !== J.Kind.Empty) {
            return expression;
        }
        empty = expression as J.Empty;

        const statement = await this.visitStatement(empty, p);
        if (!statement?.kind || statement.kind !== J.Kind.Empty) {
            return statement;
        }
        empty = statement as J.Empty;

        return this.produceJava<J.Empty>(empty, p);
    }

    protected async visitEnumValue(enumValue: J.EnumValue, p: P): Promise<J | undefined> {
        return this.produceJava<J.EnumValue>(enumValue, p, async draft => {
            draft.annotations = await mapAsync(enumValue.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.name = await this.visitDefined(enumValue.name, p) as J.Identifier;
            if (enumValue.initializer) {
                draft.initializer = await this.visitDefined(enumValue.initializer, p) as J.NewClass;
            }
        });
    }

    protected async visitEnumValueSet(enumValueSet: J.EnumValueSet, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(enumValueSet, p);
        if (!statement?.kind || statement.kind !== J.Kind.EnumValueSet) {
            return statement;
        }
        enumValueSet = statement as J.EnumValueSet;

        return this.produceJava<J.EnumValueSet>(enumValueSet, p, async draft => {
            draft.enums = await mapAsync(enumValueSet.enums, e => this.visitRightPadded(e, p));
        });
    }

    protected async visitErroneous(erroneous: J.Erroneous, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(erroneous, p);
        if (!expression?.kind || expression.kind !== J.Kind.Erroneous) {
            return expression;
        }
        erroneous = expression as J.Erroneous;

        const statement = await this.visitStatement(erroneous, p);
        if (!statement?.kind || statement.kind !== J.Kind.Erroneous) {
            return statement;
        }
        erroneous = statement as J.Erroneous;

        return this.produceJava<J.Erroneous>(erroneous, p);
    }

    protected async visitFieldAccess(fieldAccess: J.FieldAccess, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(fieldAccess, p);
        if (!expression?.kind || expression.kind !== J.Kind.FieldAccess) {
            return expression;
        }
        fieldAccess = expression as J.FieldAccess;

        const statement = await this.visitStatement(fieldAccess, p);
        if (!statement?.kind || statement.kind !== J.Kind.FieldAccess) {
            return statement;
        }
        fieldAccess = statement as J.FieldAccess;

        return this.produceJava<J.FieldAccess>(fieldAccess, p, async draft => {
            draft.target = await this.visitDefined(fieldAccess.target, p) as Expression;
            draft.name = await this.visitLeftPadded(fieldAccess.name, p);
            draft.type = await this.visitType(fieldAccess.type, p);
        });
    }

    protected async visitForEachLoop(forLoop: J.ForEachLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(forLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.ForEachLoop) {
            return statement;
        }
        forLoop = statement as J.ForEachLoop;

        return this.produceJava<J.ForEachLoop>(forLoop, p, async draft => {
            draft.control = await this.visitDefined(forLoop.control, p) as J.ForEachLoop.Control;
            draft.body = await this.visitRightPadded(forLoop.body, p);
        });
    }

    protected async visitForEachLoopControl(control: J.ForEachLoop.Control, p: P): Promise<J | undefined> {
        return this.produceJava<J.ForEachLoop.Control>(control, p, async draft => {
            draft.variable = await this.visitRightPadded(control.variable, p);
            draft.iterable = await this.visitRightPadded(control.iterable, p);
        });
    }

    protected async visitForLoop(forLoop: J.ForLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(forLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.ForLoop) {
            return statement;
        }
        forLoop = statement as J.ForLoop;

        return this.produceJava<J.ForLoop>(forLoop, p, async draft => {
            draft.control = await this.visitDefined(forLoop.control, p) as J.ForLoop.Control;
            draft.body = await this.visitRightPadded(forLoop.body, p);
        });
    }

    protected async visitForLoopControl(control: J.ForLoop.Control, p: P): Promise<J | undefined> {
        return this.produceJava<J.ForLoop.Control>(control, p, async draft => {
            draft.init = await mapAsync(control.init, i => this.visitRightPadded(i, p));
            draft.condition = await this.visitOptionalRightPadded(control.condition, p);
            draft.update = await mapAsync(control.update, u => this.visitRightPadded(u, p));
        });
    }

    protected async visitIdentifier(ident: J.Identifier, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(ident, p);
        if (!expression?.kind || expression.kind !== J.Kind.Identifier) {
            return expression;
        }
        ident = expression as J.Identifier;

        return this.produceJava<J.Identifier>(ident, p, async draft => {
            draft.annotations = await mapAsync(ident.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.type = await this.visitType(ident.type, p);
            draft.fieldType = await this.visitType(ident.fieldType, p) as JavaType.Variable | undefined;
        });
    }

    protected async visitIf(iff: J.If, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(iff, p);
        if (!statement?.kind || statement.kind !== J.Kind.If) {
            return statement;
        }
        iff = statement as J.If;

        return this.produceJava<J.If>(iff, p, async draft => {
            draft.ifCondition = await this.visitDefined(iff.ifCondition, p) as J.ControlParentheses<Expression>;
            draft.thenPart = await this.visitRightPadded(iff.thenPart, p);
            if (iff.elsePart) {
                draft.elsePart = await this.visitDefined(iff.elsePart, p) as J.If.Else;
            }
        });
    }

    protected async visitElse(anElse: J.If.Else, p: P): Promise<J | undefined> {
        return this.produceJava<J.If.Else>(anElse, p, async draft => {
            draft.body = await this.visitRightPadded(anElse.body, p);
        });
    }

    protected async visitImport(anImport: J.Import, p: P): Promise<J | undefined> {
        return this.produceJava<J.Import>(anImport, p, async draft => {
            draft.static = await this.visitLeftPadded(anImport.static, p);
            draft.qualid = await this.visitDefined(anImport.qualid, p) as J.FieldAccess;
            draft.alias = await this.visitOptionalLeftPadded(anImport.alias, p);
        });
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(instanceOf, p);
        if (!expression?.kind || expression.kind !== J.Kind.InstanceOf) {
            return expression;
        }
        instanceOf = expression as J.InstanceOf;

        return this.produceJava<J.InstanceOf>(instanceOf, p, async draft => {
            draft.expression = await this.visitRightPadded(instanceOf.expression, p);
            draft.class = await this.visitDefined(instanceOf.class, p) as J;
            if (instanceOf.pattern) {
                draft.pattern = await this.visitDefined(instanceOf.pattern, p) as J;
            }
            draft.type = await this.visitType(instanceOf.type, p);
            if (instanceOf.modifier) {
                draft.modifier = await this.visitDefined(instanceOf.modifier, p) as J.Modifier;
            }
        });
    }

    protected async visitIntersectionType(intersectionType: J.IntersectionType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(intersectionType, p);
        if (!expression?.kind || expression.kind !== J.Kind.IntersectionType) {
            return expression;
        }
        intersectionType = expression as J.IntersectionType;

        return this.produceJava<J.IntersectionType>(intersectionType, p, async draft => {
            draft.bounds = await this.visitContainer(intersectionType.bounds, p);
        });
    }

    protected async visitLabel(label: J.Label, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(label, p);
        if (!statement?.kind || statement.kind !== J.Kind.Label) {
            return statement;
        }
        label = statement as J.Label;

        return this.produceJava<J.Label>(label, p, async draft => {
            draft.label = await this.visitRightPadded(label.label, p);
            draft.statement = await this.visitDefined(label.statement, p) as Statement;
        });
    }

    protected async visitLambda(lambda: J.Lambda, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(lambda, p);
        if (!expression?.kind || expression.kind !== J.Kind.Lambda) {
            return expression;
        }
        lambda = expression as J.Lambda;

        const statement = await this.visitStatement(lambda, p);
        if (!statement?.kind || statement.kind !== J.Kind.Lambda) {
            return statement;
        }
        lambda = statement as J.Lambda;

        return this.produceJava<J.Lambda>(lambda, p, async draft => {
            draft.parameters = await this.visitDefined(lambda.parameters, p) as J.Lambda.Parameters;
            draft.arrow = await this.visitSpace(lambda.arrow, p);
            draft.body = await this.visitDefined(lambda.body, p) as Statement | Expression;
            draft.type = await this.visitType(lambda.type, p);
        });
    }

    protected async visitLambdaParameters(params: J.Lambda.Parameters, p: P): Promise<J | undefined> {
        return this.produceJava<J.Lambda.Parameters>(params, p, async draft => {
            draft.parameters = await mapAsync(params.parameters, param => this.visitRightPadded(param, p));
        });
    }

    protected async visitLiteral(literal: J.Literal, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(literal, p);
        if (!expression?.kind || expression.kind !== J.Kind.Literal) {
            return expression;
        }
        literal = expression as J.Literal;

        return this.produceJava<J.Literal>(literal, p, async draft => {
            draft.type = await this.visitType(literal.type, p) as JavaType.Primitive | undefined;
        });
    }

    protected async visitMemberReference(memberRef: J.MemberReference, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(memberRef, p);
        if (!expression?.kind || expression.kind !== J.Kind.MemberReference) {
            return expression;
        }
        memberRef = expression as J.MemberReference;

        return this.produceJava<J.MemberReference>(memberRef, p, async draft => {
            draft.containing = await this.visitRightPadded(memberRef.containing, p);
            draft.typeParameters = await this.visitOptionalContainer(memberRef.typeParameters, p);
            draft.reference = await this.visitLeftPadded(memberRef.reference, p);
            draft.type = await this.visitType(memberRef.type, p);
            draft.methodType = await this.visitType(memberRef.methodType, p) as JavaType.Method | undefined;
            draft.variableType = await this.visitType(memberRef.variableType, p) as JavaType.Variable | undefined;
        });
    }

    protected async visitMethodDeclaration(methodDecl: J.MethodDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(methodDecl, p);
        if (!statement?.kind || statement.kind !== J.Kind.MethodDeclaration) {
            return statement;
        }
        methodDecl = statement as J.MethodDeclaration;

        return this.produceJava<J.MethodDeclaration>(methodDecl, p, async draft => {
            draft.leadingAnnotations = await mapAsync(methodDecl.leadingAnnotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.modifiers = await mapAsync(methodDecl.modifiers, m => this.visitDefined<J.Modifier>(m, p));

            if (methodDecl.typeParameters) {
                draft.typeParameters = await this.visitDefined(methodDecl.typeParameters, p) as J.TypeParameters;
            }

            if (methodDecl.returnTypeExpression) {
                draft.returnTypeExpression = await this.visitDefined(methodDecl.returnTypeExpression, p) as TypedTree;
            }

            draft.nameAnnotations = await mapAsync(methodDecl.nameAnnotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.name = await this.visitDefined(methodDecl.name, p);
            draft.parameters = await this.visitContainer(methodDecl.parameters, p);
            draft.throws = methodDecl.throws && await this.visitContainer(methodDecl.throws, p);
            draft.body = methodDecl.body && await this.visitDefined(methodDecl.body, p) as J.Block;
            draft.defaultValue = await this.visitOptionalLeftPadded(methodDecl.defaultValue, p);
            draft.methodType = await this.visitType(methodDecl.methodType, p) as JavaType.Method | undefined;
        });
    }

    protected async visitMethodInvocation(methodInv: J.MethodInvocation, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(methodInv, p);
        if (!expression?.kind || expression.kind !== J.Kind.MethodInvocation) {
            return expression;
        }
        methodInv = expression as J.MethodInvocation;

        const statement = await this.visitStatement(methodInv, p);
        if (!statement?.kind || statement.kind !== J.Kind.MethodInvocation) {
            return statement;
        }
        methodInv = statement as J.MethodInvocation;

        return this.produceJava<J.MethodInvocation>(methodInv, p, async draft => {
            draft.select = await this.visitOptionalRightPadded(methodInv.select, p);
            draft.typeParameters = await this.visitOptionalContainer(methodInv.typeParameters, p);
            draft.name = await this.visitDefined(methodInv.name, p) as J.Identifier;
            draft.arguments = await this.visitContainer(methodInv.arguments, p);
            draft.methodType = await this.visitType(methodInv.methodType, p) as JavaType.Method | undefined;
        });
    }

    protected async visitModifier(modifier: J.Modifier, p: P): Promise<J | undefined> {
        return this.produceJava<J.Modifier>(modifier, p, async draft => {
            draft.annotations = await mapAsync(modifier.annotations, a => this.visitDefined<J.Annotation>(a, p));
        });
    }

    protected async visitMultiCatch(multiCatch: J.MultiCatch, p: P): Promise<J | undefined> {
        return this.produceJava<J.MultiCatch>(multiCatch, p, async draft => {
            draft.alternatives = await mapAsync(multiCatch.alternatives, alt => this.visitRightPadded(alt, p));
        });
    }

    protected async visitNewArray(newArray: J.NewArray, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(newArray, p);
        if (!expression?.kind || expression.kind !== J.Kind.NewArray) {
            return expression;
        }
        newArray = expression as J.NewArray;

        return this.produceJava<J.NewArray>(newArray, p, async draft => {
            if (newArray.typeExpression) {
                draft.typeExpression = await this.visitDefined(newArray.typeExpression, p) as TypedTree;
            }

            draft.dimensions = await mapAsync(newArray.dimensions, dim =>
                this.visitDefined<J.ArrayDimension>(dim, p));

            draft.initializer = await this.visitOptionalContainer(newArray.initializer, p);
            draft.type = await this.visitType(newArray.type, p);
        });
    }

    protected async visitNewClass(newClass: J.NewClass, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(newClass, p);
        if (!expression?.kind || expression.kind !== J.Kind.NewClass) {
            return expression;
        }
        newClass = expression as J.NewClass;

        return this.produceJava<J.NewClass>(newClass, p, async draft => {
            if (newClass.enclosing) {
                draft.enclosing = await this.visitRightPadded(newClass.enclosing, p);
            }

            draft.new = await this.visitSpace(newClass.new, p);

            if (newClass.class) {
                draft.class = await this.visitDefined(newClass.class, p) as TypedTree;
            }

            draft.arguments = await this.visitContainer(newClass.arguments, p);

            if (newClass.body) {
                draft.body = await this.visitDefined(newClass.body, p) as J.Block;
            }

            draft.constructorType = await this.visitType(newClass.constructorType, p) as JavaType.Method | undefined;
        });
    }

    protected async visitNullableType(nullableType: J.NullableType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(nullableType, p);
        if (!expression?.kind || expression.kind !== J.Kind.NullableType) {
            return expression;
        }
        nullableType = expression as J.NullableType;

        return this.produceJava<J.NullableType>(nullableType, p, async draft => {
            draft.annotations = await mapAsync(nullableType.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.typeTree = await this.visitRightPadded(nullableType.typeTree, p);
        });
    }

    protected async visitPackage(aPackage: J.Package, p: P): Promise<J | undefined> {
        return this.produceJava<J.Package>(aPackage, p, async draft => {
            draft.expression = await this.visitDefined(aPackage.expression, p) as Expression;
            if (aPackage.annotations) {
                draft.annotations = await mapAsync(aPackage.annotations, a => this.visitDefined<J.Annotation>(a, p));
            }
        });
    }

    protected async visitParameterizedType(parameterizedType: J.ParameterizedType, p: P): Promise<J | undefined> {
        return this.produceJava<J.ParameterizedType>(parameterizedType, p, async draft => {
            draft.class = await this.visitTypeName(parameterizedType.class, p);
            draft.typeParameters = await this.visitOptionalContainer(parameterizedType.typeParameters, p);
            draft.type = await this.visitType(parameterizedType.type, p);
        });
    }

    protected async visitParentheses<T extends J>(parentheses: J.Parentheses<T>, p: P): Promise<J | undefined> {
        return this.produceJava<J.Parentheses<T>>(parentheses, p, async draft => {
            (draft.tree as J.RightPadded<J>) = await this.visitRightPadded(parentheses.tree, p);
        });
    }

    protected async visitParenthesizedTypeTree(parTypeTree: J.ParenthesizedTypeTree, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(parTypeTree, p);
        if (!expression?.kind || expression.kind !== J.Kind.ParenthesizedTypeTree) {
            return expression;
        }
        parTypeTree = expression as J.ParenthesizedTypeTree;

        return this.produceJava<J.ParenthesizedTypeTree>(parTypeTree, p, async draft => {
            draft.annotations = await mapAsync(parTypeTree.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.parenthesizedType = await this.visitDefined(parTypeTree.parenthesizedType, p) as J.Parentheses<TypeTree>;
        });
    }

    protected async visitPrimitive(primitive: J.Primitive, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(primitive, p);
        if (!expression?.kind || expression.kind !== J.Kind.Primitive) {
            return expression;
        }
        primitive = expression as J.Primitive;

        return this.produceJava<J.Primitive>(primitive, p, async draft => {
            draft.type = await this.visitType(primitive.type, p) as JavaType.Primitive;
        });
    }

    protected async visitReturn(ret: J.Return, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(ret, p);
        if (!statement?.kind || statement.kind !== J.Kind.Return) {
            return statement;
        }
        ret = statement as J.Return;

        return this.produceJava<J.Return>(ret, p, async draft => {
            if (ret.expression) {
                draft.expression = await this.visitDefined(ret.expression, p) as Expression;
            }
        });
    }

    protected async visitSwitch(aSwitch: J.Switch, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(aSwitch, p);
        if (!statement?.kind || statement.kind !== J.Kind.Switch) {
            return statement;
        }
        aSwitch = statement as J.Switch;

        return this.produceJava<J.Switch>(aSwitch, p, async draft => {
            draft.selector = await this.visitDefined(aSwitch.selector, p) as J.ControlParentheses<Expression>;
            draft.cases = await this.visitDefined(aSwitch.cases, p) as J.Block;
        });
    }

    protected async visitSwitchExpression(switchExpr: J.SwitchExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(switchExpr, p);
        if (!expression?.kind || expression.kind !== J.Kind.SwitchExpression) {
            return expression;
        }
        switchExpr = expression as J.SwitchExpression;

        return this.produceJava<J.SwitchExpression>(switchExpr, p, async draft => {
            draft.selector = await this.visitDefined(switchExpr.selector, p) as J.ControlParentheses<Expression>;
            draft.cases = await this.visitDefined(switchExpr.cases, p) as J.Block;
            draft.type = await this.visitType(switchExpr.type, p);
        });
    }

    protected async visitSynchronized(sync: J.Synchronized, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(sync, p);
        if (!statement?.kind || statement.kind !== J.Kind.Synchronized) {
            return statement;
        }
        sync = statement as J.Synchronized;

        return this.produceJava<J.Synchronized>(sync, p, async draft => {
            draft.lock = await this.visitDefined(sync.lock, p) as J.ControlParentheses<Expression>;
            draft.body = await this.visitDefined(sync.body, p) as J.Block;
        });
    }

    protected async visitTernary(ternary: J.Ternary, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(ternary, p);
        if (!expression?.kind || expression.kind !== J.Kind.Ternary) {
            return expression;
        }
        ternary = expression as J.Ternary;

        const statement = await this.visitStatement(ternary, p);
        if (!statement?.kind || statement.kind !== J.Kind.Ternary) {
            return statement;
        }
        ternary = statement as J.Ternary;

        return this.produceJava<J.Ternary>(ternary, p, async draft => {
            draft.condition = await this.visitDefined(ternary.condition, p) as Expression;
            draft.truePart = await this.visitLeftPadded(ternary.truePart, p);
            draft.falsePart = await this.visitLeftPadded(ternary.falsePart, p);
            draft.type = await this.visitType(ternary.type, p);
        });
    }

    protected async visitThrow(thrown: J.Throw, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(thrown, p);
        if (!statement?.kind || statement.kind !== J.Kind.Throw) {
            return statement;
        }
        thrown = statement as J.Throw;

        return this.produceJava<J.Throw>(thrown, p, async draft => {
            draft.exception = await this.visitDefined(thrown.exception, p) as Expression;
        });
    }

    protected async visitTry(tryable: J.Try, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(tryable, p);
        if (!statement?.kind || statement.kind !== J.Kind.Try) {
            return statement;
        }
        tryable = statement as J.Try;

        return this.produceJava<J.Try>(tryable, p, async draft => {
            draft.resources = await this.visitOptionalContainer(tryable.resources, p);
            draft.body = await this.visitDefined(tryable.body, p) as J.Block;
            draft.catches = await mapAsync(tryable.catches, c => this.visitDefined<J.Try.Catch>(c, p));
            draft.finally = await this.visitOptionalLeftPadded(tryable.finally, p);
        });
    }

    protected async visitTryResource(resource: J.Try.Resource, p: P): Promise<J | undefined> {
        return this.produceJava<J.Try.Resource>(resource, p, async draft => {
            draft.variableDeclarations = await this.visitDefined(resource.variableDeclarations, p) as TypedTree;
        });
    }

    protected async visitTryCatch(tryCatch: J.Try.Catch, p: P): Promise<J | undefined> {
        return this.produceJava<J.Try.Catch>(tryCatch, p, async draft => {
            draft.parameter = await this.visitDefined(tryCatch.parameter, p) as J.ControlParentheses<J.VariableDeclarations>;
            draft.body = await this.visitDefined(tryCatch.body, p) as J.Block;
        });
    }

    protected async visitTypeCast(typeCast: J.TypeCast, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeCast, p);
        if (!expression?.kind || expression.kind !== J.Kind.TypeCast) {
            return expression;
        }
        typeCast = expression as J.TypeCast;

        return this.produceJava<J.TypeCast>(typeCast, p, async draft => {
            draft.class = await this.visitDefined(typeCast.class, p) as J.ControlParentheses<TypedTree>;
            draft.expression = await this.visitDefined(typeCast.expression, p) as Expression;
        });
    }

    protected async visitTypeParameter(typeParam: J.TypeParameter, p: P): Promise<J | undefined> {
        return this.produceJava<J.TypeParameter>(typeParam, p, async draft => {
            draft.annotations = await mapAsync(typeParam.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.modifiers = await mapAsync(typeParam.modifiers, m => this.visitDefined<J.Modifier>(m, p));
            draft.name = await this.visitDefined(typeParam.name, p) as J.Identifier;
            draft.bounds = await this.visitOptionalContainer(typeParam.bounds, p);
        });
    }

    protected async visitTypeParameters(typeParams: J.TypeParameters, p: P): Promise<J | undefined> {
        return this.produceJava<J.TypeParameters>(typeParams, p, async draft => {
            draft.annotations = await mapAsync(typeParams.annotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.typeParameters = await mapAsync(typeParams.typeParameters, tp => this.visitRightPadded(tp, p));
        });
    }

    protected async visitUnary(unary: J.Unary, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(unary, p);
        if (!expression?.kind || expression.kind !== J.Kind.Unary) {
            return expression;
        }
        unary = expression as J.Unary;

        const statement = await this.visitStatement(unary, p);
        if (!statement?.kind || statement.kind !== J.Kind.Unary) {
            return statement;
        }
        unary = statement as J.Unary;

        return this.produceJava<J.Unary>(unary, p, async draft => {
            draft.operator = await this.visitLeftPadded(unary.operator, p);
            draft.expression = await this.visitDefined(unary.expression, p) as Expression;
            draft.type = await this.visitType(unary.type, p);
        });
    }

    protected async visitUnknown(unknown: J.Unknown, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(unknown, p);
        if (!expression?.kind || expression.kind !== J.Kind.Unknown) {
            return expression;
        }
        unknown = expression as J.Unknown;

        const statement = await this.visitStatement(unknown, p);
        if (!statement?.kind || statement.kind !== J.Kind.Unknown) {
            return statement;
        }
        unknown = statement as J.Unknown;

        return this.produceJava<J.Unknown>(unknown, p, async draft => {
            draft.source = await this.visitDefined(unknown.source, p) as J.UnknownSource;
        });
    }

    protected async visitUnknownSource(source: J.UnknownSource, p: P): Promise<J | undefined> {
        return this.produceJava<J.UnknownSource>(source, p);
    }

    protected async visitVariableDeclarations(varDecls: J.VariableDeclarations, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(varDecls, p);
        if (!statement?.kind || statement.kind !== J.Kind.VariableDeclarations) {
            return statement;
        }
        varDecls = statement as J.VariableDeclarations;

        return this.produceJava<J.VariableDeclarations>(varDecls, p, async draft => {
            draft.leadingAnnotations = await mapAsync(varDecls.leadingAnnotations, a => this.visitDefined<J.Annotation>(a, p));
            draft.modifiers = await mapAsync(varDecls.modifiers, m => this.visitDefined<J.Modifier>(m, p));

            if (varDecls.typeExpression) {
                draft.typeExpression = await this.visitDefined(varDecls.typeExpression, p) as TypedTree;
            }

            if (varDecls.varargs) {
                draft.varargs = await this.visitSpace(varDecls.varargs, p);
            }

            draft.variables = await mapAsync(varDecls.variables, v => this.visitRightPadded(v, p));
        });
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: P): Promise<J | undefined> {
        return this.produceJava<J.VariableDeclarations.NamedVariable>(variable, p, async draft => {
            draft.name = await this.visitDefined(variable.name, p) as J.Identifier;
            draft.dimensionsAfterName = await mapAsync(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, p));
            draft.initializer = await this.visitOptionalLeftPadded(variable.initializer, p);
            draft.variableType = await this.visitType(variable.variableType, p) as JavaType.Variable | undefined;
        });
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(whileLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.WhileLoop) {
            return statement;
        }
        whileLoop = statement as J.WhileLoop;

        return this.produceJava<J.WhileLoop>(whileLoop, p, async draft => {
            draft.condition = await this.visitDefined(whileLoop.condition, p) as J.ControlParentheses<Expression>;
            draft.body = await this.visitRightPadded(whileLoop.body, p);
        });
    }

    protected async visitWildcard(wildcard: J.Wildcard, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(wildcard, p);
        if (!expression?.kind || expression.kind !== J.Kind.Wildcard) {
            return expression;
        }
        wildcard = expression as J.Wildcard;

        return this.produceJava<J.Wildcard>(wildcard, p, async draft => {
            draft.bound = await this.visitOptionalLeftPadded(wildcard.bound, p);
            if (wildcard.boundedType) {
                draft.boundedType = await this.visitTypeName(wildcard.boundedType, p);
            }
        });
    }

    protected async visitYield(aYield: J.Yield, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(aYield, p);
        if (!statement?.kind || statement.kind !== J.Kind.Yield) {
            return statement;
        }
        aYield = statement as J.Yield;

        return this.produceJava<J.Yield>(aYield, p, async draft => {
            draft.value = await this.visitDefined(aYield.value, p) as Expression;
        });
    }

    protected async visitOptionalRightPadded<T extends J | boolean>(right: J.RightPadded<T> | undefined, p: P): Promise<J.RightPadded<T> | undefined> {
        return right ? this.visitRightPadded(right, p) : undefined;
    }

    protected async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: P): Promise<J.RightPadded<T>> {
        return produceAsync<J.RightPadded<T>>(right, async draft => {
            if (isTree(right.element)) {
                (draft.element as J) = await this.visitDefined(right.element, p);
            }
            draft.after = await this.visitSpace(right.after, p);
            draft.markers = await this.visitMarkers(right.markers, p);
        });
    }

    protected async visitOptionalLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T> | undefined, p: P): Promise<J.LeftPadded<T> | undefined> {
        return left ? this.visitLeftPadded(left, p) : undefined;
    }

    protected async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, p: P): Promise<J.LeftPadded<T>> {
        return produceAsync<J.LeftPadded<T>>(left, async draft => {
            draft.before = await this.visitSpace(left.before, p);
            if (isTree(left.element)) {
                draft.element = await this.visitDefined(left.element, p) as Draft<T>;
            } else if (isSpace(left.element)) {
                draft.element = await this.visitSpace(left.element, p) as Draft<T>;
            }
            draft.markers = await this.visitMarkers(left.markers, p);
        });
    }

    protected async visitOptionalContainer<T extends J>(container: J.Container<T> | undefined, p: P): Promise<J.Container<T> | undefined> {
        return container ? this.visitContainer(container, p) : undefined;
    }

    protected async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        return produceAsync<J.Container<T>>(container, async draft => {
            draft.before = await this.visitSpace(container.before, p);
            (draft.elements as J.RightPadded<J>[]) = await mapAsync(container.elements, e => this.visitRightPadded(e, p));
            draft.markers = await this.visitMarkers(container.markers, p);
        });
    }

    protected async produceJava<J2 extends J>(
        before: J | undefined,
        p: P,
        recipe?: (draft: Draft<J2>) =>
            ValidImmerRecipeReturnType<Draft<J2>> |
            PromiseLike<ValidImmerRecipeReturnType<Draft<J2>>>
    ): Promise<J2> {
        const draft: Draft<J2> = createDraft(before as J2);
        (draft as Draft<J>).prefix = await this.visitSpace(before!.prefix, p);
        (draft as Draft<J>).markers = await this.visitMarkers(before!.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft(draft) as J2;
    }

    protected accept(t: J, p: P): Promise<J | undefined> {
        switch (t.kind) {
            case J.Kind.AnnotatedType:
                return this.visitAnnotatedType(t as J.AnnotatedType, p);
            case J.Kind.Annotation:
                return this.visitAnnotation(t as J.Annotation, p);
            case J.Kind.ArrayAccess:
                return this.visitArrayAccess(t as J.ArrayAccess, p);
            case J.Kind.ArrayDimension:
                return this.visitArrayDimension(t as J.ArrayDimension, p);
            case J.Kind.ArrayType:
                return this.visitArrayType(t as J.ArrayType, p);
            case J.Kind.Assert:
                return this.visitAssert(t as J.Assert, p);
            case J.Kind.Assignment:
                return this.visitAssignment(t as J.Assignment, p);
            case J.Kind.AssignmentOperation:
                return this.visitAssignmentOperation(t as J.AssignmentOperation, p);
            case J.Kind.Binary:
                return this.visitBinary(t as J.Binary, p);
            case J.Kind.Block:
                return this.visitBlock(t as J.Block, p);
            case J.Kind.Break:
                return this.visitBreak(t as J.Break, p);
            case J.Kind.Case:
                return this.visitCase(t as J.Case, p);
            case J.Kind.ClassDeclaration:
                return this.visitClassDeclaration(t as J.ClassDeclaration, p);
            case J.Kind.ClassDeclarationKind:
                return this.visitClassDeclarationKind(t as J.ClassDeclaration.Kind, p);
            case J.Kind.CompilationUnit:
                return this.visitCompilationUnit(t as J.CompilationUnit, p);
            case J.Kind.Continue:
                return this.visitContinue(t as J.Continue, p);
            case J.Kind.ControlParentheses:
                return this.visitControlParentheses(t as J.ControlParentheses<J>, p);
            case J.Kind.DeconstructionPattern:
                return this.visitDeconstructionPattern(t as J.DeconstructionPattern, p);
            case J.Kind.DoWhileLoop:
                return this.visitDoWhileLoop(t as J.DoWhileLoop, p);
            case J.Kind.Empty:
                return this.visitEmpty(t as J.Empty, p);
            case J.Kind.EnumValue:
                return this.visitEnumValue(t as J.EnumValue, p);
            case J.Kind.EnumValueSet:
                return this.visitEnumValueSet(t as J.EnumValueSet, p);
            case J.Kind.Erroneous:
                return this.visitErroneous(t as J.Erroneous, p);
            case J.Kind.FieldAccess:
                return this.visitFieldAccess(t as J.FieldAccess, p);
            case J.Kind.ForEachLoop:
                return this.visitForEachLoop(t as J.ForEachLoop, p);
            case J.Kind.ForEachLoopControl:
                return this.visitForEachLoopControl(t as J.ForEachLoop.Control, p);
            case J.Kind.ForLoop:
                return this.visitForLoop(t as J.ForLoop, p);
            case J.Kind.ForLoopControl:
                return this.visitForLoopControl(t as J.ForLoop.Control, p);
            case J.Kind.Identifier:
                return this.visitIdentifier(t as J.Identifier, p);
            case J.Kind.If:
                return this.visitIf(t as J.If, p);
            case J.Kind.IfElse:
                return this.visitElse(t as J.If.Else, p);
            case J.Kind.Import:
                return this.visitImport(t as J.Import, p);
            case J.Kind.InstanceOf:
                return this.visitInstanceOf(t as J.InstanceOf, p);
            case J.Kind.IntersectionType:
                return this.visitIntersectionType(t as J.IntersectionType, p);
            case J.Kind.Label:
                return this.visitLabel(t as J.Label, p);
            case J.Kind.Lambda:
                return this.visitLambda(t as J.Lambda, p);
            case J.Kind.LambdaParameters:
                return this.visitLambdaParameters(t as J.Lambda.Parameters, p);
            case J.Kind.Literal:
                return this.visitLiteral(t as J.Literal, p);
            case J.Kind.MemberReference:
                return this.visitMemberReference(t as J.MemberReference, p);
            case J.Kind.MethodDeclaration:
                return this.visitMethodDeclaration(t as J.MethodDeclaration, p);
            case J.Kind.MethodInvocation:
                return this.visitMethodInvocation(t as J.MethodInvocation, p);
            case J.Kind.Modifier:
                return this.visitModifier(t as J.Modifier, p);
            case J.Kind.MultiCatch:
                return this.visitMultiCatch(t as J.MultiCatch, p);
            case J.Kind.NewArray:
                return this.visitNewArray(t as J.NewArray, p);
            case J.Kind.NewClass:
                return this.visitNewClass(t as J.NewClass, p);
            case J.Kind.NullableType:
                return this.visitNullableType(t as J.NullableType, p);
            case J.Kind.Package:
                return this.visitPackage(t as J.Package, p);
            case J.Kind.ParameterizedType:
                return this.visitParameterizedType(t as J.ParameterizedType, p);
            case J.Kind.Parentheses:
                return this.visitParentheses(t as J.Parentheses<J>, p);
            case J.Kind.ParenthesizedTypeTree:
                return this.visitParenthesizedTypeTree(t as J.ParenthesizedTypeTree, p);
            case J.Kind.Primitive:
                return this.visitPrimitive(t as J.Primitive, p);
            case J.Kind.Return:
                return this.visitReturn(t as J.Return, p);
            case J.Kind.Switch:
                return this.visitSwitch(t as J.Switch, p);
            case J.Kind.SwitchExpression:
                return this.visitSwitchExpression(t as J.SwitchExpression, p);
            case J.Kind.Synchronized:
                return this.visitSynchronized(t as J.Synchronized, p);
            case J.Kind.Ternary:
                return this.visitTernary(t as J.Ternary, p);
            case J.Kind.Throw:
                return this.visitThrow(t as J.Throw, p);
            case J.Kind.Try:
                return this.visitTry(t as J.Try, p);
            case J.Kind.TryResource:
                return this.visitTryResource(t as J.Try.Resource, p);
            case J.Kind.TryCatch:
                return this.visitTryCatch(t as J.Try.Catch, p);
            case J.Kind.TypeCast:
                return this.visitTypeCast(t as J.TypeCast, p);
            case J.Kind.TypeParameter:
                return this.visitTypeParameter(t as J.TypeParameter, p);
            case J.Kind.TypeParameters:
                return this.visitTypeParameters(t as J.TypeParameters, p);
            case J.Kind.Unary:
                return this.visitUnary(t as J.Unary, p);
            case J.Kind.Unknown:
                return this.visitUnknown(t as J.Unknown, p);
            case J.Kind.UnknownSource:
                return this.visitUnknownSource(t as J.UnknownSource, p);
            case J.Kind.VariableDeclarations:
                return this.visitVariableDeclarations(t as J.VariableDeclarations, p);
            case J.Kind.NamedVariable:
                return this.visitVariable(t as J.VariableDeclarations.NamedVariable, p);
            case J.Kind.WhileLoop:
                return this.visitWhileLoop(t as J.WhileLoop, p);
            case J.Kind.Wildcard:
                return this.visitWildcard(t as J.Wildcard, p);
            case J.Kind.Yield:
                return this.visitYield(t as J.Yield, p);
            default:
                return Promise.resolve(t);
        }
    }
}
