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
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.RecipesThatMadeChanges;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds recipes that return a new source file without changing anything in it. Each recipe's own edit is walked
 * together with what it was given: trees pair by id, type attribution compares by identity and the rest by value.
 */
final class UnchangedTreeIdentity {
    /**
     * Beyond these limits the walk gives up and counts the edit as a change.
     */
    private static final int MAX_COMPARISONS = 100_000;
    private static final int MAX_DEPTH = 1_000;

    private static final int MAX_CAUSES = 5;

    private enum Status {SAME, REBUILT, CHANGED}

    private enum Kind {TYPE, VALUE, LIST, SET, MAP, ARRAY, MARKERS, OBJECT, OPAQUE}

    /**
     * The fields of a class that hold its state, markers last, or null when they can't all be read.
     */
    private static final ClassValue<@Nullable List<Field>> FIELDS = new ClassValue<@Nullable List<Field>>() {
        @Override
        protected @Nullable List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            List<Field> markers = new ArrayList<>();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                    } catch (RuntimeException e) {
                        return null;
                    }
                    // markers rarely hold the real change, so comparing them last ends most walks sooner
                    (Markers.class.equals(field.getType()) ? markers : fields).add(field);
                }
            }
            fields.addAll(markers);
            return fields;
        }
    };

    private static final ClassValue<Kind> KINDS = new ClassValue<Kind>() {
        @Override
        protected Kind computeValue(Class<?> type) {
            if (isJavaType(type)) {
                return Kind.TYPE;
            } else if (type.isArray()) {
                return Kind.ARRAY;
            } else if (List.class.isAssignableFrom(type)) {
                return Kind.LIST;
            } else if (Set.class.isAssignableFrom(type)) {
                return Kind.SET;
            } else if (Map.class.isAssignableFrom(type)) {
                return Kind.MAP;
            } else if (Markers.class.equals(type)) {
                return Kind.MARKERS;
            }
            String name = type.getName();
            if (Enum.class.isAssignableFrom(type) || name.startsWith("java.") || name.startsWith("javax.") ||
                name.startsWith("jdk.") || name.startsWith("sun.") || name.startsWith("com.sun.")) {
                return Kind.VALUE;
            }
            return FIELDS.get(type) == null ? Kind.OPAQUE : Kind.OBJECT;
        }
    };

    private final List<Finding> findings = new ArrayList<>();

    private final Set<UUID> generatedInThisCycle = new HashSet<>();

    void assertNoneFound() {
        if (!findings.isEmpty()) {
            fail(message());
        }
    }

    void startCycle() {
        generatedInThisCycle.clear();
    }

    /**
     * Compares a recipe's edit of a source file with what the recipe was given.
     */
    void record(@Nullable SourceFile before, @Nullable SourceFile after, List<Recipe> recipeStack, int cycle) {
        if (before == null) {
            if (after != null) {
                generatedInThisCycle.add(after.getId());
            }
        } else if (after != null && !generatedInThisCycle.contains(before.getId())) {
            // a file generated earlier in this cycle is new anyway, so a copy of it costs nothing
            compare(before, after, recipeStack.get(recipeStack.size() - 1), cycle);
        }
    }

    private void compare(SourceFile before, SourceFile after, Recipe recipe, int cycle) {
        Walk walk = new Walk();
        try {
            if (walk.compare(before, after, new Cursor(null, Cursor.ROOT_VALUE), "", 0) == Status.REBUILT) {
                findings.add(new Finding(recipe.getName(), after.getSourcePath(), cycle, walk.causes, walk.causeCount));
            }
        } catch (RuntimeException | StackOverflowError ignored) {
            // a tree the walk can't compare counts as changed
        }
    }

    /**
     * Compares a recipe's output with its input, descending only into values that are not the same instance.
     */
    private static final class Walk {
        final List<Cause> causes = new ArrayList<>();
        final Set<Object> inProgress = Collections.newSetFromMap(new IdentityHashMap<>());
        int causeCount;
        int budget = MAX_COMPARISONS;

        /**
         * @param owner A cursor on the nearest tree that holds the values.
         * @param path  The fields that lead from that tree to the values.
         */
        Status compare(@Nullable Object before, @Nullable Object after, Cursor owner, String path, int depth) {
            if (before == after) {
                return Status.SAME;
            } else if (before == null || after == null || --budget < 0 || depth > MAX_DEPTH) {
                return Status.CHANGED;
            }
            Kind kind = KINDS.get(after.getClass());
            // a copy of a collection is often of another class, as when an empty list is copied into an ArrayList
            boolean comparable = kind == Kind.LIST || kind == Kind.SET || kind == Kind.MAP ?
                    KINDS.get(before.getClass()) == kind : before.getClass() == after.getClass();
            if (!comparable || kind == Kind.TYPE || kind == Kind.OPAQUE) {
                // Type attribution compares by identity, because JavaType equality compares names and so would
                // call a real update of it equal. Values whose fields can't be read can't be compared at all.
                return Status.CHANGED;
            }
            switch (kind) {
                case VALUE:
                    if (!after.equals(before)) {
                        return Status.CHANGED;
                    }
                    cause(owner, path, "an equal " + valueTypeName(after) + " that is not the same instance" +
                                       quoted(after));
                    return Status.REBUILT;
                case LIST:
                case SET:
                    return elements((Collection<?>) before, (Collection<?>) after, owner, path, depth);
                case MAP:
                    return entries((Map<?, ?>) before, (Map<?, ?>) after, owner, path, depth);
                case ARRAY:
                    return array(before, after, owner, path, depth);
                case MARKERS:
                    return markers((Markers) before, (Markers) after, owner, path, depth);
                default:
                    return fields(before, after, owner, path, depth);
            }
        }

        private void cause(Cursor owner, String path, String what) {
            if (causeCount++ < MAX_CAUSES) {
                causes.add(new Cause(owner, path, what));
            }
        }

        private Status fields(Object before, Object after, Cursor owner, String path, int depth) {
            boolean tree = after instanceof Tree;
            if (tree && !((Tree) after).getId().equals(((Tree) before).getId()) || !inProgress.add(after)) {
                // a tree replaced by another one, or values that refer back to themselves
                return Status.CHANGED;
            }
            try {
                Cursor fieldOwner = tree ? new Cursor(owner, after) : owner;
                boolean rebuilt = false;
                for (Field field : requireNonNull(FIELDS.get(after.getClass()))) {
                    Object b = field.get(before);
                    Object a = field.get(after);
                    if (a == b) {
                        continue;
                    }
                    String fieldPath = tree ? field.getName() : path + "." + field.getName();
                    Status status = field.getType().isPrimitive() ? (a.equals(b) ? Status.SAME : Status.CHANGED) :
                            compare(b, a, fieldOwner, fieldPath, depth + 1);
                    if (status == Status.CHANGED) {
                        return Status.CHANGED;
                    }
                    rebuilt |= status == Status.REBUILT;
                }
                if (!rebuilt) {
                    if (tree) {
                        cause(fieldOwner, "", "a new instance whose fields are all the same instances");
                    } else {
                        cause(owner, path, "a new " + AddedMarkers.typeName(after.getClass()) +
                                           " whose fields are all the same instances");
                    }
                }
                return Status.REBUILT;
            } catch (IllegalAccessException e) {
                return Status.CHANGED;
            } finally {
                inProgress.remove(after);
            }
        }

        private Status elements(Collection<?> before, Collection<?> after, Cursor owner, String path, int depth) {
            if (before.size() != after.size()) {
                return Status.CHANGED;
            }
            boolean rebuilt = false;
            Iterator<?> beforeElements = before.iterator();
            int i = 0;
            for (Object a : after) {
                Object b = beforeElements.next();
                if (a != b) {
                    Status status = compare(b, a, owner, path + "[" + i + "]", depth + 1);
                    if (status == Status.CHANGED) {
                        return Status.CHANGED;
                    }
                    rebuilt |= status == Status.REBUILT;
                }
                i++;
            }
            if (!rebuilt) {
                String type = AddedMarkers.typeName(after.getClass());
                cause(owner, path, after.isEmpty() ? "a new empty " + type :
                        "a new " + type + " with the same " + after.size() + " elements");
            }
            return Status.REBUILT;
        }

        private Status entries(Map<?, ?> before, Map<?, ?> after, Cursor owner, String path, int depth) {
            if (before.size() != after.size()) {
                return Status.CHANGED;
            }
            boolean rebuilt = false;
            Iterator<? extends Map.Entry<?, ?>> beforeEntries = before.entrySet().iterator();
            for (Map.Entry<?, ?> a : after.entrySet()) {
                Map.Entry<?, ?> b = beforeEntries.next();
                String entryPath = path + "[" + abbreviate(String.valueOf(a.getKey())) + "]";
                Status key = compare(b.getKey(), a.getKey(), owner, entryPath, depth + 1);
                Status value = key == Status.CHANGED ? Status.CHANGED :
                        compare(b.getValue(), a.getValue(), owner, entryPath, depth + 1);
                if (value == Status.CHANGED) {
                    return Status.CHANGED;
                }
                rebuilt |= key == Status.REBUILT || value == Status.REBUILT;
            }
            if (!rebuilt) {
                cause(owner, path, "a new " + AddedMarkers.typeName(after.getClass()) + " with the same " +
                                   after.size() + " entries");
            }
            return Status.REBUILT;
        }

        private Status array(Object before, Object after, Cursor owner, String path, int depth) {
            int length = Array.getLength(after);
            if (Array.getLength(before) != length) {
                return Status.CHANGED;
            }
            boolean primitive = after.getClass().getComponentType().isPrimitive();
            boolean rebuilt = false;
            for (int i = 0; i < length; i++) {
                Object b = Array.get(before, i);
                Object a = Array.get(after, i);
                Status status = primitive ? (a.equals(b) ? Status.SAME : Status.CHANGED) :
                        compare(b, a, owner, path + "[" + i + "]", depth + 1);
                if (status == Status.CHANGED) {
                    return Status.CHANGED;
                }
                rebuilt |= status == Status.REBUILT;
            }
            if (!rebuilt) {
                cause(owner, path, "a new " + after.getClass().getSimpleName() + " with the same " + length +
                                   " elements");
            }
            return Status.REBUILT;
        }

        /**
         * Neither the id of a {@link Markers} nor the recipes the scheduler records as having made changes say
         * anything about the tree.
         */
        private Status markers(Markers before, Markers after, Cursor owner, String path, int depth) {
            List<Marker> b = withoutRecipesThatMadeChanges(before.getMarkers());
            List<Marker> a = withoutRecipesThatMadeChanges(after.getMarkers());
            if (b.size() != a.size()) {
                return Status.CHANGED;
            }
            boolean rebuilt = false;
            for (int i = 0; i < a.size(); i++) {
                Status status = compare(b.get(i), a.get(i), owner, path + "[" + i + "]", depth + 1);
                if (status == Status.CHANGED) {
                    return Status.CHANGED;
                }
                rebuilt |= status == Status.REBUILT;
            }
            if (!rebuilt) {
                cause(owner, path, "a new Markers with the same markers");
            }
            return Status.REBUILT;
        }
    }

    private static List<Marker> withoutRecipesThatMadeChanges(List<Marker> markers) {
        for (Marker marker : markers) {
            if (marker instanceof RecipesThatMadeChanges) {
                List<Marker> without = new ArrayList<>(markers);
                without.removeIf(m -> m instanceof RecipesThatMadeChanges);
                return without;
            }
        }
        return markers;
    }

    static boolean isJavaType(@Nullable Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if ("org.openrewrite.java.tree.JavaType".equals(c.getName())) {
                return true;
            }
            for (Class<?> i : c.getInterfaces()) {
                if (isJavaType(i)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String message() {
        StringBuilder message = new StringBuilder("When a recipe changes nothing in a source file, it must return " +
                                                  "the LST it was given, not an equal copy:\n");
        for (Finding finding : findings) {
            message.append("  ").append(finding.getRecipe()).append(" returned an equal copy of ")
                    .append(finding.getSourcePath()).append(" in cycle ").append(finding.getCycle()).append('\n');
            for (Cause cause : finding.getCauses()) {
                message.append("    ").append(describe(cause)).append('\n');
            }
            int more = finding.getCauseCount() - finding.getCauses().size();
            if (more > 0) {
                message.append("    and ").append(more).append(" more\n");
            }
        }
        return message.append("OpenRewrite decides whether a recipe changed a file by reference, so an equal copy ")
                .append("counts as a change: the file is reported with an empty diff, the recipe is recorded as ")
                .append("having changed it, and it can cause another cycle. To return the original:\n")
                .append("  - edit LST lists with ListUtils.map, flatMap, filter, concat or mapFirst, returning null ")
                .append("to remove an element. They return the original list when no element changed, while ")
                .append("streams, loops and new ArrayList<>(..) always return a new one\n")
                .append("  - don't pass a with* method a value that only equals the current one, such as a recomputed ")
                .append("String, a new Space, Markers or list, or a node built with new. Compare first and keep the ")
                .append("original\n")
                .append("  - check that a value differs before calling a method that always rebuilds, such as ")
                .append("Xml.Tag.withValue, or before applying a JavaTemplate\n")
                .toString();
    }

    private static String describe(Cause cause) {
        Object owner = cause.getOwner().getValue();
        if (!(owner instanceof Tree)) {
            return cause.getWhat();
        }
        StringBuilder description = new StringBuilder(AddedMarkers.typeName(owner.getClass()));
        if (!(owner instanceof SourceFile)) {
            String snippet = AddedMarkers.snippet((Tree) owner, cause.getOwner());
            if (!snippet.isEmpty()) {
                description.append(" `").append(snippet).append('`');
            }
        }
        if (!cause.getPath().isEmpty()) {
            description.append(" > ").append(cause.getPath());
        }
        return description.append(": ").append(cause.getWhat()).toString();
    }

    private static String valueTypeName(Object value) {
        return value instanceof Path ? "Path" : value instanceof Charset ? "Charset" :
                AddedMarkers.typeName(value.getClass());
    }

    private static String quoted(Object value) {
        return value instanceof String ? " \"" + abbreviate((String) value)
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"" : "";
    }

    private static String abbreviate(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    @Value
    private static class Finding {
        String recipe;
        Path sourcePath;
        int cycle;
        List<Cause> causes;
        int causeCount;
    }

    @Value
    private static class Cause {
        Cursor owner;
        String path;
        String what;
    }
}
