package com.tac5studios.codexarmorstands.menus;

import com.tac5studios.codexarmorstands.config.Steps;
import com.tac5studios.codexarmorstands.edit.Copies;
import com.tac5studios.codexarmorstands.edit.Editor;
import com.tac5studios.codexarmorstands.edit.Mode;
import com.tac5studios.codexarmorstands.edit.Session;
import com.tac5studios.codexarmorstands.messages.Msg;
import com.tac5studios.codexarmorstands.ui.Button;
import com.tac5studios.codexarmorstands.ui.Icons;
import com.tac5studios.codexarmorstands.ui.Menu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Pick a copy slot. Afterwards: left-click a stand copies it into the slot, right-click pastes the slot.
 * Shift + right-click a slot here empties it.
 */
public final class CopyMenu extends Menu {

    private final ServerPlayer player;

    public CopyMenu(ServerPlayer player) {
        super(2, Msg.text("copy.title"));
        this.player = player;
    }

    @Override
    protected void draw() {
        Session ses = Session.of(player);
        int slots = Steps.COPY_SLOTS.get();
        for (int i = 0; i < slots; i++) {
            final int slot = i;
            boolean filled = Copies.get(player, i) != null;
            boolean selected = ses.mode == Mode.COPY_PASTE && ses.copySlot == i;
            ItemStack icon = Icons.named(filled ? Items.FILLED_MAP : Items.MAP, Msg.menu("copy.slot", "slot", i + 1), List.of(
                    Msg.menu(filled ? "copy.filled" : "copy.empty_lore"),
                    Msg.menu("copy.how"),
                    Msg.menu(selected ? "menu.selected" : "menu.click_select")));
            icon.setCount(i + 1);
            if (selected) icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            set(i, Button.of(icon, (p, click) -> {
                if (click == com.tac5studios.codexarmorstands.ui.Click.SHIFT_RIGHT) {
                    Copies.clear(p, slot);
                    Msg.send(p, "copy.cleared", "slot", slot + 1);
                    refresh();
                    return;
                }
                ses.mode = Mode.COPY_PASTE;
                ses.copySlot = slot;
                close();
                Editor.bar(p, Msg.text("bar.copy_slot", "slot", slot + 1));
            }));
        }
        fillRow(0);
        set(1, 4, Button.of(Icons.named(Items.OAK_DOOR, Msg.menu("menu.back")), (p, c) -> new ModeMenu(p).open(p)));
        fillRow(1);
    }
}
