/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.internal.template;

import org.openrewrite.java.internal.template.Substitutions;

import static java.util.Collections.emptySet;

public class ScalaSubstitutions extends Substitutions {
    public ScalaSubstitutions(String code, Object[] parameters) {
        super(code, emptySet(), parameters);
    }

    @Override
    protected String newObjectParameter(String fqn, int index) {
        return "__P__./*__p" + index + "__*/p[" + fqn + "]()";
    }

    @Override
    protected String newPrimitiveParameter(String fqn, int index) {
        return newObjectParameter(fqn, index);
    }
}
