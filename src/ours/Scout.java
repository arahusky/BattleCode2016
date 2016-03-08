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

	private List<MapLocation> reportedLocations = new ArrayList<>();

	private Direction mainDirection = Direction.NONE;

	// Sub-directions are parts of main directions
	private Direction subDirection1 = Direction.NONE;
	private Direction subDirection2 = Direction.NONE;

	@Override
	public void run() {

		while (true) {

			handleMessages();

			RobotInfo[] robots = rc.senseNearbyRobots();
			try {
				// Not reporting zombie dens
				/**
				 * for (RobotInfo robot : robots) { if (robot.type ==
				 * RobotType.ZOMBIEDEN) {
				 * 
				 * if (!reportedLocations.contains(robot.location)) {
				 * broadcastLocation(robot.location);
				 * reportedLocations.add(robot.location); } } } /
				 **/

				if (Archon.checkIfCornered(rc)) {
					MapLocation loc = rc.getLocation();
					if (!reportedLocations.contains(loc)) {
						broadcastLocation(loc);
						reportedLocations.add(loc);
					}
				}

				if (rc.isCoreReady()) {
					if (tryToGoToDirection(mainDirection)) {
						continue;
					}
					if (tryToGoToDirection(subDirection1)) {
						continue;
					}
					if (tryToGoToDirection(subDirection2)) {
						continue;
					}
					for (Direction dir : directions) {
						if (tryToGoToDirection(dir)) {
							break;
						} else {
							MapLocation loc = rc.getLocation().add(dir);
							if (rc.senseRubble(loc) > GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
								rc.clearRubble(dir);
								break;
							}
						}
					}
				}
			} catch (GameActionException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Tries to go to the specified direction.
	 * 
	 * @param dir
	 *            Direction to go.
	 * @return True if the move was successful.
	 */
	private boolean tryToGoToDirection(Direction dir) throws GameActionException {
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}

	/*
	 * Broadcast the specified location.
	 */
	private void broadcastLocation(MapLocation loc) {
		try {
			MapLocation[] archonLocations = rc.getInitialArchonLocations(rc.getTeam());
			int maxDistance = Integer.MIN_VALUE;
			for (int i = 0; i < archonLocations.length; i++) {
				// Compute the distance between archon and the current scout.
				int distance = archonLocations[i].distanceSquaredTo(rc.getLocation());
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
			rc.broadcastMessageSignal(loc.x, loc.y, maxDistance);
			System.out.println("Rep: " + loc.x + "," + loc.y);
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
						subDirection1 = Direction.NORTH;
						subDirection2 = Direction.EAST;
						break;
					case ConfigUtils.GO_NORTH_WEST:
						mainDirection = Direction.NORTH_WEST;
						subDirection1 = Direction.NORTH;
						subDirection2 = Direction.WEST;
						break;
					case ConfigUtils.GO_SOUTH_EAST:
						mainDirection = Direction.SOUTH_EAST;
						subDirection1 = Direction.SOUTH;
						subDirection2 = Direction.EAST;
						break;
					case ConfigUtils.GO_SOUTH_WEST:
						mainDirection = Direction.SOUTH_WEST;
						subDirection1 = Direction.SOUTH;
						subDirection2 = Direction.WEST;
						break;

					default:
						break;
					}
				}
			}
		}
	}

}
