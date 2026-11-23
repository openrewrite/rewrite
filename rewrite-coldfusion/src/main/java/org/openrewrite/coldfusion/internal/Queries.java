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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The tags and functions whose result is a query, unless they ask for another type. A {@code <cfquery name="q">}
 * assigns it to the variable it names, as {@code <cfdirectory action="list">}, {@code <cfdbinfo>},
 * {@code <cfprocresult>}, {@code <cfldap action="query">} and {@code <cfzip action="list">} do, in tags or in script.
 * {@code queryExecute(sql, params, options)} returns one, and so does {@code directoryList(path, recurse, "query")},
 * whose other {@code listInfo} values give an array.
 */
final class Queries {
    private Queries() {
    }

    /**
     * The tags that assign a query, by their names without {@code cf}, to the action that does, or to null when each
     * of their actions does.
     */
    private static final Map<String, @Nullable String> TAGS = new HashMap<>();

    /**
     * The action a tag takes when it names none.
     */
    private static final Map<String, String> DEFAULT_ACTIONS = new HashMap<>();

    static {
        TAGS.put("query", null);
        TAGS.put("dbinfo", null);
        TAGS.put("procresult", null);
        TAGS.put("directory", "list");
        TAGS.put("ldap", "query");
        TAGS.put("zip", "list");
        DEFAULT_ACTIONS.put("directory", "list");
        DEFAULT_ACTIONS.put("ldap", "query");
        DEFAULT_ACTIONS.put("zip", "zip");
    }

    /**
     * The variable a query tag assigns its result to, or null for another statement or a name only known when it runs.
     */
    static @Nullable String variable(J statement) {
        String name = string(isQueryTag(statement) ? attribute(statement, "name") : null);
        return name == null ? null : name.trim();
    }

    /**
     * What a query tag assigns or a {@code queryExecute} or {@code directoryList} call returns, or null for anything
     * else or for a type asked for only when it runs.
     */
    static @Nullable JavaType result(J queryOrCall) {
        if (queryOrCall instanceof J.MethodInvocation) {
            J.MethodInvocation call = (J.MethodInvocation) queryOrCall;
            if (call.getSelect() != null) {
                return null;
            }
            if ("queryExecute".equalsIgnoreCase(call.getSimpleName())) {
                Expression options = argument(call.getArguments(), "options", 2);
                if (options == null) {
                    return BuiltIns.QUERY;
                }
                return options instanceof Cf.StructLiteral ? returnType(key((Cf.StructLiteral) options, "returntype")) : null;
            }
            if ("directoryList".equalsIgnoreCase(call.getSimpleName())) {
                Expression listInfo = argument(call.getArguments(), "listInfo", 2);
                String asked = listInfo == null ? "path" : string(listInfo);
                return asked == null ? null : listing(asked);
            }
            return null;
        }
        if (!isQueryTag(queryOrCall)) {
            return null;
        }
        return "query".equals(tagName(queryOrCall)) ? returnType(attribute(queryOrCall, "returntype")) : BuiltIns.QUERY;
    }

    /**
     * What {@code directoryList} returns for a {@code listInfo}: a query of the files, or an array of their paths or
     * their names.
     */
    private static @Nullable JavaType listing(String listInfo) {
        switch (listInfo.trim().toLowerCase(Locale.ROOT)) {
            case "query":
                return BuiltIns.QUERY;
            case "path":
            case "name":
                return BuiltIns.ARRAY;
            default:
                return null;
        }
    }

    private static @Nullable JavaType returnType(@Nullable Expression returnType) {
        if (returnType == null) {
            return BuiltIns.QUERY;
        }
        String asked = string(returnType);
        if (asked == null) {
            return null;
        }
        switch (asked.trim().toLowerCase(Locale.ROOT)) {
            case "query":
                return BuiltIns.QUERY;
            case "array":
                return BuiltIns.ARRAY;
            case "struct":
                return BuiltIns.STRUCT;
            default:
                return null;
        }
    }

    /**
     * A tag's name without {@code cf}, in tag syntax, in script or as a call, or null for another statement.
     */
    private static @Nullable String tagName(J statement) {
        String name;
        if (statement instanceof Cf.Tag) {
            name = ((Cf.Tag) statement).getName();
        } else if (statement instanceof Cf.TagCall) {
            name = ((Cf.TagCall) statement).getCall().getSimpleName();
        } else if (statement instanceof Cf.ScriptTag) {
            name = ((Cf.ScriptTag) statement).getName().getSimpleName();
            if (!name.regionMatches(true, 0, "cf", 0, 2)) {
                return name.toLowerCase(Locale.ROOT);
            }
        } else {
            return null;
        }
        return name.regionMatches(true, 0, "cf", 0, 2) ? name.substring(2).toLowerCase(Locale.ROOT) : null;
    }

    private static boolean isQueryTag(J statement) {
        String name = tagName(statement);
        if (name == null || !TAGS.containsKey(name)) {
            return false;
        }
        String action = TAGS.get(name);
        if (action == null) {
            return true;
        }
        Expression written = attribute(statement, "action");
        String taken = written == null ? DEFAULT_ACTIONS.get(name) : string(written);
        return taken != null && action.equalsIgnoreCase(taken.trim());
    }

    private static @Nullable Expression attribute(J statement, String name) {
        if (statement instanceof Cf.TagCall) {
            return argument(((Cf.TagCall) statement).getCall().getArguments(), name, -1);
        }
        List<Cf.TagAttribute> attributes = statement instanceof Cf.Tag ? ((Cf.Tag) statement).getAttributes() :
                statement instanceof Cf.ScriptTag ? ((Cf.ScriptTag) statement).getAttributes() : null;
        if (attributes != null) {
            for (Cf.TagAttribute attribute : attributes) {
                if (attribute.getName().getSimpleName().equalsIgnoreCase(name)) {
                    return attribute.getValue();
                }
            }
        }
        return null;
    }

    /**
     * An argument a call passes by name, or at a position when it passes it positionally.
     */
    private static @Nullable Expression argument(List<Expression> arguments, String name, int position) {
        for (int i = 0; i < arguments.size(); i++) {
            Expression argument = arguments.get(i);
            if (argument instanceof Cf.KeyValue) {
                if (name.equalsIgnoreCase(keyName(((Cf.KeyValue) argument).getKey()))) {
                    return ((Cf.KeyValue) argument).getValue();
                }
            } else if (i == position && !(argument instanceof J.Empty)) {
                return argument;
            }
        }
        return null;
    }

    private static @Nullable Expression key(Cf.StructLiteral struct, String name) {
        for (Expression entry : struct.getEntries()) {
            if (entry instanceof Cf.KeyValue && name.equalsIgnoreCase(keyName(((Cf.KeyValue) entry).getKey()))) {
                return ((Cf.KeyValue) entry).getValue();
            }
        }
        return null;
    }

    private static @Nullable String keyName(Expression key) {
        return key instanceof J.Identifier ? ((J.Identifier) key).getSimpleName() : string(key);
    }

    private static @Nullable String string(@Nullable Expression value) {
        return value instanceof J.Literal && ((J.Literal) value).getValue() instanceof String ?
                (String) ((J.Literal) value).getValue() : null;
    }
}
