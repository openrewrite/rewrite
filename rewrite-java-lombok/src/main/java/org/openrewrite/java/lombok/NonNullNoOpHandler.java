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
import lombok.NonNull;
import lombok.core.AnnotationValues;
import lombok.core.HandlerPriority;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;

@HandlerPriority(512)
public class NonNullNoOpHandler extends JavacAnnotationHandler<NonNull> {
    public NonNullNoOpHandler() {
        super();
    }

    @Override
    public void handle(AnnotationValues<NonNull> annotationValues, JCTree.JCAnnotation jcAnnotation, JavacNode javacNode) {
    }
}
