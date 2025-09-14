package com.aerial.bombing.screen;

import com.aerial.bombing.AerialBombing;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<MissileTableScreenHandler> MISSILE_TABLE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(AerialBombing.MOD_ID, "missile_table"),
                    new ExtendedScreenHandlerType<>(MissileTableScreenHandler::new));

    public static void registerScreenHandlers() {
        AerialBombing.LOGGER.info("Registering Screen Handlers for " + AerialBombing.MOD_ID);
    }
}
