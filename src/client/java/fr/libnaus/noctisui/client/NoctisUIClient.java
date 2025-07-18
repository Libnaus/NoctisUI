package fr.libnaus.noctisui.client;

import fr.libnaus.noctisui.client.api.system.Shaders;
import fr.libnaus.noctisui.client.api.system.render.font.Fonts;
import fr.libnaus.noctisui.client.component.system.NotificationManager;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import fr.libnaus.noctisui.client.hud.HudOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;

public class NoctisUIClient implements ClientModInitializer {

    @Getter
    private static NoctisUIClient instance;

    @Getter
    private Fonts fonts;

    private boolean shiftPressedLastTick = false;

    private NotificationManager notificationManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        notificationManager = new NotificationManager();
        NotificationManager.init();
        this.fonts = new Fonts();
        Shaders.load();
        HudRenderCallback.EVENT.register(new HudOverlay());
    }
}
