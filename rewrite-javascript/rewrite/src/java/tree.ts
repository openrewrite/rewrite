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

import {emptyMarkers, Markers, SourceFile, Tree, TreeKind} from "../";
import {JavaType} from "./type";
import {RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {JavaReceiver, JavaSender} from "./rpc";

export interface J extends Tree {
    readonly prefix: J.Space;
}

export interface Expression extends J {
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
        readonly type?: JavaType
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
        readonly type?: JavaType;
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
        readonly type?: JavaType;
    }

    export interface AssignmentOperation extends J, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.AssignmentOperation;
        readonly variable: Expression;
        readonly operator: LeftPadded<AssignmentOperation.Type>;
        readonly assignment: Expression;
        readonly type?: JavaType;
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
        readonly type?: JavaType;
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

    export interface ClassDeclaration extends J, TypedTree {
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
        readonly type?: JavaType.FullyQualified;
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

    export interface ParenthesizedTypeTree extends J, TypeTree, Expression {
        readonly kind: typeof Kind.ParenthesizedTypeTree;
        readonly annotations: Annotation[];
        readonly parenthesizedType: Parentheses<TypeTree>;
    }

    export interface Identifier extends J, TypeTree, Expression, VariableDeclarator {
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

    export interface InstanceOf extends J, Expression, TypedTree {
        readonly kind: typeof Kind.InstanceOf;
        readonly expression: RightPadded<Expression>;
        readonly class: J;
        readonly pattern?: J;
        readonly type?: JavaType;
        readonly modifier?: Modifier;
    }

    export interface DeconstructionPattern extends J, TypedTree {
        readonly kind: typeof Kind.DeconstructionPattern;
        readonly deconstructor: Expression;
        readonly nested: Container<J>;
        readonly type?: JavaType;
    }

    export interface IntersectionType extends J, TypeTree, Expression {
        readonly kind: typeof Kind.IntersectionType;
        readonly bounds: Container<TypeTree>;
        readonly type?: JavaType;
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
        readonly type?: JavaType;
    }

    export namespace Lambda {
        export interface Parameters extends J {
            readonly kind: typeof Kind.LambdaParameters;
            readonly parenthesized: boolean;
            readonly parameters: RightPadded<J>[];
        }
    }

    export interface Literal extends J, TypedTree, Expression {
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

    export interface MemberReference extends J, Expression {
        readonly kind: typeof Kind.MemberReference;
        readonly containing: RightPadded<Expression>;
        readonly typeParameters?: Container<Expression>;
        readonly reference: LeftPadded<Identifier>;
        readonly type?: JavaType;
        readonly methodType?: JavaType.Method;
        readonly variableType?: JavaType.Variable;
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
        readonly methodType?: JavaType.Method;
    }

    export interface MethodInvocation extends J, TypedTree, Expression {
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
        readonly type?: JavaType;
    }

    export interface NewClass extends J, TypedTree, Expression {
        readonly kind: typeof Kind.NewClass;
        readonly enclosing?: RightPadded<Expression>;
        readonly new: Space;
        readonly class?: TypeTree;
        readonly arguments: Container<Expression>;
        readonly body?: Block;
        readonly constructorType?: JavaType.Method;
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
        readonly type?: JavaType;
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

    export interface SwitchExpression extends J, TypedTree, Expression {
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

    export interface Ternary extends J, Statement, Expression, TypedTree {
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
        readonly type?: JavaType;
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
            readonly variableType?: JavaType.Variable;
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
}

export type ContainerLocation =
    "ANNOTATION_ARGUMENTS" |
    "ARRAY_BINDING_PATTERN_ELEMENTS" |
    "CASE_LABELS" |
    "CASE_STATEMENTS" |
    "CLASS_DECLARATION_IMPLEMENTS" |
    "CLASS_DECLARATION_PERMITTING" |
    "CLASS_DECLARATION_PRIMARY_CONSTRUCTOR" |
    "CLASS_DECLARATION_TYPE_PARAMETERS" |
    "COMPUTED_PROPERTY_METHOD_DECLARATION_PARAMETERS" |
    "DECONSTRUCTION_PATTERN" |
    "DECONSTRUCTION_PATTERN_NESTED" |
    "EXPRESSION_WITH_TYPE_ARGUMENTS_TYPE_ARGUMENTS" |
    "FUNCTION_TYPE_PARAMETERS" |
    "IMPORT_ATTRIBUTES_ELEMENTS" |
    "IMPORT_TYPE_ARGUMENT_AND_ATTRIBUTES" |
    "IMPORT_TYPE_ATTRIBUTES_ELEMENTS" |
    "IMPORT_TYPE_TYPE_ARGUMENTS" |
    "INDEX_SIGNATURE_DECLARATION_PARAMETERS" |
    "INTERSECTION_TYPE" |
    "INTERSECTION_TYPE_BOUNDS" |
    "MAPPED_TYPE_VALUE_TYPE" |
    "MEMBER_REFERENCE_TYPE_PARAMETERS" |
    "METHOD_DECLARATION_PARAMETERS" |
    "METHOD_DECLARATION_THROWS" |
    "METHOD_INVOCATION_ARGUMENTS" |
    "METHOD_INVOCATION_TYPE_PARAMETERS" |
    "NAMED_EXPORTS_ELEMENTS" |
    "NAMED_IMPORTS_ELEMENTS" |
    "NEW_ARRAY_INITIALIZER" |
    "NEW_CLASS_ARGUMENTS" |
    "OBJECT_BINDING_DECLARATIONS_BINDINGS" |
    "PARAMETRIZED_TYPE_TYPE_PARAMETERS" |
    "TAGGED_TEMPLATE_EXPRESSION_TYPE_ARGUMENTS" |
    "TRY_RESOURCES" |
    "TUPLE_ELEMENTS" |
    "TYPE_PARAMETER_BOUNDS" |
    "TYPE_QUERY_TYPE_ARGUMENTS";

export type LeftPaddedLocation =
    "ARRAY_TYPE_DIMENSION" |
    "ASSERT_DETAIL" |
    "ASSIGNMENT_ASSIGNMENT" |
    "ASSIGNMENT_OPERATION_EXTENSIONS_OPERATOR" |
    "ASSIGNMENT_OPERATION_OPERATOR" |
    "BINARY_EXTENSIONS_OPERATOR" |
    "BINARY_OPERATOR" |
    "BINDING_ELEMENT_INITIALIZER" |
    "CLASS_DECLARATION_EXTENDS" |
    "CONDITIONAL_TYPE_CONDITION" |
    "DO_WHILE_LOOP_WHILE_CONDITION" |
    "EXPORT_ASSIGNMENT_EXPRESSION" |
    "EXPORT_DECLARATION_MODULE_SPECIFIER" |
    "EXPORT_DECLARATION_TYPE_ONLY" |
    "EXPORT_SPECIFIER_TYPE_ONLY" |
    "FIELD_ACCESS_NAME" |
    "FUNCTION_TYPE_CONSTRUCTOR_TYPE" |
    "FUNCTION_TYPE_RETURN_TYPE" |
    "IMPORT_ALIAS" |
    "IMPORT_ATTRIBUTE_VALUE" |
    "IMPORT_DECLARATION_MODULE_SPECIFIER" |
    "IMPORT_SPECIFIER_IMPORT_TYPE" |
    "IMPORT_STATIC" |
    "IMPORT_TYPE_QUALIFIER" |
    "INDEX_SIGNATURE_DECLARATION_TYPE_EXPRESSION" |
    "INFER_TYPE_TYPE_PARAMETER" |
    "MAPPED_TYPE_HAS_QUESTION_TOKEN" |
    "MAPPED_TYPE_HAS_READONLY" |
    "MAPPED_TYPE_PARAMETER_ITERATE_TYPE" |
    "MAPPED_TYPE_PREFIX_TOKEN" |
    "MAPPED_TYPE_SUFFIX_TOKEN" |
    "MEMBER_REFERENCE_REFERENCE" |
    "METHOD_DECLARATION_DEFAULT_VALUE" |
    "NAMESPACE_DECLARATION_KEYWORD_TYPE" |
    "OBJECT_BINDING_DECLARATIONS_INITIALIZER" |
    "SATISFIES_EXPRESSION_SATISFIES_TYPE" |
    "SCOPED_VARIABLE_DECLARATIONS_SCOPE" |
    "TERNARY_FALSE_PART" |
    "TERNARY_TRUE_PART" |
    "TRY_FINALLY" |
    "TYPE_DECLARATION_INITIALIZER" |
    "TYPE_DECLARATION_NAME" |
    "TYPE_OPERATOR_EXPRESSION" |
    "TYPE_PREDICATE_ASSERTS" |
    "TYPE_PREDICATE_EXPRESSION" |
    "UNARY_OPERATOR" |
    "VARIABLE_DIMENSIONS_AFTER_NAME" |
    "VARIABLE_INITIALIZER" |
    "WILDCARD_BOUND";

export type RightPaddedLocation =
    "ALIAS_PROPERTY_NAME" |
    "ARRAY_DIMENSION_INDEX" |
    "BINDING_ELEMENT_PROPERTY_NAME" |
    "BLOCK_STATEMENTS" |
    "BLOCK_STATIC" |
    "CASE_BODY" |
    "COMPILATION_UNIT_IMPORTS" |
    "COMPILATION_UNIT_PACKAGE_DECLARATION" |
    "COMPUTED_PROPERTY_NAME_EXPRESSION" |
    "CONTAINER_ELEMENTS" |
    "CONTINUE_TREE" |
    "CONTROL_PARENTHESES_TREE" |
    "DO_WHILE_LOOP_BODY" |
    "ELSE_BODY" |
    "ENUM_VALUE_EXPRESSION" |
    "ENUM_VALUE_SET_ENUMS" |
    "FOR_EACH_LOOP_BODY" |
    "FOR_EACH_LOOP_CONTROL_ITERABLE" |
    "FOR_EACH_LOOP_CONTROL_VARIABLE" |
    "FOR_IN_LOOP_BODY" |
    "FOR_IN_LOOP_ITERABLE" |
    "FOR_IN_LOOP_VARIABLE" |
    "FOR_LOOP_BODY" |
    "FOR_LOOP_CONTROL_CONDITION" |
    "FOR_LOOP_CONTROL_INIT" |
    "FOR_LOOP_CONTROL_UPDATE" |
    "FOR_OF_LOOP_ITERABLE" |
    "FOR_OF_LOOP_LOOP" |
    "FOR_OF_LOOP_VARIABLE" |
    "IF_THEN_PART" |
    "IMPORT_CLAUSE_NAME" |
    "IMPORT_TYPE_ATTRIBUTES_TOKEN" |
    "IMPORT_TYPE_HAS_TYPEOF" |
    "INDEX_TYPE_ELEMENT" |
    "INSTANCE_OF_EXPRESSION" |
    "INTERSECTION_TYPES" |
    "JS_COMPILATION_UNIT_STATEMENTS" |
    "KEYS_REMAPPING_NAME_TYPE" |
    "KEYS_REMAPPING_TYPE_PARAMETER" |
    "LABEL_LABEL" |
    "LAMBDA_PARAMETERS_PARAMETERS" |
    "MEMBER_REFERENCE_CONTAINING" |
    "METHOD_INVOCATION_SELECT" |
    "MULTI_CATCH_ALTERNATIVES" |
    "NAMESPACE_DECLARATION_NAME" |
    "NEW_CLASS_ENCLOSING" |
    "NULLABLE_TYPE_TYPE_TREE" |
    "PARAMETERIZED_TYPE_TREE" |
    "PARENTHESES_TREE" |
    "PROPERTY_ASSIGNMENT_NAME" |
    "SCOPED_VARIABLE_DECLARATIONS_VARIABLES" |
    "SPACE_ELEMENTS" |
    "TAGGED_TEMPLATE_EXPRESSION_TAG" |
    "TEMPLATE_EXPRESSION_SPANS" |
    "TRAILING_TOKEN_STATEMENT_EXPRESSION" |
    "TYPE_PARAMETERS_TYPE_PARAMETERS" |
    "TYPE_PARAMETER_CONSTRAINT_TYPE" |
    "TYPE_PARAMETER_DEFAULT_TYPE" |
    "UNION_TYPES" |
    "VARIABLE_DECLARATIONS_VARIABLES" |
    "WHILE_LOOP_BODY" |
    "WITH_STATEMENT_BODY";

export type ElementPrefixLocation =
    "ALIAS_PREFIX" |
    "ANNOTATED_TYPE_PREFIX" |
    "ANNOTATION_PREFIX" |
    "ARRAY_ACCESS_PREFIX" |
    "ARRAY_BINDING_PATTERN_PREFIX" |
    "ARRAY_DIMENSION_PREFIX" |
    "ARRAY_TYPE_PREFIX" |
    "ARROW_FUNCTION_PREFIX" |
    "ASSERT_PREFIX" |
    "ASSIGNMENT_OPERATION_EXTENSIONS_PREFIX" |
    "ASSIGNMENT_OPERATION_PREFIX" |
    "ASSIGNMENT_PREFIX" |
    "AWAIT_PREFIX" |
    "BINARY_EXTENSIONS_PREFIX" |
    "BINARY_PREFIX" |
    "BINDING_ELEMENT_PREFIX" |
    "BLOCK_PREFIX" |
    "BREAK_PREFIX" |
    "CASE_PREFIX" |
    "CLASS_DECLARATION_KIND_PREFIX" |
    "CLASS_DECLARATION_PREFIX" |
    "COMPILATION_UNIT_PREFIX" |
    "COMPUTED_PROPERTY_METHOD_DECLARATION_PREFIX" |
    "COMPUTED_PROPERTY_NAME_PREFIX" |
    "CONDITIONAL_TYPE_PREFIX" |
    "CONTINUE_PREFIX" |
    "CONTROL_PARENTHESES_PREFIX" |
    "DECONSTRUCTION_PATTERN_PREFIX" |
    "DELETE_PREFIX" |
    "DO_WHILE_LOOP_PREFIX" |
    "ELSE_PREFIX" |
    "EMPTY_PREFIX" |
    "ENUM_VALUE_PREFIX" |
    "ENUM_VALUE_SET_PREFIX" |
    "ERRONEOUS_PREFIX" |
    "EXPORT_ASSIGNMENT_PREFIX" |
    "EXPORT_DECLARATION_PREFIX" |
    "EXPORT_SPECIFIER_PREFIX" |
    "EXPRESSION_STATEMENT_PREFIX" |
    "EXPRESSION_WITH_TYPE_ARGUMENTS_PREFIX" |
    "FIELD_ACCESS_PREFIX" |
    "FOR_EACH_LOOP_CONTROL_PREFIX" |
    "FOR_EACH_LOOP_PREFIX" |
    "FOR_IN_LOOP_PREFIX" |
    "FOR_LOOP_CONTROL_PREFIX" |
    "FOR_LOOP_PREFIX" |
    "FOR_OF_LOOP_PREFIX" |
    "FUNCTION_TYPE_PREFIX" |
    "IDENTIFIER_PREFIX" |
    "IF_PREFIX" |
    "IMPORT_ATTRIBUTES_PREFIX" |
    "IMPORT_ATTRIBUTE_PREFIX" |
    "IMPORT_CLAUSE_PREFIX" |
    "IMPORT_DECLARATION_PREFIX" |
    "IMPORT_PREFIX" |
    "IMPORT_SPECIFIER_PREFIX" |
    "IMPORT_TYPE_ATTRIBUTES_PREFIX" |
    "IMPORT_TYPE_PREFIX" |
    "INDEXED_ACCESS_TYPE_PREFIX" |
    "INDEX_SIGNATURE_DECLARATION_PREFIX" |
    "INDEX_TYPE_PREFIX" |
    "INFER_TYPE_PREFIX" |
    "INSTANCE_OF_PREFIX" |
    "INTERSECTION_PREFIX" |
    "INTERSECTION_TYPE_PREFIX" |
    "JS_COMPILATION_UNIT_PREFIX" |
    "KEYS_REMAPPING_PREFIX" |
    "LABEL_PREFIX" |
    "LAMBDA_PARAMETERS_PREFIX" |
    "LAMBDA_PREFIX" |
    "LITERAL_PREFIX" |
    "LITERAL_TYPE_PREFIX" |
    "MAPPED_TYPE_PARAMETER_PREFIX" |
    "MAPPED_TYPE_PREFIX" |
    "MEMBER_REFERENCE_PREFIX" |
    "METHOD_DECLARATION_PREFIX" |
    "METHOD_INVOCATION_PREFIX" |
    "MODIFIER_PREFIX" |
    "MULTI_CATCH_PREFIX" |
    "NAMED_EXPORTS_PREFIX" |
    "NAMED_IMPORTS_PREFIX" |
    "NAMESPACE_DECLARATION_PREFIX" |
    "NEW_ARRAY_PREFIX" |
    "NEW_CLASS_PREFIX" |
    "NULLABLE_TYPE_PREFIX" |
    "OBJECT_BINDING_DECLARATIONS_PREFIX" |
    "PACKAGE_PREFIX" |
    "PARAMETERIZED_TYPE_PREFIX" |
    "PARENTHESES_PREFIX" |
    "PARENTHESIZED_TYPE_TREE_PREFIX" |
    "PRIMITIVE_PREFIX" |
    "PROPERTY_ASSIGNMENT_PREFIX" |
    "RETURN_PREFIX" |
    "SATISFIES_EXPRESSION_PREFIX" |
    "SCOPED_VARIABLE_DECLARATIONS_PREFIX" |
    "STATEMENT_EXPRESSION_PREFIX" |
    "SWITCH_EXPRESSION_PREFIX" |
    "SWITCH_PREFIX" |
    "SYNCHRONIZED_PREFIX" |
    "TAGGED_TEMPLATE_EXPRESSION_PREFIX" |
    "TEMPLATE_EXPRESSION_PREFIX" |
    "TEMPLATE_EXPRESSION_SPAN_PREFIX" |
    "TERNARY_PREFIX" |
    "THROW_PREFIX" |
    "TRAILING_TOKEN_STATEMENT_PREFIX" |
    "TRY_CATCH_PREFIX" |
    "TRY_PREFIX" |
    "TRY_RESOURCE_PREFIX" |
    "TUPLE_PREFIX" |
    "TYPE_CAST_PREFIX" |
    "TYPE_DECLARATION_PREFIX" |
    "TYPE_INFO_PREFIX" |
    "TYPE_LITERAL_PREFIX" |
    "TYPE_OF_PREFIX" |
    "TYPE_OPERATOR_PREFIX" |
    "TYPE_PARAMETERS_PREFIX" |
    "TYPE_PARAMETER_PREFIX" |
    "TYPE_PREDICATE_PREFIX" |
    "TYPE_QUERY_PREFIX" |
    "TYPE_TREE_EXPRESSION_PREFIX" |
    "UNARY_PREFIX" |
    "UNION_PREFIX" |
    "UNKNOWN_PREFIX" |
    "UNKNOWN_SOURCE_PREFIX" |
    "VARIABLE_DECLARATIONS_PREFIX" |
    "VARIABLE_PREFIX" |
    "VOID_PREFIX" |
    "WHILE_LOOP_PREFIX" |
    "WILDCARD_PREFIX" |
    "WITH_STATEMENT_PREFIX" |
    "YIELD_PREFIX";

export type SpaceLocation =
    "ANY" |
    ElementPrefixLocation |
    LeftPaddedLocation | // notice in Java JLeftPadded.beforeLocation has the same name for all values
    "ARRAY_TYPE_DIMENSION" |
    "ARROW_FUNCTION_ARROW" |
    "ARROW_FUNCTION_PARAMETERS" |
    "ARROW_FUNCTION_PREFIX" |
    "ASSIGNMENT_OPERATION_EXTENSIONS_OPERATOR" |
    "ASSIGNMENT_OPERATION_OPERATOR" |
    "BINARY_EXTENSIONS_OPERATOR" |
    "BINARY_OPERATOR" |
    "BLOCK_END" |
    "CASE_STATEMENTS" |
    "CLASS_DECLARATION" |
    "CLASS_DECLARATION_CLASS_KIND" |
    "COMPILATION_UNIT_EOF" |
    "COMPUTED_PROPERTY_METHOD_DECLARATION" |
    "COMPUTED_PROPERTY_METHOD_DECLARATION_PREFIX" |
    "ENUM_VALUE_PREFIX" |
    "FOR_IN_LOOP_PREFIX" |
    "FOR_LOOP_PREFIX" |
    "FOR_OF_LOOP_AWAIT" |
    "FOR_OF_LOOP_PREFIX" |
    "FUNCTION_TYPE_PREFIX" |
    "IDENTIFIER" |
    "IMPORT_TYPE_ATTRIBUTES_END" |
    "JRIGHT_PADDED_LOCAL_AFTER" |
    "JRIGHT_PADDED_LOCAL_SINGLE_AFTER" |
    "JS_COMPILATION_UNIT_EOF" |
    "LAMBDA_ARROW" |
    "MARKER_NON_NULL_ASSERTION" |
    "MARKER_OPTIONAL" |
    "MARKER_SPREAD" |
    "MARKER_TRAILING_COMMA" |
    "METHOD_DECLARATION" |
    "METHOD_DECLARATION_PREFIX" |
    "NAMESPACE_DECLARATION_KEYWORD_TYPE" |
    "NEW_CLASS_NEW" |
    "RIGHT_PADDED_LOCAL_AFTER" |
    "SCOPED_VARIABLE_DECLARATIONS_BEFORE" |
    "SPACE_AFTER" |
    "SPACE_BEFORE" |
    "SPACE_ELEMENT" |
    "STATEMENT_LOCAL_AFTER" |
    "TODO_UNKNOWN" |
    "TYPE_DECLARATION_PREFIX" |
    "TYPE_PARAMETER_BEFORE" |
    "TYPE_PREFIX" |
    "UNARY_OPERATOR" |
    "VARIABLE_DECLARATIONS_AFTER" |
    "VARIABLE_DECLARATIONS_VARARGS" |
    "YIELD_AFTER" |
    "YIELD_BEFORE" |
    "YIELD_ELEMENT" |
    "YIELD_PREFIX";

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

export namespace TypedTree {
    const typeGetters = new Map<TypedTree["kind"], (tree: TypedTree) => JavaType | undefined>();

    export function getType(typeTree?: TypedTree): JavaType | undefined {
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

    export function registerTypeGetter<T extends TypedTree>(kind: T["kind"], fn: (tree: T) => JavaType | undefined): void {
        typeGetters.set(kind, fn as any);
    }

    registerTypeGetter(J.Kind.MethodDeclaration, (tree: J.MethodDeclaration) => tree.methodType?.returnType);
    registerTypeGetter(J.Kind.MethodInvocation, (tree: J.MethodInvocation) => tree.methodType?.returnType);
    registerTypeGetter(J.Kind.Parentheses, (tree: J.Parentheses<TypedTree>) => getType(tree.tree.element));
    registerTypeGetter(J.Kind.NewClass, (tree: J.NewClass) => tree.constructorType?.returnType);

    // TODO ControlParentheses here isn't a TypedTree so why does this compile?
    registerTypeGetter(J.Kind.TypeCast, (tree: J.TypeCast) => getType(tree.class));

    registerTypeGetter(J.Kind.Empty, () => JavaType.unknownType);
    registerTypeGetter(J.Kind.MultiCatch, (tree: J.MultiCatch) => {
        const bounds = tree.alternatives.map(a => getType(a.element));
        return {kind: JavaType.Kind.Union, bounds: bounds};
    });
    registerTypeGetter(J.Kind.NullableType, (tree: J.NullableType) => getType(tree.typeTree.element));
    registerTypeGetter(J.Kind.Wildcard, () => JavaType.unknownType);
    registerTypeGetter(J.Kind.Unknown, () => JavaType.unknownType);
}

const javaReceiver = new JavaReceiver();
const javaSender = new JavaSender();

const javaCodec: RpcCodec<J> = {
    async rpcReceive(before: J, q: RpcReceiveQueue): Promise<J> {
        return (await javaReceiver.visit(before, q))!;
    },

    async rpcSend(after: J, q: RpcSendQueue): Promise<void> {
        await javaSender.visit(after, q);
    }
}

// Register codec for all Java AST node types
Object.values(J.Kind).forEach(kind => {
    if (kind === J.Kind.Space) {
        RpcCodecs.registerCodec(kind, {
                async rpcReceive(before: J.Space, q: RpcReceiveQueue): Promise<J.Space> {
                    return (await javaReceiver.visitSpace(before, "ANY", q))!;
                },

                async rpcSend(after: J.Space, q: RpcSendQueue): Promise<void> {
                    await javaSender.visitSpace(after, "ANY", q);
                }
            }
        );
    } else {
        RpcCodecs.registerCodec(kind, javaCodec);
    }
});
