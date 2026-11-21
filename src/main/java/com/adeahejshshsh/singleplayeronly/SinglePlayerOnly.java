package com.adeahejshshsh.singleplayeronly;

import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/**
 * Forge 1.20.1 entry point of "Single Player Only".
 *
 * <p>The actual work is done by {@code com.adeahejshshsh.singleplayeronly.client.ClientScreenGuard},
 * which is registered through {@link Mod.EventBusSubscriber} on the {@code FORGE} bus and only on the
 * physical client.</p>
 */
@Mod(SinglePlayerOnly.MOD_ID)
public final class SinglePlayerOnly {

    /** Must match {@code modId} in META-INF/mods.toml. */
    public static final String MOD_ID = "singleplayeronly";

    public static final Logger LOGGER = LogUtils.getLogger();

    public SinglePlayerOnly() {
        LOGGER.info("SinglePlayerOnly loaded: multiplayer functions blocked.");
    }
}
