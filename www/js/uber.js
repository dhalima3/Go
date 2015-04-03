angular.module('uber', [])

    .controller('Uber', function ($scope, $ionicModal, $timeout) {

        $scope.getPriceEstimatesForUserLocation = function (latitude, longitude) {
          var source = "West Peachtree Street Northwest, Atlanta, GA, United States";
          var sourceLatitude = 33.761506;
          var sourceLongitude = -84.38555600000001;

          var destination = "Atlanta Marriott Marquis, Peachtree Center Avenue Northeast, Atlanta, GA, United States";
          var destinationLatitude = 33.7788652;
          var destinationLongitude = -84.3872821;

          // CHANGE FOR SECURITY
          var uberClientId = "MibBLkhaD19lbPQpU03xCnkdbyO0K-z7";
          var uberServerToken = "ipdXLSoVVnIyIkQIFKFp2BwrFttPdmkVKNVHzJFf";

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

        $scope.getTimeEstimatesForUserLocation = function (latitude, longitude) {
          var source = "West Peachtree Street Northwest, Atlanta, GA, United States";
          var sourceLatitude = 33.761506;
          var sourceLongitude = -84.38555600000001;

          var destination = "Atlanta Marriott Marquis, Peachtree Center Avenue Northeast, Atlanta, GA, United States";
          var destinationLatitude = 33.7788652;
          var destinationLongitude = -84.3872821;

          // CHANGE FOR SECURITY
          var uberClientId = "MibBLkhaD19lbPQpU03xCnkdbyO0K-z7";
          var uberServerToken = "ipdXLSoVVnIyIkQIFKFp2BwrFttPdmkVKNVHzJFf";

          console.log("hit");
      
          $.ajax({
            url: "https://api.uber.com/v1/estimates/time",
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

   


// // Uber API Constants
// var uberClientId = "MibBLkhaD19lbPQpU03xCnkdbyO0K-z7"
//   , uberServerToken = "ipdXLSoVVnIyIkQIFKFp2BwrFttPdmkVKNVHzJFf";

// // Create variables to store latitude and longitude
// var userLatitude = 33.761506
//   , userLongitude = -84.38555600000001
//   , partyLatitude = 33.7788652
//   , partyLongitude = -84.3872821;

// navigator.geolocation.watchPosition(function(position) {
//     // Update latitude and longitude
//     // userLatitude = position.coords.latitude;
//     // userLongitude = position.coords.longitude;

//   // Query Uber API if needed
//     getEstimatesForUserLocation(userLatitude, userLongitude);
// });

// function getEstimatesForUserLocation(latitude,longitude) {
//   $.ajax({
//     url: "https://api.uber.com/v1/estimates/price",
//     headers: {
//         Authorization: "Token " + uberServerToken
//     },
//     data: {
//       start_latitude: latitude,
//       start_longitude: longitude,
//       end_latitude: partyLatitude,
//       end_longitude: partyLongitude
//     },
//     success: function(result) {
//       console.log(result);
//     }
//   });
// }