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
package org.openrewrite.java.tree;

import org.openrewrite.Incubating;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.service.AutoFormatService;
import org.openrewrite.java.service.ImportService;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;

public interface JavaSourceFile extends J {
    TypesInUse getTypesInUse();

    @Nullable
    J.Package getPackageDeclaration();

    JavaSourceFile withPackageDeclaration(J.Package pkg);

    List<Import> getImports();

    JavaSourceFile withImports(List<Import> imports);

    List<J.ClassDeclaration> getClasses();

    JavaSourceFile withClasses(List<J.ClassDeclaration> classes);

    Space getEof();

    JavaSourceFile withEof(Space eof);

    Padding getPadding();

    /**
     * @return An absolute or relative file path.
     */
    Path getSourcePath();

    SourceFile withSourcePath(Path path);

    @Incubating(since = "8.2.0")
    default <S> S service(Class<S> service) {
        try {
            // use name indirection due to possibility of multiple class loaders being used
            if (ImportService.class.getName().equals(service.getName())) {
                return service.getConstructor().newInstance();
            } else if (AutoFormatService.class.getName().equals(service.getName())) {
                return service.getConstructor().newInstance();
            } else if (AnnotationService.class.getName().equals(service.getName())) {
                return service.getConstructor().newInstance();
            } else {
                throw new UnsupportedOperationException("Service " + service + " not supported");
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    interface Padding {
        List<JRightPadded<Import>> getImports();
        JavaSourceFile withImports(List<JRightPadded<Import>> imports);
    }
}
