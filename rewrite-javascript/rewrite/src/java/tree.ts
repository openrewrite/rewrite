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

import {emptyMarkers, Markers} from "../markers";
import {SourceFile, Tree} from "../tree";
import {Type} from "./type";

export interface J extends Tree {
    readonly prefix: J.Space;
}

export namespace J {
    export function isMethodCall(e?: Expression): e is MethodCall {
        return e?.kind === J.Kind.NewClass ||
            e?.kind === J.Kind.MethodInvocation ||
            e?.kind === J.Kind.MemberReference;
    }

    export function hasType<T extends J>(tree: T): tree is T & { type: Type } {
        return tree && 'type' in tree && tree.type != null;
    }
}

export interface Expression extends J {
}

export interface MethodCall extends Expression {
    readonly methodType?: Type.Method;
}

export interface TypedTree extends J {
}

export interface NameTree extends TypedTree {
}

export interface TypeTree extends NameTree {
}

export interface VariableDeclarator extends J {
}

export interface JavaSourceFile extends J, SourceFile {
    readonly packageDeclaration: J.RightPadded<J.Package>;
    readonly imports: J.RightPadded<J.Import>[];
    readonly classes: J.ClassDeclaration[];
}

export interface Statement extends J {
}

export interface NameTree extends J {
}

export namespace J {
    export const Kind = {
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
        Container: "org.openrewrite.java.tree.JContainer",
        LeftPadded: "org.openrewrite.java.tree.JLeftPadded",
        RightPadded: "org.openrewrite.java.tree.JRightPadded",
        Space: "org.openrewrite.java.tree.Space",
    } as const;

    export interface AnnotatedType extends J, TypeTree, Expression {
        readonly kind: typeof Kind.AnnotatedType;
        readonly annotations: Annotation[];
        readonly typeExpression: TypeTree;
    }

    export interface Annotation extends J, Expression {
        readonly kind: typeof Kind.Annotation;
        readonly annotationType: NameTree;
        readonly arguments?: Container<Expression>;
    }

    export interface ArrayAccess extends J, TypedTree, Expression {
        readonly kind: typeof Kind.ArrayAccess;
        readonly indexed: Expression;
        readonly dimension: ArrayDimension;
        readonly type?: Type
    }

    export interface ArrayDimension extends J {
        readonly kind: typeof Kind.ArrayDimension;
        readonly index: RightPadded<Expression>;
    }

    export interface ArrayType extends J, TypeTree, Expression {
        readonly kind: typeof Kind.ArrayType;
        readonly elementType: TypeTree;
        readonly annotations?: Annotation[];
        readonly dimension: LeftPadded<Space>;
        readonly type?: Type;
    }

    export interface Assert extends J, Statement {
        readonly kind: typeof Kind.Assert;
        readonly condition: Expression;
        readonly detail?: LeftPadded<Expression>;
    }

    export interface Assignment extends J, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.Assignment;
        readonly variable: Expression;
        readonly assignment: LeftPadded<Expression>;
        readonly type?: Type;
    }

    export interface AssignmentOperation extends J, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.AssignmentOperation;
        readonly variable: Expression;
        readonly operator: LeftPadded<AssignmentOperation.Type>;
        readonly assignment: Expression;
        readonly type?: Type;
    }

    export namespace AssignmentOperation {
        export const enum Type {
            Addition = "Addition",
            BitAnd = "BitAnd",
            BitOr = "BitOr",
            BitXor = "BitXor",
            Division = "Division",
            Exponentiation = "Exponentiation",    // Raises the left operand to the power of the right operand. Used in Python
            FloorDivision = "FloorDivision",     // Division rounding down to the nearest integer. Used in Python
            LeftShift = "LeftShift",
            MatrixMultiplication = "MatrixMultiplication", // Matrix multiplication. Used in Python
            Modulo = "Modulo",
            Multiplication = "Multiplication",
            RightShift = "RightShift",
            Subtraction = "Subtraction",
            UnsignedRightShift = "UnsignedRightShift"
        }
    }

    export interface Binary extends J, Expression, TypedTree {
        readonly kind: typeof Kind.Binary;
        readonly left: Expression;
        readonly operator: LeftPadded<Binary.Type>;
        readonly right: Expression;
        readonly type?: Type;
    }

    export namespace Binary {
        export const enum Type {
            Addition = "Addition",
            Subtraction = "Subtraction",
            Multiplication = "Multiplication",
            Division = "Division",
            Modulo = "Modulo",
            LessThan = "LessThan",
            GreaterThan = "GreaterThan",
            LessThanOrEqual = "LessThanOrEqual",
            GreaterThanOrEqual = "GreaterThanOrEqual",
            Equal = "Equal",
            NotEqual = "NotEqual",
            BitAnd = "BitAnd",
            BitOr = "BitOr",
            BitXor = "BitXor",
            LeftShift = "LeftShift",
            RightShift = "RightShift",
            UnsignedRightShift = "UnsignedRightShift",
            Or = "Or",
            And = "And"
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

    export interface Case extends J, Statement {
        readonly kind: typeof Kind.Case;
        readonly type: Case.Type;
        readonly caseLabels: Container<J>;
        readonly statements: Container<Statement>;
        readonly body?: RightPadded<J>;
        readonly guard?: Expression;
    }

    export namespace Case {
        export const enum Type {
            Statement = "Statement",
            Rule = "Rule"
        }
    }

    export interface ClassDeclaration extends J, Statement, TypedTree {
        readonly kind: typeof Kind.ClassDeclaration;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly classKind: ClassDeclaration.Kind;
        readonly name: Identifier;
        readonly typeParameters?: Container<TypeParameter>;
        readonly primaryConstructor?: Container<Statement>;
        readonly extends?: LeftPadded<TypeTree>;
        readonly implements?: Container<TypeTree>;
        readonly permitting?: Container<TypeTree>;
        readonly body: Block;
        readonly type?: Type.FullyQualified;
    }

    export namespace ClassDeclaration {
        export interface Kind extends J {
            readonly kind: typeof J.Kind.ClassDeclarationKind;
            readonly annotations: Annotation[];
            readonly type: Kind.Type;
        }

        export namespace Kind {
            export const enum Type {
                Class = "Class",
                Interface = "Interface",
                Enum = "Enum",
                Annotation = "Annotation",
                Record = "Record",
                Value = "Value"
            }
        }
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

    export interface Empty extends J, Statement, TypeTree, Expression {
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

    export interface FieldAccess extends Statement, Expression, TypeTree {
        readonly kind: typeof Kind.FieldAccess;
        readonly target: Expression;
        readonly name: LeftPadded<Identifier>;
        readonly type?: Type;
    }

    export interface ForEachLoop extends J, Statement {
        readonly kind: typeof Kind.ForEachLoop;
        readonly control: ForEachLoop.Control;
        readonly body: RightPadded<Statement>;
    }

    export namespace ForEachLoop {
        export interface Control extends J {
            readonly kind: typeof Kind.ForEachLoopControl;
            readonly variable: RightPadded<Statement>;
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

    export interface ParenthesizedTypeTree extends J, TypeTree, Expression {
        readonly kind: typeof Kind.ParenthesizedTypeTree;
        readonly annotations: Annotation[];
        readonly parenthesizedType: Parentheses<TypeTree>;
    }

    export interface Identifier extends J, TypeTree, Expression, VariableDeclarator {
        readonly kind: typeof Kind.Identifier;
        readonly annotations: Annotation[];
        readonly simpleName: string;
        readonly type?: Type;
        readonly fieldType?: Type.Variable;
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

    export interface InstanceOf extends J, Expression, TypedTree {
        readonly kind: typeof Kind.InstanceOf;
        readonly expression: RightPadded<Expression>;
        readonly class: J;
        readonly pattern?: J;
        readonly type?: Type;
        readonly modifier?: Modifier;
    }

    export interface DeconstructionPattern extends J, TypedTree {
        readonly kind: typeof Kind.DeconstructionPattern;
        readonly deconstructor: Expression;
        readonly nested: Container<J>;
        readonly type?: Type;
    }

    export interface IntersectionType extends J, TypeTree, Expression {
        readonly kind: typeof Kind.IntersectionType;
        readonly bounds: Container<TypeTree>;
        readonly type?: Type;
    }

    export interface Label extends J, Statement {
        readonly kind: typeof Kind.Label;
        readonly label: RightPadded<Identifier>;
        readonly statement: Statement;
    }

    export interface Lambda extends J, Statement, TypedTree, Expression {
        readonly kind: typeof Kind.Lambda;
        readonly parameters: Lambda.Parameters;
        readonly arrow: Space;
        readonly body: Statement | Expression;
        readonly type?: Type;
    }

    export namespace Lambda {
        export interface Parameters extends J {
            readonly kind: typeof Kind.LambdaParameters;
            readonly parenthesized: boolean;
            readonly parameters: RightPadded<J>[];
        }
    }

    export interface Literal extends J, TypedTree, Expression, VariableDeclarator {
        readonly kind: typeof Kind.Literal;
        readonly value?: string | number | bigint | boolean | null | undefined;
        readonly valueSource?: string;
        readonly unicodeEscapes?: LiteralUnicodeEscape[];
        readonly type?: Type.Primitive;
    }

    export interface LiteralUnicodeEscape {
        readonly valueSourceIndex: number;
        readonly codePoint: string;
    }

    export interface MemberReference extends J, MethodCall {
        readonly kind: typeof Kind.MemberReference;
        readonly containing: RightPadded<Expression>;
        readonly typeParameters?: Container<Expression>;
        readonly reference: LeftPadded<Identifier>;
        readonly type?: Type;
        readonly variableType?: Type.Variable;
    }

    export interface MethodDeclaration extends J, TypedTree {
        readonly kind: typeof Kind.MethodDeclaration;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly typeParameters?: TypeParameters;
        readonly returnTypeExpression?: TypeTree;
        readonly nameAnnotations: Annotation[];
        readonly name: Identifier;
        readonly parameters: Container<Statement>;
        readonly throws?: Container<NameTree>;
        readonly body?: Block;
        readonly defaultValue?: LeftPadded<Expression>;
        readonly methodType?: Type.Method;
    }

    export interface MethodInvocation extends J, TypedTree, MethodCall {
        readonly kind: typeof Kind.MethodInvocation;
        readonly select?: RightPadded<Expression>;
        readonly typeParameters?: Container<Expression>;
        readonly name: Identifier;
        readonly arguments: Container<Expression>;
    }

    export interface Modifier extends J {
        readonly kind: typeof Kind.Modifier;
        readonly keyword?: string;
        readonly type: ModifierType;
        readonly annotations: Annotation[];
    }

    export const enum ModifierType {
        Default = "Default",
        Public = "Public",
        Protected = "Protected",
        Private = "Private",
        Abstract = "Abstract",
        Static = "Static",
        Final = "Final",
        Sealed = "Sealed",
        NonSealed = "NonSealed",
        Transient = "Transient",
        Volatile = "Volatile",
        Synchronized = "Synchronized",
        Native = "Native",
        Strictfp = "Strictfp",
        Async = "Async",
        Reified = "Reified",
        Inline = "Inline",
        /**
         * For modifiers not seen in Java this is used in conjunction with "keyword"
         */
        LanguageExtension = "LanguageExtension"
    }

    export interface MultiCatch extends J, TypeTree {
        readonly kind: typeof Kind.MultiCatch;
        readonly alternatives: RightPadded<NameTree>[];
    }

    export interface NewArray extends J, TypedTree, Expression {
        readonly kind: typeof Kind.NewArray;
        readonly typeExpression?: TypeTree;
        readonly dimensions: ArrayDimension[];
        readonly initializer?: Container<Expression>;
        readonly type?: Type;
    }

    export interface NewClass extends J, TypedTree, MethodCall {
        readonly kind: typeof Kind.NewClass;
        readonly enclosing?: RightPadded<Expression>;
        readonly new: Space;
        readonly class?: TypeTree;
        readonly arguments: Container<Expression>;
        readonly body?: Block;
        readonly constructorType?: Type.Method;
    }

    export interface NullableType extends J, TypeTree, Expression {
        readonly kind: typeof Kind.NullableType;
        readonly annotations: Annotation[];
        readonly typeTree: RightPadded<TypeTree>;
    }

    export interface Package extends J {
        readonly kind: typeof Kind.Package;
        readonly expression: Expression;
        readonly annotations?: Annotation[];
    }

    export interface ParameterizedType extends J, TypeTree {
        readonly kind: typeof Kind.ParameterizedType;
        readonly class: NameTree;
        readonly typeParameters?: Container<Expression>;
        readonly type?: Type;
    }

    export interface Parentheses<J2 extends J> extends J, Expression {
        readonly kind: typeof Kind.Parentheses;
        readonly tree: RightPadded<J2>;
    }

    export interface ControlParentheses<J2 extends J> extends J, Expression {
        readonly kind: typeof Kind.ControlParentheses;
        readonly tree: RightPadded<J2>;
    }

    export interface Primitive extends J, TypeTree, Expression {
        readonly kind: typeof Kind.Primitive;
        type: Type.Primitive;
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

    export interface SwitchExpression extends J, TypedTree, Expression {
        readonly kind: typeof Kind.SwitchExpression;
        readonly selector: ControlParentheses<Expression>;
        readonly cases: Block;
        readonly type?: Type;
    }

    export interface Synchronized extends J, Statement {
        readonly kind: typeof Kind.Synchronized;
        readonly lock: ControlParentheses<Expression>;
        readonly body: Block;
    }

    export interface Ternary extends J, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.Ternary;
        readonly condition: Expression;
        readonly truePart: LeftPadded<Expression>;
        readonly falsePart: LeftPadded<Expression>;
        readonly type?: Type;
    }

    export interface Throw extends J, Statement {
        readonly kind: typeof Kind.Throw;
        readonly exception: Expression;
    }

    export interface Try extends J, Statement {
        readonly kind: typeof Kind.Try;
        readonly resources?: Container<Try.Resource>;
        readonly body: Block;
        readonly catches: Try.Catch[];
        readonly finally?: LeftPadded<Block>;
    }

    export namespace Try {
        export interface Resource extends J {
            readonly kind: typeof Kind.TryResource;
            readonly variableDeclarations: TypedTree;
            readonly terminatedWithSemicolon: boolean;
        }

        export interface Catch extends J {
            readonly kind: typeof Kind.TryCatch;
            readonly parameter: ControlParentheses<VariableDeclarations>;
            readonly body: Block;
        }
    }

    export interface TypeCast extends J, TypedTree, Expression {
        readonly kind: typeof Kind.TypeCast;
        readonly class: ControlParentheses<TypeTree>;
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

    export interface Unary extends J, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.Unary;
        readonly operator: LeftPadded<Unary.Type>;
        readonly expression: Expression;
        readonly type?: Type;
    }


    export namespace Unary {
        export const enum Type {
            PreIncrement = "PreIncrement",
            PreDecrement = "PreDecrement",
            PostIncrement = "PostIncrement",
            PostDecrement = "PostDecrement",
            Positive = "Positive",
            Negative = "Negative",
            Complement = "Complement",
            Not = "Not"
        }
    }

    export interface VariableDeclarations extends J, TypedTree {
        readonly kind: typeof Kind.VariableDeclarations;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly typeExpression?: TypeTree;
        readonly varargs?: Space;
        readonly variables: RightPadded<J.VariableDeclarations.NamedVariable>[];
    }

    export namespace VariableDeclarations {
        export interface NamedVariable extends J {
            readonly kind: typeof Kind.NamedVariable;
            readonly name: VariableDeclarator;
            readonly dimensionsAfterName: LeftPadded<Space>[];
            readonly initializer?: LeftPadded<Expression>;
            readonly variableType?: Type.Variable;
        }
    }

    export interface WhileLoop extends J, Statement {
        readonly kind: typeof Kind.WhileLoop;
        readonly condition: ControlParentheses<Expression>;
        readonly body: RightPadded<Statement>;
    }

    export interface Wildcard extends J, TypeTree, Expression {
        readonly kind: typeof Kind.Wildcard;
        readonly bound?: LeftPadded<Wildcard.Bound>;
        readonly boundedType?: NameTree;
    }

    export namespace Wildcard {
        export const enum Bound {
            Extends = "Extends",
            Super = "Super"
        }
    }

    export interface Yield extends J, Statement {
        readonly kind: typeof Kind.Yield;
        readonly implicit: boolean;
        readonly value: Expression;
    }

    export interface Unknown extends J, Statement, Expression, TypeTree {
        readonly kind: typeof Kind.Unknown;
        readonly source: UnknownSource;
    }

    export interface UnknownSource extends J {
        readonly kind: typeof Kind.UnknownSource;
        readonly text: string;
    }

    export interface Erroneous extends J, Expression, Statement {
        readonly kind: typeof Kind.Erroneous;
        readonly text: string;
    }

    export interface LeftPadded<T extends J | Space | number | string | boolean> {
        readonly kind: typeof Kind.LeftPadded;
        readonly before: Space;
        readonly element: T;
        readonly markers: Markers;
    }

    export interface RightPadded<T extends J | boolean> {
        readonly kind: typeof Kind.RightPadded;
        readonly element: T;
        readonly after: Space;
        readonly markers: Markers;
    }

    export interface Container<T extends J> {
        readonly kind: typeof Kind.Container;
        readonly before: Space;
        readonly elements: RightPadded<T>[];
        readonly markers: Markers;
    }

    export interface Space {
        readonly kind: typeof J.Kind.Space;
        readonly comments: Comment[];
        readonly whitespace: string;
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
        kind: J.Kind.Container,
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

export function isLiteral(tree: any): tree is J.Literal {
    return tree["kind"] === J.Kind.Literal;
}

export function rightPadded<T extends J | boolean>(t: T, trailing: J.Space, markers?: Markers): J.RightPadded<T> {
    return {
        kind: J.Kind.RightPadded,
        element: t,
        after: trailing,
        markers: markers ?? emptyMarkers
    };
}

export namespace TypedTree {
    const typeGetters = new Map<TypedTree["kind"], (tree: TypedTree) => Type | undefined>();

    export function getType(typeTree?: TypedTree): Type | undefined {
        if (!typeTree) {
            return undefined;
        }
        const customFn = typeGetters.get(typeTree.kind as any);
        if (customFn) {
            return customFn(typeTree as any) as any;
        }
        if ("typeExpression" in typeTree && (typeTree as any).typeExpression) {
            return getType((typeTree as any).typeExpression);
        }
        return (typeTree as any).type;
    }

    export function registerTypeGetter<T extends TypedTree>(kind: T["kind"], fn: (tree: T) => Type | undefined): void {
        typeGetters.set(kind, fn as any);
    }

    registerTypeGetter(J.Kind.MethodDeclaration, (tree: J.MethodDeclaration) => tree.methodType?.returnType);
    registerTypeGetter(J.Kind.MethodInvocation, (tree: J.MethodInvocation) => tree.methodType?.returnType);
    registerTypeGetter(J.Kind.Parentheses, (tree: J.Parentheses<TypedTree>) => getType(tree.tree.element));
    registerTypeGetter(J.Kind.NewClass, (tree: J.NewClass) => tree.constructorType?.returnType);

    // TODO ControlParentheses here isn't a TypedTree so why does this compile?
    registerTypeGetter(J.Kind.TypeCast, (tree: J.TypeCast) => getType(tree.class));

    registerTypeGetter(J.Kind.Empty, () => Type.unknownType);
    registerTypeGetter(J.Kind.MultiCatch, (tree: J.MultiCatch) => {
        const bounds = tree.alternatives.map(a => getType(a.element));
        return {kind: Type.Kind.Union, bounds: bounds};
    });
    registerTypeGetter(J.Kind.NullableType, (tree: J.NullableType) => getType(tree.typeTree.element));
    registerTypeGetter(J.Kind.Wildcard, () => Type.unknownType);
    registerTypeGetter(J.Kind.Unknown, () => Type.unknownType);
}
