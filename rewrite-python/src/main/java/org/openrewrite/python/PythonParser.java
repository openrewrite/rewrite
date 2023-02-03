package org.openrewrite.python;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.impl.PsiBuilderImpl;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockProject;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.DefaultPluginDescriptor;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.jetbrains.python.PythonDialectsTokenSetContributor;
import com.jetbrains.python.PythonParserDefinition;
import com.jetbrains.python.PythonTokenSetContributor;
import com.jetbrains.python.documentation.doctest.PyDocstringTokenSetContributor;
import com.jetbrains.python.parsing.PyParser;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFileElementType;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.Parser;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.python.tree.PythonFile;

import com.intellij.openapi.*;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class PythonParser implements Parser<PythonFile> {

    public static class Builder extends org.openrewrite.Parser.Builder {
        public Builder() {
            super(PythonFile.class);
        }

        @Override
        public PythonParser build() {
            return new PythonParser();
        }

        @Override
        public String getDslName() {
            return "python";
        }
    }
    @Override
    public List<PythonFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        Disposable mockDisposable = () -> {
        };

        Application app = new MockApplication(mockDisposable);
        ApplicationManager.setApplication(app, mockDisposable);

        PluginDescriptor pluginDescriptor = new DefaultPluginDescriptor("io.moderne.test");

        ExtensionsAreaImpl extensionsArea = (ExtensionsAreaImpl) app.getExtensionArea();
        ExtensionPointImpl<PythonDialectsTokenSetContributor> extensionPoint = extensionsArea.registerPoint(
                PythonDialectsTokenSetContributor.EP_NAME.getName(),
                PythonDialectsTokenSetContributor.class, pluginDescriptor,
                false
        );
        extensionPoint.registerExtension(new PythonTokenSetContributor());
        extensionPoint.registerExtension(new PyDocstringTokenSetContributor());

        final PyParser parser = new PyParser();
        final ParserDefinition parserDefinition = new PythonParserDefinition();
        final Project project = new MockProject(null, mockDisposable);

        return acceptedInputs(sources).stream().map(sourceFile -> {
            EncodingDetectingInputStream is = sourceFile.getSource(ctx);

            final PyFile file = null;

            final PsiBuilder psiBuilder = new PsiBuilderImpl(
                    project,
                    file,
                    parserDefinition,
                    parserDefinition.createLexer(project),
                    null,
                    is.readFully(),
                    null,
                    null
            );

            @NotNull ASTNode ast = parser.parse(PyFileElementType.INSTANCE, psiBuilder);

            System.out.println(ast);

            return new PythonFile(
                    UUID.randomUUID(),
                    Markers.EMPTY,
                    sourceFile.getPath(),
                    null,
                    is.getCharset(),
                    is.isCharsetBomMarked(),
                    FileAttributes.fromPath(sourceFile.getPath()),
                    ast
            );
        }).collect(toList());
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".py") || fileName.endsWith(".py3");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        // FIXME wrong
        return prefix.resolve("file.py");
    }

    static Builder builder() {
        return new Builder();
    }
}
