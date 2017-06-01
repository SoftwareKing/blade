package com.blade.kit;

import com.blade.mvc.http.Request;
import com.blade.kit.ason.Ason;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * @author biezhi
 *         2017/5/31
 */
public class BladeKit {

    private static final Random random = new Random();

    public static String toJSONString(Object object) {
        return Ason.serialize(object).toString();
    }

    public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            assert inputChannel != null;
            inputChannel.close();
            assert outputChannel != null;
            outputChannel.close();
        }
    }

    public static int nowUnixTime() {
        return (int) System.currentTimeMillis() / 1000;
    }

    public static int getCurrentUnixTime() {
        return (int) System.currentTimeMillis() / 1000;
    }

    public static int rand(int min, int max) {
        return random.nextInt(max) % (max - min + 1) + min;
    }

    public static String flowAutoShow(int value) {
        int kb = 1024;
        int mb = 1048576;
        int gb = 1073741824;
        if (Math.abs(value) > gb) {
            return Math.round(value / gb) + "GB";
        } else if (Math.abs(value) > mb) {
            return Math.round(value / mb) + "MB";
        } else if (Math.abs(value) > kb) {
            return Math.round(value / kb) + "KB";
        }
        return Math.round(value) + "";
    }

    public static String md5(String str) {
        return EncrypKit.md5(str);
    }

    public static String md5(String str1, String str2) {
        return EncrypKit.md5(str1 + str2);
    }

    public static String sha1(String str) {
        return EncrypKit.encryptSHA1ToString(str);
    }

    public static String sha256(String str) {
        return EncrypKit.encryptSHA256ToString(str);
    }

    public static String sha512(String str) {
        return EncrypKit.encryptSHA512ToString(str);
    }

    public static String enAes(String data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return new BASE64Encoder().encode(encryptedBytes);
    }

    public static String deAes(String data, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] cipherTextBytes = new BASE64Decoder().decodeBuffer(data);
        byte[] decValue = cipher.doFinal(cipherTextBytes);
        return new String(decValue);
    }

    public static String readToString(String file) throws IOException {
        StringBuffer sbuf = new StringBuffer();
        BufferedReader crunchifyBufferReader = Files.newBufferedReader(Paths.get(file));
        List<String> crunchifyList = crunchifyBufferReader.lines().collect(Collectors.toList());
        crunchifyList.forEach(sbuf::append);
        return sbuf.toString();
    }

    /**
     * 根据request对象获取客户端ip地址
     *
     * @param request
     * @return
     */
    public static String ipAddr(Request request) {
        //ipAddress = this.getRequest().getRemoteAddr();
        Optional<String> ipAddress = request.header("x-forwarded-for");
        if (!ipAddress.isPresent() || "unknown".equalsIgnoreCase(ipAddress.get())) {
            ipAddress = request.header("Proxy-Client-IP");
        }
        if (!ipAddress.isPresent() || "unknown".equalsIgnoreCase(ipAddress.get())) {
            ipAddress = request.header("WL-Proxy-Client-IP");
        }
        if (!ipAddress.isPresent() || "unknown".equalsIgnoreCase(ipAddress.get())) {
            ipAddress = request.header("X-Real-IP");
        }
        if (!ipAddress.isPresent()) {
            ipAddress = Optional.of("127.0.0.1");
        }
        return ipAddress.get();
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isEmpty(Collection<?> c) {
        return null == c || c.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> c) {
        return null != c && !c.isEmpty();
    }

}
