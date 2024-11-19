package com.starshootercity.emojicraft;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EmojiCraft extends JavaPlugin implements Listener, CommandExecutor {
    private FileConfiguration fileConfiguration;

    private static EmojiCraft instance;

    public static EmojiCraft getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Logger l = ((Logger) LogManager.getRootLogger());
        l.addFilter(new EmojiConsoleFilter());


        File file = new File(getDataFolder(), "emojis.yml");
        if (!file.exists()) {
            saveResource("emojis.yml", false);
        }

        fileConfiguration = new YamlConfiguration();

        try {
            fileConfiguration.load(file);
        } catch (InvalidConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }

        File smile = new File(getDataFolder(), "emojis/smile.png");
        if (!smile.getParentFile().exists()) {
            saveResource("emojis/smile.png", false);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("emojicrafter-reload");
        if (command != null) {
            command.setExecutor(this);
        }

        loadEmojis();
    }

    public void loadEmojis() {
        emojiComponentMap.clear();

        for (String emoji : fileConfiguration.getKeys(false)) {
            Emoji emojiComponent = makeEmoji(emoji);
            if (emojiComponent == null) continue;
            emojiComponentMap.put(emoji, emojiComponent);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        EmojiConsoleFilter.messages.add(new EmojiConsoleFilter.ComponentMessage(event.getPlayer().getName(), event.message()));
        Component component = event.message();
        if (event.getPlayer().hasPermission("emojicrafter.default")) {
            for (Emoji emoji : emojiComponentMap.values()) {
                if (emoji.permission() != null) {
                    if (!event.getPlayer().hasPermission(emoji.permission())) continue;
                }
                Component emojiComponent = emoji.emoji();
                for (String replacement : emoji.replaces()) {
                    component = component.replaceText(builder -> builder.matchLiteral(replacement).replacement(emojiComponent));
                }
            }
        }
        event.message(component.append(Component.text(EmojiCraft.getInstance().getConfig().getString("filter-flag", "\u0000")).font(Key.key("minecraft:pixels"))));
    }

    private final Map<String, Emoji> emojiComponentMap = new HashMap<>();

    public @Nullable Emoji makeEmoji(String emoji) {
        String path = fileConfiguration.getString("%s.path".formatted(emoji));
        if (path == null) {
            getLogger().warning("Emoji '%s' does not have a path set, ignoring".formatted(emoji));
            return null;
        }
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            getLogger().warning("Emoji '%s' has no image at its path '%s', ignoring".formatted(emoji, path));
            return null;
        }
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (image.getHeight() != 16) {
            getLogger().warning("Emoji '%s' image at path '%s' is not 16 pixels tall, ignoring".formatted(emoji, path));
            return null;
        }

        String pixels = "\uE000\uE001\uE002\uE003\uE004\uE005\uE006\uE007\uE008\uE009\uE00A\uE00B\uE00C\uE00D\uE00E\uE00F";
        Component result = Component.text('\uF001');
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < 16; y++) {
                int col = image.getRGB(x, y);
                if (col == 0) {
                    result = result.append(Component.text('\uF002'));
                    if (y != 15) result = result.append(Component.text('\uF000'));
                    continue;
                }
                result = result.append(Component.text(pixels.charAt(y)).color(TextColor.color(col)));
                if (y != 15) result = result.append(Component.text('\uF000'));
            }
            result = result.append(Component.text('\uF001'));
        }

        return new Emoji(result.append(Component.text('\uF003')).font(Key.key("minecraft:pixels")), fileConfiguration.getStringList("%s.replaces".formatted(emoji)), fileConfiguration.getString("%s.permission".formatted(emoji), null));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        loadEmojis();
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!getConfig().getBoolean("use-resource-pack")) return;
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            try {
                event.getPlayer().sendResourcePacks(
                        ResourcePackInfo.resourcePackInfo().uri(URI.create("https://github.com/cometcake575/EmojiCrafter/raw/main/EmojiPack.zip")).computeHashAndBuild().get()
                );
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }, 30);
    }
}