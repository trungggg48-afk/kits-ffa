package com.hyperffa.kit.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MessageManager {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;
    private FileConfiguration messagesConfig;
    private String prefix = "";

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        this.reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            this.messagesConfig.setDefaults(defaultConfig);
        }

        this.prefix = messagesConfig.getString("prefix", "<gradient:#3b82f6:#60a5fa><bold>HYPER-KIT</bold></gradient> <dark_gray>»</dark_gray> ");
    }

    public Component parse(String text, Map<String, String> placeholders) {
        if (text == null) {
            return Component.empty();
        }
        String formatted = text.replace("<prefix>", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        // Handle both MiniMessage tags and legacy '&' color codes seamlessly
        if (formatted.contains("&")) {
            Component legacyComponent = legacySerializer.deserialize(formatted);
            formatted = miniMessage.serialize(legacyComponent);
        }
        return miniMessage.deserialize(formatted);
    }

    public Component parse(String text) {
        return parse(text, Collections.emptyMap());
    }

    public Component getMessage(String path, Map<String, String> placeholders) {
        String raw = messagesConfig.getString(path, "<red>Missing message: " + path + "</red>");
        return parse(raw, placeholders);
    }

    public Component getMessage(String path) {
        return getMessage(path, Collections.emptyMap());
    }

    public List<Component> getMessageList(String path, Map<String, String> placeholders) {
        List<String> list = messagesConfig.getStringList(path);
        if (list.isEmpty()) {
            String single = messagesConfig.getString(path);
            if (single != null) {
                return Collections.singletonList(parse(single, placeholders));
            }
            return Collections.emptyList();
        }
        return list.stream().map(s -> parse(s, placeholders)).collect(Collectors.toList());
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (messagesConfig.isList(path)) {
            for (Component line : getMessageList(path, placeholders)) {
                sender.sendMessage(line);
            }
        } else {
            sender.sendMessage(getMessage(path, placeholders));
        }
    }

    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Collections.emptyMap());
    }

    public String getPrefix() {
        return prefix;
    }
}
