package org.openrewrite.remote;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Value
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@With
public class RemoteArchive implements Remote {
    @EqualsAndHashCode.Include
    UUID id;

    Path sourcePath;
    Markers markers;
    URI uri;
    Path path;

    /**
     * Any text describing what this remote URI represents. This will only
     * be used to present results to an end user in a way that is more human
     * readable then just a raw URI.
     */
    @Language("markdown")
    String description;

    @Override
    public InputStream getInputStream(HttpSender httpSender) {
        //noinspection resource
        HttpSender.Response response = httpSender.send(httpSender.get(uri.toString()).build());
        InputStream body = response.getBody();
        ZipInputStream zis = new ZipInputStream(body);

        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(path.toString())) {
                    return new InputStream() {
                        @Override
                        public int read() throws IOException {
                            return zis.read();
                        }

                        @Override
                        public void close() throws IOException {
                            zis.closeEntry();
                            zis.close();
                        }
                    };
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load path " + path + " in zip file " + uri, e);
        }
        throw new IllegalArgumentException("Unable to find path " + path + " in zip file " + uri);
    }
}
