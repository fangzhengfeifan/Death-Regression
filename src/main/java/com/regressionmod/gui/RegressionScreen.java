/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.BufferBuilder
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.Tesselator
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.network.chat.Component
 */
package com.regressionmod.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

public class RegressionScreen
extends Screen {
    private long displayStartTime;

    public RegressionScreen() {
        super((Component)Component.literal((String)"\u4e16\u754c\u56de\u6eaf\u4e2d..."));
    }

    protected void init() {
        super.init();
        this.displayStartTime = System.currentTimeMillis();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderDarkBackground(graphics);
        this.renderText(graphics, partialTick);
    }

    private void renderDarkBackground(GuiGraphics graphics) {
        int width = this.width;
        int height = this.height;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float alpha = 0.7f;
        buffer.vertex(0.0, (double)height, 0.0).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        buffer.vertex((double)width, (double)height, 0.0).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        buffer.vertex((double)width, 0.0, 0.0).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        buffer.vertex(0.0, 0.0, 0.0).color(0.0f, 0.0f, 0.0f, alpha).endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
    }

    private void renderText(GuiGraphics graphics, float partialTick) {
        long elapsed = System.currentTimeMillis() - this.displayStartTime;
        float seconds = (float)elapsed / 1000.0f;
        double pulse = 0.8 + 0.2 * Math.sin((double)seconds * Math.PI);
        int alpha = (int)(255.0 * pulse);
        int color = alpha << 24 | 0xFFFFFF;
        String mainText = "\u4e16\u754c\u56de\u6eaf\u4e2d...";
        int textWidth = this.font.width(mainText);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.drawString(this.font, mainText, centerX - textWidth / 2, centerY - 10, color, false);
        String subText = "\u6b63\u5728\u6062\u590d\u5230\u6700\u8fd1\u5b58\u6863\u70b9...";
        int subTextWidth = this.font.width(subText);
        int subAlpha = (int)((double)alpha * 0.7);
        int subColor = subAlpha << 24 | 0xCCCCCC;
        graphics.drawString(this.font, subText, centerX - subTextWidth / 2, centerY + 15, subColor, false);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            return true;
        }
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return true;
    }

    public boolean isPauseScreen() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }
}

