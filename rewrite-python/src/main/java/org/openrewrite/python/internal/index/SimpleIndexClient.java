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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.python.internal.index.PythonIndexException.Reason;
import org.openrewrite.python.internal.pep508.Pep508Requirement;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client for the PyPI Simple Repository API, negotiating PEP 691 JSON with a
 * PEP 503 HTML fallback. Listings are cached in memory per (index url, package).
 */
public class SimpleIndexClient {
    private static final String ACCEPT = "application/vnd.pypi.simple.v1+json, text/html;q=0.1";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static final Pattern ANCHOR = Pattern.compile("<a\\s([^>]*)>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z0-9_-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final Pattern YANKED = Pattern.compile("(?:^|\\s)data-yanked(?:[=\\s]|$)", Pattern.CASE_INSENSITIVE);

    private final HttpSender httpSender;
    private final Map<String, PackageListing> cache = new ConcurrentHashMap<>();

    public SimpleIndexClient(HttpSender httpSender) {
        this.httpSender = httpSender;
    }

    /**
     * PEP 503 name canonicalization.
     */
    public static String canonicalName(String name) {
        return Pep508Requirement.canonicalize(name);
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
        if (index.isUnresolvedPlaceholders()) {
            throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                    "Index URL contains unresolved environment placeholders: " + index.getUrl());
        }
        String base = Urls.stripUserinfo(index.getUrl());
        String pageUrl = (base.endsWith("/") ? base : base + "/") + canonicalName + "/";
        HttpSender.Request request;
        try {
            request = httpSender.get(pageUrl)
                    .withHeader("Accept", ACCEPT)
                    .withBasicAuthentication(index.getUsername(), index.getPassword())
                    .build();
        } catch (Exception e) {
            throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(), "Invalid index URL: " + pageUrl, e);
        }
        try (HttpSender.Response response = httpSender.send(request)) {
            int code = response.getCode();
            if (code == 401 || code == 403) {
                throw new PythonIndexException(Reason.AUTH_FAILED, index.getUrl(),
                        "HTTP " + code + " from " + pageUrl);
            }
            if (code == 404) {
                throw new PythonIndexException(Reason.NOT_FOUND, index.getUrl(),
                        "Package " + canonicalName + " not found at " + pageUrl);
            }
            if (!response.isSuccessful()) {
                throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                        "HTTP " + code + " from " + pageUrl);
            }
            byte[] body = response.getBodyAsBytes();
            if (isJson(contentType(response), body)) {
                return parseJson(canonicalName, pageUrl, body);
            }
            return parseHtml(canonicalName, pageUrl, new String(body, StandardCharsets.UTF_8));
        } catch (PythonIndexException e) {
            throw e;
        } catch (Exception e) {
            throw new PythonIndexException(Reason.UNREACHABLE, index.getUrl(),
                    "Failed to list " + canonicalName + " from " + pageUrl + ": " + e, e);
        }
    }

    private static @Nullable String contentType(HttpSender.Response response) {
        for (Map.Entry<String, List<String>> header : response.getHeaders().entrySet()) {
            if (header.getKey() != null && "content-type".equalsIgnoreCase(header.getKey()) &&
                    !header.getValue().isEmpty()) {
                return header.getValue().get(0);
            }
        }
        return null;
    }

    private static boolean isJson(@Nullable String contentType, byte[] body) {
        if (contentType != null) {
            return contentType.toLowerCase(Locale.ROOT).contains("json");
        }
        for (byte b : body) {
            if (!Character.isWhitespace(b)) {
                return b == '{';
            }
        }
        return false;
    }

    private static PackageListing parseJson(String packageName, String pageUrl, byte[] body) throws IOException {
        Pep691Listing listing = MAPPER.readValue(body, Pep691Listing.class);
        List<PackageFile> files = new ArrayList<>();
        if (listing.files != null) {
            for (Pep691File file : listing.files) {
                if (file.filename == null || file.url == null) {
                    continue;
                }
                String sha256 = file.hashes != null ? file.hashes.get("sha256") : null;
                // PEP 714: prefer core-metadata, fall back to the legacy dist-info-metadata key
                JsonNode metadata = file.coreMetadata != null ? file.coreMetadata : file.distInfoMetadata;
                Boolean coreMetadata;
                if (metadata == null || metadata.isNull()) {
                    coreMetadata = null;
                } else if (metadata.isBoolean()) {
                    coreMetadata = metadata.asBoolean();
                } else {
                    // a hash dict also means "available"
                    coreMetadata = metadata.isObject();
                }
                boolean yanked = file.yanked != null &&
                        (file.yanked.isBoolean() ? file.yanked.asBoolean() : file.yanked.isTextual());
                files.add(new PackageFile(file.filename, resolve(pageUrl, file.url), sha256,
                        file.requiresPython, coreMetadata, yanked,
                        file.size, file.uploadTime));
            }
        }
        return new PackageListing(packageName, files);
    }

    /**
     * The slice of a PEP 691 project listing consumed here. {@code core-metadata} /
     * {@code dist-info-metadata} (bool or hash dict, PEP 714) and {@code yanked}
     * (bool or reason string, PEP 592) are genuine unions and stay raw nodes.
     */
    private static class Pep691Listing {
        public @Nullable List<Pep691File> files;
    }

    private static class Pep691File {
        public @Nullable String filename;
        public @Nullable String url;
        public @Nullable Map<String, String> hashes;

        @JsonProperty("requires-python")
        public @Nullable String requiresPython;

        @JsonProperty("core-metadata")
        public @Nullable JsonNode coreMetadata;

        @JsonProperty("dist-info-metadata")
        public @Nullable JsonNode distInfoMetadata;

        public @Nullable JsonNode yanked;
        public @Nullable Long size;

        @JsonProperty("upload-time")
        public @Nullable String uploadTime;
    }

    // package-private so FlatIndexClient can reuse the anchor parsing for flat HTML pages
    static PackageListing parseHtml(String packageName, String pageUrl, String html) {
        List<PackageFile> files = new ArrayList<>();
        Matcher anchor = ANCHOR.matcher(html);
        while (anchor.find()) {
            String attrsText = anchor.group(1);
            String filename = unescapeHtml(anchor.group(2).trim());
            Map<String, String> attrs = new HashMap<>();
            Matcher attribute = ATTRIBUTE.matcher(attrsText);
            while (attribute.find()) {
                String value = attribute.group(2) != null ? attribute.group(2) :
                        attribute.group(3) != null ? attribute.group(3) : attribute.group(4);
                attrs.put(attribute.group(1).toLowerCase(Locale.ROOT), unescapeHtml(value));
            }
            String href = attrs.get("href");
            if (href == null || filename.isEmpty()) {
                continue;
            }
            String sha256 = null;
            int fragment = href.indexOf('#');
            if (fragment >= 0) {
                String frag = href.substring(fragment + 1);
                if (frag.startsWith("sha256=")) {
                    sha256 = frag.substring("sha256=".length());
                }
                href = href.substring(0, fragment);
            }
            String metadata = attrs.containsKey("data-core-metadata") ?
                    attrs.get("data-core-metadata") : attrs.get("data-dist-info-metadata");
            Boolean coreMetadata = metadata == null ? null : !"false".equalsIgnoreCase(metadata);
            // PEP 592: presence of data-yanked means yanked, its value is only the reason
            boolean yanked = YANKED.matcher(attrsText).find();
            files.add(new PackageFile(filename, resolve(pageUrl, href), sha256,
                    attrs.get("data-requires-python"), coreMetadata, yanked, null, null));
        }
        return new PackageListing(packageName, files);
    }

    private static String resolve(String pageUrl, String ref) {
        try {
            return URI.create(pageUrl).resolve(ref).toString();
        } catch (Exception e) {
            return ref;
        }
    }

    private static String unescapeHtml(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '&') {
                sb.append(c);
                continue;
            }
            int semi = s.indexOf(';', i);
            if (semi < 0 || semi - i > 10) {
                sb.append(c);
                continue;
            }
            String entity = s.substring(i + 1, semi);
            String replacement = null;
            try {
                if (entity.startsWith("#x") || entity.startsWith("#X")) {
                    replacement = new String(Character.toChars(Integer.parseInt(entity.substring(2), 16)));
                } else if (entity.startsWith("#")) {
                    replacement = new String(Character.toChars(Integer.parseInt(entity.substring(1))));
                }
            } catch (RuntimeException ignored) {
                // malformed numeric entity stays literal
            }
            if ("amp".equals(entity)) {
                replacement = "&";
            } else if ("lt".equals(entity)) {
                replacement = "<";
            } else if ("gt".equals(entity)) {
                replacement = ">";
            } else if ("quot".equals(entity)) {
                replacement = "\"";
            } else if ("apos".equals(entity)) {
                replacement = "'";
            }
            if (replacement != null) {
                sb.append(replacement);
                i = semi;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
