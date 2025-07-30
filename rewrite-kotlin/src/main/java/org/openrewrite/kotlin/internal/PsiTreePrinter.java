/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.kotlin.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.kotlin.KtFakeSourceElement;
import org.jetbrains.kotlin.KtRealPsiSourceElement;
import org.jetbrains.kotlin.KtSourceElement;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.backend.FirMetadataSource;
import org.jetbrains.kotlin.fir.declarations.FirDeclaration;
import org.jetbrains.kotlin.fir.declarations.FirFile;
import org.jetbrains.kotlin.fir.declarations.FirProperty;
import org.jetbrains.kotlin.fir.expressions.*;
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference;
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag;
import org.jetbrains.kotlin.fir.types.*;
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor;
import org.jetbrains.kotlin.ir.IrElement;
import org.jetbrains.kotlin.ir.declarations.IrFile;
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner;
import org.jetbrains.kotlin.ir.declarations.MetadataSource;
import org.jetbrains.kotlin.ir.expressions.IrConst;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIrTypeMapping;
import org.openrewrite.kotlin.tree.K;

import java.util.*;

import static java.util.Collections.nCopies;
import static java.util.Collections.reverse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

@SuppressWarnings({"unused", "DuplicatedCode"})
public class PsiTreePrinter {
    private static final String TAB = "    ";
    private static final String ELEMENT_PREFIX = "\\---";
    private static final char BRANCH_CONTINUE_CHAR = '|';
    private static final char BRANCH_END_CHAR = '\\';
    private static final int CONTENT_MAX_LENGTH = 200;

    private static final String CONTINUE_PREFIX = "----";
    private static final String UNVISITED_PREFIX = "#";
    private static final KotlinIrTypeMapping irTypeMapping = new KotlinIrTypeMapping(new JavaTypeCache());

    // Set to true to print types and verify, otherwise just verify the parse to print idempotent.
    private final static boolean printTypes = true;

    private final List<StringBuilder> outputLines;

    protected PsiTreePrinter() {
        outputLines = new ArrayList<>();
    }

    public static String print(PsiElement psiElement) {
        return printPsiTree(psiElement);
    }

    public static String print(Parser.Input input) {
        return printIndexedSourceCode(input.getSource(new InMemoryExecutionContext()).readFully());
    }

    public static String print(@Nullable  IrFile file) {
        return printIrFile(file);
    }

    public static String print(Tree tree) {
        return printJTree(tree);
    }

    public static String print(@Nullable FirElement firElement) {
        if (firElement == null) {
            return "null";
        }
        return printFirTree(firElement);
    }

    public static String printPsiTreeSkeleton(PsiElement psiElement) {
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        StringBuilder sb = new StringBuilder();
        sb.append("------------").append("\n");
        sb.append("PSI Tree Skeleton").append("\n");
        Set<TextRange> covered =  new HashSet<>();
        collectCovered(psiElement, covered);
        treePrinter.printSkeletonNode(psiElement, 1);
        sb.append(java.lang.String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    public static String printPsiTree(PsiElement psiElement) {
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        StringBuilder sb = new StringBuilder();
        treePrinter.printNode(psiElement, 1);
        sb.append(java.lang.String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    @AllArgsConstructor
    @Data
    public static class TreePrinterContext {
        List<StringBuilder> lines;
        int depth;
    }

    public static @Nullable String printFirFile(FirFile file) {
        StringBuilder sb = new StringBuilder();
        List<StringBuilder> lines = new ArrayList<>();
        sb.append("------------").append("\n");
        sb.append("FirFile:").append("\n\n");

        TreePrinterContext context = new TreePrinterContext(lines, 1);
        new FirDefaultVisitor<Void, TreePrinterContext>() {

            @Override
            public @Nullable Void visitElement(FirElement firElement, TreePrinterContext ctx) {
                StringBuilder line = new StringBuilder();
                line.append(leftPadding(ctx.getDepth()))
                        .append(printFirElement(firElement));
                connectToLatestSibling(ctx.getDepth(), ctx.getLines());
                ctx.getLines().add(line);
                ctx.setDepth(ctx.getDepth() + 1);
                firElement.acceptChildren(this, ctx);

                if (firElement instanceof FirResolvedTypeRef) {
                    // not sure why this isn't taken care of by `FirResolvedTypeRefImpl#acceptChildren()`
                    FirTypeRef firTypeRef = ((FirResolvedTypeRef) firElement).getDelegatedTypeRef();
                    if (firTypeRef != null) {
                        firTypeRef.accept(this, ctx);
                    }
                }

                ctx.setDepth(ctx.getDepth() - 1);
                return null;
            }
        }.visitFile(file, context);
        sb.append(String.join("\n", lines));
        return sb.toString();
    }

    public static @Nullable String printFirTree(FirElement firElement) {
        StringBuilder sb = new StringBuilder();
        List<StringBuilder> lines = new ArrayList<>();
        TreePrinterContext context = new TreePrinterContext(lines, 1);
        new FirDefaultVisitor<Void, TreePrinterContext>() {

            @Override
            public @Nullable Void visitElement(FirElement fir, TreePrinterContext ctx) {
                StringBuilder line = new StringBuilder();
                line.append(leftPadding(ctx.getDepth()))
                        .append(printFirElement(fir));
                connectToLatestSibling(ctx.getDepth(), ctx.getLines());
                ctx.getLines().add(line);
                ctx.setDepth(ctx.getDepth() + 1);
                fir.acceptChildren(this, ctx);

                if (fir instanceof FirResolvedTypeRef) {
                    // not sure why this isn't taken care of by `FirResolvedTypeRefImpl#acceptChildren()`
                    FirTypeRef firTypeRef = ((FirResolvedTypeRef) fir).getDelegatedTypeRef();
                    if (firTypeRef != null) {
                        firTypeRef.accept(this, ctx);
                    }
                }

                ctx.setDepth(ctx.getDepth() - 1);
                return null;
            }
        }.visitElement(firElement, context);
        sb.append(String.join("\n", lines));
        return sb.toString();
    }

    public static class IrPrinter {
        public void printElement(IrElement element, PsiTreePrinter.TreePrinterContext ctx) {
            StringBuilder line = new StringBuilder();
            line.append(leftPadding(ctx.getDepth()))
                    .append(printIrElement(element));
            connectToLatestSibling(ctx.getDepth(), ctx.getLines());
            ctx.getLines().add(line);
        }
    }

    public static String printIrFile(@Nullable IrFile file) {
        if (file == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        List<StringBuilder> lines = new ArrayList<>();
        sb.append("------------").append("\n");
        sb.append("IrFile:").append("\n\n");

        TreePrinterContext context = new TreePrinterContext(lines, 1);
        new IrTreePrinterVisitor(new IrPrinter()).visitFile(file, context);
        sb.append(java.lang.String.join("\n", lines));
        return sb.toString();
    }

    /**
     * print J tree with all types
     */
    static class TreeVisitingPrinter extends TreeVisitor<Tree, ExecutionContext> {
        private List<Object> lastCursorStack;
        private final List<StringBuilder> outputLines;
        private final boolean skipUnvisitedElement;
        private final boolean printContent;

        public TreeVisitingPrinter(boolean skipUnvisitedElement, boolean printContent) {
            lastCursorStack = new ArrayList<>();
            outputLines = new ArrayList<>();
            this.skipUnvisitedElement = skipUnvisitedElement;
            this.printContent = printContent;
        }

        public String print() {
            return java.lang.String.join("\n", outputLines);
        }

        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (tree == null) {
                return super.visit((Tree) null, ctx);
            }

            Cursor cursor = this.getCursor();
            List<Object> cursorStack =
                    stream(Spliterators.spliteratorUnknownSize(cursor.getPath(), 0), false)
                            .collect(toList());
            reverse(cursorStack);
            int depth = cursorStack.size();

            // Compare lastCursorStack vs cursorStack, find the fork and print the diff
            int diffPos = -1;
            for (int i = 0; i < cursorStack.size(); i++) {
                if (i >= lastCursorStack.size() || cursorStack.get(i) != lastCursorStack.get(i)) {
                    diffPos = i;
                    break;
                }
            }

            StringBuilder line = new StringBuilder();

            // print cursor stack diff
            if (diffPos >= 0) {
                for (int i = diffPos; i < cursorStack.size(); i++) {
                    Object element = cursorStack.get(i);
                    if (skipUnvisitedElement) {
                        // skip unvisited elements, just print indents in the line
                        if (i == diffPos) {
                            line.append(leftPadding(i));
                            connectToLatestSibling(i, outputLines);
                        } else {
                            line.append(CONTINUE_PREFIX);
                        }
                    } else {
                        // print each unvisited element to a line
                        connectToLatestSibling(i, outputLines);
                        StringBuilder newLine = new StringBuilder()
                                .append(leftPadding(i))
                                .append(UNVISITED_PREFIX)
                                .append(element instanceof String ? element : element.getClass().getSimpleName());

                        if (element instanceof JRightPadded) {
                            JRightPadded rp = (JRightPadded) element;
                            newLine.append(" | ");
                            newLine.append(" after = ").append(printSpace(rp.getAfter()));
                        }

                        if (element instanceof JLeftPadded) {
                            JLeftPadded lp = (JLeftPadded) element;
                            newLine.append(" | ");
                            newLine.append(" before = ").append(printSpace(lp.getBefore()));
                        }

                        outputLines.add(newLine);
                    }
                }
            }

            // print current visiting element
            String typeName = tree instanceof J ?
                    tree.getClass().getCanonicalName().substring(tree.getClass().getPackage().getName().length() + 1) :
                    tree.getClass().getCanonicalName();

            if (skipUnvisitedElement) {
                boolean leftPadded = diffPos >= 0;
                if (leftPadded) {
                    line.append(CONTINUE_PREFIX);
                } else {
                    connectToLatestSibling(depth, outputLines);
                    line.append(leftPadding(depth));
                }
                line.append(typeName);
            } else {
                connectToLatestSibling(depth, outputLines);
                line.append(leftPadding(depth)).append(typeName);
            }

            String type = printType(tree);
            if (printTypes && !type.isEmpty()) {
                line.append(" | TYPE = ").append(type);
            }

            if (printContent) {
                String content = truncate(printTreeElement(tree));
                if (!content.isEmpty()) {
                    line.append(" | \"").append(content).append("\"");
                }
            }

            outputLines.add(line);

            cursorStack.add(tree);
            lastCursorStack = cursorStack;
            return super.visit(tree, ctx);
        }
    }

    private static String printType(Tree tree) {
        StringBuilder sb = new StringBuilder();
        if (tree instanceof TypedTree) {
            JavaType type = ((TypedTree) tree).getType();
            if (type != null && !(type instanceof JavaType.Unknown)) {
                sb.append(type);
            }
        }

        if (tree instanceof J.MethodInvocation) {
            J.MethodInvocation m = (J.MethodInvocation) tree;
            if (m.getMethodType() != null) {
                sb.append(" MethodType = ").append(m.getMethodType());
            }
        }

        if (tree instanceof J.MethodDeclaration) {
            J.MethodDeclaration m = (J.MethodDeclaration) tree;
            if (m.getMethodType() != null) {
                sb.append(" MethodType = ").append(m.getMethodType());
            }
        }

        if (tree instanceof J.VariableDeclarations.NamedVariable) {
            J.VariableDeclarations.NamedVariable v = (J.VariableDeclarations.NamedVariable) tree;
            if (v.getVariableType() != null) {
                sb.append(" VariableType = ").append(v.getVariableType());
            }
        }

        if (tree instanceof J.Identifier) {
            J.Identifier id = (J.Identifier) tree;
            if (id.getFieldType() != null) {
                sb.append(" FieldType = ").append(id.getFieldType());
            }
        }

        return sb.toString();
    }

    private static String printTreeElement(Tree tree) {
        // skip some specific types printed in the output to make the output looks clean
        if (tree instanceof J.CompilationUnit ||
            tree instanceof J.ClassDeclaration ||
            tree instanceof J.Block ||
            tree instanceof J.Empty ||
            tree instanceof J.Try ||
            tree instanceof J.Try.Catch ||
            tree instanceof J.ForLoop ||
            tree instanceof J.WhileLoop ||
            tree instanceof J.DoWhileLoop ||
            tree instanceof J.Lambda ||
            tree instanceof J.Lambda.Parameters ||
            tree instanceof J.If ||
            tree instanceof J.If.Else ||
            tree instanceof J.EnumValueSet ||
            tree instanceof J.ParenthesizedTypeTree ||
            tree instanceof J.TypeParameter ||
            tree instanceof K.ClassDeclaration ||
            tree instanceof K.CompilationUnit ||
            tree instanceof K.SpreadArgument ||
            tree instanceof K.StatementExpression ||
            tree instanceof K.StringTemplate ||
            tree instanceof K.StringTemplate.Expression ||
            tree instanceof K.ExpressionStatement ||
            tree instanceof K.FunctionType ||
            tree instanceof K.ListLiteral ||
            tree instanceof K.When || tree instanceof J.Package ||
            tree instanceof J.ForEachLoop
        ) {
            return "";
        }

        if (tree instanceof J.Literal) {
            String s = ((J.Literal) tree).getValueSource();
            return s != null ? s : "";
        }

        String[] lines = tree.toString().split("\n");
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            output.append(lines[i].trim());
            if (i < lines.length - 1) {
                output.append(" ");
            }
        }
        return output.toString();
    }

    private static String printSpace(Space space) {
        String sb = " whitespace=\"" +
                    space.getWhitespace() + "\"" +
                    " comments=\"" +
                    space.getComments().stream().map(c -> c.printComment(new Cursor(null, "root"))).collect(joining(",")) +
                    "\"";
        return sb.replace("\n", "\\s\n");
    }

    public static String printJTree(Tree tree) {
        TreeVisitingPrinter visitor = new TreeVisitingPrinter(true, true);
        visitor.visit(tree, new InMemoryExecutionContext());
        return visitor.print();
    }


    public static String printIrElement(IrElement element) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(element.getStartOffset()).append(",").append(element.getEndOffset())
                .append(") | ").append(element.getClass().getSimpleName());

        if (element instanceof IrMetadataSourceOwner) {
            String typeFromIr = getType(element);
            if (!typeFromIr.isEmpty()) {
                sb.append(" | IrType = ").append(typeFromIr);
            }

            IrMetadataSourceOwner irMetadataSourceOwner = (IrMetadataSourceOwner) element;
            MetadataSource metadata = irMetadataSourceOwner.getMetadata();
            if (metadata != null) {
                if (metadata instanceof FirMetadataSource) {
                    FirMetadataSource firMetadataSource = (FirMetadataSource) irMetadataSourceOwner.getMetadata();
                    FirDeclaration firDeclaration = firMetadataSource.getFir();
                    if (firDeclaration != null) {
                        sb.append(" | ").append(printFirElement(firDeclaration));
                    }
                } else {
                    throw new UnsupportedOperationException("TODO");
                }
            }
        } else if (element instanceof IrConst) {
            IrConst irConst = (IrConst) element;
            sb.append(" | ").append(irConst.getValue());
        }
        return sb.toString();
    }

    public static String getType(IrElement element) {
        return irTypeMapping.type(element).toString();
    }

    public static String printFirElement(FirElement firElement) {
        StringBuilder sb = new StringBuilder();
        sb.append(firElement.getClass().getSimpleName());

        if (firElement.getSource() != null) {
            KtSourceElement source = firElement.getSource();
            sb.append(" | ");

            if (source instanceof KtRealPsiSourceElement) {
                sb.append("Real ");
            } else if (source instanceof KtFakeSourceElement) {
                sb.append("Fake ");
            } else {
                sb.append(source.getClass().getSimpleName());
            }

            sb.append("PSI(")
                    .append("[").append(source.getStartOffset())
                    .append(",")
                    .append(source.getEndOffset())
                    .append("]")
                    .append(" ")
                    .append(source.getElementType())
                    .append(")");
        }

        String firValue = firElementToString(firElement);
        if (firValue != null && !firValue.isEmpty()) {
            sb.append(" | ").append(firValue);
        }

        return sb.toString();
    }

    private static String printConeKotlinType(ConeTypeProjection coneKotlinType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type:[").append(coneKotlinType);
        if (coneKotlinType instanceof ConeClassLikeType) {
            ConeClassLikeType coneClassLikeType = (ConeClassLikeType) coneKotlinType;
            ConeClassLikeLookupTag coneClassLikeLookupTag = coneClassLikeType.getLookupTag();
            ClassId classId = coneClassLikeLookupTag.getClassId();

            FqName packageFqName = classId.getPackageFqName();
            sb.append(" packageFqName: ").append(packageFqName);

            FqName className = classId.getRelativeClassName();
            sb.append(" className: ").append(className);

            coneClassLikeLookupTag.getName();

            ConeTypeProjection[] typeArguments = coneClassLikeType.getTypeArguments();
            if (typeArguments != null && typeArguments.length > 0) {
                sb.append(" typeArgument: ");
                for (ConeTypeProjection typeArgument : typeArguments) {
                    sb.append(printConeKotlinType(typeArgument));
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static @Nullable String firElementToString(FirElement firElement) {
        if (firElement instanceof FirFile) {
            return ((FirFile) firElement).getName();
        } else if (firElement instanceof FirProperty) {
            return ((FirProperty) firElement).getName().toString();
        } else if (firElement instanceof FirResolvedTypeRef) {
            FirResolvedTypeRef resolvedTypeRef = (FirResolvedTypeRef) firElement;
            ConeKotlinType coneKotlinType = resolvedTypeRef.getType();
            return printConeKotlinType(coneKotlinType);
        } else if (firElement instanceof FirResolvedNamedReference) {
            return ((FirResolvedNamedReference) firElement).getName().toString();
        } else if (firElement instanceof FirResolvedQualifier) {
            FirResolvedQualifier qualifier = (FirResolvedQualifier) firElement;
            FqName fqName = qualifier.getRelativeClassFqName();
            return fqName != null ? " RelativeClassFqName: " + fqName : "";
        } else if (firElement instanceof FirFunctionCall) {
            FirFunctionCall functionCall = (FirFunctionCall) firElement;
            if (functionCall.getExplicitReceiver() != null) {
                return firElementToString(functionCall.getExplicitReceiver()) + "." +
                        ((FirFunctionCall) firElement).getCalleeReference().getName() + "(" + firElementToString(((FirFunctionCall) firElement).getArgumentList()) + ")";
            } else {
                return ((FirFunctionCall) firElement).getCalleeReference().getName() + "(" + firElementToString(((FirFunctionCall) firElement).getArgumentList()) + ")";
            }
        } else if (firElement instanceof FirArgumentList) {
            List<FirExpression> args = ((FirArgumentList) firElement).getArguments();
            if (!args.isEmpty()) {
                boolean first = true;
                StringBuilder sb = new StringBuilder();
                for (FirExpression arg : args) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(firElementToString(arg));
                    first = false;
                }
                return sb.toString();
            }
        } else if (firElement instanceof FirConstExpression) {
            Object value = ((FirConstExpression<?>) firElement).getValue();
            return value != null ? value.toString() : null;
            // return ((FirConstExpression<?>) firElement).getKind().toString();
        } else if (firElement instanceof FirWhenBranch) {
            FirWhenBranch whenBranch = (FirWhenBranch) firElement;
            return "when(" + firElementToString(whenBranch.getCondition()) + ")" + " -> " + firElementToString(whenBranch.getResult());
        } else if ("FirElseIfTrueCondition".equals(firElement.getClass().getSimpleName())) {
            return PsiElementAssociations.Companion.printElement(firElement);
        } else if ("FirSingleExpressionBlock".equals(firElement.getClass().getSimpleName())) {
            return PsiElementAssociations.Companion.printElement(firElement);
        }
        return "";
    }

    public static String printIndexedSourceCode(String sourceCode) {
        int count = 0;
        String[] lines = sourceCode.split("\n");
        StringBuilder sb = new StringBuilder();
        sb.append("------------").append("\n");
        sb.append("Source code with index:").append("\n\n");
        Queue<Integer> digits = new ArrayDeque<>();

        for (String line : lines) {
            StringBuilder spacesSb = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                if (count % 10 == 0) {
                    String numStr = Integer.toString(count);
                    for (int j = 0; j < numStr.length(); j++) {
                        char c = numStr.charAt(j);
                        int digit = Character.getNumericValue(c);
                        digits.add(digit);
                    }
                }

                if (!digits.isEmpty()) {
                    spacesSb.append(digits.poll()) ;
                } else {
                    spacesSb.append(" ");
                }

                count++;
            }

            sb.append(line)
                .append("\n")
                .append(spacesSb)
                .append("\n");
            count++;
        }
        return sb.toString();
    }

    private String toString(PsiElement psiElement) {
        return psiElement.getTextRange() +
                " | " +
                psiElement.getNode().getElementType() +
                " | " +
                psiElement.getClass().getSimpleName() +
                " | Text: \"" +
                truncate(psiElement.getText()).replace("\n", "\\n").replace("\r", "\\r") +
                "\"";
    }

    private void printSkeletonNode(PsiElement psiElement, int depth) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth))
            .append(toString(psiElement));
        connectToLatestSibling(depth);
        outputLines.add(line);

        for (PsiElement childNode : psiElement.getChildren()) {
            printSkeletonNode(childNode, depth + 1);
        }
    }

    private void printNode(PsiElement psiElement, int depth) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth))
            .append(toString(psiElement));
        connectToLatestSibling(depth);
        outputLines.add(line);

        PsiUtilsKt.getAllChildren(psiElement);
        Iterator<PsiElement> iterator = PsiUtilsKt.getAllChildren(psiElement).iterator();
        while (iterator.hasNext()) {
            PsiElement it = iterator.next();
            printNode(it, depth + 1);
        }
    }

    private static void collectCovered(PsiElement psiElement, Set<TextRange> covered) {
        covered.add(psiElement.getTextRange());
        for (PsiElement childNode : psiElement.getChildren()) {
            collectCovered(childNode, covered);
        }
    }

    /**
     * print left padding for a line
     * @param depth, depth starts from 0 (the root)
     */
    private static String leftPadding(int depth) {
        StringBuilder sb = new StringBuilder();
        int tabCount = depth - 1;
        if (tabCount > 0) {
            sb.append(java.lang.String.join("", nCopies(tabCount, TAB)));
        }
        // only root has not prefix
        if (depth > 0) {
            sb.append(ELEMENT_PREFIX);
        }
        return sb.toString();
    }

    /**
     * Print a vertical line that connects the current element to the latest sibling.
     * @param depth current element depth
     */
    private void connectToLatestSibling(int depth) {
        if (depth <= 1) {
            return;
        }

        int pos = (depth - 1) * TAB.length();
        for (int i = outputLines.size() - 1; i > 0; i--) {
            StringBuilder line = outputLines.get(i);
            if (pos >= line.length()) {
                break;
            }

            if (line.charAt(pos) != ' ') {
                if (line.charAt(pos) == BRANCH_END_CHAR) {
                    line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
                }
                break;
            }
            line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
        }
    }

    /**
     * Print a vertical line that connects the current element to the latest sibling.
     * @param depth current element depth
     */
    private static void connectToLatestSibling(int depth, List<StringBuilder> lines) {
        if (depth <= 1) {
            return;
        }

        int pos = (depth - 1) * TAB.length();
        for (int i = lines.size() - 1; i > 0; i--) {
            StringBuilder line = lines.get(i);
            if (pos >= line.length()) {
                break;
            }

            if (line.charAt(pos) != ' ') {
                if (line.charAt(pos) == BRANCH_END_CHAR) {
                    line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
                }
                break;
            }
            line.setCharAt(pos, BRANCH_CONTINUE_CHAR);
        }
    }

    private static String truncate(String content) {
        if (content.length() > CONTENT_MAX_LENGTH) {
            return content.substring(0, CONTENT_MAX_LENGTH - 3) + "...";
        }
        return content;
    }
}
