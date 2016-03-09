package team369;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Soldier extends Robot{
	public Soldier(RobotController rc) throws Exception{
		super(rc);
	}
	MapLocation goalLoc;
	int commanderID = -1;
	public void loop() throws Exception{
		sa.fetch();
		
		if(rc.getRoundNum() % 100 == 0){
			commanderID = -1;
			for(Signal s : sa.allyMessageSignals)
				if(SignalAdapter.isCommand(s.getMessage(), SignalAdapter.Cmd.COMMANDER_VOTE))
					commanderID = s.getRobotID();
		}
		
        RobotInfo[] enemiesWithinRange = senseEnemyRobotsCached();
        if(enemiesWithinRange.length == 0)
        	enemiesWithinRange = senseZombieRobotsCached();
        
        
		double aggression = rc.getHealth()/rc.getType().maxHealth; //also account for infection
		rc.setIndicatorString(1,"areZombiesSpawning: "+areZombiesSpawning()+" Current: "+rc.getRoundNum());
        RobotInfo nearestEnemy = nearest(rc.getLocation(), enemiesWithinRange);
        
        if(nearestEnemy != null){
			if(hostileCount() == 1 && ((nearestEnemy.type == RobotType.ZOMBIEDEN && friendlyCount() > 7) || nearestEnemy.type == RobotType.FASTZOMBIE))
				aggression = 100;
			else if(areZombiesSpawning() || nearestEnemy.type == RobotType.BIGZOMBIE) //nearby (in terms of round#) a zombie spawn, run away from zombies
				aggression = -1;
			else
				aggression = Math.max(aggression, 0);
        }
		rc.setIndicatorString(0, "Aggression: "+aggression);
        
        if (enemiesWithinRange.length > 0){
    		int distToNearestEnemy = rc.getLocation().distanceSquaredTo(nearestEnemy.location);
        	if(rc.isWeaponReady() && rc.canAttackLocation(nearestEnemy.location) && (rc.getType() == RobotType.GUARD || distToNearestEnemy > 3)){
        		//todo - prioritize by low health
        		rc.attackLocation(nearestEnemy.location);
    			rc.broadcastSignal(60);
        		return;
        	}
        	else if(rc.isCoreReady()){ //kiting!
        		Direction dir = null;
        		
        		if(rc.senseRubble(rc.getLocation()) > 0)
        			aggression -= 1;
        		
        		if(hostileCount() > 1 && hostileHealthTotal() > friendlyHealthTotal()) //overwhelming power, run away
        			dir = hostileDirection().opposite();
        		else if(distToNearestEnemy > myAttackRange - 4 - aggression * 2 && distToNearestEnemy > 2) //too far, be aggressive
        			dir = rc.getLocation().directionTo(nearestEnemy.location);
        		else{ //so it'll circle around and kinda surround the enemy?
        			if(rand.nextBoolean()) //or you can just wait for the weapon cooldown
        				return;
        			else if(rand.nextBoolean())
        				dir = rc.getLocation().directionTo(nearestEnemy.location).rotateLeft().rotateLeft();
        			else
        				dir = rc.getLocation().directionTo(nearestEnemy.location).rotateRight().rotateRight();
        		}
        		
        		if(dir != null){
        			squishMove(dir);
        			return;
        		}
        	}
        }
        
        if (rc.isCoreReady()) {
            // Choose a direction to try to move in
        		//todo - how would I bait a group of zombies to move with me toward the enemy team?
        	Direction dir = null;
        	
        	if(friendlyCount() == 0 && hostileCount() > 5) //bait toward enemies if alone
        		dir = rc.getLocation().directionTo(enemyHomeAreaLocation);
	    	if(friendlyCount() < 15 && rc.getRoundNum() < 500 || friendlyCount() < 15 - 10 * aggression) //too few friends, come back
	    		dir = friendlyDirection();
	    	
			if(dir == null){
	        	for(Signal s : sa.allyMessageSignals){
	        		int[] msg = s.getMessage();
	        		if(SignalAdapter.isCommand(msg,SignalAdapter.Cmd.ZOMBIE_HOME)){
	        			goalLoc = SignalAdapter.getLocation(msg);
			        	if(rc.getLocation().distanceSquaredTo(goalLoc) >= 10)
			        		break;
			        	else
			        		goalLoc = null;
	        		}
	        		else if(SignalAdapter.isCommand(msg,SignalAdapter.Cmd.MOVE_HERE)){
	        			goalLoc = SignalAdapter.getLocation(msg);
			        	if(rc.getLocation().distanceSquaredTo(goalLoc) >= 10)
			        		break;
			        	else
			        		goalLoc = null;
	        		}
	        	}
			}
        	if(dir == null && goalLoc == null)
        		for(Signal s : sa.allyBasicSignals)
		        	if(rc.getLocation().distanceSquaredTo(s.getLocation()) >= 10){
		        		goalLoc = s.getLocation();
		        		break;
		        	}
        	
        	
        	if(dir == null && goalLoc != null){
        		if(rc.getLocation().distanceSquaredTo(goalLoc) < 10)
        			goalLoc = null;
        		else
        			dir = rc.getLocation().directionTo(goalLoc);
        	}
        	
        	//run to archon for health benefits
        	if(dir == null && rc.getHealth() < rc.getType().maxHealth / 4 && commanderID != -1 && rc.canSenseRobot(commanderID))
        		dir = rc.getLocation().directionTo(rc.senseRobot(commanderID).location);
    		
        	if((dir == null || dir == Direction.NONE) && rand.nextBoolean()){
    			dir = directions[rand.nextInt(8)];
	        	if(rand.nextBoolean() || !clearIfRubble(dir)) //only try to clear the rubble half the time
	        		squishMove(dir);
    		}
        	else if(!(dir == null || dir == Direction.NONE))
        		squishMove(dir);
        	
        	return;
        }
	}
}
