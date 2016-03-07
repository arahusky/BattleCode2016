package ours;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Scout extends BattlecodeRobot {

	public Scout(RobotController rc) {
		this.rc = rc;
	}

	private Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private List<MapLocation> reportedDens = new ArrayList<>();
	
	private Direction mainDirection = Direction.NONE;

	@Override
	public void run() {

		while (true) {
			
			handleMessages();
			
			RobotInfo[] robots = rc.senseNearbyRobots();
			try {
				// Search for zombie den
				for (RobotInfo robot : robots) {
					if (robot.type == RobotType.ZOMBIEDEN) {
						if (!reportedDens.contains(robot.location)) {
							broadcastDenLocation(robot.location);
							reportedDens.add(robot.location);
							break;
						}
					}
				}

				if (rc.isCoreReady()) {
					for (Direction dir : directions) {
						if (rc.canMove(mainDirection)) {
							rc.move(mainDirection);
							continue;
						}
						if (rc.canMove(dir)) {
							rc.move(dir);
							continue;
						} else {
							MapLocation loc = rc.getLocation().add(dir);
							if (rc.senseRubble(loc) > GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
								rc.clearRubble(dir);
								continue;
							}
						}
					}
				}
			} catch (GameActionException e) {
				// System.out.println(e.getMessage());
				// e.printStackTrace();
			}
		}
	}

	/**
	 * Broadcast information about zombie den. Broadcasting to all archons.
	 * 
	 * @param loc
	 *            Location of the zombie den.
	 */
	private void broadcastDenLocation(MapLocation loc) {
		try {
			MapLocation[] archons = rc.getInitialArchonLocations(rc.getTeam());
			int maxDistance = Integer.MIN_VALUE;
			for (int i = 0; i < archons.length; i++) {
				// Compute the distance between archon and the current scout.
				int distance = archons[i].distanceSquaredTo(rc.getLocation());
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
			rc.broadcastMessageSignal(loc.x, loc.y, maxDistance);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}

	}

	/**
	 * Handles a message received by scout.
	 */
	private void handleMessages() {
		// Get all signals
		Signal[] signals = rc.emptySignalQueue();

		for (Signal s : signals) {
			int[] message = s.getMessage();
			if (message == null) {
				System.out.println("NULL MESSAGE");
			} else {
				int x = message[0];
				int y = message[1];

				if (x == ConfigUtils.MESSAGE_FOR_SCOUT) {
					switch (y) {
					case ConfigUtils.GO_NORTH_EAST:
						mainDirection = Direction.NORTH_EAST;
						break;
					case ConfigUtils.GO_NORTH_WEST:
						mainDirection = Direction.NORTH_WEST;
						break;
					case ConfigUtils.GO_SOUTH_EAST:
						mainDirection = Direction.SOUTH_EAST;
						break;
					case ConfigUtils.GO_SOUTH_WEST:
						mainDirection = Direction.SOUTH_WEST;
						break;

					default:
						break;
					}
				}
			}
		}
	}

}
