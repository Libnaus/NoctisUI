package fr.libnaus.noctisui.client.component;

import net.minecraft.client.util.math.MatrixStack;

public interface UIComponent {

    void render(MatrixStack matrices, double mouseX, double mouseY, float delta);

    boolean mouseClicked(double mouseX, double mouseY, int button);

}
