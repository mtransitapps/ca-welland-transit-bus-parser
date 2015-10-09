package org.mtransit.parser.ca_welland_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.niagararegion.ca/government/opendata/data-set.aspx#id=32
// http://www.niagararegion.ca/government/opendata/data-set.aspx#id=32&tab=data_table&f=xml&r=500&p=1
// http://maps-dev.niagararegion.ca/GoogleTransit/NiagaraRegionTransit.zip
public class WellandTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-welland-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WellandTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Welland Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Welland Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	private static final String WELLAND_TRANSIT = "WE";

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!WELLAND_TRANSIT.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final String R_WE_NF = "R_WE_NF";
	private static final String R_WE_NOTL = "R_WE_NOTL";
	private static final String R_PC001 = "R_PC001";
	private static final String R_PC002 = "R_PC002";
	private static final String R_WEPC = "R_WEPC";
	private static final String R_WESC = "R_WESC";
	private static final String R_WE0 = "R_WE0";

	private static final long R_PC002_ID = 16002l;
	private static final long R_PC001_ID = 16001l;
	private static final long R_WEPC_ID = 16003l;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (R_WE_NF.equals(gRoute.getRouteId())) {
			return 14001l;
		} else if (R_WE_NOTL.equals(gRoute.getRouteId())) {
			return 14002l;
		} else if (R_PC001.equals(gRoute.getRouteId())) {
			return R_PC001_ID;
		} else if (R_PC002.equals(gRoute.getRouteId())) {
			return R_PC002_ID;
		} else if (R_WEPC.equals(gRoute.getRouteId())) {
			return R_WEPC_ID;
		} else if (R_WESC.equals(gRoute.getRouteId())) {
			return 19001l;
		} else if (gRoute.getRouteId().startsWith(R_WE0)) {
			if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				return Long.valueOf(gRoute.getRouteShortName()); // using route short name as route ID
			}
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String NF = "NF";
	private static final String NOTL = "NOTL";
	private static final String PC1EAST = "PC1";
	private static final String PC2WEST = "PC2";
	private static final String PCL = "PCL";
	private static final String BL = "BL";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (R_WE_NF.equals(gRoute.getRouteId())) {
			return NF;
		} else if (R_WE_NOTL.equals(gRoute.getRouteId())) {
			return NOTL;
		} else if (R_PC001.equals(gRoute.getRouteId())) {
			return PC1EAST;
		} else if (R_PC002.equals(gRoute.getRouteId())) {
			return PC2WEST;
		} else if (R_WEPC.equals(gRoute.getRouteId())) {
			return PCL;
		} else if (R_WESC.equals(gRoute.getRouteId())) {
			return BL;
		} else if (gRoute.getRouteId().startsWith(R_WE0)) {
			if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				return gRoute.getRouteShortName();
			}
		}
		System.out.printf("\nUnexpected route short name for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final Pattern POINT = Pattern.compile("((^|\\W){1}([\\w]{1})\\.(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String POINT_REPLACEMENT = "$2$3$4";

	private static final Pattern POINTS = Pattern.compile("((^|\\W){1}([\\w]+)\\.(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String POINTS_REPLACEMENT = "$2$3$4";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = POINT.matcher(routeLongName).replaceAll(POINT_REPLACEMENT);
		routeLongName = POINTS.matcher(routeLongName).replaceAll(POINTS_REPLACEMENT);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "00AAA0"; // GREEN (from PDF)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_A0C2E9 = "A0C2E9";
	private static final String COLOR_F8A08A = "F8A08A";
	private static final String COLOR_9E50AE = "9E50AE";
	private static final String COLOR_ED1C24 = "ED1C24";
	private static final String COLOR_A05843 = "A05843";
	private static final String COLOR_00A990 = "00A990";
	private static final String COLOR_2E3192 = "2E3192";
	private static final String COLOR_7B2178 = "7B2178";
	private static final String COLOR_19B5F1 = "19B5F1";
	private static final String COLOR_EC008C = "EC008C";
	private static final String COLOR_127BCA = "127BCA";
	private static final String COLOR_F7903F = "F7903F";
	private static final String COLOR_8CA2D7 = "8CA2D7";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (R_WE_NF.equals(gRoute.getRouteId())) {
			return null;
		} else if (R_WE_NOTL.equals(gRoute.getRouteId())) {
			return null;
		} else if (R_PC001.equals(gRoute.getRouteId())) {
			return COLOR_F8A08A; // COLOR_ED1C24;
		} else if (R_PC002.equals(gRoute.getRouteId())) {
			return COLOR_A0C2E9; // COLOR_127BCA;
		} else if (R_WEPC.equals(gRoute.getRouteId())) {
			return COLOR_9E50AE;
		} else if (R_WESC.equals(gRoute.getRouteId())) {
			return null;
		} else if (gRoute.getRouteId().startsWith(R_WE0)) {
			if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				int rsn = Integer.parseInt(gRoute.getRouteShortName());
				switch (rsn) {
				// @formatter:off
				case 1: return COLOR_ED1C24;
				case 2: return COLOR_A05843;
				case 3: return COLOR_00A990;
				case 4: return COLOR_2E3192;
				case 5: return COLOR_7B2178;
				case 6: return COLOR_19B5F1;
				case 8: return COLOR_EC008C;
				case 9: return COLOR_127BCA;
				case 10: return COLOR_F7903F; // "ED1C24";
				case 11: return COLOR_8CA2D7; // "2E3192";
				// @formatter:on
				}
			}

		}
		System.out.printf("\nUnexpected route color for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static final String ST_GEORGE_ROACH = "St George / Roach";
	private static final String WOODLAWN_S_PELHAM = "Woodlawn / S Pelham";
	private static final String SOUTHWORTH_GORDON = "Southworth / Gordon";
	private static final String SEAWAY_MALL = "Seaway Mall";
	private static final String NIAGARA_COLLEGE = "Niagara College";
	private static final String DOWNTOWN_TERMINAL = "Downtown Terminal";
	private static final String HOSPITAL = "Hospital";
	private static final String WELLAND = "Welland";
	private static final String PORT_COLBORNE = "Port Colborne";
	private static final String COLBORNE_MC_RAE = "Colborne / McRae";
	private static final String PC_MALL = "PC Mall";
	private static final String CITY_HALL = "City Hall";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_GEORGE_ROACH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "WE_405899TS01", "WE_426050TS01", "WE0" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE_416075TS01", "WE_405899TS01" })) //
				.compileBothTripSort());
		map2.put(2l, new RouteTripSpec(2l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "WE102", "WE51", "WE0" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE60", "WE102" })) //
				.compileBothTripSort());
		map2.put(3l, new RouteTripSpec(3l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE_416284TS01", "WE75" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE75", "WE_426241TS01", "WE0" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WOODLAWN_S_PELHAM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { "WE137", "WE4_25", "WE0" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE004_01", "WE137" })) //
				.compileBothTripSort());
		map2.put(6l, new RouteTripSpec(6l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWORTH_GORDON) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "WE203", "WE217", "WE0" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE25", "WE203" })) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE248", "WE75" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE75", "WE247", "WE0" })) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SEAWAY_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE254", "WE250" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE250", "WE257", "WE0" })) //
				.compileBothTripSort());
		map2.put(10l, new RouteTripSpec(10l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE248", "WE250", "WE75" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE75", "WE140", "WE0" })) //
				.compileBothTripSort());
		map2.put(11l, new RouteTripSpec(11l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOSPITAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "WE27", "WE188", "WE0" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE0", "WE7", "WE22", "WE27" })) //
				.compileBothTripSort());
		map2.put(R_WEPC_ID, new RouteTripSpec(R_WEPC_ID, // Port Colborne - Welland Link
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WELLAND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PORT_COLBORNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "PC_4", "WE400", "WE75" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "WE75", "WE0", "PC_4" })) //
				.compileBothTripSort());
		map2.put(R_PC001_ID, new RouteTripSpec(R_PC001_ID, // Port Colborne 1 East
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLBORNE_MC_RAE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "PC0", "PC105", "PC32" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "PC32", "PC38", "PC0" })) //
				.compileBothTripSort());
		map2.put(R_PC002_ID, new RouteTripSpec(R_PC002_ID, // Port Colborne 2 West
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PC_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { "PC0", "PC59", "PC64", })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { "PC64", "PC57", "PC0" })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		System.out.printf("\n%s: Unexpected compare early route!\n", routeId);
		System.exit(-1);
		return -1;
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		System.out.printf("\n%s: Unexpected split trip route!\n", mRoute.getId());
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		System.out.printf("\n%s: Unexptected split trip stop route!\n", mRoute.getId());
		System.exit(-1);
		return null;
	}


	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\n%s: Unexptected trip %s!", mRoute.getId(), gTrip);
		System.exit(-1);
		return;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = AND.matcher(tripHeadsign).replaceAll(AND_REPLACEMENT);
		tripHeadsign = POINT.matcher(tripHeadsign).replaceAll(POINT_REPLACEMENT);
		tripHeadsign = POINTS.matcher(tripHeadsign).replaceAll(POINTS_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern AND_NOT = Pattern.compile("(&)", Pattern.CASE_INSENSITIVE);
	private static final String AND_NOT_REPLACEMENT = "and";

	private static final Pattern AND = Pattern.compile("((^|\\W){1}(and)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AND_REPLACEMENT = "$2&$4";

	private static final Pattern AT = Pattern.compile(
			"((^|\\W){1}(across fr[\\.]?|after|at|before|between both|between|east of|in front of|north of|opp|south of|west of)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String AT_REPLACEMENT = "$2/$4";

	private static final Pattern AND_SLASH = Pattern.compile("((^|\\W){1}(&|@)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AND_SLASH_REPLACEMENT = "$2/$4";

	private static final Pattern ENDS_WITH = Pattern.compile("((&|/|\\-)[\\W]*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_FLAG_STOP = Pattern.compile("(^(flag stop \\- )+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern AVE = Pattern.compile("((^|\\W){1}(aven|avenu)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AVE_REPLACEMENT = "$2Avenue$4";

	private static final Pattern DRIV = Pattern.compile("((^|\\W){1}(driv)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String DRIV_REPLACEMENT = "$2Drive$4";

	private static final Pattern ST = Pattern.compile("((^|\\W){1}(stree|stre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ST_REPLACEMENT = "$2Street$4";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_FLAG_STOP.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = AVE.matcher(gStopName).replaceAll(AVE_REPLACEMENT);
		gStopName = DRIV.matcher(gStopName).replaceAll(DRIV_REPLACEMENT);
		gStopName = ST.matcher(gStopName).replaceAll(ST_REPLACEMENT);
		gStopName = AND_NOT.matcher(gStopName).replaceAll(AND_NOT_REPLACEMENT); // fix Alex&ra
		gStopName = AND.matcher(gStopName).replaceAll(AND_REPLACEMENT);
		gStopName = AND_SLASH.matcher(gStopName).replaceAll(AND_SLASH_REPLACEMENT);
		gStopName = AT.matcher(gStopName).replaceAll(AT_REPLACEMENT);
		gStopName = POINT.matcher(gStopName).replaceAll(POINT_REPLACEMENT);
		gStopName = POINTS.matcher(gStopName).replaceAll(POINTS_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = ENDS_WITH.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String ZERO_0 = "0";

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String WE42 = "We4_";
	private static final String WE4 = "WE4_";
	private static final String WE004 = "WE004_";
	private static final String PC2 = "PC_";
	private static final String PC = "PC";
	private static final String WE2 = "WE_";
	private static final String WE = "WE";

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			stopCode = gStop.getStopId();
		}
		if (Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		if (stopCode.startsWith(WE42)) {
			return 230000000 + Integer.parseInt(stopCode.substring(WE42.length()));
		} else if (stopCode.startsWith(WE4)) {
			return 231000000 + Integer.parseInt(stopCode.substring(WE4.length()));
		} else if (stopCode.startsWith(WE004)) {
			return 232000000 + Integer.parseInt(stopCode.substring(WE004.length()));
		}
		try {
			Matcher matcher = DIGITS.matcher(stopCode);
			if (matcher.find()) {
				int routeId = Integer.parseInt(matcher.group());
				if (stopCode.startsWith(NOTL)) {
					routeId += 14000000;
				} else if (stopCode.startsWith(PC2)) {
					routeId += 160000000;
				} else if (stopCode.startsWith(PC)) {
					routeId += 161000000;
				} else if (stopCode.startsWith(WE2)) {
					routeId += 233000000;
				} else if (stopCode.startsWith(WE)) {
					routeId += 234000000;
				} else {
					System.out.printf("\nUnexpected stop ID (starts with digits) %s!\n", gStop);
					System.exit(-1);
					routeId = -1;
				}
				return routeId;
			}
		} catch (Exception e) {
			System.out.printf("\nError while finding stop ID for %s!\n", gStop);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
		System.out.printf("\nUnexpected stop ID %s!\n", gStop);
		System.exit(-1);
		return -1;
	}

}
