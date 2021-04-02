package com.phatvu1294.blkcapture;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.app.Activity.RESULT_OK;
import static androidx.core.content.ContextCompat.checkSelfPermission;

public class DetailFragment extends Fragment {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Khởi tạo class */
    Libraries libraries = new Libraries();

    /* Biến ánh xạ các thành phần */
    public static TextView txtProductName;
    public static TextView txtProductCode;
    public static TextView txtProductLocation;
    public static ImageButton btnBackPrevious;
    public static ImageButton btnCaptureProduct;
    public static ImageButton btnMarkAsDone;
    public static ImageButton btnRequestAgain;
    public static ImageButton btnClearProductRequest;
    public static ImageButton btnClearProductDone;
    public static ImageButton btnUploadToDrive;
    public static ListView lsvProductPictureList;

    /* Các biến danh sách hình ảnh */
    public static ArrayList<String> fileProductNameList;
    public static ArrayList<String> fileProductDateList;
    public static ArrayList<String> fileProductSizeList;
    public static ArrayList<String> fileProductPictureList;

    /* Các biến chi tiết của sản phẩm */
    public static int detailType;
    public static int detailProductIndex;
    public static String detailProductName;
    public static String detailProductCode;
    public static String detailProductLocation;

    /* Hộp thoại xem ảnh */
    private Dialog dlPictureViewer;

    /* Biến hình ảnh */
    private Uri imageUri;

    /* Biến vị trí toàn cục */
    private int deletePos;
    public static Parcelable lsvRequestState;
    public static Parcelable lsvDoneState;

    /*============================================================================================*/
    /* Sự kiện */
    /*============================================================================================*/

    /* Sự kiện khi Fragment được tạo */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_detail, container, false);

        try {
            /* Ánh xạ thành phần */
            txtProductName = (TextView) layout.findViewById(R.id.txtProductName);
            txtProductCode = (TextView) layout.findViewById(R.id.txtProductCode);
            txtProductLocation = (TextView) layout.findViewById(R.id.txtProductLocation);
            btnBackPrevious = (ImageButton) layout.findViewById(R.id.btnBackPrevious);
            btnCaptureProduct = (ImageButton) layout.findViewById(R.id.btnCaptureProduct);
            btnMarkAsDone = (ImageButton) layout.findViewById(R.id.btnMarkAsDone);
            btnRequestAgain = (ImageButton) layout.findViewById(R.id.btnRequestAgain);
            btnClearProductRequest = (ImageButton) layout.findViewById(R.id.btnClearProductRequest);
            btnClearProductDone = (ImageButton) layout.findViewById(R.id.btnClearProductDone);
            btnUploadToDrive = (ImageButton) layout.findViewById(R.id.btnUploadToDrive);
            lsvProductPictureList = (ListView) layout.findViewById(R.id.lsvProductPictureList);

            /* Đặt sự kiện các thành phần */
            btnBackPrevious.setOnClickListener(btnBackPreviousClickListener);
            btnCaptureProduct.setOnClickListener(btnCaptureProductClickListener);
            btnMarkAsDone.setOnClickListener(btnMarkAsDoneClickListener);
            btnRequestAgain.setOnClickListener(btnRequestAgainClickListener);
            btnClearProductRequest.setOnClickListener(btnClearProductRequestClickListener);
            btnClearProductDone.setOnClickListener(btnClearProductDoneClickListener);
            btnUploadToDrive.setOnClickListener(btnUploadToDriveClickListener);
            lsvProductPictureList.setOnItemClickListener(lsvProductPictureItemClickListener);
            lsvProductPictureList.setOnItemLongClickListener(lsvProductPictureItemLongClickListener);

            /* Lấy dữ liệu từ Main Activity và đổ vào txtProductName */
            txtProductName.setText(detailProductName);
            txtProductCode.setText(detailProductCode);
            txtProductLocation.setText(detailProductLocation);

            /* Tuỳ chọn các nút nhấn cho mỗi loại detail */
            if (detailType == libraries.DETAIL_REQUEST) {
                /* Ẩn các thành phần không cần thiết */
                btnRequestAgain.setVisibility(View.GONE);
                btnClearProductDone.setVisibility(View.GONE);

                /* Biến kiểm tra tồn tại */
                boolean exists = false;
                /* Nếu có trong danh sách yêu cầu đã xem thì đặt tồn tại */
                for (String item : RequestFragment.requestProductNameViewedList) {
                    if (item.equals(detailProductName)) {
                        exists = true;
                    }
                }
                /* Nếu không tồn tại thì thêm vào danh sách yêu cầu đã xem */
                if (exists == false) {
                    RequestFragment.requestProductNameViewedList.add(detailProductName);
                }

                /* Ghi danh sách yêu cầu đã xem */
                MainActivity.writeUserPreferences(getActivity());

                /* Tải danh sách ảnh từ folder */
                new loadListViewProductPictureListTask().execute();
            } else if (detailType == libraries.DETAIL_DONE) {
                /* Ẩn các thành phần không cần thiết */
                btnCaptureProduct.setVisibility(View.GONE);
                btnMarkAsDone.setVisibility(View.GONE);
                btnClearProductRequest.setVisibility(View.GONE);
                btnUploadToDrive.setVisibility(View.GONE);
                lsvProductPictureList.setVisibility(View.GONE);
            }

            /* Lưu trạng thái listview (vị trí, ...) */
            DetailFragment.lsvRequestState =
                    RequestFragment.lsvProductNameRequestList.onSaveInstanceState();
            DetailFragment.lsvDoneState =
                    DoneFragment.lsvProductNameDoneList.onSaveInstanceState();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return layout;
    }

    /* Sự kiện  khi nút quay trở lại được nhấn */
    private View.OnClickListener btnBackPreviousClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (DetailFragment.detailType == libraries.DETAIL_REQUEST) {
                            MainActivity.bottomNavigation.setSelectedItemId(R.id.nav_request);
                        } else if (DetailFragment.detailType == libraries.DETAIL_DONE) {
                            MainActivity.bottomNavigation.setSelectedItemId(R.id.nav_done);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi nút chụp ảnh được nhấn */
    private View.OnClickListener btnCaptureProductClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /* Nếu hệ điều hành >= marshmallow, yêu cầu cấp quyền truy cập */
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(getActivity(), Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_DENIED ||
                                    checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                            PackageManager.PERMISSION_DENIED ||
                                    checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                            PackageManager.PERMISSION_DENIED) {

                                /* Nếu không có quyền truy cập thì yêu cầu */
                                String[] permission = {Manifest.permission.CAMERA,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE};

                                /* Hiển thị popup yêu cầu cấp quyền */
                                requestPermissions(permission, libraries.PERMISSON_CODE);
                            } else {
                                /* Nếu được cấp quyền thì tạo thư mục làm việc */
                                libraries.createFolderStorage(MainActivity.folderWorkingPath,
                                        detailProductName);

                                /* Đã được cấp quyền */
                                openCamera();
                            }
                        } else {
                            /* Nếu được cấp quyền thì tạo thư mục làm việc */
                            libraries.createFolderStorage(MainActivity.folderWorkingPath,
                                    detailProductName);

                            /* Hệ điều hành < marshmallow */
                            openCamera();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi nút đánh dấu là hoàn thành được nhấn */
    private View.OnClickListener btnMarkAsDoneClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /* Hiển thị hộp thoại hỏi */
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.dialog_message_mark_as_done))
                                .setPositiveButton(getString(R.string.dialog_positive_yes),
                                        dialogMarAsDoneClickListener)
                                .setNegativeButton(getString(R.string.dialog_negative_no),
                                        dialogMarAsDoneClickListener)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi hộp thoại hỏi đánh dấu đã hoàn thành */
    private DialogInterface.OnClickListener dialogMarAsDoneClickListener = new
            DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            try {
                                /* Thêm vào danh sách done */
                                DoneFragment.doneProductNameList.add(
                                        RequestFragment.requestProductNameList.get(detailProductIndex));
                                DoneFragment.doneProductCodeList.add(
                                        RequestFragment.requestProductCodeList.get(detailProductIndex));
                                DoneFragment.doneProductLocationList.add(
                                        RequestFragment.requestProductLocationList.get(detailProductIndex));

                                /* Xoá khỏi danh sách request */
                                ArrayList<String> removeList = new ArrayList<>();
                                for (String item : RequestFragment.requestProductNameList) {
                                    if (item.equals(detailProductName)) {
                                        removeList.add(item);
                                    }
                                }
                                for (String item : removeList) {
                                    RequestFragment.requestProductCodeList.remove(
                                            RequestFragment.requestProductNameList.indexOf(item));
                                    RequestFragment.requestProductLocationList.remove(
                                            RequestFragment.requestProductNameList.indexOf(item));
                                    RequestFragment.requestProductNameList.remove(item);
                                }

                                /* Public lên MQTT */
                                MainActivity.publicDataToServer();

                                /* Chuyển về Fragment Request */
                                MainActivity.bottomNavigation.setSelectedItemId(R.id.nav_request);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    /* Sự kiện khi nút yêu cầu chụp lại được nhấn */
    private View.OnClickListener btnRequestAgainClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /* Hiển thị hộp thoại hỏi */
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.dialog_message_request_again))
                                .setPositiveButton(getString(R.string.dialog_positive_yes),
                                        dialogRequestAgainClickListener)
                                .setNegativeButton(getString(R.string.dialog_negative_no),
                                        dialogRequestAgainClickListener)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi hộp thoại hỏi yêu cầu chụp lại */
    private DialogInterface.OnClickListener dialogRequestAgainClickListener = new
            DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            try {
                                /* Thêm vào danh sách request */
                                RequestFragment.requestProductNameList.add(
                                        DoneFragment.doneProductNameList.get(detailProductIndex));
                                RequestFragment.requestProductCodeList.add(
                                        DoneFragment.doneProductCodeList.get(detailProductIndex));
                                RequestFragment.requestProductLocationList.add(
                                        DoneFragment.doneProductLocationList.get(detailProductIndex));

                                /* Xoá khỏi danh sách done */
                                ArrayList<String> removeList = new ArrayList<>();
                                for (String item : DoneFragment.doneProductNameList) {
                                    if (item.equals(detailProductName)) {
                                        removeList.add(item);
                                    }
                                }
                                for (String item : removeList) {
                                    DoneFragment.doneProductCodeList.remove(
                                            DoneFragment.doneProductNameList.indexOf(item));
                                    DoneFragment.doneProductLocationList.remove(
                                            DoneFragment.doneProductNameList.indexOf(item));
                                    DoneFragment.doneProductNameList.remove(item);
                                }

                                /* Xoá khỏi danh sách viewed */
                                for (String item : RequestFragment.requestProductNameViewedList) {
                                    if (item.equals(detailProductName)) {
                                        RequestFragment.requestProductNameViewedList.remove(item);
                                    }
                                }

                                /* Ghi dữ liệu người dùng */
                                MainActivity.writeUserPreferences(getActivity());

                                /* Public lên MQTT */
                                MainActivity.publicDataToServer();

                                /* Chuyển về Fragment Done */
                                MainActivity.bottomNavigation.setSelectedItemId(R.id.nav_done);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    /* Sự kiện khi nút xoá sản phẩm yêu cầu được nhấn */
    private View.OnClickListener btnClearProductRequestClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /* Hiển thị hộp thoại hỏi */
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.dialog_message_clear_product_request))
                                .setPositiveButton(getString(R.string.dialog_positive_yes),
                                        dialogClearProdutRequestClickListener)
                                .setNegativeButton(getString(R.string.dialog_negative_no),
                                        dialogClearProdutRequestClickListener)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi hộp thoại hỏi xoá sản phẩm yêu cầu */
    private DialogInterface.OnClickListener dialogClearProdutRequestClickListener = new
            DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            try {
                                /* Xoá khỏi danh sách request */
                                ArrayList<String> removeList = new ArrayList<>();
                                for (String item : RequestFragment.requestProductNameList) {
                                    if (item.equals(detailProductName)) {
                                        removeList.add(item);
                                    }
                                }
                                for (String item : removeList) {
                                    RequestFragment.requestProductCodeList.remove(
                                            RequestFragment.requestProductNameList.indexOf(item));
                                    RequestFragment.requestProductLocationList.remove(
                                            RequestFragment.requestProductNameList.indexOf(item));
                                    RequestFragment.requestProductNameList.remove(item);
                                }

                                /* Public lên MQTT */
                                MainActivity.publicDataToServer();

                                /* Chuyển về Fragment Request */
                                MainActivity.bottomNavigation.setSelectedItemId(R.id.nav_request);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    /* Sự kiện khi nút xoá sản phẩm hoàn thành được nhấn */
    private View.OnClickListener btnClearProductDoneClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /* Hiển thị hộp thoại hỏi */
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.dialog_message_clear_product_done))
                                .setPositiveButton(getString(R.string.dialog_positive_yes),
                                        dialogClearProdutDoneClickListener)
                                .setNegativeButton(getString(R.string.dialog_negative_no),
                                        dialogClearProdutDoneClickListener)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi hộp thoại hỏi xóa sản phẩm hoàn thành */
    private DialogInterface.OnClickListener dialogClearProdutDoneClickListener = new
            DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            try {
                                /* Xoá khỏi danh sách done */
                                ArrayList<String> removeList = new ArrayList<>();
                                for (String item : DoneFragment.doneProductNameList) {
                                    if (item.equals(detailProductName)) {
                                        removeList.add(item);
                                    }
                                }
                                for (String item : removeList) {
                                    DoneFragment.doneProductCodeList.remove(
                                            DoneFragment.doneProductNameList.indexOf(item));
                                    DoneFragment.doneProductLocationList.remove(
                                            DoneFragment.doneProductNameList.indexOf(item));
                                    DoneFragment.doneProductNameList.remove(item);
                                }

                                /* Public lên MQTT */
                                MainActivity.publicDataToServer();

                                /* Chuyển về Fragment Done */
                                MainActivity.bottomNavigation.setSelectedItemId(R.id.nav_done);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    /* Sự kiện khi nút khi nút tải lên Drive được nhấn */
    private View.OnClickListener btnUploadToDriveClickListener = new
            View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        /* Hiển thị hộp thoại hỏi */
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.dialog_message_upload_to_drive_1) + " "
                                + String.valueOf(fileProductNameList.size()) + " " +
                                getString(R.string.dialog_message_upload_to_drive_2))
                                .setPositiveButton(getString(R.string.dialog_positive_yes),
                                        dialogUploadToDriveClickListener)
                                .setNegativeButton(getString(R.string.dialog_negative_no),
                                        dialogUploadToDriveClickListener)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi hộp thoại hỏi tải lên drive */
    private DialogInterface.OnClickListener dialogUploadToDriveClickListener = new
            DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            try {
                                /* Tải ảnh lên Google Drive */
                                uploadMultipleFileToDrive();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    /* Sự kiện khi nhập chọn vào item trong danh sách ảnh */
    private AdapterView.OnItemClickListener lsvProductPictureItemClickListener = new
            AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        dlPictureViewer = new Dialog(getActivity());
                        new showDialogPictureViewerTask(dlPictureViewer).execute(
                                fileProductPictureList.get(position));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

    /* Sự kiện khi nhấn giữ vào item trong danh sách ảnh */
    private AdapterView.OnItemLongClickListener lsvProductPictureItemLongClickListener = new
            AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                               long id) {
                    try {
                        /* Đặt vịt trí xoá */
                        deletePos = position;

                        /* Hiển thị hộp thoại hỏi */
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(getString(R.string.dialog_message_delete_picture))
                                .setPositiveButton(getString(R.string.dialog_positive_yes),
                                        dialogDeletePictureClickListener)
                                .setNegativeButton(getString(R.string.dialog_negative_no),
                                        dialogDeletePictureClickListener)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return true;
                }
            };

    /* Sự kiện khi hộp thoại xoá item khỏi danh sách ảnh */
    private DialogInterface.OnClickListener dialogDeletePictureClickListener = new
            DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            try {
                                /* Thực thi task xoá ảnh */
                                new deletePictureTask().execute(
                                        fileProductPictureList.get(deletePos));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            break;
                    }
                }
            };

    /* Sự kiện gọi về khi popup yêu cầu quyền truy cập */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == libraries.PERMISSON_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    /* Nếu được cấp quyền thì tạo thư mục làm việc */
                    libraries.createFolderStorage(MainActivity.folderWorkingPath,
                            detailProductName);

                    /* Mở camera */
                    openCamera();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                /* Bị từ chối quyền truy cập */
            }
        }
    }

    /* Sự kiện gọi về khi Activity trả về kết quả */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == libraries.CAMERA_CODE) {
            if (resultCode == RESULT_OK) {
                saveImageToFolderStorageParam param = new saveImageToFolderStorageParam(
                        imageUri,
                        detailProductName,
                        detailProductName);
                new saveImageToFolderStorageTask().execute(param);
            }
        }
    }

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Hàm tải danh sách tệp lên Google Drive */
    private void uploadMultipleFileToDrive() {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle(getString(R.string.progess_dialog_upload_to_drive_title));
        progressDialog.setMessage(getString(R.string.progess_dialog_upload_to_drive_message));
        progressDialog.show();

        MainActivity.driveService.uploadMultipleFileToDriveTask(
                detailProductName,
                fileProductNameList,
                fileProductPictureList,
                "image/jpeg")
                .addOnSuccessListener(new OnSuccessListener<ArrayList<String>>() {
                    @Override
                    public void onSuccess(ArrayList<String> strings) {
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(),
                                getString(R.string.toast_upload_to_drive_success),
                                Toast.LENGTH_SHORT).show();

                        /* Kiểm tra đã tải lên drive chưa và tick */
                        new loadListViewProductPictureListTask().execute();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(getActivity(),
                        getString(R.string.toast_upload_to_drive_failure),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* Hàm mở Camera chụp ảnh */
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, detailProductName);
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.app_name));

        imageUri = getActivity().getApplicationContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, libraries.CAMERA_CODE);
    }

    /*============================================================================================*/
    /* Hàm bất đồng bộ */
    /*============================================================================================*/

    /* Tham số Task lưu ảnh vào thư mục trên máy */
    private static class saveImageToFolderStorageParam {
        Uri imgUri;
        String fileName;
        String folderName;

        /* Khởi tạo class */
        saveImageToFolderStorageParam(Uri imgUri, String fileName, String folderName) {
            this.imgUri = imgUri;
            this.fileName = fileName;
            this.folderName = folderName;
        }
    }

    /* Task lưu ảnh vào thư mục trên máy */
    private class saveImageToFolderStorageTask extends AsyncTask<saveImageToFolderStorageParam,
            Void, Void> {
        @Override
        /* Thực thi tiến trình nền */
        protected Void doInBackground(saveImageToFolderStorageParam... params) {
            try {
                /* Tạo tệp mới từ đường dẫn */
                File file = new File(MainActivity.folderWorkingPath + "/" +
                        libraries.convertNameToFolderName(params[0].folderName) + "/",
                        libraries.convertNameToFileName(params[0].fileName, "jpg"));

                /* Nếu tệp tồn tại thì xoá */
                if (file.exists()) {
                    file.delete();
                }

                /* Ghi tệp từ đường dẫn uri */
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getActivity().getApplicationContext().getContentResolver(),
                        params[0].imgUri);
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /* Thực thi tiến trình nền thành công */
        @Override
        protected void onPostExecute(Void aVoid) {
            /* Thực thi Task tải danh sách ảnh */
            new loadListViewProductPictureListTask().execute();
        }
    }

    /* Task tải danh sách tệp vào danh sách xem chi tiết */
    private class loadListViewProductPictureListTask extends AsyncTask<Void, Void,
            ArrayList<String>> {
        /* Thực thi tiến trình nền */
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String>fileDriveNameList = new ArrayList<>();
            try {
                /* Tải danh sách tệp từ Google Drive */
                fileDriveNameList = MainActivity.driveService.getAllFileDriveName();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Trả về danh sách */
            return fileDriveNameList;
        }

        /* Thực thi xong tiến trình nền */
        @Override
        protected void onPostExecute(ArrayList<String> fileDriveNameList) {
            /* Nếu tiến trình bị huỷ thì trả về giá trị khởi tạo */
            if (isCancelled()) {
                fileDriveNameList = new ArrayList<>();
            }

            try {
                /* Tạo mới thông số của danh sách tệp */
                fileProductNameList = new ArrayList<>();
                fileProductDateList = new ArrayList<>();
                fileProductSizeList = new ArrayList<>();
                fileProductPictureList = new ArrayList<>();

                /* Kiểm tra thư mục và thêm danh sách ảnh */
                File directory = new File(MainActivity.folderWorkingPath + "/" +
                        libraries.convertNameToFolderName(detailProductName));
                File[] files = directory.listFiles();
                for (int i = 0; i < files.length; i++) {
                    fileProductNameList.add(files[i].getName());
                    String fileDate = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                            DateFormat.SHORT).format(new Date(files[i].lastModified()));
                    fileProductDateList.add(fileDate);
                    fileProductSizeList.add((files[i].length() / 1024) + "KB");
                    fileProductPictureList.add(files[i].getAbsolutePath());
                }

                /* Tạo adapter đổ dữ liệu */
                SingleAdapter detailAdapter;
                detailAdapter = new SingleAdapter(getActivity(), fileProductNameList,
                        fileProductDateList, fileProductSizeList, fileProductPictureList,
                        fileDriveNameList);

                /* Đổ dữ liệu vào danh sách xem chi tiết */
                lsvProductPictureList.setAdapter(detailAdapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Task xóa ảnh khỏi thư mục trên máy */
    private class deletePictureTask extends AsyncTask<String, Void, Void> {
        /* Thực thi tiến trìn nền */
        @Override
        protected Void doInBackground(String... params) {
            try {
                File file = new File(params[0]);
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /* Thực thi xong tiến trình nền */
        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                /* Tải lại danh sách ảnh */
                new loadListViewProductPictureListTask().execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Task hiển thị trình xem ảnh  */
    private class showDialogPictureViewerTask extends AsyncTask<String, Void, Bitmap> {
        /* Thành phần đồng bộ */
        private final WeakReference<Dialog> dialogWeakReference;

        /* Thành phần đồng bộ */
        public showDialogPictureViewerTask(Dialog dialog) {
            dialogWeakReference = new WeakReference<Dialog>(dialog);
        }

        /* Tiến trình nền đang thực thi */
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bmp = null;
            try {
                /* Giải mã tệp sang định dạng hình ảnh */
                bmp =  BitmapFactory.decodeFile(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
                bmp = null;
            }
            return bmp;
        }

        /* Tiến trình nền thực thi xong */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            /* Nếu tiến trình bị huỷ thì trả về giá trị khởi tạo */
            if (isCancelled()) {
                bitmap = null;
            }

            try {
                if (dialogWeakReference != null) {
                    Dialog dialog = dialogWeakReference.get();
                    if (dialog != null) {
                        dialog.setContentView(R.layout.picture_viewer);

                        /* Ánh xạ các thành phần */
                        ImageButton btnCloseViewer = (ImageButton) dialog.findViewById(
                                R.id.btnCloseViewer);
                        ImageView imgPictureViewer = (ImageView) dialog.findViewById(
                                R.id.imgPictureViewer);

                        /* Sự kiện khi nhấn nút đóng */
                        btnCloseViewer.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        /* Nếu bitmap khác null thì đặt ảnh */
                        if (bitmap != null) {
                            imgPictureViewer.setImageBitmap(bitmap);
                        }

                        /* Hiển thị trình xem ảnh */
                        dialog.getWindow()
                                .setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        dialog.show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
