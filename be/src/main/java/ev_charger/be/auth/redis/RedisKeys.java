package ev_charger.be.auth.redis;

public final class RedisKeys {
    public RedisKeys() {
    }

    public static String tempUser(String token) {
        return "temp:"+token;
    }
    public static String blacklist(String token) {
        return "blacklist:"+token;
    }

}
