package sk.mimacom.techsession.vertx;


import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Configuration {
    private static final Pattern OPTION_NESTED_PATTERN = Pattern.compile("(.+?)\\.(.*)");

    public static String getString(String key, Context context) {
        return (String) getOptionRecursive(key, context.config(), JsonObject::getString);
    }

    public static Integer getInteger(String key, Context remoteContainer) {
        return (Integer) getOptionRecursive(key, remoteContainer.config(), JsonObject::getInteger);
    }

    public static JsonArray getArray(String key, Context context) {
        return (JsonArray) getOptionRecursive(key, context.config(), JsonObject::getJsonArray);
    }

    public static String getMandatoryString(String key, JsonObject jsonObject) {
        checkKeyExists(key, jsonObject);
        return jsonObject.getString(key);
    }

    private static Object getOptionRecursive(String key, JsonObject jsonObject, BiFunction<JsonObject, String, ? extends Object> getterFunction) {
        if (!key.contains(".")) { //simple key, return directly
            checkKeyExists(key, jsonObject);
            return getterFunction.apply(jsonObject, key);
        } else { //complicated expression in form of obj.property.property2 .... etc
            Matcher matcher = OPTION_NESTED_PATTERN.matcher(key);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(key + " is not a valid option expression");
            }
            String immediateKey = matcher.group(1);
            String followingKeys = matcher.group(2);
            JsonObject nestedObject = jsonObject.getJsonObject(immediateKey);
            if (nestedObject == null) {
                throw new IllegalArgumentException("Nested JSON object " + key + " not found");
            }
            return getOptionRecursive(followingKeys, nestedObject, getterFunction);
        }
    }

    private static void checkKeyExists(String key, JsonObject jsonObject) {
        if (!jsonObject.containsKey(key)) {
            throw new IllegalArgumentException("Key " + key + " does not exist in this JSON object");
        }
    }

}
