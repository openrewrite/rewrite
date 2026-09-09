/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.python.internal.index;

import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.internal.Hashing;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.index.PythonIndexException.Reason;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client for uv {@code format = "flat"} / {@code find-links} index locations: either
 * a local directory of wheels and sdists, or an HTML page of artifact links. Filenames
 * are parsed with {@link DistFilename}; a package absent from the listing yields an
 * empty {@link PackageListing} rather than an error. For local directories the sha256
 * is computed from the file bytes of the requested package's artifacts only; for HTML
 * pages it comes from {@code #sha256=} URL fragments when present.
 */
public class FlatIndexClient {
    private final HttpSender httpSender;
    private final Map<String, PackageListing> cache = new ConcurrentHashMap<>();

    public FlatIndexClient(HttpSender httpSender) {
        this.httpSender = httpSender;
    }

    public PackageListing listFiles(PythonPackageIndex index, String canonicalName) {
        String key = index.getUrl() + "#" + canonicalName;
        PackageListing listing = cache.get(key);
        if (listing == null) {
            listing = fetch(index, canonicalName);
            cache.put(key, listing);
        }
        return listing;
    }

    private PackageListing fetch(PythonPackageIndex index, String canonicalName) {
        String location = index.getUrl();
        if (location.startsWith("http://") || location.startsWith("https://")) {
            return fetchHtml(index, canonicalName);
        }
        Path dir;
        if (location.startsWith("file:")) {
            try {
                dir = Paths.get(URI.create(location));
            } catch (RuntimeException e) {
                throw new PythonIndexException(Reason.UNREACHABLE, location,
                        "Invalid flat index location: " + location, e);
            }
        } else {
            dir = Paths.get(location);
        }
        return listDirectory(dir, location, canonicalName);
    }

    private static PackageListing listDirectory(Path dir, String location, String canonicalName) {
        if (!Files.isDirectory(dir)) {
            throw new PythonIndexException(Reason.UNREACHABLE, location,
                    "Flat index directory does not exist: " + dir);
        }
        List<PackageFile> files = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
            for (Path entry : entries) {
                if (!Files.isRegularFile(entry)) {
                    continue;
                }
                String filename = entry.getFileName().toString();
                DistFilename dist = DistFilename.parse(filename);
                if (dist == null ||
                        !canonicalName.equals(SimpleIndexClient.canonicalName(dist.getDistribution()))) {
                    continue;
                }
                String sha256 = Hashing.sha256Hex(Files.readAllBytes(entry));
                files.add(new PackageFile(filename, entry.toUri().toString(), sha256,
                        null, null, false, null, null));
            }
        } catch (IOException e) {
            throw new PythonIndexException(Reason.UNREACHABLE, location,
                    "Failed to list flat index directory " + dir + ": " + e, e);
        }
        return new PackageListing(canonicalName, files);
    }

    private PackageListing fetchHtml(PythonPackageIndex index, String canonicalName) {
        if (index.isUnresolvedPlaceholders()) {
            throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                    "Index URL contains unresolved environment placeholders: " + index.getUrl());
        }
        String pageUrl = Urls.stripUserinfo(index.getUrl());
        HttpSender.Request request;
        try {
            request = httpSender.get(pageUrl)
                    .withHeader("Accept", "text/html")
                    .withBasicAuthentication(index.getUsername(), index.getPassword())
                    .build();
        } catch (Exception e) {
            throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                    "Invalid flat index URL: " + pageUrl, e);
        }
        try (HttpSender.Response response = httpSender.send(request)) {
            int code = response.getCode();
            if (code == 401 || code == 403) {
                throw new PythonIndexException(Reason.AUTH_FAILED, index.getUrl(),
                        "HTTP " + code + " from " + pageUrl);
            }
            if (!response.isSuccessful()) {
                // the page is the whole index, so any failure is the index being unreachable
                throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                        "HTTP " + code + " from " + pageUrl);
            }
            String html = new String(response.getBodyAsBytes(), StandardCharsets.UTF_8);
            PackageListing all = SimpleIndexClient.parseHtml(canonicalName, pageUrl, html);
            List<PackageFile> files = new ArrayList<>();
            for (PackageFile file : all.getFiles()) {
                DistFilename dist = DistFilename.parse(file.getFilename());
                if (dist != null &&
                        canonicalName.equals(SimpleIndexClient.canonicalName(dist.getDistribution()))) {
                    files.add(file);
                }
            }
            return new PackageListing(canonicalName, files);
        } catch (PythonIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                    "Failed to list " + canonicalName + " from " + pageUrl + ": " + e, e);
        }
    }
}
