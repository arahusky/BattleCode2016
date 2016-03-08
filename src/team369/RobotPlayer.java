package team369;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class RobotPlayer {
	
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) {
    	/* TODOS
    	 * something based on ant behavior?
    	 * since vipers infect, send viper raids
    	*/
    	
    	Robot r = null;
    	
        try {
            // Any code here gets executed exactly once at the beginning of the game.
        	switch(rc.getType()){
        	case ARCHON:
        		r = new Archon(rc);
        		break;
        	case VIPER:
        	case SOLDIER:
        		r = new Soldier(rc);
        		break;
        	case TURRET:
        	case TTM:
        		r = new Turret(rc);
        		break;
        	case SCOUT:
        		r = new Scout(rc);
        		break;
			default:
				r = new Soldier(rc);
				break;
        	}
        } catch (Exception e) {
            // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
            // Caught exceptions will result in a bytecode penalty.
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
                r.loop();
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}