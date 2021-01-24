package com.sn4k3ch4rm3r.filcupdatemanager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private String pack = "hu.filcnaplo.ellenorzo";

    private String installed;
    private Release latest;

    private long downloadID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                1);

        httpCall("https://api.github.com/repos/filc/filcnaplo/releases");

        registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        registerReceiver(onInstall, filter);
        checkInstalled();

        final Button installButton = findViewById(R.id.install);
        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkInstalled();
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/filcnaplo-"+latest.getVersion()+".apk";
                File file = new File(path);
                Log.i("Location", path);
                if(!file.exists()){
                    Log.i("InstallManager", "Downloading");
                    DownloadManager downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    Uri uri = Uri.parse(latest.getDownloadURL());
                    Log.i("Download", latest.getDownloadURL());

                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setTitle("Filc Napló " + latest.getVersion());
//                    request.setDescription("Letöltés");//request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"filcnaplo-"+latest.getVersion()+".apk");
                    downloadID = downloadmanager.enqueue(request);
                }
                else {
                    installAPK();
                }
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }

    private void checkInstalled() {
        TextView installed_tv = findViewById(R.id.installed_vers);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(pack, 0);
            installed = pInfo.versionName;
            installed_tv.setText("Telepített verzió: " + installed);
        } catch (PackageManager.NameNotFoundException e) {
            installed_tv.setText("Nincs Telepítve");
            installed = "none";
        }
    }

    private void httpCall(String url) {

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        getLatestRelease(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // enjoy your error status
            }
        });

        queue.add(stringRequest);
    }

    public void getLatestRelease(String json) {
        try {
            JSONArray versions = new JSONArray(json);
            int i = 0;
            JSONObject ver = null;
            do {
                ver = (JSONObject) versions.get(i++);
                Log.i(ver.getString("tag_name"), String.valueOf(ver.getBoolean("prerelease")));
            } while (ver.getBoolean("prerelease") || ver.getBoolean("draft"));

            latest = new Release(ver.getString("tag_name"), ((JSONObject)((JSONArray) ver.get("assets")).get(0)).getString("browser_download_url"));

            TextView tv = findViewById(R.id.textview1);
            tv.setText("Legújabb verzió: " + latest.getVersion());
        } catch (Exception ignored) { }
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                Toast.makeText(MainActivity.this, "Letöltés befejeződött", Toast.LENGTH_LONG).show();
                installAPK();
            }
        }
    };

    private BroadcastReceiver onInstall = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getData().getSchemeSpecificPart().equals(pack))
                checkInstalled();
                if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)){
                    installAPK();
                }
            }
        };

    public void installAPK() {
        if(installed != "none") {
            Log.i("InstallManager", "Uninstalling");
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:"+pack));
            startActivity(intent);
        }
        else {
            Log.i("InstallManager", "Installing");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/filcnaplo-" + latest.getVersion() + ".apk")), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}