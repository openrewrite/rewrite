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
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.*;

import java.util.*;

/**
 * The mappings a repository declares for itself. An {@code Application.cfc} declares them in its pseudo-constructor,
 * {@code this.mappings["/testbox"] = rootPath & "testbox"}, and a ColdBox module's {@code ModuleConfig.cfc} declares
 * one for its own directory, {@code this.cfmapping = "cbauth"}. A path is known as far as it is made of strings,
 * {@code getDirectoryFromPath(getCurrentTemplatePath())}, {@code expandPath} and the variables assigned before it,
 * and a mapping whose path is not known, or leaves the repository, is left out. Paths are relative to the
 * repository's root.
 */
final class DeclaredMappings {
    private DeclaredMappings() {
    }

    /**
     * An {@code Application.cfc}'s mappings, by logical path in lower case without slashes around it, to directories.
     */
    static Map<String, String> application(J.ClassDeclaration application, String file) {
        Map<String, String> variables = new HashMap<>();
        Map<String, String> mappings = new LinkedHashMap<>();
        for (Statement statement : pseudoConstructor(application.getBody())) {
            Expression expression = statement instanceof Cf.Tag ? (Expression) ((Cf.Tag) statement).getExpression() :
                    statement instanceof Expression ? (Expression) statement : null;
            if (!(expression instanceof J.Assignment)) {
                continue;
            }
            J.Assignment assignment = (J.Assignment) expression;
            Expression variable = assignment.getVariable();
            String mapping = mappingName(variable);
            if (mapping != null) {
                String directory = directory(evaluate(assignment.getAssignment(), variables, mappings, file));
                if (directory != null) {
                    mappings.put(mapping, directory);
                }
            } else if (isMappings(variable) && assignment.getAssignment() instanceof Cf.StructLiteral) {
                for (Expression element : ((Cf.StructLiteral) assignment.getAssignment()).getEntries()) {
                    if (!(element instanceof Cf.KeyValue)) {
                        continue;
                    }
                    Cf.KeyValue entry = (Cf.KeyValue) element;
                    String name = entry.getKey() instanceof J.Identifier ? ((J.Identifier) entry.getKey()).getSimpleName() :
                            evaluate(entry.getKey(), variables, mappings, file);
                    String directory = directory(evaluate(entry.getValue(), variables, mappings, file));
                    if (name != null && directory != null) {
                        mappings.put(logical(name), directory);
                    }
                }
            } else {
                String name = variableName(variable);
                String value = evaluate(assignment.getAssignment(), variables, mappings, file);
                if (name != null && value != null) {
                    variables.put(name, value);
                }
            }
        }
        return mappings;
    }

    /**
     * The logical path a ColdBox module maps to its own directory, or null when it declares none.
     */
    static @Nullable String module(J.ClassDeclaration moduleConfig) {
        for (Statement statement : pseudoConstructor(moduleConfig.getBody())) {
            Expression expression = statement instanceof Cf.Tag ? (Expression) ((Cf.Tag) statement).getExpression() :
                    statement instanceof Expression ? (Expression) statement : null;
            if (expression instanceof J.Assignment) {
                J.Assignment assignment = (J.Assignment) expression;
                if ("cfmapping".equalsIgnoreCase(thisMember(assignment.getVariable())) &&
                    assignment.getAssignment() instanceof J.Literal &&
                    ((J.Literal) assignment.getAssignment()).getValue() instanceof String) {
                    String name = logical((String) ((J.Literal) assignment.getAssignment()).getValue());
                    return name.isEmpty() ? null : name;
                }
            }
        }
        return null;
    }

    /**
     * A logical path as a mapping's name: in lower case, with {@code /} between its names and none around them.
     */
    static String logical(String name) {
        String normalized = name.replace('\\', '/').toLowerCase(Locale.ROOT);
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * The statements a component runs when it is created, outside its functions, including those in its
     * {@code <cfscript>} tags.
     */
    private static List<Statement> pseudoConstructor(J.Block body) {
        List<Statement> statements = new ArrayList<>();
        for (Statement statement : body.getStatements()) {
            if (statement instanceof Cf.Tag && ((Cf.Tag) statement).isNamed("cfscript") && ((Cf.Tag) statement).getBody() != null) {
                statements.addAll(Objects.requireNonNull(((Cf.Tag) statement).getBody()).getStatements());
            } else {
                statements.add(statement);
            }
        }
        return statements;
    }

    /**
     * The mapping {@code this.mappings["/name"]} or {@code this.mappings.name} assigns, or null.
     */
    private static @Nullable String mappingName(Expression variable) {
        if (variable instanceof J.ArrayAccess && isMappings(((J.ArrayAccess) variable).getIndexed())) {
            Expression index = ((J.ArrayAccess) variable).getDimension().getIndex();
            return index instanceof J.Literal && ((J.Literal) index).getValue() instanceof String ?
                    logical((String) ((J.Literal) index).getValue()) : null;
        }
        if (variable instanceof J.FieldAccess && isMappings(((J.FieldAccess) variable).getTarget())) {
            return logical(((J.FieldAccess) variable).getSimpleName());
        }
        return null;
    }

    private static boolean isMappings(Expression expression) {
        return "mappings".equalsIgnoreCase(thisMember(expression));
    }

    /**
     * The name of {@code this.name}, or null for anything else.
     */
    private static @Nullable String thisMember(Expression expression) {
        if (expression instanceof J.FieldAccess && ((J.FieldAccess) expression).getTarget() instanceof J.Identifier &&
            "this".equalsIgnoreCase(((J.Identifier) ((J.FieldAccess) expression).getTarget()).getSimpleName())) {
            return ((J.FieldAccess) expression).getSimpleName();
        }
        return null;
    }

    /**
     * The name of a variable a pseudo-constructor assigns, {@code rootPath}, {@code variables.rootPath} or
     * {@code this.rootPath}, in lower case, or null.
     */
    private static @Nullable String variableName(Expression variable) {
        if (variable instanceof J.Identifier) {
            return ((J.Identifier) variable).getSimpleName().toLowerCase(Locale.ROOT);
        }
        if (variable instanceof J.FieldAccess && ((J.FieldAccess) variable).getTarget() instanceof J.Identifier) {
            String scope = ((J.Identifier) ((J.FieldAccess) variable).getTarget()).getSimpleName();
            if ("variables".equalsIgnoreCase(scope) || "this".equalsIgnoreCase(scope)) {
                return ((J.FieldAccess) variable).getSimpleName().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    /**
     * What a string expression evaluates to, where paths are relative to the repository's root, or null when it is
     * not known.
     */
    private static @Nullable String evaluate(Expression expression, Map<String, String> variables,
                                             Map<String, String> mappings, String file) {
        if (expression instanceof J.Literal) {
            Object value = ((J.Literal) expression).getValue();
            return value instanceof String ? (String) value : null;
        }
        if (expression instanceof Cf.Binary && ((Cf.Binary) expression).getOperator() == Cf.Binary.Type.Concat) {
            String left = evaluate(((Cf.Binary) expression).getLeft(), variables, mappings, file);
            String right = evaluate(((Cf.Binary) expression).getRight(), variables, mappings, file);
            return left == null || right == null ? null : left + right;
        }
        if (expression instanceof J.Parentheses && ((J.Parentheses<?>) expression).getTree() instanceof Expression) {
            return evaluate((Expression) ((J.Parentheses<?>) expression).getTree(), variables, mappings, file);
        }
        String variable = variableName(expression);
        if (variable != null) {
            return variables.get(variable);
        }
        if (expression instanceof J.ArrayAccess) {
            String mapping = mappingName(expression);
            return mapping == null ? null : mappings.get(mapping);
        }
        if (expression instanceof J.MethodInvocation && ((J.MethodInvocation) expression).getSelect() == null) {
            J.MethodInvocation call = (J.MethodInvocation) expression;
            List<Expression> arguments = call.getArguments();
            String argument = arguments.size() == 1 && !(arguments.get(0) instanceof J.Empty) ?
                    evaluate(arguments.get(0), variables, mappings, file) : null;
            switch (call.getSimpleName().toLowerCase(Locale.ROOT)) {
                case "getcurrenttemplatepath":
                    return arguments.isEmpty() || arguments.get(0) instanceof J.Empty ? file : null;
                case "getdirectoryfrompath":
                    return argument == null ? null : argument.substring(0, argument.replace('\\', '/').lastIndexOf('/') + 1);
                case "expandpath":
                    // a path from the web root, or from the directory of the file calling it
                    return argument == null ? null : argument.startsWith("/") || argument.startsWith("\\") ? argument :
                            file.substring(0, file.lastIndexOf('/') + 1) + argument;
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * A path as a directory relative to the repository's root, in lower case, or null when it leaves the repository.
     */
    static @Nullable String directory(@Nullable String path) {
        if (path == null) {
            return null;
        }
        Deque<String> names = new ArrayDeque<>();
        for (String name : path.replace('\\', '/').split("/")) {
            if (name.isEmpty() || ".".equals(name)) {
                continue;
            }
            if ("..".equals(name)) {
                if (names.isEmpty()) {
                    return null;
                }
                names.removeLast();
            } else {
                names.addLast(name.toLowerCase(Locale.ROOT));
            }
        }
        return String.join("/", names);
    }
}
