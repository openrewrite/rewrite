// noinspection JSUnusedGlobalSymbols

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

import {emptyMarkers, Markers, SourceFile, Tree, TreeKind, UUID} from "../";


type JavaExpressionBase = J.FieldAccess | J.Assignment | J.AssignmentOperation | J.InstanceOf | J.Binary | J.Unary |
    J.Ternary | J.Parentheses<J> | J.ArrayAccess | J.MethodInvocation | J.NewArray | J.NewClass | J.Lambda |
    J.TypeCast | J.ParenthesizedTypeTree | J.MemberReference | J.AnnotatedType | J.ArrayType | J.Literal |
    J.Annotation | J.SwitchExpression | J.Primitive | J.Wildcard | J.IntersectionType | J.NullableType |
    J.ControlParentheses<J> | J.Empty | J.Unknown | J.Erroneous | J.Identifier;

export type Expression<T extends { kind: string } = never> = JavaExpressionBase | T;

type TypedTreeBase = J.ArrayAccess | J.Assignment | J.AssignmentOperation | J.Binary | J.ClassDeclaration |
    J.InstanceOf | J.DeconstructionPattern | J.Lambda | J.Literal | J.MethodDeclaration | J.MethodInvocation |
    J.NewArray | J.NewClass | J.SwitchExpression | J.Ternary | J.TypeCast | J.Unary | J.VariableDeclarations;

export type TypedTree<T extends { kind: string } = never> = TypedTreeBase | T;

export type NameTree = TypedTree;

type TypeTreeBase = J.AnnotatedType | J.ArrayType | J.Empty | J.FieldAccess | J.ParenthesizedTypeTree |
    J.Identifier | J.IntersectionType | J.MultiCatch | J.NullableType | J.ParameterizedType | J.Primitive |
    J.Wildcard | J.Unknown;

export type TypeTree<T extends { kind: string } = never> = TypeTreeBase | T;

export interface J extends Tree {
    readonly prefix: J.Space;
}

export interface JavaSourceFile extends J, SourceFile {
    readonly packageDeclaration: J.RightPadded<J.Package>;
    readonly imports: J.RightPadded<J.Import>[];
    readonly classes: J.ClassDeclaration[];
}

export interface Statement extends J {
}

export namespace J {
    export const Kind = {
        ...TreeKind,
        AnnotatedType: "org.openrewrite.java.tree.J$AnnotatedType",
        Annotation: "org.openrewrite.java.tree.J$Annotation",
        ArrayAccess: "org.openrewrite.java.tree.J$ArrayAccess",
        ArrayDimension: "org.openrewrite.java.tree.J$ArrayDimension",
        ArrayType: "org.openrewrite.java.tree.J$ArrayType",
        Assert: "org.openrewrite.java.tree.J$Assert",
        Assignment: "org.openrewrite.java.tree.J$Assignment",
        AssignmentOperation: "org.openrewrite.java.tree.J$AssignmentOperation",
        Binary: "org.openrewrite.java.tree.J$Binary",
        Block: "org.openrewrite.java.tree.J$Block",
        Break: "org.openrewrite.java.tree.J$Break",
        Case: "org.openrewrite.java.tree.J$Case",
        ClassDeclaration: "org.openrewrite.java.tree.J$ClassDeclaration",
        ClassDeclarationKind: "org.openrewrite.java.tree.J$ClassDeclaration$Kind",
        ControlParentheses: "org.openrewrite.java.tree.J$ControlParentheses",
        CompilationUnit: "org.openrewrite.java.tree.J$CompilationUnit",
        Continue: "org.openrewrite.java.tree.J$Continue",
        DeconstructionPattern: "org.openrewrite.java.tree.J$DeconstructionPattern",
        DoWhileLoop: "org.openrewrite.java.tree.J$DoWhileLoop",
        Empty: "org.openrewrite.java.tree.J$Empty",
        EnumValueSet: "org.openrewrite.java.tree.J$EnumValueSet",
        EnumValue: "org.openrewrite.java.tree.J$EnumValue",
        Erroneous: "org.openrewrite.java.tree.J$Erroneous",
        FieldAccess: "org.openrewrite.java.tree.J$FieldAccess",
        ForEachLoop: "org.openrewrite.java.tree.J$ForEachLoop",
        ForEachLoopControl: "org.openrewrite.java.tree.J$ForEachLoop$Control",
        ForLoop: "org.openrewrite.java.tree.J$ForLoop",
        ForLoopControl: "org.openrewrite.java.tree.J$ForLoop$Control",
        Identifier: "org.openrewrite.java.tree.J$Identifier",
        If: "org.openrewrite.java.tree.J$If",
        IfElse: "org.openrewrite.java.tree.J$If$Else",
        Import: "org.openrewrite.java.tree.J$Import",
        InstanceOf: "org.openrewrite.java.tree.J$InstanceOf",
        IntersectionType: "org.openrewrite.java.tree.J$IntersectionType",
        Label: "org.openrewrite.java.tree.J$Label",
        Lambda: "org.openrewrite.java.tree.J$Lambda",
        LambdaParameters: "org.openrewrite.java.tree.J$Lambda$Parameters",
        Literal: "org.openrewrite.java.tree.J$Literal",
        MemberReference: "org.openrewrite.java.tree.J$MemberReference",
        MethodDeclaration: "org.openrewrite.java.tree.J$MethodDeclaration",
        MethodInvocation: "org.openrewrite.java.tree.J$MethodInvocation",
        Modifier: "org.openrewrite.java.tree.J$Modifier",
        MultiCatch: "org.openrewrite.java.tree.J$MultiCatch",
        NamedVariable: "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable",
        NewArray: "org.openrewrite.java.tree.J$NewArray",
        NewClass: "org.openrewrite.java.tree.J$NewClass",
        NullableType: "org.openrewrite.java.tree.J$NullableType",
        Package: "org.openrewrite.java.tree.J$Package",
        ParameterizedType: "org.openrewrite.java.tree.J$ParameterizedType",
        Parentheses: "org.openrewrite.java.tree.J$Parentheses",
        ParenthesizedTypeTree: "org.openrewrite.java.tree.J$ParenthesizedTypeTree",
        Primitive: "org.openrewrite.java.tree.J$Primitive",
        Return: "org.openrewrite.java.tree.J$Return",
        Switch: "org.openrewrite.java.tree.J$Switch",
        SwitchExpression: "org.openrewrite.java.tree.J$SwitchExpression",
        Synchronized: "org.openrewrite.java.tree.J$Synchronized",
        Ternary: "org.openrewrite.java.tree.J$Ternary",
        TextComment: "org.openrewrite.java.tree.TextComment",
        Throw: "org.openrewrite.java.tree.J$Throw",
        Try: "org.openrewrite.java.tree.J$Try",
        TryResource: "org.openrewrite.java.tree.J$Try$Resource",
        TryCatch: "org.openrewrite.java.tree.J$Try$Catch",
        TypeCast: "org.openrewrite.java.tree.J$TypeCast",
        TypeParameter: "org.openrewrite.java.tree.J$TypeParameter",
        TypeParameters: "org.openrewrite.java.tree.J$TypeParameters",
        Unary: "org.openrewrite.java.tree.J$Unary",
        Unknown: "org.openrewrite.java.tree.J$Unknown",
        UnknownSource: "org.openrewrite.java.tree.J$Unknown$Source",
        VariableDeclarations: "org.openrewrite.java.tree.J$VariableDeclarations",
        WhileLoop: "org.openrewrite.java.tree.J$WhileLoop",
        Wildcard: "org.openrewrite.java.tree.J$Wildcard",
        Yield: "org.openrewrite.java.tree.J$Yield",
        JContainer: "org.openrewrite.java.tree.JContainer",
        JLeftPadded: "org.openrewrite.java.tree.JLeftPadded",
        JRightPadded: "org.openrewrite.java.tree.JRightPadded",
        Space: "org.openrewrite.java.tree.Space",
    } as const;


    export interface AnnotatedType extends J {
        readonly kind: typeof Kind.AnnotatedType;
        readonly annotations: Annotation[];
        readonly typeExpression: TypeTree;
    }

    export interface Annotation extends J {
        readonly kind: typeof Kind.Annotation;
        readonly annotationType: NameTree;
        readonly arguments?: Container<Expression>;
    }

    export interface ArrayAccess extends J {
        readonly kind: typeof Kind.ArrayAccess;
        readonly indexed: Expression;
        readonly dimension: ArrayDimension;
    }

    export interface ArrayDimension extends J {
        readonly kind: typeof Kind.ArrayDimension;
        readonly index: RightPadded<Expression>;
    }

    export interface ArrayType extends J {
        readonly kind: typeof Kind.ArrayType;
        readonly elementType: TypeTree;
        readonly annotations?: Annotation[];
        readonly dimension: LeftPadded<Space>;
        readonly type?: JavaType;
    }

    export interface Assert extends J, Statement {
        readonly kind: typeof Kind.Assert;
        readonly condition: Expression;
        readonly detail?: LeftPadded<Expression>;
    }

    export interface Assignment extends J, Statement {
        readonly kind: typeof Kind.Assignment;
        readonly variable: Expression;
        readonly assignment: LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface AssignmentOperation extends J, Statement {
        readonly kind: typeof Kind.AssignmentOperation;
        readonly variable: Expression;
        readonly operator: LeftPadded<AssignmentOperation.Type>;
        readonly assignment: Expression;
        readonly type?: JavaType;
    }

    export namespace AssignmentOperation {
        export const enum Type {
            Addition,
            BitAnd,
            BitOr,
            BitXor,
            Division,
            Exponentiation,    // Raises the left operand to the power of the right operand. Used in Python
            FloorDivision,     // Division rounding down to the nearest integer. Used in Python
            LeftShift,
            MatrixMultiplication, // Matrix multiplication. Used in Python
            Modulo,
            Multiplication,
            RightShift,
            Subtraction,
            UnsignedRightShift
        }
    }

    export interface Binary extends J {
        readonly kind: typeof Kind.Binary;
        readonly left: Expression;
        readonly operator: LeftPadded<Binary.Type>;
        readonly right: Expression;
        readonly type?: JavaType;
    }

    export namespace Binary {
        export const enum Type {
            Addition,
            Subtraction,
            Multiplication,
            Division,
            Modulo,
            LessThan,
            GreaterThan,
            LessThanOrEqual,
            GreaterThanOrEqual,
            Equal,
            NotEqual,
            BitAnd,
            BitOr,
            BitXor,
            LeftShift,
            RightShift,
            UnsignedRightShift,
            Or,
            And
        }
    }

    export interface Block extends J {
        readonly kind: typeof Kind.Block;
        readonly static: RightPadded<boolean>;
        readonly statements: RightPadded<Statement>[];
        readonly end: J.Space;
    }

    export interface Break extends J, Statement {
        readonly kind: typeof Kind.Break;
        readonly label?: Identifier;
    }

    export interface Case extends J {
        readonly kind: typeof Kind.Case;
        readonly type: Case.Type;
        readonly caseLabels: Container<J>;
        readonly statements: Container<Statement>;
        readonly body?: RightPadded<J>;
        readonly guard?: Expression;
    }

    export namespace Case {
        export const enum Type {
            Statement,
            Rule
        }
    }

    export interface ClassDeclaration extends J {
        readonly kind: typeof Kind.ClassDeclaration;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly classKind: ClassDeclarationKind;
        readonly name: Identifier;
        readonly typeParameters?: Container<TypeParameter>;
        readonly primaryConstructor?: Container<Statement>;
        readonly extends?: LeftPadded<TypeTree>;
        readonly implements?: Container<TypeTree>;
        readonly permitting?: Container<TypeTree>;
        readonly body: Block;
        readonly type?: JavaType.Class;
    }

    export interface ClassDeclarationKind extends J {
        readonly kind: typeof Kind.ClassDeclarationKind;
        readonly annotations: Annotation[];
        readonly type: ClassType;
    }

    export interface CompilationUnit extends JavaSourceFile, J {
        readonly kind: typeof Kind.CompilationUnit;
        readonly eof: Space;
    }

    export interface Continue extends J, Statement {
        readonly kind: typeof Kind.Continue;
        readonly label?: Identifier;
    }

    export interface DoWhileLoop extends J, Statement {
        readonly kind: typeof Kind.DoWhileLoop;
        readonly body: RightPadded<Statement>;
        readonly whileCondition: LeftPadded<ControlParentheses<Expression>>;
    }

    export interface Empty extends J, Statement {
        readonly kind: typeof Kind.Empty;
    }

    export interface EnumValue extends J {
        readonly kind: typeof Kind.EnumValue;
        readonly annotations: Annotation[];
        readonly name: Identifier;
        readonly initializer?: NewClass;
    }

    export interface EnumValueSet extends J, Statement {
        readonly kind: typeof Kind.EnumValueSet;
        readonly enums: RightPadded<EnumValue>[];
        readonly terminatedWithSemicolon: boolean;
    }

    export interface FieldAccess extends Statement {
        readonly kind: typeof Kind.FieldAccess;
        readonly target: Expression;
        readonly name: LeftPadded<Identifier>;
        readonly type?: JavaType;
    }

    export interface ForEachLoop extends J, Statement {
        readonly kind: typeof Kind.ForEachLoop;
        readonly control: ForEachLoop.Control;
        readonly body: RightPadded<Statement>;
    }

    export namespace ForEachLoop {
        export interface Control extends J {
            readonly kind: typeof Kind.ForEachLoopControl;
            readonly variable: RightPadded<VariableDeclarations>;
            readonly iterable: RightPadded<Expression>;
        }
    }

    export interface ForLoop extends J, Statement {
        readonly kind: typeof Kind.ForLoop;
        readonly control: ForLoop.Control;
        readonly body: RightPadded<Statement>;
    }

    export namespace ForLoop {
        export interface Control extends J {
            readonly kind: typeof Kind.ForLoopControl;
            readonly init: RightPadded<Statement>[];
            readonly condition?: RightPadded<Expression>;
            readonly update: RightPadded<Statement>[];
        }
    }

    export interface ParenthesizedTypeTree extends J {
        readonly kind: typeof Kind.ParenthesizedTypeTree;
        readonly annotations: Annotation[];
        readonly parenthesizedType: Parentheses<TypeTree>;
    }

    export interface Identifier extends J {
        readonly kind: typeof Kind.Identifier;
        readonly annotations: Annotation[];
        readonly simpleName: string;
        readonly type?: JavaType;
        readonly fieldType?: JavaType.Variable;
    }

    export interface If extends J, Statement {
        readonly kind: typeof Kind.If;
        readonly ifCondition: ControlParentheses<Expression>;
        readonly thenPart: RightPadded<Statement>;
        readonly elsePart?: If.Else;
    }

    export namespace If {
        export interface Else extends J {
            readonly kind: typeof Kind.IfElse;
            readonly body: RightPadded<Statement>;
        }
    }

    export interface Import extends J {
        readonly kind: typeof Kind.Import;
        readonly static: LeftPadded<boolean>;
        readonly qualid: FieldAccess;
        readonly alias?: LeftPadded<Identifier>;
    }

    export interface InstanceOf extends J {
        readonly kind: typeof Kind.InstanceOf;
        readonly expression: RightPadded<Expression>;
        readonly clazz: J;
        readonly pattern?: J;
        readonly type?: JavaType;
        readonly modifier?: Modifier;
    }

    export interface DeconstructionPattern extends J {
        readonly kind: typeof Kind.DeconstructionPattern;
        readonly deconstructor: Expression;
        readonly nested: Container<J>;
        readonly type?: JavaType;
    }

    export interface IntersectionType extends J {
        readonly kind: typeof Kind.IntersectionType;
        readonly bounds: Container<TypeTree>;
        readonly type?: JavaType;
    }

    export interface Label extends J, Statement {
        readonly kind: typeof Kind.Label;
        readonly label: RightPadded<Identifier>;
        readonly statement: Statement;
    }

    export interface Lambda extends J, Statement {
        readonly kind: typeof Kind.Lambda;
        readonly parameters: Lambda.Parameters;
        readonly arrow: Space;
        readonly body: Statement | Expression;
        readonly type?: JavaType;
    }

    export namespace Lambda {
        export interface Parameters extends J {
            readonly kind: typeof Kind.LambdaParameters;
            readonly parenthesized: boolean;
            readonly parameters: RightPadded<J>[];
        }
    }

    export interface Literal extends J {
        readonly kind: typeof Kind.Literal;
        readonly value?: Object;
        readonly valueSource?: string;
        readonly unicodeEscapes?: LiteralUnicodeEscape[];
        readonly type?: JavaType.Primitive;
    }

    export interface LiteralUnicodeEscape {
        readonly valueSourceIndex: number;
        readonly codePoint: string;
    }

    export interface MemberReference extends J {
        readonly kind: typeof Kind.MemberReference;
        readonly containing: RightPadded<Expression>;
        readonly typeParameters?: Container<Expression>;
        readonly reference: LeftPadded<Identifier>;
        readonly type?: JavaType;
        readonly methodType?: JavaType.Method;
        readonly variableType?: JavaType.Variable;
    }

    export interface MethodDeclaration extends J {
        readonly kind: typeof Kind.MethodDeclaration;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly typeParameters?: TypeParameters;
        readonly returnTypeExpression?: TypeTree;
        readonly nameAnnotations: Annotation[];
        readonly name: Identifier;
        readonly parameters: Container<Statement>;
        readonly throws: Container<NameTree>;
        readonly body?: Block;
        readonly defaultValue?: LeftPadded<Expression>;
        readonly methodType?: JavaType.Method;
    }

    export interface MethodInvocation extends J {
        readonly kind: typeof Kind.MethodInvocation;
        readonly select?: RightPadded<Expression>;
        readonly typeParameters?: Container<Expression>;
        readonly name: Identifier;
        readonly arguments: Container<Expression>;
        readonly methodType?: JavaType.Method;
    }

    export interface Modifier extends J {
        readonly kind: typeof Kind.Modifier;
        readonly keyword?: string;
        readonly type: ModifierType;
        readonly annotations: Annotation[];
    }

    export const enum ModifierType {
        Default,
        Public,
        Protected,
        Private,
        Abstract,
        Static,
        Final,
        Sealed,
        NonSealed,
        Transient,
        Volatile,
        Synchronized,
        Native,
        Strictfp,
        Async,
        Reified,
        Inline,
        /**
         * For modifiers not seen in Java this is used in conjunction with "keyword"
         */
        LanguageExtension
    }

    export interface MultiCatch extends J {
        readonly kind: typeof Kind.MultiCatch;
        readonly alternatives: RightPadded<NameTree>[];
    }

    export interface NewArray extends J {
        readonly kind: typeof Kind.NewArray;
        readonly typeExpression?: TypeTree;
        readonly dimensions: ArrayDimension[];
        readonly initializer?: Container<Expression>;
        readonly type?: JavaType;
    }

    export interface NewClass extends J {
        readonly kind: typeof Kind.NewClass;
        readonly enclosing?: RightPadded<Expression>;
        readonly new: Space;
        readonly clazz?: TypeTree;
        readonly arguments: Container<Expression>;
        readonly body?: Block;
        readonly constructorType?: JavaType.Method;
    }

    export interface NullableType extends J {
        readonly kind: typeof Kind.NullableType;
        readonly annotations: Annotation[];
        readonly typeTree: RightPadded<TypeTree>;
    }

    export interface Package extends J {
        readonly kind: typeof Kind.Package;
        readonly expression: Expression;
        readonly annotations?: Annotation[];
    }

    export interface ParameterizedType extends J {
        readonly kind: typeof Kind.ParameterizedType;
        readonly clazz: NameTree;
        readonly typeParameters?: Container<Expression>;
        readonly type?: JavaType;
    }

    export interface Parentheses<J2 extends J> extends J {
        readonly kind: typeof Kind.Parentheses;
        readonly tree: RightPadded<J2>;
    }

    export interface ControlParentheses<J2 extends J> extends J {
        readonly kind: typeof Kind.ControlParentheses;
        readonly tree: RightPadded<J2>;
    }

    export interface Primitive extends J {
        readonly kind: typeof Kind.Primitive;
        readonly type: JavaType.Primitive;
    }

    export interface Return extends J, Statement {
        readonly kind: typeof Kind.Return;
        readonly expression?: Expression;
    }

    export interface Switch extends J, Statement {
        readonly kind: typeof Kind.Switch;
        readonly selector: ControlParentheses<Expression>;
        readonly cases: Block;
    }

    export interface SwitchExpression extends J {
        readonly kind: typeof Kind.SwitchExpression;
        readonly selector: ControlParentheses<Expression>;
        readonly cases: Block;
        readonly type?: JavaType;
    }

    export interface Synchronized extends J, Statement {
        readonly kind: typeof Kind.Synchronized;
        readonly lock: ControlParentheses<Expression>;
        readonly body: Block;
    }

    export interface Ternary extends J, Statement {
        readonly kind: typeof Kind.Ternary;
        readonly condition: Expression;
        readonly truePart: LeftPadded<Expression>;
        readonly falsePart: LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface Throw extends J, Statement {
        readonly kind: typeof Kind.Throw;
        readonly exception: Expression;
    }

    export interface Try extends J, Statement {
        readonly kind: typeof Kind.Try;
        readonly resources?: Container<TryResource>;
        readonly body: Block;
        readonly catches: TryCatch[];
        readonly finally?: LeftPadded<Block>;
    }

    export interface TryResource extends J {
        readonly kind: typeof Kind.TryResource;
        readonly variableDeclarations: TypedTree;
        readonly terminatedWithSemicolon: boolean;
    }

    export interface TryCatch extends J {
        readonly kind: typeof Kind.TryCatch;
        readonly parameter: ControlParentheses<VariableDeclarations>;
        readonly body: Block;
    }

    export interface TypeCast extends J {
        readonly kind: typeof Kind.TypeCast;
        readonly clazz: ControlParentheses<TypeTree>;
        readonly expression: Expression;
    }

    export interface TypeParameter extends J {
        readonly kind: typeof Kind.TypeParameter;
        readonly annotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly name: Expression;
        readonly bounds?: Container<TypeTree>;
    }

    export interface TypeParameters extends J {
        readonly kind: typeof Kind.TypeParameters;
        readonly annotations: Annotation[];
        readonly typeParameters: RightPadded<TypeParameter>[];
    }

    export interface Unary extends J, Statement {
        readonly kind: typeof Kind.Unary;
        readonly operator: LeftPadded<Unary.Type>;
        readonly expression: Expression;
        readonly type?: JavaType;
    }


    export namespace Unary {
        export const enum Type {
            PreIncrement,
            PreDecrement,
            PostIncrement,
            PostDecrement,
            Positive,
            Negative,
            Complement,
            Not
        }
    }

    export function newAssignmentOperation(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        variable: Expression,
        operator: J.LeftPadded<J.AssignmentOperation.Type>,
        assignment: Expression,
        type?: JavaType
    ): J.AssignmentOperation {
        return {
            kind: J.Kind.AssignmentOperation,
            id,
            prefix,
            markers,
            variable,
            operator,
            assignment,
            type
        };
    }

    export function newBinary(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        left: Expression,
        operator: J.LeftPadded<J.Binary.Type>,
        right: Expression,
        type?: JavaType
    ): J.Binary {
        return {
            kind: J.Kind.Binary,
            id,
            prefix,
            markers,
            left,
            operator,
            right,
            type
        };
    }

    export function newTernary(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        condition: Expression,
        truePart: J.LeftPadded<Expression>,
        falsePart: J.LeftPadded<Expression>,
        type?: JavaType
    ): J.Ternary {
        return {
            kind: J.Kind.Ternary,
            id,
            prefix,
            markers,
            condition,
            truePart,
            falsePart,
            type
        };
    }

    export function newBlock(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        static_: J.RightPadded<boolean>,
        statements: J.RightPadded<Statement>[],
        end: J.Space
    ): J.Block {
        return {
            kind: J.Kind.Block,
            id,
            prefix,
            markers,
            static: static_,
            statements,
            end
        };
    }

    export function newFieldAccess(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        target: Expression,
        name: J.LeftPadded<J.Identifier>,
        type?: JavaType
    ): J.FieldAccess {
        return {
            kind: J.Kind.FieldAccess,
            id,
            prefix,
            markers,
            target,
            name,
            type
        };
    }

    export function newForEachLoop(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        control: J.ForEachLoop.Control,
        body: J.RightPadded<Statement>
    ): J.ForEachLoop {
        return {
            kind: J.Kind.ForEachLoop,
            id,
            prefix,
            markers,
            control,
            body
        };
    }

    export function newForEachLoopControl(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        variable: J.RightPadded<J.VariableDeclarations>,
        iterable: J.RightPadded<Expression>
    ): J.ForEachLoop.Control {
        return {
            kind: J.Kind.ForEachLoopControl,
            id,
            prefix,
            markers,
            variable,
            iterable
        };
    }

    export function newForLoop(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        control: J.ForLoop.Control,
        body: J.RightPadded<Statement>
    ): J.ForLoop {
        return {
            kind: J.Kind.ForLoop,
            id,
            prefix,
            markers,
            control,
            body
        };
    }

    export function newForLoopControl(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        init: J.RightPadded<Statement>[],
        condition: J.RightPadded<Expression> | undefined,
        update: J.RightPadded<Statement>[]
    ): J.ForLoop.Control {
        return {
            kind: J.Kind.ForLoopControl,
            id,
            prefix,
            markers,
            init,
            condition,
            update
        };
    }

    export function newParenthesizedTypeTree(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotations: J.Annotation[],
        parenthesizedType: J.Parentheses<TypeTree>,
        type?: JavaType
    ): J.ParenthesizedTypeTree {
        return {
            kind: J.Kind.ParenthesizedTypeTree,
            id,
            prefix,
            markers,
            annotations,
            parenthesizedType
        };
    }

    export function newIdentifier(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotations: J.Annotation[],
        simpleName: string,
        type?: JavaType,
        fieldType?: JavaType.Variable
    ): J.Identifier {
        return {
            kind: J.Kind.Identifier,
            id,
            prefix,
            markers,
            annotations,
            simpleName,
            type,
            fieldType
        };
    }

    export function newIf(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        ifCondition: J.ControlParentheses<Expression>,
        thenPart: J.RightPadded<Statement>,
        elsePart?: J.If.Else
    ): J.If {
        return {
            kind: J.Kind.If,
            id,
            prefix,
            markers,
            ifCondition,
            thenPart,
            elsePart
        };
    }

    export function newIfElse(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        body: J.RightPadded<Statement>
    ): J.If.Else {
        return {
            kind: J.Kind.IfElse,
            id,
            prefix,
            markers,
            body
        };
    }

    export function newImport(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        static_: J.LeftPadded<boolean>,
        qualid: J.FieldAccess,
        alias?: J.LeftPadded<J.Identifier>
    ): J.Import {
        return {
            kind: J.Kind.Import,
            id,
            prefix,
            markers,
            static: static_,
            qualid,
            alias
        };
    }

    export function newInstanceOf(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: J.RightPadded<Expression>,
        clazz: J,
        pattern?: J,
        type?: JavaType,
        modifier?: J.Modifier
    ): J.InstanceOf {
        return {
            kind: J.Kind.InstanceOf,
            id,
            prefix,
            markers,
            expression,
            clazz,
            pattern,
            type,
            modifier
        };
    }

    export function newClassDeclaration(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        leadingAnnotations: J.Annotation[],
        modifiers: J.Modifier[],
        classKind: J.ClassDeclarationKind,
        name: J.Identifier,
        typeParameters: J.Container<J.TypeParameter> | undefined,
        primaryConstructor: J.Container<Statement> | undefined,
        extends_: J.LeftPadded<TypeTree> | undefined,
        implements_: J.Container<TypeTree> | undefined,
        permitting: J.Container<TypeTree> | undefined,
        body: J.Block,
        type?: JavaType.Class
    ): J.ClassDeclaration {
        return {
            kind: J.Kind.ClassDeclaration,
            id,
            prefix,
            markers,
            leadingAnnotations,
            modifiers,
            classKind,
            name,
            typeParameters,
            primaryConstructor,
            extends: extends_,
            implements: implements_,
            permitting,
            body,
            type
        };
    }

    export function newClassDeclarationKind(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotations: J.Annotation[],
        type: J.ClassType
    ): J.ClassDeclarationKind {
        return {
            kind: J.Kind.ClassDeclarationKind,
            id,
            prefix,
            markers,
            annotations,
            type
        };
    }

    export interface VariableDeclarations extends J {
        readonly kind: typeof Kind.VariableDeclarations;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly typeExpression?: TypeTree;
        readonly varargs?: Space;
        readonly variables: RightPadded<VariableDeclarations.NamedVariable>[];
    }

    export function newVariableDeclarations(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        leadingAnnotations: J.Annotation[],
        modifiers: J.Modifier[],
        typeExpression: TypeTree | undefined,
        varargs: J.Space | undefined,
        variables: J.RightPadded<VariableDeclarations.NamedVariable>[]
    ): J.VariableDeclarations {
        return {
            kind: J.Kind.VariableDeclarations,
            id,
            prefix,
            markers,
            leadingAnnotations,
            modifiers,
            typeExpression,
            varargs,
            variables
        };
    }

    export namespace VariableDeclarations {
        export interface NamedVariable extends J {
            readonly kind: typeof Kind.NamedVariable;
            readonly name: Identifier;
            readonly dimensionsAfterName: LeftPadded<Space>[];
            readonly initializer?: LeftPadded<Expression>;
            readonly variableType?: JavaType.Variable;
        }
    }

    export function newNamedVariable(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        name: J.Identifier,
        dimensionsAfterName: J.LeftPadded<J.Space>[],
        initializer?: J.LeftPadded<Expression>,
        variableType?: JavaType.Variable
    ): J.VariableDeclarations.NamedVariable {
        return {
            kind: J.Kind.NamedVariable,
            id,
            prefix,
            markers,
            name,
            dimensionsAfterName,
            initializer,
            variableType
        };
    }

    export interface WhileLoop extends J, Statement {
        readonly kind: typeof Kind.WhileLoop;
        readonly condition: ControlParentheses<Expression>;
        readonly body: RightPadded<Statement>;
    }

    export interface Wildcard extends J {
        readonly kind: typeof Kind.Wildcard;
        readonly bound?: LeftPadded<WildcardBound>;
        readonly boundedType?: NameTree;
    }

    export const enum WildcardBound {
        Extends,
        Super
    }

    export interface Yield extends J, Statement {
        readonly kind: typeof Kind.Yield;
        readonly implicit: boolean;
        readonly value: Expression;
    }

    export interface Unknown extends J, Statement {
        readonly kind: typeof Kind.Unknown;
        readonly source: UnknownSource;
    }

    export interface UnknownSource extends J {
        readonly kind: typeof Kind.UnknownSource;
        readonly text: string;
    }

    export interface Erroneous extends J {
        readonly kind: typeof Kind.Erroneous;
        readonly text: string;
    }

    export const enum ClassType {
        Class,
        Interface,
        Enum,
        Annotation,
        Record,
        Value
    }

    export interface LeftPadded<T extends J | Space | number | boolean> {
        readonly kind: typeof Kind.JLeftPadded;
        readonly before: Space;
        readonly element: T;
        readonly markers: Markers;
    }

    export interface RightPadded<T extends J | boolean> {
        readonly kind: typeof Kind.JRightPadded;
        readonly element: T;
        readonly after: Space;
        readonly markers: Markers;
    }

    export interface Container<T extends J> {
        readonly kind: typeof Kind.JContainer;
        readonly before: Space;
        readonly elements: RightPadded<T>[];
        readonly markers: Markers;
    }

    export interface Space {
        readonly kind: typeof J.Kind.Space;
        readonly comments: Comment[];
        readonly whitespace: string;
    }

    // === Inserted factory functions ===
    export function newParameterizedType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        clazz: NameTree,
        typeParameters?: J.Container<Expression>,
        type?: JavaType
    ): J.ParameterizedType {
        return {
            kind: J.Kind.ParameterizedType,
            id,
            prefix,
            markers,
            clazz,
            typeParameters,
            type
        };
    }

    export function newParentheses<T extends J>(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        tree: J.RightPadded<T>
    ): J.Parentheses<T> {
        return {
            kind: J.Kind.Parentheses,
            id,
            prefix,
            markers,
            tree
        };
    }

    export function newControlParentheses<T extends J>(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        tree: J.RightPadded<T>
    ): J.ControlParentheses<T> {
        return {
            kind: J.Kind.ControlParentheses,
            id,
            prefix,
            markers,
            tree
        };
    }

    export function newPrimitive(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        type: JavaType.Primitive
    ): J.Primitive {
        return {
            kind: J.Kind.Primitive,
            id,
            prefix,
            markers,
            type
        };
    }

    export function newReturn(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression?: Expression
    ): J.Return {
        return {
            kind: J.Kind.Return,
            id,
            prefix,
            markers,
            expression
        };
    }

    export function newSwitch(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        selector: J.ControlParentheses<Expression>,
        cases: J.Block
    ): J.Switch {
        return {
            kind: J.Kind.Switch,
            id,
            prefix,
            markers,
            selector,
            cases
        };
    }

    export function newSwitchExpression(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        selector: J.ControlParentheses<Expression>,
        cases: J.Block,
        type?: JavaType
    ): J.SwitchExpression {
        return {
            kind: J.Kind.SwitchExpression,
            id,
            prefix,
            markers,
            selector,
            cases,
            type
        };
    }

    export function newSynchronized(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        lock: J.ControlParentheses<Expression>,
        body: J.Block
    ): J.Synchronized {
        return {
            kind: J.Kind.Synchronized,
            id,
            prefix,
            markers,
            lock,
            body
        };
    }

    export function newThrow(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        exception: Expression
    ): J.Throw {
        return {
            kind: J.Kind.Throw,
            id,
            prefix,
            markers,
            exception
        };
    }

    export function newTry(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        resources: J.Container<J.TryResource> | undefined,
        body: J.Block,
        catches: J.TryCatch[],
        finally_?: J.LeftPadded<J.Block>
    ): J.Try {
        return {
            kind: J.Kind.Try,
            id,
            prefix,
            markers,
            resources,
            body,
            catches,
            finally: finally_
        };
    }

    export function newAnnotatedType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotations: J.Annotation[],
        typeExpression: TypeTree
    ): J.AnnotatedType {
        return {
            kind: J.Kind.AnnotatedType,
            id,
            prefix,
            markers,
            annotations,
            typeExpression
        };
    }

    export function newAnnotation(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotationType: NameTree,
        arguments_: J.Container<Expression> | undefined
    ): J.Annotation {
        return {
            kind: J.Kind.Annotation,
            id,
            prefix,
            markers,
            annotationType,
            arguments: arguments_
        };
    }

    export function newArrayAccess(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        indexed: Expression,
        dimension: J.ArrayDimension
    ): J.ArrayAccess {
        return {
            kind: J.Kind.ArrayAccess,
            id,
            prefix,
            markers,
            indexed,
            dimension
        };
    }

    export function newArrayDimension(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        index: J.RightPadded<Expression>
    ): J.ArrayDimension {
        return {
            kind: J.Kind.ArrayDimension,
            id,
            prefix,
            markers,
            index
        };
    }

    export function newArrayType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        elementType: TypeTree,
        annotations: J.Annotation[] | undefined,
        dimension: LeftPadded<Space>,
        type?: JavaType
    ): J.ArrayType {
        return {
            kind: J.Kind.ArrayType,
            id,
            prefix,
            markers,
            elementType,
            annotations,
            dimension,
            type
        };
    }

    export function newAssert(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        condition: Expression,
        detail?: J.LeftPadded<Expression>
    ): J.Assert {
        return {
            kind: J.Kind.Assert,
            id,
            prefix,
            markers,
            condition,
            detail
        };
    }

    export function newAssignment(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        variable: Expression,
        assignment: J.LeftPadded<Expression>,
        type?: JavaType
    ): J.Assignment {
        return {
            kind: J.Kind.Assignment,
            id,
            prefix,
            markers,
            variable,
            assignment,
            type
        };
    }

    export function newBreak(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        label?: J.Identifier
    ): J.Break {
        return {
            kind: J.Kind.Break,
            id,
            prefix,
            markers,
            label
        };
    }

    export function newCase(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        type: J.Case.Type,
        caseLabels: J.Container<J>,
        statements: J.Container<Statement>,
        body?: J.RightPadded<J>,
        guard?: Expression
    ): J.Case {
        return {
            kind: J.Kind.Case,
            id,
            prefix,
            markers,
            type,
            caseLabels,
            statements,
            body,
            guard
        };
    }

    export function newContinue(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        label?: J.Identifier
    ): J.Continue {
        return {
            kind: J.Kind.Continue,
            id,
            prefix,
            markers,
            label
        };
    }

    export function newDoWhileLoop(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        body: J.RightPadded<Statement>,
        whileCondition: J.LeftPadded<J.ControlParentheses<Expression>>
    ): J.DoWhileLoop {
        return {
            kind: J.Kind.DoWhileLoop,
            id,
            prefix,
            markers,
            body,
            whileCondition
        };
    }

    export function newEmpty(
        id: UUID,
        prefix: J.Space,
        markers: Markers
    ): J.Empty {
        return {
            kind: J.Kind.Empty,
            id,
            prefix,
            markers
        };
    }

    export function newEnumValue(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotations: J.Annotation[],
        name: J.Identifier,
        initializer?: J.NewClass
    ): J.EnumValue {
        return {
            kind: J.Kind.EnumValue,
            id,
            prefix,
            markers,
            annotations,
            name,
            initializer
        };
    }

    export function newEnumValueSet(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        enums: J.RightPadded<J.EnumValue>[],
        terminatedWithSemicolon: boolean
    ): J.EnumValueSet {
        return {
            kind: J.Kind.EnumValueSet,
            id,
            prefix,
            markers,
            enums,
            terminatedWithSemicolon
        };
    }

    export function newDeconstructionPattern(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        deconstructor: Expression,
        nested: J.Container<J>,
        type?: JavaType
    ): J.DeconstructionPattern {
        return {
            kind: J.Kind.DeconstructionPattern,
            id,
            prefix,
            markers,
            deconstructor,
            nested,
            type
        };
    }

    export function newIntersectionType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        bounds: J.Container<TypeTree>,
        type?: JavaType
    ): J.IntersectionType {
        return {
            kind: J.Kind.IntersectionType,
            id,
            prefix,
            markers,
            bounds,
            type
        };
    }

    export function newLabel(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        label: J.RightPadded<J.Identifier>,
        statement: Statement
    ): J.Label {
        return {
            kind: J.Kind.Label,
            id,
            prefix,
            markers,
            label,
            statement
        };
    }

    export function newLambda(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        parameters: J.Lambda.Parameters,
        arrow: J.Space,
        body: Statement | Expression,
        type?: JavaType
    ): J.Lambda {
        return {
            kind: J.Kind.Lambda,
            id,
            prefix,
            markers,
            parameters,
            arrow,
            body,
            type
        };
    }

    export function newLambdaParameters(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        parenthesized: boolean,
        parameters: J.RightPadded<J>[]
    ): J.Lambda.Parameters {
        return {
            kind: J.Kind.LambdaParameters,
            id,
            prefix,
            markers,
            parenthesized,
            parameters
        };
    }

    export function newLiteral(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        value?: Object,
        valueSource?: string,
        unicodeEscapes?: LiteralUnicodeEscape[],
        type?: JavaType.Primitive
    ): J.Literal {
        return {
            kind: J.Kind.Literal,
            id,
            prefix,
            markers,
            value,
            valueSource,
            unicodeEscapes,
            type
        };
    }

    export function newMemberReference(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        containing: J.RightPadded<Expression>,
        typeParameters: J.Container<Expression> | undefined,
        reference: J.LeftPadded<J.Identifier>,
        type?: JavaType,
        methodType?: JavaType.Method,
        variableType?: JavaType.Variable
    ): J.MemberReference {
        return {
            kind: J.Kind.MemberReference,
            id,
            prefix,
            markers,
            containing,
            typeParameters,
            reference,
            type,
            methodType,
            variableType
        };
    }

    export function newMethodDeclaration(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        leadingAnnotations: J.Annotation[],
        modifiers: J.Modifier[],
        typeParameters: J.TypeParameters | undefined,
        returnTypeExpression: TypeTree | undefined,
        nameAnnotations: J.Annotation[],
        name: J.Identifier,
        parameters: J.Container<Statement>,
        throws: J.Container<NameTree>,
        body?: J.Block,
        defaultValue?: J.LeftPadded<Expression>,
        methodType?: JavaType.Method
    ): J.MethodDeclaration {
        return {
            kind: J.Kind.MethodDeclaration,
            id,
            prefix,
            markers,
            leadingAnnotations,
            modifiers,
            typeParameters,
            returnTypeExpression,
            nameAnnotations,
            name,
            parameters,
            throws,
            body,
            defaultValue,
            methodType
        };
    }

    export function newMethodInvocation(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        select: J.RightPadded<Expression> | undefined,
        typeParameters: J.Container<Expression> | undefined,
        name: J.Identifier,
        arguments_: J.Container<Expression>,
        methodType?: JavaType.Method
    ): J.MethodInvocation {
        return {
            kind: J.Kind.MethodInvocation,
            id,
            prefix,
            markers,
            select,
            typeParameters,
            name,
            arguments: arguments_,
            methodType
        };
    }

    export function newModifier(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        keyword: string | undefined,
        type: J.ModifierType,
        annotations: J.Annotation[]
    ): J.Modifier {
        return {
            kind: J.Kind.Modifier,
            id,
            prefix,
            markers,
            keyword,
            type,
            annotations
        };
    }

    export function newMultiCatch(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        alternatives: J.RightPadded<NameTree>[]
    ): J.MultiCatch {
        return {
            kind: J.Kind.MultiCatch,
            id,
            prefix,
            markers,
            alternatives
        };
    }

    export function newNewArray(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeExpression: TypeTree | undefined,
        dimensions: J.ArrayDimension[],
        initializer?: J.Container<Expression>,
        type?: JavaType
    ): J.NewArray {
        return {
            kind: J.Kind.NewArray,
            id,
            prefix,
            markers,
            typeExpression,
            dimensions,
            initializer,
            type
        };
    }

    export function newNewClass(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        enclosing: J.RightPadded<Expression> | undefined,
        new_: J.Space,
        clazz: TypeTree | undefined,
        arguments_: J.Container<Expression>,
        body?: J.Block,
        constructorType?: JavaType.Method
    ): J.NewClass {
        return {
            kind: J.Kind.NewClass,
            id,
            prefix,
            markers,
            enclosing,
            new: new_,
            clazz,
            arguments: arguments_,
            body,
            constructorType
        };
    }

    export function newNullableType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        annotations: J.Annotation[],
        typeTree: J.RightPadded<TypeTree>
    ): J.NullableType {
        return {
            kind: J.Kind.NullableType,
            id,
            prefix,
            markers,
            annotations,
            typeTree
        };
    }

    export function newPackage(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression,
        annotations?: J.Annotation[]
    ): J.Package {
        return {
            kind: J.Kind.Package,
            id,
            prefix,
            markers,
            expression,
            annotations
        };
    }
}

export interface JavaType {
    readonly kind: string;
}

export namespace JavaType {
    export const Kind = {
        Annotation: "org.openrewrite.java.tree.JavaType$Annotation",
        AnnotationElementValue: "org.openrewrite.java.tree.JavaType$Annotation$ElementValue",
        Class: "org.openrewrite.java.tree.JavaType$Class",
        Parameterized: "org.openrewrite.java.tree.JavaType$Parameterized",
        Primitive: "org.openrewrite.java.tree.JavaType$Primitive",
        Array: "org.openrewrite.java.tree.JavaType$Array",
        Method: "org.openrewrite.java.tree.JavaType$Method",
        Intersection: "org.openrewrite.java.tree.JavaType$Intersection",
        GenericTypeVariable: "org.openrewrite.java.tree.JavaType$GenericTypeVariable",
        ShallowClass: "org.openrewrite.java.tree.JavaType$ShallowClass",
        Union: "org.openrewrite.java.tree.JavaType$MultiCatch",
        Unknown: "org.openrewrite.java.tree.JavaType$Unknown",
        Variable: "org.openrewrite.java.tree.JavaType$Variable",
    }

    export interface Class extends JavaType {
        readonly kind: typeof Kind.Class,
        readonly classKind: J.ClassType;
        readonly fullyQualifiedName: string;
        readonly typeParameters: JavaType[];
        readonly supertype?: JavaType.Class;
        readonly owningClass?: JavaType.Class;
        readonly annotations: JavaType.Annotation[];
        readonly interfaces: JavaType.Class[];
        readonly members: JavaType.Variable[];
        readonly methods: JavaType.Method[];
    }

    export interface Annotation extends JavaType {
        readonly kind: typeof Kind.Annotation,
        readonly type: JavaType.Class;
        readonly values: Annotation.ElementValue[];
    }

    export namespace Annotation {
        export interface ElementValue {
            readonly kind: typeof Kind.AnnotationElementValue;
            readonly element: JavaType;
            readonly value: any;
        }
    }

    export interface Method extends JavaType {
        readonly kind: typeof Kind.Method;
        readonly declaringType: JavaType.Class;
        readonly name: string;
        readonly returnType: JavaType;
        readonly parameterNames: string[];
        readonly parameterTypes: JavaType[];
        readonly thrownExceptions: JavaType[];
        readonly annotations: JavaType.Annotation[];
        readonly defaultValue?: string[];
        readonly declaredFormalTypeNames: string[];
    }

    export interface Variable extends JavaType {
        readonly kind: typeof Kind.Variable;
        readonly name: string;
        readonly owner?: JavaType;
        readonly type: JavaType;
        readonly annotations: JavaType.Annotation[];
    }

    export interface Parameterized extends JavaType {
        readonly kind: typeof Kind.Parameterized;
        readonly type: JavaType.Class;
        readonly typeParameters: JavaType[];
    }

    export interface GenericTypeVariable extends JavaType {
        readonly kind: typeof Kind.GenericTypeVariable;
        readonly name: string;
        readonly variance: GenericTypeVariable.Variance;
        readonly bounds: JavaType[];
    }

    export namespace GenericTypeVariable {
        export const enum Variance {
            Covariant,
            Contravariant,
            Invariant
        }
    }

    export interface Array extends JavaType {
        readonly kind: typeof Kind.Array;
        readonly elemType: JavaType;
        readonly annotations: JavaType.Annotation[];
    }


    export class Primitive implements JavaType {
        private constructor(
            public readonly keyword: string,
            public readonly kind = JavaType.Kind.Primitive
        ) {}

        static readonly Boolean = new Primitive('boolean');
        static readonly Byte    = new Primitive('byte');
        static readonly Char    = new Primitive('char');
        static readonly Double  = new Primitive('double');
        static readonly Float   = new Primitive('float');
        static readonly Int     = new Primitive('int');
        static readonly Long    = new Primitive('long');
        static readonly Short   = new Primitive('short');
        static readonly String   = new Primitive('String');
        static readonly Void    = new Primitive('void');
        static readonly Null    = new Primitive('null');
        static readonly None    = new Primitive('');

        private static _all = [
            Primitive.Boolean,
            Primitive.Byte,
            Primitive.Char,
            Primitive.Double,
            Primitive.Float,
            Primitive.Int,
            Primitive.Long,
            Primitive.Short,
            Primitive.String,
            Primitive.Void,
            Primitive.Null,
            Primitive.None
        ];

        static values(): Primitive[] {
            return Primitive._all.slice();
        }
    }

    export interface Primitive extends JavaType {
        readonly kind: typeof Kind.Primitive;
        readonly keyword: string;
    }

    export interface Union extends JavaType {
        readonly kind: typeof Kind.Union;
        readonly bounds: JavaType[];
    }

    export interface Intersection extends JavaType {
        readonly kind: typeof Kind.Intersection;
        readonly bounds: JavaType[];
    }

    export interface ShallowClass extends JavaType.Class {
        readonly kind: typeof Kind.ShallowClass;
    }

    export const unknownType: JavaType = {
        kind: JavaType.Kind.Unknown
    };

    export function isPrimitive(type?: JavaType): type is JavaType.Primitive {
        return type?.kind === JavaType.Kind.Primitive;
    }

    export function isClass(type?: JavaType): type is JavaType.Class {
        return type?.kind === JavaType.Kind.Class;
    }

    export function isArray(type?: JavaType): type is JavaType.Array {
        return type?.kind === JavaType.Kind.Array;
    }
}

export function isSpace(tree: any): tree is J.Space {
    return tree &&
        typeof tree === 'object' &&
        tree.kind === J.Kind.Space;
}

export function space(whitespace: string): J.Space {
    return {
        kind: J.Kind.Space,
        comments: [],
        whitespace: whitespace,
    };
}

export const emptySpace: J.Space = {
    kind: J.Kind.Space,
    comments: [],
    whitespace: "",
};

export function emptyContainer<T extends J>(): J.Container<T> {
    return {
        kind: J.Kind.JContainer,
        before: emptySpace,
        elements: [],
        markers: emptyMarkers,
    };
}

export interface Comment {
    readonly kind: string
    readonly suffix: string;
    readonly markers: Markers;
}

export interface TextComment extends Comment {
    readonly kind: typeof J.Kind.TextComment
    readonly multiline: boolean;
    readonly text: string;
    readonly suffix: string;
    readonly markers: Markers;
}

export interface DocComment extends Comment {
    // TODO implement me!
}

const javaKindValues = new Set(Object.values(J.Kind));

export function isJava(tree: any): tree is J {
    return javaKindValues.has(tree["kind"]);
}
