package fr.libnaus.noctisui.client.component;

import fr.libnaus.noctisui.client.NoctisUIClient;
import fr.libnaus.noctisui.client.api.system.Render2DEngine;
import fr.libnaus.noctisui.client.api.system.render.font.FontAtlas;
import fr.libnaus.noctisui.client.common.QuickImports;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.function.Consumer;

public class Button implements QuickImports, UIComponent {

    private float width, height, x, y;
    private String label;
    private Color labelColor;
    private Color backgroundColor;

    // Outline properties
    private boolean hasOutline = false;
    private Color outlineColor;
    private float outlineWidth;

    // Blur properties
    private boolean hasBlur = false;
    private float blurRadius;

    // Font properties
    private FontAtlas font = NoctisUIClient.getInstance().getFonts().getInterMedium();

    // Hover properties
    private boolean hasHover = false;
    private long hoverAnimationDuration; // in milliseconds
    private Color hoverBackgroundColor;
    private Color hoverLabelColor;

    // Internal state for hover animation
    private long hoverStartTime = -1;
    private boolean isHovered = false;

    // Action to perform on click
    private Consumer<Button> onClickAction;

    /**
     * Creates a new button instance.
     *
     * @param width The width of the button.
     * @param height The height of the button.
     * @param x The X-coordinate of the button's top-left corner.
     * @param y The Y-coordinate of the button's top-left corner.
     * @param label The text label displayed on the button.
     * @param backgroundColor The background color of the button.
     * @param labelColor The color of the button's label.
     */
    public Button(float width, float height, float x, float y, String label, Color backgroundColor, Color labelColor) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.label = label;
        this.backgroundColor = backgroundColor;
        this.labelColor = labelColor;
    }

    /**
     * Sets the outline properties for the button.
     *
     * @param outlineColor The color of the outline.
     * @param outlineWidth The width of the outline.
     * @return This Button instance for chaining.
     */
    public Button setOutline(Color outlineColor, float outlineWidth) {
        this.hasOutline = true;
        this.outlineColor = outlineColor;
        this.outlineWidth = outlineWidth;
        return this;
    }

    /**
     * Sets the blur properties for the button.
     *
     * @param blurRadius The radius of the blur effect.
     * @return This Button instance for chaining.
     */
    public Button setBlur(float blurRadius) {
        this.hasBlur = true;
        this.blurRadius = blurRadius;
        return this;
    }

    /**
     * Sets the font for the button's label.
     *
     * @param font The FontAtlas to use for rendering the label.
     * @return This Button instance for chaining.
     */
    public Button setFont(FontAtlas font) {
        this.font = font;
        return this;
    }

    /**
     * Configures hover effects for the button.
     *
     * @param animationDuration The duration of the hover animation in milliseconds.
     * @param hoverBackgroundColor The background color when hovered.
     * @param hoverLabelColor The label color when hovered.
     * @return This Button instance for chaining.
     */
    public Button hover(long animationDuration, Color hoverBackgroundColor, Color hoverLabelColor) {
        this.hasHover = true;
        this.hoverAnimationDuration = animationDuration;
        this.hoverBackgroundColor = hoverBackgroundColor;
        this.hoverLabelColor = hoverLabelColor;
        return this;
    }

    /**
     * Sets the action to be performed when the button is clicked.
     *
     * @param action A Consumer that accepts the Button instance when clicked.
     * @return This Button instance for chaining.
     */
    public Button setOnClick(Consumer<Button> action) {
        this.onClickAction = action;
        return this;
    }

    @Override
    public void render(MatrixStack matrices, double mouseX, double mouseY, float delta) {
// Determine current colors based on hover state
        Color currentBackgroundColor = backgroundColor;
        Color currentLabelColor = labelColor;

        boolean isMouseOver = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        if (hasHover) {
            if (isMouseOver && !isHovered) {
                // Mouse entered button area
                isHovered = true;
                hoverStartTime = System.currentTimeMillis();
            } else if (!isMouseOver && isHovered) {
                // Mouse left button area
                isHovered = false;
                hoverStartTime = System.currentTimeMillis(); // Reset for smooth transition out
            }

            if (isHovered && hoverStartTime != -1) {
                long elapsed = System.currentTimeMillis() - hoverStartTime;
                float progress = Math.min(1f, (float) elapsed / hoverAnimationDuration);

                // Interpolate colors
                currentBackgroundColor = interpolateColor(backgroundColor, hoverBackgroundColor, progress);
                currentLabelColor = interpolateColor(labelColor, hoverLabelColor, progress);
            } else if (!isHovered && hoverStartTime != -1) {
                long elapsed = System.currentTimeMillis() - hoverStartTime;
                float progress = Math.min(1f, (float) elapsed / hoverAnimationDuration);
                // Interpolate back to original colors
                currentBackgroundColor = interpolateColor(hoverBackgroundColor, backgroundColor, progress);
                currentLabelColor = interpolateColor(hoverLabelColor, labelColor, progress);
                if (progress == 1f) {
                    hoverStartTime = -1; // Animation finished
                }
            }
        }

        // Draw the button background
        if (hasBlur) {
            Render2DEngine.drawBlurredRoundedRect(matrices, x, y, width, height, 5.0f, blurRadius,
             1.0F, currentBackgroundColor); // Using a fixed radius for blur, adjust as needed
        } else {
            Render2DEngine.drawRoundedRect(matrices, x, y, width, height, 5.0f, currentBackgroundColor); // Using a fixed radius, adjust as needed
        }

        // Draw the outline if enabled
        if (hasOutline) {
            Render2DEngine.drawRoundedOutline(matrices, x, y, width, height, 5.0f, outlineWidth, outlineColor); // Using a fixed radius, adjust as needed
        }

        if (font != null) {
            float textWidth = font.getWidth(label);
            float textHeight = font.getLineHeight();
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2;
            font.render(matrices, label, textX, textY, currentLabelColor.getRGB());
        } else {
            float textWidth = mc.textRenderer.getWidth(label);
            float textHeight = mc.textRenderer.fontHeight;
            float textX = x + (width - textWidth) / 2;
            float textY = y + (height - textHeight) / 2;
            font.renderWithShadow(matrices, label, textX, textY, currentLabelColor.getRGB());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (onClickAction != null) {
                onClickAction.accept(this);
            }
            return true;
        }
        return false;
    }

    /**
     * Helper method to interpolate between two colors.
     */
    private Color interpolateColor(Color color1, Color color2, float progress) {
        float r = color1.getRed() + (color2.getRed() - color1.getRed()) * progress;
        float g = color1.getGreen() + (color2.getGreen() - color1.getGreen()) * progress;
        float b = color1.getBlue() + (color2.getBlue() - color1.getBlue()) * progress;
        float a = color1.getAlpha() + (color2.getAlpha() - color1.getAlpha()) * progress;
        return new Color((int) r, (int) g, (int) b, (int) a);
    }

    // Getters for properties (optional, but good practice)
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public String getLabel() { return label; }
}