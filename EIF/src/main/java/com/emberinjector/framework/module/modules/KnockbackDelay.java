package com.emberinjector.framework.module.modules;

import com.emberinjector.framework.module.Module;
import com.emberinjector.framework.property.properties.BooleanProperty;
import com.emberinjector.framework.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.MovingObjectPosition;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KnockbackDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random RANDOM = new Random();

    public final IntProperty airDelay = new IntProperty("AirDelay", 90, 0, 1000);
    public final IntProperty groundDelay = new IntProperty("GroundDelay", 0, 0, 1000);
    public final IntProperty chance = new IntProperty("Chance", 100, 0, 100);
    public final BooleanProperty realtimeDamage = new BooleanProperty("RealtimeDamage", true);
    public final BooleanProperty requireTarget = new BooleanProperty("RequireTarget", false);
    public final BooleanProperty onlySwords = new BooleanProperty("OnlySwords", false);
    public final BooleanProperty waterCheck = new BooleanProperty("WaterCheck", false);

    private final Queue<TimedPacket> packets = new ConcurrentLinkedQueue<>();
    private boolean blink;

    public KnockbackDelay() {
        super("KnockbackDelay", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{airDelay.getValue() + " - " + groundDelay.getValue()};
    }

    @Override
    public void onDisabled() {
        reset();
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) return;

        if (mc.currentScreen != null) {
            reset();
            return;
        }

        if (!shouldActivate()) {
            reset();
            return;
        }

        int delay = mc.thePlayer.onGround ? groundDelay.getValue() : airDelay.getValue();

        if (!packets.isEmpty()) {
            handle(delay);
        }

        if (mc.thePlayer.hurtTime > 0) {
            blink = true;
        } else if (packets.isEmpty()) {
            blink = false;
        }
    }

    @Override
    public boolean onPacketReceived(Packet<?> packet) {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        if (mc.isSingleplayer() || mc.thePlayer.ticksExisted < 20) return false;

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId() && !blink && shouldActivate()) {
                blink = true;
            }
        }

        if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion exp = (S27PacketExplosion) packet;
            if ((exp.func_149149_c() != 0.0F || exp.func_149144_d() != 0.0F || exp.func_149147_e() != 0.0F)
                    && !blink && shouldActivate()) {
                blink = true;
            }
        }

        if (!blink) return false;

        if (realtimeDamage.getValue() && packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus status = (S19PacketEntityStatus) packet;
            if (status.getOpCode() == 2 && status.getEntity(mc.theWorld) == mc.thePlayer) {
                return false;
            }
        }

        packets.add(new TimedPacket(packet, System.currentTimeMillis()));
        return true;
    }

    private boolean shouldActivate() {
        if (RANDOM.nextInt(100) >= chance.getValue()) return false;

        if (requireTarget.getValue() && findTarget() == null) return false;

        if (onlySwords.getValue()) {
            if (mc.thePlayer == null || !(mc.thePlayer.getHeldItem() != null
                    && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return false;
            }
        }

        if (waterCheck.getValue() && mc.thePlayer != null && mc.thePlayer.isInWater()) {
            return false;
        }

        return true;
    }

    private void reset() {
        if (!blink) return;
        blink = false;
        flush();
    }

    private void handle(int delay) {
        while (!packets.isEmpty()) {
            TimedPacket wrapper = packets.peek();
            if (wrapper != null && wrapper.elapsed(delay)) {
                packets.poll();
                processPacketSilent(wrapper.packet);
            } else {
                break;
            }
        }
    }

    private void flush() {
        TimedPacket wrapper;
        while ((wrapper = packets.poll()) != null) {
            processPacketSilent(wrapper.packet);
        }
    }

    @SuppressWarnings("unchecked")
    private void processPacketSilent(Packet<?> packet) {
        try {
            if (mc.getNetHandler() != null) {
                ((Packet<net.minecraft.network.play.INetHandlerPlayClient>) packet)
                        .processPacket(mc.getNetHandler());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TimedPacket {
        private final Packet<?> packet;
        private final long time;

        public TimedPacket(Packet<?> packet, long time) {
            this.packet = packet;
            this.time = time;
        }

        public boolean elapsed(int delayMs) {
            return System.currentTimeMillis() - time >= delayMs;
        }
    }

    private Entity findTarget() {
        if (mc.pointedEntity != null) return mc.pointedEntity;

        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return mc.objectMouseOver.entityHit;
        }

        return null;
    }
}
