package defender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;

public class Scout extends BattlecodeRobot {

	public Scout(RobotController rc) {
		super(rc);
	}

	private Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private List<MapLocation> reportedLocations = new ArrayList<>();

	private Direction mainDirection = Direction.NONE;
	// Sub-directions are parts of main directions
	private Direction subDirection1 = Direction.NONE;
	private Direction subDirection2 = Direction.NONE;

	private boolean goAwayPhase = false;
	private MapLocation archonBarricadeLoc = null;

	private Random rnd = null;

	private final int STEP_LIMIT = 50;
	private final int LOW_HEALTH = 10;
	private int actualSteps = 0;

	@Override
	public void run() {

		rnd = new Random((int) Math.pow(rc.getID(), 2));

		while (true) {

			handleMessages();

			RobotInfo[] robots = rc.senseNearbyRobots();
			try {

				for (RobotInfo robot : robots) {
					if (robot.type == RobotType.ZOMBIEDEN) {
						broadcastLocation(robot.location, ConfigUtils.REPORTING_DEN_LOCATION);
					}
					if (Utility.checkIfCornered(rc, robot.location)) {
						broadcastLocation(robot.location, ConfigUtils.REPORTING_CORNER_LOCATION);
						goAwayPhase = true;
					}
					if (robot.team == rc.getTeam().opponent()) {
						// Broadcasting location of the opponent's robot. The
						// locations are static and we do NOT update them.
						// Reporting only Archons.
						if (robot.type == RobotType.ARCHON) {
							broadcastLocation(robot.location, ConfigUtils.REPORTING_OPPONENT);
						}
					}
				}

				if (rc.getHealth() <= LOW_HEALTH) {
					broadcastLowHealth();
				}

				if (Utility.checkIfCornered(rc, rc.getLocation())) {
					MapLocation loc = rc.getLocation();
					broadcastLocation(loc, ConfigUtils.REPORTING_CORNER_LOCATION);
					goAwayPhase = true;
				}

				if (Utility.seeCorner(rc)) {
					MapLocation loc = Utility.getNearCorner(rc);
					if (loc != ConfigUtils.UNDEFINED_LOCATION) {
						broadcastLocation(loc, ConfigUtils.REPORTING_CORNER_LOCATION);
						goAwayPhase = true;
					}
				}

				if (goAwayPhase) {
					goAway();
					if (actualSteps >= STEP_LIMIT) {
						fakeBroadcast();
						Clock.yield();
					}
				} else {
					goToCorner();
				}

			} catch (GameActionException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}

	}

	private void fakeBroadcast() throws GameActionException {
		final int distance = ConfigUtils.MAX_BROADCAST_RADIUS;
		int flag = rnd.nextInt();
		int value = rnd.nextInt();
		if (ConfigUtils.USED_CONSTANTS.contains(flag)) {
			return;
		}

		final int maxBroadcastCount = 20;
		if (rc.getBasicSignalCount() >= maxBroadcastCount) {
			return;
		}
		rc.broadcastMessageSignal(flag, value, distance);
	}

	private void goAway() throws GameActionException {
		if (rc.isCoreReady()) {
			int maxDistance = Integer.MIN_VALUE;
			Direction dirToGo = Direction.NONE;
			for (Direction dir : directions) {
				if (rc.canMove(dir)) {
					if (archonBarricadeLoc == null) {
						// We need to wait for location, where archon will
						// barricade
						return;
					}
					int distance = archonBarricadeLoc.distanceSquaredTo(rc.getLocation().add(dir));
					if (maxDistance < distance) {
						maxDistance = distance;
						dirToGo = dir;
					}
				}
			}
			if (rc.canMove(dirToGo)) {
				rc.move(dirToGo);
				actualSteps++;
			} else {
				rc.disintegrate();
			}
		}
	}

	private void goToCorner() throws GameActionException {
		if (rc.isCoreReady()) {
			Utility.forwardish(rc, mainDirection);
			/*
			 * if (tryToGoToDirection(mainDirection)) { return; } if
			 * (tryToGoToDirection(subDirection1)) { return; } if
			 * (tryToGoToDirection(subDirection2)) { return; } for (Direction
			 * dir : directions) { if (tryToGoToDirection(dir)) { break; } else
			 * { MapLocation loc = rc.getLocation().add(dir); if
			 * (rc.senseRubble(loc) > GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
			 * rc.clearRubble(dir); break; } } }
			 */
		}
	}

	/**
	 * Tries to go to the specified direction.
	 * 
	 * @param dir
	 *            Direction to go.
	 * @return True if the move was successful.
	 */
	/*
	 * private boolean tryToGoToDirection(Direction dir) throws
	 * GameActionException { if (rc.canMove(dir)) { rc.move(dir); return true; }
	 * return false; }
	 */
	/*
	 * Broadcast the specified location.
	 */
	private void broadcastLocation(MapLocation loc, int broadcastType) {
		try {

			if (reportedLocations.contains(loc)) {
				return;
			}

			/**
			 * MapLocation[] archonLocations =
			 * rc.getInitialArchonLocations(rc.getTeam()); int maxDistance =
			 * Integer.MIN_VALUE; for (int i = 0; i < archonLocations.length;
			 * i++) { // Compute the distance between archon and the current
			 * scout. int distance =
			 * archonLocations[i].distanceSquaredTo(rc.getLocation()); if
			 * (distance > maxDistance) { maxDistance = distance; } } /
			 **/
			rc.broadcastMessageSignal(broadcastType, ConfigUtils.encodeLocation(loc), ConfigUtils.MAX_BROADCAST_RADIUS);
			reportedLocations.add(loc);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	private void broadcastLowHealth() throws GameActionException {
		int radius = ConfigUtils.MAX_BROADCAST_RADIUS;
		rc.broadcastMessageSignal(ConfigUtils.SCOUT_LOW_HEALTH, rc.getID(), radius);
	}

	/**
	 * Handles a message received by scout.
	 */
	private void handleMessages() {
		// Get all signals
		Signal[] signals = rc.emptySignalQueue();

		for (Signal s : signals) {

			if (s.getTeam() != rc.getTeam()) {
				// Broadcast from another team
				continue;
			}

			int[] message = s.getMessage();
			if (message == null) {
				return;
			}
			int identifier = message[0];
			int value = message[1];

			if (identifier == ConfigUtils.SCOUT_DIRECTION) {
				switch (value) {
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

			if (identifier == ConfigUtils.SCOUT_LOCATION) {
				archonBarricadeLoc = ConfigUtils.decodeLocation(value);
				goAwayPhase = true;

			}
		}
	}

}
