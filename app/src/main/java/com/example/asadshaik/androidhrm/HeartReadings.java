package com.example.asadshaik.androidhrm;

/**
 * Created by AsadShaik on 15/03/17.
 */

public class HeartReadings {
    private String reading;
    private String userid;

    public HeartReadings(){
    }

    public HeartReadings(String userid,String reading){

        this.userid=userid;

        this.reading = reading;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }





    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }
}
