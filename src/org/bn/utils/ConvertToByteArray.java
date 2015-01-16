package org.bn.utils;

public class ConvertToByteArray {
	public static byte[] getByteArray(String str) {
		byte[] result = new byte[str.length() / 2];
		byte[] raw = str.getBytes();
		for (int i = 0; i < result.length; i++) {
			byte temp1 = (byte) (getByte(raw[2 * i]) << 4);
			byte temp2 = getByte(raw[2 * i + 1]);
			result[i] = (byte) (temp1 | temp2);
		}
		return result;
	}

	public static byte getByte(Byte b) {
		if (b >= (byte) 'a' && b <= (byte) 'f')
			return (byte) (b - 'a' + 10);
		else if (b >= (byte) 'A' && b <= (byte) 'F')
			return (byte) (b - 'A' + 10);
		else
			return (byte) (b - '0');
	}
}
