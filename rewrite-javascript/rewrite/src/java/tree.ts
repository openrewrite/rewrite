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
    Binary: "org.openrewrite.java.tree.J$Binary",
    Block: "org.openrewrite.java.tree.J$Block",
    Break: "org.openrewrite.java.tree.J$Break",
    Case: "org.openrewrite.java.tree.J$Case",
    Catch: "org.openrewrite.java.tree.J$Catch",
    ClassDeclaration: "org.openrewrite.java.tree.J$ClassDeclaration",
    CompilationUnit: "org.openrewrite.java.tree.J$CompilationUnit",
    Continue: "org.openrewrite.java.tree.J$Continue",
    DoWhileLoop: "org.openrewrite.java.tree.J$DoWhileLoop",
    Empty: "org.openrewrite.java.tree.J$Empty",
    EnumValue: "org.openrewrite.java.tree.J$EnumValue",
    FieldAccess: "org.openrewrite.java.tree.J$FieldAccess",
    ForEachLoop: "org.openrewrite.java.tree.J$ForEachLoop",
    ForEachLoopControl: "org.openrewrite.java.tree.J$ForEachLoop$Control",
    ForLoop: "org.openrewrite.java.tree.J$ForLoop",
    ForLoopControl: "org.openrewrite.java.tree.J$ForLoop$Control",
    Identifier: "org.openrewrite.java.tree.J$Identifier",
    If: "org.openrewrite.java.tree.J$If",
    Import: "org.openrewrite.java.tree.J$Import",
    InstanceOf: "org.openrewrite.java.tree.J$InstanceOf",
    Label: "org.openrewrite.java.tree.J$Label",
    Lambda: "org.openrewrite.java.tree.J$Lambda",
    Literal: "org.openrewrite.java.tree.J$Literal",
    MemberReference: "org.openrewrite.java.tree.J$MemberReference",
    MethodDeclaration: "org.openrewrite.java.tree.J$MethodDeclaration",
    MethodInvocation: "org.openrewrite.java.tree.J$MethodInvocation",
    Modifier: "org.openrewrite.java.tree.J$Modifier",
    NewArray: "org.openrewrite.java.tree.J$NewArray",
    NewClass: "org.openrewrite.java.tree.J$NewClass",
    Package: "org.openrewrite.java.tree.J$Package",
    ParameterizedType: "org.openrewrite.java.tree.J$ParameterizedType",
    Parentheses: "org.openrewrite.java.tree.J$Parentheses",
    Primitive: "org.openrewrite.java.tree.J$Primitive",
    Return: "org.openrewrite.java.tree.J$Return",
    Switch: "org.openrewrite.java.tree.J$Switch",
    Synchronized: "org.openrewrite.java.tree.J$Synchronized",
    Ternary: "org.openrewrite.java.tree.J$Ternary",
    Throw: "org.openrewrite.java.tree.J$Throw",
    Try: "org.openrewrite.java.tree.J$Try",
    TypeCast: "org.openrewrite.java.tree.J$TypeCast",
    TypeParameter: "org.openrewrite.java.tree.J$TypeParameter",
    Unary: "org.openrewrite.java.tree.J$Unary",
    VariableDeclarations: "org.openrewrite.java.tree.J$VariableDeclarations",
    Variable: "org.openrewrite.java.tree.J$VariableDeclarations$NamedVariable",
    WhileLoop: "org.openrewrite.java.tree.J$WhileLoop",
    Wildcard: "org.openrewrite.java.tree.J$Wildcard",
    TextComment: "org.openrewrite.java.tree.TextComment",
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
    readonly kind: typeof JavaKind.TextComment;
    readonly multiline: boolean;
    readonly text: string;
    readonly suffix: string;
    readonly markers: Markers;
}

export interface J extends Tree {
    readonly prefix: Space;
}

export interface AnnotatedType extends J, Expression, TypeTree {
    readonly kind: typeof JavaKind.AnnotatedType;
    readonly annotations: Annotation[];
    readonly typeExpression: TypeTree;
    readonly type: JavaType | null;
}

export interface ArrayAccess extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.ArrayAccess;
    readonly indexed: Expression;
    readonly dimension: ArrayDimension;
    readonly type: JavaType | null;
}

export interface ArrayDimension extends J {
    readonly kind: typeof JavaKind.ArrayDimension;
    readonly index: Expression;
}

export interface ArrayType extends J, TypeTree, Expression {
    readonly kind: typeof JavaKind.ArrayType;
    readonly elementType: TypeTree;
    readonly annotations: Annotation[] | null;
    readonly dimension: JLeftPadded<Space> | null;
    readonly type: JavaType;
}

export interface CompilationUnit extends JavaSourceFile, J {
    readonly kind: typeof JavaKind.CompilationUnit;
    readonly eof: Space;
}

export interface Empty extends J {
    readonly kind: typeof JavaKind.Empty;
}

export interface Identifier extends J {
    readonly kind: typeof JavaKind.Identifier;
    readonly name: string;
}

export interface Literal extends J {
    readonly kind: typeof JavaKind.Literal;
    readonly source: string;
    readonly value?: Object;
}

export interface JRightPadded<T extends J> {
    readonly kind: typeof JavaKind.JRightPadded;
    readonly element: T;
    readonly after: Space;
    readonly markers: Markers;
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
    readonly annotations: JRightPadded<Annotation>[];
    readonly modifiers: JRightPadded<Modifier>[];
    readonly classKind: ClassKind;
    readonly name: Identifier;
    readonly typeParameters: JRightPadded<TypeParameter>[];
    readonly extends: JRightPadded<TypeTree> | null;
    readonly implements: JRightPadded<TypeTree>[];
    readonly body: Block;
}

export interface Annotation extends J, Expression {
    readonly kind: typeof JavaKind.Annotation;
    readonly annotationType: NameTree;
    readonly arguments: JContainer<Expression> | null;
    readonly type: JavaType | null;
}

export interface Modifier extends J {
    readonly kind: typeof JavaKind.Modifier;
    readonly type: ModifierType;
}

export enum ModifierType {
    Public = "public",
    Protected = "protected",
    Private = "private",
    Abstract = "abstract",
    Static = "static",
    Final = "final",
    Transient = "transient",
    Volatile = "volatile",
    Synchronized = "synchronized",
    Native = "native",
    Strictfp = "strictfp"
}

export interface TypeParameter extends J {
    readonly kind: typeof JavaKind.TypeParameter;
    readonly annotations: JRightPadded<Annotation>[];
    readonly name: Identifier;
    readonly bounds: JRightPadded<TypeTree>[];
}

export interface TypedTree extends J {
    readonly type: JavaType | null;
    withType(type: JavaType | null): this;
}

export interface NameTree extends TypedTree {
}

export interface TypeTree extends NameTree {
}

export interface Primitive extends TypeTree {
    readonly kind: typeof JavaKind.Primitive;
    readonly keyword: string;
    readonly type: JavaType;
}

export interface Block extends J {
    readonly kind: typeof JavaKind.Block;
    readonly statements: JRightPadded<Statement>[];
}

export interface Statement extends J {
}

export interface Expression extends J {
}

export interface FieldAccess extends Expression, TypedTree {
    readonly kind: typeof JavaKind.FieldAccess;
    readonly target: Expression;
    readonly name: Identifier;
    readonly type: JavaType | null;
}

export interface MethodDeclaration extends J {
    readonly kind: typeof JavaKind.MethodDeclaration;
    readonly annotations: JRightPadded<Annotation>[];
    readonly modifiers: JRightPadded<Modifier>[];
    readonly returnTypeExpression: TypeTree | null;
    readonly name: Identifier;
    readonly parameters: JRightPadded<VariableDeclarations>[];
    readonly throws: JRightPadded<TypeTree>[];
    readonly body: Block | null;
    readonly defaultValue: Expression | null;
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
    readonly initializer: JLeftPadded<Expression> | null;
    readonly variableType: JavaType.Variable | null;
}

export interface JavaType {
}

export namespace JavaType {
    export interface Class extends JavaType {
        readonly fullyQualifiedName: string;
        readonly kind: ClassKind;
        readonly typeParameters: JavaType[];
        readonly supertype: JavaType.Class | null;
        readonly owningClass: JavaType.Class | null;
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
        readonly defaultValue: string[] | null;
        readonly declaredFormalTypeNames: string[];
    }

    export interface Variable extends JavaType {
        readonly name: string;
        readonly owner: JavaType | null;
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

    export enum Variance {
        Covariant = "COVARIANT",
        Contravariant = "CONTRAVARIANT",
        Invariant = "INVARIANT"
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

export enum ClassKind {
    Class = "class",
    Interface = "interface",
    Enum = "enum",
    Annotation = "annotation",
    Record = "record",
    Value = "value"
}

export interface JavaCoordinates {
}

export interface JContainer<T extends J> {
    readonly kind: typeof JavaKind.JContainer;
    readonly elements: JRightPadded<T>[];
}

export interface JLeftPadded<T> {
    readonly kind: typeof JavaKind.JLeftPadded;
    readonly element: T;
    readonly before: Space;
    readonly markers: Markers;
}

export interface Assert extends J, Statement {
    readonly kind: typeof JavaKind.Assert;
    readonly condition: Expression;
    readonly detail: JLeftPadded<Expression> | null;
}

export interface Assignment extends J, Statement, Expression, TypedTree {
    readonly kind: typeof JavaKind.Assignment;
    readonly variable: Expression;
    readonly assignment: JLeftPadded<Expression>;
    readonly type: JavaType | null;
}

export interface Binary extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.Binary;
    readonly left: Expression;
    readonly operator: BinaryOperator;
    readonly right: Expression;
    readonly type: JavaType | null;
}

export enum BinaryOperator {
    Equal = "==",
    NotEqual = "!=",
    LessThan = "<",
    LessThanOrEqual = "<=",
    GreaterThan = ">",
    GreaterThanOrEqual = ">=",
    Plus = "+",
    Minus = "-",
    Multiply = "*",
    Divide = "/",
    Modulo = "%",
    And = "&",
    Or = "|",
    Xor = "^",
    LeftShift = "<<",
    RightShift = ">>",
    UnsignedRightShift = ">>>"
}

export interface Break extends J, Statement {
    readonly kind: typeof JavaKind.Break;
    readonly label: Identifier | null;
}

export interface Case extends J {
    readonly kind: typeof JavaKind.Case;
    readonly pattern: Expression | null;
    readonly body: JRightPadded<Statement>[];
}

export interface Catch extends J {
    readonly kind: typeof JavaKind.Catch;
    readonly parameter: VariableDeclarations;
    readonly body: Block;
}

export interface Continue extends J, Statement {
    readonly kind: typeof JavaKind.Continue;
    readonly label: Identifier | null;
}

export interface DoWhileLoop extends J, Statement {
    readonly kind: typeof JavaKind.DoWhileLoop;
    readonly condition: Expression;
    readonly body: Statement;
}

export interface EnumValue extends J {
    readonly kind: typeof JavaKind.EnumValue;
    readonly name: Identifier;
    readonly arguments: JRightPadded<Expression>[];
    readonly body: Block | null;
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
    readonly condition: Expression | null;
    readonly update: JRightPadded<Statement>[];
}

export interface If extends J, Statement {
    readonly kind: typeof JavaKind.If;
    readonly condition: Expression;
    readonly thenPart: Statement;
    readonly elsePart: Statement | null;
}

export interface InstanceOf extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.InstanceOf;
    readonly expression: Expression;
    readonly clazz: TypeTree;
    readonly type: JavaType | null;
}

export interface Label extends J, Statement {
    readonly kind: typeof JavaKind.Label;
    readonly label: Identifier;
    readonly statement: Statement;
}

export interface Lambda extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.Lambda;
    readonly parameters: JRightPadded<VariableDeclarations>[];
    readonly body: Statement | Expression;
    readonly type: JavaType | null;
}

export interface MemberReference extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.MemberReference;
    readonly reference: Expression;
    readonly typeParameters: JContainer<TypeTree> | null;
    readonly name: Identifier;
    readonly type: JavaType | null;
}

export interface MethodInvocation extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.MethodInvocation;
    readonly select: Expression | null;
    readonly typeParameters: JContainer<TypeTree> | null;
    readonly name: Identifier;
    readonly arguments: JContainer<Expression>;
    readonly type: JavaType | null;
    readonly methodType: JavaType.Method | null;
}

export interface NewArray extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.NewArray;
    readonly typeExpression: TypeTree | null;
    readonly dimensions: JRightPadded<Expression>[];
    readonly initializer: JContainer<Expression> | null;
    readonly type: JavaType | null;
}

export interface NewClass extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.NewClass;
    readonly enclosing: Expression | null;
    readonly typeParameters: JContainer<TypeTree> | null;
    readonly clazz: TypeTree;
    readonly arguments: JContainer<Expression>;
    readonly body: Block | null;
    readonly type: JavaType | null;
}

export interface ParameterizedType extends J, TypeTree {
    readonly kind: typeof JavaKind.ParameterizedType;
    readonly clazz: TypeTree;
    readonly typeArguments: JContainer<TypeTree>;
    readonly type: JavaType;
}

export interface Parentheses extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.Parentheses;
    readonly expression: Expression;
    readonly type: JavaType | null;
}

export interface Return extends J, Statement {
    readonly kind: typeof JavaKind.Return;
    readonly expression: Expression | null;
}

export interface Switch extends J, Statement {
    readonly kind: typeof JavaKind.Switch;
    readonly selector: Expression;
    readonly cases: JRightPadded<Case>[];
}

export interface Synchronized extends J, Statement {
    readonly kind: typeof JavaKind.Synchronized;
    readonly lock: Expression;
    readonly body: Block;
}

export interface Ternary extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.Ternary;
    readonly condition: Expression;
    readonly truePart: Expression;
    readonly falsePart: Expression;
    readonly type: JavaType | null;
}

export interface Throw extends J, Statement {
    readonly kind: typeof JavaKind.Throw;
    readonly exception: Expression;
}

export interface Try extends J, Statement {
    readonly kind: typeof JavaKind.Try;
    readonly resources: JRightPadded<VariableDeclarations>[];
    readonly body: Block;
    readonly catches: JRightPadded<Catch>[];
    readonly finally: Block | null;
}

export interface TypeCast extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.TypeCast;
    readonly clazz: TypeTree;
    readonly expression: Expression;
    readonly type: JavaType | null;
}

export interface Unary extends J, Expression, TypedTree {
    readonly kind: typeof JavaKind.Unary;
    readonly operator: UnaryOperator;
    readonly expression: Expression;
    readonly type: JavaType | null;
}

export enum UnaryOperator {
    PreIncrement = "++",
    PreDecrement = "--",
    PostIncrement = "++",
    PostDecrement = "--",
    Positive = "+",
    Negative = "-",
    Complement = "~",
    Not = "!"
}

export interface WhileLoop extends J, Statement {
    readonly kind: typeof JavaKind.WhileLoop;
    readonly condition: Expression;
    readonly body: Statement;
}

export interface Wildcard extends J, TypeTree {
    readonly kind: typeof JavaKind.Wildcard;
    readonly bound: WildcardBound | null;
    readonly boundedType: TypeTree | null;
    readonly type: JavaType;
}

export enum WildcardBound {
    Extends = "extends",
    Super = "super"
}