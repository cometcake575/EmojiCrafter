package com.starshootercity.emojicraft;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmojiCraft extends JavaPlugin implements Listener, CommandExecutor {
    private FileConfiguration fileConfiguration;

    @Override
    public void onEnable() {

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
            Component emojiComponent = makeEmoji(emoji);
            if (emojiComponent == null) continue;
            emojiComponentMap.put(emoji, emojiComponent);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Component component = event.message();
        for (String emoji : emojiComponentMap.keySet()) {
            Component emojiComponent = emojiComponentMap.get(emoji);
            for (String replacement : fileConfiguration.getStringList("%s.replaces".formatted(emoji))) {
                component = component.replaceText(builder -> builder.matchLiteral(replacement).replacement(emojiComponent));
            }
        }
        event.message(component);
    }

    private final Map<String, Component> emojiComponentMap = new HashMap<>();

    public @Nullable Component makeEmoji(String emoji) {
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

        return result.append(Component.text("\uF003\u0000")).font(Key.key("minecraft:pixels"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        loadEmojis();
        return true;
    }
}