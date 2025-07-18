package fr.libnaus.noctisui.client.api.system;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.libnaus.noctisui.client.common.QuickImports;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL30;

public class BlurProgram implements QuickImports {
    private Framebuffer input;
    private Framebuffer tempBuffer;

    private float blurX;
    private float blurY;
    private float blurWidth;
    private float blurHeight;
    private float blurRadius;
    private float blurStrength;
    private float blurOpacity;

    private int lastWidth = -1;
    private int lastHeight = -1;
    private float lastScaleFactor = -1;

    public BlurProgram() {
        setup();
    }

    private void ensureBuffersExist() {
        int currentWidth = mc.getWindow().getFramebufferWidth();
        int currentHeight = mc.getWindow().getFramebufferHeight();
        float currentScaleFactor = (float) mc.getWindow().getScaleFactor();

        boolean dimensionsChanged = currentWidth != lastWidth ||
                currentHeight != lastHeight ||
                currentScaleFactor != lastScaleFactor;

        if (dimensionsChanged || input == null || tempBuffer == null) {
            lastWidth = currentWidth;
            lastHeight = currentHeight;
            lastScaleFactor = currentScaleFactor;

            if (input == null) {
                input = new SimpleFramebuffer(currentWidth, currentHeight, false, MinecraftClient.IS_SYSTEM_MAC);
            } else if (input.textureWidth != currentWidth || input.textureHeight != currentHeight) {
                input.resize(currentWidth, currentHeight, MinecraftClient.IS_SYSTEM_MAC);
            }

            if (tempBuffer == null) {
                tempBuffer = new SimpleFramebuffer(currentWidth, currentHeight, false, MinecraftClient.IS_SYSTEM_MAC);
            } else if (tempBuffer.textureWidth != currentWidth || tempBuffer.textureHeight != currentHeight) {
                tempBuffer.resize(currentWidth, currentHeight, MinecraftClient.IS_SYSTEM_MAC);
            }
        }
    }

    public void beginBlur(float x, float y, float width, float height, float radius, float blurStrength, float blurOpacity) {
        this.blurX = x;
        this.blurY = y;
        this.blurWidth = width;
        this.blurHeight = height;
        this.blurRadius = radius;
        this.blurStrength = blurStrength;
        this.blurOpacity = blurOpacity;

        ensureBuffersExist();

        if (width <= 0 || height <= 0 || blurOpacity <= 0 || radius <= 0 || blurStrength <= 0) {
            return;
        }

        var mainBuffer = MinecraftClient.getInstance().getFramebuffer();
        tempBuffer.beginWrite(false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainBuffer.fbo);
        GL30.glBlitFramebuffer(0, 0, mainBuffer.textureWidth, mainBuffer.textureHeight,
                0, 0, tempBuffer.textureWidth, tempBuffer.textureHeight,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);

        mainBuffer.beginWrite(false);
    }

    public void beginBlur(float radius, float blurStrength) {
        var window = mc.getWindow();
        float width = window.getScaledWidth();
        float height = window.getScaledHeight();

        beginBlur(0, 0, width, height, radius, blurStrength, 1.0f);
    }

    public void endBlur() {
        ensureBuffersExist();

        if (blurOpacity <= 0 || blurStrength <= 0) return;

        var mainBuffer = MinecraftClient.getInstance().getFramebuffer();

        input.beginWrite(false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainBuffer.fbo);
        GL30.glBlitFramebuffer(0, 0, mainBuffer.textureWidth, mainBuffer.textureHeight,
                0, 0, input.textureWidth, input.textureHeight,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);

        mainBuffer.beginWrite(false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, tempBuffer.fbo);
        GL30.glBlitFramebuffer(0, 0, tempBuffer.textureWidth, tempBuffer.textureHeight,
                0, 0, mainBuffer.textureWidth, mainBuffer.textureHeight,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);

        float scaleFactor = (float) mc.getWindow().getScaleFactor();

        applyShaderParameters(scaleFactor, mainBuffer);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        drawFullscreenQuad(mainBuffer);

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void applyShaderParameters(float scaleFactor, Framebuffer mainBuffer) {
        Shaders.blurRadius.set(blurRadius * scaleFactor);
        Shaders.blurLocation.set(
                blurX * scaleFactor,
                -blurY * scaleFactor + mc.getWindow().getScaledHeight() * scaleFactor - blurHeight * scaleFactor
        );

        Shaders.BLUR.getUniform("radius").set(blurRadius * scaleFactor);
        Shaders.BLUR.getUniform("uLocation").set(blurX * scaleFactor,
                -blurY * scaleFactor + mc.getWindow().getScaledHeight() * scaleFactor - blurHeight * scaleFactor);
        Shaders.BLUR.getUniform("uSize").set(blurWidth * scaleFactor, blurHeight * scaleFactor);
        Shaders.BLUR.getUniform("Quality").set(blurStrength);
        Shaders.BLUR.getUniform("Brightness").set(blurOpacity);
        Shaders.BLUR.getUniform("InputResolution").set((float) mainBuffer.textureWidth, (float) mainBuffer.textureHeight);

        Shaders.BLUR.addSampler("InputSampler", input.getColorAttachment());
        RenderSystem.setShader(() -> Shaders.BLUR);
    }

    private void drawFullscreenQuad(Framebuffer buffer) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        MatrixStack matrices = new MatrixStack();

        builder.vertex(matrices.peek().getPositionMatrix(), 0, 0, 0);
        builder.vertex(matrices.peek().getPositionMatrix(), 0, buffer.textureHeight, 0);
        builder.vertex(matrices.peek().getPositionMatrix(), buffer.textureWidth, buffer.textureHeight, 0);
        builder.vertex(matrices.peek().getPositionMatrix(), buffer.textureWidth, 0, 0);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public void setParameters(float x, float y, float width, float height, float r, float blurStrength, float blurOpacity) {
        ensureBuffersExist();

        if (width <= 0 || height <= 0 || blurOpacity <= 0 || r <= 0 || blurStrength <= 0) {
            return;
        }

        float scaleFactor = (float) mc.getWindow().getScaleFactor();

        Shaders.BLUR.getUniform("radius").set(r * scaleFactor);
        Shaders.BLUR.getUniform("uLocation").set(r * scaleFactor,
                -y * scaleFactor + mc.getWindow().getScaledHeight() * scaleFactor - height * scaleFactor);
        Shaders.BLUR.getUniform("uSize").set(width * scaleFactor, height * scaleFactor);
        Shaders.BLUR.getUniform("Quality").set(blurStrength);

        Shaders.BLUR.addSampler("InputSampler", input.getColorAttachment());
    }

    public void use() {
        ensureBuffersExist();

        var buffer = MinecraftClient.getInstance().getFramebuffer();
        input.beginWrite(false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.fbo);
        GL30.glBlitFramebuffer(0, 0, buffer.textureWidth, buffer.textureHeight,
                0, 0, buffer.textureWidth, buffer.textureHeight,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
        buffer.beginWrite(false);

        Shaders.blurInputResolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
        Shaders.BLUR.addSampler("InputSampler", input.getColorAttachment());

        RenderSystem.setShader(() -> Shaders.BLUR);
    }

    protected void setup() {
        if (mc.getWindow() != null) {
            lastWidth = mc.getWindow().getFramebufferWidth();
            lastHeight = mc.getWindow().getFramebufferHeight();
            lastScaleFactor = (float) mc.getWindow().getScaleFactor();

            input = new SimpleFramebuffer(lastWidth, lastHeight, false, MinecraftClient.IS_SYSTEM_MAC);
            tempBuffer = new SimpleFramebuffer(lastWidth, lastHeight, false, MinecraftClient.IS_SYSTEM_MAC);
        }
    }

    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;

        lastWidth = width;
        lastHeight = height;

        if (input != null) {
            input.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
        }
        if (tempBuffer != null) {
            tempBuffer.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
        }
    }
}