package com.phatvu1294.blkcapture;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    /*============================================================================================*/
    /* C??c th??nh ph???n to??n c???c */
    /*============================================================================================*/

    /* Kh???i t???o class */
    Libraries libraries = new Libraries();

    /* Bi???n ??nh x??? c??c th??nh ph???n */
    public static BottomNavigationView bottomNavigation;

    /* Bi???n MQTT */
    public static MqttAndroidClient mqttClient;
    public static String mqttTopic;

    /* Bi???n Google Drive */
    public static DriveService driveService;

    /* Bi???n ???????ng d???n l??m vi???c tr??n m??y */
    public static String folderWorkingPath;

    /* Bi???n tho??t ???ng d???ng sau khi nh???n hai l???n */
    boolean doubleBackToExitPressedOnce = false;

    /*============================================================================================*/
    /* S??? ki???n */
    /*============================================================================================*/

    /* S??? ki???n khi Activity ???????c t???o */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            /* ?????c d??? li???u ng?????i d??ng */
            readUserPreferences(MainActivity.this);

            /* T???o k??nh th??ng b??o */
            createNotificationChannel();

            /* L???y ???????ng d???n */
            folderWorkingPath = getExternalFilesDir(null).getAbsolutePath();
            mqttTopic = getString(R.string.mqtt_topic);

            /* ??nh x??? c??c th??nh ph???n */
            bottomNavigation = findViewById(R.id.bottom_navigation);

            /* ?????t s??? ki???n c??c th??nh ph???n */
            bottomNavigation.setOnNavigationItemSelectedListener(navSelectedListener);

            /* Ch???n fragment l???n ?????u hi???n th??? */
            bottomNavigation.setSelectedItemId(R.id.nav_request);

            /* Y??u c???u ????ng nh???p Google Drive */
            googleDriveRequestSignIn();

            /* N???u h??? ??i???u h??nh >= marshmallow, y??u c???u c???p quy???n truy c???p */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_DENIED) {
                    /* N???u kh??ng c?? quy???n truy c???p th?? y??u c???u */
                    String[] permission = {Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE};

                    /* Hi???n th??? popup y??u c???u c???p quy???n */
                    requestPermissions(permission, libraries.PERMISSON_CODE);
                } else {
                    /* N???u ???? ???????c c???p quy???n th?? t???o th?? m???c l??m vi???c */
                }
            } else {
                /* N???u h??? ??i???u h??nh < marshallow th?? t???o th?? m???c l??m vi???c */
            }

            /* Kh???i t???o m???ng danh s??ch */
            RequestFragment.requestProductNameList = new ArrayList<>();
            RequestFragment.requestProductCodeList = new ArrayList<>();
            RequestFragment.requestProductLocationList = new ArrayList<>();
            DoneFragment.doneProductNameList = new ArrayList<>();
            DoneFragment.doneProductCodeList = new ArrayList<>();
            DoneFragment.doneProductLocationList = new ArrayList<>();

            /* Kh???i t???o client MQTT */
            mqttClient = new MqttAndroidClient(getApplicationContext(),
                    getString(R.string.mqtt_host), MqttClient.generateClientId());

            /* ?????t Callback MQTT */
            mqttClient.setCallback(mqttCallback);

            /* Th??? k???t n???i MQTT */
            new tryConnectMQTTTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* S??? ki???n khi thanh ??i???u h?????ng d?????i ???????c l???a ch???n */
    private BottomNavigationView.OnNavigationItemSelectedListener navSelectedListener = new
            BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    try {
                        Fragment selectedFragment = null;

                        switch (item.getItemId()) {
                            case R.id.nav_request:
                                DetailFragment.detailType = libraries.DETAIL_NONE;
                                selectedFragment = new RequestFragment();
                                break;

                            case R.id.nav_done:
                                DetailFragment.detailType = libraries.DETAIL_NONE;
                                selectedFragment = new DoneFragment();
                                break;

                            case R.id.nav_setting:
                                DetailFragment.detailType = libraries.DETAIL_NONE;
                                selectedFragment = new SettingFragment();
                                break;
                        }

                        /* Chuy???n sang fragment m???i */
                        getSupportFragmentManager().beginTransaction().setCustomAnimations(
                                R.anim.slide_in,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out  // popExit
                        ).replace(R.id.fragment_container, selectedFragment).commit();

                        /* L??u tr???ng th??i listview (v??? tr??, ...) */
                        DetailFragment.lsvRequestState =
                                RequestFragment.lsvProductNameRequestList.onSaveInstanceState();
                        DetailFragment.lsvDoneState =
                                DoneFragment.lsvProductNameDoneList.onSaveInstanceState();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return true;
                }
            };

    /* S??? ki???n g???i v??? khi k???t n???i c???a MQTT */
    private IMqttActionListener mqttActionCallback = new
            IMqttActionListener() {
                /* S??? ki???n khi MQTT k???t n???i th??nh c??ng */
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        /* ????ng k?? topic MQTT */
                        mqttClient.subscribe(getString(R.string.mqtt_topic), 0);
                    } catch (MqttException e) {
                    }
                }

                /* S??? ki???n khi MQTT k???t n???i th???t b???i */
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            };

    /* S??? ki???n g???i v??? c???a MQTT */
    private MqttCallback mqttCallback = new
            MqttCallback() {
                /* S??? ki???n khi MQTT m???t k???t n???i */
                @Override
                public void connectionLost(Throwable cause) {
                    try {
                        /* Task k???t n???i l???i MQTT */
                        new tryConnectMQTTTask().execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                /* S??? ki???n khi c?? tin nh???n ?????n */
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    try {
                        /* X??? l?? d??? li???u nh???n ???????c */
                        processDataFromServer(message);

                        /* H??m t???i danh s??ch y??u c???u v?? ho??n th??nh */
                        loadListViewProductNameRequestDoneList();

                        /* ?????t l???i tr???ng th??i c??c listview */
                        if (DetailFragment.lsvRequestState != null) {
                            RequestFragment.lsvProductNameRequestList.onRestoreInstanceState(
                                    DetailFragment.lsvRequestState);
                        }
                        if (DetailFragment.lsvDoneState != null) {
                            DoneFragment.lsvProductNameDoneList.onRestoreInstanceState(
                                    DetailFragment.lsvDoneState);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                /* S??? ki???n khi tin nh???n ???????c chuy???n ??i */
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            };

    /* S??? ki???n khi popup y??u c???u quy???n truy c???p tr??? v??? k???t qu??? */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == libraries.PERMISSON_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /* N???u ???????c c???p quy???n th?? t???o th?? m???c l??m vi???c */
            } else {
                /* B??? t??? ch???i quy???n truy c???p */
            }
        }
    }

    /* S??? ki???n khi popup ????ng nh???p Google Drive tr??? v??? k???t qu??? */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == libraries.GDRIVE_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    /* Tr??nh qu???n l?? Google Drive */
                    googleDriveHandleSignInIntent(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* S??? ki???n khi n??t quay l???i ???????c nh???n */
    @Override
    public void onBackPressed() {
        try {
            if (doubleBackToExitPressedOnce == true) {
                super.onBackPressed();
                return;
            } else {
                if (DetailFragment.detailType == libraries.DETAIL_REQUEST) {
                    bottomNavigation.setSelectedItemId(R.id.nav_request);
                } else if (DetailFragment.detailType == libraries.DETAIL_DONE) {
                    bottomNavigation.setSelectedItemId(R.id.nav_done);
                }
            }

            doubleBackToExitPressedOnce = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*============================================================================================*/
    /* H??m */
    /*============================================================================================*/

    /* H??m x??? l?? tin nh???n nh???n ???????c t??? MQTT */
    private void processDataFromServer(MqttMessage message) {
        /* T??ch l???y h??ng s???n ph???m */
        String rows[] = message.toString().split(Pattern.quote("|"));

        /* Xo?? danh s??ch */
        RequestFragment.requestProductNameList.clear();
        RequestFragment.requestProductCodeList.clear();
        RequestFragment.requestProductLocationList.clear();
        DoneFragment.doneProductNameList.clear();
        DoneFragment.doneProductCodeList.clear();
        DoneFragment.doneProductLocationList.clear();

        /* X??? l?? t???ng h??ng */
        for (String row : rows) {
            /* T??ch l???y c???t */
            String columns[] = row.split(Pattern.quote("\\"), 4);

            /* G??n c???t v??? ????ng ch???c n??ng */
            String command = columns[0].trim();
            String productName = columns[1].trim();
            String productCode = columns[2].trim();
            String productLocation = columns[3].trim();

            /* N???u l???nh l?? xo?? (-) */
            if (command.equals("-")) {
                /* T???o m???ng danh s??ch c???n xo?? */
                ArrayList<String> removeList = new ArrayList<>();
                /* N???u c?? gi?? tr??? c???n xo?? trong danh s??ch y??u c???u th?? th??m v??o danh s??ch c???n xo?? */
                for (String item : RequestFragment.requestProductNameList) {
                    if (item.equals(productName)) {
                        removeList.add(item);
                    }
                }
                /* Xo?? s???n ph???m c?? trong danh s??ch c???n xo?? kh???i danh s??ch y??u c???u */
                for (String item : removeList) {
                    RequestFragment.requestProductCodeList.remove(
                            RequestFragment.requestProductNameList.indexOf(item));
                    RequestFragment.requestProductLocationList.remove(
                            RequestFragment.requestProductNameList.indexOf(item));
                    RequestFragment.requestProductNameList.remove(item);
                }

                /* Bi???n ki???m tra t???n t???i */
                boolean exists = false;
                /* N???u c?? trong danh s??ch ho??n th??nh th?? ?????t t???n t???i */
                for (String item : DoneFragment.doneProductNameList) {
                    if (item.equals(productName)) {
                        exists = true;
                    }
                }
                /* N???u kh??ng t???n t???i th?? th??m v??o danh s??ch ho??n th??nh */
                if (exists == false) {
                    DoneFragment.doneProductNameList.add(productName);
                    DoneFragment.doneProductCodeList.add(productCode);
                    DoneFragment.doneProductLocationList.add(productLocation);
                }
            }

            /* N???u l?? l???nh th??m th??m (+) */
            else if (command.equals("+")) {
                /* Bi???n ki???m tra t???n t???i */
                boolean exists = false;
                /* N???u c?? trong danh s??ch y??u c???u th?? ?????t t???n t???i */
                for (String item : RequestFragment.requestProductNameList) {
                    if (item.equals(productName)) {
                        exists = true;
                    }
                }
                /* N???u kh??ng t???n t???i th?? th??m v??o danh s??ch y??u c???u */
                if (exists == false) {
                    RequestFragment.requestProductNameList.add(productName);
                    RequestFragment.requestProductCodeList.add(productCode);
                    RequestFragment.requestProductLocationList.add(productLocation);
                }
            }
        }
    }

    /* H??m public danh s??ch t???i MQTT */
    public static void publicDataToServer() {
        String pubString = new String();

        for (int i = 0; i < RequestFragment.requestProductNameList.size(); i++) {
            if (i < RequestFragment.requestProductNameList.size() - 1) {
                pubString += "+\\" + RequestFragment.requestProductNameList.get(i)
                        + "\\" + RequestFragment.requestProductCodeList.get(i)
                        + "\\" + RequestFragment.requestProductLocationList.get(i) + "|";
            } else {
                pubString += "+\\" + RequestFragment.requestProductNameList.get(i)
                        + "\\" + RequestFragment.requestProductCodeList.get(i)
                        + "\\" + RequestFragment.requestProductLocationList.get(i);
            }
        }

        if (DoneFragment.doneProductNameList.size() > 0
                && RequestFragment.requestProductNameList.size() > 0) {
            pubString += "|";
        }

        for (int i = 0; i < DoneFragment.doneProductNameList.size(); i++) {
            if (i < DoneFragment.doneProductNameList.size() - 1) {
                pubString += "-\\" + DoneFragment.doneProductNameList.get(i)
                        + "\\" + DoneFragment.doneProductCodeList.get(i)
                        + "\\" + DoneFragment.doneProductLocationList.get(i) + "|";
            } else {
                pubString += "-\\" + DoneFragment.doneProductNameList.get(i)
                        + "\\" + DoneFragment.doneProductCodeList.get(i)
                        + "\\" + DoneFragment.doneProductLocationList.get(i);
            }
        }

        try {
            mqttClient.publish(mqttTopic, pubString.getBytes(), 0, true);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /* H??m y??u c???u ????ng nh???p Google Drive */
    private void googleDriveRequestSignIn() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient driveClient = GoogleSignIn.getClient(MainActivity.this,
                googleSignInOptions);
        startActivityForResult(driveClient.getSignInIntent(), libraries.GDRIVE_CODE);
    }

    /* H??m tr??nh qu???n l?? Google Drive */
    private void googleDriveHandleSignInIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                                MainActivity.this,
                                Collections.singleton(DriveScopes.DRIVE_FILE));

                        credential.setSelectedAccount(googleSignInAccount.getAccount());

                        Drive googleDriveService = new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(), credential)
                                .setApplicationName(getString(R.string.app_name))
                                .build();

                        driveService = new DriveService(googleDriveService);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    /* H??m t???o k??nh th??ng b??o */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(
                    getString(R.string.channel_id), name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /* H??m hi???n th??? th??ng b??o */
    private void showNotification(int notifyId, String title, String message) {
        /* X??? l?? s??? ki???n ph???n h???i t??? th??ng b??o */
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this,
                libraries.NOTIFY_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* T???o th??ng b??o */
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(MainActivity.this, getString(R.string.channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        /* Hi???n th??? th??ng b??o */
        NotificationManagerCompat notificationManagerCompt = NotificationManagerCompat
                .from(MainActivity.this);
        notificationManagerCompt.notify(notifyId, builder.build());
    }

    /* H??m ghi d??? li???u ng?????i d??ng */
    public static void writeUserPreferences(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                activity.getString(R.string.app_name), Context.MODE_PRIVATE);

        /* Kh???i t???o editor ch???nh s???a */
        SharedPreferences.Editor editor = sharedPreferences.edit();

        /* Ghi danh s??ch y??u c???u ???? xem */
        Set<String> set = new HashSet<String>();
        set.addAll(RequestFragment.requestProductNameViewedList);
        editor.putStringSet(activity.getString(R.string.key_request_viewed_list), set);

        /* C???p nh???t thay ?????i */
        editor.commit();
    }

    /* H??m ?????c d??? li???u ng?????i d??ng */
    public static void readUserPreferences(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                activity.getString(R.string.app_name), Context.MODE_PRIVATE);

        /* ?????c danh s??ch y??u c???u ???? xem */
        Set<String> set = sharedPreferences.getStringSet(
                activity.getString(R.string.key_request_viewed_list), null);
        RequestFragment.requestProductNameViewedList = new ArrayList<String>();
        if (set != null) {
            for (String str : set) {
                RequestFragment.requestProductNameViewedList.add(str);
            }
        }
    }

    /* H??m t???i danh s??ch y??u c???u ho??n th??nh v??o danh s??ch xem y??u c???u ho??n th??nh */
    private void loadListViewProductNameRequestDoneList() {
        /* T???o Adapter ????? d??? li???u */
        TextAdapter requestAdapter = new TextAdapter(MainActivity.this,
                RequestFragment.requestProductNameList,
                RequestFragment.requestProductNameViewedList);

        TextAdapter doneAdapter = new TextAdapter(MainActivity.this,
                DoneFragment.doneProductNameList,
                new ArrayList<>());

        /* ????? d??? li???u t??? Adapter v??o ListView */
        RequestFragment.lsvProductNameRequestList.setAdapter(requestAdapter);
        DoneFragment.lsvProductNameDoneList.setAdapter(doneAdapter);
    }

    /*============================================================================================*/
    /* H??m b???t ?????ng b??? */
    /*============================================================================================*/

    /* Task th??? k???t n???i MQTT */
    private class tryConnectMQTTTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            /* Th??? k???t n???i MQTT */
            try {
                /* K???t n???i MQTT v???i tu??? ch???n */
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(getString(R.string.mqtt_username));
                options.setPassword(getString(R.string.mqtt_password).toCharArray());
                options.setKeepAliveInterval(60);
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);

                /* K???t n???i MQTT */
                IMqttToken token = mqttClient.connect(options);

                /* ?????t callback k???t n???i MQTT */
                token.setActionCallback(mqttActionCallback);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
