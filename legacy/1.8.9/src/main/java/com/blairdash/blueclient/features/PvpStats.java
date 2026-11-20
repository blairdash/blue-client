package com.blairdash.blueclient.features;

import com.blairdash.blueclient.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Combo counter, reach display and the hitbox toggle. */
public class PvpStats {
    private static final long COMBO_TIMEOUT_MS = 3000;
    private static final long REACH_SHOW_MS = 4000;

    public static int combo = 0;
    private static long lastHit = 0;
    private static double lastReach = -1;
    private static long reachTime = 0;
    private static boolean prevHurt = false;
    private static boolean hitboxApplied = false;

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.entityPlayer != mc.thePlayer || !(event.target instanceof EntityLivingBase)) return;
        EntityLivingBase target = (EntityLivingBase) event.target;
        // A target still in its hurt animation can't take another hit, so don't count it.
        if (target.hurtTime > 0) return;
        long now = System.currentTimeMillis();
        combo++;
        lastHit = now;
        lastReach = reach(event.entityPlayer, target);
        reachTime = now;
    }

    private static double reach(EntityPlayer p, EntityLivingBase target) {
        Vec3 eye = p.getPositionEyes(1.0F);
        AxisAlignedBB b = target.getEntityBoundingBox();
        double x = MathHelper.clamp_double(eye.xCoord, b.minX, b.maxX);
        double y = MathHelper.clamp_double(eye.yCoord, b.minY, b.maxY);
        double z = MathHelper.clamp_double(eye.zCoord, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3(x, y, z));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            boolean hurt = mc.thePlayer.hurtTime > 0;
            if (hurt && !prevHurt) combo = 0;
            prevHurt = hurt;
            if (combo > 0 && System.currentTimeMillis() - lastHit > COMBO_TIMEOUT_MS) combo = 0;
        }
        if (ModConfig.hitboxesEnabled != hitboxApplied && mc.getRenderManager() != null) {
            mc.getRenderManager().setDebugBoundingBox(ModConfig.hitboxesEnabled);
            hitboxApplied = ModConfig.hitboxesEnabled;
        }
    }

    public static String comboText() {
        return "Combo: " + combo;
    }

    public static String reachText() {
        if (lastReach < 0 || System.currentTimeMillis() - reachTime > REACH_SHOW_MS) return "Reach: -";
        return String.format("Reach: %.2f", lastReach);
    }
}
