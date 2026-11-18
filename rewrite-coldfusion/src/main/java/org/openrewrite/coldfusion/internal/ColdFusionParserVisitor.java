/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.coldfusion.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.FileAttributes;
import org.openrewrite.coldfusion.ColdFusionParser.ColdFusionParsingException;
import org.openrewrite.coldfusion.internal.grammar.CFMLLexer;
import org.openrewrite.coldfusion.internal.grammar.CFMLParserBaseVisitor;
import org.openrewrite.coldfusion.marker.*;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.TagComment;
import org.openrewrite.java.marker.OmitBraces;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.marker.Semicolon;
import org.openrewrite.java.marker.TrailingComma;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.coldfusion.internal.grammar.CFMLParser.*;

/**
 * Maps the CFML parse tree to the {@link Cf} LST. A cursor walks the source behind the tokens, so the
 * whitespace and comments before each token become the prefix of the element it starts.
 */
public class ColdFusionParserVisitor extends CFMLParserBaseVisitor<J> {
    private final Path path;
    private final String source;
    private final @Nullable FileAttributes fileAttributes;
    private final Charset charset;
    private final boolean charsetBomMarked;

    /**
     * Token indexes count code points. This maps one to a string index, when the two differ.
     */
    private final int @Nullable [] charIndex;

    /**
     * Where each {@code <!--- --->} comment ends, by where it starts. Only the lexer knows: a comment
     * that opens one more comment than it closes treats the extra opening as text.
     */
    private final Map<Integer, Integer> tagCommentEnds = new HashMap<>();

    private final LanguageAttributes languageAttributes = new LanguageAttributes();

    private int cursor;

    /**
     * Whether a {@code <cfcomponent>} or {@code <cfinterface>} has already declared the file's class.
     */
    private boolean declaredClass;

    public ColdFusionParserVisitor(Path path, @Nullable FileAttributes fileAttributes, String source,
                                   Charset charset, boolean charsetBomMarked, List<? extends Token> tokens) {
        this.path = path;
        this.fileAttributes = fileAttributes;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
        int codePoints = source.codePointCount(0, source.length());
        if (codePoints == source.length()) {
            charIndex = null;
        } else {
            charIndex = new int[codePoints + 1];
            for (int c = 0, i = 0; c < codePoints; c++) {
                charIndex[c] = i;
                i += Character.charCount(source.codePointAt(i));
            }
            charIndex[codePoints] = source.length();
        }
        for (Token token : tokens) {
            int start = start(token);
            int end = charIndex(token.getStopIndex() + 1);
            if (end - start >= 9 && source.startsWith("<!---", start) && source.startsWith("--->", end - 4)) {
                tagCommentEnds.put(start, end);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Files
    // ------------------------------------------------------------------------------------------

    @Override
    public Cf.CompilationUnit visitScriptComponent(ScriptComponentContext ctx) {
        List<JRightPadded<Statement>> statements = paddedStatements(ctx.paddedStatement());
        Space eof = space(cursor, source.length());
        cursor = source.length();
        return compilationUnit(statements, eof);
    }

    /**
     * Whitespace and {@code <!--- --->} comments are not text: the cursor stays before them until what
     * follows takes them as its prefix, the end of a body as the space before its end tag, or the end
     * of the file as {@link Cf.CompilationUnit#getEof()}.
     */
    @Override
    public Cf.CompilationUnit visitTemplate(TemplateContext ctx) {
        Deque<Frame> stack = new ArrayDeque<>();
        Frame root = new Frame(null);
        stack.push(root);
        int textStart = -1;
        int textEnd = -1;
        for (TemplateElementContext element : ctx.templateElement()) {
            if (element instanceof TextElementContext) {
                Token text = element.getStart();
                for (int i = start(text), end = charIndex(text.getStopIndex() + 1); i < end; i++) {
                    if (!isTemplateWhitespace(source.charAt(i))) {
                        textStart = textStart < 0 ? i : textStart;
                        textEnd = i + 1;
                    }
                }
                continue;
            }
            if (textStart >= 0) {
                stack.peek().children.add(text(textStart, textEnd));
                textStart = -1;
            }
            if (element instanceof InterpolationElementContext) {
                stack.peek().children.add(interpolation(((InterpolationElementContext) element).interpolation()));
            } else if (element instanceof StartTagElementContext) {
                startTag(stack, ((StartTagElementContext) element).startTag());
            } else if (element instanceof CfscriptElementContext) {
                stack.peek().children.add(cfscript(((CfscriptElementContext) element).cfscriptTag()));
            } else if (element instanceof EndTagElementContext) {
                endTag(stack, ((EndTagElementContext) element).endTag());
            }
        }
        if (textStart >= 0) {
            stack.peek().children.add(text(textStart, textEnd));
        }
        Space eof = space(cursor, source.length());
        cursor = source.length();
        while (stack.size() > 1) {
            flatten(stack);
        }
        return compilationUnit(padded(root.children), eof);
    }

    private Cf.Text text(int start, int end) {
        Space prefix = space(cursor, start);
        cursor = end;
        return new Cf.Text(randomId(), prefix, Markers.EMPTY, source.substring(start, end));
    }

    /**
     * Whitespace between tags. A no-break space is text, since a page shows it.
     */
    private static boolean isTemplateWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B';
    }

    private Cf.CompilationUnit compilationUnit(List<JRightPadded<Statement>> statements, Space eof) {
        return new Cf.CompilationUnit(randomId(), Space.EMPTY, Markers.EMPTY, path, fileAttributes, charset.name(),
                charsetBomMarked, null, statements, eof, null);
    }

    // ------------------------------------------------------------------------------------------
    // Template
    // ------------------------------------------------------------------------------------------

    private static class Frame {
        final Cf.@Nullable Tag open;
        final List<Statement> children = new ArrayList<>();

        Frame(Cf.@Nullable Tag open) {
            this.open = open;
        }
    }

    private void startTag(Deque<Frame> stack, StartTagContext ctx) {
        Token open = ctx.getStart();
        Space prefix = sourceBefore(open);
        String name = open.getText().substring(1);
        String lower = name.toLowerCase(Locale.ROOT);
        List<Cf.TagAttribute> attributes = new ArrayList<>();
        J expression = null;
        if (ctx.expression() != null) {
            VarPrimaryContext var = "cfset".equals(lower) ? bareVar(ctx.expression()) : null;
            expression = var != null ? varStatement(var) : expression(ctx.expression());
        }
        for (TagAttributeContext attribute : ctx.tagAttribute()) {
            attributes.add(tagAttribute(attribute));
        }
        Token close = ctx.getStop();
        Space beforeEnd = sourceBefore(close);
        boolean selfClosing = close.getType() == TAG_SELF_CLOSE;
        Cf.Tag tag = new Cf.Tag(randomId(), prefix, Markers.EMPTY, name, attributes, expression, beforeEnd,
                selfClosing, null, null);
        if (selfClosing || CFMLLexer.BODYLESS_TAGS.contains(lower)) {
            stack.peek().children.add(selfClosing && "cffunction".equals(lower) ? tagFunction(tag) : tag);
        } else {
            stack.push(new Frame(tag));
        }
    }

    private Cf.Tag cfscript(CfscriptTagContext ctx) {
        Token open = ctx.getStart();
        Space prefix = sourceBefore(open);
        List<Cf.TagAttribute> attributes = new ArrayList<>();
        for (TagAttributeContext attribute : ctx.tagAttribute()) {
            attributes.add(tagAttribute(attribute));
        }
        if (ctx.TAG_SELF_CLOSE() != null) {
            Space beforeEnd = sourceBefore(ctx.TAG_SELF_CLOSE().getSymbol());
            return new Cf.Tag(randomId(), prefix, Markers.EMPTY, open.getText().substring(1), attributes, null,
                    beforeEnd, true, null, null);
        }
        Space beforeEnd = sourceBefore(ctx.TAG_CLOSE().getSymbol());
        List<JRightPadded<Statement>> statements = paddedStatements(ctx.paddedStatement());
        Space end = sourceBefore(ctx.CFSCRIPT_END().getSymbol());
        Cf.Tag.Closing closing = new Cf.Tag.Closing(ctx.CFSCRIPT_END().getText().substring(2),
                sourceBefore(ctx.END_TAG_CLOSE().getSymbol()));
        J.Block body = new J.Block(randomId(), Space.EMPTY, Markers.build(singletonList(new OmitBraces(randomId()))),
                new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), statements, end);
        return new Cf.Tag(randomId(), prefix, Markers.EMPTY, open.getText().substring(1), attributes, null,
                beforeEnd, false, body, closing);
    }

    private void endTag(Deque<Frame> stack, EndTagContext ctx) {
        Token open = ctx.getStart();
        Space end = sourceBefore(open);
        String name = open.getText().substring(2);
        Cf.Tag.Closing closing = new Cf.Tag.Closing(name, sourceBefore(ctx.END_TAG_CLOSE().getSymbol()));

        Frame target = null;
        for (Frame frame : stack) {
            if (frame.open != null && frame.open.getName().equalsIgnoreCase(name)) {
                target = frame;
                break;
            }
        }
        if (target != null) {
            while (stack.peek() != target) {
                flatten(stack);
            }
            Frame frame = stack.pop();
            Cf.Tag tag = Objects.requireNonNull(frame.open).withBody(templateBody(frame.children, end)).withClosing(closing);
            stack.peek().children.add(declaration(tag, stack.peek().open == null));
            return;
        }

        // a tag that takes no body closed anyway, as in <cfthrow ...></cfthrow>
        List<Statement> children = stack.peek().children;
        for (int i = children.size() - 1; i >= 0; i--) {
            Statement child = children.get(i);
            if (child instanceof Cf.Tag) {
                Cf.Tag tag = (Cf.Tag) child;
                if (tag.getBody() == null && !tag.isSelfClosing() && tag.getName().equalsIgnoreCase(name)) {
                    List<Statement> body = new ArrayList<>(children.subList(i + 1, children.size()));
                    children.subList(i + 1, children.size()).clear();
                    children.set(i, tag.withBody(templateBody(body, end)).withClosing(closing));
                    return;
                }
            }
        }
        throw error("an end tag with no start tag", open);
    }

    /**
     * A tag left open at the end of its parent, or at the end of the file, takes no body: what
     * followed it belongs to its parent.
     */
    private static void flatten(Deque<Frame> stack) {
        Frame frame = stack.pop();
        List<Statement> parent = stack.peek().children;
        parent.add(Objects.requireNonNull(frame.open));
        parent.addAll(frame.children);
    }

    private static J.Block templateBody(List<Statement> children, Space end) {
        return new J.Block(randomId(), Space.EMPTY, Markers.build(singletonList(new OmitBraces(randomId()))),
                new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), padded(children), end);
    }

    private static List<JRightPadded<Statement>> padded(List<Statement> statements) {
        List<JRightPadded<Statement>> padded = new ArrayList<>(statements.size());
        for (Statement statement : statements) {
            padded.add(new JRightPadded<>(statement, Space.EMPTY, Markers.EMPTY));
        }
        return padded;
    }

    private Cf.TagAttribute tagAttribute(TagAttributeContext ctx) {
        Token name = ctx.ATTRIBUTE_NAME().getSymbol();
        Space prefix = sourceBefore(name);
        JLeftPadded<Expression> value = null;
        if (ctx.TAG_EQUALS() != null) {
            Space beforeEquals = sourceBefore(ctx.TAG_EQUALS().getSymbol());
            value = new JLeftPadded<>(beforeEquals, attributeValue(ctx.attributeValue()), Markers.EMPTY);
        }
        return new Cf.TagAttribute(randomId(), prefix, Markers.EMPTY, identifier(Space.EMPTY, name.getText()), value);
    }

    /**
     * A quoted string, or an unquoted run up to whitespace or the end of the tag, either of which
     * may hold {@code #interpolations#}.
     */
    private Expression attributeValue(AttributeValueContext ctx) {
        if (ctx.stringLiteral() != null) {
            return string(ctx.stringLiteral());
        }
        Space prefix = prefix(ctx.getStart());
        List<J> parts = new ArrayList<>();
        StringBuilder raw = new StringBuilder();
        boolean interpolated = false;
        for (ParseTree child : ctx.children) {
            if (child instanceof TerminalNode) {
                Token text = ((TerminalNode) child).getSymbol();
                raw.append(text.getText());
                skip(text);
            } else {
                if (raw.length() > 0) {
                    parts.add(segment(raw.toString(), raw.toString().replace("##", "#")));
                    raw.setLength(0);
                }
                parts.add(interpolation((InterpolationContext) child));
                interpolated = true;
            }
        }
        if (!interpolated) {
            return new J.Literal(randomId(), prefix, Markers.EMPTY, raw.toString().replace("##", "#"), raw.toString(), null,
                    JavaType.Primitive.String);
        }
        if (raw.length() > 0) {
            parts.add(segment(raw.toString(), raw.toString().replace("##", "#")));
        }
        return new Cf.StringTemplate(randomId(), prefix, Markers.EMPTY, "", parts, JavaType.Primitive.String);
    }

    private Cf.Interpolation interpolation(InterpolationContext ctx) {
        Space prefix = sourceBefore(ctx.HASH().getSymbol());
        Expression expression = expression(ctx.expression());
        Space after = sourceBefore(ctx.HASH_CLOSE().getSymbol());
        return new Cf.Interpolation(randomId(), prefix, Markers.EMPTY, expression, after);
    }

    // ------------------------------------------------------------------------------------------
    // Statements
    // ------------------------------------------------------------------------------------------

    private List<JRightPadded<Statement>> paddedStatements(List<PaddedStatementContext> contexts) {
        List<JRightPadded<Statement>> statements = new ArrayList<>(contexts.size());
        for (PaddedStatementContext context : contexts) {
            statements.add(paddedStatement(context));
        }
        return statements;
    }

    private JRightPadded<Statement> paddedStatement(PaddedStatementContext ctx) {
        if (ctx.functionDeclaration() != null) {
            return functionDeclaration(ctx.functionDeclaration());
        }
        if (ctx.statement() == null) {
            Token semicolon = ctx.SEMI().getSymbol();
            Space prefix = sourceBefore(semicolon);
            return new JRightPadded<>(new J.Empty(randomId(), prefix, Markers.EMPTY), Space.EMPTY, semicolon());
        }
        Statement statement = statement(ctx.statement());
        if (ctx.SEMI() != null) {
            return new JRightPadded<>(statement, sourceBefore(ctx.SEMI().getSymbol()), semicolon());
        }
        return new JRightPadded<>(statement, Space.EMPTY, Markers.EMPTY);
    }

    private Statement statement(StatementContext ctx) {
        ParseTree statement = ctx.getChild(0);
        if (statement instanceof BlockContext) {
            return block((BlockContext) statement);
        } else if (statement instanceof IfStatementContext) {
            return ifStatement((IfStatementContext) statement);
        } else if (statement instanceof ForInStatementContext) {
            return forInStatement((ForInStatementContext) statement);
        } else if (statement instanceof ForStatementContext) {
            return forStatement((ForStatementContext) statement);
        } else if (statement instanceof WhileStatementContext) {
            WhileStatementContext loop = (WhileStatementContext) statement;
            Space prefix = prefix(loop.getStart());
            String keyword = keyword(loop.WHILE());
            return new J.WhileLoop(randomId(), prefix, keywordMarkers(keyword, "while"),
                    controlParentheses(loop.LPAREN(), loop.expression(), loop.RPAREN()), paddedStatement(loop.paddedStatement()));
        } else if (statement instanceof DoWhileStatementContext) {
            return doWhileStatement((DoWhileStatementContext) statement);
        } else if (statement instanceof SwitchStatementContext) {
            return switchStatement((SwitchStatementContext) statement);
        } else if (statement instanceof TryStatementContext) {
            return tryStatement((TryStatementContext) statement);
        } else if (statement instanceof ReturnStatementContext) {
            ReturnStatementContext ret = (ReturnStatementContext) statement;
            return returnStatement(ret.getStart(), ret.expression());
        } else if (statement instanceof BreakStatementContext) {
            BreakStatementContext brk = (BreakStatementContext) statement;
            Space prefix = prefix(brk.getStart());
            String keyword = keyword(brk.BREAK());
            return new J.Break(randomId(), prefix, keywordMarkers(keyword, "break"), label(brk.IDENTIFIER()));
        } else if (statement instanceof ContinueStatementContext) {
            ContinueStatementContext cont = (ContinueStatementContext) statement;
            Space prefix = prefix(cont.getStart());
            String keyword = keyword(cont.CONTINUE());
            return new J.Continue(randomId(), prefix, keywordMarkers(keyword, "continue"), label(cont.IDENTIFIER()));
        } else if (statement instanceof ThrowStatementContext) {
            ThrowStatementContext thr = (ThrowStatementContext) statement;
            Space prefix = prefix(thr.getStart());
            String keyword = keyword(thr.THROW());
            return new J.Throw(randomId(), prefix, keywordMarkers(keyword, "throw"), expression(thr.expression()));
        } else if (statement instanceof ComponentDeclarationContext) {
            return component((ComponentDeclarationContext) statement);
        } else if (statement instanceof PropertyDeclarationContext) {
            return property((PropertyDeclarationContext) statement);
        } else if (statement instanceof ImportStatementContext) {
            return importStatement((ImportStatementContext) statement);
        } else if (statement instanceof StaticInitializerContext) {
            StaticInitializerContext init = (StaticInitializerContext) statement;
            Space prefix = prefix(init.getStart());
            skip(init.STATIC().getSymbol());
            J.Block block = block(init.block());
            return block.getPadding().withStatic(new JRightPadded<>(true, block.getPrefix(), Markers.EMPTY)).withPrefix(prefix);
        } else if (statement instanceof ScriptTagContext) {
            return scriptTag((ScriptTagContext) statement);
        }
        ExpressionStatementContext expressionStatement = (ExpressionStatementContext) statement;
        if (expressionStatement.bodyBlock() != null) {
            // `cfloop(array=items, index="i") { }`, which read as a call and then a block would lose its body
            J.MethodInvocation call = (J.MethodInvocation) expression(expressionStatement.expression());
            return new Cf.TagCall(randomId(), call.getPrefix(), Markers.EMPTY, call.withPrefix(Space.EMPTY),
                    block(expressionStatement.bodyBlock()));
        }
        return statementOf(expressionStatement.expression());
    }

    /**
     * An expression written as a statement. {@code var x = 1} declares a variable, while {@code var
     * x.y = 1} assigns with the keyword alongside, as {@link #varAssignment} does in an expression.
     */
    private Statement statementOf(ExpressionContext ctx) {
        VarPrimaryContext var = bareVar(ctx);
        if (var != null) {
            return varStatement(var);
        }
        Expression expression = expression(ctx);
        return expression instanceof Statement ? (Statement) expression : new Cf.ExpressionStatement(randomId(), expression);
    }

    private Statement varStatement(VarPrimaryContext var) {
        Space prefix = prefix(var.getStart());
        String keyword = keyword(var.VAR());
        Expression declared = expression(var.expression());
        if (declared instanceof J.Identifier) {
            J.Identifier name = (J.Identifier) declared;
            return varDeclaration(prefix, keyword, name.getPrefix(), name.getSimpleName(), null);
        }
        if (declared instanceof J.Assignment && ((J.Assignment) declared).getVariable() instanceof J.Identifier) {
            J.Assignment assignment = (J.Assignment) declared;
            return varDeclaration(prefix, keyword, assignment.getPrefix(),
                    ((J.Identifier) assignment.getVariable()).getSimpleName(),
                    new JLeftPadded<>(assignment.getPadding().getAssignment().getBefore(), assignment.getAssignment(), Markers.EMPTY));
        }
        return (Statement) varAssignment(prefix, keyword, declared, var);
    }

    private static @Nullable VarPrimaryContext bareVar(ParserRuleContext ctx) {
        ParseTree node = ctx;
        while (!(node instanceof PrimaryContext)) {
            if (node instanceof PostfixExpressionContext) {
                PostfixExpressionContext postfix = (PostfixExpressionContext) node;
                if (!postfix.postfixOperator().isEmpty()) {
                    return null;
                }
                node = postfix.primary();
            } else if (node instanceof ParserRuleContext && node.getChildCount() == 1) {
                node = node.getChild(0);
            } else {
                return null;
            }
        }
        return node instanceof VarPrimaryContext ? (VarPrimaryContext) node : null;
    }

    private J.VariableDeclarations varDeclaration(Space prefix, String keyword, Space namePrefix, String name,
                                                  @Nullable JLeftPadded<Expression> initializer) {
        return new J.VariableDeclarations(randomId(), prefix, Markers.EMPTY, emptyList(),
                singletonList(modifier(Space.EMPTY, keyword, J.Modifier.Type.LanguageExtension)), null, null,
                singletonList(new JRightPadded<>(namedVariable(namePrefix, name, initializer), Space.EMPTY, Markers.EMPTY)));
    }

    private J.Block block(ParserRuleContext ctx) {
        List<PaddedStatementContext> statements;
        if (ctx instanceof BlockContext) {
            statements = ((BlockContext) ctx).paddedStatement();
        } else {
            statements = ((BodyBlockContext) ctx).paddedStatement();
        }
        Space prefix = sourceBefore(ctx.getStart());
        List<JRightPadded<Statement>> padded = paddedStatements(statements);
        Space end = sourceBefore(ctx.getStop());
        return new J.Block(randomId(), prefix, Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY), padded, end);
    }

    private J.@Nullable Identifier label(@Nullable TerminalNode label) {
        return label == null ? null : identifier(sourceBefore(label.getSymbol()), label.getText());
    }

    private J.ControlParentheses<Expression> controlParentheses(TerminalNode open, ExpressionContext expression, TerminalNode close) {
        Space prefix = sourceBefore(open.getSymbol());
        Expression condition = expression(expression);
        Space after = sourceBefore(close.getSymbol());
        return new J.ControlParentheses<>(randomId(), prefix, Markers.EMPTY, new JRightPadded<>(condition, after, Markers.EMPTY));
    }

    private J.If ifStatement(IfStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.IF());
        return ifTail(prefix, keywordMarkers(keyword, "if"), ctx.ifTail());
    }

    private J.If ifTail(Space prefix, Markers markers, IfTailContext ctx) {
        J.ControlParentheses<Expression> condition = controlParentheses(ctx.LPAREN(), ctx.expression(), ctx.RPAREN());
        JRightPadded<Statement> thenPart = paddedStatement(ctx.paddedStatement());
        return new J.If(randomId(), prefix, markers, condition, thenPart,
                ctx.elseClause() == null ? null : elseClause(ctx.elseClause()));
    }

    /**
     * {@code elseif}, which Lucee accepts in script, is an else whose keyword is spelled {@code elseif}
     * around an if whose own keyword is spelled as nothing.
     */
    private J.If.Else elseClause(ElseClauseContext ctx) {
        Token keyword = ctx.getStart();
        Space prefix = sourceBefore(keyword);
        if (keyword.getType() == ELSEIF) {
            // the if spelled as nothing starts at its condition, so the space before that is its prefix
            Space conditionPrefix = prefix(ctx.ifTail().LPAREN().getSymbol());
            J.If nested = ifTail(conditionPrefix, Markers.build(singletonList(new KeywordSpelling(randomId(), ""))), ctx.ifTail());
            return new J.If.Else(randomId(), prefix, Markers.build(singletonList(new KeywordSpelling(randomId(), keyword.getText()))),
                    new JRightPadded<>(nested, Space.EMPTY, Markers.EMPTY));
        }
        return new J.If.Else(randomId(), prefix, keywordMarkers(keyword.getText(), "else"), paddedStatement(ctx.paddedStatement()));
    }

    private J.ForEachLoop forInStatement(ForInStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.FOR());
        Space controlPrefix = sourceBefore(ctx.LPAREN().getSymbol());
        ForInVariableContext variableContext = ctx.forInVariable();
        Statement variable;
        if (variableContext.VAR() != null) {
            Space variablePrefix = prefix(variableContext.getStart());
            String var = keyword(variableContext.VAR());
            Token name = variableContext.anyWord(0).getStart();
            Space namePrefix = sourceBefore(name);
            variable = new J.VariableDeclarations(randomId(), variablePrefix, Markers.EMPTY, emptyList(),
                    singletonList(modifier(Space.EMPTY, var, J.Modifier.Type.LanguageExtension)), null, null,
                    singletonList(new JRightPadded<>(namedVariable(namePrefix, name.getText(), null), Space.EMPTY, Markers.EMPTY)));
        } else {
            Expression target = (Expression) dottedName(variableContext.children);
            if (target instanceof J.Identifier) {
                J.Identifier name = (J.Identifier) target;
                variable = new J.VariableDeclarations(randomId(), name.getPrefix(), Markers.EMPTY, emptyList(), emptyList(),
                        null, null, singletonList(new JRightPadded<>(namedVariable(Space.EMPTY, name.getSimpleName(), null),
                        Space.EMPTY, Markers.EMPTY)));
            } else {
                variable = (J.FieldAccess) target;
            }
        }
        Space beforeIn = prefix(ctx.IN().getSymbol());
        String in = keyword(ctx.IN());
        Expression iterable = expression(ctx.expression());
        Space beforeClose = sourceBefore(ctx.RPAREN().getSymbol());
        J.ForEachLoop.Control control = new J.ForEachLoop.Control(randomId(), controlPrefix, keywordMarkers(in, "in"),
                new JRightPadded<>(variable, beforeIn, Markers.EMPTY), new JRightPadded<>(iterable, beforeClose, Markers.EMPTY));
        return new J.ForEachLoop(randomId(), prefix, keywordMarkers(keyword, "for"), control, paddedStatement(ctx.paddedStatement()));
    }

    private J.ForLoop forStatement(ForStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.FOR());
        Space controlPrefix = sourceBefore(ctx.LPAREN().getSymbol());
        List<JRightPadded<Statement>> init = forClauses(ctx.forClauses(0), ctx.SEMI(0).getSymbol());
        skip(ctx.SEMI(0).getSymbol());
        Expression condition = ctx.expression() == null ?
                new J.Empty(randomId(), prefix(ctx.SEMI(1).getSymbol()), Markers.EMPTY) :
                expression(ctx.expression());
        Space afterCondition = sourceBefore(ctx.SEMI(1).getSymbol());
        List<JRightPadded<Statement>> update = forClauses(ctx.forClauses(1), ctx.RPAREN().getSymbol());
        skip(ctx.RPAREN().getSymbol());
        J.ForLoop.Control control = new J.ForLoop.Control(randomId(), controlPrefix, Markers.EMPTY, init,
                new JRightPadded<>(condition, afterCondition, Markers.EMPTY), update);
        return new J.ForLoop(randomId(), prefix, keywordMarkers(keyword, "for"), control, paddedStatement(ctx.paddedStatement()));
    }

    /**
     * The statements of a for loop's initialization or update, separated by commas, each of which may
     * be empty.
     */
    private List<JRightPadded<Statement>> forClauses(ForClausesContext ctx, Token end) {
        List<JRightPadded<Statement>> statements = new ArrayList<>();
        List<ParseTree> children = ctx.children == null ? emptyList() : ctx.children;
        int i = 0;
        while (true) {
            Statement statement;
            if (i < children.size() && children.get(i) instanceof ExpressionContext) {
                statement = statementOf((ExpressionContext) children.get(i++));
            } else {
                Token delimiter = i < children.size() ? ((TerminalNode) children.get(i)).getSymbol() : end;
                statement = new J.Empty(randomId(), prefix(delimiter), Markers.EMPTY);
            }
            if (i < children.size()) {
                Token comma = ((TerminalNode) children.get(i++)).getSymbol();
                statements.add(new JRightPadded<>(statement, sourceBefore(comma), Markers.EMPTY));
            } else {
                statements.add(new JRightPadded<>(statement, prefix(end), Markers.EMPTY));
                return statements;
            }
        }
    }

    private J.DoWhileLoop doWhileStatement(DoWhileStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.DO());
        J.Block block = block(ctx.block());
        JRightPadded<Statement> body = ctx.SEMI() == null ? new JRightPadded<>(block, Space.EMPTY, Markers.EMPTY) :
                new JRightPadded<>(block, sourceBefore(ctx.SEMI().getSymbol()), semicolon());
        Space beforeWhile = prefix(ctx.WHILE().getSymbol());
        String whileKeyword = keyword(ctx.WHILE());
        J.ControlParentheses<Expression> condition = controlParentheses(ctx.LPAREN(), ctx.expression(), ctx.RPAREN());
        return new J.DoWhileLoop(randomId(), prefix, keywordMarkers(keyword, "do"), body,
                new JLeftPadded<>(beforeWhile, condition, keywordMarkers(whileKeyword, "while")));
    }

    private J.Switch switchStatement(SwitchStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.SWITCH());
        J.ControlParentheses<Expression> selector = controlParentheses(ctx.LPAREN(), ctx.expression(), ctx.RPAREN());
        Space blockPrefix = sourceBefore(ctx.LBRACE().getSymbol());
        List<JRightPadded<Statement>> cases = new ArrayList<>();
        for (CaseClauseContext caseClause : ctx.caseClause()) {
            cases.add(new JRightPadded<>(caseClause(caseClause), Space.EMPTY, Markers.EMPTY));
        }
        Space end = sourceBefore(ctx.RBRACE().getSymbol());
        J.Block body = new J.Block(randomId(), blockPrefix, Markers.EMPTY, new JRightPadded<>(false, Space.EMPTY, Markers.EMPTY),
                cases, end);
        return new J.Switch(randomId(), prefix, keywordMarkers(keyword, "switch"), selector, body);
    }

    private J.Case caseClause(CaseClauseContext ctx) {
        Token keyword = ctx.getStart();
        Space prefix = sourceBefore(keyword);
        J label;
        Markers markers = Markers.EMPTY;
        if (keyword.getType() == DEFAULT) {
            label = identifier(Space.EMPTY, keyword.getText());
        } else {
            markers = keywordMarkers(keyword.getText(), "case");
            label = expression(ctx.ternaryExpression());
        }
        Space beforeColon = sourceBefore(ctx.COLON().getSymbol());
        List<JRightPadded<Statement>> statements = paddedStatements(ctx.paddedStatement());
        return new J.Case(randomId(), prefix, markers, J.Case.Type.Statement,
                JContainer.build(Space.EMPTY, singletonList(new JRightPadded<>(label, Space.EMPTY, Markers.EMPTY)), Markers.EMPTY),
                JContainer.build(beforeColon, statements, Markers.EMPTY), null, null);
    }

    private J.Try tryStatement(TryStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.TRY());
        J.Block body = block(ctx.block(0));
        List<J.Try.Catch> catches = new ArrayList<>();
        for (CatchClauseContext catchClause : ctx.catchClause()) {
            Space catchPrefix = prefix(catchClause.getStart());
            String catchKeyword = keyword(catchClause.CATCH());
            Space parenPrefix = sourceBefore(catchClause.LPAREN().getSymbol());
            J.VariableDeclarations parameter = catchParameter(catchClause.catchParameter());
            Space beforeClose = sourceBefore(catchClause.RPAREN().getSymbol());
            J.ControlParentheses<J.VariableDeclarations> parentheses = new J.ControlParentheses<>(randomId(), parenPrefix,
                    Markers.EMPTY, new JRightPadded<>(parameter, beforeClose, Markers.EMPTY));
            catches.add(new J.Try.Catch(randomId(), catchPrefix, keywordMarkers(catchKeyword, "catch"), parentheses,
                    block(catchClause.block())));
        }
        JLeftPadded<J.Block> finallyBlock = null;
        if (ctx.FINALLY() != null) {
            Space finallyPrefix = prefix(ctx.FINALLY().getSymbol());
            String finallyKeyword = keyword(ctx.FINALLY());
            finallyBlock = new JLeftPadded<>(finallyPrefix, block(ctx.block(1)), keywordMarkers(finallyKeyword, "finally"));
        }
        return new J.Try(randomId(), prefix, keywordMarkers(keyword, "try"), null, body, catches, finallyBlock);
    }

    private J.VariableDeclarations catchParameter(CatchParameterContext ctx) {
        Space prefix = prefix(ctx.getStart());
        TypeTree type = null;
        J.VariableDeclarations.NamedVariable variable;
        Token name = ctx.anyWord().getStart();
        if (ctx.catchType() != null) {
            CatchTypeContext catchType = ctx.catchType();
            type = catchType.stringLiteral() != null ?
                    new Cf.TypeLiteral(randomId(), prefix(catchType.getStart()), Markers.EMPTY, string(catchType.stringLiteral()), null) :
                    dottedName(catchType.children);
            if (ctx.VAR() != null) {
                Space varPrefix = prefix(ctx.VAR().getSymbol());
                variable = varVariable(varPrefix, keyword(ctx.VAR()), name);
            } else {
                variable = namedVariable(sourceBefore(name), name.getText(), null);
            }
        } else if (ctx.VAR() != null) {
            variable = varVariable(Space.EMPTY, keyword(ctx.VAR()), name);
        } else {
            skip(name);
            variable = namedVariable(Space.EMPTY, name.getText(), null);
        }
        return new J.VariableDeclarations(randomId(), prefix, Markers.EMPTY, emptyList(), emptyList(), type, null,
                singletonList(new JRightPadded<>(variable, Space.EMPTY, Markers.EMPTY)));
    }

    /**
     * {@code var e} in a catch, after its type or alone.
     */
    private J.VariableDeclarations.NamedVariable varVariable(Space prefix, String keyword, Token name) {
        Space namePrefix = sourceBefore(name);
        return new J.VariableDeclarations.NamedVariable(randomId(), prefix,
                Markers.build(singletonList(new VarKeyword(randomId(), keyword))), identifier(namePrefix, name.getText()),
                emptyList(), null, null);
    }

    private J.Return returnStatement(Token keyword, @Nullable ExpressionContext expression) {
        Space prefix = sourceBefore(keyword);
        return new J.Return(randomId(), prefix, keywordMarkers(keyword.getText(), "return"),
                expression == null ? null : expression(expression));
    }

    /**
     * A script component, which is the class its file declares. Its {@code extends} and {@code implements}
     * name its supertypes when their values are dotted names, and its other attributes are annotations.
     */
    private J.ClassDeclaration component(ComponentDeclarationContext ctx) {
        Space prefix = prefix(ctx.getStart());
        List<J.Modifier> modifiers = new ArrayList<>();
        Token keyword = null;
        for (ParseTree child : ctx.children) {
            if (!(child instanceof TerminalNode)) {
                break;
            }
            Token token = ((TerminalNode) child).getSymbol();
            if (token.getType() == ABSTRACT || token.getType() == FINAL) {
                Space modifierPrefix = sourceBefore(token);
                modifiers.add(modifier(modifierPrefix, token.getText(),
                        token.getType() == ABSTRACT ? J.Modifier.Type.Abstract : J.Modifier.Type.Final));
            } else {
                keyword = token;
            }
        }
        Space keywordPrefix = sourceBefore(Objects.requireNonNull(keyword));
        boolean isInterface = keyword.getType() == INTERFACE;
        JavaType.FullyQualified type = componentType(isInterface);
        J.ClassDeclaration.Kind kind = new J.ClassDeclaration.Kind(randomId(), keywordPrefix,
                keywordMarkers(keyword.getText(), isInterface ? "interface" : "component"), emptyList(),
                isInterface ? J.ClassDeclaration.Kind.Type.Interface : J.ClassDeclaration.Kind.Type.Class);
        J.Identifier name = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), componentName(), type, null);
        ClassAttributes attributes = classAttributes(headerAttributes(ctx.headerAttribute()), isInterface);
        return new J.ClassDeclaration(randomId(), prefix, Markers.EMPTY, attributes.annotations, modifiers, kind, name, null,
                null, attributes.extendings, attributes.implementings, null, block(ctx.block()), type);
    }

    private static final class ClassAttributes {
        final List<J.Annotation> annotations = new ArrayList<>();

        @Nullable
        JLeftPadded<TypeTree> extendings;

        @Nullable
        JContainer<TypeTree> implementings;
    }

    private ClassAttributes classAttributes(List<Cf.TagAttribute> attributes, boolean isInterface) {
        ClassAttributes result = new ClassAttributes();
        for (int position = 0; position < attributes.size(); position++) {
            Cf.TagAttribute attribute = attributes.get(position);
            String attributeName = attribute.getName().getSimpleName();
            // an interface extends interfaces, which J.ClassDeclaration holds as what it implements
            boolean superclass = !isInterface && "extends".equalsIgnoreCase(attributeName) && result.extendings == null;
            boolean interfaces = (isInterface ? "extends" : "implements").equalsIgnoreCase(attributeName) &&
                                 result.implementings == null;
            JLeftPadded<Expression> value = attribute.getPadding().getValue();
            SupertypeNames supertypes = value == null || !(superclass || interfaces) ? null : supertypeNames(value.getElement());
            if (supertypes != null && superclass && supertypes.names.size() == 1 &&
                supertypes.names.get(0).getAfter().isEmpty()) {
                result.extendings = new JLeftPadded<>(attribute.getPrefix(), supertypes.names.get(0).getElement(),
                        Markers.build(singletonList(clause(attribute, position, value.getBefore(), supertypes))));
            } else if (supertypes != null && interfaces) {
                result.implementings = JContainer.build(attribute.getPrefix(), supertypes.names,
                        Markers.build(singletonList(clause(attribute, position, value.getBefore(), supertypes))));
            } else {
                result.annotations.add(annotation(attribute, isInterface ? LanguageAttributes.INTERFACE : LanguageAttributes.COMPONENT));
            }
        }
        return result;
    }

    /**
     * A tag that closed, which is a declaration when it is a function, or a component or an interface around a
     * component file's content.
     */
    private Statement declaration(Cf.Tag tag, boolean atRoot) {
        if (tag.isNamed("cffunction")) {
            return tagFunction(tag);
        }
        if (atRoot && !declaredClass && (tag.isNamed("cfcomponent") || tag.isNamed("cfinterface")) &&
            path.toString().toLowerCase(Locale.ROOT).endsWith(".cfc")) {
            declaredClass = true;
            return tagComponent(tag);
        }
        return tag;
    }

    /**
     * A component written as a tag, which is the class its file declares, as a script component is. The
     * {@code <cfproperty>} tags in it are its fields.
     */
    private J.ClassDeclaration tagComponent(Cf.Tag tag) {
        boolean isInterface = tag.isNamed("cfinterface");
        JavaType.FullyQualified type = componentType(isInterface);
        J.ClassDeclaration.Kind kind = new J.ClassDeclaration.Kind(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                isInterface ? J.ClassDeclaration.Kind.Type.Interface : J.ClassDeclaration.Kind.Type.Class);
        J.Identifier name = new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), componentName(), type, null);
        ClassAttributes attributes = classAttributes(tag.getAttributes(), isInterface);
        J.Block body = Objects.requireNonNull(tag.getBody());
        body = body.getPadding().withStatements(ListUtils.map(body.getPadding().getStatements(), statement -> {
            if (statement.getElement() instanceof Cf.Tag && ((Cf.Tag) statement.getElement()).isNamed("cfproperty")) {
                J.VariableDeclarations field = tagVariable((Cf.Tag) statement.getElement(), LanguageAttributes.PROPERTY);
                return field == null ? statement : statement.withElement(field);
            }
            return statement;
        }));
        return new J.ClassDeclaration(randomId(), tag.getPrefix(), Markers.build(singletonList(tagSyntax(tag))),
                attributes.annotations, emptyList(), kind, name, null, null, attributes.extendings, attributes.implementings,
                null, body, type);
    }

    /**
     * A function written as a tag. The attributes naming it and its return type are its name and its return type,
     * and the {@code <cfargument>} tags it starts with are its parameters. One without a name it can take stays a tag.
     */
    private Statement tagFunction(Cf.Tag tag) {
        DeclarationAttributes attributes = declarationAttributes(tag.getAttributes(), LanguageAttributes.FUNCTION,
                "returntype", false, false);
        if (attributes.name == null) {
            return tag;
        }
        J.Block body = tag.getBody();
        List<JRightPadded<Statement>> parameters = new ArrayList<>();
        if (body != null) {
            List<JRightPadded<Statement>> statements = body.getPadding().getStatements();
            int first = 0;
            while (first < statements.size()) {
                Statement statement = statements.get(first).getElement();
                J.VariableDeclarations parameter = statement instanceof Cf.Tag && ((Cf.Tag) statement).isNamed("cfargument") ?
                        tagVariable((Cf.Tag) statement, LanguageAttributes.ARGUMENT) : null;
                if (parameter == null) {
                    break;
                }
                parameters.add(new JRightPadded<>(parameter, Space.EMPTY, Markers.EMPTY));
                first++;
            }
            body = body.getPadding().withStatements(new ArrayList<>(statements.subList(first, statements.size())));
        }
        if (parameters.isEmpty()) {
            parameters.add(new JRightPadded<>(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY), Space.EMPTY, Markers.EMPTY));
        }
        attributes.markers.add(0, tagSyntax(tag));
        return new J.MethodDeclaration(randomId(), tag.getPrefix(), Markers.build(attributes.markers),
                attributes.annotations, emptyList(), null, attributes.type,
                new J.MethodDeclaration.IdentifierWithAnnotations(attributes.name, emptyList()),
                JContainer.build(Space.EMPTY, parameters, Markers.EMPTY), null, body, null, null);
    }

    /**
     * A {@code <cfargument>} or {@code <cfproperty>} tag as the variable it declares, or null for one without a name
     * it can take.
     */
    private J.@Nullable VariableDeclarations tagVariable(Cf.Tag tag, String languageTag) {
        if (tag.getBody() != null || tag.getClosing() != null || tag.getExpression() != null) {
            return null;
        }
        DeclarationAttributes attributes = declarationAttributes(tag.getAttributes(), languageTag, "type", false, false);
        if (attributes.name == null) {
            return null;
        }
        attributes.markers.add(0, tagSyntax(tag));
        return new J.VariableDeclarations(randomId(), tag.getPrefix(), Markers.build(attributes.markers),
                attributes.annotations, emptyList(), attributes.type, null, singletonList(new JRightPadded<>(
                namedVariable(Space.EMPTY, attributes.name.getSimpleName(), null), Space.EMPTY, Markers.EMPTY)));
    }

    private static TagSyntax tagSyntax(Cf.Tag tag) {
        return new TagSyntax(randomId(), tag.getName(), tag.getBeforeEnd(), tag.isSelfClosing(), tag.getClosing());
    }

    /**
     * A declaration's attributes: the ones naming its name and its type, each kept as a clause among its markers, and
     * the rest, which are its annotations.
     */
    private static final class DeclarationAttributes {
        final List<J.Annotation> annotations = new ArrayList<>();
        final List<Marker> markers = new ArrayList<>();

        J.@Nullable Identifier name;

        @Nullable
        TypeTree type;
    }

    /**
     * Sorts a declaration's attributes. {@code named} and {@code typed} say whether its syntax already gives it a
     * name and a type, so that an attribute naming either is only an annotation.
     */
    private DeclarationAttributes declarationAttributes(List<Cf.TagAttribute> attributes, String languageTag,
                                                        String typeAttribute, boolean named, boolean typed) {
        DeclarationAttributes result = new DeclarationAttributes();
        for (int position = 0; position < attributes.size(); position++) {
            Cf.TagAttribute attribute = attributes.get(position);
            String attributeName = attribute.getName().getSimpleName();
            Written written = written(attribute);
            if (!named && result.name == null && written != null && "name".equalsIgnoreCase(attributeName) &&
                NAME.matcher(written.text).matches()) {
                result.name = identifier(Space.EMPTY, written.text);
                result.markers.add(clause(attribute, position, written));
            } else if (!typed && result.type == null && written != null && typeAttribute.equalsIgnoreCase(attributeName) &&
                       DOTTED_NAME.matcher(written.text).matches()) {
                result.type = dottedTypeName(written.text);
                result.markers.add(clause(attribute, position, written));
            } else {
                result.annotations.add(annotation(attribute, languageTag));
            }
        }
        return result;
    }

    private static final Pattern NAME = Pattern.compile("[\\p{L}_$][\\p{L}\\p{Nd}_$]*");

    /**
     * An attribute's value as written, when the printer can write it back from the name it holds: a string with no
     * interpolation or escape, or a name written without quotes.
     */
    private static final class Written {
        final Space beforeEquals;
        final Space afterEquals;
        final String quote;
        final String text;

        Written(Space beforeEquals, Space afterEquals, String quote, String text) {
            this.beforeEquals = beforeEquals;
            this.afterEquals = afterEquals;
            this.quote = quote;
            this.text = text;
        }
    }

    private static @Nullable Written written(Cf.TagAttribute attribute) {
        JLeftPadded<Expression> value = attribute.getPadding().getValue();
        if (value == null) {
            return null;
        }
        Expression element = value.getElement();
        if (element instanceof J.Literal && ((J.Literal) element).getValue() instanceof String) {
            String source = Objects.requireNonNull(((J.Literal) element).getValueSource());
            String quote = quote(source);
            String text = source.substring(quote.length(), source.length() - quote.length());
            return text.equals(((J.Literal) element).getValue()) ? new Written(value.getBefore(), element.getPrefix(), quote, text) : null;
        }
        String text = dottedText(element, true);
        return text == null ? null : new Written(value.getBefore(), element.getPrefix(), "", text);
    }

    /**
     * The quote around a string as written, or none for an attribute value written without quotes.
     */
    private static String quote(String valueSource) {
        char first = valueSource.isEmpty() ? 0 : valueSource.charAt(0);
        return (first == '"' || first == '\'') && valueSource.length() >= 2 &&
               valueSource.charAt(valueSource.length() - 1) == first ? String.valueOf(first) : "";
    }

    /**
     * The text of a dotted name with nothing between its parts, or null for anything else.
     */
    private static @Nullable String dottedText(Expression expression, boolean outermost) {
        if (!outermost && !expression.getPrefix().isEmpty()) {
            return null;
        }
        if (expression instanceof J.Identifier) {
            return ((J.Identifier) expression).getSimpleName();
        }
        if (expression instanceof J.FieldAccess && expression.getMarkers().getMarkers().isEmpty()) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            String target = dottedText(fieldAccess.getTarget(), false);
            return target == null || !fieldAccess.getPadding().getName().getBefore().isEmpty() ||
                   !fieldAccess.getName().getPrefix().isEmpty() ? null : target + "." + fieldAccess.getSimpleName();
        }
        return null;
    }

    private static TypeTree dottedTypeName(String dotted) {
        TypeTree name = null;
        for (String segment : dotted.split("\\.")) {
            J.Identifier identifier = identifier(Space.EMPTY, segment);
            name = name == null ? identifier : new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, (Expression) name,
                    new JLeftPadded<>(Space.EMPTY, identifier, Markers.EMPTY), null);
        }
        return Objects.requireNonNull(name);
    }

    private static AttributeClause clause(Cf.TagAttribute attribute, int position, Written written) {
        return new AttributeClause(randomId(), attribute.getPrefix(), attribute.getName().getSimpleName(), position,
                written.beforeEquals, written.afterEquals, written.quote);
    }

    /**
     * The class a script component's file declares, named for its path in the repository: {@code models/User.cfc}
     * is {@code models.User}. An application can reach it by another name, through a mapping or from the
     * directory of the file that names it.
     */
    private JavaType.FullyQualified componentType(boolean isInterface) {
        String file = path.toString().replace('\\', '/');
        int extension = file.lastIndexOf('.');
        String fullyQualifiedName = (extension > file.lastIndexOf('/') ? file.substring(0, extension) : file).replace('/', '.');
        return new JavaType.Class(null, 0, fullyQualifiedName, isInterface ? JavaType.FullyQualified.Kind.Interface :
                JavaType.FullyQualified.Kind.Class, null, null, null, null, null, null, null);
    }

    private String componentName() {
        String file = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        int extension = file.lastIndexOf('.');
        return extension > 0 ? file.substring(0, extension) : file;
    }

    private static final class SupertypeNames {
        final Space afterEquals;
        final String quote;
        final List<JRightPadded<TypeTree>> names;

        SupertypeNames(Space afterEquals, String quote, List<JRightPadded<TypeTree>> names) {
            this.afterEquals = afterEquals;
            this.quote = quote;
            this.names = names;
        }
    }

    private static final Pattern DOTTED_NAME = Pattern.compile("[\\p{L}\\p{Nd}_$\\-]+(\\.[\\p{L}\\p{Nd}_$\\-]+)*");

    /**
     * The names an {@code extends} or {@code implements} attribute's value lists: a string such as
     * {@code "models.Base"} or {@code "IFoo, IBar"}, or an unquoted dotted name. Null when it lists anything
     * else, as an interpolated string does.
     */
    private static @Nullable SupertypeNames supertypeNames(Expression value) {
        if (value instanceof J.Identifier || value instanceof J.FieldAccess && isDottedName(value)) {
            return new SupertypeNames(value.getPrefix(), "", singletonList(new JRightPadded<>(
                    (TypeTree) value.withPrefix(Space.EMPTY), Space.EMPTY, Markers.EMPTY)));
        }
        if (!(value instanceof J.Literal) || !(((J.Literal) value).getValue() instanceof String)) {
            return null;
        }
        String valueSource = Objects.requireNonNull(((J.Literal) value).getValueSource());
        String quote = quote(valueSource);
        String text = valueSource.substring(quote.length(), valueSource.length() - quote.length());
        if (!text.equals(((J.Literal) value).getValue())) {
            return null;
        }
        List<JRightPadded<TypeTree>> names = new ArrayList<>();
        for (String listed : text.split(",", -1)) {
            int start = 0;
            while (start < listed.length() && isTemplateWhitespace(listed.charAt(start))) {
                start++;
            }
            int end = listed.length();
            while (end > start && isTemplateWhitespace(listed.charAt(end - 1))) {
                end--;
            }
            String dotted = listed.substring(start, end);
            if (!DOTTED_NAME.matcher(dotted).matches()) {
                return null;
            }
            TypeTree name = null;
            for (String segment : dotted.split("\\.")) {
                J.Identifier identifier = identifier(Space.EMPTY, segment);
                name = name == null ? identifier : new J.FieldAccess(randomId(), Space.EMPTY, Markers.EMPTY, (Expression) name,
                        new JLeftPadded<>(Space.EMPTY, identifier, Markers.EMPTY), null);
            }
            names.add(new JRightPadded<>(Objects.requireNonNull(name).withPrefix(Space.format(listed.substring(0, start))),
                    Space.format(listed.substring(end)), Markers.EMPTY));
        }
        return new SupertypeNames(value.getPrefix(), quote, names);
    }

    private static boolean isDottedName(Expression expression) {
        if (expression instanceof J.Identifier) {
            return true;
        }
        return expression instanceof J.FieldAccess && expression.getMarkers().getMarkers().isEmpty() &&
               isDottedName(((J.FieldAccess) expression).getTarget());
    }

    private static AttributeClause clause(Cf.TagAttribute attribute, int position, Space beforeEquals, SupertypeNames names) {
        return new AttributeClause(randomId(), Space.EMPTY, attribute.getName().getSimpleName(), position, beforeEquals,
                names.afterEquals, names.quote);
    }

    /**
     * An attribute of a declaration, which is an annotation on it, typed when the language defines it.
     */
    private J.Annotation annotation(Cf.TagAttribute attribute, String tag) {
        J.Identifier name = attribute.getName().withType(languageAttributes.type(tag, attribute.getName().getSimpleName()));
        JLeftPadded<Expression> value = attribute.getPadding().getValue();
        JContainer<Expression> arguments = value == null ? null : JContainer.build(value.getBefore(),
                singletonList(new JRightPadded<>(value.getElement(), Space.EMPTY, Markers.EMPTY)), Markers.EMPTY);
        return new J.Annotation(randomId(), attribute.getPrefix(), Markers.EMPTY, name, arguments);
    }

    private List<J.Annotation> annotations(List<Cf.TagAttribute> attributes, String tag) {
        List<J.Annotation> annotations = new ArrayList<>(attributes.size());
        for (Cf.TagAttribute attribute : attributes) {
            annotations.add(annotation(attribute, tag));
        }
        return annotations;
    }

    /**
     * A property, which is a field. {@code property string name;} gives its type and name, and
     * {@code property name="name" type="string";} gives them as attributes. A property with no name it can take is
     * a tag written as a statement, as it would be a {@code <cfproperty>} tag in tag syntax.
     */
    private Statement property(PropertyDeclarationContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.PROPERTY());
        TypeTree type = null;
        J.Identifier name = null;
        if (ctx.dottedName() != null) {
            TypeTree first = dottedName(ctx.dottedName().children);
            if (ctx.anyWord() != null) {
                type = first;
                Token word = ctx.anyWord().getStart();
                name = identifier(sourceBefore(word), word.getText());
            } else if (first instanceof J.Identifier) {
                name = (J.Identifier) first;
            } else {
                throw error("a property named with a dotted name", ctx.dottedName().getStart());
            }
        }
        List<Cf.TagAttribute> attributeList = statementAttributes(ctx.statementAttribute());
        DeclarationAttributes attributes = declarationAttributes(attributeList, LanguageAttributes.PROPERTY, "type",
                name != null, type != null);
        J.VariableDeclarations.NamedVariable variable;
        if (name != null) {
            variable = namedVariable(name.getPrefix(), name.getSimpleName(), null);
        } else if (attributes.name != null) {
            variable = namedVariable(Space.EMPTY, attributes.name.getSimpleName(), null);
        } else {
            return new Cf.ScriptTag(randomId(), prefix, Markers.EMPTY, identifier(Space.EMPTY, keyword), attributeList, null, null);
        }
        return new J.VariableDeclarations(randomId(), prefix, Markers.build(attributes.markers), attributes.annotations,
                singletonList(modifier(Space.EMPTY, keyword, J.Modifier.Type.LanguageExtension)),
                type != null ? type : attributes.type, null,
                singletonList(new JRightPadded<>(variable, Space.EMPTY, Markers.EMPTY)));
    }

    private J.Import importStatement(ImportStatementContext ctx) {
        Space prefix = prefix(ctx.getStart());
        Token keyword = ctx.IMPORT().getSymbol();
        if (!"import".equals(keyword.getText())) {
            throw error("an import keyword not in lower case", keyword);
        }
        skip(keyword);
        Expression qualid = null;
        Space before = Space.EMPTY;
        for (int i = 1; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            Token token = child instanceof TerminalNode ? ((TerminalNode) child).getSymbol() : ((ParserRuleContext) child).getStart();
            if (token.getType() == DOT) {
                before = sourceBefore(token);
                continue;
            }
            J.Identifier name = identifier(sourceBefore(token), token.getText());
            qualid = qualid == null ? name : new J.FieldAccess(randomId(), qualid.getPrefix(), Markers.EMPTY,
                    qualid.withPrefix(Space.EMPTY), new JLeftPadded<>(before, name, Markers.EMPTY), null);
        }
        if (!(qualid instanceof J.FieldAccess)) {
            throw error("an import of an undotted name", keyword);
        }
        return new J.Import(randomId(), prefix, Markers.EMPTY, new JLeftPadded<>(Space.EMPTY, false, Markers.EMPTY),
                (J.FieldAccess) qualid, null);
    }

    private Cf.ScriptTag scriptTag(ScriptTagContext ctx) {
        Space prefix = prefix(ctx.getStart());
        Token name = ctx.getStart();
        skip(name);
        Expression expression = null;
        List<Cf.TagAttribute> attributes;
        if (ctx.expression() != null) {
            expression = expression(ctx.expression());
        }
        attributes = new ArrayList<>(ctx.statementAttribute().size());
        AttributeComma comma = null;
        for (ParseTree child : ctx.children) {
            if (child instanceof TerminalNode && ((TerminalNode) child).getSymbol().getType() == COMMA) {
                comma = new AttributeComma(randomId(), sourceBefore(((TerminalNode) child).getSymbol()));
            } else if (child instanceof StatementAttributeContext) {
                Cf.TagAttribute attribute = statementAttribute((StatementAttributeContext) child);
                attributes.add(comma == null ? attribute : attribute.withMarkers(attribute.getMarkers().add(comma)));
                comma = null;
            }
        }
        J.Block body = ctx.bodyBlock() == null ? null : block(ctx.bodyBlock());
        return new Cf.ScriptTag(randomId(), prefix, Markers.EMPTY, identifier(Space.EMPTY, name.getText()), attributes,
                expression, body);
    }

    private JRightPadded<Statement> functionDeclaration(FunctionDeclarationContext ctx) {
        Space prefix = prefix(ctx.getStart());
        List<J.Modifier> modifiers = new ArrayList<>();
        TypeTree returnType = null;
        int returnTypePosition = 0;
        for (ParseTree child : ctx.children) {
            if (child instanceof FunctionModifierContext) {
                Token token = ((FunctionModifierContext) child).getStart();
                Space modifierPrefix = sourceBefore(token);
                modifiers.add(modifier(modifierPrefix, token.getText(), modifierType(token.getType())));
            } else if (child instanceof ReturnTypeContext) {
                returnType = returnType((ReturnTypeContext) child);
                returnTypePosition = modifiers.size();
            } else if (child instanceof TerminalNode) {
                break;
            }
        }
        Token function = ctx.FUNCTION().getSymbol();
        FunctionKeyword keyword = new FunctionKeyword(randomId(), sourceBefore(function), function.getText());
        Token nameToken = ctx.anyWord().getStart();
        J.Identifier name = identifier(sourceBefore(nameToken), nameToken.getText());
        Token open = ctx.LPAREN() != null ? ctx.LPAREN().getSymbol() : ctx.CALL_LPAREN().getSymbol();
        Space parametersPrefix = prefix(open);
        List<JRightPadded<Statement>> parameters = new ArrayList<>();
        for (JRightPadded<J> parameter : parameters(open, ctx.parameters(), ctx.RPAREN().getSymbol())) {
            parameters.add(new JRightPadded<>((Statement) parameter.getElement(), parameter.getAfter(), parameter.getMarkers()));
        }
        DeclarationAttributes attributes = declarationAttributes(headerAttributes(ctx.headerAttribute()),
                LanguageAttributes.FUNCTION, "returntype", true, returnType != null);
        attributes.markers.add(0, keyword);
        if (returnType != null && returnTypePosition < modifiers.size()) {
            attributes.markers.add(new ReturnTypePosition(randomId(), returnTypePosition));
        }
        J.Block body = ctx.block() == null ? null : block(ctx.block());
        J.MethodDeclaration method = new J.MethodDeclaration(randomId(), prefix, Markers.build(attributes.markers),
                attributes.annotations, modifiers, null, returnType != null ? returnType : attributes.type,
                new J.MethodDeclaration.IdentifierWithAnnotations(name, emptyList()),
                JContainer.build(parametersPrefix, parameters, Markers.EMPTY), null, body, null, null);
        if (ctx.SEMI() != null) {
            return new JRightPadded<>(method, sourceBefore(ctx.SEMI().getSymbol()), semicolon());
        }
        return new JRightPadded<>(method, Space.EMPTY, Markers.EMPTY);
    }

    private static J.Modifier.Type modifierType(int type) {
        switch (type) {
            case PUBLIC:
                return J.Modifier.Type.Public;
            case PRIVATE:
                return J.Modifier.Type.Private;
            case STATIC:
                return J.Modifier.Type.Static;
            case FINAL:
                return J.Modifier.Type.Final;
            case ABSTRACT:
                return J.Modifier.Type.Abstract;
            default:
                return J.Modifier.Type.LanguageExtension;
        }
    }

    private TypeTree returnType(ReturnTypeContext ctx) {
        TypeTree type = dottedName(ctx.children);
        return ctx.RBRACKET() == null ? type :
                arrayType(type, ctx.LBRACKET() != null ? ctx.LBRACKET() : ctx.INDEX_LBRACKET(), ctx.RBRACKET());
    }

    /**
     * A parenthesized parameter list, each a {@link J.VariableDeclarations}: {@code required string
     * name = "x"}. An empty one holds a single {@link J.Empty}.
     */
    private List<JRightPadded<J>> parameters(Token open, @Nullable ParametersContext ctx, Token close) {
        skip(open);
        if (ctx == null) {
            Space inner = sourceBefore(close);
            return singletonList(new JRightPadded<>(new J.Empty(randomId(), inner, Markers.EMPTY), Space.EMPTY, Markers.EMPTY));
        }
        return commaSeparated(ctx.children, close, p -> parameter((ParameterContext) p));
    }

    private List<JRightPadded<J>> parameters(ParameterListContext ctx) {
        return parameters(ctx.LPAREN().getSymbol(), ctx.parameters(), ctx.RPAREN().getSymbol());
    }

    /**
     * A parameter, and the attributes written after it on its line, {@code hint="..."}.
     */
    private J parameter(ParameterContext ctx) {
        Space prefix = prefix(ctx.getStart());
        List<J.Modifier> modifiers = new ArrayList<>();
        if (ctx.REQUIRED() != null) {
            Token required = ctx.REQUIRED().getSymbol();
            skip(required);
            modifiers.add(modifier(Space.EMPTY, required.getText(), J.Modifier.Type.LanguageExtension));
        }
        TypeTree type = null;
        if (ctx.parameterType() != null) {
            ParameterTypeContext parameterType = ctx.parameterType();
            type = dottedName(parameterType.dottedName().children);
            if (parameterType.RBRACKET() != null) {
                type = arrayType(type, parameterType.LBRACKET() != null ? parameterType.LBRACKET() :
                        parameterType.INDEX_LBRACKET(), parameterType.RBRACKET());
            }
        }
        Token name = ctx.anyWord().getStart();
        Space namePrefix = sourceBefore(name);
        JLeftPadded<Expression> initializer = null;
        if (ctx.ASSIGN() != null) {
            Space beforeEquals = sourceBefore(ctx.ASSIGN().getSymbol());
            initializer = new JLeftPadded<>(beforeEquals, expression(ctx.ternaryExpression()), Markers.EMPTY);
        }
        J.VariableDeclarations.NamedVariable variable = namedVariable(namePrefix, name.getText(), initializer);
        List<Cf.TagAttribute> attributeList = new ArrayList<>();
        for (NamedAttributeContext attribute : ctx.namedAttribute()) {
            attributeList.add(namedAttribute(attribute));
        }
        DeclarationAttributes attributes = declarationAttributes(attributeList, LanguageAttributes.ARGUMENT, "type",
                true, type != null);
        return new J.VariableDeclarations(randomId(), prefix, Markers.build(attributes.markers), attributes.annotations,
                modifiers, type != null ? type : attributes.type, null,
                singletonList(new JRightPadded<>(variable, Space.EMPTY, Markers.EMPTY)));
    }

    private List<Cf.TagAttribute> headerAttributes(List<HeaderAttributeContext> contexts) {
        List<Cf.TagAttribute> attributes = new ArrayList<>(contexts.size());
        for (HeaderAttributeContext ctx : contexts) {
            Space prefix = prefix(ctx.getStart());
            J.Identifier name = attributeName(ctx.attributeName());
            JLeftPadded<Expression> value = null;
            if (ctx.ASSIGN() != null) {
                Space beforeEquals = sourceBefore(ctx.ASSIGN().getSymbol());
                value = new JLeftPadded<>(beforeEquals, expression(ctx.ternaryExpression()), Markers.EMPTY);
            }
            attributes.add(new Cf.TagAttribute(randomId(), prefix, Markers.EMPTY, name, value));
        }
        return attributes;
    }

    private List<Cf.TagAttribute> statementAttributes(List<StatementAttributeContext> contexts) {
        List<Cf.TagAttribute> attributes = new ArrayList<>(contexts.size());
        for (StatementAttributeContext ctx : contexts) {
            attributes.add(statementAttribute(ctx));
        }
        return attributes;
    }

    private Cf.TagAttribute statementAttribute(StatementAttributeContext ctx) {
        Space prefix = prefix(ctx.getStart());
        int start = start(ctx.getStart());
        Token last = ctx.getStart();
        for (ParseTree child : ctx.children) {
            Token token = child instanceof TerminalNode ? ((TerminalNode) child).getSymbol() : ((ParserRuleContext) child).getStop();
            if (token.getType() == ASSIGN) {
                break;
            }
            last = token;
        }
        skip(last);
        J.Identifier name = identifier(Space.EMPTY, source.substring(start, cursor));
        JLeftPadded<Expression> value = null;
        if (ctx.ASSIGN() != null) {
            Space beforeEquals = sourceBefore(ctx.ASSIGN().getSymbol());
            value = new JLeftPadded<>(beforeEquals, expression(ctx.ternaryExpression()), Markers.EMPTY);
        }
        return new Cf.TagAttribute(randomId(), prefix, Markers.EMPTY, name, value);
    }

    private Cf.TagAttribute namedAttribute(NamedAttributeContext ctx) {
        Space prefix = prefix(ctx.getStart());
        J.Identifier name = attributeName(ctx.attributeName());
        Space beforeEquals = sourceBefore(ctx.ASSIGN().getSymbol());
        return new Cf.TagAttribute(randomId(), prefix, Markers.EMPTY, name,
                new JLeftPadded<>(beforeEquals, expression(ctx.ternaryExpression()), Markers.EMPTY));
    }

    private J.Identifier attributeName(ParserRuleContext ctx) {
        int start = start(ctx.getStart());
        skip(ctx.getStop());
        return identifier(Space.EMPTY, source.substring(start, cursor));
    }

    // ------------------------------------------------------------------------------------------
    // Expressions
    // ------------------------------------------------------------------------------------------

    private Expression expression(ParserRuleContext ctx) {
        if (ctx instanceof ExpressionContext) {
            return assignment((ExpressionContext) ctx);
        } else if (ctx instanceof TernaryExpressionContext) {
            return ternary((TernaryExpressionContext) ctx);
        } else if (ctx instanceof OperationContext) {
            return operation((OperationContext) ctx);
        } else if (ctx instanceof UnaryExpressionContext) {
            UnaryExpressionContext unary = (UnaryExpressionContext) ctx;
            if (unary.postfixExpression() != null) {
                return postfix(unary.postfixExpression());
            }
            return unary(unary.getStart(), unary.unaryExpression());
        } else if (ctx instanceof PostfixExpressionContext) {
            return postfix((PostfixExpressionContext) ctx);
        }
        throw error("an expression of an unknown kind", ctx.getStart());
    }

    // The binary nodes on the left spine are folded in a loop, up through the parents of the leftmost operand,
    // since a run of left-associative operators nests to the left as deep as the run is long.
    private Expression operation(OperationContext ctx) {
        if (ctx.unaryExpression() != null) {
            return expression(ctx.unaryExpression());
        } else if (ctx.NOT() != null || ctx.BANG() != null) {
            return unary(ctx.getStart(), ctx.operation(0));
        }
        OperationContext left = ctx;
        while (left.operation(1) != null) {
            left = left.operation(0);
        }
        Expression result = operation(left);
        while (left != ctx) {
            left = (OperationContext) left.getParent();
            ComparisonOperatorContext comparison = left.comparisonOperator();
            Token operator = comparison != null ? comparison.getStart() : ((TerminalNode) left.getChild(1)).getSymbol();
            Space before = sourceBefore(operator);
            result = binary(result, before, operator, expression(left.operation(1)));
        }
        return result;
    }

    private Expression assignment(ExpressionContext ctx) {
        Expression left = expression(ctx.ternaryExpression());
        if (ctx.assignmentOperator() == null) {
            return left;
        }
        Token operator = ctx.assignmentOperator().getStart();
        Space before = sourceBefore(operator);
        Expression right = expression(ctx.expression());
        if (operator.getType() == ASSIGN) {
            return new J.Assignment(randomId(), left.getPrefix(), Markers.EMPTY, left.withPrefix(Space.EMPTY),
                    new JLeftPadded<>(before, right, Markers.EMPTY), null);
        }
        if (operator.getType() == CONCAT_ASSIGN) {
            return new Cf.AssignmentOperation(randomId(), left.getPrefix(), Markers.EMPTY, left.withPrefix(Space.EMPTY),
                    new JLeftPadded<>(before, Cf.AssignmentOperation.Type.Concat, Markers.EMPTY), right, null);
        }
        J.AssignmentOperation.Type type;
        switch (operator.getType()) {
            case PLUS_ASSIGN:
                type = J.AssignmentOperation.Type.Addition;
                break;
            case MINUS_ASSIGN:
                type = J.AssignmentOperation.Type.Subtraction;
                break;
            case STAR_ASSIGN:
                type = J.AssignmentOperation.Type.Multiplication;
                break;
            case SLASH_ASSIGN:
                type = J.AssignmentOperation.Type.Division;
                break;
            default:
                type = J.AssignmentOperation.Type.Modulo;
        }
        return new J.AssignmentOperation(randomId(), left.getPrefix(), Markers.EMPTY, left.withPrefix(Space.EMPTY),
                new JLeftPadded<>(before, type, Markers.EMPTY), right, null);
    }

    private Expression ternary(TernaryExpressionContext ctx) {
        Expression condition = expression(ctx.operation());
        if (ctx.ELVIS() != null) {
            Token elvis = ctx.ELVIS().getSymbol();
            Space before = sourceBefore(elvis);
            return cfBinary(condition, before, Cf.Binary.Type.Elvis, "?:".equals(elvis.getText()) ? null : elvis.getText(),
                    expression(ctx.ternaryExpression(0)));
        }
        if (ctx.QUESTION() == null) {
            return condition;
        }
        Space before = sourceBefore(ctx.QUESTION().getSymbol());
        Expression truePart = expression(ctx.ternaryExpression(0));
        Space beforeColon = sourceBefore(ctx.COLON().getSymbol());
        Expression falsePart = expression(ctx.ternaryExpression(1));
        return new J.Ternary(randomId(), condition.getPrefix(), Markers.EMPTY, condition.withPrefix(Space.EMPTY),
                new JLeftPadded<>(before, truePart, Markers.EMPTY), new JLeftPadded<>(beforeColon, falsePart, Markers.EMPTY), null);
    }

    private Expression binary(Expression left, Space before, Token operator, Expression right) {
        String spelling = operator.getText();
        switch (operator.getType()) {
            case IMP:
                return cfBinary(left, before, Cf.Binary.Type.Implication, spelling, right);
            case EQV:
                return cfBinary(left, before, Cf.Binary.Type.Equivalence, spelling, right);
            case XOR:
                return cfBinary(left, before, Cf.Binary.Type.Xor, spelling, right);
            case PIPEPIPE:
                return binary(left, before, J.Binary.Type.Or, null, right);
            case OR:
                return binary(left, before, J.Binary.Type.Or, spelling, right);
            case AMPAMP:
                return binary(left, before, J.Binary.Type.And, null, right);
            case AND:
                return binary(left, before, J.Binary.Type.And, spelling, right);
            case GREATER_THAN_OR_EQUAL_TO:
            case GTE:
            case GE:
                return binary(left, before, J.Binary.Type.GreaterThanOrEqual, spelling, right);
            case GTE_SIGN:
                return binary(left, before, J.Binary.Type.GreaterThanOrEqual, null, right);
            case LESS_THAN_OR_EQUAL_TO:
            case LTE:
            case LE:
                return binary(left, before, J.Binary.Type.LessThanOrEqual, spelling, right);
            case LTE_SIGN:
                return binary(left, before, J.Binary.Type.LessThanOrEqual, null, right);
            case GREATER_THAN:
            case GT:
                return binary(left, before, J.Binary.Type.GreaterThan, spelling, right);
            case GT_SIGN:
                return binary(left, before, J.Binary.Type.GreaterThan, null, right);
            case LESS_THAN:
            case LT:
                return binary(left, before, J.Binary.Type.LessThan, spelling, right);
            case LT_SIGN:
                return binary(left, before, J.Binary.Type.LessThan, null, right);
            case DOES_NOT_CONTAIN:
                return cfBinary(left, before, Cf.Binary.Type.DoesNotContain, spelling, right);
            case CONTAINS:
                return cfBinary(left, before, Cf.Binary.Type.Contains, spelling, right);
            case IS_NOT:
            case NOT_EQUAL:
            case NEQ:
            case LTGT:
                return binary(left, before, J.Binary.Type.NotEqual, spelling, right);
            case NEQ_SIGN:
                return binary(left, before, J.Binary.Type.NotEqual, null, right);
            case EQ:
            case IS:
            case EQUAL:
                return binary(left, before, J.Binary.Type.Equal, spelling, right);
            case EQEQ:
                return binary(left, before, J.Binary.Type.Equal, null, right);
            case TEQ:
                return cfBinary(left, before, Cf.Binary.Type.Identical, null, right);
            case TNEQ:
                return cfBinary(left, before, Cf.Binary.Type.NotIdentical, null, right);
            case AMP:
                return cfBinary(left, before, Cf.Binary.Type.Concat, null, right);
            case PLUS:
                return binary(left, before, J.Binary.Type.Addition, null, right);
            case MINUS:
                return binary(left, before, J.Binary.Type.Subtraction, null, right);
            case PERCENT:
                return binary(left, before, J.Binary.Type.Modulo, null, right);
            case MOD:
                return binary(left, before, J.Binary.Type.Modulo, spelling, right);
            case BACKSLASH:
                return cfBinary(left, before, Cf.Binary.Type.IntegerDivision, null, right);
            case STAR:
                return binary(left, before, J.Binary.Type.Multiplication, null, right);
            case SLASH:
                return binary(left, before, J.Binary.Type.Division, null, right);
            case CARET:
                return cfBinary(left, before, Cf.Binary.Type.Power, null, right);
            default:
                throw error("an unknown operator", operator);
        }
    }

    private J.Unary unary(Token operator, ParserRuleContext operand) {
        Space prefix = sourceBefore(operator);
        J.Unary.Type type;
        String spelling = null;
        switch (operator.getType()) {
            case INCR:
                type = J.Unary.Type.PreIncrement;
                break;
            case DECR:
                type = J.Unary.Type.PreDecrement;
                break;
            case UNARY_MINUS:
                type = J.Unary.Type.Negative;
                break;
            case UNARY_PLUS:
                type = J.Unary.Type.Positive;
                break;
            case NOT:
            case TIGHT_NOT:
                spelling = operator.getText();
                type = J.Unary.Type.Not;
                break;
            default:
                type = J.Unary.Type.Not;
        }
        return new J.Unary(randomId(), prefix, spelling(spelling), new JLeftPadded<>(Space.EMPTY, type, Markers.EMPTY),
                expression(operand), null);
    }

    private Expression postfix(PostfixExpressionContext ctx) {
        List<PostfixOperatorContext> operators = ctx.postfixOperator();
        int from = 0;
        Expression expression;
        if (ctx.primary() instanceof NewPrimaryContext) {
            // `new Foo(...)` takes the arguments that follow on its line; after a line break, which
            // only a tag's expression allows, they call what the new returned
            J.NewClass newClass = newClass((NewPrimaryContext) ctx.primary());
            ArgumentsContext arguments = operators.isEmpty() ? null : operators.get(0).arguments();
            if (arguments != null && noLineBreakBefore(arguments.getStart())) {
                newClass = newClass.getPadding().withArguments(arguments(arguments));
                from = 1;
            }
            expression = newClass;
        } else {
            expression = primary(ctx.primary());
        }
        for (PostfixOperatorContext operator : operators.subList(from, operators.size())) {
            Token first = operator.getStart();
            if (operator.memberName() != null) {
                Space before = sourceBefore(first);
                Markers markers = first.getType() == SAFE_DOT ? Markers.build(singletonList(new SafeNavigation(randomId()))) :
                        first.getType() == DOUBLE_COLON ? Markers.build(singletonList(new DoubleColon(randomId()))) : Markers.EMPTY;
                Token name = operator.memberName().getStart();
                expression = new J.FieldAccess(randomId(), expression.getPrefix(), markers, expression.withPrefix(Space.EMPTY),
                        new JLeftPadded<>(before, identifier(sourceBefore(name), name.getText()), Markers.EMPTY), null);
            } else if (operator.arguments() != null) {
                expression = call(expression, operator.arguments());
            } else if (operator.INDEX_LBRACKET() != null) {
                Space before = sourceBefore(first);
                Expression index = expression(operator.expression());
                Space after = sourceBefore(operator.RBRACKET().getSymbol());
                expression = new J.ArrayAccess(randomId(), expression.getPrefix(), Markers.EMPTY, expression.withPrefix(Space.EMPTY),
                        new J.ArrayDimension(randomId(), before, Markers.EMPTY, new JRightPadded<>(index, after, Markers.EMPTY)), null);
            } else {
                Space before = sourceBefore(first);
                J.Unary.Type type = first.getType() == INCR ? J.Unary.Type.PostIncrement : J.Unary.Type.PostDecrement;
                expression = new J.Unary(randomId(), expression.getPrefix(), Markers.EMPTY,
                        new JLeftPadded<>(before, type, Markers.EMPTY), expression.withPrefix(Space.EMPTY), null);
            }
        }
        return expression;
    }

    private Expression call(Expression callee, ArgumentsContext ctx) {
        JContainer<Expression> arguments = arguments(ctx);
        if (callee instanceof J.Identifier) {
            return new J.MethodInvocation(randomId(), callee.getPrefix(), Markers.EMPTY, null, null,
                    ((J.Identifier) callee).withPrefix(Space.EMPTY), arguments, null);
        }
        if (callee instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) callee;
            return new J.MethodInvocation(randomId(), fieldAccess.getPrefix(), fieldAccess.getMarkers(),
                    new JRightPadded<>(fieldAccess.getTarget(), fieldAccess.getPadding().getName().getBefore(), Markers.EMPTY),
                    null, fieldAccess.getName(), arguments, null);
        }
        return new Cf.Invocation(randomId(), callee.getPrefix(), Markers.EMPTY, callee.withPrefix(Space.EMPTY), arguments, null);
    }

    private JContainer<Expression> arguments(ArgumentsContext ctx) {
        Space before = sourceBefore(ctx.CALL_LPAREN().getSymbol());
        Token close = ctx.RPAREN().getSymbol();
        if (ctx.argumentList() == null) {
            Space inner = sourceBefore(close);
            return JContainer.build(before, singletonList(new JRightPadded<>(new J.Empty(randomId(), inner, Markers.EMPTY),
                    Space.EMPTY, Markers.EMPTY)), Markers.EMPTY);
        }
        return JContainer.build(before, commaSeparated(ctx.argumentList().children, close, a -> argument((ArgumentContext) a)),
                Markers.EMPTY);
    }

    private Expression argument(ArgumentContext ctx) {
        if (ctx instanceof PositionalArgumentContext) {
            return expression(((PositionalArgumentContext) ctx).ternaryExpression());
        }
        NamedArgumentContext named = (NamedArgumentContext) ctx;
        Space prefix = prefix(named.getStart());
        Expression key;
        if (named.stringLiteral() != null) {
            key = string(named.stringLiteral());
        } else {
            Token word = named.anyWord().getStart();
            skip(word);
            key = identifier(Space.EMPTY, word.getText());
        }
        Token separator = named.COLON() != null ? named.COLON().getSymbol() : named.ASSIGN().getSymbol();
        Space beforeSeparator = sourceBefore(separator);
        return new Cf.KeyValue(randomId(), prefix, Markers.EMPTY, key, new JLeftPadded<>(beforeSeparator,
                separator.getType() == COLON ? Cf.KeyValue.Separator.Colon : Cf.KeyValue.Separator.Equals, Markers.EMPTY),
                expression(named.ternaryExpression()));
    }

    /**
     * Elements separated by commas, perhaps with a trailing one, or without one between named
     * arguments and between parameters on separate lines. The children are the elements, the commas
     * and, ignored, the brackets; {@code close} ends them.
     */
    private <T extends J> List<JRightPadded<T>> commaSeparated(List<ParseTree> children, Token close,
                                                               Function<ParserRuleContext, T> element) {
        List<JRightPadded<T>> elements = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            if (!(children.get(i) instanceof ParserRuleContext)) {
                continue;
            }
            T converted = element.apply((ParserRuleContext) children.get(i));
            ParseTree next = i + 1 < children.size() ? children.get(i + 1) : null;
            if (next instanceof TerminalNode && ((TerminalNode) next).getSymbol().getType() == COMMA) {
                Space after = sourceBefore(((TerminalNode) next).getSymbol());
                if (i + 2 < children.size() && children.get(i + 2) instanceof ParserRuleContext) {
                    elements.add(new JRightPadded<>(converted, after, Markers.EMPTY));
                } else {
                    elements.add(new JRightPadded<>(converted, after, Markers.build(singletonList(new TrailingComma(randomId(), prefix(close))))));
                }
                i++;
            } else if (next instanceof ParserRuleContext) {
                elements.add(new JRightPadded<>(converted, Space.EMPTY, Markers.build(singletonList(new OmitComma(randomId())))));
            } else {
                elements.add(new JRightPadded<>(converted, prefix(close), Markers.EMPTY));
            }
        }
        skip(close);
        return elements;
    }

    private Expression primary(PrimaryContext ctx) {
        if (ctx instanceof IdentifierPrimaryContext) {
            Token name = ctx.getStart();
            return identifier(sourceBefore(name), name.getText());
        } else if (ctx instanceof NumberPrimaryContext) {
            Token number = ctx.getStart();
            return number(sourceBefore(number), number.getText());
        } else if (ctx instanceof BooleanPrimaryContext) {
            Token word = ctx.getStart();
            return new J.Literal(randomId(), sourceBefore(word), Markers.EMPTY, "true".equalsIgnoreCase(word.getText()),
                    word.getText(), null, JavaType.Primitive.Boolean);
        } else if (ctx instanceof NullPrimaryContext) {
            Token word = ctx.getStart();
            return new J.Literal(randomId(), sourceBefore(word), Markers.EMPTY, null, word.getText(), null, JavaType.Primitive.Null);
        } else if (ctx instanceof StringPrimaryContext) {
            return string(((StringPrimaryContext) ctx).stringLiteral());
        } else if (ctx instanceof InterpolationPrimaryContext) {
            return interpolation(((InterpolationPrimaryContext) ctx).interpolation());
        } else if (ctx instanceof ArrowFunctionPrimaryContext) {
            return arrowFunction(((ArrowFunctionPrimaryContext) ctx).arrowFunction());
        } else if (ctx instanceof ParenthesizedPrimaryContext) {
            ParenthesizedPrimaryContext parentheses = (ParenthesizedPrimaryContext) ctx;
            Space prefix = sourceBefore(parentheses.LPAREN().getSymbol());
            Expression inner = expression(parentheses.expression());
            Space after = sourceBefore(parentheses.RPAREN().getSymbol());
            return new J.Parentheses<>(randomId(), prefix, Markers.EMPTY, new JRightPadded<>(inner, after, Markers.EMPTY));
        } else if (ctx instanceof EmptyBracketPrimaryContext) {
            return emptyBracket((EmptyBracketPrimaryContext) ctx);
        } else if (ctx instanceof OrderedStructPrimaryContext) {
            Space prefix = sourceBefore(ctx.getStart());
            return new Cf.StructLiteral(randomId(), prefix, Markers.EMPTY, true, JContainer.build(Space.EMPTY,
                    commaSeparated(ctx.children, ctx.getStop(), e -> structEntry((StructEntryContext) e)), Markers.EMPTY), null);
        } else if (ctx instanceof ArrayPrimaryContext) {
            Space prefix = sourceBefore(ctx.getStart());
            return arrayLiteral(prefix, commaSeparated(ctx.children, ctx.getStop(), this::expression));
        } else if (ctx instanceof EmptyStructPrimaryContext) {
            Space prefix = sourceBefore(ctx.getStart());
            Space inner = sourceBefore(ctx.getStop());
            return new Cf.StructLiteral(randomId(), prefix, Markers.EMPTY, false, JContainer.build(Space.EMPTY,
                    singletonList(new JRightPadded<>(new J.Empty(randomId(), inner, Markers.EMPTY), Space.EMPTY, Markers.EMPTY)),
                    Markers.EMPTY), null);
        } else if (ctx instanceof StructPrimaryContext) {
            Space prefix = sourceBefore(ctx.getStart());
            return new Cf.StructLiteral(randomId(), prefix, Markers.EMPTY, false, JContainer.build(Space.EMPTY,
                    commaSeparated(ctx.children, ctx.getStop(), e -> structEntry((StructEntryContext) e)), Markers.EMPTY), null);
        } else if (ctx instanceof ClosurePrimaryContext) {
            return closure(((ClosurePrimaryContext) ctx).closure());
        }
        VarPrimaryContext var = (VarPrimaryContext) ctx;
        Space prefix = prefix(var.getStart());
        String keyword = keyword(var.VAR());
        return varAssignment(prefix, keyword, expression(var.expression()), var);
    }

    /**
     * {@code var} before an assignment to a member, or inside an expression, which a
     * {@link J.VariableDeclarations} cannot hold.
     */
    private Expression varAssignment(Space prefix, String keyword, Expression assignment, VarPrimaryContext ctx) {
        Markers var = Markers.build(singletonList(new VarKeyword(randomId(), keyword)));
        if (assignment instanceof J.Assignment) {
            J.Assignment a = (J.Assignment) assignment;
            return a.withVariable(a.getVariable().withPrefix(a.getPrefix())).withPrefix(prefix).withMarkers(var);
        }
        if (assignment instanceof J.AssignmentOperation) {
            J.AssignmentOperation a = (J.AssignmentOperation) assignment;
            return a.withVariable(a.getVariable().withPrefix(a.getPrefix())).withPrefix(prefix).withMarkers(var);
        }
        if (assignment instanceof Cf.AssignmentOperation) {
            Cf.AssignmentOperation a = (Cf.AssignmentOperation) assignment;
            return a.withVariable(a.getVariable().withPrefix(a.getPrefix())).withPrefix(prefix).withMarkers(var);
        }
        throw error("a var that assigns nothing", ctx.getStart());
    }

    /**
     * {@code []} is an empty array; {@code [:]} and {@code [=]} an empty ordered struct.
     */
    private Expression emptyBracket(EmptyBracketPrimaryContext ctx) {
        Space prefix = sourceBefore(ctx.LBRACKET().getSymbol());
        TerminalNode separator = ctx.COLON() != null ? ctx.COLON() : ctx.ASSIGN();
        if (separator == null) {
            Space inner = sourceBefore(ctx.RBRACKET().getSymbol());
            return arrayLiteral(prefix, singletonList(new JRightPadded<>(new J.Empty(randomId(), inner, Markers.EMPTY),
                    Space.EMPTY, Markers.EMPTY)));
        }
        // the entry starts at its separator and is followed by the space before the bracket, as any last entry is
        Space beforeSeparator = sourceBefore(separator.getSymbol());
        Space afterSeparator = sourceBefore(ctx.RBRACKET().getSymbol());
        Cf.KeyValue empty = new Cf.KeyValue(randomId(), beforeSeparator, Markers.EMPTY, new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY),
                new JLeftPadded<>(Space.EMPTY, separator.getSymbol().getType() == COLON ? Cf.KeyValue.Separator.Colon :
                        Cf.KeyValue.Separator.Equals, Markers.EMPTY), new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY));
        return new Cf.StructLiteral(randomId(), prefix, Markers.EMPTY, true, JContainer.build(Space.EMPTY,
                singletonList(new JRightPadded<>(empty, afterSeparator, Markers.EMPTY)), Markers.EMPTY), null);
    }

    /**
     * {@code [1, 2, 3]}, an array with no type written before it, as JavaScript's is.
     */
    private static J.NewArray arrayLiteral(Space prefix, List<JRightPadded<Expression>> elements) {
        return new J.NewArray(randomId(), prefix, Markers.EMPTY, null, emptyList(),
                JContainer.build(Space.EMPTY, elements, Markers.EMPTY), null);
    }

    private Cf.KeyValue structEntry(StructEntryContext ctx) {
        Space prefix = prefix(ctx.getStart());
        StructKeyContext keyContext = ctx.structKey();
        Expression key = keyContext.stringLiteral() != null ? string(keyContext.stringLiteral()) :
                (Expression) dottedName(keyContext.children);
        Token separator = ctx.getChild(1) instanceof TerminalNode ? ((TerminalNode) ctx.getChild(1)).getSymbol() : null;
        Space beforeSeparator = sourceBefore(Objects.requireNonNull(separator));
        // a struct value may itself be an assignment, {info: data = {}}
        return new Cf.KeyValue(randomId(), prefix, Markers.EMPTY, key, new JLeftPadded<>(beforeSeparator,
                separator.getType() == COLON ? Cf.KeyValue.Separator.Colon : Cf.KeyValue.Separator.Equals, Markers.EMPTY),
                expression(ctx.expression()));
    }

    private Expression closure(ClosureContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.FUNCTION());
        Space parametersPrefix = prefix(ctx.parameterList().getStart());
        J.Lambda.Parameters parameters = new J.Lambda.Parameters(randomId(), parametersPrefix, Markers.EMPTY, true,
                parameters(ctx.parameterList()));
        List<J.Annotation> attributes = annotations(headerAttributes(ctx.headerAttribute()), LanguageAttributes.FUNCTION);
        J.Block body = block(ctx.block());
        J.Lambda lambda = new J.Lambda(randomId(), prefix, Markers.build(singletonList(new FunctionKeyword(randomId(), Space.EMPTY, keyword))),
                parameters, Space.EMPTY, body, null);
        return attributes.isEmpty() ? lambda :
                new Cf.Closure(randomId(), prefix, Markers.EMPTY, lambda.withPrefix(Space.EMPTY), attributes);
    }

    private J.Lambda arrowFunction(ArrowFunctionContext ctx) {
        Space prefix = prefix(ctx.getStart());
        J.Lambda.Parameters parameters;
        if (ctx.parameterList() != null) {
            parameters = new J.Lambda.Parameters(randomId(), Space.EMPTY, Markers.EMPTY, true, parameters(ctx.parameterList()));
        } else {
            Token name = ctx.anyWord().getStart();
            skip(name);
            J.VariableDeclarations parameter = new J.VariableDeclarations(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(),
                    emptyList(), null, null, singletonList(new JRightPadded<>(namedVariable(Space.EMPTY, name.getText(), null),
                    Space.EMPTY, Markers.EMPTY)));
            parameters = new J.Lambda.Parameters(randomId(), Space.EMPTY, Markers.EMPTY, false,
                    singletonList(new JRightPadded<>(parameter, Space.EMPTY, Markers.EMPTY)));
        }
        Space arrow = sourceBefore(ctx.ARROW().getSymbol());
        ArrowBodyContext bodyContext = ctx.arrowBody();
        J body;
        if (bodyContext.block() != null) {
            body = block(bodyContext.block());
        } else if (bodyContext.RETURN() != null || bodyContext.BARE_RETURN() != null) {
            // `(s) => return {...}` is accepted in place of an expression body
            body = returnStatement(bodyContext.getStart(), bodyContext.expression());
        } else {
            body = expression(bodyContext.expression());
        }
        return new J.Lambda(randomId(), prefix, Markers.EMPTY, parameters, arrow, body, null);
    }

    private J.NewClass newClass(NewPrimaryContext ctx) {
        Space prefix = prefix(ctx.getStart());
        String keyword = keyword(ctx.NEW());
        TypeTree clazz = typeName(ctx.typeName());
        JContainer<Expression> arguments = JContainer.<Expression>empty()
                .withMarkers(Markers.build(singletonList(new OmitParentheses(randomId()))));
        return new J.NewClass(randomId(), prefix, keywordMarkers(keyword, "new"), null, Space.EMPTY, clazz, arguments, null, null);
    }

    /**
     * Whether the whitespace from the cursor to a token, up to any comment, holds no line break.
     */
    private boolean noLineBreakBefore(Token token) {
        int end = start(token);
        for (int i = cursor; i < end && isWhitespace(source.charAt(i)); i++) {
            if (source.charAt(i) == '\n') {
                return false;
            }
        }
        return true;
    }

    /**
     * A dotted name or a string naming a type, as the component of a {@code new} or the type of a
     * catch.
     */
    private TypeTree typeName(TypeNameContext ctx) {
        if (ctx.stringLiteral() != null) {
            Space prefix = prefix(ctx.getStart());
            return new Cf.TypeLiteral(randomId(), prefix, Markers.EMPTY, string(ctx.stringLiteral()), null);
        }
        return dottedName(ctx.dottedName().children);
    }

    /**
     * A name that may be dotted, {@code com.example.Widget}, from parse tree children that are words
     * and dots. It ends at the first child that is neither.
     */
    private TypeTree dottedName(List<ParseTree> children) {
        TypeTree name = null;
        Space before = Space.EMPTY;
        for (ParseTree child : children) {
            Token token = child instanceof TerminalNode ? ((TerminalNode) child).getSymbol() : ((ParserRuleContext) child).getStart();
            if (token.getType() == DOT) {
                before = sourceBefore(token);
                continue;
            }
            if (child instanceof TerminalNode && token.getType() != TYPE_WORD && token.getType() != NUMBER) {
                break;
            }
            J.Identifier identifier = identifier(sourceBefore(token), token.getText());
            name = name == null ? identifier : new J.FieldAccess(randomId(), name.getPrefix(), Markers.EMPTY,
                    ((Expression) name).withPrefix(Space.EMPTY), new JLeftPadded<>(before, identifier, Markers.EMPTY), null);
        }
        return Objects.requireNonNull(name);
    }

    /**
     * {@code []} after a type, {@code string[] function names()}.
     */
    private TypeTree arrayType(TypeTree elementType, TerminalNode open, TerminalNode close) {
        Space before = sourceBefore(open.getSymbol());
        Space inner = sourceBefore(close.getSymbol());
        return new J.ArrayType(randomId(), elementType.getPrefix(), Markers.EMPTY, elementType.withPrefix(Space.EMPTY),
                null, new JLeftPadded<>(before, inner, Markers.EMPTY), null);
    }

    /**
     * A quoted string. Doubled quotes and {@code ##} stand for themselves, and {@code #expression#}
     * is interpolated, so a string with any interpolation is a {@link Cf.StringTemplate}.
     */
    private Expression string(StringLiteralContext ctx) {
        Token open = ctx.getStart();
        Space prefix = sourceBefore(open);
        String quote = open.getText();
        List<J> parts = new ArrayList<>();
        StringBuilder raw = new StringBuilder();
        boolean interpolated = false;
        for (int i = 1; i < ctx.getChildCount() - 1; i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof TerminalNode) {
                Token text = ((TerminalNode) child).getSymbol();
                raw.append(text.getText());
                skip(text);
            } else {
                if (raw.length() > 0) {
                    parts.add(segment(raw.toString(), unescape(raw.toString(), quote)));
                    raw.setLength(0);
                }
                parts.add(interpolation((InterpolationContext) child));
                interpolated = true;
            }
        }
        skip(ctx.getStop());
        if (!interpolated) {
            return new J.Literal(randomId(), prefix, Markers.EMPTY, unescape(raw.toString(), quote), quote + raw + quote, null,
                    JavaType.Primitive.String);
        }
        if (raw.length() > 0) {
            parts.add(segment(raw.toString(), unescape(raw.toString(), quote)));
        }
        return new Cf.StringTemplate(randomId(), prefix, Markers.EMPTY, quote, parts, JavaType.Primitive.String);
    }

    private static String unescape(String raw, String quote) {
        return raw.replace(quote + quote, quote).replace("##", "#");
    }

    private static J.Literal segment(String raw, String value) {
        return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, value, raw, null, JavaType.Primitive.String);
    }

    private static J.Literal number(Space prefix, String text) {
        boolean decimal = text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0;
        if (!decimal) {
            try {
                return new J.Literal(randomId(), prefix, Markers.EMPTY, Integer.parseInt(text), text, null, JavaType.Primitive.Int);
            } catch (NumberFormatException e) {
                try {
                    return new J.Literal(randomId(), prefix, Markers.EMPTY, Long.parseLong(text), text, null, JavaType.Primitive.Long);
                } catch (NumberFormatException ignored) {
                    // falls through to a double
                }
            }
        }
        return new J.Literal(randomId(), prefix, Markers.EMPTY, Double.parseDouble(text), text, null, JavaType.Primitive.Double);
    }

    // ------------------------------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------------------------------

    private static J.Identifier identifier(Space prefix, String name) {
        return new J.Identifier(randomId(), prefix, Markers.EMPTY, emptyList(), name, null, null);
    }

    private static J.VariableDeclarations.NamedVariable namedVariable(Space prefix, String name,
                                                                      @Nullable JLeftPadded<Expression> initializer) {
        return new J.VariableDeclarations.NamedVariable(randomId(), prefix, Markers.EMPTY, identifier(Space.EMPTY, name),
                emptyList(), initializer, null);
    }

    private static J.Modifier modifier(Space prefix, String keyword, J.Modifier.Type type) {
        return new J.Modifier(randomId(), prefix, Markers.EMPTY, keyword, type, emptyList());
    }

    private static Markers keywordMarkers(String written, String canonical) {
        return written.equals(canonical) ? Markers.EMPTY :
                Markers.build(singletonList(new KeywordSpelling(randomId(), written)));
    }

    private static Markers semicolon() {
        return Markers.build(singletonList(new Semicolon(randomId())));
    }

    private static Expression binary(Expression left, Space before, J.Binary.Type type, @Nullable String spelling, Expression right) {
        return new J.Binary(randomId(), left.getPrefix(), spelling(spelling), left.withPrefix(Space.EMPTY),
                new JLeftPadded<>(before, type, Markers.EMPTY), right, null);
    }

    private static Expression cfBinary(Expression left, Space before, Cf.Binary.Type type, @Nullable String spelling, Expression right) {
        return new Cf.Binary(randomId(), left.getPrefix(), spelling(spelling), left.withPrefix(Space.EMPTY),
                new JLeftPadded<>(before, type, Markers.EMPTY), right, type == Cf.Binary.Type.Concat ? JavaType.Primitive.String : null);
    }

    private static Markers spelling(@Nullable String spelling) {
        return spelling == null ? Markers.EMPTY : Markers.build(singletonList(new OperatorSpelling(randomId(), spelling)));
    }

    // ------------------------------------------------------------------------------------------
    // The cursor
    // ------------------------------------------------------------------------------------------

    private int charIndex(int codePointIndex) {
        return charIndex == null ? codePointIndex : charIndex[codePointIndex];
    }

    private int start(Token token) {
        return charIndex(token.getStartIndex());
    }

    /**
     * The whitespace and comments from the cursor to a token, which the cursor moves to.
     */
    private Space prefix(Token token) {
        int start = start(token);
        if (start < cursor) {
            return Space.EMPTY;
        }
        Space prefix = space(cursor, start);
        cursor = start;
        return prefix;
    }

    private Space prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private void skip(Token token) {
        cursor = Math.max(cursor, charIndex(token.getStopIndex() + 1));
    }

    private Space sourceBefore(Token token) {
        Space prefix = prefix(token);
        skip(token);
        return prefix;
    }

    private String keyword(TerminalNode node) {
        Token token = node.getSymbol();
        skip(token);
        return token.getText();
    }

    /**
     * Whitespace with the comments in it: {@code //} and {@code /* *}{@code /} in script, and
     * {@code <!--- --->} in a tag or, as Lucee allows, in script. The lexer saw to it that nothing
     * else lies between two tokens.
     */
    private Space space(int start, int end) {
        if (start >= end) {
            return Space.EMPTY;
        }
        int i = start;
        while (i < end && isWhitespace(source.charAt(i))) {
            i++;
        }
        String whitespace = source.substring(start, i);
        if (i == end) {
            return Space.build(whitespace, emptyList());
        }
        List<Comment> comments = new ArrayList<>();
        while (i < end) {
            Comment comment;
            int commentEnd;
            if (source.startsWith("<!---", i)) {
                Integer lexed = tagCommentEnds.get(i);
                if (lexed == null || lexed > end) {
                    throw new IllegalStateException("A comment the lexer did not see at " + i + ": " +
                                                    source.substring(i, Math.min(end, i + 20)));
                }
                commentEnd = lexed;
                comment = new TagComment(source.substring(i + 5, commentEnd - 4), "", Markers.EMPTY);
            } else if (source.startsWith("/*", i)) {
                commentEnd = source.indexOf("*/", i + 2) + 2;
                comment = new TextComment(true, source.substring(i + 2, commentEnd - 2), "", Markers.EMPTY);
            } else if (source.startsWith("//", i)) {
                commentEnd = i;
                while (commentEnd < end && source.charAt(commentEnd) != '\n' && source.charAt(commentEnd) != '\r') {
                    commentEnd++;
                }
                comment = new TextComment(false, source.substring(i + 2, commentEnd), "", Markers.EMPTY);
            } else {
                throw new IllegalStateException("Unexpected text between tokens at " + i + ": " +
                                                source.substring(i, Math.min(end, i + 20)));
            }
            int suffixEnd = commentEnd;
            while (suffixEnd < end && isWhitespace(source.charAt(suffixEnd))) {
                suffixEnd++;
            }
            comments.add(comment.withSuffix(source.substring(commentEnd, suffixEnd)));
            i = suffixEnd;
        }
        return Space.build(whitespace, comments);
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B' || c == '\u00A0' || c == '\uFEFF';
    }

    private ColdFusionParsingException error(String what, Token at) {
        return new ColdFusionParsingException(what, where(path, source, start(at)));
    }

    /**
     * Where in the source a problem is, as {@code path:line:column near 'text'}.
     */
    public static String where(Path path, String source, int index) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < index && i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\n' || (c == '\r' && (i + 1 >= source.length() || source.charAt(i + 1) != '\n'))) {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        String near = source.substring(Math.min(index, source.length()), Math.min(index + 40, source.length()))
                .replace("\r", "\\r").replace("\n", "\\n");
        return path + ":" + line + ":" + column + " near '" + near + "'";
    }
}
