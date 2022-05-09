package com.project.DiseaseDiagnosis;

public class Pair {

    private int low,high;

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        Pair p = (Pair) o;
        return ((p.getLow() == this.low) && (p.getHigh() == this.high));
    }

    @Override
    public String toString() {
        return "Pair{" +
                "low=" + low +
                ", high=" + high +
                '}';
    }
}
