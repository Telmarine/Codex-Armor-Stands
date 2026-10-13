package com.tac5studios.codexarmorstands.edit;

import com.tac5studios.codexarmorstands.mixin.ArmorStandAccessor;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;

/** Reads and changes armor stand data. Everything here is synced to players by vanilla. */
public final class Stands {

    /** Every slot: blocked from changing (bits 0-5). Same as DisabledSlots 63 in commands. */
    public static final int ALL_SLOTS_LOCKED = 63;

    private Stands() {}

    // ---------- pose ----------

    public static Rotations pose(ArmorStand s, Mode part) {
        return switch (part) {
            case HEAD -> s.getHeadPose();
            case BODY -> s.getBodyPose();
            case LEFT_ARM -> s.getLeftArmPose();
            case RIGHT_ARM -> s.getRightArmPose();
            case LEFT_LEG -> s.getLeftLegPose();
            case RIGHT_LEG -> s.getRightLegPose();
            default -> throw new IllegalArgumentException(part.name());
        };
    }

    public static void setPose(ArmorStand s, Mode part, Rotations r) {
        switch (part) {
            case HEAD -> s.setHeadPose(r);
            case BODY -> s.setBodyPose(r);
            case LEFT_ARM -> s.setLeftArmPose(r);
            case RIGHT_ARM -> s.setRightArmPose(r);
            case LEFT_LEG -> s.setLeftLegPose(r);
            case RIGHT_LEG -> s.setRightLegPose(r);
            default -> throw new IllegalArgumentException(part.name());
        }
    }

    /** Adds degrees on one axis. Angles are kept between -180 and 180. */
    public static float turn(ArmorStand s, Mode part, Axis axis, float degrees) {
        Rotations r = pose(s, part);
        float x = r.getX(), y = r.getY(), z = r.getZ();
        switch (axis) {
            case X -> x = wrap(x + degrees);
            case Y -> y = wrap(y + degrees);
            case Z -> z = wrap(z + degrees);
        }
        setPose(s, part, new Rotations(x, y, z));
        return switch (axis) {
            case X -> x;
            case Y -> y;
            case Z -> z;
        };
    }

    public static float wrap(float a) {
        float w = a % 360f;
        if (w >= 180f) w -= 360f;
        if (w < -180f) w += 360f;
        return Math.round(w * 100f) / 100f;
    }

    public static void resetPose(ArmorStand s) {
        Pose.DEFAULT.apply(s);
    }

    /** Swaps left and right, and flips the sideways angles, like looking in a mirror. */
    public static void mirror(ArmorStand s) {
        Rotations head = s.getHeadPose(), body = s.getBodyPose();
        Rotations la = s.getLeftArmPose(), ra = s.getRightArmPose(), ll = s.getLeftLegPose(), rl = s.getRightLegPose();
        s.setHeadPose(flip(head));
        s.setBodyPose(flip(body));
        s.setLeftArmPose(flip(ra));
        s.setRightArmPose(flip(la));
        s.setLeftLegPose(flip(rl));
        s.setRightLegPose(flip(ll));
    }

    private static Rotations flip(Rotations r) {
        return new Rotations(r.getX(), -r.getY(), -r.getZ());
    }

    // ---------- flags ----------

    public static void setSmall(ArmorStand s, boolean small) {
        // ArmorStand.setSmall is private; the flag lives in the synced client flags (bit 1).
        SynchedEntityData data = s.getEntityData();
        byte flags = data.get(ArmorStand.DATA_CLIENT_FLAGS);
        data.set(ArmorStand.DATA_CLIENT_FLAGS, (byte) (small ? flags | 1 : flags & ~1));
    }

    public static int disabledSlots(ArmorStand s) {
        return ((ArmorStandAccessor) s).codexArmorStands$getDisabledSlots();
    }

    public static boolean locked(ArmorStand s) {
        return (disabledSlots(s) & ALL_SLOTS_LOCKED) == ALL_SLOTS_LOCKED;
    }

    public static void setLocked(ArmorStand s, boolean locked) {
        ((ArmorStandAccessor) s).codexArmorStands$setDisabledSlots(locked ? ALL_SLOTS_LOCKED : 0);
    }

    // ---------- scale ----------

    public static double scale(ArmorStand s) {
        AttributeInstance a = s.getAttribute(Attributes.SCALE);
        return a == null ? 1.0 : a.getBaseValue();
    }

    public static void setScale(ArmorStand s, double v) {
        AttributeInstance a = s.getAttribute(Attributes.SCALE);
        if (a != null) a.setBaseValue(Math.round(v * 1000.0) / 1000.0);
    }

    // ---------- copy / paste ----------

    /** Pose and style (not items, name or position). */
    public static CompoundTag copy(ArmorStand s) {
        CompoundTag t = Pose.of(s).save();
        t.putBoolean("ShowArms", s.isShowArms());
        t.putBoolean("Small", s.isSmall());
        t.putBoolean("NoBasePlate", s.isNoBasePlate());
        t.putBoolean("Invisible", s.isInvisible());
        t.putBoolean("NoGravity", s.isNoGravity());
        t.putBoolean("NameVisible", s.isCustomNameVisible());
        t.putDouble("Scale", scale(s));
        return t;
    }

    /**
     * Applies a copy. Style flags the player isn't allowed to change are left alone,
     * so a paste can't do more than the player could do by hand.
     */
    public static void paste(ArmorStand s, CompoundTag t, java.util.function.Predicate<Mode> allowed) {
        if (allowed.test(Mode.HEAD)) Pose.load(t).apply(s);
        if (allowed.test(Mode.SHOW_ARMS)) s.setShowArms(t.getBoolean("ShowArms"));
        if (allowed.test(Mode.SMALL)) setSmall(s, t.getBoolean("Small"));
        if (allowed.test(Mode.BASE_PLATE)) s.setNoBasePlate(t.getBoolean("NoBasePlate"));
        if (allowed.test(Mode.INVISIBLE)) s.setInvisible(t.getBoolean("Invisible"));
        if (allowed.test(Mode.GRAVITY)) s.setNoGravity(t.getBoolean("NoGravity"));
        if (allowed.test(Mode.NAME_VISIBLE)) s.setCustomNameVisible(t.getBoolean("NameVisible"));
        if (allowed.test(Mode.SCALE) && t.contains("Scale")) setScale(s, t.getDouble("Scale"));
    }

    // ---------- pose snapshots ----------

    /** A full pose: six parts, three angles each. */
    public record Pose(Rotations head, Rotations body, Rotations leftArm, Rotations rightArm, Rotations leftLeg, Rotations rightLeg) {

        private static final Rotations ZERO = new Rotations(0, 0, 0);

        /** Vanilla's default pose. */
        public static final Pose DEFAULT = new Pose(ZERO, ZERO, new Rotations(-10, 0, -10), new Rotations(-15, 0, 10),
                new Rotations(-1, 0, -1), new Rotations(1, 0, 1));

        public static Pose of(ArmorStand s) {
            return new Pose(s.getHeadPose(), s.getBodyPose(), s.getLeftArmPose(), s.getRightArmPose(), s.getLeftLegPose(), s.getRightLegPose());
        }

        public void apply(ArmorStand s) {
            s.setHeadPose(head);
            s.setBodyPose(body);
            s.setLeftArmPose(leftArm);
            s.setRightArmPose(rightArm);
            s.setLeftLegPose(leftLeg);
            s.setRightLegPose(rightLeg);
        }

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.put("Head", head.save());
            t.put("Body", body.save());
            t.put("LeftArm", leftArm.save());
            t.put("RightArm", rightArm.save());
            t.put("LeftLeg", leftLeg.save());
            t.put("RightLeg", rightLeg.save());
            return t;
        }

        public static Pose load(CompoundTag t) {
            return new Pose(rot(t, "Head", DEFAULT.head), rot(t, "Body", DEFAULT.body), rot(t, "LeftArm", DEFAULT.leftArm),
                    rot(t, "RightArm", DEFAULT.rightArm), rot(t, "LeftLeg", DEFAULT.leftLeg), rot(t, "RightLeg", DEFAULT.rightLeg));
        }

        private static Rotations rot(CompoundTag t, String key, Rotations def) {
            ListTag l = t.getList(key, FloatTag.TAG_FLOAT);
            return l.size() == 3 ? new Rotations(l) : def;
        }
    }
}
