package examplefuncsplayer;

import battlecode.common.*;

public class RobotPlayer {

	/**
	 * run() is the method that is called when a robot is instantiated in the
	 * Battlecode world. If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) {

		RobotType[] robotTypes = { RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
				RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET };

		BattlecodeRobot robot = null;
		RobotType rt = rc.getType();
		switch (rt) {
		case ARCHON:
			robot = new Archon(rc);
			break;
		case TURRET:
			robot = new Turret(rc);
			break;
		case SOLDIER:
			robot = new Soldier(rc);
			break;

		/* TODO:
		case SCOUT:
			robot = new Scout(rc);
			break;
		case GUARD:
			robot = new Guard(rc);
			break;
		case VIPER:
			robot = new Viper(rc);
			break;
		*/
		default:
			robot = new Soldier(rc);
			break;
		}
		robot.run();
	}

}
