package com.awesomehippo.chamelemobs.config;

import com.awesomehippo.chamelemobs.network.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private ToggleOptionButton enabledButton;
    private StrengthSlider strengthSlider;
    private StrengthSlider brightnessSlider;
    private MultiLineEditBox mobsBox;
    private Button doneButton;
    private String mobsText;
    private int leftX;
    private int titleY;
    private int mobsHeaderY;
    private boolean widgetsLocked;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("chamelemobs.config.title"));
        this.parent = parent;
        Config.load();
        this.mobsText = String.join("\n", Config.mobIds());
    }

    @Override
    protected void init() {
        int contentWidth = 200;
        int controlHeight = 20;
        int boxHeight = 90;
        int toggleHeight = 16;
        int toggleWidth = Math.max(font.width(Component.translatable("chamelemobs.config.toggle.enabled")), font.width(Component.translatable("chamelemobs.config.toggle.disabled"))) + 16;
        leftX = width / 2 - contentWidth / 2;

        int blockHeight = controlHeight + 4 + controlHeight + 12 + toggleHeight + 4 + boxHeight + 12 + controlHeight;
        int y = Math.max(28, (height - blockHeight) / 2);
        titleY = Math.max(8, y - 16);

        if (Config.locked) {
            mobsText = String.join("\n", Config.mobIds());
        }

        strengthSlider = new StrengthSlider(leftX, y, contentWidth, controlHeight, "chamelemobs.config.strength.label", Config.strength);
        strengthSlider.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.strength.tooltip")));
        addRenderableWidget(strengthSlider);
        y += 24;

        brightnessSlider = new StrengthSlider(leftX, y, contentWidth, controlHeight, "chamelemobs.config.brightness.label", Config.brightness);
        brightnessSlider.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.brightness.tooltip")));
        addRenderableWidget(brightnessSlider);
        y += 32;

        mobsHeaderY = y + (toggleHeight - font.lineHeight) / 2;
        enabledButton = new ToggleOptionButton(leftX + contentWidth - toggleWidth, y, toggleWidth, toggleHeight, Config.enabled);
        enabledButton.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.enabled.tooltip")));
        addRenderableWidget(enabledButton);
        y += toggleHeight + 4;
        mobsBox = new MultiLineEditBox(font, leftX, y, contentWidth, boxHeight, Component.empty(), Component.translatable("chamelemobs.config.mobs.label")) {
            @Override
            public boolean keyPressed(int key, int scan, int mods) {
                return this.active && super.keyPressed(key, scan, mods);
            }

            @Override
            public boolean charTyped(char c, int mods) {
                return this.active && super.charTyped(c, mods);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.active && super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return this.active && super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            @Override
            public void setFocused(boolean focused) {
                super.setFocused(focused);
                setTooltip(focused ? null : Tooltip.create(Component.translatable("chamelemobs.config.mobs.tooltip")));
            }
        };
        mobsBox.setValue(mobsText);
        mobsBox.setValueListener(value -> mobsText = value);
        mobsBox.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.mobs.tooltip")));
        addRenderableWidget(mobsBox);
        y += boxHeight + 12;

        doneButton = Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(leftX, y, contentWidth, controlHeight).tooltip(Tooltip.create(Component.translatable("chamelemobs.config.save.tooltip"))).build();
        addRenderableWidget(doneButton);
        widgetsLocked = false;
        applyLock();
    }

    @Override
    public void tick() {
        mobsBox.tick();
        applyLock();
    }

    private void applyLock() {
        if (Config.locked == widgetsLocked) {
            return;
        }
        widgetsLocked = Config.locked;
        strengthSlider.active = !widgetsLocked;
        brightnessSlider.active = !widgetsLocked;
        enabledButton.active = !widgetsLocked;
        mobsBox.active = !widgetsLocked;
        if (widgetsLocked) {
            setFocused(null);
            Tooltip tip = Tooltip.create(Component.translatable("chamelemobs.config.locked.tooltip"));
            strengthSlider.setTooltip(tip);
            brightnessSlider.setTooltip(tip);
            enabledButton.setTooltip(tip);
            mobsBox.setTooltip(tip);
            doneButton.setTooltip(tip);
        } else {
            strengthSlider.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.strength.tooltip")));
            brightnessSlider.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.brightness.tooltip")));
            enabledButton.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.enabled.tooltip")));
            mobsBox.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.mobs.tooltip")));
            doneButton.setTooltip(Tooltip.create(Component.translatable("chamelemobs.config.save.tooltip")));
        }
    }

    private void saveSettings() {
        if (Config.locked) {
            return;
        }
        Config.enabled = enabledButton.selected();
        Config.strength = (float) strengthSlider.getActualValue();
        Config.brightness = (float) brightnessSlider.getActualValue();
        List<String> ids = new ArrayList<>();
        for (String line : mobsText.split("\\R")) {
            for (String part : line.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    ids.add(trimmed);
                }
            }
        }
        Config.setMobs(ids);
        Config.save();
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.execute(Network::sendToAll);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(font, title, width / 2, titleY, 0xFFFFFF);
        guiGraphics.drawString(font, Component.translatable("chamelemobs.config.mobs.label"), leftX, mobsHeaderY, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        saveSettings();
        Minecraft.getInstance().setScreen(parent);
    }

    public static class StrengthSlider extends AbstractSliderButton {
        private final String label;

        public StrengthSlider(int x, int y, int width, int height, String label, double currentValue) {
            super(x, y, width, height, Component.empty(), currentValue);
            this.label = label;
            updateMessage();
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            return this.active && super.keyPressed(key, scan, mods);
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(label, String.format("%.2f", getActualValue())));
        }

        public double getActualValue() {
            return value;
        }
    }

    public static class ToggleOptionButton extends Button {
        private boolean selected;

        public ToggleOptionButton(int x, int y, int width, int height, boolean selected) {
            super(x, y, width, height, Component.empty(), btn -> {}, DEFAULT_NARRATION);
            this.selected = selected;
            updateLabel();
        }

        @Override
        public void onPress() {
            this.selected = !this.selected;
            updateLabel();
        }

        public boolean selected() {
            return this.selected;
        }

        private void updateLabel() {
            setMessage(Component.translatable(this.selected ? "chamelemobs.config.toggle.enabled" : "chamelemobs.config.toggle.disabled").withStyle(this.selected ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
    }
}
