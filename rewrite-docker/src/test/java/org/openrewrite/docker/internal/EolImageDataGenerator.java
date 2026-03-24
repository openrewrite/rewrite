/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fetches end-of-life data from <a href="https://endoflife.date">endoflife.date</a>
 * and generates {@code eol-images.yaml} for use by {@code FindEndOfLifeImages}.
 * <p>
 * Run via Gradle: {@code ./gradlew :rewrite-docker:syncEolImages}
 */
public class EolImageDataGenerator {

    private static final Path OUTPUT = Paths.get(
            "rewrite-docker/src/main/resources/eol-images.yaml");

    private static final String API_BASE = "https://endoflife.date/api/";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /**
     * Each tracked product with its Docker image name, API product key,
     * and product-specific configuration for tag patterns and replacement suggestions.
     */
    private record TrackedProduct(
            String imageName,
            String apiProduct,
            TagStyle tagStyle,
            ReplacementStrategy replacementStrategy,
            String comment
    ) {}

    private enum TagStyle {
        /** Major version only: "14", "14.*", "14-*" */
        MAJOR,
        /** Major.minor: "3.14", "3.14.*", "3.14-*" */
        MAJOR_MINOR,
        /** Debian codename + version: jessie, "jessie-*", "8", "8.*" */
        DEBIAN,
        /** Ubuntu codename + version: focal, "focal-*", "20.04", "20.04.*" */
        UBUNTU,
        /** CentOS: "7", "7.*" */
        CENTOS
    }

    private enum ReplacementStrategy {
        /** Suggest the two newest non-EOL versions */
        TWO_NEWEST,
        /** Suggest the two newest LTS versions (even-numbered for Node, LTS flag for Java) */
        TWO_NEWEST_LTS,
        /** Suggest CentOS replacements */
        CENTOS_REPLACEMENT,
        /** Suggest Debian trixie */
        DEBIAN_LATEST
    }

    // Debian codename mapping
    private static final Map<String, String> DEBIAN_CODENAMES = new LinkedHashMap<>();
    static {
        DEBIAN_CODENAMES.put("13", "trixie");
        DEBIAN_CODENAMES.put("12", "bookworm");
        DEBIAN_CODENAMES.put("11", "bullseye");
        DEBIAN_CODENAMES.put("10", "buster");
        DEBIAN_CODENAMES.put("9", "stretch");
        DEBIAN_CODENAMES.put("8", "jessie");
        DEBIAN_CODENAMES.put("7", "wheezy");
        DEBIAN_CODENAMES.put("6", "squeeze");
    }

    // Ubuntu codename mapping
    private static final Map<String, String> UBUNTU_CODENAMES = new LinkedHashMap<>();
    static {
        UBUNTU_CODENAMES.put("26.04", "plucky");  // placeholder, name TBD
        UBUNTU_CODENAMES.put("25.10", "questing");
        UBUNTU_CODENAMES.put("25.04", "plucky");
        UBUNTU_CODENAMES.put("24.10", "oracular");
        UBUNTU_CODENAMES.put("24.04", "noble");
        UBUNTU_CODENAMES.put("23.10", "mantic");
        UBUNTU_CODENAMES.put("23.04", "lunar");
        UBUNTU_CODENAMES.put("22.10", "kinetic");
        UBUNTU_CODENAMES.put("22.04", "jammy");
        UBUNTU_CODENAMES.put("21.10", "impish");
        UBUNTU_CODENAMES.put("21.04", "hirsute");
        UBUNTU_CODENAMES.put("20.10", "groovy");
        UBUNTU_CODENAMES.put("20.04", "focal");
        UBUNTU_CODENAMES.put("18.04", "bionic");
        UBUNTU_CODENAMES.put("16.04", "xenial");
        UBUNTU_CODENAMES.put("14.04", "trusty");
    }

    private static final List<TrackedProduct> PRODUCTS = List.of(
            new TrackedProduct("debian", "debian", TagStyle.DEBIAN,
                    ReplacementStrategy.DEBIAN_LATEST, ""),
            new TrackedProduct("ubuntu", "ubuntu", TagStyle.UBUNTU,
                    ReplacementStrategy.TWO_NEWEST_LTS, ""),
            new TrackedProduct("alpine", "alpine-linux", TagStyle.MAJOR_MINOR,
                    ReplacementStrategy.TWO_NEWEST, ""),
            new TrackedProduct("python", "python", TagStyle.MAJOR_MINOR,
                    ReplacementStrategy.TWO_NEWEST, ""),
            new TrackedProduct("node", "nodejs", TagStyle.MAJOR,
                    ReplacementStrategy.TWO_NEWEST_LTS, ""),
            // openjdk and adoptopenjdk are handled as DEPRECATED_IMAGES below (all tags EOL)
            new TrackedProduct("eclipse-temurin", "eclipse-temurin", TagStyle.MAJOR,
                    ReplacementStrategy.TWO_NEWEST_LTS,
                    "# Eclipse Temurin (formerly AdoptOpenJDK) - most common OpenJDK distribution"),
            new TrackedProduct("amazoncorretto", "amazon-corretto", TagStyle.MAJOR,
                    ReplacementStrategy.TWO_NEWEST_LTS, "# Amazon Corretto"),
            new TrackedProduct("azul/zulu-openjdk", "azul-zulu", TagStyle.MAJOR,
                    ReplacementStrategy.TWO_NEWEST_LTS, "# Azul Zulu OpenJDK"),
            new TrackedProduct("centos", "centos", TagStyle.CENTOS,
                    ReplacementStrategy.CENTOS_REPLACEMENT,
                    "# CentOS (all versions EOL - commonly found in legacy Dockerfiles)"),
            new TrackedProduct("nginx", "nginx", TagStyle.MAJOR_MINOR,
                    ReplacementStrategy.TWO_NEWEST, "# nginx"),
            new TrackedProduct("golang", "go", TagStyle.MAJOR_MINOR,
                    ReplacementStrategy.TWO_NEWEST, "# Go (golang)")
    );

    /**
     * Deprecated Docker images where ALL tags are EOL because the image itself is deprecated.
     * These are written as static YAML blocks since there's no meaningful API to fetch.
     */
    private static String deprecatedImageEntries(String temReplacement) {
        StringBuilder sb = new StringBuilder();

        // openjdk - deprecated in 2022
        sb.append("# Official OpenJDK image (deprecated in 2022, recommends eclipse-temurin)\n");
        for (int v = 7; v <= 21; v++) {
            String eolDate = switch (v) {
                case 7 -> "2019-07-01";
                case 8, 11, 17, 21 -> "2022-07-01";
                case 9 -> "2018-03-20";
                case 10 -> "2018-09-25";
                case 12 -> "2019-09-17";
                case 13 -> "2020-03-17";
                case 14 -> "2020-09-15";
                case 15 -> "2021-03-16";
                case 16 -> "2021-09-14";
                case 18 -> "2022-09-20";
                case 19 -> "2023-03-21";
                case 20 -> "2023-09-19";
                default -> "2022-07-01";
            };
            String tagExtra = v <= 8 ? ", \"" + v + "u*\"" : ", \"" + v + ".*\"";
            sb.append("- imageName: openjdk\n");
            sb.append("  tagPatterns: [\"").append(v).append("\", \"").append(v).append("-*\"").append(tagExtra).append("]\n");
            sb.append("  eolDate: \"").append(eolDate).append("\"\n");
            sb.append("  suggestedReplacement: ").append(temReplacement).append("\n\n");
        }

        // adoptopenjdk - deprecated in 2021
        sb.append("# AdoptOpenJDK (deprecated in 2021, superseded by eclipse-temurin)\n");
        for (int v : new int[]{8, 9, 10, 11, 12, 13, 14, 15, 16}) {
            String eolDate = switch (v) {
                case 8, 11, 16 -> "2021-08-01";
                case 9 -> "2018-03-20";
                case 10 -> "2018-09-25";
                case 12 -> "2019-09-17";
                case 13 -> "2020-03-17";
                case 14 -> "2020-09-15";
                case 15 -> "2021-03-16";
                default -> "2021-08-01";
            };
            String tagExtra = v <= 8 ? ", \"" + v + "u*\"" : ", \"" + v + ".*\"";
            sb.append("- imageName: adoptopenjdk\n");
            sb.append("  tagPatterns: [\"").append(v).append("\", \"").append(v).append("-*\"").append(tagExtra).append("]\n");
            sb.append("  eolDate: \"").append(eolDate).append("\"\n");
            sb.append("  suggestedReplacement: ").append(temReplacement).append("\n\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        StringBuilder yaml = new StringBuilder();
        yaml.append(HEADER);

        // Compute eclipse-temurin replacement string for deprecated images
        String temReplacement;
        try {
            List<Map<String, Object>> temVersions = fetchVersions("eclipse-temurin");
            List<VersionInfo> temParsed = parseVersions(temVersions);
            List<VersionInfo> temActive = temParsed.stream()
                    .filter(v -> !v.isEol && v.isLts)
                    .sorted(Comparator.reverseOrder())
                    .toList();
            if (temActive.size() >= 2) {
                temReplacement = "eclipse-temurin:" + temActive.get(0).cycle +
                        " or eclipse-temurin:" + temActive.get(1).cycle + " (LTS)";
            } else {
                temReplacement = "eclipse-temurin:21 or eclipse-temurin:17 (LTS)";
            }
        } catch (Exception e) {
            temReplacement = "eclipse-temurin:21 or eclipse-temurin:17 (LTS)";
        }

        for (TrackedProduct product : PRODUCTS) {
            List<Map<String, Object>> versions = fetchVersions(product.apiProduct);
            if (versions.isEmpty()) {
                System.err.println("WARNING: No data returned for " + product.apiProduct);
                continue;
            }

            List<VersionInfo> parsed = parseVersions(versions);
            List<VersionInfo> eolVersions = filterEolVersions(parsed);
            List<VersionInfo> activeVersions = parsed.stream()
                    .filter(v -> !v.isEol)
                    .collect(Collectors.toList());

            if (eolVersions.isEmpty()) {
                continue;
            }

            String replacement = computeReplacement(product, activeVersions, parsed);

            if (!product.comment.isEmpty()) {
                yaml.append(product.comment).append('\n');
            }

            for (VersionInfo v : eolVersions) {
                List<String> tagPatterns = buildTagPatterns(product, v);
                yaml.append("- imageName: ").append(product.imageName).append('\n');
                yaml.append("  tagPatterns: ").append(formatTagPatterns(tagPatterns)).append('\n');
                yaml.append("  eolDate: \"").append(v.eolDate).append("\"\n");
                yaml.append("  suggestedReplacement: ").append(replacement).append('\n');
                yaml.append('\n');
            }
        }

        // Add deprecated Docker images (openjdk, adoptopenjdk)
        yaml.append(deprecatedImageEntries(temReplacement));

        Files.createDirectories(OUTPUT.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT)) {
            writer.write(yaml.toString());
        }

        System.out.println("Generated " + OUTPUT);
    }

    private static List<Map<String, Object>> fetchVersions(String product) throws IOException, InterruptedException {
        String url = API_BASE + product + ".json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("HTTP " + response.statusCode() + " for " + url);
            return Collections.emptyList();
        }
        return JSON.readValue(response.body(), new TypeReference<>() {});
    }

    private record VersionInfo(
            String cycle,
            String eolDate,
            boolean isEol,
            boolean isLts
    ) implements Comparable<VersionInfo> {
        @Override
        public int compareTo(VersionInfo other) {
            return compareVersionStrings(this.cycle, other.cycle);
        }
    }

    private static List<VersionInfo> parseVersions(List<Map<String, Object>> versions) {
        List<VersionInfo> result = new ArrayList<>();
        for (Map<String, Object> v : versions) {
            String cycle = String.valueOf(v.get("cycle"));
            Object eolObj = v.get("eol");
            boolean isEol;
            String eolDate;
            if (eolObj instanceof Boolean) {
                isEol = (Boolean) eolObj;
                eolDate = isEol ? "1970-01-01" : "";
            } else {
                eolDate = String.valueOf(eolObj);
                try {
                    isEol = !LocalDate.parse(eolDate).isAfter(LocalDate.now());
                } catch (Exception e) {
                    isEol = false;
                    eolDate = "";
                }
            }

            boolean isLts = false;
            Object ltsObj = v.get("lts");
            if (ltsObj instanceof Boolean) {
                isLts = (Boolean) ltsObj;
            } else if (ltsObj instanceof String) {
                isLts = !((String) ltsObj).isEmpty();
            }

            if (!eolDate.isEmpty()) {
                result.add(new VersionInfo(cycle, eolDate, isEol, isLts));
            }
        }
        result.sort(VersionInfo::compareTo);
        return result;
    }

    /**
     * Only include EOL versions with a real EOL date from 2015 onward.
     * Older versions predate Docker Hub and produce noise.
     */
    private static final LocalDate MIN_EOL_DATE = LocalDate.of(2015, 1, 1);

    private static List<VersionInfo> filterEolVersions(List<VersionInfo> versions) {
        return versions.stream()
                .filter(v -> v.isEol)
                .filter(v -> {
                    try {
                        return !LocalDate.parse(v.eolDate).isBefore(MIN_EOL_DATE);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    private static String computeReplacement(TrackedProduct product,
                                             List<VersionInfo> activeVersions,
                                             List<VersionInfo> allVersions) {
        return switch (product.replacementStrategy) {
            case TWO_NEWEST -> {
                List<VersionInfo> sorted = new ArrayList<>(activeVersions);
                sorted.sort(Comparator.reverseOrder());
                yield sorted.stream()
                        .limit(2)
                        .map(v -> v.cycle)
                        .collect(Collectors.joining(" or "));
            }
            case TWO_NEWEST_LTS -> {
                // For Ubuntu, LTS versions end in .04
                if ("ubuntu".equals(product.imageName)) {
                    List<VersionInfo> lts = activeVersions.stream()
                            .filter(v -> v.cycle.endsWith(".04"))
                            .sorted(Comparator.reverseOrder())
                            .toList();
                    if (lts.size() >= 2) {
                        String newest = lts.get(0).cycle;
                        String codename = UBUNTU_CODENAMES.getOrDefault(newest, "");
                        yield codename.isEmpty() ? newest : codename + " (" + newest + ")";
                    }
                    if (!lts.isEmpty()) {
                        String newest = lts.get(0).cycle;
                        String codename = UBUNTU_CODENAMES.getOrDefault(newest, "");
                        yield codename.isEmpty() ? newest : codename + " (" + newest + ")";
                    }
                    yield "latest LTS";
                }
                // For Node.js, LTS = even major versions
                if ("node".equals(product.imageName)) {
                    List<VersionInfo> lts = activeVersions.stream()
                            .filter(v -> {
                                try { return Integer.parseInt(v.cycle) % 2 == 0; }
                                catch (NumberFormatException e) { return v.isLts; }
                            })
                            .sorted(Comparator.reverseOrder())
                            .toList();
                    if (lts.size() >= 2) {
                        yield lts.get(0).cycle + " or " + lts.get(1).cycle;
                    }
                    yield lts.isEmpty() ? "latest LTS" : lts.get(0).cycle;
                }
                // For Java distributions, use LTS flag
                List<VersionInfo> lts = activeVersions.stream()
                        .filter(v -> v.isLts)
                        .sorted(Comparator.reverseOrder())
                        .toList();
                if (lts.size() >= 2) {
                    yield lts.get(0).cycle + " or " + lts.get(1).cycle + " (LTS)";
                }
                yield lts.isEmpty() ? "latest LTS" : lts.get(0).cycle + " (LTS)";
            }
            case CENTOS_REPLACEMENT -> "almalinux:9 or rockylinux:9";
            case DEBIAN_LATEST -> {
                // Find the newest non-EOL Debian version
                List<VersionInfo> active = new ArrayList<>(activeVersions);
                active.sort(Comparator.reverseOrder());
                if (!active.isEmpty()) {
                    String ver = active.get(0).cycle;
                    String codename = DEBIAN_CODENAMES.getOrDefault(ver, "");
                    yield codename.isEmpty() ? ver : codename + " (" + ver + ")";
                }
                yield "trixie (13)";
            }
        };
    }

    private static List<String> buildTagPatterns(TrackedProduct product, VersionInfo version) {
        return switch (product.tagStyle) {
            case MAJOR -> List.of(
                    quote(version.cycle),
                    quote(version.cycle + "-*"),
                    quote(version.cycle + ".*")
            );
            case MAJOR_MINOR -> List.of(
                    quote(version.cycle),
                    quote(version.cycle + ".*"),
                    quote(version.cycle + "-*")
            );
            case DEBIAN -> {
                String codename = DEBIAN_CODENAMES.get(version.cycle);
                if (codename != null) {
                    yield List.of(codename, quote(codename + "-*"),
                            quote(version.cycle), quote(version.cycle + ".*"));
                }
                yield List.of(quote(version.cycle), quote(version.cycle + ".*"));
            }
            case UBUNTU -> {
                String codename = UBUNTU_CODENAMES.get(version.cycle);
                if (codename != null) {
                    yield List.of(codename, quote(codename + "-*"),
                            quote(version.cycle), quote(version.cycle + ".*"));
                }
                yield List.of(quote(version.cycle), quote(version.cycle + ".*"));
            }
            case CENTOS -> List.of(
                    quote(version.cycle),
                    quote(version.cycle + ".*")
            );
        };
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    private static String formatTagPatterns(List<String> patterns) {
        return "[" + String.join(", ", patterns) + "]";
    }

    private static int compareVersionStrings(String a, String b) {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        for (int i = 0; i < Math.max(aParts.length, bParts.length); i++) {
            int aVal = i < aParts.length ? parseIntSafe(aParts[i]) : 0;
            int bVal = i < bParts.length ? parseIntSafe(bParts[i]) : 0;
            if (aVal != bVal) {
                return Integer.compare(aVal, bVal);
            }
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final String HEADER = """
            #
            # Copyright 2026 the original author or authors.
            # <p>
            # Licensed under the Apache License, Version 2.0 (the "License");
            # you may not use this file except in compliance with the License.
            # You may obtain a copy of the License at
            # <p>
            # https://www.apache.org/licenses/LICENSE-2.0
            # <p>
            # Unless required by applicable law or agreed to in writing, software
            # distributed under the License is distributed on an "AS IS" BASIS,
            # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            # See the License for the specific language governing permissions and
            # limitations under the License.
            #
            # AUTO-GENERATED by EolImageDataGenerator - do not edit manually.
            # Run: ./gradlew :rewrite-docker:syncEolImages
            #
            # Data source: https://endoflife.date/
            #
            """;
}
