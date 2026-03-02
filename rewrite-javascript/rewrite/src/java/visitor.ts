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
import {Cursor, isTree, SourceFile} from "../tree";
import {mapAsync, updateIfChanged} from "../util";
import {TreeVisitor, ValidRecipeReturnType} from "../visitor";
import {emptySpace, Expression, isSpace, J, NameTree, Statement, TypedTree, TypeTree} from "./tree";
import {emptyMarkers, Markers} from "../markers";
import {create, Draft, rawReturn} from "mutative";
import {Type} from "./type";

const javaKindValues = new Set(Object.values(J.Kind));

const extendedJavaKinds = new Map<string, <P>(visitor: JavaVisitor<P>) => JavaVisitor<P>>();

/**
 * Register additional kind values for interfaces that extend J.
 * This allows isJava to recognize implementations of those interfaces.
 * @param kinds - Array of kind values to register
 * @param adapter - Adapter function to transform a JavaVisitor to, for example, a JavaScriptVisitor
 */
export function registerJavaExtensionKinds(
    kinds: readonly string[],
    adapter: <P>(visitor: JavaVisitor<P>) => JavaVisitor<P>
): void {
    for (const kind of kinds) {
        extendedJavaKinds.set(kind, adapter);
    }
}

export function isJava(tree: any): tree is J {
    return javaKindValues.has(tree["kind"]) || extendedJavaKinds.has(tree["kind"]);
}

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
    public async visitSpace(space: J.Space, p: P): Promise<J.Space> {
        return space;
    }

    // noinspection JSUnusedLocalSymbols
    protected async visitType(javaType: Type | undefined, p: P): Promise<Type | undefined> {
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

        const updates = {
            prefix: await this.visitSpace(annotatedType.prefix, p),
            markers: await this.visitMarkers(annotatedType.markers, p),
            annotations: await mapAsync(annotatedType.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            typeExpression: await this.visitDefined(annotatedType.typeExpression, p) as TypedTree
        };
        return updateIfChanged(annotatedType, updates);
    }

    protected async visitAnnotation(annotation: J.Annotation, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(annotation, p);
        if (!expression?.kind || expression.kind !== J.Kind.Annotation) {
            return expression;
        }
        annotation = expression as J.Annotation;

        const updates = {
            prefix: await this.visitSpace(annotation.prefix, p),
            markers: await this.visitMarkers(annotation.markers, p),
            annotationType: await this.visitTypeName(annotation.annotationType, p),
            arguments: await this.visitOptionalContainer(annotation.arguments, p)
        };
        return updateIfChanged(annotation, updates);
    }

    protected async visitArrayAccess(arrayAccess: J.ArrayAccess, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(arrayAccess, p);
        if (!expression?.kind || expression.kind !== J.Kind.ArrayAccess) {
            return expression;
        }
        arrayAccess = expression as J.ArrayAccess;

        const updates = {
            prefix: await this.visitSpace(arrayAccess.prefix, p),
            markers: await this.visitMarkers(arrayAccess.markers, p),
            indexed: await this.visitDefined(arrayAccess.indexed, p) as Expression,
            dimension: await this.visitDefined(arrayAccess.dimension, p) as J.ArrayDimension
        };
        return updateIfChanged(arrayAccess, updates);
    }

    protected async visitArrayDimension(arrayDimension: J.ArrayDimension, p: P): Promise<J | undefined> {
        const updates = {
            prefix: await this.visitSpace(arrayDimension.prefix, p),
            markers: await this.visitMarkers(arrayDimension.markers, p),
            index: await this.visitRightPadded(arrayDimension.index, p)
        };
        return updateIfChanged(arrayDimension, updates);
    }


    protected async visitArrayType(arrayType: J.ArrayType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(arrayType, p);
        if (!expression?.kind || expression.kind !== J.Kind.ArrayType) {
            return expression;
        }
        arrayType = expression as J.ArrayType;

        const updates: any = {
            prefix: await this.visitSpace(arrayType.prefix, p),
            markers: await this.visitMarkers(arrayType.markers, p),
            elementType: await this.visitDefined(arrayType.elementType, p) as TypedTree,
            dimension: await this.visitLeftPadded(arrayType.dimension, p),
            type: await this.visitType(arrayType.type, p)
        };
        if (arrayType.annotations) {
            updates.annotations = await mapAsync(arrayType.annotations, a => this.visitDefined<J.Annotation>(a, p));
        }
        return updateIfChanged(arrayType, updates);
    }

    protected async visitAssert(anAssert: J.Assert, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(anAssert, p);
        if (!statement?.kind || statement.kind !== J.Kind.Assert) {
            return statement;
        }
        anAssert = statement as J.Assert;

        const updates = {
            prefix: await this.visitSpace(anAssert.prefix, p),
            markers: await this.visitMarkers(anAssert.markers, p),
            condition: await this.visitDefined(anAssert.condition, p) as Expression,
            detail: await this.visitOptionalLeftPadded(anAssert.detail, p)
        };
        return updateIfChanged(anAssert, updates);
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

        const updates = {
            prefix: await this.visitSpace(assignment.prefix, p),
            markers: await this.visitMarkers(assignment.markers, p),
            variable: await this.visitDefined(assignment.variable, p) as Expression,
            assignment: await this.visitLeftPadded(assignment.assignment, p),
            type: await this.visitType(assignment.type, p)
        };
        return updateIfChanged(assignment, updates);
    }

    protected async visitAssignmentOperation(assignOp: J.AssignmentOperation, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(assignOp, p);
        if (!expression?.kind || expression.kind !== J.Kind.AssignmentOperation) {
            return expression;
        }
        assignOp = expression as J.AssignmentOperation;

        const updates = {
            prefix: await this.visitSpace(assignOp.prefix, p),
            markers: await this.visitMarkers(assignOp.markers, p),
            variable: await this.visitDefined(assignOp.variable, p) as Expression,
            operator: await this.visitLeftPadded(assignOp.operator, p) as typeof assignOp.operator,
            assignment: await this.visitDefined(assignOp.assignment, p) as Expression,
            type: await this.visitType(assignOp.type, p)
        };
        return updateIfChanged(assignOp, updates);
    }

    protected async visitBinary(binary: J.Binary, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(binary, p);
        if (!expression?.kind || expression.kind !== J.Kind.Binary) {
            return expression;
        }
        binary = expression as J.Binary;

        const updates = {
            prefix: await this.visitSpace(binary.prefix, p),
            markers: await this.visitMarkers(binary.markers, p),
            left: await this.visitDefined(binary.left, p) as Expression,
            operator: await this.visitLeftPadded(binary.operator, p) as typeof binary.operator,
            right: await this.visitDefined(binary.right, p) as Expression,
            type: await this.visitType(binary.type, p)
        };
        return updateIfChanged(binary, updates);
    }

    protected async visitBlock(block: J.Block, p: P): Promise<J | undefined> {
        const updates = {
            prefix: await this.visitSpace(block.prefix, p),
            markers: await this.visitMarkers(block.markers, p),
            static: await this.visitRightPadded(block.static, p) as typeof block.static,
            statements: await mapAsync(block.statements, stmt => this.visitRightPadded(stmt, p)),
            end: await this.visitSpace(block.end, p)
        };
        return updateIfChanged(block, updates);
    }

    protected async visitBreak(breakStatement: J.Break, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(breakStatement, p);
        if (!statement?.kind || statement.kind !== J.Kind.Break) {
            return statement;
        }
        breakStatement = statement as J.Break;

        const updates: any = {
            prefix: await this.visitSpace(breakStatement.prefix, p),
            markers: await this.visitMarkers(breakStatement.markers, p)
        };
        if (breakStatement.label) {
            updates.label = await this.visitDefined(breakStatement.label, p) as J.Identifier;
        }
        return updateIfChanged(breakStatement, updates);
    }

    protected async visitCase(aCase: J.Case, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(aCase, p);
        if (!statement?.kind || statement.kind !== J.Kind.Case) {
            return statement;
        }
        aCase = statement as J.Case;

        const updates: any = {
            prefix: await this.visitSpace(aCase.prefix, p),
            markers: await this.visitMarkers(aCase.markers, p),
            caseLabels: await this.visitContainer(aCase.caseLabels, p),
            statements: await this.visitContainer(aCase.statements, p),
            body: await this.visitOptionalRightPadded(aCase.body, p)
        };
        if (aCase.guard) {
            updates.guard = await this.visitDefined(aCase.guard, p) as Expression;
        }
        return updateIfChanged(aCase, updates);
    }

    protected async visitClassDeclaration(classDecl: J.ClassDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(classDecl, p);
        if (!statement?.kind || statement.kind !== J.Kind.ClassDeclaration) {
            return statement;
        }
        classDecl = statement as J.ClassDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(classDecl.prefix, p),
            markers: await this.visitMarkers(classDecl.markers, p),
            leadingAnnotations: await mapAsync(classDecl.leadingAnnotations, a => this.visitDefined<J.Annotation>(a, p)),
            modifiers: await mapAsync(classDecl.modifiers, m => this.visitDefined<J.Modifier>(m, p)),
            classKind: await this.visitDefined(classDecl.classKind, p) as J.ClassDeclaration.Kind,
            name: await this.visitDefined(classDecl.name, p) as J.Identifier,
            typeParameters: await this.visitOptionalContainer(classDecl.typeParameters, p),
            primaryConstructor: await this.visitOptionalContainer(classDecl.primaryConstructor, p),
            extends: await this.visitOptionalLeftPadded(classDecl.extends, p),
            implements: await this.visitOptionalContainer(classDecl.implements, p),
            permitting: await this.visitOptionalContainer(classDecl.permitting, p),
            body: await this.visitDefined(classDecl.body, p) as J.Block,
            type: await this.visitType(classDecl.type, p) as Type.Class | undefined
        };
        return updateIfChanged(classDecl, updates);
    }

    protected async visitClassDeclarationKind(kind: J.ClassDeclaration.Kind, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(kind.prefix, p),
            markers: await this.visitMarkers(kind.markers, p),
            annotations: await mapAsync(kind.annotations, a => this.visitDefined<J.Annotation>(a, p))
        };
        return updateIfChanged(kind, updates);
    }

    protected async visitCompilationUnit(cu: J.CompilationUnit, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(cu.prefix, p),
            markers: await this.visitMarkers(cu.markers, p),
            packageDeclaration: await this.visitRightPadded(cu.packageDeclaration, p) as J.RightPadded<J.Package>,
            imports: await mapAsync(cu.imports, imp => this.visitRightPadded(imp, p)),
            classes: await mapAsync(cu.classes, cls => this.visitDefined(cls, p) as Promise<J.ClassDeclaration>),
            eof: await this.visitSpace(cu.eof, p)
        };
        return updateIfChanged(cu, updates);
    }

    protected async visitContinue(continueStatement: J.Continue, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(continueStatement, p);
        if (!statement?.kind || statement.kind !== J.Kind.Continue) {
            return statement;
        }
        continueStatement = statement as J.Continue;

        const updates: any = {
            prefix: await this.visitSpace(continueStatement.prefix, p),
            markers: await this.visitMarkers(continueStatement.markers, p)
        };
        if (continueStatement.label) {
            updates.label = await this.visitDefined(continueStatement.label, p) as J.Identifier;
        }
        return updateIfChanged(continueStatement, updates);
    }

    protected async visitControlParentheses<T extends J>(controlParens: J.ControlParentheses<T>, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(controlParens.prefix, p),
            markers: await this.visitMarkers(controlParens.markers, p),
            tree: await this.visitRightPadded(controlParens.tree, p)
        };
        return updateIfChanged(controlParens, updates);
    }

    protected async visitDeconstructionPattern(pattern: J.DeconstructionPattern, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(pattern.prefix, p),
            markers: await this.visitMarkers(pattern.markers, p),
            deconstructor: await this.visitDefined(pattern.deconstructor, p) as Expression,
            nested: await this.visitContainer(pattern.nested, p),
            type: await this.visitType(pattern.type, p)
        };
        return updateIfChanged(pattern, updates);
    }

    protected async visitDoWhileLoop(doWhileLoop: J.DoWhileLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(doWhileLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.DoWhileLoop) {
            return statement;
        }
        doWhileLoop = statement as J.DoWhileLoop;

        const updates: any = {
            prefix: await this.visitSpace(doWhileLoop.prefix, p),
            markers: await this.visitMarkers(doWhileLoop.markers, p),
            body: await this.visitRightPadded(doWhileLoop.body, p),
            whileCondition: await this.visitLeftPadded(doWhileLoop.whileCondition, p)
        };
        return updateIfChanged(doWhileLoop, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(empty.prefix, p),
            markers: await this.visitMarkers(empty.markers, p)
        };
        return updateIfChanged(empty, updates);
    }

    protected async visitEnumValue(enumValue: J.EnumValue, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(enumValue.prefix, p),
            markers: await this.visitMarkers(enumValue.markers, p),
            annotations: await mapAsync(enumValue.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            name: await this.visitDefined(enumValue.name, p) as J.Identifier
        };
        if (enumValue.initializer) {
            updates.initializer = await this.visitDefined(enumValue.initializer, p) as J.NewClass;
        }
        return updateIfChanged(enumValue, updates);
    }

    protected async visitEnumValueSet(enumValueSet: J.EnumValueSet, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(enumValueSet, p);
        if (!statement?.kind || statement.kind !== J.Kind.EnumValueSet) {
            return statement;
        }
        enumValueSet = statement as J.EnumValueSet;

        const updates: any = {
            prefix: await this.visitSpace(enumValueSet.prefix, p),
            markers: await this.visitMarkers(enumValueSet.markers, p),
            enums: await mapAsync(enumValueSet.enums, e => this.visitRightPadded(e, p))
        };
        return updateIfChanged(enumValueSet, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(erroneous.prefix, p),
            markers: await this.visitMarkers(erroneous.markers, p)
        };
        return updateIfChanged(erroneous, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(fieldAccess.prefix, p),
            markers: await this.visitMarkers(fieldAccess.markers, p),
            target: await this.visitDefined(fieldAccess.target, p) as Expression,
            name: await this.visitLeftPadded(fieldAccess.name, p),
            type: await this.visitType(fieldAccess.type, p)
        };
        return updateIfChanged(fieldAccess, updates);
    }

    protected async visitForEachLoop(forLoop: J.ForEachLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(forLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.ForEachLoop) {
            return statement;
        }
        forLoop = statement as J.ForEachLoop;

        const updates: any = {
            prefix: await this.visitSpace(forLoop.prefix, p),
            markers: await this.visitMarkers(forLoop.markers, p),
            control: await this.visitDefined(forLoop.control, p) as J.ForEachLoop.Control,
            body: await this.visitRightPadded(forLoop.body, p)
        };
        return updateIfChanged(forLoop, updates);
    }

    protected async visitForEachLoopControl(control: J.ForEachLoop.Control, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(control.prefix, p),
            markers: await this.visitMarkers(control.markers, p),
            variable: await this.visitRightPadded(control.variable, p),
            iterable: await this.visitRightPadded(control.iterable, p)
        };
        return updateIfChanged(control, updates);
    }

    protected async visitForLoop(forLoop: J.ForLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(forLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.ForLoop) {
            return statement;
        }
        forLoop = statement as J.ForLoop;

        const updates: any = {
            prefix: await this.visitSpace(forLoop.prefix, p),
            markers: await this.visitMarkers(forLoop.markers, p),
            control: await this.visitDefined(forLoop.control, p) as J.ForLoop.Control,
            body: await this.visitRightPadded(forLoop.body, p)
        };
        return updateIfChanged(forLoop, updates);
    }

    protected async visitForLoopControl(control: J.ForLoop.Control, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(control.prefix, p),
            markers: await this.visitMarkers(control.markers, p),
            init: await mapAsync(control.init, i => this.visitRightPadded(i, p)),
            condition: await this.visitOptionalRightPadded(control.condition, p),
            update: await mapAsync(control.update, u => this.visitRightPadded(u, p))
        };
        return updateIfChanged(control, updates);
    }

    protected async visitIdentifier(ident: J.Identifier, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(ident, p);
        if (!expression?.kind || expression.kind !== J.Kind.Identifier) {
            return expression;
        }
        ident = expression as J.Identifier;

        const updates: any = {
            prefix: await this.visitSpace(ident.prefix, p),
            markers: await this.visitMarkers(ident.markers, p),
            annotations: await mapAsync(ident.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            type: await this.visitType(ident.type, p),
            fieldType: await this.visitType(ident.fieldType, p) as Type.Variable | undefined
        };
        return updateIfChanged(ident, updates);
    }

    protected async visitIf(iff: J.If, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(iff, p);
        if (!statement?.kind || statement.kind !== J.Kind.If) {
            return statement;
        }
        iff = statement as J.If;

        const updates: any = {
            prefix: await this.visitSpace(iff.prefix, p),
            markers: await this.visitMarkers(iff.markers, p),
            ifCondition: await this.visitDefined(iff.ifCondition, p) as J.ControlParentheses<Expression>,
            thenPart: await this.visitRightPadded(iff.thenPart, p)
        };
        if (iff.elsePart) {
            updates.elsePart = await this.visitDefined(iff.elsePart, p) as J.If.Else;
        }
        return updateIfChanged(iff, updates);
    }

    protected async visitElse(anElse: J.If.Else, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(anElse.prefix, p),
            markers: await this.visitMarkers(anElse.markers, p),
            body: await this.visitRightPadded(anElse.body, p)
        };
        return updateIfChanged(anElse, updates);
    }

    protected async visitImport(anImport: J.Import, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(anImport.prefix, p),
            markers: await this.visitMarkers(anImport.markers, p),
            static: await this.visitLeftPadded(anImport.static, p),
            qualid: await this.visitDefined(anImport.qualid, p) as J.FieldAccess,
            alias: await this.visitOptionalLeftPadded(anImport.alias, p)
        };
        return updateIfChanged(anImport, updates);
    }

    protected async visitInstanceOf(instanceOf: J.InstanceOf, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(instanceOf, p);
        if (!expression?.kind || expression.kind !== J.Kind.InstanceOf) {
            return expression;
        }
        instanceOf = expression as J.InstanceOf;

        const updates: any = {
            prefix: await this.visitSpace(instanceOf.prefix, p),
            markers: await this.visitMarkers(instanceOf.markers, p),
            expression: await this.visitRightPadded(instanceOf.expression, p),
            class: await this.visitDefined(instanceOf.class, p) as J,
            type: await this.visitType(instanceOf.type, p)
        };
        if (instanceOf.pattern) {
            updates.pattern = await this.visitDefined(instanceOf.pattern, p) as J;
        }
        if (instanceOf.modifier) {
            updates.modifier = await this.visitDefined(instanceOf.modifier, p) as J.Modifier;
        }
        return updateIfChanged(instanceOf, updates);
    }

    protected async visitIntersectionType(intersectionType: J.IntersectionType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(intersectionType, p);
        if (!expression?.kind || expression.kind !== J.Kind.IntersectionType) {
            return expression;
        }
        intersectionType = expression as J.IntersectionType;

        const updates: any = {
            prefix: await this.visitSpace(intersectionType.prefix, p),
            markers: await this.visitMarkers(intersectionType.markers, p),
            bounds: await this.visitContainer(intersectionType.bounds, p)
        };
        return updateIfChanged(intersectionType, updates);
    }

    protected async visitLabel(label: J.Label, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(label, p);
        if (!statement?.kind || statement.kind !== J.Kind.Label) {
            return statement;
        }
        label = statement as J.Label;

        const updates: any = {
            prefix: await this.visitSpace(label.prefix, p),
            markers: await this.visitMarkers(label.markers, p),
            label: await this.visitRightPadded(label.label, p),
            statement: await this.visitDefined(label.statement, p) as Statement
        };
        return updateIfChanged(label, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(lambda.prefix, p),
            markers: await this.visitMarkers(lambda.markers, p),
            parameters: await this.visitDefined(lambda.parameters, p) as J.Lambda.Parameters,
            arrow: await this.visitSpace(lambda.arrow, p),
            body: await this.visitDefined(lambda.body, p) as Statement | Expression,
            type: await this.visitType(lambda.type, p)
        };
        return updateIfChanged(lambda, updates);
    }

    protected async visitLambdaParameters(params: J.Lambda.Parameters, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(params.prefix, p),
            markers: await this.visitMarkers(params.markers, p),
            parameters: await mapAsync(params.parameters, param => this.visitRightPadded(param, p))
        };
        return updateIfChanged(params, updates);
    }

    protected async visitLiteral(literal: J.Literal, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(literal, p);
        if (!expression?.kind || expression.kind !== J.Kind.Literal) {
            return expression;
        }
        literal = expression as J.Literal;

        const updates: any = {
            prefix: await this.visitSpace(literal.prefix, p),
            markers: await this.visitMarkers(literal.markers, p),
            type: await this.visitType(literal.type, p) as Type.Primitive | undefined
        };
        return updateIfChanged(literal, updates);
    }

    protected async visitMemberReference(memberRef: J.MemberReference, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(memberRef, p);
        if (!expression?.kind || expression.kind !== J.Kind.MemberReference) {
            return expression;
        }
        memberRef = expression as J.MemberReference;

        const updates: any = {
            prefix: await this.visitSpace(memberRef.prefix, p),
            markers: await this.visitMarkers(memberRef.markers, p),
            containing: await this.visitRightPadded(memberRef.containing, p),
            typeParameters: await this.visitOptionalContainer(memberRef.typeParameters, p),
            reference: await this.visitLeftPadded(memberRef.reference, p),
            type: await this.visitType(memberRef.type, p),
            methodType: await this.visitType(memberRef.methodType, p) as Type.Method | undefined,
            variableType: await this.visitType(memberRef.variableType, p) as Type.Variable | undefined
        };
        return updateIfChanged(memberRef, updates);
    }

    protected async visitMethodDeclaration(method: J.MethodDeclaration, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(method, p);
        if (!statement?.kind || statement.kind !== J.Kind.MethodDeclaration) {
            return statement;
        }
        method = statement as J.MethodDeclaration;

        const updates: any = {
            prefix: await this.visitSpace(method.prefix, p),
            markers: await this.visitMarkers(method.markers, p),
            leadingAnnotations: await mapAsync(method.leadingAnnotations, a => this.visitDefined<J.Annotation>(a, p)),
            modifiers: await mapAsync(method.modifiers, m => this.visitDefined<J.Modifier>(m, p)),
            nameAnnotations: await mapAsync(method.nameAnnotations, a => this.visitDefined<J.Annotation>(a, p)),
            name: await this.visitDefined(method.name, p),
            parameters: await this.visitContainer(method.parameters, p),
            throws: method.throws && await this.visitContainer(method.throws, p),
            body: method.body && await this.visitDefined(method.body, p) as J.Block,
            defaultValue: await this.visitOptionalLeftPadded(method.defaultValue, p),
            methodType: await this.visitType(method.methodType, p) as Type.Method | undefined
        };
        if (method.typeParameters) {
            updates.typeParameters = await this.visitDefined(method.typeParameters, p) as J.TypeParameters;
        }
        if (method.returnTypeExpression) {
            updates.returnTypeExpression = await this.visitDefined(method.returnTypeExpression, p) as TypedTree;
        }
        return updateIfChanged(method, updates);
    }

    protected async visitMethodInvocation(method: J.MethodInvocation, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(method, p);
        if (!expression?.kind || expression.kind !== J.Kind.MethodInvocation) {
            return expression;
        }
        method = expression as J.MethodInvocation;

        const statement = await this.visitStatement(method, p);
        if (!statement?.kind || statement.kind !== J.Kind.MethodInvocation) {
            return statement;
        }
        method = statement as J.MethodInvocation;

        const updates: any = {
            prefix: await this.visitSpace(method.prefix, p),
            markers: await this.visitMarkers(method.markers, p),
            select: await this.visitOptionalRightPadded(method.select, p),
            typeParameters: await this.visitOptionalContainer(method.typeParameters, p),
            name: await this.visitDefined(method.name, p) as J.Identifier,
            arguments: await this.visitContainer(method.arguments, p),
            methodType: await this.visitType(method.methodType, p) as Type.Method | undefined
        };
        return updateIfChanged(method, updates);
    }

    protected async visitModifier(modifier: J.Modifier, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(modifier.prefix, p),
            markers: await this.visitMarkers(modifier.markers, p),
            annotations: await mapAsync(modifier.annotations, a => this.visitDefined<J.Annotation>(a, p))
        };
        return updateIfChanged(modifier, updates);
    }

    protected async visitMultiCatch(multiCatch: J.MultiCatch, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(multiCatch.prefix, p),
            markers: await this.visitMarkers(multiCatch.markers, p),
            alternatives: await mapAsync(multiCatch.alternatives, alt => this.visitRightPadded(alt, p))
        };
        return updateIfChanged(multiCatch, updates);
    }

    protected async visitNewArray(newArray: J.NewArray, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(newArray, p);
        if (!expression?.kind || expression.kind !== J.Kind.NewArray) {
            return expression;
        }
        newArray = expression as J.NewArray;

        const updates: any = {
            prefix: await this.visitSpace(newArray.prefix, p),
            markers: await this.visitMarkers(newArray.markers, p),
            dimensions: await mapAsync(newArray.dimensions, dim => this.visitDefined<J.ArrayDimension>(dim, p)),
            initializer: await this.visitOptionalContainer(newArray.initializer, p),
            type: await this.visitType(newArray.type, p)
        };
        if (newArray.typeExpression) {
            updates.typeExpression = await this.visitDefined(newArray.typeExpression, p) as TypedTree;
        }
        return updateIfChanged(newArray, updates);
    }

    protected async visitNewClass(newClass: J.NewClass, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(newClass, p);
        if (!expression?.kind || expression.kind !== J.Kind.NewClass) {
            return expression;
        }
        newClass = expression as J.NewClass;

        const updates: any = {
            prefix: await this.visitSpace(newClass.prefix, p),
            markers: await this.visitMarkers(newClass.markers, p),
            new: await this.visitSpace(newClass.new, p),
            arguments: await this.visitContainer(newClass.arguments, p),
            constructorType: await this.visitType(newClass.constructorType, p) as Type.Method | undefined
        };
        if (newClass.enclosing) {
            updates.enclosing = await this.visitRightPadded(newClass.enclosing, p);
        }
        if (newClass.class) {
            updates.class = await this.visitDefined(newClass.class, p) as TypedTree;
        }
        if (newClass.body) {
            updates.body = await this.visitDefined(newClass.body, p) as J.Block;
        }
        return updateIfChanged(newClass, updates);
    }

    protected async visitNullableType(nullableType: J.NullableType, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(nullableType, p);
        if (!expression?.kind || expression.kind !== J.Kind.NullableType) {
            return expression;
        }
        nullableType = expression as J.NullableType;

        const updates: any = {
            prefix: await this.visitSpace(nullableType.prefix, p),
            markers: await this.visitMarkers(nullableType.markers, p),
            annotations: await mapAsync(nullableType.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            typeTree: await this.visitRightPadded(nullableType.typeTree, p)
        };
        return updateIfChanged(nullableType, updates);
    }

    protected async visitPackage(aPackage: J.Package, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(aPackage.prefix, p),
            markers: await this.visitMarkers(aPackage.markers, p),
            expression: await this.visitDefined(aPackage.expression, p) as Expression
        };
        if (aPackage.annotations) {
            updates.annotations = await mapAsync(aPackage.annotations, a => this.visitDefined<J.Annotation>(a, p));
        }
        return updateIfChanged(aPackage, updates);
    }

    protected async visitParameterizedType(parameterizedType: J.ParameterizedType, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(parameterizedType.prefix, p),
            markers: await this.visitMarkers(parameterizedType.markers, p),
            class: await this.visitTypeName(parameterizedType.class, p),
            typeParameters: await this.visitOptionalContainer(parameterizedType.typeParameters, p),
            type: await this.visitType(parameterizedType.type, p)
        };
        return updateIfChanged(parameterizedType, updates);
    }

    protected async visitParentheses<T extends J>(parentheses: J.Parentheses<T>, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(parentheses.prefix, p),
            markers: await this.visitMarkers(parentheses.markers, p),
            tree: await this.visitRightPadded(parentheses.tree, p)
        };
        return updateIfChanged(parentheses, updates);
    }

    protected async visitParenthesizedTypeTree(parTypeTree: J.ParenthesizedTypeTree, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(parTypeTree, p);
        if (!expression?.kind || expression.kind !== J.Kind.ParenthesizedTypeTree) {
            return expression;
        }
        parTypeTree = expression as J.ParenthesizedTypeTree;

        const updates: any = {
            prefix: await this.visitSpace(parTypeTree.prefix, p),
            markers: await this.visitMarkers(parTypeTree.markers, p),
            annotations: await mapAsync(parTypeTree.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            parenthesizedType: await this.visitDefined(parTypeTree.parenthesizedType, p) as J.Parentheses<TypeTree>
        };
        return updateIfChanged(parTypeTree, updates);
    }

    protected async visitPrimitive(primitive: J.Primitive, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(primitive, p);
        if (!expression?.kind || expression.kind !== J.Kind.Primitive) {
            return expression;
        }
        primitive = expression as J.Primitive;

        const updates: any = {
            prefix: await this.visitSpace(primitive.prefix, p),
            markers: await this.visitMarkers(primitive.markers, p),
            type: await this.visitType(primitive.type, p) as Type.Primitive
        };
        return updateIfChanged(primitive, updates);
    }

    protected async visitReturn(ret: J.Return, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(ret, p);
        if (!statement?.kind || statement.kind !== J.Kind.Return) {
            return statement;
        }
        ret = statement as J.Return;

        const updates: any = {
            prefix: await this.visitSpace(ret.prefix, p),
            markers: await this.visitMarkers(ret.markers, p)
        };
        if (ret.expression) {
            updates.expression = await this.visitDefined(ret.expression, p) as Expression;
        }
        return updateIfChanged(ret, updates);
    }

    protected async visitSwitch(aSwitch: J.Switch, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(aSwitch, p);
        if (!statement?.kind || statement.kind !== J.Kind.Switch) {
            return statement;
        }
        aSwitch = statement as J.Switch;

        const updates: any = {
            prefix: await this.visitSpace(aSwitch.prefix, p),
            markers: await this.visitMarkers(aSwitch.markers, p),
            selector: await this.visitDefined(aSwitch.selector, p) as J.ControlParentheses<Expression>,
            cases: await this.visitDefined(aSwitch.cases, p) as J.Block
        };
        return updateIfChanged(aSwitch, updates);
    }

    protected async visitSwitchExpression(switchExpr: J.SwitchExpression, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(switchExpr, p);
        if (!expression?.kind || expression.kind !== J.Kind.SwitchExpression) {
            return expression;
        }
        switchExpr = expression as J.SwitchExpression;

        const updates: any = {
            prefix: await this.visitSpace(switchExpr.prefix, p),
            markers: await this.visitMarkers(switchExpr.markers, p),
            selector: await this.visitDefined(switchExpr.selector, p) as J.ControlParentheses<Expression>,
            cases: await this.visitDefined(switchExpr.cases, p) as J.Block,
            type: await this.visitType(switchExpr.type, p)
        };
        return updateIfChanged(switchExpr, updates);
    }

    protected async visitSynchronized(sync: J.Synchronized, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(sync, p);
        if (!statement?.kind || statement.kind !== J.Kind.Synchronized) {
            return statement;
        }
        sync = statement as J.Synchronized;

        const updates: any = {
            prefix: await this.visitSpace(sync.prefix, p),
            markers: await this.visitMarkers(sync.markers, p),
            lock: await this.visitDefined(sync.lock, p) as J.ControlParentheses<Expression>,
            body: await this.visitDefined(sync.body, p) as J.Block
        };
        return updateIfChanged(sync, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(ternary.prefix, p),
            markers: await this.visitMarkers(ternary.markers, p),
            condition: await this.visitDefined(ternary.condition, p) as Expression,
            truePart: await this.visitLeftPadded(ternary.truePart, p),
            falsePart: await this.visitLeftPadded(ternary.falsePart, p),
            type: await this.visitType(ternary.type, p)
        };
        return updateIfChanged(ternary, updates);
    }

    protected async visitThrow(thrown: J.Throw, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(thrown, p);
        if (!statement?.kind || statement.kind !== J.Kind.Throw) {
            return statement;
        }
        thrown = statement as J.Throw;

        const updates: any = {
            prefix: await this.visitSpace(thrown.prefix, p),
            markers: await this.visitMarkers(thrown.markers, p),
            exception: await this.visitDefined(thrown.exception, p) as Expression
        };
        return updateIfChanged(thrown, updates);
    }

    protected async visitTry(tryable: J.Try, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(tryable, p);
        if (!statement?.kind || statement.kind !== J.Kind.Try) {
            return statement;
        }
        tryable = statement as J.Try;

        const updates: any = {
            prefix: await this.visitSpace(tryable.prefix, p),
            markers: await this.visitMarkers(tryable.markers, p),
            resources: await this.visitOptionalContainer(tryable.resources, p),
            body: await this.visitDefined(tryable.body, p) as J.Block,
            catches: await mapAsync(tryable.catches, c => this.visitDefined<J.Try.Catch>(c, p)),
            finally: await this.visitOptionalLeftPadded(tryable.finally, p)
        };
        return updateIfChanged(tryable, updates);
    }

    protected async visitTryResource(resource: J.Try.Resource, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(resource.prefix, p),
            markers: await this.visitMarkers(resource.markers, p),
            variableDeclarations: await this.visitDefined(resource.variableDeclarations, p) as TypedTree
        };
        return updateIfChanged(resource, updates);
    }

    protected async visitTryCatch(tryCatch: J.Try.Catch, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(tryCatch.prefix, p),
            markers: await this.visitMarkers(tryCatch.markers, p),
            parameter: await this.visitDefined(tryCatch.parameter, p) as J.ControlParentheses<J.VariableDeclarations>,
            body: await this.visitDefined(tryCatch.body, p) as J.Block
        };
        return updateIfChanged(tryCatch, updates);
    }

    protected async visitTypeCast(typeCast: J.TypeCast, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(typeCast, p);
        if (!expression?.kind || expression.kind !== J.Kind.TypeCast) {
            return expression;
        }
        typeCast = expression as J.TypeCast;

        const updates: any = {
            prefix: await this.visitSpace(typeCast.prefix, p),
            markers: await this.visitMarkers(typeCast.markers, p),
            class: await this.visitDefined(typeCast.class, p) as J.ControlParentheses<TypedTree>,
            expression: await this.visitDefined(typeCast.expression, p) as Expression
        };
        return updateIfChanged(typeCast, updates);
    }

    protected async visitTypeParameter(typeParam: J.TypeParameter, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(typeParam.prefix, p),
            markers: await this.visitMarkers(typeParam.markers, p),
            annotations: await mapAsync(typeParam.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            modifiers: await mapAsync(typeParam.modifiers, m => this.visitDefined<J.Modifier>(m, p)),
            name: await this.visitDefined(typeParam.name, p) as J.Identifier,
            bounds: await this.visitOptionalContainer(typeParam.bounds, p)
        };
        return updateIfChanged(typeParam, updates);
    }

    protected async visitTypeParameters(typeParams: J.TypeParameters, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(typeParams.prefix, p),
            markers: await this.visitMarkers(typeParams.markers, p),
            annotations: await mapAsync(typeParams.annotations, a => this.visitDefined<J.Annotation>(a, p)),
            typeParameters: await mapAsync(typeParams.typeParameters, tp => this.visitRightPadded(tp, p))
        };
        return updateIfChanged(typeParams, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(unary.prefix, p),
            markers: await this.visitMarkers(unary.markers, p),
            operator: await this.visitLeftPadded(unary.operator, p),
            expression: await this.visitDefined(unary.expression, p) as Expression,
            type: await this.visitType(unary.type, p)
        };
        return updateIfChanged(unary, updates);
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

        const updates: any = {
            prefix: await this.visitSpace(unknown.prefix, p),
            markers: await this.visitMarkers(unknown.markers, p),
            source: await this.visitDefined(unknown.source, p) as J.UnknownSource
        };
        return updateIfChanged(unknown, updates);
    }

    protected async visitUnknownSource(source: J.UnknownSource, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(source.prefix, p),
            markers: await this.visitMarkers(source.markers, p)
        };
        return updateIfChanged(source, updates);
    }

    protected async visitVariableDeclarations(varDecls: J.VariableDeclarations, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(varDecls, p);
        if (!statement?.kind || statement.kind !== J.Kind.VariableDeclarations) {
            return statement;
        }
        varDecls = statement as J.VariableDeclarations;

        const updates: any = {
            prefix: await this.visitSpace(varDecls.prefix, p),
            markers: await this.visitMarkers(varDecls.markers, p),
            leadingAnnotations: await mapAsync(varDecls.leadingAnnotations, a => this.visitDefined<J.Annotation>(a, p)),
            modifiers: await mapAsync(varDecls.modifiers, m => this.visitDefined<J.Modifier>(m, p)),
            variables: await mapAsync(varDecls.variables, v => this.visitRightPadded(v, p))
        };
        if (varDecls.typeExpression) {
            updates.typeExpression = await this.visitDefined(varDecls.typeExpression, p) as TypedTree;
        }
        if (varDecls.varargs) {
            updates.varargs = await this.visitSpace(varDecls.varargs, p);
        }
        return updateIfChanged(varDecls, updates);
    }

    protected async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: P): Promise<J | undefined> {
        const updates: any = {
            prefix: await this.visitSpace(variable.prefix, p),
            markers: await this.visitMarkers(variable.markers, p),
            name: await this.visitDefined(variable.name, p) as J.Identifier,
            dimensionsAfterName: await mapAsync(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, p)),
            initializer: await this.visitOptionalLeftPadded(variable.initializer, p),
            variableType: await this.visitType(variable.variableType, p) as Type.Variable | undefined
        };
        return updateIfChanged(variable, updates);
    }

    protected async visitWhileLoop(whileLoop: J.WhileLoop, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(whileLoop, p);
        if (!statement?.kind || statement.kind !== J.Kind.WhileLoop) {
            return statement;
        }
        whileLoop = statement as J.WhileLoop;

        const updates: any = {
            prefix: await this.visitSpace(whileLoop.prefix, p),
            markers: await this.visitMarkers(whileLoop.markers, p),
            condition: await this.visitDefined(whileLoop.condition, p) as J.ControlParentheses<Expression>,
            body: await this.visitRightPadded(whileLoop.body, p)
        };
        return updateIfChanged(whileLoop, updates);
    }

    protected async visitWildcard(wildcard: J.Wildcard, p: P): Promise<J | undefined> {
        const expression = await this.visitExpression(wildcard, p);
        if (!expression?.kind || expression.kind !== J.Kind.Wildcard) {
            return expression;
        }
        wildcard = expression as J.Wildcard;

        const updates: any = {
            prefix: await this.visitSpace(wildcard.prefix, p),
            markers: await this.visitMarkers(wildcard.markers, p),
            bound: await this.visitOptionalLeftPadded(wildcard.bound, p)
        };
        if (wildcard.boundedType) {
            updates.boundedType = await this.visitTypeName(wildcard.boundedType, p);
        }
        return updateIfChanged(wildcard, updates);
    }

    protected async visitYield(aYield: J.Yield, p: P): Promise<J | undefined> {
        const statement = await this.visitStatement(aYield, p);
        if (!statement?.kind || statement.kind !== J.Kind.Yield) {
            return statement;
        }
        aYield = statement as J.Yield;

        const updates: any = {
            prefix: await this.visitSpace(aYield.prefix, p),
            markers: await this.visitMarkers(aYield.markers, p),
            value: await this.visitDefined(aYield.value, p) as Expression
        };
        return updateIfChanged(aYield, updates);
    }

    protected async visitOptionalRightPadded<T extends J | boolean>(right: J.RightPadded<T> | undefined, p: P): Promise<J.RightPadded<T> | undefined> {
        return right ? this.visitRightPadded(right, p) : undefined;
    }

    /**
     * Visits a right-padded value.
     *
     * For tree nodes (J): The padded value IS the tree with `padding: Suffix` mixed in.
     * For booleans: The padded value has an `element` property containing the boolean.
     *
     * @overload Primitives (boolean) - always returns (primitives cannot be deleted by visitors)
     * @overload Tree nodes (J) - may return undefined if the element is deleted
     * @overload Generic fallback for union types (J | boolean) - may return undefined
     */
    public async visitRightPadded<T extends boolean>(right: J.RightPadded<T>, p: P): Promise<J.RightPadded<T>>;
    public async visitRightPadded<T extends J>(right: J.RightPadded<T>, p: P): Promise<J.RightPadded<T> | undefined>;
    public async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: P): Promise<J.RightPadded<T> | undefined>;
    public async visitRightPadded<T extends J | boolean>(right: J.RightPadded<T>, p: P): Promise<J.RightPadded<T> | undefined> {
        this.cursor = new Cursor(right, this.cursor);

        const hasElement = 'element' in right;
        const visitedElement: T | undefined = hasElement
            ? (right as { element: boolean }).element as T
            : await this.visitDefined(right as J, p) as T | undefined;

        const after = await this.visitSpace(right.padding.after, p);
        const paddingMarkers = await this.visitMarkers(right.padding.markers, p);
        this.cursor = this.cursor.parent!;

        if (visitedElement === undefined) {
            return undefined;
        }

        const paddingUnchanged = after === right.padding.after && paddingMarkers === right.padding.markers;
        const padding: J.Suffix = paddingUnchanged ? right.padding : { after, markers: paddingMarkers };

        if (hasElement) {
            return updateIfChanged(right, { element: visitedElement, padding } as any);
        }
        return updateIfChanged(visitedElement as J & { padding: J.Suffix }, { padding }) as J.RightPadded<T>;
    }

    protected async visitOptionalLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T> | undefined, p: P): Promise<J.LeftPadded<T> | undefined> {
        return left ? this.visitLeftPadded(left, p) : undefined;
    }

    /**
     * Visits a left-padded value.
     *
     * For tree nodes (J) and Space: The padded value IS the element with `padding: Prefix` mixed in.
     * For primitives (boolean, number, string): The padded value has an `element` property.
     *
     * @overload Primitives (number, string, boolean) - always returns (primitives cannot be deleted by visitors)
     * @overload Tree nodes (J) - may return undefined if the element is deleted
     * @overload Space - may return undefined if the space is deleted
     * @overload Generic fallback for union types - may return undefined
     */
    public async visitLeftPadded<T extends number | string | boolean>(left: J.LeftPadded<T>, p: P): Promise<J.LeftPadded<T>>;
    public async visitLeftPadded<T extends J>(left: J.LeftPadded<T>, p: P): Promise<J.LeftPadded<T> | undefined>;
    public async visitLeftPadded(left: J.LeftPadded<J.Space>, p: P): Promise<J.LeftPadded<J.Space> | undefined>;
    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, p: P): Promise<J.LeftPadded<T> | undefined>;
    public async visitLeftPadded<T extends J | J.Space | number | string | boolean>(left: J.LeftPadded<T>, p: P): Promise<J.LeftPadded<T> | undefined> {
        this.cursor = new Cursor(left, this.cursor);
        const before = await this.visitSpace(left.padding.before, p);

        const hasElement = 'element' in left;
        let visitedElement: T | undefined;

        if (hasElement) {
            visitedElement = (left as unknown as { element: T }).element;
        } else if (isSpace(left)) {
            visitedElement = await this.visitSpace(left as J.Space, p) as T;
        } else {
            visitedElement = await this.visitDefined(left as J, p) as T | undefined;
        }

        const paddingMarkers = await this.visitMarkers(left.padding.markers, p);
        this.cursor = this.cursor.parent!;

        if (visitedElement === undefined) {
            return undefined;
        }

        const paddingUnchanged = before === left.padding.before && paddingMarkers === left.padding.markers;
        const padding: J.Prefix = paddingUnchanged ? left.padding : { before, markers: paddingMarkers };

        if (hasElement) {
            return updateIfChanged(left, { element: visitedElement, padding } as any);
        }
        return updateIfChanged(visitedElement as (J | J.Space) & { padding: J.Prefix }, { padding }) as J.LeftPadded<T>;
    }

    protected async visitOptionalContainer<T extends J>(container: J.Container<T> | undefined, p: P): Promise<J.Container<T> | undefined> {
        return container ? this.visitContainer(container, p) : undefined;
    }

    public async visitContainer<T extends J>(container: J.Container<T>, p: P): Promise<J.Container<T>> {
        this.cursor = new Cursor(container, this.cursor);
        const before = await this.visitSpace(container.before, p);
        const elements = await mapAsync(container.elements, e => this.visitRightPadded(e, p));
        const markers = await this.visitMarkers(container.markers, p);
        this.cursor = this.cursor.parent!;
        return updateIfChanged(container, { before, elements, markers });
    }

    protected async produceJava<J2 extends J>(
        before: J2,
        p: P,
        recipe?: (draft: Draft<J2>) =>
            ValidRecipeReturnType<Draft<J2>> |
            PromiseLike<ValidRecipeReturnType<Draft<J2>>>
    ): Promise<J2> {
        const [draft, finishDraft] = create(before);
        (draft as Draft<J>).prefix = await this.visitSpace(before!.prefix, p);
        (draft as Draft<J>).markers = await this.visitMarkers(before!.markers, p);
        if (recipe) {
            await recipe(draft);
        }
        return finishDraft() as J2;
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
                const adapter = extendedJavaKinds.get(t.kind)
                if (adapter) {
                    return adapter(this).visit(t, p);
                }
                return Promise.resolve(t);
        }
    }
}
