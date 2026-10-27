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

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.util.Collections.newSetFromMap;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds the {@link Heavyweight} objects, such as trees, that ScanningRecipe accumulators keep. An accumulator lives
 * from the first source file scanned until the cycle ends. The walk reads fields and JDK collections, and runs no
 * code of the recipe's own.
 */
final class AccumulatorContents {
    /**
     * Beyond this many objects in one accumulator the walk stops descending.
     */
    private static final int MAX_OBJECTS = 100_000;

    private static final int MAX_PATHS = 5;

    private static final Pattern KEY_OR_INDEX = Pattern.compile("\\[[^\\]]*]");

    /**
     * JDK superclasses that keep no elements, so that a collection extending one keeps them in fields of its own.
     */
    private static final Set<Class<?>> SKELETONS = new HashSet<>(Arrays.asList(Object.class,
            AbstractCollection.class, AbstractList.class, AbstractSequentialList.class, AbstractSet.class,
            AbstractQueue.class, AbstractMap.class));

    private enum Kind {RETAINED, LEAF, COLLECTION, MAP, ENTRY, ARRAY, OPTIONAL, ATOMIC_REFERENCE, OBJECT}

    private static final ClassValue<Kind> KINDS = new ClassValue<Kind>() {
        @Override
        protected Kind computeValue(Class<?> type) {
            if (Heavyweight.class.isAssignableFrom(type)) {
                return Kind.RETAINED;
            } else if (Recipe.class.isAssignableFrom(type) || UnchangedTreeIdentity.isJavaType(type)) {
                // a recipe lives for the whole run anyway, and type attribution refers to no tree
                return Kind.LEAF;
            } else if (type.isArray()) {
                return type.getComponentType().isPrimitive() ? Kind.LEAF : Kind.ARRAY;
            } else if ((Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type) ||
                        Map.Entry.class.isAssignableFrom(type)) && !keepsElementsInJdk(type)) {
                // read by its fields, since calling a collection class of the recipe's own runs its code
                return Kind.OBJECT;
            } else if (Collection.class.isAssignableFrom(type)) {
                return Kind.COLLECTION;
            } else if (Map.class.isAssignableFrom(type)) {
                return Kind.MAP;
            } else if (Map.Entry.class.isAssignableFrom(type)) {
                return Kind.ENTRY;
            } else if (Optional.class.equals(type)) {
                return Kind.OPTIONAL;
            } else if (AtomicReference.class.isAssignableFrom(type)) {
                return Kind.ATOMIC_REFERENCE;
            } else if (Reference.class.isAssignableFrom(type) || ClassLoader.class.isAssignableFrom(type) ||
                       Thread.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type) || isJdk(type)) {
                return Kind.LEAF;
            }
            return Kind.OBJECT;
        }
    };

    /**
     * The fields of a class that can hold a reference and can be read.
     */
    private static final ClassValue<List<Field>> FIELDS = new ClassValue<List<Field>>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null && !isJdk(c); c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        fields.add(field);
                    } catch (RuntimeException ignored) {
                        // a field that can't be read can't be walked
                    }
                }
            }
            return fields;
        }
    };

    /**
     * The scanning recipes that ran in this cycle, in the order they first ran.
     */
    private final List<ScanningRecipe<?>> scanningRecipes = new ArrayList<>();
    private final Set<Recipe> ran = newSetFromMap(new IdentityHashMap<>());

    /**
     * What each recipe's accumulator keeps, by recipe name and then by the shape of the path to it.
     */
    private final Map<String, Map<String, Finding>> findings = new LinkedHashMap<>();

    void startCycle() {
        scanningRecipes.clear();
        ran.clear();
    }

    void ran(Recipe recipe) {
        if (recipe instanceof ScanningRecipe && ran.add(recipe)) {
            scanningRecipes.add((ScanningRecipe<?>) recipe);
        }
    }

    /**
     * Walks the accumulator of each scanning recipe that ran in this cycle so far.
     */
    void check(Cursor rootCursor, ExecutionContext ctx, String when) {
        Set<Object> walked = newSetFromMap(new IdentityHashMap<>());
        for (ScanningRecipe<?> recipe : scanningRecipes) {
            Object acc = recipe.getAccumulator(rootCursor, ctx);
            if (acc != null && walked.add(acc)) {
                try {
                    new Walk(recipe.getName(), when).walk(acc);
                } catch (RuntimeException ignored) {
                    // an accumulator the walk can't read is left alone
                }
            }
        }
    }

    void assertNoneFound() {
        if (!findings.isEmpty()) {
            fail(message());
        }
    }

    private final class Walk {
        final String recipe;
        final String when;
        final Set<Object> visited = newSetFromMap(new IdentityHashMap<>());
        final Deque<Node> queue = new ArrayDeque<>();
        int budget = MAX_OBJECTS;

        Walk(String recipe, String when) {
            this.recipe = recipe;
            this.when = when;
        }

        /**
         * Breadth first, so that the path to what the accumulator keeps is a short one.
         */
        void walk(Object acc) {
            visit(acc, null, AddedMarkers.typeName(acc.getClass()));
            while (!queue.isEmpty()) {
                Node node = queue.poll();
                Object value = node.getValue();
                switch (KINDS.get(value.getClass())) {
                    case COLLECTION:
                        int i = 0;
                        for (Object element : (Collection<?>) value) {
                            if (walks(element)) {
                                visit(element, node, i);
                            }
                            i++;
                        }
                        break;
                    case MAP:
                        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                            if (walks(entry.getKey()) || walks(entry.getValue())) {
                                Key key = new Key(entry.getKey());
                                visit(entry.getKey(), node, key);
                                visit(entry.getValue(), node, key);
                            }
                        }
                        break;
                    case ENTRY:
                        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) value;
                        visit(entry.getKey(), node, ".key");
                        visit(entry.getValue(), node, ".value");
                        break;
                    case ARRAY:
                        for (int j = 0; j < Array.getLength(value); j++) {
                            Object element = Array.get(value, j);
                            if (walks(element)) {
                                visit(element, node, j);
                            }
                        }
                        break;
                    case OPTIONAL:
                        visit(((Optional<?>) value).orElse(null), node, ".get()");
                        break;
                    case ATOMIC_REFERENCE:
                        visit(((AtomicReference<?>) value).get(), node, ".get()");
                        break;
                    default:
                        for (Field field : FIELDS.get(value.getClass())) {
                            try {
                                visit(field.get(value), node, field);
                            } catch (IllegalAccessException ignored) {
                                // a field that can't be read can't be walked
                            }
                        }
                }
            }
        }

        /**
         * @param step how the parent reaches the value: a field, an index, a {@link Key} or the text of the step
         */
        private void visit(@Nullable Object value, @Nullable Node parent, Object step) {
            if (!walks(value)) {
                return;
            }
            if (KINDS.get(value.getClass()) == Kind.RETAINED) {
                found(new Node(value, parent, step));
            } else if (budget > 0 && visited.add(value)) {
                budget--;
                queue.add(new Node(value, parent, step));
            }
        }

        private void found(Node node) {
            String path = node.path();
            String what = describe(node.getValue());
            findings.computeIfAbsent(recipe, r -> new LinkedHashMap<>()).putIfAbsent(
                    KEY_OR_INDEX.matcher(path).replaceAll("[]") + " -> " + what, new Finding(path, what, when));
        }
    }

    private static boolean walks(@Nullable Object value) {
        return value != null && KINDS.get(value.getClass()) != Kind.LEAF;
    }

    private static String describe(Object value) {
        if (value instanceof SourceFile) {
            return AddedMarkers.typeName(value.getClass()) + " " + ((SourceFile) value).getSourcePath();
        } else if (value instanceof Cursor) {
            return "Cursor";
        } else if (value instanceof ExecutionContext) {
            return "ExecutionContext";
        } else if (value instanceof TreeVisitor) {
            return "TreeVisitor";
        }
        return AddedMarkers.typeName(value.getClass());
    }

    private static String key(@Nullable Object key) {
        if (key instanceof Recipe) {
            return ((Recipe) key).getName();
        } else if (key instanceof SourceFile) {
            return ((SourceFile) key).getSourcePath().toString();
        } else if (key instanceof Tree || key instanceof Marker) {
            return AddedMarkers.typeName(key.getClass());
        }
        String s;
        try {
            s = String.valueOf(key);
        } catch (RuntimeException e) {
            s = AddedMarkers.typeName(key.getClass());
        }
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    /**
     * Whether a collection is a JDK class or extends one that keeps its elements, so that only its methods can read
     * them.
     */
    private static boolean keepsElementsInJdk(Class<?> type) {
        Class<?> jdk = type;
        while (!isJdk(jdk)) {
            jdk = jdk.getSuperclass();
        }
        return jdk == type || !SKELETONS.contains(jdk);
    }

    private static boolean isJdk(Class<?> type) {
        String name = type.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.") ||
               name.startsWith("sun.") || name.startsWith("com.sun.");
    }

    private String message() {
        StringBuilder message = new StringBuilder("A ScanningRecipe's accumulator should keep the values the recipe " +
                                                  "needs, such as coordinates, property values, paths and ids, and no Heavyweight objects, such as " +
                                                  "trees, cursors, visitors and build models:\n");
        for (Map.Entry<String, Map<String, Finding>> recipe : findings.entrySet()) {
            message.append("  ").append(recipe.getKey()).append(" keeps\n");
            int shown = 0;
            for (Finding finding : recipe.getValue().values()) {
                if (shown++ == MAX_PATHS) {
                    message.append("    and ").append(recipe.getValue().size() - MAX_PATHS).append(" more\n");
                    break;
                }
                message.append("    ").append(finding.getPath()).append(" -> ").append(finding.getWhat())
                        .append(", ").append(finding.getWhen()).append('\n');
            }
        }
        return message.append("An accumulator stays in memory from the first source file scanned until the cycle ")
                .append("ends, so what it keeps for each source file adds up over a large repository. A ")
                .append("MavenResolutionResult or GradleProject can reach the model of a whole build, and a ")
                .append("JavaSourceSet a whole classpath, while small markers such as JavaProject are fine to keep. ")
                .append("A recipe that must keep a Heavyweight object opts out with ")
                .append("RecipeQualityValidation.builder().lightweightAccumulators(false).")
                .toString();
    }

    /**
     * An object the walk reached, with the way there, which is only spelled out for a finding so that the walk calls
     * no {@code toString()} of the recipe's own.
     */
    @Value
    private static class Node {
        Object value;

        @Nullable
        Node parent;

        Object step;

        String path() {
            Deque<String> steps = new ArrayDeque<>();
            for (Node node = this; node != null; node = node.parent) {
                steps.push(node.step instanceof Field ? "." + ((Field) node.step).getName() :
                        node.step instanceof Integer ? "[" + node.step + "]" :
                                node.step instanceof Key ? "[" + key(((Key) node.step).getKey()) + "]" :
                                        (String) node.step);
            }
            return String.join("", steps);
        }
    }

    @Value
    private static class Key {
        @Nullable
        Object key;
    }

    @Value
    private static class Finding {
        String path;
        String what;
        String when;
    }
}
