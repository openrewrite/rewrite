package org.openrewrite.kotlin;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.kotlin.table.KotlinSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindKotlinSources extends Recipe {
    KotlinSourceFile kotlinSourceFile = new KotlinSourceFile(this);

    @Override
    public String getDisplayName() {
        return "Find Kotlin sources and collect data metrics";
    }

    @Override
    public String getDescription() {
        return "Use data table to collect source files types and counts of files with extensions `.kt`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visitSourceFile(SourceFile sourceFile, ExecutionContext ctx) {
                if (sourceFile.getSourcePath().toString().endsWith(".kt")) {
                    KotlinSourceFile.SourceFileType sourceFileType = null;
                    if (sourceFile instanceof K.CompilationUnit) {
                        sourceFileType = KotlinSourceFile.SourceFileType.Kotlin;
                    } else if (sourceFile instanceof Quark) {
                        sourceFileType = KotlinSourceFile.SourceFileType.Quark;
                    } else if (sourceFile instanceof PlainText) {
                        sourceFileType = KotlinSourceFile.SourceFileType.PlainText;
                    }
                    kotlinSourceFile.insertRow(ctx, new KotlinSourceFile.Row(sourceFile.getSourcePath().toString(), sourceFileType));
                }
                return sourceFile;
            }
        };
    }
}
