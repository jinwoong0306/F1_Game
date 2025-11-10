package com.mygame.f1.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class UserStore {
    private static final String STORE_PATH = "data/users.json";
    private final Map<String, Record> users = new HashMap<>();

    public static class Record {
        public String username;
        public String salt;
        public String passwordHash;
    }

    public UserStore() {
        load();
    }

    public boolean register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < 4) return false;
        if (users.containsKey(username)) return false;
        String salt = genSalt();
        String hash = hash(password, salt);
        Record r = new Record();
        r.username = username;
        r.salt = salt;
        r.passwordHash = hash;
        users.put(username, r);
        save();
        return true;
    }

    public boolean verify(String username, String password) {
        Record r = users.get(username);
        if (r == null) return false;
        String h = hash(password, r.salt);
        return r.passwordHash.equals(h);
    }

    private void load() {
        try {
            FileHandle fh = Gdx.files.local(STORE_PATH);
            if (!fh.exists()) return;
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(fh);
            for (JsonValue v : root) {
                Record r = new Record();
                r.username = v.getString("username");
                r.salt = v.getString("salt");
                r.passwordHash = v.getString("passwordHash");
                users.put(r.username, r);
            }
        } catch (Exception ignored) {}
    }

    private void save() {
        try {
            Json json = new Json();
            FileHandle fh = Gdx.files.local(STORE_PATH);
            fh.parent().mkdirs();
            fh.writeString(json.prettyPrint(users.values().toArray()), false);
        } catch (Exception ignored) {}
    }

    private static String genSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] out = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            return "";
        }
    }
}

