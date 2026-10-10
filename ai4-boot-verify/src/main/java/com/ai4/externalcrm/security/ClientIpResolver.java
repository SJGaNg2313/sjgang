package com.ai4.externalcrm.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** 仅在请求来自可信反向代理时采用代理传入的真实客户端 IP。 */
@Component
public class ClientIpResolver {

    private static final Pattern IPV6_LITERAL = Pattern.compile("[0-9a-fA-F:]{2,64}");

    private final Set<String> trustedProxyIps;

    public ClientIpResolver(CrmWhitelistProperties properties) {
        this.trustedProxyIps = new HashSet<>();
        if (properties.trustedProxyIps() != null) {
            properties.trustedProxyIps().stream()
                    .map(this::normalize)
                    .filter(this::isIpLiteral)
                    .forEach(trustedProxyIps::add);
        }
    }

    public ClientIpInfo resolve(HttpServletRequest request) {
        String remoteIp = normalize(request.getRemoteAddr());
        String xRealIp = normalize(request.getHeader("X-Real-IP"));
        String xForwardedFor = sanitizeForLog(request.getHeader("X-Forwarded-For"));
        boolean trustedProxy = trustedProxyIps.contains(remoteIp);

        if (!isIpLiteral(remoteIp)) {
            return new ClientIpInfo(
                    "unknown",
                    remoteIp,
                    xRealIp,
                    xForwardedFor,
                    false,
                    "UNRESOLVED",
                    "REMOTE_IP_INVALID"
            );
        }

        if (!trustedProxy) {
            return new ClientIpInfo(
                    remoteIp,
                    remoteIp,
                    xRealIp,
                    xForwardedFor,
                    false,
                    "DIRECT_CONNECTION",
                    ""
            );
        }

        if (!isIpLiteral(xRealIp)) {
            return new ClientIpInfo(
                    "unknown",
                    remoteIp,
                    xRealIp,
                    xForwardedFor,
                    true,
                    "UNRESOLVED",
                    "TRUSTED_PROXY_X_REAL_IP_MISSING_OR_INVALID"
            );
        }

        return new ClientIpInfo(
                xRealIp,
                remoteIp,
                xRealIp,
                xForwardedFor,
                true,
                "TRUSTED_PROXY_X_REAL_IP",
                ""
        );
    }

    private boolean isIpLiteral(String value) {
        if (!StringUtils.hasText(value) || "unknown".equals(value)) {
            return false;
        }
        if (value.indexOf(':') >= 0) {
            if (!IPV6_LITERAL.matcher(value).matches()) {
                return false;
            }
            try {
                return InetAddress.getByName(value) instanceof Inet6Address;
            } catch (UnknownHostException exception) {
                return false;
            }
        }

        String[] segments = value.split("\\.", -1);
        if (segments.length != 4) {
            return false;
        }
        for (String segment : segments) {
            if (!segment.matches("\\d{1,3}")) {
                return false;
            }
            int number = Integer.parseInt(segment);
            if (number > 255) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        String normalized = value.trim();
        if ("0:0:0:0:0:0:0:1".equals(normalized)) {
            return "::1";
        }
        if (normalized.regionMatches(true, 0, "::ffff:", 0, 7)) {
            String ipv4 = normalized.substring(7);
            if (isIpLiteral(ipv4)) {
                return ipv4;
            }
        }
        return normalized;
    }

    private String sanitizeForLog(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String sanitized = value.replace('\r', '_').replace('\n', '_').trim();
        return sanitized.length() <= 255 ? sanitized : sanitized.substring(0, 255);
    }

    public record ClientIpInfo(
            String clientIp,
            String remoteIp,
            String xRealIp,
            String xForwardedFor,
            boolean trustedProxy,
            String resolutionSource,
            String failureReason
    ) {
        public boolean resolved() {
            return !"unknown".equals(clientIp);
        }
    }
}
