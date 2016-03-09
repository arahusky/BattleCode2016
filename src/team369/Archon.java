package team369;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Archon extends Robot {
	int openingBuildIndex = 0;
	boolean isMasterArchon = false;
	RobotType[] masterOpeningBuild = {RobotType.TURRET, RobotType.TURRET, RobotType.TURRET, RobotType.SCOUT, RobotType.VIPER, RobotType.VIPER, RobotType.VIPER, RobotType.VIPER, RobotType.VIPER, RobotType.SOLDIER, RobotType.SOLDIER};
	RobotType[] subordOpeningBuild = {RobotType.GUARD, RobotType.VIPER, RobotType.GUARD, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.VIPER, RobotType.SOLDIER};
	RobotType[] robotTypesBefore500 = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, 
            RobotType.TURRET, RobotType.TURRET, RobotType.TURRET, RobotType.TURRET};
	RobotType[] robotTypesAfter500 = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.VIPER,
            RobotType.TURRET, RobotType.TURRET};
	
	public Archon(RobotController rc){
		super(rc);
		
		/*int minIndex = -1;
		int minSum = 10000000;
		for(int i = 0; i < initialAllyLocs.length; i++){
			int sum = 0;
			for(int j = 0; j < initialEnemyLocs.length; j++)
				sum += initialAllyLocs[i].distanceSquaredTo(initialEnemyLocs[j]);
			if(sum < minSum){
				minSum = sum;
				minIndex = i;
			}
		}
		if(initialAllyLocs[minIndex] == rc.getLocation())
			isMasterArchon = true;*/
		
		
		//if globally the master, go opening build
		//otherwise
	}
	public void loop() throws Exception{
		sa.fetch();
		if(rc.getRoundNum()%100 == 0)
			sa.sendCommanderVote();

		RobotInfo[] enemyRobots = senseEnemyRobotsCached();
		int messagesSent = 0;
		for(; messagesSent < GameConstants.MESSAGE_SIGNALS_PER_TURN && messagesSent < enemyRobots.length; messagesSent++)
			sa.broadcastEnemyPosition(enemyRobots[messagesSent].location, 50);
		if(messagesSent < GameConstants.MESSAGE_SIGNALS_PER_TURN){
			RobotInfo[] zombieRobots = senseZombieRobotsCached();
			for(int j = 0; messagesSent < GameConstants.MESSAGE_SIGNALS_PER_TURN && j < zombieRobots.length; messagesSent++, j++)
				sa.broadcastZombiePosition(zombieRobots[j].location, 50);
		}
		
		//Repair
        RobotInfo[] friendsWithinRange = rc.senseNearbyRobots(myAttackRange, myTeam);
        for(int i = 0; i < friendsWithinRange.length; i++)
        	if(friendsWithinRange[i].type!=RobotType.ARCHON && friendsWithinRange[i].health < friendsWithinRange[i].maxHealth){
        		rc.repair(friendsWithinRange[i].location);
        		break;
        	}
        
        //if(sa.isCommander()){
        //	sa.broadcastMoveHere(rc.getLocation(), 50);
        //}
        
        if (rc.isCoreReady()) {
        	RobotInfo[] hostiles = rc.senseHostileRobots(rc.getLocation(), 40);
        	if(rc.getHealth() < 300 && friendlyCount() < hostileCount() && rc.getInfectedTurns() > 3){
        		//If it's hopeless, run toward the enemy
        		squishMove(rc.getLocation().directionTo(enemyHomeAreaLocation));
        		return;
        	}
        		
        		
        	if(hostiles.length > 0 && !(hostiles.length == 1 && hostiles[0].type == RobotType.ZOMBIEDEN)){
        		Direction friendlyDirection = friendlyDirection();
        		if(friendlyDirection == Direction.NONE)
        			friendlyDirection = nearest(rc.getLocation(),hostiles).location.directionTo(rc.getLocation());
    			squishMove(friendlyDirection);
    			return;
        	}
        	
        	int moveable = 0;
        	for(Direction d : directions)
        		if(rc.canMove(d))
        			moveable++;
        	
        	RobotInfo[] neutralWithinRange = rc.senseNearbyRobots(2 /*adjacent squares*/,Team.NEUTRAL);
        	if(neutralWithinRange.length > 0){
        		rc.activate(neutralWithinRange[0].location);
        		return;
        	}
        	else if ((rc.getRoundNum() <= 500 && rand.nextBoolean()) || rc.getRoundNum() > 500 && (moveable <= 2 || rand.nextInt(10) < (rc.getTeamParts()>300?6:9))) { //if over 300 parts, build more!
                // Choose a random direction to try to move in, or look for parts
            	MapLocation[] parts = rc.sensePartLocations(10);
            	RobotInfo[] neutrals = rc.senseNearbyRobots(10,Team.NEUTRAL);
            	MapLocation toMove = null;
            	
            	////////////////todo - make this more effective
            	if(rc.getRoundNum() < zss.getRounds()[0]){//move only toward other archons until first zomb spawn
        			squishMove(rc.getLocation().directionTo(allyHomeAreaLocation));
        			return;
            	}
            	else{
            		
	            	if(hostileCount() == 1 && senseHostileRobotsCached()[0].type == RobotType.ZOMBIEDEN)
	        			sa.broadcastZombieHome(senseHostileRobotsCached()[0].location, 100);
	            	if(!rc.isCoreReady())return;
	            	
	            	Direction dirToMove;
	            	if(friendlyCount() < 10 || rc.getRoundNum() < 500)
	            		dirToMove = friendlyDirection();
	            	else{
		            	if(neutrals.length > 0){
		            		toMove = nearest(rc.getLocation(), neutrals).location;
		            		dirToMove = rc.getLocation().directionTo(toMove);
		            	}
		            	else if(parts.length > 0){
		            		toMove = nearest(rc.getLocation(), parts);
	                		dirToMove = rc.getLocation().directionTo(toMove);
		            	}
		            	else{
		            		dirToMove = directions[rand.nextInt(8)];
		            	}
	            	}
	            	// Check the rubble in that direction
	                double rubble = rc.senseRubble(rc.getLocation().add(dirToMove));
	                if (rubble > 5000){
	                	dirToMove = directions[rand.nextInt(8)];
	                	rubble = rc.senseRubble(rc.getLocation().add(dirToMove));
	                }
	                
	                if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
	                    // Too much rubble, so I should clear it
	                    rc.clearRubble(dirToMove);
	                    return;
	                    // Check if I can move in this direction
	                }
	                squishMove(dirToMove);
            	}
            } else {
                // Choose a random unit to build
            	RobotType typeToBuild;
            	if(isMasterArchon && openingBuildIndex < masterOpeningBuild.length)
            		typeToBuild = masterOpeningBuild[openingBuildIndex]; //the increment for openingBuildIndex is right after rc.build()
            	else if(!isMasterArchon && openingBuildIndex < subordOpeningBuild.length)
            		typeToBuild = subordOpeningBuild[openingBuildIndex];
            	else if(rc.getRoundNum() < 500)
            		typeToBuild = robotTypesBefore500[rand.nextInt(robotTypesBefore500.length)];
            	else
            		typeToBuild = robotTypesAfter500[rand.nextInt(robotTypesAfter500.length)];
                // Check for sufficient parts
                if (rc.isCoreReady() && rc.hasBuildRequirements(typeToBuild)) {
                    // Choose a random direction to try to build in
                    Direction dirToBuild = directions[rand.nextInt(8)];
                    for (int i = 0; i < 8; i++) {
                        // If possible, build in this direction
                        if (rc.canBuild(dirToBuild, typeToBuild)) {
                        	rc.build(dirToBuild, typeToBuild);
                    		openingBuildIndex++;
                            return;
                        } else {
                            // Rotate the direction to try
                            dirToBuild = dirToBuild.rotateLeft();
                        }
                    }
                }
            }
        }
	}
}
