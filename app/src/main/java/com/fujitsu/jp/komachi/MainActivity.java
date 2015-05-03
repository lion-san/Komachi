package com.fujitsu.jp.komachi;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, UsbReceiver.UsbReceiverActivity, RemoconConst,
        WebViewFragment.OnFragmentInteractionListener
{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    /**
     *
     * User constraint value
     */
    /** Activityの継続性を維持するためのID */
    private String activityId = null;
    private IrrcUsbDriver irrcUsbDriver;
    private UsbReceiver usbReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        //==== Remocon ================================
        // Activityの継続性を維持するためのID取得。@see #onSaveInstanceState()
        String aid = (savedInstanceState != null) ? savedInstanceState.getString(ACTIVITY_ID) : null;
        activityId = (aid != null) ? aid : ("@" + this.hashCode());

        irrcUsbDriver = ((RemoconApplication) getApplication()).getIrrcUsbDriver(this);
        usbReceiver = UsbReceiver.init(this, irrcUsbDriver);

        //赤外線の受信モードON
        //受信モードで待機
        if(irrcUsbDriver.isReady()) {
            Toast.makeText(this, "USBデバイスを認識", Toast.LENGTH_SHORT).show();

            irrcUsbDriver.startReceiveIr(new IrrcUsbDriver.IrrcResponseListener() {
                @Override
                public void onIrrcResponse(byte[] data) {
                    irrcUsbDriver.getReceiveIrData(new IrrcUsbDriver.IrrcResponseListener() {
                        @Override
                        public void onIrrcResponse(byte[] data) {
                            if(data != null) {
                                info(data.toString());
                            }
                            irrcUsbDriver.endReceiveIr(null);
                        }
                    }, 5000);
                }
            });
        }
        else {
            Toast.makeText(this, "USBデバイスが見つかりません", Toast.LENGTH_SHORT).show();
        }

}
    /**
     * Activityの再構築に備えて自身にIDを保存して置く。
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ACTIVITY_ID, activityId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!irrcUsbDriver.hasDevice()) {
            if (!irrcUsbDriver.findDevice()) {
                Log.i("MainActivity", "Not found USB device.");
                errorDialog("Not found USB device.");
            }
        }
        //viewPagerAdapter.onResume();
    }


    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        if(position == 0){
            fragmentManager.beginTransaction()
                    .replace(R.id.container, WebViewFragment.newInstance("file:///android_asset/index.html"))
                    .commit();
        }
        else {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                    .commit();
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }*/
        switch(item.getItemId()){
            case R.id.action_settings:
                Toast.makeText(this, "リモコンの初期設定を開始します", Toast.LENGTH_LONG).show();

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, WebViewFragment.newInstance("file:///android_asset/setting.html"))
                        .commit();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void errorDialog(String message) {
        alertDalog("Error!",message);
    }
    private void alertDalog(String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
    }

    public void info(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}