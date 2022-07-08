/*
 * Copyright (C) 2017, Ulrich Wolffgang <ulrich.wolffgang@proleap.io>
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.openrewrite.cobol.internal.util;

public class FilenameUtils {

	public static String getBaseName(final String filename) {
		return removeExtension(getName(filename));
	}

	public static String getExtension(final String fileName) throws IllegalArgumentException {
		if (fileName == null) {
			return null;
		} else {
			final int index = indexOfExtension(fileName);
			return index == -1 ? "" : fileName.substring(index + 1);
		}
	}

	public static String getName(final String filename) {
		if (filename == null) {
			return null;
		} else {
			final int index = indexOfLastSeparator(filename);
			return filename.substring(index + 1);
		}
	}

	private static int indexOfExtension(final String filename) {
		if (filename == null) {
			return -1;
		} else {
			final int extensionPos = filename.lastIndexOf(46);
			final int lastSeparator = indexOfLastSeparator(filename);
			return lastSeparator > extensionPos ? -1 : extensionPos;
		}
	}

	private static int indexOfLastSeparator(final String filename) {
		if (filename == null) {
			return -1;
		} else {
			final int lastUnixPos = filename.lastIndexOf(47);
			final int lastWindowsPos = filename.lastIndexOf(92);
			return Math.max(lastUnixPos, lastWindowsPos);
		}
	}

	public static String removeExtension(final String filename) {
		if (filename == null) {
			return null;
		} else {
			final int index = indexOfExtension(filename);
			return index == -1 ? filename : filename.substring(0, index);
		}
	}
}
