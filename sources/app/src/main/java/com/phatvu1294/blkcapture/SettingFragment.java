package com.phatvu1294.blkcapture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingFragment extends Fragment {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Biến ánh xạ các thành phần */


    /*============================================================================================*/
    /* Sự kiện */
    /*============================================================================================*/

    /* Sự kiện khi Fragment được tạo */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout =  inflater.inflate(R.layout.fragment_setting, container, false);

        try {
            /* Ánh xạ các thành phần */

        } catch (Exception e) {
            e.printStackTrace();
        }

        return layout;
    }
}
