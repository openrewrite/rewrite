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
import {Expression, J, JavaType, Statement, TypedTree, TypeTree, VariableDeclarator,} from "../java";
import getType = TypedTree.getType;
import FunctionType = JS.FunctionType;

export interface JS extends J {
}

export namespace JS {
    export const Kind = {
        ...TreeKind,
        Alias: "org.openrewrite.javascript.tree.JS$Alias",
        ArrayBindingPattern: "org.openrewrite.javascript.tree.JS$ArrayBindingPattern",
        ArrowFunction: "org.openrewrite.javascript.tree.JS$ArrowFunction",
        Await: "org.openrewrite.javascript.tree.JS$Await",
        BindingElement: "org.openrewrite.javascript.tree.JS$BindingElement",
        CompilationUnit: "org.openrewrite.javascript.tree.JS$CompilationUnit",
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
        ForInLoop: "org.openrewrite.javascript.tree.JS$ForInLoop",
        ForOfLoop: "org.openrewrite.javascript.tree.JS$ForOfLoop",
        ComputedPropertyMethodDeclaration: "org.openrewrite.javascript.tree.JS$ComputedPropertyMethodDeclaration",
        AssignmentOperation: "org.openrewrite.javascript.tree.JS$AssignmentOperation",
        Binary: "org.openrewrite.javascript.tree.JS$Binary",
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
        ObjectBindingDeclarations: "org.openrewrite.javascript.tree.JS$ObjectBindingDeclarations",
        PropertyAssignment: "org.openrewrite.javascript.tree.JS$PropertyAssignment",
        SatisfiesExpression: "org.openrewrite.javascript.tree.JS$SatisfiesExpression",
        ScopedVariableDeclarations: "org.openrewrite.javascript.tree.JS$ScopedVariableDeclarations",
        StatementExpression: "org.openrewrite.javascript.tree.JS$StatementExpression",
        TaggedTemplateExpression: "org.openrewrite.javascript.tree.JS$TaggedTemplateExpression",
        TemplateExpression: "org.openrewrite.javascript.tree.JS$TemplateExpression",
        TemplateExpressionSpan: "org.openrewrite.javascript.tree.JS$TemplateExpression$TemplateSpan",
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
        Union: "org.openrewrite.javascript.tree.JS$Union",
        Void: "org.openrewrite.javascript.tree.JS$Void",
        WithStatement: "org.openrewrite.javascript.tree.JS$WithStatement",
    } as const;

    /**
     * Represents the root of a JavaScript AST (compilation unit).
     * @example // file.js as AST root
     * // statements and EOF marker
     */
    export interface CompilationUnit extends JS, SourceFile {
        readonly kind: typeof Kind.CompilationUnit;
        readonly imports: J.RightPadded<J.Import>[];
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
     * Represents the `await` operator in an async function.
     * @example const result = await fetchData();
     */
    export interface Await extends JS, Expression {
        readonly kind: typeof Kind.Await;
        readonly expression: Expression;
        readonly type?: JavaType;
    }

    /**
     * Represents a TypeScript conditional type (ternary type).
     * @example T extends U ? X : Y
     */
    export interface ConditionalType extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.ConditionalType;
        readonly checkType: Expression;
        readonly condition: J.LeftPadded<J.Ternary>;
        readonly type?: JavaType;
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
        readonly type?: JavaType;
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
        readonly type?: JavaType;
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
        readonly type?: JavaType;
    }

    /**
     * Represents an import declaration.
     * @example import x from 'module';
     */
    export interface Import extends JS, Statement {
        readonly kind: typeof Kind.Import;
        readonly importClause?: ImportClause;
        readonly moduleSpecifier: J.LeftPadded<Expression>;
        readonly attributes?: ImportAttributes;
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
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
    }

    /**
     * Represents a single specifier in a named import.
     * @example import { foo as bar } from 'baz';
     */
    export interface ImportSpecifier extends JS, Expression, TypedTree {
        readonly kind: typeof Kind.ImportSpecifier;
        readonly importType: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: JavaType;
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
        readonly type?: JavaType;
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
        readonly type: JavaType;
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
        readonly type?: JavaType;
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
    export interface ObjectBindingDeclarations extends JS, Expression, TypedTree, VariableDeclarator {
        readonly kind: typeof Kind.ObjectBindingDeclarations;
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
        readonly type?: JavaType;
    }

    /**
     * Represents scoped variable declarations with keywords like const/let/var.
     * @example let x = 10;
     */
    export interface ScopedVariableDeclarations extends JS, Statement {
        readonly kind: typeof Kind.ScopedVariableDeclarations;
        readonly modifiers: J.Modifier[];
        readonly scope?: J.LeftPadded<ScopedVariableDeclarations.Scope>;
        readonly variables: J.RightPadded<J>[];
    }

    export namespace ScopedVariableDeclarations {
        export const enum Scope {
            Const = "Const",
            Let = "Let",
            Var = "Var",
            Using = "Using",
            Import = "Import"
        }
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
        readonly type?: JavaType;
    }

    /**
     * Represents a template string expression.
     * @example `Hello ${name}`;
     */
    export interface TemplateExpression extends JS, Statement, Expression, TypeTree {
        readonly kind: typeof Kind.TemplateExpression;
        readonly head: J.Literal;
        readonly spans: J.RightPadded<TemplateExpression.Span>[];
        readonly type?: JavaType;
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
     * Represents a statement ending with a trailing token.
     * @example function foo(){};
     */
    export interface TrailingTokenStatement extends JS, Statement, Expression {
        readonly kind: typeof Kind.TrailingTokenStatement;
        readonly expression: J.RightPadded<J>;
        readonly type?: JavaType;
    }

    /**
     * Represents a tuple type in TypeScript.
     * @example type T = [string, number];
     */
    export interface Tuple extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.Tuple;
        readonly elements: J.Container<J>;
        readonly type?: JavaType;
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
        readonly type?: JavaType;
    }

    /**
     * Represents the `typeof` type operator in TypeScript.
     * @example type T = typeof x;
     */
    export interface TypeOf extends JS, Expression {
        readonly kind: typeof Kind.TypeOf;
        readonly expression: Expression;
        readonly type?: JavaType;
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
        readonly type?: JavaType;
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
        readonly type?: JavaType;
    }


    export namespace IndexedAccessType {
        /**
         * Represents the index type in an indexed access.
         * @example 'key'
         */
        export interface IndexType extends JS {
            readonly kind: typeof Kind.IndexedAccessTypeIndexType;
            readonly element: J.RightPadded<TypeTree>;
            readonly type?: JavaType;
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
        readonly type?: JavaType;
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
        readonly type?: JavaType;
    }

    /**
     * Represents a union type in TypeScript.
     * @example type U = A | B;
     */
    export interface Union extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.Union;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: JavaType;
    }

    /**
     * Represents an intersection type in TypeScript.
     * @example type I = A & B;
     */
    export interface Intersection extends JS, Expression, TypeTree {
        readonly kind: typeof Kind.Intersection;
        readonly types: J.RightPadded<Expression>[];
        readonly type?: JavaType;
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
        readonly type?: JavaType;
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
        readonly methodType?: JavaType.Method;
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
        export enum KeywordType {
            Namespace,
            Module,
            Empty,
        }
    }

    /**
     * Represents a literal type object in TypeScript.
     * @example type T = { a: number };
     */
    export interface TypeLiteral extends JS, TypeTree {
        readonly kind: typeof Kind.TypeLiteral;
        readonly members: J.Block;
        readonly type?: JavaType;
    }

    /**
     * Represents an array destructuring pattern.
     * @example const [a, b] = arr;
     */
    export interface ArrayBindingPattern extends JS, TypedTree, VariableDeclarator {
        readonly kind: typeof Kind.ArrayBindingPattern;
        readonly elements: J.Container<Expression>;
        readonly type?: JavaType;
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
        readonly variableType?: JavaType.Variable;
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
        readonly type?: JavaType;
    }

    /**
     * Represents a specifier in a named export.
     * @example export { foo as bar };
     */
    export interface ExportSpecifier extends JS {
        readonly kind: typeof Kind.ExportSpecifier;
        readonly typeOnly: J.LeftPadded<boolean>;
        readonly specifier: Expression;
        readonly type?: JavaType;
    }
}

const KindValues = new Set(Object.values(JS.Kind));

export function isJavaScript(tree: any): tree is JS {
    return KindValues.has(tree["kind"]);
}


TypedTree.registerTypeGetter(JS.Kind.PropertyAssignment, (tree: JS.PropertyAssignment) => getType(tree.initializer));
TypedTree.registerTypeGetter(JS.Kind.FunctionType, (tree: FunctionType) => getType(tree.returnType.element))
