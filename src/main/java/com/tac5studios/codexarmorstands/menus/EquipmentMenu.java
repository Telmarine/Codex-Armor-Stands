package com.tac5studios.codexarmorstands.menus;

import com.tac5studios.codexarmorstands.edit.Protection;
import com.tac5studios.codexarmorstands.edit.Stands;
import com.tac5studios.codexarmorstands.messages.Msg;
import com.tac5studios.codexarmorstands.perms.Perm;
import com.tac5studios.codexarmorstands.ui.Icons;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * The stand's six slots in a one-row chest: head, chest, legs, feet, main hand, off hand.
 * The slots read and write the stand directly, so nothing is copied and nothing can be duplicated.
 * A locked stand can only be changed here by its owner or staff.
 */
@SuppressWarnings("resource") // levels belong to the server
public final class EquipmentMenu extends AbstractContainerMenu {

    static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private final ArmorStand stand;
    private final ServerPlayer viewer;
    private final SimpleContainer filler = new SimpleContainer(3);

    public static void open(ServerPlayer p, ArmorStand stand) {
        if (Stands.locked(stand) && !Protection.canBreak(p, stand)) {
            Msg.send(p, "equipment.locked");
            return;
        }
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new EquipmentMenu(id, inv, stand, p), Msg.text("equipment.title")));
    }

    private EquipmentMenu(int id, Inventory inv, ArmorStand stand, ServerPlayer viewer) {
        super(MenuType.GENERIC_9x1, id);
        this.stand = stand;
        this.viewer = viewer;
        Container standSlots = new StandContainer(stand);
        for (int i = 0; i < SLOTS.length; i++) addSlot(new StandSlot(standSlots, i));
        for (int i = 0; i < 3; i++) {
            filler.setItem(i, Icons.filler());
            addSlot(new Locked(filler, i));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 109));
    }

    private boolean mayChange() {
        return !Stands.locked(stand) || Protection.canBreak(viewer, stand) || Perm.has(viewer, Perm.BYPASS);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return stand.isAlive() && stand.level() == player.level() && player.distanceToSqr(stand) < 64;
    }

    @Override
    public void clicked(int slotId, int button, @Nonnull ClickType type, @Nonnull Player player) {
        if (slotId >= 6 && slotId < 9) { // filler panes
            sendAllDataToRemote();
            return;
        }
        if (type == ClickType.PICKUP_ALL || type == ClickType.QUICK_CRAFT) {
            sendAllDataToRemote();
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < 6) {
            if (!moveItemStackTo(stack, 9, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index >= 9) {
            int target = targetSlot(stack);
            if (target < 0) return ItemStack.EMPTY;
            Slot to = slots.get(target);
            if (to.hasItem() || !to.mayPlace(stack)) return ItemStack.EMPTY;
            to.set(stack.split(to.getMaxStackSize(stack)));
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    /** Where shift-click puts an item: its armor slot, else the main hand, else the off hand. */
    private int targetSlot(ItemStack stack) {
        EquipmentSlot es = stand.getEquipmentSlotForItem(stack);
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == es && es.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !slots.get(i).hasItem()) return i;
        }
        if (!slots.get(4).hasItem()) return 4;
        if (!slots.get(5).hasItem()) return 5;
        return -1;
    }

    @Override
    public boolean canDragTo(@Nonnull Slot slot) {
        return !(slot.container == filler);
    }

    /** One of the stand's slots. */
    private final class StandSlot extends Slot {

        private final EquipmentSlot es;

        StandSlot(Container c, int i) {
            super(c, i, 0, 0);
            this.es = SLOTS[i];
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            if (!mayChange()) return false;
            if (es.getType() == EquipmentSlot.Type.HAND) return stand.isShowArms();
            if (es == EquipmentSlot.HEAD) return true; // anything can be worn on the head, like /item replace
            return stand.getEquipmentSlotForItem(stack) == es;
        }

        @Override
        public boolean mayPickup(@Nonnull Player player) {
            return mayChange();
        }

        @Override
        public int getMaxStackSize() {
            return es.getType() == EquipmentSlot.Type.HAND ? 64 : 1;
        }

        @Override
        public int getMaxStackSize(@Nonnull ItemStack stack) {
            return Math.min(getMaxStackSize(), stack.getMaxStackSize());
        }
    }

    private static final class Locked extends Slot {
        Locked(Container c, int i) {
            super(c, i, 0, 0);
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@Nonnull Player player) {
            return false;
        }
    }

    /** A Container view of the stand's slots. */
    private static final class StandContainer implements Container {

        private final ArmorStand stand;

        StandContainer(ArmorStand stand) {
            this.stand = stand;
        }

        @Override
        public int getContainerSize() {
            return SLOTS.length;
        }

        @Override
        public boolean isEmpty() {
            for (EquipmentSlot s : SLOTS) if (!stand.getItemBySlot(s).isEmpty()) return false;
            return true;
        }

        @Override
        @Nonnull
        public ItemStack getItem(int i) {
            return stand.getItemBySlot(SLOTS[i]);
        }

        @Override
        @Nonnull
        public ItemStack removeItem(int i, int count) {
            ItemStack cur = stand.getItemBySlot(SLOTS[i]);
            if (cur.isEmpty() || count <= 0) return ItemStack.EMPTY;
            ItemStack out = cur.split(count);
            stand.setItemSlot(SLOTS[i], cur.isEmpty() ? ItemStack.EMPTY : cur);
            return out;
        }

        @Override
        @Nonnull
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack cur = stand.getItemBySlot(SLOTS[i]);
            stand.setItemSlot(SLOTS[i], ItemStack.EMPTY);
            return cur;
        }

        @Override
        public void setItem(int i, @Nonnull ItemStack stack) {
            stand.setItemSlot(SLOTS[i], stack);
        }

        @Override
        public void setChanged() {
            // The stand syncs its own equipment.
        }

        @Override
        public boolean stillValid(@Nonnull Player player) {
            return stand.isAlive();
        }

        @Override
        public void clearContent() {
            // Never clear the stand from here.
        }
    }
}
