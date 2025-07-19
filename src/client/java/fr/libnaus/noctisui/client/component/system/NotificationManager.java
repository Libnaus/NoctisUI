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
    private static final int NOTIFICATION_SPACING = 6;
    private static final int MARGIN_X = 12;
    private static final int MARGIN_Y = 12;

    public NotificationManager() {
        instance = this;
    }

    public static void init() {
        HudRenderCallback.EVENT.register(NotificationManager::renderNotifications);
    }

    public void addNotification(String title, String message, NotificationType type) {
        addNotification(title, message, type, 3000);
    }

    public void addNotification(String title, String message, NotificationType type, long duration) {
        Notification newNotification = new Notification(title, message, type, duration);

        // Vérifie si une notification similaire existe déjà
        for (Notification existing : notifications) {
            if (existing.isSimilarTo(newNotification)) {
                existing.incrementStack();
                return; // On ne crée pas de nouvelle notification
            }
        }

        // Aucune notification similaire trouvée, on ajoute la nouvelle
        notifications.add(newNotification);
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
        List<Notification> visibleNotif = new ArrayList<>();

        for (Notification notification : notifications) {
            notification.update();
            if (notification.shouldRemove())
                toRemove.add(notification);
            else if (!notification.isExpiring())
                visibleNotif.add(notification);
        }

        notifications.removeAll(toRemove);

        for (int i = 0; i < visibleNotif.size(); i++) {
            visibleNotif.get(i).setTargetY(i * (NOTIFICATION_HEIGHT + NOTIFICATION_SPACING));
        }
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
            float animatedY = notification.getCurrentY();
            int x = (int) (startX + offsetX);
            int y = (int) (startY + animatedY);
            renderNotification(matrices, notification, x, y, alpha);
        }
    }

    private void renderNotification(MatrixStack matrices, Notification notification, int x, int y, float alpha) {
        alpha = Math.max(0f, Math.min(1f, alpha));

        Color bgColor = new Color(24, 26, 29, (int) (250 * alpha));
        Render2DEngine.drawRoundedRect(matrices, x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 8, bgColor);

        Color borderColor = new Color(52, 58, 64, (int) (180 * alpha));
        Render2DEngine.drawRoundedOutline(matrices, x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 8, 1.2f, borderColor);

        Color accentColor = new Color(
                notification.getColor().getRed(),
                notification.getColor().getGreen(),
                notification.getColor().getBlue(),
                (int) (255 * alpha)
        );

        Render2DEngine.drawRoundedRect(matrices, x + 4, y + 6, 3, NOTIFICATION_HEIGHT - 12, 1, accentColor);

        Color iconBgColor = new Color(
                notification.getColor().getRed(),
                notification.getColor().getGreen(),
                notification.getColor().getBlue(),
                (int) (25 * alpha)
        );
        Render2DEngine.drawRoundedRect(matrices, x + 13, y + 10, 20, 20, 6, iconBgColor);

        renderIcon(matrices, notification.getType(), x + 23, y + 20, accentColor);

        int textStartX = x + 38;

        if (notification.getTitle() != null && !notification.getTitle().isEmpty()) {
            Color titleColor = new Color(255, 255, 255, (int) (255 * alpha));
            drawText(matrices, notification.getTitle(), textStartX, y + 10, titleColor, true);
        }

        if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
            Color messageColor = new Color(170, 178, 190, (int) (240 * alpha));
            drawText(matrices, notification.getMessage(), textStartX, y + 22, messageColor, false);
        }

        if (notification.hasStack())
            renderStackCounter(matrices, notification, x, y, alpha);

        renderProgressBar(matrices, notification, x, y, alpha);
    }

    private void renderStackCounter(MatrixStack matrices, Notification notification, int x, int y, float alpha) {
        String stackText = "x" + notification.getStackCount();

        FontAtlas font = NoctisUIClient.getInstance().getFonts().getInterBold();
        int textWidth = (int)font.getWidth(stackText);
        int stackWidth = Math.max(textWidth + 8, 20);

        int stackX = x + NOTIFICATION_WIDTH - stackWidth - 6;
        int stackY = y + 6;

        Color stackTextColor = new Color(255, 255, 255, (int) (120 * alpha));
        int textX = stackX + (stackWidth - textWidth) / 2;
        int textY = stackY + 2;

        drawText(matrices, stackText, textX, textY, 8, stackTextColor, true);
    }

    private void renderIcon(MatrixStack matrices, NotificationType type, int x, int y, Color color) {
        FontAtlas lucide = NoctisUIClient.getInstance().getFonts().getLucide();
        lucide.render(matrices, type.getIcon(), x - 5, y - 5, 10, color.getRGB());
    }

    private void renderProgressBar(MatrixStack matrices, Notification notification, int x, int y, float alpha) {
        long elapsed = System.currentTimeMillis() - notification.getLastStackTime();
        float progress = Math.min(elapsed / (float) notification.getDuration(), 1f);

        Color trackColor = new Color(40, 44, 48, (int) (120 * alpha));
        Render2DEngine.drawRoundedRect(matrices, x + 4, y + NOTIFICATION_HEIGHT - 4, NOTIFICATION_WIDTH - 8, 3, 1,
                trackColor);

        if (progress < 1f) {
            int barWidth = (int) ((NOTIFICATION_WIDTH - 8) * (1f - progress));
            Color progressColor = new Color(
                    notification.getColor().getRed(),
                    notification.getColor().getGreen(),
                    notification.getColor().getBlue(),
                    (int) (200 * alpha)
            );
            Render2DEngine.drawRoundedRect(matrices, x + 4, y + NOTIFICATION_HEIGHT - 4, barWidth, 3, 1, progressColor);
        }
    }

    private void drawText(MatrixStack matrices, String text, int x, int y, Color color, boolean bold) {
        FontAtlas font = bold ?
                NoctisUIClient.getInstance().getFonts().getInterBold() :
                NoctisUIClient.getInstance().getFonts().getInterMedium();

        font.render(matrices, text, x, y, color.getRGB());
    }

    private void drawText(MatrixStack matrices, String text, int x, int y, float size, Color color, boolean bold) {
        FontAtlas font = bold ?
                NoctisUIClient.getInstance().getFonts().getInterBold() :
                NoctisUIClient.getInstance().getFonts().getInterMedium();

        font.render(matrices, text, x, y, size, color.getRGB());
    }

    public void clearAll() {
        notifications.clear();
    }
}