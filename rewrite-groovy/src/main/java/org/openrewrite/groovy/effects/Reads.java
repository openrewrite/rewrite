/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.groovy.effects;

import org.openrewrite.Incubating;
import org.openrewrite.groovy.tree.GroovyDispatcher1;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Incubating(since = "7.25.0")
public class Reads extends org.openrewrite.java.effects.Reads implements GroovyDispatcher1<Boolean, JavaType.Variable> {

    @Override
    public Boolean visitMapEntry(G.MapEntry pp, JavaType.Variable v) {
        return reads(pp.getKey(), v) || reads(pp.getValue(), v);
    }

    @Override
    public Boolean visitMapLiteral(G.MapLiteral pp, JavaType.Variable v) {
        for (G.MapEntry e : pp.getElements()) {
            if (reads(e, v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitListLiteral(G.ListLiteral pp, JavaType.Variable v) {
        for (Expression e : pp.getElements()) {
            if (reads(e, v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitGString(G.GString pp, JavaType.Variable v) {
        for (J s : pp.getStrings()) {
            if (s instanceof Expression && reads(s, v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitGBinary(G.Binary pp, JavaType.Variable v) {
        return reads(pp.getLeft(), v) || reads(pp.getRight(), v);
    }
}

