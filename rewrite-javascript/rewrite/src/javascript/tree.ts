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
    Annotation,
    ControlParentheses,
    Identifier,
    Import,
    J,
    JavaType,
    JContainer,
    JLeftPadded,
    JRightPadded,
    LambdaParameters,
    Literal,
    Modifier,
    NameTree,
    Space,
    Statement,
    TypedTree,
    TypeParameters,
    TypeTree
} from "../java";

export const JavaScriptKind = {
    ...TreeKind,
    Alias: "org.openrewrite.javascript.tree.JS$Alias",
    ArrowFunction: "org.openrewrite.javascript.tree.JS$ArrowFunction",
    Await: "org.openrewrite.javascript.tree.JS$Await",
    CompilationUnit: "org.openrewrite.javascript.tree.JS$CompilationUnit",
    ConditionalType: "org.openrewrite.javascript.tree.JS$ConditionalType",
    DefaultType: "org.openrewrite.javascript.tree.JS$DefaultType",
    Delete: "org.openrewrite.javascript.tree.JS$Delete",
    Export: "org.openrewrite.javascript.tree.JS$Export",
    ExpressionStatement: "org.openrewrite.javascript.tree.JS$ExpressionStatement",
    ExpressionWithTypeArguments: "org.openrewrite.javascript.tree.JS$ExpressionWithTypeArguments",
    FunctionType: "org.openrewrite.javascript.tree.JS$FunctionType",
    ImportAttribute: "org.openrewrite.javascript.tree.JS$ImportAttribute",
    ImportAttributes: "org.openrewrite.javascript.tree.JS$ImportAttributes",
    ImportType: "org.openrewrite.javascript.tree.JS$ImportType",
    ImportTypeAttributes: "org.openrewrite.javascript.tree.JS$ImportTypeAttributes",
    InferType: "org.openrewrite.javascript.tree.JS$InferType",
    JsImport: "org.openrewrite.javascript.tree.JS$JsImport",
    JsImportClause: "org.openrewrite.javascript.tree.JS$JsImportClause",
    JsImportSpecifier: "org.openrewrite.javascript.tree.JS$JsImportSpecifier",
    JsBinary: "org.openrewrite.javascript.tree.JS$JsBinary",
    LiteralType: "org.openrewrite.javascript.tree.JS$LiteralType",
    MappedType: "org.openrewrite.javascript.tree.JS$MappedType",
    MappedTypeKeysRemapping: "org.openrewrite.javascript.tree.JS$MappedType$KeysRemapping",
    MappedTypeMappedTypeParameter: "org.openrewrite.javascript.tree.JS$MappedType$MappedTypeParameter",
    NamedImports: "org.openrewrite.javascript.tree.JS$NamedImports",
    ObjectBindingDeclarations: "org.openrewrite.javascript.tree.JS$ObjectBindingDeclarations",
    PropertyAssignment: "org.openrewrite.javascript.tree.JS$PropertyAssignment",
    SatisfiesExpression: "org.openrewrite.javascript.tree.JS$SatisfiesExpression",
    ScopedVariableDeclarations: "org.openrewrite.javascript.tree.JS$ScopedVariableDeclarations",
    StatementExpression: "org.openrewrite.javascript.tree.JS$StatementExpression",
    TrailingTokenStatement: "org.openrewrite.javascript.tree.JS$TrailingTokenStatement",
    TaggedTemplateExpression: "org.openrewrite.javascript.tree.JS$TaggedTemplateExpression",
    TemplateExpression: "org.openrewrite.javascript.tree.JS$TemplateExpression",
    TemplateExpressionTemplateSpan: "org.openrewrite.javascript.tree.JS$TemplateExpression$TemplateSpan",
    Tuple: "org.openrewrite.javascript.tree.JS$Tuple",
    TypeDeclaration: "org.openrewrite.javascript.tree.JS$TypeDeclaration",
    TypeOf: "org.openrewrite.javascript.tree.JS$TypeOf",
    WithStatement: "org.openrewrite.javascript.tree.JS$WithStatement"
} as const;

const javaScriptKindValues = new Set(Object.values(JavaScriptKind));

export function isJavaScript(tree: any): tree is JS {
    return javaScriptKindValues.has(tree["kind"]);
}

export interface JS extends J {}

type JavaScriptExpressionBase = Alias | ArrowFunction | Await | ConditionalType | DefaultType | Delete |
    ExpressionStatement | TrailingTokenStatement | ExpressionWithTypeArguments | FunctionType | InferType |
    ImportType | NamedImports | JsImportSpecifier | JsBinary | LiteralType | MappedType | ObjectBindingDeclarations |
    SatisfiesExpression | StatementExpression | TaggedTemplateExpression | TemplateExpression | Tuple | TypeOf;

export type Expression<T extends { kind: string } = never> = import("../java/tree").Expression<JavaScriptExpressionBase | T>;

export interface JavaScriptSourceFile extends JS, SourceFile {
    readonly imports: JRightPadded<Import>[];
    readonly statements: JRightPadded<Statement>[];
}

export interface CompilationUnit extends JavaScriptSourceFile, JS {
    readonly kind: typeof JavaScriptKind.CompilationUnit;
    readonly eof: Space;
}

export interface Alias extends JS {
    readonly kind: typeof JavaScriptKind.Alias;
    readonly propertyName: JRightPadded<Identifier>;
    readonly alias: Expression;
    readonly type?: JavaType;
}

export interface ArrowFunction extends JS, Statement, TypedTree {
    readonly kind: typeof JavaScriptKind.ArrowFunction;
    readonly leadingAnnotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly typeParameters?: TypeParameters;
    readonly parameters: LambdaParameters;
    readonly returnTypeExpression?: TypeTree;
    readonly body: JLeftPadded<J>;
    readonly type?: JavaType;
}

export interface Await extends JS {
    readonly kind: typeof JavaScriptKind.Await;
    readonly expression: Expression;
    readonly type?: JavaType;
}

export interface ConditionalType extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.ConditionalType;
    readonly checkType: Expression;
    readonly condition: JContainer<TypedTree>;
    readonly type?: JavaType;
}

export interface DefaultType extends JS, TypedTree, NameTree {
    readonly kind: typeof JavaScriptKind.DefaultType;
    readonly left: Expression;
    readonly beforeEquals: Space;
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
    readonly exports?: JContainer<Expression>;
    readonly from?: Space;
    readonly target?: Literal;
    readonly initializer?: JLeftPadded<Expression>;
}

export interface ExpressionStatement extends JS, Statement {
    readonly kind: typeof JavaScriptKind.ExpressionStatement;
    readonly expression: Expression;
}

export interface TrailingTokenStatement extends JS, Statement {
    readonly kind: typeof JavaScriptKind.TrailingTokenStatement;
    readonly expression: JRightPadded<J>;
    readonly type?: JavaType;
}

export interface ExpressionWithTypeArguments extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.ExpressionWithTypeArguments;
    readonly clazz: J;
    readonly typeArguments?: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface FunctionType extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.FunctionType;
    readonly modifiers: Modifier[];
    readonly constructorType: JLeftPadded<boolean>;
    readonly typeParameters?: TypeParameters;
    readonly parameters: JContainer<Statement>;
    readonly returnType: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface InferType extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.InferType;
    readonly typeParameter: JLeftPadded<J>;
    readonly type?: JavaType;
}

export interface ImportType extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.ImportType;
    readonly hasTypeof: JRightPadded<boolean>;
    readonly argumentAndAttributes: JContainer<J>;
    readonly qualifier?: JLeftPadded<Expression>;
    readonly typeArguments?: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface JsImport extends JS, Statement {
    readonly kind: typeof JavaScriptKind.JsImport;
    readonly modifiers: Modifier[];
    readonly importClause?: JsImportClause;
    readonly moduleSpecifier: JLeftPadded<Expression>;
    readonly attributes?: ImportAttributes;
}

export interface JsImportClause extends JS {
    readonly kind: typeof JavaScriptKind.JsImportClause;
    readonly typeOnly: boolean;
    readonly name?: JRightPadded<Identifier>;
    readonly namedBindings?: Expression;
}

export interface NamedImports extends JS {
    readonly kind: typeof JavaScriptKind.NamedImports;
    readonly elements: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface JsImportSpecifier extends JS, TypedTree {
    readonly kind: typeof JavaScriptKind.JsImportSpecifier;
    readonly importType: JLeftPadded<boolean>;
    readonly specifier: Expression;
    readonly type?: JavaType;
}

export interface ImportAttributes extends JS {
    readonly kind: typeof JavaScriptKind.ImportAttributes;
    readonly token: ImportAttributesToken;
    readonly elements: JContainer<Statement>;
}

export const enum ImportAttributesToken {
    With,
    Assert
}

export interface ImportTypeAttributes extends JS {
    readonly kind: typeof JavaScriptKind.ImportTypeAttributes;
    readonly token: JRightPadded<Expression>;
    readonly elements: JContainer<ImportAttribute>;
    readonly end: Space;
}

export interface ImportAttribute extends JS, Statement {
    readonly kind: typeof JavaScriptKind.ImportAttribute;
    readonly name: Expression;
    readonly value: JLeftPadded<Expression>;
}

export interface JsBinary extends JS, TypedTree {
    readonly kind: typeof JavaScriptKind.JsBinary;
    readonly left: Expression;
    readonly operator: JLeftPadded<JsBinaryOperator>;
    readonly right: Expression;
    readonly type?: JavaType;
}

export const enum JsBinaryOperator {
    As,
    IdentityEquals,
    IdentityNotEquals,
    In,
    QuestionQuestion,
    Comma
}

export interface LiteralType extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.LiteralType;
    readonly literal: Expression;
    readonly type: JavaType;
}

export interface MappedType extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.MappedType;
    readonly prefixToken?: JLeftPadded<Literal>;
    readonly hasReadonly: JLeftPadded<boolean>;
    readonly keysRemapping: MappedTypeKeysRemapping;
    readonly suffixToken?: JLeftPadded<Literal>;
    readonly hasQuestionToken: JLeftPadded<boolean>;
    readonly valueType: JContainer<TypeTree>;
    readonly type?: JavaType;
}

export interface MappedTypeKeysRemapping extends JS, Statement {
    readonly kind: typeof JavaScriptKind.MappedTypeKeysRemapping;
    readonly typeParameter: JRightPadded<MappedTypeMappedTypeParameter>;
    readonly nameType?: JRightPadded<Expression>;
}

export interface MappedTypeMappedTypeParameter extends JS, Statement {
    readonly kind: typeof JavaScriptKind.MappedTypeMappedTypeParameter;
    readonly name: Expression;
    readonly iterateType: JLeftPadded<TypeTree>;
}

export interface ObjectBindingDeclarations extends JS, TypedTree {
    readonly kind: typeof JavaScriptKind.ObjectBindingDeclarations;
    readonly leadingAnnotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly typeExpression?: TypeTree;
    readonly bindings: JContainer<J>;
    readonly initializer?: JLeftPadded<Expression>;
}

export interface PropertyAssignment extends JS, Statement, TypedTree {
    readonly kind: typeof JavaScriptKind.PropertyAssignment;
    readonly name: JRightPadded<Expression>;
    readonly assigmentToken: PropertyAssignmentToken;
    readonly initializer?: Expression;
}

export const enum PropertyAssignmentToken {
    Colon,
    Equals,
    Empty
}

export interface SatisfiesExpression extends JS {
    readonly kind: typeof JavaScriptKind.SatisfiesExpression;
    readonly expression: J;
    readonly satisfiesType: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface ScopedVariableDeclarations extends JS, Statement {
    readonly kind: typeof JavaScriptKind.ScopedVariableDeclarations;
    readonly modifiers: Modifier[];
    readonly scope?: JLeftPadded<ScopedVariableDeclarationsScope>;
    readonly variables: JRightPadded<J>[];
}

export const enum ScopedVariableDeclarationsScope {
    Const,
    Let,
    Var,
    Using,
    Import
}

export interface StatementExpression extends JS, Statement {
    readonly kind: typeof JavaScriptKind.StatementExpression;
    readonly statement: Statement;
}

export interface TaggedTemplateExpression extends JS, Statement {
    readonly kind: typeof JavaScriptKind.TaggedTemplateExpression;
    readonly tag?: JRightPadded<Expression>;
    readonly typeArguments?: JContainer<Expression>;
    readonly templateExpression: Expression;
    readonly type?: JavaType;
}

export interface TemplateExpression extends JS, Statement, TypeTree {
    readonly kind: typeof JavaScriptKind.TemplateExpression;
    readonly head: Literal;
    readonly templateSpans: JRightPadded<TemplateExpressionTemplateSpan>[];
    readonly type?: JavaType;
}

export interface TemplateExpressionTemplateSpan extends JS {
    readonly kind: typeof JavaScriptKind.TemplateExpressionTemplateSpan;
    readonly expression: J;
    readonly tail: Literal;
}

export interface Tuple extends JS, TypeTree {
    readonly kind: typeof JavaScriptKind.Tuple;
    readonly elements: JContainer<J>;
    readonly type?: JavaType;
}

export interface TypeDeclaration extends JS, Statement, TypedTree {
    readonly kind: typeof JavaScriptKind.TypeDeclaration;
    readonly modifiers: Modifier[];
    readonly name: JLeftPadded<Identifier>;
    readonly typeParameters?: TypeParameters;
    readonly initializer: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface TypeOf extends JS {
    readonly kind: typeof JavaScriptKind.TypeOf;
    readonly expression: Expression;
    readonly type?: JavaType;
}

export interface WithStatement extends JS, Statement {
    readonly kind: typeof JavaScriptKind.WithStatement;
    readonly expression: ControlParentheses<Expression>;
    readonly body: JRightPadded<Statement>;
}
