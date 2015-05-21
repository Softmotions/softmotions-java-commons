package com.softmotions.commons.cont;

/**
 * Simple pair of values.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class Pair<One, Two> {

    private One one;
    private Two two;

    public Pair(One one, Two two) {
        this.one = one;
        this.two = two;
    }

    public One getOne() {
        return one;
    }

    public void setOne(One one) {
        this.one = one;
    }

    public Two getTwo() {
        return two;
    }

    public void setTwo(Two two) {
        this.two = two;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (one != null ? !one.equals(pair.one) : pair.one != null) return false;
        if (two != null ? !two.equals(pair.two) : pair.two != null) return false;

        return true;
    }

    public int hashCode() {
        int result = one != null ? one.hashCode() : 0;
        result = 31 * result + (two != null ? two.hashCode() : 0);
        return result;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Pair");
        sb.append('{').append(one).append(", ");
        sb.append(two);
        sb.append('}');
        return sb.toString();
    }
}
