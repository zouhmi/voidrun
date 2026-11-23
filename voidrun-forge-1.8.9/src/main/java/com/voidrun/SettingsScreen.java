package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Settings screen. Layout and editing rules live in the shared SettingsModel and SettingsView. */
public final class SettingsScreen extends GuiScreen {
    private final GuiScreen parent;
    private final SettingsModel model;
    private int scroll;
    private int draggingRow = -1;

    public SettingsScreen(GuiScreen parent) {
        this.parent = parent;
        this.model = new SettingsModel(RunManager.INSTANCE.config());
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, UiSpec.BG);
        RenderUtil.begin(width, height);
        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);
        SettingsView.render(RenderUtil.PAINTER, model, scroll, vx, vy,
                SettingsView.buttonAt(vx, vy), System.currentTimeMillis());
        RenderUtil.end();
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll = SettingsView.clampScroll(model, scroll - (wheel > 0 ? 24 : -24));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws java.io.IOException {
        if (mouseButton != 0) {
            return;
        }
        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);

        int button = SettingsView.buttonAt(vx, vy);
        if (button == SettingsView.BTN_RESET) {
            model.resetToDefaults();
            model.endEdit();
            return;
        }
        if (button == SettingsView.BTN_CANCEL) {
            Minecraft.getMinecraft().displayGuiScreen(parent);
            return;
        }
        if (button == SettingsView.BTN_DONE) {
            saveAndClose();
            return;
        }

        SettingsModel.Row row = SettingsView.rowAt(model, scroll, vx, vy);
        if (row == null) {
            model.endEdit();
            return;
        }
        if (row.kind == SettingsModel.Kind.TEXT) {
            if (SettingsView.inControlArea(vx)) {
                model.beginEdit(row);
            } else {
                model.endEdit();
            }
            return;
        }
        model.endEdit();
        if (row.kind == SettingsModel.Kind.SLIDER) {
            if (SettingsView.inSlider(vx) || SettingsView.inControlArea(vx)) {
                draggingRow = model.rows.indexOf(row);
                model.setFraction(row, SettingsView.sliderFraction(vx));
            }
            return;
        }
        if (SettingsView.inControlArea(vx)) {
            model.cycle(row, isShiftKeyDown() ? -1 : 1);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        if (draggingRow < 0 || draggingRow >= model.rows.size()) {
            return;
        }
        SettingsModel.Row row = model.rows.get(draggingRow);
        if (row.kind == SettingsModel.Kind.SLIDER) {
            model.setFraction(row, SettingsView.sliderFraction(RenderUtil.mouseX(mouseX)));
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggingRow = -1;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (model.editing() != null) {
                model.endEdit();
            } else {
                Minecraft.getMinecraft().displayGuiScreen(parent);
            }
            return;
        }
        if (model.editing() != null) {
            if (keyCode == Keyboard.KEY_RETURN) {
                model.endEdit();
            } else if (keyCode == Keyboard.KEY_TAB) {
                focusNext();
            } else if (keyCode == Keyboard.KEY_BACK) {
                model.backspace();
            } else if (typedChar >= 32 && typedChar != 127) {
                model.typeChar(typedChar);
            }
            return;
        }
        if (keyCode == Keyboard.KEY_RETURN) {
            saveAndClose();
        }
    }

    private void focusNext() {
        int start = model.editingRow;
        for (int i = 1; i <= model.rows.size(); i++) {
            int index = (start + i) % model.rows.size();
            if (model.rows.get(index).kind == SettingsModel.Kind.TEXT) {
                model.editingRow = index;
                return;
            }
        }
    }

    private void saveAndClose() {
        model.endEdit();
        String error = model.apply(Minecraft.getMinecraft().mcDataDir);
        if (error != null) {
            model.message = error;
            return;
        }
        RunManager.INSTANCE.reloadConfig();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
