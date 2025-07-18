package fr.libnaus.noctisui.client.api.system.render.font;

import fr.libnaus.noctisui.NoctisUI;
import fr.libnaus.noctisui.client.NoctisUIClient;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;

@Getter
public class Fonts implements SimpleSynchronousResourceReloadListener {

    private FontAtlas interBold, interSemiBold, interMedium, proggyClean, poppins, icons, lucide;

    public Fonts() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this);
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of(NoctisUI.MODID, "reload_fonts");
    }

    @Override
    public void reload(ResourceManager manager) {
        try {
            this.interBold = new FontAtlas(manager, "inter-bold");
            this.interSemiBold = new FontAtlas(manager, "inter-semibold");
            this.interMedium = new FontAtlas(manager, "inter-medium");
            this.proggyClean = new FontAtlas(manager, "proggy-clean");
            this.poppins = new FontAtlas(manager, "poppins");
            this.icons = new FontAtlas(manager, "icons");
            this.lucide = new FontAtlas(manager, "lucide");
        } catch (final IOException e) {
            throw new RuntimeException("Couldn't load fonts", e);
        }
    }
}
