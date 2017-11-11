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
		if (!WELLAND_TRANSIT.equals(gRoute.getAgencyId()) //
				&& !"WE_F17Welland Transit".equals(gRoute.getAgencyId())) {
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
			case 100: return COLOR_9E50AE;
			case 101: return COLOR_F8A08A; // COLOR_ED1C24;
			case 102: return COLOR_A0C2E9; // COLOR_127BCA;
			case 214: return null;
			case 215: return null;
			// @formatter:on
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
	private static final String COMMUNITY_LIVING = "Community Living";
	private static final String HOSPITAL = "Hospital";
	private static final String WELLAND = "Welland";
	private static final String PORT_COLBORNE = "Port Colborne";
	private static final String COLBORNE_MC_RAE = "Colborne / McRae";
	private static final String PC_MALL = "PC Mall";
	private static final String CITY_HALL = "City Hall";

	private static final String STOP_ = "WE_F17";

	private static final String STOP_3005 = STOP_ + "Stop" + "3005";
	private static final String STOP_3015 = STOP_ + "Stop" + "3015";
	private static final String STOP_3031 = STOP_ + "Stop" + "3031";
	private static final String STOP_3051 = STOP_ + "Stop" + "3051";
	private static final String STOP_3066 = STOP_ + "Stop" + "3066";
	private static final String STOP_3071 = STOP_ + "Stop" + "3071";
	private static final String STOP_3076 = STOP_ + "Stop" + "3076";

	private static final String STOP_4004 = STOP_ + "Stop" + "4004";
	private static final String STOP_4010 = STOP_ + "Stop" + "4010";
	private static final String STOP_4029 = STOP_ + "Stop" + "4029";
	private static final String STOP_4046 = STOP_ + "Stop" + "4046";
	private static final String STOP_4047 = STOP_ + "Stop" + "4047";
	private static final String STOP_4061 = STOP_ + "Stop" + "4061";
	private static final String STOP_4077 = STOP_ + "Stop" + "4077";
	private static final String STOP_4078 = STOP_ + "Stop" + "4078";
	private static final String STOP_4102 = STOP_ + "Stop" + "4102";
	private static final String STOP_4146 = STOP_ + "Stop" + "4146";
	private static final String STOP_4165 = STOP_ + "Stop" + "4165";
	private static final String STOP_4181 = STOP_ + "Stop" + "4181";
	private static final String STOP_4191 = STOP_ + "Stop" + "4191";
	private static final String STOP_4111 = STOP_ + "Stop" + "4111";
	private static final String STOP_4198 = STOP_ + "Stop" + "4198";
	private static final String STOP_4205 = STOP_ + "Stop" + "4205";
	private static final String STOP_4224 = STOP_ + "Stop" + "4224";
	private static final String STOP_4227 = STOP_ + "Stop" + "4227";
	private static final String STOP_4238 = STOP_ + "Stop" + "4238";
	private static final String STOP_4249 = STOP_ + "Stop" + "4249";

	private static final String STOP_AQE = STOP_ + "AQE";
	private static final String STOP_BRU = STOP_ + "BRU";
	private static final String STOP_FIN = STOP_ + "FIN";
	private static final String STOP_KIO = STOP_ + "KIO";
	private static final String STOP_LIO = STOP_ + "LIO";
	private static final String STOP_NCC = STOP_ + "NCC";
	private static final String STOP_PCH = STOP_ + "PCH";
	private static final String STOP_SGR = STOP_ + "SGR";
	private static final String STOP_WOS = STOP_ + "WOS";
	private static final String STOP_WPC = STOP_ + "WPC";

	private static final String STOP_GLND_CMPS = STOP_ + "GlndCmps";
	private static final String STOP_SEWAY_MAL = STOP_ + "SewayMal";
	private static final String STOP_WELLAND = STOP_ + "Welland";
	private static final String STOP_WIND_CAMP = STOP_ + "WlndCamp";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1l, new RouteTripSpec(1l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, ST_GEORGE_ROACH) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						STOP_SGR, // St. George St & Roach Av
								STOP_4046, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_LIO, // ++
								STOP_SGR, // St. George St & Roach Av
						})) //
				.compileBothTripSort());
		map2.put(2l, new RouteTripSpec(2l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						STOP_NCC, // Niagara College Welland - Chippa
								STOP_4078, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4061, // ++
								STOP_NCC, // Niagara College Welland - Chippa
						})) //
				.compileBothTripSort());
		map2.put(3l, new RouteTripSpec(3l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_FIN, // ++
								STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
								STOP_4102, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, WOODLAWN_S_PELHAM) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WOS, // Woodlawn Rd & South Pelham Rd
								STOP_AQE, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4111, // ++
								STOP_WOS, // Woodlawn Rd & South Pelham Rd
						})) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COMMUNITY_LIVING) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WPC, // Welland Pelham Community Living
								STOP_4165, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4146, // ++
								STOP_WPC, // Welland Pelham Community Living
						})) //
				.compileBothTripSort());
		map2.put(6l, new RouteTripSpec(6l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SOUTHWORTH_GORDON) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_4191, // Gordon St & Southworth St
								STOP_4205, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_KIO, // ++
								STOP_4191, // Gordon St & Southworth St
						})) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4224, // ++
								STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
								STOP_4227, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, SEAWAY_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4238, // ++
								STOP_SEWAY_MAL, // Seaway Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_SEWAY_MAL, // Seaway Mall
								STOP_4249, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(10l, new RouteTripSpec(10l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4224, // ++
								STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
								STOP_4077, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.compileBothTripSort());
		map2.put(11l, new RouteTripSpec(11l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, DOWNTOWN_TERMINAL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, HOSPITAL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_4047, // King St & Fourth St #Hospital
								STOP_4198, // ++
								STOP_WELLAND, // Welland Bus Terminal
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WELLAND, // Welland Bus Terminal
								STOP_4029, // ++
								STOP_4047, // King St & Fourth St #Hospital
						})) //
				.compileBothTripSort());
		map2.put(215L, new RouteTripSpec(215L, // R_WE_NOTL Niagara-on-the-Lake Link
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WELLAND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NOTL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_GLND_CMPS, // 135 Taylor Rd N.O.T.L.
								STOP_4010, // ++
								STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
								STOP_4004, // ++
								STOP_GLND_CMPS, // 135 Taylor Rd N.O.T.L.
						})) //
				.compileBothTripSort());
		map2.put(214L, new RouteTripSpec(214L, // R_WESC Brock Link
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, NIAGARA_COLLEGE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Brock U") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_BRU, // Brock University
								STOP_4010, // ++
								STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
								STOP_4004, // ++
								STOP_BRU, // Brock University
						})) //
				.compileBothTripSort());
		map2.put(100L, new RouteTripSpec(100L, // Port Colborne - Welland Link
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, WELLAND, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PORT_COLBORNE) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_PCH, // Port Colborne City Hall
								STOP_3076, // ++
								STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_WIND_CAMP, // Woodlawn Road #NiagaraCollege
								STOP_4181, // ++
								STOP_PCH, // Port Colborne City Hall
						})) //
				.compileBothTripSort());
		map2.put(101L, new RouteTripSpec(101L, // Port Colborne 1 East
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, COLBORNE_MC_RAE, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_PCH, // Port Colborne City Hall
								STOP_3051, // ++
								STOP_3066, // Flag Stop - Colborne St & MacRae
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_3066, // Flag Stop - Colborne St & MacRae
								STOP_3071, // ++
								STOP_PCH, // Port Colborne City Hall
						})) //
				.compileBothTripSort());
		map2.put(102L, new RouteTripSpec(102L, // Port Colborne 2 West
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, PC_MALL, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, CITY_HALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_PCH, // Port Colborne City Hall
								STOP_3031, // ++
								STOP_3005, // Flag Stop - Port Colborne Mall
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						STOP_3005, // Flag Stop - Port Colborne Mall
								STOP_3015, // ++
								STOP_PCH, // Port Colborne City Hall
						})) //
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
		System.out.printf("\n%s: Unexpected trip to split %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
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
		stopCode = PRE_STOP_ID.matcher(stopCode).replaceAll(StringUtils.EMPTY);
		return stopCode;
	}

	private static final Pattern PRE_STOP_ID = Pattern.compile("(" //
			+ STOP_ + "Stop" //
			+ "|" //
			+ STOP_ //
			+ ")", //
			Pattern.CASE_INSENSITIVE);

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			stopCode = gStop.getStopId();
		}
		stopCode = PRE_STOP_ID.matcher(stopCode).replaceAll(StringUtils.EMPTY);
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
		} else if (stopCode.equals("LIP")) {
			return 9000012;
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
