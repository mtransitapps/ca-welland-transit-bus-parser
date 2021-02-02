package org.mtransit.parser.ca_welland_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://www.niagararegion.ca/government/opendata/data-set.aspx#id=32
// http://www.niagararegion.ca/government/opendata/data-set.aspx#id=32&tab=data_table&f=xml&r=500&p=1
// http://maps-dev.niagararegion.ca/GoogleTransit/NiagaraRegionTransit.zip
// https://www.niagararegion.ca/downloads/transit/NiagaraRegionTransit.zip
public class WellandTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-welland-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WellandTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Welland Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Welland Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection deprecation
		final String agencyId = gRoute.getAgencyId();
		if (!agencyId.startsWith("WE_") //
				&& !agencyId.startsWith("Wel_") //
				&& !agencyId.startsWith("WEL_") //
				&& !agencyId.contains("AllNRT_")) {
			return true; // exclude
		}
		if (agencyId.contains("AllNRT_")) {
			if (!Utils.isDigitsOnly(gRoute.getRouteShortName())) {
				return true; // exclude
			}
			final int rsn = Integer.parseInt(gRoute.getRouteShortName());
			if (rsn < 500 || rsn > 799) { // includes Port Colborne for now
				return true; // exclude
			}
		}
		if (gRoute.getRouteLongName().startsWith("NRT - ")) {
			return true; // exclude Niagara Region Transit buses
		}
		return super.excludeRoute(gRoute);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName()); // using route short name as route ID
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	private static final String NOTL = "NOTL";
	private static final String PC1EAST = "PC1";
	private static final String PC2WEST = "PC2";
	private static final String PCL = "PCL";
	private static final String BL = "BL";

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if ("NOTL Link".equals(gRoute.getRouteLongName())) {
			return NOTL;
		} else if ("Port Colborne East Side - Rt. 1".equals(gRoute.getRouteLongName())) {
			return PC1EAST;
		} else if ("Port Colborne West Side - Rt 2".equals(gRoute.getRouteLongName())) {
			return PC2WEST;
		} else if ("Port Colborne Link".equals(gRoute.getRouteLongName())) {
			return PCL;
		} else //noinspection deprecation
			if ("College-Brock".equals(gRoute.getRouteId())) {
				return BL;
			} else {
				if (gRoute.getRouteShortName() != null && gRoute.getRouteShortName().length() > 0 && Utils.isDigitsOnly(gRoute.getRouteShortName())) {
					return gRoute.getRouteShortName();
				}
			}
		throw new MTLog.Fatal("Unexpected route short name for %s!", gRoute);
	}

	private static final Pattern POINT = Pattern.compile("((^|\\W)([\\w])\\.(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String POINT_REPLACEMENT = "$2" + "$3" + "$4";

	private static final Pattern POINTS = Pattern.compile("((^|\\W)([\\w]+)\\.(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String POINTS_REPLACEMENT = "$2" + "$3" + "$4";

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = POINT.matcher(routeLongName).replaceAll(POINT_REPLACEMENT);
		routeLongName = POINTS.matcher(routeLongName).replaceAll(POINTS_REPLACEMENT);
		routeLongName = CleanUtils.cleanNumbers(routeLongName);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "00AAA0"; // GREEN (from PDF)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (gRoute.getRouteShortName() != null
				&& gRoute.getRouteShortName().length() > 0
				&& Utils.isDigitsOnly(gRoute.getRouteShortName())) {
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
			case 599: return null; // TODO
			case 510: return "ED1C24";
			case 511: return "2E3192";
			case 701: return "ED1C24";
			case 702: return "127BCA";
			// @formatter:on
			}
		}
		throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
	}

	private static final Pattern STARTS_WITH_WE_A00_ = Pattern.compile(
			"((^)((allnrt|wel|we)_[a-z]{1,3}[\\d]{2,4}(_)?(stop)?))",
			Pattern.CASE_INSENSITIVE
	);

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = STARTS_WITH_WE_A00_.matcher(gStopId).replaceAll(EMPTY);
		return gStopId;
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsign()),
				gTrip.getDirectionId()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern ENDS_WITH = Pattern.compile("(([&/\\-])[\\W]*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_FLAG_STOP = Pattern.compile("(^(flag stop - )+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern FIX_AVENUE = Pattern.compile("((^|\\W)(aven|avenu)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_AVENUE_REPLACEMENT = "$2" + "Avenue" + "$4";

	private static final Pattern FIX_DRIVE = Pattern.compile("((^|\\W)(driv)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_DRIVE_REPLACEMENT = "$2" + "Drive" + "$4";

	private static final Pattern FIX_STREET = Pattern.compile("((^|\\W)(stree|stre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String FIX_STREET_REPLACEMENT = "$2" + "Street" + "$4";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName);
		gStopName = STARTS_WITH_FLAG_STOP.matcher(gStopName).replaceAll(EMPTY);
		gStopName = FIX_AVENUE.matcher(gStopName).replaceAll(FIX_AVENUE_REPLACEMENT);
		gStopName = FIX_DRIVE.matcher(gStopName).replaceAll(FIX_DRIVE_REPLACEMENT);
		gStopName = FIX_STREET.matcher(gStopName).replaceAll(FIX_STREET_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = POINT.matcher(gStopName).replaceAll(POINT_REPLACEMENT);
		gStopName = POINTS.matcher(gStopName).replaceAll(POINTS_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = ENDS_WITH.matcher(gStopName).replaceAll(EMPTY);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String ZERO_0 = "0";

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String PC2 = "PC_";
	private static final String PC = "PC";
	private static final String WE2 = "WE_";
	private static final String WE = "WE";

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			//noinspection deprecation
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WE_A00_.matcher(stopCode).replaceAll(EMPTY);
		return stopCode;
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		if (true) {
			//noinspection deprecation
			String stopId = gStop.getStopId();
			stopId = STARTS_WITH_WE_A00_.matcher(stopId).replaceAll(EMPTY);
			if (stopId.isEmpty()) {
				throw new MTLog.Fatal("Unexpected stop ID (%d) %s!", stopId, gStop.toStringPlus());
			}
			if (Utils.isDigitsOnly(stopId)) {
				return Integer.parseInt(stopId);
			}
			throw new MTLog.Fatal("Unexpected stop ID %s!", gStop.toStringPlus());
		}
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.length() == 0 || ZERO_0.equals(stopCode)) {
			//noinspection deprecation
			stopCode = gStop.getStopId();
		}
		stopCode = STARTS_WITH_WE_A00_.matcher(stopCode).replaceAll(EMPTY);
		if (Utils.isDigitsOnly(stopCode)) {
			return Integer.parseInt(stopCode); // using stop code as stop ID
		}
		switch (stopCode) {
		case "AQE":
			return 9_000_000;
		case "BRC":
			return 9_000_001;
		case "BRU":
			return 9_000_002;
		case "EMW":
			return 9_000_003;
		case "FIF":
			return 9_000_004;
		case "FIN":
			return 9_000_005;
		case "FIO":
			return 9_000_006;
		case "GlndCmps":
			return 9_000_007;
		case "HAW":
			return 9_000_008;
		case "HOW":
			return 9_000_009;
		case "KIO":
			return 9_000_010;
		case "LIO":
			return 9_000_011;
		case "NCC":
			return 9_000_012;
		case "NIE":
			return 9_000_013;
		case "ONC":
			return 9_000_014;
		case "ONS":
			return 9_000_015;
		case "PCH":
			return 9_000_016;
		case "PRW":
			return 9_000_017;
		case "SewayMal":
			return 9_000_018;
		case "SGR":
			return 9_000_019;
		case "SPM":
			return 9_000_020;
		case "THB":
			return 9_000_021;
		case "THN":
			return 9_000_022;
		case "Welland":
			return 9_000_023;
		case "WlndCamp":
			return 9_000_024;
		case "WOR":
			return 9_000_025;
		case "WOS":
			return 9_000_026;
		case "WPC":
			return 9_000_027;
		case "WWA":
			return 9_000_028;
		case "LIP":
			return 9_000_029;
		case "XXXX":
			return 9_000_030;
		}
		try {
			Matcher matcher = DIGITS.matcher(stopCode);
			if (matcher.find()) {
				int routeId = Integer.parseInt(matcher.group());
				if (stopCode.startsWith(NOTL)) {
					routeId += 14_000_000;
				} else if (stopCode.startsWith(PC2)) {
					routeId += 160_000_000;
				} else if (stopCode.startsWith(PC)) {
					routeId += 161_000_000;
				} else if (stopCode.startsWith(WE2)) {
					routeId += 233_000_000;
				} else if (stopCode.startsWith(WE)) {
					routeId += 234_000_000;
				} else {
					throw new MTLog.Fatal("Unexpected stop ID (starts with digits) %s!", gStop.toStringPlus());
				}
				return routeId;
			}
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while finding stop ID for %s!", gStop.toStringPlus());
		}
		throw new MTLog.Fatal("Unexpected stop ID %s!", gStop.toStringPlus());
	}
}
