package javagi.casestudies.servlet;

interface Callback<X> {
    ApplicationState invoke (X value);
}
