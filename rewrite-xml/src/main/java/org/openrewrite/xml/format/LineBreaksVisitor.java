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
package org.openrewrite.xml.format;

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Misc;
import org.openrewrite.xml.tree.Xml;

public class LineBreaksVisitor<P> extends XmlIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public LineBreaksVisitor() {
        this(null);
    }

    public LineBreaksVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Xml.Comment visitComment(Xml.Comment comment, P p) {
        return keepMaximumLines(minimumLines(super.visitComment(comment, p),
                isFirstMisc(comment) ? 0 : 1), 2);
    }

    @Override
    public Xml.DocTypeDecl visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, P p) {
        return keepMaximumLines(minimumLines(super.visitDocTypeDecl(docTypeDecl, p),
                isFirstMisc(docTypeDecl) ? 0 : 1), 1);
    }

    @Override
    public Xml.ProcessingInstruction visitProcessingInstruction(Xml.ProcessingInstruction processingInstruction, P p) {
        return keepMaximumLines(minimumLines(super.visitProcessingInstruction(processingInstruction, p),
                isFirstMisc(processingInstruction) ? 0 : 1), 1);
    }

    @Override
    public Xml.Prolog visitProlog(Xml.Prolog prolog, P p) {
        return super.visitProlog(prolog, p);
    }

    @Override
    public Xml.Tag visitTag(Xml.Tag tag, P p) {
        Xml.Document doc = getCursor().firstEnclosingOrThrow(Xml.Document.class);
        Xml.Tag t = keepMaximumLines(minimumLines(super.visitTag(tag, p),
                doc.getRoot().isScope(tag) &&
                        doc.getProlog().getXmlDecl() == null &&
                        doc.getProlog().getMisc().isEmpty() ? 0 : 1), 2);
        if (t.getClosing() != null && !t.getChildren().isEmpty()) {
            t = t.withClosing(keepMaximumLines(minimumLines(t.getClosing(), 1), 2));
        }
        return t;
    }

    private boolean isFirstMisc(Misc misc) {
        Xml.Document doc = getCursor().firstEnclosingOrThrow(Xml.Document.class);
        if (doc.getProlog().getXmlDecl() == null) {
            return !doc.getProlog().getMisc().isEmpty() &&
                    doc.getProlog().getMisc().get(0) == misc;
        }
        return misc == doc.getProlog().getXmlDecl();
    }

    private <X extends Xml> X keepMaximumLines(X tree, int max) {
        //noinspection unchecked
        return (X) tree.withPrefix(keepMaximumLines(tree.getPrefix(), max));
    }

    private String keepMaximumLines(String whitespace, int max) {
        long blankLines = whitespace.chars().filter(c -> c == '\n').count() - 1;
        if (blankLines > max) {
            int startWhitespaceAtIndex = 0;
            for (int i = 0; i < blankLines - max; i++, startWhitespaceAtIndex++) {
                startWhitespaceAtIndex = whitespace.indexOf('\n', startWhitespaceAtIndex);
            }
            startWhitespaceAtIndex--;
            return whitespace.substring(startWhitespaceAtIndex);
        }
        return whitespace;
    }

    private <X extends Xml> X minimumLines(X tree, int max) {
        //noinspection unchecked
        return (X) tree.withPrefix(minimumLines(tree.getPrefix(), max));
    }

    private String minimumLines(String whitespace, int min) {
        if (min == 0) {
            return whitespace;
        }
        String minWhitespace = whitespace;
        for (int i = 0; i < min - whitespace.chars().filter(c -> c == '\n').count(); i++) {
            //noinspection StringConcatenationInLoop
            minWhitespace = "\n" + minWhitespace;
        }
        return minWhitespace;
    }

    @Override
    public @Nullable Xml postVisit(Xml tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable Xml visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (Xml) tree;
        }
        return super.visit(tree, p);
    }
}
