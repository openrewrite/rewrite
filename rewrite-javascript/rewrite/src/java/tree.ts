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

export const JavaKind = {
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

const javaKindValues = new Set(Object.values(JavaKind));

export function isJava(tree: any): tree is J {
    return javaKindValues.has(tree["kind"]);
}

export interface Space {
    readonly kind: typeof JavaKind.Space;
    readonly comments: Comment[];
    readonly whitespace: string;
}

export function space(whitespace: string): Space {
    return {
        kind: JavaKind.Space,
        comments: [],
        whitespace: whitespace,
    };
}

export const emptySpace: Space = {
    kind: JavaKind.Space,
    comments: [],
    whitespace: "",
};

export interface Comment {
    readonly multiline: boolean;
    readonly text: string;
    readonly suffix: string;
    readonly markers: Markers;
}

export interface TextComment extends Comment {
    readonly multiline: boolean;
    readonly text: string;
    readonly suffix: string;
    readonly markers: Markers;
}

export interface J extends Tree {
    readonly prefix: Space;
}

export interface JLeftPadded<T> {
    readonly kind: typeof JavaKind.JLeftPadded;
    readonly element: T;
    readonly before: Space;
    readonly markers: Markers;
}

export interface JRightPadded<T extends J> {
    readonly kind: typeof JavaKind.JRightPadded;
    readonly element: T;
    readonly after: Space;
    readonly markers: Markers;
}

export interface JContainer<T extends J> {
    readonly kind: typeof JavaKind.JContainer;
    readonly elements: JRightPadded<T>[];
}

export interface JavaSourceFile extends J, SourceFile {
    readonly typesInUse: TypesInUse;
    readonly packageDeclaration: JRightPadded<Package>;
    readonly imports: JRightPadded<Import>[];
    readonly classes: ClassDeclaration[];
}

export interface TypesInUse {
    readonly usedTypes: JavaType[];
    readonly usedMethods: JavaType.Method[];
    readonly usedVariables: JavaType.Variable[];
    readonly usedAnnotations: JavaType.Annotation[];
}

export interface TypedTree extends J {
    readonly type?: JavaType;
}

export interface NameTree extends TypedTree {
}

export interface TypeTree extends NameTree {
}

export interface Statement extends J {
}

type JavaExpressionBase = FieldAccess | Assignment | AssignmentOperation | InstanceOf | Binary | Unary | Ternary |
    Parentheses<J> | ArrayAccess | MethodInvocation | NewArray | NewClass | Lambda | TypeCast | ParenthesizedTypeTree |
    MemberReference | AnnotatedType | ArrayType | Literal | Annotation | SwitchExpression | Primitive | Wildcard |
    IntersectionType | NullableType | ControlParentheses<J> | Empty | Unknown | Erroneous;

type Expression<T extends { kind: string } = never> = JavaExpressionBase | T;

// noinspection JSUnusedLocalSymbols
type JavaExpression = Expression;

export interface AnnotatedType extends J, TypeTree {
    readonly kind: typeof JavaKind.AnnotatedType;
    readonly annotations: Annotation[];
    readonly typeExpression: TypeTree;
    readonly type?: JavaType;
}

export interface ArrayAccess extends J, TypedTree {
    readonly kind: typeof JavaKind.ArrayAccess;
    readonly indexed: Expression;
    readonly dimension: ArrayDimension;
    readonly type?: JavaType;
}

export interface ArrayDimension extends J {
    readonly kind: typeof JavaKind.ArrayDimension;
    readonly index: Expression;
}

export interface ArrayType extends J, TypeTree {
    readonly kind: typeof JavaKind.ArrayType;
    readonly elementType: TypeTree;
    readonly annotations?: Annotation[];
    readonly dimension?: JLeftPadded<Space>;
    readonly type: JavaType;
}

export interface CompilationUnit extends JavaSourceFile, J {
    readonly kind: typeof JavaKind.CompilationUnit;
    readonly eof: Space;
}

export interface ControlParentheses<T extends J> extends J {
    readonly kind: typeof JavaKind.ControlParentheses;
    readonly tree: JRightPadded<T>;
}

export interface Empty extends J, Statement, TypeTree {
    readonly kind: typeof JavaKind.Empty;
}

export interface Identifier extends J {
    readonly kind: typeof JavaKind.Identifier;
    readonly name: string;
}

export interface Literal extends J {
    readonly kind: typeof JavaKind.Literal;
    readonly value?: Object;
    readonly valueSource?: string;
    readonly unicodeEscapes?: LiteralUnicodeEscape[];
    readonly type?: JavaType.Primitive;
}

export interface LiteralUnicodeEscape {
    readonly valueSourceIndex: number;
    readonly codePoint: string;
}

export interface Package extends J {
    readonly kind: typeof JavaKind.Package;
    readonly expression: Expression;
}

export interface Import extends J {
    readonly kind: typeof JavaKind.Import;
    readonly statik: boolean;
    readonly qualid: FieldAccess;
}

export interface ClassDeclaration extends J {
    readonly kind: typeof JavaKind.ClassDeclaration;
    readonly leadingAnnotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly classKind: ClassDeclarationKind;
    readonly name: Identifier;
    readonly typeParameters: JContainer<TypeParameter>;
    readonly primaryConstructor?: JContainer<Statement>;
    readonly extends?: JLeftPadded<TypeTree>;
    readonly implements?: JContainer<TypeTree>;
    readonly permitting?: JContainer<TypeTree>;
    readonly body: Block;
    readonly type?: JavaType.Class;
}

export interface ClassDeclarationKind extends J {
    readonly kind: typeof JavaKind.ClassDeclarationKind;
    readonly annotations: Annotation[];
    readonly type: ClassType;
}

export interface Annotation extends J {
    readonly kind: typeof JavaKind.Annotation;
    readonly annotationType: NameTree;
    readonly arguments?: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface Modifier extends J {
    readonly kind: typeof JavaKind.Modifier;
    readonly type: ModifierType;
}

export const enum ModifierType {
    Public,
    Protected,
    Private,
    Abstract,
    Static,
    Final,
    Transient,
    Volatile,
    Synchronized,
    Native,
    Strictfp
}

export interface MultiCatch extends J, TypeTree {
    readonly kind: typeof JavaKind.MultiCatch;
    readonly alternatives: JRightPadded<TypeTree>[];
    readonly type?: JavaType;
}

export interface TypeParameter extends J {
    readonly kind: typeof JavaKind.TypeParameter;
    readonly annotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly name: Identifier;
    readonly bounds?: JContainer<TypeTree>;
}

export interface TypeParameters extends J {
    readonly kind: typeof JavaKind.TypeParameters;
    readonly annotations: Annotation[];
    readonly typeParameters: JRightPadded<TypeParameter>[];
}

export interface Primitive extends TypeTree {
    readonly kind: typeof JavaKind.Primitive;
    readonly keyword: string;
    readonly type: JavaType.Primitive;
}

export interface Block extends J {
    readonly kind: typeof JavaKind.Block;
    readonly statements: JRightPadded<Statement>[];
}

export interface Erroneous extends J {
    readonly kind: typeof JavaKind.Erroneous;
    readonly text: string;
}

export interface FieldAccess extends Statement, TypedTree {
    readonly kind: typeof JavaKind.FieldAccess;
    readonly target: Expression;
    readonly name: Identifier;
    readonly type?: JavaType;
}

export interface MethodDeclaration extends J {
    readonly kind: typeof JavaKind.MethodDeclaration;
    readonly annotations: JRightPadded<Annotation>[];
    readonly modifiers: JRightPadded<Modifier>[];
    readonly returnTypeExpression?: TypeTree;
    readonly name: Identifier;
    readonly parameters: JRightPadded<VariableDeclarations>[];
    readonly throws: JRightPadded<TypeTree>[];
    readonly body?: Block;
    readonly defaultValue?: Expression;
}

export interface VariableDeclarations extends J {
    readonly kind: typeof JavaKind.VariableDeclarations;
    readonly annotations: JRightPadded<Annotation>[];
    readonly modifiers: JRightPadded<Modifier>[];
    readonly typeExpression: TypeTree;
    readonly variables: JRightPadded<Variable>[];
}

export interface Variable extends J {
    readonly kind: typeof JavaKind.Variable;
    readonly name: Identifier;
    readonly dimensionsAfterName: JLeftPadded<Space>[];
    readonly initializer?: JLeftPadded<Expression>;
    readonly variableType?: JavaType.Variable;
}

export const enum ClassType {
    Class,
    Interface,
    Enum,
    Annotation,
    Record,
    Value
}

export interface Assert extends J, Statement {
    readonly kind: typeof JavaKind.Assert;
    readonly condition: Expression;
    readonly detail?: JLeftPadded<Expression>;
}

export interface Assignment extends J, Statement, TypedTree {
    readonly kind: typeof JavaKind.Assignment;
    readonly variable: Expression;
    readonly assignment: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface AssignmentOperation extends J, Statement, TypedTree {
    readonly kind: typeof JavaKind.AssignmentOperation;
    readonly variable: Expression;
    readonly operator: JLeftPadded<AssignmentOperationType>;
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

export interface Binary extends J, TypedTree {
    readonly kind: typeof JavaKind.Binary;
    readonly left: Expression;
    readonly operator: BinaryOperator;
    readonly right: Expression;
    readonly type?: JavaType;
}

export const enum BinaryOperator {
    Equal,
    NotEqual,
    LessThan,
    LessThanOrEqual,
    GreaterThan,
    GreaterThanOrEqual,
    Plus,
    Minus,
    Multiply,
    Divide,
    Modulo,
    And,
    Or,
    Xor,
    LeftShift,
    RightShift,
    UnsignedRightShift
}

export interface Break extends J, Statement {
    readonly kind: typeof JavaKind.Break;
    readonly label?: Identifier;
}

export interface Case extends J {
    readonly kind: typeof JavaKind.Case;
    readonly pattern?: Expression;
    readonly body: JRightPadded<Statement>[];
}

export interface Continue extends J, Statement {
    readonly kind: typeof JavaKind.Continue;
    readonly label?: Identifier;
}

export interface DeconstructionPattern extends J, TypedTree {
    readonly kind: typeof JavaKind.DeconstructionPattern;
    readonly deconstructor: Expression;
    readonly nested: JContainer<J>;
    readonly type?: JavaType;
}

export interface DoWhileLoop extends J, Statement {
    readonly kind: typeof JavaKind.DoWhileLoop;
    readonly condition: Expression;
    readonly body: Statement;
}

export interface EnumValueSet extends J, Statement {
    readonly kind: typeof JavaKind.EnumValueSet;
    readonly enums: JRightPadded<EnumValue>[];
    readonly terminatedWithSemicolon: boolean;
}

export interface EnumValue extends J {
    readonly kind: typeof JavaKind.EnumValue;
    readonly annotations: Annotation[];
    readonly name: Identifier;
    readonly initializer?: NewClass;
}

export interface ForEachLoop extends J, Statement {
    readonly kind: typeof JavaKind.ForEachLoop;
    readonly control: ForEachLoopControl;
    readonly body: Statement;
}

export interface ForEachLoopControl extends J {
    readonly kind: typeof JavaKind.ForEachLoopControl;
    readonly variable: VariableDeclarations;
    readonly iterable: Expression;
}

export interface ForLoop extends J, Statement {
    readonly kind: typeof JavaKind.ForLoop;
    readonly control: ForLoopControl;
    readonly body: Statement;
}

export interface ForLoopControl extends J {
    readonly kind: typeof JavaKind.ForLoopControl;
    readonly init: JRightPadded<Statement>[];
    readonly condition?: Expression;
    readonly update: JRightPadded<Statement>[];
}

export interface If extends J, Statement {
    readonly kind: typeof JavaKind.If;
    readonly ifCondition: ControlParentheses<Expression>;
    readonly thenPart: JRightPadded<Statement>;
    readonly elsePart?: IfElse;
}

export interface IfElse extends J {
    readonly kind: typeof JavaKind.IfElse;
    readonly body: JRightPadded<Statement>;
}

export interface InstanceOf extends J, TypedTree {
    readonly kind: typeof JavaKind.InstanceOf;
    readonly expression: Expression;
    readonly clazz: TypeTree;
    readonly type?: JavaType;
}

export interface IntersectionType extends J, TypeTree {
    readonly kind: typeof JavaKind.IntersectionType;
    readonly bounds: JContainer<TypeTree>;
}

export interface Label extends J, Statement {
    readonly kind: typeof JavaKind.Label;
    readonly label: Identifier;
    readonly statement: Statement;
}

export interface Lambda extends J, Statement, TypedTree {
    readonly kind: typeof JavaKind.Lambda;
    readonly parameters: LambdaParameters;
    readonly arrow: Space;
    readonly body: Statement | Expression;
    readonly type?: JavaType;
}

export interface LambdaParameters extends J {
    readonly kind: typeof JavaKind.LambdaParameters;
    readonly parenthesized: boolean;
    readonly parameters: JRightPadded<J>[];
}

export interface MemberReference extends J, TypedTree {
    readonly kind: typeof JavaKind.MemberReference;
    readonly reference: Expression;
    readonly typeParameters?: JContainer<TypeTree>;
    readonly name: Identifier;
    readonly type?: JavaType;
}

export interface MethodInvocation extends J, TypedTree {
    readonly kind: typeof JavaKind.MethodInvocation;
    readonly select?: Expression;
    readonly typeParameters?: JContainer<TypeTree>;
    readonly name: Identifier;
    readonly arguments: JContainer<Expression>;
    readonly type?: JavaType;
    readonly methodType?: JavaType.Method;
}

export interface NewArray extends J, TypedTree {
    readonly kind: typeof JavaKind.NewArray;
    readonly typeExpression?: TypeTree;
    readonly dimensions: JRightPadded<Expression>[];
    readonly initializer?: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface NewClass extends J, TypedTree {
    readonly kind: typeof JavaKind.NewClass;
    readonly enclosing?: Expression;
    readonly typeParameters?: JContainer<TypeTree>;
    readonly clazz: TypeTree;
    readonly arguments: JContainer<Expression>;
    readonly body?: Block;
    readonly type?: JavaType;
}

export interface NullableType extends J, TypeTree {
    readonly kind: typeof JavaKind.NullableType;
    readonly annotations: Annotation[];
    readonly typeTree: JRightPadded<TypeTree>;
    readonly type?: JavaType;
}

export interface ParameterizedType extends J, TypeTree {
    readonly kind: typeof JavaKind.ParameterizedType;
    readonly clazz: TypeTree;
    readonly typeArguments: JContainer<TypeTree>;
    readonly type: JavaType;
}

export interface Parentheses<J2 extends J> extends J, TypedTree {
    readonly kind: typeof JavaKind.Parentheses;
    readonly tree: JRightPadded<J2>;
    readonly type?: JavaType;
}

export interface ParenthesizedTypeTree extends J, TypeTree {
    readonly kind: typeof JavaKind.ParenthesizedTypeTree;
    readonly annotations: Annotation[];
    readonly parenthesizedType: Parentheses<TypeTree>;
    readonly type?: JavaType;
}

export interface Return extends J, Statement {
    readonly kind: typeof JavaKind.Return;
    readonly expression?: Expression;
}

export interface Switch extends J, Statement {
    readonly kind: typeof JavaKind.Switch;
    readonly selector: Expression;
    readonly cases: JRightPadded<Case>[];
}

export interface SwitchExpression extends J, TypedTree {
    readonly kind: typeof JavaKind.SwitchExpression;
    readonly selector: ControlParentheses<Expression>;
    readonly cases: Block;
    readonly type?: JavaType;
}

export interface Synchronized extends J, Statement {
    readonly kind: typeof JavaKind.Synchronized;
    readonly lock: Expression;
    readonly body: Block;
}

export interface Ternary extends J, TypedTree, Statement {
    readonly kind: typeof JavaKind.Ternary;
    readonly condition: Expression;
    readonly truePart: Expression;
    readonly falsePart: Expression;
    readonly type?: JavaType;
}

export interface Throw extends J, Statement {
    readonly kind: typeof JavaKind.Throw;
    readonly exception: Expression;
}

export interface Try extends J, Statement {
    readonly kind: typeof JavaKind.Try;
    readonly resources?: JContainer<TryResource>;
    readonly body: Block;
    readonly catches: TryCatch[];
    readonly finally?: JLeftPadded<Block>;
}

export interface TryResource extends J {
    readonly kind: typeof JavaKind.TryResource;
    readonly variableDeclarations: TypedTree;
    readonly terminatedWithSemicolon: boolean;
}

export interface TryCatch extends J {
    readonly kind: typeof JavaKind.TryCatch;
    readonly parameter: ControlParentheses<VariableDeclarations>;
    readonly body: Block;
}

export interface TypeCast extends J, TypedTree {
    readonly kind: typeof JavaKind.TypeCast;
    readonly clazz: TypeTree;
    readonly expression: Expression;
    readonly type?: JavaType;
}

export interface Unary extends J, TypedTree, Statement {
    readonly kind: typeof JavaKind.Unary;
    readonly operator: UnaryOperator;
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

export interface Unknown extends J, Statement, TypeTree {
    readonly kind: typeof JavaKind.Unknown;
    readonly source: UnknownSource;
}

export interface UnknownSource extends J {
    readonly kind: typeof JavaKind.UnknownSource;
    readonly text: string;
}

export interface WhileLoop extends J, Statement {
    readonly kind: typeof JavaKind.WhileLoop;
    readonly condition: Expression;
    readonly body: Statement;
}

export interface Wildcard extends J, TypeTree {
    readonly kind: typeof JavaKind.Wildcard;
    readonly bound?: WildcardBound;
    readonly boundedType?: TypeTree;
    readonly type: JavaType;
}

export const enum WildcardBound {
    Extends,
    Super
}

export interface Yield extends J, Statement {
    readonly kind: typeof JavaKind.Yield;
    readonly implicit: boolean;
    readonly value: Expression;
}

export interface JavaType {
}

export namespace JavaType {
    export interface Class extends JavaType {
        readonly fullyQualifiedName: string;
        readonly kind: ClassType;
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
