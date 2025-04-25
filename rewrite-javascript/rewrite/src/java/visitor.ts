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
import {mapAsync, produceAsync, SourceFile, TreeVisitor, ValidImmerRecipeReturnType} from "../";
import {
    Annotation,
    AnnotatedType,
    ArrayAccess,
    ArrayDimension,
    ArrayType,
    Assert,
    Assignment,
    AssignmentOperation,
    Binary,
    Block,
    Break,
    Case,
    ClassDeclaration,
    ClassDeclarationKind,
    CompilationUnit,
    Continue,
    ControlParentheses,
    DeconstructionPattern,
    DoWhileLoop,
    Empty,
    EnumValue,
    EnumValueSet,
    Erroneous,
    Expression,
    FieldAccess,
    ForEachLoop,
    ForEachLoopControl,
    ForLoop,
    ForLoopControl,
    Identifier,
    IdentifierWithAnnotations,
    If,
    IfElse,
    Import,
    InstanceOf,
    IntersectionType,
    isJava,
    isSpace,
    J,
    JavaKind,
    JLeftPadded,
    JRightPadded,
    JContainer,
    Label,
    Lambda,
    LambdaParameters,
    Literal,
    MemberReference,
    MethodDeclaration,
    MethodInvocation,
    Modifier,
    MultiCatch,
    NameTree,
    NewArray,
    NewClass,
    NullableType,
    Package,
    ParameterizedType,
    Parentheses,
    ParenthesizedTypeTree,
    Primitive,
    Return,
    Space,
    Statement,
    Switch,
    SwitchExpression,
    Synchronized,
    Ternary,
    Throw,
    Try,
    TryResource,
    TryCatch,
    TypeCast,
    TypeParameter,
    TypeParameters,
    TypeTree,
    TypedTree,
    Unary,
    Unknown,
    UnknownSource,
    VariableDeclarations,
    Variable,
    WhileLoop,
    Wildcard,
    Yield,
    JavaType
} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";

export class JavaVisitor<P> extends TreeVisitor<J, P> {
    // protected javadocVisitor: any | null = null;

    isAcceptable(sourceFile: SourceFile): boolean {
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
    protected async visitSpace(space: Space, p: P): Promise<Space> {
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

    protected async visitAnnotatedType(annotatedType: AnnotatedType, p: P): Promise<J | undefined> {
        return this.produceJava<AnnotatedType>(annotatedType, p, async draft => {
            draft.annotations = await mapAsync(annotatedType.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.typeExpression = await this.visitDefined(annotatedType.typeExpression, p) as TypeTree;
            draft.type = await this.visitType(annotatedType.type, p);
        });
    }

    protected async visitAnnotation(annotation: Annotation, p: P): Promise<J | undefined> {
        return this.produceJava<Annotation>(annotation, p, async draft => {
            draft.annotationType = await this.visitTypeName(annotation.annotationType, p);
            draft.arguments = await this.visitContainer(annotation.arguments, p);
            draft.type = await this.visitType(annotation.type, p);
        });
    }

    protected async visitArrayAccess(arrayAccess: ArrayAccess, p: P): Promise<J | undefined> {
        return this.produceJava<ArrayAccess>(arrayAccess, p, async draft => {
            draft.indexed = await this.visitDefined(arrayAccess.indexed, p) as Expression;
            draft.dimension = await this.visitDefined(arrayAccess.dimension, p) as ArrayDimension;
            draft.type = await this.visitType(arrayAccess.type, p);
        });
    }

    protected async visitArrayDimension(arrayDimension: ArrayDimension, p: P): Promise<J | undefined> {
        return this.produceJava<ArrayDimension>(arrayDimension, p, async draft => {
            draft.index = await this.visitRightPadded(arrayDimension.index, p);
        });
    }

    protected async visitArrayType(arrayType: ArrayType, p: P): Promise<J | undefined> {
        return this.produceJava<ArrayType>(arrayType, p, async draft => {
            draft.elementType = await this.visitDefined(arrayType.elementType, p) as TypeTree;
            if (arrayType.annotations) {
                draft.annotations = await mapAsync(arrayType.annotations, a => this.visitDefined<Annotation>(a, p));
            }
            draft.dimension = await this.visitLeftPadded(arrayType.dimension, p);
            draft.type = await this.visitType(arrayType.type, p) as JavaType;
        });
    }

    protected async visitAssert(assert_: Assert, p: P): Promise<J | undefined> {
        return this.produceJava<Assert>(assert_, p, async draft => {
            draft.condition = await this.visitDefined(assert_.condition, p) as Expression;
            draft.detail = await this.visitLeftPadded(assert_.detail, p);
        });
    }

    protected async visitAssignment(assignment: Assignment, p: P): Promise<J | undefined> {
        return this.produceJava<Assignment>(assignment, p, async draft => {
            draft.variable = await this.visitDefined(assignment.variable, p) as Expression;
            draft.assignment = await this.visitLeftPadded(assignment.assignment, p);
            draft.type = await this.visitType(assignment.type, p);
        });
    }

    protected async visitAssignmentOperation(assignOp: AssignmentOperation, p: P): Promise<J | undefined> {
        return this.produceJava<AssignmentOperation>(assignOp, p, async draft => {
            draft.variable = await this.visitDefined(assignOp.variable, p) as Expression;
            draft.operator = await this.visitLeftPadded(assignOp.operator, p);
            draft.assignment = await this.visitDefined(assignOp.assignment, p) as Expression;
            draft.type = await this.visitType(assignOp.type, p);
        });
    }

    protected async visitBinary(binary: Binary, p: P): Promise<J | undefined> {
        return this.produceJava<Binary>(binary, p, async draft => {
            draft.left = await this.visitDefined(binary.left, p) as Expression;
            draft.operator = await this.visitLeftPadded(binary.operator, p);
            draft.right = await this.visitDefined(binary.right, p) as Expression;
            draft.type = await this.visitType(binary.type, p);
        });
    }

    protected async visitBlock(block: Block, p: P): Promise<J | undefined> {
        return this.produceJava<Block>(block, p, async draft => {
            draft.static = await this.visitRightPadded(block.static, p);
            draft.statements = await mapAsync(block.statements, stmt => this.visitRightPadded(stmt, p));
            draft.end = await this.visitSpace(block.end, p);
        });
    }

    protected async visitBreak(breakStatement: Break, p: P): Promise<J | undefined> {
        return this.produceJava<Break>(breakStatement, p, async draft => {
            if (breakStatement.label) {
                draft.label = await this.visitDefined(breakStatement.label, p) as Identifier;
            }
        });
    }

    protected async visitCase(case_: Case, p: P): Promise<J | undefined> {
        return this.produceJava<Case>(case_, p, async draft => {
            draft.caseLabels = await this.visitContainer(case_.caseLabels, p);
            draft.statements = await this.visitContainer(case_.statements, p);
            draft.body = await this.visitRightPadded(case_.body, p);
            if (case_.guard) {
                draft.guard = await this.visitDefined(case_.guard, p) as Expression;
            }
        });
    }

    protected async visitClassDeclaration(classDecl: ClassDeclaration, p: P): Promise<J | undefined> {
        return this.produceJava<ClassDeclaration>(classDecl, p, async draft => {
            draft.leadingAnnotations = await mapAsync(classDecl.leadingAnnotations, a => this.visitDefined<Annotation>(a, p));
            draft.modifiers = await mapAsync(classDecl.modifiers, m => this.visitDefined<Modifier>(m, p));
            draft.classKind = await this.visitDefined(classDecl.classKind, p) as ClassDeclarationKind;
            draft.name = await this.visitDefined(classDecl.name, p) as Identifier;
            draft.typeParameters = await this.visitContainer(classDecl.typeParameters, p);
            draft.primaryConstructor = await this.visitContainer(classDecl.primaryConstructor, p);
            draft.extends = await this.visitLeftPadded(classDecl.extends, p);
            draft.implements = await this.visitContainer(classDecl.implements, p);
            draft.permitting = await this.visitContainer(classDecl.permitting, p);
            draft.body = await this.visitDefined(classDecl.body, p) as Block;
            draft.type = await this.visitType(classDecl.type, p) as JavaType.Class | undefined;
        });
    }

    protected async visitClassDeclarationKind(kind: ClassDeclarationKind, p: P): Promise<J | undefined> {
        return this.produceJava<ClassDeclarationKind>(kind, p, async draft => {
            draft.annotations = await mapAsync(kind.annotations, a => this.visitDefined<Annotation>(a, p));
        });
    }

    protected async visitCompilationUnit(cu: CompilationUnit, p: P): Promise<J | undefined> {
        return this.produceJava<CompilationUnit>(cu, p, async draft => {
            draft.packageDeclaration = await this.visitRightPadded(cu.packageDeclaration, p) as JRightPadded<Package>;
            draft.imports = await mapAsync(cu.imports, imp => this.visitRightPadded(imp, p));
            draft.classes = await mapAsync(cu.classes, cls => this.visitDefined(cls, p) as Promise<ClassDeclaration>);
            draft.eof = await this.visitSpace(cu.eof, p);
        });
    }

    protected async visitContinue(continueStatement: Continue, p: P): Promise<J | undefined> {
        return this.produceJava<Continue>(continueStatement, p, async draft => {
            if (continueStatement.label) {
                draft.label = await this.visitDefined(continueStatement.label, p) as Identifier;
            }
        });
    }

    protected async visitControlParentheses<T extends J>(controlParens: ControlParentheses<T>, p: P): Promise<J | undefined> {
        return this.produceJava<ControlParentheses<T>>(controlParens, p, async draft => {
            (draft.tree as JRightPadded<J>) = await this.visitRightPadded(controlParens.tree, p);
        });
    }

    protected async visitDeconstructionPattern(pattern: DeconstructionPattern, p: P): Promise<J | undefined> {
        return this.produceJava<DeconstructionPattern>(pattern, p, async draft => {
            draft.deconstructor = await this.visitDefined(pattern.deconstructor, p) as Expression;
            draft.nested = await this.visitContainer(pattern.nested, p);
            draft.type = await this.visitType(pattern.type, p);
        });
    }

    protected async visitDoWhileLoop(doWhileLoop: DoWhileLoop, p: P): Promise<J | undefined> {
        return this.produceJava<DoWhileLoop>(doWhileLoop, p, async draft => {
            draft.body = await this.visitRightPadded(doWhileLoop.body, p);
            draft.whileCondition = await this.visitDefined(doWhileLoop.whileCondition, p) as ControlParentheses<Expression>;
        });
    }

    protected async visitEmpty(empty: Empty, p: P): Promise<J | undefined> {
        return this.produceJava<Empty>(empty, p);
    }

    protected async visitEnumValue(enumValue: EnumValue, p: P): Promise<J | undefined> {
        return this.produceJava<EnumValue>(enumValue, p, async draft => {
            draft.annotations = await mapAsync(enumValue.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.name = await this.visitDefined(enumValue.name, p) as Identifier;
            if (enumValue.initializer) {
                draft.initializer = await this.visitDefined(enumValue.initializer, p) as NewClass;
            }
        });
    }

    protected async visitEnumValueSet(enumValueSet: EnumValueSet, p: P): Promise<J | undefined> {
        return this.produceJava<EnumValueSet>(enumValueSet, p, async draft => {
            draft.enums = await mapAsync(enumValueSet.enums, e => this.visitRightPadded(e, p));
        });
    }

    protected async visitErroneous(erroneous: Erroneous, p: P): Promise<J | undefined> {
        return this.produceJava<Erroneous>(erroneous, p);
    }

    protected async visitFieldAccess(fieldAccess: FieldAccess, p: P): Promise<J | undefined> {
        return this.produceJava<FieldAccess>(fieldAccess, p, async draft => {
            draft.target = await this.visitDefined(fieldAccess.target, p) as Expression;
            draft.name = await this.visitLeftPadded(fieldAccess.name, p);
            draft.type = await this.visitType(fieldAccess.type, p);
        });
    }

    protected async visitForEachLoop(forLoop: ForEachLoop, p: P): Promise<J | undefined> {
        return this.produceJava<ForEachLoop>(forLoop, p, async draft => {
            draft.control = await this.visitDefined(forLoop.control, p) as ForEachLoopControl;
            draft.body = await this.visitRightPadded(forLoop.body, p);
        });
    }

    protected async visitForEachLoopControl(control: ForEachLoopControl, p: P): Promise<J | undefined> {
        return this.produceJava<ForEachLoopControl>(control, p, async draft => {
            draft.variable = await this.visitRightPadded(control.variable, p);
            draft.iterable = await this.visitRightPadded(control.iterable, p);
        });
    }

    protected async visitForLoop(forLoop: ForLoop, p: P): Promise<J | undefined> {
        return this.produceJava<ForLoop>(forLoop, p, async draft => {
            draft.control = await this.visitDefined(forLoop.control, p) as ForLoopControl;
            draft.body = await this.visitRightPadded(forLoop.body, p);
        });
    }

    protected async visitForLoopControl(control: ForLoopControl, p: P): Promise<J | undefined> {
        return this.produceJava<ForLoopControl>(control, p, async draft => {
            draft.init = await mapAsync(control.init, i => this.visitRightPadded(i, p));
            draft.condition = await this.visitRightPadded(control.condition, p);
            draft.update = await mapAsync(control.update, u => this.visitRightPadded(u, p));
        });
    }

    protected async visitIdentifier(ident: Identifier, p: P): Promise<J | undefined> {
        return this.produceJava<Identifier>(ident, p, async draft => {
            draft.annotations = await mapAsync(ident.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.type = await this.visitType(ident.type, p);
            draft.fieldType = await this.visitType(ident.fieldType, p) as JavaType.Variable | undefined;
        });
    }

    protected async visitIdentifierWithAnnotations(idWithAnn: IdentifierWithAnnotations, p: P): Promise<J | undefined> {
        return this.produceJava<IdentifierWithAnnotations>(idWithAnn, p, async draft => {
            draft.identifier = await this.visitDefined(idWithAnn.identifier, p) as Identifier;
            draft.annotations = await mapAsync(idWithAnn.annotations, a => this.visitDefined<Annotation>(a, p));
        });
    }

    protected async visitIf(iff: If, p: P): Promise<J | undefined> {
        return this.produceJava<If>(iff, p, async draft => {
            draft.ifCondition = await this.visitDefined(iff.ifCondition, p) as ControlParentheses<Expression>;
            draft.thenPart = await this.visitRightPadded(iff.thenPart, p);
            if (iff.elsePart) {
                draft.elsePart = await this.visitDefined(iff.elsePart, p) as IfElse;
            }
        });
    }

    protected async visitIfElse(else_: IfElse, p: P): Promise<J | undefined> {
        return this.produceJava<IfElse>(else_, p, async draft => {
            draft.body = await this.visitRightPadded(else_.body, p);
        });
    }

    protected async visitImport(import_: Import, p: P): Promise<J | undefined> {
        return this.produceJava<Import>(import_, p, async draft => {
            draft.static = await this.visitLeftPadded(import_.static, p);
            draft.qualid = await this.visitDefined(import_.qualid, p) as FieldAccess;
            draft.alias = await this.visitLeftPadded(import_.alias, p);
        });
    }

    protected async visitInstanceOf(instanceOf: InstanceOf, p: P): Promise<J | undefined> {
        return this.produceJava<InstanceOf>(instanceOf, p, async draft => {
            draft.expression = await this.visitRightPadded(instanceOf.expression, p);
            draft.clazz = await this.visitDefined(instanceOf.clazz, p) as J;
            if (instanceOf.pattern) {
                draft.pattern = await this.visitDefined(instanceOf.pattern, p) as J;
            }
            draft.type = await this.visitType(instanceOf.type, p);
            if (instanceOf.modifier) {
                draft.modifier = await this.visitDefined(instanceOf.modifier, p) as Modifier;
            }
        });
    }

    protected async visitIntersectionType(intersectionType: IntersectionType, p: P): Promise<J | undefined> {
        return this.produceJava<IntersectionType>(intersectionType, p, async draft => {
            draft.bounds = await this.visitContainer(intersectionType.bounds, p);
        });
    }

    protected async visitLabel(label: Label, p: P): Promise<J | undefined> {
        return this.produceJava<Label>(label, p, async draft => {
            draft.label = await this.visitRightPadded(label.label, p);
            draft.statement = await this.visitDefined(label.statement, p) as Statement;
        });
    }

    protected async visitLambda(lambda: Lambda, p: P): Promise<J | undefined> {
        return this.produceJava<Lambda>(lambda, p, async draft => {
            draft.parameters = await this.visitDefined(lambda.parameters, p) as LambdaParameters;
            draft.arrow = await this.visitSpace(lambda.arrow, p);
            draft.body = await this.visitDefined(lambda.body, p) as Statement | Expression;
            draft.type = await this.visitType(lambda.type, p);
        });
    }

    protected async visitLambdaParameters(params: LambdaParameters, p: P): Promise<J | undefined> {
        return this.produceJava<LambdaParameters>(params, p, async draft => {
            draft.parameters = await mapAsync(params.parameters, param => this.visitRightPadded(param, p));
        });
    }

    protected async visitLiteral(literal: Literal, p: P): Promise<J | undefined> {
        return this.produceJava<Literal>(literal, p, async draft => {
            draft.type = await this.visitType(literal.type, p) as JavaType.Primitive | undefined;
        });
    }

    protected async visitMemberReference(memberRef: MemberReference, p: P): Promise<J | undefined> {
        return this.produceJava<MemberReference>(memberRef, p, async draft => {
            draft.containing = await this.visitRightPadded(memberRef.containing, p);
            draft.typeParameters = await this.visitContainer(memberRef.typeParameters, p);
            draft.reference = await this.visitLeftPadded(memberRef.reference, p);
            draft.type = await this.visitType(memberRef.type, p);
            draft.methodType = await this.visitType(memberRef.methodType, p) as JavaType.Method | undefined;
            draft.variableType = await this.visitType(memberRef.variableType, p) as JavaType.Variable | undefined;
        });
    }

    protected async visitMethodDeclaration(methodDecl: MethodDeclaration, p: P): Promise<J | undefined> {
        return this.produceJava<MethodDeclaration>(methodDecl, p, async draft => {
            draft.leadingAnnotations = await mapAsync(methodDecl.leadingAnnotations, a => this.visitDefined<Annotation>(a, p));
            draft.modifiers = await mapAsync(methodDecl.modifiers, m => this.visitDefined<Modifier>(m, p));

            if (methodDecl.typeParameters) {
                draft.typeParameters = await this.visitDefined(methodDecl.typeParameters, p) as TypeParameters;
            }

            if (methodDecl.returnTypeExpression) {
                draft.returnTypeExpression = await this.visitDefined(methodDecl.returnTypeExpression, p) as TypeTree;
            }

            draft.name = await this.visitDefined(methodDecl.name, p) as IdentifierWithAnnotations;
            draft.parameters = await this.visitContainer(methodDecl.parameters, p);
            draft.throws = await this.visitContainer(methodDecl.throws, p);

            if (methodDecl.body) {
                draft.body = await this.visitDefined(methodDecl.body, p) as Block;
            }

            draft.defaultValue = await this.visitLeftPadded(methodDecl.defaultValue, p);
            draft.methodType = await this.visitType(methodDecl.methodType, p) as JavaType.Method | undefined;
        });
    }

    protected async visitMethodInvocation(methodInv: MethodInvocation, p: P): Promise<J | undefined> {
        return this.produceJava<MethodInvocation>(methodInv, p, async draft => {
            draft.select = await this.visitRightPadded(methodInv.select, p);
            draft.typeParameters = await this.visitContainer(methodInv.typeParameters, p);
            draft.name = await this.visitDefined(methodInv.name, p) as Identifier;
            draft.arguments = await this.visitContainer(methodInv.arguments, p);
            draft.methodType = await this.visitType(methodInv.methodType, p) as JavaType.Method | undefined;
            draft.type = await this.visitType(methodInv.type, p);
        });
    }

    protected async visitModifier(modifier: Modifier, p: P): Promise<J | undefined> {
        return this.produceJava<Modifier>(modifier, p, async draft => {
            draft.annotations = await mapAsync(modifier.annotations, a => this.visitDefined<Annotation>(a, p));
        });
    }

    protected async visitMultiCatch(multiCatch: MultiCatch, p: P): Promise<J | undefined> {
        return this.produceJava<MultiCatch>(multiCatch, p, async draft => {
            draft.alternatives = await mapAsync(multiCatch.alternatives, alt => this.visitRightPadded(alt, p));
            draft.type = await this.visitType(multiCatch.type, p);
        });
    }

    protected async visitNewArray(newArray: NewArray, p: P): Promise<J | undefined> {
        return this.produceJava<NewArray>(newArray, p, async draft => {
            if (newArray.typeExpression) {
                draft.typeExpression = await this.visitDefined(newArray.typeExpression, p) as TypeTree;
            }

            draft.dimensions = await mapAsync(newArray.dimensions, dim =>
                this.visitDefined<ArrayDimension>(dim, p));

            draft.initializer = await this.visitContainer(newArray.initializer, p);
            draft.type = await this.visitType(newArray.type, p);
        });
    }

    protected async visitNewClass(newClass: NewClass, p: P): Promise<J | undefined> {
        return this.produceJava<NewClass>(newClass, p, async draft => {
            if (newClass.enclosing) {
                draft.enclosing = await this.visitRightPadded(newClass.enclosing, p);
            }

            draft.new = await this.visitSpace(newClass.new, p);

            if (newClass.clazz) {
                draft.clazz = await this.visitDefined(newClass.clazz, p) as TypeTree;
            }

            draft.arguments = await this.visitContainer(newClass.arguments, p);

            if (newClass.body) {
                draft.body = await this.visitDefined(newClass.body, p) as Block;
            }

            draft.constructorType = await this.visitType(newClass.constructorType, p) as JavaType.Method | undefined;
            draft.type = await this.visitType(newClass.type, p);
        });
    }

    protected async visitNullableType(nullableType: NullableType, p: P): Promise<J | undefined> {
        return this.produceJava<NullableType>(nullableType, p, async draft => {
            draft.annotations = await mapAsync(nullableType.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.typeTree = await this.visitRightPadded(nullableType.typeTree, p);
            draft.type = await this.visitType(nullableType.type, p);
        });
    }

    protected async visitPackage(package_: Package, p: P): Promise<J | undefined> {
        return this.produceJava<Package>(package_, p, async draft => {
            draft.expression = await this.visitDefined(package_.expression, p) as Expression;
            if (package_.annotations) {
                draft.annotations = await mapAsync(package_.annotations, a => this.visitDefined<Annotation>(a, p));
            }
        });
    }

    protected async visitParameterizedType(parameterizedType: ParameterizedType, p: P): Promise<J | undefined> {
        return this.produceJava<ParameterizedType>(parameterizedType, p, async draft => {
            draft.clazz = await this.visitTypeName(parameterizedType.clazz, p);
            draft.typeParameters = await this.visitContainer(parameterizedType.typeParameters, p);
            draft.type = await this.visitType(parameterizedType.type, p);
        });
    }

    protected async visitParentheses<T extends J>(parentheses: Parentheses<T>, p: P): Promise<J | undefined> {
        return this.produceJava<Parentheses<T>>(parentheses, p, async draft => {
            (draft.tree as JRightPadded<J>) = await this.visitRightPadded(parentheses.tree, p);
            draft.type = await this.visitType(parentheses.type, p);
        });
    }

    protected async visitParenthesizedTypeTree(parTypeTree: ParenthesizedTypeTree, p: P): Promise<J | undefined> {
        return this.produceJava<ParenthesizedTypeTree>(parTypeTree, p, async draft => {
            draft.annotations = await mapAsync(parTypeTree.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.parenthesizedType = await this.visitDefined(parTypeTree.parenthesizedType, p) as Parentheses<TypeTree>;
            draft.type = await this.visitType(parTypeTree.type, p);
        });
    }

    protected async visitPrimitive(primitive: Primitive, p: P): Promise<J | undefined> {
        return this.produceJava<Primitive>(primitive, p, async draft => {
            draft.type = await this.visitType(primitive.type, p) as JavaType.Primitive;
        });
    }

    protected async visitReturn(ret: Return, p: P): Promise<J | undefined> {
        return this.produceJava<Return>(ret, p, async draft => {
            if (ret.expression) {
                draft.expression = await this.visitDefined(ret.expression, p) as Expression;
            }
        });
    }

    protected async visitSwitch(switch_: Switch, p: P): Promise<J | undefined> {
        return this.produceJava<Switch>(switch_, p, async draft => {
            draft.selector = await this.visitDefined(switch_.selector, p) as ControlParentheses<Expression>;
            draft.cases = await this.visitDefined(switch_.cases, p) as Block;
        });
    }

    protected async visitSwitchExpression(switchExpr: SwitchExpression, p: P): Promise<J | undefined> {
        return this.produceJava<SwitchExpression>(switchExpr, p, async draft => {
            draft.selector = await this.visitDefined(switchExpr.selector, p) as ControlParentheses<Expression>;
            draft.cases = await this.visitDefined(switchExpr.cases, p) as Block;
            draft.type = await this.visitType(switchExpr.type, p);
        });
    }

    protected async visitSynchronized(synch: Synchronized, p: P): Promise<J | undefined> {
        return this.produceJava<Synchronized>(synch, p, async draft => {
            draft.lock = await this.visitDefined(synch.lock, p) as ControlParentheses<Expression>;
            draft.body = await this.visitDefined(synch.body, p) as Block;
        });
    }

    protected async visitTernary(ternary: Ternary, p: P): Promise<J | undefined> {
        return this.produceJava<Ternary>(ternary, p, async draft => {
            draft.condition = await this.visitDefined(ternary.condition, p) as Expression;
            draft.truePart = await this.visitLeftPadded(ternary.truePart, p);
            draft.falsePart = await this.visitLeftPadded(ternary.falsePart, p);
            draft.type = await this.visitType(ternary.type, p);
        });
    }

    protected async visitThrow(thrown: Throw, p: P): Promise<J | undefined> {
        return this.produceJava<Throw>(thrown, p, async draft => {
            draft.exception = await this.visitDefined(thrown.exception, p) as Expression;
        });
    }

    protected async visitTry(tryable: Try, p: P): Promise<J | undefined> {
        return this.produceJava<Try>(tryable, p, async draft => {
            draft.resources = await this.visitContainer(tryable.resources, p);
            draft.body = await this.visitDefined(tryable.body, p) as Block;
            draft.catches = await mapAsync(tryable.catches, c => this.visitDefined<TryCatch>(c, p));
            draft.finally = await this.visitLeftPadded(tryable.finally, p);
        });
    }

    protected async visitTryResource(resource: TryResource, p: P): Promise<J | undefined> {
        return this.produceJava<TryResource>(resource, p, async draft => {
            draft.variableDeclarations = await this.visitDefined(resource.variableDeclarations, p) as TypedTree;
        });
    }

    protected async visitTryCatch(tryCatch: TryCatch, p: P): Promise<J | undefined> {
        return this.produceJava<TryCatch>(tryCatch, p, async draft => {
            draft.parameter = await this.visitDefined(tryCatch.parameter, p) as ControlParentheses<VariableDeclarations>;
            draft.body = await this.visitDefined(tryCatch.body, p) as Block;
        });
    }

    protected async visitTypeCast(typeCast: TypeCast, p: P): Promise<J | undefined> {
        return this.produceJava<TypeCast>(typeCast, p, async draft => {
            draft.clazz = await this.visitDefined(typeCast.clazz, p) as ControlParentheses<TypeTree>;
            draft.expression = await this.visitDefined(typeCast.expression, p) as Expression;
            draft.type = await this.visitType(typeCast.type, p);
        });
    }

    protected async visitTypeParameter(typeParam: TypeParameter, p: P): Promise<J | undefined> {
        return this.produceJava<TypeParameter>(typeParam, p, async draft => {
            draft.annotations = await mapAsync(typeParam.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.modifiers = await mapAsync(typeParam.modifiers, m => this.visitDefined<Modifier>(m, p));
            draft.name = await this.visitDefined(typeParam.name, p) as Identifier;
            draft.bounds = await this.visitContainer(typeParam.bounds, p);
        });
    }

    protected async visitTypeParameters(typeParams: TypeParameters, p: P): Promise<J | undefined> {
        return this.produceJava<TypeParameters>(typeParams, p, async draft => {
            draft.annotations = await mapAsync(typeParams.annotations, a => this.visitDefined<Annotation>(a, p));
            draft.typeParameters = await mapAsync(typeParams.typeParameters, tp => this.visitRightPadded(tp, p));
        });
    }

    protected async visitUnary(unary: Unary, p: P): Promise<J | undefined> {
        return this.produceJava<Unary>(unary, p, async draft => {
            draft.operator = await this.visitLeftPadded(unary.operator, p);
            draft.expression = await this.visitDefined(unary.expression, p) as Expression;
            draft.type = await this.visitType(unary.type, p);
        });
    }

    protected async visitUnknown(unknown: Unknown, p: P): Promise<J | undefined> {
        return this.produceJava<Unknown>(unknown, p, async draft => {
            draft.source = await this.visitDefined(unknown.source, p) as UnknownSource;
        });
    }

    protected async visitUnknownSource(source: UnknownSource, p: P): Promise<J | undefined> {
        return this.produceJava<UnknownSource>(source, p);
    }

    protected async visitVariableDeclarations(varDecs: VariableDeclarations, p: P): Promise<J | undefined> {
        return this.produceJava<VariableDeclarations>(varDecs, p, async draft => {
            draft.leadingAnnotations = await mapAsync(varDecs.leadingAnnotations, a => this.visitDefined<Annotation>(a, p));
            draft.modifiers = await mapAsync(varDecs.modifiers, m => this.visitDefined<Modifier>(m, p));

            if (varDecs.typeExpression) {
                draft.typeExpression = await this.visitDefined(varDecs.typeExpression, p) as TypeTree;
            }

            if (varDecs.varargs) {
                draft.varargs = await this.visitSpace(varDecs.varargs, p);
            }

            draft.variables = await mapAsync(varDecs.variables, v => this.visitRightPadded(v, p));
        });
    }

    protected async visitVariable(variable: Variable, p: P): Promise<J | undefined> {
        return this.produceJava<Variable>(variable, p, async draft => {
            draft.name = await this.visitDefined(variable.name, p) as Identifier;
            draft.dimensionsAfterName = await mapAsync(variable.dimensionsAfterName, dim => this.visitLeftPadded(dim, p));
            draft.initializer = await this.visitLeftPadded(variable.initializer, p);
            draft.variableType = await this.visitType(variable.variableType, p) as JavaType.Variable | undefined;
        });
    }

    protected async visitWhileLoop(whileLoop: WhileLoop, p: P): Promise<J | undefined> {
        return this.produceJava<WhileLoop>(whileLoop, p, async draft => {
            draft.condition = await this.visitDefined(whileLoop.condition, p) as ControlParentheses<Expression>;
            draft.body = await this.visitRightPadded(whileLoop.body, p);
        });
    }

    protected async visitWildcard(wildcard: Wildcard, p: P): Promise<J | undefined> {
        return this.produceJava<Wildcard>(wildcard, p, async draft => {
            draft.bound = await this.visitLeftPadded(wildcard.bound, p);
            if (wildcard.boundedType) {
                draft.boundedType = await this.visitTypeName(wildcard.boundedType, p);
            }
            draft.type = await this.visitType(wildcard.type, p) as JavaType;
        });
    }

    protected async visitYield(yield_: Yield, p: P): Promise<J | undefined> {
        return this.produceJava<Yield>(yield_, p, async draft => {
            draft.value = await this.visitDefined(yield_.value, p) as Expression;
        });
    }

    protected async visitRightPadded<T extends J>(right: JRightPadded<T>, p: P): Promise<JRightPadded<T>>;
    protected async visitRightPadded<T extends boolean>(right: JRightPadded<T>, p: P): Promise<JRightPadded<T>>;
    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T> | undefined, p: P): Promise<JRightPadded<T> | undefined>;
    protected async visitRightPadded<T extends J | boolean>(right: JRightPadded<T> | undefined, p: P): Promise<JRightPadded<T> | undefined> {
        if (!right) return undefined;
        return produceAsync<JRightPadded<T>>(right, async draft => {
            if (isJava(right.element)) {
                (draft.element as J) = await this.visitDefined(right.element, p);
            }
            draft.after = await this.visitSpace(right.after, p);
        });
    }

    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T>, p: P): Promise<JLeftPadded<T>>;
    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T> | undefined, p: P): Promise<JLeftPadded<T> | undefined>;
    protected async visitLeftPadded<T extends J | Space | number | boolean>(left: JLeftPadded<T> | undefined, p: P): Promise<JLeftPadded<T> | undefined> {
        if (!left) return undefined;
        return produceAsync<JLeftPadded<T>>(left, async draft => {
            draft.before = await this.visitSpace(left.before, p);
            if (isJava(left.element)) {
                draft.element = await this.visitDefined(left.element, p) as Draft<T>;
            } else if (isSpace(left.element)) {
                draft.element = await this.visitSpace(left.element, p) as Draft<T>;
            }
        });
    }

    protected async visitContainer<T extends J>(container: JContainer<T>, p: P): Promise<JContainer<T>>;
    protected async visitContainer<T extends J>(container: JContainer<T> | undefined, p: P): Promise<JContainer<T> | undefined>;
    protected async visitContainer<T extends J>(container: JContainer<T> | undefined, p: P): Promise<JContainer<T> | undefined> {
        if (!container) return undefined;
        return produceAsync<JContainer<T>>(container, async draft => {
            draft.before = await this.visitSpace(container.before, p);
            (draft.elements as JRightPadded<J>[]) = await mapAsync(container.elements, e => this.visitRightPadded(e, p));
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
            case JavaKind.AnnotatedType:
                return this.visitAnnotatedType(t as AnnotatedType, p);
            case JavaKind.Annotation:
                return this.visitAnnotation(t as Annotation, p);
            case JavaKind.ArrayAccess:
                return this.visitArrayAccess(t as ArrayAccess, p);
            case JavaKind.ArrayDimension:
                return this.visitArrayDimension(t as ArrayDimension, p);
            case JavaKind.ArrayType:
                return this.visitArrayType(t as ArrayType, p);
            case JavaKind.Assert:
                return this.visitAssert(t as Assert, p);
            case JavaKind.Assignment:
                return this.visitAssignment(t as Assignment, p);
            case JavaKind.AssignmentOperation:
                return this.visitAssignmentOperation(t as AssignmentOperation, p);
            case JavaKind.Binary:
                return this.visitBinary(t as Binary, p);
            case JavaKind.Block:
                return this.visitBlock(t as Block, p);
            case JavaKind.Break:
                return this.visitBreak(t as Break, p);
            case JavaKind.Case:
                return this.visitCase(t as Case, p);
            case JavaKind.ClassDeclaration:
                return this.visitClassDeclaration(t as ClassDeclaration, p);
            case JavaKind.ClassDeclarationKind:
                return this.visitClassDeclarationKind(t as ClassDeclarationKind, p);
            case JavaKind.CompilationUnit:
                return this.visitCompilationUnit(t as CompilationUnit, p);
            case JavaKind.Continue:
                return this.visitContinue(t as Continue, p);
            case JavaKind.ControlParentheses:
                return this.visitControlParentheses(t as ControlParentheses<J>, p);
            case JavaKind.DeconstructionPattern:
                return this.visitDeconstructionPattern(t as DeconstructionPattern, p);
            case JavaKind.DoWhileLoop:
                return this.visitDoWhileLoop(t as DoWhileLoop, p);
            case JavaKind.Empty:
                return this.visitEmpty(t as Empty, p);
            case JavaKind.EnumValue:
                return this.visitEnumValue(t as EnumValue, p);
            case JavaKind.EnumValueSet:
                return this.visitEnumValueSet(t as EnumValueSet, p);
            case JavaKind.Erroneous:
                return this.visitErroneous(t as Erroneous, p);
            case JavaKind.FieldAccess:
                return this.visitFieldAccess(t as FieldAccess, p);
            case JavaKind.ForEachLoop:
                return this.visitForEachLoop(t as ForEachLoop, p);
            case JavaKind.ForEachLoopControl:
                return this.visitForEachLoopControl(t as ForEachLoopControl, p);
            case JavaKind.ForLoop:
                return this.visitForLoop(t as ForLoop, p);
            case JavaKind.ForLoopControl:
                return this.visitForLoopControl(t as ForLoopControl, p);
            case JavaKind.Identifier:
                return this.visitIdentifier(t as Identifier, p);
            case JavaKind.IdentifierWithAnnotations:
                return this.visitIdentifierWithAnnotations(t as IdentifierWithAnnotations, p);
            case JavaKind.If:
                return this.visitIf(t as If, p);
            case JavaKind.IfElse:
                return this.visitIfElse(t as IfElse, p);
            case JavaKind.Import:
                return this.visitImport(t as Import, p);
            case JavaKind.InstanceOf:
                return this.visitInstanceOf(t as InstanceOf, p);
            case JavaKind.IntersectionType:
                return this.visitIntersectionType(t as IntersectionType, p);
            case JavaKind.Label:
                return this.visitLabel(t as Label, p);
            case JavaKind.Lambda:
                return this.visitLambda(t as Lambda, p);
            case JavaKind.LambdaParameters:
                return this.visitLambdaParameters(t as LambdaParameters, p);
            case JavaKind.Literal:
                return this.visitLiteral(t as Literal, p);
            case JavaKind.MemberReference:
                return this.visitMemberReference(t as MemberReference, p);
            case JavaKind.MethodDeclaration:
                return this.visitMethodDeclaration(t as MethodDeclaration, p);
            case JavaKind.MethodInvocation:
                return this.visitMethodInvocation(t as MethodInvocation, p);
            case JavaKind.Modifier:
                return this.visitModifier(t as Modifier, p);
            case JavaKind.MultiCatch:
                return this.visitMultiCatch(t as MultiCatch, p);
            case JavaKind.NewArray:
                return this.visitNewArray(t as NewArray, p);
            case JavaKind.NewClass:
                return this.visitNewClass(t as NewClass, p);
            case JavaKind.NullableType:
                return this.visitNullableType(t as NullableType, p);
            case JavaKind.Package:
                return this.visitPackage(t as Package, p);
            case JavaKind.ParameterizedType:
                return this.visitParameterizedType(t as ParameterizedType, p);
            case JavaKind.Parentheses:
                return this.visitParentheses(t as Parentheses<J>, p);
            case JavaKind.ParenthesizedTypeTree:
                return this.visitParenthesizedTypeTree(t as ParenthesizedTypeTree, p);
            case JavaKind.Primitive:
                return this.visitPrimitive(t as Primitive, p);
            case JavaKind.Return:
                return this.visitReturn(t as Return, p);
            case JavaKind.Switch:
                return this.visitSwitch(t as Switch, p);
            case JavaKind.SwitchExpression:
                return this.visitSwitchExpression(t as SwitchExpression, p);
            case JavaKind.Synchronized:
                return this.visitSynchronized(t as Synchronized, p);
            case JavaKind.Ternary:
                return this.visitTernary(t as Ternary, p);
            case JavaKind.Throw:
                return this.visitThrow(t as Throw, p);
            case JavaKind.Try:
                return this.visitTry(t as Try, p);
            case JavaKind.TryResource:
                return this.visitTryResource(t as TryResource, p);
            case JavaKind.TryCatch:
                return this.visitTryCatch(t as TryCatch, p);
            case JavaKind.TypeCast:
                return this.visitTypeCast(t as TypeCast, p);
            case JavaKind.TypeParameter:
                return this.visitTypeParameter(t as TypeParameter, p);
            case JavaKind.TypeParameters:
                return this.visitTypeParameters(t as TypeParameters, p);
            case JavaKind.Unary:
                return this.visitUnary(t as Unary, p);
            case JavaKind.Unknown:
                return this.visitUnknown(t as Unknown, p);
            case JavaKind.UnknownSource:
                return this.visitUnknownSource(t as UnknownSource, p);
            case JavaKind.VariableDeclarations:
                return this.visitVariableDeclarations(t as VariableDeclarations, p);
            case JavaKind.Variable:
                return this.visitVariable(t as Variable, p);
            case JavaKind.WhileLoop:
                return this.visitWhileLoop(t as WhileLoop, p);
            case JavaKind.Wildcard:
                return this.visitWildcard(t as Wildcard, p);
            case JavaKind.Yield:
                return this.visitYield(t as Yield, p);
            default:
                return Promise.resolve(t);
        }
    }
}