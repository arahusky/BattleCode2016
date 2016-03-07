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

public class Scout extends BattlecodeRobot {

	public Scout(RobotController rc) {
		this.rc = rc;
	}

	private Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private List<MapLocation> reportedDens = new ArrayList<>();

	@Override
	public void run() {

		Direction mainDirection = getMainDirection();

		while (true) {
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
	 * Evaluates starting direction for the current scout.
	 * @return Random direction to start with.
	 */
	private Direction getMainDirection() {
		// Using only rc.getID() for seed is not enough!
		Random rnd = new Random((int) Math.pow(rc.getID(), 2));
		int randomIndex = rnd.nextInt(directions.length);
		return directions[randomIndex];
	}

	/**
	 * Broadcast information about zombie den. Broadcasting to all archons.
	 * @param loc Location of the zombie den.
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

}
