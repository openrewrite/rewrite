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
    WrappingAndBracesStyle: "org.openrewrite.javascript.style.WrappingAndBracesStyle",
    BlankLinesStyle: "org.openrewrite.javascript.style.BlankLinesStyle",
    TabsAndIndentsStyle: "org.openrewrite.javascript.style.TabsAndIndentsStyle",
    PrettierStyle: "org.openrewrite.javascript.style.PrettierStyle"
} as const;

/**
 * Style for Prettier-based formatting.
 *
 * This implements NamedStyles so it can be both:
 * - A marker attached to source files (detected from project's .prettierrc)
 * - A style that can be passed via the styles parameter or found via getStyle
 *
 * When this style is present, AutoformatVisitor will use Prettier for formatting
 * instead of the built-in formatting visitors.
 */
export class PrettierStyle implements NamedStyles<typeof StyleKind.PrettierStyle> {
    readonly kind = StyleKind.PrettierStyle;
    readonly name = "org.openrewrite.javascript.Prettier";
    readonly displayName = "Prettier";
    readonly description = "Prettier code formatter configuration.";
    readonly tags: string[] = [];
    readonly styles: Style[] = [];

    constructor(
        readonly id: string,
        /**
         * The resolved Prettier options for this file (with overrides applied).
         */
        readonly config: Record<string, unknown>,
        /**
         * The Prettier version from the project's package.json.
         * At formatting time, this version of Prettier will be loaded dynamically
         * to ensure consistent formatting.
         */
        readonly prettierVersion?: string,
        /**
         * Whether this file is ignored by .prettierignore.
         * When true, Prettier formatting should be skipped for this file.
         */
        readonly ignored: boolean = false
    ) {}
}

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
    WrappingAndBracesStyleKeepWhenReformatting: "org.openrewrite.javascript.style.WrappingAndBracesStyle$KeepWhenReformatting",
} as const;

export interface WrappingAndBracesStyle extends Style {
    // TODO add more flags; this is what we have in Java, but IntelliJ has way more settings
    readonly kind: typeof StyleKind.WrappingAndBracesStyle;
    readonly ifStatement: WrappingAndBracesStyle.IfStatement;
    readonly keepWhenReformatting: WrappingAndBracesStyle.KeepWhenReformatting;
}

export namespace WrappingAndBracesStyle {
    export interface IfStatement {
        readonly kind: typeof WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleIfStatement;
        readonly elseOnNewLine: boolean;
    }

    export interface KeepWhenReformatting {
        readonly kind: typeof WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleKeepWhenReformatting;
        readonly simpleBlocksInOneLine: boolean;
        readonly simpleMethodsInOneLine: boolean;
    }
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

export interface TabsAndIndentsStyle extends Style {
    readonly kind: typeof StyleKind.TabsAndIndentsStyle;
    readonly useTabCharacter: boolean;
    readonly tabSize: number;
    readonly indentSize: number;
    readonly continuationIndent: number;
    readonly keepIndentsOnEmptyLines: boolean;
    readonly indentChainedMethods: boolean;
    readonly indentAllChainedCallsInAGroup: boolean;
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
            styles: [spaces(), wrappingAndBraces(), blankLines(), tabsAndIndents()],
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
                    unary: false,
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
                },
                keepWhenReformatting: {
                    kind: WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleKeepWhenReformatting,
                    simpleBlocksInOneLine: false,
                    simpleMethodsInOneLine: false
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

        export function tabsAndIndents(): TabsAndIndentsStyle {
            return {
                kind: StyleKind.TabsAndIndentsStyle,
                useTabCharacter: false,
                tabSize: 4,
                indentSize: 4,
                continuationIndent: 4,
                keepIndentsOnEmptyLines: false,
                indentChainedMethods: true,
                indentAllChainedCallsInAGroup: false
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
            styles: [spaces(), wrappingAndBraces(), blankLines(), tabsAndIndents()],
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
                    unary: false,
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
                },
                keepWhenReformatting: {
                    kind: WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleKeepWhenReformatting,
                    simpleBlocksInOneLine: false,
                    simpleMethodsInOneLine: false
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

        export function tabsAndIndents(): TabsAndIndentsStyle {
            return {
                kind: StyleKind.TabsAndIndentsStyle,
                useTabCharacter: false,
                tabSize: 4,
                indentSize: 4,
                continuationIndent: 4,
                keepIndentsOnEmptyLines: false,
                indentChainedMethods: true,
                indentAllChainedCallsInAGroup: false
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

/**
 * Get a style by kind, with passed-in styles taking precedence over source file styles.
 * Falls back to IntelliJ defaults if no style is found.
 *
 * @param styleKind The kind of style to retrieve
 * @param sourceFile The source file to check for styles
 * @param styles Optional array of NamedStyles that take precedence over source file styles
 */
export function getStyle(styleKind: string, sourceFile: Tree, styles?: NamedStyles<string>[]): Style | undefined {
    // First check passed-in styles (highest precedence)
    if (styles) {
        for (const namedStyle of styles) {
            const found = namedStyle.styles.find(s => s.kind === styleKind);
            if (found) {
                return found;
            }
        }
    }

    // Then check source file markers
    const namedStyles = sourceFile.markers.markers.filter(marker => marker.kind === MarkersKind.NamedStyles) as NamedStyles[];
    for (const namedStyle of namedStyles) {
        const found = namedStyle.styles.find(s => s.kind === styleKind);
        if (found) {
            return found;
        }
    }

    // Fall back to defaults
    return IntelliJ.TypeScript.defaults.styles.find(style => style.kind === styleKind) as Style;
}
