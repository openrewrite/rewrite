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
package org.openrewrite.coldfusion.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.coldfusion.marker.TagSyntax;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.marker.OmitBraces;
import org.openrewrite.java.tree.*;

import java.util.*;

/**
 * Which whitespace a template writes to its output. Text between tags and the whitespace around it are written to
 * the response, so changing that whitespace changes the page, a CSV export or the inside of a {@code <pre>}. A
 * template writes none of it where:
 * <ul>
 *     <li>a {@code <cfsilent>} encloses it,</li>
 *     <li>it is in a {@code <cffunction>} whose {@code output} is false, or in the body of a {@code <cfcomponent>}
 *     whose {@code output} is false, outside its functions,</li>
 *     <li>a {@code <cfsetting enablecfoutputonly="true">} before it, in the same body or a body around it, holds
 *     outside {@code <cfoutput>}.</li>
 * </ul>
 * Only the tags that run their body in place, such as {@code <cfif>} and {@code <cfloop>}, pass that silence on to
 * what they enclose. The body of any other tag, such as {@code <cfsavecontent>}, {@code <cfquery>}, {@code
 * <cfmail>} or a custom tag, is used for something and keeps its whitespace. So does {@code <cfoutput>}. Whitespace
 * inside a tag, inside {@code <cfscript>} and in script components is never written.
 */
public final class TemplateOutput {

    private static final Set<String> IN_PLACE = new HashSet<>(Arrays.asList(
            "cfif", "cfelse", "cfelseif", "cfloop", "cfwhile", "cfswitch", "cfcase", "cfdefaultcase", "cftry",
            "cfcatch", "cffinally", "cflock", "cftransaction", "cfprocessingdirective", "cftimer", "cfstoredproc",
            "cfinvoke", "cfhttp"));

    private static final String LEVELS = "org.openrewrite.coldfusion.cfoutputonly.";
    private static final String INDEX = "org.openrewrite.coldfusion.statementIndex";

    private TemplateOutput() {
    }

    /**
     * Whether a file is a template, in tag syntax, rather than a script component.
     */
    public static boolean isTemplate(Cf.CompilationUnit cu) {
        for (Statement statement : cu.getStatements()) {
            if (statement instanceof J.ClassDeclaration && !isTagSyntax(statement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the block at this cursor is the body of a tag, written between its start and end tags, rather than
     * script.
     */
    public static boolean isTemplateBody(Cursor block) {
        Object value = block.getValue();
        if (!(value instanceof J.Block) || !((J.Block) value).getMarkers().findFirst(OmitBraces.class).isPresent()) {
            return false;
        }
        Object owner = block.getParentTreeCursor().getValue();
        return !(owner instanceof Cf.Tag && ((Cf.Tag) owner).isNamed("cfscript"));
    }

    /**
     * Whether the list at this cursor holds template statements, tags and text, separated by template whitespace:
     * a tag's body, a template file, or the {@code <cfargument>} tags of a function written as a tag.
     */
    public static boolean isTemplateList(Cursor list) {
        Object value = list.getValue();
        if (value instanceof Cf.CompilationUnit) {
            return isTemplate((Cf.CompilationUnit) value);
        }
        if (value instanceof J.MethodDeclaration) {
            return isTagSyntax((J) value);
        }
        return isTemplateBody(list);
    }

    /**
     * Whether the statement at this cursor is in a template list, so the whitespace before it is template whitespace.
     */
    public static boolean isInTemplateList(Cursor statement) {
        Cursor list = statement.getParentTreeCursor();
        Object value = list.getValue();
        if (value instanceof J.MethodDeclaration) {
            return isTagSyntax((J) value) && containsSame(((J.MethodDeclaration) value).getParameters(), statement.getValue());
        }
        return value instanceof Tree && isTemplateList(list);
    }

    /**
     * Whether the template writes the whitespace before the statement at this cursor, which is in a template list.
     */
    public static boolean writesBefore(Cursor statement) {
        Cursor list = statement.getParentTreeCursor();
        if (list.getValue() instanceof J.MethodDeclaration) {
            return writes(list, 0);
        }
        return writes(list, indexIn(list, statement.getValue()));
    }

    /**
     * Whether the template writes the whitespace at the end of the template list at this cursor: the whitespace
     * before a tag's end tag, or at the end of a template file.
     */
    public static boolean writesAtEnd(Cursor list) {
        return writes(list, Integer.MAX_VALUE);
    }

    /**
     * Whether the template writes the whitespace before the statement at this position of the template list at this
     * cursor.
     */
    public static boolean writes(Cursor list, int position) {
        List<Cursor> lists = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        Cursor c = list;
        int at = position;
        while (true) {
            Object value = c.getValue();
            if (value instanceof J.Block) {
                lists.add(c);
                positions.add(at);
                Cursor ownerCursor = c.getParentTreeCursor();
                Object owner = ownerCursor.getValue();
                if (owner instanceof Cf.Tag) {
                    Cf.Tag tag = (Cf.Tag) owner;
                    if (tag.isNamed("cfsilent")) {
                        return false;
                    }
                    if (tag.isNamed("cffunction")) {
                        return !isFalse(tagAttribute(tag, "output")) && level(lists, positions) == 0;
                    }
                    if (!IN_PLACE.contains(tag.getName().toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                } else if (owner instanceof J.MethodDeclaration && isTagSyntax((J) owner)) {
                    return !isFalse(annotation(((J.MethodDeclaration) owner).getLeadingAnnotations(), "output")) &&
                           level(lists, positions) == 0;
                } else if (owner instanceof J.ClassDeclaration && isTagSyntax((J) owner)) {
                    if (isFalse(annotation(((J.ClassDeclaration) owner).getLeadingAnnotations(), "output"))) {
                        return false;
                    }
                } else {
                    return true;
                }
                Cursor parent = ownerCursor.getParentTreeCursor();
                if (!(parent.getValue() instanceof Tree)) {
                    return level(lists, positions) == 0;
                }
                at = indexIn(parent, owner);
                c = parent;
            } else if (value instanceof J.MethodDeclaration) {
                // the parameters of a function written as a tag, which come before its body
                return !isFalse(annotation(((J.MethodDeclaration) value).getLeadingAnnotations(), "output")) &&
                       level(lists, positions) == 0;
            } else if (value instanceof Cf.CompilationUnit) {
                lists.add(c);
                positions.add(at);
                return level(lists, positions) == 0;
            } else {
                return true;
            }
        }
    }

    /**
     * The count of {@code enablecfoutputonly} settings in force at a position, given the lists around it from the
     * innermost out.
     */
    private static int level(List<Cursor> lists, List<Integer> positions) {
        int level = 0;
        for (int i = lists.size() - 1; i >= 0; i--) {
            int[] levels = levels(lists.get(i), level);
            level = levels[Math.min(positions.get(i), levels.length - 1)];
        }
        return level;
    }

    /**
     * The count before each statement of a list, and after its last, given the count at its start.
     */
    private static int[] levels(Cursor list, int start) {
        return list.computeMessageIfAbsent(LEVELS + start, k -> {
            List<Statement> statements = statements(list.getValue());
            int[] levels = new int[statements.size() + 1];
            levels[0] = start;
            for (int i = 0; i < statements.size(); i++) {
                levels[i + 1] = after(statements.get(i), levels[i]);
            }
            return levels;
        });
    }

    private static int after(Statement statement, int level) {
        if (!(statement instanceof Cf.Tag)) {
            return level;
        }
        Cf.Tag tag = (Cf.Tag) statement;
        if (tag.isNamed("cfsetting")) {
            Cf.TagAttribute setting = tag.getAttribute("enablecfoutputonly");
            if (setting == null) {
                return level;
            }
            String value = literal(setting.getValue());
            if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) {
                return level + 1;
            }
            if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value)) {
                return Math.max(0, level - 1);
            }
            return 0;
        }
        J.Block body = tag.getBody();
        return body == null || tag.isNamed("cffunction") ? level : lowered(body, level);
    }

    /**
     * The count after a body that may or may not have run. A setting in it that would raise the count is ignored,
     * since the lower count writes more.
     */
    private static int lowered(J.Block body, int level) {
        int result = level;
        for (Statement statement : body.getStatements()) {
            if (statement instanceof Cf.Tag) {
                int after = after(statement, result);
                if (after < result) {
                    result = after;
                }
            }
        }
        return result;
    }

    private static int indexIn(Cursor list, Object statement) {
        Map<Object, Integer> index = list.computeMessageIfAbsent(INDEX, k -> {
            List<Statement> statements = statements(list.getValue());
            Map<Object, Integer> byIdentity = new IdentityHashMap<>(statements.size());
            for (int i = 0; i < statements.size(); i++) {
                byIdentity.put(statements.get(i), i);
            }
            return byIdentity;
        });
        Integer i = index.get(statement);
        if (i != null) {
            return i;
        }
        if (statement instanceof Tree) {
            List<Statement> statements = statements(list.getValue());
            for (int s = 0; s < statements.size(); s++) {
                if (statements.get(s).getId().equals(((Tree) statement).getId())) {
                    return s;
                }
            }
        }
        // a statement not yet in the list, as one a recipe formats before adding it
        return 0;
    }

    private static List<Statement> statements(Object list) {
        if (list instanceof J.Block) {
            return ((J.Block) list).getStatements();
        }
        if (list instanceof Cf.CompilationUnit) {
            return ((Cf.CompilationUnit) list).getStatements();
        }
        return Collections.emptyList();
    }

    private static boolean containsSame(List<?> list, Object element) {
        for (Object e : list) {
            if (e == element) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTagSyntax(J j) {
        return j.getMarkers().findFirst(TagSyntax.class).isPresent();
    }

    private static @Nullable Expression annotation(List<J.Annotation> annotations, String name) {
        for (J.Annotation annotation : annotations) {
            if (annotation.getSimpleName().equalsIgnoreCase(name)) {
                List<Expression> arguments = annotation.getArguments();
                return arguments == null || arguments.isEmpty() ? null : arguments.get(0);
            }
        }
        return null;
    }

    private static @Nullable Expression tagAttribute(Cf.Tag tag, String name) {
        Cf.TagAttribute attribute = tag.getAttribute(name);
        return attribute == null ? null : attribute.getValue();
    }

    private static boolean isFalse(@Nullable Expression value) {
        String literal = literal(value);
        return "false".equalsIgnoreCase(literal) || "no".equalsIgnoreCase(literal) || "0".equals(literal);
    }

    private static @Nullable String literal(@Nullable Expression value) {
        if (value instanceof J.Literal) {
            Object literal = ((J.Literal) value).getValue();
            return literal == null ? null : literal.toString();
        }
        if (value instanceof J.Identifier) {
            return ((J.Identifier) value).getSimpleName();
        }
        return null;
    }
}
