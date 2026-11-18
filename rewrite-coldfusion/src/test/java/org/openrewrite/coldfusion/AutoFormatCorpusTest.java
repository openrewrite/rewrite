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
package org.openrewrite.coldfusion;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openrewrite.Cursor;
import org.openrewrite.TreeVisitor;
import org.openrewrite.coldfusion.format.*;
import org.openrewrite.coldfusion.internal.TemplateOutput;
import org.openrewrite.coldfusion.marker.KeywordSpelling;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.coldfusion.style.*;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.CfSpace;
import org.openrewrite.coldfusion.tree.TagComment;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.java.tree.*;
import org.openrewrite.jgit.diff.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Formats every file under {@code -Dcoldfusion.corpus.dir=...} with the style detected from its repository, each
 * directory under the corpus being one repository, and checks that the result parses and prints back, that only
 * whitespace changed, that a template writes what it wrote before, and that formatting again changes nothing. It
 * reports how many files and lines changed, which measures how well the detected style matches the code. Opt-in, like
 * {@link LstShapeCorpusTest}. {@code -Dcoldfusion.corpus.limit} checks only the first files.
 */
class AutoFormatCorpusTest {

    private static final List<String> PASSES = Arrays.asList("NormalizeFormat", "MinimumViableSpacing", "WrappingAndBraces",
            "Spaces", "BlankLines", "NormalizeTabsOrSpaces", "TabsAndIndents", "NormalizeLineBreaks", "RemoveTrailingWhitespace");

    @Test
    @EnabledIfSystemProperty(named = "coldfusion.corpus.dir", matches = ".+")
    void autoFormat() throws IOException {
        Path root = Paths.get(System.getProperty("coldfusion.corpus.dir"));
        ColdFusionParser parser = ColdFusionParser.builder().build();
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root, FileVisitOption.FOLLOW_LINKS)) {
            walk.filter(Files::isRegularFile).filter(parser::accept).sorted().forEach(paths::add);
        }
        int limit = Integer.getInteger("coldfusion.corpus.limit", Integer.MAX_VALUE);
        Map<Path, List<Path>> byRepository = new LinkedHashMap<>();
        for (Path path : paths.subList(0, Math.min(limit, paths.size()))) {
            byRepository.computeIfAbsent(root.resolve(root.relativize(path).getName(0)), r -> new ArrayList<>()).add(path);
        }

        Report report = new Report();
        for (Map.Entry<Path, List<Path>> repository : byRepository.entrySet()) {
            Repository repo = new Repository(repository.getKey().getFileName().toString());
            report.repositories.add(repo);
            List<Parsed> parsed = new ArrayList<>();
            for (Path path : repository.getValue()) {
                repo.files++;
                Parsed p = parse(repository.getKey(), path);
                if (p != null) {
                    parsed.add(p);
                }
            }
            Autodetect.Detector detector = Autodetect.detector();
            for (Parsed p : parsed) {
                detector.sample(p.cu);
            }
            Autodetect style = detector.build();
            repo.style = describe(style);
            for (Parsed p : parsed) {
                try {
                    check(p, style, repo, report);
                } catch (Throwable t) {
                    report.finding("harness", t.toString(), repo.name + "/" + p.relative, stackTop(t));
                }
            }
        }

        String text = report.render(root);
        System.out.println(text);
        Path out = Paths.get(System.getProperty("coldfusion.corpus.report", "build/coldfusion-format-report.txt"));
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.write(out, text.getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(out + ".details"), report.details.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static final class Parsed {
        final Path repository;
        final Path relative;
        final String source;
        final Cf.CompilationUnit cu;

        Parsed(Path repository, Path relative, String source, Cf.CompilationUnit cu) {
            this.repository = repository;
            this.relative = relative;
            this.source = source;
            this.cu = cu;
        }
    }

    private static @Nullable Parsed parse(Path repository, Path path) {
        Path relative = repository.relativize(path);
        try (InputStream in = Files.newInputStream(path);
             EncodingDetectingInputStream is = new EncodingDetectingInputStream(in)) {
            String source = ColdFusionParser.text(relative, is);
            Cf.CompilationUnit cu = ColdFusionParser.parse(relative, source, null, is.getCharset(), false);
            return cu.printAll().equals(source) ? new Parsed(repository, relative, source, cu) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void check(Parsed parsed, Autodetect style, Repository repo, Report report) {
        String where = repo.name + "/" + parsed.relative;
        Cf.CompilationUnit cu = parsed.cu.withMarkers(parsed.cu.getMarkers().add(style));
        repo.parsed++;
        repo.lines += lines(parsed.source);

        Cf.CompilationUnit formattedTree = (Cf.CompilationUnit) new AutoFormatVisitor<Integer>().visit(cu, 0);
        String formatted = Objects.requireNonNull(formattedTree).printAll();

        // which pass changes how many lines
        String previous = parsed.source;
        J pass = cu;
        for (int i = 0; i < PASSES.size(); i++) {
            pass = Objects.requireNonNull(passVisitor(i, cu).visit(pass, 0));
            String printed = ((Cf.CompilationUnit) pass).printAll();
            repo.passLines[i] += changed(previous, printed);
            previous = printed;
        }
        if (!previous.equals(formatted)) {
            report.finding("pipeline", "the passes run one by one format differently from AutoFormatVisitor", where, "");
        }

        if (formatted.equals(parsed.source)) {
            return;
        }
        repo.changedFiles++;
        int changedLines = changed(parsed.source, formatted);
        repo.changedLines += changedLines;
        report.detail(where, changedLines, parsed.source, formatted);

        Cf.CompilationUnit reparsed;
        try {
            reparsed = ColdFusionParser.parse(parsed.relative, formatted, null, StandardCharsets.UTF_8, false);
        } catch (Throwable t) {
            repo.failures++;
            report.finding("reparse", "does not parse: " + firstLine(t.getMessage()), where, around(formatted, 0));
            return;
        }
        if (!reparsed.printAll().equals(formatted)) {
            repo.failures++;
            report.finding("reparse", "does not print back", where, "");
            return;
        }
        String shapeBefore = shape(parsed.cu);
        String shapeAfter = shape(reparsed);
        if (!shapeBefore.equals(shapeAfter)) {
            repo.failures++;
            report.finding("structure", "changed more than whitespace", where, firstDifference(shapeBefore, shapeAfter));
        }
        String outputBefore = output(parsed.cu);
        String outputAfter = output(reparsed);
        if (!outputBefore.equals(outputAfter)) {
            repo.failures++;
            report.finding("output", "changed what the template writes", where, firstDifference(outputBefore, outputAfter));
        }
        Cf.CompilationUnit again = (Cf.CompilationUnit) new AutoFormatVisitor<Integer>()
                .visit(reparsed.withMarkers(reparsed.getMarkers().add(style)), 0);
        String twice = Objects.requireNonNull(again).printAll();
        if (!twice.equals(formatted)) {
            repo.failures++;
            report.finding("idempotence", "a second run changes it", where, firstDifference(formatted, twice));
        }
    }

    private static TreeVisitor<J, Integer> passVisitor(int i, Cf.CompilationUnit cu) {
        switch (PASSES.get(i)) {
            case "NormalizeFormat":
                return new NormalizeFormatVisitor<>();
            case "MinimumViableSpacing":
                return new MinimumViableSpacingVisitor<>();
            case "WrappingAndBraces":
                return new WrappingAndBracesVisitor<>(style(WrappingAndBracesStyle.class, cu, CFFormat.wrappingAndBraces()));
            case "Spaces":
                return new SpacesVisitor<>(style(SpacesStyle.class, cu, CFFormat.spaces()));
            case "BlankLines":
                return new BlankLinesVisitor<>(style(BlankLinesStyle.class, cu, CFFormat.blankLines()));
            case "NormalizeTabsOrSpaces":
                return new NormalizeTabsOrSpacesVisitor<>(style(TabsAndIndentsStyle.class, cu, CFFormat.tabsAndIndents()));
            case "TabsAndIndents":
                return new TabsAndIndentsVisitor<>(style(TabsAndIndentsStyle.class, cu, CFFormat.tabsAndIndents()));
            case "NormalizeLineBreaks":
                return new NormalizeLineBreaksVisitor<>(style(GeneralFormatStyle.class, cu, GeneralFormatStyle.DEFAULT));
            default:
                return new RemoveTrailingWhitespaceVisitor<>(style(TabsAndIndentsStyle.class, cu, CFFormat.tabsAndIndents()), null);
        }
    }

    private static <S extends Style> S style(Class<S> styleClass, Cf.CompilationUnit cu, S otherwise) {
        S s = NamedStyles.merge(styleClass, cu.getMarkers().findAll(NamedStyles.class));
        return s == null ? otherwise : s;
    }

    /**
     * The tree with its whitespace left out: each element's type and the names, values, text and comments in it.
     */
    private static String shape(Cf.CompilationUnit cu) {
        StringBuilder shape = new StringBuilder();
        new ColdFusionIsoVisitor<StringBuilder>() {
            @Override
            public @Nullable J preVisit(J tree, StringBuilder out) {
                out.append(tree.getClass().getSimpleName());
                if (tree instanceof J.Identifier) {
                    out.append(':').append(((J.Identifier) tree).getSimpleName());
                } else if (tree instanceof J.Literal) {
                    out.append(':').append(((J.Literal) tree).getValueSource());
                } else if (tree instanceof Cf.Text) {
                    out.append(':').append(((Cf.Text) tree).getText());
                } else if (tree instanceof Cf.Tag) {
                    out.append(':').append(((Cf.Tag) tree).getName());
                } else if (tree instanceof J.Modifier) {
                    out.append(':').append(((J.Modifier) tree).getKeyword());
                }
                for (Marker marker : tree.getMarkers().getMarkers()) {
                    out.append('@').append(marker.getClass().getSimpleName());
                    if (marker instanceof OperatorSpelling) {
                        out.append(((OperatorSpelling) marker).getText());
                    } else if (marker instanceof KeywordSpelling) {
                        out.append(((KeywordSpelling) marker).getText());
                    }
                }
                out.append('\n');
                return tree;
            }

            @Override
            public <T> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, JRightPadded.Location loc, StringBuilder out) {
                if (right != null) {
                    for (Marker marker : right.getMarkers().getMarkers()) {
                        out.append("padding@").append(marker.getClass().getSimpleName()).append('\n');
                    }
                }
                return super.visitRightPadded(right, loc, out);
            }

            @Override
            public Space visitSpace(Space space, Space.Location loc, StringBuilder out) {
                for (Comment comment : space.getComments()) {
                    String text = comment instanceof TagComment ? ((TagComment) comment).getText() :
                            comment instanceof TextComment ? ((TextComment) comment).getText() : comment.toString();
                    out.append("comment:").append(text.replaceAll("[\\r\\n]+[ \\t]*", "\n")).append('\n');
                }
                return space;
            }

            @Override
            public Space visitSpace(Space space, CfSpace.Location loc, StringBuilder out) {
                return visitSpace(space, Space.Location.LANGUAGE_EXTENSION, out);
            }
        }.visit(cu, shape);
        return shape.toString();
    }

    /**
     * What a template writes, as far as whitespace can change it: its text and the whitespace between its tags that
     * it writes.
     */
    private static String output(Cf.CompilationUnit cu) {
        StringBuilder output = new StringBuilder();
        new ColdFusionIsoVisitor<StringBuilder>() {
            @Override
            public @Nullable J preVisit(J tree, StringBuilder out) {
                Cursor cursor = getCursor();
                if (tree instanceof Statement && !(tree instanceof Cf.CompilationUnit) && TemplateOutput.isInTemplateList(cursor) &&
                    TemplateOutput.writesBefore(cursor)) {
                    out.append("[").append(printed(tree.getPrefix())).append("]");
                }
                if (tree instanceof Cf.Text) {
                    out.append(((Cf.Text) tree).getText());
                } else if (tree instanceof Cf.Interpolation) {
                    out.append("#interpolation#");
                }
                return tree;
            }

            @Override
            public J.Block visitBlock(J.Block block, StringBuilder out) {
                J.Block b = super.visitBlock(block, out);
                if (TemplateOutput.isTemplateBody(getCursor()) && TemplateOutput.writesAtEnd(getCursor())) {
                    out.append("[end:").append(printed(b.getEnd())).append("]");
                }
                return b;
            }

            @Override
            public Cf.CompilationUnit visitCompilationUnit(Cf.CompilationUnit compilationUnit, StringBuilder out) {
                Cf.CompilationUnit c = super.visitCompilationUnit(compilationUnit, out);
                if (TemplateOutput.isTemplate(c) && TemplateOutput.writesAtEnd(getCursor())) {
                    out.append("[eof:").append(printed(c.getEof())).append("]");
                }
                return c;
            }
        }.visit(cu, output);
        return output.toString();
    }

    private static String printed(Space space) {
        StringBuilder s = new StringBuilder(space.getWhitespace());
        for (Comment comment : space.getComments()) {
            s.append(comment instanceof TagComment ? "<!---" + ((TagComment) comment).getText() + "--->" : comment.toString());
            s.append(comment.getSuffix());
        }
        return s.toString();
    }

    private static int changed(String before, String after) {
        if (before.equals(after)) {
            return 0;
        }
        RawText a = new RawText(before.getBytes(StandardCharsets.UTF_8));
        RawText b = new RawText(after.getBytes(StandardCharsets.UTF_8));
        EditList edits = new HistogramDiff().diff(RawTextComparator.DEFAULT, a, b);
        int changed = 0;
        for (Edit edit : edits) {
            changed += Math.max(edit.getEndA() - edit.getBeginA(), edit.getEndB() - edit.getBeginB());
        }
        return changed;
    }

    private static int lines(String source) {
        int lines = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n' || source.charAt(i) == '\r' && (i + 1 == source.length() || source.charAt(i + 1) != '\n')) {
                lines++;
            }
        }
        return lines;
    }

    private static String describe(Autodetect autodetect) {
        TabsAndIndentsStyle tabs = autodetect.getStyle(TabsAndIndentsStyle.class);
        SpacesStyle spaces = autodetect.getStyle(SpacesStyle.class);
        BlankLinesStyle blank = autodetect.getStyle(BlankLinesStyle.class);
        WrappingAndBracesStyle wrapping = autodetect.getStyle(WrappingAndBracesStyle.class);
        return (tabs.getUseTabCharacter() ? "tabs" : tabs.getIndentSize() + " spaces") +
               ", continuation " + tabs.getContinuationIndent() +
               (tabs.getTags().getIndentComponentBody() ? "" : ", flat <cfcomponent>") +
               (tabs.getTags().getIndentScriptBody() ? ", indented <cfscript>" : "") +
               (spaces.getWithin().getFunctionCallParentheses() ? ", padded calls" : "") +
               (spaces.getWithin().getKeywordParentheses() ? ", padded keywords" : "") +
               (spaces.getAroundOperators().getBinary() ? "" : ", tight operators") +
               (wrapping.getBraces().getStatementOnNewLine() ? ", braces on new lines" : "") +
               (wrapping.getKeywordsAfterBlock().getElseOnNewLine() ? ", else on new line" : "") +
               ", blank lines " + blank.getKeepMaximum().getInCode() + "/" + blank.getKeepMaximum().getInDeclarations() +
               "/" + blank.getKeepMaximum().getBeforeEndOfBlock() + " min " + blank.getMinimum().getAroundFunction();
    }

    private static final class Repository {
        final String name;
        int files;
        int parsed;
        long lines;
        int changedFiles;
        long changedLines;
        int failures;
        final long[] passLines = new long[PASSES.size()];
        String style = "";

        Repository(String name) {
            this.name = name;
        }
    }

    private static final class Report {
        final List<Repository> repositories = new ArrayList<>();
        final Map<String, List<String[]>> findings = new TreeMap<>();
        final StringBuilder details = new StringBuilder();

        void finding(String kind, String what, String where, String context) {
            findings.computeIfAbsent(kind + ": " + what, k -> new ArrayList<>()).add(new String[]{where, context});
        }

        void detail(String where, int changedLines, String before, String after) {
            details.append("=== ").append(where).append(" (").append(changedLines).append(" lines)\n");
            RawText a = new RawText(before.getBytes(StandardCharsets.UTF_8));
            RawText b = new RawText(after.getBytes(StandardCharsets.UTF_8));
            EditList edits = new HistogramDiff().diff(RawTextComparator.DEFAULT, a, b);
            int shown = 0;
            for (Edit edit : edits) {
                if (shown++ == 3) {
                    break;
                }
                for (int i = edit.getBeginA(); i < Math.min(edit.getEndA(), edit.getBeginA() + 4); i++) {
                    details.append("- ").append(visible(a.getString(i))).append('\n');
                }
                for (int i = edit.getBeginB(); i < Math.min(edit.getEndB(), edit.getBeginB() + 4); i++) {
                    details.append("+ ").append(visible(b.getString(i))).append('\n');
                }
            }
        }

        String render(Path root) {
            StringBuilder out = new StringBuilder();
            long files = 0, parsed = 0, lines = 0, changedFiles = 0, changedLines = 0, failures = 0;
            long[] passLines = new long[PASSES.size()];
            for (Repository r : repositories) {
                files += r.files;
                parsed += r.parsed;
                lines += r.lines;
                changedFiles += r.changedFiles;
                changedLines += r.changedLines;
                failures += r.failures;
                for (int i = 0; i < passLines.length; i++) {
                    passLines[i] += r.passLines[i];
                }
            }
            out.append("AutoFormat of ").append(parsed).append(" parsed files (").append(files).append(" files, ")
                    .append(repositories.size()).append(" repositories) under ").append(root).append('\n');
            out.append(String.format("changed %d files (%.1f%%), %d of %d lines (%.2f%%), %d files failing a check%n",
                    changedFiles, 100.0 * changedFiles / Math.max(1, parsed), changedLines, lines,
                    100.0 * changedLines / Math.max(1, lines), failures));
            out.append("\nlines changed by each pass:\n");
            for (int i = 0; i < PASSES.size(); i++) {
                out.append(String.format("  %-24s %d%n", PASSES.get(i), passLines[i]));
            }
            out.append("\nfindings:\n");
            for (Map.Entry<String, List<String[]>> finding : findings.entrySet()) {
                out.append(String.format("  %6d  %s%n", finding.getValue().size(), finding.getKey()));
                for (String[] example : finding.getValue().subList(0, Math.min(5, finding.getValue().size()))) {
                    out.append("            ").append(example[0]).append("  ").append(visible(example[1])).append('\n');
                }
            }
            out.append("\nrepositories, by lines changed per thousand:\n");
            List<Repository> sorted = new ArrayList<>(repositories);
            sorted.sort(Comparator.comparingDouble((Repository r) -> -perThousand(r)));
            for (Repository r : sorted) {
                out.append(String.format("  %7.1f  %5d/%-5d files  %7d/%-8d lines  %3d failing  %-50s %s%n", perThousand(r),
                        r.changedFiles, r.parsed, r.changedLines, r.lines, r.failures, r.name, r.style));
            }
            return out.toString();
        }

        private static double perThousand(Repository r) {
            return 1000.0 * r.changedLines / Math.max(1, r.lines);
        }
    }

    private static String visible(String s) {
        String v = s.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
        return v.length() > 200 ? v.substring(0, 200) + "..." : v;
    }

    private static String firstLine(@Nullable String s) {
        if (s == null) {
            return "";
        }
        int end = s.indexOf('\n');
        return end < 0 ? s : s.substring(0, end);
    }

    private static String around(String s, int at) {
        return s.substring(Math.max(0, at - 40), Math.min(s.length(), at + 40));
    }

    private static String firstDifference(String a, String b) {
        int at = 0;
        while (at < a.length() && at < b.length() && a.charAt(at) == b.charAt(at)) {
            at++;
        }
        return around(a, at) + "  |  " + around(b, at);
    }

    private static String stackTop(Throwable t) {
        StringBuilder s = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            if (s.length() > 300) {
                break;
            }
            s.append(e.getClassName().replaceAll(".*\\.", "")).append('.').append(e.getMethodName()).append(':').append(e.getLineNumber()).append(' ');
        }
        return s.toString();
    }
}
