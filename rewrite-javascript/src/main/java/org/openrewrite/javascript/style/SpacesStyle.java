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
package org.openrewrite.javascript.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.javascript.JavaScriptStyle;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Value
@With
public class SpacesStyle implements JavaScriptStyle, RpcCodec<SpacesStyle> {

    BeforeParentheses beforeParentheses;
    AroundOperators aroundOperators;
    BeforeLeftBrace beforeLeftBrace;
    BeforeKeywords beforeKeywords;
    Within within;
    TernaryOperator ternaryOperator;
    Other other;

    @Override
    public void rpcSend(SpacesStyle after, RpcSendQueue q) {
        q.getAndSend(after, SpacesStyle::getBeforeParentheses);
        q.getAndSend(after, SpacesStyle::getAroundOperators);
        q.getAndSend(after, SpacesStyle::getBeforeLeftBrace);
        q.getAndSend(after, SpacesStyle::getBeforeKeywords);
        q.getAndSend(after, SpacesStyle::getWithin);
        q.getAndSend(after, SpacesStyle::getTernaryOperator);
        q.getAndSend(after, SpacesStyle::getOther);
    }

    @Override
    public SpacesStyle rpcReceive(SpacesStyle before, RpcReceiveQueue q) {
        return before
                .withBeforeParentheses(q.receive(before.getBeforeParentheses()))
                .withAroundOperators(q.receive(before.getAroundOperators()))
                .withBeforeLeftBrace(q.receive(before.getBeforeLeftBrace()))
                .withBeforeKeywords(q.receive(before.getBeforeKeywords()))
                .withWithin(q.receive(before.getWithin()))
                .withTernaryOperator(q.receive(before.getTernaryOperator()))
                .withOther(q.receive(before.getOther()));
    }

    @Value
    @With
    public static class BeforeParentheses implements RpcCodec<BeforeParentheses> {
        Boolean functionDeclarationParentheses;
        Boolean functionCallParentheses;
        Boolean ifParentheses;
        Boolean forParentheses;
        Boolean whileParentheses;
        Boolean switchParentheses;
        Boolean catchParentheses;
        Boolean inFunctionCallExpression;
        Boolean inAsyncArrowFunction;

        @Override
        public void rpcSend(BeforeParentheses after, RpcSendQueue q) {
            q.getAndSend(after, BeforeParentheses::getFunctionDeclarationParentheses);
            q.getAndSend(after, BeforeParentheses::getFunctionCallParentheses);
            q.getAndSend(after, BeforeParentheses::getIfParentheses);
            q.getAndSend(after, BeforeParentheses::getForParentheses);
            q.getAndSend(after, BeforeParentheses::getWhileParentheses);
            q.getAndSend(after, BeforeParentheses::getSwitchParentheses);
            q.getAndSend(after, BeforeParentheses::getCatchParentheses);
            q.getAndSend(after, BeforeParentheses::getInFunctionCallExpression);
            q.getAndSend(after, BeforeParentheses::getInAsyncArrowFunction);
        }

        @Override
        public BeforeParentheses rpcReceive(BeforeParentheses before, RpcReceiveQueue q) {
            return before
                    .withFunctionDeclarationParentheses(q.receive(before.getFunctionDeclarationParentheses()))
                    .withFunctionCallParentheses(q.receive(before.getFunctionCallParentheses()))
                    .withIfParentheses(q.receive(before.getIfParentheses()))
                    .withForParentheses(q.receive(before.getForParentheses()))
                    .withWhileParentheses(q.receive(before.getWhileParentheses()))
                    .withSwitchParentheses(q.receive(before.getSwitchParentheses()))
                    .withCatchParentheses(q.receive(before.getCatchParentheses()))
                    .withInFunctionCallExpression(q.receive(before.getInFunctionCallExpression()))
                    .withInAsyncArrowFunction(q.receive(before.getInAsyncArrowFunction()));
        }
    }

    @Value
    @With
    public static class AroundOperators implements RpcCodec<AroundOperators> {
        Boolean assignment;
        Boolean logical;
        Boolean equality;
        Boolean relational;
        Boolean bitwise;
        Boolean additive;
        Boolean multiplicative;
        Boolean shift;
        Boolean unary;
        Boolean arrowFunction;
        Boolean beforeUnaryNotAndNotNull;
        Boolean afterUnaryNotAndNotNull;

        @Override
        public void rpcSend(AroundOperators after, RpcSendQueue q) {
            q.getAndSend(after, AroundOperators::getAssignment);
            q.getAndSend(after, AroundOperators::getLogical);
            q.getAndSend(after, AroundOperators::getEquality);
            q.getAndSend(after, AroundOperators::getRelational);
            q.getAndSend(after, AroundOperators::getBitwise);
            q.getAndSend(after, AroundOperators::getAdditive);
            q.getAndSend(after, AroundOperators::getMultiplicative);
            q.getAndSend(after, AroundOperators::getShift);
            q.getAndSend(after, AroundOperators::getUnary);
            q.getAndSend(after, AroundOperators::getArrowFunction);
            q.getAndSend(after, AroundOperators::getBeforeUnaryNotAndNotNull);
            q.getAndSend(after, AroundOperators::getAfterUnaryNotAndNotNull);
        }

        @Override
        public AroundOperators rpcReceive(AroundOperators before, RpcReceiveQueue q) {
            return before
                    .withAssignment(q.receive(before.getAssignment()))
                    .withLogical(q.receive(before.getLogical()))
                    .withEquality(q.receive(before.getEquality()))
                    .withRelational(q.receive(before.getRelational()))
                    .withBitwise(q.receive(before.getBitwise()))
                    .withAdditive(q.receive(before.getAdditive()))
                    .withMultiplicative(q.receive(before.getMultiplicative()))
                    .withShift(q.receive(before.getShift()))
                    .withUnary(q.receive(before.getUnary()))
                    .withArrowFunction(q.receive(before.getArrowFunction()))
                    .withBeforeUnaryNotAndNotNull(q.receive(before.getBeforeUnaryNotAndNotNull()))
                    .withAfterUnaryNotAndNotNull(q.receive(before.getAfterUnaryNotAndNotNull()));
        }
    }

    @Value
    @With
    public static class BeforeLeftBrace implements RpcCodec<BeforeLeftBrace> {
        Boolean functionLeftBrace;
        Boolean ifLeftBrace;
        Boolean elseLeftBrace;
        Boolean forLeftBrace;
        Boolean whileLeftBrace;
        Boolean doLeftBrace;
        Boolean switchLeftBrace;
        Boolean tryLeftBrace;
        Boolean catchLeftBrace;
        Boolean finallyLeftBrace;
        Boolean classInterfaceModuleLeftBrace;

        @Override
        public void rpcSend(BeforeLeftBrace after, RpcSendQueue q) {
            q.getAndSend(after, BeforeLeftBrace::getFunctionLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getIfLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getElseLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getForLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getWhileLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getDoLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getSwitchLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getTryLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getCatchLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getFinallyLeftBrace);
            q.getAndSend(after, BeforeLeftBrace::getClassInterfaceModuleLeftBrace);
        }

        @Override
        public BeforeLeftBrace rpcReceive(BeforeLeftBrace before, RpcReceiveQueue q) {
            return before
                    .withFunctionLeftBrace(q.receive(before.getFunctionLeftBrace()))
                    .withIfLeftBrace(q.receive(before.getIfLeftBrace()))
                    .withElseLeftBrace(q.receive(before.getElseLeftBrace()))
                    .withForLeftBrace(q.receive(before.getForLeftBrace()))
                    .withWhileLeftBrace(q.receive(before.getWhileLeftBrace()))
                    .withDoLeftBrace(q.receive(before.getDoLeftBrace()))
                    .withSwitchLeftBrace(q.receive(before.getSwitchLeftBrace()))
                    .withTryLeftBrace(q.receive(before.getTryLeftBrace()))
                    .withCatchLeftBrace(q.receive(before.getCatchLeftBrace()))
                    .withFinallyLeftBrace(q.receive(before.getFinallyLeftBrace()))
                    .withClassInterfaceModuleLeftBrace(q.receive(before.getClassInterfaceModuleLeftBrace()));
        }
    }

    @Value
    @With
    public static class BeforeKeywords implements RpcCodec<BeforeKeywords> {
        Boolean elseKeyword;
        Boolean whileKeyword;
        Boolean catchKeyword;
        Boolean finallyKeyword;

        @Override
        public void rpcSend(BeforeKeywords after, RpcSendQueue q) {
            q.getAndSend(after, BeforeKeywords::getElseKeyword);
            q.getAndSend(after, BeforeKeywords::getWhileKeyword);
            q.getAndSend(after, BeforeKeywords::getCatchKeyword);
            q.getAndSend(after, BeforeKeywords::getFinallyKeyword);
        }

        @Override
        public BeforeKeywords rpcReceive(BeforeKeywords before, RpcReceiveQueue q) {
            return before
                    .withElseKeyword(q.receive(before.getElseKeyword()))
                    .withWhileKeyword(q.receive(before.getWhileKeyword()))
                    .withCatchKeyword(q.receive(before.getCatchKeyword()))
                    .withFinallyKeyword(q.receive(before.getFinallyKeyword()));
        }
    }

    @Value
    @With
    public static class Within implements RpcCodec<Within> {
        Boolean indexAccessBrackets;
        Boolean groupingParentheses;
        Boolean functionDeclarationParentheses;
        Boolean functionCallParentheses;
        Boolean ifParentheses;
        Boolean forParentheses;
        Boolean whileParentheses;
        Boolean switchParentheses;
        Boolean catchParentheses;
        Boolean objectLiteralBraces;
        Boolean es6ImportExportBraces;
        Boolean arrayBrackets;
        Boolean interpolationExpressions;
        Boolean objectLiteralTypeBraces;
        Boolean unionAndIntersectionTypes;
        Boolean typeAssertions;

        @Override
        public void rpcSend(Within after, RpcSendQueue q) {
            q.getAndSend(after, Within::getIndexAccessBrackets);
            q.getAndSend(after, Within::getGroupingParentheses);
            q.getAndSend(after, Within::getFunctionDeclarationParentheses);
            q.getAndSend(after, Within::getFunctionCallParentheses);
            q.getAndSend(after, Within::getIfParentheses);
            q.getAndSend(after, Within::getForParentheses);
            q.getAndSend(after, Within::getWhileParentheses);
            q.getAndSend(after, Within::getSwitchParentheses);
            q.getAndSend(after, Within::getCatchParentheses);
            q.getAndSend(after, Within::getObjectLiteralBraces);
            q.getAndSend(after, Within::getEs6ImportExportBraces);
            q.getAndSend(after, Within::getArrayBrackets);
            q.getAndSend(after, Within::getInterpolationExpressions);
            q.getAndSend(after, Within::getObjectLiteralTypeBraces);
            q.getAndSend(after, Within::getUnionAndIntersectionTypes);
            q.getAndSend(after, Within::getTypeAssertions);
        }

        @Override
        public Within rpcReceive(Within before, RpcReceiveQueue q) {
            return before
                    .withIndexAccessBrackets(q.receive(before.getIndexAccessBrackets()))
                    .withGroupingParentheses(q.receive(before.getGroupingParentheses()))
                    .withFunctionDeclarationParentheses(q.receive(before.getFunctionDeclarationParentheses()))
                    .withFunctionCallParentheses(q.receive(before.getFunctionCallParentheses()))
                    .withIfParentheses(q.receive(before.getIfParentheses()))
                    .withForParentheses(q.receive(before.getForParentheses()))
                    .withWhileParentheses(q.receive(before.getWhileParentheses()))
                    .withSwitchParentheses(q.receive(before.getSwitchParentheses()))
                    .withCatchParentheses(q.receive(before.getCatchParentheses()))
                    .withObjectLiteralBraces(q.receive(before.getObjectLiteralBraces()))
                    .withEs6ImportExportBraces(q.receive(before.getEs6ImportExportBraces()))
                    .withArrayBrackets(q.receive(before.getArrayBrackets()))
                    .withInterpolationExpressions(q.receive(before.getInterpolationExpressions()))
                    .withObjectLiteralTypeBraces(q.receive(before.getObjectLiteralTypeBraces()))
                    .withUnionAndIntersectionTypes(q.receive(before.getUnionAndIntersectionTypes()))
                    .withTypeAssertions(q.receive(before.getTypeAssertions()));
        }
    }

    @Value
    @With
    public static class TernaryOperator implements RpcCodec<TernaryOperator> {
        Boolean beforeQuestionMark;
        Boolean afterQuestionMark;
        Boolean beforeColon;
        Boolean afterColon;

        @Override
        public void rpcSend(TernaryOperator after, RpcSendQueue q) {
            q.getAndSend(after, TernaryOperator::getBeforeQuestionMark);
            q.getAndSend(after, TernaryOperator::getAfterQuestionMark);
            q.getAndSend(after, TernaryOperator::getBeforeColon);
            q.getAndSend(after, TernaryOperator::getAfterColon);
        }

        @Override
        public TernaryOperator rpcReceive(TernaryOperator before, RpcReceiveQueue q) {
            return before
                    .withBeforeQuestionMark(q.receive(before.getBeforeQuestionMark()))
                    .withAfterQuestionMark(q.receive(before.getAfterQuestionMark()))
                    .withBeforeColon(q.receive(before.getBeforeColon()))
                    .withAfterColon(q.receive(before.getAfterColon()));
        }
    }

    @Value
    @With
    public static class Other implements RpcCodec<Other> {
        Boolean beforeComma;
        Boolean afterComma;
        Boolean beforeForSemicolon;
        Boolean beforePropertyNameValueSeparator;
        Boolean afterPropertyNameValueSeparator;
        Boolean afterVarArgInRestOrSpread;
        Boolean beforeAsteriskInGenerator;
        Boolean afterAsteriskInGenerator;
        Boolean beforeTypeReferenceColon;
        Boolean afterTypeReferenceColon;

        @Override
        public void rpcSend(Other after, RpcSendQueue q) {
            q.getAndSend(after, Other::getBeforeComma);
            q.getAndSend(after, Other::getAfterComma);
            q.getAndSend(after, Other::getBeforeForSemicolon);
            q.getAndSend(after, Other::getBeforePropertyNameValueSeparator);
            q.getAndSend(after, Other::getAfterPropertyNameValueSeparator);
            q.getAndSend(after, Other::getAfterVarArgInRestOrSpread);
            q.getAndSend(after, Other::getBeforeAsteriskInGenerator);
            q.getAndSend(after, Other::getAfterAsteriskInGenerator);
            q.getAndSend(after, Other::getBeforeTypeReferenceColon);
            q.getAndSend(after, Other::getAfterTypeReferenceColon);
        }

        @Override
        public Other rpcReceive(Other before, RpcReceiveQueue q) {
            return before
                    .withBeforeComma(q.receive(before.getBeforeComma()))
                    .withAfterComma(q.receive(before.getAfterComma()))
                    .withBeforeForSemicolon(q.receive(before.getBeforeForSemicolon()))
                    .withBeforePropertyNameValueSeparator(q.receive(before.getBeforePropertyNameValueSeparator()))
                    .withAfterPropertyNameValueSeparator(q.receive(before.getAfterPropertyNameValueSeparator()))
                    .withAfterVarArgInRestOrSpread(q.receive(before.getAfterVarArgInRestOrSpread()))
                    .withBeforeAsteriskInGenerator(q.receive(before.getBeforeAsteriskInGenerator()))
                    .withAfterAsteriskInGenerator(q.receive(before.getAfterAsteriskInGenerator()))
                    .withBeforeTypeReferenceColon(q.receive(before.getBeforeTypeReferenceColon()))
                    .withAfterTypeReferenceColon(q.receive(before.getAfterTypeReferenceColon()));
        }
    }
}
