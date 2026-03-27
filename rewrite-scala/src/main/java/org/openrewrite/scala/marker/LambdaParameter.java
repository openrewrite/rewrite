/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.Tree;
import org.openrewrite.marker.Marker;

import java.util.UUID;

/**
 * A marker to indicate that a J.VariableDeclarations represents a lambda parameter
 * in Scala and should be printed without val/var keywords.
 */
@Value
@With
public class LambdaParameter implements Marker {
    UUID id;
    
    public LambdaParameter() {
        this.id = Tree.randomId();
    }
    
    public LambdaParameter(UUID id) {
        this.id = id;
    }
}