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

import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Marker;
import org.openrewrite.xml.tree.Xml;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import static org.openrewrite.Tree.randomId;

@RequiredArgsConstructor
@Incubating(since = "8.30.0")
public class XsltTransformationVisitor extends XmlVisitor<ExecutionContext> {

    private final String xslt;

    @Override
    public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
        for (Marker marker : document.getMarkers().getMarkers()) {
            if (marker instanceof AlreadyReplaced) {
                AlreadyReplaced alreadyReplaced = (AlreadyReplaced) marker;
                if (Objects.equals(xslt, alreadyReplaced.getReplace())) {
                    return document;
                }
            }
        }

        Xml.Document d = (Xml.Document) super.visitDocument(document, executionContext);
        d = transform(d, xslt);
        return d.withMarkers(document.getMarkers().add(new AlreadyReplaced(randomId(), null, xslt)));
    }

    private static Xml.Document transform(Xml.Document document, String xslt) {
        try {
            Source xsltSource = new StreamSource(new ByteArrayInputStream(xslt.getBytes()));
            Transformer transformer = TransformerFactory.newInstance().newTransformer(xsltSource);

            String originalDocument = document.printAll();
            Source text = new StreamSource(new ByteArrayInputStream(originalDocument.getBytes()));
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                transformer.transform(text, new StreamResult(os));
                return document.withRoot(Xml.Tag.build(os.toString().replace("\r", "")));
            }
        } catch (IOException | TransformerException e) {
            throw new RuntimeException("XSLT transformation exception: " + e.getMessage(), e);
        }
    }

    /**
     * Transform XML content using XSLT transformation. There are no formatting guarantees with this transformation.
     *
     * @param xmlContent XML content to transform
     * @param xslt       XSLT transformation
     * @return Transformed XML content
     */
    @SuppressWarnings("unused")
    public static Xml.Tag transformTag(@Language("xml") String xmlContent, @Language("xslt") String xslt) {
        try {
            Source xsltSource = new StreamSource(new ByteArrayInputStream(xslt.getBytes()));
            Transformer transformer = TransformerFactory.newInstance().newTransformer(xsltSource);

            Source text = new StreamSource(new ByteArrayInputStream(xmlContent.getBytes()));
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                transformer.transform(text, new StreamResult(baos));
                return Xml.Tag.build(baos.toString());
            }
        } catch (IOException | TransformerException e) {
            throw new RuntimeException("XSLT transformation exception: " + e.getMessage(), e);
        }
    }
}
