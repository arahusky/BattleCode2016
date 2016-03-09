package lectureplayer1;

import java.util.ArrayList;

import battlecode.common.*;

public class Utility {
	static int[] possibleDirections = new int[] { 0, 1, -1, 2, -2, 3, -3, 4 };
	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	static boolean patient = true;

	public static void forwardish(Direction ahead) throws GameActionException {
		RobotController rc = RobotPlayer.rc;
		if (checkIfCornered(ahead)) {
			RobotType typeToBuild = RobotType.TURRET;

			Direction dirToBuild = directions[0];
			int possibleDirectionsToBuild = 0;
			for (int i = 0; i < 8; i++) {
				// If possible, build in this direction
				if (rc.canBuild(dirToBuild, typeToBuild)) {
					possibleDirectionsToBuild++;
				}
				dirToBuild = dirToBuild.rotateLeft();
			}

			if (possibleDirectionsToBuild <= 1) {
				typeToBuild = RobotType.GUARD;
			}

			// Check for sufficient parts
			if (rc.hasBuildRequirements(typeToBuild)) {
				// Choose a random direction to try to build in
				dirToBuild = directions[0];
				for (int i = 0; i < 8; i++) {
					// If possible, build in this direction
					if (rc.canBuild(dirToBuild, typeToBuild)) {
						rc.build(dirToBuild, typeToBuild);
						break;
					} else {
						// Rotate the direction to try
						dirToBuild = dirToBuild.rotateLeft();
					}
				}
			}
			return;
		}

		for (int i : possibleDirections) {
			Direction canidateDirection = Direction.values()[(ahead.ordinal() + i + 8) % 8];
			MapLocation canidateLocation = rc.getLocation().add(canidateDirection);
			if (patient) {
				if (rc.canMove(canidateDirection) && !pastLocations.contains(canidateDirection)) {
					pastLocations.add(rc.getLocation());
					if (pastLocations.size() > 20) {
						pastLocations.remove(0);
					}
					rc.move(canidateDirection);
					return;
				}
			} else {
				if (rc.canMove(canidateDirection)) {
					rc.move(canidateDirection);
					return;
				} else {
					// DIG
					if (rc.senseRubble(canidateLocation) > GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
						rc.clearRubble(canidateDirection);
						return;
					}
				}
			}
		}
		patient = false;
	}

	public static RobotInfo[] joinRobotInfo(RobotInfo[] a, RobotInfo[] b) {
		RobotInfo[] all = new RobotInfo[a.length + b.length];
		int index = 0;
		for (RobotInfo i : a) {
			all[index] = i;
			index++;
		}
		for (RobotInfo i : b) {
			all[index] = i;
			index++;
		}
		return all;
	}

	private static boolean checkIfCornered(Direction dir) {
		Direction[] dirToCheck = new Direction[2];
		RobotController rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		Team enemyTeam = myTeam.opponent();

		switch (dir) {
		case NORTH_EAST:
			dirToCheck = new Direction[] { Direction.NORTH, Direction.EAST };
			break;
		case NORTH_WEST:
			dirToCheck = new Direction[] { Direction.NORTH, Direction.WEST };
			break;
		case SOUTH_EAST:
			dirToCheck = new Direction[] { Direction.SOUTH, Direction.EAST };
			break;
		case SOUTH_WEST:
			dirToCheck = new Direction[] { Direction.SOUTH, Direction.WEST };
			break;
		default:
			return false;
		}

		try {
			MapLocation candidateLocation0 = rc.getLocation().add(dirToCheck[0]);
			MapLocation candidateLocation1 = rc.getLocation().add(dirToCheck[1]);

			if (!rc.onTheMap(candidateLocation0) && !rc.onTheMap(candidateLocation1)) {
				return true;
			}
		} catch (GameActionException e) {
			// nothing to do here
		}

		return false;
	}

}
