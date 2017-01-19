package cn.hq.utils.security;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;

import cn.hq.utils.codec.Base58Utils;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
/**
 * 摘要工具类
 * @author hobo
 *
 */
public class ShaUtils {
	/**
	 * 组装待签名数据
	 * @param userAgent 客户端
	 * @param url 请求路径  http://bubi.com?param1=value1
	 * @param requestBody 请求体 可为空
	 * @return
	 */
	public static byte[] getData(byte[] userAgent,byte[] url,byte[] requestBody){
		
		byte[] bytes = new byte[userAgent.length + url.length + (requestBody == null ? 0 :requestBody.length)];
		System.arraycopy(userAgent, 0, bytes, 0, userAgent.length);
		System.arraycopy(url, 0, bytes, userAgent.length, url.length);
		if (requestBody != null) {
			System.arraycopy(requestBody, 0, bytes, userAgent.length+url.length, requestBody.length);
		}
		return bytes;
	}
	/**
	 * 获取摘要
	 * @param data
	 * @param privateKey
	 * @return
	 */
	public static byte[] sign_512(byte[] data, byte[] privateKey) {
		try {
			java.security.Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
			EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
			EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(privateKey, spec);
			PrivateKey sKey = new EdDSAPrivateKey(privateKeySpec);
			sgr.initSign(sKey);
			sgr.update(data);
			return sgr.sign();
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (SignatureException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	/**
	 * 验证摘要
	 * @param data
	 * @param pubKey
	 * @return
	 */
	public static boolean vervify(byte[] data, byte[] pubKeyByte,byte[] signature){
		try {
			java.security.Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
			EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
			EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(pubKeyByte, spec);
			EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);
			sgr.initVerify(pubKey);
			sgr.update(data);
			return sgr.verify(signature);
		} catch (InvalidKeyException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (SignatureException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	/**
	 * 将base58编码的私钥转换为非对称加密算法需要的字节数组
	 * @param base58PrivKeyStr
	 * @return
	 */
	public static byte[] getPriKeyBytes(String base58PrivKeyStr){
		byte[] base58PrivKey = Base58Utils.decode(base58PrivKeyStr);
		if (base58PrivKey == null || base58PrivKey.length != 41){
			throw new RuntimeException("私钥不正确！");
		}
		byte[] privArr = new byte[32];
		System.arraycopy(base58PrivKey, 4, privArr, 0, privArr.length);
		return privArr;
	}
}
