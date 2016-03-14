package defender;

import battlecode.common.*;

import java.util.ArrayList;

public class Utility {
	static int[] possibleDirections = new int[]{0, 1, -1, 2, -2, 3, -3, 4};
	static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	static boolean patient = true;

	public static void resetPastLocations() {
		pastLocations.clear();
	}

	public static void forwardish(RobotController rc, Direction ahead) throws GameActionException {
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

	public static void goToLocation(RobotController rc, MapLocation goToLocation) throws GameActionException {
		MapLocation myPos = rc.getLocation();

		int x = goToLocation.x - myPos.x;
		int y = goToLocation.y - myPos.y;

		Direction goToDirection;
		if (x < 0 && y < 0)
			goToDirection = Direction.NORTH_WEST;
		else if (x > 0 && y < 0)
			goToDirection = Direction.NORTH_EAST;
		else if (x < 0 && y > 0)
			goToDirection = Direction.SOUTH_WEST;
		else if (x > 0 && y > 0)
			goToDirection = Direction.SOUTH_EAST;
		else if (x < 0)
			goToDirection = Direction.WEST;
		else if (x > 0)
			goToDirection = Direction.EAST;
		else if (y < 0)
			goToDirection = Direction.NORTH;
		else if (y > 0)
			goToDirection = Direction.SOUTH;
		else
			goToDirection = Direction.NONE;

		// rc.setIndicatorString(0, "mypos " + myPos);
		// rc.setIndicatorString(1, "goto " + goToLocation);
		// rc.setIndicatorString(2, "dir " + goToDirection + " x " + x + " y " +
		// y);

		forwardishNoReturn(rc, goToDirection);
	}

	public static void forwardishNoReturn(RobotController rc, Direction ahead) throws GameActionException {
		for (int i : possibleDirections) {
			Direction canidateDirection = Direction.values()[(ahead.ordinal() + i + 8) % 8];
			MapLocation canidateLocation = rc.getLocation().add(canidateDirection);

			if (rc.canMove(canidateDirection) && !pastLocations.contains(canidateLocation)) {
				pastLocations.add(rc.getLocation());
				if (pastLocations.size() > 20) {
					pastLocations.remove(0);
				}
				rc.move(canidateDirection);
				return;
			}
		}

		throw new GameActionException(GameActionExceptionType.CANT_MOVE_THERE, "Cant move");
	}

	public static boolean seeCorner(RobotController rc) throws GameActionException {
		int sight = rc.getType().sensorRadiusSquared;
		Direction[] dirsToCheck = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
		int outOfMapDirections = 0;
		for (Direction dir : dirsToCheck) {
			MapLocation locToCheck = rc.getLocation().add(dir, (int)Math.sqrt(sight));
			if (!rc.onTheMap(locToCheck)) {
				outOfMapDirections++;
			}
		}
		if (outOfMapDirections >= 2) {
			return true;
		}
		return false;
	}

}
