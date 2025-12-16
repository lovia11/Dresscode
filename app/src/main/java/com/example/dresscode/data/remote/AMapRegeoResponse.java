package com.example.dresscode.data.remote;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class AMapRegeoResponse {
    @SerializedName("status")
    public String status;
    @SerializedName("info")
    public String info;

    @SerializedName("regeocode")
    public Regeocode regeocode;

    public static class Regeocode {
        @SerializedName("addressComponent")
        public AddressComponent addressComponent;
    }

    public static class AddressComponent {
        @SerializedName("city")
        public JsonElement city;
        @SerializedName("province")
        public String province;
        @SerializedName("district")
        public String district;
        @SerializedName("adcode")
        public String adcode;
    }
}

