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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.marker.*;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.tree.ParseError;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds markers in a recipe's output that neither its input nor a fresh parse of its output carry in the same kind
 * of place (on the source file, or on an element).
 */
final class AddedMarkers {

    /**
     * Markers that recipes in rewrite add on purpose: to apply a change only once, to steer the formatting of a
     * merged YAML scalar, or to say which method a search matched.
     */
    private static final Set<String> REWRITE_RECIPE_MARKERS = new HashSet<>(Arrays.asList(
            "org.openrewrite.marker.AlreadyReplaced",
            "org.openrewrite.maven.marker.AlreadyIncremented",
            "org.openrewrite.yaml.MultilineScalarChanged",
            "org.openrewrite.java.search.UsesMethod$MethodMatch"
    ));

    private static final int SNIPPET_LENGTH = 60;

    /**
     * The fields of each class that holds a {@link Markers} directly, or null for any other class.
     */
    private static final ClassValue<@Nullable List<Field>> MARKER_HOLDER_FIELDS = new ClassValue<@Nullable List<Field>>() {
        @Override
        protected @Nullable List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            boolean holdsMarkers = false;
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) ||
                        field.isSynthetic() || field.getType().isPrimitive()) {
                        continue;
                    }
                    holdsMarkers |= Markers.class.equals(field.getType());
                    fields.add(field);
                }
            }
            if (!holdsMarkers) {
                return null;
            }
            List<Field> accessible = new ArrayList<>(fields.size());
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    accessible.add(field);
                } catch (RuntimeException ignored) {
                    // a field the test runtime won't let us read can't be checked
                }
            }
            return accessible;
        }
    };

    private final Set<Placement> explained = new HashSet<>();
    private final Function<SourceFile, List<Parser.Builder>> parsersFor;
    private final @Nullable Path relativeTo;
    private final ExecutionContext ctx;

    AddedMarkers(Collection<? extends SourceFile> before, Function<SourceFile, List<Parser.Builder>> parsersFor,
                 @Nullable Path relativeTo, ExecutionContext ctx) {
        this.parsersFor = parsersFor;
        this.relativeTo = relativeTo;
        this.ctx = ctx;
        for (SourceFile sourceFile : before) {
            explained.addAll(placements(sourceFile).keySet());
        }
    }

    void assertNoneAdded(Collection<Result> results) {
        Map<Result, List<Occurrence>> added = new LinkedHashMap<>();
        for (Result result : results) {
            SourceFile after = result.getAfter();
            if (after == null) {
                continue;
            }
            List<Occurrence> unexplained = new ArrayList<>();
            for (Occurrence occurrence : placements(after).values()) {
                if (!explained.contains(occurrence.getPlacement()) && !mayBeAdded(occurrence)) {
                    unexplained.add(occurrence);
                }
            }
            if (!unexplained.isEmpty()) {
                // A recipe that writes code may bring along syntax markers that only a parser knows to produce
                SourceFile reparsed = reparse(after, emptyList(), parsersFor, relativeTo, ctx);
                if (reparsed != null) {
                    Set<Placement> fromParser = placements(reparsed).keySet();
                    unexplained.removeIf(occurrence -> fromParser.contains(occurrence.getPlacement()));
                }
            }
            if (!unexplained.isEmpty()) {
                added.put(result, unexplained);
            }
        }
        if (!added.isEmpty()) {
            fail(message(added));
        }
    }

    private static boolean mayBeAdded(Occurrence occurrence) {
        Marker marker = occurrence.getMarker();
        return marker instanceof SearchResult ||
               marker instanceof Markup ||
               marker instanceof CommitMessage ||
               marker instanceof RecipesThatMadeChanges ||
               // a recipe that formats with a style it was given passes the style on to later formatting
               marker instanceof NamedStyles && occurrence.getPlacement().isOnSourceFile() ||
               REWRITE_RECIPE_MARKERS.contains(marker.getClass().getName());
    }

    /**
     * A fresh parse of the source's printed output by the first parser that turns it into the same kind of source,
     * parsed together with {@code others} so that it can refer to the types they declare.
     */
    static @Nullable SourceFile reparse(SourceFile after, Collection<SourceFile> others,
                                        Function<SourceFile, List<Parser.Builder>> parsersFor,
                                        @Nullable Path relativeTo, ExecutionContext ctx) {
        if (after instanceof Quark || after instanceof Binary || after instanceof Remote || after instanceof ParseError) {
            return null;
        }
        for (Parser.Builder builder : parsersFor.apply(after)) {
            try {
                ExecutionContext reparseCtx = new InMemoryExecutionContext(t -> {
                });
                ctx.getMessages().forEach(reparseCtx::putMessage);
                reparseCtx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);

                List<Parser.Input> inputs = new ArrayList<>(others.size() + 1);
                inputs.add(printed(after, relativeTo));
                for (SourceFile other : others) {
                    inputs.add(printed(other, relativeTo));
                }
                SourceFile reparsed = null;
                List<SourceFile> parsedInputs = builder.clone().build()
                        .parseInputs(inputs, relativeTo, reparseCtx).collect(toList());
                for (SourceFile parsed : parsedInputs) {
                    if (reparsed == null || parsed.getSourcePath().equals(after.getSourcePath())) {
                        reparsed = parsed;
                    }
                }
                if (reparsed != null && reparsed.getClass() == after.getClass()) {
                    return reparsed;
                }
            } catch (Exception | LinkageError ignored) {
                // a parser that can't parse it explains nothing
            }
        }
        return null;
    }

    private static Parser.Input printed(SourceFile source, @Nullable Path relativeTo) {
        Charset charset = source.getCharset() == null ? StandardCharsets.UTF_8 : source.getCharset();
        String printed = source.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
        Path sourcePath = relativeTo == null ? source.getSourcePath() : relativeTo.resolve(source.getSourcePath());
        return Parser.Input.fromString(sourcePath, printed, charset);
    }

    private static Map<Placement, Occurrence> placements(SourceFile sourceFile) {
        Map<Placement, Occurrence> placements = new LinkedHashMap<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree tree, Integer p) {
                add(placements, tree.getMarkers(), tree instanceof SourceFile, tree, tree, getCursor());
                List<Field> fields = MARKER_HOLDER_FIELDS.get(tree.getClass());
                if (fields != null) {
                    for (Field field : fields) {
                        Object value = read(field, tree);
                        if (!(value instanceof Markers)) {
                            scanPadding(value, tree, getCursor(), placements, 0);
                        }
                    }
                }
                return tree;
            }
        }.visit(sourceFile, 0);
        return placements;
    }

    /**
     * Markers also sit on the padding around elements, which language visitors don't all visit.
     */
    private static void scanPadding(@Nullable Object value, Tree owner, Cursor cursor,
                                    Map<Placement, Occurrence> placements, int depth) {
        if (value == null || value instanceof Tree || depth > 8) {
            // trees are visited on their own
            return;
        }
        if (value instanceof Collection) {
            for (Object element : (Collection<?>) value) {
                scanPadding(element, owner, cursor, placements, depth + 1);
            }
            return;
        }
        List<Field> fields = MARKER_HOLDER_FIELDS.get(value.getClass());
        if (fields == null) {
            return;
        }
        for (Field field : fields) {
            Object fieldValue = read(field, value);
            if (fieldValue instanceof Markers) {
                add(placements, (Markers) fieldValue, false, value, owner, cursor);
            } else {
                scanPadding(fieldValue, owner, cursor, placements, depth + 1);
            }
        }
    }

    private static @Nullable Object read(Field field, Object o) {
        try {
            return field.get(o);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void add(Map<Placement, Occurrence> placements, @Nullable Markers markers, boolean onSourceFile,
                            Object holder, Tree owner, Cursor cursor) {
        if (markers == null) {
            return;
        }
        for (Marker marker : markers.getMarkers()) {
            //noinspection ConstantValue
            if (marker != null) {
                Placement placement = new Placement(onSourceFile, marker.getClass().getName());
                placements.putIfAbsent(placement, new Occurrence(placement, marker, holder, owner, cursor));
            }
        }
    }

    private static String message(Map<Result, List<Occurrence>> added) {
        StringBuilder message = new StringBuilder("The recipe added markers to the LST that neither the sources it was " +
                                                  "given nor a fresh parse of its output contain:\n");
        for (Map.Entry<Result, List<Occurrence>> entry : added.entrySet()) {
            Result result = entry.getKey();
            SourceFile after = Objects.requireNonNull(result.getAfter());
            message.append("  ").append(after.getSourcePath());
            if (result.getBefore() == null) {
                message.append(" (generated)");
            } else if (printsTheSame(result.getBefore(), after)) {
                message.append(" (the printed source is unchanged)");
            }
            message.append('\n');
            for (Occurrence occurrence : entry.getValue()) {
                message.append("    ").append(occurrence.getPlacement().getMarkerType())
                        .append(" on ").append(describe(occurrence)).append('\n');
            }
        }
        return message.append("Markers travel with the LST to every recipe that runs after this one and to every tool ")
                .append("that reads the result, so they are not a place for a recipe to keep information for itself. Instead:\n")
                .append("  - share information between the visit methods of one visitor with cursor messages, e.g. ")
                .append("getCursor().putMessageOnFirstEnclosing(..) and getCursor().getNearestMessage(..)\n")
                .append("  - share information between source files, or from scanning to editing, through the ")
                .append("accumulator of a ScanningRecipe\n")
                .append("  - keep the recipe idempotent by recognizing code it has already changed, rather than tagging that code\n")
                .append("  - report a finding with SearchResult, and a problem with Markup\n")
                .toString();
    }

    private static boolean printsTheSame(SourceFile before, SourceFile after) {
        try {
            return before.printAll().equals(after.printAll());
        } catch (Exception e) {
            return false;
        }
    }

    private static String describe(Occurrence occurrence) {
        if (occurrence.getPlacement().isOnSourceFile()) {
            return "the source file";
        }
        Tree owner = occurrence.getOwner();
        String description = occurrence.getHolder() == owner ? typeName(owner.getClass()) :
                typeName(occurrence.getHolder().getClass()) + " of " + typeName(owner.getClass());
        String snippet = snippet(owner, occurrence.getCursor());
        return snippet.isEmpty() ? description : description + " `" + snippet + "`";
    }

    static String typeName(Class<?> type) {
        String name = type.getName();
        return name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
    }

    static String snippet(Tree owner, Cursor cursor) {
        try {
            String printed = owner.printTrimmed(cursor.getParentOrThrow()).trim();
            int newline = printed.indexOf('\n');
            boolean truncated = newline >= 0 || printed.length() > SNIPPET_LENGTH;
            String firstLine = newline >= 0 ? printed.substring(0, newline).trim() : printed;
            if (firstLine.length() > SNIPPET_LENGTH) {
                firstLine = firstLine.substring(0, SNIPPET_LENGTH);
            }
            return truncated ? firstLine + "..." : firstLine;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * A type of marker in a kind of place: on the source file itself or on one of its elements.
     */
    @Value
    private static class Placement {
        boolean onSourceFile;
        String markerType;
    }

    @Value
    private static class Occurrence {
        Placement placement;
        Marker marker;
        Object holder;
        Tree owner;
        Cursor cursor;
    }
}
