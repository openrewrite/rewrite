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

import {SourceFile} from "../tree";
import {
    Expression,
    J,
    JavaVisitor,
    MethodCall,
    NameTree,
    Statement,
    Type,
    TypedTree,
    TypeTree,
    VariableDeclarator,
    registerJavaExtensionKinds,
    getPaddedElement
} from "../java";
import {JavaScriptVisitor} from "./visitor";

export interface JS extends J {
}

export namespace JS {
    export const Kind = {
        Alias: "org.openrewrite.javascript.tree.JS$Alias",
        ArrayBindingPattern: "org.openrewrite.javascript.tree.JS$ArrayBindingPattern",
        ArrowFunction: "org.openrewrite.javascript.tree.JS$ArrowFunction",
        As: "org.openrewrite.javascript.tree.JS$As",
        AssignmentOperation: "org.openrewrite.javascript.tree.JS$AssignmentOperation",
        Await: "org.openrewrite.javascript.tree.JS$Await",
        Binary: "org.openrewrite.javascript.tree.JS$Binary",
        BindingElement: "org.openrewrite.javascript.tree.JS$BindingElement",
        CompilationUnit: "org.openrewrite.javascript.tree.JS$CompilationUnit",
        ComputedPropertyMethodDeclaration: "org.openrewrite.javascript.tree.JS$ComputedPropertyMethodDeclaration",
        ComputedPropertyName: "org.openrewrite.javascript.tree.JS$ComputedPropertyName",
        ConditionalType: "org.openrewrite.javascript.tree.JS$ConditionalType",
        Delete: "org.openrewrite.javascript.tree.JS$Delete",
        Export: "org.openrewrite.javascript.tree.JS$Export",
        ExportAssignment: "org.openrewrite.javascript.tree.JS$ExportAssignment",
        ExportDeclaration: "org.openrewrite.javascript.tree.JS$ExportDeclaration",
        ExportSpecifier: "org.openrewrite.javascript.tree.JS$ExportSpecifier",
        ExpressionStatement: "org.openrewrite.javascript.tree.JS$ExpressionStatement",
        ExpressionWithTypeArguments: "org.openrewrite.javascript.tree.JS$ExpressionWithTypeArguments",
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
        FunctionCall: "org.openrewrite.javascript.tree.JS$FunctionCall",
        ForInLoop: "org.openrewrite.javascript.tree.JS$ForInLoop",
        ForOfLoop: "org.openrewrite.javascript.tree.JS$ForOfLoop",
        JsxEmbeddedExpression: "org.openrewrite.javascript.tree.JSX$EmbeddedExpression",
        JsxSpreadAttribute: "org.openrewrite.javascript.tree.JSX$SpreadAttribute",
        JsxNamespacedName: "org.openrewrite.javascript.tree.JSX$NamespacedName",
        JsxTag: "org.openrewrite.javascript.tree.JSX$Tag",
        JsxAttribute: "org.openrewrite.javascript.tree.JSX$Attribute",
        Import: "org.openrewrite.javascript.tree.JS$Import",
        ImportClause: "org.openrewrite.javascript.tree.JS$ImportClause",
        ImportSpecifier: "org.openrewrite.javascript.tree.JS$ImportSpecifier",
        LiteralType: "org.openrewrite.javascript.tree.JS$LiteralType",
        MappedType: "org.openrewrite.javascript.tree.JS$MappedType",
        MappedTypeKeysRemapping: "org.openrewrite.javascript.tree.JS$MappedType$KeysRemapping",
        MappedTypeParameter: "org.openrewrite.javascript.tree.JS$MappedType$Parameter",
        NamedExports: "org.openrewrite.javascript.tree.JS$NamedExports",
        NamedImports: "org.openrewrite.javascript.tree.JS$NamedImports",
        NamespaceDeclaration: "org.openrewrite.javascript.tree.JS$NamespaceDeclaration",
        ObjectBindingPattern: "org.openrewrite.javascript.tree.JS$ObjectBindingPattern",
        PropertyAssignment: "org.openrewrite.javascript.tree.JS$PropertyAssignment",
        SatisfiesExpression: "org.openrewrite.javascript.tree.JS$SatisfiesExpression",
        ScopedVariableDeclarations: "org.openrewrite.javascript.tree.JS$ScopedVariableDeclarations",
        Shebang: "org.openrewrite.javascript.tree.JS$Shebang",
        Spread: "org.openrewrite.javascript.tree.JS$Spread",
        StatementExpression: "org.openrewrite.javascript.tree.JS$StatementExpression",
        TaggedTemplateExpression: "org.openrewrite.javascript.tree.JS$TaggedTemplateExpression",
        TemplateExpression: "org.openrewrite.javascript.tree.JS$TemplateExpression",
        TemplateExpressionSpan: "org.openrewrite.javascript.tree.JS$TemplateExpression$Span",
        Tuple: "org.openrewrite.javascript.tree.JS$Tuple",
        TypeDeclaration: "org.openrewrite.javascript.tree.JS$TypeDeclaration",
        TypeInfo: "org.openrewrite.javascript.tree.JS$TypeInfo",
        TypeLiteral: "org.openrewrite.javascript.tree.JS$TypeLiteral",
        TypeOf: "org.openrewrite.javascript.tree.JS$TypeOf",
        TypeOperator: "org.openrewrite.javascript.tree.JS$TypeOperator",
        TypePredicate: "org.openrewrite.javascript.tree.JS$TypePredicate",
        TypeQuery: "org.openrewrite.javascript.tree.JS$TypeQuery",
        TypeTreeExpression: "org.openrewrite.javascript.tree.JS$TypeTreeExpression",
        Union: "org.openrewrite.javascript.tree.JS$Union",
        Void: "org.openrewrite.javascript.tree.JS$Void",
        WithStatement: "org.openrewrite.javascript.tree.JS$WithStatement",
    } as const;

    export function isMethodCall(e?: Expression): e is MethodCall {
        return J.isMethodCall(e) || e?.kind === JS.Kind.FunctionCall;
    }

    /**
     * Represents the root of a JavaScript AST (compilation unit).
     * @example // file.js as AST root
     * // statements and EOF marker
     */
    export interface CompilationUnit extends JS, SourceFile {
        readonly kind: typeof Kind.CompilationUnit;
        readonly statements: J.RightPadded<Statement>[];
        readonly eof: J.Space;
    }

    /**
     * In a namespace import, aliases the contents of the namespace.
     * @example import * as path from "path"
     */
    export interface Alias extends JS, Expression {
        readonly kind: typeof Kind.Alias;
        readonly propertyName: J.RightPadded<J.Identifier>;
        readonly alias: Expression;
    }

    /**
     * Represents an arrow function expression.
     * @example const f = (x: number) => x + 1;
     */
    export interface ArrowFunction extends JS, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.ArrowFunction;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeParameters?: J.TypeParameters;
        readonly lambda: J.Lambda;
        readonly returnTypeExpression?: TypeTree;
    }

    /**
     * Represents an "as" expression, used for type assertions or casting.
     * @example const x = value as Type;
     */
    export interface As extends JS, Expression, TypedTree {
        readonly kind: typeof Kind.As;
        readonly left: J.RightPadded<Expression>;
        readonly right: Expression;
        readonly type?: Type;
    }

    /**
     * Represents the `await` operator in an async function.
     * @example const result = await fetchData();
     */
    export interface Await extends JS, Expression {
        readonly kind: typeof Kind.Await;
        readonly expression: Expression;
        readonly type?: Type;
    }

    /**
     * Represents a TypeScript conditional type (ternary type).
     * @example T extends U ? X : Y
     */
    export interface ConditionalType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.ConditionalType;
        readonly checkType: Expression;
        readonly condition: J.LeftPadded<J.Ternary>;
        readonly type?: Type;
    }

    /**
     * Represents the `delete` operator.
     * @example delete obj.prop;
     */
    export interface Delete extends JS, Statement, Expression {
        readonly kind: typeof Kind.Delete;
        readonly expression: Expression;
    }

    /**
     * Represents an expression used as a statement.
     * @example obj.method();
     */
    export interface ExpressionStatement extends JS, Statement, Expression {
        readonly kind: typeof Kind.ExpressionStatement;
        readonly expression: Expression;
    }

    /**
     * Represents an expression with type arguments (generic invocation).
     * @example myFunc<number>(arg);
     */
    export interface ExpressionWithTypeArguments extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.ExpressionWithTypeArguments;
        readonly clazz: J;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents a TypeScript function type.
     * @example type Fn = (x: number) => string;
     */
    export interface FunctionType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.FunctionType;
        readonly modifiers: J.Modifier[];
        readonly constructorType: J.LeftPadded<boolean>;
        readonly typeParameters?: J.TypeParameters;
        readonly parameters: J.Container<Statement>;
        readonly returnType: J.LeftPadded<Expression>;
    }

    /**
     * Represents the `infer` keyword in conditional types.
     * @example T extends (...args: any[]) => infer R ? R : any;
     */
    export interface InferType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.InferType;
        readonly typeParameter: J.LeftPadded<J>;
        readonly type?: Type;
    }

    /**
     * Represents a dynamic import type.
     * @example type T = import('module').Foo;
     */
    export interface ImportType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.ImportType;
        readonly hasTypeof: J.RightPadded<boolean>;
        readonly argumentAndAttributes: J.Container<J>;
        readonly qualifier?: J.LeftPadded<Expression>;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents an import declaration.
     * @example import x from 'module';
     */
    export interface Import extends JS, Statement {
        readonly kind: typeof Kind.Import;
        readonly modifiers: J.Modifier[];
        readonly importClause?: ImportClause;
        readonly moduleSpecifier?: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
        readonly initializer?: J.LeftPadded<Expression>;
    }

    /**
     * Represents the clause of an import statement, including default and named bindings.
     * @example import {A, B as C} from 'module';
     */
    export interface ImportClause extends JS {
        readonly kind: typeof Kind.ImportClause;
        readonly typeOnly: boolean;
        readonly name?: J.RightPadded<J.Identifier>;
        readonly namedBindings?: Expression;
    }

    /**
     * Represents named imports in an import declaration.
     * @example import { a, b } from 'fs';
     */
    export interface NamedImports extends JS, Expression {
        readonly kind: typeof Kind.NamedImports;
        readonly elements: J.Container<ImportSpecifier>;
        readonly type?: Type;
    }

    /**
     * Represents a single specifier in a named import.
     * @example import { foo as bar } from 'baz';
     */
    export interface ImportSpecifier extends JS, Expression, TypedTree {
        readonly kind: typeof Kind.ImportSpecifier;
        readonly importType: J.LeftPadded<boolean>;
        readonly specifier: J.Identifier | Alias;
        readonly type?: Type;
    }

    /**
     * Represents import assertion attributes.
     * @example import data from './data.json' assert { type: 'json' };
     */
    export interface ImportAttributes extends JS {
        readonly kind: typeof Kind.ImportAttributes;
        readonly token: ImportAttributes.Token;
        readonly elements: J.Container<Statement>;
    }

    export namespace ImportAttributes {
        export const enum Token {
            With = "With",
            Assert = "Assert"
        }
    }

    /**
     * Represents attributes for import type declarations.
     * @example import type A from './module';
     */
    export interface ImportTypeAttributes extends JS {
        readonly kind: typeof Kind.ImportTypeAttributes;
        readonly token: J.RightPadded<Expression>;
        readonly elements: J.Container<ImportAttribute>;
        readonly end: J.Space;
    }

    /**
     * Represents a single import assertion attribute key-value pair.
     * @example assert { type: 'json' };
     */
    export interface ImportAttribute extends JS, Statement {
        readonly kind: typeof Kind.ImportAttribute;
        readonly name: Expression;
        readonly value: J.LeftPadded<Expression>;
    }

    /**
     * Represents a binary expression.
     * @example a + b;
     */
    export interface Binary extends JS, Expression, TypedTree {
        readonly kind: typeof Kind.Binary;
        readonly left: Expression;
        readonly operator: J.LeftPadded<Binary.Type>;
        readonly right: Expression;
        readonly type?: Type;
    }

    export namespace Binary {
        export const enum Type {
            As = "As",
            IdentityEquals = "IdentityEquals",
            IdentityNotEquals = "IdentityNotEquals",
            In = "In",
            QuestionQuestion = "QuestionQuestion",
            Comma = "Comma"
        }
    }

    /**
     * Represents a literal type in TypeScript.
     * @example type T = 'foo';
     */
    export interface LiteralType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.LiteralType;
        readonly literal: Expression;
        type: Type;
    }


    /**
     * Represents a mapped type in TypeScript.
     * @example type Readonly<T> = { readonly [P in keyof T]: T[P] };
     */
    export interface MappedType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.MappedType;
        readonly prefixToken?: J.LeftPadded<J.Literal>;
        readonly hasReadonly: J.LeftPadded<boolean>;
        readonly keysRemapping: MappedType.KeysRemapping;
        readonly suffixToken?: J.LeftPadded<J.Literal>;
        readonly hasQuestionToken: J.LeftPadded<boolean>;
        readonly valueType: J.Container<TypeTree>;
        readonly type?: Type;
    }

    export namespace MappedType {
        /**
         * Represents key remapping in a mapped type.
         * @example { [K in keyof T as Uppercase<K>]: T[K] }
         */
        export interface KeysRemapping extends JS, Statement {
            readonly kind: typeof Kind.MappedTypeKeysRemapping;
            readonly typeParameter: J.RightPadded<MappedType.Parameter>;
            readonly nameType?: J.RightPadded<Expression>;
        }

        /**
         * Represents a type parameter in a mapped type.
         * @example [P in K]
         */
        export interface Parameter extends JS, Statement {
            readonly kind: typeof Kind.MappedTypeParameter;
            readonly name: Expression;
            readonly iterateType: J.LeftPadded<TypeTree>;
        }
    }

    /**
     * Represents object destructuring patterns.
     * @example const { a, b } = obj;
     */
    export interface ObjectBindingPattern extends JS, Expression, TypedTree, VariableDeclarator {
        readonly kind: typeof Kind.ObjectBindingPattern;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeExpression?: TypeTree;
        readonly bindings: J.Container<J>;
        readonly initializer?: J.LeftPadded<Expression>;
    }

    /**
     * Represents a property assignment in an object literal.
     * @example { key: value }
     */
    export interface PropertyAssignment extends JS, Statement, TypedTree {
        readonly kind: typeof Kind.PropertyAssignment;
        readonly name: J.RightPadded<Expression>;
        readonly assigmentToken: PropertyAssignment.Token;
        readonly initializer?: Expression;
    }

    export namespace PropertyAssignment {
        export const enum Token {
            Colon = "Colon",
            Equals = "Equals",
            Empty = "Empty"
        }
    }

    /**
     * Represents the `satisfies` operator in TypeScript.
     * @example const x = { a: 1 } satisfies Foo;
     */
    export interface SatisfiesExpression extends JS, Expression {
        readonly kind: typeof Kind.SatisfiesExpression;
        readonly expression: J;
        readonly satisfiesType: J.LeftPadded<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents scoped variable declarations with keywords like const/let/var.
     * @example let x = 10;
     */
    export interface ScopedVariableDeclarations extends JS, Statement {
        readonly kind: typeof Kind.ScopedVariableDeclarations;
        readonly modifiers: J.Modifier[];
        readonly variables: J.RightPadded<J>[];
    }

    /**
     * Represents a shebang line at the beginning of a script.
     * @example #!/usr/bin/env node
     */
    export interface Shebang extends JS, Statement {
        readonly kind: typeof Kind.Shebang;
        readonly text: string;
    }

    /**
     * Represents the spread syntax (...) applied to an expression.
     * Used in array literals, object literals, function call arguments,
     * and rest syntax in function parameters and destructuring patterns.
     * @example [...arr]
     * @example {...obj}
     * @example f(...args)
     * @example function f(...args) {}
     * @example const [first, ...rest] = arr
     */
    export interface Spread extends JS, Statement, Expression, TypedTree, VariableDeclarator {
        readonly kind: typeof Kind.Spread;
        readonly expression: Expression;
        readonly type?: Type;
    }

    /**
     * Represents a statement used as an expression. The example shows a function expressions.
     * @example const greet = function  (name: string) : string { return name; };
     */
    export interface StatementExpression extends JS, Statement, Expression {
        readonly kind: typeof Kind.StatementExpression;
        readonly statement: Statement;
    }

    /**
     * Represents a tagged template literal.
     * @example html`<div>${content}</div>`;
     */
    export interface TaggedTemplateExpression extends JS, Statement, Expression {
        readonly kind: typeof Kind.TaggedTemplateExpression;
        readonly tag?: J.RightPadded<Expression>;
        readonly typeArguments?: J.Container<Expression>;
        readonly templateExpression: Expression;
        readonly type?: Type;
    }

    /**
     * Represents a template string expression.
     * @example `Hello ${name}`;
     */
    export interface TemplateExpression extends JS, Statement, Expression, TypeTree {
        readonly kind: typeof Kind.TemplateExpression;
        readonly head: J.Literal;
        readonly spans: J.RightPadded<TemplateExpression.Span>[];
        readonly type?: Type;
    }

    export namespace TemplateExpression {
        /**
         * Represents a span in a template expression.
         * @example the `${expr}` and tail parts
         */
        export interface Span extends JS {
            readonly kind: typeof Kind.TemplateExpressionSpan;
            readonly expression: J;
            readonly tail: J.Literal;
        }
    }

    /**
     * Represents a tuple type in TypeScript.
     * @example type T = [string, number];
     */
    export interface Tuple extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.Tuple;
        readonly elements: J.Container<J>;
        readonly type?: Type;
    }

    /**
     * Represents a type alias declaration.
     * @example type Foo = string;
     */
    export interface TypeDeclaration extends JS, Statement, TypedTree {
        readonly kind: typeof Kind.TypeDeclaration;
        readonly modifiers: J.Modifier[];
        readonly name: J.LeftPadded<J.Identifier>;
        readonly typeParameters?: J.TypeParameters;
        readonly initializer: J.LeftPadded<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents the `typeof` type operator in TypeScript.
     * @example type T = typeof x;
     */
    export interface TypeOf extends JS, Expression {
        readonly kind: typeof Kind.TypeOf;
        readonly expression: Expression;
        readonly type?: Type;
    }

    /**
     * Represents an expression used as a type tree.
     * @example (<T>value)
     */
    export interface TypeTreeExpression extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.TypeTreeExpression;
        readonly expression: Expression;
    }

    /**
     * Represents an assignment operation.
     * @example a ||= b;
     */
    export interface AssignmentOperation extends JS, Statement, Expression, TypedTree {
        readonly kind: typeof Kind.AssignmentOperation;
        readonly variable: Expression;
        readonly operator: J.LeftPadded<AssignmentOperation.Type>;
        readonly assignment: Expression;
        readonly type?: Type;
    }

    export namespace AssignmentOperation {
        export const enum Type {
            QuestionQuestion = "QuestionQuestion",
            And = "And",
            Or = "Or",
            Power = "Power",
            Exp = "Exp"
        }
    }

    /**
     * Represents an indexed access type in TypeScript.
     * @example type A = T['prop'];
     */
    export interface IndexedAccessType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.IndexedAccessType;
        readonly objectType: TypeTree;
        readonly indexType: TypeTree;
        readonly type?: Type;
    }


    export namespace IndexedAccessType {
        /**
         * Represents the index type in an indexed access.
         * @example 'key'
         */
        export interface IndexType extends JS {
            readonly kind: typeof Kind.IndexedAccessTypeIndexType;
            readonly element: J.RightPadded<TypeTree>;
            readonly type?: Type;
        }
    }

    /**
     * Represents a typeof type query in TypeScript.
     * @example type T = typeof obj;
     */
    export interface TypeQuery extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.TypeQuery;
        readonly typeExpression: TypeTree;
        readonly typeArguments?: J.Container<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents explicit type information.
     * @example use in type cast contexts
     */
    export interface TypeInfo extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.TypeInfo;
        readonly typeIdentifier: TypeTree;
    }

    /**
     * Represents TypeScript computed property name.
     * @example { [key]: value }
     */
    export interface ComputedPropertyName extends JS, Expression, TypedTree, VariableDeclarator {
        readonly kind: typeof Kind.ComputedPropertyName;
        readonly expression: J.RightPadded<Expression>;
    }

    /**
     * Represents TypeScript type operator keywords like readonly.
     * @example type R = readonly number[];
     */
    export interface TypeOperator extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.TypeOperator;
        readonly operator: TypeOperator.Type;
        readonly expression: J.LeftPadded<Expression>;
    }

    export namespace TypeOperator {
        export const enum Type {
            ReadOnly = "ReadOnly",
            KeyOf = "KeyOf",
            Unique = "Unique"
        }
    }

    /**
     * Represents a type predicate in TypeScript.
     * @example function isString(x): x is string;
     */
    export interface TypePredicate extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.TypePredicate;
        readonly asserts: J.LeftPadded<boolean>;
        readonly parameterName: J.Identifier;
        readonly expression?: J.LeftPadded<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents a union type in TypeScript.
     * @example type U = A | B;
     */
    export interface Union extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.Union;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: Type;
    }

    /**
     * Represents an intersection type in TypeScript.
     * @example type I = A & B;
     */
    export interface Intersection extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.Intersection;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: Type;
    }

    /**
     * Represents function calls which are not method invocations.
     * @example f(5, 0, 4)
     * @example f?.(5, 0, 4)
     * @example data["key"](5, 0, 4)
     * @example (() => { return 3 + 5 })()
     */
    export interface FunctionCall extends JS, MethodCall {
        readonly kind: typeof Kind.FunctionCall;
        readonly function?: J.RightPadded<Expression>;
        readonly typeParameters?: J.Container<Expression>;
        readonly arguments: J.Container<Expression>;
    }

    /**
     * Represents the `void` operator.
     * @example void 0;
     */
    export interface Void extends JS, Expression {
        readonly kind: typeof Kind.Void;
        readonly expression: Expression;
    }

    /**
     * Represents a `with` statement.
     * @example with (obj) { console.log(a); }
     */
    export interface WithStatement extends JS, Statement {
        readonly kind: typeof Kind.WithStatement;
        readonly expression: J.ControlParentheses<Expression>;
        readonly body: J.RightPadded<Statement>;
    }

    /**
     * Represents an index signature in TypeScript interfaces.
     * @example interface Foo { [key: string]: any; }
     */
    export interface IndexSignatureDeclaration extends JS, Statement, TypedTree {
        readonly kind: typeof Kind.IndexSignatureDeclaration;
        readonly modifiers: J.Modifier[];
        readonly parameters: J.Container<J>;
        readonly typeExpression: J.LeftPadded<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents a method declaration in classes or objects.
     * @example class A { [Symbol.asyncIterator]() { ... } }
     */
    export interface ComputedPropertyMethodDeclaration extends JS, Statement {
        readonly kind: typeof Kind.ComputedPropertyMethodDeclaration;
        readonly leadingAnnotations: J.Annotation[];
        readonly modifiers: J.Modifier[];
        readonly typeParameters?: J.TypeParameters;
        readonly returnTypeExpression?: TypeTree;
        readonly name: ComputedPropertyName;
        readonly parameters: J.Container<Statement>;
        readonly body?: J.Block;
        readonly methodType?: Type.Method;
    }

    /**
     * Represents a for-of loop.
     * @example for (const x of arr) { }
     * @example for await (const x of arr) { }
     */
    export interface ForOfLoop extends JS {
        readonly kind: typeof Kind.ForOfLoop;
        readonly await?: J.Space;
        readonly loop: J.ForEachLoop;
    }

    export namespace ForOfLoop {
        export type Control = J.ForEachLoop.Control;
    }

    /**
     * Represents a for-in loop.
     * @example for (let key in obj) { }
     */
    export interface ForInLoop extends JS {
        readonly kind: typeof Kind.ForInLoop;
        readonly control: ForInLoop.Control;
        readonly body: J.RightPadded<Statement>;
    }

    export namespace ForInLoop {
        export type Control = J.ForEachLoop.Control;
    }

    /**
     * Represents a namespace declaration in TypeScript.
     * @example namespace MyNS { }
     */
    export interface NamespaceDeclaration extends JS, Statement {
        readonly kind: typeof Kind.NamespaceDeclaration;
        readonly modifiers: J.Modifier[];
        readonly keywordType: J.LeftPadded<NamespaceDeclaration.KeywordType>;
        readonly name: J.RightPadded<Expression>;
        readonly body?: J.Block;
    }

    export namespace NamespaceDeclaration {
        export const enum KeywordType {
            Namespace = "Namespace",
            Module = "Module",
            Empty = "Empty"
        }
    }

    /**
     * Represents a literal type object in TypeScript.
     * @example type T = { a: number };
     */
    export interface TypeLiteral extends JS, TypeTree {
        readonly kind: typeof Kind.TypeLiteral;
        readonly members: J.Block;
        readonly type?: Type;
    }

    /**
     * Represents an array destructuring pattern.
     * @example const [a, b] = arr;
     */
    export interface ArrayBindingPattern extends JS, TypedTree, VariableDeclarator {
        readonly kind: typeof Kind.ArrayBindingPattern;
        readonly elements: J.Container<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents a named element in a binding pattern.
     * @example const { x: alias } = obj;
     */
    export interface BindingElement extends JS, Statement, TypeTree {
        readonly kind: typeof Kind.BindingElement;
        readonly propertyName?: J.RightPadded<Expression>;
        readonly name: TypedTree;
        readonly initializer?: J.LeftPadded<Expression>;
        readonly variableType?: Type.Variable;
    }

    /**
     * Represents an export declaration (export ...).
     * @example export class X {}
     */
    export interface ExportDeclaration extends JS, Statement {
        readonly kind: typeof Kind.ExportDeclaration;
        readonly modifiers: J.Modifier[];
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly exportClause?: Expression;
        readonly moduleSpecifier?: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
    }

    /**
     * Represents an export assignment.
     * @example export = foo;
     * @example export default foo;
     */
    export interface ExportAssignment extends JS, Statement {
        readonly kind: typeof Kind.ExportAssignment;
        readonly exportEquals: boolean;
        readonly expression: J.LeftPadded<Expression>;
    }

    /**
     * Represents named exports in an export declaration.
     * @example export { a, b };
     */
    export interface NamedExports extends JS {
        readonly kind: typeof Kind.NamedExports;
        readonly elements: J.Container<Expression>;
        readonly type?: Type;
    }

    /**
     * Represents a specifier in a named export.
     * @example export { foo as bar };
     */
    export interface ExportSpecifier extends JS {
        readonly kind: typeof Kind.ExportSpecifier;
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: Type;
    }
}

export namespace JSX {
    /**
     * Represents a JSX tag. Note that `selfClosing` and `children` are mutually exclusive.
     * @example <div>{child}</div>
     */
    export type Tag =
        (BaseTag & {
            readonly selfClosing: J.Space;
            readonly children?: undefined;
            readonly closingName?: undefined;
            readonly afterClosingName?: undefined;
        }) |
        (BaseTag & {
            readonly selfClosing?: undefined;
            readonly children: (EmbeddedExpression | Tag | J.Identifier | J.Literal | J.Empty)[];
            readonly closingName: J.LeftPadded<J.Identifier | J.FieldAccess | NamespacedName | J.Empty>;
            readonly afterClosingName: J.Space;
        });

    interface BaseTag extends JS, Expression {
        readonly kind: typeof JS.Kind.JsxTag;
        readonly openName: J.LeftPadded<J.Identifier | J.FieldAccess | NamespacedName | J.Empty>;
        readonly typeArguments?: J.Container<Expression>;
        readonly afterName: J.Space;
        readonly attributes: J.RightPadded<Attribute | SpreadAttribute>[];
    }

    /**
     * Represents a single JSX attribute.
     * @example prop="value"
     */
    export interface Attribute extends JS {
        readonly kind: typeof JS.Kind.JsxAttribute;
        readonly key: J.Identifier | NamespacedName;
        readonly value?: J.LeftPadded<Expression>;
    }

    /**
     * Represents a spread attribute in JSX.
     * @example {...props}
     */
    export interface SpreadAttribute extends JS {
        readonly kind: typeof JS.Kind.JsxSpreadAttribute;
        readonly dots: J.Space
        readonly expression: J.RightPadded<Expression>;
    }

    /**
     * Represents a JSX expression container.
     * @example {expression}
     */
    export interface EmbeddedExpression extends JS, Expression {
        readonly kind: typeof JS.Kind.JsxEmbeddedExpression;
        readonly expression: J.RightPadded<Expression>;
    }

    /**
     * Represents a namespaced JSX name.
     * @example namespace:Name
     */
    export interface NamespacedName extends JS, NameTree {
        readonly kind: typeof JS.Kind.JsxNamespacedName;
        readonly namespace: J.Identifier;
        readonly name: J.LeftPadded<J.Identifier>;
    }
}

const KindValues = new Set(Object.values(JS.Kind));

function javascriptVisitorAdapter<P>(visitor: JavaVisitor<P>): JavaVisitor<P> {
    const adaptedVisitor = new JavaScriptVisitor<P>();

    // Walk up the prototype chain of the visitor to get all overridden methods
    let proto = Object.getPrototypeOf(visitor);
    while (proto && proto !== JavaVisitor.prototype) {
        Object.getOwnPropertyNames(proto).forEach(name => {
            const descriptor = Object.getOwnPropertyDescriptor(proto, name);
            if (descriptor && typeof descriptor.value === 'function' && name !== 'constructor') {
                // Copy the method, binding it to jsVisitor
                (adaptedVisitor as any)[name] = descriptor.value.bind(adaptedVisitor);
            }
        });
        proto = Object.getPrototypeOf(proto);
    }

    // Also copy any instance properties
    Object.keys(visitor).forEach(key => {
        if (!(key in adaptedVisitor)) {
            (adaptedVisitor as any)[key] = (visitor as any)[key];
        }
    });

    return adaptedVisitor;
}

registerJavaExtensionKinds(Object.values(JS.Kind), javascriptVisitorAdapter);

export function isJavaScript(tree: any): tree is JS {
    return KindValues.has(tree["kind"]);
}

export function isExpressionStatement(tree: any): tree is JS.ExpressionStatement {
    return tree["kind"] === JS.Kind.ExpressionStatement;
}

TypedTree.registerTypeGetter(JS.Kind.PropertyAssignment, (tree: JS.PropertyAssignment) => TypedTree.getType(tree.initializer));
TypedTree.registerTypeGetter(JS.Kind.FunctionType, (tree: JS.FunctionType) => TypedTree.getType(getPaddedElement(tree.returnType)))
