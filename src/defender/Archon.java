package defender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import battlecode.common.*;

public class Archon extends BattlecodeRobot {

	public Archon(RobotController rc) {
		super(rc);
	}

	Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	private static final int NUMBER_OF_SCOUTS = 2; // For each archon

	// Discovered corners
	private List<MapLocation> corners = new ArrayList<>();
	// Discovered dens
	private List<MapLocation> dens = new ArrayList<>();
	// Discovered opponent's robots. Not updating them.
	private List<MapLocation> opponent = new ArrayList<>();

	private static final MapLocation UNDEFINED_LOCATION = new MapLocation(-42, -42);

	/*
	 * Maximum number of rounds (=time) for finding the location where to go.
	 */
	private static final int MAX_ROUNDS = 200;

	private Random rand = null;
	private int scoutsCreated = 0;
	private int broadcastedToScouts = 0;
	private List<Integer> directionsForScouts = new ArrayList<>();
	private List<Integer> scoutsWithLowHealth = new ArrayList<>();

	private int bestDistanceFromCorner = 10000;
	private boolean isNearCorner = false;
	private boolean gotResultFromArchon = false;
	private static final int SEARCH_CORNER_DISTANCE = 12;
	private static final int GOING_AWAY_FROM_CORNER = 25;
	private MapLocation result = UNDEFINED_LOCATION;

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

				if (!isEnemyAhead() && getMyNearbyUnitsCount() > 2) {
					if (rand.nextDouble() <= 0.5) {
						typeToBuild = RobotType.SOLDIER;
					} else {
						typeToBuild = RobotType.GUARD;
					}
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
		} catch (GameActionException e) {
		}

		while (!Utility.checkIfCornered(rc, rc.getLocation())) {
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

							if (isNearCorner
									&& bestDistanceFromCorner < goToLocation.distanceSquaredTo(rc.getLocation())) {
								// System.out.println("STOPPING ARCHON");
								break;
							}

							if(!isNearCorner && (goToLocation.distanceSquaredTo(rc.getLocation()) - bestDistanceFromCorner > GOING_AWAY_FROM_CORNER)) {
								break;
								// GOING TOO FAR
							}

							if (bestDistanceFromCorner > goToLocation.distanceSquaredTo(rc.getLocation())) {
								bestDistanceFromCorner = goToLocation.distanceSquaredTo(rc.getLocation());
								// System.out.println("ARCHON IN BETTER LOCA");
							}

							if (goToLocation.distanceSquaredTo(rc.getLocation()) < SEARCH_CORNER_DISTANCE) {
								isNearCorner = true;
								bestDistanceFromCorner = goToLocation.distanceSquaredTo(rc.getLocation());
								Utility.resetPastLocations();
								// System.out.println("ARCHON GONE TOO FAR,
								// GOING BACK");
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
		rand = new Random((int) Math.pow(rc.getID(), 2));

		if (rc.getTeam() == Team.B) {
			directionsForScouts.add(ConfigUtils.GO_NORTH_WEST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_EAST);
			directionsForScouts.add(ConfigUtils.GO_NORTH_EAST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_WEST);
		} else {
			directionsForScouts.add(ConfigUtils.GO_NORTH_EAST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_WEST);
			directionsForScouts.add(ConfigUtils.GO_NORTH_WEST);
			directionsForScouts.add(ConfigUtils.GO_SOUTH_EAST);
		}

		while (true) {
			try {

				if (Utility.seeCorner(rc) && !gotResultFromArchon) {
					result = Utility.getNearCorner(rc);
					sendScoutsAway(result);
					broadcastResultToArchons(result);
					return result;
				}

				if (rc.getRoundNum() > MAX_ROUNDS) {
					if (!corners.isEmpty()) {
						result = corners.get(0);
					} else if (!dens.isEmpty()) {
						result = dens.get(0);
					}
					if (Utility.seeCorner(rc) && !rc.canSense(result)) {
						result = rc.getLocation();
					}
					sendScoutsAway(result);
					return result;
				}

				handleMessages();

				if (NUMBER_OF_SCOUTS > scoutsCreated && isThisMainArchon()) {
					tryCreateUnit(RobotType.SCOUT);
				}

				tryCreateUnit(RobotType.GUARD);

				if (isThisMainArchon()) {
					// We have location where to go if this archon created
					// scouts
					// and gets results. If this archon is not creating scouts,
					// we wait for results from another archon.
					int broadcastedOrLowHealth = broadcastedToScouts + scoutsWithLowHealth.size();
					if (NUMBER_OF_SCOUTS <= scoutsCreated && NUMBER_OF_SCOUTS <= broadcastedOrLowHealth) {
						// Got all results
						if (corners.size() >= NUMBER_OF_SCOUTS) {
							MapLocation loc = getBestCornerToGo();
							if (Utility.seeCorner(rc) && !rc.canSense(loc)) {
								loc = rc.getLocation();
							}
							sendScoutsAway(loc);
							broadcastResultToArchons(loc);
							return loc;
						}
					}
				}

				if (gotResultFromArchon) {
					return result;
				}

				Clock.yield();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}

	}

	/*
	 * Determines whether the current Archon should create scouts.
	 */
	private boolean isThisMainArchon() {
		MapLocation[] locs = rc.getInitialArchonLocations(rc.getTeam());
		Arrays.sort(locs);
		Arrays.sort(locs, new Comparator<MapLocation>() {
			public int compare(MapLocation o1, MapLocation o2) {
				if (o1.x < o2.x) {
					return -1;
				}
				if (o1.x > o2.x) {
					return 1;
				}
				if (o1.y < o2.y) {
					return -1;
				}
				if (o1.y > o2.y) {
					return 1;
				}
				return 0;
			}
		});

		MapLocation middle = locs[locs.length / 2];
		if (rc.getLocation().equals(middle)) {
			return true;
		}
		return false;
	}

	/*
	 * If this Archon is main archon, broadcasts a result to other Archons.
	 */
	private void broadcastResultToArchons(MapLocation loc) {
		try {
			rc.broadcastMessageSignal(ConfigUtils.RESULT, ConfigUtils.encodeLocation(loc),
					ConfigUtils.MAX_BROADCAST_RADIUS);
		} catch (GameActionException ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	private MapLocation getBestCornerToGo() {
		MapLocation result;
		List<MapLocation> dangerous = new ArrayList<>();
		dangerous.addAll(dens);
		dangerous.addAll(opponent);
		if (dangerous.isEmpty()) {
			result = getMinimumDistanceFrom(corners, rc.getLocation());
		} else {
			int distance = 0;
			int maxDistance = Integer.MIN_VALUE;
			int indexOfMax = 0;
			for (int i = 0; i < corners.size(); i++) {
				distance = getMinimumDistanceFrom(dangerous, rc.getLocation()).distanceSquaredTo(corners.get(i));
				if (distance > maxDistance) {
					maxDistance = distance;
					indexOfMax = i;
				}
			}
			result = corners.get(indexOfMax);

		}
		sendScoutsAway(result);
		return result;
	}

	private MapLocation getMinimumDistanceFrom(List<MapLocation> locs, MapLocation locationToMeasure) {
		int distance = 0;
		int minDistance = Integer.MAX_VALUE;
		int indexOfMin = 0;
		for (int i = 0; i < locs.size(); i++) {
			distance = locationToMeasure.distanceSquaredTo(locs.get(i));
			if (distance < minDistance) {
				minDistance = distance;
				indexOfMin = i;
			}
		}
		return locs.get(indexOfMin);
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

			if (s.getTeam() != rc.getTeam()) {
				// Broadcast from another team...
				continue;
			}

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
			case ConfigUtils.REPORTING_OPPONENT:
				if (!opponent.contains(loc)) {
					opponent.add(loc);
				}
				break;
			case ConfigUtils.SCOUT_LOW_HEALTH:
				if (!scoutsWithLowHealth.contains(value)) {
					scoutsWithLowHealth.add(value);
				}
				break;
			case ConfigUtils.RESULT:
				result = loc;
				gotResultFromArchon = true;
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
			rc.broadcastMessageSignal(ConfigUtils.SCOUT_DIRECTION, direction, 2);
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
			int mapSizeSq = ConfigUtils.MAX_BROADCAST_RADIUS;
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

}
