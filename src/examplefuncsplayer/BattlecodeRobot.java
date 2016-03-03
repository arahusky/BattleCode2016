package examplefuncsplayer;

import battlecode.common.RobotController;

public abstract class BattlecodeRobot {

	RobotController rc;

	/**
	 * Provides a behavior of robot in the Battlecode world. If this method
	 * returns, the robot dies.
	 */
	public abstract void run();

}
