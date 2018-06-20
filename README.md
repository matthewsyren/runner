# runner
An Android app that helps users to track their fitness by saving information about the runs that they take, as well as allowing users to set weekly targets for themselves.

In order to set up the project on your machine, you will need to do the following:
1) Create a Firebase project and connect the app with your Firebase project by adding the google-services.json file from your Firebase project to the app directory of the project in Android Studio.
2) Enable Email/Password and Google authentication for the app in the Firebase console.
3) Add your SHA-1 fingerprint from your app to the project settings in your Firebase project (this can be retrieved using the signingReport Gradle task in Android Studio).
4) Obtain a Google Maps API key and add the key to the google_maps_api.xml file in the debug and release folders of the project in Android Studio. 
