/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby;

import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.internal.PrismSource;
import org.openrewrite.ruby.internal.StringUtils;
import org.openrewrite.ruby.marker.*;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.ruby.tree.RubySpace;
import org.ruby_lang.prism.AbstractNodeVisitor;
import org.ruby_lang.prism.Nodes;
import org.ruby_lang.prism.ParseResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

/**
 * Maps Prism's byte-offset AST onto the {@code Rb}/{@code J} LST. Prism gives every node an exact
 * span but no comments and no token-level positions, so whitespace, comments and keywords are still
 * re-lexed from the source with a linear cursor — now anchored to node offsets rather than guessed.
 */
public class RubyParserVisitor extends AbstractNodeVisitor<J> {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final PrismSource src;
    private final String source;
    private final boolean charsetBomMarked;

    private int cursor = 0;

    /**
     * Heredoc markers seen on the current line whose bodies have not been reached yet. The bodies
     * live outside the parent node's span, so the linear cursor claims them the first time it
     * crosses a newline.
     */
    private Deque<PendingHeredoc> pendingHeredocs = new ArrayDeque<>();

    /**
     * A heredoc's body is only known once the cursor reaches it, which is after the LST node has
     * already been placed in the tree, so the completed values are folded in by a final pass.
     * Keyed by id rather than identity, because the placeholder in the tree may have been copied
     * (e.g. re-prefixed) since it was queued.
     */
    private Map<UUID, Rb.Heredoc> finalizedHeredocs = new HashMap<>();

    public RubyParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, PrismSource src,
                             boolean charsetBomMarked) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.src = src;
        this.source = src.getText();
        this.charsetBomMarked = charsetBomMarked;
    }

    private static final class PendingHeredoc {
        final Rb.Heredoc placeholder;
        final String marker;
        final String id;
        final boolean squiggly;

        PendingHeredoc(Rb.Heredoc placeholder, String marker, String id, boolean squiggly) {
            this.placeholder = placeholder;
            this.marker = marker;
            this.id = id;
            this.squiggly = squiggly;
        }
    }

    @Override
    protected J defaultVisit(Nodes.Node node) {
        throw new UnsupportedOperationException(String.format(
                "Prism node type %s is not yet implemented (%s at offset %d)",
                node.getClass().getSimpleName(), sourcePath, charStart(node)));
    }

    public Rb.CompilationUnit visitProgram(ParseResult result) {
        Nodes.ProgramNode program = (Nodes.ProgramNode) result.value;
        Space prefix = whitespace();
        List<JRightPadded<Statement>> statements = program == null ?
                emptyList() : statements(program.statements);
        Rb.CompilationUnit cu = new Rb.CompilationUnit(
                randomId(),
                prefix,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                src.getCharset().name(),
                charsetBomMarked,
                null,
                statements,
                whitespace()
        );
        if (finalizedHeredocs.isEmpty()) {
            return cu;
        }
        return (Rb.CompilationUnit) new RubyIsoVisitor<Integer>() {
            @Override
            public Rb.Heredoc visitHeredoc(Rb.Heredoc heredoc, Integer p) {
                Rb.Heredoc finalized = finalizedHeredocs.get(heredoc.getId());
                return finalized == null ? heredoc :
                        heredoc.withValue(finalized.getValue())
                                .withAroundValue(finalized.getAroundValue())
                                .withEnd(finalized.getEnd());
            }
        }.visitNonNull(cu, 0);
    }

    // ------------------------------------------------------------------ offsets

    private int charStart(Nodes.Node node) {
        return src.charOffset(node.startOffset);
    }

    private int charEnd(Nodes.Node node) {
        return src.charOffset(node.endOffset());
    }

    private String text(Nodes.Node node) {
        return source.substring(charStart(node), charEnd(node));
    }

    private static String str(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Consumes whitespace and comments and asserts that the cursor then sits exactly on the node's
     * start, so a desync fails here rather than corrupting everything downstream.
     */
    private Space prefix(Nodes.Node node) {
        Space space = whitespace();
        int expected = charStart(node);
        if (cursor != expected) {
            throw new IllegalStateException(String.format(
                    "Cursor desync in %s: expected to be at offset %d (start of %s) but was at %d. " +
                    "Source at cursor: |%s|",
                    sourcePath, expected, node.getClass().getSimpleName(), cursor,
                    source.substring(Math.max(0, cursor - 20), Math.min(source.length(), cursor + 20))));
        }
        return space;
    }

    // ------------------------------------------------------------------ cursor

    private Space whitespace() {
        int next = indexOfNextNonWhitespace(cursor);
        if (!pendingHeredocs.isEmpty()) {
            int newline = source.indexOf('\n', cursor);
            if (newline >= 0 && newline < next) {
                Space space = RubySpace.format(source, cursor, newline + 1);
                cursor = newline + 1;
                for (UUID flushed : flushHeredocs()) {
                    finalizedHeredocs.put(flushed, finalizedHeredocs.get(flushed).withAroundValue(space));
                }
                return space;
            }
        }
        Space space = RubySpace.format(source, cursor, next);
        cursor = next;
        return space;
    }

    private List<UUID> flushHeredocs() {
        List<UUID> flushed = new ArrayList<>(pendingHeredocs.size());
        while (!pendingHeredocs.isEmpty()) {
            PendingHeredoc pending = pendingHeredocs.poll();
            int bodyStart = cursor;
            int terminator = indexOfHeredocTerminator(bodyStart, pending.id);
            String body = source.substring(bodyStart, terminator);
            cursor = terminator + pending.id.length();

            Rb.Heredoc heredoc = pending.placeholder.withValue(new J.Literal(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    pending.squiggly ? org.openrewrite.internal.StringUtils.trimIndentPreserveCRLF(body) : body,
                    pending.marker + "\n" + body + pending.id,
                    null,
                    JavaType.Primitive.String
            ));

            // When another heredoc body follows on the next line, this heredoc's trailing space is
            // just the newline that closes its terminator line.
            Space end;
            if (pendingHeredocs.isEmpty()) {
                end = whitespace();
            } else {
                int newline = source.indexOf('\n', cursor);
                int stop = newline < 0 ? source.length() : newline + 1;
                end = RubySpace.format(source, cursor, stop);
                cursor = stop;
            }

            finalizedHeredocs.put(pending.placeholder.getId(), heredoc.withEnd(end));
            flushed.add(pending.placeholder.getId());
        }
        return flushed;
    }

    private int indexOfHeredocTerminator(int from, String id) {
        for (int i = from; i < source.length(); i++) {
            if (source.startsWith(id, i)) {
                boolean lineStart = true;
                for (int j = i - 1; j >= 0; j--) {
                    char c = source.charAt(j);
                    if (c == '\n') {
                        break;
                    } else if (c != ' ' && c != '\t') {
                        lineStart = false;
                        break;
                    }
                }
                if (lineStart) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("Unterminated heredoc <<" + id + " in " + sourcePath);
    }

    private Space sourceBefore(String untilDelim) {
        return peekWhitespace(0, (n, before) -> {
            if (source.startsWith(untilDelim, cursor)) {
                cursor += untilDelim.length();
                return before;
            }
            return null;
        }).orElse(EMPTY);
    }

    private <T, U> Optional<U> peekWhitespace(T t, BiFunction<T, Space, U> conditional) {
        int cursorBefore = cursor;
        Deque<PendingHeredoc> heredocsBefore = new ArrayDeque<>(pendingHeredocs);
        Map<UUID, Rb.Heredoc> finalizedBefore = finalizedHeredocs.isEmpty() ?
                Collections.emptyMap() : new HashMap<>(finalizedHeredocs);
        U converted = conditional.apply(t, whitespace());
        if (converted != null) {
            return Optional.of(converted);
        }
        cursor = cursorBefore;
        pendingHeredocs = heredocsBefore;
        finalizedHeredocs = finalizedBefore.isEmpty() && finalizedHeredocs.isEmpty() ?
                finalizedHeredocs : new HashMap<>(finalizedBefore);
        return Optional.empty();
    }

    /**
     * Consumes everything up to {@code to} as prefix without scanning for comments. Used inside
     * string literals, where {@code #} starts an interpolation rather than a comment and where the
     * exact span of every part is already known.
     */
    private Space prefixTo(int to) {
        Space space = RubySpace.format(source, cursor, to);
        cursor = to;
        return space;
    }

    private boolean skip(@Nullable String token) {
        if (token != null && source.startsWith(token, cursor)) {
            cursor += token.length();
            return true;
        }
        return false;
    }

    private boolean peekKeyword(String keyword) {
        return peekKeywordAt(keyword, cursor);
    }

    private boolean peekKeywordAt(String keyword, int at) {
        if (!source.startsWith(keyword, at)) {
            return false;
        }
        int after = at + keyword.length();
        return after >= source.length() || !Character.isJavaIdentifierPart(source.charAt(after));
    }

    private int indexOfNextNonWhitespace(int from) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int length = source.length();
        int i = from;
        for (; i < length; i++) {
            char current = source.charAt(i);
            if (inSingleLineComment) {
                inSingleLineComment = current != '\n';
                continue;
            } else if (length > i + 1) {
                if (current == '#') {
                    // deliberately does not skip the next character: an empty `#` comment line
                    // would otherwise swallow the newline and take the following line with it
                    inSingleLineComment = true;
                    continue;
                } else if (i == 0 || i == source.length() - "=end".length() ||
                           source.charAt(i - 1) == '\n') {
                    if (source.startsWith("=begin", i)) {
                        inMultiLineComment = true;
                        i++;
                        continue;
                    } else if (source.startsWith("=end", i)) {
                        inMultiLineComment = false;
                        i += "=end".length() - 1; // the loop increment adds another 1
                        continue;
                    }
                }
            }
            if (!inMultiLineComment && !Character.isWhitespace(current)) {
                char next = i < source.length() - 1 ? source.charAt(i + 1) : '\0';
                // line continuations count as whitespace in Ruby
                if (current != '\\' || !(next == '\n' || next == '\r')) {
                    break;
                }
            }
        }
        return i;
    }

    // ------------------------------------------------------------------ padding

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return new JRightPadded<>(tree, right, Markers.EMPTY);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private Space maybeTrailingComma(AtomicReference<Markers> markers, @Nullable String after) {
        return peekWhitespace(0, (n, next) -> {
            if (cursor < source.length() && source.charAt(cursor) == ',') {
                cursor++;
                markers.set(markers.get().add(new TrailingComma(randomId(),
                        after == null ? EMPTY : sourceBefore(after))));
                return next;
            } else if (after != null) {
                skip(after);
                return next;
            }
            return null;
        }).orElse(EMPTY);
    }

    // ------------------------------------------------------------------ conversion entry points

    private J convert(Nodes.Node node) {
        return node.accept(this);
    }

    private Expression convertExpression(Nodes.@Nullable Node node) {
        if (node == null) {
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        J j = convert(node);
        return j instanceof Expression ? (Expression) j :
                new Rb.StatementExpression(randomId(), (Statement) j);
    }

    private Statement convertStatement(Nodes.@Nullable Node node) {
        if (node == null) {
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        J j = convert(node);
        return j instanceof Statement ? (Statement) j :
                new Rb.ExpressionStatement(randomId(), (Expression) j);
    }

    private TypeTree convertTypeTree(Nodes.@Nullable Node node) {
        return asTypeTree(convertExpression(node));
    }

    private TypeTree asTypeTree(J j) {
        return j instanceof TypeTree ? (TypeTree) j :
                new Rb.ExpressionTypeTree(randomId(), j.getPrefix(), Markers.EMPTY, j.withPrefix(EMPTY));
    }

    private List<JRightPadded<Statement>> statements(Nodes.@Nullable StatementsNode statements) {
        List<JRightPadded<Statement>> converted = new ArrayList<>(
                statements == null ? 0 : statements.body.length);
        if (statements != null) {
            for (Nodes.Node statement : statements.body) {
                emptySeparators(converted);
                converted.add(separated(convertStatement(statement)));
            }
        }
        emptySeparators(converted);
        return converted;
    }

    /**
     * @return The statements as a single {@link Statement}, collapsing to the sole statement when
     * there is exactly one so that the common case does not gain a synthetic block. A `;` anywhere
     * in the list keeps the block, because only a right-padded statement can carry the separator.
     */
    private Statement bodyStatement(Nodes.@Nullable StatementsNode statements) {
        List<JRightPadded<Statement>> converted = statements(statements);
        if (converted.isEmpty()) {
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        JRightPadded<Statement> only = converted.get(0);
        if (converted.size() == 1 && only.getAfter().isEmpty() && only.getMarkers().getMarkers().isEmpty()) {
            return only.getElement();
        }
        return new J.Block(randomId(), EMPTY, Markers.EMPTY, JRightPadded.build(false),
                converted, EMPTY);
    }

    private Expression bodyExpression(Nodes.@Nullable StatementsNode statements) {
        Statement statement = bodyStatement(statements);
        return statement instanceof Expression ? (Expression) statement :
                new Rb.StatementExpression(randomId(), statement);
    }

    private List<JRightPadded<Statement>> bodyStatements(Nodes.@Nullable Node body) {
        if (body instanceof Nodes.StatementsNode) {
            return statements((Nodes.StatementsNode) body);
        }
        List<JRightPadded<Statement>> converted = new ArrayList<>(1);
        emptySeparators(converted);
        if (body != null) {
            converted.add(separated(convertStatement(body)));
            emptySeparators(converted);
        }
        return converted;
    }

    /**
     * A `;` after a statement rides on that statement's right padding as a {@link Semicolon}
     * marker, with the space in front of it as the padding's suffix.
     */
    private JRightPadded<Statement> separated(Statement statement) {
        return peekWhitespace(statement, (s, before) -> {
            if (!source.startsWith(";", cursor)) {
                return null;
            }
            cursor++;
            return padRight(s, before).withMarkers(Markers.EMPTY.add(new Semicolon(randomId())));
        }).orElseGet(() -> padRight(statement, EMPTY));
    }

    /**
     * A `;` with no statement in front of it — after a `def`/`class`/`if` header, or doubled up as
     * in {@code a;;b} — becomes a {@link J.Empty} statement carrying the {@link Semicolon} marker,
     * so that the statement list still accounts for every byte.
     */
    private void emptySeparators(List<JRightPadded<Statement>> into) {
        while (true) {
            Optional<JRightPadded<Statement>> separator = peekWhitespace(0, (n, before) -> {
                if (!source.startsWith(";", cursor)) {
                    return null;
                }
                cursor++;
                return padRight((Statement) new J.Empty(randomId(), before, Markers.EMPTY), EMPTY)
                        .withMarkers(Markers.EMPTY.add(new Semicolon(randomId())));
            });
            if (!separator.isPresent()) {
                return;
            }
            into.add(separator.get());
        }
    }

    /**
     * A {@code do ... end}, {@code class ... end} or {@code def ... end} body: the whitespace before
     * the first statement is the block's prefix and the whitespace before the terminator is its end.
     */
    private J.Block keywordBlock(Nodes.@Nullable Node body, String terminator) {
        Space prefix = whitespace();
        List<JRightPadded<Statement>> statements = bodyStatements(body);
        return new J.Block(randomId(), prefix, Markers.EMPTY, JRightPadded.build(false),
                statements, sourceBefore(terminator));
    }

    private J.Identifier identifier(String name) {
        return new J.Identifier(randomId(), sourceBefore(name), Markers.EMPTY, emptyList(), name,
                null, null);
    }

    private J.Identifier identifier(Nodes.Node node) {
        Space prefix = prefix(node);
        String name = text(node);
        cursor = charEnd(node);
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), name, null, null);
    }

    // ------------------------------------------------------------------ literals

    @Override
    public J visitIntegerNode(Nodes.IntegerNode node) {
        Space prefix = prefix(node);
        String valueSource = text(node);
        cursor = charEnd(node);
        return new J.Literal(randomId(), prefix, Markers.EMPTY, node.value, valueSource, null,
                JavaType.Primitive.Long);
    }

    @Override
    public J visitFloatNode(Nodes.FloatNode node) {
        Space prefix = prefix(node);
        String valueSource = text(node);
        cursor = charEnd(node);
        return new J.Literal(randomId(), prefix, Markers.EMPTY, node.value, valueSource, null,
                JavaType.Primitive.Float);
    }

    @Override
    public J visitTrueNode(Nodes.TrueNode node) {
        return new J.Literal(randomId(), prefix(node), Markers.EMPTY, true, skipText(node), null,
                JavaType.Primitive.Boolean);
    }

    @Override
    public J visitFalseNode(Nodes.FalseNode node) {
        return new J.Literal(randomId(), prefix(node), Markers.EMPTY, false, skipText(node), null,
                JavaType.Primitive.Boolean);
    }

    private String skipText(Nodes.Node node) {
        String text = text(node);
        cursor = charEnd(node);
        return text;
    }

    @Override
    public J visitNilNode(Nodes.NilNode node) {
        return identifier(node);
    }

    @Override
    public J visitSelfNode(Nodes.SelfNode node) {
        return identifier(node);
    }

    @Override
    public J visitSourceEncodingNode(Nodes.SourceEncodingNode node) {
        return identifier(node);
    }

    @Override
    public J visitSourceFileNode(Nodes.SourceFileNode node) {
        return identifier(node);
    }

    @Override
    public J visitSourceLineNode(Nodes.SourceLineNode node) {
        return identifier(node);
    }

    @Override
    public J visitImaginaryNode(Nodes.ImaginaryNode node) {
        return new Rb.NumericDomain(
                randomId(),
                prefix(node),
                Markers.EMPTY,
                padRight(convertExpression(node.numeric), sourceBefore("i")),
                Rb.NumericDomain.Domain.Complex,
                null
        );
    }

    @Override
    public J visitRationalNode(Nodes.RationalNode node) {
        Space prefix = prefix(node);
        int numeratorEnd = charEnd(node) - 1;
        J.Literal numerator = new J.Literal(randomId(), EMPTY, Markers.EMPTY, node.numerator,
                source.substring(cursor, numeratorEnd), null, JavaType.Primitive.Long);
        cursor = numeratorEnd;
        return new Rb.NumericDomain(
                randomId(),
                prefix,
                Markers.EMPTY,
                padRight(numerator, sourceBefore("r")),
                Rb.NumericDomain.Domain.Rational,
                null
        );
    }

    @Override
    public J visitBackReferenceReadNode(Nodes.BackReferenceReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitNumberedReferenceReadNode(Nodes.NumberedReferenceReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitItLocalVariableReadNode(Nodes.ItLocalVariableReadNode node) {
        return identifier(node);
    }

    // ------------------------------------------------------------------ strings

    /**
     * A heredoc's own span covers only its {@code <<~ID} marker; the body sits further down the
     * file and is claimed later by {@link #flushHeredocs()}.
     */
    private boolean isHeredocMarker(Nodes.Node node) {
        String text = text(node);
        if (!text.startsWith("<<")) {
            return false;
        }
        int i = 2;
        if (i < text.length() && (text.charAt(i) == '~' || text.charAt(i) == '-')) {
            i++;
        }
        char quote = 0;
        if (i < text.length() && (text.charAt(i) == '"' || text.charAt(i) == '\'' || text.charAt(i) == '`')) {
            quote = text.charAt(i++);
        }
        int idStart = i;
        while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) {
            i++;
        }
        if (i == idStart) {
            return false;
        }
        return quote == 0 ? i == text.length() : i == text.length() - 1 && text.charAt(i) == quote;
    }

    private Rb.Heredoc heredoc(Nodes.Node node) {
        Space prefix = prefix(node);
        String marker = text(node);
        cursor = charEnd(node);

        int i = 2;
        boolean squiggly = marker.charAt(i) == '~';
        if (marker.charAt(i) == '~' || marker.charAt(i) == '-') {
            i++;
        }
        String id = marker.substring(i);
        if (!id.isEmpty() && (id.charAt(0) == '"' || id.charAt(0) == '\'' || id.charAt(0) == '`')) {
            id = id.substring(1, id.length() - 1);
        }

        Rb.Heredoc placeholder = new Rb.Heredoc(randomId(), prefix, Markers.EMPTY, null, null, EMPTY);
        pendingHeredocs.add(new PendingHeredoc(placeholder, marker, id, squiggly));
        return placeholder;
    }

    @Override
    public J visitStringNode(Nodes.StringNode node) {
        if (isHeredocMarker(node)) {
            return heredoc(node);
        }
        return stringLiteral(node, str(node.unescaped));
    }

    private J.Literal stringLiteral(Nodes.Node node, String value) {
        Space prefix = prefix(node);
        String valueSource = text(node);
        cursor = charEnd(node);
        return new J.Literal(randomId(), prefix, Markers.EMPTY, value, valueSource, null,
                JavaType.Primitive.String);
    }

    @Override
    public J visitInterpolatedStringNode(Nodes.InterpolatedStringNode node) {
        if (isHeredocMarker(node)) {
            return heredoc(node);
        }
        return interpolated(node, node.parts, emptyList());
    }

    @Override
    public J visitInterpolatedXStringNode(Nodes.InterpolatedXStringNode node) {
        return interpolated(node, node.parts, emptyList());
    }

    @Override
    public J visitXStringNode(Nodes.XStringNode node) {
        Space prefix = prefix(node);
        String delimiter = readDelimiter(charStart(node));
        String text = text(node);
        String inner = text.substring(delimiter.length(),
                text.length() - StringUtils.endDelimiter(delimiter).length());
        cursor = charEnd(node);
        return new Rb.ComplexString(
                randomId(),
                prefix,
                Markers.EMPTY,
                delimiter,
                JContainer.build(singletonList(padRight(new J.Literal(randomId(), EMPTY, Markers.EMPTY,
                        str(node.unescaped), inner, null, JavaType.Primitive.String), EMPTY))),
                emptyList()
        );
    }

    @Override
    public J visitRegularExpressionNode(Nodes.RegularExpressionNode node) {
        return regularExpression(node, str(node.unescaped), regexpOptions(node));
    }

    @Override
    public J visitMatchLastLineNode(Nodes.MatchLastLineNode node) {
        return regularExpression(node, str(node.unescaped), regexpOptions(node));
    }

    private J regularExpression(Nodes.Node node, String value, List<Rb.ComplexString.RegexpOptions> options) {
        Space prefix = prefix(node);
        String delimiter = readDelimiter(charStart(node));
        String text = text(node);
        String endDelimiter = StringUtils.endDelimiter(delimiter);
        String inner = text.substring(delimiter.length(),
                text.length() - options.size() - endDelimiter.length());
        cursor = charEnd(node);
        return new Rb.ComplexString(
                randomId(),
                prefix,
                Markers.EMPTY,
                delimiter,
                JContainer.build(singletonList(padRight(new J.Literal(randomId(), EMPTY, Markers.EMPTY,
                        value, inner, null, JavaType.Primitive.String), EMPTY))),
                options
        );
    }

    @Override
    public J visitInterpolatedRegularExpressionNode(Nodes.InterpolatedRegularExpressionNode node) {
        return interpolated(node, node.parts, regexpOptions(node));
    }

    @Override
    public J visitInterpolatedMatchLastLineNode(Nodes.InterpolatedMatchLastLineNode node) {
        return interpolated(node, node.parts, regexpOptions(node));
    }

    private List<Rb.ComplexString.RegexpOptions> regexpOptions(Nodes.RegularExpressionNode node) {
        return regexpOptions(node, node.isIgnoreCase(), node.isExtended(), node.isMultiLine(),
                node.isOnce(), node.isEucJp(), node.isAscii8bit(), node.isWindows31j(), node.isUtf8());
    }

    private List<Rb.ComplexString.RegexpOptions> regexpOptions(Nodes.MatchLastLineNode node) {
        return regexpOptions(node, node.isIgnoreCase(), node.isExtended(), node.isMultiLine(),
                node.isOnce(), node.isEucJp(), node.isAscii8bit(), node.isWindows31j(), node.isUtf8());
    }

    private List<Rb.ComplexString.RegexpOptions> regexpOptions(Nodes.InterpolatedRegularExpressionNode node) {
        return regexpOptions(node, node.isIgnoreCase(), node.isExtended(), node.isMultiLine(),
                node.isOnce(), node.isEucJp(), node.isAscii8bit(), node.isWindows31j(), node.isUtf8());
    }

    private List<Rb.ComplexString.RegexpOptions> regexpOptions(Nodes.InterpolatedMatchLastLineNode node) {
        return regexpOptions(node, node.isIgnoreCase(), node.isExtended(), node.isMultiLine(),
                node.isOnce(), node.isEucJp(), node.isAscii8bit(), node.isWindows31j(), node.isUtf8());
    }

    /**
     * The regexp option characters have no location in Prism, so only their count comes from the
     * flags; the characters themselves are read back from the tail of the node in source order.
     */
    private List<Rb.ComplexString.RegexpOptions> regexpOptions(Nodes.Node node, boolean... flags) {
        int count = 0;
        for (boolean flag : flags) {
            if (flag) {
                count++;
            }
        }
        if (count == 0) {
            return emptyList();
        }
        String options = source.substring(charEnd(node) - count, charEnd(node));
        List<Rb.ComplexString.RegexpOptions> mapped = new ArrayList<>(count);
        for (int i = 0; i < options.length(); i++) {
            switch (options.charAt(i)) {
                case 'x':
                    mapped.add(Rb.ComplexString.RegexpOptions.Extended);
                    break;
                case 'i':
                    mapped.add(Rb.ComplexString.RegexpOptions.IgnoreCase);
                    break;
                case 'm':
                    mapped.add(Rb.ComplexString.RegexpOptions.Multiline);
                    break;
                case 'j':
                    mapped.add(Rb.ComplexString.RegexpOptions.Java);
                    break;
                case 'o':
                    mapped.add(Rb.ComplexString.RegexpOptions.Once);
                    break;
                case 'n':
                    mapped.add(Rb.ComplexString.RegexpOptions.None);
                    break;
                case 'e':
                    mapped.add(Rb.ComplexString.RegexpOptions.EUCJPEncoding);
                    break;
                case 's':
                    mapped.add(Rb.ComplexString.RegexpOptions.SJISEncoding);
                    break;
                case 'u':
                    mapped.add(Rb.ComplexString.RegexpOptions.UTF8Encoding);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown regexp option " + options.charAt(i));
            }
        }
        return mapped;
    }

    /**
     * Reads the opening delimiter of a string-like literal at {@code at}: a single quote character,
     * or a percent literal such as {@code %r{} or {@code %[}.
     */
    private String readDelimiter(int at) {
        char c = source.charAt(at);
        if (c == '%') {
            char kind = source.charAt(at + 1);
            switch (kind) {
                case 'q':
                case 'Q':
                case 'w':
                case 'W':
                case 'i':
                case 'I':
                case 's':
                case 'x':
                case 'r':
                    return source.substring(at, at + 3);
                default:
                    return source.substring(at, at + 2);
            }
        }
        return source.substring(at, at + 1);
    }

    /**
     * Prism represents implicitly concatenated adjacent literals ({@code 'a' 'b'}) with the same
     * node type as a single interpolated literal. They are told apart by where the first part
     * starts: a concatenation's first part begins with its own opening delimiter, at the very start
     * of the parent, while an interpolation's first part begins after the parent's delimiter.
     */
    private J interpolated(Nodes.Node node, Nodes.Node[] parts,
                           List<Rb.ComplexString.RegexpOptions> options) {
        if (parts.length > 1 && charStart(parts[0]) == charStart(node)) {
            Space prefix = prefix(node);
            Expression combined = convertExpression(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                Space beforeOperator = whitespace();
                combined = new Rb.Binary(
                        randomId(),
                        combined.getPrefix(),
                        Markers.EMPTY,
                        combined.withPrefix(EMPTY),
                        padLeft(beforeOperator, Rb.Binary.Type.ImplicitStringConcatenation),
                        convertExpression(parts[i]),
                        null
                );
            }
            return combined.withPrefix(prefix);
        }

        Space prefix = prefix(node);
        String delimiter = readDelimiter(cursor);
        skip(delimiter);
        List<JRightPadded<J>> converted = new ArrayList<>(parts.length);
        for (Nodes.Node part : parts) {
            converted.add(padRight(interpolatedPart(part), EMPTY));
        }
        skip(StringUtils.endDelimiter(delimiter));
        cursor = charEnd(node);
        return new Rb.ComplexString(randomId(), prefix, Markers.EMPTY, delimiter,
                JContainer.build(converted), options);
    }

    private J interpolatedPart(Nodes.Node part) {
        if (part instanceof Nodes.StringNode) {
            // The raw source slice preserves escapes exactly as written. It must not be scanned for
            // whitespace because a segment can legitimately begin with a newline.
            Space prefix = prefixTo(charStart(part));
            String valueSource = text(part);
            cursor = charEnd(part);
            return new J.Literal(randomId(), prefix, Markers.EMPTY,
                    str(((Nodes.StringNode) part).unescaped), valueSource, null,
                    JavaType.Primitive.String);
        }
        return convert(part);
    }

    @Override
    public J visitEmbeddedStatementsNode(Nodes.EmbeddedStatementsNode node) {
        prefixTo(charStart(node));
        skip("#{");
        J tree = node.statements == null ?
                new J.Empty(randomId(), EMPTY, Markers.EMPTY) :
                bodyStatement(node.statements);
        return new Rb.ComplexString.Value(randomId(), Markers.EMPTY, tree, sourceBefore("}"));
    }

    @Override
    public J visitEmbeddedVariableNode(Nodes.EmbeddedVariableNode node) {
        prefixTo(charStart(node));
        skip("#");
        return new Rb.ComplexString.Value(randomId(), Markers.EMPTY, convert(node.variable), EMPTY);
    }

    // ------------------------------------------------------------------ symbols

    @Override
    public J visitSymbolNode(Nodes.SymbolNode node) {
        return symbol(node, false);
    }

    /**
     * @param label {@code true} when this symbol is a hash key written as {@code foo:}, whose
     *              trailing colon belongs to the key/value separator rather than to the symbol.
     */
    private Rb.Symbol symbol(Nodes.SymbolNode node, boolean label) {
        Space prefix = prefix(node);
        int end = charEnd(node) - (label ? 1 : 0);
        String text = source.substring(cursor, end);

        String delimiter;
        String name;
        if (text.startsWith("%")) {
            delimiter = text.substring(0, 3);
            name = text.substring(3, text.length() - 1);
        } else if (text.startsWith(":\"") || text.startsWith(":'")) {
            delimiter = text.substring(0, 2);
            name = text.substring(2, text.length() - 1);
        } else if (text.startsWith(":")) {
            delimiter = ":";
            name = text.substring(1);
        } else if (text.startsWith("\"") || text.startsWith("'")) {
            delimiter = text.substring(0, 1);
            name = text.substring(1, text.length() - 1);
        } else {
            delimiter = "";
            name = text;
        }
        cursor = end;

        return new Rb.Symbol(randomId(), prefix, Markers.EMPTY, delimiter,
                new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), name, null, null),
                null);
    }

    @Override
    public J visitInterpolatedSymbolNode(Nodes.InterpolatedSymbolNode node) {
        Space prefix = prefix(node);
        String delimiter = source.substring(cursor, charStart(node.parts[0]));
        cursor = charStart(node.parts[0]);
        Expression name;
        if (node.parts.length == 1) {
            name = convertExpression(node.parts[0]);
        } else {
            List<JRightPadded<J>> converted = new ArrayList<>(node.parts.length);
            for (Nodes.Node part : node.parts) {
                converted.add(padRight(interpolatedPart(part), EMPTY));
            }
            name = new Rb.ComplexString(randomId(), EMPTY, Markers.EMPTY, "",
                    JContainer.build(converted), emptyList());
        }
        cursor = charEnd(node);
        return new Rb.Symbol(randomId(), prefix, Markers.EMPTY, delimiter, name, null);
    }

    // ------------------------------------------------------------------ variable reads

    @Override
    public J visitLocalVariableReadNode(Nodes.LocalVariableReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitLocalVariableTargetNode(Nodes.LocalVariableTargetNode node) {
        return identifier(node);
    }

    @Override
    public J visitInstanceVariableReadNode(Nodes.InstanceVariableReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitInstanceVariableTargetNode(Nodes.InstanceVariableTargetNode node) {
        return identifier(node);
    }

    @Override
    public J visitClassVariableReadNode(Nodes.ClassVariableReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitClassVariableTargetNode(Nodes.ClassVariableTargetNode node) {
        return identifier(node);
    }

    @Override
    public J visitGlobalVariableReadNode(Nodes.GlobalVariableReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitGlobalVariableTargetNode(Nodes.GlobalVariableTargetNode node) {
        return identifier(node);
    }

    @Override
    public J visitConstantReadNode(Nodes.ConstantReadNode node) {
        return identifier(node);
    }

    @Override
    public J visitConstantTargetNode(Nodes.ConstantTargetNode node) {
        return identifier(node);
    }

    @Override
    public J visitConstantPathNode(Nodes.ConstantPathNode node) {
        return constantPath(node, node.parent, str(node.name));
    }

    @Override
    public J visitConstantPathTargetNode(Nodes.ConstantPathTargetNode node) {
        return constantPath(node, node.parent, str(node.name));
    }

    private J constantPath(Nodes.Node node, Nodes.@Nullable Node parent, String name) {
        Space prefix = prefix(node);
        Expression left = parent == null ?
                new J.Empty(randomId(), EMPTY, Markers.EMPTY) :
                convertExpression(parent);
        return new J.MemberReference(
                randomId(),
                prefix,
                Markers.EMPTY,
                padRight(left, sourceBefore("::")),
                null,
                padLeft(EMPTY, identifier(name)),
                null,
                null,
                null
        );
    }

    // ------------------------------------------------------------------ assignment

    @Override
    public J visitLocalVariableWriteNode(Nodes.LocalVariableWriteNode node) {
        return assignment(node, str(node.name), node.value);
    }

    @Override
    public J visitInstanceVariableWriteNode(Nodes.InstanceVariableWriteNode node) {
        return assignment(node, str(node.name), node.value);
    }

    @Override
    public J visitClassVariableWriteNode(Nodes.ClassVariableWriteNode node) {
        return assignment(node, str(node.name), node.value);
    }

    @Override
    public J visitGlobalVariableWriteNode(Nodes.GlobalVariableWriteNode node) {
        return assignment(node, str(node.name), node.value);
    }

    @Override
    public J visitConstantWriteNode(Nodes.ConstantWriteNode node) {
        return assignment(node, str(node.name), node.value);
    }

    private J assignment(Nodes.Node node, String name, Nodes.Node value) {
        Space prefix = prefix(node);
        J.Identifier variable = identifier(name);
        return new J.Assignment(randomId(), prefix, Markers.EMPTY, variable,
                padLeft(sourceBefore("="), convertExpression(value)), null);
    }

    @Override
    public J visitConstantPathWriteNode(Nodes.ConstantPathWriteNode node) {
        Space prefix = prefix(node);
        Expression target = convertExpression(node.target);
        return new J.Assignment(randomId(), prefix, Markers.EMPTY, target,
                padLeft(sourceBefore("="), convertExpression(node.value)), null);
    }

    @Override
    public J visitLocalVariableOperatorWriteNode(Nodes.LocalVariableOperatorWriteNode node) {
        return operatorWrite(node, str(node.name), str(node.binary_operator), node.value);
    }

    @Override
    public J visitInstanceVariableOperatorWriteNode(Nodes.InstanceVariableOperatorWriteNode node) {
        return operatorWrite(node, str(node.name), str(node.binary_operator), node.value);
    }

    @Override
    public J visitClassVariableOperatorWriteNode(Nodes.ClassVariableOperatorWriteNode node) {
        return operatorWrite(node, str(node.name), str(node.binary_operator), node.value);
    }

    @Override
    public J visitGlobalVariableOperatorWriteNode(Nodes.GlobalVariableOperatorWriteNode node) {
        return operatorWrite(node, str(node.name), str(node.binary_operator), node.value);
    }

    @Override
    public J visitConstantOperatorWriteNode(Nodes.ConstantOperatorWriteNode node) {
        return operatorWrite(node, str(node.name), str(node.binary_operator), node.value);
    }

    @Override
    public J visitLocalVariableAndWriteNode(Nodes.LocalVariableAndWriteNode node) {
        return operatorWrite(node, str(node.name), "&&", node.value);
    }

    @Override
    public J visitInstanceVariableAndWriteNode(Nodes.InstanceVariableAndWriteNode node) {
        return operatorWrite(node, str(node.name), "&&", node.value);
    }

    @Override
    public J visitClassVariableAndWriteNode(Nodes.ClassVariableAndWriteNode node) {
        return operatorWrite(node, str(node.name), "&&", node.value);
    }

    @Override
    public J visitGlobalVariableAndWriteNode(Nodes.GlobalVariableAndWriteNode node) {
        return operatorWrite(node, str(node.name), "&&", node.value);
    }

    @Override
    public J visitConstantAndWriteNode(Nodes.ConstantAndWriteNode node) {
        return operatorWrite(node, str(node.name), "&&", node.value);
    }

    @Override
    public J visitLocalVariableOrWriteNode(Nodes.LocalVariableOrWriteNode node) {
        return operatorWrite(node, str(node.name), "||", node.value);
    }

    @Override
    public J visitInstanceVariableOrWriteNode(Nodes.InstanceVariableOrWriteNode node) {
        return operatorWrite(node, str(node.name), "||", node.value);
    }

    @Override
    public J visitClassVariableOrWriteNode(Nodes.ClassVariableOrWriteNode node) {
        return operatorWrite(node, str(node.name), "||", node.value);
    }

    @Override
    public J visitGlobalVariableOrWriteNode(Nodes.GlobalVariableOrWriteNode node) {
        return operatorWrite(node, str(node.name), "||", node.value);
    }

    @Override
    public J visitConstantOrWriteNode(Nodes.ConstantOrWriteNode node) {
        return operatorWrite(node, str(node.name), "||", node.value);
    }

    private J operatorWrite(Nodes.Node node, String name, String operator, Nodes.Node value) {
        Space prefix = prefix(node);
        J.Identifier variable = identifier(name);
        return operatorAssignment(prefix, variable, operator, value);
    }

    private J operatorAssignment(Space prefix, Expression target, String operator, Nodes.Node value) {
        Object type = assignmentOperationType(operator);
        Space before = sourceBefore(operator + "=");
        if (type instanceof Rb.AssignmentOperation.Type) {
            return new Rb.AssignmentOperation(randomId(), prefix, Markers.EMPTY, target,
                    padLeft(before, (Rb.AssignmentOperation.Type) type), convertExpression(value), null);
        }
        return new J.AssignmentOperation(randomId(), prefix, Markers.EMPTY, target,
                padLeft(before, (J.AssignmentOperation.Type) type), convertExpression(value), null);
    }

    private Object assignmentOperationType(String operator) {
        switch (operator) {
            case "+":
                return J.AssignmentOperation.Type.Addition;
            case "-":
                return J.AssignmentOperation.Type.Subtraction;
            case "*":
                return J.AssignmentOperation.Type.Multiplication;
            case "/":
                return J.AssignmentOperation.Type.Division;
            case "%":
                return J.AssignmentOperation.Type.Modulo;
            case "**":
                return J.AssignmentOperation.Type.Exponentiation;
            case "&":
                return J.AssignmentOperation.Type.BitAnd;
            case "|":
                return J.AssignmentOperation.Type.BitOr;
            case "^":
                return J.AssignmentOperation.Type.BitXor;
            case "<<":
                return J.AssignmentOperation.Type.LeftShift;
            case ">>":
                return J.AssignmentOperation.Type.RightShift;
            case "&&":
                return Rb.AssignmentOperation.Type.And;
            case "||":
                return Rb.AssignmentOperation.Type.Or;
            default:
                throw new UnsupportedOperationException("Unsupported assignment operator " + operator);
        }
    }

    @Override
    public J visitCallOperatorWriteNode(Nodes.CallOperatorWriteNode node) {
        return callWrite(node, node.receiver, str(node.read_name), str(node.binary_operator), node.value,
                node.isSafeNavigation());
    }

    @Override
    public J visitCallAndWriteNode(Nodes.CallAndWriteNode node) {
        return callWrite(node, node.receiver, str(node.read_name), "&&", node.value, node.isSafeNavigation());
    }

    @Override
    public J visitCallOrWriteNode(Nodes.CallOrWriteNode node) {
        return callWrite(node, node.receiver, str(node.read_name), "||", node.value, node.isSafeNavigation());
    }

    private J callWrite(Nodes.Node node, Nodes.@Nullable Node receiver, String name, String operator,
                        Nodes.Node value, boolean safeNavigation) {
        Space prefix = prefix(node);
        if (receiver == null) {
            return operatorAssignment(prefix, identifier(name), operator, value);
        }
        Expression target = convertExpression(receiver);
        Space beforeDot = whitespace();
        skip(safeNavigation ? "&." : ".");
        J.FieldAccess field = new J.FieldAccess(randomId(), EMPTY, Markers.EMPTY,
                target, padLeft(beforeDot, identifier(name)), null);
        return operatorAssignment(prefix, field, operator, value);
    }

    @Override
    public J visitIndexOperatorWriteNode(Nodes.IndexOperatorWriteNode node) {
        return indexWrite(node, node.receiver, node.arguments, str(node.binary_operator), node.value);
    }

    @Override
    public J visitIndexAndWriteNode(Nodes.IndexAndWriteNode node) {
        return indexWrite(node, node.receiver, node.arguments, "&&", node.value);
    }

    @Override
    public J visitIndexOrWriteNode(Nodes.IndexOrWriteNode node) {
        return indexWrite(node, node.receiver, node.arguments, "||", node.value);
    }

    private J indexWrite(Nodes.Node node, Nodes.Node receiver, Nodes.@Nullable ArgumentsNode arguments,
                         String operator, Nodes.Node value) {
        Space prefix = prefix(node);
        J.ArrayAccess access = arrayAccess(EMPTY, receiver,
                arguments == null ? new Nodes.Node[0] : arguments.arguments);
        return operatorAssignment(prefix, access, operator, value);
    }

    @Override
    public J visitMultiWriteNode(Nodes.MultiWriteNode node) {
        Space prefix = prefix(node);

        List<Nodes.Node> targets = new ArrayList<>();
        Collections.addAll(targets, node.lefts);
        if (node.rest != null && !(node.rest instanceof Nodes.ImplicitRestNode)) {
            targets.add(node.rest);
        }
        Collections.addAll(targets, node.rights);

        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        List<JRightPadded<Expression>> assignments = new ArrayList<>(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            Expression target = convertExpression(targets.get(i));
            assignments.add(padRight(target, i == targets.size() - 1 ?
                    maybeTrailingComma(markers, null) : sourceBefore(",")));
        }

        Space initializerPrefix = sourceBefore("=");
        List<JRightPadded<Expression>> initializers;
        if (node.value instanceof Nodes.ArrayNode && source.charAt(charStart(node.value)) != '[') {
            Nodes.Node[] values = ((Nodes.ArrayNode) node.value).elements;
            initializers = new ArrayList<>(values.length);
            for (int i = 0; i < values.length; i++) {
                initializers.add(padRight(convertExpression(values[i]),
                        i == values.length - 1 ? EMPTY : sourceBefore(",")));
            }
        } else {
            initializers = singletonList(padRight(convertExpression(node.value), EMPTY));
        }

        return new Rb.MultipleAssignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                JContainer.build(EMPTY, assignments, markers.get()),
                JContainer.build(initializerPrefix, initializers, Markers.EMPTY),
                null
        );
    }

    // ------------------------------------------------------------------ calls

    @Override
    public J visitCallNode(Nodes.CallNode node) {
        String name = str(node.name);

        if (name.equals("[]") && node.receiver != null) {
            Space prefix = prefix(node);
            return arrayAccess(prefix, node.receiver,
                    node.arguments == null ? new Nodes.Node[0] : node.arguments.arguments);
        }
        if (name.equals("[]=") && node.receiver != null && node.arguments != null) {
            return indexAssignment(node);
        }
        if (node.isAttributeWrite() && node.receiver != null && name.endsWith("=") &&
            node.arguments != null && node.arguments.arguments.length == 1) {
            return attributeAssignment(node, name);
        }
        if (node.isVariableCall() && node.receiver == null && node.arguments == null && node.block == null) {
            return identifier(node);
        }
        if (node.receiver != null && node.arguments == null && node.block == null &&
            UNARY_OPERATORS.containsKey(name) && node.startOffset < node.receiver.startOffset) {
            return unary(node, name);
        }
        if (node.receiver != null && node.arguments != null && node.arguments.arguments.length == 1 &&
            node.block == null && isInfixOperator(node, name)) {
            return binary(node, name);
        }

        Space prefix = prefix(node);
        if (node.receiver == null) {
            J.Identifier methodName = identifier(name);
            return new J.MethodInvocation(randomId(), prefix, Markers.EMPTY, null, null, methodName,
                    callArguments(node.arguments, node.block), null);
        }

        Expression receiver = convertExpression(node.receiver);
        Space beforeDot = whitespace();
        Markers markers = Markers.EMPTY;
        if (skip("&")) {
            markers = markers.add(new SafeNavigation(randomId()));
        }
        if (skip("::")) {
            markers = markers.add(new Colon2(randomId()));
        } else {
            skip(".");
        }

        J.Identifier methodName = identifier(name);
        if (name.equals("new")) {
            return new J.NewClass(
                    randomId(),
                    prefix,
                    markers,
                    padRight(new J.Empty(randomId(), EMPTY, markers), beforeDot),
                    methodName.getPrefix(),
                    asTypeTree(receiver),
                    callArguments(node.arguments, node.block),
                    null,
                    null
            );
        }
        return new J.MethodInvocation(randomId(), prefix, markers, padRight(receiver, beforeDot),
                null, methodName, callArguments(node.arguments, node.block), null);
    }

    @Override
    public J visitCallTargetNode(Nodes.CallTargetNode node) {
        Space prefix = prefix(node);
        Expression receiver = convertExpression(node.receiver);
        Space beforeDot = whitespace();
        Markers markers = Markers.EMPTY;
        if (skip("&")) {
            markers = markers.add(new SafeNavigation(randomId()));
        }
        skip(".");
        String name = str(node.name);
        return new J.FieldAccess(randomId(), prefix, markers, receiver,
                padLeft(beforeDot, identifier(name.endsWith("=") ?
                        name.substring(0, name.length() - 1) : name)), null);
    }

    @Override
    public J visitIndexTargetNode(Nodes.IndexTargetNode node) {
        Space prefix = prefix(node);
        return arrayAccess(prefix, node.receiver,
                node.arguments == null ? new Nodes.Node[0] : node.arguments.arguments);
    }

    @Override
    public J visitMatchWriteNode(Nodes.MatchWriteNode node) {
        return convert(node.call);
    }

    private J.ArrayAccess arrayAccess(Space prefix, Nodes.Node receiverNode, Nodes.Node[] indexNodes) {
        Expression receiver = convertExpression(receiverNode);
        Space beforeBracket = sourceBefore("[");
        Expression index = index(indexNodes);
        return new J.ArrayAccess(
                randomId(),
                prefix,
                Markers.EMPTY,
                receiver,
                new J.ArrayDimension(randomId(), beforeBracket, Markers.EMPTY,
                        padRight(index, sourceBefore("]"))),
                null
        );
    }

    private Expression index(Nodes.Node[] indexNodes) {
        if (indexNodes.length == 0) {
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        if (indexNodes.length == 1) {
            return convertExpression(indexNodes[0]);
        }
        if (indexNodes.length == 2) {
            Expression start = convertExpression(indexNodes[0]);
            return new Rb.SubArrayIndex(randomId(), start.getPrefix(), Markers.EMPTY,
                    start.withPrefix(EMPTY),
                    padLeft(sourceBefore(","), convertExpression(indexNodes[1])));
        }
        List<JRightPadded<Expression>> elements = new ArrayList<>(indexNodes.length);
        for (int i = 0; i < indexNodes.length; i++) {
            elements.add(padRight(convertExpression(indexNodes[i]),
                    i == indexNodes.length - 1 ? EMPTY : sourceBefore(",")));
        }
        return new Rb.Array(randomId(), EMPTY, Markers.EMPTY,
                JContainer.build(EMPTY, elements, Markers.EMPTY.add(new OmitParentheses(randomId()))),
                null);
    }

    private J indexAssignment(Nodes.CallNode node) {
        Space prefix = prefix(node);
        Nodes.Node[] all = node.arguments.arguments;
        Nodes.Node[] indexes = Arrays.copyOf(all, all.length - 1);
        J.ArrayAccess access = arrayAccess(EMPTY, node.receiver, indexes);
        return new J.Assignment(randomId(), prefix, Markers.EMPTY, access,
                padLeft(sourceBefore("="), convertExpression(all[all.length - 1])), null);
    }

    private J attributeAssignment(Nodes.CallNode node, String name) {
        Space prefix = prefix(node);
        Expression receiver = convertExpression(node.receiver);
        Space beforeDot = whitespace();
        Markers markers = Markers.EMPTY;
        if (skip("&")) {
            markers = markers.add(new SafeNavigation(randomId()));
        }
        skip(".");
        J.FieldAccess field = new J.FieldAccess(randomId(), EMPTY, markers, receiver,
                padLeft(beforeDot, identifier(name.substring(0, name.length() - 1))), null);
        return new J.Assignment(randomId(), prefix, Markers.EMPTY, field,
                padLeft(sourceBefore("="), convertExpression(node.arguments.arguments[0])), null);
    }

    // ------------------------------------------------------------------ operators

    private static final Map<String, J.Unary.Type> UNARY_OPERATORS = new HashMap<>();

    static {
        UNARY_OPERATORS.put("!", J.Unary.Type.Not);
        UNARY_OPERATORS.put("~", J.Unary.Type.Complement);
        UNARY_OPERATORS.put("-@", J.Unary.Type.Negative);
        UNARY_OPERATORS.put("+@", J.Unary.Type.Positive);
    }

    private J unary(Nodes.CallNode node, String name) {
        Space prefix = prefix(node);
        String operator = name.endsWith("@") ? name.substring(0, name.length() - 1) : name;
        Markers markers = Markers.EMPTY;
        if (operator.equals("!") && peekKeyword("not")) {
            operator = "not";
            markers = markers.add(new EnglishOperator(randomId()));
        }
        return new J.Unary(randomId(), prefix, markers,
                padLeft(sourceBefore(operator), UNARY_OPERATORS.get(name)),
                convertExpression(node.receiver), null);
    }

    private boolean isInfixOperator(Nodes.CallNode node, String name) {
        if (!BINARY_OPERATORS.containsKey(name) && !RUBY_BINARY_OPERATORS.containsKey(name)) {
            return false;
        }
        // `a.+(b)` is a method call written with a dot, not an infix expression
        int at = indexOfNextNonWhitespace(charEnd(node.receiver));
        return source.startsWith(name, at);
    }

    private J binary(Nodes.CallNode node, String name) {
        Space prefix = prefix(node);
        Expression left = convertExpression(node.receiver);
        Space beforeOperator = sourceBefore(name);
        Nodes.Node right = node.arguments.arguments[0];
        J.Binary.Type javaType = BINARY_OPERATORS.get(name);
        if (javaType != null) {
            return new J.Binary(randomId(), prefix, Markers.EMPTY, left,
                    padLeft(beforeOperator, javaType), convertExpression(right), null);
        }
        return new Rb.Binary(randomId(), prefix, Markers.EMPTY, left,
                padLeft(beforeOperator, RUBY_BINARY_OPERATORS.get(name)), convertExpression(right), null);
    }

    private static final Map<String, J.Binary.Type> BINARY_OPERATORS = new HashMap<>();
    private static final Map<String, Rb.Binary.Type> RUBY_BINARY_OPERATORS = new HashMap<>();

    static {
        BINARY_OPERATORS.put("+", J.Binary.Type.Addition);
        BINARY_OPERATORS.put("-", J.Binary.Type.Subtraction);
        BINARY_OPERATORS.put("*", J.Binary.Type.Multiplication);
        BINARY_OPERATORS.put("/", J.Binary.Type.Division);
        BINARY_OPERATORS.put("%", J.Binary.Type.Modulo);
        BINARY_OPERATORS.put(">>", J.Binary.Type.RightShift);
        BINARY_OPERATORS.put("<<", J.Binary.Type.LeftShift);
        BINARY_OPERATORS.put("&", J.Binary.Type.BitAnd);
        BINARY_OPERATORS.put("|", J.Binary.Type.BitOr);
        BINARY_OPERATORS.put("^", J.Binary.Type.BitXor);
        BINARY_OPERATORS.put("==", J.Binary.Type.Equal);
        BINARY_OPERATORS.put("!=", J.Binary.Type.NotEqual);
        BINARY_OPERATORS.put("<", J.Binary.Type.LessThan);
        BINARY_OPERATORS.put("<=", J.Binary.Type.LessThanOrEqual);
        BINARY_OPERATORS.put(">", J.Binary.Type.GreaterThan);
        BINARY_OPERATORS.put(">=", J.Binary.Type.GreaterThanOrEqual);

        RUBY_BINARY_OPERATORS.put("**", Rb.Binary.Type.Exponentiation);
        RUBY_BINARY_OPERATORS.put("===", Rb.Binary.Type.Within);
        RUBY_BINARY_OPERATORS.put("<=>", Rb.Binary.Type.Comparison);
        RUBY_BINARY_OPERATORS.put("=~", Rb.Binary.Type.Match);
    }

    @Override
    public J visitAndNode(Nodes.AndNode node) {
        return logical(node, node.left, node.right, J.Binary.Type.And, "&&", "and");
    }

    @Override
    public J visitOrNode(Nodes.OrNode node) {
        return logical(node, node.left, node.right, J.Binary.Type.Or, "||", "or");
    }

    private J logical(Nodes.Node node, Nodes.Node left, Nodes.Node right, J.Binary.Type type,
                      String symbolic, String english) {
        Space prefix = prefix(node);
        Expression leftExpr = convertExpression(left);
        Space beforeOperator = whitespace();
        boolean isEnglish = !source.startsWith(symbolic, cursor);
        skip(isEnglish ? english : symbolic);
        return new J.Binary(
                randomId(),
                prefix,
                isEnglish ? Markers.EMPTY.add(new EnglishOperator(randomId())) : Markers.EMPTY,
                leftExpr,
                padLeft(beforeOperator, type),
                convertExpression(right),
                null
        );
    }

    @Override
    public J visitRangeNode(Nodes.RangeNode node) {
        return range(node, node.left, node.right, node.isExcludeEnd() ?
                Rb.Binary.Type.RangeExclusive : Rb.Binary.Type.RangeInclusive);
    }

    @Override
    public J visitFlipFlopNode(Nodes.FlipFlopNode node) {
        return range(node, node.left, node.right, node.isExcludeEnd() ?
                Rb.Binary.Type.FlipFlopExclusive : Rb.Binary.Type.FlipFlopInclusive);
    }

    private J range(Nodes.Node node, Nodes.@Nullable Node left, Nodes.@Nullable Node right,
                    Rb.Binary.Type type) {
        Space prefix = prefix(node);
        Expression leftExpr = left == null ?
                new J.Empty(randomId(), EMPTY, Markers.EMPTY) : convertExpression(left);
        Space beforeOperator = whitespace();
        boolean exclusive = type == Rb.Binary.Type.RangeExclusive ||
                            type == Rb.Binary.Type.FlipFlopExclusive;
        skip(exclusive ? "..." : "..");
        Expression rightExpr = right == null ?
                new J.Empty(randomId(), EMPTY, Markers.EMPTY) : convertExpression(right);
        return new Rb.Binary(randomId(), prefix, Markers.EMPTY, leftExpr,
                padLeft(beforeOperator, type), rightExpr, null);
    }

    @Override
    public J visitDefinedNode(Nodes.DefinedNode node) {
        Space prefix = prefix(node);
        skip("defined?");
        // Prism models no node for the optional parentheses around the operand
        Space beforeValue = whitespace();
        Expression value;
        if (skip("(")) {
            value = new J.Parentheses<>(randomId(), beforeValue, Markers.EMPTY,
                    padRight(convertExpression(node.value), sourceBefore(")")));
        } else {
            value = convertExpression(node.value).withPrefix(beforeValue);
        }
        return new Rb.Unary(randomId(), prefix, Markers.EMPTY, Rb.Unary.Type.Defined, value);
    }

    @Override
    public J visitParenthesesNode(Nodes.ParenthesesNode node) {
        Space prefix = prefix(node);
        skip("(");
        J body = node.body == null ?
                new J.Empty(randomId(), EMPTY, Markers.EMPTY) :
                node.body instanceof Nodes.StatementsNode ?
                        bodyStatement((Nodes.StatementsNode) node.body) :
                        convert(node.body);
        return new J.Parentheses<>(randomId(), prefix, Markers.EMPTY, padRight(body, sourceBefore(")")));
    }

    // ------------------------------------------------------------------ argument lists

    /**
     * @param block Either a {@code BlockArgumentNode} ({@code &b}, printed inside the parentheses)
     *              or a {@code BlockNode} ({@code do...end} / <code>{...}</code>, printed after
     *              them). The printer pulls a trailing {@link Rb.Block} back out of the container.
     */
    private JContainer<Expression> callArguments(Nodes.@Nullable ArgumentsNode arguments,
                                                 Nodes.@Nullable Node block) {
        List<Nodes.Node> args = new ArrayList<>();
        if (arguments != null) {
            Collections.addAll(args, arguments.arguments);
        }
        if (block instanceof Nodes.BlockArgumentNode) {
            args.add(block);
        }
        Nodes.Node trailingBlock = block instanceof Nodes.BlockNode ? block : null;

        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        AtomicReference<Boolean> parenthesized = new AtomicReference<>(Boolean.FALSE);

        Optional<JContainer<Expression>> built = peekWhitespace(0, (n, prefix) -> {
            // A `(` that starts exactly where the arguments start belongs to the first argument
            // (`print ("x")`), not to the call.
            boolean parens = source.startsWith("(", cursor) &&
                             (arguments == null || cursor < charStart(arguments));
            if (parens) {
                skip("(");
                parenthesized.set(Boolean.TRUE);
            } else {
                markers.set(markers.get().add(new OmitParentheses(randomId())));
            }

            List<JRightPadded<Expression>> mapped = new ArrayList<>(args.size());
            for (int i = 0; i < args.size(); i++) {
                Expression arg = convertExpression(args.get(i));
                mapped.add(padRight(arg, i == args.size() - 1 ?
                        maybeTrailingComma(markers, parens ? ")" : null) : sourceBefore(",")));
            }

            if (mapped.isEmpty()) {
                if (!parens) {
                    if (trailingBlock == null) {
                        return null;
                    }
                } else {
                    mapped = new ArrayList<>(singletonList(padRight(
                            (Expression) new J.Empty(randomId(), EMPTY, Markers.EMPTY), sourceBefore(")"))));
                }
            }

            if (trailingBlock != null) {
                mapped.add(padRight((Expression) convert(trailingBlock), EMPTY));
            }

            return JContainer.build(prefix, mapped, markers.get());
        });

        return built.orElseGet(() -> JContainer.<Expression>empty().withMarkers(markers.get()));
    }

    @Override
    public J visitBlockArgumentNode(Nodes.BlockArgumentNode node) {
        Space prefix = prefix(node);
        skip("&");
        return new Rb.BlockArgument(randomId(), prefix, Markers.EMPTY,
                node.expression == null ?
                        new J.Empty(randomId(), EMPTY, Markers.EMPTY) :
                        convertExpression(node.expression));
    }

    @Override
    public J visitForwardingArgumentsNode(Nodes.ForwardingArgumentsNode node) {
        return identifier(node);
    }

    @Override
    public J visitBlockNode(Nodes.BlockNode node) {
        Space prefix = prefix(node);
        boolean inline = source.charAt(cursor) == '{';
        skip(inline ? "{" : "do");

        JContainer<J> parameters = node.parameters == null ? null : blockParameters(node.parameters);
        J.Block body = new J.Block(randomId(), whitespace(), Markers.EMPTY, JRightPadded.build(false),
                bodyStatements(node.body), sourceBefore(inline ? "}" : "end"));

        return new Rb.Block(randomId(), prefix, Markers.EMPTY, inline, parameters, body);
    }

    private JContainer<J> blockParameters(Nodes.Node parameters) {
        Space before = whitespace();
        boolean pipes = source.charAt(cursor) == '|';
        skip(pipes ? "|" : "(");
        List<Nodes.Node> params = new ArrayList<>();
        if (parameters instanceof Nodes.BlockParametersNode) {
            Nodes.BlockParametersNode block = (Nodes.BlockParametersNode) parameters;
            if (block.parameters != null) {
                collectParameters(block.parameters, params);
            }
            Collections.addAll(params, block.locals);
        } else if (parameters instanceof Nodes.ParametersNode) {
            collectParameters((Nodes.ParametersNode) parameters, params);
        } else {
            params.add(parameters);
        }

        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        List<JRightPadded<J>> mapped = new ArrayList<>(params.size());
        for (int i = 0; i < params.size(); i++) {
            J param = parameter(params.get(i));
            mapped.add(padRight(param, i == params.size() - 1 ?
                    maybeTrailingComma(markers, pipes ? "|" : ")") : sourceBefore(",")));
        }
        if (mapped.isEmpty()) {
            mapped = singletonList(padRight((J) new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                    sourceBefore(pipes ? "|" : ")")));
        }
        return JContainer.build(before, mapped, markers.get());
    }

    @Override
    public J visitLambdaNode(Nodes.LambdaNode node) {
        Space prefix = prefix(node);
        skip("->");
        Space parametersPrefix = whitespace();
        List<JRightPadded<J>> params = node.parameters == null ?
                emptyList() : blockParameters(node.parameters).getPadding().getElements();
        return new J.Lambda(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.Lambda.Parameters(randomId(), parametersPrefix, Markers.EMPTY, true, params),
                EMPTY,
                new J.Block(randomId(), sourceBefore("{"), Markers.EMPTY, JRightPadded.build(false),
                        bodyStatements(node.body), sourceBefore("}")),
                null
        );
    }

    // ------------------------------------------------------------------ arrays and hashes

    @Override
    public J visitArrayNode(Nodes.ArrayNode node) {
        Space prefix = prefix(node);
        String delimiter = source.charAt(cursor) == '%' ? readDelimiter(cursor) : null;
        if (delimiter != null) {
            skip(delimiter);
            int closing = charEnd(node) - StringUtils.endDelimiter(delimiter).length();
            List<JRightPadded<Expression>> elements = new ArrayList<>(node.elements.length);
            for (int i = 0; i < node.elements.length; i++) {
                Expression element = delimitedArrayElement(node.elements[i]);
                elements.add(padRight(element, prefixTo(i == node.elements.length - 1 ?
                        closing : charStart(node.elements[i + 1]))));
            }
            cursor = charEnd(node);
            return new Rb.DelimitedArray(randomId(), prefix, Markers.EMPTY, delimiter,
                    JContainer.build(EMPTY, elements, Markers.EMPTY), null);
        }
        return array(prefix, node.elements);
    }

    /**
     * Inside {@code %w}/{@code %i} literals the elements have no delimiters of their own, so they
     * are read from their exact spans rather than by scanning for a delimiter.
     */
    private Expression delimitedArrayElement(Nodes.Node element) {
        Space prefix = prefixTo(charStart(element));
        if (element instanceof Nodes.StringNode) {
            String valueSource = text(element);
            cursor = charEnd(element);
            return new J.Literal(randomId(), prefix, Markers.EMPTY,
                    str(((Nodes.StringNode) element).unescaped), valueSource, null,
                    JavaType.Primitive.String);
        }
        if (element instanceof Nodes.SymbolNode) {
            String name = text(element);
            cursor = charEnd(element);
            return new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), name, null, null);
        }
        Nodes.Node[] parts = element instanceof Nodes.InterpolatedSymbolNode ?
                ((Nodes.InterpolatedSymbolNode) element).parts :
                element instanceof Nodes.InterpolatedStringNode ?
                        ((Nodes.InterpolatedStringNode) element).parts : null;
        if (parts != null) {
            List<JRightPadded<J>> converted = new ArrayList<>(parts.length);
            for (Nodes.Node part : parts) {
                converted.add(padRight(interpolatedPart(part), EMPTY));
            }
            cursor = charEnd(element);
            return new Rb.ComplexString(randomId(), prefix, Markers.EMPTY, "",
                    JContainer.build(converted), emptyList());
        }
        return convertExpression(element).withPrefix(prefix);
    }

    private Rb.Array array(Space prefix, Nodes.Node[] elements) {
        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        Space before = whitespace();
        boolean brackets = source.startsWith("[", cursor);
        if (brackets) {
            skip("[");
        } else {
            markers.set(markers.get().add(new OmitParentheses(randomId())));
        }
        List<JRightPadded<Expression>> mapped = new ArrayList<>(elements.length);
        for (int i = 0; i < elements.length; i++) {
            mapped.add(padRight(convertExpression(elements[i]), i == elements.length - 1 ?
                    maybeTrailingComma(markers, brackets ? "]" : null) : sourceBefore(",")));
        }
        if (mapped.isEmpty() && brackets) {
            mapped = singletonList(padRight((Expression) new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                    sourceBefore("]")));
        }
        return new Rb.Array(randomId(), prefix, Markers.EMPTY,
                JContainer.build(before, mapped, markers.get()), null);
    }

    @Override
    public J visitSplatNode(Nodes.SplatNode node) {
        Space prefix = prefix(node);
        skip("*");
        return new Rb.Splat(randomId(), prefix, Markers.EMPTY,
                node.expression == null ?
                        new J.Empty(randomId(), EMPTY, Markers.EMPTY) :
                        convertExpression(node.expression));
    }

    @Override
    public J visitImplicitRestNode(Nodes.ImplicitRestNode node) {
        return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
    }

    @Override
    public J visitImplicitNode(Nodes.ImplicitNode node) {
        // `{x:}` and `in {x:}` both elide the value; nothing is printed for it
        return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
    }

    @Override
    public J visitHashNode(Nodes.HashNode node) {
        return hash(prefix(node), node.elements, null);
    }

    @Override
    public J visitKeywordHashNode(Nodes.KeywordHashNode node) {
        if (node.elements.length == 1 && node.elements[0] instanceof Nodes.AssocSplatNode) {
            return convert(node.elements[0]);
        }
        return hash(prefix(node), node.elements, null);
    }

    @Override
    public J visitAssocSplatNode(Nodes.AssocSplatNode node) {
        Space prefix = prefix(node);
        skip("**");
        return convertExpression(node.value)
                .withPrefix(prefix)
                .withMarkers(Markers.EMPTY.add(new KeywordRestArgument(randomId())));
    }

    private Rb.Hash hash(Space prefix, Nodes.Node[] elements, Nodes.@Nullable Node rest) {
        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        Space before = whitespace();
        boolean braces = source.startsWith("{", cursor);
        Markers hashMarkers = Markers.EMPTY;
        if (braces) {
            skip("{");
        } else {
            hashMarkers = hashMarkers.add(new OmitParentheses(randomId()));
        }

        List<Nodes.Node> all = new ArrayList<>(Arrays.asList(elements));
        if (rest != null) {
            all.add(rest);
        }

        List<JRightPadded<Expression>> pairs = new ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            Expression pair = all.get(i) instanceof Nodes.AssocNode ?
                    keyValue((Nodes.AssocNode) all.get(i)) :
                    convertExpression(all.get(i));
            pairs.add(padRight(pair, i == all.size() - 1 ?
                    maybeTrailingComma(markers, braces ? "}" : null) : sourceBefore(",")));
        }
        if (pairs.isEmpty()) {
            pairs = singletonList(padRight((Expression) new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                    braces ? sourceBefore("}") : EMPTY));
        }

        return new Rb.Hash(randomId(), prefix, hashMarkers,
                JContainer.build(before, pairs, markers.get()), null);
    }

    private Rb.Hash.KeyValue keyValue(Nodes.AssocNode node) {
        Space prefix = prefix(node);
        boolean label = node.key instanceof Nodes.SymbolNode && text(node.key).endsWith(":");
        Expression key = label ?
                symbol((Nodes.SymbolNode) node.key, true) :
                convertExpression(node.key);
        Space separatorPrefix = whitespace();
        Rb.Hash.KeyValue.Separator separator;
        if (label) {
            skip(":");
            separator = Rb.Hash.KeyValue.Separator.Colon;
        } else {
            skip("=>");
            separator = Rb.Hash.KeyValue.Separator.Rocket;
        }
        return new Rb.Hash.KeyValue(randomId(), prefix, Markers.EMPTY, key,
                padLeft(separatorPrefix, separator), convertExpression(node.value), null);
    }

    // ------------------------------------------------------------------ control flow

    @Override
    public J visitIfNode(Nodes.IfNode node) {
        Space prefix = whitespace();
        if (peekKeyword("if")) {
            return conditional(node.predicate, node.statements, node.subsequent, "if", Markers.EMPTY)
                    .withPrefix(prefix);
        }
        if (node.subsequent instanceof Nodes.ElseNode) {
            return ternary(node).withPrefix(prefix);
        }
        return modifier(node.predicate, node.statements, "if", Markers.EMPTY).withPrefix(prefix);
    }

    @Override
    public J visitUnlessNode(Nodes.UnlessNode node) {
        Space prefix = whitespace();
        Markers markers = Markers.EMPTY.add(new Unless(randomId()));
        if (peekKeyword("unless")) {
            return conditional(node.predicate, node.statements, node.else_clause, "unless", markers)
                    .withPrefix(prefix);
        }
        return modifier(node.predicate, node.statements, "unless", markers).withPrefix(prefix);
    }

    private J.Ternary ternary(Nodes.IfNode node) {
        return new J.Ternary(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                convertExpression(node.predicate),
                padLeft(sourceBefore("?"), bodyExpression(node.statements)),
                padLeft(sourceBefore(":"),
                        bodyExpression(((Nodes.ElseNode) node.subsequent).statements)),
                null
        );
    }

    private J.If modifier(Nodes.Node predicate, Nodes.@Nullable StatementsNode statements,
                          String keyword, Markers markers) {
        Statement then = bodyStatement(statements);
        JRightPadded<Statement> thenPart = padRight(then, whitespace());
        skip(keyword);
        Space conditionPrefix = whitespace();
        return new J.If(
                randomId(),
                EMPTY,
                markers.add(new IfModifier(randomId())),
                new J.ControlParentheses<>(randomId(), conditionPrefix, Markers.EMPTY,
                        padRight(convertExpression(predicate), EMPTY)),
                thenPart,
                null
        );
    }

    private J.If conditional(Nodes.Node predicate, Nodes.@Nullable StatementsNode statements,
                             Nodes.@Nullable Node subsequent, String keyword, Markers markers) {
        skip(keyword);
        Space conditionPrefix = whitespace();
        Expression condition = convertExpression(predicate);
        boolean explicitThen = source.startsWith("then", indexOfNextNonWhitespace(cursor));
        J.ControlParentheses<Expression> control = new J.ControlParentheses<>(
                randomId(),
                conditionPrefix,
                explicitThen ? Markers.EMPTY.add(new ExplicitThen(randomId())) : Markers.EMPTY,
                padRight(condition, explicitThen ? sourceBefore("then") : EMPTY)
        );

        Statement then = bodyStatement(statements);
        J.If.Else anElse = null;
        JRightPadded<Statement> thenPart;
        if (subsequent == null) {
            thenPart = padRight(then, sourceBefore("end"));
        } else {
            thenPart = padRight(then, EMPTY);
            Space elsePrefix = whitespace();
            if (subsequent instanceof Nodes.IfNode) {
                skip("els");
                anElse = new J.If.Else(randomId(), elsePrefix, Markers.EMPTY,
                        padRight(convertStatement(subsequent), EMPTY));
            } else {
                skip("else");
                Statement elseBody = bodyStatement(((Nodes.ElseNode) subsequent).statements);
                if (elseBody instanceof J.If) {
                    // The printer reads a bare `J.If` in the else part as an `elsif`, so an `else`
                    // whose whole body is a conditional has to be wrapped to stay distinguishable.
                    elseBody = new J.Block(randomId(), EMPTY, Markers.EMPTY, JRightPadded.build(false),
                            singletonList(padRight(elseBody, EMPTY)), EMPTY);
                }
                anElse = new J.If.Else(randomId(), elsePrefix, Markers.EMPTY,
                        padRight(elseBody, sourceBefore("end")));
            }
        }
        return new J.If(randomId(), EMPTY, markers, control, thenPart, anElse);
    }

    @Override
    public J visitWhileNode(Nodes.WhileNode node) {
        return loop(node, node.predicate, node.statements, Markers.EMPTY, "while");
    }

    @Override
    public J visitUntilNode(Nodes.UntilNode node) {
        return loop(node, node.predicate, node.statements, Markers.EMPTY.add(new Until(randomId())), "until");
    }

    private J loop(Nodes.Node node, Nodes.Node predicate, Nodes.@Nullable StatementsNode statements,
                   Markers markers, String keyword) {
        Space prefix = prefix(node);
        boolean isModifier = statements != null && charStart(statements) == charStart(node);
        if (!isModifier) {
            skip(keyword);
            Space conditionPrefix = whitespace();
            Expression condition = convertExpression(predicate);
            boolean explicitDo = peekKeywordAt("do", indexOfNextNonWhitespace(cursor));
            return new J.WhileLoop(
                    randomId(),
                    prefix,
                    markers,
                    new J.ControlParentheses<>(randomId(), conditionPrefix,
                            explicitDo ? Markers.EMPTY.add(new ExplicitDo(randomId())) : Markers.EMPTY,
                            padRight(condition, explicitDo ? sourceBefore("do") : EMPTY)),
                    padRight(bodyStatement(statements), sourceBefore("end"))
            );
        }
        JRightPadded<Statement> body = padRight(bodyStatement(statements), whitespace());
        skip(keyword);
        Space conditionPrefix = whitespace();
        return new J.WhileLoop(
                randomId(),
                prefix,
                markers.add(new WhileModifier(randomId())),
                new J.ControlParentheses<>(randomId(), conditionPrefix, Markers.EMPTY,
                        padRight(convertExpression(predicate), EMPTY)),
                body
        );
    }

    @Override
    public J visitForNode(Nodes.ForNode node) {
        Space prefix = prefix(node);
        skip("for");
        Expression index = convertExpression(node.index);
        JRightPadded<Statement> variable = padRight(variableDeclaration(index), sourceBefore("in"));
        JRightPadded<Expression> iterable = padRight(convertExpression(node.collection), whitespace());
        return new J.ForEachLoop(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.ForEachLoop.Control(randomId(), EMPTY, Markers.EMPTY, variable, iterable),
                padRight(bodyStatement(node.statements), sourceBefore("end"))
        );
    }

    private J.VariableDeclarations variableDeclaration(Expression name) {
        return new J.VariableDeclarations(
                randomId(),
                name.getPrefix(),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(), EMPTY, Markers.EMPTY, name.withPrefix(EMPTY), emptyList(), null, null
                ), EMPTY))
        );
    }

    @Override
    public J visitBreakNode(Nodes.BreakNode node) {
        Space prefix = prefix(node);
        skip("break");
        return new Rb.Break(randomId(), prefix, Markers.EMPTY,
                new J.Break(randomId(), EMPTY, Markers.EMPTY, null), argumentValue(node.arguments));
    }

    @Override
    public J visitNextNode(Nodes.NextNode node) {
        Space prefix = prefix(node);
        skip("next");
        return new Rb.Next(randomId(), prefix, Markers.EMPTY,
                new J.Continue(randomId(), EMPTY, Markers.EMPTY, null), argumentValue(node.arguments));
    }

    private Expression argumentValue(Nodes.@Nullable ArgumentsNode arguments) {
        if (arguments == null || arguments.arguments.length == 0) {
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        if (arguments.arguments.length == 1) {
            return convertExpression(arguments.arguments[0]);
        }
        return array(whitespace(), arguments.arguments);
    }

    @Override
    public J visitRedoNode(Nodes.RedoNode node) {
        Space prefix = prefix(node);
        skip("redo");
        return new Rb.Redo(randomId(), prefix, Markers.EMPTY);
    }

    @Override
    public J visitRetryNode(Nodes.RetryNode node) {
        Space prefix = prefix(node);
        skip("retry");
        return new Rb.Retry(randomId(), prefix, Markers.EMPTY);
    }

    @Override
    public J visitReturnNode(Nodes.ReturnNode node) {
        Space prefix = prefix(node);
        skip("return");
        return new J.Return(randomId(), prefix, Markers.EMPTY, argumentValue(node.arguments));
    }

    @Override
    public J visitYieldNode(Nodes.YieldNode node) {
        Space prefix = prefix(node);
        skip("yield");
        //noinspection unchecked
        return new Rb.Yield(randomId(), prefix, Markers.EMPTY,
                (JContainer<Statement>) (JContainer<?>) callArguments(node.arguments, null));
    }

    @Override
    public J visitSuperNode(Nodes.SuperNode node) {
        Space prefix = prefix(node);
        J.Identifier name = identifier("super");
        return new J.MethodInvocation(randomId(), prefix, Markers.EMPTY, null, null, name,
                callArguments(node.arguments, node.block), null);
    }

    @Override
    public J visitForwardingSuperNode(Nodes.ForwardingSuperNode node) {
        Space prefix = prefix(node);
        J.Identifier name = identifier("super");
        return new J.MethodInvocation(randomId(), prefix, Markers.EMPTY, null, null, name,
                callArguments(null, node.block), null);
    }

    @Override
    public J visitPreExecutionNode(Nodes.PreExecutionNode node) {
        Space prefix = prefix(node);
        skip("BEGIN");
        return new Rb.PreExecution(randomId(), prefix, Markers.EMPTY, braceBlock(node.statements));
    }

    @Override
    public J visitPostExecutionNode(Nodes.PostExecutionNode node) {
        Space prefix = prefix(node);
        skip("END");
        return new Rb.PostExecution(randomId(), prefix, Markers.EMPTY, braceBlock(node.statements));
    }

    private J.Block braceBlock(Nodes.@Nullable StatementsNode statements) {
        Space prefix = sourceBefore("{");
        return new J.Block(randomId(), prefix, Markers.EMPTY, JRightPadded.build(false),
                statements(statements), sourceBefore("}"));
    }

    @Override
    public J visitBeginNode(Nodes.BeginNode node) {
        if (node.rescue_clause == null && node.else_clause == null && node.ensure_clause == null) {
            Space prefix = whitespace();
            skip("begin");
            return new Rb.Begin(randomId(), prefix, Markers.EMPTY,
                    new J.Block(randomId(), whitespace(), Markers.EMPTY, JRightPadded.build(false),
                            statements(node.statements), sourceBefore("end")));
        }
        return rescue(node);
    }

    private J rescue(Nodes.BeginNode node) {
        Space prefix = whitespace();
        boolean explicitBegin = skip("begin");
        Space tryPrefix = whitespace();

        J.Block body = new J.Block(randomId(), EMPTY, Markers.EMPTY, JRightPadded.build(false),
                statements(node.statements), EMPTY);

        List<J.Try.Catch> catches = new ArrayList<>(2);
        for (Nodes.RescueNode r = node.rescue_clause; r != null; r = r.subsequent) {
            catches.add(rescueClause(r));
        }

        J.Block elseBlock = null;
        if (node.else_clause != null) {
            Space elsePrefix = sourceBefore("else");
            elseBlock = new J.Block(randomId(), elsePrefix, Markers.EMPTY, JRightPadded.build(false),
                    statements(node.else_clause.statements), EMPTY);
        }

        Space tail = whitespace();
        J.Block finallyBlock = null;
        if (node.ensure_clause != null) {
            skip("ensure");
            List<JRightPadded<Statement>> ensured = statements(node.ensure_clause.statements);
            Space beforeEnd = whitespace();
            if (explicitBegin) {
                skip("end");
            }
            finallyBlock = new J.Block(randomId(), tail, Markers.EMPTY, JRightPadded.build(false),
                    ensured, beforeEnd);
        } else {
            if (elseBlock != null) {
                elseBlock = elseBlock.withEnd(tail);
            } else if (!catches.isEmpty()) {
                catches = ListUtils.mapLast(catches, c -> c.withBody(c.getBody().withEnd(tail)));
            } else {
                body = body.withEnd(tail);
            }
            if (explicitBegin) {
                skip("end");
            }
        }

        return new Rb.Rescue(
                randomId(),
                prefix,
                explicitBegin ? Markers.EMPTY.add(new ExplicitBegin(randomId())) : Markers.EMPTY,
                new J.Try(randomId(), tryPrefix, Markers.EMPTY, JContainer.empty(), body, catches,
                        finallyBlock == null ? null : padLeft(EMPTY, finallyBlock)),
                elseBlock
        );
    }

    private J.Try.Catch rescueClause(Nodes.RescueNode node) {
        Space prefix = sourceBefore("rescue");
        Space typesPrefix = whitespace();

        List<JRightPadded<NameTree>> exceptionTypes = new ArrayList<>(node.exceptions.length);
        for (int i = 0; i < node.exceptions.length; i++) {
            NameTree type = asTypeTree(convertExpression(node.exceptions[i]));
            exceptionTypes.add(padRight(type, i == node.exceptions.length - 1 ? EMPTY : sourceBefore(",")));
        }

        Space beforeName = whitespace();
        List<JRightPadded<J.VariableDeclarations.NamedVariable>> names = new ArrayList<>(1);
        Space bodyPrefix = beforeName;
        if (skip("=>")) {
            names.add(padRight(new J.VariableDeclarations.NamedVariable(randomId(), beforeName,
                    Markers.EMPTY, identifier(node.reference), emptyList(), null, null), EMPTY));
            bodyPrefix = whitespace();
        }

        TypeTree exceptionType = exceptionTypes.size() == 1 ?
                (TypeTree) exceptionTypes.get(0).getElement() :
                new J.MultiCatch(randomId(), EMPTY, Markers.EMPTY, exceptionTypes);

        return new J.Try.Catch(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.ControlParentheses<>(randomId(), EMPTY, Markers.EMPTY, padRight(
                        new J.VariableDeclarations(randomId(), typesPrefix, Markers.EMPTY, emptyList(),
                                emptyList(), exceptionType, null, names), EMPTY)),
                new J.Block(randomId(), bodyPrefix, Markers.EMPTY, JRightPadded.build(false),
                        statements(node.statements), EMPTY)
        );
    }

    // ------------------------------------------------------------------ declarations

    @Override
    public J visitDefNode(Nodes.DefNode lazy) {
        Nodes.DefNode node = lazy.isLazy() ? lazy.getNonLazy() : lazy;
        Space prefix = whitespace();
        skip("def");

        Expression receiver = null;
        Space receiverDot = null;
        if (node.receiver != null) {
            receiver = convertExpression(node.receiver);
            receiverDot = sourceBefore(".");
        }

        J.MethodDeclaration.IdentifierWithAnnotations name =
                new J.MethodDeclaration.IdentifierWithAnnotations(identifier(str(node.name)), emptyList());

        JContainer<Statement> parameters = methodParameters(node.parameters);

        List<JRightPadded<Statement>> bodyStatements = ListUtils.mapLast(bodyStatements(node.body), statement -> {
            J element = statement.getElement();
            if (element instanceof J.Return || !(element instanceof Expression)) {
                return statement;
            }
            return statement.withElement(new J.Return(randomId(), element.getPrefix(),
                    Markers.EMPTY.add(new ImplicitReturn(randomId())), ((Expression) element).withPrefix(EMPTY)));
        });

        J.Block body = new J.Block(randomId(), EMPTY, Markers.EMPTY, JRightPadded.build(false),
                bodyStatements, sourceBefore("end"));

        //noinspection unchecked
        J.MethodDeclaration method = new J.MethodDeclaration(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                name,
                parameters,
                emptyList(),
                null,
                body,
                null,
                null
        );

        if (receiver != null) {
            return new Rb.ClassMethod(randomId(), method.getPrefix(), Markers.EMPTY, receiver,
                    padLeft(receiverDot, method.withPrefix(EMPTY)));
        }
        return method;
    }

    private JContainer<Statement> methodParameters(Nodes.@Nullable ParametersNode parameters) {
        List<Nodes.Node> params = new ArrayList<>();
        if (parameters != null) {
            collectParameters(parameters, params);
        }
        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        Optional<JContainer<J>> built = peekWhitespace(0, (n, prefix) -> {
            boolean parens = source.startsWith("(", cursor);
            if (parens) {
                skip("(");
            } else {
                markers.set(markers.get().add(new OmitParentheses(randomId())));
            }
            List<JRightPadded<J>> mapped = new ArrayList<>(params.size());
            for (int i = 0; i < params.size(); i++) {
                J param = parameter(params.get(i));
                if (param instanceof J.Identifier) {
                    // `...` and other bare names still need to look like declarations
                    param = variableDeclaration((J.Identifier) param);
                }
                mapped.add(padRight(param, i == params.size() - 1 ?
                        maybeTrailingComma(markers, parens ? ")" : null) : sourceBefore(",")));
            }
            if (mapped.isEmpty()) {
                if (!parens) {
                    return null;
                }
                mapped = singletonList(padRight((J) new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                        sourceBefore(")")));
            }
            return JContainer.build(prefix, mapped, markers.get());
        });
        //noinspection unchecked
        return (JContainer<Statement>) (JContainer<?>) built.orElseGet(
                () -> JContainer.<J>empty().withMarkers(markers.get()));
    }

    private void collectParameters(Nodes.ParametersNode parameters, List<Nodes.Node> into) {
        List<Nodes.Node> collected = new ArrayList<>();
        Collections.addAll(collected, parameters.requireds);
        Collections.addAll(collected, parameters.optionals);
        if (parameters.rest != null) {
            collected.add(parameters.rest);
        }
        Collections.addAll(collected, parameters.posts);
        Collections.addAll(collected, parameters.keywords);
        if (parameters.keyword_rest != null) {
            collected.add(parameters.keyword_rest);
        }
        if (parameters.block != null) {
            collected.add(parameters.block);
        }
        collected.removeIf(p -> p instanceof Nodes.ImplicitRestNode);
        collected.sort(Comparator.comparingInt(p -> p.startOffset));
        into.addAll(collected);
    }

    private J parameter(Nodes.Node node) {
        if (node instanceof Nodes.RequiredParameterNode) {
            return variableDeclaration(identifier(node));
        }
        if (node instanceof Nodes.OptionalParameterNode) {
            Nodes.OptionalParameterNode optional = (Nodes.OptionalParameterNode) node;
            Space prefix = prefix(node);
            J.Identifier name = identifier(str(optional.name));
            return namedVariable(prefix, Markers.EMPTY, null, name,
                    padLeft(sourceBefore("="), convertExpression(optional.value)));
        }
        if (node instanceof Nodes.RestParameterNode) {
            Nodes.RestParameterNode rest = (Nodes.RestParameterNode) node;
            Space prefix = prefix(node);
            Space varargs = sourceBefore("*");
            J.Identifier name = identifier(rest.name == null ? "" : str(rest.name));
            return namedVariable(prefix, Markers.EMPTY, varargs, name, null);
        }
        if (node instanceof Nodes.KeywordRestParameterNode) {
            Nodes.KeywordRestParameterNode rest = (Nodes.KeywordRestParameterNode) node;
            Space prefix = prefix(node);
            Space varargs = sourceBefore("**");
            J.Identifier name = identifier(rest.name == null ? "" : str(rest.name));
            return namedVariable(prefix, Markers.EMPTY.add(new KeywordRestArgument(randomId())),
                    varargs, name, null);
        }
        if (node instanceof Nodes.RequiredKeywordParameterNode) {
            Nodes.RequiredKeywordParameterNode kw = (Nodes.RequiredKeywordParameterNode) node;
            Space prefix = prefix(node);
            J.Identifier name = identifier(str(kw.name));
            return namedVariable(prefix, Markers.EMPTY.add(new KeywordArgument(randomId())), null, name,
                    padLeft(sourceBefore(":"), new J.Empty(randomId(), EMPTY, Markers.EMPTY)));
        }
        if (node instanceof Nodes.OptionalKeywordParameterNode) {
            Nodes.OptionalKeywordParameterNode kw = (Nodes.OptionalKeywordParameterNode) node;
            Space prefix = prefix(node);
            J.Identifier name = identifier(str(kw.name));
            return namedVariable(prefix, Markers.EMPTY.add(new KeywordArgument(randomId())), null, name,
                    padLeft(sourceBefore(":"), convertExpression(kw.value)));
        }
        if (node instanceof Nodes.BlockParameterNode) {
            Nodes.BlockParameterNode block = (Nodes.BlockParameterNode) node;
            Space prefix = prefix(node);
            skip("&");
            return new Rb.BlockArgument(randomId(), prefix, Markers.EMPTY,
                    identifier(block.name == null ? "" : str(block.name)));
        }
        if (node instanceof Nodes.BlockLocalVariableNode) {
            return identifier(node);
        }
        if (node instanceof Nodes.ForwardingParameterNode || node instanceof Nodes.NoKeywordsParameterNode) {
            return identifier(node);
        }
        return convert(node);
    }

    private J.VariableDeclarations namedVariable(Space prefix, Markers markers, @Nullable Space varargs,
                                                 J.Identifier name,
                                                 @Nullable JLeftPadded<Expression> initializer) {
        return new J.VariableDeclarations(
                randomId(),
                prefix,
                markers,
                emptyList(),
                emptyList(),
                null,
                varargs,
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(), EMPTY, Markers.EMPTY, name, emptyList(), initializer, null), EMPTY))
        );
    }

    @Override
    public J visitClassNode(Nodes.ClassNode node) {
        Space prefix = prefix(node);
        skip("class");
        J.Identifier name = identifier(node.constant_path);

        JLeftPadded<TypeTree> extendings = null;
        if (node.superclass != null) {
            extendings = padLeft(sourceBefore("<"), convertTypeTree(node.superclass));
        }

        return new J.ClassDeclaration(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                new J.ClassDeclaration.Kind(randomId(), EMPTY, Markers.EMPTY, emptyList(),
                        J.ClassDeclaration.Kind.Type.Class),
                name,
                null,
                null,
                extendings,
                null,
                null,
                keywordBlock(node.body, "end"),
                null
        );
    }

    @Override
    public J visitModuleNode(Nodes.ModuleNode node) {
        Space prefix = prefix(node);
        skip("module");
        return new Rb.Module(randomId(), prefix, Markers.EMPTY, identifier(node.constant_path),
                keywordBlock(node.body, "end"));
    }

    @Override
    public J visitSingletonClassNode(Nodes.SingletonClassNode node) {
        Space prefix = prefix(node);
        skip("class");
        return new Rb.OpenEigenclass(randomId(), prefix, Markers.EMPTY,
                padLeft(sourceBefore("<<"), convertExpression(node.expression)),
                keywordBlock(node.body, "end"));
    }

    @Override
    public J visitAliasMethodNode(Nodes.AliasMethodNode node) {
        Space prefix = prefix(node);
        skip("alias");
        return new Rb.Alias(randomId(), prefix, Markers.EMPTY,
                aliasName(node.new_name), aliasName(node.old_name));
    }

    @Override
    public J visitAliasGlobalVariableNode(Nodes.AliasGlobalVariableNode node) {
        Space prefix = prefix(node);
        skip("alias");
        return new Rb.Alias(randomId(), prefix, Markers.EMPTY,
                aliasName(node.new_name), aliasName(node.old_name));
    }

    private Expression aliasName(Nodes.Node node) {
        return node instanceof Nodes.SymbolNode ? identifier(node) : convertExpression(node);
    }

    // ------------------------------------------------------------------ case / pattern matching

    @Override
    public J visitCaseNode(Nodes.CaseNode node) {
        Space prefix = prefix(node);
        skip("case");
        J.ControlParentheses<Expression> selector = new J.ControlParentheses<>(randomId(), EMPTY,
                Markers.EMPTY, padRight(convertExpression(node.predicate), EMPTY));

        List<JRightPadded<Statement>> cases = new ArrayList<>(node.conditions.length);
        emptySeparators(cases);
        for (Nodes.WhenNode when : node.conditions) {
            cases.add(padRight(caseClause(when, when.conditions, when.statements, false), EMPTY));
        }
        return switchStatement(prefix, selector, cases, node.else_clause);
    }

    @Override
    public J visitCaseMatchNode(Nodes.CaseMatchNode node) {
        Space prefix = prefix(node);
        skip("case");
        J.ControlParentheses<Expression> selector = new J.ControlParentheses<>(randomId(), EMPTY,
                Markers.EMPTY, padRight(convertExpression(node.predicate), EMPTY));

        List<JRightPadded<Statement>> cases = new ArrayList<>(node.conditions.length);
        emptySeparators(cases);
        for (Nodes.InNode in : node.conditions) {
            cases.add(padRight(caseClause(in, new Nodes.Node[]{in.pattern}, in.statements, true), EMPTY));
        }
        return switchStatement(prefix, selector, cases, node.else_clause);
    }

    private J.Switch switchStatement(Space prefix, J.ControlParentheses<Expression> selector,
                                     List<JRightPadded<Statement>> cases, Nodes.@Nullable ElseNode elseNode) {
        J.Block caseBlock;
        if (elseNode == null) {
            caseBlock = new J.Block(randomId(), EMPTY, Markers.EMPTY, JRightPadded.build(false),
                    cases, sourceBefore("end"));
        } else {
            Space elsePrefix = sourceBefore("else");
            JContainer<Statement> body = JContainer.build(whitespace(),
                    statements(elseNode.statements), Markers.EMPTY);
            cases = ListUtils.concat(cases, padRight((Statement) new J.Case(randomId(), elsePrefix,
                    Markers.EMPTY, J.Case.Type.Statement, null, null, JContainer.empty(), null, body,
                    null), EMPTY));
            caseBlock = new J.Block(randomId(), EMPTY, Markers.EMPTY, JRightPadded.build(false),
                    cases, sourceBefore("end"));
        }
        return new J.Switch(randomId(), prefix, Markers.EMPTY, selector, caseBlock);
    }

    private J.Case caseClause(Nodes.Node node, Nodes.Node[] labels, Nodes.@Nullable StatementsNode statements,
                              boolean pattern) {
        Space prefix = prefix(node);
        skip(pattern ? "in" : "when");
        JContainer<J> caseLabels = caseLabels(labels);
        Markers markers = pattern ? Markers.EMPTY.add(new PatternCase(randomId())) : Markers.EMPTY;
        JContainer<Statement> body = JContainer.build(whitespace(), statements(statements), Markers.EMPTY);
        return new J.Case(randomId(), prefix, markers, J.Case.Type.Statement, null, null, caseLabels,
                null, body, null);
    }

    private JContainer<J> caseLabels(Nodes.Node[] labels) {
        Space before = whitespace();
        List<JRightPadded<J>> mapped = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i++) {
            mapped.add(padRight(convert(labels[i]), i == labels.length - 1 ? EMPTY : sourceBefore(",")));
        }
        JContainer<J> container = JContainer.build(before, mapped, Markers.EMPTY);
        return peekWhitespace(container, (c, beforeBody) -> {
            if (peekKeyword("then")) {
                skip("then");
                return c.withMarkers(c.getMarkers().add(new ExplicitThen(randomId())))
                        .getPadding().withElements(ListUtils.mapLast(c.getPadding().getElements(),
                                last -> last.withAfter(beforeBody)));
            }
            return null;
        }).orElse(container);
    }

    @Override
    public J visitMatchPredicateNode(Nodes.MatchPredicateNode node) {
        Space prefix = prefix(node);
        Expression left = convertExpression(node.value);
        return new Rb.BooleanCheck(randomId(), prefix, Markers.EMPTY, left,
                inlinePattern(node.pattern, "in", true), null);
    }

    @Override
    public J visitMatchRequiredNode(Nodes.MatchRequiredNode node) {
        Space prefix = prefix(node);
        Expression left = convertExpression(node.value);
        return new Rb.RightwardAssignment(randomId(), prefix, Markers.EMPTY, left,
                inlinePattern(node.pattern, "=>", false), null);
    }

    private J.Case inlinePattern(Nodes.Node pattern, String keyword, boolean patternCase) {
        Space prefix = whitespace();
        skip(keyword);
        JContainer<J> labels = JContainer.build(whitespace(),
                singletonList(padRight(convert(pattern), EMPTY)), Markers.EMPTY);
        Markers markers = patternCase ? Markers.EMPTY.add(new PatternCase(randomId())) : Markers.EMPTY;
        return new J.Case(randomId(), prefix, markers, J.Case.Type.Statement, null, null, labels, null,
                JContainer.empty(), null);
    }

    @Override
    public J visitArrayPatternNode(Nodes.ArrayPatternNode node) {
        List<Nodes.Node> elements = new ArrayList<>();
        Collections.addAll(elements, node.requireds);
        if (node.rest != null) {
            elements.add(node.rest);
        }
        Collections.addAll(elements, node.posts);
        return patternWithConstant(node, node.constant, elements.toArray(new Nodes.Node[0]), "[");
    }

    @Override
    public J visitFindPatternNode(Nodes.FindPatternNode node) {
        List<Nodes.Node> elements = new ArrayList<>();
        if (node.left != null) {
            elements.add(node.left);
        }
        Collections.addAll(elements, node.requireds);
        if (node.right != null) {
            elements.add(node.right);
        }
        return patternWithConstant(node, node.constant, elements.toArray(new Nodes.Node[0]), "[");
    }

    private J patternWithConstant(Nodes.Node node, Nodes.@Nullable Node constant, Nodes.Node[] elements,
                                  String defaultDelimiter) {
        Space prefix = prefix(node);
        if (constant == null) {
            return array(prefix, elements);
        }
        Expression constantExpr = convertExpression(constant);
        Space beforeDelimiter = whitespace();
        String delimiter = source.substring(cursor, cursor + 1);
        skip(delimiter);
        Rb.Array inner = new Rb.Array(randomId(), EMPTY, Markers.EMPTY, patternElements(elements), null);
        return new Rb.StructPattern(
                randomId(),
                prefix,
                Markers.EMPTY,
                constantExpr,
                delimiter,
                JContainer.build(beforeDelimiter,
                        singletonList(padRight((Expression) inner,
                                sourceBefore(StringUtils.endDelimiter(delimiter)))),
                        Markers.EMPTY)
        );
    }

    private JContainer<Expression> patternElements(Nodes.Node[] elements) {
        List<JRightPadded<Expression>> mapped = new ArrayList<>(elements.length);
        for (int i = 0; i < elements.length; i++) {
            mapped.add(padRight(convertExpression(elements[i]),
                    i == elements.length - 1 ? EMPTY : sourceBefore(",")));
        }
        if (mapped.isEmpty()) {
            mapped = singletonList(padRight((Expression) new J.Empty(randomId(), EMPTY, Markers.EMPTY), EMPTY));
        }
        return JContainer.build(EMPTY, mapped, Markers.EMPTY.add(new OmitParentheses(randomId())));
    }

    @Override
    public J visitHashPatternNode(Nodes.HashPatternNode node) {
        Space prefix = prefix(node);
        if (node.constant == null) {
            return hash(prefix, node.elements, node.rest);
        }
        Expression constantExpr = convertExpression(node.constant);
        Space beforeDelimiter = whitespace();
        String delimiter = source.substring(cursor, cursor + 1);
        skip(delimiter);
        Rb.Hash inner = hash(EMPTY, node.elements, node.rest);
        return new Rb.StructPattern(
                randomId(),
                prefix,
                Markers.EMPTY,
                constantExpr,
                delimiter,
                JContainer.build(beforeDelimiter,
                        singletonList(padRight((Expression) inner,
                                sourceBefore(StringUtils.endDelimiter(delimiter)))),
                        Markers.EMPTY)
        );
    }

    @Override
    public J visitNoKeywordsParameterNode(Nodes.NoKeywordsParameterNode node) {
        return identifier(node);
    }
}
