// MIT License. Copyright (c) EIF-framework author. See NOTICE.md.
// SPDX-License-Identifier: MIT

package com.emberinjector.framework.internal;

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
import net.minecraft.util.IChatComponent;

public interface EventDelegate {
    void onRunTick(Minecraft mc);
    void onRunGameLoop(Minecraft mc);
    boolean onClickMouse(Minecraft mc);
    boolean onRightClickMouse(Minecraft mc);
    void onDisplayGuiScreen(Minecraft mc, GuiScreen guiScreen);
    void onPlayerUpdate(EntityPlayerSP player);
    void onPlayerUpdateReturn(EntityPlayerSP player);
    boolean onUpdateWalkingPlayer(EntityPlayerSP player);
    boolean onSendPacket(NetworkManager nm, Packet<?> packet);
    boolean onReceivePacket(NetworkManager nm, ChannelHandlerContext ctx, Packet<?> packet);
    void onDispatchPacket(NetworkManager nm, Packet<?> packet, GenericFutureListener[] listeners);
    boolean onAttackEntity(PlayerControllerMP pc, EntityPlayer player, Entity target);
    void onGetMouseOver(EntityRenderer er, float partialTicks);
    void onGetMouseOverReturn(EntityRenderer er, float partialTicks);
    void onMoveEntity(Entity entity, double x, double y, double z);
    boolean onJump(EntityLivingBase entity);
    boolean onChangeCurrentItem(InventoryPlayer inv, int slot);
    boolean onSetVelocity(Entity entity, double x, double y, double z);
    void onCloseScreen(EntityPlayerSP player);
    boolean onHandleExplosion(NetHandlerPlayClient handler, S27PacketExplosion packet);
    boolean onPrintChatMessage(IChatComponent message);
    boolean onSendChatMessage(String message);
}
