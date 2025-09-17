package com.ruzgar.kodeLibs;

import org.bukkit.plugin.java.JavaPlugin;

public final class KodeLibs extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getCommand("redcore").setExecutor(new RedCoreCommand(this));

        getServer().getPluginManager().registerEvents(new CrystalAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new NecromancyAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new DarkStarListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageReductionListener(this), this);
    }

    @Override
    public void onDisable() {
    }
}
