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
// https://www.niagararegion.ca/downloads/transit/NiagaraRegionTransit.zip
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
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating Welland Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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

	private static final String WE_ = "WE_";

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!gRoute.getAgencyId().startsWith(WE_)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final String NOTL = "NOTL";
	private static final String PC1EAST = "PC1";
	private static final String PC2WEST = "PC2";
	private static final String PCL = "PCL";
	private static final String BL = "BL";

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if ("NOTL Link".equals(gRoute.getRouteLongName())) {
			return NOTL;
		} else if ("Port Colborne East Side - Rt. 1".equals(gRoute.getRouteLongName())) {
			return PC1EAST;
		} else if ("Port Colborne West Side - Rt 2".equals(gRoute.getRouteLongName())) {
			return PC2WEST;
		} else if ("Port Colborne Link".equals(gRoute.getRouteLongName())) {
			return PCL;
		} else if ("College-Brock".equals(gRoute.getRouteId())) {
			return BL;
		} else {
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

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 23: return "2B6ABC";
			case 25: return "9E50AE";
			case 34: return "2B6ABC";
			case 501: return "ED1C24";
			case 502: return "A05843";
			case 503: return "00A990";
			case 504: return "2E3192";
			case 505: return "7B2178";
			case 506: return "19B5F1";
			case 508: return "EC008C";
			case 509: return "127BCA";
			case 510: return "ED1C24";
			case 511: return "2E3192";
			case 701: return "ED1C24";
			case 702: return "127BCA";
			// @formatter:on
			}
		}
		System.out.printf("\nUnexpected route color for %s!\n", gRoute);
		System.exit(-1);
		return null;
	}


	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(23L, new RouteTripSpec(23L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Niagara College", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Brock U") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"BRU", // Brock University
								"4010", // ++
								"WlndCamp", // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"WlndCamp", // Woodlawn Road #NiagaraCollege
								"4004", // ++
								"BRU", // Brock University
						})) //
				.compileBothTripSort());
		map2.put(25L, new RouteTripSpec(25L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Welland", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Port Colborne") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"PCH", // Port Colborne City Hall
								"3076", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4181", // ++
								"PCH", // Port Colborne City Hall
						})) //
				.compileBothTripSort());
		map2.put(34L, new RouteTripSpec(34L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Welland", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "NOTL") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"GlndCmps", // 135 Taylor Rd N.O.T.L.
								"4010", // ++
								"WlndCamp", // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"WlndCamp", // Woodlawn Road #NiagaraCollege
								"4004", // ++
								"GlndCmps", // 135 Taylor Rd N.O.T.L.
						})) //
				.compileBothTripSort());
		map2.put(501L, new RouteTripSpec(501L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "St George / Roach") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"SGR", // St. George St & Roach Av
								"4046", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"LIO", // ++
								"SGR", // St. George St & Roach Av
						})) //
				.compileBothTripSort());
		map2.put(502L, new RouteTripSpec(502L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Niagara College") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"NCC", // Niagara College Welland - Chippa
								"4078", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4061", // ++
								"NCC", // Niagara College Welland - Chippa
						})) //
				.compileBothTripSort());
		map2.put(503L, new RouteTripSpec(503L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Niagara College", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"FIN", // ++
								"WlndCamp", // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"WlndCamp", // Woodlawn Road #NiagaraCollege
								"4102", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(504L, new RouteTripSpec(504L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Woodlawn / S Pelham") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"WOS", // Woodlawn Rd & South Pelham Rd
								"AQE", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4111", // ++
								"WOS", // Woodlawn Rd & South Pelham Rd
						})) //
				.compileBothTripSort());
		map2.put(505L, new RouteTripSpec(505L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Community Living") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"WPC", // Welland Pelham Community Living
								"4165", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4146", // ++
								"WPC", // Welland Pelham Community Living
						})) //
				.compileBothTripSort());
		map2.put(506L, new RouteTripSpec(506L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Southworth / Gordon") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4191", // Gordon St & Southworth St
								"4205", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"KIO", // ++
								"4191", // Gordon St & Southworth St
						})) //
				.compileBothTripSort());
		map2.put(508L, new RouteTripSpec(508L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Niagara College", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4224", // ++
								"WlndCamp", // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"WlndCamp", // Woodlawn Road #NiagaraCollege
								"4227", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(509L, new RouteTripSpec(509L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Seaway Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4238", // ++
								"SewayMal", // Seaway Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"SewayMal", // Seaway Mall
								"4249", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(510L, new RouteTripSpec(510L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Niagara College", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4224", // ++
								"WlndCamp", // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"WlndCamp", // Woodlawn Road #NiagaraCollege
								"4077", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(511L, new RouteTripSpec(511L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown Terminal", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Hospital") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"4047", // King St & Fourth St #Hospital
								"4198", // ++
								"Welland", // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Welland", // Welland Bus Terminal
								"4029", // ++
								"4047", // King St & Fourth St #Hospital
						})) //
				.compileBothTripSort());
		map2.put(701L, new RouteTripSpec(701L, // Port Colborne 1 East
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Colborne / McRae", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "City Hall") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"PCH", // Port Colborne City Hall
								"3051", // ++
								"3066", // Flag Stop - Colborne St & MacRae
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"3066", // Flag Stop - Colborne St & MacRae
								"3071", // ++
								"PCH", // Port Colborne City Hall
						})) //
				.compileBothTripSort());
		map2.put(702L, new RouteTripSpec(702L, // Port Colborne 2 West
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "PC Mall", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "City Hall") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"PCH", // Port Colborne City Hall
								"3031", // ++
								"3005", // Flag Stop - Port Colborne Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"3005", // Flag Stop - Port Colborne Mall
								"3015", // ++
								"PCH", // Port Colborne City Hall
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	public static final Pattern STARTS_WITH_WE_A00_ = Pattern.compile("((^){1}(we\\_[A-Z]{1}[\\d]{2}\\_(stop)?))", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopOriginalId(String gStopId) {
		gStopId = STARTS_WITH_WE_A00_.matcher(gStopId).replaceAll(StringUtils.EMPTY);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
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
		System.out.printf("\n%s: Unexpected trip to split %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		System.out.printf("\n%s: Unexpected trip to split %s!\n", mRoute.getId(), gTrip);
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

	private static final String PC2 = "PC_";
	private static final String PC = "PC";
	private static final String WE2 = "WE_";
	private static final String WE = "WE";

	@Override
	public String getStopCode(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WE_A00_.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		return stopCode;
	}

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WE_A00_.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		if (Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		if (stopCode.equals("AQE")) {
			return 9000000;
		} else if (stopCode.equals("BRC")) {
			return 9000001;
		} else if (stopCode.equals("BRU")) {
			return 9000002;
		} else if (stopCode.equals("EMW")) {
			return 9000003;
		} else if (stopCode.equals("FIF")) {
			return 9000004;
		} else if (stopCode.equals("FIN")) {
			return 9000005;
		} else if (stopCode.equals("FIO")) {
			return 9000006;
		} else if (stopCode.equals("GlndCmps")) {
			return 9000007;
		} else if (stopCode.equals("HAW")) {
			return 9000008;
		} else if (stopCode.equals("HOW")) {
			return 9000009;
		} else if (stopCode.equals("KIO")) {
			return 9000010;
		} else if (stopCode.equals("LIO")) {
			return 9000011;
		} else if (stopCode.equals("NCC")) {
			return 9000012;
		} else if (stopCode.equals("NIE")) {
			return 9000013;
		} else if (stopCode.equals("ONC")) {
			return 9000014;
		} else if (stopCode.equals("ONS")) {
			return 9000015;
		} else if (stopCode.equals("PCH")) {
			return 9000016;
		} else if (stopCode.equals("PRW")) {
			return 9000017;
		} else if (stopCode.equals("SewayMal")) {
			return 9000018;
		} else if (stopCode.equals("SGR")) {
			return 9000019;
		} else if (stopCode.equals("SPM")) {
			return 9000020;
		} else if (stopCode.equals("THB")) {
			return 9000021;
		} else if (stopCode.equals("THN")) {
			return 9000022;
		} else if (stopCode.equals("Welland")) {
			return 9000023;
		} else if (stopCode.equals("WlndCamp")) {
			return 9000024;
		} else if (stopCode.equals("WOR")) {
			return 9000025;
		} else if (stopCode.equals("WOS")) {
			return 9000026;
		} else if (stopCode.equals("WPC")) {
			return 9000027;
		} else if (stopCode.equals("WWA")) {
			return 9000028;
		} else if (stopCode.equals("LIP")) {
			return 9000029;
		} else if (stopCode.equals("XXXX")) {
			return 9000030;
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
