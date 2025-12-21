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
import {Marker} from "../markers";
import {J} from "../java";
import {JS} from "./tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {
    prettierStyle,
    PrettierStyle,
    SpacesStyle,
    SpacesStyleDetailKind,
    StyleKind,
    TabsAndIndentsStyle,
    WrappingAndBracesStyle,
    WrappingAndBracesStyleDetailKind
} from "./style";
import {Autodetect} from "./autodetect";
import {updateIfChanged} from "../util";

declare module "./tree" {
    namespace JS {
        export const Markers: {
            readonly Generator: "org.openrewrite.javascript.marker.Generator";
            readonly Optional: "org.openrewrite.javascript.marker.Optional";
            readonly NonNullAssertion: "org.openrewrite.javascript.marker.NonNullAssertion";
            readonly Spread: "org.openrewrite.javascript.marker.Spread";
            readonly DelegatedYield: "org.openrewrite.javascript.marker.DelegatedYield";
            readonly FunctionDeclaration: "org.openrewrite.javascript.marker.FunctionDeclaration";
        };
    }
}

// At runtime actually attach it to JS
(JS as any).Markers = {
    Generator: "org.openrewrite.javascript.marker.Generator",
    DelegatedYield: "org.openrewrite.javascript.marker.DelegatedYield",
    Optional: "org.openrewrite.javascript.marker.Optional",
    NonNullAssertion: "org.openrewrite.javascript.marker.NonNullAssertion",
    Spread: "org.openrewrite.javascript.marker.Spread",
    FunctionDeclaration: "org.openrewrite.javascript.marker.FunctionDeclaration",
} as const;

/**
 * The `yield*` operator in JavaScript is a special case of the `yield` operator,
 * used in generator functions to yield to another iterator. This marker will
 * be applied to a {@link J.Yield} expression.
 */
export interface DelegatedYield extends Marker {
    readonly kind: typeof JS.Markers.DelegatedYield;
    readonly prefix: J.Space;
}

export interface Optional extends Marker {
    readonly kind: typeof JS.Markers.Optional;
    readonly prefix: J.Space;
}

export interface Generator extends Marker {
    readonly kind: typeof JS.Markers.Generator;
    readonly prefix: J.Space;
}

export interface NonNullAssertion extends Marker {
    readonly kind: typeof JS.Markers.NonNullAssertion;
    readonly prefix: J.Space;
}

export interface Spread extends Marker {
    readonly kind: typeof JS.Markers.Spread;
    readonly prefix: J.Space;
}

export interface FunctionDeclaration extends Marker {
    readonly kind: typeof JS.Markers.FunctionDeclaration;
    readonly prefix: J.Space;
}

/**
 * Registers an RPC codec for any marker that has a `prefix: J.Space` field.
 */
function registerPrefixedMarkerCodec<M extends Marker & { prefix: J.Space }>(
    kind: M["kind"]
) {
    RpcCodecs.registerCodec(kind, {
        async rpcReceive(before: M, q: RpcReceiveQueue): Promise<M> {
            return updateIfChanged(before, {
                id: await q.receive(before.id),
                prefix: await q.receive(before.prefix)
            } as Partial<M>);
        },

        async rpcSend(after: M, q: RpcSendQueue): Promise<void> {
            await q.getAndSend(after, a => a.id);
            await q.getAndSend(after, a => a.prefix);
        }
    });
}

registerPrefixedMarkerCodec<DelegatedYield>(JS.Markers.DelegatedYield);
registerPrefixedMarkerCodec<Optional>(JS.Markers.Optional);
registerPrefixedMarkerCodec<Generator>(JS.Markers.Generator);
registerPrefixedMarkerCodec<NonNullAssertion>(JS.Markers.NonNullAssertion);
registerPrefixedMarkerCodec<Spread>(JS.Markers.Spread);
registerPrefixedMarkerCodec<FunctionDeclaration>(JS.Markers.FunctionDeclaration);

// Register codec for PrettierStyle (a NamedStyles that contains Prettier configuration)
// Only serialize the variable fields; constant fields are defined in the interface
RpcCodecs.registerCodec(StyleKind.PrettierStyle, {
    async rpcReceive(before: PrettierStyle, q: RpcReceiveQueue): Promise<PrettierStyle> {
        const id = await q.receive(before.id);
        const config = await q.receive(before.config);
        const version = await q.receive(before.prettierVersion);
        const ignored = await q.receive(before.ignored);
        return prettierStyle(id, config, version, ignored);
    },

    async rpcSend(after: PrettierStyle, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.config);
        await q.getAndSend(after, a => a.prettierVersion);
        await q.getAndSend(after, a => a.ignored);
    }
});

// Register codec for Autodetect (auto-detected styles for JavaScript/TypeScript)
// Only serialize the variable fields (id, styles); constant fields are defined in the interface
RpcCodecs.registerCodec(StyleKind.Autodetect, {
    async rpcReceive(before: Autodetect, q: RpcReceiveQueue): Promise<Autodetect> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            styles: (await q.receiveList(before.styles))!,
        });
    },

    async rpcSend(after: Autodetect, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSendList(after, a => a.styles, s => s.kind);
    }
});

// ============================================================================
// Style RPC Codecs
// ============================================================================

// Helper to create a simple codec for objects where all fields are primitives
function registerSimpleCodec<T extends { kind: string }>(
    kind: T["kind"],
    fields: (keyof T)[]
) {
    RpcCodecs.registerCodec(kind, {
        async rpcReceive(before: T, q: RpcReceiveQueue): Promise<T> {
            const updates: Partial<T> = {};
            for (const field of fields) {
                (updates as any)[field] = await q.receive((before as any)[field]);
            }
            return updateIfChanged(before, updates);
        },

        async rpcSend(after: T, q: RpcSendQueue): Promise<void> {
            for (const field of fields) {
                await q.getAndSend(after, a => (a as any)[field]);
            }
        }
    });
}

// TabsAndIndentsStyle - simple style with primitive fields
registerSimpleCodec<TabsAndIndentsStyle>(StyleKind.TabsAndIndentsStyle, [
    'useTabCharacter', 'tabSize', 'indentSize', 'continuationIndent',
    'keepIndentsOnEmptyLines', 'indentChainedMethods', 'indentAllChainedCallsInAGroup'
]);

// SpacesStyle and its nested types
registerSimpleCodec<SpacesStyle.BeforeParentheses>(SpacesStyleDetailKind.SpacesStyleBeforeParentheses, [
    'functionDeclarationParentheses', 'functionCallParentheses', 'ifParentheses',
    'forParentheses', 'whileParentheses', 'switchParentheses', 'catchParentheses',
    'inFunctionCallExpression', 'inAsyncArrowFunction'
]);

registerSimpleCodec<SpacesStyle.AroundOperators>(SpacesStyleDetailKind.SpacesStyleAroundOperators, [
    'assignment', 'logical', 'equality', 'relational', 'bitwise', 'additive',
    'multiplicative', 'shift', 'unary', 'arrowFunction',
    'beforeUnaryNotAndNotNull', 'afterUnaryNotAndNotNull'
]);

registerSimpleCodec<SpacesStyle.BeforeLeftBrace>(SpacesStyleDetailKind.SpacesStyleBeforeLeftBrace, [
    'functionLeftBrace', 'ifLeftBrace', 'elseLeftBrace', 'forLeftBrace',
    'whileLeftBrace', 'doLeftBrace', 'switchLeftBrace', 'tryLeftBrace',
    'catchLeftBrace', 'finallyLeftBrace', 'classInterfaceModuleLeftBrace'
]);

registerSimpleCodec<SpacesStyle.BeforeKeywords>(SpacesStyleDetailKind.SpacesStyleBeforeKeywords, [
    'elseKeyword', 'whileKeyword', 'catchKeyword', 'finallyKeyword'
]);

registerSimpleCodec<SpacesStyle.Within>(SpacesStyleDetailKind.SpacesStyleWithin, [
    'indexAccessBrackets', 'groupingParentheses', 'functionDeclarationParentheses',
    'functionCallParentheses', 'ifParentheses', 'forParentheses', 'whileParentheses',
    'switchParentheses', 'catchParentheses', 'objectLiteralBraces', 'es6ImportExportBraces',
    'arrayBrackets', 'interpolationExpressions', 'objectLiteralTypeBraces',
    'unionAndIntersectionTypes', 'typeAssertions'
]);

registerSimpleCodec<SpacesStyle.TernaryOperator>(SpacesStyleDetailKind.SpacesStyleTernaryOperator, [
    'beforeQuestionMark', 'afterQuestionMark', 'beforeColon', 'afterColon'
]);

registerSimpleCodec<SpacesStyle.Other>(SpacesStyleDetailKind.SpacesStyleOther, [
    'beforeComma', 'afterComma', 'beforeForSemicolon',
    'beforePropertyNameValueSeparator', 'afterPropertyNameValueSeparator',
    'afterVarArgInRestOrSpread', 'beforeAsteriskInGenerator', 'afterAsteriskInGenerator',
    'beforeTypeReferenceColon', 'afterTypeReferenceColon'
]);

// SpacesStyle - has nested objects
RpcCodecs.registerCodec(StyleKind.SpacesStyle, {
    async rpcReceive(before: SpacesStyle, q: RpcReceiveQueue): Promise<SpacesStyle> {
        return updateIfChanged(before, {
            beforeParentheses: await q.receive(before.beforeParentheses),
            aroundOperators: await q.receive(before.aroundOperators),
            beforeLeftBrace: await q.receive(before.beforeLeftBrace),
            beforeKeywords: await q.receive(before.beforeKeywords),
            within: await q.receive(before.within),
            ternaryOperator: await q.receive(before.ternaryOperator),
            other: await q.receive(before.other),
        });
    },

    async rpcSend(after: SpacesStyle, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.beforeParentheses);
        await q.getAndSend(after, a => a.aroundOperators);
        await q.getAndSend(after, a => a.beforeLeftBrace);
        await q.getAndSend(after, a => a.beforeKeywords);
        await q.getAndSend(after, a => a.within);
        await q.getAndSend(after, a => a.ternaryOperator);
        await q.getAndSend(after, a => a.other);
    }
});

// WrappingAndBracesStyle and its nested types
registerSimpleCodec<WrappingAndBracesStyle.IfStatement>(
    WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleIfStatement,
    ['elseOnNewLine']
);

registerSimpleCodec<WrappingAndBracesStyle.KeepWhenReformatting>(
    WrappingAndBracesStyleDetailKind.WrappingAndBracesStyleKeepWhenReformatting,
    ['simpleBlocksInOneLine', 'simpleMethodsInOneLine']
);

RpcCodecs.registerCodec(StyleKind.WrappingAndBracesStyle, {
    async rpcReceive(before: WrappingAndBracesStyle, q: RpcReceiveQueue): Promise<WrappingAndBracesStyle> {
        return updateIfChanged(before, {
            ifStatement: await q.receive(before.ifStatement),
            keepWhenReformatting: await q.receive(before.keepWhenReformatting),
        });
    },

    async rpcSend(after: WrappingAndBracesStyle, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.ifStatement);
        await q.getAndSend(after, a => a.keepWhenReformatting);
    }
});
