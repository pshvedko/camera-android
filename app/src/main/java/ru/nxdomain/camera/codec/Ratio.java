package ru.nxdomain.camera.codec;


import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Ratio {
    public int width;
    public int height;

    public Ratio(int a, int b) {
        if (a < b) {
            a ^= b;
            b ^= a;
            a ^= b;
        }
        int d = gcd(a, b);
        width = a / d;
        height = b / d;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ratio && (((Ratio) o).width == width && ((Ratio) o).height == height);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + width;
        result = prime * result + height;
        return result;
    }

    @NotNull
    @Override
    public String toString() {
        return width + ":" + height;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    public static Ratio get(int w, int h) {
        return new Ratio(w, h);
    }
}

