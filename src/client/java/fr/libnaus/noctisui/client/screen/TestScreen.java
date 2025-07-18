package fr.libnaus.noctisui.client.screen;

import fr.libnaus.noctisui.client.component.Button;
import fr.libnaus.noctisui.client.component.DivComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.awt.*;

public class TestScreen extends Screen {

    private DivComponent div;
    private Button btn;

    public TestScreen() {
        super(Text.literal("Test"));
    }

    @Override
    protected void init() {
        float divWPixel = 751f;
        float divHPixel = 819f;
        float divWidth = divWPixel * 0.5f;
        float divHeight = divHPixel * 0.5f;
        float divX = (width - divWidth) / 2f;
        float divY = (height - divHeight) / 2f;

        div = new DivComponent(divX, divY, divWidth, divHeight)
                .withBackground(new Color(27, 26, 31, 204))
                .withCornerRadius(10f)
                .withOutline(new Color(56, 59, 76), 1f)
                .withBlur(12f);

        btn = new Button(200, 39, 10, 10, "Click Me", new Color(27, 26, 31, 80), Color.WHITE)
                .setOutline(new Color(14, 181, 81), 1f)
                .hover(200l, new Color(14, 181, 81, 150), Color.WHITE)
                .setOnClick(button -> {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("Button clicked!"), false);
                });

        div.addChild(btn);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//        super.render(context, mouseX, mouseY, delta);
        MatrixStack matrices = context.getMatrices();
        div.render(matrices, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (div.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
