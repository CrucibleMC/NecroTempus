package io.github.cruciblemc.necrotempus.modules.features.dialog.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;

class PreJoinClientSupportTest {

    @Test
    void handlerGatePassesThroughOrRejectsBeforeStarting() {
        assertEquals(PreJoinServerAdapter.HookDecision.PASS_THROUGH, PreJoinServerAdapter.decideHook(false, false));
        assertEquals(PreJoinServerAdapter.HookDecision.PASS_THROUGH, PreJoinServerAdapter.decideHook(false, true));
        assertEquals(
            PreJoinServerAdapter.HookDecision.REJECT_UNSUPPORTED,
            PreJoinServerAdapter.decideHook(true, false));
        assertEquals(PreJoinServerAdapter.HookDecision.START, PreJoinServerAdapter.decideHook(true, true));
    }

    @Test
    void onlyARegisteredPreJoinChannelMarksAClientSupported() {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        try {
            assertFalse(PreJoinServerAdapter.isClientSupported(channel));

            PreJoinServerAdapter.markClientSupported(channel);

            assertTrue(PreJoinServerAdapter.isClientSupported(channel));
        } finally {
            channel.close();
        }
    }
}
