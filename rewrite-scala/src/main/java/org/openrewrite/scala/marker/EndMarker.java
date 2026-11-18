/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.scala.marker;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.Quoted;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.scala.tree.S;

import java.util.UUID;

/**
 * Marks an element closed by a Scala 3 end marker, as in {@code end A} after an indented class body.
 * The name after {@code end} is not stored. The printer takes it from the element with {@link #nameOf},
 * so a rename carries over. {@code beforeEnd} holds the space before {@code end} unless the element's
 * body block already ends with it.
 */
@Value
@With
public class EndMarker implements Marker {
    UUID id;
    Space beforeEnd;
    Space beforeName;

    public static EndMarker create(Space beforeEnd, Space beforeName) {
        return new EndMarker(Tree.randomId(), beforeEnd, beforeName);
    }

    /**
     * The name an end marker closing {@code j} carries, or null when {@code j} is no element an end marker can close.
     */
    public static @Nullable String nameOf(J j) {
        if (j instanceof J.ClassDeclaration) {
            return asWritten(((J.ClassDeclaration) j).getName());
        } else if (j instanceof J.MethodDeclaration) {
            return asWritten(((J.MethodDeclaration) j).getName());
        } else if (j instanceof J.VariableDeclarations) {
            return asWritten(((J.VariableDeclarations) j).getVariables().get(0).getName());
        } else if (j instanceof S.AnonymousGiven) {
            return "given";
        } else if (j instanceof S.PatternDefinition) {
            return "val";
        } else if (j instanceof S.ExtensionMethods) {
            return "extension";
        } else if (j instanceof J.Package) {
            String packageName = ((J.Package) j).getPackageName();
            return packageName.substring(packageName.lastIndexOf('.') + 1);
        } else if (j instanceof J.If) {
            return "if";
        } else if (j instanceof J.WhileLoop) {
            return "while";
        } else if (j instanceof J.ForEachLoop || j instanceof S.For) {
            return "for";
        } else if (j instanceof J.Try || j instanceof S.Try) {
            return "try";
        } else if (j instanceof J.Switch) {
            return "match";
        } else if (j instanceof J.NewClass) {
            return "new";
        }
        return null;
    }

    /**
     * An anonymous given has no name, so its end marker says {@code given}.
     */
    private static String asWritten(J.Identifier name) {
        if (name.getSimpleName().isEmpty()) {
            return "given";
        }
        return name.getMarkers().findFirst(Quoted.class).isPresent() ?
                "`" + name.getSimpleName() + "`" :
                name.getSimpleName();
    }
}
