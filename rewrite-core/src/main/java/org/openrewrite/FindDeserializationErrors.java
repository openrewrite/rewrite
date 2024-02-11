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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.marker.DeserializationError;
import org.openrewrite.marker.LstProvenance;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.OutdatedSerializer;
import org.openrewrite.table.DeserializationErrorTable;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDeserializationErrors extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find deserialization errors";
    }

    @Override
    public String getDescription() {
        return "Produces a data table collecting all deserialization errors of serialized LSTs.";
    }

    transient DeserializationErrorTable dataTable = new DeserializationErrorTable(this);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return tree.getMarkers().findFirst(DeserializationError.class)
                        .map(error -> {
                            Optional<OutdatedSerializer> outdatedSerializer = tree.getMarkers().findFirst(OutdatedSerializer.class);
                            Optional<LstProvenance> lstProvenance = tree.getMarkers().findFirst(LstProvenance.class);
                            dataTable.insertRow(ctx, new DeserializationErrorTable.Row(
                                    tree instanceof SourceFile ? ((SourceFile) tree).getSourcePath().toString() : null,
                                    error.getMessage(),
                                    error.getDetail(),
                                    outdatedSerializer.map(OutdatedSerializer::getLanguage).orElse(null),
                                    outdatedSerializer.map(OutdatedSerializer::getMinimumVersion).orElse(null),
                                    outdatedSerializer.map(OutdatedSerializer::getActualVersion)
                                            .orElseGet(() -> lstProvenance.map(LstProvenance::getBuildToolVersion).orElse(null)),
                                    lstProvenance.map(LstProvenance::getTimestampUtc).map(Instant::toEpochMilli).orElse(null),
                                    lstProvenance.map(LstProvenance::getTimestampUtc).map(ts -> ts.atZone(UTC)).map(ZonedDateTime::toString).orElse(null)
                            ));

                            return Markup.info(tree, error.getMessage());
                        })
                        .orElse(tree);
            }
        };
    }
}
