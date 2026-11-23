package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, Ui.withAlpha(UiSpec.BG, (int) (255 * view.fade())));

        RenderUtil.begin(graphics, width, height);
        view.render(RenderUtil.PAINTER, RenderUtil.mouseX(mouseX), RenderUtil.mouseY(mouseY));
        RenderUtil.end();

        if (view.closeFinished()) {
            RunManager.INSTANCE.dismissResults(false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int action = view.click(RenderUtil.mouseX(mouseX), RenderUtil.mouseY(mouseY));
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
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        view.release();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
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
