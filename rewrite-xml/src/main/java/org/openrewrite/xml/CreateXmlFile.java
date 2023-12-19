/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.xml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateXmlFile extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Relative file path",
            description = "File path of new file.",
            example = "foo/bar/baz.xml")
    String relativeFileName;

    @Language("xml")
    @Option(displayName = "File contents",
            description = "Multiline text content for the file.",
            example = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<root>\n" +
                      "    <child>1</child>" +
                      "</root>",
            required = false)
    @Nullable
    String fileContents;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Override
    public String getDisplayName() {
        return "Create XML file";
    }

    @Override
    public String getDescription() {
        return "Create a new XML file.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(Paths.get(relativeFileName), shouldCreate);
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            return XmlParser.builder().build().parse("")
                    .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.equals(document.getSourcePath())) {
                    if (StringUtils.isBlank(fileContents)) {
                        return document.withProlog(null).withRoot(null);
                    }
                    Optional<SourceFile> sourceFiles = XmlParser.builder().build()
                            .parse(fileContents)
                            .findFirst();
                    if (sourceFiles.isPresent()) {
                        SourceFile sourceFile = sourceFiles.get();
                        if (sourceFile instanceof Xml.Document) {
                            Xml.Document newXmlDocument = (Xml.Document) sourceFile;
                            return document
                                    .withProlog(newXmlDocument.getProlog())
                                    .withRoot(newXmlDocument.getRoot());
                        }
                    }
                }
                return document;
            }
        };
    }
}
