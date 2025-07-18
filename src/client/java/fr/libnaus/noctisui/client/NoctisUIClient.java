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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Vérifie si RIGHT SHIFT est pressé
            long handle = client.getWindow().getHandle();
            boolean isPressed = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            // Détection de pression (pas maintien)
            if (isPressed && !shiftPressedLastTick) {
                (new Thread(() -> {
                    try {
                        notificationManager.success("Succès", "Opération terminée avec succès !");
                        Thread.sleep(1000L);
                        notificationManager.error("Erreur", "Une erreur s'est produite");
                        Thread.sleep(1000L);
                        notificationManager.warning("Attention", "Ceci est un avertissement");
                        Thread.sleep(1000L);
                        notificationManager.info("Information", "Voici une information utile");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        notificationManager.error("Erreur", "Le thread a été interrompu");
                    }
                }, "NotificationThread")).start();
            }

            // Mise à jour du dernier état
            shiftPressedLastTick = isPressed;
        });
    }
}
