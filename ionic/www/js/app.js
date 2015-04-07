// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
angular.module('starter', ['ionic', 'starter.controllers', 'ion-google-place', 'uber'])

  .run(function ($ionicPlatform) {
    $ionicPlatform.ready(function() {
      // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard
      // for form inputs)
      if(window.cordova && window.cordova.plugins.Keyboard) {
        cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
      }
      if(window.StatusBar) {
        StatusBar.styleDefault();
      }
    });
  })

.directive('googleplace', function() {
    return {
        require: 'ngModel',
        scope: {
            ngModel: '=',
            details: '=?',
            sourceLatitude: '=',
            sourceLongitude: '='
        },
        link: function(scope, element, attrs, model) {
            var options = {
                types: [],
            };
            scope.gPlace = new google.maps.places.Autocomplete(element[0], options);
 
            google.maps.event.addListener(scope.gPlace, 'place_changed', function() {
                scope.$apply(function() {
                    console.log("D: " + scope.gPlace.getPlace().geometry.location.k);
                    console.log("k: " + scope.gPlace.getPlace().geometry.location.D);
                    scope.sourceLatitude = scope.gPlace.getPlace().geometry.location.k;
                    scope.sourceLongitude = scope.gPlace.getPlace().geometry.location.D;
                    scope.details = scope.gPlace.getPlace();
                    model.$setViewValue(element.val());                
                });
            });
        }
    };
})

    // .controller('Uber', function ($scope, $ionicModal, $timeout) {

    //     $scope.getEstimatesForUserLocation = function (latitude, longitude) {
    //       var source = "West Peachtree Street Northwest, Atlanta, GA, United States";
    //       var sourceLatitude = 33.7788652;
    //       var sourceLongitude = -84.3872821;

    //       var destination = "Atlanta Marriott Marquis, Peachtree Center Avenue Northeast, Atlanta, GA, United States";
    //       var destinationLatitude = 33.761506;
    //       var destinationLongitude = -84.38555600000001;

    //       // CHANGE FOR SECURITY
    //       var uberClientId = "MibBLkhaD19lbPQpU03xCnkdbyO0K-z7";
    //       var uberServerToken = "N1zzwuD4_gO807UsUvXF2Ba9KOmurCAn-EgLujh8";

    //       console.log("hit");
      
    //       $.ajax({
    //         url: "https://api.uber.com/v1/estimates/time",
    //         headers: {
    //           Authorization: "Token " + uberServerToken
    //         },
    //         data: {
    //           start_latitude: sourceLatitude,
    //           start_longitude: sourceLongitude,
    //           end_latitude: destinationLatitude,
    //           end_longitude: destinationLongitude
    //         },
    //         success: function(result) {
    //           console.log(result);
    //         }
    //       });
    //     };
    // })

  .controller('MyCtrl', function ($scope) {
    $scope.gPlace;
    // $scope.latitude;
    // $scope.longitude;
    console.log($scope.sourceLatitude);
  })

  .config(function ($stateProvider, $urlRouterProvider) {
    $stateProvider

      .state('app', {
        url: "/app",
        abstract: true,
        templateUrl: "templates/menu.html",
        controller: 'AppCtrl'
      })
      .state('app.home', {
        url: "/home",
        views: {
            'menuContent': {
              templateUrl: "templates/request.html",
              controller: 'Uber'
              // ADD LYFT
            }
          }
      });

    $urlRouterProvider.otherwise('/app/home');
  });