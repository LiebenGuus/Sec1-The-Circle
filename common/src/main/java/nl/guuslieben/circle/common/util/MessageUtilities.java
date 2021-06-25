package nl.guuslieben.circle.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

public class MessageUtilities {

    public static final Logger log = LoggerFactory.getLogger(MessageUtilities.class);
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String HASH_ALGORITHM = "SHA-512";

    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN);

    public static final String INVALID = "InvalidatedContent";
    public static final String REJECT = "Rejected#";

    public static Message fromJson(String message) {
        try {
            return MAPPER.readValue(message, Message.class);
        }
        catch (JsonProcessingException e) {
            return new Message(INVALID);
        }
    }

    public static <T> Optional<T> verifyContent(Message message, Class<T> type) {
        if (verify(message)) {
            return getContent(message, type);
        }
        else {
            return Optional.empty();
        }
    }

    public static boolean verify(Message message) {
        final var actual = message.getHash();
        final var expected = generateHash(message.getContent());

        return expected.equals(actual);
    }

    public static <T> Optional<T> getContent(Message message, Class<T> type) {
        try {
            return Optional.ofNullable(MAPPER.readValue(message.getContent(), type));
        }
        catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public static String generateHash(String content) {
        try {
            var md = MessageDigest.getInstance(HASH_ALGORITHM);
            var messageDigest = md.digest(content.getBytes(StandardCharsets.UTF_8));

            var no = new BigInteger(1, messageDigest);
            var hash = new StringBuilder(no.toString(16));
            while (hash.length() < 32) {
                hash.insert(0, "0");
            }
            return hash.toString();
        }
        catch (NoSuchAlgorithmException e) {
            return INVALID;
        }
    }

    public static String generateTimestamp() {
        return DATE_FORMAT.format(new Date());
    }

    public static LocalDateTime parseTimestamp(String timestamp) {
        try {
            final var instant = DATE_FORMAT.parse(timestamp).toInstant();
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }
        catch (ParseException e) {
            return null;
        }
    }

    public static <T> String toJson(T content) {
        if (content instanceof String) return (String) content;
        try {
            final String json = MAPPER.writeValueAsString(content);
            // Jackson can map invalid JSON strings to string literal 'null', ensure we catch it here
            return json == null || "null".equals(json) ? INVALID : json;
        }
        catch (JsonProcessingException e) {
            return INVALID;
        }
    }

    public static Message reject(String reason) {
        return new Message(REJECT + reason);
    }

    public static Optional<String> getRejection(Message message) {
        final boolean rejected = message.getContent().startsWith(REJECT);
        if (rejected) {
            final String reason = message.getContent().replace(REJECT, "");
            return Optional.of(reason);
        }
        else {
            return Optional.empty();
        }
    }
}