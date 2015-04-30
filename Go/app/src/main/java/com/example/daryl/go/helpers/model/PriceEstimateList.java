package com.example.daryl.go.helpers.model;

import java.util.List;

/**
 * Used by the price estimates endpoint and lists the estimated price range for each product
 * offered at a given location.
 */
public class PriceEstimateList extends UberModel {

    /**
     * List of the estimated price range for each product.
     */
    List<PriceEstimate> prices;

    public List<PriceEstimate> getPrices() {
        return prices;
    }

}
