/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java;

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReportingRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Generating an asciidoc report";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        List<String> allDatabaseBeanDefinitions = new ArrayList<>();

        for (SourceFile sourceFile : before) {
            new JavaVisitor<Integer>() {
                @Override
                public Marker visitMarker(Marker marker, Integer p) {
                    if(marker instanceof DatabaseBeanDefinition) {
                        // you now have the full context of the entire AST of the database
                        // bean definition and can visit further down to scoop out whatever
                        // needed for the report. for example if you put this marker
                        // on a class declaration...
                        allDatabaseBeanDefinitions.add(getCursor()
                                .getParentOrThrow()
                                .<J.ClassDeclaration>getValue()
                                .getSimpleName()
                        );
                    }
                    return super.visitMarker(marker, p);
                }
            }.visit(sourceFile, 0);
        }

        for (String allDatabaseBeanDefinition : allDatabaseBeanDefinitions) {
            // write report
        }


        return super.visit(before, ctx);
    }
}

@Value
class DatabaseBeanDefinition implements Marker {
    UUID id;
}
