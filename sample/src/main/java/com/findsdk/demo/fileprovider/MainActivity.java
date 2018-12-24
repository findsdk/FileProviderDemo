package com.findsdk.demo.fileprovider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import com.findsdk.library.fileprovider.FileUtils;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    final int permissionRequestId_create = 100;
    final int permissionRequestId_delete = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        textView = (TextView) findViewById(R.id.text1);
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(permissionRequestId_create);
            }
        });
        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(permissionRequestId_delete);
            }
        });

    }

    private void checkPermission(int requestCode) {
        if (PermissionUtil.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode)) {
            if (requestCode == permissionRequestId_create)
                createFile();
            else deleteAll();
        }
    }

    private void createFile() {
        File dir = FileUtils.getBasePath(this);
        String name = "tmp_" + System.currentTimeMillis();
        File file = new File(dir, name);
        if (!file.exists()) {
            try {
                boolean flag = file.createNewFile();
                textView.append("\n");
                textView.append(file.getAbsolutePath() + " create " + flag);
            } catch (IOException e) {
                e.printStackTrace();
                textView.append(file.getAbsolutePath() + " create exception");
            }
        }
    }

    private void deleteAll() {
        File dir = FileUtils.getBasePath(this);
        deleteDir(dir);
    }

    public boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        String name = dir.getAbsolutePath();
        boolean flag = dir.delete();
        textView.append("\n");
        textView.append(name + " delete " + flag);
        return flag;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults != null) {
            switch (requestCode) {
                case permissionRequestId_create: {
                    boolean ret = true;
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            ret = false;
                        }
                    }
                    if (ret)
                        createFile();
                    else {
                        Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case permissionRequestId_delete: {
                    boolean ret = true;
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            ret = false;
                        }
                    }
                    if (ret)
                        deleteAll();
                    else {
                        Toast.makeText(this, "no permission", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        }
    }
}
