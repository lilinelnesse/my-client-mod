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
    private static final int PANEL_BG = 0xFF1A1A1A;
    private static final int HEADER_BG = 0xFF141414;
    private static final int ROW_HOVER = 0x18FFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int DIM = 0xFFB8B8B8;
    private static final int ROW_H = 22;
    private static final int SETTING_H = 16;
    private static final int SLIDER_H = 22;
    private static final int HEADER_H = 24;

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
        return ModuleManager.accentArgb();
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
        int pw = Math.max(100, Math.min(128, (width - 16 - (n - 1) * 3) / n));
        int total = n * pw + (n - 1) * 3;
        int x = (width - total) / 2;
        int y = Math.max(8, (height - 250) / 4);

        drawSettingsPanel(context, x, y, pw, mouseX, mouseY);
        x += pw + 3;
        for (String category : ModuleManager.CATEGORIES) {
            drawCategoryPanel(context, category, x, y, pw, mouseX, mouseY);
            x += pw + 3;
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

    /** Rounded-corner rectangle (radius 6): top and/or bottom corners can be rounded. */
    private void rrect(DrawContext context, int x, int y, int w, int h, int color, boolean top, boolean bottom) {
        int[] inset = {4, 2, 1, 1, 0, 0};
        for (int row = 0; row < h; row++) {
            int in = 0;
            if (top && row < inset.length) {
                in = inset[row];
            }
            if (bottom && h - 1 - row < inset.length) {
                in = Math.max(in, inset[h - 1 - row]);
            }
            context.fill(x + in, y + row, x + w - in, y + row + 1, color);
        }
    }

    private void panel(DrawContext context, int x, int y, int w, int h) {
        rrect(context, x, y, w, h, PANEL_BG, true, true);
    }

    private static final String[] ICON_HUD = {
            "#########", "#.......#", "#.#####.#", "#.......#", "#.###...#", "#.......#", "#########"};
    private static final String[] ICON_RENDER = {
            "..#####..", ".#.....#.", "#..###..#", "#.#####.#", "#..###..#", ".#.....#.", "..#####.."};
    private static final String[] ICON_UTILITY = {
            "##.....##", ".##...##.", "..##.##..", "...###...", "..##.##..", ".##...##.", "##.....##"};
    private static final String[] ICON_PROFILES = {
            "#########", ".........", "#########", ".........", "#####...."};
    private static final String[] ICON_DEFAULT = {
            ".#######.", "#.......#", "#.#.#.#.#", "#.......#", ".#######."};

    private static String[] iconFor(String title) {
        return switch (title) {
            case "HUD" -> ICON_HUD;
            case "Render" -> ICON_RENDER;
            case "Utility" -> ICON_UTILITY;
            case "Profiles" -> ICON_PROFILES;
            default -> ICON_DEFAULT;
        };
    }

    private void icon(DrawContext context, int x, int y, String[] rows, int color) {
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length(); c++) {
                if (rows[r].charAt(c) == '#') {
                    context.fill(x + c, y + r, x + c + 1, y + r + 1, color);
                }
            }
        }
    }

    private int drawHeader(DrawContext context, String title, int x, int y, int w) {
        rrect(context, x, y, w, HEADER_H, HEADER_BG, true, false);
        icon(context, x + 8, y + 8, iconFor(title), TEXT);
        context.drawTextWithShadow(textRenderer, title, x + 22, y + 8, TEXT);
        return y + HEADER_H;
    }

    // ---- Settings panel ----

    /** Filled circle of the given radius. */
    private void circle(DrawContext context, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.5));
            context.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** A gradient slider (colors come from the gradient function) bound to an IntSetting. */
    private int drawColorSlider(DrawContext context, String label, IntSetting setting, int x, int y, int w,
                                java.util.function.IntUnaryOperator gradient, boolean showReset, boolean ringKnob) {
        if (label != null) {
            context.drawTextWithShadow(textRenderer, label, x + 8, y, DIM);
        }
        if (showReset) {
            String reset = "RESET";
            int rx = x + w - 8 - textRenderer.getWidth(reset);
            context.drawTextWithShadow(textRenderer, reset, rx, y, 0xFF8A8A8A);
            addHit(rx - 2, y - 3, x + w - 6, y + 11, (cx, b) -> {
                ModuleManager.resetAccent();
                markDirty();
            }, null);
        }
        int sx = x + 10, sw = w - 20;
        int sy = label != null ? y + 16 : y + 5;
        for (int i = 0; i < sw; i++) {
            int rgb = gradient.applyAsInt(i * 1000 / Math.max(1, sw - 1));
            context.fill(sx + i, sy, sx + i + 1, sy + 2, 0xFF000000 | rgb);
        }
        double frac = (setting.get() - setting.getMin()) / (double) (setting.getMax() - setting.getMin());
        int kx = sx + (int) Math.round(frac * (sw - 1));
        if (ringKnob) {
            circle(context, kx, sy, 5, 0xFF000000 | ModuleManager.hsb(setting.get(), 100, 100));
            circle(context, kx, sy, 3, 0xFF1A1A1A);
        } else {
            circle(context, kx, sy, 5, 0xFFE9E9E9);
        }
        DoubleConsumer setter = m -> {
            double f = Math.max(0, Math.min(1, (m - sx) / (double) (sw - 1)));
            int val = setting.getMin() + (int) Math.round(f * (setting.getMax() - setting.getMin()));
            if (val != setting.get()) {
                setting.set(val);
                markDirty();
            }
        };
        addHit(x, sy - 8, x + w, sy + 9, (cx, b) -> setter.accept(cx), setter);
        return label != null ? y + 30 : y + 16;
    }

    private static final String[] NAV_ROWS = {"General", "Modules", "GUI", "Sound", "Notifications"};

    private void drawSettingsPanel(DrawContext context, int x, int y, int w, int mx, int my) {
        int h = HEADER_H + NAV_ROWS.length * ROW_H + 26 + 16 + 96 + 26 + 6;
        panel(context, x, y, w, h);

        // header: round back button, title, close
        rrect(context, x, y, w, HEADER_H, HEADER_BG, true, false);
        circle(context, x + 15, y + HEADER_H / 2, 7, 0xFF2C2C2C);
        context.drawTextWithShadow(textRenderer, "<", x + 13, y + 8, TEXT);
        context.drawTextWithShadow(textRenderer, "Settings", x + 29, y + 8, TEXT);
        context.drawTextWithShadow(textRenderer, "x", x + w - 13, y + 8, DIM);
        addHit(x + w - 22, y, x + w, y + HEADER_H, (cx, b) -> close(), null);

        int cy = y + HEADER_H;
        for (String name : NAV_ROWS) {
            boolean hover = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
            if (hover) {
                context.fill(x, cy, x + w, cy + ROW_H, ROW_HOVER);
            }
            context.drawTextWithShadow(textRenderer, name, x + 8, cy + 7, DIM);
            context.drawTextWithShadow(textRenderer, ">", x + w - 14, cy + 7, 0xFF777777);
            cy += ROW_H;
        }

        // GUI Theme section
        context.drawTextWithShadow(textRenderer, "GUI Theme", x + 8, cy + 7, DIM);
        context.drawTextWithShadow(textRenderer, "^", x + 8 + textRenderer.getWidth("GUI Theme") + 5, cy + 7, DIM);
        rrect(context, x + w - 18, cy + 6, 10, 10, accent(), true, true);
        cy += 22 + 4;

        cy = drawColorSlider(context, null, ModuleManager.GUI_HUE, x, cy, w,
                v -> ModuleManager.hsb(v * 360 / 1000, 100, 100), false, true);
        final int hue = ModuleManager.GUI_HUE.get();
        cy = drawColorSlider(context, "Custom color", ModuleManager.GUI_HUE, x, cy + 2, w,
                v -> ModuleManager.hsb(v * 360 / 1000, 100, 100), true, false);
        cy = drawColorSlider(context, "Saturation", ModuleManager.GUI_SAT, x, cy + 2, w,
                v -> ModuleManager.hsb(hue, v / 10, 100), false, false);
        cy = drawColorSlider(context, "Vibrance", ModuleManager.GUI_VIB, x, cy + 2, w,
                v -> ModuleManager.hsb(hue, ModuleManager.GUI_SAT.get(), v / 10), false, false);

        context.drawTextWithShadow(textRenderer, "Rebind GUI", x + 8, cy + 8, DIM);
        String key = "Right Shift";
        int kw = textRenderer.getWidth(key) + 10;
        rrect(context, x + w - 8 - kw, cy + 5, kw, 14, 0xFF232323, true, true);
        context.drawTextWithShadow(textRenderer, key, x + w - 3 - kw, cy + 8, 0xFF6A6A6A);
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
        panel(context, x, y, w, h);
        int cy = drawHeader(context, category, x, y, w);
        context.drawTextWithShadow(textRenderer, "^", x + w - 14, y + 8, DIM);

        for (Module m : modules) {
            boolean open = expanded.contains(m.getId());
            boolean hover = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
            boolean lastRow = !open && m == modules.get(modules.size() - 1);
            if (m.isEnabled()) {
                rrect(context, x, cy, w, ROW_H, accent(), false, lastRow);
            } else if (hover) {
                context.fill(x, cy, x + w, cy + ROW_H, ROW_HOVER);
            }
            context.drawTextWithShadow(textRenderer, m.getName(), x + 8, cy + 7, m.isEnabled() ? TEXT : DIM);

            if (m.hasSettings()) {
                int dx = x + w - 12;
                int dy = cy + 6;
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
            if (str == ModuleManager.HL_BLOCKS || str == ModuleManager.SE_BLOCKS) {
                // block list: a button that opens the search screen with block pictures
                int n = BlockPickerScreen.count(str.get());
                context.fill(ix, y + 2, ix + iw, y + h - 2, 0xFF262626);
                String label = trim("Choose " + (str == ModuleManager.SE_BLOCKS ? "storage" : "blocks") + " (" + n + ")", iw - 6);
                context.drawTextWithShadow(textRenderer, label,
                        ix + (iw - textRenderer.getWidth(label)) / 2, y + 7, accent());
                addHit(x, y, x + w, y + h, (cx, btn) -> {
                    if (client != null) {
                        client.setScreen(str == ModuleManager.SE_BLOCKS
                                ? new BlockPickerScreen(this, str, com.example.clientmod.feature.StorageEsp::isStorage,
                                        "Choose storage blocks to show")
                                : new BlockPickerScreen(this));
                    }
                }, null);
            } else {
                context.drawTextWithShadow(textRenderer, trim(s.getLabel(), iw), ix, y + 1, DIM);
                boolean active = editing == str;
                int by = y + 11;
                context.fill(ix, by, ix + iw, by + 10, active ? 0xFF2A2A2A : 0xFF1E1E1E);
                String shown = str.get() + (active && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
                while (shown.length() > 1 && textRenderer.getWidth(shown) > iw - 4) {
                    shown = shown.substring(1);
                }
                context.drawTextWithShadow(textRenderer, shown, ix + 2, by + 1, TEXT);
                addHit(x, y, x + w, y + h, (cx, btn) -> editing = str, null);
            }
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
        panel(context, x, y, w, HEADER_H + rows * ROW_H + 4);
        int cy = drawHeader(context, "Profiles", x, y, w);

        // CREATE button: accent plus-circle and text
        boolean hoverCreate = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
        if (hoverCreate) {
            context.fill(x, cy, x + w, cy + ROW_H, ROW_HOVER);
        }
        int ccx = x + 14, ccy = cy + ROW_H / 2;
        circle(context, ccx, ccy, 6, accent());
        context.fill(ccx - 3, ccy, ccx + 4, ccy + 1, TEXT);
        context.fill(ccx, ccy - 3, ccx + 1, ccy + 4, TEXT);
        context.drawTextWithShadow(textRenderer, "CREATE", x + 26, cy + 7, TEXT);
        addHit(x, cy, x + w, cy + ROW_H, (cx, b) -> {
            creating = true;
            newName = "";
            editing = null;
        }, null);
        cy += ROW_H;

        if (creating) {
            context.fill(x + 4, cy + 3, x + w - 4, cy + ROW_H - 3, 0xFF2A2A2A);
            String shown = newName + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            while (shown.length() > 1 && textRenderer.getWidth(shown) > w - 16) {
                shown = shown.substring(1);
            }
            context.drawTextWithShadow(textRenderer, shown.isEmpty() ? "name..." : shown, x + 8, cy + 7,
                    newName.isEmpty() ? DIM : TEXT);
            cy += ROW_H;
        }

        String active = ProfileManager.getActive();
        for (String name : new ArrayList<>(profiles)) {
            boolean isActive = name.equals(active);
            boolean hover = mx >= x && mx < x + w && my >= cy && my < cy + ROW_H;
            if (isActive) {
                rrect(context, x + 4, cy + 2, w - 8, ROW_H - 4, accent(), true, true);
            } else if (hover) {
                rrect(context, x + 4, cy + 2, w - 8, ROW_H - 4, ROW_HOVER, true, true);
            }
            boolean deletable = !ProfileManager.DEFAULT.equals(name);
            int textMax = w - 12 - (deletable ? 14 : 0);
            context.drawTextWithShadow(textRenderer, trim(name, textMax), x + 11, cy + 7, isActive ? TEXT : DIM);
            if (deletable) {
                context.drawTextWithShadow(textRenderer, "x", x + w - 14, cy + 7, isActive ? TEXT : DIM);
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
