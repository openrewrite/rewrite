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
package org.openrewrite;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.Marker;
import org.openrewrite.table.DistinctGitProvenance;
import org.openrewrite.table.RecipeRunStats;
import org.openrewrite.table.SourcesFileResults;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.GitProvenance.AutoCRLF.False;
import static org.openrewrite.marker.GitProvenance.EOL.Native;
import static org.openrewrite.test.SourceSpecs.text;

class RecipeEstimatedEffortTest implements RewriteTest {
    private static final Long EXPECTED_DEFAULT_ESTIMATED_EFFORT = 300L;
    private static final Long EXPECTED_CUSTOM_ESTIMATED_EFFORT = 900L;

    @Test
    void zeroEstimatedEffortForRecipeThatDoesNotGenerateSourcesFileResults() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new NoChangeRecipe())
            .afterRecipe(recipeRun ->
              assertThat(recipeRun.getDataTables().isEmpty()).isTrue()
            )
        );
    }

    @Test
    void zeroEstimatedEffortForRecipeThatOnlyCreatesCustomDataTable() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new FindGitProvenance())
            .afterRecipe(recipeRun -> {
                  assertThat(recipeRun.getDataTables().size()).isEqualTo(2);
                  assertThat(recipeRun.getDataTable(DistinctGitProvenance.class.getName())).isNotNull();
                  assertThat(recipeRun.getDataTable(RecipeRunStats.class.getName())).isNotNull();
                  assertThat(recipeRun.getDataTable(SourcesFileResults.class.getName())).isNull();
              }
            ),
          text(
            "Hello, World!",
            spec -> spec.markers(new GitProvenance(Tree.randomId(), "https://github.com/openrewrite/rewrite",
              "main", "1234567", False, Native, emptyList()))
          )
        );
    }

    @Test
    void defaultEstimatedEffortForRecipeThatChangesSourceFiles() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new FindAndReplace("replace_me", "replacement", null, null, null, null, null, null))
            .afterRecipe(recipeRun -> assertEstimatedEffort(recipeRun, 1, EXPECTED_DEFAULT_ESTIMATED_EFFORT)),
          text(
            """
              replace_me
              """,
            """
              replacement
              """
          ));
    }

    @Test
    void customEstimatedEffortForRecipeThatChangesSourceFiles() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new CustomEstimatedEffortAppendToTextRecipe("before", "after"))
            .afterRecipe(recipeRun -> assertEstimatedEffort(recipeRun, 1, EXPECTED_CUSTOM_ESTIMATED_EFFORT)),
          text(
            """
              before
              """,
            """
              beforeafter
              """
          ));
    }

    @Test
    void defaultEstimatedEffortForRecipeThatGeneratesSourceFiles() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new CreateTextFile("foo", "manifest.txt", false))
            .afterRecipe(recipeRun -> assertEstimatedEffort(recipeRun, 1, EXPECTED_DEFAULT_ESTIMATED_EFFORT)),
          text(
            null,
            "foo",
            spec -> spec.path("manifest.txt")
          ));
    }

    @Test
    void customEstimatedEffortForRecipeThatGeneratesSourceFiles() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new CustomEstimatedEffortCreateTextFile("foo", "manifest.txt", false))
            .afterRecipe(recipeRun -> assertEstimatedEffort(recipeRun, 1, EXPECTED_CUSTOM_ESTIMATED_EFFORT)),
          text(
            null,
            "foo",
            spec -> spec.path("manifest.txt")
          ));
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class NoChangeRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "No change recipe";
        }

        @Override
        public String getDescription() {
            return "No change recipe.";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CustomEstimatedEffortAppendToTextRecipe extends Recipe {
        @Option(displayName = "Search term",
          example = "before",
          description = "The text to be searched for")
        String searchTerm;

        @Option(displayName = "Appended text",
          example = "after",
          description = "The text to be appended if the search term can found")
        String appendText;

        @Override
        public String getDisplayName() {
            return "CustomEstimatedEffortRecipe";
        }

        @Override
        public String getDescription() {
            return "NoArgRecipe.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new PlainTextVisitor<>() {
                @Override
                public PlainText visitText(PlainText text, ExecutionContext ctx) {
                    for (Marker marker : text.getMarkers().getMarkers()) {
                        if (marker instanceof AlreadyReplaced) {
                            AlreadyReplaced alreadyReplaced = (AlreadyReplaced) marker;
                            if (Objects.equals(searchTerm, alreadyReplaced.getFind()) && Objects.equals(appendText, alreadyReplaced.getReplace())) {
                                return text;
                            }
                        }
                    }

                    if (text.getText().contains(searchTerm)) {
                        return text.withText(text.getText() + appendText).withMarkers(text.getMarkers().add(new AlreadyReplaced(randomId(), searchTerm, appendText)));
                    }
                    return super.visitText(text, ctx);
                }
            };
        }

        @Override
        public @Nullable Duration getEstimatedEffortPerOccurrence() {
            return Duration.ofMinutes(15);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class CustomEstimatedEffortCreateTextFile extends ScanningRecipe<AtomicBoolean> {
        @Option(displayName = "File contents",
          description = "Multiline text content for the file.",
          example = "Some text.")
        String fileContents;

        @Option(displayName = "Relative file path",
          description = "File path of new file.",
          example = "foo/bar/baz.txt")
        String relativeFileName;

        @Option(displayName = "Overwrite existing file",
          description = "If there is an existing file, should it be overwritten.",
          required = false)
        @Nullable
        Boolean overwriteExisting;

        private Duration effort = Duration.ofHours(1);

        @Override
        public String getDisplayName() {
            return "Create text file";
        }

        @Override
        public String getDescription() {
            return "Creates a new plain text file.";
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
                return PlainTextParser.builder().build().parse(fileContents)
                  .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                  .collect(Collectors.toList());
            }
            return emptyList();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
            Path path = Paths.get(relativeFileName);
            return new TreeVisitor<SourceFile, ExecutionContext>() {
                @Override
                public SourceFile visit(@Nullable Tree tree, ExecutionContext ctx) {
                    SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                    if (Boolean.TRUE.equals(overwriteExisting) && path.equals(sourceFile.getSourcePath())) {
                        if (sourceFile instanceof PlainText) {
                            return ((PlainText) sourceFile).withText(fileContents);
                        }
                        PlainText plainText = PlainText.builder()
                          .id(sourceFile.getId())
                          .sourcePath(sourceFile.getSourcePath())
                          .fileAttributes(sourceFile.getFileAttributes())
                          .charsetBomMarked(sourceFile.isCharsetBomMarked())
                          .text(fileContents)
                          .build();
                        if (sourceFile.getCharset() != null) {
                            return plainText.withCharset(sourceFile.getCharset());
                        }
                        return plainText;
                    }
                    return sourceFile;
                }
            };
        }

        @Override
        public @Nullable Duration getEstimatedEffortPerOccurrence() {
            return Duration.ofMinutes(15);
        }
    }

    private void assertEstimatedEffort(RecipeRun recipeRun, int rowIndex, long expectedEffort) {
        String sourceFileResultsCSV = readCSVContentFromSourceFileResults(recipeRun);
        Long estimatedEffort = readEstimatedEffortFromCSV(sourceFileResultsCSV, rowIndex);
        assertThat(estimatedEffort).isEqualTo(expectedEffort);
    }

    private String readCSVContentFromSourceFileResults(RecipeRun recipeRun) {
        StringBuilder output = new StringBuilder();
        final String dataTableName = SourcesFileResults.class.getName();
        RecipeRun.exportCsv(new InMemoryExecutionContext(), recipeRun.getDataTable(dataTableName),
          s -> output.append(s).append("\n"), recipeRun.getDataTableRows(dataTableName));
        return output.toString();
    }

    private Long readEstimatedEffortFromCSV(String csvContent, int rowIndex) {
        String estimatedEffort = readColumnValueFromCSV(csvContent, rowIndex, 4);
        return Long.parseLong(estimatedEffort);
    }

    private String readColumnValueFromCSV(String csvContent, int rowIndex, int columnIndex) {
        try {
            List<List<String>> dataRows = readSourcesFileResultsFromCSV(csvContent);
            return dataRows.get(rowIndex).get(columnIndex);
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private List<List<String>> readSourcesFileResultsFromCSV(String csvContent) throws IOException, CsvValidationException {
        CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(new StringReader(csvContent));
        List<List<String>> dataRows = new ArrayList<>();
        String[] nextRecord;
        while ((nextRecord = csvReader.readNext()) != null) {
            dataRows.add(Arrays.asList(nextRecord));
        }
        return dataRows;
    }
}
