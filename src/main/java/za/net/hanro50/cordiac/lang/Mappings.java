package za.net.hanro50.cordiac.lang;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class Mappings {
    public static String DMTranslate(DamageCause DM) {
        switch (DM) {
            case BLOCK_EXPLOSION: // BLOCK_EXPLOSION("death.attack.explosion"),
                return "death.attack.explosion";
            case CONTACT: // CONTACT("death.attack.generic.player"),
                return "death.attack.cactus";
            case CRAMMING: // CRAMMING("death.attack.cramming"),
                return "death.attack.cramming";
            case CUSTOM: // CUSTOM("%1$s died"),
                return "death.attack.generic";
            case DRAGON_BREATH: // DRAGON_BREATH("death.attack.dragonBreath"),
                return "death.attack.dragonBreath";
            case DROWNING: // DROWNING("death.attack.drown"),
                return "death.attack.drown";
            case DRYOUT:
                return "death.attack.drown";
            case ENTITY_ATTACK: // ENTITY_ATTACK("death.attack.mob"),
                return "death.attack.mob";
            case ENTITY_EXPLOSION: // ENTITY_EXPLOSION("death.attack.explosion.player"),
                return "death.attack.explosion";
            case ENTITY_SWEEP_ATTACK:
                return "death.attack.player";
            case FALL: // FALL("death.attack.fall"),
                return "death.attack.fall";
            case FALLING_BLOCK: // FALLING_BLOCK("death.attack.fallingBlock"),
                return "death.attack.fallingBlock"; // "death.attack.anvil"
            case FIRE: // FIRE("death.attack.inFire"),
                return "death.attack.inFire";
            case FIRE_TICK: // FIRE_TICK("death.attack.onFire"),
                return "death.attack.onFire";
            case FLY_INTO_WALL: // FLY_INTO_WALL("death.attack.flyIntoWall"),
                return "death.attack.flyIntoWall";
            case HOT_FLOOR: // HOT_FLOOR("death.attack.hotFloor"),
                return "death.attack.hotFloor";
            case LAVA: // LAVA("death.attack.lava"),
                return "death.attack.lava";
            case LIGHTNING: // LIGHTNING("death.attack.lightningBolt"),
                return "death.attack.lightningBolt";
            case MAGIC: // MAGIC("death.attack.magic"),
                return "death.attack.magic";
            case MELTING: // MELTING("death.attack.generic"),
                return "death.attack.generic";
            case POISON: // POISON("death.attack.magic"),
                return "death.attack.magic";
            case PROJECTILE: // PROJECTILE("death.attack.arrow"),
                return "death.attack.arrow";
            case STARVATION: // STARVATION("death.attack.starve"),
                return "death.attack.starve";
            case SUFFOCATION: // SUFFOCATION("death.attack.inWall"),
                return "death.attack.inWall";
            case SUICIDE: // SUICIDE("death.attack.even_more_magic"),
                return "death.attack.even_more_magic";
            case THORNS: // THORNS("death.attack.thorns"),
                return "death.attack.thorns";
            case VOID: // VOID("death.attack.outOfWorld"),
                return "death.attack.outOfWorld";
            case WITHER: // WITHER("death.attack.wither"),
                return "death.attack.wither";
            case FREEZE:
                return "death.attack.freeze";
            case SONIC_BOOM:
                return "death.attack.sonic_boom";
            default:
                return "death.attack.generic";

        }
    }

}
