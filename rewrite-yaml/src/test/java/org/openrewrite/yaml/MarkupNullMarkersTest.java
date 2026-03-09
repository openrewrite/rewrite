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
import org.openrewrite.*;
import org.openrewrite.internal.FindRecipeRunException;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Markup;
import org.openrewrite.yaml.tree.Yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MarkupNullMarkersTest {

    @Test
    void markupErrorShouldHandleNullMarkers() {
        // Parse a valid YAML document
        Yaml.Documents yaml = (Yaml.Documents) new YamlParser().parse("key: value").findFirst().get();
        Yaml.Document doc = yaml.getDocuments().get(0);
        Yaml.Mapping mapping = (Yaml.Mapping) doc.getBlock();
        Yaml.Mapping.Entry entry = mapping.getEntries().get(0);
        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();

        // Simulate a node with null markers (e.g., from RPC deserialization)
        Yaml.Scalar corruptedScalar = new Yaml.Scalar(
                scalar.getId(), scalar.getPrefix(), null, scalar.getStyle(),
                scalar.getAnchor(), scalar.getTag(), scalar.getValue());

        // Markup.error should not throw NPE when markers are null
        Yaml.Scalar result = assertDoesNotThrow(
                () -> Markup.error(corruptedScalar, new RuntimeException("test error")));
        assertThat(result.getMarkers()).isNotNull();
        assertThat(result.getMarkers().findFirst(Markup.Error.class)).isPresent();
    }

    @Test
    void findRecipeRunExceptionShouldHandleNullMarkers() {
        // Parse a valid YAML document
        Yaml.Documents yaml = (Yaml.Documents) new YamlParser().parse("key: value").findFirst().get();
        Yaml.Document doc = yaml.getDocuments().get(0);
        Yaml.Mapping mapping = (Yaml.Mapping) doc.getBlock();
        Yaml.Mapping.Entry entry = mapping.getEntries().get(0);
        Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();

        // Simulate a node with null markers (e.g., from RPC deserialization)
        Yaml.Scalar corruptedScalar = new Yaml.Scalar(
                scalar.getId(), scalar.getPrefix(), null, scalar.getStyle(),
                scalar.getAnchor(), scalar.getTag(), scalar.getValue());

        // Build a tree that contains the corrupted node
        Yaml.Mapping.Entry corruptedEntry = entry.withValue(corruptedScalar);
        Yaml.Mapping corruptedMapping = mapping.withEntries(java.util.List.of(corruptedEntry));
        Yaml.Document corruptedDoc = doc.withBlock(corruptedMapping);
        Yaml.Documents corruptedYaml = yaml.withDocuments(java.util.List.of(corruptedDoc));

        // Create a RecipeRunException with a cursor pointing to the corrupted node
        Cursor rootCursor = new Cursor(null, Cursor.ROOT_VALUE);
        Cursor docsCursor = new Cursor(rootCursor, corruptedYaml);
        Cursor docCursor = new Cursor(docsCursor, corruptedDoc);
        Cursor mappingCursor = new Cursor(docCursor, corruptedMapping);
        Cursor entryCursor = new Cursor(mappingCursor, corruptedEntry);
        Cursor scalarCursor = new Cursor(entryCursor, corruptedScalar);

        RecipeRunException rre = new RecipeRunException(
                new RuntimeException("original error"), scalarCursor);

        // FindRecipeRunException should not throw NPE when marking up a node with null markers
        FindRecipeRunException visitor = new FindRecipeRunException(rre);
        Yaml.Documents result = assertDoesNotThrow(
                () -> (Yaml.Documents) visitor.visitNonNull(corruptedYaml, 0));
        assertThat(result).isNotNull();
    }
}
