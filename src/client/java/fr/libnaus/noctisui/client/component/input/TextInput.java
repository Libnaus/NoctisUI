package fr.libnaus.noctisui.client.component.input;

import fr.libnaus.noctisui.client.api.system.Render2DEngine;
import fr.libnaus.noctisui.client.api.system.render.font.FontAtlas;
import fr.libnaus.noctisui.client.common.QuickImports;
import fr.libnaus.noctisui.client.component.UIComponent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class TextInput implements UIComponent, QuickImports {

    private final float x, y, width, height;
    // Getters and Setters
    @Getter
    private String text = "";
    @Setter
    @Getter
    private String placeholder = "";
    @Getter
    private boolean focused = false;
    @Getter
    @Setter
    private boolean enabled = true;
    @Getter
    @Setter
    private boolean visible = true;

    private int cursorPosition = 0;
    private int selectionStart = 0;
    private int selectionEnd = 0;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;

    @Getter
    private int maxLength = Integer.MAX_VALUE;
    // Color setters
    @Setter
    private Color backgroundColor = new Color(40, 40, 40, 180);
    @Setter
    private Color focusedBackgroundColor = new Color(50, 50, 50, 200);
    @Setter
    private Color borderColor = new Color(60, 60, 60, 255);
    @Setter
    private Color focusedBorderColor = new Color(100, 150, 255, 255);
    @Setter
    private Color textColor = new Color(255, 255, 255, 255);
    @Setter
    private Color placeholderColor = new Color(150, 150, 150, 255);
    @Setter
    private Color selectionColor = new Color(100, 150, 255, 100);
    @Setter
    private Color cursorColor = new Color(255, 255, 255, 255);

    private float borderRadius = 4.0f;
    private float borderWidth = 1.0f;
    private final float padding = 8.0f;
    @Getter
    @Setter
    private float fontSize = 9.0f;

    private final FontAtlas fontAtlas;

    // Scroll offset for long text
    private float scrollOffset = 0.0f;

    public TextInput(float x, float y, float width, float height, FontAtlas fontAtlas) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fontAtlas = fontAtlas;
    }

    @Override
    public void render(MatrixStack matrices, double mouseX, double mouseY, float delta) {
        if (!visible) return;

        // Update cursor blink
        updateCursorBlink();

        // Background
        Color bgColor = focused ? focusedBackgroundColor : backgroundColor;
        Render2DEngine.drawRoundedRect(matrices, x, y, width, height, borderRadius, bgColor);

        // Border
        Color bColor = focused ? focusedBorderColor : borderColor;
        Render2DEngine.drawRoundedOutline(matrices, x, y, width, height, borderRadius, borderWidth, bColor);

        // Text rendering area
        float textX = x + padding - scrollOffset;
        float textY = y + (height - fontAtlas.getLineHeight(fontSize)) / 2;
        float textAreaWidth = width - (padding * 2);

        // Scissor test for text clipping
        enableScissor(x + padding, y, textAreaWidth, height);

        // Selection background
        if (hasSelection() && focused) {
            renderSelection(matrices, textX, textY);
        }

        // Text or placeholder
        if (text.isEmpty() && !placeholder.isEmpty() && !focused) {
            fontAtlas.render(matrices, placeholder, textX, textY, fontSize,
                    placeholderColor.getRGB() | (placeholderColor.getAlpha() << 24));
        } else if (!text.isEmpty()) {
            fontAtlas.render(matrices, text, textX, textY, fontSize,
                    textColor.getRGB() | (textColor.getAlpha() << 24));
        }

        // Cursor
        if (focused && cursorVisible && enabled) {
            renderCursor(matrices, textX, textY);
        }

        disableScissor();
    }

    private void updateCursorBlink() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorBlink > 530) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = currentTime;
        }
    }

    private void renderSelection(MatrixStack matrices, float textX, float textY) {
        if (!hasSelection()) return;

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        String beforeSelection = text.substring(0, start);
        String selection = text.substring(start, end);

        float selectionStartX = textX + fontAtlas.getWidth(beforeSelection, fontSize);
        float selectionWidth = fontAtlas.getWidth(selection, fontSize);

        Render2DEngine.drawRect(matrices, selectionStartX, textY - 2,
                selectionWidth, fontAtlas.getLineHeight(fontSize), selectionColor);
    }

    private void renderCursor(MatrixStack matrices, float textX, float textY) {
        String beforeCursor = text.substring(0, cursorPosition);
        float cursorX = textX + fontAtlas.getWidth(beforeCursor, fontSize);

        Render2DEngine.drawRect(matrices, cursorX, textY, 1.0f,
                fontAtlas.getLineHeight(fontSize), cursorColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        boolean wasInBounds = isPointInBounds(mouseX, mouseY);

        if (wasInBounds && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (!focused) {
                setFocused(true);
            }

            // Calculate cursor position from mouse click
            float relativeX = (float) mouseX - (x + padding) + scrollOffset;
            setCursorFromPosition(relativeX);

            // Clear selection
            clearSelection();
            return true;
        } else if (!wasInBounds) {
            setFocused(false);
        }

        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused || !enabled) return false;

        boolean ctrlPressed = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT:
                if (shiftPressed) {
                    extendSelection(-1);
                } else {
                    clearSelection();
                    moveCursor(-1);
                }
                return true;

            case GLFW.GLFW_KEY_RIGHT:
                if (shiftPressed) {
                    extendSelection(1);
                } else {
                    clearSelection();
                    moveCursor(1);
                }
                return true;

            case GLFW.GLFW_KEY_HOME:
                if (shiftPressed) {
                    setSelection(cursorPosition, 0);
                } else {
                    clearSelection();
                }
                setCursorPosition(0);
                return true;

            case GLFW.GLFW_KEY_END:
                if (shiftPressed) {
                    setSelection(cursorPosition, text.length());
                } else {
                    clearSelection();
                }
                setCursorPosition(text.length());
                return true;

            case GLFW.GLFW_KEY_BACKSPACE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                    moveCursor(-1);
                }
                return true;

            case GLFW.GLFW_KEY_DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                }
                return true;

            case GLFW.GLFW_KEY_A:
                if (ctrlPressed) {
                    selectAll();
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_C:
                if (ctrlPressed && hasSelection()) {
                    copySelection();
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_V:
                if (ctrlPressed) {
                    paste();
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_X:
                if (ctrlPressed && hasSelection()) {
                    cutSelection();
                    return true;
                }
                break;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused || !enabled) return false;

        if (isValidChar(chr)) {
            insertText(String.valueOf(chr));
            return true;
        }

        return false;
    }

    private boolean isValidChar(char chr) {
        return chr >= 32 && chr != 127; // Printable ASCII characters
    }

    private void insertText(String str) {
        if (hasSelection()) {
            deleteSelection();
        }

        if (text.length() + str.length() <= maxLength) {
            text = text.substring(0, cursorPosition) + str + text.substring(cursorPosition);
            moveCursor(str.length());
        }
    }

    private void setCursorFromPosition(float x) {
        float currentWidth = 0;
        int position = 0;

        for (int i = 0; i < text.length(); i++) {
            float charWidth = fontAtlas.getWidth(text.substring(i, i + 1), fontSize);
            if (currentWidth + charWidth / 2 > x) {
                break;
            }
            currentWidth += charWidth;
            position = i + 1;
        }

        setCursorPosition(Math.max(0, Math.min(position, text.length())));
    }

    private void moveCursor(int delta) {
        setCursorPosition(cursorPosition + delta);
    }

    private void setCursorPosition(int position) {
        cursorPosition = Math.max(0, Math.min(position, text.length()));
        resetCursorBlink();
        updateScrollOffset();
    }

    private void resetCursorBlink() {
        cursorVisible = true;
        lastCursorBlink = System.currentTimeMillis();
    }

    private void updateScrollOffset() {
        String beforeCursor = text.substring(0, cursorPosition);
        float cursorX = fontAtlas.getWidth(beforeCursor, fontSize);
        float visibleWidth = width - (padding * 2);

        // Scroll right if cursor is past right edge
        if (cursorX - scrollOffset > visibleWidth - 10) {
            scrollOffset = cursorX - visibleWidth + 10;
        }
        // Scroll left if cursor is past left edge
        else if (cursorX - scrollOffset < 0) {
            scrollOffset = Math.max(0, cursorX - 10);
        }
    }

    private boolean hasSelection() {
        return selectionStart != selectionEnd;
    }

    private void clearSelection() {
        selectionStart = selectionEnd = cursorPosition;
    }

    private void setSelection(int start, int end) {
        selectionStart = Math.max(0, Math.min(start, text.length()));
        selectionEnd = Math.max(0, Math.min(end, text.length()));
    }

    private void extendSelection(int direction) {
        if (!hasSelection()) {
            selectionStart = cursorPosition;
        }
        moveCursor(direction);
        selectionEnd = cursorPosition;
    }

    private void selectAll() {
        setSelection(0, text.length());
        setCursorPosition(text.length());
    }

    private void deleteSelection() {
        if (!hasSelection()) return;

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        text = text.substring(0, start) + text.substring(end);
        setCursorPosition(start);
        clearSelection();
    }

    private void copySelection() {
        if (!hasSelection()) return;

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        String selection = text.substring(start, end);

        // Copy to clipboard (vous pourriez utiliser une librairie comme LWJGL pour cela)
        GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), selection);
    }

    private void paste() {
        String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (clipboardText != null && !clipboardText.isEmpty()) {
            clipboardText = clipboardText.replaceAll("[\\r\\n]", "");
            insertText(clipboardText);
        }
    }

    private void cutSelection() {
        copySelection();
        deleteSelection();
    }

    private boolean isPointInBounds(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private void enableScissor(float x, float y, float width, float height) {
        double scale = mc.getWindow().getScaleFactor();
        int scissorX = (int) (x * scale);
        int scissorY = (int) ((mc.getWindow().getScaledHeight() - (y + height)) * scale);
        int scissorWidth = (int) (width * scale);
        int scissorHeight = (int) (height * scale);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            resetCursorBlink();
        }
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        setCursorPosition(Math.min(cursorPosition, this.text.length()));
    }

    public void setMaxLength(int maxLength) { this.maxLength = Math.max(0, maxLength); }

}