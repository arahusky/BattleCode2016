package team369;

import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Scout extends Robot{
	public Scout(RobotController rc){
		super(rc);
	}
	public void loop() throws Exception{
		sa.fetch();
		
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(60, enemyTeam);
		int i = 0;
		for(; i < GameConstants.MESSAGE_SIGNALS_PER_TURN && i < enemyRobots.length; i++)
			sa.broadcastEnemyPosition(enemyRobots[i].location, 50);
		
		RobotInfo[] zombieRobots = rc.senseNearbyRobots(60, Team.ZOMBIE);
		for(int j = 0; j + i < GameConstants.MESSAGE_SIGNALS_PER_TURN && j < zombieRobots.length; j++){
			if(zombieRobots[j].type == RobotType.ZOMBIEDEN)
				sa.broadcastZombieHome(zombieRobots[j].location, 100);
			else
				sa.broadcastZombiePosition(zombieRobots[j].location, 50);
		}
		
		/*for(Signal s : sa.allyMessageSignals) //rebroadcast
			if(rc.getMessageSignalCount() < 20 && SignalAdapter.isCommand(s.getMessage(), SignalAdapter.Cmd.ZOMBIE_HOME))
				sa.broadcastZombieHome(SignalAdapter.getLocation(s.getMessage()),100);
		*/
		
		
        if (rc.isCoreReady()) {
            // Choose a direction to try to move in
        		//todo - how would I bait a group of zombies to move with me toward the enemy team?
        	Direction dir = null;
        	
        	if(friendlyCount() == 0 && hostileHealthTotal() > 2000) //bait toward enemies if alone
        		dir = rc.getLocation().directionTo(enemyHomeAreaLocation);
        	else if(hostileCount() > 1) //enemies near, go away
    			dir = hostileDirection().opposite();
    		else if(sa.allyBasicSignals.size() > 0) //help is needed, go there
    			dir = rc.getLocation().directionTo(sa.allyBasicSignals.get(0).getLocation());
        	
        	if(dir == null || (dir == Direction.NONE && rand.nextBoolean()))
        		dir = directions[rand.nextInt(8)];
        	
        	squishMove(dir); //using this to avoid random walk loopback
        }
	}
}
