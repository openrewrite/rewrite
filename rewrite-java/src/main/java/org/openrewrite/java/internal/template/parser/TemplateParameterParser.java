/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal.template.parser;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.template.parser.TemplateParameterLexer.TokenKind;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptyList;

/**
 * Hand-rolled recursive-descent parser for the {@code TemplateParameter}
 * grammar previously implemented with ANTLR. Produces small immutable
 * POJO ASTs so callers can {@code switch}/{@code if} over them directly
 * instead of paying for visitor dispatch.
 *
 * <p>Grammar (verbatim from the original {@code TemplateParameterParser.g4}):
 * <pre>
 * matcherPattern  : typedPattern | parameterName ;
 * genericPattern  : genericName (Extends (type AND)* type)? ;
 * typedPattern    : (parameterName COLON)? patternType ;
 * patternType     : matcherName LPAREN type? RPAREN ;
 * type            : typeName (LBRACK (typeParameter COMMA)* typeParameter RBRACK)? typeArray* ;
 * typeParameter   : variance? type | WILDCARD ;
 * variance        : WILDCARD Extends | WILDCARD Super ;
 * typeArray       : LSBRACK RSBRACK ;
 * </pre>
 */
public final class TemplateParameterParser {

    public enum Variance { EXTENDS, SUPER }

    /** {@code matcherPattern : typedPattern | parameterName}. */
    public static final class MatcherPatternNode {
        private final @Nullable TypedPatternNode typedPattern;
        private final @Nullable String parameterName;

        MatcherPatternNode(@Nullable TypedPatternNode typedPattern, @Nullable String parameterName) {
            this.typedPattern = typedPattern;
            this.parameterName = parameterName;
        }

        public @Nullable TypedPatternNode typedPattern() {
            return typedPattern;
        }

        public @Nullable String parameterName() {
            return parameterName;
        }
    }

    /** {@code typedPattern : (parameterName COLON)? patternType}. */
    public static final class TypedPatternNode {
        private final @Nullable String parameterName;
        private final PatternTypeNode patternType;

        TypedPatternNode(@Nullable String parameterName, PatternTypeNode patternType) {
            this.parameterName = parameterName;
            this.patternType = patternType;
        }

        public @Nullable String parameterName() {
            return parameterName;
        }

        public PatternTypeNode patternType() {
            return patternType;
        }
    }

    /** {@code patternType : matcherName LPAREN type? RPAREN}. */
    public static final class PatternTypeNode {
        private final String matcherName;
        private final @Nullable TypeNode type;

        PatternTypeNode(String matcherName, @Nullable TypeNode type) {
            this.matcherName = matcherName;
            this.type = type;
        }

        public String matcherName() {
            return matcherName;
        }

        public @Nullable TypeNode type() {
            return type;
        }
    }

    /** {@code type : typeName (LBRACK (typeParameter COMMA)* typeParameter RBRACK)? typeArray*}. */
    public static final class TypeNode {
        private final String typeName;
        private final List<TypeParameterNode> typeParameters;
        private final int arrayDepth;

        TypeNode(String typeName, List<TypeParameterNode> typeParameters, int arrayDepth) {
            this.typeName = typeName;
            this.typeParameters = typeParameters;
            this.arrayDepth = arrayDepth;
        }

        public String typeName() {
            return typeName;
        }

        public List<TypeParameterNode> typeParameters() {
            return typeParameters;
        }

        public int arrayDepth() {
            return arrayDepth;
        }
    }

    /** {@code typeParameter : variance? type | WILDCARD}. {@code isWildcard} is true for the bare {@code ?} alternative. */
    public static final class TypeParameterNode {
        private final @Nullable Variance variance;
        private final @Nullable TypeNode type;
        private final boolean isWildcard;

        TypeParameterNode(@Nullable Variance variance, @Nullable TypeNode type, boolean isWildcard) {
            this.variance = variance;
            this.type = type;
            this.isWildcard = isWildcard;
        }

        public @Nullable Variance variance() {
            return variance;
        }

        public @Nullable TypeNode type() {
            return type;
        }

        public boolean isWildcard() {
            return isWildcard;
        }
    }

    /** {@code genericPattern : genericName (Extends (type AND)* type)?}. */
    public static final class GenericPatternNode {
        private final String genericName;
        private final List<TypeNode> bounds;

        GenericPatternNode(String genericName, List<TypeNode> bounds) {
            this.genericName = genericName;
            this.bounds = bounds;
        }

        public String genericName() {
            return genericName;
        }

        public List<TypeNode> bounds() {
            return bounds;
        }
    }

    /**
     * Parse results are immutable POJOs and the same input strings are seen
     * over and over (each recipe matcher visit feeds the same placeholder
     * content back through {@link #parseMatcherPattern}). Caching by input
     * string lets the second-and-later parses fall to a single hash lookup.
     */
    private static final ConcurrentMap<String, MatcherPatternNode> MATCHER_PATTERN_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, GenericPatternNode> GENERIC_PATTERN_CACHE = new ConcurrentHashMap<>();

    private final TemplateParameterLexer lexer;

    private TemplateParameterParser(TemplateParameterLexer lexer) {
        this.lexer = lexer;
    }

    public static MatcherPatternNode parseMatcherPattern(String input) {
        MatcherPatternNode cached = MATCHER_PATTERN_CACHE.get(input);
        if (cached != null) {
            return cached;
        }
        TemplateParameterParser p = new TemplateParameterParser(new TemplateParameterLexer(input));
        MatcherPatternNode result = p.matcherPattern();
        p.expectEof();
        MatcherPatternNode existing = MATCHER_PATTERN_CACHE.putIfAbsent(input, result);
        return existing != null ? existing : result;
    }

    public static GenericPatternNode parseGenericPattern(String input) {
        GenericPatternNode cached = GENERIC_PATTERN_CACHE.get(input);
        if (cached != null) {
            return cached;
        }
        TemplateParameterParser p = new TemplateParameterParser(new TemplateParameterLexer(input));
        GenericPatternNode result = p.genericPattern();
        p.expectEof();
        GenericPatternNode existing = GENERIC_PATTERN_CACHE.putIfAbsent(input, result);
        return existing != null ? existing : result;
    }

    public static TypeNode parseType(String input) {
        TemplateParameterParser p = new TemplateParameterParser(new TemplateParameterLexer(input));
        TypeNode result = p.type();
        p.expectEof();
        return result;
    }

    private MatcherPatternNode matcherPattern() {
        // Both alternatives start with an Identifier.
        // The typedPattern alternative requires either an LPAREN after the
        // identifier (matcherName with no parameter name) or a COLON after
        // the identifier (parameterName followed by patternType). Anything
        // else falls through to the parameterName-only alternative.
        if (lexer.kind() != TokenKind.IDENTIFIER) {
            throw error("Expected identifier at start of matcher pattern");
        }
        // Lookahead is implicit through current token plus one peek. We don't
        // have peek-ahead state on the lexer, so we read the identifier first
        // and decide based on the next token.
        String firstIdent = lexer.tokenText();
        lexer.advance();

        if (lexer.kind() == TokenKind.LPAREN) {
            // matcherName ( ... ) — typedPattern with no parameterName
            TypedPatternNode typed = new TypedPatternNode(null, patternTypeFromMatcherName(firstIdent));
            return new MatcherPatternNode(typed, null);
        }
        if (lexer.kind() == TokenKind.COLON) {
            // parameterName COLON patternType
            lexer.advance();
            if (lexer.kind() != TokenKind.IDENTIFIER) {
                throw error("Expected matcher name after ':'");
            }
            String matcherName = lexer.tokenText();
            lexer.advance();
            TypedPatternNode typed = new TypedPatternNode(firstIdent, patternTypeFromMatcherName(matcherName));
            return new MatcherPatternNode(typed, null);
        }
        // Bare parameterName.
        return new MatcherPatternNode(null, firstIdent);
    }

    private PatternTypeNode patternTypeFromMatcherName(String matcherName) {
        if (lexer.kind() != TokenKind.LPAREN) {
            throw error("Expected '(' after matcher name '" + matcherName + "'");
        }
        lexer.advance();
        TypeNode inner = null;
        if (lexer.kind() != TokenKind.RPAREN) {
            inner = type();
        }
        if (lexer.kind() != TokenKind.RPAREN) {
            throw error("Expected ')'");
        }
        lexer.advance();
        return new PatternTypeNode(matcherName, inner);
    }

    private GenericPatternNode genericPattern() {
        if (lexer.kind() != TokenKind.IDENTIFIER) {
            throw error("Expected generic name (identifier)");
        }
        String name = lexer.tokenText();
        lexer.advance();
        if (lexer.kind() == TokenKind.EOF) {
            return new GenericPatternNode(name, emptyList());
        }
        if (lexer.kind() != TokenKind.EXTENDS) {
            throw error("Expected 'extends' or end of input after generic name");
        }
        lexer.advance();
        // (type AND)* type — read at least one type, separated by AND.
        List<TypeNode> bounds = new ArrayList<>(2);
        bounds.add(type());
        while (lexer.kind() == TokenKind.AND) {
            lexer.advance();
            bounds.add(type());
        }
        return new GenericPatternNode(name, bounds);
    }

    private TypeNode type() {
        // typeName : FullyQualifiedName | Identifier
        if (lexer.kind() != TokenKind.FULLY_QUALIFIED_NAME && lexer.kind() != TokenKind.IDENTIFIER) {
            throw error("Expected type name");
        }
        String typeName = lexer.tokenText();
        lexer.advance();

        // Optional <...> type parameter list.
        List<TypeParameterNode> typeParameters = emptyList();
        if (lexer.kind() == TokenKind.LBRACK) {
            lexer.advance();
            typeParameters = new ArrayList<>(2);
            typeParameters.add(typeParameter());
            while (lexer.kind() == TokenKind.COMMA) {
                lexer.advance();
                typeParameters.add(typeParameter());
            }
            if (lexer.kind() != TokenKind.RBRACK) {
                throw error("Expected '>' or ',' in type parameter list");
            }
            lexer.advance();
        }

        // typeArray*
        int arrayDepth = 0;
        while (lexer.kind() == TokenKind.LSBRACK) {
            lexer.advance();
            if (lexer.kind() != TokenKind.RSBRACK) {
                throw error("Expected ']'");
            }
            lexer.advance();
            arrayDepth++;
        }

        return new TypeNode(typeName, typeParameters, arrayDepth);
    }

    private TypeParameterNode typeParameter() {
        // typeParameter : variance? type | WILDCARD
        if (lexer.kind() == TokenKind.WILDCARD) {
            // Could be `?`, `? extends type`, or `? super type`.
            lexer.advance();
            if (lexer.kind() == TokenKind.EXTENDS) {
                lexer.advance();
                return new TypeParameterNode(Variance.EXTENDS, type(), false);
            }
            if (lexer.kind() == TokenKind.SUPER) {
                lexer.advance();
                return new TypeParameterNode(Variance.SUPER, type(), false);
            }
            return new TypeParameterNode(null, null, true);
        }
        return new TypeParameterNode(null, type(), false);
    }

    private void expectEof() {
        if (lexer.kind() != TokenKind.EOF) {
            throw error("Expected end of input");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + lexer.tokenStart() +
                " in input '" + lexer.input() + "'");
    }
}
