/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.internal.Heredocs;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.tree.ParseError;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class Assertions {
    private static final Pattern LINE_CONTINUATION = Pattern.compile("[\\\\`][ \\t]*(?=\\r?\\n)");

    private Assertions() {
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before) {
        return docker(before, s -> {
        });
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     Consumer<SourceSpec<Docker.File>> spec) {
        SourceSpec<Docker.File> dockerfile = new SourceSpec<>(
                Docker.File.class,
                null,
                DockerParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        );
        spec.accept(dockerfile);
        return dockerfile;
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     @Language("dockerfile") @Nullable String after) {
        return docker(before, after, s -> {
        });
    }

    public static SourceSpecs docker(@Language("dockerfile") @Nullable String before,
                                     @Language("dockerfile") @Nullable String after,
                                     Consumer<SourceSpec<Docker.File>> spec) {
        SourceSpec<Docker.File> dockerfile = new SourceSpec<>(
                Docker.File.class,
                null,
                DockerParser.builder(),
                before,
                Assertions::validate,
                ctx -> {
                }
        ).after(s -> after);
        spec.accept(dockerfile);
        return dockerfile;
    }

    private static SourceFile validate(SourceFile sf, TypeValidation tv) {
        if (!tv.allowNonWhitespaceInWhitespace()) {
            List<Docker> elementsWithNonBlankWhitespace = new DockerIsoVisitor<List<Docker>>() {
                @Override
                public Space visitSpace(Space space, List<Docker> elements) {
                    if (!isWhitespace(space.getWhitespace())) {
                        elements.add(getCursor().firstEnclosingOrThrow(Docker.class));
                    }
                    return super.visitSpace(space, elements);
                }
            }.reduce(sf, new ArrayList<>());
            if (!elementsWithNonBlankWhitespace.isEmpty()) {
                throw new AssertionError("Expected no non-whitespace in whitespace, but found: " + elementsWithNonBlankWhitespace);
            }
        }
        assertWellFormed(sf);
        if (tv.parseAndPrintEquality()) {
            assertReparsesToTheSameTree(sf);
        }
        return sf;
    }

    /// Line continuations are whitespace to a Dockerfile, so the `\` that introduces one is not text.
    private static boolean isWhitespace(String text) {
        return LINE_CONTINUATION.matcher(text).replaceAll("").trim().isEmpty();
    }

    /// A tree prints losslessly by construction, so printing alone cannot tell whether its elements mean
    /// what they say. Reading the printed source back and comparing the two trees catches one that prints
    /// correctly but holds the wrong thing, as an image reference whose tag sits in its name does.
    private static void assertReparsesToTheSameTree(SourceFile sf) {
        String printed = sf.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
        List<SourceFile> reparsed = DockerParser.builder().build()
                .parse(new InMemoryExecutionContext(), printed)
                .collect(toList());
        if (reparsed.size() != 1 || reparsed.get(0) instanceof ParseError) {
            throw new AssertionError("Expected printing the tree to yield a parseable Dockerfile, but got " +
                    (reparsed.size() == 1 ? "a parse error" : reparsed.size() + " source files") + ":\n" + printed);
        }
        String printedSemantics = semantics(reparsed.get(0));
        String treeSemantics = semantics(sf);
        if (!printedSemantics.equals(treeSemantics)) {
            throw new AssertionError("Expected the tree to model what it prints, but\n" + printed +
                    "\nmodels " + printedSemantics + "\nwhile the tree models " + treeSemantics);
        }
    }

    /// Everything a tree models apart from its formatting.
    private static String semantics(Tree tree) {
        return new DockerIsoVisitor<StringBuilder>() {
            @Override
            public @Nullable Docker visit(@Nullable Tree t, StringBuilder out) {
                if (!(t instanceof Docker)) {
                    return super.visit(t, out);
                }
                Docker d = (Docker) t;
                out.append('(').append(d.getClass().getSimpleName());
                if (d instanceof Docker.Literal) {
                    Docker.Literal l = (Docker.Literal) d;
                    out.append(' ').append(l.getQuoteStyle()).append(' ').append(l.getText());
                } else if (d instanceof Docker.EnvironmentVariable) {
                    Docker.EnvironmentVariable e = (Docker.EnvironmentVariable) d;
                    out.append(' ').append(e.getName()).append(' ').append(e.isBraced());
                } else if (d instanceof Docker.Flag) {
                    out.append(' ').append(((Docker.Flag) d).getName());
                } else if (d instanceof Docker.Port) {
                    Docker.Port p = (Docker.Port) d;
                    out.append(' ').append(p.getText()).append(' ').append(p.getStart())
                            .append('-').append(p.getEnd()).append(' ').append(p.getProtocol());
                } else if (d instanceof Docker.Volume) {
                    out.append(' ').append(((Docker.Volume) d).isJsonForm());
                } else if (d instanceof Docker.Healthcheck) {
                    out.append(' ').append(((Docker.Healthcheck) d).isNone());
                } else if (d instanceof Docker.HeredocForm) {
                    out.append(' ').append(((Docker.HeredocForm) d).getPreamble());
                } else if (d instanceof Docker.HeredocBody) {
                    Docker.HeredocBody b = (Docker.HeredocBody) d;
                    out.append(' ').append(b.getOpening()).append(' ').append(b.getClosing())
                            .append(' ').append(b.getContentLines());
                }
                Docker visited = super.visit(t, out);
                out.append(')');
                return visited;
            }

            @Override
            public Docker.From visitFrom(Docker.From from, StringBuilder out) {
                if (from.getFlags() != null) {
                    from.getFlags().forEach(flag -> visit(flag, out));
                }
                out.append(" name");
                visit(from.getImageName(), out);
                if (from.getTag() != null) {
                    out.append(" tag");
                    visit(from.getTag(), out);
                }
                if (from.getDigest() != null) {
                    out.append(" digest");
                    visit(from.getDigest(), out);
                }
                if (from.getAs() != null) {
                    visitFromAs(from.getAs(), out);
                }
                return from;
            }

            @Override
            public Docker.From.As visitFromAs(Docker.From.As as, StringBuilder out) {
                out.append("(As ").append(as.getKeyword());
                visit(as.getName(), out);
                out.append(')');
                return as;
            }

            @Override
            public Docker.Env.EnvPair visitEnvPair(Docker.Env.EnvPair pair, StringBuilder out) {
                out.append("(EnvPair ").append(pair.isHasEquals());
                Docker.Env.EnvPair p = super.visitEnvPair(pair, out);
                out.append(')');
                return p;
            }

            @Override
            public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, StringBuilder out) {
                out.append("(LabelPair ").append(pair.isHasEquals());
                Docker.Label.LabelPair p = super.visitLabelPair(pair, out);
                out.append(')');
                return p;
            }

            @Override
            public Docker.CopyShellForm visitCopyShellForm(Docker.CopyShellForm form, StringBuilder out) {
                out.append(" sources");
                form.getSources().forEach(source -> visit(source, out));
                out.append(" destination");
                visit(form.getDestination(), out);
                return form;
            }
        }.reduce(tree, new StringBuilder()).toString();
    }

    /// A recipe that assembles a tree by hand can leave it holding what the printer cannot express, or
    /// can express only by changing what the source means: a subtree copied without fresh ids, an
    /// instruction that does not start its own line, a flag whose name already carries its dashes.
    /// There is no flag to turn these off, unlike [TypeValidation#parseAndPrintEquality].
    static void assertWellFormed(SourceFile sf) {
        List<String> violations = new WellFormedVisitor().reduce(sf, new ArrayList<>());
        if (!violations.isEmpty()) {
            throw new AssertionError("Expected a well formed tree, but found:\n  " + String.join("\n  ", violations));
        }
    }

    private static class WellFormedVisitor extends DockerIsoVisitor<List<String>> {
        // The lookarounds keep a here-string out: `<<<'x'` redirects a word rather than opening a heredoc,
        // and its last two '<' would otherwise read as a marker naming the word that follows them.
        private static final Pattern HEREDOC_MARKER =
                Pattern.compile("(?<!<)<<(?!<)(-?(?:[A-Za-z_][A-Za-z0-9_]*|'[^'\\r\\n]*'|\"[^\"\\r\\n]*\")+)");

        private final Set<UUID> ids = new HashSet<>();

        /// The one [Space] whose trailing comment needs no newline after it. A [Space] holding comments
        /// is never a flyweight, so reference identity is enough.
        private @Nullable Space eof;

        @Override
        public @Nullable Docker visit(@Nullable Tree tree, List<String> violations) {
            if (tree instanceof Docker) {
                Docker d = (Docker) tree;
                uniqueId(d.getId(), d.getClass(), violations);
                if (d instanceof Docker.Instruction) {
                    keyword(((Docker.Instruction) d).getKeyword(), d.getClass(), violations);
                }
            }
            return super.visit(tree, violations);
        }

        @Override
        public Docker.File visitFile(Docker.File file, List<String> violations) {
            eof = file.getEof();
            List<Docker.Arg> globalArgs = file.getGlobalArgs();
            for (int i = 1; i < globalArgs.size(); i++) {
                startsItsOwnLine(globalArgs.get(i), violations);
            }
            List<Docker.Stage> stages = file.getStages();
            for (int i = globalArgs.isEmpty() ? 1 : 0; i < stages.size(); i++) {
                Docker.Stage stage = stages.get(i);
                if (!containsNewline(stage.getPrefix()) && !containsNewline(stage.getFrom().getPrefix())) {
                    violations.add("expected the Stage to start its own line, but neither it nor its From has a" +
                            " newline in its prefix");
                }
            }
            return super.visitFile(file, violations);
        }

        @Override
        public Docker.Stage visitStage(Docker.Stage stage, List<String> violations) {
            for (Docker.Instruction instruction : stage.getInstructions()) {
                startsItsOwnLine(instruction, violations);
            }
            return super.visitStage(stage, violations);
        }

        @Override
        public Space visitSpace(Space space, List<String> violations) {
            List<Comment> comments = space.getComments();
            for (int i = 0; i < comments.size(); i++) {
                Comment comment = comments.get(i);
                if (!comment.getText().startsWith("#")) {
                    violations.add("expected the comment " + quoted(comment.getText()) + " to start with \"#\"");
                }
                if (comment.getText().indexOf('\n') >= 0) {
                    violations.add("expected the comment " + quoted(comment.getText()) + " to hold a single line");
                }
                if (!isWhitespace(comment.getPrefix())) {
                    violations.add("expected the prefix of the comment " + quoted(comment.getText()) +
                            " to be whitespace, but it is " + quoted(comment.getPrefix()));
                }
                boolean last = i + 1 == comments.size();
                String following = last ? space.getWhitespace() : comments.get(i + 1).getPrefix();
                if (following.indexOf('\n') < 0 && !(last && space == eof)) {
                    violations.add("expected a newline after the comment " + quoted(comment.getText()) +
                            ", or whatever follows it is read back as part of it");
                }
            }
            return super.visitSpace(space, violations);
        }

        @Override
        public Docker.From visitFrom(Docker.From from, List<String> violations) {
            nonEmpty(from.getImageName(), "the image name of a From", violations);
            return super.visitFrom(from, violations);
        }

        @Override
        public Docker.From.@Nullable As visitFromAs(Docker.From.As as, List<String> violations) {
            uniqueId(as.getId(), as.getClass(), violations);
            keyword(as.getKeyword(), as.getClass(), violations);
            return super.visitFromAs(as, violations);
        }

        @Override
        public Docker.Env.EnvPair visitEnvPair(Docker.Env.EnvPair pair, List<String> violations) {
            uniqueId(pair.getId(), pair.getClass(), violations);
            return super.visitEnvPair(pair, violations);
        }

        @Override
        public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, List<String> violations) {
            uniqueId(pair.getId(), pair.getClass(), violations);
            return super.visitLabelPair(pair, violations);
        }

        @Override
        public Docker.Workdir visitWorkdir(Docker.Workdir workdir, List<String> violations) {
            nonEmpty(workdir.getPath(), "the path of a Workdir", violations);
            return super.visitWorkdir(workdir, violations);
        }

        @Override
        public Docker visitCopyShellForm(Docker.CopyShellForm form, List<String> violations) {
            nonEmpty(form.getDestination(), "the destination of a CopyShellForm", violations);
            return super.visitCopyShellForm(form, violations);
        }

        @Override
        public Docker.Flag visitFlag(Docker.Flag flag, List<String> violations) {
            String name = flag.getName();
            if (name.startsWith("-") || name.indexOf('=') >= 0 || StringUtils.containsWhitespace(name)) {
                violations.add("expected the flag name " + quoted(name) + " to be bare, as the printer writes the" +
                        " leading \"--\" and any \"=\" itself");
            }
            return super.visitFlag(flag, violations);
        }

        @Override
        public Docker.Healthcheck visitHealthcheck(Docker.Healthcheck healthcheck, List<String> violations) {
            if (healthcheck.isNone() && healthcheck.getFlags() != null && !healthcheck.getFlags().isEmpty()) {
                violations.add("expected HEALTHCHECK NONE to carry no flags, but it carries " +
                        healthcheck.getFlags().size() + ", which the printer drops");
            }
            return super.visitHealthcheck(healthcheck, violations);
        }

        @Override
        public Docker.ExecForm visitExecForm(Docker.ExecForm execForm, List<String> violations) {
            for (Docker.Literal argument : execForm.getArguments()) {
                doubleQuoted(argument, violations);
            }
            return super.visitExecForm(execForm, violations);
        }

        @Override
        public Docker.Shell visitShell(Docker.Shell shell, List<String> violations) {
            for (Docker.Argument argument : shell.getArguments()) {
                doubleQuotedContents(argument, violations);
            }
            return super.visitShell(shell, violations);
        }

        @Override
        public Docker.Volume visitVolume(Docker.Volume volume, List<String> violations) {
            if (volume.isJsonForm()) {
                for (Docker.Argument value : volume.getValues()) {
                    doubleQuotedContents(value, violations);
                }
            }
            return super.visitVolume(volume, violations);
        }

        @Override
        public Docker.Port visitPort(Docker.Port port, List<String> violations) {
            if (!port.isVariable()) {
                portModelsItsText(port, violations);
            }
            return super.visitPort(port, violations);
        }

        @Override
        public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm form, List<String> violations) {
            List<String> markers = new ArrayList<>();
            Matcher matcher = HEREDOC_MARKER.matcher(form.getPreamble());
            while (matcher.find()) {
                markers.add(matcher.group(1));
            }
            if (markers.size() != form.getBodies().size()) {
                violations.add("expected the preamble " + quoted(form.getPreamble()) + " to open one heredoc per" +
                        " body, but it opens " + markers.size() + " for " + form.getBodies().size() + " bodies");
            }
            if (!form.getBodies().isEmpty() &&
                    !form.getBodies().get(0).getPrefix().getWhitespace().contains("\n")) {
                violations.add("expected the heredoc opened by the preamble " + quoted(form.getPreamble()) +
                        " to begin on the line after it");
            }
            for (int i = 0; i < form.getBodies().size(); i++) {
                Docker.HeredocBody body = form.getBodies().get(i);
                if (i < markers.size() && !Heredocs.closes(markers.get(i), body.getClosing())) {
                    String name = Heredocs.delimiter(markers.get(i));
                    violations.add("expected the heredoc opened by " + quoted(name) + " to close with it," +
                            " but it closes with " + quoted(body.getClosing()));
                }
                if (!body.getOpening().startsWith("<<")) {
                    violations.add("expected the heredoc opening " + quoted(body.getOpening()) +
                            " to start with \"<<\"");
                }
                for (String line : body.getContentLines()) {
                    if (!line.endsWith("\n")) {
                        violations.add("expected the heredoc line " + quoted(line) + " to end with a newline");
                    }
                }
            }
            return super.visitHeredocForm(form, violations);
        }

        @Override
        public Docker.Argument visitArgument(Docker.Argument argument, List<String> violations) {
            List<Docker.ArgumentContent> contents = argument.getContents();
            for (int i = 1; i < contents.size(); i++) {
                if (i + 1 == contents.size() && isEmptyLiteral(contents.get(i))) {
                    continue;
                }
                Space prefix = contents.get(i).getPrefix();
                if (!prefix.getWhitespace().isEmpty() || !prefix.getComments().isEmpty()) {
                    violations.add("expected the contents of the argument " + quoted(ArgumentContents.textWithVariables(argument)) +
                            " to be contiguous, but one is preceded by " + quoted(prefix.toString()));
                }
            }
            return super.visitArgument(argument, violations);
        }

        @Override
        public Docker.Literal visitLiteral(Docker.Literal literal, List<String> violations) {
            Docker.Literal.QuoteStyle style = literal.getQuoteStyle();
            if (style == null) {
                outerWhitespace(literal, violations);
            }
            if (style != null && hasUnescapedQuote(literal.getText(), style)) {
                violations.add("expected the quoted literal " + quoted(literal.getText()) + " to hold no unescaped " +
                        quoteOf(style) + ", which ends the string when it is read back");
            }
            return super.visitLiteral(literal, violations);
        }

        /// An empty content closing an argument is the one place whitespace between contents is not that
        /// argument's text; see `DockerParserVisitor`.
        private static boolean isEmptyLiteral(Docker.ArgumentContent content) {
            return content instanceof Docker.Literal && ((Docker.Literal) content).getText().isEmpty();
        }

        /// A literal whose text runs into the whitespace around it hands every recipe reading its value
        /// the formatting too. Whitespace where two contents meet is the value's own text, as the space
        /// in `"pre $V post"` is.
        private void outerWhitespace(Docker.Literal literal, List<String> violations) {
            String text = literal.getText();
            if (text.isEmpty()) {
                return;
            }
            List<Docker.ArgumentContent> value = valueItIsPartOf(literal);
            if ((value.get(0) == literal && Character.isWhitespace(text.charAt(0))) ||
                    (value.get(value.size() - 1) == literal &&
                            Character.isWhitespace(text.charAt(text.length() - 1)))) {
                violations.add("expected the literal " + quoted(text) + " to hold only its value, but it starts or" +
                        " ends with whitespace that belongs in the space around it");
            }
        }

        /// The literal alone where it is a whole value of its own, as an `ARG`'s name is.
        private List<Docker.ArgumentContent> valueItIsPartOf(Docker.Literal literal) {
            Object parent = getCursor().getParentTreeCursor().getValue();
            return parent instanceof Docker.Argument ? ((Docker.Argument) parent).getContents() :
                    singletonList(literal);
        }

        /// A subtree copied without fresh ids still prints, but any visitor that navigates by id, and
        /// any recipe that edits one of the two, sees both.
        private void uniqueId(UUID id, Class<?> type, List<String> violations) {
            if (!ids.add(id)) {
                violations.add("expected every element to have its own id, but a " + type.getSimpleName() +
                        " repeats " + id);
            }
        }

        /// Every element that has a keyword spells it the same as its own type, so the type is the
        /// expectation and a new instruction is checked without anyone adding it here. Dockerfile
        /// keywords are case insensitive, so a lower case keyword is the one the source wrote.
        private void keyword(String keyword, Class<?> type, List<String> violations) {
            String element = type.getSimpleName();
            if (!element.equalsIgnoreCase(keyword)) {
                violations.add("expected the keyword of a " + element + " to be " +
                        element.toUpperCase(Locale.ROOT) + " ignoring case, but it is " + quoted(keyword));
            }
        }

        /// An instruction inserted without a prefix prints as `FROM alpineRUN x`.
        private void startsItsOwnLine(Docker d, List<String> violations) {
            if (!containsNewline(d.getPrefix())) {
                violations.add("expected the " + d.getClass().getSimpleName() + " to start its own line, but its" +
                        " prefix is " + quoted(d.getPrefix().getWhitespace()));
            }
        }

        private void nonEmpty(Docker.Argument argument, String what, List<String> violations) {
            if (argument.getContents().isEmpty()) {
                violations.add("expected " + what + " to hold at least one content");
            }
        }

        private void doubleQuotedContents(Docker.Argument argument, List<String> violations) {
            for (Docker.ArgumentContent content : argument.getContents()) {
                if (content instanceof Docker.Literal) {
                    doubleQuoted((Docker.Literal) content, violations);
                }
            }
        }

        /// A JSON array element that loses its quotes prints an array Docker refuses to read.
        private void doubleQuoted(Docker.Literal literal, List<String> violations) {
            if (literal.getQuoteStyle() != Docker.Literal.QuoteStyle.DOUBLE) {
                violations.add("expected the JSON array element " + quoted(literal.getText()) +
                        " to be double quoted, but it is " + (literal.isQuoted() ? "single quoted" : "unquoted"));
            }
        }

        /// A [Docker.Port] prints its text and models its fields, so the two can disagree silently.
        private void portModelsItsText(Docker.Port port, List<String> violations) {
            String text = port.getText();
            String ports = text;
            Docker.Port.Protocol protocol = Docker.Port.Protocol.TCP;
            int slash = text.indexOf('/');
            if (slash >= 0) {
                ports = text.substring(0, slash);
                String name = text.substring(slash + 1);
                if ("udp".equalsIgnoreCase(name)) {
                    protocol = Docker.Port.Protocol.UDP;
                } else if (!"tcp".equalsIgnoreCase(name)) {
                    violations.add("expected the port " + quoted(text) + " to name a protocol Docker knows");
                    return;
                }
            }
            Integer start;
            Integer end = null;
            int dash = ports.indexOf('-');
            try {
                start = Integer.valueOf(dash < 0 ? ports : ports.substring(0, dash));
                if (dash >= 0) {
                    end = Integer.valueOf(ports.substring(dash + 1));
                }
            } catch (NumberFormatException e) {
                violations.add("expected the port " + quoted(text) + " to hold a port number or a range of them");
                return;
            }
            if (!Objects.equals(start, port.getStart()) || !Objects.equals(end, port.getEnd()) ||
                    protocol != port.getProtocol()) {
                violations.add("expected the port " + quoted(text) + " to model " + range(start, end) + " " +
                        protocol + ", but it models " + range(port.getStart(), port.getEnd()) + " " +
                        port.getProtocol());
            }
            if (outOfRange(start) || outOfRange(end)) {
                violations.add("expected the port " + quoted(text) + " to be between 0 and 65535");
            }
        }

        private static String range(@Nullable Integer start, @Nullable Integer end) {
            return end == null ? String.valueOf(start) : start + "-" + end;
        }

        private static boolean outOfRange(@Nullable Integer port) {
            return port != null && (port < 0 || port > 65535);
        }

        /// A single quoted string has no escape processing, so any quote of its own style ends it; a
        /// double quoted one can hold an escaped quote. A literal does not know which escape character
        /// its file declared, so either counts here.
        private static boolean hasUnescapedQuote(String text, Docker.Literal.QuoteStyle style) {
            boolean escapable = style == Docker.Literal.QuoteStyle.DOUBLE;
            char quote = quoteOf(style);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (escapable && (c == '\\' || c == '`')) {
                    i++;
                } else if (c == quote) {
                    return true;
                }
            }
            return false;
        }

        private static char quoteOf(Docker.Literal.QuoteStyle style) {
            return style == Docker.Literal.QuoteStyle.DOUBLE ? '"' : '\'';
        }

        /// [Space#getLastWhitespace()] answers with the last comment's prefix when a [Space] holds
        /// comments, but [org.openrewrite.docker.internal.DockerPrinter] writes comments before the
        /// whitespace, so the text abutting the next token is always the whitespace.
        private static boolean containsNewline(Space space) {
            return space.getWhitespace().indexOf('\n') >= 0;
        }

        private static String quoted(String text) {
            return '"' + text.replace("\n", "\\n").replace("\t", "\\t") + '"';
        }
    }
}
