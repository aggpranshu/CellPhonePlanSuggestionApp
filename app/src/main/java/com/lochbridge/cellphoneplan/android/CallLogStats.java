
package com.lochbridge.cellphoneplan.android;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.lochbridge.cellphoneplan.spring.AggregatedLogStats;
import com.lochbridge.cellphoneplan.spring.BillPlansList;
import com.lochbridge.cellphoneplan.spring.ListPlanDetailsByDuration;

public class CallLogStats extends Activity {

    List<ListPlanDetailsByDuration> listPlanDetailsByDuration;
    private String providerName;
    private Button buttonBill;
    private TextView totalDayCallsTv;
    private TextView totalNightCallsTv;
    private TextView totalCallsTv;
    private TextView totalSMSCountTv;
    private HashMap<String, CallLogs> callLogsDataMap = new LinkedHashMap<String, CallLogs>();
    private AggregatedLogStats aggregatedLogStats;
    private int smsCount = 0;
    private String[] projectionCall = new String[] {
            CallLog.Calls.DATE,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
    };

    private String[] projectionMesg = new String[] {
            "type", "date"
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalDayCallsTv = (TextView) findViewById(R.id.durationDayTextView);
        totalNightCallsTv = (TextView) findViewById(R.id.durationNightTextView);
        totalCallsTv = (TextView) findViewById(R.id.totalCallTextView);
        totalSMSCountTv = (TextView) findViewById(R.id.totalSMSCountTextView);
        TextView circleNameTv = (TextView) findViewById(R.id.circleNameTextView);
        TextView validityTv = (TextView) findViewById(R.id.providerNameTextView);
        TableLayout tableLayoutForBill = (TableLayout) findViewById(R.id.tableForBill);
        buttonBill = (Button) findViewById(R.id.billButton);

        Date d = (Date) getIntent().getSerializableExtra("date");
        String whenItHappened = getIntent().getStringExtra("whenItHappened");

        circleNameTv.setText(((ApplicationClass) getApplication()).getCircleName());
        validityTv.setText(((ApplicationClass) getApplication()).getDays());

        if (whenItHappened.equals("before")) {
            CallRecords(d);
            MesgRecords(d);
            isRoaming();

            // Toast.makeText(getApplicationContext(), ((ApplicationClass)
            // getApplication()).getDays(), Toast.LENGTH_SHORT).show();
        } else {
            // fetchCallRecordsFromPast(d,whenItHappened);
        }
        // payPerSecond();
        buttonBill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new BgAsyncTaskForBillGeneration().execute();
            }
        });
    }

    private void MesgRecords(Date d) {
        Uri uri = Uri.parse("content://sms");
        Cursor curMesg = getContentResolver().query(uri, projectionMesg, "type=2", null, null);

        if (curMesg.moveToFirst()) {
            for (int i = 0; i < curMesg.getCount(); i++) {
                String date = curMesg.getString(curMesg.getColumnIndexOrThrow("date"));
                Date smsDayTime = new Date(Long.valueOf(date));
                if (smsDayTime.compareTo(d) > 0) {
                    smsCount++;
                }
                curMesg.moveToNext();
            }

            curMesg.close();

            totalSMSCountTv.setText(String.valueOf(smsCount));
        }
    }

    private void CallRecords(Date d) {
        String selection = "type = 2";
        ContentResolver resolver = getApplicationContext().getContentResolver();
        Cursor curCall = resolver.query(CallLog.Calls.CONTENT_URI, projectionCall, selection, null,
                null);

        while (curCall.moveToNext()) {
            String truncatedNumber;
            String number = curCall.getString(curCall.getColumnIndex(CallLog.Calls.NUMBER));
            String duration = curCall.getString(curCall.getColumnIndex(CallLog.Calls.DURATION));

            String date = curCall.getString(curCall.getColumnIndex(CallLog.Calls.DATE));
            Date callDate = new Date(Long.valueOf(date));

            Log.i("DATE", d.toString());

            if (callDate.compareTo(d) > 0 && Integer.valueOf(duration) > 0) {
                Log.i("duration", number + "    " + Long.valueOf(duration).toString());
                if (number.length() > 10) {
                    truncatedNumber = "91"
                            + number.substring((Math.abs(10 - number.length())), number.length());
                } else
                    truncatedNumber = number;

                if (callLogsDataMap.containsKey(truncatedNumber)) {
                    CallLogs obj = callLogsDataMap.get(truncatedNumber);
                    obj.setDuration(Integer.valueOf(duration), callDate);
                    callLogsDataMap.put(truncatedNumber, obj);

                } else {
                    CallLogs object = new CallLogs(truncatedNumber, duration, callDate,
                            providerName);
                    object.setDuration(Integer.valueOf(duration), callDate);
                    callLogsDataMap.put(truncatedNumber, object);
                    object.setSmsCount(smsCount);
                }
            }
        }
        curCall.close();
        callServiceForData(callLogsDataMap);
    }

    private void callServiceForData(HashMap<String, CallLogs> callLogsDataMap) {
        new BgAsyncTaskForLogAggregation().execute();
    }

    private void isRoaming() {

        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info.isRoaming()) {
            Toast.makeText(getApplicationContext(), "roaming", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Not roaming", Toast.LENGTH_SHORT).show();
        }

    }

    private class BgAsyncTaskForLogAggregation extends AsyncTask<Void, Void, AggregatedLogStats> {

        String response;

        @Override
        protected AggregatedLogStats doInBackground(Void... params) {
            try {
                String jsonMap = new Gson().toJson(callLogsDataMap);
                Log.i("jsonmap", jsonMap);

                RestTemplate restTemplate = new RestTemplate(true);
                restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                // String result=
                // restTemplate.postForObject("http://192.168.1.6:8080/SpringHibernate/user/data/call",callLogsDataMap,
                // String.class);
                aggregatedLogStats = restTemplate.postForObject(
                        "http://172.22.41.154:8080/SpringHibernate/user/data/logaggregation",
                        callLogsDataMap, AggregatedLogStats.class);
                return aggregatedLogStats;
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
        protected void onPostExecute(AggregatedLogStats aggregatedLogStats) {
            super.onPostExecute(aggregatedLogStats);
            totalDayCallsTv.setText(String.valueOf(aggregatedLogStats
                    .getTotalCallDuringDayInSeconds()) + " sec");
            totalNightCallsTv.setText(String.valueOf(aggregatedLogStats
                    .getTotalCallDuringNightInSeconds()) + " sec");
            totalCallsTv.setText(String.valueOf(aggregatedLogStats.getTotalCallDuringDayInSeconds()
                    + aggregatedLogStats.getTotalCallDuringNightInSeconds())
                    +
                    " sec");

            // Toast.makeText(getApplicationContext(), aggregatedLogStats.toString(),
            // Toast.LENGTH_SHORT).show();
            // setLayoutData(aggregatedLogStats);
        }
    }

    private class BgAsyncTaskForBillGeneration extends AsyncTask<Void, Void, BillPlansList> {

        String response;

        @Override
        protected BillPlansList doInBackground(Void... params) {
            try {

                String url = "http://172.22.41.154:8080/SpringHibernate/telecomdata/"
                        + ((ApplicationClass) getApplication()).getProviderName() + "/" +
                        ((ApplicationClass) getApplication()).getCircleName() + "/"
                        + ((ApplicationClass) getApplication()).getDays();
                RestTemplate restTemplate = new RestTemplate(true);
                restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                return restTemplate.postForObject(url + "/bill", aggregatedLogStats,
                        BillPlansList.class);
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
        protected void onPostExecute(BillPlansList billPlansList) {
            super.onPostExecute(billPlansList);
            Gson gson = new Gson();
            Log.i("BILLS123", gson.toJson(billPlansList));
            Toast.makeText(getApplicationContext(), billPlansList.toString(), Toast.LENGTH_SHORT)
                    .show();

            Intent i = new Intent(CallLogStats.this, BillList.class);
            i.putExtra("billObject", billPlansList);

            startActivity(i);

        }
    }

}
