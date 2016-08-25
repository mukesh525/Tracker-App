/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vmc.in.mrecorder.syncadapter;

import android.Manifest;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.android.volley.RequestQueue;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import vmc.in.mrecorder.callbacks.TAG;
import vmc.in.mrecorder.datahandler.MDatabase;
import vmc.in.mrecorder.entity.CallData;
import vmc.in.mrecorder.entity.Model;
import vmc.in.mrecorder.myapplication.CallApplication;
import vmc.in.mrecorder.parser.Parser;
import vmc.in.mrecorder.parser.Requestor;
import vmc.in.mrecorder.service.CallRecorderServiceAll;
import vmc.in.mrecorder.util.JSONParser;
import vmc.in.mrecorder.util.SingleTon;
import vmc.in.mrecorder.util.Utils;


public class SyncAdapter extends AbstractThreadedSyncAdapter implements TAG {
    private final ContentResolver mContentResolver;
    private ArrayList<Model> callList;
    private ArrayList<Model> FiltercallList;
    private JSONObject response;
    private String authkey;
    private int offset = 0;
    private ArrayList<CallData> callDataArrayList;
    private String code, recording, mcubeRecording, workhour;

    private RequestQueue requestQueue;
    private SingleTon volleySingleton;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }


    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        int wifionly = Integer.parseInt(sharedPrefs.getString("prefSyncNetwork", "0"));

        Log.d(TAG, "Beginning network synchronization");
        // StartOrStopRecording();
        CallApplication.getInstance().isstartRecording();

        try {
            if (!CallRecorderServiceAll.recording && Utils.isLogin(getContext())) {
                LoadCalls();
            }

          //  callList = CallApplication.getWritabledatabase().getAllOfflineCalls();
            FiltercallList=CallApplication.getWritabledatabase().getAllOfflineCalls();
//            FiltercallList=new ArrayList<Model>();
//            for(int i =0 ;i<callList.size();i++){
//                FiltercallList.add(getFilterCallDetail(callList.get(i)));
//            }


            Log.d(TAG, "offline data Size" + FiltercallList.size());
            Log.d(TAG, "Location" + FiltercallList.get(0).getLocation());

            if (wifionly == 1 && Utils.hasWIFIConnection(getContext())) {

                Log.d(TAG + "WIFI", "Wifi only enabled");
                for (int i = 0; i < FiltercallList.size(); i++) {
                    if (!CallRecorderServiceAll.recording && Utils.isLogin(getContext())) {

                        if (new File(FiltercallList.get(i).getFilePath()).exists()) {
                            uploadMultipartData(FiltercallList.get(i), true);
                        } else {
                            uploadMultipartData(FiltercallList.get(i), false);

                        }


                    }
                }
            } else if (wifionly == 0) {
                Log.d(TAG + "WIFI", "Wifi only disabled");
                for (int i = 0; i < FiltercallList.size(); i++) {

                    if (!CallRecorderServiceAll.recording && Utils.isLogin(getContext())) {

                        if (new File(FiltercallList.get(i).getFilePath()).exists()) {
                            uploadMultipartData(FiltercallList.get(i), true);
                        } else {
                            uploadMultipartData(FiltercallList.get(i), false);

                        }


                    }
                }
            }
        } catch (Exception e) {
            // Log.d(TAG, e.getMessage().toString());
        }


    }

    private synchronized void LoadCalls() {
        volleySingleton = SingleTon.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor ed = sharedPrefs.edit();
        CallApplication.getInstance().startRecording();

        authkey = Utils.getFromPrefs(getContext(), AUTHKEY, "N/A");
        try {
            response = Requestor.requestGetCalls(requestQueue, GET_CALL_LIST, authkey, "10", offset + "",
                    CallApplication.getInstance().getDeviceId(), TYPE_ALL);
            Log.d("GetCalls", "" + response);

            if (response.has(CODE)) {
                code = response.getString(CODE);
                if (response.has(RECORDING)) {
                    recording = response.getString(RECORDING);
                    if (recording.equals("1")) {
                        ed.putBoolean("prefRecording", true);
                    } else {
                        ed.putBoolean("prefRecording", false);
                    }


                }
                if (response.has(MCUBECALLS)) {
                    mcubeRecording = response.getString(MCUBECALLS);
                    if (mcubeRecording.equals("1")) {
                        ed.putBoolean("prefMcubeRecording", true);
                    } else {
                        ed.putBoolean("prefMcubeRecording", false);
                    }

                }
                if (response.has(WORKHOUR)) {
                    workhour = response.getString(WORKHOUR);
                    if (workhour.equals("1")) {
                        ed.putBoolean("prefOfficeTimeRecording", true);
                    } else {
                        ed.putBoolean("prefOfficeTimeRecording", false);
                    }
                }
                ed.commit();
                if (code.equals("202") || code.equals("401")) {
                    Utils.isLogoutBackground(getContext());
                }
                if (code.equals("203")) {
                    // Utils.isLogoutBackground(getContext());
                    Log.d("NORECORD", "Record is not in Mcube contacts.");
                }

            }
            callDataArrayList = new ArrayList<CallData>();
            callDataArrayList = Parser.ParseData(response);
            Log.d(TAG, "ALL_CALLS " + callDataArrayList.size());
            CallApplication.getWritabledatabase().insertCallRecords(MDatabase.ALL, callDataArrayList, true);
        } catch (Exception e) {
            Log.d(TAG, "Error " + e.getMessage().toString());
        }
        try {


            response = Requestor.requestGetCalls(requestQueue, GET_CALL_LIST, authkey, "10", offset + "",
                    CallApplication.getInstance().getDeviceId(), TYPE_OUTGOING);

            if (response.has(CODE)) {
                code = response.getString(CODE);
                if (response.has(RECORDING)) {
                    recording = response.getString(RECORDING);
                    if (recording.equals("1")) {
                        ed.putBoolean("prefRecording", true);
                    } else {
                        ed.putBoolean("prefRecording", false);
                    }

                }
                if (response.has(MCUBECALLS)) {
                    mcubeRecording = response.getString(MCUBECALLS);
                    if (mcubeRecording.equals("1")) {
                        ed.putBoolean("prefMcubeRecording", true);
                    } else {
                        ed.putBoolean("prefMcubeRecording", false);
                    }

                }
                if (response.has(WORKHOUR)) {
                    workhour = response.getString(WORKHOUR);
                    if (workhour.equals("1")) {
                        ed.putBoolean("prefOfficeTimeRecording", true);
                    } else {
                        ed.putBoolean("prefOfficeTimeRecording", false);
                    }
                }
                ed.commit();
                if (code.equals("202") || code.equals("401")) {
                    Utils.isLogoutBackground(getContext());
                }
            }

            callDataArrayList = new ArrayList<CallData>();
            callDataArrayList = Parser.ParseData(response);
            Log.d(TAG, OUTGOING + callDataArrayList.size());
            CallApplication.getWritabledatabase().insertCallRecords(MDatabase.OUTBOUND, callDataArrayList, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            response = Requestor.requestGetCalls(requestQueue, GET_CALL_LIST, authkey, "10", offset + "",
                    CallApplication.getInstance().getDeviceId(), TYPE_INCOMING);

            if (response.has(CODE)) {
                code = response.getString(CODE);
                if (response.has(RECORDING)) {
                    recording = response.getString(RECORDING);
                    if (recording.equals("1")) {
                        ed.putBoolean("prefRecording", true);
                    } else {
                        ed.putBoolean("prefRecording", false);
                    }

                }
                if (response.has(MCUBECALLS)) {
                    mcubeRecording = response.getString(MCUBECALLS);
                    if (mcubeRecording.equals("1")) {
                        ed.putBoolean("prefMcubeRecording", true);
                    } else {
                        ed.putBoolean("prefMcubeRecording", false);
                    }

                }
                if (response.has(WORKHOUR)) {
                    workhour = response.getString(WORKHOUR);
                    if (workhour.equals("1")) {
                        ed.putBoolean("prefOfficeTimeRecording", true);
                    } else {
                        ed.putBoolean("prefOfficeTimeRecording", false);
                    }
                }
                ed.commit();
                if (code.equals("202") || code.equals("401")) {
                    Utils.isLogoutBackground(getContext());
                }
            }
            callDataArrayList = new ArrayList<CallData>();
            callDataArrayList = Parser.ParseData(response);
            Log.d(TAG, INCOMING + callDataArrayList.size());
            CallApplication.getWritabledatabase().insertCallRecords(MDatabase.INBOUND, callDataArrayList, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            response = Requestor.requestGetCalls(requestQueue, GET_CALL_LIST, authkey, "10", offset + "",
                    CallApplication.getInstance().getDeviceId(), TYPE_MISSED);

            if (response.has(CODE)) {
                code = response.getString(CODE);
                if (response.has(RECORDING)) {
                    recording = response.getString(RECORDING);
                    if (recording.equals("1")) {
                        ed.putBoolean("prefRecording", true);
                    } else {
                        ed.putBoolean("prefRecording", false);
                    }

                }
                if (response.has(MCUBECALLS)) {
                    mcubeRecording = response.getString(MCUBECALLS);
                    if (mcubeRecording.equals("1")) {
                        ed.putBoolean("prefMcubeRecording", true);
                    } else {
                        ed.putBoolean("prefMcubeRecording", false);
                    }

                }
                if (response.has(WORKHOUR)) {
                    workhour = response.getString(WORKHOUR);
                    if (workhour.equals("1")) {
                        ed.putBoolean("prefOfficeTimeRecording", true);
                    } else {
                        ed.putBoolean("prefOfficeTimeRecording", false);
                    }
                }
                ed.commit();
                if (code.equals("202") || code.equals("401")) {
                    Utils.isLogoutBackground(getContext());
                }
            }
            callDataArrayList = new ArrayList<CallData>();

            callDataArrayList = Parser.ParseData(response);
            CallApplication.getWritabledatabase().insertCallRecords(MDatabase.MISSED, callDataArrayList, true);
            Log.d(TAG, MISSED + callDataArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private void StartOrStopRecording() {
        if (Utils.isLogin(getContext())) {
            if (!Utils.isMyServiceRunning(CallRecorderServiceAll.class, getContext())) {
                CallApplication.getInstance().startRecording();
                Log.d(TAG, "service started");
            } else {
                Log.d(TAG, "service already started");
            }
        } else {
            if (Utils.isMyServiceRunning(CallRecorderServiceAll.class, getContext())) {
                CallApplication.getInstance().stopRecording();
                Log.d(TAG, "service stopped");
            } else {
                Log.d(TAG, "service already stopped");
            }
        }
    }

    private synchronized void uploadMultipartData(Model model, boolean fileExist) throws Exception {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor ed = sharedPrefs.edit();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SS");
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        int duration;
        if (fileExist) {
            try {
                MediaPlayer mp = MediaPlayer.create(getContext(), Uri.fromFile(model.getFile()));
                duration = mp.getDuration();
            } catch (Exception e) {
                duration = 0;
            }
            builder.addPart(UPLOADEDFILE, new FileBody(model.getFile()));
            Log.d(TAG, UPLOADEDFILE + ":" + model.getFile().getName());
            Log.d(TAG, DURATION + ":" + duration + "");
            builder.addPart(DURATION, new StringBody(duration + "", ContentType.TEXT_PLAIN));
            Long time = Long.valueOf(model.getTime()).longValue();
            long endtime = time + duration;
            builder.addPart(ENDTIME, new StringBody(sdf.format(new Date(endtime)), ContentType.TEXT_PLAIN));
            Log.d(TAG, ENDTIME + ":" + sdf.format(new Date(endtime)));
        }
        builder.addPart(AUTHKEY, new StringBody(Utils.getFromPrefs(getContext(), AUTHKEY, "n"), ContentType.TEXT_PLAIN));
        Log.d(TAG, AUTHKEY + ":" + Utils.getFromPrefs(getContext(), AUTHKEY, "n"));
        builder.addPart(DEVICE_ID, new StringBody(CallApplication.getInstance().getDeviceId(), ContentType.TEXT_PLAIN));
        Log.d(TAG, DEVICE_ID + ":" + CallApplication.getInstance().getDeviceId());
        builder.addPart(CALLTO, new StringBody(model.getPhoneNumber(), ContentType.TEXT_PLAIN));
        Log.d(TAG, CALLTO + ":" + model.getPhoneNumber());
        builder.addPart(STARTTIME, new StringBody(sdf.format(new Date(Long.parseLong(model.getTime()))), ContentType.TEXT_PLAIN));
        Log.d(TAG, STARTTIME + ":" + sdf.format(new Date(Long.parseLong(model.getTime()))));
        builder.addPart(CALLTYPEE, new StringBody(model.getCallType(), ContentType.TEXT_PLAIN));
        builder.addPart(CONTACTNAME, new StringBody(getContactName(model.getPhoneNumber()), ContentType.TEXT_PLAIN));
        Log.d(TAG, CALLTYPEE + ":" + model.getCallType());
        Log.d(TAG, "CONTACTNAME" + ":" + getContactName(model.getPhoneNumber()));
        builder.addPart(LOCATION, new StringBody(model.getLocation(), ContentType.TEXT_PLAIN));
        Log.d(TAG, LOCATION + ":" + model.getLocation());
        if (!fileExist) {
            //  builder.addPart(ENDTIME, new StringBody("0000000", ContentType.TEXT_PLAIN));
            builder.addPart(ENDTIME, new StringBody(sdf.format(new Date(Long.parseLong(model.getTime()))), ContentType.TEXT_PLAIN));
            Log.d(TAG, ENDTIME + ":" + sdf.format(new Date(Long.parseLong(model.getTime()))));
        }
        HttpEntity entity = builder.build();

        URL url = null;

        try {
            url = new URL(UPLOAD_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.addRequestProperty("Content-length", entity.getContentLength() + "");
            urlConnection.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());
            OutputStream os = urlConnection.getOutputStream();
            entity.writeTo(urlConnection.getOutputStream());
            os.close();
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String s = "";
            StringBuilder stringBuilder = new StringBuilder("");
            while ((s = bufferedReader.readLine()) != null) {
                stringBuilder.append(s);
            }
            String serverResponseMessage = stringBuilder.toString();
            Log.d(TAG, "RESPONSE:" + serverResponseMessage);
            String code;
            JSONObject obj = new JSONObject(serverResponseMessage);
            if (obj.has(CODE)) {
                code = obj.getString(CODE);

                if (response.has(RECORDING)) {
                    recording = response.getString(RECORDING);
                    if (recording.equals("1")) {
                        ed.putBoolean("prefRecording", true);
                    } else {
                        ed.putBoolean("prefRecording", false);
                    }

                }
                if (response.has(MCUBECALLS)) {
                    mcubeRecording = response.getString(MCUBECALLS);
                    if (mcubeRecording.equals("1")) {
                        ed.putBoolean("prefMcubeRecording", true);
                    } else {
                        ed.putBoolean("prefMcubeRecording", false);
                    }

                }
                if (response.has(WORKHOUR)) {
                    workhour = response.getString(WORKHOUR);
                    if (workhour.equals("1")) {
                        ed.putBoolean("prefOfficeTimeRecording", true);
                    } else {
                        ed.putBoolean("prefOfficeTimeRecording", false);
                    }
                }
                ed.commit();
                Log.d(TAG, "RESPONSE CODE:" + code);
                if (code.equals("400")) {
                    CallApplication.getWritabledatabase().delete(model.getId());//from db
                    if (new File(model.getFilePath()).exists()) {
                        new File(model.getFilePath()).delete();//from internal storage
                        Log.d(TAG, "FILE DELETED" + ":" + model.getFile().getName());
                    }
                    Log.d(TAG, "RECODRD DELETED" + ":" + model.getFile().getName());
                }
                if (code.equals("202") || code.equals("401")) {
                    Utils.isLogoutBackground(getContext());
                }


            }


            Log.d(TAG, "Response :" + serverResponseMessage);


        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }

    }

    //outgoing
    public synchronized Model getFilterCallDetail(Model model1) {
        String whereClause = CallLog.Calls.NUMBER + " = " + model1.getPhoneNumber() + " AND " + CallLog.Calls.TYPE + "=" + CallLog.Calls.OUTGOING_TYPE;
        StringBuffer sb = new StringBuffer();

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
           // return ;
        }
        Cursor managedCursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null, whereClause,
                null, CallLog.Calls.DATE + " DESC");
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        ArrayList<Model> templog = new ArrayList<Model>();

        sb.append("Call Details :");
        while (managedCursor.moveToNext()) {

            Model model = new Model();
            String phNumber = managedCursor.getString(number);
            model.setPhoneNumber(phNumber);
            String callType = managedCursor.getString(type);
            model.setCallType(callType);
            String callDate = managedCursor.getString(date);
            model.setTime(callDate);
            Date callDayTime = new Date(Long.valueOf(callDate));
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
            String exacttime = sdf.format(callDayTime);
            //  Log.d("Numbers", exacttime);
            String callDuration = managedCursor.getString(duration);
            model.setDuration(callDuration);
            templog.add(model);


//            String dir = null;
//            int dircode = Integer.parseInt(callType);
//            switch (dircode) {
//                case CallLog.Calls.OUTGOING_TYPE:
//                    dir = "OUTGOING";
//                    break;
//
//                case CallLog.Calls.INCOMING_TYPE:
//                    dir = "INCOMING";
//                    break;
//
//                case CallLog.Calls.MISSED_TYPE:
//                    dir = "MISSED";
//                    break;
//            }
//            sb.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
//                    + dir + " \nCall Date:--- " + callDayTime + " \nCall time:--- " + exacttime
//                    + " \nCall duration in sec :--- " + callDuration);
//            sb.append("\n----------------------------------");
        }
        for (int i = 0; i < templog.size(); i++) {

            Model model = templog.get(i);
            if (model.getPhoneNumber().equals(model1.getPhoneNumber())) {
                //     Log.d("Numbers", "Number equal" + model1.getPhoneNumber());
                Date callDayTime = new Date(Long.valueOf(model.getTime()));
                Date callDayTime1 = new Date(Long.valueOf(model1.getTime()));
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
                String time1 = sdf.format(callDayTime);
                String time2 = sdf.format(callDayTime1);


                if (time1.equals(time2)) {

                    if (Integer.parseInt(model.getDuration()) == 0) {
                        Log.d("Numbers", "Call To be Deleted");
                        Log.d("Numbers", model1.getPhoneNumber() + " " + time2);
                        Log.d("Numbers", "Duration " + model.getDuration());
                       // CallApplication.getWritabledatabase().delete(model1.getId());//from db
                        if (new File(model1.getFilePath()).exists()) {
                            new File(model1.getFilePath()).delete();//from internal storage
                            Log.d("Numbers", "FILE DELETED" + ":" + model1.getFile().getName());
                        }
                        Log.d("Numbers", "RECODRD DELETED" + ":" + model1.getFile().getName());
                    }
                }


            }


        }
        managedCursor.close();


        return model1;

    }

    public String getContactName(String snumber) throws Exception {
        ContentResolver cr = getContext().getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(snumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = "";
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return contactName;
    }
}
