/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.internal;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.openrewrite.FileAttributes;
import org.openrewrite.hcl.internal.grammar.HCLParser;
import org.openrewrite.hcl.internal.grammar.HCLParserBaseVisitor;
import org.openrewrite.hcl.tree.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@SuppressWarnings("ConstantConditions")
public class HclParserVisitor extends HCLParserBaseVisitor<Hcl> {
    private final Path path;
    private final String source;
    private final Charset charset;
    private final boolean charsetBomMarked;

    @Nullable
    private final FileAttributes fileAttributes;

    private int cursor = 0;

    public HclParserVisitor(Path path, String source, Charset charset, boolean charsetBomMarked, @Nullable FileAttributes fileAttributes) {
        this.path = path;
        this.source = source;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
        this.fileAttributes = fileAttributes;
    }

    @Override
    public Hcl visitAttribute(HCLParser.AttributeContext ctx) {
        return convert(ctx, (c, prefix) -> new Hcl.Attribute(
                randomId(),
                Space.format(prefix),
                Markers.EMPTY,
                visitIdentifier(c.Identifier()),
                new HclLeftPadded<>(
                        sourceBefore("="),
                        Hcl.Attribute.Type.Assignment,
                        Markers.EMPTY
                ),
                (Expression) visit(c.expression()),
                null
        ));
    }

    @Override
    public Hcl visitAttributeAccessExpression(HCLParser.AttributeAccessExpressionContext ctx) {
        return convert(ctx, (c, prefix) -> new Hcl.AttributeAccess(
                randomId(),
                Space.format(prefix),
                Markers.EMPTY,
                (Expression) visit(c.exprTerm()),
                new HclLeftPadded<>(
                        sourceBefore("."),
                        visitIdentifier(c.getAttr().Identifier()),
                        Markers.EMPTY
                )
        ));
    }

    @Override
    public Hcl visitBinaryOp(HCLParser.BinaryOpContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Expression left, right;

            // left can be unaryOp or exprTerm, right can be another operation or exprTerm
            if (c.unaryOp() != null) {
                left = (Expression) visit(c.unaryOp());
            }else {
                left = (Expression) visit(c.exprTerm(0));
            }

            Hcl.Binary.Type op;
            switch (ctx.binaryOperator().getText()) {
                case "+":
                    op = Hcl.Binary.Type.Addition;
                    break;
                case "-":
                    op = Hcl.Binary.Type.Subtraction;
                    break;
                case "*":
                    op = Hcl.Binary.Type.Multiplication;
                    break;
                case "/":
                    op = Hcl.Binary.Type.Division;
                    break;
                case "%":
                    op = Hcl.Binary.Type.Modulo;
                    break;
                case "||":
                    op = Hcl.Binary.Type.Or;
                    break;
                case "&&":
                    op = Hcl.Binary.Type.And;
                    break;
                case "<":
                    op = Hcl.Binary.Type.LessThan;
                    break;
                case "<=":
                    op = Hcl.Binary.Type.LessThanOrEqual;
                    break;
                case ">":
                    op = Hcl.Binary.Type.GreaterThan;
                    break;
                case ">=":
                    op = Hcl.Binary.Type.GreaterThanOrEqual;
                    break;
                case "==":
                    op = Hcl.Binary.Type.Equal;
                    break;
                case "!=":
                default:
                    op = Hcl.Binary.Type.NotEqual;
                    break;
            }
            Space opPrefix = Space.format(prefix(ctx.binaryOperator()));
            cursor = ctx.binaryOperator().getStop().getStopIndex() + 1;

            if (c.unaryOp() != null) {
                right = (Expression) visit(c.operation() != null ? c.operation() : c.exprTerm(0));
            }else {
                right = (Expression) visit(c.operation() != null ? c.operation() : c.exprTerm(1));
            }

            return new Hcl.Binary(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    left,
                    new HclLeftPadded<>(opPrefix, op, Markers.EMPTY),
                    right
            );
        });
    }

    @Override
    public Hcl visitBlockExpr(HCLParser.BlockExprContext ctx) {
        return convert(ctx, (c, prefix) -> new Hcl.Block(
                randomId(),
                Space.format(prefix),
                Markers.EMPTY,
                null,
                emptyList(),
                sourceBefore("{"),
                c.body().bodyContent().stream()
                        .map(bc -> (BodyContent) visit(bc))
                        .collect(toList()),
                sourceBefore("}")));
    }

    @Override
    public Hcl visitBlock(HCLParser.BlockContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Hcl.Identifier type = visitIdentifier(ctx.Identifier());
            List<Label> labels = c.blockLabel().stream()
                    .map(l -> l.Identifier() != null ?
                            visitIdentifier(l.Identifier()) :
                            (Label) visit(l))
                    .collect(toList());
            Hcl.Block blockExpr = ((Hcl.Block) visit(c.blockExpr()))
                    .withType(type)
                    .withLabels(labels);
            return blockExpr
                    .withOpen(blockExpr.getPrefix())
                    .withPrefix(Space.format(prefix));
        });
    }

    @Override
    public Hcl visitBlockLabel(HCLParser.BlockLabelContext ctx) {
        return convert(ctx, (c, prefix) -> {
            if (ctx.Identifier() != null) {
                return visitIdentifier(ctx.Identifier());
            }
            TerminalNode stringLiteral = ctx.stringLiteral().TemplateStringLiteral();
            skip(stringLiteral);
            sourceBefore("\"");
            return new Hcl.Literal(randomId(), Space.format(prefix), Markers.EMPTY, stringLiteral.getText(),
                    "\"" + stringLiteral.getText() + '"');
        });
    }

    @Override
    public Hcl visitConditionalExpression(HCLParser.ConditionalExpressionContext ctx) {
        return convert(ctx, (c, prefix) -> new Hcl.Conditional(randomId(), Space.format(prefix), Markers.EMPTY,
                (Expression) visit(c.expression(0)),
                new HclLeftPadded<>(sourceBefore("?"), (Expression) visit(c.expression(1)), Markers.EMPTY),
                new HclLeftPadded<>(sourceBefore(":"), (Expression) visit(c.expression(2)), Markers.EMPTY)
        ));
    }

    @Override
    public Hcl visitConfigFile(HCLParser.ConfigFileContext ctx) {
        return convert(ctx, (c, prefix) -> new Hcl.ConfigFile(
                randomId(),
                path,
                fileAttributes,
                Space.format(prefix),
                Markers.EMPTY,
                charset.name(),
                charsetBomMarked,
                null,
                c.body().bodyContent().stream()
                        .map(bc -> (BodyContent) visit(bc))
                        .collect(toList()),
                Space.format(source.substring(cursor)))
        );
    }

    @Override
    public Hcl visitForIntro(HCLParser.ForIntroContext ctx) {
        return convert(ctx, (c, prefix) -> {
            List<HclRightPadded<Hcl.Identifier>> mappedVariables = new ArrayList<>();
            List<TerminalNode> variables = ctx.Identifier();
            int lastFor = prefix.lastIndexOf("for");
            String beforeFor = prefix.substring(0, lastFor);
            String afterFor = prefix.substring(lastFor + 3);

            for (int i = 0; i < variables.size(); i++) {
                TerminalNode variable = variables.get(i);
                Hcl.Identifier expression = visitIdentifier(variable);
                if (i == 0) {
                    expression = expression.withPrefix(Space.format(afterFor));
                }
                mappedVariables.add(HclRightPadded.build(expression)
                        .withAfter(sourceBefore(i == variables.size() - 1 ? "in" : ",")));
            }

            return new Hcl.ForIntro(
                    randomId(),
                    Space.format(beforeFor),
                    Markers.EMPTY,
                    HclContainer.build(Space.EMPTY, mappedVariables, Markers.EMPTY),
                    (Expression) visit(ctx.expression())
            );
        });
    }

    @Override
    public Hcl visitForObjectExpr(HCLParser.ForObjectExprContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("{");
            return new Hcl.ForObject(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    (Hcl.ForIntro) visit(ctx.forIntro()),
                    new HclLeftPadded<>(
                            sourceBefore(":"),
                            (Expression) visit(ctx.expression().get(0)),
                            Markers.EMPTY
                    ),
                    new HclLeftPadded<>(
                            sourceBefore("=>"),
                            (Expression) visit(ctx.expression().get(1)),
                            Markers.EMPTY
                    ),
                    ctx.ELLIPSIS() == null ?
                            null :
                            new Hcl.Empty(randomId(), sourceBefore("..."), Markers.EMPTY),
                    ctx.forCond() == null ?
                            null :
                            new HclLeftPadded<>(
                                    sourceBefore("if"),
                                    (Expression) visit(ctx.forCond().expression()),
                                    Markers.EMPTY
                            ),
                    sourceBefore("}")
            );
        });
    }

    @Override
    public Hcl visitForTupleExpr(HCLParser.ForTupleExprContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("[");
            return new Hcl.ForTuple(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    (Hcl.ForIntro) visit(ctx.forIntro()),
                    new HclLeftPadded<>(
                            sourceBefore(":"),
                            (Expression) visit(ctx.expression()),
                            Markers.EMPTY
                    ),
                    ctx.forCond() == null ?
                            null :
                            new HclLeftPadded<>(
                                    sourceBefore("if"),
                                    (Expression) visit(ctx.forCond().expression()),
                                    Markers.EMPTY
                            ),
                    sourceBefore("]")
            );
        });
    }

    @Override
    public Hcl visitFunctionCall(HCLParser.FunctionCallContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Hcl.Identifier name = visitIdentifier(ctx.Identifier());

            Space argPrefix = sourceBefore("(");
            List<HclRightPadded<Expression>> mappedArgs = new ArrayList<>();
            if (ctx.arguments() != null) {
                List<HCLParser.ExpressionContext> args = ctx.arguments().expression();
                for (int i = 0; i < args.size(); i++) {
                    HCLParser.ExpressionContext arg = args.get(i);
                    mappedArgs.add(HclRightPadded.build((Expression) visit(arg))
                            .withAfter(sourceBefore(i == args.size() - 1 ? ")" : ",")));
                }
            } else {
                mappedArgs = singletonList(HclRightPadded.build((Expression) new Hcl.Empty(randomId(), Space.EMPTY, Markers.EMPTY))
                        .withAfter(sourceBefore(")")));
            }

            return new Hcl.FunctionCall(randomId(), Space.format(prefix), Markers.EMPTY, name,
                    HclContainer.build(argPrefix, mappedArgs, Markers.EMPTY));
        });
    }

    @Override
    public Hcl visitHeredoc(HCLParser.HeredocContext ctx) {
        return convert(ctx, (c, prefix) -> {
            String arrow = ctx.HEREDOC_START().getText();
            sourceBefore(arrow);
            Hcl.Identifier delimiter = visitIdentifier(ctx.Identifier(0));
            List<Expression> expressions = visitHeredocTemplateExpressions(ctx.heredocTemplatePart());

            return new Hcl.HeredocTemplate(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    arrow,
                    delimiter,
                    expressions,
                    // Identifier(1) may have leading whitespace
                    sourceBefore(ctx.Identifier(0).getText())
            );
        });
    }

    @NonNull
    private List<Expression> visitHeredocTemplateExpressions(List<HCLParser.HeredocTemplatePartContext> ctx) {
        List<Expression> expressions = new ArrayList<>(ctx.size());
        for (HCLParser.HeredocTemplatePartContext part : ctx) {
            if (part.heredocLiteral() != null) {
                Space prefix = Space.format(prefix(part.heredocLiteral()));
                String value = part.heredocLiteral().getText();
                cursor = part.heredocLiteral().getStop().getStopIndex() + 1;
                expressions.add(new Hcl.Literal(randomId(), prefix, Markers.EMPTY, value, value));
            } else if (part.templateInterpolation() != null) {
                Space prefix = Space.format(prefix(part.templateInterpolation()));
                expressions.add(visit(part.templateInterpolation()).withPrefix(prefix));
            } else {
                throw new IllegalStateException("Unsupported terminal node");
            }
        }
        return expressions;
    }

    @Override
    public Hcl visitIndexAccessExpression(HCLParser.IndexAccessExpressionContext ctx) {
        return convert(ctx, (c, prefix) -> new Hcl.Index(
                randomId(),
                Space.format(prefix),
                Markers.EMPTY,
                (Expression) visit(ctx.exprTerm()),
                new Hcl.Index.Position(
                        randomId(),
                        sourceBefore("["),
                        Markers.EMPTY,
                        HclRightPadded.build((Expression) visit(ctx.index().expression()))
                                .withAfter(sourceBefore("]"))
                )
        ));
    }

    @Override
    public Hcl visitLiteralValue(HCLParser.LiteralValueContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Object value;
            String valueSource;
            if (c.BooleanLiteral() != null) {
                valueSource = c.BooleanLiteral().getText();
                value = Boolean.parseBoolean(valueSource);
            } else if (c.NumericLiteral() != null) {
                valueSource = c.NumericLiteral().getText();
                if (valueSource.contains(".")) {
                    value = Double.parseDouble(valueSource);
                } else {
                    value = Long.parseLong(valueSource);
                    if (((Long) value) < Integer.MAX_VALUE) {
                        value = ((Long) value).intValue();
                    }
                }
            } else if (c.NULL() != null) {
                valueSource = c.NULL().getText();
                value = null;
            } else {
                throw new IllegalStateException("Unsupported terminal node");
            }

            return new Hcl.Literal(randomId(), Space.format(prefix), Markers.EMPTY, value, valueSource);
        });
    }

    @Override
    public Hcl visitObject(HCLParser.ObjectContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Space tuplePrefix = sourceBefore("{");
            List<HclRightPadded<Expression>> mappedValues = new ArrayList<>();
            List<HCLParser.ObjectelemContext> values = ctx.objectelem();
            for (int i = 0; i < values.size(); i++) {
                HCLParser.ObjectelemContext value = values.get(i);
                mappedValues.add(HclRightPadded.build((Expression) visit(value))
                        .withAfter(i == values.size() - 1 ? sourceBefore("}") : Space.EMPTY));
            }

            return new Hcl.ObjectValue(randomId(), Space.format(prefix), Markers.EMPTY,
                    HclContainer.build(tuplePrefix, mappedValues, Markers.EMPTY));
        });
    }

    @Override
    public Hcl visitObjectelem(HCLParser.ObjectelemContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Expression name;
            if (ctx.QUOTE(0) != null) {
                Space quotePrefix = sourceBefore("\"");
                List<Expression> expressions = visitTemplateExpressions(ctx.quotedTemplatePart());
                name = new Hcl.QuotedTemplate(randomId(), quotePrefix, Markers.EMPTY, expressions);
                skip(ctx.QUOTE(1));
            } else {
                Space parenthesesPrefix = null;
                if (ctx.LPAREN() != null) {
                    parenthesesPrefix = sourceBefore("(");
                }
                name = visitIdentifier(ctx.Identifier());
                if (ctx.RPAREN() != null) {
                    name = new Hcl.Parentheses(randomId(), parenthesesPrefix, Markers.EMPTY,
                            HclRightPadded.build(name).withAfter(sourceBefore(")")));
                }
            }

            return new Hcl.Attribute(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    name,
                    new HclLeftPadded<>(
                            c.ASSIGN() != null ? sourceBefore("=") : sourceBefore(":"),
                            c.ASSIGN() != null ? Hcl.Attribute.Type.Assignment : Hcl.Attribute.Type.ObjectElement,
                            Markers.EMPTY
                    ),
                    (Expression) visit(c.expression()),
                    ctx.COMMA() == null ?
                            null :
                            new Hcl.Empty(randomId(), sourceBefore(","), Markers.EMPTY)
            );
        });
    }

    @Override
    public Hcl visitParentheticalExpression(HCLParser.ParentheticalExpressionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            sourceBefore("(");
            return new Hcl.Parentheses(randomId(), Space.format(prefix), Markers.EMPTY,
                    HclRightPadded.build((Expression) visit(c.expression()))
                            .withAfter(sourceBefore(")")));
        });
    }

    @Override
    public Hcl visitSplatExpression(HCLParser.SplatExpressionContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Expression select = (Expression) visit(ctx.exprTerm());

            Hcl.Splat.Operator operator;
            if (c.splat().attrSplat() != null) {
                operator = new Hcl.Splat.Operator(
                        randomId(),
                        sourceBefore("."),
                        Markers.EMPTY,
                        Hcl.Splat.Operator.Type.Attribute,
                        HclRightPadded.build(new Hcl.Empty(randomId(), sourceBefore("*"), Markers.EMPTY))
                );
            } else {
                operator = new Hcl.Splat.Operator(
                        randomId(),
                        sourceBefore("["),
                        Markers.EMPTY,
                        Hcl.Splat.Operator.Type.Full,
                        HclRightPadded.build(new Hcl.Empty(randomId(), sourceBefore("*"), Markers.EMPTY))
                                .withAfter(sourceBefore("]"))
                );
            }

            Expression splat = new Hcl.Splat(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    select,
                    operator
            );

            return visitSplatAttr(splat, c.splat().attrSplat() != null ?
                    c.splat().attrSplat().children :
                    c.splat().fullSplat().children);
        });
    }

    public Expression visitSplatAttr(Expression acc, List<ParseTree> attrs) {
        for (ParseTree attr : attrs) {
            if (attr instanceof HCLParser.GetAttrContext) {
                acc = new Hcl.AttributeAccess(
                        randomId(),
                        acc.getPrefix(),
                        Markers.EMPTY,
                        acc.withPrefix(Space.EMPTY),
                        new HclLeftPadded<>(sourceBefore("."),
                                visitIdentifier(((HCLParser.GetAttrContext) attr).Identifier()), Markers.EMPTY)
                );
            } else if (attr instanceof HCLParser.IndexContext) {
                acc = new Hcl.Index(
                        randomId(),
                        acc.getPrefix(),
                        Markers.EMPTY,
                        acc.withPrefix(Space.EMPTY),
                        new Hcl.Index.Position(
                                randomId(),
                                sourceBefore("["),
                                Markers.EMPTY,
                                HclRightPadded.build((Expression) visit(((HCLParser.IndexContext) attr).expression()))
                                        .withAfter(sourceBefore("]"))
                        )
                );
            }
        }
        return acc;
    }

    @Override
    public Hcl visitQuotedTemplate(HCLParser.QuotedTemplateContext ctx) {
        Space quotePrefix = sourceBefore("\"");
        Hcl.QuotedTemplate quotedTemplate = convert(ctx, (c, prefix) -> {
            List<Expression> expressions = visitTemplateExpressions(ctx.quotedTemplatePart());
            return new Hcl.QuotedTemplate(randomId(), quotePrefix, Markers.EMPTY, expressions);
        });
        skip(ctx.QUOTE(1));
        return quotedTemplate;
    }

    @NonNull
    private List<Expression> visitTemplateExpressions(List<HCLParser.QuotedTemplatePartContext> ctx) {
        List<Expression> expressions = new ArrayList<>(ctx.size());
        for (HCLParser.QuotedTemplatePartContext part : ctx) {
            if (part.stringLiteral() != null) {
                Space prefix = Space.format(prefix(part.stringLiteral()));
                String value = part.stringLiteral().getText();
                cursor = part.stringLiteral().getStop().getStopIndex() + 1;
                expressions.add(new Hcl.Literal(randomId(), prefix, Markers.EMPTY, value, value));
            } else if (part.templateInterpolation() != null) {
                Space prefix = Space.format(prefix(part.templateInterpolation()));
                expressions.add(visit(part.templateInterpolation()).withPrefix(prefix));
            } else {
                throw new IllegalStateException("Unsupported terminal node");
            }
        }
        return expressions;
    }

    @Override
    public Hcl visitTemplateInterpolation(HCLParser.TemplateInterpolationContext ctx) {
        skip(ctx.TEMPLATE_INTERPOLATION_START());
        Hcl.TemplateInterpolation templateInterpolation = convert(ctx, (c, prefix) -> new Hcl.TemplateInterpolation(randomId(), Space.format(prefix), Markers.EMPTY,
                (Expression) visit(ctx.expression())));
        skip(ctx.RBRACE());
        return templateInterpolation;
    }

    @Override
    public Hcl visitTuple(HCLParser.TupleContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Space tuplePrefix = sourceBefore("[");
            List<HclRightPadded<Expression>> mappedValues = new ArrayList<>();
            List<HCLParser.ExpressionContext> values = ctx.expression();
            for (int i = 0; i < values.size(); i++) {
                HCLParser.ExpressionContext value = values.get(i);
                mappedValues.add(HclRightPadded.build((Expression) visit(value))
                        .withAfter(i == values.size() - 1 ? sourceBefore("]") : sourceBefore(",")));
            }

            return new Hcl.Tuple(randomId(), Space.format(prefix), Markers.EMPTY, HclContainer
                    .build(tuplePrefix, mappedValues, Markers.EMPTY));
        });
    }

    @Override
    public Hcl visitUnaryOp(HCLParser.UnaryOpContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Hcl.Unary.Type op;
            if (ctx.MINUS() != null) {
                skip(ctx.MINUS());
                op = Hcl.Unary.Type.Negative;
            } else {
                skip(ctx.NOT());
                op = Hcl.Unary.Type.Not;
            }

            return new Hcl.Unary(
                    randomId(),
                    Space.format(prefix),
                    Markers.EMPTY,
                    op,
                    (Expression) visit(c.exprTerm())
            );
        });
    }

    @Override
    public Hcl visitVariableExpr(HCLParser.VariableExprContext ctx) {
        return convert(ctx, (c, prefix) -> {
            Hcl.Identifier ident = visitIdentifier(c.Identifier());
            return new Hcl.VariableExpression(randomId(), Space.format(prefix),
                    Markers.EMPTY, ident);
        });
    }

    @NonNull
    private Hcl.Identifier visitIdentifier(TerminalNode identifier) {
        Hcl.Identifier ident = new Hcl.Identifier(randomId(), Space.format(prefix(identifier)),
                Markers.EMPTY, identifier.getText());
        skip(identifier);
        return ident;
    }

    private String prefix(ParserRuleContext ctx) {
        return prefix(ctx.getStart());
    }

    private String prefix(Token token) {
        int start = token.getStartIndex();
        if (start < cursor) {
            return "";
        }
        String prefix = source.substring(cursor, start);
        cursor = start;
        return prefix;
    }

    private String prefix(@Nullable TerminalNode terminalNode) {
        return terminalNode == null ? "" : prefix(terminalNode.getSymbol());
    }

    @Nullable
    private <C extends ParserRuleContext, T> T convert(C ctx, BiFunction<C, String, T> conversion) {
        if (ctx == null) {
            return null;
        }

        T t = conversion.apply(ctx, prefix(ctx));
        if (ctx.getStop() != null) {
            cursor = ctx.getStop().getStopIndex() + (Character.isWhitespace(source.charAt(ctx.getStop().getStopIndex())) ? 0 : 1);
        }

        return t;
    }

    private String skip(TerminalNode node) {
        String prefix = prefix(node);
        cursor = node.getSymbol().getStopIndex() + 1;
        return prefix;
    }

    private Space sourceBefore(String untilDelim) {
        return sourceBefore(untilDelim, null);
    }

    /**
     * @return Source from <code>cursor</code> to next occurrence of <code>untilDelim</code>,
     * and if not found in the remaining source, the empty String. If <code>stop</code> is reached before
     * <code>untilDelim</code> return the empty String.
     */
    private Space sourceBefore(String untilDelim, @Nullable Character stop) {
        int delimIndex = positionOfNext(untilDelim, stop);
        if (delimIndex < 0) {
            return Space.EMPTY; // unable to find this delimiter
        }

        String prefix = source.substring(cursor, delimIndex);
        cursor += prefix.length() + untilDelim.length(); // advance past the delimiter
        return Space.format(prefix);
    }

    private int positionOfNext(String untilDelim, @Nullable Character stop) {
        boolean inMultiLineComment = false;
        boolean inSingleLineComment = false;

        int delimIndex = cursor;
        for (; delimIndex < source.length() - untilDelim.length() + 1; delimIndex++) {
            if (inSingleLineComment && source.charAt(delimIndex) == '\n') {
                inSingleLineComment = false;
            } else {
                if (source.length() - untilDelim.length() > delimIndex + 1) {
                    if ('#' == source.charAt(delimIndex)) {
                        inSingleLineComment = true;
                        delimIndex++;
                    } else switch (source.substring(delimIndex, delimIndex + 2)) {
                        case "//":
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
                    if (stop != null && source.charAt(delimIndex) == stop)
                        return -1; // reached stop word before finding the delimiter

                    if (source.startsWith(untilDelim, delimIndex)) {
                        break; // found it!
                    }
                }
            }
        }

        return delimIndex > source.length() - untilDelim.length() ? -1 : delimIndex;
    }
}
