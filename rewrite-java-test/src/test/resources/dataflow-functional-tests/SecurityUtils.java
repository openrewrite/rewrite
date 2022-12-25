/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal;

import com.amazonaws.serverless.proxy.model.ContainerConfig;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This class contains utility methods to address FSB security issues found in the application, such as string sanitization
 * and file path validation.
 */
public final class SecurityUtils {
    private static Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    private static Set<String> SCHEMES = new HashSet<String>() {{
        add("http");
        add("https");
        add("HTTP");
        add("HTTPS");
    }};

    private static Set<Integer> PORTS = new HashSet<Integer>() {{
        add(443);
        add(80);
        add(3000); // we allow port 3000 for SAM local
    }};

    public static boolean isValidPort(String port) {
        if (port == null) {
            return false;
        }
        try {
            int intPort = Integer.parseInt(port);
            return PORTS.contains(intPort);
        } catch (NumberFormatException e) {
            log.error("Invalid port parameter: " + crlf(port));
            return false;
        }
    }

    public static boolean isValidScheme(String scheme) {
        return SCHEMES.contains(scheme);
    }

    public static boolean isValidHost(String host, String apiId, String region) {
        if (host == null) {
            return false;
        }
        if (host.endsWith(".amazonaws.com")) {
            String defaultHost = new StringBuilder().append(apiId)
                    .append(".execute-api.")
                    .append(region)
                    .append(".amazonaws.com").toString();
            return host.equals(defaultHost);
        } else {
            return LambdaContainerHandler.getContainerConfig().getCustomDomainNames().contains(host);
        }
    }

    /**
     * Replaces CRLF characters in a string with empty string ("").
     * @param s The string to be cleaned
     * @return A copy of the original string without CRLF characters
     */
    public static String crlf(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[\r\n]", "");
    }


    /**
     * Escapes all special characters in a java string
     * @param s The string to be cleaned
     * @return The escaped string
     */
    public static String encode(String s) {
        if (s == null) {
            return null;
        }

        int sz = s.length();

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < sz; i++) {
            char ch = s.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                buffer.append("\\u" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
            } else if (ch > 0xff) {
                buffer.append("\\u0" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
            } else if (ch > 0x7f) {
                buffer.append("\\u00" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        buffer.append('\\');
                        buffer.append('b');
                        break;
                    case '\n':
                        buffer.append('\\');
                        buffer.append('n');
                        break;
                    case '\t':
                        buffer.append('\\');
                        buffer.append('t');
                        break;
                    case '\f':
                        buffer.append('\\');
                        buffer.append('f');
                        break;
                    case '\r':
                        buffer.append('\\');
                        buffer.append('r');
                        break;
                    default:
                        if (ch > 0xf) {
                            buffer.append("\\u00" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
                        } else {
                            buffer.append("\\u000" + Integer.toHexString(ch).toUpperCase(Locale.ENGLISH));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':

                        buffer.append('\'');
                        break;
                    case '"':
                        buffer.append('\\');
                        buffer.append('"');
                        break;
                    case '\\':
                        buffer.append('\\');
                        buffer.append('\\');
                        break;
                    case '/':
                        buffer.append('/');
                        break;
                    default:
                        buffer.append(ch);
                        break;
                }
            }
        }

        return buffer.toString();
    }

    public static String getValidFilePath(String inputPath) {
        return getValidFilePath(inputPath, false);
    }

    /**
     * Returns an absolute file path given an input path and validates that it is not trying
     * to write/read from a directory other than /tmp.
     *
     * We suppress the path traversal warnings because this method is used to validate paths passed
     * to the servlet implementation methods such as {@link com.amazonaws.serverless.proxy.internal.servlet.AwsProxyRequestPart#write(String)}.
     * Using relative paths is a valid use-case for developers implementing a servlet-based API. We
     * mitigate the potential partial path traversal by checking the resulting absolute path against
     * the list of allowed paths specified in {@link ContainerConfig#getValidFilePaths()}. We also
     * block the /var/task directory regardless.
     *
     * @param inputPath The input path
     * @return The absolute path to the file
     * @throws IllegalArgumentException If the given path is not valid or outside of /tmp
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static String getValidFilePath(final String inputPath, boolean isWrite) {
        if (inputPath == null || "".equals(inputPath.trim())) {
            return null;
        }
        String testInputPath = inputPath;
        if (testInputPath.startsWith("file://")) {
            testInputPath = testInputPath.substring(6);
        }

        File f = new File(testInputPath);
        try {
            String canonicalPath = f.getCanonicalPath();

            if (isWrite && canonicalPath.startsWith("/var/task")) {
                throw new IllegalArgumentException("Trying to write to /var/task folder");
            }

            boolean isAllowed = false;
            for (String allowedPath : LambdaContainerHandler.getContainerConfig().getValidFilePaths()) {
                if (canonicalPath.startsWith(allowedPath)) {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed) {
                throw new IllegalArgumentException("File path not allowed: " + encode(canonicalPath));
            }

            return (inputPath.startsWith("file://") ? "file://" + canonicalPath : canonicalPath);
        } catch (IOException e) {
            log.error("Invalid file path: {}", encode(testInputPath));
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }
}
