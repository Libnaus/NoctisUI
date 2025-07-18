package fr.libnaus.noctisui.client.component.system;

import fr.libnaus.noctisui.client.NoctisUIClient;
import fr.libnaus.noctisui.client.api.system.Render2DEngine;
import fr.libnaus.noctisui.client.api.system.render.font.FontAtlas;
import fr.libnaus.noctisui.client.common.QuickImports;
import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager implements QuickImports {

    @Getter
    private static NotificationManager instance;
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private static final int NOTIFICATION_WIDTH = 220;
    private static final int NOTIFICATION_HEIGHT = 40;
    private static final int NOTIFICATION_SPACING = 4;
    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;

    public NotificationManager() {
        instance = this;
    }

    public static void init() {
        HudRenderCallback.EVENT.register(NotificationManager::renderNotifications);
    }

    public void addNotification(String title, String message, NotificationType type) {
        notifications.add(new Notification(title, message, type));
    }

    public void addNotification(String title, String message, NotificationType type, long duration) {
        notifications.add(new Notification(title, message, type, duration));
    }

    public void success(String title, String message) {
        addNotification(title, message, NotificationType.SUCCESS);
    }

    public void error(String title, String message) {
        addNotification(title, message, NotificationType.ERROR);
    }

    public void warning(String title, String message) {
        addNotification(title, message, NotificationType.WARNING);
    }

    public void info(String title, String message) {
        addNotification(title, message, NotificationType.INFO);
    }

    private static void renderNotifications(DrawContext ctx, float tickDelta) {
        NotificationManager manager = getInstance();
        manager.update();
        manager.render(ctx.getMatrices());
    }

    private void update() {
        List<Notification> toRemove = new ArrayList<>();

        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = notifications.get(i);
            notification.setTargetY(i * (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING));
            notification.update();

            if (notification.shouldRemove()) {
                toRemove.add(notification);
            }
        }

        // Supprimer les notifications expirées
        notifications.removeAll(toRemove);
    }

    private void render(MatrixStack matrices) {
        if (notifications.isEmpty()) return;
        int screenWidth = mc.getWindow().getScaledWidth();
        int startX = screenWidth - NOTIFICATION_WIDTH - MARGIN_X;
        int startY = MARGIN_Y;

        List<Notification> notificationsCopy = new ArrayList<>(notifications);

        for (Notification notification : notificationsCopy) {
            float offsetX = notification.getSlideOffset();
            float alpha = notification.getAlpha();
            float animatedY = notification.getAnimatedY();
            int x = (int) (startX + offsetX);
            int y = (int) (startY + animatedY);
            renderNotification(matrices, notification, x, y, alpha);
        }
    }

    private void renderNotification(MatrixStack matrices, Notification notification, int x, int y, float alpha) {
        // Clamp alpha to valid range [0, 1]
        alpha = Math.max(0f, Math.min(1f, alpha));

        // Background with smoother opacity transition
        Color bgColor = new Color(28, 30, 33, (int) (245 * alpha));
        Render2DEngine.drawRoundedRect(matrices, x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 6, bgColor);

        // Border with smoother transition
        Color borderColor = new Color(45, 50, 55, (int) (160 * alpha));
        Render2DEngine.drawRoundedOutline(matrices, x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 6, 1f, borderColor);

        Color accentColor = new Color(
                notification.getColor().getRed(),
                notification.getColor().getGreen(),
                notification.getColor().getBlue(),
                (int) (255 * alpha)
        );

        // Accent line with transition
        Render2DEngine.drawRoundedRect(matrices, x + 3, y + 8, 3, NOTIFICATION_HEIGHT - 16, 2, accentColor);

        // Background area for the icon with subtle color
        Color iconBgColor = new Color(40, 45, 50, (int) (80 * alpha));
        Render2DEngine.drawRoundedRect(matrices, x + 8, y + 10, 20, 20, 4, iconBgColor);

        // Smaller icon (better centered in the area)
        renderIcon(matrices, notification.getType(), x + 18, y + 20, accentColor);

        // Title text with opacity transition (shifted to the right with more space)
        if (notification.getTitle() != null && !notification.getTitle().isEmpty()) {
            Color titleColor = new Color(248, 250, 252, (int) (255 * alpha));
            drawText(matrices, notification.getTitle(), x + 35, y + 8, titleColor, true);
        }

        // Message text with opacity transition (shifted to the right)
        if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
            Color messageColor = new Color(180, 188, 200, (int) (235 * alpha));
            drawText(matrices, notification.getMessage(), x + 35, y + 20, messageColor, false);
        }

        // Progress bar with transition
        renderProgressBar(matrices, notification, x, y, alpha);
    }

    private void renderIcon(MatrixStack matrices, NotificationType type, int x, int y, Color color) {
        FontAtlas lucide = NoctisUIClient.getInstance().getFonts().getLucide();
        lucide.render(matrices, type.getIcon(), x - 5, y - 5, 10, color.getRGB());
    }

    private void renderProgressBar(MatrixStack matrices, Notification notification, int x, int y, float alpha) {
        long elapsed = System.currentTimeMillis() - notification.getCreatedTime();
        float progress = Math.min(elapsed / (float) notification.getDuration(), 1f);
        // Arrière-plan de la barre plus visible avec hauteur augmentée
        Color trackColor = new Color(45, 50, 55, (int) (150 * alpha));
        Render2DEngine.drawRoundedRect(matrices, x + 3, y + NOTIFICATION_HEIGHT - 5, NOTIFICATION_WIDTH - 6, 3,
                2, trackColor);
        // Remplissage de progression avec couleur plus saturée et hauteur augmentée
        if (progress > 0) {
            int barWidth = (int) ((NOTIFICATION_WIDTH) * progress);
            Color progressColor = new Color(
                    notification.getColor().getRed(),
                    notification.getColor().getGreen(),
                    notification.getColor().getBlue(),
                    (int) (220 * alpha)
            );
            Render2DEngine.drawRoundedRect(matrices, x + 3, y + NOTIFICATION_HEIGHT - 5, barWidth, 3, 1, progressColor);
        }
    }

    private void drawText(MatrixStack matrices, String text, int x, int y, Color color, boolean bold) {
        FontAtlas font = bold ?
                NoctisUIClient.getInstance().getFonts().getInterBold() :
                NoctisUIClient.getInstance().getFonts().getInterMedium();
        font.render(matrices, text, x, y, color.getRGB());
    }

    public void clearAll() {
        notifications.clear();
    }
}
