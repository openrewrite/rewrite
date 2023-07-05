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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PsiTreePrinter {
    private static final String TAB = "    ";
    private static final String ELEMENT_PREFIX = "\\----";
    private static final char BRANCH_CONTINUE_CHAR = '|';
    private static final char BRANCH_END_CHAR = '\\';
    private static final int CONTENT_MAX_LENGTH = 200;

    private final List<StringBuilder> outputLines;

    protected PsiTreePrinter() {
        outputLines = new ArrayList<>();
    }

    public static String printPsiTree(PsiFile psiFile) {
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        StringBuilder sb = new StringBuilder();
        sb.append("Raw PSI AST").append("\n");
        treePrinter.printNode(psiFile, 1);
        sb.append(String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    public static String printPsiTree(PsiTree psiTree) {
        StringBuilder sb = new StringBuilder();

        // 0. Source code
        sb.append("------------").append("\n");
        sb.append("Source code with index").append("\n");
        sb.append(printIndexedSourceCode(psiTree.getSource())).append("\n");

        // 1. print Tokens
        sb.append("------------").append("\n");
        sb.append("PSI Tokens").append("\n");
        for (int i = 0; i < psiTree.getTokens().size(); i++) {
            PsiToken t = psiTree.getTokens().get(i);
            sb.append(i).append(": ").append(t).append("\n");
        }

        // 2. print AST
        PsiTreePrinter treePrinter = new PsiTreePrinter();
        sb.append("------------").append("\n");
        sb.append("Parsed Full PSI AST").append("\n");
        treePrinter.printNode(psiTree.getRoot(), 1);
        sb.append(String.join("\n", treePrinter.outputLines));
        return sb.toString();
    }

    private void printNode(PsiElement psiElement, int depth) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth));
        line.append(" ")
            .append(psiElement.getTextRange())
            .append(" | Type:")
            .append(psiElement.getNode().getElementType())
            .append(" | Text: \"")
            .append(truncate(psiElement.getText()).replace("\n", "\\n"))
            .append("\"");
        connectToLatestSibling(depth);
        outputLines.add(line);
        for (PsiElement childNode : psiElement.getChildren()) {
            printNode(childNode, depth + 1);
        }
    }

    private void printNode(PsiTree.Node node, int depth) {
        StringBuilder line = new StringBuilder();
        line.append(leftPadding(depth));
        line.append(" ")
            .append(node.getRange())
            .append(" | Type:")
            .append(node.getType())
            .append(" | Text: \"")
            .append(truncate(node.getPsiElement().getText()).replace("\n", "\\n"))
            .append("\"");
        connectToLatestSibling(depth);
        outputLines.add(line);
        for (PsiTree.Node childNode : node.getChildNodes()) {
            printNode(childNode, depth + 1);
        }
    }

    private static String printIndexedSourceCode(String sourceCode) {
        int count = 0;
        String[] lines = sourceCode.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            StringBuilder spacesSb = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                if (count % 10 == 0) {
                    spacesSb.append((count / 10) % 10);
                } else {
                    spacesSb.append(" ");
                }
                count++;
            }

            result.append(line)
                .append("\n")
                .append(spacesSb)
                .append("\n");
            count++;
        }
        return result.toString();
    }

    /**
     * print left padding for a line
     * @param depth, depth starts from 0 (the root)
     */
    private static String leftPadding(int depth) {
        StringBuilder sb = new StringBuilder();
        int tabCount = depth - 1;
        if (tabCount > 0) {
            sb.append(String.join("", Collections.nCopies(tabCount, TAB)));
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

    private String truncate(String content) {
        if (content.length() > CONTENT_MAX_LENGTH) {
            return content.substring(0, CONTENT_MAX_LENGTH - 3) + "...";
        }
        return content;
    }
}
