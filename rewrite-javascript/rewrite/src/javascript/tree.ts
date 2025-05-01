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

import {Checksum, FileAttributes, Markers, SourceFile, TreeKind, UUID} from "../";
import {
    J,
    JavaType,
    Statement,
} from "../java";

export interface JS extends J {
}

type JavaScriptExpressionBase = JS.Alias | JS.ArrowFunction | JS.Await | JS.ConditionalType | JS.DefaultType | JS.Delete |
    JS.ExpressionStatement | JS.TrailingTokenStatement | JS.ExpressionWithTypeArguments | JS.FunctionType | JS.InferType |
    JS.ImportType | JS.IndexedAccessType | JS.NamedImports | JS.JsImportSpecifier | JS.JsBinary | JS.JsAssignmentOperation |
    JS.LiteralType | JS.MappedType | JS.ObjectBindingDeclarations | JS.SatisfiesExpression | JS.StatementExpression |
    JS.TaggedTemplateExpression | JS.TemplateExpression | JS.Tuple | JS.TypeOf | JS.TypeTreeExpression | JS.TypeQuery |
    JS.TypeInfo | JS.TypeOperator | JS.TypePredicate | JS.Unary | JS.Union | JS.Intersection | JS.Void | JS.Yield;

export type Expression<T extends {
    kind: string
} = never> = import("../java/tree").Expression<JavaScriptExpressionBase | T>;

type JavaScriptTypedTreeBase = JS.ArrowFunction | JS.DefaultType | JS.JsImportSpecifier | JS.JsBinary | JS.ObjectBindingDeclarations |
    JS.PropertyAssignment | JS.TypeDeclaration | JS.Unary | JS.JSVariableDeclarations | JS.JSMethodDeclaration | JS.FunctionDeclaration |
    JS.IndexSignatureDeclaration | JS.ArrayBindingPattern | JS.JsAssignmentOperation;

export type TypedTree<T extends {
    kind: string
} = never> = import("../java/tree").TypedTree<JavaScriptTypedTreeBase | T>;

type JavaScriptTypeTreeBase = JS.ConditionalType | JS.ExpressionWithTypeArguments | JS.FunctionType |
    JS.InferType | JS.ImportType | JS.LiteralType | JS.MappedType | JS.TemplateExpression | JS.Tuple |
    JS.TypeQuery | JS.TypeOperator | JS.TypePredicate | JS.Union | JS.Intersection | JS.TypeInfo | JS.TypeLiteral |
    JS.BindingElement | JS.IndexedAccessType;

export type TypeTree<T extends {
    kind: string
} = never> = import("../java/tree").TypeTree<JavaScriptTypeTreeBase | T>;

export namespace JS {
    export const Kind = {
        ...TreeKind,
        Alias: "org.openrewrite.javascript.tree.JS$Alias",
        ArrayBindingPattern: "org.openrewrite.javascript.tree.JS$ArrayBindingPattern",
        ArrowFunction: "org.openrewrite.javascript.tree.JS$ArrowFunction",
        Await: "org.openrewrite.javascript.tree.JS$Await",
        BindingElement: "org.openrewrite.javascript.tree.JS$BindingElement",
        CompilationUnit: "org.openrewrite.javascript.tree.JS$CompilationUnit",
        ConditionalType: "org.openrewrite.javascript.tree.JS$ConditionalType",
        DefaultType: "org.openrewrite.javascript.tree.JS$DefaultType",
        Delete: "org.openrewrite.javascript.tree.JS$Delete",
        Export: "org.openrewrite.javascript.tree.JS$Export",
        ExportAssignment: "org.openrewrite.javascript.tree.JS$ExportAssignment",
        ExportDeclaration: "org.openrewrite.javascript.tree.JS$ExportDeclaration",
        ExportSpecifier: "org.openrewrite.javascript.tree.JS$ExportSpecifier",
        ExpressionStatement: "org.openrewrite.javascript.tree.JS$ExpressionStatement",
        ExpressionWithTypeArguments: "org.openrewrite.javascript.tree.JS$ExpressionWithTypeArguments",
        FunctionDeclaration: "org.openrewrite.javascript.tree.JS$FunctionDeclaration",
        FunctionType: "org.openrewrite.javascript.tree.JS$FunctionType",
        ImportAttribute: "org.openrewrite.javascript.tree.JS$ImportAttribute",
        ImportAttributes: "org.openrewrite.javascript.tree.JS$ImportAttributes",
        ImportType: "org.openrewrite.javascript.tree.JS$ImportType",
        ImportTypeAttributes: "org.openrewrite.javascript.tree.JS$ImportTypeAttributes",
        IndexSignatureDeclaration: "org.openrewrite.javascript.tree.JS$IndexSignatureDeclaration",
        IndexType: "org.openrewrite.javascript.tree.JS$IndexedAccessType$IndexType",
        IndexedAccessType: "org.openrewrite.javascript.tree.JS$IndexedAccessType",
        IndexedAccessTypeIndexType: "org.openrewrite.javascript.tree.JS$IndexedAccessType$IndexType",
        InferType: "org.openrewrite.javascript.tree.JS$InferType",
        Intersection: "org.openrewrite.javascript.tree.JS$Intersection",
        JSCatch: "org.openrewrite.javascript.tree.JS$JSTry$JSCatch",
        JSForInLoop: "org.openrewrite.javascript.tree.JS$JSForInLoop",
        JSForInOfLoopControl: "org.openrewrite.javascript.tree.JS$JSForInOfLoopControl",
        JSForOfLoop: "org.openrewrite.javascript.tree.JS$JSForOfLoop",
        JSMethodDeclaration: "org.openrewrite.javascript.tree.JS$JSMethodDeclaration",
        JSNamedVariable: "org.openrewrite.javascript.tree.JS$JSVariableDeclarations$JSNamedVariable",
        JSTry: "org.openrewrite.javascript.tree.JS$JSTry",
        JSVariableDeclarations: "org.openrewrite.javascript.tree.JS$JSVariableDeclarations",
        JsAssignmentOperation: "org.openrewrite.javascript.tree.JS$JsAssignmentOperation",
        JsBinary: "org.openrewrite.javascript.tree.JS$JsBinary",
        JsImport: "org.openrewrite.javascript.tree.JS$JsImport",
        JsImportClause: "org.openrewrite.javascript.tree.JS$JsImportClause",
        JsImportSpecifier: "org.openrewrite.javascript.tree.JS$JsImportSpecifier",
        LiteralType: "org.openrewrite.javascript.tree.JS$LiteralType",
        MappedType: "org.openrewrite.javascript.tree.JS$MappedType",
        MappedTypeKeysRemapping: "org.openrewrite.javascript.tree.JS$MappedType$KeysRemapping",
        MappedTypeMappedTypeParameter: "org.openrewrite.javascript.tree.JS$MappedType$MappedTypeParameter",
        NamedExports: "org.openrewrite.javascript.tree.JS$NamedExports",
        NamedImports: "org.openrewrite.javascript.tree.JS$NamedImports",
        NamespaceDeclaration: "org.openrewrite.javascript.tree.JS$NamespaceDeclaration",
        ObjectBindingDeclarations: "org.openrewrite.javascript.tree.JS$ObjectBindingDeclarations",
        PropertyAssignment: "org.openrewrite.javascript.tree.JS$PropertyAssignment",
        SatisfiesExpression: "org.openrewrite.javascript.tree.JS$SatisfiesExpression",
        ScopedVariableDeclarations: "org.openrewrite.javascript.tree.JS$ScopedVariableDeclarations",
        StatementExpression: "org.openrewrite.javascript.tree.JS$StatementExpression",
        TaggedTemplateExpression: "org.openrewrite.javascript.tree.JS$TaggedTemplateExpression",
        TemplateExpression: "org.openrewrite.javascript.tree.JS$TemplateExpression",
        TemplateExpressionTemplateSpan: "org.openrewrite.javascript.tree.JS$TemplateExpression$TemplateSpan",
        TrailingTokenStatement: "org.openrewrite.javascript.tree.JS$TrailingTokenStatement",
        Tuple: "org.openrewrite.javascript.tree.JS$Tuple",
        TypeDeclaration: "org.openrewrite.javascript.tree.JS$TypeDeclaration",
        TypeInfo: "org.openrewrite.javascript.tree.JS$TypeInfo",
        TypeLiteral: "org.openrewrite.javascript.tree.JS$TypeLiteral",
        TypeOf: "org.openrewrite.javascript.tree.JS$TypeOf",
        TypeOperator: "org.openrewrite.javascript.tree.JS$TypeOperator",
        TypePredicate: "org.openrewrite.javascript.tree.JS$TypePredicate",
        TypeQuery: "org.openrewrite.javascript.tree.JS$TypeQuery",
        TypeTreeExpression: "org.openrewrite.javascript.tree.JS$TypeTreeExpression",
        Unary: "org.openrewrite.javascript.tree.JS$Unary",
        Union: "org.openrewrite.javascript.tree.JS$Union",
        Void: "org.openrewrite.javascript.tree.JS$Void",
        WithStatement: "org.openrewrite.javascript.tree.JS$WithStatement",
        Yield: "org.openrewrite.javascript.tree.JS$Yield",
    } as const;

    export interface JavaScriptSourceFile extends JS, SourceFile {
        readonly imports: J.RightPadded<J.Import>[];
        readonly statements: J.RightPadded<Statement>[];
    }

    export interface CompilationUnit extends JavaScriptSourceFile, JS {
        readonly kind: typeof Kind.CompilationUnit;
        readonly eof: J.Space;
    }

    export interface Alias extends JS {
        readonly kind: typeof Kind.Alias;
        readonly propertyName: J.RightPadded<J.Identifier>;
        readonly alias: Expression;
    }

    export interface ArrowFunction extends JS, Statement {
        readonly kind: typeof Kind.ArrowFunction;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeParameters?: J.TypeParameters;
        readonly parameters: J.Lambda.Parameters;
        readonly returnTypeExpression?: TypeTree;
        readonly body: J.LeftPadded<J>;
        readonly type?: JavaType;
    }

    export interface Await extends JS {
        readonly kind: typeof Kind.Await;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export interface ConditionalType extends JS {
        readonly kind: typeof Kind.ConditionalType;
        readonly checkType: Expression;
        readonly condition: J.Container<TypedTree>;
        readonly type?: JavaType;
    }

    export interface DefaultType extends JS {
        readonly kind: typeof Kind.DefaultType;
        readonly left: Expression;
        readonly beforeEquals: J.Space;
        readonly right: Expression;
        readonly type?: JavaType;
    }

    export interface Delete extends JS, Statement {
        readonly kind: typeof Kind.Delete;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export interface Export extends JS, Statement {
        readonly kind: typeof Kind.Export;
        readonly exports?: J.Container<Expression>;
        readonly from?: J.Space;
        readonly target?: J.Literal;
        readonly initializer?: J.LeftPadded<Expression>;
    }

    export interface ExpressionStatement extends JS, Statement {
        readonly kind: typeof Kind.ExpressionStatement;
        readonly expression: Expression;
    }

    export interface ExpressionWithTypeArguments extends JS {
        readonly kind: typeof Kind.ExpressionWithTypeArguments;
        readonly clazz: J;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface FunctionType extends JS {
        readonly kind: typeof Kind.FunctionType;
        readonly modifiers: J.Modifier[];
        readonly constructorType: J.LeftPadded<boolean>;
        readonly typeParameters?: J.TypeParameters;
        readonly parameters: J.Container<Statement>;
        readonly returnType: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface InferType extends JS {
        readonly kind: typeof Kind.InferType;
        readonly typeParameter: J.LeftPadded<J>;
        readonly type?: JavaType;
    }

    export interface ImportType extends JS {
        readonly kind: typeof Kind.ImportType;
        readonly hasTypeof: J.RightPadded<boolean>;
        readonly argumentAndAttributes: J.Container<J>;
        readonly qualifier?: J.LeftPadded<Expression>;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface JsImport extends JS, Statement {
        readonly kind: typeof Kind.JsImport;
        readonly modifiers: J.Modifier[];
        readonly importClause?: JsImportClause;
        readonly moduleSpecifier: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
    }

    export interface JsImportClause extends JS {
        readonly kind: typeof Kind.JsImportClause;
        readonly typeOnly: boolean;
        readonly name?: J.RightPadded<J.Identifier>;
        readonly namedBindings?: Expression;
    }

    export interface NamedImports extends JS {
        readonly kind: typeof Kind.NamedImports;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface JsImportSpecifier extends JS {
        readonly kind: typeof Kind.JsImportSpecifier;
        readonly importType: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: JavaType;
    }

    export interface JSVariableDeclarations extends JS, Statement {
        readonly kind: typeof Kind.JSVariableDeclarations;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeExpression?: TypeTree;
        readonly varargs?: J.Space;
        readonly variables: J.RightPadded<JSVariableDeclarations.JSNamedVariable>[];
    }

    export namespace JSVariableDeclarations {
        export interface JSNamedVariable extends JS {
            readonly kind: typeof Kind.JSNamedVariable;
            readonly name: Expression;
            readonly dimensionsAfterName: J.LeftPadded<J.Space>[];
            readonly initializer?: J.LeftPadded<Expression>;
            readonly variableType?: JavaType.Variable;
        }
    }

    export interface ImportAttributes extends JS {
        readonly kind: typeof Kind.ImportAttributes;
        readonly token: ImportAttributes.Token;
        readonly elements: J.Container<Statement>;
    }

    export namespace ImportAttributes {
        export const enum Token {
            With,
            Assert
        }
    }

    export interface ImportTypeAttributes extends JS {
        readonly kind: typeof Kind.ImportTypeAttributes;
        readonly token: J.RightPadded<Expression>;
        readonly elements: J.Container<ImportAttribute>;
        readonly end: J.Space;
    }

    export interface ImportAttribute extends JS, Statement {
        readonly kind: typeof Kind.ImportAttribute;
        readonly name: Expression;
        readonly value: J.LeftPadded<Expression>;
    }

    export interface JsBinary extends JS {
        readonly kind: typeof Kind.JsBinary;
        readonly left: Expression;
        readonly operator: J.LeftPadded<JsBinary.Type>;
        readonly right: Expression;
        readonly type?: JavaType;
    }

    export namespace JsBinary {
        export const enum Type {
            As,
            IdentityEquals,
            IdentityNotEquals,
            In,
            QuestionQuestion,
            Comma
        }
    }

    export interface LiteralType extends JS {
        readonly kind: typeof Kind.LiteralType;
        readonly literal: Expression;
        readonly type: JavaType;
    }

    export interface MappedType extends JS {
        readonly kind: typeof Kind.MappedType;
        readonly prefixToken?: J.LeftPadded<J.Literal>;
        readonly hasReadonly: J.LeftPadded<boolean>;
        readonly keysRemapping: MappedType.KeysRemapping;
        readonly suffixToken?: J.LeftPadded<J.Literal>;
        readonly hasQuestionToken: J.LeftPadded<boolean>;
        readonly valueType: J.Container<TypeTree>;
        readonly type?: JavaType;
    }

    export namespace MappedType {
        export interface KeysRemapping extends JS, Statement {
            readonly kind: typeof Kind.MappedTypeKeysRemapping;
            readonly typeParameter: J.RightPadded<MappedType.MappedTypeParameter>;
            readonly nameType?: J.RightPadded<Expression>;
        }

        export interface MappedTypeParameter extends JS, Statement {
            readonly kind: typeof Kind.MappedTypeMappedTypeParameter;
            readonly name: Expression;
            readonly iterateType: J.LeftPadded<TypeTree>;
        }
    }

    export interface ObjectBindingDeclarations extends JS {
        readonly kind: typeof Kind.ObjectBindingDeclarations;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeExpression?: TypeTree;
        readonly bindings: J.Container<J>;
        readonly initializer?: J.LeftPadded<Expression>;
    }

    export interface PropertyAssignment extends JS, Statement {
        readonly kind: typeof Kind.PropertyAssignment;
        readonly name: J.RightPadded<Expression>;
        readonly assigmentToken: PropertyAssignment.Token;
        readonly initializer?: Expression;
    }

    export namespace PropertyAssignment {
        export const enum Token {
            Colon,
            Equals,
            Empty
        }
    }

    export interface SatisfiesExpression extends JS {
        readonly kind: typeof Kind.SatisfiesExpression;
        readonly expression: J;
        readonly satisfiesType: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface ScopedVariableDeclarations extends JS, Statement {
        readonly kind: typeof Kind.ScopedVariableDeclarations;
        readonly modifiers: J.Modifier[];
        readonly scope?: J.LeftPadded<ScopedVariableDeclarations.Scope>;
        readonly variables: J.RightPadded<J>[];
    }

    export namespace ScopedVariableDeclarations {
        export const enum Scope {
            Const,
            Let,
            Var,
            Using,
            Import
        }
    }

    export interface StatementExpression extends JS, Statement {
        readonly kind: typeof Kind.StatementExpression;
        readonly statement: Statement;
    }

    export interface TaggedTemplateExpression extends JS, Statement {
        readonly kind: typeof Kind.TaggedTemplateExpression;
        readonly tag?: J.RightPadded<Expression>;
        readonly typeArguments?: J.Container<Expression>;
        readonly templateExpression: Expression;
        readonly type?: JavaType;
    }

    export interface TemplateExpression extends JS, Statement {
        readonly kind: typeof Kind.TemplateExpression;
        readonly head: J.Literal;
        readonly templateSpans: J.RightPadded<TemplateExpression.TemplateSpan>[];
        readonly type?: JavaType;
    }

    export namespace TemplateExpression {
        export interface TemplateSpan extends JS {
            readonly kind: typeof Kind.TemplateExpressionTemplateSpan;
            readonly expression: J;
            readonly tail: J.Literal;
        }
    }

    export interface TrailingTokenStatement extends JS, Statement {
        readonly kind: typeof Kind.TrailingTokenStatement;
        readonly expression: J.RightPadded<J>;
        readonly type?: JavaType;
    }

    export interface Tuple extends JS {
        readonly kind: typeof Kind.Tuple;
        readonly elements: J.Container<J>;
        readonly type?: JavaType;
    }

    export interface TypeDeclaration extends JS, Statement {
        readonly kind: typeof Kind.TypeDeclaration;
        readonly modifiers: J.Modifier[];
        readonly name: J.LeftPadded<J.Identifier>;
        readonly typeParameters?: J.TypeParameters;
        readonly initializer: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface TypeOf extends JS {
        readonly kind: typeof Kind.TypeOf;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export interface TypeTreeExpression extends JS {
        readonly kind: typeof Kind.TypeTreeExpression;
        readonly expression: Expression;
    }

    export interface JsAssignmentOperation extends JS, Statement {
        readonly kind: typeof Kind.JsAssignmentOperation;
        readonly variable: Expression;
        readonly operator: J.LeftPadded<JsAssignmentOperation.Type>;
        readonly assignment: Expression;
        readonly type?: JavaType;
    }

    export namespace JsAssignmentOperation {
        export const enum Type {
            QuestionQuestion,
            And,
            Or,
            Power,
            Exp
        }
    }

    export interface IndexedAccessType extends JS {
        readonly kind: typeof Kind.IndexedAccessType;
        readonly objectType: TypeTree;
        readonly indexType: TypeTree;
        readonly type?: JavaType;
    }

    export function newIndexedAccessType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        objectType: TypeTree,
        indexType: TypeTree,
        type?: JavaType
    ): JS.IndexedAccessType {
        return {
            kind: JS.Kind.IndexedAccessType,
            id,
            prefix,
            markers,
            objectType,
            indexType,
            type
        };
    }

    export namespace IndexedAccessType {
        export interface IndexType extends JS {
            readonly kind: typeof Kind.IndexedAccessTypeIndexType;
            readonly element: J.RightPadded<TypeTree>;
            readonly type?: JavaType;
        }
    }

    export interface TypeQuery extends JS {
        readonly kind: typeof Kind.TypeQuery;
        readonly typeExpression: TypeTree;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface TypeInfo extends JS {
        readonly kind: typeof Kind.TypeInfo;
        readonly typeIdentifier: TypeTree;
    }

    export interface TypeOperator extends JS {
        readonly kind: typeof Kind.TypeOperator;
        readonly operator: TypeOperator.Type;
        readonly expression: J.LeftPadded<Expression>;
    }

    export namespace TypeOperator {
        export const enum Type {
            ReadOnly,
            KeyOf,
            Unique
        }
    }

    export interface TypePredicate extends JS {
        readonly kind: typeof Kind.TypePredicate;
        readonly asserts: J.LeftPadded<boolean>;
        readonly parameterName: J.Identifier;
        readonly expression?: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface Union extends JS {
        readonly kind: typeof Kind.Union;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: JavaType;
    }

    export interface Intersection extends JS {
        readonly kind: typeof Kind.Intersection;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: JavaType;
    }

    export interface Void extends JS {
        readonly kind: typeof Kind.Void;
        readonly expression: Expression;
    }

    export interface Unary extends JS {
        readonly kind: typeof Kind.Unary;
        readonly operator: J.LeftPadded<Unary.Type>;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export namespace Unary {
        export const enum Type {
            Spread,
            Optional,
            Exclamation,
            QuestionDot,
            QuestionDotWithDot,
            Asterisk,
        }
    }

    export interface Yield extends JS {
        readonly kind: typeof Kind.Yield;
        readonly delegated: J.LeftPadded<boolean>;
        readonly expression?: Expression;
        readonly type?: JavaType;
    }

    export interface WithStatement extends JS, Statement {
        readonly kind: typeof Kind.WithStatement;
        readonly expression: J.ControlParentheses<Expression>;
        readonly body: J.RightPadded<Statement>;
    }

    export interface IndexSignatureDeclaration extends JS, Statement {
        readonly kind: typeof Kind.IndexSignatureDeclaration;
        readonly modifiers: J.Modifier[];
        readonly parameters: J.Container<J>;
        readonly typeExpression: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface JSMethodDeclaration extends JS, Statement {
        readonly kind: typeof Kind.JSMethodDeclaration;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeParameters?: J.TypeParameters;
        readonly returnTypeExpression?: TypeTree;
        readonly name: Expression;
        readonly parameters: J.Container<Statement>;
        readonly body?: J.Block;
        readonly defaultValue?: J.LeftPadded<Expression>;
        readonly methodType?: JavaType.Method;
    }

    export interface JSForOfLoop extends JS {
        readonly kind: typeof Kind.JSForOfLoop;
        readonly await: J.LeftPadded<boolean>;
        readonly control: JSForInOfLoopControl;
        readonly body: J.RightPadded<Statement>;
    }

    export function newObjectBindingDeclarations(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        leadingAnnotations: J.Annotation[],
        modifiers: J.Modifier[],
        typeExpression: TypeTree | undefined,
        bindings: J.Container<J>,
        initializer: J.LeftPadded<Expression> | undefined
    ): JS.ObjectBindingDeclarations {
        return {
            kind: JS.Kind.ObjectBindingDeclarations,
            id,
            prefix,
            markers,
            leadingAnnotations,
            modifiers,
            typeExpression,
            bindings,
            initializer
        };
    }

    export function newPropertyAssignment(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        name: J.RightPadded<Expression>,
        assigmentToken: PropertyAssignment.Token,
        initializer: Expression | undefined
    ): JS.PropertyAssignment {
        return {
            kind: JS.Kind.PropertyAssignment,
            id,
            prefix,
            markers,
            name,
            assigmentToken,
            initializer
        };
    }

    export function newSatisfiesExpression(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: J,
        satisfiesType: J.LeftPadded<Expression>,
        type: JavaType | undefined
    ): JS.SatisfiesExpression {
        return {
            kind: JS.Kind.SatisfiesExpression,
            id,
            prefix,
            markers,
            expression,
            satisfiesType,
            type
        };
    }

    export function newScopedVariableDeclarations(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        scope: J.LeftPadded<ScopedVariableDeclarations.Scope> | undefined,
        variables: J.RightPadded<J>[]
    ): JS.ScopedVariableDeclarations {
        return {
            kind: JS.Kind.ScopedVariableDeclarations,
            id,
            prefix,
            markers,
            modifiers,
            scope,
            variables
        };
    }

    export function newStatementExpression(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        statement: Statement
    ): JS.StatementExpression {
        return {
            kind: JS.Kind.StatementExpression,
            id,
            prefix,
            markers,
            statement
        };
    }

    export function newTaggedTemplateExpression(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        tag: J.RightPadded<Expression> | undefined,
        typeArguments: J.Container<Expression> | undefined,
        templateExpression: Expression,
        type: JavaType | undefined
    ): JS.TaggedTemplateExpression {
        return {
            kind: JS.Kind.TaggedTemplateExpression,
            id,
            prefix,
            markers,
            tag,
            typeArguments,
            templateExpression,
            type
        };
    }

    export function newTemplateExpression(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        head: J.Literal,
        templateSpans: J.RightPadded<TemplateExpression.TemplateSpan>[],
        type: JavaType | undefined
    ): JS.TemplateExpression {
        return {
            kind: JS.Kind.TemplateExpression,
            id,
            prefix,
            markers,
            head,
            templateSpans,
            type
        };
    }

    export function newTemplateExpressionTemplateSpan(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: J,
        tail: J.Literal
    ): JS.TemplateExpression.TemplateSpan {
        return {
            kind: JS.Kind.TemplateExpressionTemplateSpan,
            id,
            prefix,
            markers,
            expression,
            tail
        };
    }

    export function newTrailingTokenStatement(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: J.RightPadded<J>,
        type: JavaType | undefined
    ): JS.TrailingTokenStatement {
        return {
            kind: JS.Kind.TrailingTokenStatement,
            id,
            prefix,
            markers,
            expression,
            type
        };
    }

    export function newTuple(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        elements: J.Container<J>,
        type: JavaType | undefined
    ): JS.Tuple {
        return {
            kind: JS.Kind.Tuple,
            id,
            prefix,
            markers,
            elements,
            type
        };
    }

    export function newTypeDeclaration(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        name: J.LeftPadded<J.Identifier>,
        typeParameters: J.TypeParameters | undefined,
        initializer: J.LeftPadded<Expression>,
        type?: JavaType
    ): JS.TypeDeclaration {
        return {
            kind: JS.Kind.TypeDeclaration,
            id,
            prefix,
            markers,
            modifiers,
            name,
            typeParameters,
            initializer,
            type
        };
    }

    export function newTypeInfo(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeIdentifier: TypeTree
    ): JS.TypeInfo {
        return {
            kind: JS.Kind.TypeInfo,
            id,
            prefix,
            markers,
            typeIdentifier
        };
    }

    export function newTypeOf(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression,
        type?: JavaType
    ): JS.TypeOf {
        return {
            kind: JS.Kind.TypeOf,
            id,
            prefix,
            markers,
            expression,
            type
        };
    }

    export function newTypeOperator(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        operator: JS.TypeOperator.Type,
        expression: J.LeftPadded<Expression>
    ): JS.TypeOperator {
        return {
            kind: JS.Kind.TypeOperator,
            id,
            prefix,
            markers,
            operator,
            expression
        };
    }

    export function newTypePredicate(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        asserts: J.LeftPadded<boolean>,
        parameterName: J.Identifier,
        expression?: J.LeftPadded<Expression>,
        type?: JavaType
    ): JS.TypePredicate {
        return {
            kind: JS.Kind.TypePredicate,
            id,
            prefix,
            markers,
            asserts,
            parameterName,
            expression,
            type
        };
    }

    export function newTypeQuery(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeExpression: TypeTree,
        typeArguments?: J.Container<Expression>,
        type?: JavaType
    ): JS.TypeQuery {
        return {
            kind: JS.Kind.TypeQuery,
            id,
            prefix,
            markers,
            typeExpression,
            typeArguments,
            type
        };
    }

    export function newTypeTreeExpression(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression
    ): JS.TypeTreeExpression {
        return {
            kind: JS.Kind.TypeTreeExpression,
            id,
            prefix,
            markers,
            expression
        };
    }

    export function newUnion(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        types: J.RightPadded<Expression>[],
        type?: JavaType
    ): JS.Union {
        return {
            kind: JS.Kind.Union,
            id,
            prefix,
            markers,
            types,
            type
        };
    }

    export function newIntersection(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        types: J.RightPadded<Expression>[],
        type?: JavaType
    ): JS.Intersection {
        return {
            kind: JS.Kind.Intersection,
            id,
            prefix,
            markers,
            types,
            type
        };
    }

    export function newVoid(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression
    ): JS.Void {
        return {
            kind: JS.Kind.Void,
            id,
            prefix,
            markers,
            expression
        };
    }

    export function newUnary(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        operator: J.LeftPadded<JS.Unary.Type>,
        expression: Expression,
        type?: JavaType
    ): JS.Unary {
        return {
            kind: JS.Kind.Unary,
            id,
            prefix,
            markers,
            operator,
            expression,
            type
        };
    }

    export function newJSForOfLoop(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        await_: J.LeftPadded<boolean>,
        control: JSForInOfLoopControl,
        body: J.RightPadded<Statement>
    ): JS.JSForOfLoop {
        return {
            kind: JS.Kind.JSForOfLoop,
            id,
            prefix,
            markers,
            await: await_,
            control,
            body
        };
    }

    export function newJSTry(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        body: J.Block,
        catches: JSTry.JSCatch,
        finally_?: J.LeftPadded<J.Block>
    ): JS.JSTry {
        return {
            kind: JS.Kind.JSTry,
            id,
            prefix,
            markers,
            body,
            catches,
            finally: finally_
        };
    }

    export function newJSTryJSCatch(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        parameter: J.ControlParentheses<JSVariableDeclarations>,
        body: J.Block
    ): JS.JSTry.JSCatch {
        return {
            kind: JS.Kind.JSCatch,
            id,
            prefix,
            markers,
            parameter,
            body
        };
    }

    export function newNamespaceDeclaration(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        keywordType: J.LeftPadded<NamespaceDeclaration.KeywordType>,
        name: J.RightPadded<Expression>,
        body?: J.Block
    ): JS.NamespaceDeclaration {
        return {
            kind: JS.Kind.NamespaceDeclaration,
            id,
            prefix,
            markers,
            modifiers,
            keywordType,
            name,
            body
        };
    }

    export function newFunctionDeclaration(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        asteriskToken: J.LeftPadded<boolean>,
        name: J.LeftPadded<J.Identifier>,
        typeParameters: J.TypeParameters | undefined,
        parameters: J.Container<Statement>,
        returnTypeExpression?: TypeTree,
        body?: J,
        type?: JavaType
    ): JS.FunctionDeclaration {
        return {
            kind: JS.Kind.FunctionDeclaration,
            id,
            prefix,
            markers,
            modifiers,
            asteriskToken,
            name,
            typeParameters,
            parameters,
            returnTypeExpression,
            body,
            type
        };
    }

    export function newTypeLiteral(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        members: J.Block,
        type?: JavaType
    ): JS.TypeLiteral {
        return {
            kind: JS.Kind.TypeLiteral,
            id,
            prefix,
            markers,
            members,
            type
        };
    }

    export function newArrayBindingPattern(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        elements: J.Container<Expression>,
        type?: JavaType
    ): JS.ArrayBindingPattern {
        return {
            kind: JS.Kind.ArrayBindingPattern,
            id,
            prefix,
            markers,
            elements,
            type
        };
    }

    export function newBindingElement(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        propertyName: J.RightPadded<Expression> | undefined,
        name: TypedTree,
        initializer?: J.LeftPadded<Expression>,
        variableType?: JavaType.Variable
    ): JS.BindingElement {
        return {
            kind: JS.Kind.BindingElement,
            id,
            prefix,
            markers,
            propertyName,
            name,
            initializer,
            variableType
        };
    }

    export function newExportDeclaration(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        typeOnly: J.LeftPadded<boolean>,
        exportClause?: Expression,
        moduleSpecifier?: J.LeftPadded<Expression>,
        attributes?: ImportAttributes
    ): JS.ExportDeclaration {
        return {
            kind: JS.Kind.ExportDeclaration,
            id,
            prefix,
            markers,
            modifiers,
            typeOnly,
            exportClause,
            moduleSpecifier,
            attributes
        };
    }

    export function newExportAssignment(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        exportEquals: J.LeftPadded<boolean>,
        expression: Expression | undefined
    ): JS.ExportAssignment {
        return {
            kind: JS.Kind.ExportAssignment,
            id,
            prefix,
            markers,
            modifiers,
            exportEquals,
            expression
        };
    }

    export function newNamedExports(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        elements: J.Container<Expression>,
        type: JavaType | undefined
    ): JS.NamedExports {
        return {
            kind: JS.Kind.NamedExports,
            id,
            prefix,
            markers,
            elements,
            type
        };
    }

    export function newExportSpecifier(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeOnly: J.LeftPadded<boolean>,
        specifier: Expression,
        type: JavaType | undefined
    ): JS.ExportSpecifier {
        return {
            kind: JS.Kind.ExportSpecifier,
            id,
            prefix,
            markers,
            typeOnly,
            specifier,
            type
        };
    }

    export function newCompilationUnit(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        sourcePath: string,
        charsetBomMarked: boolean,
        charsetName: string,
        checksum: Checksum | undefined,
        fileAttributes: FileAttributes | undefined,
        imports: J.RightPadded<J.Import>[],
        statements: J.RightPadded<Statement>[],
        eof: J.Space
    ): JS.CompilationUnit {
        return {
            kind: JS.Kind.CompilationUnit,
            id,
            prefix,
            markers,
            sourcePath: sourcePath,
            charsetBomMarked: charsetBomMarked,
            charsetName: charsetName,
            checksum: checksum,
            fileAttributes: fileAttributes,
            imports: imports,
            statements: statements,
            eof
        };
    }

    export function newAlias(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        propertyName: J.RightPadded<J.Identifier>,
        alias: Expression
    ): JS.Alias {
        return {
            kind: JS.Kind.Alias,
            id,
            prefix,
            markers,
            propertyName,
            alias
        };
    }

    export function newArrowFunction(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        leadingAnnotations: J.Annotation[],
        modifiers: J.Modifier[],
        typeParameters: J.TypeParameters | undefined,
        parameters: J.Lambda.Parameters,
        returnTypeExpression: TypeTree | undefined,
        body: J.LeftPadded<J>,
        type: JavaType | undefined
    ): JS.ArrowFunction {
        return {
            kind: JS.Kind.ArrowFunction,
            id,
            prefix,
            markers,
            leadingAnnotations,
            modifiers,
            typeParameters,
            parameters,
            returnTypeExpression,
            body,
            type
        };
    }

    export function newAwait(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression,
        type: JavaType | undefined
    ): JS.Await {
        return {
            kind: JS.Kind.Await,
            id,
            prefix,
            markers,
            expression,
            type
        };
    }

    export function newConditionalType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        checkType: Expression,
        condition: J.Container<TypedTree>,
        type: JavaType | undefined
    ): JS.ConditionalType {
        return {
            kind: JS.Kind.ConditionalType,
            id,
            prefix,
            markers,
            checkType,
            condition,
            type
        };
    }

    export function newDefaultType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        left: Expression,
        beforeEquals: J.Space,
        right: Expression,
        type: JavaType | undefined
    ): JS.DefaultType {
        return {
            kind: JS.Kind.DefaultType,
            id,
            prefix,
            markers,
            left,
            beforeEquals,
            right,
            type
        };
    }

    export function newDelete(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression,
        type?: JavaType
    ): JS.Delete {
        return {
            kind: JS.Kind.Delete,
            id,
            prefix,
            markers,
            expression,
            type
        };
    }

    export function newExport(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        exports?: J.Container<Expression>,
        from?: J.Space,
        target?: J.Literal,
        initializer?: J.LeftPadded<Expression>
    ): JS.Export {
        return {
            kind: JS.Kind.Export,
            id,
            prefix,
            markers,
            exports,
            from,
            target,
            initializer
        };
    }

    export function newExpressionStatement(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        expression: Expression
    ): JS.ExpressionStatement {
        return {
            kind: JS.Kind.ExpressionStatement,
            id,
            prefix,
            markers,
            expression
        };
    }

    export function newExpressionWithTypeArguments(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        clazz: J,
        typeArguments?: J.Container<Expression>,
        type?: JavaType
    ): JS.ExpressionWithTypeArguments {
        return {
            kind: JS.Kind.ExpressionWithTypeArguments,
            id,
            prefix,
            markers,
            clazz,
            typeArguments,
            type
        };
    }

    export function newFunctionType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        constructorType: J.LeftPadded<boolean>,
        typeParameters: J.TypeParameters | undefined,
        parameters: J.Container<Statement>,
        returnType: J.LeftPadded<Expression>,
        type?: JavaType
    ): JS.FunctionType {
        return {
            kind: JS.Kind.FunctionType,
            id,
            prefix,
            markers,
            modifiers,
            constructorType,
            typeParameters,
            parameters,
            returnType,
            type
        };
    }

    export function newInferType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeParameter: J.LeftPadded<J>,
        type?: JavaType
    ): JS.InferType {
        return {
            kind: JS.Kind.InferType,
            id,
            prefix,
            markers,
            typeParameter,
            type
        };
    }

    export function newImportType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        hasTypeof: J.RightPadded<boolean>,
        argumentAndAttributes: J.Container<J>,
        qualifier?: J.LeftPadded<Expression>,
        typeArguments?: J.Container<Expression>,
        type?: JavaType
    ): JS.ImportType {
        return {
            kind: JS.Kind.ImportType,
            id,
            prefix,
            markers,
            hasTypeof,
            argumentAndAttributes,
            qualifier,
            typeArguments,
            type
        };
    }

    export function newJsImport(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        modifiers: J.Modifier[],
        importClause: JS.JsImportClause | undefined,
        moduleSpecifier: J.LeftPadded<Expression>,
        attributes?: ImportAttributes
    ): JS.JsImport {
        return {
            kind: JS.Kind.JsImport,
            id,
            prefix,
            markers,
            modifiers,
            importClause,
            moduleSpecifier,
            attributes
        };
    }

    export function newJSIndexedAccessTypeIndexType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        element: J.RightPadded<TypeTree>,
        type?: JavaType
    ): JS.IndexedAccessType.IndexType {
        return {
            kind: JS.Kind.IndexedAccessTypeIndexType,
            id,
            prefix,
            markers,
            element,
            type
        };
    }

    export function newMappedType(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        prefixToken: J.LeftPadded<J.Literal> | undefined,
        hasReadonly: J.LeftPadded<boolean>,
        keysRemapping: JS.MappedType.KeysRemapping,
        suffixToken: J.LeftPadded<J.Literal> | undefined,
        hasQuestionToken: J.LeftPadded<boolean>,
        valueType: J.Container<TypeTree>,
        type?: JavaType
    ): JS.MappedType {
        return {
            kind: JS.Kind.MappedType,
            id,
            prefix,
            markers,
            prefixToken,
            hasReadonly,
            keysRemapping,
            suffixToken,
            hasQuestionToken,
            valueType,
            type
        };
    }

    export function newMappedTypeKeysRemapping(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeParameter: J.RightPadded<JS.MappedType.MappedTypeParameter>,
        nameType?: J.RightPadded<Expression>
    ): JS.MappedType.KeysRemapping {
        return {
            kind: JS.Kind.MappedTypeKeysRemapping,
            id,
            prefix,
            markers,
            typeParameter,
            nameType
        };
    }

    export function newJsImportClause(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        typeOnly: boolean,
        name?: J.RightPadded<J.Identifier>,
        namedBindings?: Expression
    ): JS.JsImportClause {
        return {
            kind: JS.Kind.JsImportClause,
            id,
            prefix,
            markers,
            typeOnly,
            name,
            namedBindings
        };
    }

    export function newNamedImports(
        id: UUID,
        prefix: J.Space,
        markers: Markers,
        elements: J.Container<Expression>,
        type?: JavaType
    ): JS.NamedImports {
        return {
            kind: JS.Kind.NamedImports,
            id,
            prefix,
            markers,
            elements,
            type
        };
    }

    export interface JSForInLoop extends JS {
        readonly kind: typeof Kind.JSForInLoop;
        readonly control: JSForInOfLoopControl;
        readonly body: J.RightPadded<Statement>;
    }

    export interface JSForInOfLoopControl extends JS {
        readonly kind: typeof Kind.JSForInOfLoopControl;
        readonly variable: J.RightPadded<J>;
        readonly iterable: J.RightPadded<Expression>;
    }

    export interface JSTry extends JS, Statement {
        readonly kind: typeof Kind.JSTry;
        readonly body: J.Block;
        readonly catches: JSTry.JSCatch;
        readonly finally?: J.LeftPadded<J.Block>;
    }

    export namespace JSTry {
        export interface JSCatch extends JS {
            readonly kind: typeof Kind.JSCatch;
            readonly parameter: J.ControlParentheses<JSVariableDeclarations>;
            readonly body: J.Block;
        }
    }

    export interface NamespaceDeclaration extends JS, Statement {
        readonly kind: typeof Kind.NamespaceDeclaration;
        readonly modifiers: J.Modifier[];
        readonly keywordType: J.LeftPadded<NamespaceDeclaration.KeywordType>;
        readonly name: J.RightPadded<Expression>;
        readonly body?: J.Block;
    }

    export namespace NamespaceDeclaration {
        export enum KeywordType {
            Namespace,
            Module,
            Empty,
        }
    }

    export interface FunctionDeclaration extends JS, Statement {
        readonly kind: typeof Kind.FunctionDeclaration;
        readonly modifiers: J.Modifier[];
        readonly asteriskToken: J.LeftPadded<boolean>;
        readonly name: J.LeftPadded<J.Identifier>;
        readonly typeParameters?: J.TypeParameters;
        readonly parameters: J.Container<Statement>;
        readonly returnTypeExpression?: TypeTree;
        readonly body?: J;
        readonly type?: JavaType;
    }

    export interface TypeLiteral extends JS {
        readonly kind: typeof Kind.TypeLiteral;
        readonly members: J.Block;
        readonly type?: JavaType;
    }

    export interface ArrayBindingPattern extends JS {
        readonly kind: typeof Kind.ArrayBindingPattern;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface BindingElement extends JS, Statement {
        readonly kind: typeof Kind.BindingElement;
        readonly propertyName?: J.RightPadded<Expression>;
        readonly name: TypedTree;
        readonly initializer?: J.LeftPadded<Expression>;
        readonly variableType?: JavaType.Variable;
    }

    export interface ExportDeclaration extends JS, Statement {
        readonly kind: typeof Kind.ExportDeclaration;
        readonly modifiers: J.Modifier[];
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly exportClause?: Expression;
        readonly moduleSpecifier?: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
    }

    export interface ExportAssignment extends JS, Statement {
        readonly kind: typeof Kind.ExportAssignment;
        readonly modifiers: J.Modifier[];
        readonly exportEquals: J.LeftPadded<boolean>;
        readonly expression?: Expression;
    }

    export interface NamedExports extends JS {
        readonly kind: typeof Kind.NamedExports;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface ExportSpecifier extends JS {
        readonly kind: typeof Kind.ExportSpecifier;
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: JavaType;
    }
}

const javaScriptKindValues = new Set(Object.values(JS.Kind));

export function isJavaScript(tree: any): tree is JS {
    return javaScriptKindValues.has(tree["kind"]);
}
