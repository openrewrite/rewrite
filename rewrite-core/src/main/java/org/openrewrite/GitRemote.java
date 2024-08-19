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
            servers.add(new RemoteServer(Service.GitHub, "github.com", URI.create("https://github.com"), URI.create("ssh://github.com")));
            servers.add(new RemoteServer(Service.GitLab, "gitlab.com", URI.create("https://gitlab.com"), URI.create("ssh://gitlab.com")));
            servers.add(new RemoteServer(Service.BitbucketCloud, "bitbucket.org", URI.create("https://bitbucket.org"), URI.create("ssh://bitbucket.org")));
            servers.add(new RemoteServer(Service.AzureDevOps, "dev.azure.com", URI.create("https://dev.azure.com"), URI.create("ssh://ssh.dev.azure.com")));
        }

        // todo test

        /**
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
         * @param service the type of SCM service
         * @param origin  the origin of the server
         * @return this
         * @deprecated Use {@link #registerRemote(Service, URI, Collection)} instead to configure multiple uris for a single remote server
         */
        @Deprecated
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

            RemoteServerMatch match = servers.stream()
                    .map(server -> server.match(normalizedUri))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> {
                        String[] segments = normalizedUri.getPath().split("/");
                        String origin = normalizedUri.getHost() + maybePort(normalizedUri.getPort(), normalizedUri.getScheme());
                        if (segments.length > 2) {
                            origin += Arrays.stream(segments, 0, segments.length - 2).collect(Collectors.joining("/"));
                        }
                        return new RemoteServerMatch(Service.Unknown, origin, URI.create(normalizedUri.getScheme() + "://" + origin));
                    });

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

        private String repositoryPath(RemoteServerMatch match, URI normalizedUri) {
            String origin = match.matchedUri.toString();
            String uri = normalizedUri.toString();
            if (!uri.startsWith(origin)) {
                throw new IllegalArgumentException("Unable to find origin '" + origin + "' in '" + uri + "'");
            }
            return uri.substring(origin.length()).replaceFirst("^/", "");
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
                return URI.create(scheme + "://" + host + maybePort + "/" + path);
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
                    if (normalizedUri.toString().startsWith(uri.toString())) {
                        return new RemoteServerMatch(service, origin, uri);
                    }
                }
                return null;
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
