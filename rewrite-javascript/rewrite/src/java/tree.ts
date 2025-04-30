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

import {Markers, SourceFile, Tree, TreeKind} from "../";


type JavaExpressionBase = J.FieldAccess | J.Assignment | J.AssignmentOperation | J.InstanceOf | J.Binary | J.Unary | 
    J.Ternary | J.Parentheses<J> | J.ArrayAccess | J.MethodInvocation | J.NewArray | J.NewClass | J.Lambda | 
    J.TypeCast | J.ParenthesizedTypeTree | J.MemberReference | J.AnnotatedType | J.ArrayType | J.Literal | 
    J.Annotation | J.SwitchExpression | J.Primitive | J.Wildcard | J.IntersectionType | J.NullableType | 
    J.ControlParentheses<J> | J.Empty | J.Unknown | J.Erroneous;

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
        Variable: "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable",
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
        readonly operator: LeftPadded<AssignmentOperationType>;
        readonly assignment: Expression;
        readonly type?: JavaType;
    }

    export const enum AssignmentOperationType {
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

    export interface Binary extends J {
        readonly kind: typeof Kind.Binary;
        readonly left: Expression;
        readonly operator: LeftPadded<BinaryOperator>;
        readonly right: Expression;
        readonly type?: JavaType;
    }

    export const enum BinaryOperator {
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
        readonly type: CaseType;
        readonly caseLabels: Container<J>;
        readonly statements: Container<Statement>;
        readonly body?: RightPadded<J>;
        readonly guard?: Expression;
    }

    export const enum CaseType {
        Statement,
        Rule
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
        readonly control: ForEachLoopControl;
        readonly body: RightPadded<Statement>;
    }

    export interface ForEachLoopControl extends J {
        readonly kind: typeof Kind.ForEachLoopControl;
        readonly variable: RightPadded<VariableDeclarations>;
        readonly iterable: RightPadded<Expression>;
    }

    export interface ForLoop extends J, Statement {
        readonly kind: typeof Kind.ForLoop;
        readonly control: ForLoopControl;
        readonly body: RightPadded<Statement>;
    }

    export interface ForLoopControl extends J {
        readonly kind: typeof Kind.ForLoopControl;
        readonly init: RightPadded<Statement>[];
        readonly condition?: RightPadded<Expression>;
        readonly update: RightPadded<Statement>[];
    }

    export interface ParenthesizedTypeTree extends J {
        readonly kind: typeof Kind.ParenthesizedTypeTree;
        readonly annotations: Annotation[];
        readonly parenthesizedType: Parentheses<TypeTree>;
        readonly type?: JavaType;
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
        readonly elsePart?: IfElse;
    }

    export interface IfElse extends J {
        readonly kind: typeof Kind.IfElse;
        readonly body: RightPadded<Statement>;
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
        readonly parameters: LambdaParameters;
        readonly arrow: Space;
        readonly body: Statement | Expression;
        readonly type?: JavaType;
    }

    export interface LambdaParameters extends J {
        readonly kind: typeof Kind.LambdaParameters;
        readonly parenthesized: boolean;
        readonly parameters: RightPadded<J>[];
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
        readonly name: Identifier;
        readonly bounds?: Container<TypeTree>;
    }

    export interface TypeParameters extends J {
        readonly kind: typeof Kind.TypeParameters;
        readonly annotations: Annotation[];
        readonly typeParameters: RightPadded<TypeParameter>[];
    }

    export interface Unary extends J, Statement {
        readonly kind: typeof Kind.Unary;
        readonly operator: LeftPadded<UnaryOperator>;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export const enum UnaryOperator {
        PreIncrement,
        PreDecrement,
        PostIncrement,
        PostDecrement,
        Positive,
        Negative,
        Complement,
        Not
    }

    export interface VariableDeclarations extends J {
        readonly kind: typeof Kind.VariableDeclarations;
        readonly leadingAnnotations: Annotation[];
        readonly modifiers: Modifier[];
        readonly typeExpression?: TypeTree;
        readonly varargs?: Space;
        readonly variables: RightPadded<Variable>[];
    }

    export interface Variable extends J {
        readonly kind: typeof Kind.Variable;
        readonly name: Identifier;
        readonly dimensionsAfterName: LeftPadded<Space>[];
        readonly initializer?: LeftPadded<Expression>;
        readonly variableType?: JavaType.Variable;
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
        readonly element: T;
        readonly before: Space;
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
}

export interface JavaType {
}

export namespace JavaType {
    export interface Class extends JavaType {
        readonly fullyQualifiedName: string;
        readonly kind: J.ClassType;
        readonly typeParameters: JavaType[];
        readonly supertype?: JavaType.Class;
        readonly owningClass?: JavaType.Class;
        readonly annotations: JavaType.Annotation[];
        readonly interfaces: JavaType.Class[];
        readonly members: JavaType.Variable[];
        readonly methods: JavaType.Method[];
    }

    export interface Annotation extends JavaType {
        readonly type: JavaType.Class;
        readonly values: ElementValue[];
    }

    export interface ElementValue {
        readonly element: JavaType;
        readonly value: any;
    }

    export interface Method extends JavaType {
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
        readonly name: string;
        readonly owner?: JavaType;
        readonly type: JavaType;
        readonly annotations: JavaType.Annotation[];
    }

    export interface Parameterized extends JavaType {
        readonly type: JavaType.Class;
        readonly typeParameters: JavaType[];
    }

    export interface GenericTypeVariable extends JavaType {
        readonly name: string;
        readonly variance: Variance;
        readonly bounds: JavaType[];
    }

    export const enum Variance {
        Covariant,
        Contravariant,
        Invariant
    }

    export interface Array extends JavaType {
        readonly elemType: JavaType;
        readonly annotations: JavaType.Annotation[];
    }

    export interface Primitive extends JavaType {
        readonly keyword: string;
    }

    export interface MultiCatch extends JavaType {
        readonly throwableTypes: JavaType[];
    }

    export interface Intersection extends JavaType {
        readonly bounds: JavaType[];
    }

    export interface ShallowClass extends JavaType.Class {
    }

    export interface Unknown extends JavaType {
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
