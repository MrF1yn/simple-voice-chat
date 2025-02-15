package de.maxhenkel.voicechat.voice.server;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.net.PlayerStatePacket;
import de.maxhenkel.voicechat.net.PlayerStatesPacket;
import de.maxhenkel.voicechat.voice.common.ClientGroup;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final ConcurrentHashMap<UUID, PlayerState> states;

    public PlayerStateManager() {
        states = new ConcurrentHashMap<>();
        CommonCompatibilityManager.INSTANCE.onPlayerLoggedIn(this::onPlayerLoggedIn);
        CommonCompatibilityManager.INSTANCE.onPlayerLoggedOut(this::onPlayerLoggedOut);

        CommonCompatibilityManager.INSTANCE.getNetManager().updateStateChannel.setServerListener((server, player, handler, packet) -> {
            PlayerState state = states.get(player.getUUID());

            if (state == null) {
                state = defaultDisconnectedState(player);
            }

            state.setDisconnected(packet.isDisconnected());
            state.setDisabled(packet.isDisabled());

            states.put(player.getUUID(), state);

            broadcastState(server, state);
            Voicechat.logDebug("Got state of {}: {}", player.getDisplayName().getString(), state);
        });
    }

    private void broadcastState(MinecraftServer server, PlayerState state) {
        PlayerStatePacket packet = new PlayerStatePacket(state);
        server.getPlayerList().getPlayers().forEach(p -> NetManager.sendToClient(p, packet));
    }

    public void onPlayerCompatibilityCheckSucceeded(ServerPlayer player) {
        PlayerState state = states.getOrDefault(player.getUUID(), defaultDisconnectedState(player));
        states.put(player.getUUID(), state);
        PlayerStatesPacket packet = new PlayerStatesPacket(states);
        NetManager.sendToClient(player, packet);
        Voicechat.logDebug("Setting initial state of {}: {}", player.getDisplayName().getString(), state);
    }

    private void onPlayerLoggedIn(ServerPlayer player) {
        PlayerState state = defaultDisconnectedState(player);
        states.put(player.getUUID(), state);
        broadcastState(player.server, state);
    }

    private void onPlayerLoggedOut(ServerPlayer player) {
        states.remove(player.getUUID());
        broadcastState(player.server, new PlayerState(player.getUUID(), player.getGameProfile().getName(), true, true));
        Voicechat.logDebug("Removing state of {}", player.getDisplayName().getString());
    }

    @Nullable
    public PlayerState getState(UUID playerUUID) {
        return states.get(playerUUID);
    }

    public static PlayerState defaultDisconnectedState(ServerPlayer player) {
        return new PlayerState(player.getUUID(), player.getGameProfile().getName(), false, true);
    }

    public void setGroup(MinecraftServer server, ServerPlayer player, @Nullable ClientGroup group) {
        PlayerState state = states.get(player.getUUID());
        if (state == null) {
            state = PlayerStateManager.defaultDisconnectedState(player);
            Voicechat.logDebug("Defaulting to default state for {}: {}", player.getDisplayName().getString(), state);
        }
        state.setGroup(group);
        states.put(player.getUUID(), state);
        broadcastState(server, state);
        Voicechat.logDebug("Setting group of {}: {}", player.getDisplayName().getString(), state);
    }

    public Collection<PlayerState> getStates() {
        return states.values();
    }

}
