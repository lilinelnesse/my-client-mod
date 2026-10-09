package com.example.clientmod.gui;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Search for blocks by name and click their pictures to choose which ones get highlighted. */
public class BlockPickerScreen extends Screen {
    private static final int CELL = 20;

    private record Entry(Identifier id, String name, String search, ItemStack icon) {}

    private static List<Entry> allEntries = null;

    private final Screen parent;
    private final Set<String> selected = new LinkedHashSet<>();
    private List<Entry> results = new ArrayList<>();
    private String query = "";
    private int scrollRow = 0;

    private int gridX, gridY, cols, visibleRows;
    private int panelX, panelY, panelW, panelH;
    private Entry hovered = null;

    public BlockPickerScreen(Screen parent) {
        super(Text.literal("Choose blocks"));
        this.parent = parent;
        for (String part : ModuleManager.HL_BLOCKS.get().split(",")) {
            String s = part.trim().toLowerCase(Locale.ROOT);
            if (!s.isEmpty()) {
                selected.add(s.contains(":") ? s : "minecraft:" + s);
            }
        }
    }

    /** How many blocks are in a saved block list (used by the menu button). */
    public static int count(String spec) {
        int n = 0;
        for (String part : spec.split(",")) {
            if (!part.trim().isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private static List<Entry> entries() {
        if (allEntries == null) {
            List<Entry> list = new ArrayList<>();
            for (Block block : Registries.BLOCK) {
                Item item = block.asItem();
                if (item == Items.AIR) {
                    continue; // blocks without an item (fire, water, ...) have no picture
                }
                Identifier id = Registries.BLOCK.getId(block);
                String name = block.getName().getString();
                list.add(new Entry(id, name, (name + " " + id.getPath()).toLowerCase(Locale.ROOT).replace('_', ' '),
                        new ItemStack(item)));
            }
            list.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            allEntries = list;
        }
        return allEntries;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 340);
        panelH = Math.min(height - 16, 260);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        cols = Math.max(1, (panelW - 12) / CELL);
        gridX = panelX + (panelW - cols * CELL) / 2;
        gridY = panelY + 46;
        visibleRows = Math.max(1, (panelH - 46 - 28) / CELL);
        refilter();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void refilter() {
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        List<Entry> found = new ArrayList<>();
        List<Entry> chosen = new ArrayList<>();
        for (Entry e : entries()) {
            boolean match = true;
            for (String w : words) {
                if (!w.isEmpty() && !e.search().contains(w)) {
                    match = false;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            if (query.isBlank() && selected.contains(e.id().toString())) {
                chosen.add(e); // with an empty search, show the chosen blocks first
            } else {
                found.add(e);
            }
        }
        chosen.addAll(found);
        results = chosen;
        scrollRow = 0;
    }

    private int accent() {
        return ModuleManager.accentArgb();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x90000000);
        context.fill(panelX + 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, 0x50000000);
        context.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0xFF2A2A2A);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0151515);
        context.fill(panelX, panelY, panelX + panelW, panelY + 17, 0xF00F0F0F);
        context.fill(panelX, panelY + 16, panelX + panelW, panelY + 17, accent());
        context.drawTextWithShadow(textRenderer, "Choose blocks to highlight", panelX + 6, panelY + 5, 0xFFFFFFFF);
        String count = selected.size() + " selected";
        context.drawTextWithShadow(textRenderer, count, panelX + panelW - 6 - textRenderer.getWidth(count),
                panelY + 5, accent());

        // search box
        int sx = panelX + 6, sy = panelY + 22, sw = panelW - 12;
        context.fill(sx, sy, sx + sw, sy + 16, 0xFF0C0C0C);
        context.fill(sx, sy + 15, sx + sw, sy + 16, accent());
        boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Search blocks, e.g. diamond ore..." : query + (caret ? "_" : "");
        while (shown.length() > 1 && textRenderer.getWidth(shown) > sw - 8) {
            shown = shown.substring(1);
        }
        context.drawTextWithShadow(textRenderer, shown, sx + 4, sy + 4, query.isEmpty() ? 0xFF777777 : 0xFFFFFFFF);

        // grid
        hovered = null;
        int maxScroll = Math.max(0, (results.size() + cols - 1) / cols - visibleRows);
        scrollRow = Math.max(0, Math.min(scrollRow, maxScroll));
        int start = scrollRow * cols;
        int end = Math.min(results.size(), start + visibleRows * cols);
        for (int i = start; i < end; i++) {
            Entry e = results.get(i);
            int cx = gridX + ((i - start) % cols) * CELL;
            int cy = gridY + ((i - start) / cols) * CELL;
            boolean on = selected.contains(e.id().toString());
            boolean over = mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL;
            if (on) {
                context.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, (accent() & 0x00FFFFFF) | 0x80000000);
                context.fill(cx, cy, cx + CELL - 1, cy + 1, accent());
                context.fill(cx, cy + CELL - 2, cx + CELL - 1, cy + CELL - 1, accent());
                context.fill(cx, cy, cx + 1, cy + CELL - 1, accent());
                context.fill(cx + CELL - 2, cy, cx + CELL - 1, cy + CELL - 1, accent());
            } else if (over) {
                context.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0x30FFFFFF);
            }
            context.drawItem(e.icon(), cx + 2, cy + 2);
            if (over) {
                hovered = e;
            }
        }
        if (results.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No blocks found", gridX, gridY + 4, 0xFF9A9A9A);
        }

        // bottom buttons
        int by = panelY + panelH - 20;
        drawButton(context, panelX + 6, by, 60, "Clear all", mouseX, mouseY);
        drawButton(context, panelX + panelW - 66, by, 60, "Done", mouseX, mouseY);
        if (maxScroll > 0) {
            String page = "scroll for more";
            context.drawTextWithShadow(textRenderer, page, panelX + (panelW - textRenderer.getWidth(page)) / 2,
                    by + 5, 0xFF777777);
        }

        if (hovered != null) {
            String tip = hovered.name() + "  (" + hovered.id().getPath() + ")";
            int tw = textRenderer.getWidth(tip) + 8;
            int tx = Math.min(mouseX + 8, width - tw - 2);
            int ty = mouseY + 12;
            context.fill(tx, ty, tx + tw, ty + 14, 0xF0000000);
            context.drawTextWithShadow(textRenderer, tip, tx + 4, ty + 3, 0xFFFFFFFF);
        }
    }

    private void drawButton(DrawContext context, int x, int y, int w, String label, int mx, int my) {
        boolean over = mx >= x && mx < x + w && my >= y && my < y + 16;
        context.fill(x, y, x + w, y + 16, over ? 0xFF3A3A3A : 0xFF262626);
        context.drawTextWithShadow(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 4, accent());
    }

    private void writeBack() {
        StringBuilder sb = new StringBuilder();
        for (String id : selected) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id);
        }
        ModuleManager.HL_BLOCKS.set(sb.toString());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int by = panelY + panelH - 20;
        if (my >= by && my < by + 16) {
            if (mx >= panelX + 6 && mx < panelX + 66) {
                selected.clear();
                writeBack();
                refilter();
                return true;
            }
            if (mx >= panelX + panelW - 66 && mx < panelX + panelW - 6) {
                close();
                return true;
            }
        }
        if (hovered != null) {
            String id = hovered.id().toString();
            if (!selected.remove(id)) {
                selected.add(id);
            }
            writeBack();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollRow -= (int) Math.signum(verticalAmount) * 2;
        if (scrollRow < 0) {
            scrollRow = 0;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                refilter();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // Esc closes
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127 && query.length() < 40) {
            query += chr;
            refilter();
        }
        return true;
    }

    @Override
    public void close() {
        writeBack();
        ModuleManager.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
