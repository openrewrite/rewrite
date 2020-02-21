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
package com.netflix.rewrite.tree;

import com.netflix.rewrite.internal.lang.NonNullApi;
import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.Parser;
import com.netflix.rewrite.visitor.CursorAstVisitor;
import com.netflix.rewrite.visitor.refactor.Formatter;
import com.netflix.rewrite.visitor.refactor.ShiftFormatRightVisitor;
import com.netflix.rewrite.visitor.refactor.TransformVisitor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.netflix.rewrite.internal.StringUtils.trimIndent;
import static com.netflix.rewrite.tree.Formatting.format;
import static com.netflix.rewrite.tree.Tr.randomId;
import static com.netflix.rewrite.visitor.refactor.Formatter.enclosingIndent;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

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
                expr = Tr.Ident.build(randomId(), part, null, Formatting.EMPTY);
            } else {
                fullName += "." + part;

                Matcher whitespacePrefix = whitespacePrefixPattern.matcher(part);
                var identFmt = whitespacePrefix.matches() ? format(whitespacePrefix.group(0)) : Formatting.EMPTY;

                Matcher whitespaceSuffix = whitespaceSuffixPattern.matcher(part);
                whitespaceSuffix.matches();
                var partFmt = i == parts.length - 1 ? Formatting.EMPTY : format("", whitespaceSuffix.group(1));

                expr = new Tr.FieldAccess(randomId(),
                        expr,
                        Tr.Ident.build(randomId(), part.trim(), null, identFmt),
                        (Character.isUpperCase(part.charAt(0)) || i == parts.length - 1) ?
                                Type.Class.build(fullName) :
                                null,
                        partFmt
                );
            }
        }

        //noinspection ConstantConditions
        return expr.withFormatting(fmt);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Tree> List<T> buildSnippet(Tr.CompilationUnit containing, Cursor insertionScope, String snippet, Tree... arguments) {
        Parser parser = new Parser(Charset.defaultCharset(), true);

        // Turn this on in IntelliJ: Preferences > Editor > Code Style > Formatter Control
        // @formatter:off
        String source =
            "class CodeSnippet {\n" +
                new ListScopeVariables(insertionScope).visit(containing).stream()
                    .collect(joining(";\n  ", "  // variables visible in the insertion scope\n  ", ";\n")) + "\n" +
                "  // the contents of this block are the snippet\n" +
                "  {\n" +
                MessageFormatter.arrayFormat(trimIndent(snippet), stream(arguments).map(Tree::printTrimmed).toArray()).getMessage() + "\n" +
                "  }\n" +
            "}";
        // @formatter:on

        if (logger.isDebugEnabled()) {
            logger.debug("Building code snippet using synthetic class:");
            logger.debug(source);
        }

        Tr.CompilationUnit cu = parser.parse(source);
        List<Tree> statements = cu.getClasses().get(0).getBody().getStatements();
        Tr.Block<T> block = (Tr.Block<T>) statements.get(statements.size() - 1);

        Formatter formatter = new Formatter(cu);

        Tr.CompilationUnit formattedCu = (Tr.CompilationUnit) new TransformVisitor(block.getStatements().stream()
                .flatMap(stat -> {
                    ShiftFormatRightVisitor shiftRight = new ShiftFormatRightVisitor(stat.getId(), enclosingIndent(insertionScope.getTree()) +
                            formatter.findIndent(enclosingIndent(insertionScope.getTree()), stat).getEnclosingIndent(), formatter.isIndentedWithSpaces());
                    return shiftRight.visit(cu).stream();
                })
                .collect(toList())).visit(cu);

        List<Tree> formattedStatements = formattedCu.getClasses().get(0).getBody().getStatements();
        Tr.Block<T> formattedBlock = (Tr.Block<T>) formattedStatements.get(statements.size() - 1);
        return formattedBlock.getStatements();
    }

    @RequiredArgsConstructor
    private static class ListScopeVariables extends CursorAstVisitor<List<String>> {
        private final Cursor scope;

        @Override
        public List<String> defaultTo(@Nullable Tree t) {
            return emptyList();
        }

        @Override
        public List<String> visitVariable(Tr.VariableDecls.NamedVar variable) {
            if (getCursor().isInSameNameScope(scope)) {
                Tr.VariableDecls variableDecls = getCursor().getParentOrThrow().getTree();

                Type type = variableDecls.getTypeExpr() == null ? variable.getType() : variableDecls.getTypeExpr().getType();
                String typeName = "";
                if (type instanceof Type.Class) {
                    typeName = ((Type.Class) type).getFullyQualifiedName();
                } else if (type instanceof Type.ShallowClass) {
                    typeName = ((Type.ShallowClass) type).getFullyQualifiedName();
                } else if (type instanceof Type.GenericTypeVariable) {
                    typeName = ((Type.GenericTypeVariable) type).getFullyQualifiedName();
                } else if (type instanceof Type.Primitive) {
                    typeName = ((Type.Primitive) type).getKeyword();
                }

                return singletonList(typeName + " " + variable.getSimpleName());
            }

            return super.visitVariable(variable);
        }
    }
}
