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
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Khởi tạo class */
    Libraries libraries = new Libraries();

    /* Biến ánh xạ các thành phần */
    public static BottomNavigationView bottomNavigation;

    /* Biến MQTT */
    public static MqttAndroidClient mqttClient;
    public static String mqttTopic;

    /* Biến Google Drive */
    public static DriveService driveService;

    /* Biến đường dẫn làm việc trên máy */
    public static String folderWorkingPath;

    /* Biến thoát ứng dụng sau khi nhấn hai lần */
    boolean doubleBackToExitPressedOnce = false;

    /*============================================================================================*/
    /* Sự kiện */
    /*============================================================================================*/

    /* Sự kiện khi Activity được tạo */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            /* Đọc dữ liệu người dùng */
            readUserPreferences(MainActivity.this);

            /* Tạo kênh thông báo */
            createNotificationChannel();

            /* Lấy đường dẫn */
            folderWorkingPath = getExternalFilesDir(null).getAbsolutePath();
            mqttTopic = getString(R.string.mqtt_topic);

            /* Ánh xạ các thành phần */
            bottomNavigation = findViewById(R.id.bottom_navigation);

            /* Đặt sự kiện các thành phần */
            bottomNavigation.setOnNavigationItemSelectedListener(navSelectedListener);

            /* Chọn fragment lần đầu hiển thị */
            bottomNavigation.setSelectedItemId(R.id.nav_request);

            /* Yêu cầu đăng nhập Google Drive */
            googleDriveRequestSignIn();

            /* Nếu hệ điều hành >= marshmallow, yêu cầu cấp quyền truy cập */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_DENIED) {
                    /* Nếu không có quyền truy cập thì yêu cầu */
                    String[] permission = {Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE};

                    /* Hiển thị popup yêu cầu cấp quyền */
                    requestPermissions(permission, libraries.PERMISSON_CODE);
                } else {
                    /* Nếu đã được cấp quyền thì tạo thư mục làm việc */
                }
            } else {
                /* Nếu hệ điều hành < marshallow thì tạo thư mục làm việc */
            }

            /* Khởi tạo mảng danh sách */
            RequestFragment.requestProductNameList = new ArrayList<>();
            RequestFragment.requestProductCodeList = new ArrayList<>();
            RequestFragment.requestProductLocationList = new ArrayList<>();
            DoneFragment.doneProductNameList = new ArrayList<>();
            DoneFragment.doneProductCodeList = new ArrayList<>();
            DoneFragment.doneProductLocationList = new ArrayList<>();

            /* Khởi tạo client MQTT */
            mqttClient = new MqttAndroidClient(getApplicationContext(),
                    getString(R.string.mqtt_host), MqttClient.generateClientId());

            /* Đặt Callback MQTT */
            mqttClient.setCallback(mqttCallback);

            /* Thử kết nối MQTT */
            new tryConnectMQTTTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Sự kiện khi thanh điều hướng dưới được lựa chọn */
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

                        /* Chuyển sang fragment mới */
                        getSupportFragmentManager().beginTransaction().setCustomAnimations(
                                R.anim.slide_in,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out  // popExit
                        ).replace(R.id.fragment_container, selectedFragment).commit();

                        /* Lưu trạng thái listview (vị trí, ...) */
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

    /* Sự kiện gọi về khi kết nối của MQTT */
    private IMqttActionListener mqttActionCallback = new
            IMqttActionListener() {
                /* Sự kiện khi MQTT kết nối thành công */
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        /* Đăng ký topic MQTT */
                        mqttClient.subscribe(getString(R.string.mqtt_topic), 0);
                    } catch (MqttException e) {
                    }
                }

                /* Sự kiện khi MQTT kết nối thất bại */
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                }
            };

    /* Sự kiện gọi về của MQTT */
    private MqttCallback mqttCallback = new
            MqttCallback() {
                /* Sự kiện khi MQTT mất kết nối */
                @Override
                public void connectionLost(Throwable cause) {
                    try {
                        /* Task kết nối lại MQTT */
                        new tryConnectMQTTTask().execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                /* Sự kiện khi có tin nhắn đến */
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    try {
                        /* Xử lý dữ liệu nhận được */
                        processDataFromServer(message);

                        /* Hàm tải danh sách yêu cầu và hoàn thành */
                        loadListViewProductNameRequestDoneList();

                        /* Đặt lại trạng thái các listview */
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

                /* Sự kiện khi tin nhắn được chuyển đi */
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            };

    /* Sự kiện khi popup yêu cầu quyền truy cập trả về kết quả */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == libraries.PERMISSON_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /* Nếu được cấp quyền thì tạo thư mục làm việc */
            } else {
                /* Bị từ chối quyền truy cập */
            }
        }
    }

    /* Sự kiện khi popup đăng nhập Google Drive trả về kết quả */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == libraries.GDRIVE_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    /* Trình quản lý Google Drive */
                    googleDriveHandleSignInIntent(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* Sự kiện khi nút quay lại được nhấn */
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
    /* Hàm */
    /*============================================================================================*/

    /* Hàm xử lý tin nhắn nhận được từ MQTT */
    private void processDataFromServer(MqttMessage message) {
        /* Tách lấy hàng sản phẩm */
        String rows[] = message.toString().split(Pattern.quote("|"));

        /* Xoá danh sách */
        RequestFragment.requestProductNameList.clear();
        RequestFragment.requestProductCodeList.clear();
        RequestFragment.requestProductLocationList.clear();
        DoneFragment.doneProductNameList.clear();
        DoneFragment.doneProductCodeList.clear();
        DoneFragment.doneProductLocationList.clear();

        /* Xử lý từng hàng */
        for (String row : rows) {
            /* Tách lấy cột */
            String columns[] = row.split(Pattern.quote("\\"), 4);

            /* Gán cột về đúng chức năng */
            String command = columns[0].trim();
            String productName = columns[1].trim();
            String productCode = columns[2].trim();
            String productLocation = columns[3].trim();

            /* Nếu lệnh là xoá (-) */
            if (command.equals("-")) {
                /* Tạo mảng danh sách cần xoá */
                ArrayList<String> removeList = new ArrayList<>();
                /* Nếu có giá trị cần xoá trong danh sách yêu cầu thì thêm vào danh sách cần xoá */
                for (String item : RequestFragment.requestProductNameList) {
                    if (item.equals(productName)) {
                        removeList.add(item);
                    }
                }
                /* Xoá sản phẩm có trong danh sách cần xoá khỏi danh sách yêu cầu */
                for (String item : removeList) {
                    RequestFragment.requestProductCodeList.remove(
                            RequestFragment.requestProductNameList.indexOf(item));
                    RequestFragment.requestProductLocationList.remove(
                            RequestFragment.requestProductNameList.indexOf(item));
                    RequestFragment.requestProductNameList.remove(item);
                }

                /* Biến kiểm tra tồn tại */
                boolean exists = false;
                /* Nếu có trong danh sách hoàn thành thì đặt tồn tại */
                for (String item : DoneFragment.doneProductNameList) {
                    if (item.equals(productName)) {
                        exists = true;
                    }
                }
                /* Nếu không tồn tại thì thêm vào danh sách hoàn thành */
                if (exists == false) {
                    DoneFragment.doneProductNameList.add(productName);
                    DoneFragment.doneProductCodeList.add(productCode);
                    DoneFragment.doneProductLocationList.add(productLocation);
                }
            }

            /* Nếu là lệnh thêm thêm (+) */
            else if (command.equals("+")) {
                /* Biến kiểm tra tồn tại */
                boolean exists = false;
                /* Nếu có trong danh sách yêu cầu thì đặt tồn tại */
                for (String item : RequestFragment.requestProductNameList) {
                    if (item.equals(productName)) {
                        exists = true;
                    }
                }
                /* Nếu không tồn tại thì thêm vào danh sách yêu cầu */
                if (exists == false) {
                    RequestFragment.requestProductNameList.add(productName);
                    RequestFragment.requestProductCodeList.add(productCode);
                    RequestFragment.requestProductLocationList.add(productLocation);
                }
            }
        }
    }

    /* Hàm public danh sách tới MQTT */
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

    /* Hàm yêu cầu đăng nhập Google Drive */
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

    /* Hàm trình quản lý Google Drive */
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

    /* Hàm tạo kênh thông báo */
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

    /* Hàm hiển thị thông báo */
    private void showNotification(int notifyId, String title, String message) {
        /* Xử lý sự kiện phản hồi từ thông báo */
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this,
                libraries.NOTIFY_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Tạo thông báo */
        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(MainActivity.this, getString(R.string.channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        /* Hiển thị thông báo */
        NotificationManagerCompat notificationManagerCompt = NotificationManagerCompat
                .from(MainActivity.this);
        notificationManagerCompt.notify(notifyId, builder.build());
    }

    /* Hàm ghi dữ liệu người dùng */
    public static void writeUserPreferences(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                activity.getString(R.string.app_name), Context.MODE_PRIVATE);

        /* Khởi tạo editor chỉnh sửa */
        SharedPreferences.Editor editor = sharedPreferences.edit();

        /* Ghi danh sách yêu cầu đã xem */
        Set<String> set = new HashSet<String>();
        set.addAll(RequestFragment.requestProductNameViewedList);
        editor.putStringSet(activity.getString(R.string.key_request_viewed_list), set);

        /* Cập nhật thay đổi */
        editor.commit();
    }

    /* Hàm đọc dữ liệu người dùng */
    public static void readUserPreferences(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(
                activity.getString(R.string.app_name), Context.MODE_PRIVATE);

        /* Đọc danh sách yêu cầu đã xem */
        Set<String> set = sharedPreferences.getStringSet(
                activity.getString(R.string.key_request_viewed_list), null);
        RequestFragment.requestProductNameViewedList = new ArrayList<String>();
        if (set != null) {
            for (String str : set) {
                RequestFragment.requestProductNameViewedList.add(str);
            }
        }
    }

    /* Hàm tải danh sách yêu cầu hoàn thành vào danh sách xem yêu cầu hoàn thành */
    private void loadListViewProductNameRequestDoneList() {
        /* Tạo Adapter đổ dữ liệu */
        TextAdapter requestAdapter = new TextAdapter(MainActivity.this,
                RequestFragment.requestProductNameList,
                RequestFragment.requestProductNameViewedList);

        TextAdapter doneAdapter = new TextAdapter(MainActivity.this,
                DoneFragment.doneProductNameList,
                new ArrayList<>());

        /* Đổ dữ liệu từ Adapter vào ListView */
        RequestFragment.lsvProductNameRequestList.setAdapter(requestAdapter);
        DoneFragment.lsvProductNameDoneList.setAdapter(doneAdapter);
    }

    /*============================================================================================*/
    /* Hàm bất đồng bộ */
    /*============================================================================================*/

    /* Task thử kết nối MQTT */
    private class tryConnectMQTTTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            /* Thử kết nối MQTT */
            try {
                /* Kết nối MQTT với tuỳ chọn */
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(getString(R.string.mqtt_username));
                options.setPassword(getString(R.string.mqtt_password).toCharArray());
                options.setKeepAliveInterval(60);
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);

                /* Kết nối MQTT */
                IMqttToken token = mqttClient.connect(options);

                /* Đặt callback kết nối MQTT */
                token.setActionCallback(mqttActionCallback);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
