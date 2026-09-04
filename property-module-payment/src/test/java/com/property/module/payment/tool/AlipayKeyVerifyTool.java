package com.property.module.payment.tool;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 支付宝密钥自检工具（纯 JDK，无支付宝 SDK 依赖）
 *
 * 用于排查「支付宝下单后跳转 /error 而非收银台」这类验签失败问题时，
 * 验证应用私钥格式是否正确、能否正常签名，并推导出配对的应用公钥。
 *
 * 读取环境变量（与 IDE 运行配置同名）：
 *   ALIPAY_MERCHANT_PRIVATE_KEY  应用私钥（PKCS8，裸 base64 或含 PEM 头均可）
 *   ALIPAY_ALIPAY_PUBLIC_KEY     支付宝公钥（X.509，裸 base64 或含 PEM 头均可）
 *
 * 也可通过命令行参数覆盖：java ... <应用私钥> <支付宝公钥>
 */
public class AlipayKeyVerifyTool {

    public static void main(String[] args) throws Exception {
        String privateKeyRaw = value(args, 0, System.getenv("ALIPAY_MERCHANT_PRIVATE_KEY"));
        String alipayPublicKeyRaw = value(args, 1, System.getenv("ALIPAY_ALIPAY_PUBLIC_KEY"));

        System.out.println("========== 支付宝密钥自检 ==========");

        // 1. 解析应用私钥
        PrivateKey privateKey = parsePrivateKey(privateKeyRaw);
        System.out.println("[1/6] 应用私钥解析成功，算法=" + privateKey.getAlgorithm());

        // 2. 由私钥推导配对的应用公钥
        PublicKey derivedPublic = derivePublicKey(privateKey);
        String derivedPublicBase64 = Base64.getEncoder().encodeToString(derivedPublic.getEncoded());
        System.out.println("[2/6] 由私钥推导出的【应用公钥】(base64)：");
        System.out.println(derivedPublicBase64);

        // 3. 签名 / 验签自检
        String testData = "property-management-key-check";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(testData.getBytes(StandardCharsets.UTF_8));
        byte[] sig = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(derivedPublic);
        verifier.update(testData.getBytes(StandardCharsets.UTF_8));
        boolean ok = verifier.verify(sig);
        System.out.println("[3/6] 私钥签名 -> 推导公钥验签: " + (ok ? "通过（私钥内部自洽）" : "失败"));

        // 4. 解析并检查支付宝公钥
        if (isBlank(alipayPublicKeyRaw)) {
            System.out.println("[4/6] 未提供 ALIPAY_ALIPAY_PUBLIC_KEY，跳过支付宝公钥检查");
        } else {
            PublicKey alipayPublic = parsePublicKey(alipayPublicKeyRaw);
            String alipayPublicBase64 = Base64.getEncoder().encodeToString(alipayPublic.getEncoded());
            System.out.println("[4/6] 支付宝公钥解析成功");
            // 5. 判断是否误填：支付宝公钥 与 应用公钥 相同时是错误
            boolean same = derivedPublicBase64.equals(alipayPublicBase64);
            if (same) {
                System.out.println("[5/6] ⚠️ 配置的「支付宝公钥」与「应用公钥」完全相同 —— 疑似把应用公钥误填成了支付宝公钥");
            } else {
                System.out.println("[5/6] 支付宝公钥与应用公钥不同（正常，两者本就应不同）");
            }
        }

        // 6. 提示核对方法
        System.out.println("[6/6] 核对：把上面【应用公钥】与支付宝开放平台沙箱应用里显示的「应用公钥」比对，一致才说明私钥配对");
        System.out.println("==================================");
    }

    private static PrivateKey parsePrivateKey(String raw) throws Exception {
        if (isBlank(raw)) {
            throw new IllegalStateException("缺少应用私钥（ALIPAY_MERCHANT_PRIVATE_KEY）");
        }
        byte[] keyBytes = Base64.getDecoder().decode(stripPem(raw));
        try {
            // PKCS8：-----BEGIN PRIVATE KEY-----
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "应用私钥解析失败：请确认是 PKCS8 格式（BEGIN PRIVATE KEY），而非 PKCS1（BEGIN RSA PRIVATE KEY）。原始异常: "
                            + e.getMessage());
        }
    }

    private static PublicKey parsePublicKey(String raw) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(stripPem(raw));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static PublicKey derivePublicKey(PrivateKey privateKey) throws Exception {
        if (!(privateKey instanceof RSAPrivateCrtKey)) {
            throw new IllegalStateException("私钥不是 RSA CRT 私钥，无法直接推导公钥");
        }
        RSAPrivateCrtKey crt = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec spec = new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent());
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String stripPem(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceAll("-----BEGIN [^-]*-----", "")
                .replaceAll("-----END [^-]*-----", "")
                .replaceAll("\\s", "");
    }

    private static String value(String[] args, int idx, String env) {
        if (args != null && args.length > idx && !isBlank(args[idx])) {
            return args[idx];
        }
        return env;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}