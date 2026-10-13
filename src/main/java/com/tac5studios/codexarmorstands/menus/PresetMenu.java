package com.tac5studios.codexarmorstands.menus;

import com.tac5studios.codexarmorstands.edit.Editor;
import com.tac5studios.codexarmorstands.edit.Mode;
import com.tac5studios.codexarmorstands.edit.Presets;
import com.tac5studios.codexarmorstands.edit.Session;
import com.tac5studios.codexarmorstands.messages.Msg;
import com.tac5studios.codexarmorstands.ui.Button;
import com.tac5studios.codexarmorstands.ui.Icons;
import com.tac5studios.codexarmorstands.ui.Menu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Pick a preset pose. Clicking a stand afterwards applies it. 45 presets per page. */
public final class PresetMenu extends Menu {

    private static final int PER_PAGE = 45;
    private final ServerPlayer player;
    private int page;

    public PresetMenu(ServerPlayer player) {
        super(6, Msg.text("presets.title"));
        this.player = player;
    }

    @Override
    protected void draw() {
        Session ses = Session.of(player);
        List<Presets.Preset> all = Presets.all();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < all.size(); i++) {
            Presets.Preset pr = all.get(from + i);
            boolean selected = ses.mode == Mode.PRESET && pr.id().equals(ses.preset);
            List<net.minecraft.network.chat.Component> lore = new ArrayList<>();
            if (!pr.group().isEmpty()) lore.add(Msg.menu("presets.group", "group", pr.group()));
            lore.add(Msg.menu(selected ? "menu.selected" : "presets.click"));
            ItemStack icon = Icons.named(pr.icon(), Msg.menu("presets.name", "name", pr.name()), lore);
            if (selected) icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            set(i, Button.of(icon, (p, click) -> {
                ses.mode = Mode.PRESET;
                ses.preset = pr.id();
                close();
                Editor.bar(p, Msg.text("bar.preset_picked", "preset", pr.name()));
            }));
        }
        if (page > 0) {
            set(5, 0, Button.of(Icons.named(Items.ARROW, Msg.menu("menu.previous")), (p, c) -> {
                page--;
                refresh();
            }));
        }
        if (page < pages - 1) {
            set(5, 8, Button.of(Icons.named(Items.ARROW, Msg.menu("menu.next")), (p, c) -> {
                page++;
                refresh();
            }));
        }
        set(5, 4, Button.of(Icons.named(Items.OAK_DOOR, Msg.menu("menu.back")), (p, c) -> new ModeMenu(p).open(p)));
        fillRow(5);
    }
}
