package defender;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import battlecode.common.*;

public class Archon extends BattlecodeRobot {

	public Archon(RobotController rc) {
		this.rc = rc;
	}

	Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private static final int NUMBER_OF_SCOUTS = 2; // For each archon

	private List<MapLocation> corners = new ArrayList<>(); // Discovered corners
	private List<MapLocation> dens = new ArrayList<>(); // Discovered dens

	private static final MapLocation ALREADY_IN_CORNER = new MapLocation(-1, -1);
	private static final MapLocation UNDEFINED_LOCATION = new MapLocation(-42, -42);

	/*
	 * Maximum number of rounds (=time) for finding the location where to go.
	 */
	private static final int MAX_ROUNDS = 200;

	private Random rand = null;
	private int scoutsCreated = 0;
	private int broadcastedToScouts = 0;
	private List<Integer> directionsForScouts = new ArrayList<>();

	private int bestDistanceFromCorner = 10000;
	private boolean isNearCorner = false;
	private static final int SEARCH_CORNER_DISTANCE = 12;

	@Override
	public void run() {

		MapLocation dest = getDestination();
		System.out.println("Honkl done. Returning: " + dest.toString() + " current: " + rc.getLocation().toString());
		swarmToCorner(dest);
		System.out.println("Helgrind done");
		barricade();
	}

	private void barricade() {
		while (true) {
			try {
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

				// if there's only one location left, build guard
				if (possibleDirectionsToBuild <= 1) {
					typeToBuild = RobotType.GUARD;
				}

				// if there's too little guards nearby, build guard
				if (getMyNearbyUnitsCount() < 2) {
					typeToBuild = RobotType.GUARD;
				}

				// if there's too little guards nearby, build guard
				if (isEnemyAhead() || getMyNearbyUnitsCount() < 5) {
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
			} catch (GameActionException e) {
				// nothing to do here
			}
		}
	}

	private int getMyNearbyUnitsCount() {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();

		int res = 0;
		for (RobotInfo ri : nearbyRobots) {
			if (ri.team.equals(rc.getTeam())) {
				res++;
			}
		}

		return res;
	}

	private boolean isEnemyAhead() {
		return rc.senseHostileRobots(rc.getLocation(), rc.getType().sensorRadiusSquared).length > 0;
	}

	private void swarmToCorner(MapLocation goToLocation) {
		Direction movingDirection = Direction.NORTH_EAST;
		if (rc.getTeam() == Team.B) {
			movingDirection = Direction.SOUTH_WEST;
		}
		System.out.println(movingDirection);

		// @TODO REMOVE WHEN DONE
		if (goToLocation == null) {
			goToLocation = rc.getLocation();
			goToLocation = goToLocation.add(Direction.NONE, 0);
		}

		try {
			rc.broadcastMessageSignal(ConfigUtils.MOVE_TO_CORNER_LOCATION, ConfigUtils.encodeLocation(goToLocation), 6);
		} catch (GameActionException e) { }

		while (!checkIfCornered(rc)) {
			RobotInfo[] zombieEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Team.ZOMBIE);
			RobotInfo[] normalEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, rc.getTeam().opponent());
			RobotInfo[] opponentEnemies = Utility.joinRobotInfo(zombieEnemies, normalEnemies);

			try {
				if (opponentEnemies.length > 0 && rc.getType().canAttack()) {
					if (rc.isWeaponReady()) {
						rc.attackLocation(opponentEnemies[0].location);
					}
				} else {
					if (rc.isCoreReady()) {
						if (goToLocation == null) {
							Utility.forwardish(rc, movingDirection);
						} else {
							Utility.goToLocation(rc, goToLocation);

							if(isNearCorner && bestDistanceFromCorner < goToLocation.distanceSquaredTo(rc.getLocation())){
//								System.out.println("STOPPING ARCHON");
								break;
							}

							if (bestDistanceFromCorner > goToLocation.distanceSquaredTo(rc.getLocation())) {
								bestDistanceFromCorner = goToLocation.distanceSquaredTo(rc.getLocation());
//								System.out.println("ARCHON IN BETTER LOCA");
							}

							if (goToLocation.distanceSquaredTo(rc.getLocation()) < SEARCH_CORNER_DISTANCE){
								isNearCorner = true;
								bestDistanceFromCorner = goToLocation.distanceSquaredTo(rc.getLocation());
								Utility.resetPastLocations();
//								System.out.println("ARCHON GONE TOO FAR, GOING BACK");
							}
						}
					}
				}
			} catch (GameActionException e) {
				if (e.getType() == GameActionExceptionType.CANT_MOVE_THERE) {
					System.out.println("BREAKING SWARMING");
					break;
				}
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates a few scouts and tries to find a zombie den or corner of the map.
	 * 
	 * @return The position where to go and make barricades. If result is
	 *         (-1,-1), then we are in the corner. If
	 */
	private MapLocation getDestination() {
		rand = new Random(rc.getID());

		if (rc.getTeam() == Team.A) {
			directionsForScouts.add(ConfigUtils.GO_NORTH_WEST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_EAST);
		} else {
			directionsForScouts.add(ConfigUtils.GO_NORTH_EAST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_WEST);
		}

		// Check if we start at the corner
		if (checkIfCornered(rc)) {
			sendScoutsAway(rc.getLocation());
			return ALREADY_IN_CORNER;
		}

		while (true) {

			if (rc.getRoundNum() > MAX_ROUNDS) {
				MapLocation result = UNDEFINED_LOCATION;
				if (!corners.isEmpty()) {
					result = corners.get(0);
				} else if (!dens.isEmpty()) {
					result = dens.get(0);
				}
				sendScoutsAway(result);
				return result;
			}

			handleMessages();

			try {

				if (NUMBER_OF_SCOUTS > scoutsCreated) {
					tryCreateUnit(RobotType.SCOUT);
				}

				// In the meantime, build guards
				tryCreateUnit(RobotType.GUARD);

				if (NUMBER_OF_SCOUTS <= scoutsCreated && NUMBER_OF_SCOUTS <= broadcastedToScouts) {
					if (corners.size() >= NUMBER_OF_SCOUTS) {
						int distance = 0;
						int minDistance = Integer.MAX_VALUE;
						int indexOfMin = 0;
						for (int i = 0; i < corners.size(); i++) {
							distance = rc.getLocation().distanceSquaredTo(corners.get(i));
							if (distance < minDistance) {
								minDistance = distance;
								indexOfMin = i;
							}
						}
						sendScoutsAway(corners.get(indexOfMin));
						return corners.get(indexOfMin);
					}
				}
				Clock.yield();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void tryCreateUnit(RobotType typeToBuild) throws GameActionException {
		// Check for sufficient parts
		if (rc.hasBuildRequirements(typeToBuild) && rc.isCoreReady()) {
			// Choose a random direction to try to build in
			final int spotsForBuild = 8;
			Direction dirToBuild = directions[rand.nextInt(spotsForBuild)];
			for (int i = 0; i < spotsForBuild; i++) {

				// If possible, build in this direction
				if (rc.canBuild(dirToBuild, typeToBuild)) {

					if (scoutsCreated > 0 && !directionsForScouts.isEmpty()) {
						broadcastDirectionToScout(directionsForScouts.remove(0));
						broadcastedToScouts++;
					}

					rc.build(dirToBuild, typeToBuild);
					if (typeToBuild == RobotType.SCOUT) {
						scoutsCreated++;
					}

					break;
				} else {
					// Rotate the direction to try
					dirToBuild = dirToBuild.rotateLeft();
				}
			}
		}
	}

	/**
	 * Handles a message received by archon.
	 */
	private void handleMessages() {
		// Get all signals
		Signal[] signals = rc.emptySignalQueue();
		if (signals.length > 0) {
			// Set an indicator string that can be viewed in the
			// client
			rc.setIndicatorString(0, "I received a signal this turn!");
		} else {
			rc.setIndicatorString(0, "I don't any signal buddies");
		}

		for (Signal s : signals) {
			int[] message = s.getMessage();
			if (message == null) {
				return;
			}
			int identifier = message[0];
			int value = message[1];
			MapLocation loc = ConfigUtils.decodeLocation(value);

			switch (identifier) {
			case ConfigUtils.REPORTING_CORNER_LOCATION:
				if (!corners.contains(loc)) {
					corners.add(loc);
				}
				break;
			case ConfigUtils.REPORTING_DEN_LOCATION:
				if (!dens.contains(loc)) {
					dens.add(loc);
				}
				break;
			}
		}
	}

	/*
	 * Broadcasts the specified direction to scouts. Scouts should move in this
	 * direction. Used after initialization of the scouts.
	 */
	private void broadcastDirectionToScout(int direction) {
		try {
			rc.broadcastMessageSignal(ConfigUtils.SCOUT_DIRECTION, direction, 3);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	/*
	 * Broadcasts the specified location to scouts. Scouts should go far away
	 * from this location.
	 */
	private void broadcastLocationToScout(MapLocation loc) {
		try {
			int mapSizeSq = 80 * 80; // broadcasting across whole map
			rc.broadcastMessageSignal(ConfigUtils.SCOUT_LOCATION, ConfigUtils.encodeLocation(loc), mapSizeSq);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	/*
	 * Send a message to scouts to go far away from the specified location.
	 */
	private void sendScoutsAway(MapLocation loc) {
		if (loc == UNDEFINED_LOCATION) {
			broadcastLocationToScout(rc.getLocation());
		} else {
			broadcastLocationToScout(loc);
		}
	}

	/*
	 * Checks if location of the specified robot (robot controller) is in the
	 * corner of the map.
	 */
	public static boolean checkIfCornered(RobotController rc) {
		List<Direction[]> dirToCheck = new ArrayList<>();

		dirToCheck.add(new Direction[] { Direction.NORTH, Direction.EAST });
		dirToCheck.add(new Direction[] { Direction.NORTH, Direction.WEST });
		dirToCheck.add(new Direction[] { Direction.SOUTH, Direction.EAST });
		dirToCheck.add(new Direction[] { Direction.SOUTH, Direction.WEST });

		for (Direction[] pair : dirToCheck) {
			MapLocation candidateLocation0 = rc.getLocation().add(pair[0]);
			MapLocation candidateLocation1 = rc.getLocation().add(pair[1]);

			try {
				if (!rc.onTheMap(candidateLocation0) && !rc.onTheMap(candidateLocation1)) {
					return true;
				}
			} catch (GameActionException e) {
				// nothing to do here
			}
		}

		return false;
	}

}
