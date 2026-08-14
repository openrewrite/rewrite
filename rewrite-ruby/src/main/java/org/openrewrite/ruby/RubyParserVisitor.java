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

import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.AbstractNodeVisitor;
import org.jruby.ast.visitor.OperatorCallNode;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.openrewrite.Cursor;
import org.openrewrite.FileAttributes;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.jspecify.annotations.Nullable;
import org.openrewrite.java.marker.ImplicitReturn;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.internal.OpenParenthesis;
import org.openrewrite.ruby.internal.StringUtils;
import org.openrewrite.ruby.marker.*;
import org.openrewrite.ruby.tree.Rb;
import org.openrewrite.ruby.tree.Rb.ComplexString;
import org.openrewrite.ruby.tree.RubySpace;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

/**
 * For detailed descriptions of what every node type is, see
 * <a href="https://www.rubydoc.info/gems/ruby-internal/Node">Ruby internals on Node</a>.
 */
public class RubyParserVisitor extends AbstractNodeVisitor<J> {
    private final Path sourcePath;

    @Nullable
    private final FileAttributes fileAttributes;

    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    private int cursor = 0;
    private Cursor nodes = new Cursor(null, Cursor.ROOT_VALUE);

    private Queue<Rb.Heredoc> openHeredocs = new ArrayDeque<>();
    private final Queue<OpenParenthesis> openParentheses = new ArrayDeque<>();
    private final Map<Rb.Heredoc, String> heredocDelimiters = new HashMap<>();

    /**
     * Since we're already PAST the heredoc's instantiation by the time we can fully flesh out its
     * contents, we keep them here in a map and do a last pass in {@link #visitRootNode(RootNode)}
     * to replace each heredoc with its finalized contents.
     */
    private final Map<Rb.Heredoc, Rb.Heredoc> finalizedHeredocs = new HashMap<>();

    public RubyParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, EncodingDetectingInputStream source) {
        this(sourcePath, fileAttributes, source.readFully(), source.getCharset(), source.isCharsetBomMarked());
    }

    public RubyParserVisitor(Path sourcePath, @Nullable FileAttributes fileAttributes, String source,
                             Charset charset, boolean charsetBomMarked) {
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
    }

    @Override
    protected J defaultVisit(Node node) {
        throw new UnsupportedOperationException(String.format("Node type %s not yet implemented",
                node.getClass().getSimpleName()));
    }

    @Override
    public J visitAliasNode(AliasNode node) {
        return new Rb.Alias(
                randomId(),
                sourceBefore("alias"),
                Markers.EMPTY,
                convertExpression(node.getNewName()),
                convertExpression(node.getOldName())
        );
    }

    @Override
    public J visitArgsCatNode(ArgsCatNode node) {
        Space prefix = whitespace();
        boolean omitBrackets = !skip("[");
        List<JRightPadded<Expression>> elements;
        if (node.getFirstNode() instanceof ListNode) {
            elements = convertAll(Arrays.asList(((ListNode) node.getFirstNode()).children()), n -> sourceBefore(","),
                    n -> sourceBefore(","));
        } else {
            elements = singletonList(padRight(convertExpression(node.getFirstNode()), sourceBefore(",")));
        }
        // JRuby 10 hands back the SplatNode itself where 9.4 handed back the splatted value.
        Expression splat = node.getSecondNode() instanceof SplatNode ?
                convertExpression(node.getSecondNode()) :
                new Rb.Splat(
                        randomId(),
                        sourceBefore("*"),
                        Markers.EMPTY,
                        convertExpression(node.getSecondNode())
                );
        elements = ListUtils.concat(elements, padRight(splat, omitBrackets ? EMPTY : sourceBefore("]")));

        return new Rb.Array(
                randomId(),
                prefix,
                Markers.EMPTY,
                JContainer.build(
                        EMPTY,
                        elements,
                        omitBrackets ?
                                Markers.EMPTY.add(new OmitParentheses(randomId())) :
                                Markers.EMPTY
                ),
                null
        );
    }

    @Override
    public J visitAndNode(AndNode node) {
        Space prefix = whitespace();
        Expression left = convertExpression(node.getFirstNode());
        Space opPrefix = whitespace();
        String op = source.startsWith("&&", cursor) ? "&&" : "and";
        skip(op);
        return new J.Binary(
                randomId(),
                prefix,
                op.equals("&&") ? Markers.EMPTY : Markers.EMPTY.add(new EnglishOperator(randomId())),
                left,
                padLeft(opPrefix, J.Binary.Type.And),
                convertExpression(node.getSecondNode()),
                null
        );
    }

    @Override
    public J visitArgsNode(ArgsNode node) {
        throw new UnsupportedOperationException("Handled by convertArgs and skipped.");
    }

    @Override
    public Expression visitArgsPushNode(ArgsPushNode node) {
        return convertExpression(node.getFirstNode());
    }

    @Override
    public J visitArgumentNode(ArgumentNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitArrayNode(ArrayNode node) {
        if (node.size() == 1 && node.get(0) instanceof ListNode) {
            return convert(node.get(0));
        }
        return visitListNode(node);
    }

    @Override
    public J visitArrayPatternNode(ArrayPatternNode node) {
        Space prefix = whitespace();
        if (node.getConstant() != null) {
            Expression constant = convertExpression(node.getConstant());
            String delimiter = source.substring(cursor, cursor + 1);
            return new Rb.StructPattern(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    constant,
                    delimiter,
                    JContainer.build(
                            sourceBefore(delimiter),
                            singletonList(padRight(convertExpression(node.getPreArgs()), sourceBefore(StringUtils.endDelimiter(delimiter)))),
                            Markers.EMPTY
                    )
            );
        } else {
            return convert(node.getPreArgs());
        }
    }

    @Override
    public J visitAttrAssignNode(AttrAssignNode node) {
        Space prefix = whitespace();
        String attrName = node.getName().asJavaString();
        if (attrName.equals("[]=")) {
            return convertArrayAssignment(node, prefix);
        }
        return new J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.FieldAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        convertExpression(node.getReceiverNode()),
                        padLeft(sourceBefore("."), convertIdentifier(attrName.substring(0, attrName.indexOf('=')))),
                        null
                ),
                padLeft(sourceBefore("="), convertExpression(node.getArgsNode())),
                null
        );
    }

    private J.Assignment convertArrayAssignment(AttrAssignNode node, Space prefix) {
        Expression arrayAccessReceiver = convertExpression(node.getReceiverNode());
        Space arrayDimensionPrefix = sourceBefore("[");
        Expression index;
        Node assignment;
        if (node.getArgsNode() instanceof ArgsPushNode) {
            ArgsPushNode argsPushNode = (ArgsPushNode) node.getArgsNode();
            index = visitArgsPushNode(argsPushNode);
            assignment = argsPushNode.getSecondNode();
        } else {
            ListNode args = (ListNode) node.getArgsNode();
            if (args.size() == 2) {
                index = convertExpression(args.get(0));
            } else if (args.size() == 3) {
                index = new Rb.SubArrayIndex(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        convertExpression(args.get(0)),
                        padLeft(sourceBefore(","), convertExpression(args.get(1)))
                );
            } else {
                throw new IllegalStateException("Unexpected array index with 3 nodes");
            }
            assignment = args.get(args.size() - 1);
        }

        return new J.Assignment(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.ArrayAccess(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        arrayAccessReceiver,
                        new J.ArrayDimension(
                                randomId(),
                                arrayDimensionPrefix,
                                Markers.EMPTY,
                                padRight(index, sourceBefore("]"))
                        ),
                        null
                ),
                padLeft(sourceBefore("="), convertExpression(assignment)),
                null
        );
    }

    @Override
    public J visitBackRefNode(BackRefNode node) {
        return convertIdentifier("$" + node.getType());
    }

    @Override
    public J visitBeginNode(BeginNode node) {
        // JRuby 10 wraps `begin ... rescue/ensure ... end` in a BeginNode that 9.4 did not emit.
        // Rb.Rescue owns the begin/end keywords, so unwrap rather than nest.
        if (node.getBodyNode() instanceof RescueNode || node.getBodyNode() instanceof EnsureNode) {
            return convert(node.getBodyNode());
        }
        Space prefix = sourceBefore("begin");
        J body = convert(node.getBodyNode());
        if (!(body instanceof J.Block)) {
            body = new J.Block(
                    randomId(),
                    body.getPrefix(),
                    Markers.EMPTY,
                    JRightPadded.build(false),
                    singletonList(
                            padRight(
                                    body instanceof Statement ?
                                            (Statement) body :
                                            new Rb.ExpressionStatement(randomId(), (Expression) body),
                                    EMPTY
                            )
                    ),
                    EMPTY
            );
        }
        return new Rb.Begin(
                randomId(),
                prefix,
                Markers.EMPTY,
                (J.Block) body
        );
    }

    @Override
    public J visitBignumNode(BignumNode node) {
        return new J.Literal(
                randomId(),
                sourceBefore(node.getValue().toString()),
                Markers.EMPTY,
                node.getValue(),
                node.getValue().toString(),
                null,
                JavaType.Primitive.Long
        );
    }

    @Override
    public J visitBlockArgNode(BlockArgNode node) {
        return new Rb.BlockArgument(
                randomId(),
                sourceBefore("&"),
                Markers.EMPTY,
                convertIdentifier(node.getName())
        );
    }

    @Override
    public J visitBlockPassNode(BlockPassNode node) {
        return new Rb.BlockArgument(
                randomId(),
                sourceBefore("&"),
                Markers.EMPTY,
                convertExpression(node.getBodyNode())
        );
    }

    @Override
    public J visitBlockNode(BlockNode node) {
        return visitBlock(node);
    }

    /**
     * @param node A single node or a list of nodes, which in any case needs to be wrapped in a
     *             {@link J.Block}.
     * @return A {@link J.Block}.
     */
    private J.Block visitBlock(Node node) {
        return new J.Block(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                JRightPadded.build(false),
                convertBlockStatements(
                        node,
                        n -> {
                            if (nodes.getParentOrThrow().getValue() instanceof IfNode) {
                                return EMPTY;
                            }
                            Space eob = whitespace();
                            skip("end");
                            return eob;
                        }
                ),
                EMPTY
        );
    }

    @Override
    public J visitBreakNode(BreakNode node) {
        return new Rb.Break(
                randomId(),
                sourceBefore("break"),
                Markers.EMPTY,
                new J.Break(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        null
                ),
                convertExpression(node.getValueNode())
        );
    }

    @Override
    public J visitCallNode(CallNode node) {
        if (node.getName().asJavaString().equals("[]")) {
            return convertArrayAccess(node);
        }

        Space prefix = whitespace();

        TypeTree receiver = convertTypeTree(node.getReceiverNode());

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

        J.Identifier name = convertIdentifier(node.getName());
        if (name.getSimpleName().equals("new")) {
            return new J.NewClass(
                    randomId(),
                    prefix,
                    markers,
                    padRight(new J.Empty(randomId(), EMPTY, markers), beforeDot),
                    name.getPrefix(),
                    receiver,
                    convertCallArgs(node),
                    null,
                    null
            );
        } else {
            return new J.MethodInvocation(
                    randomId(),
                    prefix,
                    markers,
                    padRight((Expression) receiver, beforeDot),
                    null,
                    name,
                    convertCallArgs(node),
                    null
            );
        }
    }

    private J.ArrayAccess convertArrayAccess(CallNode callNode) {
        return convertArrayAccess(callNode.getReceiverNode(), callNode.getArgsNode(), callNode.getIterNode());
    }

    private J.ArrayAccess convertArrayAccess(Node receiverNode, Node argsNode, @Nullable Node iterNode) {
        Space prefix = whitespace();
        Expression receiver = convertExpression(receiverNode);
        JContainer<J> args = convertArgs("[", argsNode, iterNode, "]");
        List<J> argElems = args.getElements();
        if (argElems.size() == 2) {
            argElems = singletonList(new Rb.SubArrayIndex(
                    randomId(),
                    argElems.get(0).getPrefix(),
                    Markers.EMPTY,
                    argElems.get(0).withPrefix(EMPTY),
                    padLeft(args.getPadding().getElements().get(0).getAfter(), (Expression) args.getElements().get(1))
            ));
        }

        return new J.ArrayAccess(
                randomId(),
                prefix,
                Markers.EMPTY,
                receiver,
                new J.ArrayDimension(
                        randomId(),
                        args.getBefore(),
                        Markers.EMPTY,
                        padRight((Expression) argElems.get(0), args.getLastSpace())
                ),
                null
        );
    }

    private List<JRightPadded<Statement>> convertBlockStatements(@Nullable Node node, Function<Node, Space> suffix) {
        if (node == null) {
            return emptyList();
        }
        List<? extends Node> trees;
        if (node instanceof ListNode && !(node instanceof DNode) && !(node instanceof ZArrayNode)) {
            trees = isMultipleStatements((ListNode) node) ?
                    Arrays.asList(((ListNode) node).children()) :
                    singletonList(node);
        } else {
            trees = singletonList(node);
        }

        if (trees.isEmpty()) {
            return emptyList();
        }
        List<JRightPadded<Statement>> converted = new ArrayList<>(trees.size());
        for (int i = 0; i < trees.size(); i++) {
            Statement stat = convertStatement(trees.get(i));
            if (stat instanceof J.Empty) {
                continue;
            }
            converted.add(padRight(stat, i == trees.size() - 1 && !(stat instanceof Rb.Rescue) ? suffix.apply(trees.get(i)) : EMPTY));
        }
        return converted;
    }

    /**
     * We need a heuristic to tell whether a {@link ListNode} that is given as a block body represents
     * a single statement (e.g. a {@link Rb.Array}) or multiple statements. All we have is an imperfect
     * heuristic right now, and seeking a better one.
     *
     * @param node A {@link ListNode} that serves as a block body.
     * @return {@code true} if the {@link ListNode} contains more than one statement.
     */
    private boolean isMultipleStatements(ListNode node) {
        AtomicBoolean multiple = new AtomicBoolean(false);
        peekWhitespace(node, (n, prefix) -> {
            if (!source.startsWith("[", cursor)) {
                if (node.size() <= 1) {
                    multiple.set(true);
                } else {
                    int line = node.children()[0].getLine();
                    Node[] children = node.children();
                    for (int i = 1; i < children.length; i++) {
                        if (children[i].getLine() != line) {
                            multiple.set(true);
                        }
                    }
                }
            }
            return null;
        });
        return multiple.get();
    }

    @Override
    public J visitCaseNode(CaseNode node) {
        return convertCase(node.getCaseNode(), Arrays.asList(node.getCases().children()), node.getElseNode(), false);
    }

    private J convertCase(Node caseNode, List<Node> cases, @Nullable Node elseNode,
                          boolean patternCase) {
        Space prefix = whitespace();
        if (source.startsWith("case", cursor)) {
            return convertRubyCaseStatement(caseNode, cases, elseNode, patternCase)
                    .withPrefix(prefix);
        } else {
            List<Node> groupedCases = new ArrayList<>(cases.size());
            cases.forEach(groupedCases::add);
            Expression left = convertExpression(caseNode);

            Space casePrefix = whitespace();

            boolean booleanCheck = skip("in") || !skip("=>");
            J.Case pattern = ((J.Case) convertCases(groupedCases, true).getElement())
                    .withStatements(emptyList())
                    .withPrefix(casePrefix);

            if (booleanCheck) {
                return new Rb.BooleanCheck(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        left,
                        pattern,
                        null
                );
            } else {
                return new Rb.RightwardAssignment(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        left,
                        pattern,
                        null
                );
            }
        }
    }

    private J.Switch convertRubyCaseStatement(Node caseNode, List<Node> cases, @Nullable Node elseNode,
                                              boolean patternCase) {
        skip("case");
        J.ControlParentheses<Expression> selector = new J.ControlParentheses<>(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                padRight(convertExpression(caseNode), EMPTY)
        );

        Map<Integer, List<Node>> groupedWhen = new LinkedHashMap<>();
        for (Node aCase : cases) {
            if (aCase != null) {
                groupedWhen.computeIfAbsent(aCase.getLine(), k -> new ArrayList<>()).add(aCase);
            }
        }

        List<JRightPadded<Statement>> mappedCases = new ArrayList<>(groupedWhen.size());
        for (List<Node> c : groupedWhen.values()) {
            Space prefix = sourceBefore(patternCase ? "in" : "when");
            JRightPadded<Statement> mapped = convertCases(c, patternCase);
            mappedCases.add(mapped.withElement(mapped.getElement().withPrefix(prefix)));
        }

        J.Block caseBlock = new J.Block(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                JRightPadded.build(false),
                mappedCases,
                elseNode == null ? sourceBefore("end") : EMPTY
        );

        if (elseNode != null) {
            Space elsePrefix = sourceBefore("else");
            JContainer<Statement> body = JContainer.build(
                    whitespace(),
                    convertBlockStatements(elseNode, n -> EMPTY),
                    Markers.EMPTY
            );
            caseBlock = caseBlock.getPadding().withStatements(ListUtils.concat(caseBlock.getPadding().getStatements(),
                    padRight(new J.Case(
                            randomId(),
                            elsePrefix,
                            Markers.EMPTY,
                            J.Case.Type.Statement,
                            null,
                            null,
                            JContainer.empty(),
                            null,
                            body,
                            null
                    ), EMPTY)));
            caseBlock = caseBlock.withEnd(sourceBefore("end"));
        }

        return new J.Switch(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                selector,
                caseBlock
        );
    }

    private JRightPadded<Statement> convertCases(List<Node> cases, boolean patternCase) {
        List<Node> expressionNodes = new ArrayList<>();
        for (Node node : cases) {
            if (patternCase) {
                InNode in = (InNode) node;
                if (in.getExpression() instanceof ArrayNode) {
                    Collections.addAll(expressionNodes, ((ArrayNode) in.getExpression()).children());
                } else {
                    expressionNodes.add(in.getExpression());
                }
            } else {
                WhenNode when = (WhenNode) node;
                if (when.getExpressionNodes() instanceof ArrayNode) {
                    Collections.addAll(expressionNodes, ((ArrayNode) when.getExpressionNodes()).children());
                } else {
                    expressionNodes.add(when.getExpressionNodes());
                }
            }
        }

        JContainer<J> caseLabels = JContainer.build(
                whitespace(),
                convertAll(expressionNodes, n -> sourceBefore(","), n -> EMPTY),
                Markers.EMPTY
        );

        Markers markers = Markers.EMPTY;
        if (patternCase) {
            markers = markers.add(new PatternCase(randomId()));
        }

        Node body = cases.get(0) instanceof WhenNode ?
                ((WhenNode) cases.get(0)).getBodyNode() :
                ((InNode) cases.get(0)).getBody();

        caseLabels = peekWhitespace(caseLabels, (exp, beforeBody) -> {
            if (source.startsWith("then", cursor)) {
                exp = exp.withMarkers(exp.getMarkers().add(new ExplicitThen(randomId())));
                exp = exp.getPadding().withElements(ListUtils.mapLast(
                        exp.getPadding().getElements(), last -> last.withAfter(beforeBody)));
                skip("then");
                return exp;
            }
            return null;
        }).orElse(caseLabels);

        return padRight(new J.Case(
                randomId(),
                EMPTY,
                markers,
                J.Case.Type.Statement,
                null,
                null,
                caseLabels,
                null,
                JContainer.build(
                        whitespace(),
                        convertBlockStatements(body, n -> EMPTY),
                        Markers.EMPTY
                ),
                null
        ), EMPTY);
    }

    @Override
    public J visitClassNode(ClassNode node) {
        Space prefix = whitespace();
        skip("class");
        J.Identifier name = convertIdentifier(node.getCPath().getName());

        JLeftPadded<TypeTree> extendings = null;
        Node superNode = node.getSuperNode();
        if (superNode != null) {
            Space extendingsPrefix = sourceBefore("<");
            TypeTree superClass = convertTypeTree(superNode);
            extendings = padLeft(
                    extendingsPrefix,
                    superClass
            );
        }

        return new J.ClassDeclaration(
                randomId(),
                prefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                new J.ClassDeclaration.Kind(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        J.ClassDeclaration.Kind.Type.Class
                ),
                name,
                null,
                null,
                extendings,
                null,
                null,
                visitBlock(node.getBodyNode()),
                null
        );
    }

    @Override
    public J visitClassVarAsgnNode(ClassVarAsgnNode node) {
        return visitAsgnNode(node, node.getName());
    }

    @Override
    public J visitClassVarNode(ClassVarNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitColon2Node(Colon2Node node) {
        return new J.MemberReference(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                padRight(convertExpression(node.getLeftNode()), sourceBefore("::")),
                null,
                padLeft(EMPTY, convertIdentifier(node.getName())),
                null,
                null,
                null
        );
    }

    @Override
    public J visitColon3Node(Colon3Node node) {
        return new J.MemberReference(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                padRight(new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                        source.startsWith("::", cursor) ? sourceBefore("::") : EMPTY),
                null,
                padLeft(EMPTY, convertIdentifier(node.getName())),
                null,
                null,
                null
        );
    }

    @Override
    public J visitComplexNode(ComplexNode node) {
        return new Rb.NumericDomain(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                padRight(convertExpression(node.getNumber()), sourceBefore("i")),
                Rb.NumericDomain.Domain.Complex,
                null
        );
    }

    @Override
    public J visitConstDeclNode(ConstDeclNode node) {
        return visitAsgnNode(node, node.getName());
    }

    @Override
    public J visitConstNode(ConstNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitDefinedNode(DefinedNode node) {
        return new Rb.Unary(
                randomId(),
                sourceBefore("defined?"),
                Markers.EMPTY,
                Rb.Unary.Type.Defined,
                convertExpression(node.getExpressionNode())
        );
    }

    @Override
    public J visitDefnNode(DefnNode node) {
        return convertDefNode(node);
    }

    @Override
    public J visitDefsNode(DefsNode node) {
        return convertDefNode(node);
    }

    private Statement convertDefNode(MethodDefNode node) {
        Space prefix = sourceBefore("def");

        Expression classMethodReceiver = null;
        Space classMethodReceiverDot = null;
        if (node instanceof DefsNode) {
            classMethodReceiver = convertExpression(((DefsNode) node).getReceiverNode());
            classMethodReceiverDot = sourceBefore(".");
        }

        J.MethodDeclaration.IdentifierWithAnnotations name = new J.MethodDeclaration.IdentifierWithAnnotations(
                convertIdentifier(node.getName()),
                emptyList()
        );

        boolean isDelegation = false;
        for (String variable : node.getScope().getVariables()) {
            if (variable.equals("...")) {
                isDelegation = true;
                break;
            }
        }

        JContainer<J> args = isDelegation ?
                JContainer.build(sourceBefore("("), singletonList(padRight(convertIdentifier("..."),
                        sourceBefore(")"))), Markers.EMPTY) :
                convertArgs("(", node.getArgsNode(), null, ")");

        args = JContainer.withElements(args, ListUtils.map(args.getElements(), arg -> {
            if (arg instanceof J.Identifier) {
                return new J.VariableDeclarations(
                        randomId(),
                        arg.getPrefix(),
                        Markers.EMPTY,
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                        singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                                randomId(),
                                EMPTY,
                                Markers.EMPTY,
                                arg.withPrefix(EMPTY),
                                emptyList(),
                                null,
                                null
                        ), EMPTY))
                );
            }
            return arg;
        }));

        J body = convert(node.getBodyNode());
        if (!(body instanceof J.Block)) {
            Statement bodyStatement = body instanceof Statement ?
                    (Statement) body :
                    new Rb.ExpressionStatement(randomId(), (Expression) body);
            body = new J.Block(randomId(), EMPTY, Markers.EMPTY,
                    JRightPadded.build(false), singletonList(padRight(bodyStatement, EMPTY)),
                    sourceBefore("end"));
        }

        J.Block bodyBlock = (J.Block) body;
        bodyBlock = bodyBlock.getPadding().withStatements(ListUtils.mapLast(bodyBlock.getPadding().getStatements(), statement ->
                statement.getElement() instanceof J.Return || !(statement.getElement() instanceof Expression) ?
                        statement :
                        statement.withElement(new J.Return(
                                randomId(),
                                statement.getElement().getPrefix(),
                                Markers.EMPTY.add(new ImplicitReturn(randomId())),
                                statement.getElement().withPrefix(EMPTY)
                        ))
        ));

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
                (JContainer<Statement>) (JContainer<?>) args,
                emptyList(),
                null,
                bodyBlock,
                null,
                null
        );

        if (node instanceof DefsNode) {
            return new Rb.ClassMethod(
                    randomId(),
                    method.getPrefix(),
                    Markers.EMPTY,
                    classMethodReceiver,
                    padLeft(classMethodReceiverDot, method.withPrefix(EMPTY))
            );
        }

        return method;
    }

    @Override
    public J visitDotNode(DotNode node) {
        Space prefix = whitespace();
        Expression left = convertExpression(node.getBeginNode());
        Space opPrefix = whitespace();
        String op = source.substring(cursor).startsWith("...") ? "..." : "..";
        skip(op);
        return new Rb.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, op.equals("...") ? Rb.Binary.Type.RangeExclusive : Rb.Binary.Type.RangeInclusive),
                convertExpression(node.getEndNode()),
                null
        );
    }

    @Override
    public J visitDAsgnNode(DAsgnNode node) {
        return visitAsgnNode(node, node.getName());
    }

    @Override
    public J visitDRegxNode(DRegexpNode node) {
        return convertStrings(node);
    }

    @Override
    public J visitDStrNode(DStrNode node) {
        // <<~ heredocs are handled here
        return convertMaybeHeredoc(node);
    }

    private J convertMaybeHeredoc(Node node) {
        int cursorBeforeWhitespace = cursor;
        int nonWhitespace = indexOfNextNonWhitespace();
        Space prefix = RubySpace.format(source.substring(cursor, nonWhitespace));
        cursor = nonWhitespace;
        if (source.startsWith("<<", cursor)) {
            Rb.Heredoc heredoc = new Rb.Heredoc(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    null,
                    null,
                    EMPTY
            );
            openHeredocs.add(heredoc);

            // alphabets, decimal digits, and the underscore character
            int i;
            for (i = cursor + 3; i < source.length(); i++) {
                char c = source.charAt(i);
                if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') ||
                      (c >= 'A' && c <= 'Z') || (c == '_'))) {
                    break;
                }
            }

            heredocDelimiters.put(heredoc, source.substring(cursor, i));
            cursor = i;
            return heredoc;
        }
        cursor = cursorBeforeWhitespace;
        return convertStrings(node);
    }

    @Override
    public J visitDSymbolNode(DSymbolNode node) {
        return convertSymbols(node);
    }

    @Override
    public J visitDVarNode(DVarNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitDXStrNode(DXStrNode node) {
        return convertStrings(node);
    }

    @Override
    public J visitEncodingNode(EncodingNode node) {
        return convertIdentifier("__ENCODING__");
    }

    @Override
    public J visitEnsureNode(EnsureNode node) {
        Rb.Rescue rescue = (Rb.Rescue) convert(node.getBodyNode());
        Space prefix;
        if (rescue.getElse() == null) {
            List<J.Try.Catch> catches = rescue.getTry().getCatches();
            prefix = catches.get(catches.size() - 1).getBody().getEnd();
            rescue = rescue.withTry(rescue.getTry().withCatches(ListUtils.mapLast(catches, c ->
                    c.withBody(c.getBody().withEnd(EMPTY)))));
        } else {
            prefix = rescue.getElse().getEnd();
            rescue = rescue.withElse(rescue.getElse().withEnd(EMPTY));
        }

        skip("ensure");
        List<JRightPadded<Statement>> ensureBody;
        if (node.getEnsureNode() instanceof BlockNode) {
            ensureBody = convertAll(Arrays.asList(((BlockNode) node.getEnsureNode()).children()),
                    n -> EMPTY, n -> EMPTY);
        } else {
            ensureBody = singletonList(padRight(convertStatement(node.getEnsureNode()), EMPTY));
        }

        return rescue.withTry(rescue.getTry().withFinally(new J.Block(
                randomId(),
                prefix,
                Markers.EMPTY,
                JRightPadded.build(false),
                ensureBody,
                sourceBefore("end")
        )));
    }

    @Override
    public J visitEvStrNode(EvStrNode node) {
        skip("#{");
        return new Rb.ComplexString.Value(
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
        Space prefix = whitespace();
        J.Identifier name = convertIdentifier(node.getName());

        AtomicBoolean isDelegation = new AtomicBoolean(false);
        peekWhitespace(0, (n, ws) -> {
            skip("(");
            whitespace();
            isDelegation.set(source.startsWith("...", cursor));
            return null;
        });

        JContainer<Expression> args = isDelegation.get() ?
                JContainer.build(sourceBefore("("), singletonList(padRight(convertIdentifier("..."),
                        sourceBefore(")"))), Markers.EMPTY) :
                convertCallArgs(node);

        return new J.MethodInvocation(
                randomId(),
                prefix,
                Markers.EMPTY,
                null,
                null,
                name,
                args,
                null
        );
    }

    private <T extends BlockAcceptingNode & IArgumentNode> JContainer<Expression> convertCallArgs(T node) {
        return convertArgs("(", node.getArgsNode(), node.getIterNode(), ")");
    }

    @Override
    public J visitFindPatternNode(FindPatternNode node) {
        Space prefix = whitespace();
        ArrayNode allArgs = new ArrayNode(0);
        if (node.getPreRestArg() != null) {
            allArgs.add(node.getPreRestArg());
        }
        for (Node arg : node.getArgs()) {
            allArgs.add(arg);
        }
        if (node.getPostRestArg() != null) {
            allArgs.add(node.getPostRestArg());
        }
        JContainer<Expression> elements = convertArgs("[", allArgs, null, "]");
        return new Rb.Array(
                randomId(),
                prefix,
                Markers.EMPTY,
                elements,
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
    public J visitFlipNode(FlipNode node) {
        Space prefix = whitespace();
        Expression left = convertExpression(node.getBeginNode());
        Space opPrefix = sourceBefore("..");
        Rb.Binary.Type op = Rb.Binary.Type.FlipFlopInclusive;
        if (skip(".")) {
            op = Rb.Binary.Type.FlipFlopExclusive;
        }
        return new Rb.Binary(
                randomId(),
                prefix,
                Markers.EMPTY,
                left,
                padLeft(opPrefix, op),
                convertExpression(node.getEndNode()),
                JavaType.Primitive.Boolean
        );
    }

    @Override
    public J visitFloatNode(FloatNode node) {
        return new J.Literal(
                randomId(),
                sourceBefore(Double.toString(node.getValue())),
                Markers.EMPTY,
                node.getValue(),
                Double.toString(node.getValue()),
                null,
                JavaType.Primitive.Float
        );
    }

    @Override
    public J visitForNode(ForNode node) {
        Markers markers = Markers.EMPTY;
        Space prefix = sourceBefore("for");

        Expression variableIdentifier = convertExpression(node.getVarNode());
        JRightPadded<Statement> variable = padRight(new J.VariableDeclarations(
                randomId(),
                variableIdentifier.getPrefix(),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
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

        JRightPadded<Expression> iterable = padRight(convertExpression(node.getIterNode()), whitespace());

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
                padRight(convertStatement(node.getBodyNode()), sourceBefore("end"))
        );
    }

    @Override
    public J visitGlobalAsgnNode(GlobalAsgnNode node) {
        return visitAsgnNode(node, node.getName());
    }

    @Override
    public J visitGlobalVarNode(GlobalVarNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitHashNode(HashNode node) {
        if (node.hasOnlyRestKwargs()) {
            Space prefix = sourceBefore("**");
            return convert(node.getPairs().get(0).getValue())
                    .withPrefix(prefix)
                    .withMarkers(Markers.EMPTY.add(new KeywordRestArgument(randomId())));
        }
        return convertHash(node, null);
    }

    private Rb.Hash convertHash(HashNode node, @Nullable Node restArg) {
        Space prefix = whitespace();
        boolean omitBrackets = !skip("{");
        List<JRightPadded<Expression>> pairs = new ArrayList<>(node.getPairs().size());
        List<KeyValuePair<Node, Node>> nodePairs = node.getPairs();
        for (int i = 0; i < nodePairs.size(); i++) {
            KeyValuePair<Node, Node> kv = nodePairs.get(i);
            Space kvPrefix = whitespace();
            Expression key = convertExpression(kv.getKey());
            Space separatorPrefix = whitespace();

            Rb.Hash.KeyValue.Separator separator = skip("=>") || !skip(":") ?
                    Rb.Hash.KeyValue.Separator.Rocket :
                    Rb.Hash.KeyValue.Separator.Colon;

            AtomicReference<Expression> value = new AtomicReference<>(null);
            peekWhitespace(0, (n, valuePrefix) -> {
                if (kv.getValue() instanceof LocalAsgnNode &&
                    !source.startsWith(((LocalAsgnNode) kv.getValue()).getName().asJavaString(), cursor)) {
                    // in hash pattern matching you can match on {sym:} with no value
                    // to the right of the symbol. not valid in hash literals
                    value.set(new J.Empty(randomId(), EMPTY, Markers.EMPTY));
                    return null;
                } else {
                    value.set(convert(kv.getValue()).withPrefix(valuePrefix));
                    return 0;
                }
            });

            pairs.add(padRight(new Rb.Hash.KeyValue(
                    randomId(),
                    kvPrefix,
                    Markers.EMPTY,
                    key,
                    padLeft(separatorPrefix, separator),
                    value.get(),
                    null
            ), i == nodePairs.size() - 1 && restArg == null ? EMPTY : sourceBefore(",")));
        }
        if (restArg != null) {
            pairs.add(padRight(convertExpression(restArg), EMPTY));
        }
        if (nodePairs.isEmpty() && restArg == null) {
            pairs.add(padRight(new J.Empty(randomId(), EMPTY, Markers.EMPTY), EMPTY));
        }

        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        if (!omitBrackets) {
            pairs = ListUtils.mapLast(pairs, last -> last.withAfter(maybeTrailingComma(markers, "}")));
        }

        return new Rb.Hash(
                randomId(),
                prefix,
                omitBrackets ?
                        Markers.EMPTY.add(new OmitParentheses(randomId())) :
                        Markers.EMPTY,
                JContainer.build(EMPTY, pairs, markers.get()),
                null
        );
    }

    @Override
    public J visitHashPatternNode(HashPatternNode node) {
        Space prefix = whitespace();
        if (node.getConstant() != null) {
            Expression constant = convertExpression(node.getConstant());
            String delimiter = source.substring(cursor, cursor + 1);
            return new Rb.StructPattern(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    constant,
                    delimiter,
                    JContainer.build(
                            sourceBefore(delimiter),
                            singletonList(padRight(convertHash(node.getKeywordArgs(), node.getRestArg()),
                                    sourceBefore(StringUtils.endDelimiter(delimiter)))),
                            Markers.EMPTY
                    )
            );
        } else {
            return convertHash(node.getKeywordArgs(), node.getRestArg());
        }
    }

    @Override
    public J visitIfNode(IfNode node) {
        Space prefix = whitespace();
        if (source.startsWith("if", cursor) || source.startsWith("unless", cursor)) {
            return ifStatement(node).withPrefix(prefix);
        } else if (node.getThenBody() == null || node.getElseBody() == null) {
            return ifModifier(node).withPrefix(prefix);
        }
        return ternary(node).withPrefix(prefix);
    }

    private J.Ternary ternary(IfNode node) {
        return new J.Ternary(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                convertExpression(node.getCondition()),
                padLeft(sourceBefore("?"), convertExpression(node.getThenBody())),
                padLeft(sourceBefore(":"), convertExpression(node.getElseBody())),
                null
        );
    }

    private J.If ifModifier(IfNode node) {
        J thenElem = convert(node.getThenBody() == null ? node.getElseBody() : node.getThenBody());
        Markers markers = Markers.EMPTY.add(new IfModifier(randomId()));
        if (node.getThenBody() == null) {
            markers = markers.add(new Unless(randomId()));
        }
        if (!(thenElem instanceof Statement)) {
            thenElem = new Rb.ExpressionStatement(randomId(), (Expression) thenElem);
        }
        JRightPadded<Statement> then = padRight((Statement) thenElem, whitespace());
        skip(node.getThenBody() == null ? "unless" : "if");

        Space ifConditionPrefix = whitespace();
        Expression ifConditionExpr = convertExpression(node.getCondition());
        J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<>(
                randomId(),
                ifConditionPrefix,
                Markers.EMPTY,
                padRight(ifConditionExpr, EMPTY)
        );
        return new J.If(
                randomId(),
                EMPTY,
                markers,
                ifCondition,
                then,
                null
        );
    }

    private J.If ifStatement(IfNode node) {
        Markers markers = Markers.EMPTY;
        if (!skip("if")) {
            skip("unless");
            markers = markers.add(new Unless(randomId()));
        }
        Space ifConditionPrefix = whitespace();
        Expression ifConditionExpr = convertExpression(node.getCondition());
        boolean explicitThen = source.startsWith("then", indexOfNextNonWhitespace());
        J.ControlParentheses<Expression> ifCondition = new J.ControlParentheses<>(
                randomId(),
                ifConditionPrefix,
                explicitThen ?
                        Markers.EMPTY.add(new ExplicitThen(randomId())) :
                        Markers.EMPTY,
                padRight(ifConditionExpr, explicitThen ? sourceBefore("then") : EMPTY)
        );

        Statement thenElem = convertStatement(node.getThenBody());
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
                    padRight(convertStatement(node.getElseBody()),
                            node.getElseBody() instanceof IfNode ? EMPTY : sourceBefore("end"))
            );
        }

        return new J.If(
                randomId(),
                EMPTY,
                markers,
                ifCondition,
                then,
                anElse
        );
    }

    @Override
    public J visitInNode(InNode node) {
        return super.visitInNode(node);
    }

    @Override
    public J visitInstAsgnNode(InstAsgnNode node) {
        return visitAsgnNode(node, node.getName());
    }

    @Override
    public J visitInstVarNode(InstVarNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitIterNode(IterNode node) {
        Space prefix = whitespace();
        J.Block body;
        JContainer<J> parameters = null;
        boolean inline = source.charAt(cursor) == '{';
        if (inline) {
            skip("{");
            if (!node.getArgsNode().isEmpty() || node.getArgsNode().getBlock() != null) {
                parameters = convertArgs("|", node.getArgsNode(), null, "|");
            }
            body = visitBlock(node.getBodyNode());
            skip("}");
        } else {
            skip("do");
            if (!node.getArgsNode().isEmpty()) {
                parameters = convertArgs("|", node.getArgsNode(), null, "|");
            }
            body = visitBlock(node.getBodyNode());
            // visitBlock handles the skip("end")
        }
        return new Rb.Block(
                randomId(),
                prefix,
                Markers.EMPTY,
                inline,
                parameters,
                body
        );
    }

    @Override
    public J visitKeywordArgNode(KeywordArgNode node) {
        LocalAsgnNode assignable = (LocalAsgnNode) node.getAssignable();
        return new J.VariableDeclarations(
                randomId(),
                whitespace(),
                Markers.EMPTY.add(new KeywordArgument(randomId())),
                emptyList(),
                emptyList(),
                null,
                null,
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        convertIdentifier(assignable.getName()),
                        emptyList(),
                        padLeft(sourceBefore(":"), convertExpression(assignable.getValueNode())),
                        null
                ), EMPTY))
        );
    }

    @Override
    public J visitKeywordRestArgNode(KeywordRestArgNode node) {
        return new J.VariableDeclarations(
                randomId(),
                whitespace(),
                Markers.EMPTY.add(new KeywordRestArgument(randomId())),
                emptyList(),
                emptyList(),
                null,
                sourceBefore("**"),
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        convertIdentifier(node.getName()),
                        emptyList(),
                        null,
                        null
                ), EMPTY))
        );
    }

    @Override
    public J visitLambdaNode(LambdaNode node) {
        Space prefix = sourceBefore("->");
        Space parametersPrefix = whitespace();
        JContainer<J> args = convertArgs("(", node.getArgsNode(), null, ")");
        return new J.Lambda(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.Lambda.Parameters(
                        randomId(),
                        parametersPrefix,
                        Markers.EMPTY,
                        true,
                        args.getPadding().getElements()
                ),
                EMPTY,
                new J.Block(
                        randomId(),
                        sourceBefore("{"),
                        Markers.EMPTY,
                        JRightPadded.build(false),
                        convertBlockStatements(node.getBodyNode(), n -> EMPTY),
                        sourceBefore("}")
                ),
                null
        );
    }

    @Override
    public J visitListNode(ListNode node) {
        Space prefix = whitespace();

        if (!node.isEmpty() && source.startsWith("%", cursor)) {
            Node first = node.get(0);
            if (first instanceof SymbolNode || first instanceof DSymbolNode) {
                // this is a symbol array literal like %i[foo bar baz]
                return convertSymbols(node.children()).withPrefix(prefix);
            } else if (first instanceof StrNode || first instanceof DStrNode) {
                // this is a string array literal like %w[foo bar baz]
                return convertStrings(node.children()).withPrefix(prefix);
            }
        }

        JContainer<Expression> elements = convertArgs("[", node, null, "]");
        return new Rb.Array(
                randomId(),
                prefix,
                Markers.EMPTY,
                elements,
                null
        );
    }

    @Override
    public J visitLiteralNode(LiteralNode node) {
        return convertIdentifier(node.getSymbolName());
    }

    @Override
    public J visitLocalAsgnNode(LocalAsgnNode node) {
        Space prefix = whitespace();
        boolean namedSingleSplat = source.startsWith("*" + node.getName().asJavaString(), cursor);
        if (namedSingleSplat) {
            skip("*");
        }
        openParensCloseLeft();
        Expression assignment = visitAsgnNode(node, node.getName());
        if (namedSingleSplat) {
            J.Identifier named = (J.Identifier) assignment;
            assignment = named.withSimpleName("*" + named.getSimpleName());
        }
        return assignment.withPrefix(prefix);
    }

    private Expression visitAsgnNode(AssignableNode node, RubySymbol name) {
        if (node.getValueNode() instanceof OperatorCallNode) {
            Space variablePrefix = whitespace();
            J.Identifier variable = convertIdentifier(name);
            OperatorCallNode assignOp = (OperatorCallNode) node.getValueNode();
            Space opPrefix = whitespace();
            if (source.charAt(cursor) == '=') {
                skip("=");
                return new J.Assignment(
                        randomId(),
                        variablePrefix,
                        Markers.EMPTY,
                        variable,
                        padLeft(opPrefix, visitOperatorCallNode(assignOp)),
                        null
                );
            } else {
                Expression mapped = convertOpAsgn(
                        () -> variable,
                        assignOp.getName().asJavaString(), ((ListNode) assignOp.getArgsNode()).get(0)
                ).withPrefix(variablePrefix);
                if (mapped instanceof J.AssignmentOperation) {
                    J.AssignmentOperation j = (J.AssignmentOperation) mapped;
                    return j.getPadding().withOperator(j.getPadding().getOperator().withBefore(opPrefix));
                }
                Rb.AssignmentOperation r = (Rb.AssignmentOperation) mapped;
                return r.getPadding().withOperator(r.getPadding().getOperator().withBefore(opPrefix));
            }
        } else {
            Space prefix = whitespace();
            J.Identifier variable = convertIdentifier(name);
            if (node.getValueNode() instanceof NilImplicitNode) {
                return variable.withPrefix(prefix);
            }
            return new J.Assignment(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    variable,
                    padLeft(sourceBefore("="), convertExpression(node.getValueNode())),
                    null
            );
        }
    }

    private Object convertAssignmentOpType(String op) {
        switch (op) {
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
            case "&&":
                return Rb.AssignmentOperation.Type.And;
            case "||":
                return Rb.AssignmentOperation.Type.Or;
            default:
                throw new UnsupportedOperationException("Unsupported assignment operator " + op);
        }
    }

    @Override
    public J visitLocalVarNode(LocalVarNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitMatchNode(MatchNode node) {
        return convert(node.getRegexpNode());
    }

    @Override
    public J visitMatch2Node(Match2Node node) {
        return convert(node.getReceiverNode());
    }

    @Override
    public J visitMatch3Node(Match3Node node) {
        return new Rb.Binary(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                convertExpression(node.getReceiverNode()),
                padLeft(sourceBefore("=~"), Rb.Binary.Type.Match),
                convertExpression(node.getValueNode()),
                null
        );
    }

    @Override
    public J visitModuleNode(ModuleNode node) {
        return new Rb.Module(
                randomId(),
                sourceBefore("module"),
                Markers.EMPTY,
                convertIdentifier(node.getCPath().getName()),
                new J.Block(
                        randomId(),
                        whitespace(),
                        Markers.EMPTY,
                        JRightPadded.build(false),
                        convertBlockStatements(node.getBodyNode(), n -> EMPTY),
                        sourceBefore("end")
                )
        );
    }

    @Override
    public J visitMultipleAsgnNode(MultipleAsgnNode node) {
        Space prefix = whitespace();
        JContainer<Expression> assignments = convertArgs("(", node.getPre(), null, ")");
        if (node.getRest() != null) {
            Space lastComma = assignments.getMarkers().findFirst(TrailingComma.class)
                    .map(TrailingComma::getSuffix).orElse(EMPTY);
            assignments = assignments.withMarkers(assignments.getMarkers().removeByType(TrailingComma.class));
            assignments = assignments.getPadding().withElements(ListUtils.concat(
                    ListUtils.mapLast(assignments.getPadding().getElements(), assign -> assign.withAfter(lastComma)),
                    padRight(convertExpression(node.getRest()), EMPTY)
            ));
        }
        Space initializerPrefix = sourceBefore("=");
        Space firstArgPrefix = whitespace();
        JContainer<Expression> initializers =
                source.startsWith("[", cursor) ?
                        JContainer.build(initializerPrefix, singletonList(padRight(visitArrayNode(
                                (ArrayNode) node.getValueNode()).withPrefix(firstArgPrefix), EMPTY)), Markers.EMPTY) :
                        JContainer.build(
                                prefix,
                                ListUtils.mapFirst(
                                        this.<Expression>convertArgs("[", node.getValueNode(), null, "]").getPadding().getElements(),
                                        arg -> arg.withElement(arg.getElement().withPrefix(firstArgPrefix))
                                ),
                                Markers.EMPTY
                        ).withBefore(initializerPrefix);
        return new Rb.MultipleAssignment(
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
        return new Rb.Next(
                randomId(),
                sourceBefore("next"),
                Markers.EMPTY,
                new J.Continue(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        null
                ),
                convertExpression(node.getValueNode())
        );
    }

    @Override
    public J visitNilNode(NilNode node) {
        Space prefix = whitespace();
        return source.startsWith("nil", cursor) ?
                convertIdentifier("nil").withPrefix(prefix) :
                new J.Empty(randomId(), prefix, Markers.EMPTY);
    }

    @Override
    public J visitNilRestArgNode(NilRestArgNode node) {
        return convertIdentifier("**nil");
    }

    @Override
    public J visitNthRefNode(NthRefNode node) {
        return convertIdentifier("$" + node.getMatchNumber());
    }

    @Override
    public J visitOpAsgnNode(OpAsgnNode node) {
        Supplier<Expression> receiver = () -> {
            Expression r = convertExpression(node.getReceiverNode());
            if (node.getVariableName() != null) {
                r = new J.FieldAccess(
                        randomId(),
                        r.getPrefix(),
                        Markers.EMPTY,
                        r.withPrefix(EMPTY),
                        padLeft(sourceBefore("."), convertIdentifier(node.getVariableName())),
                        null
                );
            }
            return r;
        };
        return convertOpAsgn(receiver, node.getOperatorName(), node.getValueNode());
    }

    @Override
    public J visitOpAsgnAndNode(OpAsgnAndNode node) {
        return convertOpAsgn(() -> convertExpression(node.getFirstNode()), "&&", node.getSecondNode());
    }

    @Override
    public J visitOpAsgnOrNode(OpAsgnOrNode node) {
        return convertOpAsgn(() -> convertExpression(node.getFirstNode()), "||", node.getSecondNode());
    }

    @Override
    public J visitOpElementAsgnNode(OpElementAsgnNode node) {
        return convertOpAsgn(
                () -> convertArrayAccess(node.getReceiverNode(), node.getArgsNode(), null),
                node.getOperatorName(),
                node.getValueNode()
        );
    }

    private Expression convertOpAsgn(Supplier<Expression> first, String op, Node second) {
        Expression firstExpr = first.get();
        Space prefix = firstExpr.getPrefix();
        firstExpr = firstExpr.withPrefix(EMPTY);

        Object opType = convertAssignmentOpType(op);

        if (second instanceof LocalAsgnNode) {
            second = ((LocalAsgnNode) second).getValueNode();
        } else if (second instanceof InstAsgnNode) {
            second = ((InstAsgnNode) second).getValueNode();
        }

        if (opType instanceof Rb.AssignmentOperation.Type) {
            return new Rb.AssignmentOperation(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    firstExpr,
                    padLeft(sourceBefore(op + "="), (Rb.AssignmentOperation.Type) opType),
                    convertExpression(second),
                    null
            );
        } else {
            return new J.AssignmentOperation(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    firstExpr,
                    padLeft(sourceBefore(op + "="), (J.AssignmentOperation.Type) opType),
                    convertExpression(second),
                    null
            );
        }
    }

    @Override
    public Expression visitOperatorCallNode(OperatorCallNode node) {
        String op = node.getName().asJavaString();
        if (op.endsWith("@")) {
            op = op.substring(0, op.length() - 1);
        }
        Markers markers = Markers.EMPTY;
        J.Binary.Type type = null;
        J.Unary.Type unaryType = null;
        Rb.Binary.Type rubyType = null;
        switch (op) {
            case "+":
                if (node.getArgsNode() == null) {
                    unaryType = J.Unary.Type.Positive;
                } else {
                    type = J.Binary.Type.Addition;
                }
                break;
            case "-":
                if (node.getArgsNode() == null) {
                    unaryType = J.Unary.Type.Negative;
                } else {
                    type = J.Binary.Type.Subtraction;
                }
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
                rubyType = Rb.Binary.Type.Exponentiation;
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
                unaryType = J.Unary.Type.Complement;
                break;
            case "==":
                type = J.Binary.Type.Equal;
                break;
            case "===":
                rubyType = Rb.Binary.Type.Within;
                break;
            case "!=":
                type = J.Binary.Type.NotEqual;
                break;
            case "<=>":
                rubyType = Rb.Binary.Type.Comparison;
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
                    convertExpression(node.getReceiverNode()),
                    padLeft(sourceBefore(op), type),
                    convertExpression(node.getArgsNode().childNodes().get(0)),
                    null
            );
        } else if (unaryType != null) {
            return new J.Unary(
                    randomId(),
                    whitespace(),
                    markers,
                    padLeft(sourceBefore(op), unaryType),
                    convertExpression(node.getReceiverNode()),
                    null
            );
        } else {
            return new Rb.Binary(
                    randomId(),
                    whitespace(),
                    markers,
                    convertExpression(node.getReceiverNode()),
                    padLeft(sourceBefore(op), rubyType),
                    convertExpression(node.getArgsNode().childNodes().get(0)),
                    null
            );
        }
    }

    @Override
    public J visitOptArgNode(OptArgNode node) {
        return new J.VariableDeclarations(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                null,
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        convertIdentifier(node.getName()),
                        emptyList(),
                        node.getValue() == null ?
                                null :
                                padLeft(sourceBefore("="), convertExpression(((LocalAsgnNode) node.getValue()).getValueNode())),
                        null
                ), EMPTY))
        );
    }

    @Override
    public J visitOrNode(OrNode node) {
        Space prefix = whitespace();
        Expression left = convertExpression(node.getFirstNode());
        Space opPrefix = whitespace();
        String op = source.startsWith("||", cursor) ? "||" : "or";
        skip(op);
        return new J.Binary(
                randomId(),
                prefix,
                op.equals("||") ? Markers.EMPTY : Markers.EMPTY.add(new EnglishOperator(randomId())),
                left,
                padLeft(opPrefix, J.Binary.Type.Or),
                convertExpression(node.getSecondNode()),
                null
        );
    }

    @Override
    public J visitPatternCaseNode(PatternCaseNode node) {
        return convertCase(node.getCaseNode(), Arrays.asList(node.getCases()), node.getElseNode(), true);
    }

    @Override
    public J visitPostExeNode(PostExeNode node) {
        Space prefix = sourceBefore("END");
        return new Rb.PostExecution(
                randomId(),
                prefix,
                Markers.EMPTY,
                convertBeginEndBlock(node)
        );
    }

    @Override
    public J visitPreExeNode(PreExeNode node) {
        Space prefix = sourceBefore("BEGIN");
        return new Rb.PreExecution(
                randomId(),
                prefix,
                Markers.EMPTY,
                convertBeginEndBlock(node)
        );
    }

    private J.Block convertBeginEndBlock(IterNode node) {
        return new J.Block(
                randomId(),
                sourceBefore("{"),
                Markers.EMPTY,
                JRightPadded.build(false),
                convertBlockStatements(node.getBodyNode(), n -> EMPTY),
                sourceBefore("}")
        );
    }

    @Override
    public J visitRationalNode(RationalNode node) {
        return new Rb.NumericDomain(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                padRight(convertExpression(node.getNumerator()), sourceBefore("r")),
                Rb.NumericDomain.Domain.Rational,
                null
        );
    }

    @Override
    public J visitRedoNode(RedoNode node) {
        return new Rb.Redo(
                randomId(),
                sourceBefore("redo"),
                Markers.EMPTY
        );
    }

    @Override
    public J visitRegexpNode(RegexpNode node) {
        ComplexString j = (ComplexString) convertStrings(new StrNode(node.getLine(), node.getValue()));
        return j.withRegexpOptions(convertRegexpOptions(node));
    }

    @Override
    public J visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode node) {
        return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
    }

    @Override
    public J visitRescueBodyNode(RescueBodyNode node) {
        throw new UnsupportedOperationException("RescueBodyNode is a recursive data structure that is " +
                                                "handled by visitRescueNode and so should never be called.");
    }

    @Override
    public J visitRescueNode(RescueNode node) {
        Space prefix = whitespace();
        skip("begin");
        Space bodyPrefix = whitespace();
        J.Block body = visitBlock(node.getBodyNode());
        List<J.Try.Catch> catches = convertCatches(node.getRescueNode(), emptyList());

        J.Block elseBlock = null;
        if (node.getElseNode() != null) {
            elseBlock = new J.Block(
                    randomId(),
                    sourceBefore("else"),
                    Markers.EMPTY,
                    JRightPadded.build(false),
                    convertBlockStatements(node.getElseNode(), n -> EMPTY),
                    whitespace()
            );
        }

        if (elseBlock == null) {
            // whitespace rather than sourceBefore("end") because an ensure may follow
            catches = ListUtils.mapLast(catches, c -> c.withBody(c.getBody().withEnd(whitespace())));
        }
        skip("end");

        return new Rb.Rescue(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.Try(
                        randomId(),
                        bodyPrefix,
                        Markers.EMPTY,
                        JContainer.empty(),
                        body,
                        catches,
                        null
                ),
                elseBlock
        );
    }

    private List<J.Try.Catch> convertCatches(@Nullable RescueBodyNode rescue, List<J.Try.Catch> mappedRescues) {
        if (rescue == null) {
            return mappedRescues;
        }
        // delay allocation until at least one rescue is found
        if (mappedRescues.isEmpty()) {
            mappedRescues = new ArrayList<>(3);
        }

        Space prefix = sourceBefore("rescue");
        Space exceptionVariablePrefix = whitespace();

        List<JRightPadded<J>> exceptionTypes = convertAll(
                Arrays.asList((((ArrayNode) rescue.getExceptionNodes()).children())),
                n -> sourceBefore(","), n -> EMPTY);

        List<JRightPadded<J.VariableDeclarations.NamedVariable>> names = new ArrayList<>(1);
        List<JRightPadded<Statement>> catchBody;
        Space beforeExceptionNamePrefix = whitespace();
        Space bodyPrefix = beforeExceptionNamePrefix;
        if (skip("=>")) {
            BlockNode body = (BlockNode) rescue.getBodyNode();

            // because the exceptionName is being assigned to $!
            J.Identifier exceptionName = convertIdentifier(((INameNode) body.get(0)).getName());
            names.add(padRight(new J.VariableDeclarations.NamedVariable(
                    randomId(),
                    beforeExceptionNamePrefix,
                    Markers.EMPTY,
                    exceptionName,
                    emptyList(),
                    null,
                    null
            ), EMPTY));

            bodyPrefix = whitespace();
            List<Node> bodyNodes = new ArrayList<>(3);
            if (body.children()[1] instanceof BlockNode) {
                Collections.addAll(bodyNodes, ((BlockNode) body.children()[1]).children());
            } else {
                bodyNodes.add(body.children()[1]);
            }
            catchBody = convertAll(bodyNodes, n -> EMPTY, n -> EMPTY);
        } else if (rescue.getBodyNode() instanceof BlockNode) {
            catchBody = convertAll(Arrays.asList(((BlockNode) rescue.getBodyNode()).children()),
                    n -> EMPTY, n -> EMPTY);
        } else {
            catchBody = singletonList(padRight(convertStatement(rescue.getBodyNode()), EMPTY));
        }

        TypeTree exceptionType;
        if (exceptionTypes.size() == 1) {
            exceptionType = asTypeTree(exceptionTypes.get(0).getElement());
        } else {
            exceptionType = new J.MultiCatch(randomId(), EMPTY, Markers.EMPTY,
                    exceptionTypes.stream()
                            .map(t -> new JRightPadded<NameTree>(asTypeTree(t.getElement()), t.getAfter(), t.getMarkers()))
                            .collect(toList()));
        }

        J.VariableDeclarations exceptionVariable = new J.VariableDeclarations(
                randomId(),
                exceptionVariablePrefix,
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                exceptionType,
                null,
                names
        );

        mappedRescues.add(new J.Try.Catch(
                randomId(),
                prefix,
                Markers.EMPTY,
                new J.ControlParentheses<>(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        padRight(exceptionVariable, EMPTY)
                ),
                new J.Block(
                        randomId(),
                        bodyPrefix,
                        Markers.EMPTY,
                        JRightPadded.build(false),
                        catchBody,
                        EMPTY
                )
        ));

        return convertCatches(rescue.getOptRescueNode(), mappedRescues);
    }

    @Override
    public J visitRestArgNode(RestArgNode node) {
        return new J.VariableDeclarations(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                emptyList(),
                emptyList(),
                null,
                sourceBefore("*"),
                singletonList(padRight(new J.VariableDeclarations.NamedVariable(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        convertIdentifier(node.getName()),
                        emptyList(),
                        null,
                        null
                ), EMPTY))
        );
    }

    @Override
    public J visitRetryNode(RetryNode node) {
        return new Rb.Retry(
                randomId(),
                sourceBefore("retry"),
                Markers.EMPTY
        );
    }

    @Override
    public J visitReturnNode(ReturnNode node) {
        Space prefix = sourceBefore("return");
        Expression returnValue = convertExpression(node.getValueNode());
        return new J.Return(
                randomId(),
                prefix,
                Markers.EMPTY,
                returnValue
        );
    }

    @Override
    public Rb.CompilationUnit visitRootNode(RootNode node) {
        Space prefix = whitespace();
        List<JRightPadded<Statement>> statements = convertBlockStatements(node.getBodyNode(), n -> EMPTY);
        Rb.CompilationUnit cu = new Rb.CompilationUnit(
                randomId(),
                prefix,
                Markers.EMPTY,
                sourcePath,
                fileAttributes,
                charset.name(),
                charsetBomMarked,
                null,
                statements,
                whitespace()
        );
        if (!finalizedHeredocs.isEmpty()) {
            return (Rb.CompilationUnit) new RubyIsoVisitor<Integer>() {
                @Override
                public Rb.Heredoc visitHeredoc(Rb.Heredoc heredoc, Integer p) {
                    return finalizedHeredocs.get(heredoc);
                }
            }.visitNonNull(cu, 0);
        }
        return cu;
    }

    @Override
    public J visitSClassNode(SClassNode node) {
        return new Rb.OpenEigenclass(
                randomId(),
                sourceBefore("class"),
                Markers.EMPTY,
                padLeft(sourceBefore("<<"), convertExpression(node.getReceiverNode())),
                new J.Block(
                        randomId(),
                        whitespace(),
                        Markers.EMPTY,
                        JRightPadded.build(false),
                        convertBlockStatements(node.getBodyNode(), n -> EMPTY),
                        sourceBefore("end")
                )
        );
    }

    @Override
    public J visitSelfNode(SelfNode node) {
        return convertIdentifier("self");
    }

    @Override
    public J visitSplatNode(SplatNode node) {
        return new Rb.Splat(
                randomId(),
                sourceBefore("*"),
                Markers.EMPTY,
                convertExpression(node.getValue())
        );
    }

    @Override
    public J visitStarNode(StarNode node) {
        return convertIdentifier("*");
    }

    @Override
    public J visitStrNode(StrNode node) {
        // <<- heredocs are handled here
        return convertMaybeHeredoc(node);
    }

    /**
     * @param nodes An array of either {@link StrNode} or {@link DNode}.
     * @return A {@link Rb.ComplexString}, {@link J.Literal}, or {@link Rb.DelimitedArray} node.
     */
    public J convertStrings(Node... nodes) {
        Object parentValue = this.nodes.getParentOrThrow().getValue();
        boolean inDString = parentValue instanceof DStrNode || parentValue instanceof DXStrNode ||
                            parentValue instanceof DRegexpNode;
        Space prefix = inDString ? EMPTY : whitespace();
        String delimiter = "";
        if (!inDString) {
            if (source.charAt(cursor) == '%') {
                switch (source.charAt(cursor + 1)) {
                    case 'q':
                    case 'Q':
                    case 'w':
                    case 'W':
                    case 'x':
                    case 'r':
                        delimiter = source.substring(cursor, cursor + 3);
                        break;
                    default:
                        // the solo % case
                        delimiter = source.substring(cursor, cursor + 2);
                }
            } else {
                delimiter = source.substring(cursor, cursor + 1);
            }
        }
        if (!delimiter.isEmpty()) {
            skip(delimiter);
            this.nodes.putMessage("delimiter", delimiter);
        }

        J stringly;
        if (delimiter.startsWith("%w") || delimiter.startsWith("%W")) {
            List<JRightPadded<Expression>> strings = new ArrayList<>(nodes.length);
            for (Node node : nodes) {
                if (node instanceof StrNode) {
                    strings.add(padRight(convertStringLiteral((StrNode) node, delimiter, true), whitespace()));
                } else {
                    strings.add(padRight(convertComplexString((DNode) nodes[0], ""), whitespace()));
                }
            }
            stringly = new Rb.DelimitedArray(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    delimiter,
                    JContainer.build(EMPTY, strings, Markers.EMPTY),
                    null
            );
        } else if (nodes[0] instanceof StrNode) {
            boolean isRegex = delimiter.startsWith("%r") || delimiter.startsWith("/");
            stringly = convertStringLiteral((StrNode) nodes[0], isRegex || inDString ? "" : delimiter, false);
            if (isRegex) {
                stringly = new Rb.ComplexString(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        delimiter,
                        JContainer.build(singletonList(padRight(stringly, EMPTY))),
                        emptyList()
                );
            }
        } else if (nodes[0] instanceof DNode) {
            stringly = convertComplexString((DNode) nodes[0], delimiter);
        } else {
            throw new UnsupportedOperationException("Unexpected string node type " + nodes[0].getClass().getSimpleName());
        }

        skip(StringUtils.endDelimiter(delimiter));

        if (nodes[0] instanceof DRegexpNode) {
            stringly = ((ComplexString) stringly).withRegexpOptions(convertRegexpOptions(nodes[0]));
        }

        return stringly.withPrefix(prefix);
    }

    private List<ComplexString.RegexpOptions> convertRegexpOptions(Node node) {
        RegexpOptions options = node instanceof DRegexpNode ?
                ((DRegexpNode) node).getOptions() :
                ((RegexpNode) node).getOptions();
        return regexOptionsString(options).chars().mapToObj(opt -> {
            switch (opt) {
                case 'x':
                    return ComplexString.RegexpOptions.Extended;
                case 'i':
                    return ComplexString.RegexpOptions.IgnoreCase;
                case 'm':
                    return ComplexString.RegexpOptions.Multiline;
                case 'j':
                    return ComplexString.RegexpOptions.Java;
                case 'o':
                    return ComplexString.RegexpOptions.Once;
                case 'n':
                    return ComplexString.RegexpOptions.None;
                case 'e':
                    return ComplexString.RegexpOptions.EUCJPEncoding;
                case 's':
                    return ComplexString.RegexpOptions.SJISEncoding;
                case 'u':
                    return ComplexString.RegexpOptions.UTF8Encoding;
                default:
                    throw new UnsupportedOperationException(String.format("Unknown regexp option %s", opt));
            }
        }).collect(toList());
    }

    private String regexOptionsString(RegexpOptions options) {
        int optionCount = 0;
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
        if (options.isOnce()) {
            optionCount++;
        }
        String optionsString = source.substring(cursor, cursor + optionCount);
        skip(optionsString);
        return optionsString;
    }

    private Expression convertComplexString(DNode node, String delimiter) {
        // implicitly concatenated strings
        List<JRightPadded<J>> strings = new ArrayList<>(node.size());

        // the parts of a single string (literal + evaluated portions)
        List<JRightPadded<J>> parts = new ArrayList<>(node.size());

        for (int i = 0; i < node.size(); i++) {
            Node n = node.get(i);
            if (!delimiter.isEmpty() && skip(StringUtils.endDelimiter(delimiter))) {
                strings.add(padRight(new Rb.ComplexString(
                        randomId(),
                        EMPTY,
                        Markers.EMPTY,
                        delimiter,
                        JContainer.build(parts),
                        emptyList()
                ), sourceBefore(delimiter)));
                skip(delimiter);
                parts = new ArrayList<>(node.size());
            }

            if (!(n instanceof StrNode)) {
                nodes = new Cursor(nodes, n);
                parts.add(padRight(n.accept(this), EMPTY));
                nodes = nodes.getParentOrThrow();
            } else if (!((StrNode) n).getValue().isEmpty()) {
                // bypassing convert(..) because the string literal may start with parentheses
                nodes = new Cursor(nodes, n);
                parts.add(padRight(visitStrNode((StrNode) n), EMPTY));
                nodes = nodes.getParentOrThrow();
            }
        }

        strings.add(padRight(new Rb.ComplexString(
                randomId(),
                EMPTY,
                Markers.EMPTY,
                delimiter,
                JContainer.build(parts),
                emptyList()
        ), EMPTY));

        Expression expression = (Expression) strings.get(0).getElement();
        for (int i = 1; i < strings.size(); i++) {
            expression = new Rb.Binary(
                    randomId(),
                    expression.getPrefix(),
                    Markers.EMPTY,
                    expression.withPrefix(EMPTY),
                    JLeftPadded.build(Rb.Binary.Type.ImplicitStringConcatenation)
                            .withBefore(strings.get(i - 1).getAfter()),
                    (Expression) strings.get(i).getElement(),
                    null
            );
        }
        return expression;
    }

    /**
     * @param node           The string node to convert.
     * @param delimiter      A delimiter for this string. The cursor has already advanced beyond this delimiter.
     * @param inArrayLiteral If we are in an array literal, there may be no delimiter use.
     * @return Either a {@link J.Literal} or a {@link Rb.Binary} with an implicit concatenation type.
     */
    private Expression convertStringLiteral(StrNode node, String delimiter, boolean inArrayLiteral) {
        String value = node.getValue().toString();
        String endDelimiter = StringUtils.endDelimiter(delimiter);

        if (delimiter.equals("?")) {
            return new J.Literal(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    value,
                    "?" + value,
                    null,
                    JavaType.Primitive.String
            );
        } else if (inArrayLiteral) {
            return new J.Literal(
                    randomId(),
                    sourceBefore(value),
                    Markers.EMPTY,
                    value,
                    value,
                    null,
                    JavaType.Primitive.String
            );
        } else if (endDelimiter.isEmpty()) {
            String escapedValue = value;
            if (nodes.getNearestMessage("delimiter", delimiter).equals("\"")) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < value.toCharArray().length; i++) {
                    sb.append(readMaybeEscaped("\""));
                }
                escapedValue = sb.toString();
            }
            skip(escapedValue);
            return new J.Literal(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    value,
                    escapedValue,
                    null,
                    JavaType.Primitive.String
            );
        } else if (node.getValue().isEmpty()) {
            return new J.Literal(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    value,
                    String.format("%s%s%s", delimiter, value, endDelimiter),
                    null,
                    JavaType.Primitive.String
            );
        }

        List<String[]> strings = implicitConcatenations(delimiter, value);

        // deal with the possibility of implicit concatenations
        Expression combined = null;
        Space beforeOperator = null;
        int i = 0;
        for (String[] v : strings) {
            J.Literal literal = new J.Literal(
                    randomId(),
                    EMPTY,
                    Markers.EMPTY,
                    v[1],
                    String.format("%s%s%s", delimiter, v[0], endDelimiter),
                    null,
                    JavaType.Primitive.String
            );
            skip(v[0]);
            skip(endDelimiter);
            if (combined == null) {
                combined = literal;
            } else {
                combined = new Rb.Binary(
                        randomId(),
                        combined.getPrefix(),
                        Markers.EMPTY,
                        combined.withPrefix(EMPTY),
                        JLeftPadded.build(Rb.Binary.Type.ImplicitStringConcatenation)
                                .withBefore(beforeOperator),
                        literal,
                        null
                );
            }
            if (i++ < strings.size()) {
                beforeOperator = sourceBefore(delimiter);
            } else {
                break;
            }
        }

        assert combined != null : String.format("unable to create a string literal for |%s| on line %d",
                value, node.getLine() + 1);
        return requireNonNull(combined);
    }

    /**
     * @param delimiter The delimiter of the string. Only double quote delimited strings are escapable.
     * @param value     The value of the string, which will not contain escapes.
     * @return The set of ESCAPED string values to their unescaped forms.
     */
    public List<String[]> implicitConcatenations(String delimiter, String value) {
        // most of the time there is only one string
        List<String[]> strings = new ArrayList<>(1);

        int cursorBefore = cursor;
        char endDelimiterChar = StringUtils.endDelimiter(delimiter).charAt(0);
        StringBuilder valueSrc = new StringBuilder();
        int start = 0;
        for (int i = 0; i < value.length(); ) {
            char c = source.charAt(cursor);
            char last = source.charAt(cursor - 1);
            char nextLast = cursor >= 2 ? source.charAt(cursor - 2) : '\0';
            if (c == endDelimiterChar && (last != '\\' || nextLast == '\\')) {
                strings.add(new String[] { valueSrc.toString(), value.substring(start, i) });
                valueSrc.setLength(0);
                cursor++;
                sourceBefore(delimiter);
                start = i;
            } else {
                valueSrc.append(readMaybeEscaped(delimiter));
                i++;
            }
        }
        cursor = cursorBefore;

        strings.add(new String[] { valueSrc.toString(), value.substring(start) });
        return strings;
    }

    private String readMaybeEscaped(String delimiter) {
        StringBuilder sb = new StringBuilder();
        char c = source.charAt(cursor);
        if (delimiter.equals("\"") && c == '\\') {
            cursor++;
            char next = source.charAt(cursor);
            switch (next) {
                case '\\':
                case 'a':
                case 'b':
                case 'f':
                case 'e':
                case 's':
                case 'n':
                case 'r':
                case 't':
                case 'v':
                case '"':
                    sb.append("\\");
                    sb.append(next);
                    break;
                case '0':
                case 'x':
                    // differentiate between single, double, and triple digit octals
                    int j;
                    for (j = 1; j <= 2; j++) {
                        if (!Character.isDigit(source.charAt(cursor + j))) {
                            break;
                        }
                    }
                    sb.append("\\");
                    sb.append(source, cursor, cursor + j);
                    cursor += j;
                    break;
                case 'u':
                    sb.append("\\u");
                    cursor++;
                    if (source.charAt(cursor) == '{') {
                        sb.append(source, cursor, cursor + 6);
                    } else {
                        sb.append(source, cursor, cursor + 4);
                    }
                    break;
                case 'c':
                    sb.append(source, cursor - 1, cursor + 2);
                    cursor++;
                    break;
                case 'C':
                case 'M':
                    sb.append("\\");
                    sb.append(next);
                    cursor++;
                    if (source.charAt(cursor) == '-') {
                        sb.append(source, cursor, cursor + 2);
                        cursor++;
                    } else {
                        sb.append(c);
                    }
                    break;
                default:
                    sb.append(next);
            }
        } else {
            sb.append(c);
        }
        cursor++;
        return sb.toString();
    }

    @Override
    public J visitSuperNode(SuperNode node) {
        return new J.MethodInvocation(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                null,
                null,
                convertIdentifier("super"),
                convertCallArgs(new SuperArgsNode(node)),
                null
        );
    }

    /**
     * Because {@link SuperNode} does not implement {@link IArgumentNode}.
     */
    private static class SuperArgsNode extends SuperNode implements IArgumentNode {
        public SuperArgsNode(SuperNode superNode) {
            super(superNode.getLine(), superNode.getArgsNode(), superNode.getIterNode());
        }

        @Override
        public Node setArgsNode(Node node) {
            throw new UnsupportedOperationException("Setter will never be called");
        }
    }

    @Override
    public J visitSValueNode(SValueNode node) {
        // https://www.rubydoc.info/gems/ruby-internal/Node/SVALUE
        throw new UnsupportedOperationException("Does not appear to be called even for SVALUEs as described " +
                                                "in the Ruby documentation.");
    }

    @Override
    public J visitSymbolNode(SymbolNode node) {
        return convertSymbols(node);
    }

    /**
     * @param nodes An array of either {@link SymbolNode} or {@link DSymbolNode}.
     * @return A {@link Rb.Symbol} or a {@link Rb.DelimitedArray} node.
     */
    public Expression convertSymbols(Node... nodes) {
        Space prefix = whitespace();

        String delimiter;
        boolean explicitColon = false;
        if (source.startsWith("%", cursor)) { // %s or %i
            delimiter = source.substring(cursor, cursor + 3);
        } else {
            if (source.startsWith(":", cursor)) {
                explicitColon = true;
                skip(":");
            }
            if (nodes[0] instanceof SymbolNode) {
                RubySymbol firstName = ((SymbolNode) nodes[0]).getName();
                delimiter = source.startsWith(firstName.asJavaString(), cursor) ?
                        "" : source.substring(cursor, cursor + 1);
            } else if (!(nodes[0].childNodes().get(0) instanceof StrNode)) {
                delimiter = source.substring(cursor, cursor + 1);
            } else {
                delimiter = "";
            }
        }
        skip(delimiter);

        List<JRightPadded<Expression>> nameNodes = new ArrayList<>(nodes.length);
        if (delimiter.startsWith("%i") || delimiter.startsWith("%I")) {
            // whitespace is only trimmed around symbol array names
            for (Node node : nodes) {
                nameNodes.add(padRight(
                        node instanceof SymbolNode ?
                                convertIdentifier(((SymbolNode) node).getName()) :
                                convertExpression(((DSymbolNode) node).children()[0]), whitespace()));
            }
        } else if (nodes[0] instanceof SymbolNode) {
            // whitespace on the end of non-array name symbols is actually part of the name...
            nameNodes.add(padRight(convertIdentifier(((SymbolNode) nodes[0]).getName()), EMPTY));
        } else { // instanceof DSymbolNode
            nameNodes.add(padRight(convertExpression((((DSymbolNode) nodes[0]).children()[0])), whitespace()));
        }

        if (delimiter.startsWith("%")) {
            skip(StringUtils.endDelimiter(delimiter));
        }

        if (delimiter.startsWith("%i") || delimiter.startsWith("%I")) {
            return new Rb.DelimitedArray(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    delimiter,
                    JContainer.build(EMPTY, nameNodes, Markers.EMPTY),
                    null
            );
        } else {
            return new Rb.Symbol(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    (explicitColon ? ":" : "") + delimiter,
                    nameNodes.get(0).getElement(),
                    null
            );
        }
    }

    @Override
    public J visitTrueNode(TrueNode node) {
        return new J.Literal(randomId(), sourceBefore("true"), Markers.EMPTY, true, "true",
                null, JavaType.Primitive.Boolean);
    }

    @Override
    public J visitVAliasNode(VAliasNode node) {
        return new Rb.Alias(
                randomId(),
                sourceBefore("alias"),
                Markers.EMPTY,
                convertIdentifier(node.getNewName()),
                convertIdentifier(node.getOldName())
        );
    }

    @Override
    public J visitVCallNode(VCallNode node) {
        return convertIdentifier(node.getName());
    }

    @Override
    public J visitWhenNode(WhenNode node) {
        throw new UnsupportedOperationException("Multiple WhenNodes can potentially map to one J.Case, " +
                                                "so the grouping of WhenNodes is handled in visitCaseNode " +
                                                "and this method should never be called.");
    }

    @Override
    public J visitWhileNode(WhileNode node) {
        return whileOrUntilNode(node.getConditionNode(), node.getBodyNode());
    }

    @Override
    public J visitUntilNode(UntilNode node) {
        return whileOrUntilNode(node.getConditionNode(), node.getBodyNode());
    }

    private J whileOrUntilNode(Node conditionNode, Node bodyNode) {
        Space prefix = whitespace();

        if (source.startsWith("while", cursor) || source.startsWith("until", cursor)) {
            Markers markers = whileOrUntil();
            Space conditionPrefix = whitespace();
            Expression conditionExpr = convertExpression(conditionNode);
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
                    padRight(convertStatement(bodyNode), sourceBefore("end"))
            );
        } else {
            JRightPadded<Statement> body = padRight(convertStatement(bodyNode), whitespace());
            Markers markers = whileOrUntil();

            Space conditionPrefix = whitespace();
            Expression conditionExpr = convertExpression(conditionNode);
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
    public J visitXStrNode(XStrNode node) {
        Space prefix = whitespace();
        String value = node.getValue().toString();
        String delimiter = source.charAt(cursor) == '`' ? "`" : source.substring(cursor, cursor + 3);
        skip(delimiter);
        skip(value);
        skip(StringUtils.endDelimiter(delimiter));
        return new Rb.ComplexString(
                randomId(),
                prefix,
                Markers.EMPTY,
                delimiter,
                JContainer.build(singletonList(
                        padRight(new J.Literal(
                                randomId(),
                                EMPTY,
                                Markers.EMPTY,
                                value,
                                value,
                                null,
                                JavaType.Primitive.String
                        ), EMPTY)
                )),
                emptyList()
        );
    }

    @Override
    public J visitYieldNode(YieldNode node) {
        return new Rb.Yield(
                randomId(),
                sourceBefore("yield"),
                Markers.EMPTY,
                convertArgs("(", node.getArgsNode(), null, ")")
        );
    }

    @Override
    public J visitZArrayNode(ZArrayNode node) {
        return new Rb.Array(
                randomId(),
                sourceBefore("["),
                Markers.EMPTY,
                JContainer.<Expression>empty().withBefore(sourceBefore("]")),
                null
        );
    }

    @Override
    public J visitZSuperNode(ZSuperNode node) {
        return new J.MethodInvocation(
                randomId(),
                whitespace(),
                Markers.EMPTY,
                null,
                null,
                convertIdentifier("super"),
                JContainer.<Expression>empty().withMarkers(Markers.EMPTY.add(new OmitParentheses(randomId()))),
                null
        );
    }

    private J.Identifier convertIdentifier(RubySymbol name) {
        return convertIdentifier(name.asJavaString());
    }

    private J.Identifier convertIdentifier(String name) {
        return new J.Identifier(
                randomId(),
                sourceBefore(name),
                Markers.EMPTY,
                emptyList(),
                name,
                null,
                null
        );
    }

    private Statement convertStatement(@Nullable Node t) {
        if (t == null) {
            // e.g. the empty body of `if cond then end`; the surrounding whitespace lives in the
            // padding around this element, so it must be a real element rather than null.
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        J j = convert(t);
        if (!(j instanceof Statement)) {
            j = new Rb.ExpressionStatement(randomId(), (Expression) j);
        }
        return (Statement) j;
    }

    private Expression convertExpression(@Nullable Node t) {
        if (t == null) {
            return new J.Empty(randomId(), EMPTY, Markers.EMPTY);
        }
        J j = convert(t);
        if (!(j instanceof Expression)) {
            return new Rb.StatementExpression(randomId(), (Statement) j);
        }
        return (Expression) j;
    }

    private TypeTree convertTypeTree(@Nullable Node t) {
        return asTypeTree(convert(t));
    }

    private TypeTree asTypeTree(J j) {
        if (j instanceof TypeTree) {
            return (TypeTree) j;
        }
        return new Rb.ExpressionTypeTree(
                randomId(),
                j.getPrefix(),
                Markers.EMPTY,
                j.withPrefix(EMPTY)
        );
    }

    private J convert(@Nullable Node t) {
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        nodes = new Cursor(nodes, t);
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            J j = maybeParenthesized(t);
            nodes = nodes.getParentOrThrow();
            return j;
        } catch (Throwable ex) {
            throw ex; // nice debug breakpoint
        }
    }

    private <J2 extends J> J2 maybeParenthesized(Node t) {
        for (OpenParenthesis paren : openParentheses) {
            paren.addLeft(t);
        }
        J2 j = maybeOpenParentheses(t);
        openParensCloseLeft();
        j = maybeCloseParentheses(t, j);
        return j;
    }

    private void openParensCloseLeft() {
        for (OpenParenthesis paren : openParentheses) {
            paren.closeLeft();
        }
    }

    @SuppressWarnings("unchecked")
    private <J2 extends J> J2 maybeOpenParentheses(Node t) {
        return peekWhitespace(0, (n, prefix) -> {
            if (skip("(")) {
                openParentheses.add(new OpenParenthesis(t, prefix));
                return (J2) maybeOpenParentheses(t);
            }
            return null;
        }).orElseGet(() -> (J2) t.accept(this));
    }

    private <J2 extends J> J2 maybeCloseParentheses(Node t, J2 j) {
        return peekWhitespace(j, (j2, prefix) -> {
            if (skip(")")) {
                for (OpenParenthesis paren : new ArrayList<>(openParentheses)) {
                    if (paren.isParenthesized(t)) {
                        openParentheses.remove(paren);
                        //noinspection unchecked
                        return maybeCloseParentheses(t, (J2) new J.Parentheses<>(
                                randomId(),
                                requireNonNull(paren.getBefore()),
                                Markers.EMPTY,
                                padRight(j2, prefix)
                        ));
                    }
                }
            }
            return null;
        }).orElse(j);
    }

    private <J2 extends J> JContainer<J2> convertArgs(String before,
                                                      @Nullable Node argsNode,
                                                      @Nullable Node iterNode,
                                                      String after) {
        AtomicReference<Markers> markers = new AtomicReference<>(Markers.EMPTY);
        AtomicBoolean omitParentheses = new AtomicBoolean();

        return peekWhitespace(collectArgsToList(argsNode), (args, prefix) -> {
            if (source.startsWith(before, cursor)) {
                skip(before);
                omitParentheses.set(false);
            } else {
                markers.set(markers.get().add(new OmitParentheses(randomId())));
                omitParentheses.set(true);
            }

            List<JRightPadded<J2>> mappedArgs = convertAll(args, n -> sourceBefore(","),
                    n -> maybeTrailingComma(markers, omitParentheses.get() ? null : after));

            if (iterNode != null) {
                //noinspection unchecked
                J2 blockPass = (J2) convert(iterNode);
                Space suffix = EMPTY;
                if (blockPass instanceof Rb.BlockArgument) {
                    suffix = omitParentheses.get() ? EMPTY : sourceBefore(after);
                }
                mappedArgs = ListUtils.concat(
                        mappedArgs,
                        padRight(blockPass, suffix)
                );
            }

            if (mappedArgs.isEmpty()) {
                if (omitParentheses.get()) {
                    return null;
                } else {
                    //noinspection unchecked
                    mappedArgs = singletonList(padRight((J2) new J.Empty(randomId(), EMPTY, Markers.EMPTY),
                            sourceBefore(after)));
                }
            }

            return JContainer.build(
                    prefix,
                    mappedArgs,
                    markers.get()
            );
        }).orElseGet(() -> JContainer.<J2>empty().withMarkers(markers.get()));
    }

    private static List<Node> collectArgsToList(@Nullable Node argsNode) {
        List<Node> args;
        if (argsNode == null) {
            args = new ArrayList<>(1);
        } else if (argsNode instanceof ListNode) {
            ListNode listNode = (ListNode) argsNode;
            args = new ArrayList<>(listNode.size());
            for (Node node : listNode.children()) {
                if (node != null) {
                    args.add(node);
                }
            }
        } else if (argsNode instanceof ArgsNode) {
            ArgsNode argsArgsNode = (ArgsNode) argsNode;
            args = new ArrayList<>(argsArgsNode.getArgs().length + 1);
            Collections.addAll(args, argsArgsNode.getArgs());
            if (argsArgsNode.getBlock() != null) {
                args.add(argsArgsNode.getBlock());
            }
            if (argsArgsNode.getRestArgNode() != null) {
                args.add(argsArgsNode.getRestArgNode());
            }
            if (argsArgsNode.getKeyRest() != null) {
                args.add(argsArgsNode.getKeyRest());
            }
        } else {
            args = new ArrayList<>(2);
            args.add(argsNode);
        }
        return args;
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

    private <J2 extends J> List<JRightPadded<J2>> convertAll(List<? extends Node> trees,
                                                             Function<Node, Space> innerSuffix,
                                                             Function<Node, Space> suffix) {
        if (trees.isEmpty()) {
            return emptyList();
        }
        List<JRightPadded<J2>> converted = new ArrayList<>(trees.size());
        for (int i = 0; i < trees.size(); i++) {
            @SuppressWarnings("unchecked") J2 j = (J2) convert(trees.get(i));
            converted.add(padRight(j, (i == trees.size() - 1 ? suffix : innerSuffix).apply(trees.get(i))));
        }
        return converted;
    }

    private <T> JRightPadded<T> padRight(T tree, Space right) {
        return new JRightPadded<>(tree, right, Markers.EMPTY);
    }

    private <T> JLeftPadded<T> padLeft(Space left, T tree) {
        return new JLeftPadded<>(left, tree, Markers.EMPTY);
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
        int cursorBeforeWhitespace = cursor;
        Queue<Rb.Heredoc> openHeredocsBeforeWhitespace = new ArrayDeque<>(openHeredocs);
        U converted = conditional.apply(t, whitespace());
        if (converted != null) {
            return Optional.of(converted);
        }
        openHeredocs = openHeredocsBeforeWhitespace;
        cursor = cursorBeforeWhitespace;
        return Optional.empty();
    }

    private Space whitespace() {
        String prefix = "";
        Set<Rb.Heredoc> heredocsSplitByThis = new HashSet<>(openHeredocs.size());
        do {
            int next = indexOfNextNonWhitespace();
            //noinspection StringConcatenationInLoop
            prefix += source.substring(cursor, next);
            cursor += prefix.length();
            if (!openHeredocs.isEmpty() && prefix.contains("\n")) {
                for (int i = cursor; i > 0; i--) {
                    if (source.charAt(i - 1) == '\n') {
                        prefix = source.substring(cursor - prefix.length(), i);
                        cursor = i;
                        break;
                    }
                }

                Rb.Heredoc heredoc = openHeredocs.poll();
                String delim = heredocDelimiters.get(heredoc);
                String endDelim = delim.substring(3);
                int i = cursor;

                findEndDelim:
                for (; i < source.length(); i++) {
                    if (source.startsWith(endDelim, i)) {
                        for (int j = i - 1; j > 0; j--) {
                            char c = source.charAt(j);
                            if (c == '\n') {
                                break findEndDelim;
                            } else if (c != ' ' && c != '\t') {
                                continue findEndDelim;
                            }
                        }
                    }
                }

                String value;
                if (delim.charAt(2) == '~') {
                    value = source.substring(cursor - prefix.length(), i);
                    value = org.openrewrite.internal.StringUtils.trimIndentPreserveCRLF(value);
                } else {
                    value = source.substring(cursor, i);
                }
                finalizedHeredocs.put(heredoc,
                        heredoc.withValue(
                                new J.Literal(
                                        randomId(),
                                        EMPTY,
                                        Markers.EMPTY,
                                        value,
                                        delim + source.substring(cursor - prefix.length(), i) + endDelim,
                                        null,
                                        JavaType.Primitive.String
                                )
                        )
                );
                heredocsSplitByThis.add(heredoc);
                cursor = i + endDelim.length();
                Space heredocEnd = whitespace(); // note the potential here for recursion
                finalizedHeredocs.put(heredoc, finalizedHeredocs.get(heredoc).withEnd(heredocEnd));
            }
        } while (!openHeredocs.isEmpty() && prefix.contains("\n"));

        Space space = RubySpace.format(prefix);
        for (Rb.Heredoc h : heredocsSplitByThis) {
            finalizedHeredocs.put(h, finalizedHeredocs.get(h).withAroundValue(space));
        }
        return space;
    }

    private boolean skip(@Nullable String token) {
        if (token == null) {
            //noinspection ConstantConditions
            return false;
        }
        if (source.startsWith(token, cursor)) {
            cursor += token.length();
            return true;
        }
        return false;
    }

    private int indexOfNextNonWhitespace() {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int length = source.length();
        int i = cursor;
        for (; i < length; i++) {
            char current = source.charAt(i);
            if (inSingleLineComment) {
                inSingleLineComment = current != '\n';
                continue;
            } else if (length > i + 1) {
                if (current == '#') {
                    inSingleLineComment = true;
                    i++;
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

                // we consider line continuations to be whitespace in Ruby
                if (current != '\\' || !(next == '\n' || next == '\r')) {
                    break; // found it!
                }
            }
        }
        return i;
    }
}
