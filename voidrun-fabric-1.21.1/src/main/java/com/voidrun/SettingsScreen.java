package com.voidrun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Settings screen. Layout and editing rules live in the shared SettingsModel and SettingsView. */
public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final SettingsModel model;
    private int scroll;
    private int draggingRow = -1;

    public SettingsScreen(Screen parent) {
        super(Component.literal(UiSpec.SETTINGS_TITLE));
        this.parent = parent;
        this.model = new SettingsModel(RunManager.INSTANCE.config());
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, UiSpec.BG);
        RenderUtil.begin(graphics, width, height);
        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);
        SettingsView.render(RenderUtil.PAINTER, model, scroll, vx, vy,
                SettingsView.buttonAt(vx, vy), System.currentTimeMillis());
        RenderUtil.end();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);

        int btn = SettingsView.buttonAt(vx, vy);
        if (btn == SettingsView.BTN_RESET) {
            model.resetToDefaults();
            model.endEdit();
            return true;
        }
        if (btn == SettingsView.BTN_CANCEL) {
            minecraft.setScreen(parent);
            return true;
        }
        if (btn == SettingsView.BTN_DONE) {
            saveAndClose();
            return true;
        }

        SettingsModel.Row row = SettingsView.rowAt(model, scroll, vx, vy);
        if (row == null) {
            model.endEdit();
            return true;
        }
        if (row.kind == SettingsModel.Kind.TEXT) {
            if (SettingsView.inControlArea(vx)) {
                model.beginEdit(row);
            } else {
                model.endEdit();
            }
            return true;
        }
        model.endEdit();
        if (row.kind == SettingsModel.Kind.SLIDER) {
            if (SettingsView.inSlider(vx) || SettingsView.inControlArea(vx)) {
                draggingRow = model.rows.indexOf(row);
                model.setFraction(row, SettingsView.sliderFraction(vx));
            }
            return true;
        }
        if (SettingsView.inControlArea(vx)) {
            model.cycle(row, 1);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingRow < 0 || draggingRow >= model.rows.size()) {
            return false;
        }
        SettingsModel.Row row = model.rows.get(draggingRow);
        if (row.kind == SettingsModel.Kind.SLIDER) {
            model.setFraction(row, SettingsView.sliderFraction(RenderUtil.mouseX(mouseX)));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingRow = -1;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            scroll = SettingsView.clampScroll(model,
                    scroll - (scrollY > 0.0 ? 24 : -24));
        }
        return true;
    }

    @Override
    public boolean charTyped(char typedChar, int modifiers) {
        if (model.editing() == null) {
            return false;
        }
        char c = typedChar;
        if (c >= 32 && c != 127) {
            model.typeChar(c);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (model.editing() != null) {
                model.endEdit();
            } else {
                Minecraft.getInstance().setScreen(parent);
            }
            return true;
        }
        if (model.editing() != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                model.endEdit();
            } else if (key == GLFW.GLFW_KEY_TAB) {
                focusNext();
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                model.backspace();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        return false;
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
        String error = model.apply(Minecraft.getInstance().gameDirectory);
        if (error != null) {
            model.message = error;
            return;
        }
        RunManager.INSTANCE.reloadConfig();
        Minecraft.getInstance().setScreen(parent);
    }
}
