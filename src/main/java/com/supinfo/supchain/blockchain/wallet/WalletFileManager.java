package com.supinfo.supchain.blockchain.wallet;

import com.supinfo.supchain.enums.LogLevel;
import com.supinfo.supchain.helpers.CLogger;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class WalletFileManager {
    private static CLogger cLogger = new CLogger(WalletFileManager.class);

    public static void savePrivateKey(String path, PrivateKey privateKey) throws IOException {

        // Store Private Key.
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                privateKey.getEncoded());
        FileOutputStream fos = new FileOutputStream(path + "/private.key");
        fos.write(pkcs8EncodedKeySpec.getEncoded());
        fos.close();
    }

    public static PrivateKey loadPrivateKey(String path)
            throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchProviderException {

        // Read Private Key.
        File filePrivateKey = new File(path + "/private.key");
        FileInputStream fis = new FileInputStream(path + "/private.key");
        byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
        fis.read(encodedPrivateKey);
        fis.close();

        // Generate KeyPair.
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA","SC");

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                encodedPrivateKey);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

        return privateKey;
    }

    public static void dumpKeyPair(KeyPair keyPair) {
        PublicKey pub = keyPair.getPublic();
        cLogger.log(LogLevel.BASIC,"Public Key: " + getHexString(pub.getEncoded()));

        PrivateKey priv = keyPair.getPrivate();
        cLogger.log(LogLevel.BASIC,"Private Key: " + getHexString(priv.getEncoded()));
    }

    public static PublicKey derivePublicKey(PrivateKey privateKey) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "SC");
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("prime192v1");

        ECPoint Q = ecSpec.getG().multiply(((org.spongycastle.jce.interfaces.ECPrivateKey) privateKey).getD());

        ECPublicKeySpec pubSpec = new ECPublicKeySpec(Q, ecSpec);
        return keyFactory.generatePublic(pubSpec);
    }

    private static String getHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}
