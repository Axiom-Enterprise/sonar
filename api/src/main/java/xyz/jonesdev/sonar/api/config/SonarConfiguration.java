/*
 * Copyright (C) 2025 Sonar Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.jonesdev.sonar.api.config;

import com.alessiodp.libby.Library;
import com.j256.ormlite.db.DatabaseType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.antibot.subnet.SubnetRange;
import xyz.jonesdev.sonar.api.command.SonarCommand;
import xyz.jonesdev.sonar.api.database.ormlite.H2DatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.database.ormlite.MariaDbDatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.database.ormlite.MysqlDatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.database.ormlite.PostgresDatabaseTypeAdapter;
import xyz.jonesdev.sonar.api.webhook.DiscordWebhook;
import xyz.jonesdev.sonar.api.webhook.GenericWebhook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public final class SonarConfiguration {
  private final SimpleYamlConfig generalConfig;
  private final SimpleYamlConfig messagesConfig;
  private final SimpleYamlConfig webhookConfig;
  private final File languageFile;
  private final File pluginFolder;
  private Language language;

  public SonarConfiguration(final @NotNull File pluginFolder) {
    this.pluginFolder = pluginFolder;
    this.messagesConfig = new SimpleYamlConfig(new File(pluginFolder, "messages.yml"));
    this.generalConfig = new SimpleYamlConfig(new File(pluginFolder, "config.yml"));
    this.webhookConfig = new SimpleYamlConfig(new File(pluginFolder, "webhook.yml"));
    this.languageFile = new File(pluginFolder, "language.properties");
  }

  private static final Language DEFAULT_FALLBACK_LANGUAGE = Language.EN;

  private Language getPreferredLanguage() {
    if (!languageFile.exists()) {
      final URL defaultLanguageFile = Sonar.class.getResource("/assets/language.properties");
      // Make sure the file actually exists before trying to copy it
      if (defaultLanguageFile == null) {
        Sonar.get0().getLogger().error("Cannot check for custom language (is the file missing?)");
        return DEFAULT_FALLBACK_LANGUAGE;
      }
      // Copy the file to the plugin data directory
      try (final InputStream inputStream = defaultLanguageFile.openStream()) {
        Files.copy(inputStream, languageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException exception) {
        Sonar.get0().getLogger().error("Error copying file: {}", exception);
        return DEFAULT_FALLBACK_LANGUAGE;
      }
    }

    // Try to read the property from the file
    try (final InputStream input = new FileInputStream(languageFile)) {
      final Properties properties = new Properties();
      properties.load(input);
      final String property = properties.getProperty("language");
      try {
        // Try parsing the property as a language
        return Language.fromCode(property);
      } catch (Throwable throwable) {
        Sonar.get0().getLogger().error("Could not find requested language: {}", throwable);
        Sonar.get0().getLogger().error("You can view a full list of valid language codes here:");
        Sonar.get0().getLogger().error("https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes");
        Sonar.get0().getLogger().error("If a translation does not exist yet, Sonar will use English (en).");
      }
    } catch (IOException exception) {
      Sonar.get0().getLogger().error("Error reading language file: {}", exception);
    }
    return DEFAULT_FALLBACK_LANGUAGE;
  }

  public void load() {
    // Make sure the plugin folder actually exists before trying to copy files into it
    if (!pluginFolder.exists() && !pluginFolder.mkdirs()) {
      throw new IllegalStateException("Could not create plugin folder (insufficient permissions?)");
    }

    // Generate the language file and check what it's set to
    language = getPreferredLanguage();
    if (language == Language.SYSTEM) {
      try {
        // Try using the system language to determine the language file
        final String property = System.getProperty("user.language", "en");
        language = Language.fromCode(property);
        // Make sure the user knows that we're using the system language for translations
        Sonar.get0().getLogger().info("Using system language ({}) for translations.", language);
      } catch (Exception exception) {
        Sonar.get0().getLogger().warn("Could not use system language for translations.");
        Sonar.get0().getLogger().warn("Using default language ({}) for translations.", language);
      }
    } else {
      Sonar.get0().getLogger().info("Using custom language ({}) for translations.", language);
    }

    // Load all configurations
    try {
      generalConfig.load(getAsset("config", language));
      messagesConfig.load(getAsset("messages", language));
      webhookConfig.load(getAsset("webhook", language));
    } catch (Exception exception) {
      throw new IllegalStateException("Error loading configuration", exception);
    }

    // Load all values
    loadValues();
  }

  public void loadValues() {
    // Since we are loading from a file, we don't need to provide default values

    // General settings
    logPlayerAddresses = generalConfig.getBoolean("general.log-player-addresses");
    maxOnlinePerIp = clamp(generalConfig.getInt("general.max-online-per-ip"), -1, 99);
    loadAllowlist();

    // Attack tracker
    minPlayersForAttack = clamp(generalConfig.getInt("attack-tracker.min-players-for-attack"), 2, 1024);
    minAttackDuration = clamp(generalConfig.getInt("attack-tracker.min-attack-duration"), 1000, 900000);
    minAttackThreshold = clamp(generalConfig.getInt("attack-tracker.min-attack-threshold"), 0, 20);
    attackCooldownDelay = clamp(generalConfig.getInt("attack-tracker.attack-cooldown-delay"), 100, 30000);

    // Database
    database.type = Database.Type.valueOf(generalConfig.getString("database.type").toUpperCase());
    database.maximumAge = clamp(generalConfig.getInt("database.maximum-age"), 1, 365);
    database.persistBlacklist = generalConfig.getBoolean("database.persist-blacklist");

    // Queue
    queue.maxQueuePolls = clamp(generalConfig.getInt("queue.max-polls"), 1, 1000);

    // Verification
    verification.timing = Verification.Timing.valueOf(generalConfig.getString("verification.timing"));
    // Warn the user if the verification timing is set to NEVER
    if (verification.timing == Verification.Timing.NEVER) {
      Sonar.get0().getLogger().warn(" ");
      Sonar.get0().getLogger().warn("You have set the verification timing to 'NEVER'.");
      Sonar.get0().getLogger().warn("Sonar will NOT perform the bot verification at all, therefore making it useless.");
      Sonar.get0().getLogger().warn("It is highly suggested to set this option to either 'DURING_ATTACK' or 'ALWAYS'.");
      Sonar.get0().getLogger().warn("Please only edit this option if you really know what you are doing.");
      Sonar.get0().getLogger().warn(" ");
    }

    verification.gravity.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.gravity.timing"));
    verification.gravity.collisionTiming = Verification.Timing.valueOf(generalConfig.getString("verification.checks.collision.timing"));
    verification.gravity.captchaOnFail = generalConfig.getBoolean("verification.checks.gravity.captcha-on-fail");
    verification.gravity.maxMovementTicks = clamp(generalConfig.getInt("verification.checks.gravity.max-movement-ticks"), 2, 100);

    verification.vehicle.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.vehicle.timing"));
    verification.vehicle.minimumPackets = clamp(generalConfig.getInt("verification.checks.vehicle.minimum-packets"), 0, 20);

    verification.map.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.map-captcha.timing"));
    verification.map.precomputeAmount = clamp(generalConfig.getInt("verification.checks.map-captcha.precompute"), 10, 5000);
    verification.map.maxDuration = clamp(generalConfig.getInt("verification.checks.map-captcha.max-duration"), 5000, 360000);
    verification.map.maxTries = clamp(generalConfig.getInt("verification.checks.map-captcha.max-tries"), 1, 100);
    verification.map.alphabet = generalConfig.getString("verification.checks.map-captcha.alphabet");
    verification.map.backgroundImage = null;
    verification.map.placeInOffhand = generalConfig.getBoolean("verification.checks.map-captcha.place-in-offhand");

    final String backgroundPath = generalConfig.getString("verification.checks.map-captcha.background");
    if (!backgroundPath.isEmpty()) {
      final File backgroundFile = new File(pluginFolder, backgroundPath);
      if (backgroundFile.exists()) {
        verification.map.backgroundImage = backgroundFile;
      } else {
        Sonar.get0().getLogger().error("Could not find background image {}", backgroundFile.getAbsolutePath());
      }
    }

    verification.serverListPing.timing = Verification.Timing.valueOf(generalConfig.getString("verification.checks.server-list-ping.timing"));
    verification.serverListPing.maxAge = clamp(generalConfig.getInt("verification.checks.server-list-ping.max-age"), 1000, 3600000);

    verification.brand.enabled = generalConfig.getBoolean("verification.checks.client-brand.enabled");
    verification.brand.validRegex = Pattern.compile(generalConfig.getString("verification.checks.client-brand.valid-regex"));
    verification.brand.maxLength = generalConfig.getInt("verification.checks.client-brand.max-length");

    verification.timeOfDay = clamp(generalConfig.getInt("verification.time-of-day"), 0, 24000);
    verification.gamemode = Verification.Gamemode.valueOf(generalConfig.getString("verification.gamemode"));
    verification.validNameRegex = Pattern.compile(generalConfig.getString("verification.checks.valid-name-regex"));

    verification.checkGeyser = generalConfig.getBoolean("verification.check-geyser-players");
    verification.checkEagler = generalConfig.getBoolean("verification.check-eagler-players");
    verification.logConnections = generalConfig.getBoolean("verification.log-connections");
    verification.logDuringAttack = generalConfig.getBoolean("verification.log-during-attack");
    verification.debugXYZPositions = generalConfig.getBoolean("verification.debug-xyz-positions");
    verification.readTimeout = clamp(generalConfig.getInt("verification.read-timeout"), 1000, 30000);
    verification.writeTimeout = clamp(generalConfig.getInt("verification.write-timeout"), 1000, 30000);
    verification.maxPacketCount = clamp(generalConfig.getInt("verification.max-packets"), 200, 9999);
    verification.reconnectDelay = clamp(generalConfig.getInt("verification.rejoin-delay"), 0, 100000);
    verification.rememberTime = clamp(generalConfig.getInt("verification.remember-time"), 0, 86400000);
    verification.blacklistTime = clamp(generalConfig.getInt("verification.blacklist-time"), 0, 86400000);
    verification.blacklistThreshold = clamp(generalConfig.getInt("verification.blacklist-threshold"), 0, 100);
    verification.blacklistIpv4Prefix = clamp(generalConfig.getInt("verification.blacklist-ipv4-prefix"), 8, 32);
    verification.blacklistIpv6Prefix = clamp(generalConfig.getInt("verification.blacklist-ipv6-prefix"), 32, 128);
    verification.blacklistedProtocols.clear();
    verification.blacklistedProtocols.addAll(generalConfig.getIntList("verification.blacklisted-protocols"));

    // Webhooks
    webhook.url = webhookConfig.getString("webhook.url");
    webhook.username = webhookConfig.getString("webhook.username");
    webhook.avatarUrl = webhookConfig.getString("webhook.avatar-url");
    webhook.content = webhookConfig.getString("webhook.content");
    webhook.footer.text = webhookConfig.getString("webhook.embed.footer.text");
    webhook.footer.iconUrl = webhookConfig.getString("webhook.embed.footer.icon-url");
    webhook.embed.title = webhookConfig.getString("webhook.embed.title");
    webhook.embed.titleUrl = webhookConfig.getString("webhook.embed.title-url");
    webhook.embed.description = String.join("\n", webhookConfig.getStringList("webhook.embed.description"));
    webhook.embed.r = webhookConfig.getInt("webhook.embed.color.red");
    webhook.embed.g = webhookConfig.getInt("webhook.embed.color.green");
    webhook.embed.b = webhookConfig.getInt("webhook.embed.color.blue");
    webhook.startEmbedEnabled = webhookConfig.getBoolean("webhook.attack-start-embed.enabled");
    webhook.startEmbed.title = webhookConfig.getString("webhook.attack-start-embed.title");
    webhook.startEmbed.titleUrl = webhookConfig.getString("webhook.attack-start-embed.title-url");
    webhook.startEmbed.description = String.join("\n", webhookConfig.getStringList("webhook.attack-start-embed.description"));
    webhook.startEmbed.r = webhookConfig.getInt("webhook.attack-start-embed.color.red");
    webhook.startEmbed.g = webhookConfig.getInt("webhook.attack-start-embed.color.green");
    webhook.startEmbed.b = webhookConfig.getInt("webhook.attack-start-embed.color.blue");

    final String genericWebhookUrl = webhookConfig.getString("generic-webhook.url").trim();
    webhook.genericWebhook = genericWebhookUrl.isEmpty() ? null : new GenericWebhook(genericWebhookUrl);

    if (!webhook.url.isEmpty()) {
      if (webhook.username.isEmpty()) {
        throw new IllegalStateException("Webhook username cannot be empty");
      }
      webhook.discordWebhook = new DiscordWebhook(webhook.url);
    } else if (webhook.discordWebhook != null) {
      // Reset if Discord webhooks were disabled
      webhook.discordWebhook = null;
    }

    // Messages
    prefix = MiniMessage.miniMessage().deserialize(messagesConfig.getString("prefix"));

    final String noPermissionMessage = messagesConfig.getString("commands.no-permission");
    if (noPermissionMessage.isEmpty()) {
      noPermission = null;
    } else {
      noPermission = MiniMessage.miniMessage().deserialize(noPermissionMessage,
        Placeholder.component("prefix", prefix));
    }

    SonarCommand.prepareCachedTabSuggestions();

    supportUrl = messagesConfig.getString("support-url")
      .replace("jonesdev.xyz", "sonar.top") // Migration: Replace legacy URL
      .trim();
    if (supportUrl.contains("://sonar.top/")) {
      Sonar.get0().getLogger().warn("Consider entering a custom support URL (if applicable).");
    }
    header = MiniMessage.miniMessage().deserialize(
      String.join("<newline>", messagesConfig.getStringList("header")),
      Placeholder.unparsed("support-url", supportUrl),
      Placeholder.component("prefix", prefix));
    footer = MiniMessage.miniMessage().deserialize(
      String.join("<newline>", messagesConfig.getStringList("footer")),
      Placeholder.unparsed("support-url", supportUrl),
      Placeholder.component("prefix", prefix));

    tooManyOnlinePerIp = deserializeDisconnectMessage("too-many-online-per-ip");
    verification.currentlyPreparing = deserializeDisconnectMessage("verification.currently-preparing");
    verification.unsupportedVersion = deserializeDisconnectMessage("verification.unsupported-version");
    verification.tooFastReconnect = deserializeDisconnectMessage("verification.too-fast-reconnect");
    verification.alreadyVerifying = deserializeDisconnectMessage("verification.already-verifying");
    verification.alreadyQueued = deserializeDisconnectMessage("verification.already-queued");
    verification.blacklisted = deserializeDisconnectMessage("verification.blacklisted");
    verification.invalidUsername = deserializeDisconnectMessage("verification.invalid-username");
    verification.protocolBlacklisted = deserializeDisconnectMessage("verification.blacklisted-protocol");
    verification.verificationSuccess = deserializeDisconnectMessage("verification.success");
    verification.verificationFailed = deserializeDisconnectMessage("verification.failed");
    verification.serverListPingRequired = deserializeDisconnectMessage("verification.server-list-ping-required");

    verboseAnimation = Collections.unmodifiableList(messagesConfig.getStringList("verbose.animation"));
  }

  /**
   * Parses all allowlist entries; invalid entries are skipped and reported in the console.
   */
  private void loadAllowlist() {
    final List<SubnetRange> ranges = new ArrayList<>();
    for (final String raw : generalConfig.getStringList("general.allowlist")) {
      final SubnetRange range = SubnetRange.parse(raw);
      if (range == null) {
        Sonar.get0().getLogger().error("Invalid allowlist entry '{}' (expected an IP address or CIDR range)", raw);
        continue;
      }
      // A prefix of 0 matches every address, which would disable the entire protection
      if (range.getPrefixLength() == 0) {
        Sonar.get0().getLogger().warn("The allowlist entry '{}' matches EVERY address and disables Sonar!", raw);
      }
      ranges.add(range);
    }
    allowlist = Collections.unmodifiableList(ranges);
    if (!allowlist.isEmpty()) {
      Sonar.get0().getLogger().info("Loaded {} allowlist entries.", allowlist.size());
    }
  }

  private @NotNull Component deserializeDisconnectMessage(final String path) {
    return MiniMessage.miniMessage().deserialize(
      String.join("<newline>", messagesConfig.getStringList(path)),
      Placeholder.component("prefix", prefix),
      Placeholder.component("header", header),
      Placeholder.component("footer", footer),
      Placeholder.unparsed("support-url", supportUrl));
  }

  private @NotNull URL getAsset(final String url, final @NotNull Language language) {
    final String resourceName = url + "/" + language.getCode() + ".yml";
    URL result = Sonar.class.getResource("/assets/" + resourceName);
    if (result == null) {
      Sonar.get0().getLogger().warn("Could not find " + resourceName + "! Using en.yml!");
      result = Objects.requireNonNull(Sonar.class.getResource("/assets/" + url + "/en.yml"));
    }
    return result;
  }

  private static int clamp(final int v, final int max, final int min) {
    final int output = Math.max(Math.min(v, min), max);
    if (output != v) {
      Sonar.get0().getLogger().warn("Clamped configuration value {} to {}", v, output);
    }
    return output;
  }

  public @NotNull String formatAddress(final @NotNull InetAddress inetAddress) {
    return logPlayerAddresses ? inetAddress.getHostAddress() : "<ip address withheld>";
  }

  private final Queue queue = new Queue();
  private final Verification verification = new Verification();
  private final Database database = new Database();
  private final Webhook webhook = new Webhook();

  private Component prefix;
  private String supportUrl;
  private Component header;
  private Component footer;
  private Component noPermission;
  private boolean logPlayerAddresses;
  private int maxOnlinePerIp;
  private List<SubnetRange> allowlist = Collections.emptyList();
  private int minPlayersForAttack;
  private int minAttackDuration;
  private int minAttackThreshold;
  private int attackCooldownDelay;
  private Component tooManyOnlinePerIp;
  private List<String> verboseAnimation;

  public static final class Queue {
    private int maxQueuePolls;

    public int getMaxQueuePolls() {
      return this.maxQueuePolls;
    }

    private Queue() {
    }
  }

  public static final class Verification {
    private Timing timing;

    public enum Timing {
      ALWAYS("Always"),
      DURING_ATTACK("During Attack"),
      NEVER("Never");

      private final String displayName;

      public String getDisplayName() {
        return this.displayName;
      }

      private Timing(final String displayName) {
        this.displayName = displayName;
      }
    }

    private final Map map = new Map();
    private final Gravity gravity = new Gravity();
    private final Vehicle vehicle = new Vehicle();
    private final Brand brand = new Brand();
    private final ServerListPing serverListPing = new ServerListPing();

    public static final class Map {
      private Timing timing;
      private int precomputeAmount;
      private int maxDuration;
      private int maxTries;
      private String alphabet;
      private File backgroundImage;
      private boolean placeInOffhand;

      public Timing getTiming() {
        return this.timing;
      }

      public int getPrecomputeAmount() {
        return this.precomputeAmount;
      }

      public int getMaxDuration() {
        return this.maxDuration;
      }

      public int getMaxTries() {
        return this.maxTries;
      }

      public String getAlphabet() {
        return this.alphabet;
      }

      public File getBackgroundImage() {
        return this.backgroundImage;
      }

      public boolean isPlaceInOffhand() {
        return this.placeInOffhand;
      }
    }

    public static final class Gravity {
      private Timing timing;
      private Timing collisionTiming;
      private boolean captchaOnFail;
      private int maxMovementTicks;

      public Timing getTiming() {
        return this.timing;
      }

      public Timing getCollisionTiming() {
        return this.collisionTiming;
      }

      public boolean isCaptchaOnFail() {
        return this.captchaOnFail;
      }

      public int getMaxMovementTicks() {
        return this.maxMovementTicks;
      }
    }

    public static final class Vehicle {
      private Timing timing;
      private int minimumPackets;

      public Timing getTiming() {
        return this.timing;
      }

      public int getMinimumPackets() {
        return this.minimumPackets;
      }
    }

    public static final class ServerListPing {
      private Timing timing;
      private int maxAge;

      public Timing getTiming() {
        return this.timing;
      }

      public int getMaxAge() {
        return this.maxAge;
      }
    }

    public static final class Brand {
      private boolean enabled;
      private int maxLength;
      private Pattern validRegex;

      public boolean isEnabled() {
        return this.enabled;
      }

      public int getMaxLength() {
        return this.maxLength;
      }

      public Pattern getValidRegex() {
        return this.validRegex;
      }
    }

    private Gamemode gamemode;

    public enum Gamemode {
      NOT_SET(-1),
      SURVIVAL(0),
      CREATIVE(1),
      ADVENTURE(2);

      private final int id;

      public boolean isSurvivalOrAdventure() {
        return this == SURVIVAL || this == ADVENTURE;
      }

      public int getId() {
        return this.id;
      }

      private Gamemode(final int id) {
        this.id = id;
      }
    }

    private int timeOfDay;
    private boolean checkGeyser;
    private boolean checkEagler;
    private boolean logConnections;
    private boolean logDuringAttack;
    private boolean debugXYZPositions;
    private Pattern validNameRegex;

    private int readTimeout;
    private int writeTimeout;
    private int maxPacketCount;
    private int reconnectDelay;
    private int rememberTime;
    private int blacklistTime;
    private int blacklistThreshold;
    private int blacklistIpv4Prefix;
    private int blacklistIpv6Prefix;
    private final Collection<Integer> blacklistedProtocols = new HashSet<>(0);

    private Component tooFastReconnect;
    private Component invalidUsername;
    private Component verificationSuccess;
    private Component verificationFailed;
    private Component alreadyVerifying;
    private Component alreadyQueued;
    private Component blacklisted;
    private Component protocolBlacklisted;
    private Component currentlyPreparing;
    private Component unsupportedVersion;
    private Component serverListPingRequired;

    public Timing getTiming() {
      return this.timing;
    }

    public Map getMap() {
      return this.map;
    }

    public Gravity getGravity() {
      return this.gravity;
    }

    public Vehicle getVehicle() {
      return this.vehicle;
    }

    public Brand getBrand() {
      return this.brand;
    }

    public ServerListPing getServerListPing() {
      return this.serverListPing;
    }

    public Gamemode getGamemode() {
      return this.gamemode;
    }

    public int getTimeOfDay() {
      return this.timeOfDay;
    }

    public boolean isCheckGeyser() {
      return this.checkGeyser;
    }

    public boolean isCheckEagler() {
      return this.checkEagler;
    }

    public boolean isLogConnections() {
      return this.logConnections;
    }

    public boolean isLogDuringAttack() {
      return this.logDuringAttack;
    }

    public boolean isDebugXYZPositions() {
      return this.debugXYZPositions;
    }

    public Pattern getValidNameRegex() {
      return this.validNameRegex;
    }

    public int getReadTimeout() {
      return this.readTimeout;
    }

    public int getWriteTimeout() {
      return this.writeTimeout;
    }

    public int getMaxPacketCount() {
      return this.maxPacketCount;
    }

    public int getReconnectDelay() {
      return this.reconnectDelay;
    }

    public int getRememberTime() {
      return this.rememberTime;
    }

    public int getBlacklistTime() {
      return this.blacklistTime;
    }

    public int getBlacklistThreshold() {
      return this.blacklistThreshold;
    }

    public int getBlacklistIpv4Prefix() {
      return this.blacklistIpv4Prefix;
    }

    public int getBlacklistIpv6Prefix() {
      return this.blacklistIpv6Prefix;
    }

    public Collection<Integer> getBlacklistedProtocols() {
      return this.blacklistedProtocols;
    }

    public Component getTooFastReconnect() {
      return this.tooFastReconnect;
    }

    public Component getInvalidUsername() {
      return this.invalidUsername;
    }

    public Component getVerificationSuccess() {
      return this.verificationSuccess;
    }

    public Component getVerificationFailed() {
      return this.verificationFailed;
    }

    public Component getAlreadyVerifying() {
      return this.alreadyVerifying;
    }

    public Component getAlreadyQueued() {
      return this.alreadyQueued;
    }

    public Component getBlacklisted() {
      return this.blacklisted;
    }

    public Component getProtocolBlacklisted() {
      return this.protocolBlacklisted;
    }

    public Component getCurrentlyPreparing() {
      return this.currentlyPreparing;
    }

    public Component getUnsupportedVersion() {
      return this.unsupportedVersion;
    }

    public Component getServerListPingRequired() {
      return this.serverListPingRequired;
    }

    private Verification() {
    }
  }

  public static final class Database {

    public enum Type {
      MYSQL("MySQL", "jdbc:mysql://%s:%d/%s%s", new MysqlDatabaseTypeAdapter(),
        Library.builder()
          .groupId("com{}mysql")
          .artifactId("mysql-connector-j")
          .version("9.0.0")
          .relocate("com{}mysql", "xyz{}jonesdev{}sonar{}libs{}mysql")
          .build()),
      MARIADB("MariaDB", "jdbc:mariadb://%s:%d/%s%s", new MariaDbDatabaseTypeAdapter(),
        Library.builder()
          .groupId("org{}mariadb{}jdbc")
          .artifactId("mariadb-java-client")
          .version("3.4.1")
          .relocate("org{}mariadb", "xyz{}jonesdev{}sonar{}libs{}mariadb")
          .build()),
      H2("H2", "jdbc:h2:file:%s", new H2DatabaseTypeAdapter(),
        Library.builder()
          .groupId("com{}h2database")
          .artifactId("h2")
          .version("2.2.220")
          .relocate("org{}h2", "xyz{}jonesdev{}sonar{}libs{}h2")
          .build()),
      POSTGRESQL("PostgreSQL", "jdbc:postgresql://%s:%d/%s%s", new PostgresDatabaseTypeAdapter(),
        Library.builder()
          .groupId("org{}postgresql")
          .artifactId("postgresql")
          .version("42.7.8")
          .relocate("org{}postgresql", "xyz{}jonesdev{}sonar{}libs{}postgresql")
          .build()),
      NONE("None", null, null, null);

      private final String displayName;
      private final String connectionString;
      private final DatabaseType databaseType;
      private final Library databaseDriver;
      private boolean loaded;

      public String getDisplayName() {
        return this.displayName;
      }

      public String getConnectionString() {
        return this.connectionString;
      }

      public DatabaseType getDatabaseType() {
        return this.databaseType;
      }

      public Library getDatabaseDriver() {
        return this.databaseDriver;
      }

      public boolean isLoaded() {
        return this.loaded;
      }

      private Type(final String displayName, final String connectionString, final DatabaseType databaseType, final Library databaseDriver) {
        this.displayName = displayName;
        this.connectionString = connectionString;
        this.databaseType = databaseType;
        this.databaseDriver = databaseDriver;
      }

      public void setLoaded(final boolean loaded) {
        this.loaded = loaded;
      }
    }

    private Type type;
    private int maximumAge;
    private boolean persistBlacklist;

    public Type getType() {
      return this.type;
    }

    public int getMaximumAge() {
      return this.maximumAge;
    }

    public boolean isPersistBlacklist() {
      return this.persistBlacklist;
    }

    private Database() {
    }
  }

  public static final class Webhook {
    private String url;
    private String username;
    private String avatarUrl;
    private String content;

    private final Footer footer = new Footer();

    public static final class Footer {
      private String text;
      private String iconUrl;

      public String getText() {
        return this.text;
      }

      public String getIconUrl() {
        return this.iconUrl;
      }

      private Footer() {
      }
    }

    private final Embed embed = new Embed();
    private final Embed startEmbed = new Embed();
    private boolean startEmbedEnabled;

    public static final class Embed {
      private String title;
      private String titleUrl;
      private String description;
      private int r;
      private int g;
      private int b;

      public String getTitle() {
        return this.title;
      }

      public String getTitleUrl() {
        return this.titleUrl;
      }

      public String getDescription() {
        return this.description;
      }

      public int getR() {
        return this.r;
      }

      public int getG() {
        return this.g;
      }

      public int getB() {
        return this.b;
      }

      public Embed() {
      }

      public Embed(final String title, final String titleUrl, final String description, final int r, final int g, final int b) {
        this.title = title;
        this.titleUrl = titleUrl;
        this.description = description;
        this.r = r;
        this.g = g;
        this.b = b;
      }

      public void setDescription(final String description) {
        this.description = description;
      }
    }

    private @Nullable DiscordWebhook discordWebhook;
    private @Nullable GenericWebhook genericWebhook;

    public String getUrl() {
      return this.url;
    }

    public String getUsername() {
      return this.username;
    }

    public String getAvatarUrl() {
      return this.avatarUrl;
    }

    public String getContent() {
      return this.content;
    }

    public Footer getFooter() {
      return this.footer;
    }

    public Embed getEmbed() {
      return this.embed;
    }

    public Embed getStartEmbed() {
      return this.startEmbed;
    }

    public boolean isStartEmbedEnabled() {
      return this.startEmbedEnabled;
    }

    private Webhook() {
    }

    @Nullable
    public DiscordWebhook getDiscordWebhook() {
      return this.discordWebhook;
    }

    @Nullable
    public GenericWebhook getGenericWebhook() {
      return this.genericWebhook;
    }
  }

  public SimpleYamlConfig getGeneralConfig() {
    return this.generalConfig;
  }

  public SimpleYamlConfig getMessagesConfig() {
    return this.messagesConfig;
  }

  public SimpleYamlConfig getWebhookConfig() {
    return this.webhookConfig;
  }

  public File getLanguageFile() {
    return this.languageFile;
  }

  public File getPluginFolder() {
    return this.pluginFolder;
  }

  public Language getLanguage() {
    return this.language;
  }

  public Queue getQueue() {
    return this.queue;
  }

  public Verification getVerification() {
    return this.verification;
  }

  public Database getDatabase() {
    return this.database;
  }

  public Webhook getWebhook() {
    return this.webhook;
  }

  public Component getPrefix() {
    return this.prefix;
  }

  public Component getNoPermission() {
    return this.noPermission;
  }

  public int getMaxOnlinePerIp() {
    return this.maxOnlinePerIp;
  }

  public List<SubnetRange> getAllowlist() {
    return this.allowlist;
  }

  public int getMinPlayersForAttack() {
    return this.minPlayersForAttack;
  }

  public int getMinAttackDuration() {
    return this.minAttackDuration;
  }

  public int getMinAttackThreshold() {
    return this.minAttackThreshold;
  }

  public int getAttackCooldownDelay() {
    return this.attackCooldownDelay;
  }

  public Component getTooManyOnlinePerIp() {
    return this.tooManyOnlinePerIp;
  }

  public List<String> getVerboseAnimation() {
    return this.verboseAnimation;
  }
}
