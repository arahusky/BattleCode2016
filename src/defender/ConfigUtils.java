package defender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class ConfigUtils {

	public static final Direction[] POSSIBLE_DIRECTIONS = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	public static final int SCOUT_DIRECTION = -2414;
	public static final int SCOUT_LOCATION = -5852;

	public static final int REPORTING_DEN_LOCATION = -3721;
	public static final int REPORTING_CORNER_LOCATION = -440047;
	public static final int MOVE_TO_CORNER_LOCATION = -54631;

	public static final int GO_SOUTH_WEST = 14454;
	public static final int GO_SOUTH_EAST = 2960;
	public static final int GO_NORTH_WEST = 3870;
	public static final int GO_NORTH_EAST = 43068;

	/*
	 * Holds all used flags for broadcasting.
	 */
	public static final List<Integer> USED_CONSTANTS = new ArrayList<>(Arrays.asList(
			SCOUT_DIRECTION,
			SCOUT_LOCATION,
			REPORTING_DEN_LOCATION,
			REPORTING_CORNER_LOCATION,
			GO_SOUTH_WEST,
			GO_SOUTH_EAST,
			GO_NORTH_WEST,
			GO_NORTH_EAST
	));


	private static final int ENCODING_FACTOR = 1000;

	public static int encodeLocation(MapLocation loc) {
		return ENCODING_FACTOR * loc.x + loc.y;
	}

	public static MapLocation decodeLocation(int loc) {
		return new MapLocation(loc / ENCODING_FACTOR, loc % ENCODING_FACTOR);
	}

}
