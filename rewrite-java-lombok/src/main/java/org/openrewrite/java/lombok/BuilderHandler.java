/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.lombok;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import lombok.Builder;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.core.LombokImmutableList;
import lombok.core.LombokNode;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.HandleBuilder;
import lombok.javac.handlers.HandleConstructor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static lombok.core.AST.Kind.ANNOTATION;

@HandlerPriority(-1024)
public class BuilderHandler extends JavacAnnotationHandler<Builder> {
    @Override
    public void handle(AnnotationValues<Builder> annotationValues, JCTree.JCAnnotation jcAnnotation, JavacNode javacNode) {
        JavacNode parent = javacNode.up();
        if (!(parent.get() instanceof JCTree.JCClassDecl)) {
            new HandleBuilder().handle(annotationValues, jcAnnotation, javacNode);
            return;
        }
        Map<JCTree.JCModifiers, FlagAndAnnotations> modifiersRestoreMap = new HashMap<>();
        Map<JavacNode, LombokImmutableList<JavacNode>> nodeToChildrenMap = new HashMap<>();
        Field childrenField = null;
        try {
            childrenField = LombokNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            // The Lombok handler for the @Builder annotation sets the init expression to null for fields annotated
            // with @Builder.Default. Unlike typical Lombok behavior, which either creates new methods or fields, the
            // handler for Builder annotation modifies the existing variable declaration. As a result, the AST-to-LST
            // converter never encounters the init expression, causing it to behave unexpectedly.
            // Additionally, the @Builder.Default annotation generates a private method named $default$<varname>,
            // which is unlikely to be called in original code. Therefore, it is safe to temporarily remove the
            // @Builder.Default annotation during processing.
            for (JavacNode fieldNode : HandleConstructor.findAllFields(parent, true)) {
                @SuppressWarnings("unchecked") // if the cast fails the resulting parser error is clear enough
                LombokImmutableList<JavacNode> children = (LombokImmutableList<JavacNode>) childrenField.get(fieldNode);
                nodeToChildrenMap.put(fieldNode, children);
                LombokImmutableList<JavacNode> filtered = children;
                for (int i : findBuilderDefaultIndexes(children)) {
                    filtered = filtered.removeElementAt(i);
                }
                childrenField.set(fieldNode, filtered);
                JCTree.JCVariableDecl fd = (JCTree.JCVariableDecl) fieldNode.get();
                JCTree.JCModifiers modifiers = fd.getModifiers();

                modifiersRestoreMap.put(modifiers, new FlagAndAnnotations(modifiers.flags, modifiers.annotations));
                modifiers.annotations = removeBuilderDefault(modifiers.annotations);
                modifiers.flags = modifiers.flags & (~(1 << 4) /*final mask*/);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // On exception just continue with the original handler
        }
        new HandleBuilder().handle(annotationValues, jcAnnotation, javacNode);

        // restore values
        for (Map.Entry<JCTree.JCModifiers, FlagAndAnnotations> entry : modifiersRestoreMap.entrySet()) {
            entry.getKey().flags = entry.getValue().flags;
            entry.getKey().annotations = entry.getValue().annotations;
        }
        for (Map.Entry<JavacNode, LombokImmutableList<JavacNode>> entry : nodeToChildrenMap.entrySet()) {
            try {
                childrenField.set(entry.getKey(), entry.getValue());
            } catch (IllegalAccessException e) {
                // Not possible.
            }
        }
    }

    private List<JCTree.JCAnnotation> removeBuilderDefault(List<JCTree.JCAnnotation> annotations) {
        ListBuffer<JCTree.JCAnnotation> filteredAnnotations = new ListBuffer<>();
        for (JCTree.JCAnnotation annotation : annotations) {
            if ("lombok.Builder.Default".equals(annotation.getAnnotationType().toString())) {
                continue;
            }
            filteredAnnotations = filteredAnnotations.append(annotation);
        }
        return filteredAnnotations.toList();
    }

    private List<Integer> findBuilderDefaultIndexes(LombokImmutableList<JavacNode> nodes) {
        ListBuffer<Integer> indexes = new ListBuffer<>();
        for (int i = 0; i < nodes.size(); i++) {
            JavacNode node = nodes.get(i);
            if (node.getKind() == ANNOTATION && "lombok.Builder.Default".equals(node.get().type.toString())) {
                indexes.add(i);
            }
        }
        return indexes.toList();
    }

    private static class FlagAndAnnotations {
        long flags;
        List<JCTree.JCAnnotation> annotations;

        FlagAndAnnotations(long flags, List<JCTree.JCAnnotation> annotations) {
            this.flags = flags;
            this.annotations = annotations;
        }
    }
}
