package fr.libnaus.noctisui.client.api.system;

import fr.libnaus.noctisui.NoctisUI;
import fr.libnaus.noctisui.client.NoctisUIClient;
import fr.libnaus.noctisui.client.common.QuickImports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

@Slf4j
public class Shaders implements QuickImports, SimpleSynchronousResourceReloadListener {

    public static ShaderProgram ROUNDED_RECT, ROUNDED_OUTLINE, CIRCLE, MSDF, BLUR, COLOR_PICKER;

    public static Uniform msdfPxrange;

    public static Uniform colorPickerResolution;
    public static Uniform colorPickerPosition;
    public static Uniform colorPickerHue;
    public static Uniform colorPickerAlpha;

    public static Uniform blurInputResolution;
    public static Uniform blurSize;
    public static Uniform blurLocation;
    public static Uniform blurRadius;
    public static Uniform blurBrightness;
    public static Uniform blurQuality;
    public static Uniform color;

    @Getter
    private static boolean initialized = false;

    static {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new Shaders());
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(NoctisUI.MODID, "reload_shaders");
    }

    @Override
    public void reload(ResourceManager manager) {
        log.info("Reloading shaders...");
        load();
        log.info("Shaders reloaded successfully.");
    }

    public static void load() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(new Identifier(NoctisUI.MODID, "rounded_rect"),
                    VertexFormats.POSITION_COLOR,
                    program -> ROUNDED_RECT = program
                );

                context.register(new Identifier(NoctisUI.MODID, "rounded_outline"),
                    VertexFormats.POSITION_COLOR,
                    program -> ROUNDED_OUTLINE = program
                );

                context.register(new Identifier(NoctisUI.MODID, "circle"),
                    VertexFormats.POSITION_COLOR,
                    program -> CIRCLE = program
                );

                // MSDF
                context.register(new Identifier(NoctisUI.MODID, "msdf"),
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    program -> {
                        MSDF = program;
                        msdfPxrange = program.getUniform("pxRange");
                    }
                );

                // Color Picker
                context.register(new Identifier(NoctisUI.MODID, "color_picker"),
                    VertexFormats.POSITION_COLOR,
                    program -> {
                        COLOR_PICKER = program;
                        colorPickerResolution = program.getUniform("Resolution");
                        colorPickerPosition = program.getUniform("Position");
                        colorPickerHue = program.getUniform("Hue");
                        colorPickerAlpha = program.getUniform("Alpha");
                    }
                );

                // Blur
                context.register(new Identifier(NoctisUI.MODID, "blur"),
                    VertexFormats.POSITION,
                    program -> {
                        BLUR = program;
                        blurInputResolution = program.getUniform("InputResolution");
                        blurBrightness = program.getUniform("Brightness");
                        blurQuality = program.getUniform("Quality");
                        blurSize = program.getUniform("uSize");
                        blurLocation = program.getUniform("uLocation");
                        blurRadius = program.getUniform("radius");
                        program.getUniform("InputSampler");
                    }
                );

                initialized = true;
            } catch (Exception e) {
                log.error("Failed to load shaders: {}", e.getMessage());
            }
        });
    }
}