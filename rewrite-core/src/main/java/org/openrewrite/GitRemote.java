/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite;

import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.jgit.transport.URIish;
import org.openrewrite.jgit.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

@Value
public class GitRemote {
    Service service;
    String url;
    String origin;
    String path;

    @Nullable
    String organization;

    String repositoryName;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GitRemote gitRemote = (GitRemote) o;
        return service == gitRemote.service &&
               StringUtils.equalsIgnoreCase(url, gitRemote.url) &&
               StringUtils.equalsIgnoreCase(origin, gitRemote.origin) &&
               StringUtils.equalsIgnoreCase(path, gitRemote.path) &&
               StringUtils.equalsIgnoreCase(organization, gitRemote.organization) &&
               StringUtils.equalsIgnoreCase(repositoryName, gitRemote.repositoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service,
                url == null ? null : url.toLowerCase(Locale.ENGLISH),
                origin == null ? null : origin.toLowerCase(Locale.ENGLISH),
                path == null ? null : path.toLowerCase(Locale.ENGLISH),
                organization == null ? null : organization.toLowerCase(Locale.ENGLISH),
                repositoryName == null ? null : repositoryName.toLowerCase(Locale.ENGLISH));
    }

    public enum Service {
        GitHub,
        GitLab,
        Bitbucket,
        BitbucketCloud,
        AzureDevOps,
        Unknown;

        public static Service forName(String serviceName) {
            switch (serviceName.toLowerCase(Locale.ENGLISH).replaceAll("[-_ ]", "")) {
                case "github":
                    return GitHub;
                case "gitlab":
                    return GitLab;
                case "bitbucket":
                    return Bitbucket;
                case "bitbucketcloud":
                    return BitbucketCloud;
                case "azuredevops":
                    return AzureDevOps;
                default:
                    return Unknown;
            }
        }
    }

    public static class Parser {
        private final List<RemoteServer> servers;

        public Parser() {
            servers = new ArrayList<>();
            servers.add(new RemoteServer(Service.GitHub, "github.com", URI.create("https://github.com"), URI.create("ssh://git@github.com")));
            servers.add(new RemoteServer(Service.GitLab, "gitlab.com", URI.create("https://gitlab.com"), URI.create("ssh://git@gitlab.com")));
            servers.add(new RemoteServer(Service.BitbucketCloud, "bitbucket.org", URI.create("https://bitbucket.org"), URI.create("ssh://git@bitbucket.org")));
            servers.add(new RemoteServer(Service.AzureDevOps, "dev.azure.com", URI.create("https://dev.azure.com"), URI.create("ssh://git@ssh.dev.azure.com")));
        }

        private static final Set<String> ALLOWED_PROTOCOLS = new HashSet<>(Arrays.asList("ssh", "http", "https"));

        /**
         * Transform a {@link GitRemote} into a clone url in the form of an {@link URI}
         *
         * @param remote   the previously parsed GitRemote
         * @param protocol the protocol to use. Supported protocols: ssh, http, https
         * @return the clone url
         */
        public URI toUri(GitRemote remote, String protocol) {
            return buildUri(remote.service, remote.origin, remote.path, protocol);
        }

        /**
         * Build a {@link URI} clone url from components, if that protocol is supported (configured) by the matched server
         *
         * @param service  the type of SCM service
         * @param origin   the origin of the SCM service, any protocol will be stripped (and not used for matching)
         * @param path     the path to the repository
         * @param protocol the protocol to use. Supported protocols: ssh, http, https
         * @return the clone URL if it could be created.
         * @throws IllegalArgumentException if the protocol is not supported by the server.
         */
        public URI buildUri(Service service, String origin, String path, String protocol) {
            if (!ALLOWED_PROTOCOLS.contains(protocol)) {
                throw new IllegalArgumentException("Invalid protocol: " + protocol + ". Must be one of: " + ALLOWED_PROTOCOLS);
            }
            URI selectedBaseUrl;
            if (service == Service.Unknown) {
                if (PORT_PATTERN.matcher(origin).find()) {
                    throw new IllegalArgumentException("Unable to determine protocol/port combination for an unregistered origin with a port: " + origin);
                }
                selectedBaseUrl = URI.create(protocol + "://" + stripProtocol(origin));
            } else {
                selectedBaseUrl = servers.stream()
                        .filter(server -> server.allOrigins()
                                .stream()
                                .anyMatch(o -> o.equalsIgnoreCase(stripProtocol(origin)))
                        )
                        .flatMap(server -> server.getUris().stream())
                        .filter(uri -> Parser.normalize(uri).getScheme().equals(protocol))
                        .findFirst()
                        .orElseGet(() -> {
                            URI normalizedUri = Parser.normalize(origin);
                            if (!normalizedUri.getScheme().equals(protocol)) {
                                throw new IllegalArgumentException("Unable to build clone URL. No matching server found that supports " + protocol + " for origin: " + origin);
                            }
                            return normalizedUri;
                        });
            }

            path = path.replaceFirst("^/", "");
            boolean ssh = "ssh".equals(protocol);
            switch (service) {
                case Bitbucket:
                    if (!ssh) {
                        path = "scm/" + path;
                    }
                    break;
                case AzureDevOps:
                    if (ssh) {
                        path = "v3/" + path;
                    } else {
                        path = path.replaceFirst("([^/]+)/([^/]+)/(.*)", "$1/$2/_git/$3");
                    }
                    break;
            }
            if (service != Service.AzureDevOps) {
                path += ".git";
            }
            String maybeSlash = selectedBaseUrl.toString().endsWith("/") ? "" : "/";
            return URI.create(selectedBaseUrl + maybeSlash + path);
        }

        private static String stripProtocol(String origin) {
            return origin.replaceFirst("^\\w+://", "");
        }

        /**
         * Register a remote git server with multiple protocols and ports all matching the same server/origin.
         *
         * @param service       the type of SCM service
         * @param remoteUri     the (main) origin of the server
         * @param alternateUris the alternate origins of the server
         * @return this
         */
        public Parser registerRemote(Service service, URI remoteUri, Collection<URI> alternateUris) {
            URI normalizedUri = normalize(remoteUri.toString());
            String maybePort = maybePort(remoteUri.getPort(), remoteUri.getScheme());
            String origin = normalizedUri.getHost() + maybePort + normalizedUri.getPath();
            List<URI> allUris = new ArrayList<>();
            allUris.add(remoteUri);
            allUris.addAll(alternateUris);
            add(new RemoteServer(service, origin, allUris));
            return this;
        }

        /**
         * Register a remote git server with a single origin with a single (supplied or guessed) protocol.
         * If multiple protocols and/or ports should be supported use {@link #registerRemote(Service, URI, Collection)}
         *
         * @param service the type of SCM service
         * @param origin  the origin of the server
         * @return this
         */
        public Parser registerRemote(Service service, String origin) {
            URI normalizedUri = normalize(origin);
            String maybePort = maybePort(normalizedUri.getPort(), normalizedUri.getScheme());
            String normalizedOrigin = normalizedUri.getHost() + maybePort + normalizedUri.getPath();
            add(new RemoteServer(service, normalizedOrigin, normalize(origin)));
            return this;
        }

        /**
         * Find a registered remote server by an origin.
         *
         * @param origin the origin of the server. Any protocol will be stripped (and not used to match)
         * @return The server if found, or an unknown type server with a normalized url/origin if not found.
         */
        public RemoteServer findRemoteServer(String origin) {
            String strippedOrigin = stripProtocol(origin);
            return servers.stream().filter(server -> server.origin.equalsIgnoreCase(strippedOrigin))
                    .findFirst()
                    .orElseGet(() -> {
                        URI normalizedUri = normalize(strippedOrigin);
                        String normalizedOrigin = normalizedUri.getHost() + maybePort(normalizedUri.getPort(), normalizedUri.getScheme());
                        return new RemoteServer(Service.Unknown, normalizedOrigin, normalizedUri);
                    });
        }

        private void add(RemoteServer server) {
            if (server.service != Service.Unknown || servers.stream().noneMatch(s -> s.origin.equalsIgnoreCase(server.origin))) {
                servers.add(server);
            }
        }

        public GitRemote parse(String url) {
            URI normalizedUri = normalize(url);

            RemoteServerMatch match = matchRemoteServer(normalizedUri);
            String repositoryPath = repositoryPath(match, normalizedUri);

            switch (match.service) {
                case AzureDevOps:
                    if ("ssh.dev.azure.com".equalsIgnoreCase(match.matchedUri.getHost())) {
                        repositoryPath = repositoryPath.replaceFirst("(?i)v3/", "");
                    } else {
                        repositoryPath = repositoryPath.replaceFirst("(?i)/_git/", "/");
                    }
                    break;
                case Bitbucket:
                    if (url.startsWith("http")) {
                        repositoryPath = repositoryPath.replaceFirst("(?i)scm/", "");
                    }
                    break;
            }
            String organization = null;
            String repositoryName;
            if (repositoryPath.contains("/")) {
                organization = repositoryPath.substring(0, repositoryPath.lastIndexOf("/"));
                repositoryName = repositoryPath.substring(repositoryPath.lastIndexOf("/") + 1);
            } else {
                repositoryName = repositoryPath;
            }
            return new GitRemote(match.service, url, match.origin, repositoryPath, organization, repositoryName);
        }

        private @NonNull RemoteServerMatch matchRemoteServer(URI normalizedUri) {
            return servers.stream()
                    .map(server -> server.match(normalizedUri))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> {
                        String[] segments = normalizedUri.getPath().split("/");
                        String origin = normalizedUri.getHost() + maybePort(normalizedUri.getPort(), normalizedUri.getScheme());
                        if (segments.length > 2) {
                            origin += Arrays.stream(segments, 0, segments.length - 2).collect(joining("/"));
                        }
                        return new RemoteServerMatch(Service.Unknown, origin, URI.create(normalizedUri.getScheme() + "://" + origin));
                    });
        }

        private String repositoryPath(RemoteServerMatch match, URI normalizedUri) {
            URI origin = match.matchedUri;
            String uri = normalizedUri.toString();
            String contextPath = origin.getPath();
            String path = normalizedUri.getPath();
            if (!normalizedUri.getHost().equalsIgnoreCase(origin.getHost()) ||
                normalizedUri.getPort() != origin.getPort() ||
                !path.toLowerCase(Locale.ENGLISH).startsWith(contextPath.toLowerCase(Locale.ENGLISH))) {
                throw new IllegalArgumentException("Origin: " + origin + " does not match the clone url: " + uri);
            }
            return path.substring(contextPath.length())
                    .replaceFirst("^/", "");
        }

        private static final Pattern PORT_PATTERN = Pattern.compile(":\\d+(/.+)(/.+)+");

        static URI normalize(URI url) {
            return normalize(url.toString());
        }

        static URI normalize(String url) {
            try {
                URIish uri = new URIish(url);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (host == null) {
                    if (scheme == null) {
                        if (url.contains(":")) {
                            // Looks like SCP style url
                            scheme = "ssh";
                        } else {
                            scheme = "https";
                        }
                        uri = new URIish(scheme + "://" + url);
                        host = uri.getHost();
                    } else if (!"file".equals(scheme)) {
                        throw new IllegalStateException("No host found in URL " + url);
                    }
                }
                if (scheme == null) {
                    if (PORT_PATTERN.matcher(url).find()) {
                        throw new IllegalArgumentException("Unable to normalize URL: Specifying a port without a scheme is not supported for URL " + url);
                    }
                    if (url.contains(":")) {
                        // Looks like SCP style url
                        scheme = "ssh";
                    } else {
                        scheme = "https";
                    }
                }
                String maybePort = maybePort(uri.getPort(), scheme);

                String path = uri.getPath().replaceFirst("/$", "")
                        .replaceFirst("(?i)\\.git$", "")
                        .replaceFirst("^/", "");
                return URI.create((scheme + "://" + host + maybePort + "/" + path.replace(" ", "%20")).replaceFirst("/$", ""));
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Unable to parse origin from: " + url, e);
            }
        }

        @Value
        private static class RemoteServerMatch {
            Service service;
            String origin;
            URI matchedUri;
        }

        private static String maybePort(int port, String scheme) {
            if (isDefaultPort(port, scheme))
                return "";
            return ":" + port;
        }

        private static boolean isDefaultPort(int port, String scheme) {
            return port < 1 ||
                   ("https".equals(scheme) && port == 443) ||
                   ("http".equals(scheme) && port == 80) ||
                   ("ssh".equals(scheme) && port == 22);
        }
    }

    @Value
    public static class RemoteServer {
        Service service;
        String origin;
        List<URI> uris = new ArrayList<>();

        public RemoteServer(Service service, String origin, URI... uris) {
            this(service, origin, Arrays.asList(uris));
        }

        public RemoteServer(Service service, String origin, Collection<URI> uris) {
            this.service = service;
            this.origin = origin;
            this.uris.addAll(uris);
        }

        private GitRemote.Parser.@Nullable RemoteServerMatch match(URI normalizedUri) {
            String lowerCaseNormalizedUri = normalizedUri.toString().toLowerCase(Locale.ENGLISH);
            for (URI uri : uris) {
                String normalizedServerUri = Parser.normalize(uri.toString()).toString().toLowerCase(Locale.ENGLISH);
                if (lowerCaseNormalizedUri.startsWith(normalizedServerUri)) {
                    return new Parser.RemoteServerMatch(service, origin, uri);
                }
            }
            return null;
        }

        public Set<String> allOrigins() {
            Set<String> origins = new LinkedHashSet<>();
            origins.add(origin);
            for (URI uri : uris) {
                URI normalized = Parser.normalize(uri);
                origins.add(Parser.stripProtocol(normalized.toString()));
            }
            return origins;
        }

    }
}
