// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
angular.module('starter', ['ionic', 'starter.controllers', 'ion-google-place'])

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
      link: function(scope, element, attrs, model) {
        var options = {
          types: []
        };
        scope.gPlace = new google.maps.places.Autocomplete(element[0], options);

        google.maps.event.addListener(scope.gPlace, 'place_changed', function() {
          scope.$apply(function() {
            console.log(scope.gPlace);
            model.$setViewValue(element.val());
          });
        });
      }
    };
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
              controller: 'PostRequestController'
            }
          }
      });

    $urlRouterProvider.otherwise('/app/home');
  });