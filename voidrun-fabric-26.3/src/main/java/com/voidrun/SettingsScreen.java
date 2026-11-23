package com.voidrun;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The screen paints its own backdrop.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, UiSpec.BG);
        RenderUtil.begin(graphics, width, height);
        float vx = RenderUtil.mouseX(mouseX);
        float vy = RenderUtil.mouseY(mouseY);
        SettingsView.render(RenderUtil.PAINTER, model, scroll, vx, vy,
                SettingsView.buttonAt(vx, vy), System.currentTimeMillis());
        RenderUtil.end();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return false;
        }
        float vx = RenderUtil.mouseX(event.x());
        float vy = RenderUtil.mouseY(event.y());

        int button = SettingsView.buttonAt(vx, vy);
        if (button == SettingsView.BTN_RESET) {
            model.resetToDefaults();
            model.endEdit();
            return true;
        }
        if (button == SettingsView.BTN_CANCEL) {
            minecraft.gui.setScreen(parent);
            return true;
        }
        if (button == SettingsView.BTN_DONE) {
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
            model.cycle(row, (event.modifiers() & InputConstants.MOD_SHIFT) != 0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingRow < 0 || draggingRow >= model.rows.size()) {
            return false;
        }
        SettingsModel.Row row = model.rows.get(draggingRow);
        if (row.kind == SettingsModel.Kind.SLIDER) {
            model.setFraction(row, SettingsView.sliderFraction(RenderUtil.mouseX(event.x())));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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
    public boolean charTyped(CharacterEvent event) {
        if (model.editing() == null) {
            return false;
        }
        char c = (char) event.codepoint();
        if (c >= 32 && c != 127) {
            model.typeChar(c);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            if (model.editing() != null) {
                model.endEdit();
            } else {
                Minecraft.getInstance().gui.setScreen(parent);
            }
            return true;
        }
        if (model.editing() != null) {
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                model.endEdit();
            } else if (key == InputConstants.KEY_TAB) {
                focusNext();
            } else if (key == InputConstants.KEY_BACKSPACE) {
                model.backspace();
            }
            return true;
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
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
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
