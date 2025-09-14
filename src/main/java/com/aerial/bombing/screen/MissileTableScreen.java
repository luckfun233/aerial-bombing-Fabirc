package com.aerial.bombing.screen;

import com.aerial.bombing.AerialBombing;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MissileTableScreen extends HandledScreen<MissileTableScreenHandler> {
    // 指向你的新 GUI 纹理
    private static final Identifier TEXTURE = new Identifier(AerialBombing.MOD_ID, "textures/gui/1.png");

    private ButtonWidget instantExplosionButton;

    public MissileTableScreen(MissileTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // 根据锻造台UI调整尺寸
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // 调整标题位置
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;

        // 创建一个更清晰的按钮
        instantExplosionButton = ButtonWidget.builder(getButtonText(), button -> {
            // 发送网络包到服务器切换状态
            ClientPlayNetworking.send(new Identifier(AerialBombing.MOD_ID, "toggle_instant_explosion"), PacketByteBufs.empty());
        }).dimensions(this.x + 75, this.y + 18, 50, 20).build();

        this.addDrawableChild(instantExplosionButton);
    }

    // 根据状态获取按钮文本
    private Text getButtonText() {
        return handler.isInstantExplosion()
                ? Text.translatable("gui.aerial-bombing.instant_explosion_on")
                : Text.translatable("gui.aerial-bombing.instant_explosion_off");
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        // 持续更新按钮文本以响应服务器的更新
        instantExplosionButton.setMessage(getButtonText());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = this.x;
        int y = this.y;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 绘制标题
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 4210752, false);
        // 绘制玩家物品栏标题
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
