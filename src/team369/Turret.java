package team369;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;

public class Turret extends Robot {
	public Turret(RobotController rc){
		super(rc);
	}
	MapLocation goalLoc = null;
	int goalRadius = 1000;
	int followingID = -1;
	final int dormancyPeriodMax = 20;
	int dormancyPeriodCount = -1;
	final int patienceTTMStart = 30;
	int patienceTTMCount = 0;
	public void loop() throws Exception{
		sa.fetch();
		
        // If this robot type can attack, check for enemies within range and attack one
		if(rc.getType() == RobotType.TTM){
			//Broadcast for others to follow
			rc.broadcastSignal(40);
			patienceTTMCount--;
			
			if(patienceTTMCount < 0 //got impatient
					|| patienceTTMCount < 20 && (hostileCount() > 2 || friendlyCount() < 6)){ //oh something's happening
				rc.unpack();
				dormancyPeriodCount = dormancyPeriodMax;
				return;
			}
			
			//if there's a specific goal location
			if(goalLoc != null){
				rc.setIndicatorDot(goalLoc, 255, 0, 0);
				
				goalRadius = Math.max(goalRadius, 4);
				if(rc.getLocation().distanceSquaredTo(goalLoc) <= goalRadius){ //we've reached it!
					rc.unpack();
					dormancyPeriodCount = dormancyPeriodMax;
					goalLoc = null;
					return;
				}
				else if(rc.isCoreReady()){ //still gotta move toward the goal
					squishMove(rc.getLocation().directionTo(goalLoc));
					return;
				}
			}
			else if(followingID != -1){
				//if we're following a robot
				if(rc.canSenseRobot(followingID)){
					RobotInfo r = rc.senseRobot(followingID);
					rc.setIndicatorDot(r.location, 0, 255, 0);
					
					if(r.type == RobotType.TTM && rc.isCoreReady()){ //if the target is still moving, move to it
						squishMove(rc.getLocation().directionTo(r.location));
						return;
					}
					else if(r.type == RobotType.TURRET){ //otherwise let's get close to it
						goalLoc = r.location;
						goalRadius = 4;
						followingID = -1;
						return;
					}
				}
				else{//if we can't see them anymore, unpack and stop
					followingID = -1;
					rc.unpack();
				}
			}
			else{ //something weird happened and we are a TTM with no goals.
				rc.unpack();
				dormancyPeriodCount = dormancyPeriodMax;
				return;
			}
		}
		else{
	        if (rc.isWeaponReady()) {
	            MapLocation enemyLoc = getBestAttackableHostileLocation(enemyTeam);
	            if (enemyLoc != null){
	            	rc.attackLocation(enemyLoc);
	            	return;
	            }
	            
	            enemyLoc = getBestAttackableHostileLocation(Team.ZOMBIE);
	            if (enemyLoc != null) {
	            	rc.attackLocation(enemyLoc);
	    			rc.broadcastSignal(40);
	                return;
	            }
	        }

			dormancyPeriodCount--;
	        if(hostileCount() == 0 && dormancyPeriodCount < 0){ // nothing to do
				//Gets handed a specific goal location
				for(Signal s : sa.allyMessageSignals)
					if(SignalAdapter.isCommand(s.getMessage(),SignalAdapter.Cmd.ZOMBIE_HOME)){
						goalLoc = SignalAdapter.getLocation(s.getMessage());
						if(rc.getLocation().distanceSquaredTo(goalLoc) > RobotType.TURRET.sensorRadiusSquared){
							goalRadius = RobotType.TURRET.sensorRadiusSquared;
							patienceTTMCount = patienceTTMStart;
							rc.pack();
							return;
						}
					}
				
				//Only given a robot to follow
	        	for(Signal s : sa.allyBasicSignals){
	        		followingID = s.getRobotID();
					if(rc.canSenseRobot(followingID)){
						RobotInfo r = rc.senseRobot(followingID);
						if(rc.getLocation().distanceSquaredTo(r.location) > 10){
							if(r.type == RobotType.TTM || r.type == RobotType.TURRET){
								patienceTTMCount = patienceTTMStart;
								rc.pack();
								return;
							}
							else if(rc.getLocation().distanceSquaredTo(r.location) > 15){
								goalLoc = r.location;
								goalRadius = 10;
								followingID = -1;
								patienceTTMCount = patienceTTMStart;
								rc.pack();
								return;
							}
						}
					}
	        	}
	        }
		}
	}
	public MapLocation getBestAttackableHostileLocation(Team team){
		RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,team);
		if(visibleEnemies.length > 0){
			double minHealth = Double.MAX_VALUE;
			int minIndex = -1;
			for(int i = 0; i < visibleEnemies.length; i++){
				if(visibleEnemies[i].health < minHealth && rc.canAttackLocation(visibleEnemies[i].location)){
					minHealth = visibleEnemies[i].health;
					minIndex = i;
				}
			}
			if(minIndex != -1)
				return visibleEnemies[minIndex].location;
		}
		
		if(team == enemyTeam){
			for(Signal s : sa.enemyMessageSignals)
				if(rc.canAttackLocation(s.getLocation()))
					return s.getLocation();
			
			for(Signal s : sa.enemyBasicSignals)
				if(rc.canAttackLocation(s.getLocation()))
					return s.getLocation();
		}
		
		for(Signal s : sa.allyMessageSignals){
			int[] msg = s.getMessage();
			if(team == enemyTeam && SignalAdapter.isCommand(msg,SignalAdapter.Cmd.ENEMY_POSITION) 
					|| team == Team.ZOMBIE && SignalAdapter.isCommand(msg,SignalAdapter.Cmd.ZOMBIE_POSITION)){
				MapLocation decodedLoc = SignalAdapter.getLocation(msg);
				if(rc.canAttackLocation(decodedLoc))
					return decodedLoc;
			}
		}
		
		return null;
	}
}
