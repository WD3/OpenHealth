/*
Copyright (C) 2008-2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Jose Antonio Santos Cadenas <jcaden@libresoft.es>
Author: Santiago Carot-Nemesio <scarot@libresoft.es>

This program is a (FLOS) free libre and open source implementation
of a multiplatform manager device written in java according to the
ISO/IEEE 11073-20601. Manager application is designed to work in
DalvikVM over android platform.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package es.libresoft.openhealth.error;

public class ErrorFactory {

	private static ErrorString errStr = null;

	/**
	 * Set the default error factory of the entire system
	 *
	 * @param platform target platform
	 */
	public static final void setDefaultErrorGenerator (ErrorString err) {
		errStr = err;
	}

	/**
	 * Get default error generator established for the current platform
	 *
	 * @return the storage for the specific platform
	 * @throws StorageException if there is no ConfigStorage set
	 */
	public static final ErrorString getDefaultErrorGenerator () throws ErrorException {
		if (errStr == null)
			throw new ErrorException("Default error factory is not set");
		return errStr;
	}
}
