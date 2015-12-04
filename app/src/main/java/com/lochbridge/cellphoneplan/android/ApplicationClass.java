package com.lochbridge.cellphoneplan.android;

import android.app.Application;

import org.springframework.util.StringUtils;

/**
 * Created by PAggarwal1 on 11/2/2015.
 */
public class ApplicationClass extends Application {

    private String days;

    private String circleName;

    private String providerName;

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getCircleName() {
        return circleName;
    }

    public void setCircleName(String circleName) {
        this.circleName = circleName;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        String[] start = StringUtils.split(days, " ");
        this.days = start[0];
    }

}
