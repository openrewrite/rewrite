/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const StyleKind = {
    SpacesStyle: "org.openrewrite.style.SpacesStyle"
} as const

export interface Style {
    readonly kind: string
}

export class SpacesStyle implements Style {
    readonly kind = StyleKind.SpacesStyle;
    constructor(
        readonly beforeParentheses: SpacesStyle.BeforeParentheses,
        readonly aroundOperators: SpacesStyle.AroundOperators,
        readonly beforeLeftBrace: SpacesStyle.BeforeLeftBrace,
        readonly beforeKeywords: SpacesStyle.BeforeKeywords,
        readonly within: SpacesStyle.Within,
        readonly ternaryOperator: SpacesStyle.TernaryOperator,
        readonly other: SpacesStyle.Other
    ) {}
}

export namespace SpacesStyle {
    export class BeforeParentheses {
        constructor(
            public readonly functionDeclarationParentheses: boolean,
            public readonly functionCallParentheses: boolean,
            public readonly ifParentheses: boolean,
            public readonly forParentheses: boolean,
            public readonly whileParentheses: boolean,
            public readonly switchParentheses: boolean,
            public readonly catchParentheses: boolean,
            public readonly inFunctionCallExpression: boolean,
            public readonly inAsyncArrowFunction: boolean
        ) {
        }
    }

    export class AroundOperators {
        constructor(
            public readonly assignment: boolean,
            public readonly logical: boolean,
            public readonly equality: boolean,
            public readonly relational: boolean,
            public readonly bitwise: boolean,
            public readonly additive: boolean,
            public readonly multiplicative: boolean,
            public readonly shift: boolean,
            public readonly unary: boolean,
            public readonly arrowFunction: boolean,
            public readonly beforeUnaryNotAndNotNull: boolean,
            public readonly afterUnaryNotAndNotNull: boolean
        ) {
        }
    }

    export class BeforeLeftBrace {
        constructor(
            public readonly functionLeftBrace: boolean,
            public readonly ifLeftBrace: boolean,
            public readonly elseLeftBrace: boolean,
            public readonly forLeftBrace: boolean,
            public readonly whileLeftBrace: boolean,
            public readonly doLeftBrace: boolean,
            public readonly switchLeftBrace: boolean,
            public readonly tryLeftBrace: boolean,
            public readonly catchLeftBrace: boolean,
            public readonly finallyLeftBrace: boolean,
            public readonly classInterfaceModuleLeftBrace: boolean
        ) {
        }
    }

    export class BeforeKeywords {
        constructor(
            public readonly elseKeyword: boolean,
            public readonly whileKeyword: boolean,
            public readonly catchKeyword: boolean,
            public readonly finallyKeyword: boolean
        ) {
        }
    }

    export class Within {
        constructor(
            public readonly indexAccessBrackets: boolean,
            public readonly groupingParentheses: boolean,
            public readonly functionDeclarationParentheses: boolean,
            public readonly functionCallParentheses: boolean,
            public readonly ifParentheses: boolean,
            public readonly forParentheses: boolean,
            public readonly whileParentheses: boolean,
            public readonly switchParentheses: boolean,
            public readonly catchParentheses: boolean,
            public readonly objectLiteralBraces: boolean,
            public readonly es6ImportExportBraces: boolean,
            public readonly arrayBrackets: boolean,
            public readonly interpolationExpressions: boolean,
            public readonly objectLiteralTypeBraces: boolean,
            public readonly unionAndIntersectionTypes: boolean,
            public readonly typeAssertions: boolean
        ) {
        }
    }

    export class TernaryOperator {
        constructor(
            public readonly beforeQuestionMark: boolean,
            public readonly afterQuestionMark: boolean,
            public readonly beforeColon: boolean,
            public readonly afterColon: boolean
        ) {
        }
    }

    export class Other {
        constructor(
            public readonly beforeComma: boolean,
            public readonly afterComma: boolean,
            public readonly beforeForSemicolon: boolean,
            public readonly beforePropertyNameValueSeparator: boolean,
            public readonly afterPropertyNameValueSeparator: boolean,
            public readonly afterVarArgInRestOrSpread: boolean,
            public readonly beforeAsteriskInGenerator: boolean,
            public readonly afterAsteriskInGenerator: boolean,
            public readonly beforeTypeReferenceColon: boolean,
            public readonly afterTypeReferenceColon: boolean
        ) {
        }
    }
}


export class IntelliJ {
    static JavaScript = class {
        static spaces(): SpacesStyle {
            return new SpacesStyle(
                new SpacesStyle.BeforeParentheses(false, false, true, true, true, true, true, true, true),
                new SpacesStyle.AroundOperators(true, true, true, true, true, true, true, true, false, true, false, false),
                new SpacesStyle.BeforeLeftBrace(true, true, true, true, true, true, true, true, true, true, true),
                new SpacesStyle.BeforeKeywords(true, true, true, true),
                new SpacesStyle.Within(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),
                new SpacesStyle.TernaryOperator(true, true, true, true),
                new SpacesStyle.Other(false, true, false, false, true, false, false, true, false, false)
            );
        }
    };

    static TypeScript = class {
        static spaces(): SpacesStyle {
            return new SpacesStyle(
                new SpacesStyle.BeforeParentheses(false, false, true, true, true, true, true, true, true),
                new SpacesStyle.AroundOperators(true, true, true, true, true, true, true, true, false, true, false, false),
                new SpacesStyle.BeforeLeftBrace(true, true, true, true, true, true, true, true, true, true, false),
                new SpacesStyle.BeforeKeywords(true, true, true, true),
                new SpacesStyle.Within(false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false),
                new SpacesStyle.TernaryOperator(true, true, true, true),
                new SpacesStyle.Other(false, true, false, false, true, false, false, true, false, true)
            );
        }
    };
}
