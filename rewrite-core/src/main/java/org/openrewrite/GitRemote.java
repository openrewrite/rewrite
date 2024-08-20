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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
public class GitRemote {
    Service service;
    String url;
    String origin;
    String path;

    @Nullable
    String organization;

    String repositoryName;

    public enum Service {
        GitHub,
        GitLab,
        Bitbucket,
        BitbucketCloud,
        AzureDevOps,
        Unknown
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

        public URI toUri(GitRemote remote, boolean ssh) {
            URI selectedBaseUrl;

            if (remote.service == Service.Unknown) {
                if (PORT_PATTERN.matcher(remote.origin).find()) {
                    throw new IllegalArgumentException("Unable to guess protocol port combination for an unregistered origin with a port: " + remote.origin);
                }
                selectedBaseUrl = URI.create((ssh ? "ssh://" : "https://") + stripProtocol(remote.origin));
            } else {
                selectedBaseUrl = servers.stream()
                        .filter(server -> server.allOrigins().contains(stripProtocol(remote.origin)))
                        .flatMap(server -> server.getUris().stream())
                        .filter(uri -> !ssh || uri.getScheme().equals("ssh"))
                        .findFirst()
                        .orElseGet(() -> {
                            URI normalizedUri = Parser.normalize(remote.origin);
                            if (ssh && !normalizedUri.getScheme().equals("ssh")) {
                                throw new IllegalStateException("No matching server found that supports ssh for origin: " + remote.origin);
                            }
                            return normalizedUri;
                        });
            }

            String path = remote.path.replaceFirst("^/", "");
            switch (remote.service) {
                case Bitbucket:
                    if (!ssh) {
                        path = "scm/" + remote.path;
                    }
                    break;
                case AzureDevOps:
                    if (ssh) {
                        path = "v3/" + remote.path;
                    } else {
                        path = remote.path.replaceFirst("([^/]+)/([^/]+)/(.*)", "$1/$2/_git/$3");
                    }
                    break;
            }
            if (remote.service != Service.AzureDevOps) {
                path += ".git";
            }
            String maybeSlash = selectedBaseUrl.toString().endsWith("/") ? "" : "/";
            return URI.create(selectedBaseUrl + maybeSlash + path);
        }

        private static String stripProtocol(String origin) {
            int idx = origin.indexOf("://");
            if (idx < 0) {
                return origin;
            }
            return origin.substring(idx + 3);
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
            add(new RemoteServer(service, origin, normalize(origin)));
            return this;
        }

        private void add(RemoteServer server) {
            if (server.service != Service.Unknown || servers.stream().noneMatch(s -> s.origin.equals(server.origin))) {
                servers.add(server);
            }
        }

        public GitRemote parse(String url) {
            URI normalizedUri = normalize(url);

            RemoteServerMatch match = matchRemoteServer(normalizedUri);

            String repositoryPath = repositoryPath(match, normalizedUri);

            switch (match.service) {
                case AzureDevOps:
                    if (match.matchedUri.getHost().equals("ssh.dev.azure.com")) {
                        repositoryPath = repositoryPath.replaceFirst("v3/", "");
                    } else {
                        repositoryPath = repositoryPath.replaceFirst("/_git/", "/");
                    }
                    break;
                case Bitbucket:
                    if (url.startsWith("http")) {
                        repositoryPath = repositoryPath.replaceFirst("scm/", "");
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
                        String origin1 = normalizedUri.getHost() + maybePort(normalizedUri.getPort(), normalizedUri.getScheme());
                        if (segments.length > 2) {
                            origin1 += Arrays.stream(segments, 0, segments.length - 2).collect(Collectors.joining("/"));
                        }
                        return new RemoteServerMatch(Service.Unknown, origin1, URI.create(normalizedUri.getScheme() + "://" + origin1));
                    });
        }

        private String repositoryPath(RemoteServerMatch match, URI normalizedUri) {
            URI origin = match.matchedUri;
            String uri = normalizedUri.toString();
            String contextPath = origin.getPath();
            String path = normalizedUri.getPath();
            if (!normalizedUri.getHost().equals(origin.getHost()) || normalizedUri.getPort() != origin.getPort() || !path.startsWith(contextPath)) {
                throw new IllegalArgumentException("Origin: " + origin + " does not match the clone url: " + uri);
            }
            return path.substring(contextPath.length())
                    .replaceFirst("^/", "");
        }

        private static final Pattern PORT_PATTERN = Pattern.compile(":\\d+");

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
                        throw new IllegalStateException("No host in url: " + url);
                    }
                }
                if (scheme == null) {
                    if (PORT_PATTERN.matcher(url).find()) {
                        throw new IllegalArgumentException("Unable to normalize URI. Port without a scheme is not supported: " + url);
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
                        .replaceFirst(".git$", "")
                        .replaceFirst("^/", "");
                return URI.create((scheme + "://" + host + maybePort + "/" + path).replaceFirst("/$", ""));
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Unable to parse origin from: " + url, e);
            }
        }

        @Value
        private static class RemoteServer {
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

            @Nullable
            private RemoteServerMatch match(URI normalizedUri) {
                for (URI uri : uris) {
                    if (normalizedUri.toString().startsWith(normalize(uri.toString()).toString())) {
                        return new RemoteServerMatch(service, origin, uri);
                    }
                }
                return null;
            }

            public Set<String> allOrigins() {
                Set<String> origins = new LinkedHashSet<>();
                origins.add(origin);
                for (URI uri : uris) {
                    URI normalized = normalize(uri.toString());
                    origins.add(stripProtocol(normalized.toString()));
                }
                return origins;
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
}
