package com.starshootercity.emojicraft;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record Emoji(@NotNull Component emoji, @NotNull List<String> replaces, @Nullable String permission) {
}
