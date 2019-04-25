package com.group6.tinderforfood;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.yelp.fusion.client.connection.YelpFusionApi;
import com.yelp.fusion.client.connection.YelpFusionApiFactory;
import com.yelp.fusion.client.models.ApiKey;
import com.yelp.fusion.client.models.Business;
import com.yelp.fusion.client.models.Coordinates;
import com.yelp.fusion.client.models.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import retrofit2.Call;
import retrofit2.Response;

public class RestaurantSwipeActivity extends AppCompatActivity {

    TextView mRestaurantTitle, mRating;
    ImageView restaurantImage;
    YelpFusionApiFactory apiFactory;
    YelpFusionApi yelpFusionApi;
    Map<String, String> mParams;
    OkHttpClient mClient;
    List<Restaurant> mRestaurants;
    int i, iLast;
    ProgressBar mLoading;
    boolean waiting = false;
    int rCount = 40;
    private final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 666;
    double mLongitude, mLatitude;
    Coordinates mCoordinate;


    private DrawerLayout dl;
    private ActionBarDrawerToggle abdt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.restaurant_swipe);

        mClient = new OkHttpClient();
        mRestaurantTitle = (TextView) findViewById(R.id.restaurantTitle);
        mRating = (TextView) findViewById(R.id.bottomText);
        restaurantImage = (ImageView) findViewById(R.id.restaurantImage);
        mLoading = (ProgressBar) findViewById(R.id.loading);
        mRestaurants = new ArrayList<>();
        i = 0;
        iLast = 0;




        restaurantImage.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
                sameRestaurantNewPic();
            }

            public void onSwipeRight() {
                lastRestaurant();
            }

            public void onSwipeLeft() {
                newRestaurant();
            }

            public void onSwipeBottom() {
                sameRestaurantPrevPic();
            }
        });


        // String apiKey = (String.valueOf(R.string.apiKey));//"hGAW2FySQrZqdHTxFT4s_fY-4OErTolDk-jyWn9r_6GKi0VCBw_mVcJuqidHQgNkfTSid0Rb4CS5pqrr2AoApLauOJUKalIig1V7Ye6aI2eMalROQzZcPTpiy5PAXHYx";
        apiFactory = new YelpFusionApiFactory();


        try {
            yelpFusionApi = apiFactory.createAPI("hGAW2FySQrZqdHTxFT4s_fY-4OErTolDk-jyWn9r_6GKi0VCBw_mVcJuqidHQgNkfTSid0Rb4CS5pqrr2AoApLauOJUKalIig1V7Ye6aI2eMalROQzZcPTpiy5PAXHYx");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mParams = new HashMap<>();
        mParams.put("term", "pizza");

        mParams.put("limit", "40");

        LocationManager lm = (LocationManager) getSystemService((Context.LOCATION_SERVICE));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        mParams.put("latitude", location.getLatitude()+"");
        mParams.put("longitude", location.getLongitude()+ "");

        new FetchPictures().execute("0");
        waitForRestaurant(true);

    }


    public void initLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           // mLatitude = 33.7632;
            //mLongitude = 0;
            new FetchPictures().execute("0");
        } else {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                mLongitude = location.getLongitude();
                mLatitude = location.getLatitude();
                new FetchPictures().execute("0");
            } else {
                Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                lm.requestSingleUpdate(criteria, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        mCoordinate = new Coordinates();
                        mCoordinate.setLongitude(location.getLongitude());
                        mCoordinate.setLatitude(location.getLatitude());

                        new FetchPictures().execute("0");
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                        Toast.makeText(RestaurantSwipeActivity.this, "GPS needed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderEnabled(String s) {
                        Toast.makeText(RestaurantSwipeActivity.this, "GPS needed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProviderDisabled(String s) {
                        Toast.makeText(RestaurantSwipeActivity.this, "GPS needed", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            }
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        mCoordinate = new Coordinates();
        mCoordinate.setLongitude(location.getLongitude());
        mCoordinate.setLatitude(location.getLatitude());
       //// ADD COORDINATES IF NECCESARY
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Setting San Francisco as default location", Toast.LENGTH_SHORT).show();
                }
                initLocation();
                waitForRestaurant(true);
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }



    private void sameRestaurantPrevPic() {
        Restaurant r = mRestaurants.get(i);
        if (r.getCurrPic() > 0) {
            r.decCurrPic();
            waitForRestaurant(true);

        }
    }

    private void sameRestaurantNewPic() {
        Restaurant r = mRestaurants.get(i);
        if (r.getPictures().size() - 1 > r.getCurrPic()) {
            r.incCurrPic();
            waitForRestaurant(true);
            if(r.getCurrPic() - r.getiLast() > 5 && r.getPictures().size() - r.getCurrPic() < 7 ) {
                r.setiLast(r.getCurrPic());
            }
        }

    }

    private void lastRestaurant() {
        if(i >= 0 ) {
            i--;
            waitForRestaurant(true);

        }
    }
    private void newRestaurant() {
        if(mRestaurants.size()-1 > i) {
            i++;
            waitForRestaurant(true);
            if (i - iLast > 5 && mRestaurants.size() - i < 7) {
                new FetchPictures().execute("" + rCount);
                rCount+= 40;
            }
        }
    }
   

    synchronized public void waitForRestaurant(boolean client) {
        if(client) {
            if(mRestaurants.size() > i &&
                    mRestaurants.get(i).getPictures().size() > mRestaurants.get(i).getCurrPic()) {
                // have data
                restaurantCallback();
            }else{
                waiting = true;
                mLoading.setVisibility(View.VISIBLE);
            }
        }else{
            if(waiting) {
                restaurantCallback();
                waiting = false;
                mLoading.setVisibility(View.INVISIBLE);
            }
        }
    }


    public void restaurantCallback() {
        displayRestaurant(mRestaurants.get(i));
    }

    public void displayRestaurant(Restaurant r) {
        Picasso.get().load(r.getPictures().get(r.getCurrPic())).into(restaurantImage);
        mRestaurantTitle.setText(r.getName());
        mRating.setText(r.getRating());
    }

     class FetchPictures extends AsyncTask<String, Restaurant, String> {

        List<Restaurant>  restaurants;
         @Override
         protected void onProgressUpdate(Restaurant... values) {
             super.onProgressUpdate(values);
             if (values != null) {
                 mRestaurants.add(values[0]);
                 waitForRestaurant(false);
             } else {
                 Toast.makeText(RestaurantSwipeActivity.this, "No data available for your location", Toast.LENGTH_SHORT).show();
             }
         }
        @Override
        protected String doInBackground(String... params) {
            mParams.put("offset", params[0]);
            Call<SearchResponse> call = yelpFusionApi.getBusinessSearch(mParams);
            Response<SearchResponse> response = null;
            try {
                System.out.println("************************************************");
                response = call.execute();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            if(response != null) {
                System.out.println("NOTTTT NULLOLLLLLLLLLLLLLLLLL");
                List<Business> businessList = new ArrayList<>();
                businessList= response.body().getBusinesses();

                restaurants = new ArrayList<>();
                Restaurant r;
                int i = 0;
                for (Business b : businessList) {
                    r = new Restaurant(b.getName(), b.getUrl());
                    r.setRating(b.getRating()+"");
                    restaurants.add(r);
                    fetchPictures(r, i);
                    i++;
                }
            }else{

                System.out.println("NNNNNUUUUUUUUUUUUUUUUULLLLLLLLLLLLLLLLLLLLL");
            }
            return null;
        }

        private void fetchPictures(Restaurant r, final int pos) {


                Request request = new Builder()
                        .url(r.getPicUrl())
                        .build();

                mClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {

                        List<String> pictures = RestaurantParser.getPictures(response.body().string());
                        if (pictures.size() > 0 ) {
                            restaurants.get(pos).setPictures(pictures);
                            publishProgress(restaurants.get(pos));
                        }

                    }
                }); {

                }


        }
    }
}