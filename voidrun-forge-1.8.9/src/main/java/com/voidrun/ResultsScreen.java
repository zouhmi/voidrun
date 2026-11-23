package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.awt.Desktop;
import java.io.File;

/** Results screen. Layout and animation live in the shared ResultsView. */
public final class ResultsScreen extends GuiScreen {
    private final ResultData data;
    private final VoidrunConfig cfg;
    private final ResultsView view;

    public ResultsScreen(ResultData data, String note, boolean noteError, VoidrunConfig cfg) {
        this.data = data;
        this.cfg = cfg;
        this.view = new ResultsView(data, note, noteError, cfg);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int alpha = (int) (255 * view.fade());
        drawRect(0, 0, width, height, Ui.withAlpha(UiSpec.BG, alpha));

        RenderUtil.begin(width, height);
        view.render(RenderUtil.PAINTER, RenderUtil.mouseX(mouseX), RenderUtil.mouseY(mouseY));
        RenderUtil.end();

        if (view.closeFinished()) {
            RunManager.INSTANCE.dismissResults(false);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        int action = view.click(RenderUtil.mouseX(mouseX), RenderUtil.mouseY(mouseY));
        if (action == ResultsView.ACT_COPY) {
            copyResults();
        } else if (action == ResultsView.ACT_FOLDER) {
            openFolder();
        } else if (action == ResultsView.ACT_SETTINGS) {
            Minecraft.getMinecraft().displayGuiScreen(new SettingsScreen(this));
        } else if (action == ResultsView.ACT_AGAIN) {
            RunManager.INSTANCE.dismissResults(true);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        view.release();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            view.close();
        }
    }

    private void copyResults() {
        if (data == null) {
            return;
        }
        try {
            setClipboardString(data.summaryText());
        } catch (Throwable t) {
            view.toast("Could not copy to the clipboard");
        }
    }

    private void openFolder() {
        File dir = ResultExporter.resolveFolder(Minecraft.getMinecraft().mcDataDir, cfg.outputFolder);
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
            setClipboardString(dir.getAbsolutePath());
        } catch (Throwable ignored) {
            // nothing else we can do
        }
        view.toast("Folder: " + dir.getAbsolutePath());
    }

}
