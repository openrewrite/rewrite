/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MarkupNullMarkersTest {

    @Test
    void findRecipeRunExceptionShouldHandleEmptyMarkers() {
        // Parse a valid YAML document
        Yaml.Documents yaml = (Yaml.Documents) new YamlParser().parse("key: value").findFirst().get();
        Yaml.Document doc = yaml.getDocuments().get(0);
        Yaml.Mapping mapping = (Yaml.Mapping) doc.getBlock();
        Yaml.Mapping.Entry entry = mapping.getEntries().get(0);
        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();

        // Simulate a node with Markers.EMPTY (as YamlReceiver now guarantees instead of null)
        Yaml.Scalar nodeWithEmptyMarkers = new Yaml.Scalar(
                scalar.getId(), scalar.getPrefix(), Markers.EMPTY, scalar.getStyle(),
                scalar.getAnchor(), scalar.getTag(), scalar.getValue());

        // Build a tree that contains the node
        Yaml.Mapping.Entry updatedEntry = entry.withValue(nodeWithEmptyMarkers);
        Yaml.Mapping updatedMapping = mapping.withEntries(java.util.List.of(updatedEntry));
        Yaml.Document updatedDoc = doc.withBlock(updatedMapping);
        Yaml.Documents updatedYaml = yaml.withDocuments(java.util.List.of(updatedDoc));

        // Create a RecipeRunException with a cursor pointing to the node
        Cursor rootCursor = new Cursor(null, Cursor.ROOT_VALUE);
        Cursor docsCursor = new Cursor(rootCursor, updatedYaml);
        Cursor docCursor = new Cursor(docsCursor, updatedDoc);
        Cursor mappingCursor = new Cursor(docCursor, updatedMapping);
        Cursor entryCursor = new Cursor(mappingCursor, updatedEntry);
        Cursor scalarCursor = new Cursor(entryCursor, nodeWithEmptyMarkers);

        RecipeRunException rre = new RecipeRunException(
                new RuntimeException("original error"), scalarCursor);

        // FindRecipeRunException should mark the node with an error without NPE
        FindRecipeRunException visitor = new FindRecipeRunException(rre);
        Yaml.Documents result = assertDoesNotThrow(
                () -> (Yaml.Documents) visitor.visitNonNull(updatedYaml, 0));
        assertThat(result).isNotNull();

        // Verify the error marker was applied to the scalar node
        Yaml.Document resultDoc = result.getDocuments().get(0);
        Yaml.Mapping resultMapping = (Yaml.Mapping) resultDoc.getBlock();
        Yaml.Scalar resultScalar = (Yaml.Scalar) resultMapping.getEntries().get(0).getValue();
        assertThat(resultScalar.getMarkers().findFirst(Markup.Error.class)).isPresent();
    }
}
