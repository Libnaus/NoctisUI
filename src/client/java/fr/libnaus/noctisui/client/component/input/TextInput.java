package fr.libnaus.noctisui.client.component.input;

import fr.libnaus.noctisui.client.NoctisUIClient;
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
import java.util.regex.Pattern;

public class TextInput implements UIComponent, QuickImports {

    public enum InputType {
        TEXT,
        PASSWORD,
        NUMBER,
        EMAIL,
        URL,
        SEARCH
    }

    private final float x, y, width, height;

    @Setter
    @Getter
    private InputType inputType = InputType.TEXT;

    @Getter
    private boolean passwordVisible = false;
    private final float eyeIconSize = 12.0f;
    private final float eyeIconPadding = 6.0f;

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

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$"
    );

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "^-?\\d*\\.?\\d*$"
    );

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
    @Setter
    private Color iconColor = new Color(180, 180, 180, 255);
    @Setter
    private Color iconHoverColor = new Color(255, 255, 255, 255);
    @Setter
    private Color validBorderColor = new Color(100, 255, 100, 255);
    @Setter
    private Color invalidBorderColor = new Color(255, 100, 100, 255);

    private float borderRadius = 4.0f;
    private float borderWidth = 1.0f;
    private final float padding = 8.0f;
    @Getter
    @Setter
    private float fontSize = 9.0f;

    private final FontAtlas fontAtlas;
    private final FontAtlas lucideIcon = NoctisUIClient.getInstance().getFonts().getLucide();

    private final String eyeOpenIcon = "\uE9B7";
    private final String eyeClosedIcon = "\uE9B6";
    private final String searchIcon = "\uEA98";

    private final String chevronUpIcon = "\uE95A";
    private final String chevronDownIcon = "\uE955";

    private float scrollOffset = 0.0f;

    @Getter
    private boolean showValidation = false;
    @Setter
    private boolean validateOnType = false;

    public TextInput(float x, float y, float width, float height, FontAtlas fontAtlas) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fontAtlas = fontAtlas;
    }

    public TextInput(float x, float y, float width, float height, FontAtlas fontAtlas, InputType inputType) {
        this(x, y, width, height, fontAtlas);
        this.inputType = inputType;
        setupForInputType();
    }

    private void setupForInputType() {
        switch (inputType) {
            case PASSWORD:
                if (placeholder.isEmpty()) placeholder = "Entrez votre mot de passe";
                break;
            case EMAIL:
                if (placeholder.isEmpty()) placeholder = "Entrez votre email";
                break;
            case URL:
                if (placeholder.isEmpty()) placeholder = "Entrez une URL";
                break;
            case NUMBER:
                if (placeholder.isEmpty()) placeholder = "0";
                break;
            case SEARCH:
                if (placeholder.isEmpty()) placeholder = "Rechercher...";
                break;
        }
    }

    @Override
    public void render(MatrixStack matrices, double mouseX, double mouseY, float delta) {
        if (!visible) return;

        // Update cursor blink
        updateCursorBlink();

        Color bColor = getBorderColor();

        // Background
        Color bgColor = focused ? focusedBackgroundColor : backgroundColor;
        Render2DEngine.drawRoundedRect(matrices, x, y, width, height, borderRadius, bgColor);

        // Border
        Render2DEngine.drawRoundedOutline(matrices, x, y, width, height, borderRadius, borderWidth, bColor);

        // Calculate text area considering icons
        float rightPadding = getRightPadding();
        float textX = x + padding - scrollOffset;
        float textY = y + (height - fontAtlas.getLineHeight(fontSize)) / 2;
        float textAreaWidth = width - padding - rightPadding;

        // Scissor test for text clipping
        enableScissor(x + padding, y, textAreaWidth, height);

        // Selection background
        if (hasSelection() && focused)
            renderSelection(matrices, textX, textY);

        // Text or placeholder
       String displayText = getDisplayText();
       if (displayText.isEmpty() && !placeholder.isEmpty() && !focused)
           fontAtlas.render(matrices, placeholder, textX, textY, fontSize,
                   placeholderColor.getRGB() | (placeholderColor.getAlpha() << 24));
       else if (!displayText.isEmpty())
              fontAtlas.render(matrices, displayText, textX, textY, fontSize,
                     textColor.getRGB() | (textColor.getAlpha() << 24));

        // Cursor
        if (focused && cursorVisible && enabled)
            renderCursor(matrices, textX, textY);

        disableScissor();

        renderTypeSpecificIcons(matrices, mouseX, mouseY);
    }

    private String getDisplayText() {
        if (inputType == InputType.PASSWORD && !passwordVisible) {
            return "·".repeat(text.length());
        }
        return text;
    }

    private Color getBorderColor() {
        if (focused) {
            return focusedBorderColor;
        } else if (showValidation && validateOnType && !text.isEmpty()) {
            return isValid() ? validBorderColor : invalidBorderColor;
        }
        return borderColor;
    }

    private float getRightPadding() {
        return switch (inputType) {
            case PASSWORD, SEARCH, NUMBER -> padding + eyeIconSize + eyeIconPadding;
            default -> padding;
        };
    }

    private void renderTypeSpecificIcons(MatrixStack matrices, double mouseX, double mouseY) {
        switch (inputType) {
            case NUMBER:
                renderChevronIcons(matrices, mouseX, mouseY);
                break;
            case PASSWORD:
                renderPasswordIcon(matrices, mouseX, mouseY);
                break;
            case SEARCH:
                renderSearchIcon(matrices, mouseX, mouseY);
                break;
        }
    }

    private void renderPasswordIcon(MatrixStack matrices, double mouseX, double mouseY) {
        float iconX = x + width - padding - eyeIconSize;
        float iconY = y + (height - eyeIconSize) / 2;

        boolean isHovering = mouseX >= iconX && mouseX <= iconX + eyeIconSize &&
                mouseY >= iconY && mouseY <= iconY + eyeIconSize;

        Color currentIconColor = isHovering ? iconHoverColor : iconColor;

        if (passwordVisible) {
            renderEyeOpenIcon(matrices, iconX, iconY, currentIconColor);
        } else {
            renderEyeClosedIcon(matrices, iconX, iconY, currentIconColor);
        }
    }

    private void renderSearchIcon(MatrixStack matrices, double mouseX, double mouseY) {
        float iconX = x + width - padding - eyeIconSize;
        float iconY = y + (height - eyeIconSize) / 2;

        lucideIcon.render(
                matrices, searchIcon, iconX, iconY, eyeIconSize,
                (mouseX >= iconX && mouseX <= iconX + eyeIconSize &&
                        mouseY >= iconY && mouseY <= iconY + eyeIconSize) ?
                        iconHoverColor.getRGB() | (iconHoverColor.getAlpha() << 24) :
                        iconColor.getRGB() | (iconColor.getAlpha() << 24)
        );
    }

    private void renderChevronIcons(MatrixStack matrices, double mouseX, double mouseY) {
        float iconSize = 12.0f;

        float totalIconAreaWidth = padding + iconSize + eyeIconPadding;
        float iconAreaX = x + width - totalIconAreaWidth;

        // Diviser la hauteur en deux zones égales
        float halfHeight = height / 2;
        float chevronUpAreaY = y;
        float chevronDownAreaY = y + halfHeight;

        // Vérifier le survol pour chaque zone
        boolean hoverUp = mouseX >= iconAreaX && mouseX <= iconAreaX + totalIconAreaWidth &&
                mouseY >= chevronUpAreaY && mouseY <= chevronUpAreaY + halfHeight;
        boolean hoverDown = mouseX >= iconAreaX && mouseX <= iconAreaX + totalIconAreaWidth &&
                mouseY >= chevronDownAreaY && mouseY <= chevronDownAreaY + halfHeight;

        // Centrer les icônes dans leur zone respective
        float iconX = iconAreaX + (totalIconAreaWidth - iconSize) / 2;
        float iconYUp = chevronUpAreaY + (halfHeight - iconSize) / 2;
        float iconYDown = chevronDownAreaY + (halfHeight - iconSize) / 2;

        // Rendu des icônes chevron
        lucideIcon.render(matrices, chevronUpIcon, iconX, iconYUp, iconSize,
                hoverUp ? iconHoverColor.getRGB() | (iconHoverColor.getAlpha() << 24)
                        : iconColor.getRGB() | (iconColor.getAlpha() << 24));
        lucideIcon.render(matrices, chevronDownIcon, iconX, iconYDown, iconSize,
                hoverDown ? iconHoverColor.getRGB() | (iconHoverColor.getAlpha() << 24)
                        : iconColor.getRGB() | (iconColor.getAlpha() << 24));
    }

    private void renderEyeOpenIcon(MatrixStack matrices, float x, float y, Color color) {
        lucideIcon.render(
                matrices, eyeOpenIcon, x, y, eyeIconSize,
                color.getRGB() | (color.getAlpha() << 24)
        );
    }

    private void renderEyeClosedIcon(MatrixStack matrices, float x, float y, Color color) {
        lucideIcon.render(
                matrices, eyeClosedIcon, x, y, eyeIconSize,
                color.getRGB() | (color.getAlpha() << 24)
        );
    }

    private void renderSelection(MatrixStack matrices, float textX, float textY) {
        if (!hasSelection()) return;

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        String displayText = getDisplayText();
        String beforeSelection = displayText.substring(0, start);
        String selection = displayText.substring(start, end);

        float selectionStartX = textX + fontAtlas.getWidth(beforeSelection, fontSize);
        float selectionWidth = fontAtlas.getWidth(selection, fontSize);

        Render2DEngine.drawRect(matrices, selectionStartX, textY,
                selectionWidth, fontAtlas.getLineHeight(fontSize), selectionColor);
    }

    private void renderCursor(MatrixStack matrices, float textX, float textY) {
        String displayText = getDisplayText();
        String beforeCursor = displayText.substring(0, cursorPosition);
        float cursorX = textX + fontAtlas.getWidth(beforeCursor, fontSize);

        Render2DEngine.drawRect(matrices, cursorX, textY, 1.0f,
                fontAtlas.getLineHeight(fontSize), cursorColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        boolean wasInBounds = isPointInBounds(mouseX, mouseY);

        if (wasInBounds && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (inputType == InputType.PASSWORD && isClickingPasswordIcon(mouseX, mouseY)) {
                togglePasswordVisibility();
                return true;
            }

            if (inputType == InputType.NUMBER) {
                int chevron = getClickedNumberChevron(mouseX, mouseY);
                if (chevron != 0) {
                    changeNumberValue(chevron);
                    return true;
                }
            }

            if (!focused) {
                setFocused(true);
            }

            float relativeX = (float) mouseX - (x + padding) + scrollOffset;
            setCursorFromPosition(relativeX);

            clearSelection();
            return true;
        } else if (!wasInBounds) {
            setFocused(false);
        }

        return false;
    }

    private int getClickedNumberChevron(double mouseX, double mouseY) {
        float totalIconAreaWidth = padding + eyeIconSize + eyeIconPadding;
        float iconAreaX = x + width - totalIconAreaWidth;

        float halfHeight = height / 2;
        float chevronUpAreaY = y;
        float chevronDownAreaY = y + halfHeight;

        if (mouseX >= iconAreaX && mouseX <= iconAreaX + totalIconAreaWidth) {
            if (mouseY >= chevronUpAreaY && mouseY <= chevronUpAreaY + halfHeight) {
                return 1; // Chevron haut
            } else if (mouseY >= chevronDownAreaY && mouseY <= chevronDownAreaY + halfHeight) {
                return -1; // Chevron bas
            }
        }
        return 0;
    }

    private void changeNumberValue(int delta) {
        try {
            double value = text.isEmpty() ? 0 : Double.parseDouble(text);
            value += delta;
            text = String.valueOf((value % 1 == 0) ? (int) value : value);
            setCursorPosition(text.length());
        } catch (NumberFormatException e) {
            text = "0";
            setCursorPosition(1);
        }
    }

    private boolean isClickingPasswordIcon(double mouseX, double mouseY) {
        float iconX = x + width - padding - eyeIconSize;
        float iconY = y + (height - eyeIconSize) / 2;
        return mouseX >= iconX && mouseX <= iconX + eyeIconSize &&
                mouseY >= iconY && mouseY <= iconY + eyeIconSize;
    }

    public void togglePasswordVisibility() {
        if (inputType == InputType.PASSWORD) {
            passwordVisible = !passwordVisible;
        }
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

        if (isValidCharForType(chr)) {
            String newChar = String.valueOf(chr);
            if (isValidInputForType(text + newChar)) {
                insertText(newChar);
                return true;
            }
        }

        return false;
    }

    private boolean isValidCharForType(char chr) {
        if (chr < 32 || chr == 127) return false;

        return switch (inputType) {
            case NUMBER -> Character.isDigit(chr) || chr == '.' || chr == '-';
            case EMAIL -> Character.isLetterOrDigit(chr) || "._@+-".indexOf(chr) >= 0;
            case URL -> chr != ' ';
            default -> true;
        };
    }

    private boolean isValidInputForType(String input) {
        return switch (inputType) {
            case NUMBER -> NUMBER_PATTERN.matcher(input).matches();
            case EMAIL -> input.matches("^[a-zA-Z0-9._%+-]*@?[a-zA-Z0-9.-]*\\.?[a-zA-Z]*$");
            default -> true;
        };
    }

    public boolean isValid() {
        if (text.isEmpty()) return true;

        return switch (inputType) {
            case EMAIL -> EMAIL_PATTERN.matcher(text).matches();
            case URL -> URL_PATTERN.matcher(text).matches();
            case NUMBER -> NUMBER_PATTERN.matcher(text).matches() && !text.equals("-") && !text.equals(".");
            default -> true;
        };
    }

    private void updateCursorBlink() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorBlink > 530) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = currentTime;
        }
    }

    private void insertText(String str) {
        if (hasSelection()) {
            deleteSelection();
        }

        if (text.length() + str.length() <= maxLength) {
            text = text.substring(0, cursorPosition) + str + text.substring(cursorPosition);
            moveCursor(str.length());

            if (validateOnType) {
                showValidation = true;
            }
        }
    }

    private void setCursorFromPosition(float x) {
        String displayText = getDisplayText();
        float currentWidth = 0;
        int position = 0;

        for (int i = 0; i < displayText.length(); i++) {
            float charWidth = fontAtlas.getWidth(displayText.substring(i, i + 1), fontSize);
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

        if (cursorX - scrollOffset > visibleWidth - 10) {
            scrollOffset = cursorX - visibleWidth + 10;
        } else if (cursorX - scrollOffset < 0) {
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

        GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), selection);
    }

    private void paste() {
        String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (clipboardText != null && !clipboardText.isEmpty()) {
            clipboardText = clipboardText.replaceAll("[\\r\\n]", "");

            StringBuilder filteredText = new StringBuilder();
            for (char c : clipboardText.toCharArray()) {
                if (isValidCharForType(c)) {
                    filteredText.append(c);
                }
            }

            if (!filteredText.isEmpty()) {
                insertText(filteredText.toString());
            }
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