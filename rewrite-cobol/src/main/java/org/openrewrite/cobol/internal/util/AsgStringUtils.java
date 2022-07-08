/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.util;

import java.math.BigDecimal;

public class AsgStringUtils {

	public static boolean isBigDecimal(final String str) {
		try {
			new BigDecimal(str);
		} catch (final NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	public static boolean isBoolean(final String str) {
		final Boolean b = Boolean.parseBoolean(str);
		return b != null;
	}

	public static boolean isDouble(final String str) {
		try {
			Double.parseDouble(str);
		} catch (final NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	public static boolean isFloat(final String str) {
		try {
			Float.parseFloat(str);
		} catch (final NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	public static boolean isInteger(final String str) {
		try {
			Integer.parseInt(str);
		} catch (final NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	public static boolean isLong(final String str) {
		try {
			Long.parseLong(str);
		} catch (final NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	public static BigDecimal parseBigDecimal(final String str) {
		try {
			return new BigDecimal(str);
		} catch (final NumberFormatException nfe) {
			return null;
		}
	}

	public static Boolean parseBoolean(final String str) {
		return Boolean.parseBoolean(str);
	}

	public static Double parseDouble(final String str) {
		try {
			return Double.parseDouble(str);
		} catch (final NumberFormatException nfe) {
			return null;
		}
	}

	public static Float parseFloat(final String str) {
		try {
			return Float.parseFloat(str);
		} catch (final NumberFormatException nfe) {
			return null;
		}
	}

	public static Integer parseInteger(final String str) {
		try {
			return Integer.parseInt(str);
		} catch (final NumberFormatException nfe) {
			return null;
		}
	}

	public static Long parseLong(final String str) {
		try {
			return Long.parseLong(str);
		} catch (final NumberFormatException nfe) {
			return null;
		}
	}
}
