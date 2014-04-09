package com.pauldavdesign.mineauz.minigames.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.pauldavdesign.mineauz.minigames.Minigames;
import com.pauldavdesign.mineauz.minigames.PlayerData;
import com.pauldavdesign.mineauz.minigames.gametypes.MinigameTypeBase;
import com.pauldavdesign.mineauz.minigames.minigame.Minigame;
import com.pauldavdesign.mineauz.minigames.minigame.ScoreboardPlayer;

public class SQLCompletionSaver extends Thread{
	private Minigames plugin = Minigames.plugin;
	private PlayerData pdata = Minigames.plugin.pdata;
	
	public SQLCompletionSaver(){
		start();
	}
	
	public void run(){
		SQLDatabase sql = plugin.getSQL();
		
		while(plugin.hasSQLToStore()){
			List<SQLPlayer> players = plugin.getSQLToStore();
			plugin.clearSQLToStore();
			for(SQLPlayer player : players){
				addSQLData(player, sql);
			}
		}

		plugin.removeSQLCompletionSaver();
	}
	
	private void addSQLData(SQLPlayer player, SQLDatabase database){
		
		String table = "mgm_" + player.getMinigame() + "_comp";
		
		if(!database.isOpen())
		{
			if(!database.loadSQL())
			{
				plugin.getLogger().warning("Database Connection was closed and could not be re-established!");
				return;
			}
		}
		
		Connection sql = database.getSql();
		
		if(!database.isTable(table)){
			try {
				Statement createTable = sql.createStatement();
				createTable.execute("CREATE TABLE " + table + 
						"( " +
						"UUID varchar(32) NOT NULL PRIMARY KEY, " +
						"Player varchar(32), " +
						"Completion int, " +
						"Kills int, " +
						"Deaths int, " +
						"Score int, " +
						"Time long, " +
						"Reverts int, " +
						"TotalKills int, " +
						"TotalDeaths int, " +
						"TotalScore int, " +
						"TotalReverts int, " +
						"TotalTime long, " +
						"Failures int " +
						")");
				createTable.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		else{ //TODO: Remove after 1.6.0 release
			if(!database.columnExists("Score", table))
			{
				try {
					Statement alterTable = sql.createStatement();
					
					alterTable.execute("ALTER TABLE " + table + " ADD Score int DEFAULT -1, " +
							"ADD Time long NOT NULL, " +
							"ADD Reverts int DEFAULT -1, " +
							"ADD TotalKills int DEFAULT 0, " +
							"ADD TotalDeaths int DEFAULT 0, " +
							"ADD TotalScore int DEFAULT 0, " +
							"ADD TotalReverts int DEFAULT 0, " +
							"ADD TotalTime long NOT NULL, " + 
							"ADD Failures int DEFAULT 0");
					alterTable.close();
				} catch (SQLException e1) {
					e1.printStackTrace();
					return;
				}
			}
		}
		
		try
		{
			ResultSet set = null;
			Statement getStats = sql.createStatement();
			set = getStats.executeQuery(String.format("SELECT * FROM %s WHERE UUID='%s'", table, player.getUUID()));
			
			String name = player.getPlayerName();
			String uuid = player.getUUID();
			int kills = player.getKills();
			int deaths = player.getDeaths();
			int score = player.getScore();
			int reverts = player.getReverts();
			long time = player.getTime();
			int completedNum = player.getCompletionChange();
			int failureNum = player.getFailureChange();
			boolean completed = false;
			if(player.getCompletionChange() >= 1){
				completed = true;
			}
	
			int ocompleted = 0;
			int okills = 0;
			int odeaths = -1;
			int oscore = 0;
			int oreverts = -1;
			long otime = 0;
			int otkills = 0;
			int otdeaths = 0;
			int otscore = 0;
			int otreverts = 0;
			long ottime = 0;
			int ofailures = 0;
			try {
				set.absolute(1);
				ocompleted = set.getInt(3);
				
				okills = set.getInt(4);
				odeaths = set.getInt(5);
				oscore = set.getInt(6);
				otime = set.getLong(7);
				oreverts = set.getInt(8);
				otkills = set.getInt(9);
				otdeaths = set.getInt(10);
				otscore = set.getInt(11);
				otreverts = set.getInt(12);
				ottime = set.getLong(13);
				ofailures = set.getInt(14);
			} catch (SQLException e) {
				//e.printStackTrace();
			}
			
			set.close();
			getStats.close();
			
			otkills += kills;
			otdeaths += deaths;
			otscore += score;
			otreverts += reverts;
			ottime += time;
			
			if(completed){
				ocompleted += completedNum;
				
				if(odeaths < deaths && odeaths != -1){
					deaths = odeaths;
				}
				
				if(oreverts < reverts && oreverts != -1){
					reverts = oreverts;
				}
				
				if(otime < time && otime != 0){
					time = otime;
				}
			}
			else{
				ofailures += failureNum;
				if(odeaths != -1)
					deaths = odeaths;
				else
					deaths = -1;
				if(oreverts != -1)
					reverts = oreverts;
				else
					reverts = -1;
				if(otime != 0)
					time = otime;
				else
					time = 0;
			}
			
			if(okills > kills){
				kills = okills;
			}
			
			if(oscore > score){
				score = oscore;
			}
			
			boolean hasAlreadyCompleted = false;
			
			if(name != null){
				if(ocompleted - 1 >= 1)
					hasAlreadyCompleted = true;
				Statement updateStats = sql.createStatement();
				updateStats.executeUpdate("UPDATE " + table + " SET Completion='" + ocompleted + "', " +
						"Player='" + name + "', " + 
						"Kills=" + kills + ", " +
						"Deaths=" + deaths + ", " +
						"Score=" + score + ", " +
						"Time=" + time + ", " +
						"Reverts=" + reverts + ", " +
						"TotalKills=" + otkills + ", " +
						"TotalDeaths=" + otdeaths + ", " +
						"TotalScore=" + otscore + ", " +
						"TotalReverts=" + otreverts + ", " +
						"TotalTime=" + ottime + ", " +
						"Failures=" + ofailures +
						" WHERE UUID='" + uuid + "'");
				updateStats.close();
			}
			else{
				name = player.getPlayerName();
				Statement insertStats = sql.createStatement();
				insertStats.executeUpdate("INSERT INTO " + table + " VALUES " +
						"( '" + uuid + "', '" + 
						name + "', " +
						ocompleted + ", " + 
						kills + ", " + 
						deaths + ", " +
						score + ", " +
						time + ", " +
						reverts + ", " +
						otkills + ", " +
						otdeaths + ", " +
						otscore + ", " +
						otreverts + ", " +
						ottime + ", " +
						ofailures +
						" )");
				insertStats.close();
			}
		
			Minigame mgm = Minigames.plugin.mdata.getMinigame(player.getMinigame());
			if(mgm.getScoreboardData().hasPlayer(uuid)){
				ScoreboardPlayer ply = mgm.getScoreboardData().getPlayer(uuid);
				ply.setCompletions(ocompleted);
				ply.setBestKills(kills);
				ply.setLeastDeaths(deaths);
				ply.setBestScore(score);
				ply.setBestTime(time);
				ply.setLeastReverts(reverts);
				ply.setTotalKills(otkills);
				ply.setTotalDeaths(otdeaths);
				ply.setTotalScore(otscore);
				ply.setTotalReverts(otreverts);
				ply.setTotalTime(ottime);
				ply.setFailures(ofailures);
			}
			else{
				mgm.getScoreboardData().addPlayer(new 
						ScoreboardPlayer(player.getPlayerName(), uuid, ocompleted, ofailures, kills, deaths, 
								score, time, reverts, otkills, otdeaths, otscore, otreverts, ottime));
			}
			mgm.getScoreboardData().updateDisplays();
			
			if(completed)
				MinigameTypeBase.issuePlayerRewards(pdata.getMinigamePlayer(player.getPlayerName()), mgm, hasAlreadyCompleted);
			
		}
		catch(SQLException e){
			e.printStackTrace();
		}
	}
}
