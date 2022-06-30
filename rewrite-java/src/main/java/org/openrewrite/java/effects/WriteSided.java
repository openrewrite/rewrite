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
package org.openrewrite.java.effects;

import org.openrewrite.Incubating;
import org.openrewrite.java.tree.JavaDispatcher2;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Incubating(since =  "7.25.0")
public class WriteSided  implements JavaDispatcher2<Boolean, JavaType.Variable, Side> {

    private static final Writes WRITES = new Writes();

    /**
     * @return Whether given expression, when in given position, may read variable v.
     */
    public boolean writes(J e, JavaType.Variable variable, Side side) {
        return dispatch(e, variable, side);
    }

    @Override
    public Boolean visitFieldAccess(J.FieldAccess pp, JavaType.Variable v, Side s) {
        return (s == Side.LVALUE && pp.getType() != null && pp.getType().equals(v)) || writes(pp.getTarget(), v, s);
    }

    @Override
    public Boolean visitIdentifier(J.Identifier pp, JavaType.Variable v, Side s) {
        return (s == Side.LVALUE) && pp.getFieldType() != null && pp.getFieldType().equals(v);
    }

    @Override
    public Boolean visitLiteral(J.Literal pp, JavaType.Variable variable, Side side) {
        return false;
    }

    @Override
    public Boolean visitMethodInvocation(J.MethodInvocation pp, JavaType.Variable variable, Side side) {
        assert side == Side.RVALUE;
        return WRITES.writes(pp, variable);
    }
}
