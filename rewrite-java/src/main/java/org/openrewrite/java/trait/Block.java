/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.trait;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class Block implements Trait<J.Block> {
    @Getter
    private final Cursor cursor;

    private final List<Line> lines;

    public Block(Cursor cursor) {
        Object object = cursor.getValue();
        if (!(object instanceof J.Block)) {
            throw new IllegalArgumentException("Expected a J.Block, got " + object);
        }
        J.Block block = (J.Block) object;
        List<Line> lines = new ArrayList<>();
        int indexOfFirstNewLineComment;
        int indexOfLastNewLineComment;
        Comment lastComment;
        String whitespace;
        for (Statement statement : block.getStatements()) {
            if (!statement.getComments().isEmpty()) {
                indexOfFirstNewLineComment = findIndexOfFirstCommentOnNewLine(statement.getComments(), statement.getPrefix());
                indexOfLastNewLineComment = findIndexOfLastCommentOnNewLine(statement.getComments());
                whitespace = statement.getPrefix().getWhitespace();
                if (!lines.isEmpty()) {
                    Line lastLine = lines.get(lines.size() - 1);
                    if (lastLine instanceof StatementLine) {
                        StatementLine statementLine = (StatementLine) lastLine;
                        ArrayList<Comment> comments = new ArrayList<>(statement.getComments().subList(0, indexOfFirstNewLineComment));
                        statementLine.setEndOfLine(statement.getPrefix().withComments(comments));
                        if (!comments.isEmpty()) {
                            whitespace = comments.get(comments.size() - 1).getSuffix();
                        }
                    }
                }
                if (indexOfFirstNewLineComment <= indexOfLastNewLineComment) {
                    for (Comment comment : statement.getComments().subList(indexOfFirstNewLineComment, indexOfLastNewLineComment)) {
                        lines.add(new CommentLine(whitespace, comment));
                        whitespace = comment.getSuffix();
                    }
                    lastComment = statement.getComments().get(indexOfLastNewLineComment);
                    if (lastComment.getSuffix().contains("\n")) {
                        lines.add(new CommentLine(whitespace, lastComment));
                        whitespace = lastComment.getSuffix();
                    }
                }
                if (indexOfLastNewLineComment + 1 < statement.getComments().size()) {
                    lines.add(new StatementLine(Space.build(whitespace, new ArrayList<>(statement.getComments().subList(indexOfLastNewLineComment + 1, statement.getComments().size()))), statement));
                } else {
                    lines.add(new StatementLine(Space.build(whitespace, emptyList()), statement));
                }
            } else {
                lines.add(new StatementLine(statement.getPrefix(), statement));
            }
        }
        if (!block.getEnd().getComments().isEmpty()) {
            indexOfFirstNewLineComment = findIndexOfFirstCommentOnNewLine(block.getEnd().getComments(), block.getEnd());
            whitespace = block.getEnd().getWhitespace();
            if (!lines.isEmpty()) {
                Line lastLine = lines.get(lines.size() - 1);
                if (lastLine instanceof StatementLine) {
                    ArrayList<Comment> comments = new ArrayList<>(block.getEnd().getComments().subList(0, indexOfFirstNewLineComment));
                    ((StatementLine) lastLine).setEndOfLine(block.getEnd().withComments(comments));
                    if (!comments.isEmpty()) {
                        whitespace = comments.get(comments.size() - 1).getSuffix();
                    }
                }
            }
            if (indexOfFirstNewLineComment == 0 || indexOfFirstNewLineComment <= block.getEnd().getComments().size() - 1) {
                for (Comment comment : block.getEnd().getComments().subList(indexOfFirstNewLineComment, block.getEnd().getComments().size())) {
                    lines.add(new CommentLine(whitespace, comment));
                    whitespace = comment.getSuffix();
                }
            }
        }
        this.cursor = cursor;
        this.lines = lines;
    }

    public Block filterStatements(Predicate<Statement> predicate) {
        return mapStatements(statement -> predicate.test(statement) ? statement : null);
    }

    public Block mapStatements(Function<Statement, @Nullable Statement> mapper) {
        return mapLines(line -> line.mapStatement(mapper));
    }

    public Block filterLines(Predicate<Line> predicate) {
        return mapLines(line -> predicate.test(line) ? line : null);
    }

    public Block mapLines(Function<Line, @Nullable Line> mapper) {
        J.Block block = getTree();
        List<Line> newLines = ListUtils.map(lines, mapper);
        if (lines == newLines) {
            return this;
        }

        return new Block(
                new Cursor(this.cursor.getParent(),
                        block.withEnd(buildEnd(block.getEnd(), newLines))
                                .withStatements(buildStatements(newLines))),
                newLines);
    }

    private List<Statement> buildStatements(List<Line> lines) {
        List<Statement> statements = new ArrayList<>();
        List<CommentLine> commentLines = new ArrayList<>();
        Space endOfLine = Space.EMPTY;
        for (Line line : lines) {
            if (line instanceof CommentLine) {
                commentLines.add((CommentLine) line);
            } else if (line instanceof StatementLine) {
                StatementLine statementLine = (StatementLine) line;
                Statement statement = statementLine.getStatement();
                String whitespace = statementLine.getBeforeLine().getWhitespace();
                List<Comment> comments = new ArrayList<>();
                if (!commentLines.isEmpty()) {
                    whitespace = commentLines.get(0).getWhitespace();
                }
                if (!endOfLine.getComments().isEmpty()) {
                    if (!commentLines.isEmpty()) {
                        String suffix = commentLines.get(0).getWhitespace();
                        endOfLine = endOfLine.withComments(ListUtils.mapLast(endOfLine.getComments(), (Function<Comment, Comment>) comment -> comment.withSuffix(suffix)));
                    } else {
                        endOfLine = endOfLine.withComments(ListUtils.mapLast(endOfLine.getComments(), (Function<Comment, Comment>) comment -> comment.withSuffix(statementLine.getBeforeLine().getWhitespace())));
                    }
                    comments.addAll(endOfLine.getComments());
                    whitespace = endOfLine.getWhitespace();
                }

                if (!commentLines.isEmpty()) {
                    int i = 1;
                    for (; i < commentLines.size(); i++) {
                        comments.add(commentLines.get(i - 1).getComment().withSuffix(commentLines.get(i).getWhitespace()));
                    }
                    comments.add(commentLines.get(i - 1).getComment().withSuffix(statementLine.getBeforeLine().getWhitespace()));
                }
                if (!statementLine.getBeforeLine().getComments().isEmpty()) {
                    comments.addAll(statementLine.getBeforeLine().getComments());
                }

                endOfLine = statementLine.getEndOfLine();

                Space prefix = statement.getPrefix().withWhitespace(whitespace).withComments(comments);
                statements.add(statement.withPrefix(prefix));
                commentLines = new ArrayList<>();
            }
        }

        return statements;
    }

    private Space buildEnd(Space end, List<Line> lines) {
        if (lines.isEmpty()) {
            return end.withComments(emptyList());
        }
        int i = lines.size() - 1;
        while (i >= 0 && lines.get(i) instanceof CommentLine) {
            i--;
        }
        if (i == -1) {
            i = 0;
        } else if (lines.get(i) instanceof StatementLine) {
            i++;
        }

        List<Comment> comments = new ArrayList<>();
        String suffix;
        String prefix;
        if (i < lines.size()) {
            prefix = lines.get(i).getWhitespace();
        } else {
            if (end.getComments().isEmpty()) {
                prefix = end.getWhitespace();
            } else {
                prefix = end.getComments().get(end.getComments().size() - 1).getSuffix();
            }
        }
        if (i > 0) {
            Line line = lines.get(i - 1);
            if (line instanceof StatementLine) {
                Space endOfLine = ((StatementLine) line).getEndOfLine();
                List<Comment> endOfLineComments = endOfLine.getComments();
                if (!endOfLineComments.isEmpty()) {
                    String lastPrefix = prefix;
                    endOfLineComments = ListUtils.mapLast(endOfLineComments, (Function<Comment, Comment>) comment -> comment.withSuffix(lastPrefix));
                    comments.addAll(endOfLineComments);
                    prefix = endOfLine.getWhitespace();
                }
            }
        }
        for (; i < lines.size(); i++) {
            if (i + 1 < lines.size()) {
                suffix = ((CommentLine) lines.get(i + 1)).getWhitespace();
            } else {
                suffix = end.getWhitespace();
                if (!end.getComments().isEmpty()) {
                    suffix = end.getComments().get(end.getComments().size() - 1).getSuffix();
                }
            }
            comments.add(((CommentLine) lines.get(i)).getComment().withSuffix(suffix));
        }

        return end.withWhitespace(prefix).withComments(comments);
    }

    public static class Matcher extends SimpleTraitMatcher<Block> {
        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<Block, P> visitor) {
            return new JavaVisitor<P>() {
                @Override
                public J visitBlock(J.Block block, P p) {
                    Block trait = test(getCursor());
                    return trait != null ?
                            (J) visitor.visit(trait, p) :
                            super.visitBlock(block, p);
                }
            };
        }

        @Override
        protected @Nullable Block test(Cursor cursor) {
            Object object = cursor.getValue();
            if (object instanceof J.Block) {
                return new Block(cursor);
            }

            return null;
        }
    }

    @Getter
    @EqualsAndHashCode
    public static abstract class Line {
        @Nullable
        Line remove() {
            return null;
        }

        @Nullable
        Line mapStatement(Function<Statement, Statement> mapper) {
            return this;
        }

        public abstract String getWhitespace();
    }

    @Getter
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StatementLine extends Line {
        private final Space beforeLine;
        private final Statement statement;
        @Setter
        private Space endOfLine;

        public StatementLine(Space beforeLine, Statement statement) {
            this.beforeLine = beforeLine;
            this.statement = statement;
            this.endOfLine = Space.EMPTY;
        }

        @Override
        @Nullable
        Line mapStatement(Function<Statement, @Nullable Statement> mapper) {
            Statement newStatement = mapper.apply(statement);
            if (newStatement == null) {
                return null;
            }
            if (statement != newStatement) {
                return new StatementLine(getBeforeLine(), newStatement, getEndOfLine());
            }
            return this;
        }

        @Override
        public String getWhitespace() {
            return getBeforeLine().getWhitespace();
        }
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    public static class CommentLine extends Line {
        private final String whitespace;
        private final Comment comment;
    }

    private static int findIndexOfFirstCommentOnNewLine(List<Comment> comments, Space prefix) {
        if (prefix.getWhitespace().contains("\n")) {
            return 0;
        }
        int index = 0;
        while (index < comments.size()) {
            Comment comment = comments.get(index);
            index++;
            if (comment.getSuffix().contains("\n")) {
                break;
            }
        }
        return index;
    }

    private static int findIndexOfLastCommentOnNewLine(List<Comment> comments) {
        int index = comments.size() - 1;
        while (index >= 0) {
            Comment comment = comments.get(index);
            if (comment.isMultiline()) {
                if (comment.getSuffix().contains("\n")) {
                    return index;
                } else {
                    index--;
                }
            } else {
                return index;
            }
        }
        return index;
    }
}
