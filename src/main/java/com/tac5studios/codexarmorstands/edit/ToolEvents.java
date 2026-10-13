package com.tac5studios.codexarmorstands.edit;

import com.tac5studios.codexarmorstands.CodexArmorStands;
import com.tac5studios.codexarmorstands.config.Features;
import com.tac5studios.codexarmorstands.config.Steps;
import com.tac5studios.codexarmorstands.config.ToolConfig;
import com.tac5studios.codexarmorstands.menus.ModeMenu;
import com.tac5studios.codexarmorstands.messages.Msg;
import com.tac5studios.codexarmorstands.perms.Perm;
import com.tac5studios.codexarmorstands.util.Text;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/**
 * Every way the tool is used:
 *  - right-click air or a block: open the tool menu
 *  - left-click a stand: one step +    right-click a stand: one step -
 *  - sneak + scroll the hotbar: switch axis
 *  - swap-hands key (F): edit the nearest stand (+), sneak + F (-)
 * Plus: owners for newly placed stands, the per-chunk limit, colored name tags, locked-stand protection.
 */
@EventBusSubscriber(modid = CodexArmorStands.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings({"unused", "resource"}) // handlers are called by the event bus; levels belong to the server
public final class ToolEvents {

    /** Who is placing an armor stand this tick (set by the right-click, read when the stand joins the world). */
    private static UUID placer;
    private static long placeTick = -1;

    private ToolEvents() {}

    private static boolean holdingTool(ServerPlayer p) {
        return Features.editor() && ToolConfig.isTool(p.getMainHandItem()) && Perm.has(p, Perm.USE);
    }

    // ---------- clicks on stands and frames ----------

    /** Left click: one step up. Also keeps locked stands from being broken by others. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Entity target = e.getTarget();
        if (target instanceof ArmorStand s) {
            if (holdingTool(p)) {
                e.setCanceled(true);
                Editor.click(p, s, +1);
                return;
            }
            if (Features.editor() && Features.LOCK_PROTECTS.get() && Stands.locked(s) && !Protection.canBreak(p, s)) {
                e.setCanceled(true);
                Editor.puff(s);
                Msg.send(p, "edit.locked");
            }
        } else if (target instanceof ItemFrame f && holdingTool(p) && Session.of(p).mode == Mode.ITEM_FRAME) {
            e.setCanceled(true);
            Editor.frameClick(p, f, +1);
        }
    }

    /** Right click on a stand: one step back. Armor stands use the "interact at" path. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getTarget() instanceof ArmorStand s)) return;
        if (holdingTool(p)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            if (e.getHand() == InteractionHand.MAIN_HAND) Editor.click(p, s, -1);
            return;
        }
        if (Features.editor() && Stands.locked(s) && !p.isSpectator()) Editor.puff(s);
    }

    /** Right click on an item frame (item frame mode), and colored name tags on stands. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (e.getTarget() instanceof ItemFrame f && holdingTool(p) && Session.of(p).mode == Mode.ITEM_FRAME) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            if (e.getHand() == InteractionHand.MAIN_HAND) Editor.frameClick(p, f, -1);
            return;
        }
        if (e.getTarget() instanceof ArmorStand s) colorName(e, p, s);
    }

    private static void colorName(PlayerInteractEvent.EntityInteract e, ServerPlayer p, ArmorStand s) {
        ItemStack tag = e.getItemStack();
        if (!tag.is(Items.NAME_TAG) || !Features.on(Features.RENAME_COLORS)) return;
        Component name = tag.get(DataComponents.CUSTOM_NAME);
        if (name == null || !name.getString().contains("&") || !Perm.has(p, Perm.RENAME_COLOR)) return;
        if (!Protection.canEdit(p, s)) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
            Msg.send(p, "edit.not_allowed");
            return;
        }
        s.setCustomName(Text.color(name.getString()));
        if (!p.getAbilities().instabuild) tag.shrink(1);
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ---------- opening the menu ----------

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || !holdingTool(p)) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        new ModeMenu(p).open(p);
    }

    @SubscribeEvent
    public static void onUseOnBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (e.getItemStack().is(Items.ARMOR_STAND)) {
            placer = p.getUUID();
            placeTick = p.serverLevel().getGameTime();
            return;
        }
        if (e.getHand() != InteractionHand.MAIN_HAND || !holdingTool(p)) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        new ModeMenu(p).open(p);
    }

    // ---------- swap-hands key and hotbar scroll ----------

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Features.on(Features.SWAP_HANDS_NEAREST) || !holdingTool(p)) return;
        e.setCanceled(true);
        ArmorStand s = Targeting.nearest(p, ToolConfig.NEAREST_RANGE.get());
        if (s == null) {
            Msg.send(p, "edit.none_near");
            return;
        }
        Editor.click(p, s, p.isShiftKeyDown() ? -1 : +1);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Session ses = Session.of(p);
        int sel = p.getInventory().selected;
        if (Features.on(Features.SCROLL_AXIS) && ses.lastHeldTool && p.isShiftKeyDown() && ses.lastSlot >= 0 && sel != ses.lastSlot) {
            int diff = sel - ses.lastSlot;
            if (diff > 4) diff -= 9;
            if (diff < -4) diff += 9;
            ses.axis = ses.axis.next(diff > 0 ? 1 : -1);
            p.getInventory().selected = ses.lastSlot;
            p.connection.send(new ClientboundSetCarriedItemPacket(ses.lastSlot));
            Editor.bar(p, Msg.text("bar.axis", "axis", ses.axis.name()));
            return;
        }
        ses.lastSlot = sel;
        ses.lastHeldTool = holdingTool(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Session.forget(e.getEntity().getUUID());
    }

    // ---------- placing stands ----------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() || !(e.getEntity() instanceof ArmorStand s) || !(e.getLevel() instanceof ServerLevel level)) return;
        if (!Features.editor() || placer == null || placeTick != level.getGameTime()) return;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(placer);
        placer = null;
        if (p == null || p.level() != level || p.distanceToSqr(s) > 10 * 10) return;

        if (Features.on(Features.CHUNK_LIMIT) && !Perm.has(p, Perm.LIMIT_BYPASS)) {
            ChunkPos cp = s.chunkPosition();
            AABB box = new AABB(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                    cp.getMaxBlockX() + 1, level.getMaxBuildHeight(), cp.getMaxBlockZ() + 1);
            int count = level.getEntitiesOfClass(ArmorStand.class, box, ArmorStand::isAlive).size();
            int limit = Steps.CHUNK_LIMIT.get();
            if (count >= limit) {
                e.setCanceled(true);
                // The item is used up after the stand is added; give one back so nothing is lost.
                if (!p.getAbilities().instabuild) p.getInventory().add(new ItemStack(Items.ARMOR_STAND));
                Msg.send(p, "limit.reached", "limit", limit);
                return;
            }
        }
        if (Features.OWNERSHIP.get()) Protection.setOwner(s, p.getUUID());
    }
}
