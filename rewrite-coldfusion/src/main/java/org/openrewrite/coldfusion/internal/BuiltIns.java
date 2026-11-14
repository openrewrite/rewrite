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
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CFML's built-in functions, which are the methods of one type, {@code cfml}, as {@code cfml.arrayLen}, and the
 * types of CFML's own values that have no Java counterpart: {@code cfml.Array}, {@code cfml.Struct} and
 * {@code cfml.Query}. A member function, as {@code items.append(x)}, is the built-in function it aliases.
 */
final class BuiltIns {
    static final JavaType.Class CFML = new JavaType.Class(null, Flag.Public.getBitMask(), "cfml",
            JavaType.FullyQualified.Kind.Class, null, null, null, null, null, null, null);
    static final JavaType.Class ARRAY = valueType("cfml.Array");
    static final JavaType.Class STRUCT = valueType("cfml.Struct");
    static final JavaType.Class QUERY = valueType("cfml.Query");

    /**
     * Each function by its name in lower case, since CFML's names are case-insensitive.
     */
    private static final Map<String, JavaType.Method> FUNCTIONS = new HashMap<>();

    /**
     * Each member function by the type of value it is a member of and its name in lower case, as
     * {@code array.append}, which is the built-in function it aliases.
     */
    private static final Map<String, JavaType.Method> MEMBERS = new HashMap<>();

    static {
        long flags = Flag.Public.getBitMask() | Flag.Static.getBitMask();
        try (InputStream in = BuiltIns.class.getResourceAsStream("functions.tsv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(in), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\t");
                List<String> parameterNames = new ArrayList<>(columns.length - 3);
                List<JavaType> parameterTypes = new ArrayList<>(columns.length - 3);
                for (int i = 3; i < columns.length; i++) {
                    int colon = columns[i].indexOf(':');
                    parameterNames.add(columns[i].substring(0, colon));
                    parameterTypes.add(javaType(columns[i].substring(colon + 1)));
                }
                JavaType.Method function = new JavaType.Method(null, flags, CFML, columns[0], javaType(columns[1]),
                        parameterNames, parameterTypes, null, null, null, null);
                FUNCTIONS.put(columns[0].toLowerCase(Locale.ROOT), function);
                if (!"-".equals(columns[2])) {
                    for (String member : columns[2].split(",")) {
                        boolean chaining = member.endsWith("!");
                        String spec = member.substring(1, chaining ? member.length() - 1 : member.length());
                        String receiver = spec.substring(0, spec.indexOf('.'));
                        JavaType receiverType = javaTypeOrNull(receiver);
                        MEMBERS.putIfAbsent(normalized(receiver) + "." + spec.substring(spec.indexOf('.') + 1).toLowerCase(Locale.ROOT),
                                chaining && receiverType != null ? returning(function, receiverType) : function);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        CFML.unsafeSet(null, null, null, null, null, null, new ArrayList<>(FUNCTIONS.values()));
    }

    private static JavaType.Class valueType(String fullyQualifiedName) {
        return new JavaType.Class(null, Flag.Public.getBitMask(), fullyQualifiedName, JavaType.FullyQualified.Kind.Class,
                null, null, null, null, null, null, null);
    }

    private static JavaType.Method returning(JavaType.Method type, JavaType returnType) {
        return new JavaType.Method(null, type.getFlagsBitMap(), type.getDeclaringType(), type.getName(), returnType,
                type.getParameterNames(), type.getParameterTypes(), null, null, null, null);
    }

    /**
     * A receiver's CFML type name as the member table keys it: a number's is {@code numeric}, a date's
     * {@code datetime}.
     */
    private static String normalized(String type) {
        String lower = type.toLowerCase(Locale.ROOT);
        return "number".equals(lower) ? "numeric" : "date".equals(lower) ? "datetime" : lower;
    }

    /**
     * The built-in function a member function on a value aliases, as {@code arrayAppend} for {@code items.append(x)}
     * on an array, or null when the value's type has no member by that name.
     */
    static JavaType.@Nullable Method member(@Nullable JavaType receiver, String name) {
        String type = receiver == ARRAY ? "array" : receiver == STRUCT ? "struct" : receiver == QUERY ? "query" :
                receiver == JavaType.Primitive.String ? "string" : receiver == JavaType.Primitive.Double ? "numeric" : null;
        if (type == null) {
            return null;
        }
        JavaType.Method member = MEMBERS.get(type + "." + name.toLowerCase(Locale.ROOT));
        return member != null ? member : MEMBERS.get("any." + name.toLowerCase(Locale.ROOT));
    }

    static boolean isValueType(@Nullable JavaType type) {
        return type == ARRAY || type == STRUCT || type == QUERY || type == JavaType.Primitive.String;
    }

    private BuiltIns() {
    }

    static JavaType.@Nullable Method function(String name) {
        return FUNCTIONS.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * The Java type of a CFML type that has one, as {@code string} has {@code java.lang.String}, or an unknown type.
     */
    static JavaType javaType(String name) {
        JavaType type = javaTypeOrNull(name);
        return type == null ? JavaType.Unknown.getInstance() : type;
    }

    static @Nullable JavaType javaTypeOrNull(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "string":
                return JavaType.Primitive.String;
            case "numeric":
            case "number":
                return JavaType.Primitive.Double;
            case "boolean":
                return JavaType.Primitive.Boolean;
            case "void":
                return JavaType.Primitive.Void;
            case "array":
                return ARRAY;
            case "struct":
                return STRUCT;
            case "query":
                return QUERY;
            default:
                return null;
        }
    }

    /**
     * Whether a type name is one of CFML's own, which never names a component.
     */
    static boolean isCfmlType(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "any":
            case "array":
            case "binary":
            case "boolean":
            case "component":
            case "date":
            case "datetime":
            case "function":
            case "guid":
            case "numeric":
            case "number":
            case "query":
            case "string":
            case "struct":
            case "uuid":
            case "variablename":
            case "void":
            case "xml":
                return true;
            default:
                return false;
        }
    }
}
