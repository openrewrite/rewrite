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
package org.openrewrite.test;

import lombok.Value;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.binary.Binary;
import org.openrewrite.jgit.diff.*;
import org.openrewrite.quark.Quark;

import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds lines that a recipe edits in place and gives a different indentation. A line is edited in place when the
 * recipe changes what it says and the lines around it don't change: one line in, one line out, between lines the
 * recipe left alone. Such a line keeps the leading whitespace it had, whatever indentation the new code would have had
 * in a file formatted from scratch.
 * <p>
 * What isn't held to its old indentation:
 * <ul>
 *     <li>Lines whose only change is their indentation, lines a recipe adds or removes, and runs of consecutive changed
 *     lines, which are usually code that changed shape, such as an if-else chain that became switch cases, and that
 *     may legitimately nest at another depth.</li>
 *     <li>A line edited inside code that moved or changed how deeply it nests: one whose nearest unchanged neighbor
 *     above or below also changed its indentation.</li>
 *     <li>A line that replaced a line it shares too little of its text with to be the same line edited, as when a
 *     recipe removes one line and adds an unrelated one at the same place.</li>
 *     <li>The space that aligns the leading {@code *} of a doc comment's line under its opener, which a line loses or
 *     gains when it changes to or from that style.</li>
 *     <li>YAML, whose indentation is its structure, so moving a key to another parent changes its indentation.</li>
 * </ul>
 */
final class EditedLineIndentation {
    private static final int MAX_REPORTED_PER_FILE = 10;

    /**
     * How much of their text, without indentation, two lines share to be one line edited: twice the length of their
     * longest common subsequence of characters over the sum of their lengths.
     */
    private static final double SAME_LINE_SIMILARITY = 0.4;

    /**
     * The longest stretch of a line compared for similarity, which bounds the cost of comparing generated code.
     */
    private static final int MAX_COMPARED_LENGTH = 400;

    private EditedLineIndentation() {
    }

    static void assertKept(Recipe recipe, Collection<Result> results) {
        if (FormattingScope.isFormatter(recipe)) {
            return;
        }
        Map<String, List<Reindented>> found = new LinkedHashMap<>();
        for (Result result : results) {
            SourceFile before = result.getBefore();
            SourceFile after = result.getAfter();
            if (before == null || after == null || isBinary(before) || isBinary(after) ||
                indentationIsStructure(before) || indentationIsStructure(after)) {
                continue;
            }
            List<Reindented> reindented = reindented(before.printAll(), after.printAll());
            if (!reindented.isEmpty()) {
                found.put(after.getSourcePath().toString(), reindented);
            }
        }
        if (!found.isEmpty()) {
            fail(message(found));
        }
    }

    /**
     * The lines of {@code after} that replace a line of {@code before} one for one, with different code and different
     * leading whitespace, among neighbors that kept theirs.
     */
    static List<Reindented> reindented(String before, String after) {
        RawText a = new RawText(before.getBytes(UTF_8));
        RawText b = new RawText(after.getBytes(UTF_8));
        // Lines that differ only in their indentation match, so what is left to replace is code the recipe changed
        EditList edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM)
                .diff(RawTextComparator.WS_IGNORE_LEADING, a, b);
        List<Reindented> reindented = new ArrayList<>();
        for (int e = 0; e < edits.size(); e++) {
            Edit edit = edits.get(e);
            if (edit.getType() != Edit.Type.REPLACE || edit.getLengthA() != 1 || edit.getLengthB() != 1) {
                continue;
            }
            String was = a.getString(edit.getBeginA());
            String is = b.getString(edit.getBeginB());
            if (!was.trim().isEmpty() && !is.trim().isEmpty() && !sameIndentation(was, is) &&
                similarity(was.trim(), is.trim()) >= SAME_LINE_SIMILARITY && neighborsKeptIndentation(a, b, edits, e)) {
                reindented.add(new Reindented(edit.getBeginB() + 1, was, is));
            }
        }
        return reindented;
    }

    /**
     * Whether two versions of a line have the same indentation, but for the space that aligns a doc comment's leading
     * {@code *} under its opener, when only one of them has that {@code *}.
     */
    private static boolean sameIndentation(String was, String is) {
        String wasIndent = indentation(was);
        String isIndent = indentation(is);
        if (wasIndent.equals(isIndent)) {
            return true;
        }
        boolean wasStar = was.trim().startsWith("*");
        boolean isStar = is.trim().startsWith("*");
        return wasStar && !isStar && wasIndent.equals(isIndent + " ") ||
               isStar && !wasStar && isIndent.equals(wasIndent + " ");
    }

    /**
     * Whether the nearest non-blank lines above and below the edit, which the diff matched as unchanged but for their
     * indentation, kept their indentation. A neighbor that moved with the edit means the edit is inside code that
     * moved or changed how deeply it nests.
     */
    private static boolean neighborsKeptIndentation(RawText a, RawText b, EditList edits, int e) {
        Edit edit = edits.get(e);
        int aboveLimitA = e == 0 ? 0 : edits.get(e - 1).getEndA();
        for (int i = edit.getBeginA() - 1, j = edit.getBeginB() - 1; i >= aboveLimitA && j >= 0; i--, j--) {
            if (!a.getString(i).trim().isEmpty()) {
                if (!indentation(a.getString(i)).equals(indentation(b.getString(j)))) {
                    return false;
                }
                break;
            }
        }
        int belowLimitA = e == edits.size() - 1 ? a.size() : edits.get(e + 1).getBeginA();
        for (int i = edit.getEndA(), j = edit.getEndB(); i < belowLimitA && j < b.size(); i++, j++) {
            if (!a.getString(i).trim().isEmpty()) {
                return indentation(a.getString(i)).equals(indentation(b.getString(j)));
            }
        }
        return true;
    }

    /**
     * Twice the length of the longest common subsequence of characters of two strings over the sum of their lengths:
     * 1 for equal strings and 0 for strings with no character in common.
     */
    static double similarity(String s, String t) {
        String x = s.length() > MAX_COMPARED_LENGTH ? s.substring(0, MAX_COMPARED_LENGTH) : s;
        String y = t.length() > MAX_COMPARED_LENGTH ? t.substring(0, MAX_COMPARED_LENGTH) : t;
        if (x.isEmpty() && y.isEmpty()) {
            return 1;
        }
        int[] previous = new int[y.length() + 1];
        int[] current = new int[y.length() + 1];
        for (int i = 1; i <= x.length(); i++) {
            for (int j = 1; j <= y.length(); j++) {
                current[j] = x.charAt(i - 1) == y.charAt(j - 1) ? previous[j - 1] + 1 :
                        Math.max(previous[j], current[j - 1]);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return 2.0 * previous[y.length()] / (x.length() + y.length());
    }

    private static boolean isBinary(SourceFile sourceFile) {
        return sourceFile instanceof Binary || sourceFile instanceof Quark;
    }

    private static boolean indentationIsStructure(SourceFile sourceFile) {
        String type = sourceFile.getClass().getName();
        return type.startsWith("org.openrewrite.yaml.") || type.startsWith("org.openrewrite.python.");
    }

    private static String indentation(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return line.substring(0, i);
    }

    private static String visible(String line) {
        String indent = indentation(line);
        String code = line.substring(indent.length());
        if (code.endsWith("\r")) {
            code = code.substring(0, code.length() - 1);
        }
        return indent.replace(' ', '·').replace('\t', '→') + code;
    }

    private static String message(Map<String, List<Reindented>> found) {
        StringBuilder message = new StringBuilder("Keep the indentation of the lines the recipe edits in place. These " +
                                                  "lines changed their code and their leading whitespace (· is a space, → a tab):\n");
        for (Map.Entry<String, List<Reindented>> file : found.entrySet()) {
            List<Reindented> lines = file.getValue();
            for (Reindented line : lines.subList(0, Math.min(lines.size(), MAX_REPORTED_PER_FILE))) {
                message.append("  ").append(file.getKey()).append(':').append(line.getLine()).append('\n')
                        .append("    before: ").append(visible(line.getBefore())).append('\n')
                        .append("    after:  ").append(visible(line.getAfter())).append('\n');
            }
            if (lines.size() > MAX_REPORTED_PER_FILE) {
                message.append("  ... and ").append(lines.size() - MAX_REPORTED_PER_FILE).append(" more in ")
                        .append(file.getKey()).append('\n');
            }
        }
        return message.append("Code that replaces the first element on a line takes over the prefix of the element it ")
                .append("replaced, including when a call chain collapses onto an inner element. A recipe that means to ")
                .append("reindent the lines it edits opts out with ")
                .append("RecipeQualityValidation.builder().keepEditedLineIndentation(false).\n")
                .toString();
    }

    @Value
    static class Reindented {
        /**
         * The line's number in the recipe's output, from 1.
         */
        int line;

        String before;
        String after;
    }
}
