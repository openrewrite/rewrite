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
package org.openrewrite.csharp;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.StringJoiner;

/**
 * Changes the target framework in .csproj files.
 * Handles both single-TFM ({@code <TargetFramework>}) and
 * multi-TFM ({@code <TargetFrameworks>}) elements.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDotNetTargetFramework extends Recipe {

    @Option(displayName = "Old target framework",
            description = "The target framework moniker to replace (e.g., net8.0).",
            example = "net8.0")
    String oldTargetFramework;

    @Option(displayName = "New target framework",
            description = "The target framework moniker to use instead (e.g., net9.0).",
            example = "net9.0")
    String newTargetFramework;

    @Override
    public String getDisplayName() {
        return "Change .NET target framework";
    }

    @Override
    public String getDescription() {
        return "Changes the `<TargetFramework>` or `<TargetFrameworks>` value in .csproj files. " +
               "For multi-TFM projects, replaces the matching framework within the semicolon-delimited list.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindMSBuildProject(),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);
                        String name = t.getName();

                        if ("TargetFramework".equals(name)) {
                            String value = t.getValue().orElse("");
                            if (oldTargetFramework.equals(value)) {
                                doAfterVisit(new ChangeTagValueVisitor<>(t, newTargetFramework));
                            }
                        } else if ("TargetFrameworks".equals(name)) {
                            String value = t.getValue().orElse("");
                            // Replace within semicolon-delimited list, deduplicating
                            String[] frameworks = value.split(";");
                            boolean changed = false;
                            LinkedHashSet<String> seen = new LinkedHashSet<>();
                            for (String framework : frameworks) {
                                String fw = framework.trim();
                                if (oldTargetFramework.equals(fw)) {
                                    changed = true;
                                    fw = newTargetFramework;
                                }
                                seen.add(fw);
                            }
                            if (changed) {
                                StringJoiner sj = new StringJoiner(";");
                                for (String fw : seen) {
                                    sj.add(fw);
                                }
                                doAfterVisit(new ChangeTagValueVisitor<>(t, sj.toString()));
                            }
                        }
                        return t;
                    }
                }
        );
    }
}
