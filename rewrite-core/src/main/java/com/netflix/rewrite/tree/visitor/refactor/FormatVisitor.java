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
package com.netflix.rewrite.tree.visitor.refactor;

import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.netflix.rewrite.tree.Formatting.INFER;
import static com.netflix.rewrite.tree.Formatting.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.range;

/**
 * Only formatting for nodes that we can ADD so far
 * is supported.
 * <p>
 * Emits a side-effect of mutating formatting on tree nodes as necessary
 */
public class FormatVisitor extends RefactorVisitor<Tree> {
    private FindIndentVisitor wholeSourceIndentVisitor = new FindIndentVisitor(0);

    @Override
    protected String getRuleName() {
        return "format";
    }

    @Override
    public List<AstTransform<Tree>> visitCompilationUnit(Tr.CompilationUnit cu) {
        wholeSourceIndentVisitor.reset();
        wholeSourceIndentVisitor.visit(cu);

        List<AstTransform<Tree>> changes = new ArrayList<>();

        List<Tr.Import> imports = cu.getImports();
        for (int i = 0; i < imports.size(); i++) {
            Tr.Import im = imports.get(i);
            Tr.Import firstImport = imports.get(0);
            Tr.Import lastImport = imports.get(cu.getImports().size() - 1);

            if (im.getFormatting() == Formatting.INFER) {
                if (im == lastImport) {
                    cu.getClasses().stream().findAny().ifPresent(clazz ->
                            changes.addAll(blankLinesBefore(clazz, 2)));
                    if (cu.getImports().size() > 1) {
                        changes.addAll(blankLinesBefore(im, 1));
                    }
                }

                if (im == firstImport) {
                    if (cu.getPackageDecl() != null) {
                        changes.addAll(blankLinesBefore(im, 2));
                    }

                    // a previous first import will likely have a multiple line spacing prefix
                    if (imports.size() > 1 && imports.get(1).getFormatting() != Formatting.INFER) {
                        changes.addAll(blankLinesBefore(imports.get(1), 1));
                    }
                }

                if (im != firstImport && im != lastImport) {
                    changes.addAll(blankLinesBefore(im, 1));
                    changes.addAll(blankLinesBefore(imports.get(i + 1), 1));
                }
            }
        }

        List<AstTransform<Tree>> all = new ArrayList<>(super.visitCompilationUnit(cu));
        all.addAll(changes);
        return all;
    }

    @Override
    public List<AstTransform<Tree>> visitBlock(Tr.Block<Tree> block) {
        List<AstTransform<Tree>> changes = super.visitBlock(block);

        if (block.getStatements().stream().anyMatch(t -> t.getFormatting() == INFER)) {
            Function<Tree, Tree> indentTransform = indentStatements(block.getStatements(), blockEnclosingIndent(block));
            changes.addAll(
                    block.getStatements().stream()
                            .filter(t -> t.getFormatting() == INFER)
                            .flatMap(t -> transform(t, indentTransform).stream())
                            .collect(toList())
            );
        }
        return changes;
    }

    @Override
    public List<AstTransform<Tree>> visitMultiVariable(Tr.VariableDecls multiVariable) {
        if (multiVariable.getFormatting() == Formatting.INFER) {
            // we make a simplifying assumption here that inferred variable
            // declaration formatting comes only from added fields
            @SuppressWarnings("ConstantConditions") Tr.Block<?> classBody = (Tr.Block<?>) getCursor().getParent().getTree();
            List<AstTransform<Tree>> all = new ArrayList<>(super.visitMultiVariable(multiVariable));
            all.addAll(transform(indentStatements(classBody.getStatements(), blockEnclosingIndent(classBody))));
            return all;
        }

        return super.visitMultiVariable(multiVariable);
    }

    private Function<Tree, Tree> indentStatements(Iterable<? extends Tree> tree, int enclosingIndent) {
        final var findIndentVisitor = new FindIndentVisitor(enclosingIndent);
        tree.forEach(findIndentVisitor::visit);

        var indentToUse = findIndentVisitor.getMostCommonIndent() > 0 ?
                findIndentVisitor.getMostCommonIndent() : wholeSourceIndentVisitor.getMostCommonIndent();
        var indentIsSpaces = findIndentVisitor.getTotalLines() > 0 ? findIndentVisitor.isIndentedWithSpaces() :
                wholeSourceIndentVisitor.isIndentedWithSpaces();

        return t -> indentToUse > 0 ?
                t.withFormatting(format(range(0, indentToUse + enclosingIndent)
                        .mapToObj(i -> indentIsSpaces ? " " : "\t")
                        .collect(joining("", "\n", "")))) :
                t.withFormatting(format("\n    ")); // default formatting of 4 spaces
    }

    private List<AstTransform<Tree>> blankLinesBefore(Tree t, int n) {
        if (t == null) {
            return emptyList();
        }
        if (t.getFormatting() instanceof Formatting.Reified) {
            var prefix = ((Formatting.Reified) t.getFormatting()).getPrefix();

            // add blank lines if necessary
            var addLines = blankLines(Math.max(0, n - (int) prefix.chars().takeWhile(p -> p == '\n').count()));
            var modifiedPrefix = addLines + prefix;

            // remove extra blank lines if necessary
            modifiedPrefix = modifiedPrefix.substring((int) modifiedPrefix.chars().asDoubleStream().takeWhile(p -> p == '\n').count() - n);

            if (!modifiedPrefix.equals(prefix)) {
                final String modifiedPrefixFinal = modifiedPrefix;
                return transform(t, t2 -> t2.withFormatting(t.getFormatting().withPrefix(modifiedPrefixFinal)));
            } else {
                return emptyList();
            }
        } else { // INFER, NONE
            return transform(t, t2 -> t2.withFormatting(format(blankLines(n))));
        }
    }

    private int blockEnclosingIndent(Tr.Block<?> block) {
        return (int) block.getEndOfBlockSuffix().chars().dropWhile(c -> c == '\n' || c == '\r').count();
    }

    private String blankLines(Integer n) {
        return range(0, n).mapToObj(i -> "\n").collect(joining(""));
    }
}
