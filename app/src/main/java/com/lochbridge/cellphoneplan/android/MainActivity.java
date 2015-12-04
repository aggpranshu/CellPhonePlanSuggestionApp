
package com.lochbridge.cellphoneplan.android;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.widget.LoginButton;
import com.google.gson.Gson;
import com.guna.libmultispinner.MultiSelectionSpinner;
import com.lochbridge.cellphoneplan.spring.CircleList;
import com.lochbridge.cellphoneplan.spring.Circles;
import com.lochbridge.cellphoneplan.spring.ListPlanDetailsByDuration;
import com.lochbridge.cellphoneplan.spring.ListPlanDetailsByDurationList;

public class MainActivity extends AppCompatActivity {
    private Button dateButton;
    Button planDays;
    private TextView carrierName;
    private List<ListPlanDetailsByDuration> listPlanDetailsByDuration;
    private Spinner spinnerDuration;
    private Spinner spinnerCircleList;
    private List<String> validityOfPlans = new ArrayList<String>();
    String[] listOfPlans = new String[] {};
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private List<String> circleListNames;
    private MultiSelectionSpinner multiSelectionSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /*
         * FacebookSdk.sdkInitialize(getApplicationContext()); callbackManager =
         * CallbackManager.Factory.create();
         */

        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage);

        spinnerCircleList = (Spinner) findViewById(R.id.listCircles);
        carrierName = (TextView) findViewById(R.id.carrierName);
        spinnerDuration = (Spinner) findViewById(R.id.spinnerDuration);
        multiSelectionSpinner = (MultiSelectionSpinner) findViewById(R.id.mySpinner);
        dateButton = (Button) findViewById(R.id.dateButton);

        dateButton.setVisibility(View.INVISIBLE);
        /*
         * counterForObject = getIntent().getIntExtra("isEmpty",0); if(counterForObject==0){ } else{
         * }
         */

        CircleList circleListObj = (CircleList) getIntent().getSerializableExtra("classInfo");
        try {

            List<Circles> circleList = circleListObj.getListCircles();
            circleListNames = new ArrayList<String>();
            circleListNames.add("List of Circles");
            for (int k = 0; k < circleList.size(); k++) {
                circleListNames.add(circleList.get(k).getCircleName());
            }

        } catch (NullPointerException e) {
            Toast.makeText(getApplicationContext(), "exception caught", Toast.LENGTH_SHORT).show();
        }

        dateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });

        /*
         * loginButton = (LoginButton) findViewById(R.id.login_button);
         * loginButton.setReadPermissions("public_profile"); setupTokenTracker();
         * setupProfileTracker(); mTokenTracker.startTracking(); mProfileTracker.startTracking();
         * loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
         * @Override public void onSuccess(LoginResult loginResult) { AccessToken accessToken =
         * loginResult.getAccessToken(); Profile profile = Profile.getCurrentProfile();
         * Toast.makeText(getApplicationContext(),"logged in", Toast.LENGTH_SHORT).show(); //
         * info.setText("User Name: " + profile.getFirstName() + " " + profile.getLastName()); }
         * @Override public void onCancel() { //info.setText("Login attempt canceled."); }
         * @Override public void onError(FacebookException e) {
         * //info.setText("Login attempt failed."); } });
         */

        validityOfPlans.add("Days for plans");
        validityOfPlans.add("28 days");
        validityOfPlans.add("60 days");
        validityOfPlans.add("90 days");

        carrierName.append(((ApplicationClass) getApplication()).getProviderName());

        /*
         * Spinner for specifying the name of the circles for the current Carrier of the user. This
         * is being populated by the circleListNames - ArrayList
         */
        spinnerCircleList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();

                if (item.equals("List of Circles")) {
                    Toast.makeText(getApplicationContext(), "hello", Toast.LENGTH_SHORT).show();
                    // spinnerDuration.setVisibility(View.VISIBLE);
                    // spinnerDuration.setVisibility(View.INVISIBLE);
                } else {
                    ArrayAdapter<String> dataAdapterDurationList = new ArrayAdapter<String>(
                            getApplicationContext(), android.R.layout.simple_spinner_item,
                            validityOfPlans);
                    dataAdapterDurationList
                            .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDuration.setAdapter(dataAdapterDurationList);
                    ((ApplicationClass) getApplication()).setCircleName(item);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<String> dataAdapterCircleList = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, circleListNames);
        dataAdapterCircleList
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCircleList.setAdapter(dataAdapterCircleList);

        spinnerDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                if (item.equals("List of Circles")) {
                    Toast.makeText(getApplicationContext(), "hello", Toast.LENGTH_SHORT).show();
                } else {
                    ((ApplicationClass) getApplication()).setDays(item);
                    new BgAsyncTaskForPlanByDuration().execute();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

    /*
     * Button for selecting the Date. The call logs fetched would be filtered based on the value of
     * this Date
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void setupTokenTracker() {
        AccessTokenTracker mTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {

            }
        };
    }

    private void setupProfileTracker() {
        ProfileTracker mProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                // info.setText("Welcome" + currentProfile.getFirstName() + " " +
                // currentProfile.getLastName());
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        // mTokenTracker.stopTracking();
        // mProfileTracker.stopTracking();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Profile profile = Profile.getCurrentProfile();
        // info.setText("Welcome Back user !");

    }

    private class BgAsyncTaskForPlanByDuration extends
            AsyncTask<Void, Void, ListPlanDetailsByDurationList> {

        String response;

        @Override
        protected ListPlanDetailsByDurationList doInBackground(Void... params) {
            try {

                String url = "http://172.22.41.154:8080/SpringHibernate/telecomdata/"
                        + ((ApplicationClass) getApplication()).getProviderName() + "/" +
                        ((ApplicationClass) getApplication()).getCircleName() + "/"
                        + ((ApplicationClass) getApplication()).getDays();
                RestTemplate restTemplate = new RestTemplate(true);
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                return restTemplate
                        .getForObject(url, ListPlanDetailsByDurationList.class);
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ListPlanDetailsByDurationList listPlanDetailsByDurationList) {
            super.onPostExecute(listPlanDetailsByDurationList);
            listPlanDetailsByDuration = listPlanDetailsByDurationList
                    .getListPlanDetailsByDuration();
            Log.i("FIRSTELEMENT", listPlanDetailsByDuration.get(0).getPlanDetails());
            ArrayList<String> listPlans = new ArrayList<String>();
            Gson gson = new Gson();
            for (int i = 0; i < listPlanDetailsByDuration.size(); i++) {
                try {
                    JSONObject jsonObject = new JSONObject(listPlanDetailsByDuration.get(i)
                            .getPlanDetails());
                    listPlans.add(
                            i,
                            jsonObject.getString("recharge_value") + "\t"
                                    + jsonObject.getString("recharge_description"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            String[] array = listPlans.toArray(new String[listPlans.size()]);
            multiSelectionSpinner.setItems(array);

            dateButton.setVisibility(View.VISIBLE);
            // Toast.makeText(getApplicationContext(), aggregatedLogStats.toString(),
            // Toast.LENGTH_SHORT).show();
            // setLayoutData(aggregatedLogStats);
        }
    }

}
