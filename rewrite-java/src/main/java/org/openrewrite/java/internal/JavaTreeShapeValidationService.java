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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaTreeShapeValidationService implements TreeShapeValidationService {
    private static final Pattern PARSER_VERSION = Pattern.compile("\\.Java(\\d+)Parser\\b");

    @Override
    public @Nullable Object value(Tree tree) {
        if (tree instanceof J.Identifier) {
            return "the name `" + ((J.Identifier) tree).getSimpleName() + "`";
        } else if (tree instanceof J.Literal) {
            J.Literal literal = (J.Literal) tree;
            Object value = literal.getValue();
            JavaType.Primitive type = literal.getType();
            return (value == null ? "the value null" :
                    "the " + value.getClass().getSimpleName() + " value " + quoted(value)) +
                   " of type " + (type == null ? "null" : type.getKeyword());
        } else if (tree instanceof J.Modifier) {
            J.Modifier modifier = (J.Modifier) tree;
            return "the modifier " + (modifier.getType() == J.Modifier.Type.LanguageExtension ?
                    modifier.getKeyword() : modifier.getType());
        } else if (tree instanceof J.Primitive) {
            return "the type " + ((J.Primitive) tree).getType().getKeyword();
        } else if (tree instanceof J.Binary) {
            return "the operator " + ((J.Binary) tree).getOperator();
        } else if (tree instanceof J.Unary) {
            return "the operator " + ((J.Unary) tree).getOperator();
        } else if (tree instanceof J.AssignmentOperation) {
            return "the operator " + ((J.AssignmentOperation) tree).getOperator();
        }
        return null;
    }

    private static String quoted(Object value) {
        String printed = String.valueOf(value).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t");
        if (printed.length() > 40) {
            printed = printed.substring(0, 40) + "...";
        }
        return value instanceof Character ? "'" + printed + "'" : value instanceof String ? '"' + printed + '"' :
                printed;
    }

    @Override
    public boolean skip(Tree tree) {
        // the parser adds empty elements where recipes often leave none, and documentation comments aren't code
        return tree instanceof J.Empty || tree instanceof Javadoc;
    }

    @Override
    public boolean reads(Parser.Builder parser, SourceFile sourceFile) {
        // a test can mark its sources with a newer Java than the JDK parser it parses them with
        Matcher parserVersion = PARSER_VERSION.matcher(parser.getClass().getName());
        JavaVersion version = sourceFile.getMarkers().findFirst(JavaVersion.class).orElse(null);
        return version == null || !parserVersion.find() ||
               version.getMajorVersion() <= Integer.parseInt(parserVersion.group(1));
    }

    @Override
    public boolean equivalent(Tree tree, Tree parsed) {
        // a type annotation can sit on a declaration, on its type or on a part of the type, and recipes place it
        // where the parser doesn't
        return tree instanceof J && parsed instanceof J && (hasAnnotations(tree) || hasAnnotations(parsed)) &&
               kindsWithoutAnnotations(tree).equals(kindsWithoutAnnotations(parsed));
    }

    private static boolean hasAnnotations(Tree tree) {
        AtomicBoolean found = new AtomicBoolean();
        new TreeVisitor<Tree, AtomicBoolean>() {
            @Override
            public Tree preVisit(Tree t, AtomicBoolean f) {
                if (t instanceof J.Annotation || t instanceof J.AnnotatedType) {
                    f.set(true);
                    stopAfterPreVisit();
                }
                return t;
            }
        }.visit(tree, found);
        return found.get();
    }

    /**
     * The kinds of node in a tree in the order they are visited, without annotations and what holds them.
     */
    private static List<Class<?>> kindsWithoutAnnotations(Tree tree) {
        List<Class<?>> kinds = new ArrayList<>();
        new TreeVisitor<Tree, List<Class<?>>>() {
            @Override
            public Tree preVisit(Tree t, List<Class<?>> k) {
                if (t instanceof J.Annotation || t instanceof J.Empty || t instanceof Javadoc) {
                    stopAfterPreVisit();
                } else if (!(t instanceof J.AnnotatedType)) {
                    k.add(t.getClass());
                }
                return t;
            }
        }.visit(tree, kinds);
        return kinds;
    }
}
