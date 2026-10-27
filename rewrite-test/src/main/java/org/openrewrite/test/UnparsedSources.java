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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the recipe with a source it changed in place of the file as a parse error, a quark and plain text, and fails
 * the test if the recipe throws there.
 */
final class UnparsedSources {

    private static final Set<List<Object>> PROBED = ConcurrentHashMap.newKeySet();

    private final Recipe recipe;
    private final Set<UUID> changed = new HashSet<>();

    UnparsedSources(Recipe recipe, Collection<Result> results) {
        this.recipe = recipe;
        for (Result result : results) {
            if (result.getBefore() != null) {
                changed.add(result.getBefore().getId());
            }
        }
    }

    void assertNoneThrow(List<SourceFile> sources, ExecutionContext recipeCtx, int cycles) {
        if (changed.isEmpty() || Recipe.noop().equals(recipe) || NameMatching.isTestScaffolding(recipe)) {
            return;
        }
        Set<Class<?>> probedClasses = new HashSet<>();
        for (SourceFile before : sources) {
            if (!changed.contains(before.getId()) || before instanceof Quark || before instanceof ParseError ||
                before instanceof Binary || before instanceof Remote || !probedClasses.add(before.getClass())) {
                continue;
            }
            for (Kind kind : Kind.values()) {
                List<Object> probe = Arrays.asList(recipe.getDescriptor(), before.getClass(), kind);
                if (kind.type.isInstance(before) || Probes.overBudget(recipe) || !PROBED.add(probe)) {
                    continue;
                }
                long start = System.nanoTime();
                try {
                    SourceFile unparsed = unparsed(before, kind);
                    Throwable thrown = unparsed == null ? null :
                            Probes.run(recipe, replace(sources, before, unparsed), recipeCtx, cycles);
                    if (unparsed != null && thrown != null) {
                        if (Probes.rejects(recipeCtx, thrown)) {
                            throw new AssertionError(message(unparsed, kind, thrown), thrown);
                        }
                        // a later test whose context doesn't tolerate errors should still see this
                        PROBED.remove(probe);
                    }
                } finally {
                    Probes.charge(recipe, System.nanoTime() - start);
                }
            }
        }
    }

    private static List<SourceFile> replace(List<SourceFile> sources, SourceFile before, SourceFile unparsed) {
        List<SourceFile> replaced = new ArrayList<>(sources.size());
        for (SourceFile source : sources) {
            replaced.add(source.getId().equals(before.getId()) ? unparsed : source);
        }
        return replaced;
    }

    /**
     * The file {@code before} was parsed from, as the kind of source a repository holds when its parser fails on the
     * file, leaves it unparsed or reads it as plain text, or null if it can't be printed.
     */
    private static @Nullable SourceFile unparsed(SourceFile before, Kind kind) {
        Markers markers = before.getMarkers();
        for (Marker marker : before.getMarkers().getMarkers()) {
            // these come from parsing the file itself
            if (marker instanceof ParseWarning || marker instanceof ParseExceptionResult ||
                marker.getClass().getSimpleName().endsWith("ResolutionResult")) {
                markers = markers.removeByType(marker.getClass());
            }
        }
        try {
            Charset charset = before.getCharset();
            String charsetName = charset == null ? null : charset.name();
            switch (kind) {
                case PARSE_ERROR:
                    return new ParseError(Tree.randomId(),
                            markers.add(new ParseExceptionResult(Tree.randomId(), "Parser",
                                    "IllegalStateException", "The file could not be parsed", null)),
                            before.getSourcePath(), before.getFileAttributes(), charsetName,
                            before.isCharsetBomMarked(), before.getChecksum(), before.printAll(), null);
                case QUARK:
                    return new Quark(Tree.randomId(), before.getSourcePath(), markers, before.getChecksum(),
                            before.getFileAttributes());
                default:
                    return new PlainText(Tree.randomId(), before.getSourcePath(), markers, charsetName,
                            before.isCharsetBomMarked(), before.getFileAttributes(), before.getChecksum(),
                            before.printAll(), null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String message(SourceFile unparsed, Kind kind, Throwable thrown) {
        StringBuilder message = new StringBuilder("Check the kind of source file before casting it, so that the " +
                                                  "recipe leaves a file it can't edit unchanged instead of throwing.\n")
                .append(recipe.getName()).append(" threw when ").append(unparsed.getSourcePath()).append(" was a ")
                .append(Probes.typeName(kind.type)).append(" (").append(kind.description).append(')');
        Set<String> markers = new LinkedHashSet<>();
        for (Marker marker : unparsed.getMarkers().getMarkers()) {
            if (!(marker instanceof ParseExceptionResult)) {
                markers.add(Probes.typeName(marker.getClass()));
            }
        }
        if (!markers.isEmpty()) {
            message.append(" with the markers of the test's source (").append(String.join(", ", markers)).append(')');
        }
        return message.append(":\n").append(Probes.describe(thrown, "    ", recipe))
                .append("\nBesides the files the test gives it, a repository holds files their parser failed on, ")
                .append("files left unparsed and files read as plain text, at the same paths and with the same ")
                .append("markers as files the recipe edits.\n")
                .append("  - visit with the visitor of the language the recipe edits, such as JavaIsoVisitor or ")
                .append("YamlIsoVisitor, whose isAcceptable skips other kinds of source file\n")
                .append("  - in a TreeVisitor<Tree, ExecutionContext>, check `instanceof` before a cast ")
                .append("(`tree instanceof G.CompilationUnit`), not the file's path or markers, and don't ask a ")
                .append("Quark or Binary for its charset\n")
                .append("  - when the scanner found no file of the kind the recipe edits, generate and change ")
                .append("nothing, instead of reading an empty accumulator\n")
                .append("  - don't catch the exception to make this pass")
                .toString();
    }

    private enum Kind {
        PARSE_ERROR(ParseError.class, "a file its parser failed on"),
        QUARK(Quark.class, "a file left unparsed"),
        PLAIN_TEXT(PlainText.class, "a file read as plain text");

        final Class<? extends SourceFile> type;
        final String description;

        Kind(Class<? extends SourceFile> type, String description) {
            this.type = type;
            this.description = description;
        }
    }
}
