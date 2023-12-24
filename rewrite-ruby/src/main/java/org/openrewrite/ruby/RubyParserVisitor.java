/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby;

import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.ast.visitor.OperatorCallNode;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.openrewrite.Cursor;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.marker.*;
import org.openrewrite.ruby.tree.Ruby;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
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
    private Cursor nodes = new Cursor(null, Cursor.ROOT_VALUE);

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
        Expression left = convert(node.getFirstNode());
        Space opPrefix = whitespace();
        String op = source.startsWith("&&", cursor) ? "&&" : "and";
        skip(op);
        return new J.Binary(
                randomId(),
                prefix,
                op.equals("&&") ? Markers.EMPTY : Markers.EMPTY.add(new EnglishOperator(randomId())),
                left,
                padLeft(opPrefix, J.Binary.Type.And),
                convert(node.getSecondNode()),
                null
        );
    }

    @Override
    public J visitArgumentNode(ArgumentNode node) {
        return getIdentifier(
                sourceBefore(node.getName().asJavaString()),
                node.getName().asJavaString()
        );
    }

    @Override
    public J visitArrayNode(ArrayNode node) {
        Space prefix = sourceBefore("[");
        JContainer<Expression> elements = convertArgs(node);
        elements = elements.getPadding().withElements(ListUtils.mapLast(
                elements.getPadding().getElements(),
                e -> e.withAfter(sourceBefore("]"))));
        return new Ruby.Array(
                randomId(),
                prefix,
                Markers.EMPTY,
                elements,
                null
        );
    }

    @Override
    public J visitBlockNode(BlockNode node) {
        return visitBlock(node, false);
    }

    private J.Block visitBlock(Node node, boolean explicitEnd) {
        Space prefix = whitespace();
        List<JRightPadded<Statement>> statements = convertBlockStatements(
                node instanceof ListNode ?
                        Arrays.asList(((ListNode) node).children()) :
                        singletonList(node),
                n -> explicitEnd ? sourceBefore("end") : EMPTY
        );
        return new J.Block(
                randomId(),
                prefix,
                Markers.EMPTY,
                JRightPadded.build(false),
                statements,
                EMPTY
        );
    }

    @Override
    public J visitCallNode(CallNode node) {
        String name = node.getName().asJavaString();
        Space prefix = whitespace();
        TypeTree receiver = convert(node.getReceiverNode());
        Space beforeDot = sourceBefore(".");
        Space beforeName = sourceBefore(name);
        JContainer<Expression> arguments = convertArgs(node.getArgsNode());
        if (name.equals("new")) {
            return new J.NewClass(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    padRight(new J.Empty(randomId(), EMPTY, Markers.EMPTY), beforeDot),
                    beforeName,
                    receiver,
                    arguments,
                    null,
                    null
            );
        } else {
            return new J.MethodInvocation(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    padRight((Expression) receiver, beforeDot),
                    null,
                    getIdentifier(beforeName, name),
                    arguments.withBefore(beforeDot),
                    null
            );
        }
    }

    private List<JRightPadded<Statement>> convertBlockStatements(List<? extends Node> trees,
                                                                 Function<Node, Space> suffix) {
        if (trees.isEmpty()) {
            return emptyList();
        }
        List<JRightPadded<Statement>> converted = new ArrayList<>(trees.size());
        for (int i = 0; i < trees.size(); i++) {
            JRightPadded<J> stat = convert(trees.get(i), i == trees.size() - 1 ? suffix : n -> EMPTY);
            if (!(stat.getElement() instanceof Statement)) {
                stat = stat.withElement((J) new Ruby.ExpressionStatement(randomId(), (Expression) stat.getElement()));
            }
            //noinspection ReassignedVariable,unchecked
            converted.add((JRightPadded<Statement>) (JRightPadded<?>) stat);
        }
        return converted;
    }

    @Override
    public J visitBreakNode(BreakNode node) {
        return new J.Break(
                randomId(),
                sourceBefore("break"),
                Markers.EMPTY,
                null
        );
    }

    @Override
    public J visitClassNode(ClassNode node) {
        return new J.ClassDeclaration(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                new J.ClassDeclaration.Kind(
                        randomId(),
                        sourceBefore("class"),
                        Markers.EMPTY,
                        emptyList(),
                        J.ClassDeclaration.Kind.Type.Class
                ),
                getIdentifier(
                        sourceBefore(node.getCPath().getName().asJavaString()),
                        node.getCPath().getName().asJavaString()
                ),
                null,
                null,
                node.getSuperNode() == null ?
                        null :
                        padLeft(sourceBefore("<"), convert(node.getSuperNode())),
                null,
                null,
                visitBlock(node.getBodyNode(), true),
                null
        );
    }

    @Override
    public J visitConstNode(ConstNode node) {
        return getIdentifier(
                sourceBefore(node.getName().asJavaString()),
                node.getName().asJavaString()
        );
    }

    @Override
    public J visitDefnNode(DefnNode node) {
        Space prefix = sourceBefore("def");
        J.MethodDeclaration.IdentifierWithAnnotations name = new J.MethodDeclaration.IdentifierWithAnnotations(
                getIdentifier(sourceBefore(node.getName().asJavaString()), node.getName().asJavaString()),
                emptyList()
        );

        JContainer<Statement> args = convertArgs(node.getArgsNode());

        J body = convert(node.getBodyNode());
        if (!(body instanceof J.Block)) {
            Statement bodyStatement = body instanceof Statement ?
                    (Statement) body :
                    new Ruby.ExpressionStatement(randomId(), (Expression) body);
            body = new J.Block(randomId(), EMPTY, Markers.EMPTY,
                    JRightPadded.build(false), singletonList(padRight(bodyStatement, EMPTY)),
                    sourceBefore("end"));
        }

        return new J.MethodDeclaration(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                name,
                args,
                null,
                (J.Block) body,
                null,
                null
        );
    }

    @Override
    public J visitDotNode(DotNode node) {
        Space prefix = whitespace();
        Expression left = convert(node.getBeginNode());
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
    public J visitDRegxNode(DRegexpNode node) {
        Ruby.DelimitedString dString = visitDNode(whitespace(), "/", node);
        int optionCount = 0;
        RegexpOptions options = node.getOptions();
        if (options.isExtended()) {
            optionCount++;
        }
        if (!options.isKcodeDefault()) {
            optionCount++;
        }
        if (options.isIgnorecase()) {
            optionCount++;
        }
        if (options.isJava()) {
            optionCount++;
        }
        if (options.isLiteral()) {
            optionCount++;
        }
        if (options.isMultiline()) {
            optionCount++;
        }
        String optionsString = source.substring(cursor, cursor + optionCount);
        skip(optionsString);
        return dString.withRegexpOptions(optionsString.chars().mapToObj(opt -> {
            switch (opt) {
                case 'x':
                    return Ruby.DelimitedString.RegexpOptions.Extended;
                case 'i':
                    return Ruby.DelimitedString.RegexpOptions.IgnoreCase;
                case 'm':
                    return Ruby.DelimitedString.RegexpOptions.Multiline;
                case 'j':
                    return Ruby.DelimitedString.RegexpOptions.Java;
                case 'o':
                    return Ruby.DelimitedString.RegexpOptions.Once;
                case 'n':
                    return Ruby.DelimitedString.RegexpOptions.None;
                case 'e':
                    return Ruby.DelimitedString.RegexpOptions.EUCJPEncoding;
                case 's':
                    return Ruby.DelimitedString.RegexpOptions.SJISEncoding;
                case 'u':
                    return Ruby.DelimitedString.RegexpOptions.UTF8Encoding;
                default:
                    throw new UnsupportedOperationException(String.format("Unknown regexp option %s", opt));
            }
        }).collect(toList()));
    }

    @Override
    public J visitDStrNode(DStrNode node) {
        return visitDNode(node);
    }

    @Override
    public J visitDXStrNode(DXStrNode node) {
        return visitDNode(node);
    }

    @Override
    public J visitInstAsgnNode(InstAsgnNode node) {
        return visitAsgnNode(node, node.getName());
    }

    @Override
    public J visitInstVarNode(InstVarNode node) {
        return getIdentifier(sourceBefore(node.getName().asJavaString()), node.getName().asJavaString());
    }

    @Override
    public J visitNilNode(NilNode node) {
        return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
    }

    @Override
    public J visitRegexpNode(RegexpNode node) {
        DStrNode dstr = new DStrNode(0, node.getValue().getEncoding());
        dstr.add(new StrNode(node.getLine(), node.getValue()));
        return convert(dstr);
    }

    private Ruby.DelimitedString visitDNode(DNode node) {
        Space prefix = whitespace();
        String delimiter = "\"";
        if (source.charAt(cursor) == '%') {
            switch (source.charAt(cursor + 1)) {
                case 'Q':
                case 'q':
                case 'x':
                case 'r':
                    // ex: %Q<is a string>
                    delimiter = source.substring(cursor, 3);
                    break;
                default:
                    // ex: %<is a string>
                    delimiter = source.substring(cursor, 2);
                    break;
            }
        }
        return visitDNode(prefix, delimiter, node);
    }

    private Ruby.DelimitedString visitDNode(Space prefix, String delimiter, DNode node) {
        skip(delimiter);
        Ruby.DelimitedString dString = new Ruby.DelimitedString(
                randomId(),
                prefix,
                Markers.EMPTY,
                delimiter,
                StreamSupport.stream(node.spliterator(), false)
                        .filter(Objects::nonNull)
                        .filter(n -> !(n instanceof StrNode) || !((StrNode) n).getValue().isEmpty())
                        .map(n -> (J) convert(n))
                        .collect(toList()),
                emptyList(),
                null
        );
        skip(delimiter.substring(delimiter.length() - 1));
        return dString;
    }

    @Override
    public J visitEvStrNode(EvStrNode node) {
        skip("#{");
        return new Ruby.DelimitedString.Value(
                randomId(),
                Markers.EMPTY,
                convert(node.getBody()),
                sourceBefore("}")
        );
    }

    @Override
    public J visitFalseNode(FalseNode node) {
        return new J.Literal(randomId(), sourceBefore("false"), Markers.EMPTY, true, "false",
                null, JavaType.Primitive.Boolean);
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
                getIdentifier(EMPTY, name),
                JContainer.<Expression>build(convertAll(
                                node.getArgsNode().childNodes(),
                                n -> sourceBefore(","),
                                n -> firstArgMarkers == Markers.EMPTY ?
                                        sourceBefore(")") : EMPTY
                        ))
                        .withBefore(beforeArgs)
                        .withMarkers(firstArgMarkers),
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
    public J visitForNode(ForNode node) {
        Markers markers = Markers.EMPTY;
        Space prefix = sourceBefore("for");

        J.Identifier variableIdentifier = convert(node.getVarNode());
        JRightPadded<J.VariableDeclarations> variable = padRight(new J.VariableDeclarations(
                randomId(),
                variableIdentifier.getPrefix(),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                emptyList(),
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        variableIdentifier.withPrefix(EMPTY),
                        emptyList(),
                        null,
                        null
                ), EMPTY))
        ), sourceBefore("in"));

        JRightPadded<Expression> iterable = padRight(convert(node.getIterNode()), whitespace());
        if (source.startsWith("do", cursor)) {
            skip("do");
            markers = markers.add(new ExplicitDo(randomId()));
        }
        return new J.ForEachLoop(
                randomId(),
                prefix,
                markers,
                new J.ForEachLoop.Control(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        variable,
                        iterable
                ),
                padRight(convert(node.getBodyNode()), sourceBefore("end"))
        );
    }

    @Override
    public J visitHashNode(HashNode node) {
        Space prefix = sourceBefore("{");

        List<JRightPadded<Ruby.KeyValue>> pairs = new ArrayList<>(node.getPairs().size());
        List<KeyValuePair<Node, Node>> nodePairs = node.getPairs();
        for (int i = 0; i < nodePairs.size(); i++) {
            KeyValuePair<Node, Node> kv = nodePairs.get(i);
            pairs.add(padRight(new Ruby.KeyValue(
                    randomId(),
                    whitespace(),
                    Markers.EMPTY,
                    padRight(convert(kv.getKey()), sourceBefore("=>")),
                    convert(kv.getValue()),
                    null
            ), i == nodePairs.size() - 1 ? sourceBefore("}") : sourceBefore(",")));
        }

        return new Ruby.Hash(
                randomId(),
                prefix,
                Markers.EMPTY,
                JContainer.build(EMPTY, pairs, Markers.EMPTY),
                null
        );
    }

    @Override
    public J visitIfNode(IfNode node) {
        Space prefix = whitespace();
        return (source.startsWith("if", cursor) ?
                ifStatement(node) :
                ifModifier(node)).withPrefix(prefix);
    }

    private J.If ifModifier(IfNode node) {
        Statement thenElem = convert(node.getThenBody());
        JRightPadded<Statement> then = padRight(thenElem, sourceBefore("if"));
        Expression ifConditionExpr = convert(node.getCondition());
        J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<>(
                randomId(),
                ifConditionExpr.getPrefix(),
                Markers.EMPTY,
                padRight(ifConditionExpr, EMPTY)
        );
        return new J.If(
                randomId(),
                EMPTY,
                Markers.build(singletonList(new IfModifier(randomId()))),
                ifCondition,
                then,
                null
        );
    }

    private J.If ifStatement(IfNode node) {
        Space ifConditionPrefix = whitespace();
        Expression ifConditionExpr = convert(node.getCondition());
        boolean explicitThen = Pattern.compile("\\s+then").matcher(source).find(cursor);
        J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<>(
                randomId(),
                ifConditionPrefix,
                explicitThen ?
                        Markers.EMPTY.add(new ExplicitThen(randomId())) :
                        Markers.EMPTY,
                padRight(ifConditionExpr, explicitThen ? sourceBefore("then") : EMPTY)
        );

        Statement thenElem = convert(node.getThenBody());
        JRightPadded<Statement> then = node.getElseBody() == null ?
                padRight(thenElem, sourceBefore("end")) :
                padRight(thenElem, EMPTY);

        J.If.Else anElse = null;
        if (node.getElseBody() != null) {
            Space elsePrefix = whitespace();
            skip(source.startsWith("else", cursor) ? "else" : "els");
            anElse = new J.If.Else(
                    randomId(),
                    elsePrefix,
                    Markers.EMPTY,
                    padRight(convert(node.getElseBody()),
                            node.getElseBody() instanceof IfNode ? EMPTY : sourceBefore("end"))
            );
        }

        return new J.If(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                ifCondition,
                then,
                anElse
        );
    }

    @Override
    public J visitLocalAsgnNode(LocalAsgnNode node) {
        return visitAsgnNode(node, node.getName());
    }

    private Expression visitAsgnNode(AssignableNode node, RubySymbol name) {
        if (node.getValueNode() instanceof OperatorCallNode) {
            // J.AssignmentOp
            OperatorCallNode assignOp = (OperatorCallNode) node.getValueNode();
            Expression variable = convert(assignOp.getReceiverNode());
            String op = assignOp.getName().asJavaString() + "=";
            J.AssignmentOperation.Type type;
            switch (op) {
                case "+=":
                    type = J.AssignmentOperation.Type.Addition;
                    break;
                case "-=":
                    type = J.AssignmentOperation.Type.Subtraction;
                    break;
                case "*=":
                    type = J.AssignmentOperation.Type.Multiplication;
                    break;
                case "/=":
                    type = J.AssignmentOperation.Type.Division;
                    break;
                case "%=":
                    type = J.AssignmentOperation.Type.Modulo;
                    break;
                case "**=":
                    type = J.AssignmentOperation.Type.Exponentiation;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown assignment operator " + op);
            }
            return new J.AssignmentOperation(
                    randomId(),
                    variable.getPrefix(),
                    Markers.EMPTY,
                    variable.withPrefix(EMPTY),
                    padLeft(sourceBefore(op), type),
                    convert(((ListNode) assignOp.getArgsNode()).get(0)),
                    null
            );
        } else {
            Space prefix = sourceBefore(name.asJavaString());
            J.Identifier variable = getIdentifier(EMPTY, name.asJavaString());

            if (node.getValueNode() instanceof NilImplicitNode) {
                return variable.withPrefix(prefix);
            }

            return new J.Assignment(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    variable,
                    padLeft(sourceBefore("="), convert(node.getValueNode())),
                    null
            );
        }
    }

    @Override
    public J visitLocalVarNode(LocalVarNode node) {
        return getIdentifier(sourceBefore(node.getName().asJavaString()), node.getName().asJavaString());
    }

    @Override
    public J visitMultipleAsgnNode(MultipleAsgnNode node) {
        Space prefix = whitespace();
        JContainer<Expression> assignments = convertArgs(node.getPre());
        if (node.getRest() != null) {
            assignments = assignments.getPadding().withElements(ListUtils.concat(
                    ListUtils.mapLast(assignments.getPadding().getElements(), assign -> assign.withAfter(sourceBefore(","))),
                    padRight(
                            new Ruby.Expansion(
                                    randomId(),
                                    sourceBefore("*"),
                                    Markers.EMPTY,
                                    convert(node.getRest())
                            ),
                            EMPTY
                    )
            ));
        }
        Space initializerPrefix = sourceBefore("=");
        Space firstArgPrefix = whitespace();
        JContainer<Expression> initializers =
                source.startsWith("[", cursor) ?
                        JContainer.build(initializerPrefix, singletonList(padRight(visitArrayNode(
                                (ArrayNode) node.getValueNode()).withPrefix(firstArgPrefix), EMPTY)), Markers.EMPTY) :
                        JContainer.<Expression>build(
                                prefix,
                                ListUtils.mapFirst(
                                        convertAll(StreamSupport.stream(((ArrayNode) node.getValueNode()).spliterator(), false)
                                                        .filter(Objects::nonNull)
                                                        .collect(toList()), n -> sourceBefore(","),
                                                n -> EMPTY),
                                        arg -> arg.withElement(arg.getElement().withPrefix(firstArgPrefix))
                                ),
                                Markers.EMPTY
                        ).withBefore(initializerPrefix);
        return new Ruby.MultipleAssignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                assignments,
                initializers,
                null
        );
    }

    @Override
    public J visitNextNode(NextNode node) {
        return new J.Continue(
                randomId(),
                sourceBefore("next"),
                Markers.EMPTY,
                null
        );
    }

    @Override
    public J visitOperatorCallNode(OperatorCallNode node) {
        String op = node.getName().asJavaString();
        Markers markers = Markers.EMPTY;
        J.Binary.Type type = null;
        J.Unary.Type unaryType = null;
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
                rubyType = Ruby.Binary.Type.Exponentiation;
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
            case "!":
                unaryType = J.Unary.Type.Not;
                if (source.startsWith("not", cursor)) {
                    op = "not";
                    markers = Markers.EMPTY.add(new EnglishOperator(randomId()));
                }
                break;
            default:
                throw new UnsupportedOperationException("Operator " + op + " not yet implemented");
        }

        if (type != null) {
            return new J.Binary(
                    randomId(),
                    whitespace(),
                    markers,
                    convert(node.getReceiverNode()),
                    padLeft(sourceBefore(op), type),
                    convert(node.getArgsNode().childNodes().get(0)),
                    null
            );
        } else if (unaryType != null) {
            return new J.Unary(
                    randomId(),
                    whitespace(),
                    markers,
                    padLeft(sourceBefore(op), unaryType),
                    convert(node.getReceiverNode()),
                    null
            );
        } else {
            return new Ruby.Binary(
                    randomId(),
                    whitespace(),
                    markers,
                    convert(node.getReceiverNode()),
                    padLeft(sourceBefore(op), rubyType),
                    convert(node.getArgsNode().childNodes().get(0)),
                    null
            );
        }
    }

    @Override
    public J visitOptArgNode(OptArgNode node) {
        return new J.VariableDeclarations(
                randomId(),
                sourceBefore(node.getName().asJavaString()),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                emptyList(),
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        getIdentifier(EMPTY, node.getName().asJavaString()),
                        emptyList(),
                        node.getValue() == null ?
                                null :
                                padLeft(sourceBefore("="), convert(((LocalAsgnNode) node.getValue()).getValueNode())),
                        null
                ), EMPTY))
        );
    }

    @Override
    public J visitRedoNode(RedoNode node) {
        return new Ruby.Redo(
                randomId(),
                sourceBefore("redo"),
                Markers.EMPTY
        );
    }

    @Override
    public J visitSymbolNode(SymbolNode node) {
        return getIdentifier(whitespace(), skip(node.getName().asJavaString()));
    }

    @Override
    public J visitTrueNode(TrueNode node) {
        return new J.Literal(randomId(), sourceBefore("true"), Markers.EMPTY, true, "true",
                null, JavaType.Primitive.Boolean);
    }

    @Override
    public J visitVCallNode(VCallNode node) {
        return getIdentifier(sourceBefore(node.getName().asJavaString()), node.getName().asJavaString());
    }

    @Override
    public J visitWhileNode(WhileNode node) {
        return whileOrUntilNode(node.getConditionNode(), node.getBodyNode());
    }

    @Override
    public J visitUntilNode(UntilNode node) {
        return whileOrUntilNode(node.getConditionNode(), node.getBodyNode());
    }

    @Override
    public J visitYieldNode(YieldNode node) {
        return new Ruby.Yield(
                randomId(),
                sourceBefore("yield"),
                Markers.EMPTY,
                convertArgs(node.getArgsNode())
        );
    }

    private static J.Identifier getIdentifier(Space prefix, String node) {
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(),
                node, null, null);
    }

    private J whileOrUntilNode(Node conditionNode, Node bodyNode) {
        Space prefix = whitespace();

        if (source.startsWith("while", cursor) || source.startsWith("until", cursor)) {
            Markers markers = whileOrUntil();
            Space conditionPrefix = whitespace();
            Expression conditionExpr = convert(conditionNode);
            boolean explicitDo = Pattern.compile("\\s+do").matcher(source).find(cursor);
            J.ControlParentheses<Expression> condition = new J.ControlParentheses<>(
                    randomId(),
                    conditionPrefix,
                    explicitDo ?
                            Markers.EMPTY.add(new ExplicitDo(randomId())) :
                            Markers.EMPTY,
                    padRight(conditionExpr, explicitDo ? sourceBefore("do") : EMPTY)
            );

            return new J.WhileLoop(
                    randomId(),
                    prefix,
                    markers,
                    condition,
                    padRight(convert(bodyNode), sourceBefore("end"))
            );
        } else {
            JRightPadded<Statement> body = padRight(convert(bodyNode), whitespace());
            Markers markers = whileOrUntil();

            Space conditionPrefix = whitespace();
            Expression conditionExpr = convert(conditionNode);
            J.ControlParentheses<Expression> condition = new J.ControlParentheses<>(
                    randomId(),
                    conditionPrefix,
                    Markers.EMPTY,
                    padRight(conditionExpr, EMPTY)
            );

            return new J.WhileLoop(
                    randomId(),
                    prefix,
                    markers.add(new WhileModifier(randomId())),
                    condition,
                    body
            );
        }
    }

    private Markers whileOrUntil() {
        Markers markers = Markers.EMPTY;
        if (source.startsWith("until", cursor)) {
            markers = markers.add(new Until(randomId()));
            skip("until");
        } else {
            skip("while");
        }
        return markers;
    }

    @Override
    public J visitOrNode(OrNode node) {
        Space prefix = whitespace();
        Expression left = convert(node.getFirstNode());
        Space opPrefix = whitespace();
        String op = source.startsWith("||", cursor) ? "||" : "or";
        skip(op);
        return new J.Binary(
                randomId(),
                prefix,
                op.equals("||") ? Markers.EMPTY : Markers.EMPTY.add(new EnglishOperator(randomId())),
                left,
                padLeft(opPrefix, J.Binary.Type.Or),
                convert(node.getSecondNode()),
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
                convert(node.getBodyNode()),
                Space.format(source.substring(cursor))
        );
    }

    @Override
    public J visitStrNode(StrNode node) {
        String value = new String(node.getValue().bytes(), StandardCharsets.UTF_8);
        Object parentValue = nodes.getParentOrThrow().getValue();
        boolean inDString = parentValue instanceof DStrNode || parentValue instanceof DXStrNode ||
                            parentValue instanceof DRegexpNode;
        Space prefix = inDString ? EMPTY : whitespace();
        String delimiter = "";
        if (!inDString) {
            if (source.charAt(cursor) == '%') {
                DStrNode dstr = new DStrNode(0, node.getValue().getEncoding());
                dstr.add(node);
                return convert(dstr).withPrefix(prefix);
            }
            delimiter = source.substring(cursor, ++cursor);
        }
        skip(value);
        J.Literal literal = new J.Literal(
                randomId(),
                prefix,
                Markers.EMPTY,
                value,
                String.format("%s%s%s", delimiter, value, delimiter),
                null,
                JavaType.Primitive.String
        );
        if (!inDString) {
            skip(delimiter);
        }
        return literal;
    }

    private <J2 extends J> J2 convert(@Nullable Node t) {
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        nodes = new Cursor(nodes, t);
        //noinspection unchecked
        J2 j = (J2) t.accept(this);
        nodes = nodes.getParentOrThrow();
        return j;
    }

    private <J2 extends J> JRightPadded<J2> convert(Node t, Function<Node, Space> suffix) {
        J2 j = convert(t);
        //noinspection ConstantConditions
        return j == null ? null : new JRightPadded<>(j, suffix.apply(t), Markers.EMPTY);
    }

    private <J2 extends J> JContainer<J2> convertArgs(@Nullable Node argsNode) {
        Markers markers = Markers.EMPTY;
        Space prefix = whitespace();
        boolean omitParentheses;
        if (source.startsWith("(", cursor)) {
            skip("(");
            omitParentheses = false;
        } else {
            markers = markers.add(new OmitParentheses(randomId()));
            omitParentheses = true;
        }

        List<Node> args;
        if (argsNode == null) {
            args = singletonList(new NilNode(0));
        } else if (argsNode instanceof ListNode) {
            ListNode listNode = (ListNode) argsNode;
            args = new ArrayList<>(listNode.size());
            for (Node node : listNode.children()) {
                if (node != null) {
                    args.add(node);
                }
            }
        } else if (argsNode instanceof ArgsNode) {
            args = Arrays.asList(((ArgsNode) argsNode).getArgs());
        } else {
            throw new UnsupportedOperationException("Unexpected args node type " + argsNode.getClass().getSimpleName());
        }

        return JContainer.build(
                prefix,
                convertAll(args, n -> sourceBefore(","),
                        n -> omitParentheses ? EMPTY : sourceBefore(")")),
                markers
        );
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
                    if (source.charAt(delimIndex) == '#') {
                        inSingleLineComment = true;
                        delimIndex++;
                    } else {
                        if (source.startsWith("=begin\n", delimIndex) ||
                            source.startsWith("=begin\r\n", delimIndex)) {
                            inMultiLineComment = true;
                            delimIndex += "=begin".length();
                        } else if (source.startsWith("=end\n", delimIndex) ||
                                   source.startsWith("=end\r\n", delimIndex)) {
                            inMultiLineComment = false;
                            delimIndex += "=end".length();
                        }
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
