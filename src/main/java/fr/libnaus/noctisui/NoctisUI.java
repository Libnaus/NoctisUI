package fr.libnaus.noctisui;

import lombok.Getter;
import net.fabricmc.api.ModInitializer;

public class NoctisUI implements ModInitializer {

    public static final String MODID = "noctisui";

    @Getter
    private static NoctisUI instance;

    @Override
    public void onInitialize() {
        instance = this;
    }
}
