package org.openrewrite.ruby;

import org.jruby.ast.*;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.ast.visitor.OperatorCallNode;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.marker.EnglishBinaryOperator;
import org.openrewrite.ruby.tree.Ruby;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespace;
import static org.openrewrite.java.tree.Space.EMPTY;
import static org.openrewrite.java.tree.Space.format;

public class RubyParserVisitor extends AbstractNodeVisitor<J> {
    private final Path sourcePath;
    @Nullable
    private final FileAttributes fileAttributes;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    private int cursor = 0;

    public RubyParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source.readFully();
        this.charset = source.getCharset();
        this.charsetBomMarked = source.isCharsetBomMarked();
    }

    @Override
    protected J defaultVisit(Node node) {
        throw new UnsupportedOperationException(String.format("Node type %s not yet implemented",
                node.getClass().getSimpleName()));
    }

    @Override
    public J visitAndNode(AndNode node) {
        Space prefix = whitespace();
        Expression left = (Expression) node.getFirstNode().accept(this);
        Space opPrefix = whitespace();
        String op = source.substring(cursor).startsWith("&&") ? "&&" : "and";
        skip(op);
        return new J.Binary(
                randomId(),
                prefix,
                op.equals("&&") ? Markers.EMPTY : Markers.EMPTY.add(new EnglishBinaryOperator(randomId())),
                left,
                padLeft(opPrefix, J.Binary.Type.And),
                (Expression) node.getSecondNode().accept(this),
                null
        );
    }

    @Override
    public J visitDotNode(DotNode node) {
        Space prefix = whitespace();
        Expression left = (Expression) node.getBeginNode().accept(this);
        Space opPrefix = whitespace();
        String op = source.substring(cursor).startsWith("...") ? "..." : "..";
        skip(op);
        return new Ruby.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, op.equals("...") ? Ruby.Binary.Type.RangeExclusive : Ruby.Binary.Type.RangeInclusive),
                (Expression) node.getEndNode().accept(this),
                null
        );
    }

    @Override
    public J visitFCallNode(FCallNode node) {
        String name = node.getName().asJavaString();
        Space prefix = sourceBefore(name);
        Space beforeArgs = whitespace();
        Markers firstArgMarkers;
        if (source.charAt(cursor) != '(') {
            firstArgMarkers = Markers.EMPTY.add(new OmitParentheses(randomId()));
        } else {
            firstArgMarkers = Markers.EMPTY;
            skip("(");
        }
        return new J.MethodInvocation(
                randomId(),
                prefix,
                Markers.EMPTY,
                null,
                null,
                new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), name, null, null),
                JContainer.<Expression>build(ListUtils.mapFirst(
                                convertAll(
                                        node.getArgsNode().childNodes(),
                                        n -> sourceBefore(","),
                                        n -> firstArgMarkers == Markers.EMPTY ?
                                                sourceBefore(")") : EMPTY
                                ),
                                arg -> arg.withElement(arg.getElement().withMarkers(firstArgMarkers))
                        ))
                        .withBefore(beforeArgs),
                null
        );
    }

    @Override
    public J visitFixnumNode(FixnumNode node) {
        return new J.Literal(
                randomId(),
                sourceBefore(Long.toString(node.getValue())),
                Markers.EMPTY,
                node.getValue(),
                Long.toString(node.getValue()),
                null,
                JavaType.Primitive.Long
        );
    }

    @Override
    public J visitOperatorCallNode(OperatorCallNode node) {
        String op = node.getName().asJavaString();
        Markers markers = Markers.EMPTY;
        J.Binary.Type type = null;
        Ruby.Binary.Type rubyType = null;
        switch (op) {
            case "+":
                type = J.Binary.Type.Addition;
                break;
            case "-":
                type = J.Binary.Type.Subtraction;
                break;
            case "*":
                type = J.Binary.Type.Multiplication;
                break;
            case "/":
                type = J.Binary.Type.Division;
                break;
            case "%":
                type = J.Binary.Type.Modulo;
                break;
            case "**":
                rubyType = Ruby.Binary.Type.Exponent;
                break;
            case ">>":
                type = J.Binary.Type.RightShift;
                break;
            case "<<":
                type = J.Binary.Type.LeftShift;
                break;
            case "&":
                type = J.Binary.Type.BitAnd;
                break;
            case "|":
                type = J.Binary.Type.BitOr;
                break;
            case "^":
                type = J.Binary.Type.BitXor;
                break;
            case "~":
                rubyType = Ruby.Binary.Type.OnesComplement;
                break;
            case "==":
                type = J.Binary.Type.Equal;
                break;
            case "===":
                rubyType = Ruby.Binary.Type.Within;
                break;
            case "!=":
                type = J.Binary.Type.NotEqual;
                break;
            case "<=>":
                rubyType = Ruby.Binary.Type.Comparison;
                break;
            case "<":
                type = J.Binary.Type.LessThan;
                break;
            case "<=":
                type = J.Binary.Type.LessThanOrEqual;
                break;
            case ">":
                type = J.Binary.Type.GreaterThan;
                break;
            case ">=":
                type = J.Binary.Type.GreaterThanOrEqual;
                break;
        }

        if (type != null) {
            return new J.Binary(
                    randomId(),
                    whitespace(),
                    markers,
                    (Expression) node.getReceiverNode().accept(this),
                    padLeft(sourceBefore(op), type),
                    (Expression) node.getArgsNode().childNodes().get(0).accept(this),
                    null
            );
        } else {
            assert rubyType != null;
            return new Ruby.Binary(
                    randomId(),
                    whitespace(),
                    markers,
                    (Expression) node.getReceiverNode().accept(this),
                    padLeft(sourceBefore(op), rubyType),
                    (Expression) node.getArgsNode().childNodes().get(0).accept(this),
                    null
            );
        }
    }

    @Override
    public J visitOrNode(OrNode node) {
        Space prefix = whitespace();
        Expression left = (Expression) node.getFirstNode().accept(this);
        Space opPrefix = whitespace();
        String op = source.substring(cursor).startsWith("||") ? "||" : "or";
        skip(op);
        return new J.Binary(
                randomId(),
                prefix,
                op.equals("||") ? Markers.EMPTY : Markers.EMPTY.add(new EnglishBinaryOperator(randomId())),
                left,
                padLeft(opPrefix, J.Binary.Type.Or),
                (Expression) node.getSecondNode().accept(this),
                null
        );
    }

    @Override
    public Ruby.CompilationUnit visitRootNode(RootNode node) {
        return new Ruby.CompilationUnit(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                charset,
                charsetBomMarked,
                null,
                node.getBodyNode().accept(this),
                Space.format(source.substring(cursor))
        );
    }

    @Override
    public J visitStrNode(StrNode node) {
        String value = new String(node.getValue().bytes(), StandardCharsets.UTF_8);
        J.Literal literal = new J.Literal(
                randomId(),
                sourceBefore("\""),
                Markers.EMPTY,
                sourceBefore(value),
                String.format("\"%s\"", value),
                null,
                JavaType.Primitive.String
        );
        skip("\"");
        return literal;
    }

    private <J2 extends J> List<J2> convertAll(List<? extends Node> trees) {
        List<J2> converted = new ArrayList<>(trees.size());
        for (Node tree : trees) {
            //noinspection unchecked
            converted.add((J2) tree.accept(this));
        }
        return converted;
    }

    @Nullable
    private <J2 extends J> JRightPadded<J2> convert(Node t, Function<Node, Space> suffix) {
        //noinspection unchecked
        J2 j = (J2) t.accept(this);
        return j == null ? null : new JRightPadded<>(j, suffix.apply(t), Markers.EMPTY);
    }

    private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends Node> trees,
                                                             Function<Node, Space> innerSuffix,
                                                             Function<Node, Space> suffix) {
        if (trees.isEmpty()) {
            return emptyList();
        }
        List<JRightPadded<J2>> converted = new ArrayList<>(trees.size());
        for (int i = 0; i < trees.size(); i++) {
            converted.add(convert(trees.get(i), i == trees.size() - 1 ? suffix : innerSuffix));
        }
        return converted;
    }

    private Space sourceBefore(String untilDelim) {
        int delimIndex = positionOfNext(untilDelim);
        if (delimIndex < 0) {
            return EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
    }

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return new JRightPadded<>(tree, right, Markers.EMPTY);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
    }

    private int positionOfNext(String untilDelim) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment) {
                if (source.charAt(delimIndex) == '\n') {
                    inSingleLineComment = false;
                }
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
                            inSingleLineComment = true;
                            delimIndex++;
                            break;
                        case "/*":
                            inMultiLineComment = true;
                            delimIndex++;
                            break;
                        case "*/":
                            inMultiLineComment = false;
                            delimIndex = delimIndex + 2;
                            break;
                    }
                }

                if (!inMultiLineComment && !inSingleLineComment) {
                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }

    private Space whitespace() {
        String prefix = source.substring(cursor, indexOfNextNonWhitespace(cursor, source));
        cursor += prefix.length();
        return format(prefix);
    }

    private String skip(@Nullable String token) {
        if (token == null) {
            //noinspection ConstantConditions
            return null;
        }
        if (source.startsWith(token, cursor)) {
            cursor += token.length();
        }
        return token;
    }
}
