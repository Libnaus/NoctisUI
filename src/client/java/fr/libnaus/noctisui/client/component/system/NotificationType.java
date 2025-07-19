package fr.libnaus.noctisui.client.component.system;

import lombok.Getter;

import java.awt.*;

@Getter
public enum NotificationType {
    SUCCESS(new Color(52, 211, 153), "\uE951"),
    ERROR(new Color(248, 113, 113), "\uEB15"),
    WARNING(new Color(251, 146, 60), "\uE90A"),
    INFO(new Color(59, 130, 246), "\uEA0C");

    private final Color defaultColor;
    private final String icon;

    NotificationType(Color defaultColor, String icon) {
        this.defaultColor = defaultColor;
        this.icon = icon;
    }
}