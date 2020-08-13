package ru.nxdomain.camera;

public class Hello {
    private static final String TAG = "Hello";
    private String mName;

    public Hello(String name) {
        mName = name;
    }

    public native String greetings();
}
