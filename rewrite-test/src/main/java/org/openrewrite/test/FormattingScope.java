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

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds recipes that auto-format a whole source file, class or document root that existed before the run. Test inputs
 * are usually formatted already, so the reformatting this causes on real code rarely shows in an expected output.
 */
final class FormattingScope extends FormattingListener {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int SNIPPET_LENGTH = 60;

    private final Recipe recipe;
    private final boolean enabled;
    private final List<Request> requests = new ArrayList<>();
    private final Set<Tree> requested = Collections.newSetFromMap(new IdentityHashMap<>());

    FormattingScope(Recipe recipe, boolean enabled) {
        this.recipe = recipe;
        this.enabled = enabled;
    }

    RecipeRun record(Supplier<RecipeRun> run) {
        if (!enabled) {
            return run.get();
        }
        install();
        try {
            return run.get();
        } finally {
            close();
        }
    }

    @Override
    protected void onFormat(Tree tree, @Nullable Tree stopAfter, Cursor cursor) {
        if (stopAfter != null || !isWhole(tree, cursor) || !requested.add(tree)) {
            return;
        }
        SourceFile sourceFile = tree instanceof SourceFile ? (SourceFile) tree :
                cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            return;
        }
        StackTraceElement[] stack = new Throwable().getStackTrace();
        int i = 0;
        boolean afterVisit = false;
        while (i < stack.length - 1 && isPlumbing(stack[i])) {
            afterVisit |= "applyAfterVisitors".equals(stack[i].getMethodName());
            i++;
        }
        StackTraceElement caller = stack[i];
        // Formatters format whole files, and so does a recipe whose own visitor is a formatter
        if (inFormatPackage(caller.getClassName()) ||
            !afterVisit && caller.getClassName().startsWith("org.openrewrite.scheduling.")) {
            return;
        }
        requests.add(new Request(tree, sourceFile.getId(), callSite(caller, stack[Math.max(i - 1, 0)], afterVisit),
                describe(tree, cursor)));
    }

    void assertFormattedOnlyWhatChanged(Collection<Result> results) {
        if (requests.isEmpty() || isFormatter(recipe)) {
            return;
        }
        Map<UUID, Result> resultsByBefore = new HashMap<>();
        for (Result result : results) {
            if (result.getBefore() != null && result.getAfter() != null) {
                resultsByBefore.put(result.getBefore().getId(), result);
            }
        }
        Map<UUID, Map<UUID, Tree>> treesByFile = new HashMap<>();
        Map<String, Set<String>> found = new LinkedHashMap<>();
        for (Request request : requests) {
            Result result = resultsByBefore.get(request.getSourceFileId());
            if (result == null || onlyWhitespaceChanged(result)) {
                continue;
            }
            SourceFile before = requireNonNull(result.getBefore());
            Tree original = treesByFile.computeIfAbsent(before.getId(), id -> treesById(before))
                    .get(request.getTree().getId());
            if (original != null && !holdsNoneOfItsChildren(original, request.getTree())) {
                found.computeIfAbsent(request.getCallSite(), k -> new LinkedHashSet<>())
                        .add(request.getTarget() + " of " + before.getSourcePath());
            }
        }
        if (!found.isEmpty()) {
            fail(message(found));
        }
    }

    /**
     * A source file, a class, or the element that holds all of a document: the root tag of XML, the value of JSON,
     * or a YAML document and its block.
     */
    private static boolean isWhole(Tree tree, Cursor cursor) {
        if (tree instanceof SourceFile || isClass(tree)) {
            return true;
        }
        Tree parent = parent(tree, cursor);
        if (parent == null) {
            return false;
        }
        String type = tree.getClass().getName();
        switch (parent.getClass().getName()) {
            case "org.openrewrite.xml.tree.Xml$Document":
                return "org.openrewrite.xml.tree.Xml$Tag".equals(type);
            case "org.openrewrite.yaml.tree.Yaml$Document":
                return !type.endsWith("$End") && !type.endsWith("$Directive");
            case "org.openrewrite.json.tree.Json$Document":
            case "org.openrewrite.yaml.tree.Yaml$Documents":
                return true;
            default:
                return false;
        }
    }

    private static boolean isClass(Tree tree) {
        return tree.getClass().getName().endsWith("$ClassDeclaration");
    }

    /**
     * The nearest tree on the cursor other than the tree itself, which some visitors pass the cursor of.
     */
    private static @Nullable Tree parent(Tree tree, Cursor cursor) {
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            Object value = c.getValue();
            if (value instanceof Tree && !((Tree) value).getId().equals(tree.getId())) {
                return (Tree) value;
            }
        }
        return null;
    }

    /**
     * The formatter and the visitor machinery between it and the code that asked for the format.
     */
    private static boolean isPlumbing(StackTraceElement frame) {
        String type = frame.getClassName();
        String method = frame.getMethodName();
        return type.equals(FormattingScope.class.getName()) ||
               type.equals(FormattingListener.class.getName()) ||
               type.startsWith(TreeVisitor.class.getName()) ||
               type.startsWith(Preconditions.class.getName()) ||
               type.startsWith(ScanningRecipe.class.getName()) ||
               type.endsWith(".AutoFormatVisitor") ||
               "autoFormat".equals(method) ||
               "maybeAutoFormat".equals(method);
    }

    private static String callSite(StackTraceElement caller, StackTraceElement entry, boolean afterVisit) {
        if (afterVisit) {
            // the visitor that asked is gone from the stack by the time its after-visitors run
            return "A visitor's doAfterVisit(new AutoFormatVisitor<>()) ran on";
        }
        String method = entry.getMethodName();
        return frame(caller) + ("autoFormat".equals(method) || "maybeAutoFormat".equals(method) ?
                " called " + method + " on" : " ran an AutoFormatVisitor on");
    }

    private static String frame(StackTraceElement frame) {
        String type = frame.getClassName();
        String location = frame.getFileName() == null ? "Unknown Source" :
                frame.getFileName() + ":" + frame.getLineNumber();
        return type.substring(type.lastIndexOf('.') + 1) + "." + frame.getMethodName() + "(" + location + ")";
    }

    // A formatting recipe may format whole files, and so may the recipes run alongside it
    private static boolean isFormatter(Recipe recipe) {
        if (inFormatPackage(recipe.getName()) || inFormatPackage(recipe.getClass().getName())) {
            return true;
        }
        for (Recipe child : recipe.getRecipeList()) {
            if (isFormatter(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inFormatPackage(String className) {
        int dot = className.lastIndexOf('.');
        return dot > 0 && ("." + className.substring(0, dot) + ".").contains(".format.");
    }

    private static boolean onlyWhitespaceChanged(Result result) {
        return WHITESPACE.matcher(requireNonNull(result.getBefore()).printAll()).replaceAll("")
                .equals(WHITESPACE.matcher(requireNonNull(result.getAfter()).printAll()).replaceAll(""));
    }

    private static Map<UUID, Tree> treesById(Tree tree) {
        Map<UUID, Tree> trees = new HashMap<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree t, Integer p) {
                trees.put(t.getId(), t);
                return t;
            }
        }.visit(tree, 0);
        return trees;
    }

    /**
     * A tree formatted with none of the children it had reformats no existing code below it: a class with its body
     * emptied to be put back afterwards, or a copy of a container holding only a new element. A tree that had no
     * children to begin with would format them on real code.
     */
    private static boolean holdsNoneOfItsChildren(Tree original, Tree formatted) {
        Set<UUID> children = children(original);
        return !children.isEmpty() && Collections.disjoint(children, children(formatted));
    }

    /**
     * The members of a class's body, or the direct children of any other tree.
     */
    private static Set<UUID> children(Tree tree) {
        Object parent = isClass(tree) ? invoke(tree, "getBody") : tree;
        if (!(parent instanceof Tree)) {
            return emptySet();
        }
        Set<UUID> children = new HashSet<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree t, Integer p) {
                if (t != parent) {
                    stopAfterPreVisit();
                    children.add(t.getId());
                }
                return t;
            }
        }.visit((Tree) parent, 0);
        return children;
    }

    private static String describe(Tree tree, Cursor cursor) {
        if (tree instanceof SourceFile) {
            return "all";
        }
        Object name = isClass(tree) ? invoke(tree, "getSimpleName") : null;
        if (name instanceof String) {
            return "class `" + name + "`";
        }
        String type = tree.getClass().getName();
        String description = type.substring(type.lastIndexOf('.') + 1).replace('$', '.');
        String snippet = snippet(tree, cursor);
        return snippet.isEmpty() ? description : description + " `" + snippet + "`";
    }

    private static String snippet(Tree tree, Cursor cursor) {
        try {
            String printed = tree.printTrimmed(cursor).trim();
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

    private static @Nullable Object invoke(Tree tree, String method) {
        try {
            return tree.getClass().getMethod(method).invoke(tree);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static String message(Map<String, Set<String>> found) {
        StringBuilder message = new StringBuilder("Format only what the recipe changed, not the whole class, file " +
                                                  "or document around it. These calls auto-formatted a whole file, " +
                                                  "class or document root that existed before the run, with no " +
                                                  "stopAfter:\n");
        for (Map.Entry<String, Set<String>> entry : found.entrySet()) {
            message.append("  ").append(entry.getKey()).append('\n');
            for (String target : entry.getValue()) {
                message.append("    ").append(target).append('\n');
            }
        }
        return message.append("That rewrites the whitespace of code the recipe did not change. Test inputs are ")
                .append("usually formatted already, so expected outputs rarely show it, but on real code the diff ")
                .append("will carry unrelated reformatting. Instead:\n")
                .append("  - format the element you created or replaced: maybeAutoFormat(original, changed, ctx) ")
                .append("on that element, or autoFormat(newElement, ctx, cursor) with a cursor on its parent. ")
                .append("In XML, AddToTagVisitor.addToTag(parent, newTag, cursor) adds a tag and formats only it\n")
                .append("  - when a new element only formats right inside its container, format a copy of the ")
                .append("container that holds just the new element, and add the formatted element to the original\n")
                .append("  - after changing a declaration's annotations, modifiers or extends/implements, stop ")
                .append("after its header: maybeAutoFormat(c, updated, updated.getName(), ctx, ")
                .append("getCursor().getParentTreeCursor())\n")
                .append("  - when you remove an element, give its prefix to the element that takes its place, ")
                .append("e.g. ListUtils.mapFirst(list, e -> e.withPrefix(removed.getPrefix()))\n")
                .append("  - re-indent statements you moved with autoIndent(..) rather than autoFormat\n")
                .append("  - JavaTemplate and its coordinates (addAnnotation, addStatement) already format what ")
                .append("they insert, so drop the extra call\n")
                .append("Recipes in a *.format package are exempt. Any other recipe whose purpose is formatting ")
                .append("opts out with RecipeQualityValidation.builder().minimalFormatting(false).\n")
                .toString();
    }

    @Value
    private static class Request {
        Tree tree;
        UUID sourceFileId;
        String callSite;
        String target;
    }
}
