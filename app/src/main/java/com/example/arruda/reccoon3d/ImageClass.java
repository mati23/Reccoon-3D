package com.example.arruda.reccoon3d;

import com.google.gson.annotations.SerializedName;

public class ImageClass {

    @SerializedName("title")
    private String Title;

    @SerializedName("image")
    private String image;

    @SerializedName("response")
    private String response;

    public String getResponse(){
        return response;
    }



}