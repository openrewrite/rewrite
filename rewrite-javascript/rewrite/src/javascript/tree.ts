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

import {SourceFile, TreeKind} from "../";
import {
    J,
    JavaType,
    Statement,
} from "../java";

export const JavaScriptKind = {
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

const javaScriptKindValues = new Set(Object.values(JavaScriptKind));

export function isJavaScript(tree: any): tree is JS {
    return javaScriptKindValues.has(tree["kind"]);
}

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
    export interface JavaScriptSourceFile extends JS, SourceFile {
        readonly imports: J.RightPadded<J.Import>[];
        readonly statements: J.RightPadded<Statement>[];
    }

    export interface CompilationUnit extends JavaScriptSourceFile, JS {
        readonly kind: typeof JavaScriptKind.CompilationUnit;
        readonly eof: J.Space;
    }

    export interface Alias extends JS {
        readonly kind: typeof JavaScriptKind.Alias;
        readonly propertyName: J.RightPadded<J.Identifier>;
        readonly alias: Expression;
    }

    export interface ArrowFunction extends JS, Statement {
        readonly kind: typeof JavaScriptKind.ArrowFunction;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeParameters?: J.TypeParameters;
        readonly parameters: J.LambdaParameters;
        readonly returnTypeExpression?: TypeTree;
        readonly body: J.LeftPadded<J>;
        readonly type?: JavaType;
    }

    export interface Await extends JS {
        readonly kind: typeof JavaScriptKind.Await;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export interface ConditionalType extends JS {
        readonly kind: typeof JavaScriptKind.ConditionalType;
        readonly checkType: Expression;
        readonly condition: J.Container<TypedTree>;
        readonly type?: JavaType;
    }

    export interface DefaultType extends JS {
        readonly kind: typeof JavaScriptKind.DefaultType;
        readonly left: Expression;
        readonly beforeEquals: J.Space;
        readonly right: Expression;
        readonly type?: JavaType;
    }

    export interface Delete extends JS, Statement {
        readonly kind: typeof JavaScriptKind.Delete;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export interface Export extends JS, Statement {
        readonly kind: typeof JavaScriptKind.Export;
        readonly exports?: J.Container<Expression>;
        readonly from?: J.Space;
        readonly target?: J.Literal;
        readonly initializer?: J.LeftPadded<Expression>;
    }

    export interface ExpressionStatement extends JS, Statement {
        readonly kind: typeof JavaScriptKind.ExpressionStatement;
        readonly expression: Expression;
    }

    export interface ExpressionWithTypeArguments extends JS {
        readonly kind: typeof JavaScriptKind.ExpressionWithTypeArguments;
        readonly clazz: J;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface FunctionType extends JS {
        readonly kind: typeof JavaScriptKind.FunctionType;
        readonly modifiers: J.Modifier[];
        readonly constructorType: J.LeftPadded<boolean>;
        readonly typeParameters?: J.TypeParameters;
        readonly parameters: J.Container<Statement>;
        readonly returnType: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface InferType extends JS {
        readonly kind: typeof JavaScriptKind.InferType;
        readonly typeParameter: J.LeftPadded<J>;
        readonly type?: JavaType;
    }

    export interface ImportType extends JS {
        readonly kind: typeof JavaScriptKind.ImportType;
        readonly hasTypeof: J.RightPadded<boolean>;
        readonly argumentAndAttributes: J.Container<J>;
        readonly qualifier?: J.LeftPadded<Expression>;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface JsImport extends JS, Statement {
        readonly kind: typeof JavaScriptKind.JsImport;
        readonly modifiers: J.Modifier[];
        readonly importClause?: JsImportClause;
        readonly moduleSpecifier: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
    }

    export interface JsImportClause extends JS {
        readonly kind: typeof JavaScriptKind.JsImportClause;
        readonly typeOnly: boolean;
        readonly name?: J.RightPadded<J.Identifier>;
        readonly namedBindings?: Expression;
    }

    export interface NamedImports extends JS {
        readonly kind: typeof JavaScriptKind.NamedImports;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface JsImportSpecifier extends JS {
        readonly kind: typeof JavaScriptKind.JsImportSpecifier;
        readonly importType: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: JavaType;
    }

    export interface JSVariableDeclarations extends JS, Statement {
        readonly kind: typeof JavaScriptKind.JSVariableDeclarations;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeExpression?: TypeTree;
        readonly varargs?: J.Space;
        readonly variables: J.RightPadded<JSVariableDeclarations.JSNamedVariable>[];
    }

    export namespace JSVariableDeclarations {
        export interface JSNamedVariable extends JS {
            readonly kind: typeof JavaScriptKind.JSNamedVariable;
            readonly name: Expression;
            readonly dimensionsAfterName: J.LeftPadded<J.Space>[];
            readonly initializer?: J.LeftPadded<Expression>;
            readonly variableType?: JavaType.Variable;
        }
    }

    export interface ImportAttributes extends JS {
        readonly kind: typeof JavaScriptKind.ImportAttributes;
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
        readonly kind: typeof JavaScriptKind.ImportTypeAttributes;
        readonly token: J.RightPadded<Expression>;
        readonly elements: J.Container<ImportAttribute>;
        readonly end: J.Space;
    }

    export interface ImportAttribute extends JS, Statement {
        readonly kind: typeof JavaScriptKind.ImportAttribute;
        readonly name: Expression;
        readonly value: J.LeftPadded<Expression>;
    }

    export interface JsBinary extends JS {
        readonly kind: typeof JavaScriptKind.JsBinary;
        readonly left: Expression;
        readonly operator: J.LeftPadded<JsBinary.Operator>;
        readonly right: Expression;
        readonly type?: JavaType;
    }

    export namespace JsBinary {
        export const enum Operator {
            As,
            IdentityEquals,
            IdentityNotEquals,
            In,
            QuestionQuestion,
            Comma
        }
    }

    export interface LiteralType extends JS {
        readonly kind: typeof JavaScriptKind.LiteralType;
        readonly literal: Expression;
        readonly type: JavaType;
    }

    export interface MappedType extends JS {
        readonly kind: typeof JavaScriptKind.MappedType;
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
            readonly kind: typeof JavaScriptKind.MappedTypeKeysRemapping;
            readonly typeParameter: J.RightPadded<MappedType.MappedTypeParameter>;
            readonly nameType?: J.RightPadded<Expression>;
        }

        export interface MappedTypeParameter extends JS, Statement {
            readonly kind: typeof JavaScriptKind.MappedTypeMappedTypeParameter;
            readonly name: Expression;
            readonly iterateType: J.LeftPadded<TypeTree>;
        }
    }

    export interface ObjectBindingDeclarations extends JS {
        readonly kind: typeof JavaScriptKind.ObjectBindingDeclarations;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeExpression?: TypeTree;
        readonly bindings: J.Container<J>;
        readonly initializer?: J.LeftPadded<Expression>;
    }

    export interface PropertyAssignment extends JS, Statement {
        readonly kind: typeof JavaScriptKind.PropertyAssignment;
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
        readonly kind: typeof JavaScriptKind.SatisfiesExpression;
        readonly expression: J;
        readonly satisfiesType: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface ScopedVariableDeclarations extends JS, Statement {
        readonly kind: typeof JavaScriptKind.ScopedVariableDeclarations;
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
        readonly kind: typeof JavaScriptKind.StatementExpression;
        readonly statement: Statement;
    }

    export interface TaggedTemplateExpression extends JS, Statement {
        readonly kind: typeof JavaScriptKind.TaggedTemplateExpression;
        readonly tag?: J.RightPadded<Expression>;
        readonly typeArguments?: J.Container<Expression>;
        readonly templateExpression: Expression;
        readonly type?: JavaType;
    }

    export interface TemplateExpression extends JS, Statement {
        readonly kind: typeof JavaScriptKind.TemplateExpression;
        readonly head: J.Literal;
        readonly templateSpans: J.RightPadded<TemplateExpression.TemplateSpan>[];
        readonly type?: JavaType;
    }

    export namespace TemplateExpression {
        export interface TemplateSpan extends JS {
            readonly kind: typeof JavaScriptKind.TemplateExpressionTemplateSpan;
            readonly expression: J;
            readonly tail: J.Literal;
        }
    }

    export interface TrailingTokenStatement extends JS, Statement {
        readonly kind: typeof JavaScriptKind.TrailingTokenStatement;
        readonly expression: J.RightPadded<J>;
        readonly type?: JavaType;
    }

    export interface Tuple extends JS {
        readonly kind: typeof JavaScriptKind.Tuple;
        readonly elements: J.Container<J>;
        readonly type?: JavaType;
    }

    export interface TypeDeclaration extends JS, Statement {
        readonly kind: typeof JavaScriptKind.TypeDeclaration;
        readonly modifiers: J.Modifier[];
        readonly name: J.LeftPadded<J.Identifier>;
        readonly typeParameters?: J.TypeParameters;
        readonly initializer: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface TypeOf extends JS {
        readonly kind: typeof JavaScriptKind.TypeOf;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    export interface TypeTreeExpression extends JS {
        readonly kind: typeof JavaScriptKind.TypeTreeExpression;
        readonly expression: Expression;
    }

    export interface JsAssignmentOperation extends JS, Statement {
        readonly kind: typeof JavaScriptKind.JsAssignmentOperation;
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
        readonly kind: typeof JavaScriptKind.IndexedAccessType;
        readonly objectType: TypeTree;
        readonly indexType: TypeTree;
        readonly type?: JavaType;
    }

    export namespace IndexedAccessType {
        export interface IndexType extends JS {
            readonly kind: typeof JavaScriptKind.IndexedAccessTypeIndexType;
            readonly element: J.RightPadded<TypeTree>;
            readonly type?: JavaType;
        }
    }

    export interface TypeQuery extends JS {
        readonly kind: typeof JavaScriptKind.TypeQuery;
        readonly typeExpression: TypeTree;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface TypeInfo extends JS {
        readonly kind: typeof JavaScriptKind.TypeInfo;
        readonly typeIdentifier: TypeTree;
    }

    export interface TypeOperator extends JS {
        readonly kind: typeof JavaScriptKind.TypeOperator;
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
        readonly kind: typeof JavaScriptKind.TypePredicate;
        readonly asserts: J.LeftPadded<boolean>;
        readonly parameterName: J.Identifier;
        readonly expression?: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface Union extends JS {
        readonly kind: typeof JavaScriptKind.Union;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: JavaType;
    }

    export interface Intersection extends JS {
        readonly kind: typeof JavaScriptKind.Intersection;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: JavaType;
    }

    export interface Void extends JS {
        readonly kind: typeof JavaScriptKind.Void;
        readonly expression: Expression;
    }

    export interface Unary extends JS {
        readonly kind: typeof JavaScriptKind.Unary;
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
        readonly kind: typeof JavaScriptKind.Yield;
        readonly delegated: J.LeftPadded<boolean>;
        readonly expression?: Expression;
        readonly type?: JavaType;
    }

    export interface WithStatement extends JS, Statement {
        readonly kind: typeof JavaScriptKind.WithStatement;
        readonly expression: J.ControlParentheses<Expression>;
        readonly body: J.RightPadded<Statement>;
    }

    export interface IndexSignatureDeclaration extends JS, Statement {
        readonly kind: typeof JavaScriptKind.IndexSignatureDeclaration;
        readonly modifiers: J.Modifier[];
        readonly parameters: J.Container<J>;
        readonly typeExpression: J.LeftPadded<Expression>;
        readonly type?: JavaType;
    }

    export interface JSMethodDeclaration extends JS, Statement {
        readonly kind: typeof JavaScriptKind.JSMethodDeclaration;
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
        readonly kind: typeof JavaScriptKind.JSForOfLoop;
        readonly await: J.LeftPadded<boolean>;
        readonly control: JSForInOfLoopControl;
        readonly body: J.RightPadded<Statement>;
    }

    export interface JSForInLoop extends JS {
        readonly kind: typeof JavaScriptKind.JSForInLoop;
        readonly control: JSForInOfLoopControl;
        readonly body: J.RightPadded<Statement>;
    }

    export interface JSForInOfLoopControl extends JS {
        readonly kind: typeof JavaScriptKind.JSForInOfLoopControl;
        readonly variable: J.RightPadded<J>;
        readonly iterable: J.RightPadded<Expression>;
    }

    export interface JSTry extends JS, Statement {
        readonly kind: typeof JavaScriptKind.JSTry;
        readonly body: J.Block;
        readonly catches: JSTry.JSCatch;
        readonly finally?: J.LeftPadded<J.Block>;
    }

    export namespace JSTry {
        export interface JSCatch extends JS {
            readonly kind: typeof JavaScriptKind.JSCatch;
            readonly parameter: J.ControlParentheses<JSVariableDeclarations>;
            readonly body: J.Block;
        }
    }

    export interface NamespaceDeclaration extends JS, Statement {
        readonly kind: typeof JavaScriptKind.NamespaceDeclaration;
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
        readonly kind: typeof JavaScriptKind.FunctionDeclaration;
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
        readonly kind: typeof JavaScriptKind.TypeLiteral;
        readonly members: J.Block;
        readonly type?: JavaType;
    }

    export interface ArrayBindingPattern extends JS {
        readonly kind: typeof JavaScriptKind.ArrayBindingPattern;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface BindingElement extends JS, Statement {
        readonly kind: typeof JavaScriptKind.BindingElement;
        readonly propertyName?: J.RightPadded<Expression>;
        readonly name: TypedTree;
        readonly initializer?: J.LeftPadded<Expression>;
        readonly variableType?: JavaType.Variable;
    }

    export interface ExportDeclaration extends JS, Statement {
        readonly kind: typeof JavaScriptKind.ExportDeclaration;
        readonly modifiers: J.Modifier[];
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly exportClause?: Expression;
        readonly moduleSpecifier?: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
    }

    export interface ExportAssignment extends JS, Statement {
        readonly kind: typeof JavaScriptKind.ExportAssignment;
        readonly modifiers: J.Modifier[];
        readonly exportEquals: J.LeftPadded<boolean>;
        readonly expression?: Expression;
    }

    export interface NamedExports extends JS {
        readonly kind: typeof JavaScriptKind.NamedExports;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    export interface ExportSpecifier extends JS {
        readonly kind: typeof JavaScriptKind.ExportSpecifier;
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: JavaType;
    }
}
