package com.adeahejshshsh.singleplayeronly;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.common.Mod;

import org.slf4j.Logger;

/**
 * NeoForge 1.21.1 entry point of "Single Player Only".
 *
 * <p>The actual work is done by {@code com.adeahejshshsh.singleplayeronly.client.ClientScreenGuard},
 * which is registered through {@link net.neoforged.fml.common.EventBusSubscriber} on the game bus and
 * only on the physical client.</p>
 */
@Mod(SinglePlayerOnly.MOD_ID)
public final class SinglePlayerOnly {

    /** Must match {@code modId} in META-INF/neoforge.mods.toml. */
    public static final String MOD_ID = "singleplayeronly";

    public static final Logger LOGGER = LogUtils.getLogger();

    public SinglePlayerOnly() {
        LOGGER.info("SinglePlayerOnly loaded: multiplayer functions blocked.");
    }
}
