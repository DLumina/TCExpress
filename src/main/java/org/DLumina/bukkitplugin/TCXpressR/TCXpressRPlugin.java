package org.DLumina.bukkitplugin.TCXpressR;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rail;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
//import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TCXpressRPlugin extends JavaPlugin implements Listener {
    private double MAX_SPEED = 2;//max speed per tick
    private int BUFFER_LENGTH = 5;//the final distance on which cart keep 8m/s
    private int ADJUST_LENGTH = 10;//the adjust distance on which cart decelerate from max speed to 8m/s
    private final static double NORMAL_SPEED = 0.4;
    
    private boolean is_extra_boost_enabled = false;//if an extra boost is enabled, to accelerate all carts to max speed
    private final Set<String> blacklistworlds = new HashSet<String>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        List<String> list = getConfig().getStringList("blacklist_worlds");
        if (list != null) {
            for (String w : list) {
            	blacklistworlds.add(w.toLowerCase());
            }
        }
        MAX_SPEED = getConfig().getDouble("max_speed");
        MAX_SPEED = MAX_SPEED / 20;
        BUFFER_LENGTH = getConfig().getInt("buffer_length");
        ADJUST_LENGTH = getConfig().getInt("adjust_length");
        is_extra_boost_enabled = getConfig().getBoolean("is_extra_boost_enabled");
    }

    @Override
    public void onDisable() {
    }

    /*
    @EventHandler
    void onExit(VehicleExitEvent e) {
        if (!(e.getVehicle() instanceof Minecart)) return;
        Minecart minecart = (Minecart) e.getVehicle();
        Entity t = Iterables.getFirst(minecart.getPassengers(), null);
        if (t == null || t.getType() != EntityType.PLAYER) return;
        minecart.setMaxSpeed(NORMAL_SPEED);
    }
    */

    @EventHandler
    void onMove(VehicleMoveEvent e) {
        if (!(e.getVehicle() instanceof Minecart)) return;
        if (blacklistworlds.contains(e.getVehicle().getWorld().getName().toLowerCase())) return;
        
        if(e.getVehicle().getCustomName()==null) return;//only for renamed carts
        Minecart minecart = ((Minecart) e.getVehicle());
        /*
        Entity passenger = Iterables.getFirst(minecart.getPassengers(), null);
        if (passenger == null || passenger.getType() != EntityType.PLAYER) return;
        */

        Block curBlock = minecart.getLocation().getBlock();
        if (!isRail(curBlock)) {
            minecart.setMaxSpeed(NORMAL_SPEED);
            return;
        }

        Rail.Shape curRailType = ((Rail)(curBlock.getBlockData())).getShape();
        if (curRailType != Rail.Shape.EAST_WEST && curRailType != Rail.Shape.NORTH_SOUTH) {
            minecart.setMaxSpeed(NORMAL_SPEED);
            return;
        }

        Vector vector = e.getVehicle().getVelocity();
        if (vector.getY() != 0) {
            minecart.setMaxSpeed(NORMAL_SPEED);
            return;
        }
        double x = vector.getX();
        double z = vector.getZ();

        if (x == 0 && z == 0) {
            minecart.setMaxSpeed(NORMAL_SPEED);
            return;
        }
        
        /*
        double curSpeed;
        boolean isX;
        if(x != 0 && z == 0) {
        	isX = true;
        	curSpeed = x;
        }
        else {
        	isX = false;
        	curSpeed = z;
        }
        */
        boolean isX = x != 0 && z == 0;
        boolean n = isX ? x < 0 : z < 0;
        double curSpeed = isX ? (n?(-x):x) : (n?(-z):z);
        BlockFace direction = isX ? (n ? BlockFace.WEST : BlockFace.EAST) : (n ? BlockFace.NORTH : BlockFace.SOUTH);

        int flatLength = 0;
        while ((curBlock = nextRail(direction, curBlock)) != null && flatLength < BUFFER_LENGTH + ADJUST_LENGTH) {
        	curRailType = ((Rail)(curBlock.getBlockData())).getShape();
            if (isX) {
                if (curRailType != Rail.Shape.EAST_WEST) break;
            } else {
                if (curRailType != Rail.Shape.NORTH_SOUTH) break;
            }

            flatLength++;
        }

        if (flatLength < BUFFER_LENGTH) {
            minecart.setMaxSpeed(NORMAL_SPEED);
            return;
        }

        int freeLength = flatLength - BUFFER_LENGTH;

        double s = (double) freeLength / ADJUST_LENGTH;
        if (s > 1) s = 1;
        double speed = NORMAL_SPEED + (MAX_SPEED - NORMAL_SPEED) * s;
        minecart.setMaxSpeed(speed);
        
        if(is_extra_boost_enabled == false)
        	return;
        
        if ((curSpeed <= 8) || (curSpeed >= MAX_SPEED))
        	return;
        
        double extra_boost = curSpeed * (MAX_SPEED - curSpeed + 10) / MAX_SPEED / 20;//add extra acceleration
        if (extra_boost > 1)
        	extra_boost = 1;
        
        curSpeed += extra_boost;
        if (curSpeed > MAX_SPEED)
        	curSpeed = MAX_SPEED;
        if (isX == true) {
        	vector.setX(n ? (-curSpeed) : curSpeed);
        }
        else {
        	vector.setZ(n ? (-curSpeed) : curSpeed);
        }
        minecart.setVelocity(vector);
        
    }

    private static Block nextRail(BlockFace direction, Block block) {
        Block b = block.getRelative(direction);
        return isRail(b) ? b : null;
    }

    private static boolean isRail(Block block) {
        Material mat = block.getType();
        return mat == Material.RAIL || mat == Material.ACTIVATOR_RAIL || mat == Material.DETECTOR_RAIL || mat == Material.POWERED_RAIL;
    }
}