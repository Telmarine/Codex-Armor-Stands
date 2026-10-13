package com.tac5studios.codexarmorstands.edit;

import com.tac5studios.codexarmorstands.config.Features;
import com.tac5studios.codexarmorstands.config.Steps;
import com.tac5studios.codexarmorstands.menus.EquipmentMenu;
import com.tac5studios.codexarmorstands.menus.PresetMenu;
import com.tac5studios.codexarmorstands.messages.Msg;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;

import java.util.Locale;

/** What a click with the tool does to a stand, for the player's current mode. */
public final class Editor {

    private Editor() {}

    /**
     * Applies the current mode once.
     * @param dir +1 for a left click (one step up), -1 for a right click (one step back)
     */
    public static void click(ServerPlayer p, ArmorStand s, int dir) {
        Session ses = Session.of(p);
        Mode mode = ses.mode;
        if (!mode.usable(p)) {
            Msg.send(p, "edit.no_permission", "mode", Msg.raw("mode." + mode.key()));
            return;
        }
        if (!Protection.canEdit(p, s)) {
            Msg.send(p, "edit.not_allowed");
            puff(s);
            return;
        }

        switch (mode.kind) {
            case STEP -> step(p, s, ses, mode, dir);
            case TOGGLE -> toggle(p, s, mode);
            case ACTION -> action(p, s, ses, mode, dir);
        }
    }

    private static void step(ServerPlayer p, ArmorStand s, Session ses, Mode mode, int dir) {
        String value;
        if (mode.isPose()) {
            double size = ses.fine ? Steps.POSE_FINE.get() : Steps.POSE_COARSE.get();
            float step = (float) size;
            value = deg(Stands.turn(s, mode, ses.axis, step * dir));
        } else if (mode == Mode.ROTATE) {
            double size = ses.fine ? Steps.ROTATE_FINE.get() : Steps.ROTATE_COARSE.get();
            float step = (float) size;
            float yaw = Stands.wrap(s.getYRot() + step * dir);
            s.moveTo(s.getX(), s.getY(), s.getZ(), yaw, s.getXRot());
            s.setYBodyRot(yaw);
            s.setYHeadRot(yaw);
            value = deg(yaw);
        } else if (mode == Mode.MOVE) {
            double step = (ses.fine ? Steps.MOVE_FINE.get() : Steps.MOVE_COARSE.get()) * dir;
            double x = s.getX(), y = s.getY(), z = s.getZ();
            switch (ses.axis) {
                case X -> x += step;
                case Y -> y += step;
                case Z -> z += step;
            }
            if (p.distanceToSqr(x, y, z) > 64 * 64) return;
            s.teleportTo(x, y, z);
            value = String.format(Locale.ROOT, "%.3f %.3f %.3f", x, y, z);
        } else { // SCALE
            double step = (ses.fine ? Steps.SCALE_FINE.get() : Steps.SCALE_COARSE.get()) * dir;
            double min = Math.min(Steps.SCALE_MIN.get(), Steps.SCALE_MAX.get());
            double max = Math.max(Steps.SCALE_MIN.get(), Steps.SCALE_MAX.get());
            double v = Math.max(min, Math.min(max, Stands.scale(s) + step));
            Stands.setScale(s, v);
            value = String.format(Locale.ROOT, "%.2fx", Stands.scale(s));
        }
        bar(p, Msg.text("bar.step", "mode", Msg.raw("mode." + mode.key()), "axis", axisText(mode, ses),
                "value", value, "step", Msg.raw(ses.fine ? "step.fine" : "step.coarse")));
        sound(p, 1.2f);
    }

    private static String axisText(Mode mode, Session ses) {
        if (mode == Mode.ROTATE || mode == Mode.SCALE) return "-";
        return ses.axis.name();
    }

    private static void toggle(ServerPlayer p, ArmorStand s, Mode mode) {
        boolean on = switch (mode) {
            case SHOW_ARMS -> {
                s.setShowArms(!s.isShowArms());
                yield s.isShowArms();
            }
            case SMALL -> {
                Stands.setSmall(s, !s.isSmall());
                yield s.isSmall();
            }
            case BASE_PLATE -> {
                s.setNoBasePlate(!s.isNoBasePlate());
                yield !s.isNoBasePlate();
            }
            case INVISIBLE -> {
                s.setInvisible(!s.isInvisible());
                yield s.isInvisible();
            }
            case GRAVITY -> {
                s.setNoGravity(!s.isNoGravity());
                yield !s.isNoGravity();
            }
            case GLOWING -> {
                s.setGlowingTag(!s.hasGlowingTag());
                yield s.hasGlowingTag();
            }
            case INVULNERABLE -> {
                s.setInvulnerable(!s.isInvulnerable());
                yield s.isInvulnerable();
            }
            case NAME_VISIBLE -> {
                s.setCustomNameVisible(!s.isCustomNameVisible());
                yield s.isCustomNameVisible();
            }
            case LOCK -> {
                Stands.setLocked(s, !Stands.locked(s));
                yield Stands.locked(s);
            }
            default -> false;
        };
        bar(p, Msg.text("bar.toggle", "mode", Msg.raw("mode." + mode.key()), "state", Msg.raw(on ? "state.on" : "state.off")));
        sound(p, on ? 1.4f : 0.9f);
    }

    private static void action(ServerPlayer p, ArmorStand s, Session ses, Mode mode, int dir) {
        switch (mode) {
            case EQUIPMENT -> EquipmentMenu.open(p, s);
            case PRESET -> {
                Presets.Preset preset = Presets.get(ses.preset);
                if (preset == null) {
                    new PresetMenu(p).open(p);
                    return;
                }
                preset.pose().apply(s);
                bar(p, Msg.text("bar.preset", "preset", preset.name()));
                sound(p, 1.2f);
            }
            case COPY_PASTE -> {
                int slot = ses.copySlot;
                if (dir > 0) {
                    Copies.set(p, slot, Stands.copy(s));
                    bar(p, Msg.text("bar.copied", "slot", slot + 1));
                } else {
                    CompoundTag t = Copies.get(p, slot);
                    if (t == null) {
                        Msg.send(p, "copy.empty", "slot", slot + 1);
                        return;
                    }
                    Stands.paste(s, t, m -> m.usable(p));
                    bar(p, Msg.text("bar.pasted", "slot", slot + 1));
                }
                sound(p, 1.2f);
            }
            case MIRROR -> {
                Stands.mirror(s);
                bar(p, Msg.text("bar.mirrored"));
                sound(p, 1.2f);
            }
            case RESET -> {
                Stands.resetPose(s);
                bar(p, Msg.text("bar.reset"));
                sound(p, 0.8f);
            }
            case ITEM_FRAME -> Msg.send(p, "frame.click_frame");
            default -> {
            }
        }
    }

    /** Item frame mode: right click = invisible, left click = glowing. */
    public static void frameClick(ServerPlayer p, ItemFrame f, int dir) {
        if (!Mode.ITEM_FRAME.usable(p)) {
            Msg.send(p, "edit.no_permission", "mode", Msg.raw("mode.item_frame"));
            return;
        }
        if (!Protection.canEdit(p, f)) {
            Msg.send(p, "edit.not_allowed");
            return;
        }
        boolean on;
        String what;
        if (dir < 0) {
            f.setInvisible(!f.isInvisible());
            on = f.isInvisible();
            what = Msg.raw("frame.invisible");
        } else {
            f.setGlowingTag(!f.hasGlowingTag());
            on = f.hasGlowingTag();
            what = Msg.raw("frame.glowing");
        }
        bar(p, Msg.text("bar.toggle", "mode", what, "state", Msg.raw(on ? "state.on" : "state.off")));
        sound(p, on ? 1.4f : 0.9f);
    }

    // ---------- feedback ----------

    public static void bar(ServerPlayer p, MutableComponent text) {
        if (Features.ACTION_BAR.get()) p.displayClientMessage(text, true);
    }

    private static void sound(ServerPlayer p, float pitch) {
        p.playNotifySound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.PLAYERS, 0.5f, pitch);
    }

    /** A small smoke puff on a stand someone isn't allowed to touch. */
    @SuppressWarnings("resource") // the level isn't ours to close
    public static void puff(Entity e) {
        if (e.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 6, 0.15, 0.2, 0.15, 0.01);
        }
    }

    private static String deg(float v) {
        return String.format(Locale.ROOT, "%.1f°", v);
    }
}
