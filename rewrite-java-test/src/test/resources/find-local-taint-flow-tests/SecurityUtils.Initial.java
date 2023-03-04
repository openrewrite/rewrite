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

import java.util.Locale;

public class SecurityUtils {

    private static StringBuffer source() {
        return null;
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

        StringBuffer buffer = source();
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
}
