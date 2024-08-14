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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.jgit.transport.URIish;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
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

    public URI toUri(boolean ssh) {
        return buildRemoteUrl(service, ssh, origin, path);
    }

    public static URI buildRemoteUrl(Service service, boolean ssh, String origin, String path) {
        StringBuilder url = new StringBuilder();
        path = path.replaceFirst("^/", "").replaceFirst("/$", "");
        origin = origin.replaceFirst("/$", "");
        if (service == Service.Bitbucket && !ssh) {
            path = "scm/" + path;
        }
        if (service == Service.AzureDevOps) {
            if (ssh) {
                origin = "ssh." + origin;
                path = "v3/" + path;
            } else {
                path = path.replaceFirst("([^/]+)/([^/]+)/(.*)", "$1/$2/_git/$3");
            }
        } else if (service == Service.GitLab || ssh) {
            path += ".git";
        }
        if (origin.startsWith("https://") || origin.startsWith("http://") || origin.startsWith("ssh://")) {
            origin = origin.substring(origin.indexOf("://") + 3);
        }
        if (ssh) {
            url.append("ssh://git@");
        } else {
            url.append("https://");
        }
        url.append(origin).append("/").append(path);
        return URI.create(url.toString());
    }

    public static class Parser {
        private final Map<String, Service> origins;

        public Parser() {
            origins = new LinkedHashMap<>();
            origins.put("github.com", Service.GitHub);
            origins.put("gitlab.com", Service.GitLab);
            origins.put("bitbucket.org", Service.BitbucketCloud);
            origins.put("dev.azure.com", Service.AzureDevOps);
            origins.put("ssh.dev.azure.com", Service.AzureDevOps);
        }

        public Parser registerRemote(Service service, String origin) {
            if (origin.startsWith("https://") || origin.startsWith("http://") || origin.startsWith("ssh://")) {
                origin = new Parser.HostAndPath(origin).concat();
            }
            if (origin.contains("@")) {
                origin = new Parser.HostAndPath("https://" + origin).concat();
            }
            if (service == Service.Unknown) {
                // Do not override a known with an unknown service
                origins.putIfAbsent(origin, service);
            } else {
                origins.put(origin, service);
            }
            return this;
        }

        public GitRemote parse(String url) {
            Parser.HostAndPath hostAndPath = new Parser.HostAndPath(url);

            String origin = hostAndPath.host;
            if (hostAndPath.port > 0) {
                origin = origin + ':' + hostAndPath.port;
            }
            Service service = origins.get(origin);
            if (service == null) {
                for (String maybeOrigin : origins.keySet()) {
                    if (hostAndPath.concat().startsWith(maybeOrigin)) {
                        service = origins.get(maybeOrigin);
                        origin = maybeOrigin;
                        break;
                    }
                }
            }

            if (service == null) {
                // If we cannot find a service, we assume the last 2 path segments are the organization and repository name
                service = Service.Unknown;
                String hostPath = hostAndPath.concat();
                String[] segments = hostPath.split("/");
                if (segments.length <= 2) {
                    origin = null;
                } else {
                    origin = Arrays.stream(segments, 0, segments.length - 2).collect(Collectors.joining("/"));
                }
            }

            String repositoryPath = hostAndPath.repositoryPath(origin);

            switch (service) {
                case AzureDevOps:
                    if (origin.equals("ssh.dev.azure.com")) {
                        origin = "dev.azure.com";
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
            return new GitRemote(service, url, origin, repositoryPath, organization, repositoryName);
        }

        private static class HostAndPath {
            String scheme;
            String host;
            int port;
            String path;

            public HostAndPath(String url) {
                try {
                    URIish uri = new URIish(url);
                    scheme = uri.getScheme();
                    host = uri.getHost();
                    port = uri.getPort();
                    if (host == null && !"file".equals(scheme)) {
                        throw new IllegalStateException("No host in url: " + url);
                    }
                    path = uri.getPath().replaceFirst("/$", "")
                            .replaceFirst(".git$", "")
                            .replaceFirst("^/", "");
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Unable to parse origin from: " + url, e);
                }
            }

            private String concat() {
                StringBuilder builder = new StringBuilder(64);
                if (host != null) {
                    builder.append(host);
                }
                if (!isDefaultPort()) {
                    builder.append(':').append(port);
                }
                if (!path.isEmpty()) {
                    if (builder.length() != 0) {
                        builder.append('/');
                    }
                    builder.append(path);
                }
                return builder.toString();
            }

            private boolean isDefaultPort() {
                return port < 1 ||
                       ("https".equals(scheme) && port == 443) ||
                       ("http".equals(scheme) && port == 80) ||
                       ("ssh".equals(scheme) && port == 22);
            }

            private String repositoryPath(@Nullable String origin) {
                if (origin == null) {
                    origin = "";
                }
                String hostAndPath = concat();
                if (!hostAndPath.startsWith(origin)) {
                    throw new IllegalArgumentException("Unable to find origin '" + origin + "' in '" + hostAndPath + "'");
                }
                return hostAndPath.substring(origin.length()).replaceFirst("^/", "");
            }
        }
    }
}
