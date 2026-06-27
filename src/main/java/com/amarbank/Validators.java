package com.amarbank;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralised input validation built on {@link Pattern} and {@link Matcher},
 * as required by Section 2 of the assignment.
 *
 * <p>Every rule is exposed both as a {@code isValid...} test (used by the GUI
 * to decide whether to show a {@code JOptionPane} alert) and reused by the
 * controller so the rules are enforced in exactly one place.</p>
 */
public final class Validators {

    /**
     * Phone number: a valid 11-digit Bangladeshi mobile number,
     * e.g. {@code 01712345678}. ({@code 01} + a digit in 3-9 + 8 digits.)
     */
    public static final Pattern PHONE = Pattern.compile("^01[3-9]\\d{8}$");

    /**
     * Account number: strict format {@code AMB-XXXXX} where X is exactly
     * 5 digits, e.g. {@code AMB-55412}.
     */
    public static final Pattern ACCOUNT_NUMBER = Pattern.compile("^AMB-\\d{5}$");

    /**
     * Account holder name: upper-case letters and spaces only, between 2 and
     * 50 characters long.
     */
    public static final Pattern HOLDER_NAME = Pattern.compile("^[A-Z ]{2,50}$");

    /** Branch name: letters and spaces only. */
    public static final Pattern BRANCH_NAME = Pattern.compile("^[A-Za-z ]+$");

    private Validators() {
    }

    public static boolean isValidPhone(String value) {
        return matches(PHONE, value);
    }

    public static boolean isValidAccountNumber(String value) {
        return matches(ACCOUNT_NUMBER, value);
    }

    public static boolean isValidHolderName(String value) {
        return matches(HOLDER_NAME, value);
    }

    public static boolean isValidBranchName(String value) {
        return matches(BRANCH_NAME, value);
    }

    /** True when {@code value} fully matches {@code pattern}. Null-safe. */
    private static boolean matches(Pattern pattern, String value) {
        if (value == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    // ---------- human-readable rules (shown in JOptionPane alerts) ----------

    public static final String PHONE_RULE =
            "Phone number must be a valid 11-digit number (e.g. 01712345678).";
    public static final String ACCOUNT_NUMBER_RULE =
            "Account number must follow the format AMB-XXXXX (e.g. AMB-55412).";
    public static final String HOLDER_NAME_RULE =
            "Account holder name must use UPPER CASE letters and spaces only (2-50 characters).";
    public static final String BRANCH_NAME_RULE =
            "Branch name must contain letters and spaces only.";
}
