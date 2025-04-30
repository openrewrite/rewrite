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
    Block,
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
    Space,
    Statement,
    TypeParameters
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

type JavaScriptExpressionBase = Alias | ArrowFunction | Await | ConditionalType | DefaultType | Delete |
    ExpressionStatement | TrailingTokenStatement | ExpressionWithTypeArguments | FunctionType | InferType |
    ImportType | IndexedAccessType | NamedImports | JsImportSpecifier | JsBinary | JsAssignmentOperation |
    LiteralType | MappedType | ObjectBindingDeclarations | SatisfiesExpression | StatementExpression |
    TaggedTemplateExpression | TemplateExpression | Tuple | TypeOf | TypeTreeExpression | TypeQuery |
    TypeInfo | TypeOperator | TypePredicate | Unary | Union | Intersection | Void | Yield;

export type Expression<T extends {
    kind: string
} = never> = import("../java/tree").Expression<JavaScriptExpressionBase | T>;

type JavaScriptTypedTreeBase = ArrowFunction | DefaultType | JsImportSpecifier | JsBinary | ObjectBindingDeclarations |
    PropertyAssignment | TypeDeclaration | Unary | JSVariableDeclarations | JSMethodDeclaration | FunctionDeclaration |
    IndexSignatureDeclaration | ArrayBindingPattern | JsAssignmentOperation;

export type TypedTree<T extends {
    kind: string
} = never> = import("../java/tree").TypedTree<JavaScriptTypedTreeBase | T>;

type JavaScriptTypeTreeBase = ConditionalType | ExpressionWithTypeArguments | FunctionType |
    InferType | ImportType | LiteralType | MappedType | TemplateExpression | Tuple |
    TypeQuery | TypeOperator | TypePredicate | Union | Intersection | TypeInfo | TypeLiteral |
    BindingElement | IndexedAccessType;

export type TypeTree<T extends {
    kind: string
} = never> = import("../java/tree").TypeTree<JavaScriptTypeTreeBase | T>;

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
}

export interface ArrowFunction extends JS, Statement {
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

export interface ConditionalType extends JS {
    readonly kind: typeof JavaScriptKind.ConditionalType;
    readonly checkType: Expression;
    readonly condition: JContainer<TypedTree>;
    readonly type?: JavaType;
}

export interface DefaultType extends JS {
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

export interface ExpressionWithTypeArguments extends JS {
    readonly kind: typeof JavaScriptKind.ExpressionWithTypeArguments;
    readonly clazz: J;
    readonly typeArguments?: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface FunctionType extends JS {
    readonly kind: typeof JavaScriptKind.FunctionType;
    readonly modifiers: Modifier[];
    readonly constructorType: JLeftPadded<boolean>;
    readonly typeParameters?: TypeParameters;
    readonly parameters: JContainer<Statement>;
    readonly returnType: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface InferType extends JS {
    readonly kind: typeof JavaScriptKind.InferType;
    readonly typeParameter: JLeftPadded<J>;
    readonly type?: JavaType;
}

export interface ImportType extends JS {
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

export interface JsImportSpecifier extends JS {
    readonly kind: typeof JavaScriptKind.JsImportSpecifier;
    readonly importType: JLeftPadded<boolean>;
    readonly specifier: Expression;
    readonly type?: JavaType;
}

export interface JSVariableDeclarations extends JS, Statement {
    readonly kind: typeof JavaScriptKind.JSVariableDeclarations;
    readonly leadingAnnotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly typeExpression?: TypeTree;
    readonly varargs?: Space;
    readonly variables: JRightPadded<JSVariableDeclarations.JSNamedVariable>[];
}

export namespace JSVariableDeclarations {
    export interface JSNamedVariable extends JS {
        readonly kind: typeof JavaScriptKind.JSNamedVariable;
        readonly name: Expression;
        readonly dimensionsAfterName: JLeftPadded<Space>[];
        readonly initializer?: JLeftPadded<Expression>;
        readonly variableType?: JavaType.Variable;
    }
}

export interface ImportAttributes extends JS {
    readonly kind: typeof JavaScriptKind.ImportAttributes;
    readonly token: ImportAttributes.Token;
    readonly elements: JContainer<Statement>;
}

export namespace ImportAttributes {
    export const enum Token {
        With,
        Assert
    }
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

export interface JsBinary extends JS {
    readonly kind: typeof JavaScriptKind.JsBinary;
    readonly left: Expression;
    readonly operator: JLeftPadded<JsBinary.Operator>;
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
    readonly prefixToken?: JLeftPadded<Literal>;
    readonly hasReadonly: JLeftPadded<boolean>;
    readonly keysRemapping: MappedType.KeysRemapping;
    readonly suffixToken?: JLeftPadded<Literal>;
    readonly hasQuestionToken: JLeftPadded<boolean>;
    readonly valueType: JContainer<TypeTree>;
    readonly type?: JavaType;
}

export namespace MappedType {
    export interface KeysRemapping extends JS, Statement {
        readonly kind: typeof JavaScriptKind.MappedTypeKeysRemapping;
        readonly typeParameter: JRightPadded<MappedType.MappedTypeParameter>;
        readonly nameType?: JRightPadded<Expression>;
    }

    export interface MappedTypeParameter extends JS, Statement {
        readonly kind: typeof JavaScriptKind.MappedTypeMappedTypeParameter;
        readonly name: Expression;
        readonly iterateType: JLeftPadded<TypeTree>;
    }
}

export interface ObjectBindingDeclarations extends JS {
    readonly kind: typeof JavaScriptKind.ObjectBindingDeclarations;
    readonly leadingAnnotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly typeExpression?: TypeTree;
    readonly bindings: JContainer<J>;
    readonly initializer?: JLeftPadded<Expression>;
}

export interface PropertyAssignment extends JS, Statement {
    readonly kind: typeof JavaScriptKind.PropertyAssignment;
    readonly name: JRightPadded<Expression>;
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
    readonly satisfiesType: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface ScopedVariableDeclarations extends JS, Statement {
    readonly kind: typeof JavaScriptKind.ScopedVariableDeclarations;
    readonly modifiers: Modifier[];
    readonly scope?: JLeftPadded<ScopedVariableDeclarations.Scope>;
    readonly variables: JRightPadded<J>[];
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
    readonly tag?: JRightPadded<Expression>;
    readonly typeArguments?: JContainer<Expression>;
    readonly templateExpression: Expression;
    readonly type?: JavaType;
}

export interface TemplateExpression extends JS, Statement {
    readonly kind: typeof JavaScriptKind.TemplateExpression;
    readonly head: Literal;
    readonly templateSpans: JRightPadded<TemplateExpression.TemplateSpan>[];
    readonly type?: JavaType;
}

export namespace TemplateExpression {
    export interface TemplateSpan extends JS {
        readonly kind: typeof JavaScriptKind.TemplateExpressionTemplateSpan;
        readonly expression: J;
        readonly tail: Literal;
    }
}

export interface TrailingTokenStatement extends JS, Statement {
    readonly kind: typeof JavaScriptKind.TrailingTokenStatement;
    readonly expression: JRightPadded<J>;
    readonly type?: JavaType;
}

export interface Tuple extends JS {
    readonly kind: typeof JavaScriptKind.Tuple;
    readonly elements: JContainer<J>;
    readonly type?: JavaType;
}

export interface TypeDeclaration extends JS, Statement {
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

export interface TypeTreeExpression extends JS {
    readonly kind: typeof JavaScriptKind.TypeTreeExpression;
    readonly expression: Expression;
}

export interface JsAssignmentOperation extends JS, Statement {
    readonly kind: typeof JavaScriptKind.JsAssignmentOperation;
    readonly variable: Expression;
    readonly operator: JLeftPadded<JsAssignmentOperation.Type>;
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
        readonly element: JRightPadded<TypeTree>;
        readonly type?: JavaType;
    }
}

export interface TypeQuery extends JS {
    readonly kind: typeof JavaScriptKind.TypeQuery;
    readonly typeExpression: TypeTree;
    readonly typeArguments?: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface TypeInfo extends JS {
    readonly kind: typeof JavaScriptKind.TypeInfo;
    readonly typeIdentifier: TypeTree;
}

export interface TypeOperator extends JS {
    readonly kind: typeof JavaScriptKind.TypeOperator;
    readonly operator: TypeOperator.Type;
    readonly expression: JLeftPadded<Expression>;
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
    readonly asserts: JLeftPadded<boolean>;
    readonly parameterName: Identifier;
    readonly expression?: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface Union extends JS {
    readonly kind: typeof JavaScriptKind.Union;
    readonly types: JRightPadded<Expression>[];
    readonly type?: JavaType;
}

export interface Intersection extends JS {
    readonly kind: typeof JavaScriptKind.Intersection;
    readonly types: JRightPadded<Expression>[];
    readonly type?: JavaType;
}

export interface Void extends JS {
    readonly kind: typeof JavaScriptKind.Void;
    readonly expression: Expression;
}

export interface Unary extends JS {
    readonly kind: typeof JavaScriptKind.Unary;
    readonly operator: JLeftPadded<Unary.Type>;
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
    readonly delegated: JLeftPadded<boolean>;
    readonly expression?: Expression;
    readonly type?: JavaType;
}

export interface WithStatement extends JS, Statement {
    readonly kind: typeof JavaScriptKind.WithStatement;
    readonly expression: ControlParentheses<Expression>;
    readonly body: JRightPadded<Statement>;
}

export interface IndexSignatureDeclaration extends JS, Statement {
    readonly kind: typeof JavaScriptKind.IndexSignatureDeclaration;
    readonly modifiers: Modifier[];
    readonly parameters: JContainer<J>;
    readonly typeExpression: JLeftPadded<Expression>;
    readonly type?: JavaType;
}

export interface JSMethodDeclaration extends JS, Statement {
    readonly kind: typeof JavaScriptKind.JSMethodDeclaration;
    readonly leadingAnnotations: Annotation[];
    readonly modifiers: Modifier[];
    readonly typeParameters?: TypeParameters;
    readonly returnTypeExpression?: TypeTree;
    readonly name: Expression;
    readonly parameters: JContainer<Statement>;
    readonly body?: Block;
    readonly defaultValue?: JLeftPadded<Expression>;
    readonly methodType?: JavaType.Method;
}

export interface JSForOfLoop extends JS {
    readonly kind: typeof JavaScriptKind.JSForOfLoop;
    readonly await: JLeftPadded<boolean>;
    readonly control: JSForInOfLoopControl;
    readonly body: JRightPadded<Statement>;
}

export interface JSForInLoop extends JS {
    readonly kind: typeof JavaScriptKind.JSForInLoop;
    readonly control: JSForInOfLoopControl;
    readonly body: JRightPadded<Statement>;
}

export interface JSForInOfLoopControl extends JS {
    readonly kind: typeof JavaScriptKind.JSForInOfLoopControl;
    readonly variable: JRightPadded<J>;
    readonly iterable: JRightPadded<Expression>;
}

export interface JSTry extends JS, Statement {
    readonly kind: typeof JavaScriptKind.JSTry;
    readonly body: Block;
    readonly catches: JSTry.JSCatch;
    readonly finally?: JLeftPadded<Block>;
}

export namespace JSTry {
    export interface JSCatch extends JS {
        readonly kind: typeof JavaScriptKind.JSCatch;
        readonly parameter: ControlParentheses<JSVariableDeclarations>;
        readonly body: Block;
    }
}

export interface NamespaceDeclaration extends JS, Statement {
    readonly kind: typeof JavaScriptKind.NamespaceDeclaration;
    readonly modifiers: Modifier[];
    readonly keywordType: JLeftPadded<NamespaceDeclaration.KeywordType>;
    readonly name: JRightPadded<Expression>;
    readonly body?: Block;
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
    readonly modifiers: Modifier[];
    readonly asteriskToken: JLeftPadded<boolean>;
    readonly name: JLeftPadded<Identifier>;
    readonly typeParameters?: TypeParameters;
    readonly parameters: JContainer<Statement>;
    readonly returnTypeExpression?: TypeTree;
    readonly body?: J;
    readonly type?: JavaType;
}

export interface TypeLiteral extends JS {
    readonly kind: typeof JavaScriptKind.TypeLiteral;
    readonly members: Block;
    readonly type?: JavaType;
}

export interface ArrayBindingPattern extends JS {
    readonly kind: typeof JavaScriptKind.ArrayBindingPattern;
    readonly elements: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface BindingElement extends JS, Statement {
    readonly kind: typeof JavaScriptKind.BindingElement;
    readonly propertyName?: JRightPadded<Expression>;
    readonly name: TypedTree;
    readonly initializer?: JLeftPadded<Expression>;
    readonly variableType?: JavaType.Variable;
}

export interface ExportDeclaration extends JS, Statement {
    readonly kind: typeof JavaScriptKind.ExportDeclaration;
    readonly modifiers: Modifier[];
    readonly typeOnly: JLeftPadded<boolean>;
    readonly exportClause?: Expression;
    readonly moduleSpecifier?: JLeftPadded<Expression>;
    readonly attributes?: ImportAttributes;
}

export interface ExportAssignment extends JS, Statement {
    readonly kind: typeof JavaScriptKind.ExportAssignment;
    readonly modifiers: Modifier[];
    readonly exportEquals: JLeftPadded<boolean>;
    readonly expression?: Expression;
}

export interface NamedExports extends JS {
    readonly kind: typeof JavaScriptKind.NamedExports;
    readonly elements: JContainer<Expression>;
    readonly type?: JavaType;
}

export interface ExportSpecifier extends JS {
    readonly kind: typeof JavaScriptKind.ExportSpecifier;
    readonly typeOnly: JLeftPadded<boolean>;
    readonly specifier: Expression;
    readonly type?: JavaType;
}
