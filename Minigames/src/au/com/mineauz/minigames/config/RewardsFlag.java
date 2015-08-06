package au.com.mineauz.minigames.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.material.MaterialData;

import au.com.mineauz.minigames.menu.MenuItem;
import au.com.mineauz.minigames.menu.MenuItemDisplayRewards;
import au.com.mineauz.minigames.minigame.reward.Rewards;

public class RewardsFlag extends Flag<Rewards>{
	
	public RewardsFlag(Rewards value, String name){
		setFlag(value);
		setDefaultFlag(value);
		setName(name);
	}

	@Override
	public void saveValue(String path, FileConfiguration config) {
		if (!getFlag().isEmpty()) {
			getFlag().save(config.createSection(path + "." + getName()));
		}
	}

	@Override
	public void loadValue(String path, FileConfiguration config) {
		getFlag().load(config.getConfigurationSection(path + "." + getName()));
	}

	@Override
	public MenuItem getMenuItem(String name, String description, Material displayItem) {
		return new MenuItemDisplayRewards(name, description, displayItem, getFlag());
	}
	
	@Override
	public MenuItem getMenuItem(String name, String description, MaterialData displayItem) {
		return new MenuItemDisplayRewards(name, description, displayItem, getFlag());
	}

}
