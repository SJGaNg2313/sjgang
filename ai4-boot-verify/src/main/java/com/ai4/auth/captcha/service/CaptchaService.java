package com.ai4.auth.captcha.service;

import com.ai4.auth.captcha.config.CaptchaProperties;
import com.ai4.auth.captcha.dto.CaptchaResponse;
import com.ai4.common.exception.BizException;
import com.ai4.common.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 生成和校验本地简单图形验证码。 */
@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "ai4:auth:captcha:";
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 4;
    private static final int IMAGE_WIDTH = 120;
    private static final int IMAGE_HEIGHT = 40;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('get', KEYS[1]); "
                    + "if value then redis.call('del', KEYS[1]); end; "
                    + "return value",
            String.class
    );

    private final CaptchaProperties properties;
    private final StringRedisTemplate redisTemplate;

    public CaptchaService(
            CaptchaProperties properties,
            StringRedisTemplate redisTemplate
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    /** 生成新的验证码图片，并把答案保存到 Redis。 */
    public CaptchaResponse generate() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode();
        redisTemplate.opsForValue().set(
                key(captchaId),
                code,
                Duration.ofSeconds(expireSeconds())
        );
        try {
            return new CaptchaResponse(captchaId, renderImage(code));
        } catch (IOException exception) {
            redisTemplate.delete(key(captchaId));
            throw new IllegalStateException("验证码图片生成失败", exception);
        }
    }

    /** 校验并消费验证码；无论校验成功还是失败，验证码都只能使用一次。 */
    public void verify(String captchaId, String captchaCode) {
        if (!properties.enabled()) {
            return;
        }
        if (!isValidId(captchaId) || captchaCode == null || captchaCode.isBlank()) {
            throw new BizException(ErrorCode.CAPTCHA_REQUIRED);
        }

        String expectedCode = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(key(captchaId))
        );
        String actualCode = captchaCode.trim().toUpperCase(Locale.ROOT);
        if (expectedCode == null || !MessageDigest.isEqual(
                expectedCode.getBytes(StandardCharsets.US_ASCII),
                actualCode.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new BizException(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private String renderImage(String code) throws IOException {
        BufferedImage image = new BufferedImage(
                IMAGE_WIDTH,
                IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            graphics.setColor(new Color(248, 250, 252));
            graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            drawNoiseLines(graphics);
            drawCharacters(graphics, code);
            drawNoiseDots(graphics);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private void drawNoiseLines(Graphics2D graphics) {
        graphics.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < 5; i++) {
            graphics.setColor(new Color(
                    170 + RANDOM.nextInt(50),
                    170 + RANDOM.nextInt(50),
                    170 + RANDOM.nextInt(50)
            ));
            graphics.drawLine(
                    RANDOM.nextInt(IMAGE_WIDTH),
                    RANDOM.nextInt(IMAGE_HEIGHT),
                    RANDOM.nextInt(IMAGE_WIDTH),
                    RANDOM.nextInt(IMAGE_HEIGHT)
            );
        }
    }

    private void drawCharacters(Graphics2D graphics, String code) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        int charWidth = IMAGE_WIDTH / (CODE_LENGTH + 1);
        for (int i = 0; i < code.length(); i++) {
            int x = 11 + i * charWidth;
            int y = 29 + RANDOM.nextInt(5) - 2;
            AffineTransform originalTransform = graphics.getTransform();
            graphics.setColor(new Color(
                    30 + RANDOM.nextInt(80),
                    60 + RANDOM.nextInt(80),
                    110 + RANDOM.nextInt(80)
            ));
            graphics.rotate((RANDOM.nextDouble() - 0.5) * 0.35, x, y);
            graphics.drawString(String.valueOf(code.charAt(i)), x, y);
            graphics.setTransform(originalTransform);
        }
    }

    private void drawNoiseDots(Graphics2D graphics) {
        for (int i = 0; i < 35; i++) {
            graphics.setColor(new Color(
                    RANDOM.nextInt(180),
                    RANDOM.nextInt(180),
                    RANDOM.nextInt(180)
            ));
            int x = RANDOM.nextInt(IMAGE_WIDTH);
            int y = RANDOM.nextInt(IMAGE_HEIGHT);
            graphics.fillRect(x, y, 1, 1);
        }
    }

    private String key(String captchaId) {
        return KEY_PREFIX + captchaId;
    }

    private int expireSeconds() {
        return properties.expireSeconds() > 0 ? properties.expireSeconds() : 120;
    }

    private boolean isValidId(String captchaId) {
        return captchaId != null && captchaId.matches("[a-fA-F0-9]{32}");
    }
}
