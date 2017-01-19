package cn.hq.utils.codec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class HexUtils {

	public static byte[] decode(String hexString) {
		try {
			return Hex.decodeHex(hexString.toCharArray());
		} catch (DecoderException e) {
			throw new DataDecodeException(e.getMessage(), e);
		}
	}

	public static String encode(byte[] bytes) {
		return Hex.encodeHexString(bytes);
	}

}
