package org.openrewrite.cobol.test;

import org.openrewrite.cobol.CobolParser;
import org.openrewrite.cobol.tree.Cobol;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.ParserSupplier;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;

import java.util.function.Consumer;

public interface CobolTest extends RewriteTest {

    default SourceSpecs cobol(@Nullable String before) {
        return cobol(before, s -> {
        });
    }

    default SourceSpecs cobol(@Nullable String before, Consumer<SourceSpec<Cobol.CompilationUnit>> spec) {
        SourceSpec<Cobol.CompilationUnit> cobol = new SourceSpec<>(Cobol.CompilationUnit.class, null, before, null);
        spec.accept(cobol);
        return cobol;
    }

    default SourceSpecs cobol(@Nullable String before, String after) {
        return cobol(before, after, s -> {
        });
    }

    default SourceSpecs cobol(@Nullable String before, String after,
                              Consumer<SourceSpec<Cobol.CompilationUnit>> spec) {
        SourceSpec<Cobol.CompilationUnit> cobol = new SourceSpec<>(Cobol.CompilationUnit.class, null, before, after);
        spec.accept(cobol);
        return cobol;
    }

    @Override
    default ParserSupplier parserSupplierFor(SourceSpec<?> sourceSpec) {
        if(Cobol.CompilationUnit.class.equals(sourceSpec.getSourceFileType())) {
            return new ParserSupplier(Cobol.CompilationUnit.class, sourceSpec.getDsl(), CobolParser::new);
        }
        return RewriteTest.super.parserSupplierFor(sourceSpec);
    }
}
