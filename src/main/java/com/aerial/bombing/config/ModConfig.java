package com.aerial.bombing.config;

import com.aerial.bombing.physics.AdvancedPhysicsConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "aerial-bombing")
@Config.Gui.Background("minecraft:textures/block/tnt_side.png")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip()
    public boolean enableAerialBombing = true;

    @ConfigEntry.Gui.Tooltip()
    public boolean requireFlintAndSteel = true;

    @ConfigEntry.Gui.Tooltip()
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5000)
    public int bombCooldownMs = 1000;


    // 新增选项：用于切换物理模拟引擎
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean useAdvancedPhysics = true;

    // 高级物理参数分组
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public AdvancedPhysicsConfig advancedPhysics = new AdvancedPhysicsConfig();
}
