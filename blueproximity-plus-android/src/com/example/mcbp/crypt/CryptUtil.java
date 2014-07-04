package com.example.mcbp.crypt;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class CryptUtil {

	// Debugging
    private static final String TAG = "CryptUtil";
    private static final boolean D = true;
    private static final String ENCODING = "UTF-8";
    
    /**
     * Encode the plain text to given type of HMAC 
     * (Hash-based Message Authentication Code) string.
     * The default algorithm is hmacsha256.
     * 
     * @param text the plain text to be encoded
     * @param key the secret key string
     * @param type the HMAC algorithm name from 
     * {hmacmd5, hmacsha1, hmacsha256, hmacsha384, hmacsha512}
     * 
     * @return the encoded string
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static String hmac(byte[] text, byte[] key, String type) throws 
      UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
      String alg = type;
      if (type == null || type == "") {
        alg = "hmacsha256";
      }
      SecretKeySpec secretKey = new SecretKeySpec(key, alg);
      Mac hmac = Mac.getInstance(alg);
      hmac.init(secretKey);

      byte[] digest = hmac.doFinal(text);

      return toHexString(digest);
    }
    
    /**
     * Gets the hmacsha256 hex string of given text.
     * @param text plain text string
     * @param key key string
     * @return hmacsha256 hex string
     */
    public static String hmac(String text, String key){
    	String ret = text;
    	try {
			ret = hmac(text.getBytes(ENCODING), key.getBytes(ENCODING), "hmacsha256");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			ret = text;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			ret = text;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			ret = text;
		}
    	return ret;
    }
    
    /**
     * Converts byte array to hex string
     * 
     * @param bytes the byte array
     * @return the converted hex string
     */
    private static String toHexString(byte[] bytes) {
      StringBuilder hexString = new StringBuilder();
      for (byte mDigest : bytes) {
        String h = Integer.toHexString(0xFF & mDigest);
        while (h.length() < 2){
          h = "0" + h;
        }
        hexString.append(h);
      }
      return hexString.toString();
    }
    
}
