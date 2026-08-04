package com.vincent.msyep.modules.zone;

import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Franchise commercial terms derived from the chosen membership tier:
 * the joining amount, the territory granted, and the 2-year validity window.
 * Applied on both zone registration and edit so the certificate/MOU always match.
 */
public final class FranchiseTerms {

    private FranchiseTerms() {}

    /** Non-refundable joining amount per tier. */
    public static final Map<String, Integer> TIER_AMOUNT = Map.of(
            "Silver", 75000, "Gold", 100000, "Platinum", 125000, "Diamond", 150000);

    /** Territory granted per tier: Silver→Hobli, Gold→Taluk, Platinum→3 Taluks, Diamond→District. */
    public static final Map<String, String> TIER_TERRITORY = Map.of(
            "Silver", "1 Hobli",
            "Gold", "1 Taluk",
            "Platinum", "3 Taluks",
            "Diamond", "1 District");

    /** Franchise validity: two years from the issue date. */
    public static final int VALIDITY_YEARS = 2;

    /** Fill amount, territory and valid-till from the tier + issue date (leaves unknowns null). */
    public static void apply(Zone z) {
        if (z == null) return;
        String tier = z.getMembershipTier();
        if (StringUtils.hasText(tier)) {
            if (TIER_AMOUNT.containsKey(tier)) z.setMembershipAmount(TIER_AMOUNT.get(tier));
            z.setTerritory(TIER_TERRITORY.get(tier));
        }
        z.setValidTill(validTillFrom(z.getIssueDate()));
    }

    /** issueDate (ISO yyyy-MM-dd) + 2 years, or null if the date is missing/unparseable. */
    public static String validTillFrom(String issueDate) {
        if (!StringUtils.hasText(issueDate)) return null;
        try {
            return LocalDate.parse(issueDate.trim()).plusYears(VALIDITY_YEARS).toString();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
