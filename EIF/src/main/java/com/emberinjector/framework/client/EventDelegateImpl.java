// MIT License. Copyright (c) EIF-framework author. See NOTICE.md.
// SPDX-License-Identifier: MIT

package com.emberinjector.framework.client;

import com.emberinjector.framework.module.Module;
import com.emberinjector.framework.module.ModuleManager;
import com.emberinjector.framework.property.PropertyManager;
import com.emberinjector.framework.internal.EventDelegate;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;

public class EventDelegateImpl implements EventDelegate {

    private boolean worldLoaded = false;
    private final ModuleManager moduleManager;
    private final PropertyManager propertyManager;
    private final boolean[] keyStates;

    public EventDelegateImpl(ModuleManager moduleManager, PropertyManager propertyManager) {
        this.moduleManager = moduleManager;
        this.propertyManager = propertyManager;
        this.keyStates = new boolean[256];
    }

    @Override
    public void onRunTick(Minecraft mc) {
        if (mc.theWorld != null && !worldLoaded) {
            worldLoaded = true;
            mc.thePlayer.addChatMessage(new ChatComponentText("§a[EIF] §fHello World! Injection framework loaded."));
        } else if (mc.theWorld == null) {
            worldLoaded = false;
        }

        checkModuleKeys();
        tickModules();
    }

    private void tickModules() {
        for (Module module : moduleManager.modules.values()) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    private void checkModuleKeys() {
        for (Module module : moduleManager.modules.values()) {
            int key = module.getKey();
            if (key > 0 && key < 256) {
                boolean isDown = Keyboard.isKeyDown(key);
                if (isDown && !keyStates[key]) {
                    module.toggle();
                }
                keyStates[key] = isDown;
            }
        }
    }

    @Override
    public void onRunGameLoop(Minecraft mc) {
    }

    @Override
    public boolean onClickMouse(Minecraft mc) {
        return false;
    }

    @Override
    public boolean onRightClickMouse(Minecraft mc) {
        return false;
    }

    @Override
    public void onDisplayGuiScreen(Minecraft mc, GuiScreen guiScreen) {
    }

    @Override
    public void onPlayerUpdate(EntityPlayerSP player) {
    }

    @Override
    public void onPlayerUpdateReturn(EntityPlayerSP player) {
    }

    @Override
    public boolean onUpdateWalkingPlayer(EntityPlayerSP player) {
        return false;
    }

    @Override
    public boolean onSendPacket(NetworkManager nm, Packet<?> packet) {
        return false;
    }

    @Override
    public boolean onReceivePacket(NetworkManager nm, ChannelHandlerContext ctx, Packet<?> packet) {
        for (Module module : moduleManager.modules.values()) {
            if (module.isEnabled() && module.onPacketReceived(packet)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDispatchPacket(NetworkManager nm, Packet<?> packet, GenericFutureListener[] listeners) {
    }

    @Override
    public boolean onAttackEntity(PlayerControllerMP pc, EntityPlayer player, Entity target) {
        return false;
    }

    @Override
    public void onGetMouseOver(EntityRenderer er, float partialTicks) {
    }

    @Override
    public void onGetMouseOverReturn(EntityRenderer er, float partialTicks) {
    }

    @Override
    public void onMoveEntity(Entity entity, double x, double y, double z) {
    }

    @Override
    public boolean onJump(EntityLivingBase entity) {
        return false;
    }

    @Override
    public boolean onChangeCurrentItem(InventoryPlayer inv, int slot) {
        return false;
    }

    @Override
    public boolean onSetVelocity(Entity entity, double x, double y, double z) {
        return false;
    }

    @Override
    public void onCloseScreen(EntityPlayerSP player) {
    }

    @Override
    public boolean onHandleExplosion(NetHandlerPlayClient handler, S27PacketExplosion packet) {
        return false;
    }

    @Override
    public boolean onPrintChatMessage(IChatComponent message) {
        return false;
    }

    @Override
    public boolean onSendChatMessage(String message) {
        return false;
    }
}
