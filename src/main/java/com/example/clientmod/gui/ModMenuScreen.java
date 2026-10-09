package com.example.clientmod.gui;

import com.example.clientmod.module.Module;
import com.example.clientmod.module.ModuleManager;
import com.example.clientmod.module.ProfileManager;
import com.example.clientmod.module.setting.BoolSetting;
import com.example.clientmod.module.setting.ChoiceSetting;
import com.example.clientmod.module.setting.IntSetting;
import com.example.clientmod.module.setting.Setting;
import com.example.clientmod.module.setting.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleConsumer;

/**
 * Click GUI: Settings on the left, one column per category, Profiles on the right.
 * Left-click a module to toggle it, right-click (or the three dots) to open its settings.
 */
public class ModMenuScreen extends Screen {
    private static final int PANEL_BG = 0xF0151515;
    private static final int HEADER_BG = 0xF00F0F0F;
    private static final int ROW_HOVER = 0x30FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFF9A9A9A;
    private static final int ROW_H = 16;
    private static final int SETTING_H = 16;
    private static final int SLIDER_H = 22;
    private static final int HEADER_H = 17;

    private interface Click { void run(double mx, int button); }

    private record Hit(int x1, int y1, int x2, int y2, Click click, DoubleConsumer drag) {
        boolean contains(double x, double y) { return x >= x1 && x < x2 && y >= y1 && y < y2; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private List<String> profiles = new ArrayList<>();

    private Hit dragging = null;
    private boolean dirty = false;

    private StringSetting editing = null;
    private boolean creating = false;
    private String newName = "";

    private String tooltip = null;
    private int tipX, tipY;

    public ModMenuScreen() {
        super(Text.literal("My Client Mod"));
    }

    @Override
    protected void init() {
        profiles = ProfileManager.list();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int accent() {
        return 0xFF000000 | ModuleManager.COLOR_RGB[ModuleManager.GUI_ACCENT.get()];
    }

    private void addHit(int x1, int y1, int x2, int y2, Click click, DoubleConsumer drag) {
        hits.add(new Hit(x1, y1, x2, y2, click, drag));
    }

    private void markDirty() {
        dirty = true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        hits.clear();
        tooltip = null;
        context.fill(0, 0, width, height, 0x60000000);

        int n = ModuleManager.CATEGORIES.size() + 2;
        int pw = Math.max(80, Math.min(104, (width - 16 - (n - 1) * 8) / n));
        int total = n * pw + (n - 1) * 8;
        int x = (width - total) / 2;
        int y = Math.max(8, (height - 150) / 5);

        drawSettingsPanel(context, x, y, pw, mouseX, mouseY);
        x += pw + 8;
        for (String category : ModuleManager.CATEGORIES) {
            drawCategoryPanel(context, category, x, y, pw, mouseX, mouseY);
            x += pw + 8;
        }
        drawProfilesPanel(context, x, y, pw, mouseX, mouseY);

        if (tooltip != null) {
            int tw = textRenderer.getWidth(tooltip) + 8;
            int tx = Math.min(tipX + 8, width - tw - 2);
            int ty = tipY + 10;
            context.fill(tx, ty, tx + tw, ty + 14, 0xF0000000);
            context.drawTextWithShadow(textRenderer, tooltip, tx + 4, ty + 3, TEXT);
        }
    }

    /** Panel background with a soft shadow and a thin outline. */
    private void panel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x50000000);
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A2A2A);
        panel(context, x, y, w, h);
    }

    private int drawHeader(DrawContext context, String title, int x, int y, int w) {
        context.fill(x, y, x + w, y + HEADER_H, HEADER_BG);
        context.fill(x, y + HEADER_H - 1, x + w, y + HEADER_H, accent());
        context.drawTextWithShadow(textRenderer, title, x + 5, y + 5, TEXT);
        return y + HEADER_H;
    }

    // ---- Settings panel ----

    private void drawSettingsPanel(DrawContext context, int x, int y, int w, int mx, int my) {
        int perRow0 = Math.max(1, (w - 12) / 14);
        int rows0 = (ModuleManager.COLOR_RGB.length + perRow0 - 1) / perRow0;
        panel(context, x, y, w, HEADER_H + 6 + 11 + rows0 * 14 + 6 + 12 + 6);
        int cy = drawHeader(context, "Settings", x, y, w) + 6;

        context.drawTextWithShadow(textRenderer, "GUI Theme", x + 6, cy, DIM);
        cy += 11;
        int sw = 12;
        int perRow = Math.max(1, (w - 12) / (sw + 2));
        for (int i = 0; i < ModuleManager.COLOR_RGB.length; i++) {
            int sx = x + 6 + (i % perRow) * (sw + 2);
            int sy = cy + (i / perRow) * (sw + 2);
            int color = 0xFF000000 | ModuleManager.COLOR_RGB[i];
            if (ModuleManager.GUI_ACCENT.get() == i) {
                context.fill(sx - 1, sy - 1, sx + sw + 1, sy + sw + 1, 0xFFFFFFFF);
            }
            context.fill(sx, sy, sx + sw, sy + sw, color);
            final int index = i;
            addHit(sx, sy, sx + sw, sy + sw, (m, b) -> {
                ModuleManager.GUI_ACCENT.set(index);
                markDirty();
            }, null);
            if (mx >= sx && mx < sx + sw && my >= sy && my < sy + sw) {
                tooltip = ModuleManager.COLOR_NAMES.get(i);
                tipX = mx;
                tipY = my;
            }
        }
        int rows = (ModuleManager.COLOR_RGB.length + perRow - 1) / perRow;
        int by = cy + rows * (sw + 2) + 6;
        context.drawTextWithShadow(textRenderer, trim("Profile: " + ProfileManager.getActive(), w - 12), x + 6, by, DIM);
    }

    // ---- Category panels ----

    private void drawCategoryPanel(DrawContext context, String category, int x, int y, int w, int mx, int my) {
        List<Module> modules = ModuleManager.inCategory(category);

        int h = HEADER_H;
        for (Module m : modules) {
            h += ROW_H;
            if (expanded.contains(m.getId())) {
                for (Setting s : m.getSettings()) {
                    h += settingHeight(s);
                }
            }
        }
        context.fill(x, y, x + w, y + h, PANEL_BG);
        int cy = drawHeader(context, category, x, y, w);

        for (Module m : modules) {
            boolean open = expanded.contains(m.getId());
            boolean hover = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
            if (m.isEnabled()) {
                context.fill(x, cy, x + w, cy + ROW_H, (accent() & 0x00FFFFFF) | 0x70000000);
                context.fill(x, cy, x + 2, cy + ROW_H, accent());
            } else if (hover) {
                context.fill(x, cy, x + w, cy + ROW_H, ROW_HOVER);
            }
            context.drawTextWithShadow(textRenderer, m.getName(), x + 6, cy + 4, m.isEnabled() ? TEXT : DIM);

            if (m.hasSettings()) {
                int dx = x + w - 12;
                int dy = cy + 4;
                int dotColor = open ? TEXT : (m.isEnabled() ? TEXT : DIM);
                for (int i = 0; i < 3; i++) {
                    context.fill(dx, dy + i * 3, dx + 2, dy + i * 3 + 2, dotColor);
                }
                final String id = m.getId();
                addHit(x + w - 16, cy, x + w, cy + ROW_H, (cx, b) -> toggleExpanded(id), null);
            }
            final Module module = m;
            addHit(x, cy, x + w, cy + ROW_H, (cx, b) -> {
                if (b == 0) {
                    module.toggle();
                    markDirty();
                } else if (b == 1 && module.hasSettings()) {
                    toggleExpanded(module.getId());
                }
            }, null);
            if (hover) {
                tooltip = m.getDescription();
                tipX = mx;
                tipY = my;
            }
            cy += ROW_H;

            if (open) {
                for (Setting s : m.getSettings()) {
                    cy = drawSetting(context, s, x, cy, w, mx, my);
                }
            }
        }
    }

    private void toggleExpanded(String id) {
        if (!expanded.remove(id)) {
            expanded.add(id);
        }
    }

    private int settingHeight(Setting s) {
        return s instanceof IntSetting ? SLIDER_H : (s instanceof StringSetting ? 24 : SETTING_H);
    }

    private int drawSetting(DrawContext context, Setting s, int x, int y, int w, int mx, int my) {
        int h = settingHeight(s);
        context.fill(x, y, x + w, y + h, 0xFF0C0C0C);
        int ix = x + 6;
        int iw = w - 12;

        if (s instanceof BoolSetting b) {
            context.drawTextWithShadow(textRenderer, trim(s.getLabel(), iw - 26), ix, y + 4, DIM);
            int px = x + w - 24;
            context.fill(px, y + 4, px + 18, y + 12, b.get() ? accent() : 0xFF3A3A3A);
            int knob = b.get() ? px + 10 : px + 2;
            context.fill(knob, y + 5, knob + 6, y + 11, TEXT);
            addHit(x, y, x + w, y + h, (cx, btn) -> {
                b.toggle();
                markDirty();
            }, null);
        } else if (s instanceof ChoiceSetting c) {
            context.drawTextWithShadow(textRenderer, trim(s.getLabel(), iw / 2), ix, y + 4, DIM);
            String v = c.getValue();
            context.drawTextWithShadow(textRenderer, v, x + w - 6 - textRenderer.getWidth(v), y + 4, accent());
            addHit(x, y, x + w, y + h, (cx, btn) -> {
                c.cycle();
                markDirty();
            }, null);
        } else if (s instanceof IntSetting i) {
            String label = trim(s.getLabel(), iw - 24) ;
            context.drawTextWithShadow(textRenderer, label, ix, y + 2, DIM);
            String v = Integer.toString(i.get());
            context.drawTextWithShadow(textRenderer, v, x + w - 6 - textRenderer.getWidth(v), y + 2, TEXT);
            int sy = y + 14;
            context.fill(ix, sy, ix + iw, sy + 4, 0xFF3A3A3A);
            double frac = (i.get() - i.getMin()) / (double) Math.max(1, i.getMax() - i.getMin());
            int filled = (int) Math.round(frac * iw);
            context.fill(ix, sy, ix + filled, sy + 4, accent());
            context.fill(ix + filled - 1, sy - 1, ix + filled + 1, sy + 5, TEXT);
            final int sx = ix;
            final int sw = iw;
            DoubleConsumer setter = m -> {
                double f = Math.max(0, Math.min(1, (m - sx) / (double) sw));
                int val = i.getMin() + (int) Math.round(f * (i.getMax() - i.getMin()));
                if (val != i.get()) {
                    i.set(val);
                    markDirty();
                }
            };
            addHit(x, y, x + w, y + h, (cx, btn) -> setter.accept(cx), setter);
        } else if (s instanceof StringSetting str) {
            context.drawTextWithShadow(textRenderer, trim(s.getLabel(), iw), ix, y + 1, DIM);
            boolean active = editing == str;
            int by = y + 11;
            context.fill(ix, by, ix + iw, by + 10, active ? 0xFF2A2A2A : 0xFF1E1E1E);
            String shown = str.get() + (active && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            // show the end of long text so you see what you're typing
            while (shown.length() > 1 && textRenderer.getWidth(shown) > iw - 4) {
                shown = shown.substring(1);
            }
            context.drawTextWithShadow(textRenderer, shown, ix + 2, by + 1, TEXT);
            addHit(x, y, x + w, y + h, (cx, btn) -> editing = str, null);
        }
        return y + h;
    }

    private String trim(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String t = text;
        while (t.length() > 1 && textRenderer.getWidth(t + "..") > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "..";
    }

    // ---- Profiles panel ----

    private void drawProfilesPanel(DrawContext context, int x, int y, int w, int mx, int my) {
        int rows = 1 + (creating ? 1 : 0) + profiles.size();
        panel(context, x, y, w, HEADER_H + rows * ROW_H);
        int cy = drawHeader(context, "Profiles", x, y, w);

        // CREATE button
        boolean hoverCreate = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
        context.fill(x + 4, cy + 2, x + w - 4, cy + ROW_H - 2, hoverCreate ? 0xFF3A3A3A : 0xFF262626);
        String label = "+ CREATE";
        context.drawTextWithShadow(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, cy + 4, accent());
        addHit(x, cy, x + w, cy + ROW_H, (cx, b) -> {
            creating = true;
            newName = "";
            editing = null;
        }, null);
        cy += ROW_H;

        if (creating) {
            context.fill(x + 4, cy + 2, x + w - 4, cy + ROW_H - 2, 0xFF2A2A2A);
            String shown = newName + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            while (shown.length() > 1 && textRenderer.getWidth(shown) > w - 16) {
                shown = shown.substring(1);
            }
            context.drawTextWithShadow(textRenderer, shown.isEmpty() ? "name..." : shown, x + 8, cy + 4,
                    newName.isEmpty() ? DIM : TEXT);
            cy += ROW_H;
        }

        String active = ProfileManager.getActive();
        for (String name : new ArrayList<>(profiles)) {
            boolean isActive = name.equals(active);
            boolean hover = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
            if (isActive) {
                context.fill(x, cy, x + w, cy + ROW_H, (accent() & 0x00FFFFFF) | 0x70000000);
                context.fill(x, cy, x + 2, cy + ROW_H, accent());
            } else if (hover) {
                context.fill(x, cy, x + w, cy + ROW_H, ROW_HOVER);
            }
            boolean deletable = !ProfileManager.DEFAULT.equals(name);
            int textMax = w - 12 - (deletable ? 14 : 0);
            context.drawTextWithShadow(textRenderer, trim(name, textMax), x + 6, cy + 4, isActive ? TEXT : DIM);
            if (deletable) {
                context.drawTextWithShadow(textRenderer, "x", x + w - 11, cy + 4, isActive ? TEXT : DIM);
                addHit(x + w - 16, cy, x + w, cy + ROW_H, (cx, b) -> {
                    ProfileManager.delete(name);
                    profiles = ProfileManager.list();
                }, null);
            }
            addHit(x, cy, x + w, cy + ROW_H, (cx, b) -> {
                ProfileManager.switchTo(name);
                profiles = ProfileManager.list();
            }, null);
            cy += ROW_H;
        }
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // any click ends text editing; the clicked text box / button can start it again
        editing = null;
        creating = false;

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.contains(mouseX, mouseY)) {
                hit.click().run(mouseX, button);
                if (hit.drag() != null) {
                    dragging = hit;
                }
                if (dirty) {
                    ModuleManager.save();
                    dirty = false;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null && dragging.drag() != null) {
            dragging.drag().accept(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        if (dirty) {
            ModuleManager.save();
            dirty = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (creating) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                ProfileManager.create(newName);
                profiles = ProfileManager.list();
                creating = false;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                creating = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !newName.isEmpty()) {
                newName = newName.substring(0, newName.length() - 1);
            } else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                newName = ProfileManager.sanitize(newName + clipboard());
            }
            return true;
        }
        if (editing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                    || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editing = null;
                ModuleManager.save();
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String t = editing.get();
                if (!t.isEmpty()) {
                    editing.set(t.substring(0, t.length() - 1));
                }
            } else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                editing.set(editing.get() + clipboard());
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String clipboard() {
        if (client == null) {
            return "";
        }
        String s = GLFW.glfwGetClipboardString(client.getWindow().getHandle());
        return s == null ? "" : s.replace("\n", "").replace("\r", "");
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr < 32 || chr == 127) {
            return true;
        }
        if (creating) {
            newName = ProfileManager.sanitize(newName + chr);
            // sanitize trims trailing spaces; allow typing a space between words
            if (chr == ' ' && !newName.isEmpty() && newName.length() < 20) {
                newName = newName + " ";
            }
            return true;
        }
        if (editing != null) {
            editing.set(editing.get() + chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        ModuleManager.save();
        super.close();
    }
}
