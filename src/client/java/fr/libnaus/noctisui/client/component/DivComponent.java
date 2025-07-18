package fr.libnaus.noctisui.client.component;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.libnaus.noctisui.client.api.system.Render2DEngine;
import fr.libnaus.noctisui.client.common.QuickImports;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DivComponent implements QuickImports, UIComponent {

    // Getters et setters
    // Position et dimensions
    @Getter
    private float x, y, width, height;

    // Options de style
    @Getter
    private Color backgroundColor;
    @Setter
    private boolean hasBackground = false;
    @Getter
    @Setter
    private float cornerRadius = 0f;

    @Setter
    private boolean hasOutline = false;
    @Setter
    @Getter
    private Color outlineColor;
    @Setter
    @Getter
    private float outlineWidth;

    @Setter
    private boolean hasBlur = false;
    @Setter
    @Getter
    private float blurRadius;
    private float blurOpacity = 1.0f;

    // Composants enfants
    private List<UIComponent> children = new ArrayList<>();

    // Contenu personnalisé (optionnel)
    private Runnable customRenderer;

    public DivComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Méthodes de configuration du style (pattern builder)
    public DivComponent withBackground(Color color) {
        this.backgroundColor = color;
        this.hasBackground = true;
        return this;
    }

    public DivComponent withCornerRadius(float radius) {
        this.cornerRadius = radius;
        return this;
    }

    public DivComponent withOutline(Color color, float width) {
        this.outlineColor = color;
        this.outlineWidth = width;
        this.hasOutline = true;
        return this;
    }

    public DivComponent withBlur(float radius) {
        this.blurRadius = radius;
        this.hasBlur = true;
        return this;
    }

    public DivComponent withBlur(float radius, float opacity) {
        this.blurRadius = radius;
        this.blurOpacity = opacity;
        this.hasBlur = true;
        return this;
    }

    public DivComponent withCustomRenderer(Runnable renderer) {
        this.customRenderer = renderer;
        return this;
    }

    // Gestion des enfants
    public DivComponent addChild(UIComponent child) {
        children.add(child);
        return this;
    }

    public DivComponent removeChild(UIComponent child) {
        children.remove(child);
        return this;
    }

    public void clearChildren() {
        children.clear();
    }

    @Override
    public void render(MatrixStack matrices, double mouseX, double mouseY, float delta) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (hasBackground && hasBlur) {
            Render2DEngine.drawBlurredRoundedRect(
                    matrices, x, y, width, height,
                    cornerRadius, blurRadius, blurOpacity, backgroundColor
            );
        }

        matrices.push();
        matrices.translate(x, y, 0);

        // Rendu du background avec blur si nécessaire
        if (hasBackground && !hasBlur) {
            if (cornerRadius > 0) {
                Render2DEngine.drawRoundedRect(matrices, 0, 0, width, height, cornerRadius, backgroundColor);
            } else {
                Render2DEngine.drawRect(matrices, 0, 0, width, height, backgroundColor);
            }
        }

        // Rendu de l'outline
        if (hasOutline) {
            if (cornerRadius > 0) {
                Render2DEngine.drawRoundedOutline(matrices, 0, 0, width, height, cornerRadius, outlineWidth, outlineColor);
            } else {
                Render2DEngine.drawOutline(matrices, 0, 0, width, height, outlineWidth, outlineColor);
            }
        }

        // Rendu du contenu personnalisé
        if (customRenderer != null) {
            customRenderer.run();
        }

        // Rendu des composants enfants
        for (UIComponent child : children) {
            child.render(matrices, mouseX - x, mouseY - y, delta);
        }

        // Restaurer l'état de la matrice
        matrices.pop();
        RenderSystem.disableBlend();
    }

    // Méthodes utilitaires
    public boolean contains(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void onClick(float mouseX, float mouseY) {
        if (contains(mouseX, mouseY)) {
            for (UIComponent child : children) {
                child.mouseClicked(mouseX - x, mouseY - y, 0);
            }
        }
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.hasBackground = backgroundColor != null;
    }

    public boolean hasBackground() { return hasBackground; }

    public boolean hasOutline() { return hasOutline; }

    public boolean hasBlur() { return hasBlur; }

    public List<UIComponent> getChildren() { return new ArrayList<>(children); }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (contains((float) mouseX - x, (float) mouseY - y)) {
            for (UIComponent child : children) {
                child.mouseClicked(mouseX - x, mouseY - y, 0);
            }
            return true;
        }
        return false;
    }
}