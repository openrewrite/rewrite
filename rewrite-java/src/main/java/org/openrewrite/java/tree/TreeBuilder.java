/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.tree;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNullApi;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.visitor.refactor.Formatter;
import org.openrewrite.java.visitor.refactor.ShiftFormatRightVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.visitor.refactor.Formatter.enclosingIndent;

@NonNullApi
public class TreeBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TreeBuilder.class);

    private static final Pattern whitespacePrefixPattern = Pattern.compile("^\\s*");
    private static final Pattern whitespaceSuffixPattern = Pattern.compile("\\s*[^\\s]+(\\s*)");

    private TreeBuilder() {
    }

    public static <T extends TypeTree & Expression> T buildName(String fullyQualifiedName) {
        return buildName(fullyQualifiedName, Formatting.EMPTY);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static <T extends TypeTree & Expression> T buildName(String fullyQualifiedName, Formatting fmt) {
        String[] parts = fullyQualifiedName.split("\\.");

        String fullName = "";
        Expression expr = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                fullName = part;
                expr = J.Ident.build(randomId(), part, null, Formatting.EMPTY);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                var identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : Formatting.EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                whitespaceSuffix.matches();
                var partFmt = i == parts.length - 1 ? Formatting.EMPTY : format("", whitespaceSuffix.group(1));

                expr = new J.FieldAccess(randomId(),
                        expr,
                        J.Ident.build(randomId(), part.trim(), null, identFmt),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                JavaType.Class.build(fullName) :
                                null,
                        partFmt
                );
            }
        }

        //noinspection ConstantConditions
        return expr.withFormatting(fmt);
    }

    @SuppressWarnings("unchecked")
    public static <T extends J> List<T> buildSnippet(J.CompilationUnit containing, Cursor insertionScope, String snippet, Tree... arguments) {
        JavaParser parser = new JavaParser(emptyList(), Charset.defaultCharset(), true);

        // Turn this on in IntelliJ: Preferences > Editor > Code Style > Formatter Control
        // @formatter:off
        String source =
            "class CodeSnippet {\n" +
                new ListScopeVariables(insertionScope).visit(containing).stream()
                    .collect(joining(";\n  ", "  // variables visible in the insertion scope\n  ", ";\n")) + "\n" +
                "  // the contents of this block are the snippet\n" +
                "  {\n" +
                MessageFormatter.arrayFormat(StringUtils.trimIndent(snippet), stream(arguments).map(Tree::printTrimmed).toArray()).getMessage() + "\n" +
                "  }\n" +
            "}";
        // @formatter:on

        if (logger.isDebugEnabled()) {
            logger.debug("Building code snippet using synthetic class:");
            logger.debug(source);
        }

        J.CompilationUnit cu = parser.parse(source);
        List<J> statements = cu.getClasses().get(0).getBody().getStatements();
        J.Block<T> block = (J.Block<T>) statements.get(statements.size() - 1);

        Formatter formatter = new Formatter(cu);

        return block.getStatements().stream()
                .map(stat -> {
                    ShiftFormatRightVisitor shiftRight = new ShiftFormatRightVisitor(stat.getId(), enclosingIndent(insertionScope.getTree()) +
                            formatter.findIndent(enclosingIndent(insertionScope.getTree()), stat).getEnclosingIndent(), formatter.isIndentedWithSpaces());
                    return (T) shiftRight.visit(stat);
                })
                .collect(toList());
    }

    @RequiredArgsConstructor
    private static class ListScopeVariables extends JavaSourceVisitor<List<String>> {
        private final Cursor scope;

        @Override
        public boolean isCursored() {
            return true;
        }

        @Override
        public List<String> defaultTo(@Nullable Tree t) {
            return emptyList();
        }

        @Override
        public List<String> visitVariable(J.VariableDecls.NamedVar variable) {
            if (isInSameNameScope(scope)) {
                J.VariableDecls variableDecls = getCursor().getParentOrThrow().getTree();

                JavaType type = variableDecls.getTypeExpr() == null ? variable.getType() : variableDecls.getTypeExpr().getType();
                String typeName = "";
                if (type instanceof JavaType.Class) {
                    typeName = ((JavaType.Class) type).getFullyQualifiedName();
                } else if (type instanceof JavaType.ShallowClass) {
                    typeName = ((JavaType.ShallowClass) type).getFullyQualifiedName();
                } else if (type instanceof JavaType.GenericTypeVariable) {
                    typeName = ((JavaType.GenericTypeVariable) type).getFullyQualifiedName();
                } else if (type instanceof JavaType.Primitive) {
                    typeName = ((JavaType.Primitive) type).getKeyword();
                }

                return singletonList(typeName + " " + variable.getSimpleName());
            }

            return super.visitVariable(variable);
        }
    }
}
