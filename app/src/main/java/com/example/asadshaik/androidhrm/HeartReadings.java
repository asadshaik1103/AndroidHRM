package com.example.asadshaik.androidhrm;

/**
 * Created by AsadShaik on 15/03/17.
 */

public class HeartReadings {
    private String reading;
    public HeartReadings(){
    }

    public HeartReadings(String reading){
        this.reading = reading;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }
}
