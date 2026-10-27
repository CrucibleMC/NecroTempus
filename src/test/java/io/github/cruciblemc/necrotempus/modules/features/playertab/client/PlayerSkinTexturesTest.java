package io.github.cruciblemc.necrotempus.modules.features.playertab.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;

class PlayerSkinTexturesTest {

    private final boolean previousEnabled = NecroTempusConfig.enableHeadsFallback;
    private final String previousUrl = NecroTempusConfig.headsFallbackURL;

    @AfterEach
    void restoreConfig() {
        NecroTempusConfig.enableHeadsFallback = previousEnabled;
        NecroTempusConfig.headsFallbackURL = previousUrl;
    }

    @Test
    void defaultFallbackUsesCrafatarUuidEndpoint() {
        UUID id = UUID.fromString("5b2a507b-3af4-4f31-94dc-68db809e3091");

        assertEquals(
            "https://crafatar.com/skins/5b2a507b-3af4-4f31-94dc-68db809e3091.png",
            PlayerSkinTextures.buildFallbackSkinUrl(new GameProfile(id, "Player_1")));
    }

    @Test
    void resolvesConfiguredNameAndUuidPlaceholders() {
        UUID id = UUID.fromString("5b2a507b-3af4-4f31-94dc-68db809e3091");
        GameProfile profile = new GameProfile(id, "Player_1");

        NecroTempusConfig.headsFallbackURL = "https://skins.example/%name%/%uuid%/%uuidTrim%.png";

        assertEquals(
            "https://skins.example/Player_1/5b2a507b-3af4-4f31-94dc-68db809e3091/5b2a507b3af44f3194dc68db809e3091.png",
            PlayerSkinTextures.buildFallbackSkinUrl(profile));
    }

    @Test
    void rejectsInvalidOrMissingPlaceholderValues() {
        GameProfile invalidName = new GameProfile(UUID.randomUUID(), "Bad Name");
        NecroTempusConfig.headsFallbackURL = "https://skins.example/%name%.png";
        assertNull(PlayerSkinTextures.buildFallbackSkinUrl(invalidName));

        GameProfile missingUuid = new GameProfile(null, "Player_1");
        NecroTempusConfig.headsFallbackURL = "https://skins.example/%uuid%.png";
        assertNull(PlayerSkinTextures.buildFallbackSkinUrl(missingUuid));
    }

    @Test
    void disabledOrUnresolvableFallbackReturnsNull() {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "Player_1");
        NecroTempusConfig.headsFallbackURL = "https://skins.example/%unknown%.png";
        assertNull(PlayerSkinTextures.buildFallbackSkinUrl(profile));

        NecroTempusConfig.enableHeadsFallback = false;
        NecroTempusConfig.headsFallbackURL = "https://skins.example/%name%.png";
        assertNull(PlayerSkinTextures.buildFallbackSkinUrl(profile));
    }

    @Test
    void scalesOnlySupportedVanillaAndHdSkinDimensions() {
        assertEquals(1, PlayerSkinTextures.getSkinScale(64, 32));
        assertEquals(1, PlayerSkinTextures.getSkinScale(64, 64));
        assertEquals(2, PlayerSkinTextures.getSkinScale(128, 64));
        assertEquals(2, PlayerSkinTextures.getSkinScale(128, 128));
        assertEquals(0, PlayerSkinTextures.getSkinScale(128, 96));
    }

}
