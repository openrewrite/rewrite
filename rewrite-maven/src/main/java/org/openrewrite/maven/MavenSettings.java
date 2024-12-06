/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.maven.internal.RawRepositories;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ProfileActivation;

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
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@AllArgsConstructor
@JacksonXmlRootElement(localName = "settings")
public class MavenSettings {
    @Nullable
    String localRepository;

    @Nullable
    @NonFinal
    @JsonIgnore
    MavenRepository mavenLocal;

    @Nullable
    Profiles profiles;

    @Nullable
    ActiveProfiles activeProfiles;

    @Nullable
    Mirrors mirrors;

    @Nullable
    @With
    Servers servers;

    @JsonCreator
    public MavenSettings(@Nullable String localRepository, @Nullable Profiles profiles,
                         @Nullable ActiveProfiles activeProfiles, @Nullable Mirrors mirrors,
                         @Nullable Servers servers) {
        this.localRepository = localRepository;
        this.profiles = profiles;
        this.activeProfiles = activeProfiles;
        this.mirrors = mirrors;
        this.servers = servers;
    }

    public static @Nullable MavenSettings parse(Parser.Input source, ExecutionContext ctx) {
        try {
            return new Interpolator().interpolate(
                    MavenXmlMapper.readMapper().readValue(source.getSource(ctx), MavenSettings.class));
        } catch (IOException e) {
            ctx.getOnError().accept(new IOException("Failed to parse " + source.getPath(), e));
            return null;
        }
    }

    public static @Nullable MavenSettings parse(Path settingsPath, ExecutionContext ctx) {
        return parse(new Parser.Input(settingsPath, () -> {
            try {
                return Files.newInputStream(settingsPath);
            } catch (IOException e) {
                ctx.getOnError().accept(new IOException("Failed to read settings.xml at " + settingsPath, e));
                return null;
            }
        }), ctx);
    }

    public static @Nullable MavenSettings readMavenSettingsFromDisk(ExecutionContext ctx) {
        final Optional<MavenSettings> userSettings = Optional.of(userSettingsPath())
                .filter(MavenSettings::exists)
                .map(path -> parse(path, ctx));
        final MavenSettings installSettings = findMavenHomeSettings().map(path -> parse(path, ctx)).orElse(null);
        return userSettings.map(mavenSettings -> mavenSettings.merge(installSettings))
                .orElse(installSettings);
    }

    private byte[] extractPassword(@NotNull String pwd) {
        Pattern pattern = Pattern.compile(".*?[^\\\\]?\\{(.*?)}.*");
        Matcher matcher = pattern.matcher(pwd);
        if (matcher.find()) {
            return Base64.getDecoder().decode(matcher.group(1));
        }
        return pwd.getBytes(StandardCharsets.UTF_8);
    }

    private @Nullable String decrypt(@Nullable String fieldValue, @Nullable String password) {
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
            byte[] decryptedBytes = new byte[clearBytes.length - paddingLength];
            System.arraycopy(clearBytes, 0, decryptedBytes, 0, decryptedBytes.length);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException |
                 InvalidKeyException | InvalidAlgorithmParameterException e) {
            return null;
        }
    }

    private void updateLocal(@Nullable String masterPassword) {
        if (mavenLocal == null || masterPassword == null) {
            return;
        }

        String password = decrypt(mavenLocal.getPassword(), masterPassword);
        if (password != null) {
            mavenLocal = mavenLocal.withPassword(password);
        }
    }

    private void updateServers(@Nullable String masterPassword) {
        if (servers == null || masterPassword == null) {
            return;
        }

        List<Server> newServers = new ArrayList<>();
        for (Server server : servers.servers ) {
            String password = decrypt(server.getPassword(), masterPassword);
            if (password != null) {
                server = server.withPassword(password);
            }
            newServers.add(server);
        }

        servers.servers = newServers;
    }

    public void updatePassword(ExecutionContext ctx) {
        MavenSecuritySettings security = MavenSecuritySettings.readMavenSecuritySettingsFromDisk(ctx);
        if (security == null) {
            return;
        }

        String decryptedMasterPassword = decrypt(security.getMaster(), "settings.security");

        updateLocal(decryptedMasterPassword);
        updateServers(decryptedMasterPassword);
    }

    public static boolean readFromDiskEnabled() {
        final String propertyValue = System.getProperty("org.openrewrite.test.readMavenSettingsFromDisk");
        return propertyValue != null && !propertyValue.equalsIgnoreCase("false");
    }

    private static Path userSettingsPath() {
        return Paths.get(System.getProperty("user.home")).resolve(".m2/settings.xml");
    }

    private static Optional<Path> findMavenHomeSettings() {
        for (String envVariable : Arrays.asList("MVN_HOME", "M2_HOME", "MAVEN_HOME")) {
            for (String s : Optional.ofNullable(System.getenv(envVariable)).map(Arrays::asList).orElse(emptyList())) {
                Path resolve = Paths.get(s).resolve("conf/settings.xml");
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

    public MavenSettings merge(@Nullable MavenSettings installSettings) {
        return installSettings == null ? this : new MavenSettings(
                localRepository == null ? installSettings.localRepository : localRepository,
                profiles == null ? installSettings.profiles : profiles.merge(installSettings.profiles),
                activeProfiles == null ? installSettings.activeProfiles : activeProfiles.merge(installSettings.activeProfiles),
                mirrors == null ? installSettings.mirrors : mirrors.merge(installSettings.mirrors),
                servers == null ? installSettings.servers : servers.merge(installSettings.servers)
        );
    }

    public List<RawRepositories.Repository> getActiveRepositories(Iterable<String> activeProfiles) {
        LinkedHashMap<String, RawRepositories.Repository> activeRepositories = new LinkedHashMap<>();

        if (profiles != null) {
            for (Profile profile : profiles.getProfiles()) {
                if (profile.isActive(activeProfiles) || (this.activeProfiles != null &&
                                                         profile.isActive(this.activeProfiles.getActiveProfiles()))) {
                    if (profile.repositories != null) {
                        for (RawRepositories.Repository repository : profile.repositories.getRepositories()) {
                            activeRepositories.put(repository.getId(), repository);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(activeRepositories.values());
    }

    public MavenRepository getMavenLocal() {
        if (localRepository == null) {
            return MAVEN_LOCAL_DEFAULT;
        }
        if (mavenLocal == null) {
            mavenLocal = MavenRepository.builder().id("local").uri(asUriString(localRepository)).knownToExist(true).build();
        }
        return mavenLocal;
    }

    private static String asUriString(final String pathname) {
        return pathname.startsWith("file://") ? pathname : Paths.get(pathname).toUri().toString();
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

        public MavenSettings interpolate(MavenSettings mavenSettings) {
            return new MavenSettings(
                    interpolate(mavenSettings.localRepository),
                    mavenSettings.profiles,
                    interpolate(mavenSettings.activeProfiles),
                    interpolate(mavenSettings.mirrors),
                    interpolate(mavenSettings.servers));
        }

        private @Nullable ActiveProfiles interpolate(@Nullable ActiveProfiles activeProfiles) {
            if (activeProfiles == null) return null;
            return new ActiveProfiles(ListUtils.map(activeProfiles.getActiveProfiles(), this::interpolate));
        }

        private @Nullable Mirrors interpolate(@Nullable Mirrors mirrors) {
            if (mirrors == null) return null;
            return new Mirrors(ListUtils.map(mirrors.getMirrors(), this::interpolate));
        }

        private Mirror interpolate(Mirror mirror) {
            return new Mirror(interpolate(mirror.id), interpolate(mirror.url), interpolate(mirror.getMirrorOf()), mirror.releases, mirror.snapshots);
        }

        private @Nullable Servers interpolate(@Nullable Servers servers) {
            if (servers == null) return null;
            return new Servers(ListUtils.map(servers.getServers(), this::interpolate));
        }

        private @Nullable ServerConfiguration interpolate(@Nullable ServerConfiguration configuration) {
            if (configuration == null) {
                return null;
            }
            return new ServerConfiguration(
                    ListUtils.map(configuration.httpHeaders, this::interpolate),
                    configuration.timeout
            );
        }

        private HttpHeader interpolate(HttpHeader httpHeader) {
            return new HttpHeader(interpolate(httpHeader.getName()), interpolate(httpHeader.getValue()));
        }

        private Server interpolate(Server server) {
            return new Server(interpolate(server.id), interpolate(server.username), interpolate(server.password),
                    interpolate(server.configuration));
        }

        private @Nullable String interpolate(@Nullable String s) {
            return s == null ? null : propertyPlaceholders.replacePlaceholders(s, propertyResolver);
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Profiles {
        @JacksonXmlProperty(localName = "profile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Profile> profiles = emptyList();

        public Profiles merge(@Nullable Profiles profiles) {
            final Map<String, Profile> merged = new LinkedHashMap<>();
            for (Profile profile : this.profiles) {
                merged.put(profile.id, profile);
            }
            if (profiles != null) {
                profiles.getProfiles().forEach(profile -> merged.putIfAbsent(profile.getId(), profile));
            }
            return new Profiles(new ArrayList<>(merged.values()));
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActiveProfiles {
        @JacksonXmlProperty(localName = "activeProfile")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<String> activeProfiles = emptyList();

        public ActiveProfiles merge(@Nullable ActiveProfiles activeProfiles) {
            if (activeProfiles == null) {
                return new ActiveProfiles(new ArrayList<>(this.activeProfiles));
            }
            List<String> result = new ArrayList<>();
            Set<String> uniqueValues = new HashSet<>();
            for (String s : ListUtils.concatAll(this.activeProfiles, activeProfiles.activeProfiles)) {
                if (uniqueValues.add(s)) {
                    result.add(s);
                }
            }
            return new ActiveProfiles(result);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Profile {
        @Nullable
        String id;

        @Nullable
        ProfileActivation activation;

        @Nullable
        RawRepositories repositories;

        public boolean isActive(Iterable<String> activeProfiles) {
            return ProfileActivation.isActive(id, activeProfiles, activation);
        }

        @SuppressWarnings("unused")
        public boolean isActive(String... activeProfiles) {
            return isActive(Arrays.asList(activeProfiles));
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Mirrors {
        @JacksonXmlProperty(localName = "mirror")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Mirror> mirrors = emptyList();

        public Mirrors merge(@Nullable Mirrors mirrors) {
            final Map<String, Mirror> merged = new LinkedHashMap<>();
            for (Mirror mirror : this.mirrors) {
                merged.put(mirror.id, mirror);
            }
            if (mirrors != null) {
                mirrors.getMirrors().forEach(mirror -> merged.putIfAbsent(mirror.getId(), mirror));
            }
            return new Mirrors(new ArrayList<>(merged.values()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Mirror {
        @Nullable
        String id;

        @Nullable
        String url;

        @Nullable
        String mirrorOf;

        @Nullable
        Boolean releases;

        @Nullable
        Boolean snapshots;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Servers {
        @JacksonXmlProperty(localName = "server")
        @JacksonXmlElementWrapper(useWrapping = false)
        @With
        List<Server> servers = emptyList();

        public Servers merge(@Nullable Servers servers) {
            final Map<String, Server> merged = new LinkedHashMap<>();
            for (Server server : this.servers) {
                merged.put(server.id, server);
            }
            if (servers != null) {
                servers.getServers().forEach(server -> merged.putIfAbsent(server.getId(), server));
            }
            return new Servers(new ArrayList<>(merged.values()));
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class Server {
        String id;

        String username;
        String password;

        @Nullable
        ServerConfiguration configuration;
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    @JsonIgnoreProperties(value = "httpHeaders")
    public static class ServerConfiguration {
        @JacksonXmlProperty(localName = "property")
        @JacksonXmlElementWrapper(localName = "httpHeaders", useWrapping = true) // wrapping is disabled by default on MavenXmlMapper
        @Nullable
        List<HttpHeader> httpHeaders;

        /**
         * Timeout in milliseconds for reading connecting to and reading from the connection.
         */
        @Nullable
        Long timeout;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    @With
    public static class HttpHeader {
        String name;
        String value;
    }
}
