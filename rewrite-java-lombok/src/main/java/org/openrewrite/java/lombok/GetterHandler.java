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
import lombok.Getter;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.HandleGetter;
import lombok.javac.handlers.JavacHandlerUtil;

public class GetterHandler extends JavacAnnotationHandler<Getter> {
    @Override
    public void handle(AnnotationValues<Getter> annotationValues, JCTree.JCAnnotation jcAnnotation, JavacNode javacNode) {
        if (annotationValues.getInstance().lazy()) {
            List<JCTree.JCExpression> args = jcAnnotation.args;
            int i = 0;
            JCTree.JCExpression oldRhs = null;
            for (; i < args.size(); i++) {
                JCTree.JCExpression arg = args.get(i);
                if (arg instanceof JCTree.JCAssign) {
                    JCTree.JCIdent lhs = (JCTree.JCIdent) ((JCTree.JCAssign) arg).lhs;
                    if (lhs.name.contentEquals("lazy")) {
                        oldRhs = ((JCTree.JCAssign) arg).rhs;
                        break;
                    }
                }
            }
            ((JCTree.JCAssign) jcAnnotation.args.get(i)).rhs = javacNode.getTreeMaker().Literal(false);
            AnnotationValues<Getter> annotation = JavacHandlerUtil.createAnnotation(Getter.class, jcAnnotation, javacNode);
            new HandleGetter().handle(annotation, jcAnnotation, javacNode);
            ((JCTree.JCAssign) jcAnnotation.args.get(i)).rhs = oldRhs;
        } else {
            new HandleGetter().handle(annotationValues, jcAnnotation, javacNode);
        }
    }
}
