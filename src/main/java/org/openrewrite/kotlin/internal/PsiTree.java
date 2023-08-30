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

import lombok.Data;
import lombok.val;
import org.jetbrains.kotlin.com.intellij.lang.impl.TokenSequence;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiFile;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.openrewrite.internal.lang.Nullable;

import java.util.*;

/**
 * Internal util class to present a PSI tree to help developers to debug or trouble-shooting.
 * An example usage :
 * <code>
 *      PsiTree psiTree = new PsiTree(psiFile, input.getSource(new InMemoryExecutionContext()).readFully());
 *      System.out.println(PsiTreePrinter.printPsiTree(psiTree));
 * </code>
 */
@Data
public class PsiTree {
    private PsiFile psiFile;
    private String source;
    private Node root;
    private List<PsiToken> tokens;
    private Map<Integer, PsiToken> tokenOffSetMap;

    @SuppressWarnings("DataFlowIssue")
    PsiTree(PsiFile psiFile, String source) {
        this.psiFile = psiFile;
        this.source = source;
        root = toTree(psiFile, null);
        expandRawPsiTree(root);
        tokenize(this.source);
    }

    @Data
    public static class Node {
        public Node(TextRange range, String type,  @Nullable Node parent, PsiElement psiElement) {
            this.range = range;
            this.type = type;
            this.psiElement = psiElement;
            this.childNodes = new ArrayList<>();
            this.parent = parent;
        }

        @Override
        public String toString() {
            return range + " type:" + type;
        }

        TextRange range;
        String type;
        List<Node> childNodes;

        @Nullable
        Node parent;
        PsiElement psiElement;


        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            Node other = (Node) obj;
            return range.equals(other.range) && type.equals(other.getType());
        }
    }

    private static Node toNode(PsiElement psiElement, Node parentNode) {
        return new Node(psiElement.getTextRange(), psiElement.getNode().getElementType().toString(), parentNode, psiElement);
    }

    private static Node toTree(PsiElement psiElement, Node parentNode) {
        Node node = toNode(psiElement, parentNode);
        for (PsiElement childPsiElement: psiElement.getChildren()) {
            Node childNode = toTree(childPsiElement, node);
            node.getChildNodes().add(childNode);
        }
        return node;
    }

    private void expandRawPsiTree(Node node) {
        for (Node childNode : node.getChildNodes()) {
            expandRawPsiTree(childNode);
        }

        if (node.getParent() == null) {
            return;
        }

        List<Node> childNodes = node.getParent().getChildNodes();
        int leftBound = Integer.MAX_VALUE;
        int rightBound = -1;
        if (!childNodes.isEmpty()) {
            leftBound = childNodes.get(0).getRange().getStartOffset();
            rightBound = childNodes.get(childNodes.size() - 1).getRange().getStartOffset();
        }

        List<Node> nodesBefore = new ArrayList<>();
        List<Node> nodesAfter = new ArrayList<>();
        PsiElement preSibling = node.getPsiElement().getPrevSibling();
        while (preSibling != null && preSibling.getTextRange().getStartOffset() < leftBound) {
            nodesBefore.add(toNode(preSibling, node.getParent()));
            preSibling = preSibling.getPrevSibling();
        }
        Collections.reverse(nodesBefore);

        PsiElement nextSibling = node.getPsiElement().getNextSibling();
        while (nextSibling != null && nextSibling.getTextRange().getStartOffset() > rightBound) {
            nodesAfter.add(toNode(nextSibling, node.getParent()));
            nextSibling = nextSibling.getNextSibling();
        }

        if (!nodesBefore.isEmpty() || !nodesAfter.isEmpty()) {
            List<Node> newChildNodes = new ArrayList<>();
            newChildNodes.addAll(nodesBefore);
            newChildNodes.addAll(node.getParent().getChildNodes());
            newChildNodes.addAll(nodesAfter);
            node.getParent().setChildNodes(newChildNodes);
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "DataFlowIssue"})
    private void tokenize(String sourceCode) {
        tokens = new ArrayList<>();
        tokenOffSetMap = new HashMap<>();

        val kotlinLexer = new KotlinLexer();
        TokenSequence tokenSequence = TokenSequence.performLexing(sourceCode, kotlinLexer);
        int tokenCount = tokenSequence.getTokenCount();


        PsiToken preToken = null;
        for (int i = 0; i < tokenCount; i++) {
            int tokenStart = tokenSequence.getTokenStart(i);
            IElementType iElementType = tokenSequence.getTokenType(i);
            PsiToken token = new PsiToken();
            token.setRange(new TextRange(tokenStart, Integer.MAX_VALUE));
            token.setType(iElementType.toString());

            if (preToken != null) {
                preToken.setText(sourceCode.substring(preToken.getRange().getStartOffset(), tokenStart));
                TextRange newRange = new TextRange(preToken.getRange().getStartOffset(), tokenStart);
                preToken.setRange(newRange);
            }
            tokens.add(token);
            tokenOffSetMap.put(token.getRange().getStartOffset(), token);
            preToken = token;
        }

        if (preToken != null) {
            preToken.setText(sourceCode.substring(preToken.getRange().getStartOffset()));
            TextRange newRange = new TextRange(preToken.getRange().getStartOffset(), sourceCode.length());
            preToken.setRange(newRange);
        }
    }
}
