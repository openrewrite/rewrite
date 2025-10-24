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
import * as ts from "typescript";
import {Expression, J, Statement} from "../java";
import {JS} from "./tree";

const is_statements: string[] = [
    J.Kind.Assert,
    J.Kind.Assignment,
    J.Kind.AssignmentOperation,
    J.Kind.Block,
    J.Kind.Break,
    J.Kind.Case,
    J.Kind.ClassDeclaration,
    J.Kind.Continue,
    J.Kind.DoWhileLoop,
    J.Kind.Empty,
    J.Kind.EnumValueSet,
    J.Kind.Erroneous,
    J.Kind.FieldAccess,
    J.Kind.ForEachLoop,
    J.Kind.ForLoop,
    J.Kind.If,
    J.Kind.Import,
    J.Kind.Label,
    J.Kind.Lambda,
    J.Kind.MethodDeclaration,
    J.Kind.MethodInvocation,
    J.Kind.NewClass,
    J.Kind.Package,
    J.Kind.Return,
    J.Kind.Switch,
    J.Kind.Synchronized,
    J.Kind.Ternary,
    J.Kind.Throw,
    J.Kind.Try,
    J.Kind.Unary,
    J.Kind.Unknown,
    J.Kind.VariableDeclarations,
    J.Kind.WhileLoop,
    J.Kind.Yield,
    JS.Kind.ArrowFunction,
    JS.Kind.BindingElement,
    JS.Kind.Delete,
    JS.Kind.Export,
    JS.Kind.ExportAssignment,
    JS.Kind.ExportDeclaration,
    JS.Kind.ForInLoop,
    JS.Kind.ForOfLoop,
    JS.Kind.ImportAttribute,
    JS.Kind.IndexSignatureDeclaration,
    JS.Kind.AssignmentOperation,
    JS.Kind.Import,
    JS.Kind.ComputedPropertyMethodDeclaration,
    JS.Kind.MappedTypeKeysRemapping,
    JS.Kind.MappedTypeParameter,
    JS.Kind.NamespaceDeclaration,
    JS.Kind.PropertyAssignment,
    JS.Kind.ScopedVariableDeclarations,
    JS.Kind.TaggedTemplateExpression,
    JS.Kind.TemplateExpression,
    JS.Kind.TypeDeclaration,
    JS.Kind.Void,
    JS.Kind.WithStatement,
    JS.Kind.ExpressionStatement,
    JS.Kind.StatementExpression
]

const is_expressions: string[] = [
    J.Kind.AnnotatedType,
    J.Kind.Annotation,
    J.Kind.ArrayAccess,
    J.Kind.ArrayType,
    J.Kind.Assignment,
    J.Kind.AssignmentOperation,
    J.Kind.Binary,
    J.Kind.ControlParentheses,
    J.Kind.Empty,
    J.Kind.Erroneous,
    J.Kind.FieldAccess,
    J.Kind.Identifier,
    J.Kind.InstanceOf,
    J.Kind.IntersectionType,
    J.Kind.Lambda,
    J.Kind.Literal,
    J.Kind.MethodInvocation,
    J.Kind.MemberReference,
    J.Kind.NewArray,
    J.Kind.NewClass,
    J.Kind.NullableType,
    J.Kind.ParameterizedType,
    J.Kind.Parentheses,
    J.Kind.ParenthesizedTypeTree,
    J.Kind.Primitive,
    J.Kind.SwitchExpression,
    J.Kind.Ternary,
    J.Kind.TypeCast,
    J.Kind.Unary,
    J.Kind.Unknown,
    J.Kind.Wildcard,
    JS.Kind.Alias,
    JS.Kind.ArrayBindingPattern,
    JS.Kind.ArrowFunction,
    JS.Kind.Await,
    JS.Kind.BindingElement,
    JS.Kind.ConditionalType,
    JS.Kind.Delete,
    JS.Kind.ExportSpecifier,
    JS.Kind.ExpressionWithTypeArguments,
    JS.Kind.FunctionType,
    JS.Kind.ImportType,
    JS.Kind.IndexedAccessType,
    JS.Kind.IndexedAccessTypeIndexType,
    JS.Kind.InferType,
    JS.Kind.Intersection,
    JS.Kind.AssignmentOperation,
    JS.Kind.Binary,
    JS.Kind.ImportSpecifier,
    JS.Kind.LiteralType,
    JS.Kind.MappedType,
    JS.Kind.NamedExports,
    JS.Kind.NamedImports,
    JS.Kind.ObjectBindingPattern,
    JS.Kind.SatisfiesExpression,
    JS.Kind.TaggedTemplateExpression,
    JS.Kind.TemplateExpression,
    JS.Kind.Tuple,
    JS.Kind.TypeInfo,
    JS.Kind.TypeLiteral,
    JS.Kind.TypeOf,
    JS.Kind.TypeOperator,
    JS.Kind.TypePredicate,
    JS.Kind.TypeQuery,
    JS.Kind.TypeTreeExpression,
    JS.Kind.Union,
    JS.Kind.Void,
    JS.Kind.ExpressionStatement,
    JS.Kind.StatementExpression
]

export function isStatement(statement: J):  statement is Statement {
    return is_statements.includes(statement.kind);
}

export function isExpression(expression: J):  expression is Expression {
    return is_expressions.includes(expression.kind);
}

export function getNextSibling(node: ts.Node): ts.Node | undefined {
    const parent = node.parent;
    if (!parent) {
        return undefined;
    }

    const syntaxList = findContainingSyntaxList(node);

    if (syntaxList) {
        const children = syntaxList.getChildren();
        const nodeIndex = children.indexOf(node);

        if (nodeIndex === -1) {
            throw new Error('Node not found among SyntaxList\'s children.');
        }

        // If the node is the last child in the SyntaxList, recursively check the parent's next sibling
        if (nodeIndex === children.length - 1) {
            const syntaxListIndex = parent.getChildren().indexOf(syntaxList);
            if (parent.getChildCount() > syntaxListIndex + 1) {
                return parent.getChildAt(syntaxListIndex + 1);
            }
            const parentNextSibling = getNextSibling(parent);
            if (!parentNextSibling) {
                return undefined;
            }

            // Return the first child of the parent's next sibling
            const parentSyntaxList = findContainingSyntaxList(parentNextSibling);
            if (parentSyntaxList) {
                const siblings = parentSyntaxList.getChildren();
                return siblings[0] || null;
            } else {
                return parentNextSibling;
            }
        }

        // Otherwise, return the next sibling in the SyntaxList
        return children[nodeIndex + 1];
    }

    const parentChildren = parent.getChildren();
    const nodeIndex = parentChildren.indexOf(node);

    if (nodeIndex === -1) {
        throw new Error('Node not found among parent\'s children.');
    }

    // If the node is the last child, recursively check the parent's next sibling
    if (nodeIndex === parentChildren.length - 1) {
        const parentNextSibling = getNextSibling(parent);
        if (!parentNextSibling) {
            return undefined;
        }

        // Return the first child of the parent's next sibling
        return parentNextSibling.getChildCount() > 0 ? parentNextSibling.getChildAt(0) : parentNextSibling;
    }

    // Otherwise, return the next sibling
    return parentChildren[nodeIndex + 1];
}

export function getPreviousSibling(node: ts.Node): ts.Node | undefined {
    const parent = node.parent;
    if (!parent) {
        return undefined;
    }

    const syntaxList = findContainingSyntaxList(node);

    if (syntaxList) {
        const children = syntaxList.getChildren();
        const nodeIndex = children.indexOf(node);

        if (nodeIndex === -1) {
            throw new Error('Node not found among SyntaxList\'s children.');
        }

        // If the node is the first child in the SyntaxList, recursively check the parent's previous sibling
        if (nodeIndex === 0) {
            const parentPreviousSibling = getPreviousSibling(parent);
            if (!parentPreviousSibling) {
                return undefined;
            }

            // Return the last child of the parent's previous sibling
            const parentSyntaxList = findContainingSyntaxList(parentPreviousSibling);
            if (parentSyntaxList) {
                const siblings = parentSyntaxList.getChildren();
                return siblings[siblings.length - 1] || null;
            } else {
                return parentPreviousSibling;
            }
        }

        // Otherwise, return the previous sibling in the SyntaxList
        return children[nodeIndex - 1];
    }

    const parentChildren = parent.getChildren();
    const nodeIndex = parentChildren.indexOf(node);

    if (nodeIndex === -1) {
        throw new Error('Node not found among parent\'s children.');
    }

    // If the node is the first child, recursively check the parent's previous sibling
    if (nodeIndex === 0) {
        const parentPreviousSibling = getPreviousSibling(parent);
        if (!parentPreviousSibling) {
            return undefined;
        }

        // Return the last child of the parent's previous sibling
        return parentPreviousSibling.getChildCount() > 0 ? parentPreviousSibling.getLastToken()! : parentPreviousSibling;
    }

    // Otherwise, return the previous sibling
    return parentChildren[nodeIndex - 1];
}

function findContainingSyntaxList(node: ts.Node): ts.SyntaxList | undefined {
    const parent = node.parent;
    if (!parent) {
        return undefined;
    }

    const children = parent.getChildren();
    for (const child of children) {
        if (child.kind == ts.SyntaxKind.SyntaxList && child.getChildren().includes(node)) {
            return child as ts.SyntaxList;
        }
    }

    return undefined;
}

export type TextSpan = [number, number];

export function compareTextSpans(span1: TextSpan, span2: TextSpan) {
    // First, compare the first elements
    if (span1[0] < span2[0]) {
        return -1;
    }
    if (span1[0] > span2[0]) {
        return 1;
    }

    // If the first elements are equal, compare the second elements
    if (span1[1] < span2[1]) {
        return -1;
    }
    if (span1[1] > span2[1]) {
        return 1;
    }

    // If both elements are equal, the tuples are considered equal
    return 0;
}

export function binarySearch<T>(arr: T[], target: T, compare: (a: T, b: T) => number) {
    let low = 0;
    let high = arr.length - 1;

    while (low <= high) {
        const mid = Math.floor((low + high) / 2);

        const comparison = compare(arr[mid], target);

        if (comparison === 0) {
            return mid;  // Element found, return index
        } else if (comparison < 0) {
            low = mid + 1;  // Search the right half
        } else {
            high = mid - 1;  // Search the left half
        }
    }
    return ~low;  // Element not found, return bitwise complement of the insertion point
}

export function hasFlowAnnotation(sourceFile: ts.SourceFile) {
    if (sourceFile.fileName.endsWith('.js') || sourceFile.fileName.endsWith('.jsx')) {
        const comments = sourceFile.getFullText().match(/\/\*[\s\S]*?\*\/|\/\/.*(?=[\r\n])/g);
        if (comments) {
            return comments.some(comment => comment.includes("@flow"));
        }
    }
    return false;
}

export function checkSyntaxErrors(program: ts.Program, sourceFile: ts.SourceFile) {
    const diagnostics = ts.getPreEmitDiagnostics(program, sourceFile);
    // checking Parsing and Syntax Errors
    let syntaxErrors : [errorMsg: string, errorCode: number][] = [];
    if (diagnostics.length > 0) {
        const errors = diagnostics.filter(d =>  (d.category === ts.DiagnosticCategory.Error) && isCriticalDiagnostic(d.code, sourceFile));
        if (errors.length > 0) {
            syntaxErrors = errors.map(e => {
                let errorMsg;
                if (e.file) {
                    let {line, character} = ts.getLineAndCharacterOfPosition(e.file, e.start!);
                    let message = ts.flattenDiagnosticMessageText(e.messageText, "\n");
                    errorMsg = `(${line + 1},${character + 1}): ${message}`;
                } else {
                    errorMsg = ts.flattenDiagnosticMessageText(e.messageText, "\n");
                }
                return [errorMsg, e.code];
            });
        }
    }
    return syntaxErrors;
}

const additionalCriticalCodes = new Set([
    // Syntax errors
    17019, // "'{0}' at the end of a type is not valid TypeScript syntax. Did you mean to write '{1}'?"
    17020, // "'{0}' at the start of a type is not valid TypeScript syntax. Did you mean to write '{1}'?"

    // Other critical errors
]);

// errors code description available at https://github.com/microsoft/TypeScript/blob/main/src/compiler/diagnosticMessages.json
const excludedCodes = new Set([
    1039, // Initializers are not allowed in ambient contexts.
    1064, // The return type of an async function or method must be the global Promise<T> type. Did you mean to write 'Promise<{0}>'?
    1107, // Jump target cannot cross function boundary.
    1111, // Private field '{0}' must be declared in an enclosing class.
    1117, // An object literal cannot have multiple properties with the same name.
    1155, // '{0}' declarations must be initialized.
    1166, // A computed property name in a class property declaration must have a simple literal type or a 'unique symbol' type.
    1170, // A computed property name in a type literal must refer to an expression whose type is a literal type or a 'unique symbol' type.
    1183, // An implementation cannot be declared in ambient contexts.
    1203, // Export assignment cannot be used when targeting ECMAScript modules. Consider using 'export default' or another module format instead.
    1207, // Decorators cannot be applied to multiple get/set accessors of the same name.
    1215, // Invalid use of '{0}'. Modules are automatically in strict mode.
    1238, // Unable to resolve signature of class decorator when called as an expression.
    1239, // Unable to resolve signature of parameter decorator when called as an expression.
    1240, // Unable to resolve signature of property decorator when called as an expression.
    1241, // Unable to resolve signature of method decorator when called as an expression.
    1244, // Abstract methods can only appear within an abstract class.
    1250, // Function declarations are not allowed inside blocks in strict mode when targeting 'ES5'.
    1251, // Function declarations are not allowed inside blocks in strict mode when targeting 'ES5'. Class definitions are automatically in strict mode.
    1252, // Function declarations are not allowed inside blocks in strict mode when targeting 'ES5'. Modules are automatically in strict mode.
    1253, // Abstract properties can only appear within an abstract class.
    1254, // A 'const' initializer in an ambient context must be a string or numeric literal or literal enum reference.
    1308, // 'await' expressions are only allowed within async functions and at the top levels of modules.
    1314, // Global module exports may only appear in module files.
    1315, // Global module exports may only appear in declaration files.
    1324, // Dynamic imports only support a second argument when the '--module' option is set to 'esnext', 'node16', 'node18', 'node20', 'nodenext', or 'preserve'.
    1329, // '{0}' accepts too few arguments to be used as a decorator here. Did you mean to call it first and write '@{0}()'?
    1335, // 'unique symbol' types are not allowed here.
    1338, // 'infer' declarations are only permitted in the 'extends' clause of a conditional type.
    1340, // Module '{0}' does not refer to a type, but is used as a type here. Did you mean 'typeof import('{0}')'?
    1343, // The 'import.meta' meta-property is only allowed when the '--module' option is 'es2020', 'es2022', 'esnext', 'system', 'node16', 'node18', 'node20', or 'nodenext'.
    1344, // 'A label is not allowed here.
    1345, // An expression of type 'void' cannot be tested for truthiness.
    1355, // A 'const' assertion can only be applied to references to enum members, or string, number, boolean, array, or object literals.
    1360, // Type '{0}' does not satisfy the expected type '{1}'.
    1375, // 'await' expressions are only allowed at the top level of a file when that file is a module, but this file has no imports or exports. Consider adding an empty 'export {}' to make this file a module.
    1378, // Top-level 'await' expressions are only allowed when the 'module' option is set to 'es2022', 'esnext', 'system', 'node16', 'node18', 'node20', 'nodenext', or 'preserve', and the 'target' option is set to 'es2017' or higher.
    1432, // Top-level 'for await' loops are only allowed when the 'module' option is set to 'es2022', 'esnext', 'system', 'node16', 'node18', 'node20', 'nodenext', or 'preserve', and the 'target' option is set to 'es2017' or higher.
]);

// Errors to exclude only for JavaScript files (.js, .jsx, .mjs, .cjs)
// TypeScript files (.ts, .tsx, .mts, .cts) should still report these as errors
const jsOnlyExcludedCodes = new Set([
    1101, // 'with' statements are not allowed in strict mode.
    1121, // Octal literals are not allowed. Use the syntax '{0}'.
    1125, // Hexadecimal digit expected.
    1487, // Octal escape sequences are not allowed. Use the syntax '{0}'.
]);

function isCriticalDiagnostic(code: number, sourceFile: ts.SourceFile): boolean {
    // Check if this error should be excluded for JavaScript files
    if (jsOnlyExcludedCodes.has(code)) {
        const fileName = sourceFile.fileName.toLowerCase();
        const isJavaScript = fileName.endsWith('.js') || fileName.endsWith('.jsx') ||
                           fileName.endsWith('.mjs') || fileName.endsWith('.cjs');
        if (isJavaScript) {
            return false; // Not critical for JS files
        }
    }

    return (code > 1000 && code < 2000 && !excludedCodes.has(code)) || additionalCriticalCodes.has(code);
}

export function isValidSurrogateRange(unicodeString: string): boolean {
    const matches = unicodeString.match(/(?<!\\)\\u([a-fA-F0-9]{4})/g);

    if (!matches) {
        return true;
    }

    const codes = matches.map(m => {
        const codePointStr = m.slice(2);
        const codePoint = parseInt(codePointStr, 16);
        return codePoint;
    });

    const isHighSurrogate = (charCode: number): boolean => charCode >= 0xD800 && charCode <= 0xDBFF;
    const isLowSurrogate = (charCode: number): boolean => charCode >= 0xDC00 && charCode <= 0xDFFF;

    for (let i = 0; i < codes.length; i++) {
        const c = codes[i];

        if (isHighSurrogate(c)) {
            // Ensure that the high surrogate is followed by a valid low surrogate
            if (i + 1 >= codes.length || !isLowSurrogate(codes[i + 1])) {
                return false; // Invalid high surrogate or no low surrogate after it
            }
            i++; // Skip the low surrogate
        } else if (isLowSurrogate(c)) {
            return false; // Lone low surrogate (not preceded by a high surrogate)
        }
    }
    return true;
}
