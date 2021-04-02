package com.phatvu1294.blkcapture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class TextAdapter extends BaseAdapter {
    /* Các biến sử dụng */
    private static Activity a = null;
    private static LayoutInflater inflater = null;
    private final ArrayList<String> titleList;
    private final ArrayList<String> tickList;

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Khởi tạo class */
    public TextAdapter(Activity activity, ArrayList<String> titleList,
                       ArrayList<String> tickList) {
        this.a = activity;
        this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.titleList = titleList;
        this.tickList = tickList;
    }

    /* Hàm lấy số lượng */
    @Override
    public int getCount() {
        return titleList.size();
    }

    /* Hàm lấy thành phần */
    @Override
    public Object getItem(int position) {
        return titleList.get(position);
    }

    /* Hàm lấy id thành phần */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /* Hàm lấy View */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextAdapter.ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new TextAdapter.ViewHolder();
            convertView = inflater.inflate(R.layout.text_item, parent, false);

            /* Ánh xạ các thành phần */
            viewHolder.txtTextTitle = (TextView) convertView.findViewById(R.id.txtTextTitle);
            viewHolder.imgTextTick = (ImageView) convertView.findViewById(R.id.imgTextTick);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (TextAdapter.ViewHolder) convertView.getTag();
        }

        /* Đặt nội dung */
        viewHolder.txtTextTitle.setText(titleList.get(position));
        viewHolder.imgTextTick.setVisibility(View.GONE);

        /* Đặt đã xem */
        try {
            new textTickLoaderTask(viewHolder.imgTextTick)
                    .execute(titleList.get(position));
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Trả về view */
        return convertView;
    }

    /*============================================================================================*/
    /* Hàm bất đồng bộ */
    /*============================================================================================*/

    /* Task kiểm tra và đặt hiển thị tick từ danh sách */
    private class textTickLoaderTask extends AsyncTask<String,
            Void, Boolean> {
        /* Thành phần đồng bộ */
        private final WeakReference<ImageView> imageViewReference;

        /* Thành phần đồng bộ */
        public textTickLoaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /* Thực thi tiến trình nền */
        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = false;
            try {
                /* Tìm trong tệp Drive */
                for (String driveName : tickList) {
                    if (params[0].equals(driveName)) {
                        result = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            }
            return result;
        }

        /* Thực thi xong tiến trình nền */
        @Override
        protected void onPostExecute(Boolean isTick) {
            /* Nếu tiến trình nền bị huỷ thì đặt về giá trị khởi tạo */
            if (isCancelled()) {
                isTick = false;
            }

            try {
                if (imageViewReference != null) {
                    ImageView imageView = imageViewReference.get();
                    if (imageView != null) {
                        if (isTick == true) {
                            imageView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*============================================================================================*/
    /* Class khác */
    /*============================================================================================*/

    /* Class View Holder */
    private class ViewHolder {
        TextView txtTextTitle;
        ImageView imgTextTick;
    }
}
