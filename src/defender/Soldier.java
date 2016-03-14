package defender;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class Soldier extends BattlecodeRobot {

	public Soldier(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while (true) {
			defend();

			Clock.yield();
		}
	}
}
