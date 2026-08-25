// Copyright (c) Lefraudeur. All rights reserved.
// Copyright (c) achul123. All rights reserved.
// Original work: https://github.com/Lefraudeur/MujinaBaseV2
// Fork:         https://github.com/achul123/MujinaBaseV2

package com.emberinjector.framework.client;

import com.emberinjector.framework.internal.EventDelegate;
import com.emberinjector.framework.internal.EventHandler;
import com.emberinjector.framework.internal.Canceler;
import com.emberinjector.framework.internal.Thrower;
import com.emberinjector.framework.internal.patcher.MethodModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.MovingObjectPosition;

public class EventDispatcher {

    private static volatile EventDelegate delegate;

    public static void setDelegate(EventDelegate d) {
        delegate = d;
    }

    // ===========================
    // Minecraft.runTick
    // ===========================
    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "runTick",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onRunTick(Canceler canceler, Minecraft mc) {
        EventDelegate d = delegate;
        if (d != null) d.onRunTick(mc);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "runGameLoop",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onRunGameLoop(Canceler canceler, Minecraft mc) {
        EventDelegate d = delegate;
        if (d != null) d.onRunGameLoop(mc);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "clickMouse",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onClickMouse(Canceler canceler, Minecraft mc) {
        EventDelegate d = delegate;
        if (d != null && d.onClickMouse(mc)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "rightClickMouse",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onRightClickMouse(Canceler canceler, Minecraft mc) {
        EventDelegate d = delegate;
        if (d != null && d.onRightClickMouse(mc)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/Minecraft",
            targetMethodName = "displayGuiScreen",
            targetMethodDescriptor = "(Lnet/minecraft/client/gui/GuiScreen;)V",
            targetMethodIsStatic = false)
    public static void onDisplayGuiScreen(Canceler canceler, Minecraft mc, net.minecraft.client.gui.GuiScreen guiScreen) {
        EventDelegate d = delegate;
        if (d != null) d.onDisplayGuiScreen(mc, guiScreen);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "onUpdate",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPlayerUpdate(Canceler canceler, EntityPlayerSP player) {
        EventDelegate d = delegate;
        if (d != null) d.onPlayerUpdate(player);
    }

    @EventHandler(type = MethodModifier.Type.ON_RETURN_THROW,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "onUpdate",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onPlayerUpdateReturn(Thrower thrower, EntityPlayerSP player) {
        EventDelegate d = delegate;
        if (d != null) d.onPlayerUpdateReturn(player);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "onUpdateWalkingPlayer",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onUpdateWalkingPlayer(Canceler canceler, EntityPlayerSP player) {
        EventDelegate d = delegate;
        if (d != null && d.onUpdateWalkingPlayer(player)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/network/NetworkManager",
            targetMethodName = "sendPacket",
            targetMethodDescriptor = "(Lnet/minecraft/network/Packet;)V",
            targetMethodIsStatic = false)
    public static void onSendPacket(Canceler canceler, NetworkManager nm, Packet<?> packet) {
        EventDelegate d = delegate;
        if (d != null && d.onSendPacket(nm, packet)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/network/NetworkManager",
            targetMethodName = "channelRead0",
            targetMethodDescriptor = "(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
            targetMethodIsStatic = false)
    public static void onReceivePacket(Canceler canceler, NetworkManager nm,
                                        io.netty.channel.ChannelHandlerContext ctx, Packet<?> packet) {
        EventDelegate d = delegate;
        if (d != null && d.onReceivePacket(nm, ctx, packet)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/network/NetworkManager",
            targetMethodName = "dispatchPacket",
            targetMethodDescriptor = "(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V",
            targetMethodIsStatic = false)
    public static void onDispatchPacket(Canceler canceler, NetworkManager nm,
                                         Packet<?> packet, io.netty.util.concurrent.GenericFutureListener[] listeners) {
        EventDelegate d = delegate;
        if (d != null) d.onDispatchPacket(nm, packet, listeners);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/multiplayer/PlayerControllerMP",
            targetMethodName = "attackEntity",
            targetMethodDescriptor = "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)V",
            targetMethodIsStatic = false)
    public static void onAttackEntity(Canceler canceler, PlayerControllerMP pc,
                                       EntityPlayer player, Entity target) {
        EventDelegate d = delegate;
        if (d != null && d.onAttackEntity(pc, player, target)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/renderer/EntityRenderer",
            targetMethodName = "getMouseOver",
            targetMethodDescriptor = "(F)V",
            targetMethodIsStatic = false)
    public static void onGetMouseOver(Canceler canceler, EntityRenderer er, float partialTicks) {
        EventDelegate d = delegate;
        if (d != null) d.onGetMouseOver(er, partialTicks);
    }

    @EventHandler(type = MethodModifier.Type.ON_RETURN_THROW,
            targetClass = "net/minecraft/client/renderer/EntityRenderer",
            targetMethodName = "getMouseOver",
            targetMethodDescriptor = "(F)V",
            targetMethodIsStatic = false)
    public static void onGetMouseOverReturn(Thrower thrower, EntityRenderer er, float partialTicks) {
        EventDelegate d = delegate;
        if (d != null) d.onGetMouseOverReturn(er, partialTicks);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/entity/Entity",
            targetMethodName = "moveEntity",
            targetMethodDescriptor = "(DDD)V",
            targetMethodIsStatic = false)
    public static void onMoveEntity(Canceler canceler, Entity entity, double x, double y, double z) {
        EventDelegate d = delegate;
        if (d != null) d.onMoveEntity(entity, x, y, z);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/entity/EntityLivingBase",
            targetMethodName = "jump",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onJump(Canceler canceler, EntityLivingBase entity) {
        EventDelegate d = delegate;
        if (d != null && d.onJump(entity)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/entity/player/InventoryPlayer",
            targetMethodName = "changeCurrentItem",
            targetMethodDescriptor = "(I)V",
            targetMethodIsStatic = false)
    public static void onChangeCurrentItem(Canceler canceler, InventoryPlayer inv, int slot) {
        EventDelegate d = delegate;
        if (d != null && d.onChangeCurrentItem(inv, slot)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/entity/Entity",
            targetMethodName = "setVelocity",
            targetMethodDescriptor = "(DDD)V",
            targetMethodIsStatic = false)
    public static void onSetVelocity(Canceler canceler, Entity entity, double x, double y, double z) {
        EventDelegate d = delegate;
        if (d != null && d.onSetVelocity(entity, x, y, z)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "closeScreen",
            targetMethodDescriptor = "()V",
            targetMethodIsStatic = false)
    public static void onCloseScreen(Canceler canceler, EntityPlayerSP player) {
        EventDelegate d = delegate;
        if (d != null) d.onCloseScreen(player);
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/network/NetHandlerPlayClient",
            targetMethodName = "handleExplosion",
            targetMethodDescriptor = "(Lnet/minecraft/network/play/server/S27PacketExplosion;)V",
            targetMethodIsStatic = false)
    public static void onHandleExplosion(Canceler canceler,
                                          NetHandlerPlayClient handler,
                                          S27PacketExplosion packet) {
        EventDelegate d = delegate;
        if (d != null && d.onHandleExplosion(handler, packet)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/gui/GuiNewChat",
            targetMethodName = "printChatMessage",
            targetMethodDescriptor = "(Lnet/minecraft/util/IChatComponent;)V",
            targetMethodIsStatic = false)
    public static void onPrintChatMessage(Canceler canceler, net.minecraft.client.gui.GuiNewChat guiNewChat, IChatComponent message) {
        EventDelegate d = delegate;
        if (d != null && d.onPrintChatMessage(message)) {
            canceler.cancel = true;
        }
    }

    @EventHandler(type = MethodModifier.Type.ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "sendChatMessage",
            targetMethodDescriptor = "(Ljava/lang/String;)V",
            targetMethodIsStatic = false)
    public static void onSendChatMessage(Canceler canceler, EntityPlayerSP player, String message) {
        EventDelegate d = delegate;
        if (d != null && d.onSendChatMessage(message)) {
            canceler.cancel = true;
        }
    }
}
