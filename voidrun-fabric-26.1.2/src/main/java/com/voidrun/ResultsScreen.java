package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Desktop;
import java.io.File;

/** Results screen. Layout and animation live in the shared ResultsView. */
public final class ResultsScreen extends Screen {
    private final ResultData data;
    private final VoidrunConfig cfg;
    private final ResultsView view;

    public ResultsScreen(ResultData data, String note, boolean noteError, VoidrunConfig cfg) {
        super(Component.literal(UiSpec.TITLE));
        this.data = data;
        this.cfg = cfg;
        this.view = new ResultsView(data, note, noteError, cfg);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The screen paints its own backdrop.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, Ui.withAlpha(UiSpec.BG, (int) (255 * view.fade())));

        RenderUtil.begin(graphics, width, height);
        view.render(RenderUtil.PAINTER, RenderUtil.mouseX(mouseX), RenderUtil.mouseY(mouseY));
        RenderUtil.end();

        if (view.closeFinished()) {
            RunManager.INSTANCE.dismissResults(false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }
        int action = view.click(RenderUtil.mouseX(event.x()), RenderUtil.mouseY(event.y()));
        if (action == ResultsView.ACT_COPY) {
            copyResults();
        } else if (action == ResultsView.ACT_FOLDER) {
            openFolder();
        } else if (action == ResultsView.ACT_SETTINGS) {
            Minecraft.getInstance().setScreen(new SettingsScreen(this));
        } else if (action == ResultsView.ACT_AGAIN) {
            RunManager.INSTANCE.dismissResults(true);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        view.release();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            view.close();
            return true;
        }
        return false;
    }

    private void copyResults() {
        if (data == null) {
            return;
        }
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(data.summaryText());
        } catch (Throwable t) {
            view.toast("Could not copy to the clipboard");
        }
    }

    private void openFolder() {
        File dir = ResultExporter.resolveFolder(Minecraft.getInstance().gameDirectory, cfg.outputFolder);
        try {
            if (Desktop.isDesktopSupported() && dir.isDirectory()) {
                Desktop.getDesktop().open(dir);
                view.toast(UiSpec.TOAST_OPENED);
                return;
            }
        } catch (Throwable ignored) {
            // fall through to the path on the clipboard
        }
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(dir.getAbsolutePath());
        } catch (Throwable ignored) {
            // nothing else we can do
        }
        view.toast("Folder: " + dir.getAbsolutePath());
    }

}
