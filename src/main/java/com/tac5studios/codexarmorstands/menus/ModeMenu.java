package com.tac5studios.codexarmorstands.menus;

import com.tac5studios.codexarmorstands.config.Steps;
import com.tac5studios.codexarmorstands.edit.Axis;
import com.tac5studios.codexarmorstands.edit.Editor;
import com.tac5studios.codexarmorstands.edit.Mode;
import com.tac5studios.codexarmorstands.edit.Session;
import com.tac5studios.codexarmorstands.messages.Msg;
import com.tac5studios.codexarmorstands.ui.Button;
import com.tac5studios.codexarmorstands.ui.Icons;
import com.tac5studios.codexarmorstands.ui.Menu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The tool menu (right-click air or a block with the tool).
 *  Row 1: pose parts, rotate, move, scale
 *  Row 2: on/off modes
 *  Row 3: equipment, presets, copy/paste, mirror, reset, item frames
 *  Row 5: axis X / Y / Z, coarse / fine, close
 * Modes that are switched off or not allowed show as blank panes.
 */
public final class ModeMenu extends Menu {

    private static final Mode[][] LAYOUT = {
            {Mode.HEAD, Mode.BODY, Mode.LEFT_ARM, Mode.RIGHT_ARM, Mode.LEFT_LEG, Mode.RIGHT_LEG, Mode.ROTATE, Mode.MOVE, Mode.SCALE},
            {Mode.SHOW_ARMS, Mode.SMALL, Mode.BASE_PLATE, Mode.INVISIBLE, Mode.GRAVITY, Mode.GLOWING, Mode.INVULNERABLE, Mode.NAME_VISIBLE, Mode.LOCK},
            {Mode.EQUIPMENT, Mode.PRESET, Mode.COPY_PASTE, Mode.MIRROR, Mode.RESET, Mode.ITEM_FRAME},
    };

    private final ServerPlayer player;

    public ModeMenu(ServerPlayer player) {
        super(5, Msg.text("menu.title"));
        this.player = player;
    }

    @Override
    protected void draw() {
        Session ses = Session.of(player);
        for (int r = 0; r < LAYOUT.length; r++) {
            for (int c = 0; c < LAYOUT[r].length; c++) {
                Mode m = LAYOUT[r][c];
                if (!m.usable(player)) continue;
                set(r, c, Button.of(modeIcon(m, ses.mode == m), (p, click) -> pick(p, m)));
            }
        }
        fillRow(0);
        fillRow(1);
        fillRow(2);
        fillRow(3);

        set(4, 0, axisButton(Axis.X, Items.RED_WOOL, ses));
        set(4, 1, axisButton(Axis.Y, Items.LIME_WOOL, ses));
        set(4, 2, axisButton(Axis.Z, Items.LIGHT_BLUE_WOOL, ses));

        ItemStack step = Icons.named(Items.CLOCK, Msg.menu(ses.fine ? "menu.step_fine" : "menu.step_coarse"), List.of(
                Msg.menu("menu.step_lore",
                        "pose", fmt(ses.fine ? Steps.POSE_FINE.get() : Steps.POSE_COARSE.get()),
                        "move", fmt(ses.fine ? Steps.MOVE_FINE.get() : Steps.MOVE_COARSE.get())),
                Msg.menu("menu.click_switch")));
        set(4, 4, Button.of(step, (p, click) -> {
            ses.fine = !ses.fine;
            refresh();
        }));
        set(4, 8, Button.of(Icons.named(Items.BARRIER, Msg.menu("menu.close")), (p, click) -> close()));
        fillRow(4);
    }

    private void pick(ServerPlayer p, Mode m) {
        Session ses = Session.of(p);
        ses.mode = m;
        switch (m) {
            case PRESET -> new PresetMenu(p).open(p);
            case COPY_PASTE -> new CopyMenu(p).open(p);
            default -> {
                close();
                Editor.bar(p, Msg.text("bar.mode", "mode", Msg.raw("mode." + m.key())));
            }
        }
    }

    private ItemStack modeIcon(Mode m, boolean selected) {
        ItemStack s = Icons.named(m.icon, Msg.menu("mode." + m.key()), List.of(
                Msg.menu("help." + m.key()),
                Msg.menu(selected ? "menu.selected" : "menu.click_select")));
        if (selected) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    private Button axisButton(Axis a, Item item, Session ses) {
        ItemStack s = Icons.named(item, Msg.menu("menu.axis", "axis", a.name()),
                List.of(Msg.menu("menu.axis_lore"), Msg.menu(ses.axis == a ? "menu.selected" : "menu.click_select")));
        if (ses.axis == a) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return Button.of(s, (p, click) -> {
            ses.axis = a;
            refresh();
        });
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
