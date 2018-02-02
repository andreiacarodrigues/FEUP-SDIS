package peer.message;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor
{
    private static byte[] key = { 0x74, 0x68, 0x69, 0x73, 0x49, 0x73, 0x41, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4b, 0x65, 0x79 };
	
	public static byte[] encryptBytesAndBase64Encode(byte[] bytes)
	{
		byte[] encryptedBytes = null;
		
		try
		{
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
	        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
	        encryptedBytes = Base64.getEncoder().encode(cipher.doFinal(bytes));
		}
		catch(Exception e)
		{
			System.out.println("Error encrypting");
			System.exit(-1);
		}
	    
		return encryptedBytes;      
	}

	public static byte[] base64decodeAndDecryptBytes(byte[] base64EncodedEncryptedBytes)
	{
		byte[] decryptedBytes = null;
		
		System.out.println("ENCRYPTED RECEIVED :");
		System.out.println(base64EncodedEncryptedBytes);
		
		try
		{
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
			SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(base64EncodedEncryptedBytes));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Error decrypting");
			System.exit(-1);
		}
		
		return decryptedBytes;
	}
}
