package com.emberinjector.framework.events;

public class KnockbackEvent {
    private double x;
    private double y;
    private double z;
    private boolean cancelled;

    public KnockbackEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public void setX(double x) { this.x = x; this.cancelled = true; }
    public void setY(double y) { this.y = y; this.cancelled = true; }
    public void setZ(double z) { this.z = z; this.cancelled = true; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
