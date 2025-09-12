package com.aerial.bombing.physics;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * 高级物理配置参数
 */
public class AdvancedPhysicsConfig {
    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int horizontalInertia = 95; // 水平惯性系数 (%)

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int verticalInertia = 85; // 垂直惯性系数 (%)

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int throwForce = 20; // 投掷力量 (%)

    @ConfigEntry.Gui.Tooltip()
    public boolean enableAngularVelocity = true; // 启用角速度继承

    @ConfigEntry.Gui.Tooltip()
    public boolean enableAdvancedDrag = true; // 启用高级阻力模型

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int angularMomentumTransfer = 70; // 角动量传递系数 (%)

    public AdvancedPhysicsConfig() {
        // 默认构造函数
    }
}
