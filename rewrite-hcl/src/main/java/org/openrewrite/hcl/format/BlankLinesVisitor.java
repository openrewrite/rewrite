/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.hcl.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.HclRightPadded;
import org.openrewrite.hcl.tree.Space;
import org.openrewrite.internal.ListUtils;

public class BlankLinesVisitor<P> extends HclIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final BlankLinesStyle style;

    public BlankLinesVisitor(BlankLinesStyle style) {
        this(style, null);
    }

    public BlankLinesVisitor(BlankLinesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Hcl.ConfigFile visitConfigFile(Hcl.ConfigFile configFile, P p) {
        Hcl.ConfigFile c = super.visitConfigFile(configFile, p);
        return c.withBody(ListUtils.map(c.getBody(), (i, b) -> b.withPrefix(minimumLines(
                keepMaximumLines(b.getPrefix(), style.getKeepMaximum().getInBodyContent()),
                i == 0 ? 0 : style.getMinimum().getBeforeBodyContent()
        ))));
    }

    @Override
    public Hcl.Block visitBlock(Hcl.Block block, P p) {
        Hcl.Block c = super.visitBlock(block, p);
        c = c.withBody(ListUtils.map(c.getBody(), (i, b) -> b.withPrefix(minimumLines(
                keepMaximumLines(b.getPrefix(), style.getKeepMaximum().getInBodyContent()),
                i == 0 ? 0 : style.getMinimum().getBeforeBodyContent()
        ))));
        return c.withEnd(keepMaximumLines(c.getEnd(), style.getKeepMaximum().getBeforeEndOfBlock()));
    }

    private <H extends Hcl> H keepMaximumLines(H tree, int max) {
        return tree.withPrefix(keepMaximumLines(tree.getPrefix(), max));
    }

    private Space keepMaximumLines(Space prefix, int max) {
        return prefix.withWhitespace(keepMaximumLines(prefix.getWhitespace(), max));
    }

    private String keepMaximumLines(String whitespace, int max) {
        long blankLines = whitespace.chars().filter(c -> c == '\n').count() - 1;
        if (blankLines > max) {
            int startWhitespaceAtIndex = 0;
            for (int i = 0; i < blankLines - max + 1; i++, startWhitespaceAtIndex++) {
                startWhitespaceAtIndex = whitespace.indexOf('\n', startWhitespaceAtIndex);
            }
            startWhitespaceAtIndex--;
            return whitespace.substring(startWhitespaceAtIndex);
        }
        return whitespace;
    }

    private <H extends Hcl> HclRightPadded<H> minimumLines(HclRightPadded<H> tree, int min) {
        return tree.withElement(minimumLines(tree.getElement(), min));
    }

    private <H extends Hcl> H minimumLines(H tree, int min) {
        return tree.withPrefix(minimumLines(tree.getPrefix(), min));
    }

    private Space minimumLines(Space prefix, int min) {
        if (prefix.getComments().isEmpty() || prefix.getWhitespace().contains("\n")) {
            return prefix.withWhitespace(minimumLines(prefix.getWhitespace(), min));
        }

        // the first comment is a trailing comment on the previous line
        return prefix.withComments(ListUtils.map(prefix.getComments(), (i, c) -> i == 0 ? c.withSuffix(minimumLines(c.getSuffix(), min)) : c));
    }

    private String minimumLines(String whitespace, int min) {
        if (min == 0) {
            return whitespace;
        }
        String minWhitespace = whitespace;
        for (int i = 0; i < min - whitespace.chars().filter(c -> c == '\n').count() + 1; i++) {
            //noinspection StringConcatenationInLoop
            minWhitespace = "\n" + minWhitespace;
        }
        return minWhitespace;
    }

    @Override
    public @Nullable Hcl postVisit(Hcl tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Hcl.ConfigFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Hcl visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Hcl) tree;
        }
        return super.visit(tree, p);
    }
}
