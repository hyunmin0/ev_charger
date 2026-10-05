package ev_charger.be.common.enums;

// Y == true
// N == false
public enum YN {
    Y, N;

    public boolean toBoolean() {
        return this == Y;
    }
}
