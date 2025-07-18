package fr.libnaus.noctisui.client.component.system;

import fr.libnaus.noctisui.client.common.QuickImports;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
@Getter
public class Notification implements QuickImports {
    private final NotificationType type;
    private final String title;
    private final String message;
    private final Color color;
    private final long createdTime;
    private final long duration;
    private float animationProgress;
    @Setter
    private float targetY;
    private float currentY;
    private float yVelocity;

    public Notification(String title, String message, NotificationType type) {
        this(title, message, type, 3000);
    }

    public Notification(String title, String message, NotificationType type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.color = type.getDefaultColor();
        this.duration = duration;
        this.createdTime = System.currentTimeMillis();
        this.animationProgress = 0f;
        this.targetY = 0f;
        this.currentY = 0f;
        this.yVelocity = 0f;
    }

    public void update() {
        long elapsed = System.currentTimeMillis() - createdTime;

        if (elapsed < 300) {
            float t = elapsed / 300f;
            animationProgress = 1f - (1f - t) * (1f - t);
        } else if (elapsed > duration - 200) {
            float fadeProgress = (elapsed - (duration - 200)) / 200f;
            animationProgress = 1f - fadeProgress * fadeProgress;
        } else {
            animationProgress = 1f;
        }
        updateYAnimation();
    }

    private void updateYAnimation() {
        float deltaY = targetY - currentY;

        if (Math.abs(deltaY) > 0.1f) {
            float speed = 0.12f;
            currentY = currentY + (deltaY * speed);

            if (Math.abs(deltaY) < 0.3f) {
                currentY = targetY;
                yVelocity = 0f;
            }
        } else {
            currentY = targetY;
            yVelocity = 0f;
        }
    }

    public boolean shouldRemove() {
        return System.currentTimeMillis() - createdTime > duration;
    }

    public float getSlideOffset() {
        float t = animationProgress;
        float easeOut = 1f - (1f - t) * (1f - t);
        return (1f - easeOut) * 80f;
    }

    public float getAlpha() {
        return Math.max(0f, Math.min(1f, animationProgress));
    }
}