angular.module('uber', [])

	.controller('Uber', function ($scope, $ionicModal, $timeout) {

        $scope.getEstimatesForUserLocation = function (latitude, longitude) {
			var source = "West Peachtree Street Northwest, Atlanta, GA, United States";
			var sourceLatitude = -84.3872821;
			var sourceLongitude = 33.7788652;

			var destination = "Atlanta Marriott Marquis, Peachtree Center Avenue Northeast, Atlanta, GA, United States";
			var destinationLatitude = -84.38555600000001;
			var destinationLongitude = 33.761506;

			// CHANGE FOR SECURITY
			var uberClientId = "MibBLkhaD19lbPQpU03xCnkdbyO0K-z7";
			var uberServerToken = "N1zzwuD4_gO807UsUvXF2Ba9KOmurCAn-EgLujh8";

			console.log("hit");

        	$.ajax({
        		url: "https://api.uber.com/v1/estimates/price",
        		headers: {
        			Authorization: "Token " + uberServerToken
        		},
        		data: {
        			start_latitude: sourceLatitude,
        			start_longitude: sourceLongitude,
        			end_latitude: destinationLatitude,
        			end_longitude: destinationLongitude
        		},
        		success: function(result) {
        			console.log(result);
        		}
        	});
        };
    })



