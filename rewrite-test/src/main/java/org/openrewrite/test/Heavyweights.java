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
import org.openrewrite.rpc.RewriteRpc;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.util.Collections.newSetFromMap;

/// Finds the [Heavyweight] objects, such as trees, that what a recipe keeps can reach. The walk reads fields and JDK
/// collections, and runs no code of the recipe's own.
final class Heavyweights {
    /// Beyond this many objects in one walk it stops descending.
    private static final int MAX_OBJECTS = 100_000;

    private static final int MAX_PATHS = 5;

    private static final Pattern KEY_OR_INDEX = Pattern.compile("\\[[^\\]]*]");

    /// JDK superclasses that keep no elements, so that a collection extending one keeps them in fields of its own.
    private static final Set<Class<?>> SKELETONS = new HashSet<>(Arrays.asList(Object.class,
            AbstractCollection.class, AbstractList.class, AbstractSequentialList.class, AbstractSet.class,
            AbstractQueue.class, AbstractMap.class));

    private enum Kind {RETAINED, LEAF, COLLECTION, MAP, ENTRY, ARRAY, OPTIONAL, ATOMIC_REFERENCE, OBJECT}

    private static final ClassValue<Kind> KINDS = new ClassValue<Kind>() {
        @Override
        protected Kind computeValue(Class<?> type) {
            if (Heavyweight.class.isAssignableFrom(type)) {
                return Kind.RETAINED;
            } else if (Recipe.class.isAssignableFrom(type) || UnchangedTreeIdentity.isJavaType(type) ||
                       RewriteRpc.class.isAssignableFrom(type)) {
                // a recipe lives for the whole run anyway, type attribution refers to no tree, and RpcRecipe's
                // RewriteRpc caches the trees and ExecutionContext it exchanged with the other process
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

    /// The fields of a class that can hold a reference and can be read.
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

    /// What each recipe keeps, by recipe name and then by the shape of the path to it.
    private final Map<String, Map<String, Finding>> findings = new LinkedHashMap<>();

    /// @param when         the point of the run the walk looks at, or `null` when that doesn't matter
    /// @param keptByRecipe whether the walk starts from a recipe's own fields, which may keep a visitor, such as a
    ///                     precondition's, and a tree short of a whole source file, such as one parsed from an
    ///                     option: neither grows with the source files the recipe visits
    Walk walk(String recipe, @Nullable String when, boolean keptByRecipe) {
        return new Walk(recipe, when, keptByRecipe);
    }

    boolean isEmpty() {
        return findings.isEmpty();
    }

    /// The paths to what each recipe keeps, a few per recipe, one per line.
    String report() {
        StringBuilder report = new StringBuilder();
        for (Map.Entry<String, Map<String, Finding>> recipe : findings.entrySet()) {
            report.append("  ").append(recipe.getKey()).append(" keeps\n");
            int shown = 0;
            for (Finding finding : recipe.getValue().values()) {
                if (shown++ == MAX_PATHS) {
                    report.append("    and ").append(recipe.getValue().size() - MAX_PATHS).append(" more\n");
                    break;
                }
                report.append("    ").append(finding.getPath()).append(" -> ").append(finding.getWhat());
                if (finding.getWhen() != null) {
                    report.append(", ").append(finding.getWhen());
                }
                report.append('\n');
            }
        }
        return report.toString();
    }

    final class Walk {
        final String recipe;
        final @Nullable String when;
        final boolean keptByRecipe;
        final Set<Object> visited = newSetFromMap(new IdentityHashMap<>());
        final Deque<Node> queue = new ArrayDeque<>();
        int budget = MAX_OBJECTS;

        private Walk(String recipe, @Nullable String when, boolean keptByRecipe) {
            this.recipe = recipe;
            this.when = when;
            this.keptByRecipe = keptByRecipe;
        }

        void from(@Nullable Object root, String path) {
            visit(root, null, path);
        }

        /// Breadth first, so that the path to what is kept is a short one.
        void drain() {
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

        /// @param step how the parent reaches the value: a field, an index, a [Key] or the text of the step
        private void visit(@Nullable Object value, @Nullable Node parent, Object step) {
            if (!walks(value) || keptByRecipe && (value instanceof TreeVisitor ||
                                                  value instanceof Tree && !(value instanceof SourceFile))) {
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

    /// Whether a collection is a JDK class or extends one that keeps its elements, so that only its methods can read
    /// them.
    private static boolean keepsElementsInJdk(Class<?> type) {
        Class<?> jdk = type;
        while (!isJdk(jdk)) {
            jdk = jdk.getSuperclass();
        }
        return jdk == type || !SKELETONS.contains(jdk);
    }

    static boolean isJdk(Class<?> type) {
        String name = type.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.") ||
               name.startsWith("sun.") || name.startsWith("com.sun.");
    }

    /// An object the walk reached, with the way there, which is only spelled out for a finding so that the walk calls
    /// no `toString()` of the recipe's own.
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

        @Nullable
        String when;
    }
}
