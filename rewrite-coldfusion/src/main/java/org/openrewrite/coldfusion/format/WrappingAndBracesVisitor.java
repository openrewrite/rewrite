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
package org.openrewrite.coldfusion.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.internal.Layout;
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.style.WrappingAndBracesStyle;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;

import java.util.List;

/**
 * Breaks script's lines: a block's braces and the {@code else}, {@code catch}, {@code finally} and {@code while} after
 * it go where the {@link WrappingAndBracesStyle} puts them, and a statement built with no space before it, as a
 * recipe builds one, starts a line of its own. A statement an author wrote on the line of another stays there. A block
 * written on one line stays on it when the style keeps simple blocks, but one built with no whitespace at all is laid
 * out over lines. Tags keep their lines, since the line breaks between them can be part of a template's output.
 */
public class WrappingAndBracesVisitor<P> extends FormattingVisitor<P> {
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style) {
        this(style, null);
    }

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style, @Nullable Tree stopAfter) {
        super(stopAfter);
        this.style = style;
    }

    private boolean keepsLines() {
        return getCursor().getNearestMessage("stop") != null || isInInterpolation();
    }

    @Override
    public J.Block visitBlock(J.Block block, P p) {
        J.Block b = super.visitBlock(block, p);
        if (keepsLines() || TemplateOutput.isTemplateBody(getCursor())) {
            return b;
        }
        List<JRightPadded<Statement>> statements = b.getPadding().getStatements();
        if (statements.isEmpty()) {
            return b;
        }
        boolean oneLine = !Layout.hasLineBreak(b.getEnd());
        boolean noWhitespace = b.getEnd().isEmpty();
        for (JRightPadded<Statement> statement : statements) {
            Space prefix = statement.getElement().getPrefix();
            oneLine &= !Layout.hasLineBreak(prefix);
            noWhitespace &= prefix.isEmpty();
        }
        if (oneLine && !noWhitespace && style.getKeepSimpleBlocksInOneLine()) {
            return b;
        }
        // in a block over lines, a statement an author wrote on the line of another stays there
        boolean expand = oneLine;
        b = b.getPadding().withStatements(ListUtils.map(statements, s -> {
            Statement element = s.getElement();
            Space prefix = element.getPrefix();
            return Layout.hasLineBreak(prefix) || !expand && !prefix.isEmpty() ? s : s.withElement(element.withPrefix(withLineBreak(prefix)));
        }));
        return Layout.hasLineBreak(b.getEnd()) || !expand && !b.getEnd().isEmpty() ? b : b.withEnd(endWithLineBreak(b.getEnd()));
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
        J.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        if (keepsLines() || TemplateOutput.isTagSyntax(m) || m.getBody() == null) {
            return m;
        }
        return m.withBody(brace(m.getBody(), style.getBraces().getFunctionOnNewLine()));
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
        J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (keepsLines() || TemplateOutput.isTagSyntax(c)) {
            return c;
        }
        return c.withBody(brace(c.getBody(), style.getBraces().getComponentOnNewLine()));
    }

    @Override
    public J.If visitIf(J.If iff, P p) {
        J.If i = super.visitIf(iff, p);
        return keepsLines() ? i : i.getPadding().withThenPart(braced(i.getPadding().getThenPart()));
    }

    @Override
    public J.If.Else visitElse(J.If.Else anElse, P p) {
        J.If.Else e = super.visitElse(anElse, p);
        if (keepsLines()) {
            return e;
        }
        J.If owner = getCursor().getParentTreeCursor().getValue();
        if (owner.getThenPart() instanceof J.Block) {
            e = e.withPrefix(keyword(e.getPrefix(), style.getKeywordsAfterBlock().getElseOnNewLine()));
        }
        return e.getPadding().withBody(braced(e.getPadding().getBody()));
    }

    @Override
    public J.WhileLoop visitWhileLoop(J.WhileLoop whileLoop, P p) {
        J.WhileLoop w = super.visitWhileLoop(whileLoop, p);
        return keepsLines() ? w : w.getPadding().withBody(braced(w.getPadding().getBody()));
    }

    @Override
    public J.ForLoop visitForLoop(J.ForLoop forLoop, P p) {
        J.ForLoop f = super.visitForLoop(forLoop, p);
        return keepsLines() ? f : f.getPadding().withBody(braced(f.getPadding().getBody()));
    }

    @Override
    public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, P p) {
        J.ForEachLoop f = super.visitForEachLoop(forLoop, p);
        return keepsLines() ? f : f.getPadding().withBody(braced(f.getPadding().getBody()));
    }

    @Override
    public J.DoWhileLoop visitDoWhileLoop(J.DoWhileLoop doWhileLoop, P p) {
        J.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        if (keepsLines()) {
            return d;
        }
        d = d.getPadding().withBody(braced(d.getPadding().getBody()));
        if (d.getBody() instanceof J.Block) {
            JLeftPadded<J.ControlParentheses<Expression>> condition = d.getPadding().getWhileCondition();
            d = d.getPadding().withWhileCondition(condition.withBefore(
                    keyword(condition.getBefore(), style.getKeywordsAfterBlock().getWhileOnNewLine())));
        }
        return d;
    }

    @Override
    public J.Switch visitSwitch(J.Switch aSwitch, P p) {
        J.Switch s = super.visitSwitch(aSwitch, p);
        return keepsLines() ? s : s.withCases(brace(s.getCases(), style.getBraces().getStatementOnNewLine()));
    }

    @Override
    public J.Try visitTry(J.Try tryable, P p) {
        J.Try t = super.visitTry(tryable, p);
        if (keepsLines()) {
            return t;
        }
        boolean onNewLine = style.getBraces().getStatementOnNewLine();
        boolean catchOnNewLine = style.getKeywordsAfterBlock().getCatchOnNewLine();
        t = t.withBody(brace(t.getBody(), onNewLine));
        t = t.withCatches(ListUtils.map(t.getCatches(), c -> c.withPrefix(keyword(c.getPrefix(), catchOnNewLine))
                .withBody(brace(c.getBody(), onNewLine))));
        JLeftPadded<J.Block> finallyBlock = t.getPadding().getFinally();
        if (finallyBlock != null) {
            t = t.getPadding().withFinally(finallyBlock.withBefore(keyword(finallyBlock.getBefore(), catchOnNewLine))
                    .withElement(brace(finallyBlock.getElement(), onNewLine)));
        }
        return t;
    }

    @Override
    public Cf.ScriptTag visitScriptTag(Cf.ScriptTag scriptTag, P p) {
        Cf.ScriptTag s = super.visitScriptTag(scriptTag, p);
        if (keepsLines() || s.getBody() == null) {
            return s;
        }
        return s.withBody(brace(s.getBody(), style.getBraces().getStatementOnNewLine()));
    }

    @Override
    public Cf.TagCall visitTagCall(Cf.TagCall tagCall, P p) {
        Cf.TagCall t = super.visitTagCall(tagCall, p);
        return keepsLines() ? t : t.withBody(brace(t.getBody(), style.getBraces().getStatementOnNewLine()));
    }

    private JRightPadded<Statement> braced(JRightPadded<Statement> body) {
        return body.getElement() instanceof J.Block ?
                body.withElement(brace((J.Block) body.getElement(), style.getBraces().getStatementOnNewLine())) : body;
    }

    /**
     * A block whose <code>{</code> starts a line of its own or ends the line before.
     */
    private static J.Block brace(J.Block block, boolean onNewLine) {
        return block.withPrefix(keyword(block.getPrefix(), onNewLine));
    }

    /**
     * The space before a brace or a keyword after a block, holding a line break or not. A space with a comment in it
     * stays as it is.
     */
    private static Space keyword(Space space, boolean onNewLine) {
        if (!space.getComments().isEmpty()) {
            return space;
        }
        boolean lineBreak = Layout.hasLineBreak(space.getWhitespace());
        if (onNewLine && !lineBreak) {
            return space.withWhitespace("\n");
        }
        if (!onNewLine && lineBreak) {
            return Space.SINGLE_SPACE;
        }
        return space;
    }

    private static Space withLineBreak(Space space) {
        if (Layout.hasLineBreak(space.getWhitespace())) {
            return space;
        }
        return space.withWhitespace("\n");
    }

    /**
     * The end of a block, before its <code>}</code>, starting a line. A comment there ends the line before it.
     */
    private static Space endWithLineBreak(Space end) {
        List<Comment> comments = end.getComments();
        if (comments.isEmpty()) {
            return end.withWhitespace("\n");
        }
        return end.withComments(ListUtils.mapLast(comments, c -> Layout.hasLineBreak(c.getSuffix()) ? c : c.withSuffix("\n")));
    }
}
