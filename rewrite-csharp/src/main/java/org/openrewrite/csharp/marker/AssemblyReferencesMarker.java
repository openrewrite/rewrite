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
package org.openrewrite.csharp.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Value
@With
public class AssemblyReferencesMarker implements Marker {
    @EqualsAndHashCode.Exclude
    UUID id;

    /**
     * Assembly reference
     */
    @Value
    @With
    public static class AssemblyReference
    {
        byte[] assembly;
        String fileName;
        @Nullable String packageName;
        @Nullable String packageVersion;
        public void writeToFolder(String directory) throws IOException {
            Path dirPath = Paths.get(directory);
            Files.createDirectories(dirPath); // Ensure the directory exists

            Path filePath = dirPath.resolve(fileName);
            Files.write(filePath, assembly, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
