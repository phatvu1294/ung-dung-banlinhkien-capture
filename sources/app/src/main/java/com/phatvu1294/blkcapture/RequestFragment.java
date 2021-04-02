package com.phatvu1294.blkcapture;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public class RequestFragment extends Fragment {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Khởi tạo class */
    Libraries libraries = new Libraries();

    /* Biến ánh xạ các thành phần */
    public static ListView lsvProductNameRequestList;

    /* Các biến danh sách yêu cầu */
    public static ArrayList<String> requestProductNameList;
    public static ArrayList<String> requestProductCodeList;
    public static ArrayList<String> requestProductLocationList;
    public static ArrayList<String> requestProductNameViewedList;

    /*============================================================================================*/
    /* Sự kiện */
    /*============================================================================================*/

    /* Sự kiện khi Fragment được tạo */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_request, container, false);

        try {
            /* Ánh xạ các thành phần */
            lsvProductNameRequestList = (ListView) layout.findViewById(
                    R.id.lsvProductNameRequestList);

            /* Đặt sự kiện các thành phần */
            lsvProductNameRequestList.setOnItemClickListener(lsvProductNameRequestItemClickListener);

            /* Hàm tải danh sách hoàn thành */
            loadListViewProductNameRequestList();

            /* Đặt lại trạng thái các listview */
            if (DetailFragment.lsvRequestState != null) {
                lsvProductNameRequestList.onRestoreInstanceState(
                        DetailFragment.lsvRequestState);
            }
            if (DetailFragment.lsvDoneState != null) {
                DoneFragment.lsvProductNameDoneList.onRestoreInstanceState(
                        DetailFragment.lsvDoneState);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return layout;
    }

    /* Sự kiện khi item của danh sách yêu cầu được nhấp chọn*/
    private AdapterView.OnItemClickListener lsvProductNameRequestItemClickListener = new
            AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        /* Nếu hệ điều hành >= marshmallow, yêu cầu cấp quyền truy cập */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(getActivity(), Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_DENIED ||
                                    checkSelfPermission(getActivity(),
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                            PackageManager.PERMISSION_DENIED ||
                                    checkSelfPermission(getActivity(),
                                            Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                            PackageManager.PERMISSION_DENIED) {
                                /* Nếu không có quyền truy cập thì yêu cầu */
                                String[] permission = {Manifest.permission.CAMERA,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE};

                                /* Hiển thị popup yêu cầu cấp quyền */
                                requestPermissions(permission, libraries.PERMISSON_CODE);
                            } else {
                                /* Nếu đã được cấp quyền thì tạo thư mục làm việc */
                                libraries.createFolderStorage(MainActivity.folderWorkingPath,
                                        requestProductNameList.get(
                                                DetailFragment.detailProductIndex));
                            }
                        } else {
                            /* Nếu đã được cấp quyền thì tạo thư mục làm việc */
                            libraries.createFolderStorage(MainActivity.folderWorkingPath,
                                    requestProductNameList.get(DetailFragment.detailProductIndex));
                        }

                        /* Đặt product Index */
                        DetailFragment.detailProductIndex = position;
                        DetailFragment.detailProductName = requestProductNameList.get(
                                DetailFragment.detailProductIndex);
                        DetailFragment.detailProductCode = requestProductCodeList.get(
                                DetailFragment.detailProductIndex);
                        DetailFragment.detailProductLocation = requestProductLocationList.get(
                                DetailFragment.detailProductIndex);
                        DetailFragment.detailType = libraries.DETAIL_REQUEST;

                        /* Chuyển về Fragment Detail */
                        getFragmentManager().beginTransaction().setCustomAnimations(
                                R.anim.slide_in,  // enter
                                R.anim.fade_out,  // exit
                                R.anim.fade_in,   // popEnter
                                R.anim.slide_out  // popExit
                        ).replace(R.id.fragment_container, new DetailFragment()).commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi popup yêu cầu quyền truy cập trả về kết quả */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == libraries.PERMISSON_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    /* Nếu được cấp quyền thì tạo thư mục làm việc */
                    libraries.createFolderStorage(MainActivity.folderWorkingPath,
                            requestProductNameList.get(DetailFragment.detailProductIndex));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                /* Bị từ chối quyền truy cập */
            }
        }
    }

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Hàm tải danh sách yêu cầu vào danh sách xem yêu cầu */
    private void loadListViewProductNameRequestList() {
        /* Tạo Adapter đổ dữ liệu */
        TextAdapter requestAdapter = new TextAdapter(getActivity(),
                requestProductNameList,
                requestProductNameViewedList);

        /* Đổ dữ liệu từ Adapter vào ListView */
        lsvProductNameRequestList.setAdapter(requestAdapter);
    }
}
