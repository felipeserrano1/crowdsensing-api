package com.example.apicrowdsensing.models;

import java.util.ArrayList;

public class NewFormatMarker {
    private double[] geocode;
    private Object popUp;

    public NewFormatMarker() {
    }

    public NewFormatMarker(double[] geocode, Object popUp) {
        this.geocode = geocode;
        this.popUp = popUp;
    }

    public NewFormatMarker(double[] geocode) {
        this.geocode = geocode;
    }

    public double[] getGeocode() {
        return geocode;
    }

    public void setGeocode(double[] geocode) {
        this.geocode = geocode;
    }

    public Object getPopUp() {
        String result = "";
        for(Object o: (ArrayList) this.popUp) {
            result += o + " ";
        }
        return result;
    }

    public void setPopUp(String popUp) {
        this.popUp = popUp;
    }

}
