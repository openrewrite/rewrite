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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.internal.ComponentIndex;
import org.openrewrite.coldfusion.internal.TypeAttribution;
import org.openrewrite.coldfusion.marker.KeywordSpelling;
import org.openrewrite.coldfusion.marker.OperatorSpelling;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.coldfusion.tree.TagComment;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.java.marker.OmitBraces;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Checks the shape of every parsed file under {@code -Dcoldfusion.corpus.dir=...}: that every {@link Space}
 * holds only whitespace and comments, that names hold no whitespace, that the visitor reaches every element
 * and the printer prints every element's markers, and that each element's printed span starts and ends
 * with its own syntax rather than with space. Opt-in, like {@link ColdFusionCorpusTest}.
 */
class LstShapeCorpusTest {

    private static final char OPEN = '\uE000';
    private static final char CLOSE = '\uE001';

    @Test
    @EnabledIfSystemProperty(named = "coldfusion.corpus.dir", matches = ".+")
    void shapes() throws IOException {
        Path root = Paths.get(System.getProperty("coldfusion.corpus.dir"));
        ColdFusionParser parser = ColdFusionParser.builder().build();
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).filter(parser::accept).sorted().forEach(paths::add);
        }
        int limit = Integer.getInteger("coldfusion.corpus.limit", Integer.MAX_VALUE);

        // each directory under the root is a repository, whose components the parser resolves among themselves
        Map<Path, List<Path>> byRepository = new LinkedHashMap<>();
        for (Path path : paths.subList(0, Math.min(limit, paths.size()))) {
            byRepository.computeIfAbsent(root.resolve(root.relativize(path).getName(0)), r -> new ArrayList<>()).add(path);
        }
        // -Dcoldfusion.corpus.mappings=testbox=/path/to/testbox;coldbox=/path/to/coldbox, as a classpath would be
        Map<String, Path> mappings = new LinkedHashMap<>();
        for (String mapping : System.getProperty("coldfusion.corpus.mappings", "").split(";")) {
            int equals = mapping.indexOf('=');
            if (equals > 0) {
                mappings.put(mapping.substring(0, equals).trim(), Paths.get(mapping.substring(equals + 1).trim()));
            }
        }
        ComponentIndex dependencies = mappings.isEmpty() ? null : ColdFusionParser.index(mappings);

        Findings findings = new Findings();
        int parsed = 0;
        for (Map.Entry<Path, List<Path>> repository : byRepository.entrySet()) {
            ComponentIndex index = new ComponentIndex(dependencies);
            // a corpus directory is named owner__repository, where a checkout is named for the repository
            String directory = repository.getKey().getFileName().toString();
            index.repository(directory.substring(directory.indexOf("__") < 0 ? 0 : directory.indexOf("__") + 2));
            for (Path path : repository.getValue()) {
                Path inRepository = repository.getKey().relativize(path);
                try (InputStream in = Files.newInputStream(path);
                     EncodingDetectingInputStream is = new EncodingDetectingInputStream(in)) {
                    index.add(inRepository, ColdFusionParser.parse(inRepository, ColdFusionParser.text(inRepository, is),
                            null, is.getCharset(), is.isCharsetBomMarked()));
                } catch (Throwable ignored) {
                    // reported when the file is parsed again
                }
            }
            index.link();
            for (Path path : repository.getValue()) {
                parsed += check(root, repository.getKey(), path, index, findings);
            }
        }

        String report = "LST shape of " + parsed + " parsed files under " + root + "\n" + findings.report();
        System.out.println(report);
        Path out = Paths.get(System.getProperty("coldfusion.corpus.report", "build/coldfusion-shape-report.txt"));
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.write(out, report.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and checks one file, returning 1 when it prints back.
     */
    private static int check(Path root, Path repository, Path path, ComponentIndex index, Findings findings) {
        String relative = root.relativize(path).toString();
        String source;
        Cf.CompilationUnit cu;
        try (InputStream in = Files.newInputStream(path);
             EncodingDetectingInputStream is = new EncodingDetectingInputStream(in)) {
            source = ColdFusionParser.text(repository.relativize(path), is);
            cu = TypeAttribution.attribute(ColdFusionParser.parse(repository.relativize(path), source, null,
                    is.getCharset(), is.isCharsetBomMarked()), index);
        } catch (Throwable t) {
            return 0;
        }
        try {
            if (!Arrays.equals(cu.printAllAsBytes(), Files.readAllBytes(path))) {
                findings.add("print", "does not print back its bytes", relative, "");
                return 0;
            }
        } catch (IOException e) {
            return 0;
        }
        // the byte order mark that reading the file dropped is no part of the text compared next
        cu = cu.withCharsetBomMarked(false);
        String printed = cu.printAll();
        if (!printed.equals(source)) {
            int at = 0;
            while (at < printed.length() && at < source.length() && printed.charAt(at) == source.charAt(at)) {
                at++;
            }
            findings.add("print", "does not print back", relative, around(source.substring(Math.max(0, at - 20),
                    Math.min(source.length(), at + 20))));
            return 0;
        }
        try {
            new FileCheck(relative, source, cu, findings, index).run();
        } catch (Throwable t) {
            findings.add("harness", "check threw " + t, relative, "");
        }
        return 1;
    }

    @Value
    @With
    static class Probe implements Marker {
        UUID id;
    }

    private static final PrintOutputCapture.MarkerPrinter PROBES = new PrintOutputCapture.MarkerPrinter() {
        @Override
        public String beforePrefix(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
            return marker instanceof Probe ? OPEN + "P" + marker.getId() + CLOSE : "";
        }

        @Override
        public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
            return marker instanceof Probe ? OPEN + "S" + marker.getId() + CLOSE : "";
        }

        @Override
        public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
            return marker instanceof Probe ? OPEN + "E" + marker.getId() + CLOSE : "";
        }
    };

    private static class FileCheck {
        final String path;
        final String source;
        final Cf.CompilationUnit cu;
        final Findings findings;
        final ComponentIndex index;

        final Map<UUID, J> visited = new LinkedHashMap<>();
        final Map<UUID, int[]> spans = new HashMap<>();
        final Map<UUID, UUID> parents = new HashMap<>();

        FileCheck(String path, String source, Cf.CompilationUnit cu, Findings findings, ComponentIndex index) {
            this.path = path;
            this.source = source;
            this.cu = cu;
            this.findings = findings;
            this.index = index;
        }

        void run() {
            Map<Object, String> reachable = new IdentityHashMap<>();
            walk(cu, "Cf.CompilationUnit", reachable, new HashSet<>());
            probe();
            for (Map.Entry<Object, String> e : reachable.entrySet()) {
                // JavaVisitor visits a class's kind by its prefix and annotations, never as a tree, in Java too
                if (e.getKey() instanceof J && !(e.getKey() instanceof Cf.CompilationUnit) &&
                    !(e.getKey() instanceof J.ClassDeclaration.Kind) && !visited.containsKey(((J) e.getKey()).getId())) {
                    findings.add("visitor", "not visited: " + e.getValue(), path, "");
                }
            }
            spans();
            navigation();
        }

        // ------------------------------------------------------------------------------------
        // Reflective walk: every Space, comment and name in the tree
        // ------------------------------------------------------------------------------------

        void walk(Object o, String where, Map<Object, String> reachable, Set<Object> seenValues) {
            if (o == null || o instanceof String || o instanceof Number || o instanceof Boolean ||
                o instanceof Enum || o instanceof JavaType || o instanceof UUID || o instanceof Path ||
                o instanceof Character) {
                return;
            }
            if (reachable.containsKey(o)) {
                return;
            }
            reachable.put(o, where);
            if (o instanceof Space) {
                space((Space) o, where);
                return;
            }
            if (o instanceof Collection) {
                for (Object element : (Collection<?>) o) {
                    walk(element, where, reachable, seenValues);
                }
                return;
            }
            if (o instanceof JRightPadded && ((JRightPadded<?>) o).getElement() instanceof Statement &&
                where.endsWith("statements") && !((JRightPadded<?>) o).getAfter().isEmpty() &&
                !((JRightPadded<?>) o).getMarkers().findFirst(org.openrewrite.java.marker.Semicolon.class).isPresent()) {
                findings.add("padding", "space after a statement with no ; in " + where, path,
                        around(((JRightPadded<?>) o).getAfter().getWhitespace()));
            }
            if (o instanceof J.Identifier) {
                name(((J.Identifier) o).getSimpleName(), "J.Identifier in " + where);
            } else if (o instanceof Cf.Tag) {
                name(((Cf.Tag) o).getName(), "Cf.Tag.name");
            } else if (o instanceof Cf.Tag.Closing) {
                name(((Cf.Tag.Closing) o).getName(), "Cf.Tag.Closing.name");
            } else if (o instanceof J.Modifier && ((J.Modifier) o).getKeyword() != null) {
                name(((J.Modifier) o).getKeyword(), "J.Modifier.keyword");
            } else if (o instanceof OperatorSpelling) {
                spelling(((OperatorSpelling) o).getText(), "OperatorSpelling");
            } else if (o instanceof KeywordSpelling) {
                spelling(((KeywordSpelling) o).getText(), "KeywordSpelling");
            }
            String owner = o instanceof J ? label(o.getClass()) : where;
            for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                if (c.getName().startsWith("java.")) {
                    break;
                }
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()) ||
                        field.isSynthetic()) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value;
                    try {
                        value = field.get(o);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                    String child = o instanceof J ? owner + "." + field.getName() :
                            where + " > " + label(o.getClass()) + "." + field.getName();
                    walk(value, child, reachable, seenValues);
                }
            }
        }

        void space(Space space, String where) {
            String whitespace = space.getWhitespace();
            Set<Character> unusual = new TreeSet<>();
            for (int i = 0; i < whitespace.length(); i++) {
                char c = whitespace.charAt(i);
                if (!isWhitespace(c)) {
                    findings.add("space", "non-whitespace in " + where, path, around(whitespace));
                    break;
                }
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    unusual.add(c);
                }
            }
            for (char c : unusual) {
                findings.add("space-char", String.format("U+%04X in %s", (int) c, where), path, "");
            }
            for (Comment comment : space.getComments()) {
                String suffix = comment.getSuffix();
                for (int i = 0; i < suffix.length(); i++) {
                    if (!isWhitespace(suffix.charAt(i))) {
                        findings.add("space", "non-whitespace in a comment suffix in " + where, path, around(suffix));
                        break;
                    }
                }
                if (comment instanceof TextComment) {
                    TextComment text = (TextComment) comment;
                    if (!text.isMultiline() && (text.getText().indexOf('\n') >= 0 || text.getText().indexOf('\r') >= 0)) {
                        findings.add("space", "a // comment spanning lines in " + where, path, around(text.getText()));
                    } else if (text.isMultiline() && text.getText().contains("*/")) {
                        findings.add("space", "a /* comment holding */ in " + where, path, around(text.getText()));
                    }
                } else if (comment instanceof TagComment) {
                    // the lexer reads an opening it cannot close as text, so a comment may open more than it closes
                    String text = ((TagComment) comment).getText();
                    if (count(text, "--->") > count(text, "<!---")) {
                        findings.add("space", "a <!--- comment closing more than it opens in " + where, path, around(text));
                    }
                } else {
                    findings.add("space", "a " + comment.getClass().getSimpleName() + " in " + where, path, "");
                }
            }
        }

        void name(String name, String where) {
            if (name.isEmpty()) {
                findings.add("name", "an empty name in " + where, path, "");
                return;
            }
            for (int i = 0; i < name.length(); i++) {
                if (isWhitespace(name.charAt(i))) {
                    findings.add("name", "whitespace in a name in " + where, path, around(name));
                    return;
                }
            }
            if (name.contains("/*") || name.contains("//") || name.contains("<!---")) {
                findings.add("name", "a comment in a name in " + where, path, around(name));
            }
        }

        void spelling(String text, String where) {
            if (text.contains("/*") || text.contains("//") || text.contains("<!---") || text.indexOf('\n') >= 0) {
                findings.add("name", "a comment or line break in " + where, path, around(text));
            }
        }

        // ------------------------------------------------------------------------------------
        // Probed print: where each element's prefix and syntax start and end
        // ------------------------------------------------------------------------------------

        void probe() {
            Cf.CompilationUnit probed = (Cf.CompilationUnit) new ColdFusionIsoVisitor<Integer>() {
                @Override
                public @Nullable J preVisit(J tree, Integer p) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    if (parent instanceof J) {
                        parents.put(tree.getId(), ((J) parent).getId());
                    }
                    return tree;
                }

                @Override
                public @Nullable J postVisit(J tree, Integer p) {
                    if (tree instanceof Cf.CompilationUnit) {
                        return tree;
                    }
                    visited.put(tree.getId(), tree);
                    return tree.withMarkers(tree.getMarkers().add(new Probe(tree.getId())));
                }
            }.visitNonNull(cu, 0);
            PrintOutputCapture<Integer> capture = new PrintOutputCapture<>(0, PROBES);
            probed.printAll(capture);
            String out = capture.getOut();
            StringBuilder clean = new StringBuilder(source.length());
            Map<UUID, int[]> counts = new HashMap<>();
            for (int i = 0; i < out.length(); i++) {
                char c = out.charAt(i);
                if (c != OPEN) {
                    clean.append(c);
                    continue;
                }
                int close = out.indexOf(CLOSE, i);
                char kind = out.charAt(i + 1);
                UUID id = UUID.fromString(out.substring(i + 2, close));
                int[] span = spans.computeIfAbsent(id, k -> new int[]{-1, -1, -1});
                int[] count = counts.computeIfAbsent(id, k -> new int[3]);
                int slot = kind == 'P' ? 0 : kind == 'S' ? 1 : 2;
                count[slot]++;
                if (span[slot] < 0) {
                    span[slot] = clean.length();
                }
                i = close;
            }
            if (!source.contentEquals(clean)) {
                findings.add("printer", "markers change the printed text", path, "");
            }
            for (Map.Entry<UUID, J> e : visited.entrySet()) {
                int[] count = counts.get(e.getKey());
                String type = label(e.getValue().getClass());
                if (count == null || count[1] == 0 || count[2] == 0) {
                    findings.add("printer", "markers not printed: " + type + " in " + parentLabel(e.getKey()), path, "");
                    spans.remove(e.getKey());
                } else if (count[1] > 1 || count[2] > 1) {
                    findings.add("printer", "markers printed " + count[1] + " times: " + type, path, "");
                }
            }
        }

        String parentLabel(UUID id) {
            UUID parent = parents.get(id);
            return parent == null ? "?" : label(visited.get(parent).getClass());
        }

        void spans() {
            // which element starts where: an element whose syntax starts with space has that space from
            // the prefix of the first element printed there, or from space of its own
            Map<Integer, List<UUID>> byPrefixStart = new HashMap<>();
            for (Map.Entry<UUID, int[]> e : spans.entrySet()) {
                if (e.getValue()[0] >= 0) {
                    byPrefixStart.computeIfAbsent(e.getValue()[0], k -> new ArrayList<>()).add(e.getKey());
                }
            }
            for (Map.Entry<UUID, J> e : visited.entrySet()) {
                int[] span = spans.get(e.getKey());
                if (span == null) {
                    continue;
                }
                J j = e.getValue();
                String type = label(j.getClass());
                UUID parent = parents.get(e.getKey());
                if (j instanceof J.Literal && parent != null && visited.get(parent) instanceof Cf.StringTemplate) {
                    // a segment of an interpolated string: its spaces are the string's content
                    continue;
                }
                if (j instanceof Cf.Text) {
                    // a no-break space is text, since a page shows it
                    String text = ((Cf.Text) j).getText();
                    if (text.isEmpty()) {
                        findings.add("text", "an empty Cf.Text", path, "");
                    } else if (isTemplateWhitespace(text.charAt(0)) || isTemplateWhitespace(text.charAt(text.length() - 1))) {
                        findings.add("text", "Cf.Text starting or ending with whitespace", path, around(text));
                    }
                    continue;
                }
                int start = span[1];
                int end = span[2];
                if (end <= start) {
                    continue;
                }
                boolean omitBraces = j instanceof J.Block && j.getMarkers().findFirst(OmitBraces.class).isPresent();
                if (startsWithSpace(start)) {
                    String culprit = null;
                    for (UUID inner : byPrefixStart.getOrDefault(start, emptyList())) {
                        int[] s = spans.get(inner);
                        if (s[1] > s[0] && !inner.equals(e.getKey())) {
                            culprit = label(visited.get(inner).getClass());
                            break;
                        }
                    }
                    findings.add(omitBraces ? "span-braceless" : "span", type + " starts with space" +
                            (culprit == null ? " of its own" : ", the prefix of a " + culprit + " in it"),
                            path, context(start));
                }
                if (endsWithSpace(start, end)) {
                    findings.add(omitBraces ? "span-braceless" : "span", type + " ends with space", path, context(end - 1));
                }
            }
        }

        boolean startsWithSpace(int start) {
            return isWhitespace(source.charAt(start)) || source.startsWith("//", start) ||
                   source.startsWith("/*", start) || source.startsWith("<!---", start);
        }

        boolean endsWithSpace(int start, int end) {
            return isWhitespace(source.charAt(end - 1)) || end - 2 > start && source.startsWith("*/", end - 2) ||
                   end - 4 > start && source.startsWith("--->", end - 4);
        }

        String context(int at) {
            int from = Math.max(0, at - 40);
            int to = Math.min(source.length(), at + 40);
            return (source.substring(from, at) + "\u2038" + source.substring(at, to)).replace("\r", "\\r").replace("\n", "\\n");
        }

        // ------------------------------------------------------------------------------------
        // What the code search index would find: declarations and calls it can place
        // ------------------------------------------------------------------------------------

        /**
         * What a file declares that an untyped call might name: its functions, its variables that hold functions, and
         * whether it includes other templates.
         */
        private static final class Declared {
            final Set<String> functions = new HashSet<>();
            final Set<String> functionVariables = new HashSet<>();
            boolean includes;
        }

        private static final Set<String> SCOPES = new HashSet<>(Arrays.asList("application", "session", "request",
                "server", "cgi", "form", "url", "client", "cookie", "arguments", "local", "variables", "this",
                "attributes", "caller", "thread", "super"));

        /**
         * Why a call has no method type.
         */
        String untyped(J.MethodInvocation method, Cursor cursor, Declared file) {
            String name = method.getSimpleName().toLowerCase(Locale.ROOT);
            Expression select = method.getSelect();
            J.ClassDeclaration classDecl = cursor.firstEnclosing(J.ClassDeclaration.class);
            JavaType.FullyQualified component = classDecl == null ? null : classDecl.getType();
            if (select == null) {
                if (file.functions.contains(name)) {
                    return component == null ? "unqualified: a function its template declares" :
                            "unqualified: a function the component declares inside a function";
                }
                if (file.functionVariables.contains(name)) {
                    return "unqualified: a variable holding a function";
                }
                if (component == null) {
                    String where = path.contains("/views/") || path.contains("/layouts/") ? "a ColdBox view or layout" : "a template";
                    return file.includes ? "unqualified: in " + where + " that includes others" : "unqualified: in " + where;
                }
                String unresolved = index.unresolvedSupertype(component);
                if (unresolved != null) {
                    return "unqualified: inherited from an unresolved " + firstSegment(unresolved) + ".*";
                }
                if (accessor(component, name)) {
                    return "unqualified: a property's generated accessor";
                }
                return file.includes ? "unqualified: in a component that includes templates" :
                        "unqualified: declared by neither the component nor its supertypes";
            }
            JavaType receiver = select.getType();
            JavaType.FullyQualified receiverType = TypeUtils.asFullyQualified(receiver);
            if (receiverType != null && !(receiver instanceof JavaType.Unknown)) {
                if (accessor(receiverType, name)) {
                    return "member: a property's generated accessor";
                }
                String unresolved = index.unresolvedSupertype(receiverType);
                if (unresolved != null) {
                    return "member: inherited from an unresolved " + firstSegment(unresolved) + ".*";
                }
                return receiverType.getInterfaces().isEmpty() ? "member: declared by neither the component nor its supertypes" :
                        "member: on a component with interfaces, declared by neither it nor its supertypes";
            }
            if (receiver != null && !(receiver instanceof JavaType.Unknown)) {
                return "member: on a value of type " + receiver;
            }
            String scope = scope(select);
            if (scope != null) {
                return "member: on a variable of the " + scope + " scope";
            }
            if (select instanceof J.Identifier) {
                J.MethodDeclaration enclosing = cursor.firstEnclosing(J.MethodDeclaration.class);
                if (enclosing != null) {
                    for (Statement parameter : enclosing.getParameters()) {
                        if (parameter instanceof J.VariableDeclarations &&
                            ((J.VariableDeclarations) parameter).getVariables().get(0).getSimpleName()
                                    .equalsIgnoreCase(((J.Identifier) select).getSimpleName())) {
                            J.VariableDeclarations declaration = (J.VariableDeclarations) parameter;
                            return declaration.getTypeExpression() == null ? "member: on a parameter with no type" :
                                    "member: on a parameter whose type does not resolve";
                        }
                    }
                }
                return "member: on a variable " + (enclosing == null ? "outside a function" :
                        definition(enclosing, ((J.Identifier) select).getSimpleName()));
            }
            if (select instanceof J.MethodInvocation) {
                return "member: on what " + returned((J.MethodInvocation) select);
            }
            if (select instanceof J.FieldAccess) {
                String root = root(select);
                return "member: on a field of " + (root == null ? "an expression" :
                        "rc".equals(root) || "prc".equals(root) || "event".equals(root) ? root : "a variable");
            }
            if (select instanceof J.NewClass) {
                return "member: on new of a component that does not resolve";
            }
            if (select instanceof J.Literal || select instanceof Cf.StringTemplate) {
                return "member: on a string literal";
            }
            if (select instanceof J.NewArray || select instanceof Cf.StructLiteral) {
                return "member: on an array or struct literal";
            }
            return "member: on a " + select.getClass().getSimpleName().replace("$", ".");
        }

        /**
         * What a call returns, for a call on its result: an untyped call's, a built-in function's, or a component
         * function's with no component type.
         */
        private static String returned(J.MethodInvocation call) {
            JavaType.Method type = call.getMethodType();
            if (type == null) {
                return "an untyped call returns";
            }
            return "cfml".equals(type.getDeclaringType().getFullyQualifiedName()) ?
                    "built-in " + type.getName() + "() returns" : "a component's function returns, which declares no component";
        }

        /**
         * How a function defines a variable it calls a method on, when the variable's type is not known.
         */
        private static String definition(J.MethodDeclaration function, String name) {
            String[] found = {null};
            String[] placeholder = {null};
            new ColdFusionIsoVisitor<Integer>() {
                @Override
                public J.ForEachLoop visitForEachLoop(J.ForEachLoop forEachLoop, Integer p) {
                    Statement control = forEachLoop.getControl().getVariable();
                    if (control instanceof J.VariableDeclarations) {
                        for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) control).getVariables()) {
                            if (variable.getSimpleName().equalsIgnoreCase(name)) {
                                found[0] = "a loop declares";
                            }
                        }
                    }
                    return super.visitForEachLoop(forEachLoop, p);
                }

                @Override
                public J.Try.Catch visitCatch(J.Try.Catch aCatch, Integer p) {
                    for (J.VariableDeclarations.NamedVariable variable : aCatch.getParameter().getTree().getVariables()) {
                        if (variable.getSimpleName().equalsIgnoreCase(name)) {
                            found[0] = "a catch declares";
                        }
                    }
                    return super.visitCatch(aCatch, p);
                }

                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    if (found[0] == null && variable.getSimpleName().equalsIgnoreCase(name) && variable.getInitializer() != null) {
                        if (ComponentIndex.isPlaceholder(variable.getInitializer())) {
                            placeholder[0] = "assigned only a placeholder";
                        } else {
                            found[0] = assigned(variable.getInitializer());
                        }
                    }
                    return super.visitVariable(variable, p);
                }

                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, Integer p) {
                    Expression variable = assignment.getVariable();
                    String assignedName = variable instanceof J.Identifier ? ((J.Identifier) variable).getSimpleName() :
                            variable instanceof J.FieldAccess && "local".equalsIgnoreCase(root(variable)) ?
                                    ((J.FieldAccess) variable).getSimpleName() : null;
                    if (found[0] == null && name.equalsIgnoreCase(assignedName)) {
                        found[0] = assigned(assignment.getAssignment());
                    }
                    return super.visitAssignment(assignment, p);
                }
            }.visit(function, 0);
            return found[0] != null ? found[0] : placeholder[0] != null ? placeholder[0] : "its function does not assign";
        }

        private static String assigned(Expression value) {
            if (value instanceof J.MethodInvocation) {
                return "assigned what " + returned((J.MethodInvocation) value);
            }
            if (value instanceof J.NewClass) {
                return "assigned new of a component that does not resolve";
            }
            return "assigned a " + value.getClass().getSimpleName().replace("$", ".");
        }

        private static @Nullable String root(Expression expression) {
            Expression e = expression;
            while (e instanceof J.FieldAccess || e instanceof J.ArrayAccess) {
                e = e instanceof J.FieldAccess ? ((J.FieldAccess) e).getTarget() : ((J.ArrayAccess) e).getIndexed();
            }
            return e instanceof J.Identifier ? ((J.Identifier) e).getSimpleName().toLowerCase(Locale.ROOT) : null;
        }

        private static String firstSegment(String name) {
            int dot = name.indexOf('.');
            return (dot < 0 ? name : name.substring(0, dot)).toLowerCase(Locale.ROOT);
        }

        /**
         * Whether a name is what a component's property would generate: {@code getName}, {@code setName}, or one of the
         * ORM's {@code hasName}, {@code addName} and {@code removeName}.
         */
        private static boolean accessor(JavaType.FullyQualified type, String name) {
            for (String prefix : new String[]{"get", "set", "has", "add", "remove", "is"}) {
                if (name.startsWith(prefix) && name.length() > prefix.length()) {
                    String property = name.substring(prefix.length());
                    Set<JavaType.FullyQualified> seen = Collections.newSetFromMap(new IdentityHashMap<>());
                    for (JavaType.FullyQualified t = type; t != null && seen.add(t); t = t.getSupertype()) {
                        for (JavaType.Variable member : t.getMembers()) {
                            if (member.getName().equalsIgnoreCase(property)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        /**
         * The scope a reference is in, {@code application} for {@code application.users.find}, or null.
         */
        private static @Nullable String scope(Expression expression) {
            Expression e = expression;
            while (true) {
                if (e instanceof J.FieldAccess) {
                    e = ((J.FieldAccess) e).getTarget();
                } else if (e instanceof J.ArrayAccess) {
                    e = ((J.ArrayAccess) e).getIndexed();
                } else if (e instanceof J.Identifier) {
                    String name = ((J.Identifier) e).getSimpleName().toLowerCase(Locale.ROOT);
                    return e == expression || !SCOPES.contains(name) ? null : name;
                } else {
                    return null;
                }
            }
        }

        void navigation() {
            Declared file = new Declared();
            new ColdFusionVisitor<Integer>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                    file.functions.add(method.getSimpleName().toLowerCase(Locale.ROOT));
                    return super.visitMethodDeclaration(method, p);
                }

                @Override
                public J visitAssignment(J.Assignment assignment, Integer p) {
                    Expression value = assignment.getAssignment();
                    if (value instanceof J.Lambda || value instanceof Cf.Closure) {
                        Expression variable = assignment.getVariable();
                        String name = variable instanceof J.Identifier ? ((J.Identifier) variable).getSimpleName() :
                                variable instanceof J.FieldAccess ? ((J.FieldAccess) variable).getSimpleName() : null;
                        if (name != null) {
                            file.functionVariables.add(name.toLowerCase(Locale.ROOT));
                        }
                    }
                    return super.visitAssignment(assignment, p);
                }

                @Override
                public J visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    if (variable.getInitializer() instanceof J.Lambda || variable.getInitializer() instanceof Cf.Closure) {
                        file.functionVariables.add(variable.getSimpleName().toLowerCase(Locale.ROOT));
                    }
                    return super.visitVariable(variable, p);
                }

                @Override
                public J visitTag(Cf.Tag tag, Integer p) {
                    file.includes |= tag.isNamed("cfinclude");
                    return super.visitTag(tag, p);
                }

                @Override
                public J visitScriptTag(Cf.ScriptTag scriptTag, Integer p) {
                    file.includes |= "include".equalsIgnoreCase(scriptTag.getName().getSimpleName());
                    return super.visitScriptTag(scriptTag, p);
                }
            }.visit(cu, 0);
            new ColdFusionVisitor<Integer>() {
                @Override
                public J visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                    findings.count(method.getMarkers().findFirst(TagSyntax.class).isPresent() ?
                            "function declarations written as tags (J.MethodDeclaration)" :
                            "script function declarations (J.MethodDeclaration)");
                    for (Statement parameter : method.getParameters()) {
                        if (parameter.getMarkers().findFirst(TagSyntax.class).isPresent()) {
                            findings.count("parameters written as <cfargument> (J.VariableDeclarations)");
                        }
                    }
                    return super.visitMethodDeclaration(method, p);
                }

                @Override
                public J visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    if (parent instanceof J.Block && getCursor().getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration) {
                        findings.count(multiVariable.getMarkers().findFirst(TagSyntax.class).isPresent() ?
                                "fields written as <cfproperty> (J.VariableDeclarations)" :
                                "fields written as property (J.VariableDeclarations)");
                    }
                    return super.visitVariableDeclarations(multiVariable, p);
                }

                @Override
                public J visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                    findings.count("class declarations (J.ClassDeclaration)");
                    if (classDecl.getMarkers().findFirst(TagSyntax.class).isPresent()) {
                        findings.count("class declarations written as tags (J.ClassDeclaration)");
                    }
                    if (classDecl.getExtends() != null) {
                        findings.count("components extending a named type");
                        if (classDecl.getType() != null && classDecl.getType().getSupertype() != null) {
                            findings.count("components extending a component they resolve");
                        }
                    }
                    if (classDecl.getImplements() != null) {
                        findings.count("components implementing named types");
                    }
                    return super.visitClassDeclaration(classDecl, p);
                }

                @Override
                public J visitAnnotation(J.Annotation annotation, Integer p) {
                    JavaType.FullyQualified type = TypeUtils.asFullyQualified(annotation.getType());
                    findings.count(type == null ? "attributes with no type (J.Annotation)" :
                            type.getFullyQualifiedName().indexOf('.') < 0 ? "attributes typed by their own name (J.Annotation)" :
                                    "attributes the language defines (J.Annotation typed " + type + ")");
                    return super.visitAnnotation(annotation, p);
                }

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, Integer p) {
                    findings.count("calls (J.MethodInvocation)");
                    JavaType.Method type = method.getMethodType();
                    if (type != null) {
                        findings.count("calls with a method type");
                        findings.count("cfml".equals(type.getDeclaringType().getFullyQualifiedName()) ?
                                "calls typed as built-in functions" : "calls typed as a component's methods");
                    } else {
                        String example = method.printTrimmed(getCursor().getParentOrThrow()).replaceAll("\\s+", " ");
                        findings.add("untyped calls", untyped(method, getCursor(), file),
                                path, example.substring(0, Math.min(80, example.length())));
                    }
                    return super.visitMethodInvocation(method, p);
                }

                @Override
                public J visitNewClass(J.NewClass newClass, Integer p) {
                    findings.count("new (J.NewClass)");
                    if (newClass.getConstructorType() != null) {
                        findings.count("new typed as a component's");
                    }
                    return super.visitNewClass(newClass, p);
                }

                @Override
                public J visitTag(Cf.Tag tag, Integer p) {
                    String name = tag.getName().toLowerCase(Locale.ROOT);
                    if ("cffunction".equals(name) || "cfcomponent".equals(name) || "cfinterface".equals(name) ||
                        "cfargument".equals(name) || "cfproperty".equals(name) || "cfinvoke".equals(name) ||
                        "cfmodule".equals(name) || "cfinclude".equals(name) || "cfobject".equals(name)) {
                        findings.count("tags <" + name + ">");
                    }
                    return super.visitTag(tag, p);
                }

                @Override
                public J visitInvocation(Cf.Invocation invocation, Integer p) {
                    findings.count("calls of an expression (Cf.Invocation)");
                    return super.visitInvocation(invocation, p);
                }

                @Override
                public J visitTagCall(Cf.TagCall tagCall, Integer p) {
                    findings.count("tags as calls with a body (Cf.TagCall)");
                    return super.visitTagCall(tagCall, p);
                }

                @Override
                public J visitScriptTag(Cf.ScriptTag scriptTag, Integer p) {
                    findings.count("tags as script statements (Cf.ScriptTag)");
                    return super.visitScriptTag(scriptTag, p);
                }
            }.visit(cu, 0);
        }
    }

    private static List<UUID> emptyList() {
        return Collections.emptyList();
    }

    static String label(Class<?> c) {
        String name = c.getName();
        name = name.substring(name.lastIndexOf('.') + 1).replace('$', '.');
        return name;
    }

    static String around(String s) {
        String shown = s.length() > 80 ? s.substring(0, 80) + "..." : s;
        return shown.replace("\r", "\\r").replace("\n", "\\n");
    }

    static int count(String s, String of) {
        int n = 0;
        for (int i = s.indexOf(of); i >= 0; i = s.indexOf(of, i + of.length())) {
            n++;
        }
        return n;
    }

    static boolean isWhitespace(char c) {
        return isTemplateWhitespace(c) || c == '\u00A0' || c == '\uFEFF';
    }

    static boolean isTemplateWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\u000B';
    }

    private static class Findings {
        final Map<String, Map<String, Integer>> byGroup = new TreeMap<>();
        final Map<String, Set<String>> files = new HashMap<>();
        final Map<String, List<String>> examples = new HashMap<>();
        final Map<String, Integer> counts = new TreeMap<>();

        void add(String group, String what, String path, String context) {
            byGroup.computeIfAbsent(group, g -> new TreeMap<>()).merge(what, 1, Integer::sum);
            files.computeIfAbsent(group + '\0' + what, k -> new TreeSet<>()).add(path);
            List<String> list = examples.computeIfAbsent(group + '\0' + what, k -> new ArrayList<>());
            if (list.size() < 3 || "print".equals(group)) {
                list.add(path + (context.isEmpty() ? "" : ": " + context));
            }
        }

        void count(String what) {
            counts.merge(what, 1, Integer::sum);
        }

        String report() {
            StringBuilder out = new StringBuilder();
            for (Map.Entry<String, Map<String, Integer>> group : byGroup.entrySet()) {
                out.append("== ").append(group.getKey()).append('\n');
                group.getValue().entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                        .forEach(e -> {
                            String key = group.getKey() + '\0' + e.getKey();
                            out.append(String.format("  %7d in %5d files  %s%n", e.getValue(), files.get(key).size(), e.getKey()));
                            for (String example : examples.get(key)) {
                                out.append("                          e.g. ").append(example).append('\n');
                            }
                        });
            }
            out.append("== navigation census\n");
            counts.forEach((what, n) -> out.append(String.format("  %7d  %s%n", n, what)));
            return out.toString();
        }
    }
}
