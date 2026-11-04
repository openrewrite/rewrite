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
package org.openrewrite.hcl;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.HclRightPadded;
import org.openrewrite.hcl.tree.Space;
import org.openrewrite.marker.Markers;

public class ReplaceLegacyAttributeIndexSyntax extends Recipe {
    @Getter
    final String displayName = "Replace legacy attribute index syntax";

    @Getter
    final String description = "Replace legacy attribute index syntax (`.0`) with the new syntax (`[0]`).";

    @Override
    public HclVisitor<ExecutionContext> getVisitor() {
        return new HclVisitor<ExecutionContext>() {
            @Override
            public Hcl.Index visitLegacyIndexAttribute(Hcl.LegacyIndexAttributeAccess legacy, ExecutionContext ctx) {
                Hcl.LegacyIndexAttributeAccess ret = (Hcl.LegacyIndexAttributeAccess) super.visitLegacyIndexAttribute(legacy, ctx);
                Hcl.Index.Position position = new Hcl.Index.Position(Tree.randomId(), Space.EMPTY, ret.getIndex().getMarkers(), new HclRightPadded<>(ret.getIndex(), Space.EMPTY, Markers.EMPTY));
                return new Hcl.Index(Tree.randomId(), ret.getPrefix(), ret.getMarkers(), ret.getBase().getElement(), position);
            }
        };
    }
}
