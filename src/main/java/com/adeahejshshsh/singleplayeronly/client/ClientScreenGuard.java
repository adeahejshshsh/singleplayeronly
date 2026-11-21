package com.adeahejshshsh.singleplayeronly.client;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.adeahejshshsh.singleplayeronly.SinglePlayerOnly;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Keeps the game single-player only. Everything is driven by two public NeoForge events, so no Mixin
 * config, refmap or injection point has to be maintained:
 *
 * <ul>
 *   <li>{@link ScreenEvent.Opening}: a multiplayer-ish screen is replaced before it ever opens.</li>
 *   <li>{@link ScreenEvent.Init.Post}: the multiplayer entries are hidden right after the title
 *       screen (and the pause screen) build their widgets.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SinglePlayerOnly.MOD_ID, value = Dist.CLIENT)
public final class ClientScreenGuard {

    /** Translation keys of the entries that lead out of singleplayer. */
    private static final String KEY_MULTIPLAYER = "menu.multiplayer";
    private static final String KEY_REALMS = "menu.online";
    private static final String KEY_OPEN_TO_LAN = "menu.shareToLan";

    private ClientScreenGuard() {
    }

    // ------------------------------------------------------------------
    // 1) Never let a multiplayer-ish screen open
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen target = event.getNewScreen();
        if (target == null || !isMultiplayerScreen(target)) {
            return;
        }

        SinglePlayerOnly.LOGGER.debug("Blocked screen: {}", target.getClass().getName());
        // Bounce straight back to the title screen instead of opening the blocked screen.
        event.setNewScreen(new TitleScreen());
    }

    private static boolean isMultiplayerScreen(Screen screen) {
        if (screen instanceof JoinMultiplayerScreen || screen instanceof ConnectScreen
                || screen instanceof ShareToLanScreen) {
            return true;
        }

        // Class-name matching keeps this working if a screen is renamed. Realms screens live in the
        // com.mojang.realmsclient package, outside the public NeoForge API surface, and their names
        // have moved around between versions; the LAN screen was called OpenToLanScreen before 1.19.
        String name = screen.getClass().getName();
        return name.contains("RealmsMainScreen")
                || name.contains("RealmsNotificationsScreen")
                || name.contains("ShareToLanScreen")
                || name.contains("OpenToLanScreen");
    }

    // ------------------------------------------------------------------
    // 2) Hide the multiplayer entries on the title / pause screen
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();

        if (screen instanceof TitleScreen) {
            hideWidgets(event.getListenersList(), ClientScreenGuard::isMultiplayerEntry);
        } else if (screen instanceof PauseScreen) {
            // "Open to LAN" would make the singleplayer world reachable from the outside.
            hideWidgets(event.getListenersList(), message -> hasKey(message, KEY_OPEN_TO_LAN));
        }
    }

    private static void hideWidgets(List<GuiEventListener> listeners, Predicate<Component> matcher) {
        for (GuiEventListener listener : listeners) {
            if (listener instanceof AbstractWidget widget && matcher.test(widget.getMessage())) {
                widget.visible = false;
                widget.active = false;
            }
        }
    }

    private static boolean isMultiplayerEntry(Component message) {
        if (hasKey(message, KEY_MULTIPLAYER) || hasKey(message, KEY_REALMS)) {
            return true;
        }

        // Fallback for renamed/overridden labels: also match plain text, including Chinese strings.
        String text = message.getString().toLowerCase(Locale.ROOT);
        return text.contains("multiplayer")
                || text.contains("realms")
                || text.contains("多人游戏")
                || text.contains("多人遊戲")
                || text.contains("领域")
                || text.contains("領域");
    }

    private static boolean hasKey(Component message, String key) {
        return message.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey());
    }
}
