package com.matthewsyren.runner.utilities;

public class NumberUtilities {
    /**
     * @param number The number to be rounded off
     */
    public static double roundOffToThreeDecimalPlaces(double number){
        return Math.round(number * 1000) / 1000.0;
    }

    /**
     * Rounds off to 1 decimal place
     * @param number The number to be rounded off
     */
    public static double roundOffToOneDecimalPlace(double number){
        return Math.round(number * 10) / 10.0;
    }
}
