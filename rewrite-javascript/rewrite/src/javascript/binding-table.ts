/*
 * Copyright 2026 the original author or authors.
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
import ts from "typescript";
import {isStringLiteral, J, Statement} from "../java";
import {JS, moduleExportName} from "./tree";
import {bindingNames, declarationsOf} from "./scope";

/**
 * A local name that a top-level import, `require` or `await import` gives a module or one of its exports.
 * `form` says which value the name holds, since forms that read alike hold different values:
 * `D` of `import D from "m"` is the default export, while `D` of `const D = require("m")` is the whole module.
 */
export type ModuleBinding =
    | {form: "default"} & ImportedBinding                          // import D from "m"
    | {form: "namespace"} & ImportedBinding                        // import * as NS from "m"
    | {form: "named"; member: string} & ImportedBinding            // import {a, b as c, type d, "e-f" as g} from "m"
    | {form: "import-equals"} & ImportedBinding                    // import X = require("m")
    | {form: "require"} & DeclaredBinding                          // const X = require("m"), let and var too
    | {form: "require-member"; member: string} & DeclaredBinding   // const {a, b: c} = require("m")
    | {form: "require-pattern"} & DeclaredBinding                  // const {...rest} = require("m"), {a: {b}}, [x]
    | {form: "dynamic-import"} & DeclaredBinding;                  // const X = await import("m")

/** A name an `import` statement binds. */
export interface ImportedBinding {
    name: string;
    module: string;

    /** `import type` or the specifier's own `type` erases it, so the name stands for no value at runtime. */
    typeOnly: boolean;

    statement: JS.Import;
}

/**
 * A name a top-level `const`, `let` or `var` binds to what its initializer loads.
 * `member` of a `require-member` is the property its pattern reads, `"b"` for `c` in `const {b: c} = require("m")`.
 */
export interface DeclaredBinding {
    name: string;
    module: string;

    /** A declaration binds a value, never a type alone. */
    typeOnly: false;

    /** A `let` or `var` declares it, which the file may assign another value to. */
    reassignable: boolean;

    /** The declaration holding the variable: `statement` itself, or one of a multi-declarator statement's. */
    declaration: J.VariableDeclarations;

    statement: Statement;
}

/**
 * The module bindings the file's top level declares, in statement order, so every reader of "what binds a module
 * here" reads one list. `import R, {useState as s} from "react"; const fs = require("fs")` →
 * `[{form: "default", name: "R"}, {form: "named", name: "s", member: "useState"}, {form: "require", name: "fs"}]`.
 */
export function bindingTable(cu: JS.CompilationUnit): ModuleBinding[] {
    return cu.statements.flatMap(s => bindingsOf(s.element));
}

/**
 * The module bindings one top-level statement declares, the rows {@link bindingTable} has for it, so an edit to
 * that statement reads its bindings the way every other reader does.
 * `import D, * as NS from "m"` → `D` (default) and `NS` (namespace); `import "m"` and `const x = 1` → none.
 */
export function bindingsOf(statement: J | undefined): ModuleBinding[] {
    if (statement?.kind === JS.Kind.Import) {
        return importBindings(statement as JS.Import);
    }
    const reassignable = !declaresConst(statement);
    return declarationsOf(statement).flatMap(declaration => declaration.variables.flatMap(({element: variable}) =>
        declaredBindings(variable, {reassignable, declaration, statement: statement as Statement})));
}

function importBindings(statement: JS.Import): ModuleBinding[] {
    const clause = statement.importClause;
    if (clause === undefined) {
        return [];
    }
    const typeOnly = clause.typeOnly;
    const local = clause.name?.element;
    if (statement.initializer !== undefined) {
        // `import X = A.B` names a TypeScript namespace rather than loading a module
        const module = requiredModule(statement.initializer.element);
        return module === undefined || local?.kind !== J.Kind.Identifier ? [] :
            [{form: "import-equals", name: (local as J.Identifier).simpleName, module, typeOnly, statement}];
    }
    const specifier = statement.moduleSpecifier?.element;
    if (!isStringLiteral(specifier)) {
        return [];
    }
    const module = moduleNameOf(specifier);
    const bindings: ModuleBinding[] = [];
    if (local?.kind === J.Kind.Identifier) {
        bindings.push({form: "default", name: (local as J.Identifier).simpleName, module, typeOnly, statement});
    }
    // `import * as NS` is a `JS.Alias`, while a renamed named import nests its alias inside `NamedImports`
    const named = clause.namedBindings;
    if (named?.kind === JS.Kind.Alias) {
        const alias = (named as JS.Alias).alias;
        if (alias?.kind === J.Kind.Identifier) {
            bindings.push({form: "namespace", name: (alias as J.Identifier).simpleName, module, typeOnly, statement});
        }
    } else if (named?.kind === JS.Kind.NamedImports) {
        for (const {element} of (named as JS.NamedImports).elements.elements) {
            const bound = element?.kind === JS.Kind.ImportSpecifier ? specifierBinding(element as JS.ImportSpecifier) : undefined;
            if (bound !== undefined) {
                bindings.push({form: "named", ...bound, module, typeOnly: typeOnly || (element as JS.ImportSpecifier).importType.element,
                    statement});
            }
        }
    }
    return bindings;
}

/**
 * What one variable binds where its initializer loads a module. A pattern reads each member by its property name:
 * `{b: c} = require("m")` → `c` of member `b`; a nested `{a: {b}}`, a rest `...r` or an array `[x]` reads no one member.
 */
function declaredBindings(
    variable: J.VariableDeclarations.NamedVariable | undefined,
    where: Pick<DeclaredBinding, "reassignable" | "declaration" | "statement">
): ModuleBinding[] {
    const pattern = variable?.name;
    const initializer = variable?.initializer?.element;
    const required = requiredModule(initializer);
    if (required !== undefined) {
        return pattern?.kind === J.Kind.Identifier
            ? [{form: "require", name: (pattern as J.Identifier).simpleName, module: required, typeOnly: false, ...where}]
            : bindingNames(pattern).map(({name, member}): ModuleBinding => member === undefined
                ? {form: "require-pattern", name, module: required, typeOnly: false, ...where}
                : {form: "require-member", name, member, module: required, typeOnly: false, ...where});
    }
    const imported = dynamicallyImportedModule(initializer);
    return imported === undefined || pattern?.kind !== J.Kind.Identifier ? [] :
        [{form: "dynamic-import", name: (pattern as J.Identifier).simpleName, module: imported, typeOnly: false, ...where}];
}

/** Whether `statement` declares `const`, which a list (`const a = 1, NS = require("m")`) spells on the statement. */
function declaresConst(statement: J | undefined): boolean {
    const modifiers = statement?.kind === J.Kind.VariableDeclarations ? (statement as J.VariableDeclarations).modifiers :
        statement?.kind === JS.Kind.ScopedVariableDeclarations ? (statement as JS.ScopedVariableDeclarations).modifiers : [];
    return modifiers.some(m => m.keyword === "const");
}

/** The member a specifier imports and the name it binds it under, which are the same absent an alias. */
export function specifierBinding(specifier: JS.ImportSpecifier): { name: string; member: string } | undefined {
    if (specifier.specifier?.kind === J.Kind.Identifier) {
        const name = (specifier.specifier as J.Identifier).simpleName;
        return {name, member: name};
    }
    if (specifier.specifier?.kind !== JS.Kind.Alias) {
        return undefined;
    }
    const alias = specifier.specifier as JS.Alias;
    return alias.propertyName?.element && alias.alias?.kind === J.Kind.Identifier
        ? {name: (alias.alias as J.Identifier).simpleName, member: moduleExportName(alias.propertyName.element)}
        : undefined;
}

/**
 * The module an `await import("m")` initializer loads, which resolves to the module namespace object, the value
 * `import * as X` binds. `import.defer("m")` loads it too.
 */
function dynamicallyImportedModule(initializer: J | undefined): string | undefined {
    const awaited = initializer?.kind === JS.Kind.Await ? (initializer as JS.Await).expression : undefined;
    // `import(...)` is a keyword, not an identifier, so the parser maps it to `JS.FunctionCall`
    // rather than the `J.MethodInvocation` an ordinary call gets.
    if (awaited?.kind !== JS.Kind.FunctionCall) {
        return undefined;
    }
    const call = awaited as JS.FunctionCall;
    const callee = call.function?.element;
    if (!isImportIdentifier(callee) && !isImportDefer(callee)) {
        return undefined;
    }
    const argument = call.arguments.elements[0]?.element;
    return isStringLiteral(argument) ? moduleNameOf(argument) : undefined;
}

function isImportIdentifier(expression: J | undefined): boolean {
    return expression?.kind === J.Kind.Identifier && (expression as J.Identifier).simpleName === "import";
}

function isImportDefer(expression: J | undefined): boolean {
    return expression?.kind === J.Kind.FieldAccess && isImportIdentifier((expression as J.FieldAccess).target) &&
        (expression as J.FieldAccess).name.element.simpleName === "defer";
}

/** The module a `require('...')` initializer names, `undefined` for an initializer that is anything else. */
export function requiredModule(initializer: J | undefined): string | undefined {
    return initializer?.kind === J.Kind.MethodInvocation
        ? requiredModuleOf(initializer as J.MethodInvocation)
        : undefined;
}

/** A `require(...)` call, whatever it is passed. `obj.require('x')` selects a method rather than loading a module. */
export function isRequireCall(methodInv: J.MethodInvocation): boolean {
    return !methodInv.select && methodInv.name?.kind === J.Kind.Identifier &&
        (methodInv.name as J.Identifier).simpleName === 'require';
}

/**
 * The module a `require(...)` call loads. `obj.require('x')` selects a method rather than loading a
 * module, and a specifier that is not a string literal names none that can be read here.
 */
export function requiredModuleOf(methodInv: J.MethodInvocation): string | undefined {
    if (!isRequireCall(methodInv)) {
        return undefined;
    }
    const argument = methodInv.arguments?.elements[0]?.element;
    return argument?.kind === J.Kind.Literal && typeof (argument as J.Literal).value === 'string'
        ? moduleNameOf(argument as J.Literal)
        : undefined;
}

/**
 * The parser lifts surrogate pairs out of `valueSource` into `unicodeEscapes` and, when it does,
 * sets `value` to the quoted source (`parser.ts` `mapLiteral`), so neither field alone is the
 * module name. Reuniting them and reading the quoted text as the language does yields it for
 * either shape of literal.
 */
export function moduleNameOf(module: string | J.Literal): string {
    if (typeof module === 'string') {
        return module;
    }
    const source = module.valueSource;
    if (source === undefined) {
        return String(module.value);
    }
    let restored = '';
    let cut = 0;
    for (const escape of module.unicodeEscapes ?? []) {
        restored += source.slice(cut, escape.valueSourceIndex) + String.fromCharCode(parseInt(escape.codePoint, 16));
        cut = escape.valueSourceIndex;
    }
    restored += source.slice(cut);
    // an escape such as `\u00dc` names the character it spells
    const scanner = ts.createScanner(ts.ScriptTarget.Latest, false, ts.LanguageVariant.Standard, restored);
    const token = scanner.scan();
    if ((token === ts.SyntaxKind.StringLiteral || token === ts.SyntaxKind.NoSubstitutionTemplateLiteral) &&
        scanner.getTokenEnd() === restored.length) {
        return scanner.getTokenValue();
    }
    const quote = restored.charAt(0);
    return (quote === "'" || quote === '"' || quote === '`') && restored.endsWith(quote) && restored.length > 1
        ? restored.slice(1, -1)
        : restored;
}
