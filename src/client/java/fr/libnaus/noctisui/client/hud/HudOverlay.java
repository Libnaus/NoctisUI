package fr.libnaus.noctisui.client.hud;

import fr.libnaus.noctisui.client.NoctisUIClient;
import fr.libnaus.noctisui.client.api.system.Render2DEngine;
import fr.libnaus.noctisui.client.api.system.Shaders;
import fr.libnaus.noctisui.client.api.system.render.font.FontAtlas;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;

import java.awt.*;

public final class HudOverlay implements HudRenderCallback {

    private static final String ICON_HEART = "\uE9FD";
    private static final String ICON_FOOD  = "\uE9AC";
    private static final String ICON_ARMOR = "\uEAA8";

    private static final int BASE_ICON_SIZE = 12;
    private static final int BASE_BAR_WIDTH = 80;
    private static final int BASE_BAR_HEIGHT = 6;
    private static final int BASE_PADDING = 12;
    private static final int BASE_LINE_SPACING = 18;
    private static final int BASE_BOX_RADIUS = 8;
    private static final int BASE_INNER_PADDING = 8;

    private static final float MARGIN_RIGHT_PERCENT = 0.02f;
    private static final float MARGIN_BOTTOM_PERCENT = 0.20f;
    private static final float MIN_MARGIN_RIGHT = 10f;
    private static final float MIN_MARGIN_BOTTOM = 40f;

    private static final Color HEALTH_COLOR = new Color(255, 85, 85);
    private static final Color FOOD_COLOR = new Color(255, 170, 85);
    private static final Color ARMOR_COLOR = new Color(85, 170, 255);

    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 120);
    private static final Color BORDER_COLOR = new Color(255, 255, 255, 80);
    private static final Color BAR_BACKGROUND = new Color(255, 255, 255, 30);

    private float healthAnimation = 0f;
    private float foodAnimation = 0f;
    private float armorAnimation = 0f;
    private long lastUpdateTime = 0;

    @Override
    public void onHudRender(DrawContext ctx, float v) {
        if (!initializeShaders()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        updateAnimations(player);
        renderHUD(ctx, player, mc);
    }

    private boolean initializeShaders() {
        if (Shaders.BLUR == null || Shaders.blurRadius == null) {
            ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
            Shaders.load();
            return Shaders.BLUR != null;
        }
        return true;
    }

    private void updateAnimations(PlayerEntity player) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;

        float healthTarget = Math.min(1f, player.getHealth() / player.getMaxHealth());
        float foodTarget = Math.min(1f, player.getHungerManager().getFoodLevel() / 20f);
        float armorTarget = Math.min(1f, player.getArmor() / 20f);

        healthAnimation = lerp(healthAnimation, healthTarget, deltaTime * 3f);
        foodAnimation = lerp(foodAnimation, foodTarget, deltaTime * 3f);
        armorAnimation = lerp(armorAnimation, armorTarget, deltaTime * 3f);
    }

    private float lerp(float start, float end, float factor) {
        return start + (end - start) * Math.min(factor, 1f);
    }

    private float getScale(int screenWidth, int screenHeight) {
        final float REF_WIDTH = 1920f;
        final float REF_HEIGHT = 1080f;
        float scaleW = screenWidth / REF_WIDTH;
        float scaleH = screenHeight / REF_HEIGHT;
        return Math.max(Math.min(scaleW, scaleH), 0.2F);
    }

    private ScaledDimensions getScaledDimensions(int screenWidth, int screenHeight) {
        float minecraftScale = (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
        float scale = getScale(screenWidth, screenHeight) * minecraftScale;
        return new ScaledDimensions(
            (int)(BASE_ICON_SIZE * scale),
            (int)(BASE_BAR_WIDTH * scale),
            (int)(BASE_BAR_HEIGHT * scale),
            (int)(BASE_PADDING * scale),
            (int)(BASE_LINE_SPACING * scale),
            (int)(BASE_BOX_RADIUS * scale),
            (int)(BASE_INNER_PADDING * scale)
        );
    }

    private Position calculateHUDPosition(int screenWidth, int screenHeight, float boxWidth, float boxHeight) {
        float marginRight = Math.max(screenWidth * MARGIN_RIGHT_PERCENT, MIN_MARGIN_RIGHT);
        float marginBottom = Math.max(screenHeight * MARGIN_BOTTOM_PERCENT, MIN_MARGIN_BOTTOM);

        float x = screenWidth - boxWidth - marginRight;
        float y = screenHeight - boxHeight - marginBottom + 10;

        x = Math.max(10, Math.min(x, screenWidth - boxWidth - 10));
        y = Math.max(10, Math.min(y, screenHeight - boxHeight - 10));

        return new Position(x, y);
    }

    private void renderHUD(DrawContext ctx, PlayerEntity player, MinecraftClient mc) {
        MatrixStack matrices = ctx.getMatrices();
        FontAtlas icons = NoctisUIClient.getInstance().getFonts().getLucide();
        FontAtlas inter = NoctisUIClient.getInstance().getFonts().getInterMedium();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        ScaledDimensions dims = getScaledDimensions(screenWidth, screenHeight);

        float contentWidth = dims.iconSize() + dims.innerPadding() + dims.barWidth();
        float boxWidth = contentWidth + dims.padding() * 2;
        int numberOfStats = 3;
        int contentHeight = (numberOfStats * dims.lineSpacing()) - (dims.lineSpacing() - dims.iconSize());
        int boxHeight = contentHeight + dims.padding() * 2;

        Position pos = calculateHUDPosition(screenWidth, screenHeight, boxWidth, boxHeight);

        float boxX = pos.x();
        float boxY = pos.y();
        float contentX = boxX + dims.padding();
        float contentY = boxY + dims.padding();

        renderBackground(matrices, boxX, boxY, boxWidth, boxHeight, dims.boxRadius());

        float currentY = contentY;
        drawStatLine(ctx, icons, ICON_HEART, contentX, currentY, healthAnimation, HEALTH_COLOR, getHealthText(player), dims);
        currentY += dims.lineSpacing();

        drawStatLine(ctx, icons, ICON_FOOD, contentX, currentY, foodAnimation, FOOD_COLOR, getFoodText(player), dims);
        currentY += dims.lineSpacing();

        drawStatLine(ctx, icons, ICON_ARMOR, contentX, currentY, armorAnimation, ARMOR_COLOR, getArmorText(player), dims);

        if (player.getHealth() / player.getMaxHealth() <= 0.3f) {
            renderCriticalHealthEffect(matrices, boxX, boxY, boxWidth, boxHeight, dims.boxRadius());
        }
    }

    private void renderBackground(MatrixStack matrices, float x, float y, float width, float height, float radius) {
        Render2DEngine.drawRoundedBlur(
                matrices, x, y, width, height,
                radius, 2f, BACKGROUND_COLOR
        );

        Render2DEngine.drawRoundedOutline(
                matrices, x, y, width, height,
                radius, 1f, BORDER_COLOR
        );

    }

    private void drawStatLine(DrawContext ctx, FontAtlas icons, String icon, float x, float y,
                              float ratio, Color fillColor, String text, ScaledDimensions dims) {
        MatrixStack matrices = ctx.getMatrices();

        Color iconColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 255);
        icons.render(matrices, icon, x, y + 1, dims.iconSize(), iconColor.getRGB());

        float barX = x + dims.iconSize() + dims.innerPadding();
        float barY = y + (dims.iconSize() / 2f) - (dims.barHeight() / 2f);

        Render2DEngine.drawRoundedRect(matrices, barX, barY, dims.barWidth(), dims.barHeight(), dims.barHeight() / 2f,
                BAR_BACKGROUND);

        if (ratio > 0) {
            float progressWidth = dims.barWidth() * ratio;
            Render2DEngine.drawRoundedRect(matrices, barX, barY, progressWidth, dims.barHeight(), dims.barHeight() / 2f, fillColor);
        }

        if (text != null && !text.isEmpty()) {
            FontAtlas font = NoctisUIClient.getInstance().getFonts().getLucide();
            float textX = barX + dims.barWidth() + 5;
            float textY = y + 2;
            font.render(matrices, text, textX, textY, 8, Color.WHITE.getRGB());
        }
    }

    private void renderCriticalHealthEffect(MatrixStack matrices, float x, float y, float width, float height,
                                            float radius) {
        long time = System.currentTimeMillis();
        float pulse = (float) (0.5 + 0.5 * Math.sin(time * 0.005));

        Color pulseColor = new Color(255, 0, 0, (int)(pulse * 30));
        Render2DEngine.drawRoundedOutline(matrices, x - 1, y - 1, width + 2, height + 2,
                radius + 1, 2f, pulseColor);
    }

    private String getHealthText(PlayerEntity player) {
        return String.format("%.0f", player.getHealth());
    }

    private String getFoodText(PlayerEntity player) {
        return String.valueOf(player.getHungerManager().getFoodLevel());
    }

    private String getArmorText(PlayerEntity player) {
        return String.valueOf(player.getArmor());
    }
}