package vmc.in.mrecorder.activity;
//aaa

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import de.hdodenhof.circleimageview.CircleImageView;
import vmc.in.mrecorder.R;
import vmc.in.mrecorder.callbacks.Constants;
import vmc.in.mrecorder.callbacks.TAG;
import vmc.in.mrecorder.entity.Model;
import vmc.in.mrecorder.fragment.AllCalls;
import vmc.in.mrecorder.fragment.DownloadFile;
import vmc.in.mrecorder.fragment.InboundCalls;
import vmc.in.mrecorder.fragment.MissedCalls;
import vmc.in.mrecorder.fragment.OutboundCalls;
import vmc.in.mrecorder.myapplication.CallApplication;
import vmc.in.mrecorder.service.CallRecorderServiceAll;
import vmc.in.mrecorder.syncadapter.SyncUtils;
import vmc.in.mrecorder.util.ConnectivityReceiver;
import vmc.in.mrecorder.util.CustomTheme;
import vmc.in.mrecorder.util.Utils;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Home extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener, NavigationView.OnNavigationItemSelectedListener, TAG,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DownloadFile.DownloadFileTask {

    private Toolbar mToolbar;
    public FloatingActionButton floatingActionButton, floatingActionButtonSync;
    private String titles[] = {"ALL", "INBOUND", "OUTBOUND", "MISSED"};
    private int[] tabIcons = {
            R.drawable.ic_all_home,
            R.drawable.ic_call_incoming_home,
            R.drawable.ic_call_outgoing_home,
            R.drawable.ic_call_missed_home
    };
    private NavigationView mDrawer;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private MyPagerAdapter myPagerAdapter;
    private boolean doubleBackToExitPressedOnce;
    private TextView user, email;
    private double latitude, longitude;
    public FloatingActionsMenu fabMenu;
    public CoordinatorLayout coordinatorLayout;
    private Snackbar snack;
    private CircleImageView userType;
    private String usertype;
    private ProgressDialog mProgressDialog;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    PendingResult<LocationSettingsResult> result;
    final static int REQUEST_LOCATION = 199;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private DownloadFile downloadFragment;
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private String fileName;

    private int completed = 0;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CustomTheme.onActivityCreateSetTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if (Utils.tabletSize(Home.this) < 6.0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordi_layout);
        String Firsttym = Utils.getFromPrefs(Home.this, FIRST_TYME, DEFAULT);
        if (Firsttym.equals(DEFAULT)) {

            if (!Utils.isMyServiceRunning(CallRecorderServiceAll.class, Home.this)) {
                CallApplication.getInstance().startRecording();
            }
            Utils.saveToPrefs(Home.this, FIRST_TYME, "TRUE");
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Utils.checkAndRequestPermissions(Home.this);

        }

        mDrawer = (NavigationView) findViewById(R.id.nav_view);
        mDrawer.setNavigationItemSelectedListener(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.drawer_open,
                R.string.drawer_close);
        //mDrawerLayout.setDrawerListener(drawerToggle);
        mDrawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        myPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(myPagerAdapter);
        //  mTabLayout.setTabsFromPagerAdapter(myPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        // setupWithViewPager is enough.
        setupTabIcons();
        mDrawer.setItemIconTintList(null);
        View header = mDrawer.getHeaderView(0);

        if (android.os.Build.VERSION.SDK_INT > 19) {
            header.setBackgroundResource(R.drawable.side_nav_bar);

        }


        user = (TextView) header.findViewById(R.id.tv_name);
        email = (TextView) header.findViewById(R.id.tv_email);
        userType = (CircleImageView) header.findViewById(R.id.usertype);

        usertype = Utils.getFromPrefs(this, USERTYPE, DEFAULT);
        if (usertype.equals(DEFAULT) || usertype.equals("0")) {
            userType.setVisibility(View.GONE);
            getSupportActionBar().setTitle("MTracker User");
        } else {
            getSupportActionBar().setTitle("MTracker Admin");
        }


        String useremail = Utils.getFromPrefs(this, EMAIL, DEFAULT);
        String username = Utils.getFromPrefs(this, NAME, DEFAULT);
        user.setText("Hi " + username);
        email.setText(useremail);
        mDrawer.setNavigationItemSelectedListener(this);

        floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButtonSync = (FloatingActionButton) findViewById(R.id.fab_sync);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  Utils.setRecording(Home.this);
                startActivity(new Intent(Home.this, Settings.class));
                fabMenu.collapse();
            }
        });
        floatingActionButtonSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncUtils.TriggerRefresh();
                fabMenu.collapse();
            }
        });

        showprefrenceValues();
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Constants.position = position;
                onNavigationItemSelected(mDrawer.getMenu().getItem(position));


            }

            @Override
            public void onPageScrollStateChanged(int state) {


            }
        });
        final FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frame_layout);
        frameLayout.getBackground().setAlpha(0);
        fabMenu = (FloatingActionsMenu) findViewById(R.id.fab_menu);
        fabMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                frameLayout.getBackground().setAlpha(240);
                frameLayout.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        fabMenu.collapse();
                        return true;
                    }
                });
            }

            @Override
            public void onMenuCollapsed() {
                frameLayout.getBackground().setAlpha(0);
                frameLayout.setOnTouchListener(null);
            }
        });
        // fabMenu.setBackground(Color.parseColor("#795548"));

        GoogleApiClient();
    }


    public void onShareFile(final String fileName) {
        if (ConnectivityReceiver.isConnected()) {
            downloadFragment = (DownloadFile) getSupportFragmentManager().findFragmentByTag(TAG_TASK_FRAGMENT);
            downloadFragment = new DownloadFile();
            Bundle bundle = new Bundle();
            bundle.putString("FILE", fileName);
            downloadFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().add(downloadFragment, TAG_TASK_FRAGMENT).commit();
        } else {
            Snackbar snack = Snackbar.make(coordinatorLayout, "No Internet Connection", Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.text_tryAgain), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            onShareFile(fileName);

                        }
                    })
                    .setActionTextColor(ContextCompat.getColor(Home.this, R.color.primary));
            View view = snack.getView();
            TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            snack.show();
        }

    }


    private void setupTabIcons() {
        mTabLayout.getTabAt(0).setIcon(tabIcons[0]);
        mTabLayout.getTabAt(1).setIcon(tabIcons[1]);
        mTabLayout.getTabAt(2).setIcon(tabIcons[2]);
        mTabLayout.getTabAt(3).setIcon(tabIcons[3]);
    }

    private void GoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(Home.this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(30 * 1000);
        mLocationRequest.setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                //final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        //...
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    Home.this,
                                    REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        //...
                        break;
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult()", Integer.toString(resultCode));
        Log.d("onActivityResult()", Integer.toString(requestCode));

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made
                        //Toast.makeText(Home.this, "Location enabled by user!", Toast.LENGTH_LONG).show();

                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        //  Toast.makeText(Home.this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
            case SHARE_CALL: {
                Log.d("onActivityResult()", "SHARE SUCCESS");
                deleteFiles();
                break;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("MTracker");
        alertDialog.setIcon(R.mipmap.ic_launcher);
        // Setting Dialog Message
        alertDialog.setMessage("Are you sure you want to logout?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Utils.isLogout(Home.this);

            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (!isConnected) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }

        showSnack(isConnected);
    }


    private void showSnack(boolean isConnected) {
        String message;
        int color;
        if (!isConnected) {
            message = "Sorry! Not connected to internet";
            color = Color.RED;

            Snackbar snackbar = Snackbar
                    .make(coordinatorLayout, message, Snackbar.LENGTH_LONG);

            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(color);
            snackbar.show();
        }
    }


    @Override
    public void ondownloadFilePreExecute() {


        initProgress();


    }

    private void initProgress() {
        mProgressDialog = new ProgressDialog(Home.this) {
            @Override
            public void onBackPressed() {
                mProgressDialog.dismiss();
                showDowanlodAlert();
            }
        };
        mProgressDialog.setMessage("Loading");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setProgress(0);
        mProgressDialog.setMax(100);
        mProgressDialog.show();
    }

    @Override
    public void ondownloadFileProgressUpdate(int percent) {
        if (mProgressDialog == null) {
            initProgress();
        }
        completed = percent;
        mProgressDialog.setProgress(percent);
    }


    @Override
    public void ondownloadFileCancelled() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        Log.d("SHARE", "Download Cancelled");


    }

    @Override
    public void ondownloadFilePostExecute(File file) {
        Log.d("PATH", file.getAbsolutePath());
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (file.exists()) {
            Uri uri = Uri.parse("file://" + file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setType("audio/*");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            this.startActivityForResult(Intent.createChooser(share, "Share MTracker Record "), SHARE_CALL);

        }
    }


    class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment myFragment = null;
            switch (position) {
                case 0:
                    myFragment = new AllCalls();
                    break;
                case 1:
                    myFragment = new InboundCalls();
                    break;
                case 2:
                    myFragment = new OutboundCalls();
                    break;
                case 3:
                    myFragment = new MissedCalls();
                    break;
            }
            return myFragment;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }

        @Override
        public int getCount() {
            // return titles.length;
            return tabIcons.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //  return titles[position];
            return null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        showprefrenceValues();
        deleteFiles();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            Log.d("DIALOG", "DIALOG DISMISS");
        }
        CallApplication.getInstance().setConnectivityListener(this);
        if (!Utils.isLogin(Home.this)) {
            Intent intent = new Intent(Home.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
            Home.this.startActivity(intent);
            Log.d("Logout", "Logout on resume");
        }

    }

    public void showDowanlodAlert() {
        alertDialog = new AlertDialog.Builder(Home.this).create();
        alertDialog.setTitle("MTracker");
        alertDialog.setIcon(R.mipmap.ic_launcher);
        alertDialog.setMessage("Cancel Downloading ?");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (downloadFragment != null) {
                            downloadFragment.onCancelTask();
                        }


                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {

                        dialog.dismiss();
                        if ((mProgressDialog != null && !mProgressDialog.isShowing()) && completed < 100) {
                            mProgressDialog.show();
                        }
                    }
                });


        alertDialog.show();


    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            showDowanlodAlert();
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press again to exit.", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSettingsAlert();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        item.setChecked(true);
        navigate(item.getItemId());
        return true;
    }

    public void playAudio(String url) {
        if (url != null && url.length() > 4) {
            Log.d("AUDIO", url);
            // fileName=url;
            // Toast.makeText(HomeActivity.this, url, Toast.LENGTH_LONG).show();
            //Uri myUri = Uri.parse("http://mcube.vmctechnologies.com/sounds/99000220411460096169.wav");
            Uri myUri = Uri.parse(STREAM_TRACKER + url);
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(myUri, "audio/*");
            startActivity(intent);
        }
    }


    private void navigate(int mSelectedId) {
        if (mSelectedId == R.id.all) {
            setSelection(0);
        }
        if (mSelectedId == R.id.inbound) {
            setSelection(1);
        }
        if (mSelectedId == R.id.outbound) {
            setSelection(2);
        }
        if (mSelectedId == R.id.missed) {
            setSelection(3);
        }
        if (mSelectedId == R.id.cal_log) {
            startActivity(new Intent(Home.this, ContactsActivity.class));
        }
        if (mSelectedId == R.id.nav_help_feedback) {
            startActivity(new Intent(Home.this, Feedback.class));
        }
        if (mSelectedId == R.id.settings) {
            startActivity(new Intent(Home.this, Settings.class));
        }
        if (mSelectedId == R.id.anyl_type) {
            startActivity(new Intent(Home.this, AnalyticsByType.class));
        }
        if (mSelectedId == R.id.anyl_employee) {
            startActivity(new Intent(Home.this, AnalyticsByEmp.class));
        }


        invalidateOptionsMenu();
    }

    private void setSelection(int item) {
        hideDrawer();
        mTabLayout.setScrollPosition(item, 0f, true);
        mViewPager.setCurrentItem(item);
    }


    private void hideDrawer() {
        this.mDrawerLayout.closeDrawer(GravityCompat.START);
    }


    public void showprefrenceValues() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        StringBuilder builder = new StringBuilder();

        builder.append("\n Audio Source: "
                + sharedPrefs.getString("audiosource", "NULL"));

        builder.append("\n Audio Format:"
                + sharedPrefs.getString("audioformat", "NULL"));

        builder.append("\n Sync Frequency: "
                + sharedPrefs.getString("prefSyncFrequency", "NULL"));

        Log.d("SETTINGS", builder.toString());
    }

    public void deleteFiles() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(Home.this);

        File sampleDir;
        File sample;
        String selectedFolder = sharedPrefs.getString("store_path", "null");
        if (selectedFolder.equals("null")) {
            sampleDir = Environment.getExternalStorageDirectory();
            sample = new File(sampleDir.getAbsolutePath() + "/data/share/");
            if (!sample.exists()) sample.mkdirs();

        } else {
            sampleDir = new File(selectedFolder);
            sample = new File(sampleDir.getAbsolutePath() + "/data/share/");
            if (!sample.exists()) sample.mkdirs();
        }

        List<File> files = Utils.getListFiles(sample);
        for (int i = 0; i < files.size(); i++) {
            files.get(i).delete();
            Log.d("SHARE", files.get(i).getName() + " Deleted..");
        }
    }

}
