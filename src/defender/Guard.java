package defender;

import java.util.Random;

import battlecode.common.*;
import team181.Util;

public class Guard extends BattlecodeRobot {

	static final int STATUS_INITIAL = 0;
	static final int STATUS_MOVING = 1;
	static final int STATUS_DEFENDING = 2;

	Team myTeam;
	Team opponentTeam;
	Random rand;

	int status = STATUS_INITIAL;
	MapLocation moveToLocation;

	public Guard(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		while (true) {
			handleMessages();

			try {
				if (status == STATUS_MOVING && Utility.seeCorner(rc)) {
					status = STATUS_DEFENDING;
				}
			} catch (GameActionException e) {}

			switch (status){
				case STATUS_INITIAL:
					defend();
					break;
				case STATUS_MOVING:
					move(moveToLocation);
					break;
				case STATUS_DEFENDING:
					defend();
					break;
			}

			Clock.yield();
		}
	}

	private void move(MapLocation moveToLocation) {
		try {
			Utility.goToLocation(rc, moveToLocation);
		} catch (GameActionException e) {}
	}

	/**
	 * Handles a message received by Guard.
	 */
	private void handleMessages() {
		// Get all signals
		Signal[] signals = rc.emptySignalQueue();
		if (signals.length > 0) {
			// Set an indicator string that can be viewed in the client
			rc.setIndicatorString(0, "I received a signal this turn!");
		} else {
			rc.setIndicatorString(0, "I don't any signal buddies");
		}

		for (Signal s : signals) {
			int[] message = s.getMessage();
			if (message == null) {
				// System.out.println("NULL MESSAGE");
			} else {
				int identifier = message[0];
				int value = message[1];
				MapLocation loc = ConfigUtils.decodeLocation(value);

				switch (identifier) {
					case ConfigUtils.MOVE_TO_CORNER_LOCATION:
						moveToLocation = loc;
						status = STATUS_MOVING;
						break;
				}
			}
		}
	}
}
