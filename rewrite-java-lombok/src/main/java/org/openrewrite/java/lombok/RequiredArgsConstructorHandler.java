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
import lombok.RequiredArgsConstructor;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.HandleConstructor;
import lombok.javac.handlers.JavacHandlerUtil;

public class RequiredArgsConstructorHandler extends JavacAnnotationHandler<RequiredArgsConstructor> {
    @Override
    public void handle(AnnotationValues<RequiredArgsConstructor> annotationValues, JCTree.JCAnnotation jcAnnotation, JavacNode javacNode) {
        // "staticName" and "access" should be retained, but onConstructor is not supported
        // Omit onConstructor to simplify AST -> LST translation
        if(jcAnnotation.getArguments().isEmpty()) {
            new HandleConstructor.HandleRequiredArgsConstructor().handle(annotationValues, jcAnnotation, javacNode);
        } else {
            List<JCTree.JCExpression> originalArgs = jcAnnotation.getArguments();
            List<JCTree.JCExpression> filteredArgs = List.nil();
            for (JCTree.JCExpression originalArg : originalArgs) {
                if (originalArg instanceof JCTree.JCAssign && ((JCTree.JCAssign) originalArg).getVariable() instanceof JCTree.JCIdent) {
                    JCTree.JCAssign assign = (JCTree.JCAssign) originalArg;
                    JCTree.JCIdent ident = (JCTree.JCIdent) assign.getVariable();
                    if ("onConstructor".equals(ident.getName().toString())) {
                        continue;
                    }
                }
                filteredArgs = filteredArgs.append(originalArg);
            }

            jcAnnotation.args = filteredArgs;

            AnnotationValues<RequiredArgsConstructor> annotation = JavacHandlerUtil.createAnnotation(RequiredArgsConstructor.class, jcAnnotation, javacNode);
            new HandleConstructor.HandleRequiredArgsConstructor().handle(annotation, jcAnnotation, javacNode);
            jcAnnotation.args = originalArgs;
        }
    }
}
