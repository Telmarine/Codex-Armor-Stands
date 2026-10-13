package com.tac5studios.codexarmorstands.edit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Finds armor stands near a player. Marker stands count too, since they can't be clicked. */
@SuppressWarnings("resource") // levels are owned by the server, not closed here
public final class Targeting {

    private Targeting() {}

    /** The closest stand within range, or null. */
    public static ArmorStand nearest(ServerPlayer p, double range) {
        ArmorStand best = null;
        double bestDist = range * range;
        for (ArmorStand s : p.serverLevel().getEntitiesOfClass(ArmorStand.class, p.getBoundingBox().inflate(range), ArmorStand::isAlive)) {
            double d = s.distanceToSqr(p);
            if (d <= bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    /** The stand the player is looking at within range, or null. */
    public static ArmorStand lookedAt(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1f).scale(range));
        ArmorStand best = null;
        double bestDist = Double.MAX_VALUE;
        for (ArmorStand s : p.serverLevel().getEntitiesOfClass(ArmorStand.class, p.getBoundingBox().inflate(range), ArmorStand::isAlive)) {
            AABB box = s.getBoundingBox().inflate(0.15);
            Optional<Vec3> hit = box.clip(eye, end);
            if (box.contains(eye)) hit = Optional.of(eye);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = s;
                }
            }
        }
        return best;
    }
}
