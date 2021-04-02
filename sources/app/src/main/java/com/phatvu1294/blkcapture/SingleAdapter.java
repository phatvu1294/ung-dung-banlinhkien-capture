package com.phatvu1294.blkcapture;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SingleAdapter extends BaseAdapter {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Các biến sử dụng */
    private static Activity a = null;
    private static LayoutInflater inflater = null;
    private final ArrayList<String> titleList;
    private final ArrayList<String> dateList;
    private final ArrayList<String> sizeList;
    private final ArrayList<String> pictureList;
    private final ArrayList<String> tickList;

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Khởi tạo class */
    public SingleAdapter(Activity activity, ArrayList<String> titleList,
                         ArrayList<String> dateList,
                         ArrayList<String> sizeList,
                         ArrayList<String> pictureList,
                         ArrayList<String> tickList) {
        this.a = activity;
        this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.titleList = titleList;
        this.dateList = dateList;
        this.sizeList = sizeList;
        this.pictureList = pictureList;
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
        ViewHolder viewHolder;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.single_item, parent, false);

            /* Ánh xạ các thành phần */
            viewHolder.txtSingleTitle = (TextView) convertView.findViewById(R.id.txtSingleTitle);
            viewHolder.txtSingleDate = (TextView) convertView.findViewById(R.id.txtSingleDate);
            viewHolder.txtSingleSize = (TextView) convertView.findViewById(R.id.txtSingleSize);
            viewHolder.imgSinglePicture = (ImageView) convertView
                    .findViewById(R.id.imgSinglePicture);
            viewHolder.imgSingleTick = (ImageView) convertView.findViewById(R.id.imgSingleTick);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        /* Đặt nội dung */
        viewHolder.txtSingleTitle.setText(titleList.get(position));
        viewHolder.txtSingleDate.setText(dateList.get(position));
        viewHolder.txtSingleSize.setText(sizeList.get(position));

        /* Đặt hình ảnh */
        try {
            new singlePictureLoaderTask(viewHolder.imgSinglePicture)
                    .execute(pictureList.get(position));
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* Đặt hiển thị tick */
        viewHolder.imgSingleTick.setVisibility(View.GONE);
        try {
            new singleTickLoaderTask(viewHolder.imgSingleTick).execute(titleList.get(position));
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
    private class singleTickLoaderTask extends AsyncTask<String,
            Void, Boolean> {
        /* Thành phần đồng bộ */
        private final WeakReference<ImageView> imageViewReference;

        /* Thành phần đồng bộ */
        public singleTickLoaderTask(ImageView imageView) {
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

    /* Task tải ảnh vào trình xem ảnh */
    private class singlePictureLoaderTask extends AsyncTask<String, Void, Bitmap> {
        /* Thành phần đồng bộ */
        private final WeakReference<ImageView> imageViewReference;

        /* Thành phần đồng bộ */
        public singlePictureLoaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /* Thực thi tiến trình nền */
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap result = null;
            try {
                /* Giải mã ảnh từ đường dẫn */
                Bitmap bmp = BitmapFactory.decodeFile(params[0]);

                /* Tính toán tỷ lệ */
                int originalWidth = bmp.getWidth();
                int originalHeight = bmp.getHeight();
                float scale = originalWidth / 100;

                /* Tạo ảnh với kích thước mới */
                result = Bitmap.createScaledBitmap(bmp,
                        (int) (originalWidth / scale),
                        (int) (originalHeight / scale), true);
            } catch (Exception e) {
                e.printStackTrace();
                result = null;
            }

            /* Trả về định dạng ảnh */
            return result;
        }

        /* Thực thi tiến trình nền xong */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            /* Nếu tiến trình bị huỷ thì đặt về giá trị khởi tạo */
            if (isCancelled()) {
                bitmap = null;
            }

            try {
                if (imageViewReference != null) {
                    ImageView imageView = imageViewReference.get();
                    if (imageView != null) {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
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
        TextView txtSingleTitle;
        TextView txtSingleDate;
        TextView txtSingleSize;
        ImageView imgSingleTick;
        ImageView imgSinglePicture;
    }
}
