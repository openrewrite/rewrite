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
import {Cursor, mapAsync, produceAsync, SourceFile, TreeVisitor, ValidImmerRecipeReturnType} from "../";
import {
    Annotation,
    AnnotatedType,
    ArrayAccess,
    ArrayDimension,
    ArrayType,
    Assert,
    Assignment,
    Binary,
    Block,
    Break,
    Case,
    ClassDeclaration,
    CompilationUnit,
    Continue,
    DoWhileLoop,
    Empty,
    Expression,
    FieldAccess,
    ForEachLoop,
    ForLoop,
    Identifier,
    If,
    Import,
    InstanceOf,
    isJava,
    J,
    JavaKind,
    JLeftPadded,
    JRightPadded,
    JContainer,
    Label,
    Lambda,
    Literal,
    MemberReference,
    MethodDeclaration,
    MethodInvocation,
    Modifier,
    NameTree,
    NewArray,
    NewClass,
    Package,
    ParameterizedType,
    Parentheses,
    Primitive,
    Return,
    JavaSourceFile,
    Space,
    Statement,
    Switch,
    Synchronized,
    Ternary,
    Throw,
    Try,
    TypeCast,
    TypeParameter,
    TypeTree,
    TypedTree,
    Unary,
    VariableDeclarations,
    WhileLoop,
    Wildcard,
    JavaType
} from "./tree";
import {createDraft, Draft, finishDraft} from "immer";

export class JavaVisitor<P> extends TreeVisitor<J, P> {
    protected javadocVisitor: any | null = null;

    isAcceptable(sourceFile: SourceFile): boolean {
        return isJava(sourceFile);
    }

    protected async visitExpression(expression: Expression, p: P): Promise<J | undefined> {
        return expression;
    }

    protected async visitStatement(statement: Statement, p: P): Promise<J | undefined> {
        return statement;
    }

    protected async visitSpace(space: Space, p: P): Promise<Space> {
        return space;
    }

    protected async visitType(javaType: JavaType | undefined, p: P): Promise<JavaType | undefined> {
        return javaType;
    }

    protected async visitTypeName<N extends NameTree>(nameTree: N, p: P): Promise<N> {
        return nameTree;
    }

    protected async visitTypeNames<J2 extends J>(nameTrees: JContainer<J2> | null, p: P): Promise<JContainer<J2> | null> {
        if (!nameTrees) return null;
        // Implementation for visiting containers of types
        return nameTrees;
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
            if (annotation.arguments) {
                // Handle arguments when JContainer implementation is complete
            }
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
            draft.index = await this.visitDefined(arrayDimension.index, p) as Expression;
        });
    }

    protected async visitArrayType(arrayType: ArrayType, p: P): Promise<J | undefined> {
        return this.produceJava<ArrayType>(arrayType, p, async draft => {
            draft.elementType = await this.visitDefined(arrayType.elementType, p) as TypeTree;
            if (arrayType.annotations) {
                draft.annotations = await mapAsync(arrayType.annotations, a => this.visitDefined<Annotation>(a, p));
            }
            if (arrayType.dimension) {
                // Handle JLeftPadded<Space> when implementation is complete
            }
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

    protected async visitBinary(binary: Binary, p: P): Promise<J | undefined> {
        return this.produceJava<Binary>(binary, p, async draft => {
            draft.left = await this.visitDefined(binary.left, p) as Expression;
            draft.right = await this.visitDefined(binary.right, p) as Expression;
            draft.type = await this.visitType(binary.type, p);
        });
    }

    protected async visitBlock(block: Block, p: P): Promise<J | undefined> {
        return this.produceJava<Block>(block, p, async draft => {
            draft.statements = await mapAsync(block.statements, stmt =>
                this.visitRightPadded(stmt, p) as Promise<JRightPadded<Statement>>);
        });
    }

    protected async visitBreak(breakStatement: Break, p: P): Promise<J | undefined> {
        return this.produceJava<Break>(breakStatement, p, async draft => {
            if (breakStatement.label) {
                draft.label = await this.visitDefined(breakStatement.label, p) as Identifier;
            }
        });
    }

    protected async visitCompilationUnit(cu: CompilationUnit, p: P): Promise<J | undefined> {
        return this.produceJava<CompilationUnit>(cu, p, async draft => {
            if (cu.packageDeclaration) {
                draft.packageDeclaration = await this.visitRightPadded(cu.packageDeclaration, p) as JRightPadded<Package>;
            }
            draft.imports = await mapAsync(cu.imports, imp =>
                this.visitRightPadded(imp, p) as Promise<JRightPadded<Import>>);
            draft.classes = await mapAsync(cu.classes, cls => this.visitDefined(cls, p) as Promise<ClassDeclaration>);
            draft.eof = await this.visitSpace(cu.eof, p);
        });
    }

    // Add more visitor methods following the same pattern
    // Missing methods would be added here...

    protected async visitRightPadded<T extends J>(right: undefined, p: P): Promise<undefined>;
    protected async visitRightPadded<T extends J>(right: JRightPadded<T>, p: P): Promise<JRightPadded<T>>;
    protected async visitRightPadded<T extends J>(right: JRightPadded<T> | undefined, p: P): Promise<JRightPadded<T> | undefined> {
        if (!right) return undefined;
        return produceAsync<JRightPadded<T>>(right, async draft => {
            draft.element = await this.visitDefined(right.element, p);
            draft.after = await this.visitSpace(right.after, p);
        });
    }

    protected async visitLeftPadded<T extends J>(left: JLeftPadded<T>, p: P): Promise<JLeftPadded<T>>;
    protected async visitLeftPadded<T extends J>(left: JLeftPadded<T> | undefined, p: P): Promise<JLeftPadded<T> | undefined>;
    protected async visitLeftPadded<T extends J>(left: JLeftPadded<T> | undefined, p: P): Promise<JLeftPadded<T> | undefined> {
        if (!left) return undefined;
        return produceAsync<JLeftPadded<T>>(left, async draft => {
            draft.before = await this.visitSpace(left.before, p);
            draft.element = await this.visitDefined(left.element, p);
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
            case JavaKind.CompilationUnit:
                return this.visitCompilationUnit(t as CompilationUnit, p);
            case JavaKind.Empty:
                return this.visitEmpty(t as Empty, p);
            case JavaKind.Identifier:
                return this.visitIdentifier(t as Identifier, p);
            // Add more cases for all Java kinds
            default:
                throw new Error(`Unexpected Java kind ${t.kind}`);
        }
    }

    // Add these method stubs to complete the implementation
    protected async visitClassDeclaration(classDecl: ClassDeclaration, p: P): Promise<J | undefined> {
        return this.produceJava<ClassDeclaration>(classDecl, p, async draft => {
            // Implementation would go here
        });
    }

    protected async visitEmpty(empty: Empty, p: P): Promise<J | undefined> {
        return this.produceJava(empty, p);
    }

    protected async visitIdentifier(ident: Identifier, p: P): Promise<J | undefined> {
        return this.produceJava(ident, p);
    }

    protected async visitCase(case_: Case, p: P): Promise<J | undefined> {
        return this.produceJava<Case>(case_, p, async draft => {
            // Implementation would go here
        });
    }
}