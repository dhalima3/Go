package com.go.ride.go.helpers.model;

import com.go.ride.go.helpers.Constants;

/**
 * Helper class that overrides the {@link #toString()} method to print pretty json. All models
 * extend this class.
 */
public class UberModel {

    @Override
    public String toString() {
        return Constants.GSON.toJson(this);
    }

}
