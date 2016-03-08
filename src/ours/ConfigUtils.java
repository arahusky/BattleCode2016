package ours;

import battlecode.common.MapLocation;

public class ConfigUtils {
	public static final int MESSAGE_FOR_SCOUT = -2;
	
	public static final int REPORTING_DEN_LOCATION = -3;
	public static final int REPORTING_CORNER_LOCATION = -4;
	
	
	public static final int GO_SOUTH_WEST = 1;
	public static final int GO_SOUTH_EAST = 2;
	public static final int GO_NORTH_WEST = 3;
	public static final int GO_NORTH_EAST = 4;
	
	
	private static final int ENCODING_FACTOR = 1000;
	
	public static int encodeLocation(MapLocation loc) {
		return ENCODING_FACTOR * loc.x + loc.y;
	}
	
	public static MapLocation decodeLocation(int loc) {
		return new MapLocation(loc / ENCODING_FACTOR, loc % ENCODING_FACTOR);
	}
	
}
