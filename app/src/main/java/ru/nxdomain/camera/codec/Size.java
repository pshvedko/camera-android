package ru.nxdomain.camera.codec;

import android.hardware.Camera;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public class Size implements Comparable<Object> {
    public int width, height;

    public Size(int w, int h) {
        width = w;
        height = h;
    }

    public Size(@NotNull Camera.Size size) {
        width = size.width;
        height = size.height;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Size)
            return (((Size) o).width == width && ((Size) o).height == height);
        else if (o instanceof Camera.Size)
            return (((Camera.Size) o).width == width && ((Camera.Size) o).height == height);
        else
            return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + width;
        result = prime * result + height;
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(width + "x" + height);
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Size)
            return width == ((Size) o).width ? height - ((Size) o).height : width - ((Size) o).width;
        else if (o instanceof Camera.Size)
            return width == ((Camera.Size) o).width ? height - ((Camera.Size) o).height : width - ((Camera.Size) o).width;
        else
            return 0;
    }

    public Ratio ratio() {
        return Ratio.get(width, height);
    }
}
