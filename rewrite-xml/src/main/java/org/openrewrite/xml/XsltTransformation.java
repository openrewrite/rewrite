/*
 * Copyright 2020 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

@Value
@EqualsAndHashCode(callSuper = false)
public class XsltTransformation extends Recipe {

    @Nullable
    @Language("xml")
    @Option(displayName = "XSLT Configuration transformation",
            description = "The transformation to be applied.",
            example = "<xsl:stylesheet ...>...</xsl:stylesheet>",
            required = false)
    String xslt;

    @Nullable
    @Option(displayName = "XSLT Configuration transformation classpath resource",
            description = "Recipe transformation provided as a classpath resource.",
            example = "/changePlugin.xslt",
            required = false)
    String xsltResource;

    @Option(displayName = "File pattern",
            description = "A glob expression that can be used to constrain which directories or source files should be searched. " +
                          "Multiple patterns may be specified, separated by a semicolon `;`. " +
                          "If multiple patterns are supplied any of the patterns matching will be interpreted as a match.",
            example = "**/*.xml")
    String filePattern;

    @Override
    public String getDisplayName() {
        return "XSLT transformation";
    }

    @Override
    public String getDescription() {
        return "Apply the specified XSLT transformation on matching files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new XsltTransformationVisitor(loadResource(xslt, xsltResource));

        @SuppressWarnings("unchecked")
        TreeVisitor<?, ExecutionContext> check = Preconditions.or(Arrays.stream(filePattern.split(";"))
                .map(FindSourceFiles::new)
                .map(FindSourceFiles::getVisitor)
                .toArray(TreeVisitor[]::new));

        return Preconditions.check(check, visitor);
    }

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(Validated.test("xslt", "set either xslt or xsltResource, but not both",
                        xslt, s -> StringUtils.isBlank(s) != StringUtils.isBlank(xsltResource) &&
                                   !StringUtils.isBlank(loadResource(xslt, xsltResource))));
    }

    private static @Nullable String loadResource(@Nullable String xslt, @Nullable String xsltResource) {
        if (StringUtils.isBlank(xsltResource)) {
            return xslt;
        }
        try (InputStream is = XsltTransformation.class.getResourceAsStream(StringUtils.trimIndent(xsltResource))) {
            assert is != null;
            return !StringUtils.isBlank(xsltResource) ? StringUtils.readFully(is, Charset.defaultCharset())
                    : xslt;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
