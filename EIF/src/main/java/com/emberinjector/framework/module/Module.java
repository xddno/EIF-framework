package com.emberinjector.framework.module;

import net.minecraft.network.Packet;

public abstract class Module {
    protected final String name;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected boolean enabled;
    protected int key;

    public Module(String name) {
        this(name, false);
    }

    public Module(String name, boolean enabled) {
        this(name, enabled, 0);
    }

    public Module(String name, boolean enabled, int key) {
        this.name = name;
        this.enabled = this.defaultEnabled = enabled;
        this.key = this.defaultKey = key;
    }

    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public boolean toggle() {
        this.setEnabled(!this.enabled);
        return this.enabled;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public void onTick() {
    }

    public boolean onPacketReceived(Packet<?> packet) {
        return false;
    }
}
