package com.project.DiseaseDiagnosis;

/**
 * Side class that is used in the transition map. This class stores pairs (current state, input symbol).
 * Overridden methods equals and hashCode are used for comparing objects based on their attributes.
 */
public class Key {

    int state;        // current state in finite state machine
    char symbol;    // input symbol

    public Key(int state, char symbol) {
        this.state = state;
        this.symbol = symbol;
    }

    /**
     * Overridden method equals compares two objects of class Key based on their attributes.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Key)) {
            return false;
        }
        Key key = (Key) o;
        return state == key.state && symbol == key.symbol;
    }

    @Override
    public int hashCode() {
        int result = state;
        result = 31 * result + symbol;
        return result;
    }
}
