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
package org.openrewrite.maven;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.maven.internal.MavenXmlMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@AllArgsConstructor
@JacksonXmlRootElement(localName = "settingsSecurity")
public class MavenSecuritySettings {

    @Nullable
    String master;

    @Nullable
    String relocation;

    private static @Nullable MavenSecuritySettings parse(Parser.Input source, ExecutionContext ctx) {
        try {
            return new Interpolator().interpolate(
                    MavenXmlMapper.readMapper().readValue(source.getSource(ctx), MavenSecuritySettings.class));
        } catch (IOException e) {
            ctx.getOnError().accept(new IOException("Failed to parse " + source.getPath(), e));
            return null;
        }
    }

    private static @Nullable MavenSecuritySettings parse(Path settingsPath, ExecutionContext ctx) {
        return parse(new Parser.Input(settingsPath, () -> {
            try {
                return Files.newInputStream(settingsPath);
            } catch (IOException e) {
                ctx.getOnError().accept(new IOException("Failed to read settings-security.xml at " + settingsPath, e));
                return null;
            }
        }), ctx);
    }

    public static @Nullable MavenSecuritySettings readMavenSecuritySettingsFromDisk(ExecutionContext ctx) {
        Optional<MavenSecuritySettings> userSettings = Optional.of(userSecuritySettingsPath())
                .filter(MavenSecuritySettings::exists)
                .map(path -> parse(path, ctx));
        MavenSecuritySettings installSettings = findMavenHomeSettings().map(path -> parse(path, ctx)).orElse(null);
        MavenSecuritySettings mergedSettings = userSettings
                .map(mavenSecuritySettings -> mavenSecuritySettings.merge(installSettings))
                .orElse(installSettings);
        if (mergedSettings != null && mergedSettings.relocation != null) {
            return mergedSettings.merge(parse(Paths.get(mergedSettings.relocation), ctx));
        }
        return mergedSettings;
    }

    private static Path userSecuritySettingsPath() {
        return Paths.get(System.getProperty("user.home")).resolve(".m2/settings-security.xml");
    }

    private static Optional<Path> findMavenHomeSettings() {
        for (String envVariable : Arrays.asList("MVN_HOME", "M2_HOME", "MAVEN_HOME")) {
            for (String s : Optional.ofNullable(System.getenv(envVariable)).map(Arrays::asList).orElse(emptyList())) {
                Path resolve = Paths.get(s).resolve("conf/settings-security.xml");
                if (exists(resolve)) {
                    return Optional.of(resolve);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean exists(Path path) {
        try {
            return path.toFile().exists();
        } catch (SecurityException e) {
            return false;
        }
    }

    private MavenSecuritySettings merge(@Nullable MavenSecuritySettings installSettings) {
        return installSettings == null ? this : new MavenSecuritySettings(
                master == null ? installSettings.master : master,
                relocation == null ? installSettings.relocation : relocation
        );
    }

    /**
     * Resolve all properties EXCEPT in the profiles section, which can be affected by
     * the POM using the settings.
     */
    private static class Interpolator {
        private static final PropertyPlaceholderHelper propertyPlaceholders = new PropertyPlaceholderHelper(
                "${", "}", null);

        private static final UnaryOperator<String> propertyResolver = key -> {
            String property = System.getProperty(key);
            if (property != null) {
                return property;
            }
            if (key.startsWith("env.")) {
                return System.getenv().get(key.substring(4));
            }
            return System.getenv().get(key);
        };

        public MavenSecuritySettings interpolate(MavenSecuritySettings mavenSecuritySettings) {
            return new MavenSecuritySettings(
                    interpolate(mavenSecuritySettings.master),
                    interpolate(mavenSecuritySettings.relocation)
            );
        }

        private @Nullable String interpolate(@Nullable String s) {
            return s == null ? null : propertyPlaceholders.replacePlaceholders(s, propertyResolver);
        }
    }

    @Nullable
    String decrypt(@Nullable String fieldValue, @Nullable String password) {
        if (fieldValue == null || fieldValue.isEmpty() || password == null) {
            return null;
        }

        try {
            byte[] encryptedText = extractPassword(fieldValue);

            byte[] salt = new byte[8];
            System.arraycopy(encryptedText, 0, salt, 0, 8);

            int padLength = encryptedText[8];
            byte[] encryptedBytes = new byte[encryptedText.length - 9 - padLength];
            System.arraycopy(encryptedText, 9, encryptedBytes, 0, encryptedBytes.length);

            byte[] keyAndIV = new byte[32];
            byte[] pwdBytes = extractPassword(password);
            int offset = 0;
            while (offset < 32) {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                digest.update(pwdBytes);
                digest.update(salt);
                byte[] hash = digest.digest();
                System.arraycopy(hash, 0, keyAndIV, offset, Math.min(hash.length, 32 - offset));
                offset += hash.length;
            }

            Key key = new SecretKeySpec(keyAndIV, 0, 16, "AES");
            IvParameterSpec iv = new IvParameterSpec(keyAndIV, 16, 16);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] clearBytes = cipher.doFinal(encryptedBytes);

            int paddingLength = clearBytes[clearBytes.length - 1];
            if (paddingLength >= clearBytes.length) {
                return null; // Invalid padding length
            }
            byte[] decryptedBytes = new byte[clearBytes.length - paddingLength];
            System.arraycopy(clearBytes, 0, decryptedBytes, 0, decryptedBytes.length);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException |
                 InvalidKeyException | InvalidAlgorithmParameterException | IllegalArgumentException e) {
            return null;
        }
    }

    private byte[] extractPassword(String pwd) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile(".*?[^\\\\]?\\{(.*?)}.*");
        Matcher matcher = pattern.matcher(pwd);
        if (matcher.find()) {
            return Base64.getDecoder().decode(matcher.group(1));
        }
        return pwd.getBytes(StandardCharsets.UTF_8);
    }
}
