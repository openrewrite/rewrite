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
        const errors = diagnostics.filter(d =>  (d.category === ts.DiagnosticCategory.Error) && isCriticalDiagnostic(d.code));
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
const excludedCodes = new Set([1039, 1064, 1101, 1107, 1111, 1155, 1166, 1170, 1183, 1203, 1207, 1215, 1238, 1239, 1240, 1241, 1244, 1250,
    1251, 1252, 1253, 1254, 1308, 1314, 1315, 1324, 1329, 1335, 1338, 1340, 1343, 1344, 1345, 1355, 1360, 1375, 1378, 1432]);

function isCriticalDiagnostic(code: number): boolean {
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
