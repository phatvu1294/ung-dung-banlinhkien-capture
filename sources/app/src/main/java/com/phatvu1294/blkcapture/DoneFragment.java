package com.phatvu1294.blkcapture;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class DoneFragment extends Fragment {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Khởi tạo class */
    Libraries libraries = new Libraries();

    /* Biến ánh xạ các thành phần */
    public static ListView lsvProductNameDoneList;

    /* Các biến danh sách hoàn thành */
    public static ArrayList<String> doneProductNameList;
    public static ArrayList<String> doneProductCodeList;
    public static ArrayList<String> doneProductLocationList;

    /*============================================================================================*/
    /* Sự kiện */
    /*============================================================================================*/

    /* Sự kiện khi Fragment được tạo */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_done, container, false);

        try {
            /* Ánh xạ ListView */
            lsvProductNameDoneList = (ListView) layout.findViewById(R.id.lsvProductNameRequestDone);

            /* Đặt sự kiện */
            lsvProductNameDoneList.setOnItemClickListener(lsvProductNameDoneItemClickListener);

            /* Hàm load danh sách hoàn thành */
            loadListViewProductNameDoneList();

            /* Đặt lại trạng thái các listview */
            if (DetailFragment.lsvRequestState != null) {
                RequestFragment.lsvProductNameRequestList.onRestoreInstanceState(
                        DetailFragment.lsvRequestState);
            }
            if (DetailFragment.lsvDoneState != null) {
                lsvProductNameDoneList.onRestoreInstanceState(
                        DetailFragment.lsvDoneState);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return layout;
    }

    /* Sự kiện khi item của danh sách yêu cầu được nhấp chọn */
    private AdapterView.OnItemClickListener lsvProductNameDoneItemClickListener = new
            AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        /* Đặt các thành phần của Detail */
                        DetailFragment.detailProductIndex = position;
                        DetailFragment.detailProductName = doneProductNameList.get(
                                DetailFragment.detailProductIndex);
                        DetailFragment.detailProductCode = doneProductCodeList.get(
                                DetailFragment.detailProductIndex);
                        DetailFragment.detailProductLocation = doneProductLocationList.get(
                                DetailFragment.detailProductIndex);
                        DetailFragment.detailType = libraries.DETAIL_DONE;

                        /* Lưu trạng thái listview (vị trí, ...) */
                        DetailFragment.lsvDoneState =
                                lsvProductNameDoneList.onSaveInstanceState();
                        DetailFragment.lsvRequestState =
                                RequestFragment.lsvProductNameRequestList.onSaveInstanceState();

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

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Hàm tải danh sách hoành thành vào danh sách xem hoàn thành */
    private void loadListViewProductNameDoneList() {
        /* Tạo Adapter đổ dữ liệu */
        TextAdapter doneAdapter = new TextAdapter(getActivity(),
                doneProductNameList,
                new ArrayList<>());

        /* Đổ dữ liệu từ Adapter vào ListView */
        lsvProductNameDoneList.setAdapter(doneAdapter);
    }
}
