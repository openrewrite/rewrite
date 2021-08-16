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
package org.openrewrite.text;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Incubating(since = "7.12.0")
@Value
@EqualsAndHashCode(callSuper = false)
public class CreateTextFile extends Recipe {

    @Option(displayName = "File Contents",
            description = "Multiline text content for the file.",
            example = "Some text.")
    String fileContents;

    @Option(displayName = "Relative File Path",
            description = "File path of new file.",
            example = "foo/bar/baz.txt")
    String relativeFileName;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Override
    public String getDisplayName() {
        return "Create text file";
    }

    @Override
    public String getDescription() {
        return "Creates a new plain text file.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Path path = Paths.get(relativeFileName);
        SourceFile matchingFile = null;

        for (SourceFile sourceFile: before) {
            if (path.toString().equals(sourceFile.getSourcePath().toString())) {
                matchingFile = sourceFile;
            }
        }

        // return early if file exists and there's no explicit permission to overwrite
        if (matchingFile != null && (overwriteExisting == null  || Boolean.FALSE.equals(overwriteExisting))) {
            return before;
        }

        PlainText brandNewFile = new PlainText(Tree.randomId(), path, Markers.EMPTY, fileContents);

        if (matchingFile != null && Boolean.TRUE.equals(overwriteExisting)) {
            brandNewFile = new PlainText(matchingFile.getId(), brandNewFile.getSourcePath(), brandNewFile.getMarkers(), brandNewFile.getText());
        }

        return ListUtils.concat(before,brandNewFile);
    }
}
