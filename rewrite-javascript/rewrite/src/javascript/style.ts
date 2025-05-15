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

import {NamedStyles, Style} from "../style";
import {randomId} from "../uuid";
import {Tree} from "../tree";
import {MarkersKind} from "../markers";

export const JavaScriptStyles = {
    IntelliJ: "org.openrewrite.javascript.style.IntelliJ",
}

export const StyleKind = {
    SpacesStyle: "org.openrewrite.javascript.style.SpacesStyle",
    WrappingAndBracesStyle: "org.openrewrite.java.style.WrappingAndBracesStyle",
    BlankLinesStyle: "org.openrewrite.javascript.style.BlankLinesStyle"
} as const;

export const SpacesStyleDetailKind = {
    SpacesStyleBeforeParentheses: "org.openrewrite.javascript.style.SpacesStyle$BeforeParentheses",
    SpacesStyleAroundOperators: "org.openrewrite.javascript.style.SpacesStyle$AroundOperators",
    SpacesStyleBeforeLeftBrace: "org.openrewrite.javascript.style.SpacesStyle$BeforeLeftBrace",
    SpacesStyleBeforeKeywords: "org.openrewrite.javascript.style.SpacesStyle$BeforeKeywords",
    SpacesStyleWithin: "org.openrewrite.javascript.style.SpacesStyle$Within",
    SpacesStyleTernaryOperator: "org.openrewrite.javascript.style.SpacesStyle$TernaryOperator",
    SpacesStyleOther: "org.openrewrite.javascript.style.SpacesStyle$Other",
} as const;

export interface SpacesStyle extends Style {
    readonly kind: typeof StyleKind.SpacesStyle;
    readonly beforeParentheses: SpacesStyle.BeforeParentheses;
    readonly aroundOperators: SpacesStyle.AroundOperators;
    readonly beforeLeftBrace: SpacesStyle.BeforeLeftBrace;
    readonly beforeKeywords: SpacesStyle.BeforeKeywords;
    readonly within: SpacesStyle.Within;
    readonly ternaryOperator: SpacesStyle.TernaryOperator;
    readonly other: SpacesStyle.Other;
}

export namespace SpacesStyle {
    export interface BeforeParentheses {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleBeforeParentheses;
        readonly functionDeclarationParentheses: boolean;
        readonly functionCallParentheses: boolean;
        readonly ifParentheses: boolean;
        readonly forParentheses: boolean;
        readonly whileParentheses: boolean;
        readonly switchParentheses: boolean;
        readonly catchParentheses: boolean;
        readonly inFunctionCallExpression: boolean;
        readonly inAsyncArrowFunction: boolean;
    }

    export interface AroundOperators {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleAroundOperators;
        readonly assignment: boolean;
        readonly logical: boolean;
        readonly equality: boolean;
        readonly relational: boolean;
        readonly bitwise: boolean;
        readonly additive: boolean;
        readonly multiplicative: boolean;
        readonly shift: boolean;
        readonly unary: boolean;
        readonly arrowFunction: boolean;
        readonly beforeUnaryNotAndNotNull: boolean;
        readonly afterUnaryNotAndNotNull: boolean;
    }

    export interface BeforeLeftBrace {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleBeforeLeftBrace;
        readonly functionLeftBrace: boolean;
        readonly ifLeftBrace: boolean;
        readonly elseLeftBrace: boolean;
        readonly forLeftBrace: boolean;
        readonly whileLeftBrace: boolean;
        readonly doLeftBrace: boolean;
        readonly switchLeftBrace: boolean;
        readonly tryLeftBrace: boolean;
        readonly catchLeftBrace: boolean;
        readonly finallyLeftBrace: boolean;
        readonly classInterfaceModuleLeftBrace: boolean;
    }

    export interface BeforeKeywords {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleBeforeKeywords;
        readonly elseKeyword: boolean;
        readonly whileKeyword: boolean;
        readonly catchKeyword: boolean;
        readonly finallyKeyword: boolean;
    }

    export interface Within {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleWithin;
        readonly indexAccessBrackets: boolean;
        readonly groupingParentheses: boolean;
        readonly functionDeclarationParentheses: boolean;
        readonly functionCallParentheses: boolean;
        readonly ifParentheses: boolean;
        readonly forParentheses: boolean;
        readonly whileParentheses: boolean;
        readonly switchParentheses: boolean;
        readonly catchParentheses: boolean;
        readonly objectLiteralBraces: boolean;
        readonly es6ImportExportBraces: boolean;
        readonly arrayBrackets: boolean;
        readonly interpolationExpressions: boolean;
        readonly objectLiteralTypeBraces: boolean;
        readonly unionAndIntersectionTypes: boolean;
        readonly typeAssertions: boolean;
    }

    export interface TernaryOperator {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleTernaryOperator;
        readonly beforeQuestionMark: boolean;
        readonly afterQuestionMark: boolean;
        readonly beforeColon: boolean;
        readonly afterColon: boolean;
    }

    export interface Other {
        readonly kind: typeof SpacesStyleDetailKind.SpacesStyleOther;
        readonly beforeComma: boolean;
        readonly afterComma: boolean;
        readonly beforeForSemicolon: boolean;
        readonly beforePropertyNameValueSeparator: boolean;
        readonly afterPropertyNameValueSeparator: boolean;
        readonly afterVarArgInRestOrSpread: boolean;
        readonly beforeAsteriskInGenerator: boolean;
        readonly afterAsteriskInGenerator: boolean;
        readonly beforeTypeReferenceColon: boolean;
        readonly afterTypeReferenceColon: boolean;
    }
}

export const WrappingAndBracesStyleDetailKind = {
    WrappingAndBracesStyleIfStatement: "org.openrewrite.java.style.WrappingAndBracesStyle$IfStatement",
} as const;

export interface WrappingAndBracesStyle extends Style {
    // TODO add more flags; this is what we have in Java, but IntelliJ has way more settings
    readonly kind: typeof StyleKind.WrappingAndBracesStyle;
    readonly ifStatement: WrappingAndBracesStyle.IfStatement;
}

export interface BlankLinesStyle extends Style {
    readonly kind: "org.openrewrite.javascript.style.BlankLinesStyle";
    readonly keepMaximum: BlankLinesStyle.KeepMaximum;
    readonly minimum: BlankLinesStyle.Minimum;
}

export namespace BlankLinesStyle {
    export interface KeepMaximum {
        readonly inCode: number;
    }

    export interface Minimum {
        readonly afterImports: number;
        readonly aroundClass: number;
        readonly aroundFieldInInterface?: number;
        readonly aroundField: number;
        readonly aroundMethodInInterface?: number;
        readonly aroundMethod: number;
        readonly aroundFunction: number;
    }
}

export namespace WrappingAndBracesStyle {
    export interface IfStatement {
        readonly kind: typeof WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleIfStatement;
        readonly elseOnNewLine: boolean;
    }
}

export namespace IntelliJ {
    export namespace JavaScript {
        export const defaults: NamedStyles<typeof JavaScriptStyles.IntelliJ> = {
            kind: JavaScriptStyles.IntelliJ,
            id: randomId(),
            name: "org.openrewrite.javascript.IntelliJ",
            displayName: "IntelliJ IDEA",
            description: "Default IntelliJ IDEA code style.",
            tags: [],
            styles: [spaces(), wrappingAndBraces(), blankLines()],
        }

        export function spaces(): SpacesStyle {
            return {
                kind: StyleKind.SpacesStyle,
                beforeParentheses: {
                    kind: SpacesStyleDetailKind.SpacesStyleBeforeParentheses,
                    functionDeclarationParentheses: false,
                    functionCallParentheses: false,
                    ifParentheses: true,
                    forParentheses: true,
                    whileParentheses: true,
                    switchParentheses: true,
                    catchParentheses: true,
                    inFunctionCallExpression: true,
                    inAsyncArrowFunction: true
                },
                aroundOperators: {
                    kind: SpacesStyleDetailKind.SpacesStyleAroundOperators,
                    assignment: true,
                    logical: true,
                    equality: true,
                    relational: true,
                    bitwise: true,
                    additive: true,
                    multiplicative: true,
                    shift: true,
                    unary: true,
                    arrowFunction: true,
                    beforeUnaryNotAndNotNull: false,
                    afterUnaryNotAndNotNull: false
                },
                beforeLeftBrace: {
                    kind: SpacesStyleDetailKind.SpacesStyleBeforeLeftBrace,
                    functionLeftBrace: true,
                    ifLeftBrace: true,
                    elseLeftBrace: true,
                    forLeftBrace: true,
                    whileLeftBrace: true,
                    doLeftBrace: true,
                    switchLeftBrace: true,
                    tryLeftBrace: true,
                    catchLeftBrace: true,
                    finallyLeftBrace: true,
                    classInterfaceModuleLeftBrace: true
                },
                beforeKeywords: {
                    kind: SpacesStyleDetailKind.SpacesStyleBeforeKeywords,
                    elseKeyword: true,
                    whileKeyword: true,
                    catchKeyword: true,
                    finallyKeyword: true
                },
                within: {
                    kind: SpacesStyleDetailKind.SpacesStyleWithin,
                    indexAccessBrackets: false,
                    groupingParentheses: false,
                    functionDeclarationParentheses: false,
                    functionCallParentheses: false,
                    ifParentheses: false,
                    forParentheses: false,
                    whileParentheses: false,
                    switchParentheses: false,
                    catchParentheses: false,
                    objectLiteralBraces: false,
                    es6ImportExportBraces: false,
                    arrayBrackets: false,
                    interpolationExpressions: false,
                    objectLiteralTypeBraces: false,
                    unionAndIntersectionTypes: false,
                    typeAssertions: false
                },
                ternaryOperator: {
                    kind: SpacesStyleDetailKind.SpacesStyleTernaryOperator,
                    beforeQuestionMark: true,
                    afterQuestionMark: true,
                    beforeColon: true,
                    afterColon: true
                },
                other: {
                    kind: SpacesStyleDetailKind.SpacesStyleOther,
                    beforeComma: false,
                    afterComma: true,
                    beforeForSemicolon: false,
                    beforePropertyNameValueSeparator: false,
                    afterPropertyNameValueSeparator: true,
                    afterVarArgInRestOrSpread: false,
                    beforeAsteriskInGenerator: false,
                    afterAsteriskInGenerator: true,
                    beforeTypeReferenceColon: false,
                    afterTypeReferenceColon: false
                },
            };
        }

        export function wrappingAndBraces(): WrappingAndBracesStyle {
            return {
                kind: StyleKind.WrappingAndBracesStyle,
                ifStatement: {
                    kind: WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleIfStatement,
                    elseOnNewLine: false
                }
            };
        }

        export function blankLines(): BlankLinesStyle {
            return {
                kind: StyleKind.BlankLinesStyle,
                keepMaximum: {
                    inCode: 2
                },
                minimum: {
                    afterImports: 1,
                    aroundClass: 1,
                    aroundField: 0,
                    aroundMethod: 1,
                    aroundFunction: 1
                }
            };
        }
    }

    export namespace TypeScript {
        export const defaults: NamedStyles<typeof JavaScriptStyles.IntelliJ> = {
            kind: JavaScriptStyles.IntelliJ,
            id: randomId(),
            name: "org.openrewrite.javascript.IntelliJ",
            displayName: "IntelliJ IDEA",
            description: "Default IntelliJ IDEA code style.",
            tags: [],
            styles: [spaces(), wrappingAndBraces(), blankLines()],
        }

        export function spaces(): SpacesStyle {
            return {
                kind: StyleKind.SpacesStyle,
                beforeParentheses: {
                    kind: SpacesStyleDetailKind.SpacesStyleBeforeParentheses,
                    functionDeclarationParentheses: false,
                    functionCallParentheses: false,
                    ifParentheses: true,
                    forParentheses: true,
                    whileParentheses: true,
                    switchParentheses: true,
                    catchParentheses: true,
                    inFunctionCallExpression: true,
                    inAsyncArrowFunction: true
                },
                aroundOperators: {
                    kind: SpacesStyleDetailKind.SpacesStyleAroundOperators,
                    assignment: true,
                    logical: true,
                    equality: true,
                    relational: true,
                    bitwise: true,
                    additive: true,
                    multiplicative: true,
                    shift: true,
                    unary: true,
                    arrowFunction: true,
                    beforeUnaryNotAndNotNull: false,
                    afterUnaryNotAndNotNull: false
                },
                beforeLeftBrace: {
                    kind: SpacesStyleDetailKind.SpacesStyleBeforeLeftBrace,
                    functionLeftBrace: true,
                    ifLeftBrace: true,
                    elseLeftBrace: true,
                    forLeftBrace: true,
                    whileLeftBrace: true,
                    doLeftBrace: true,
                    switchLeftBrace: true,
                    tryLeftBrace: true,
                    catchLeftBrace: true,
                    finallyLeftBrace: true,
                    classInterfaceModuleLeftBrace: false
                },
                beforeKeywords: {
                    kind: SpacesStyleDetailKind.SpacesStyleBeforeKeywords,
                    elseKeyword: true,
                    whileKeyword: true,
                    catchKeyword: true,
                    finallyKeyword: true
                },
                within: {
                    kind: SpacesStyleDetailKind.SpacesStyleWithin,
                    indexAccessBrackets: false,
                    groupingParentheses: false,
                    functionDeclarationParentheses: false,
                    functionCallParentheses: false,
                    ifParentheses: false,
                    forParentheses: false,
                    whileParentheses: false,
                    switchParentheses: false,
                    catchParentheses: false,
                    objectLiteralBraces: false,
                    es6ImportExportBraces: false,
                    arrayBrackets: false,
                    interpolationExpressions: false,
                    objectLiteralTypeBraces: true,
                    unionAndIntersectionTypes: true,
                    typeAssertions: false
                },
                ternaryOperator: {
                    kind: SpacesStyleDetailKind.SpacesStyleTernaryOperator,
                    beforeQuestionMark: true,
                    afterQuestionMark: true,
                    beforeColon: true,
                    afterColon: true
                },
                other: {
                    kind: SpacesStyleDetailKind.SpacesStyleOther,
                    beforeComma: false,
                    afterComma: true,
                    beforeForSemicolon: false,
                    beforePropertyNameValueSeparator: false,
                    afterPropertyNameValueSeparator: true,
                    afterVarArgInRestOrSpread: false,
                    beforeAsteriskInGenerator: false,
                    afterAsteriskInGenerator: true,
                    beforeTypeReferenceColon: false,
                    afterTypeReferenceColon: true
                },
            };
        }

        export function wrappingAndBraces(): WrappingAndBracesStyle {
            return {
                kind: StyleKind.WrappingAndBracesStyle,
                ifStatement: {
                    kind: WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleIfStatement,
                    elseOnNewLine: false
                }
            };
        }

        export function blankLines(): BlankLinesStyle {
            return {
                kind: StyleKind.BlankLinesStyle,
                keepMaximum: {
                    inCode: 2
                },
                minimum: {
                    afterImports: 1,
                    aroundClass: 1,
                    aroundFieldInInterface: 0,
                    aroundField: 0,
                    aroundMethodInInterface: 1,
                    aroundMethod: 1,
                    aroundFunction: 1
                }
            };
        }
    }
}

export function styleFromSourceFile(styleKind: string, sourceFile: Tree): Style | undefined {
    const namedStyles = sourceFile.markers.markers.filter(marker => marker.kind === MarkersKind.NamedStyles) as NamedStyles[];
    const candidate = namedStyles.map(namedStyle => namedStyle.styles).flat()
        .find(style => style.kind === styleKind)
    if (candidate) {
        return candidate;
    }
    return IntelliJ.TypeScript.defaults.styles.find(style => style.kind === styleKind) as Style;
}
