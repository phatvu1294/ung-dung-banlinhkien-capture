package com.phatvu1294.blkcapture;

import java.io.File;
import java.util.Random;

public class Libraries {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Biến các mã code */
    public final int PERMISSON_CODE = 1000;
    public final int CAMERA_CODE = 1001;
    public final int GDRIVE_CODE = 1002;
    public final int NOTIFY_CODE = 1003;

    /* Biến kiểu detail */
    public final int DETAIL_NONE = 0;
    public final int DETAIL_REQUEST = 1;
    public final int DETAIL_DONE = 2;

    /* Biến chuỗi ngẫu nhiên */
    private final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";

    /* Danh sách các ký tự không hợp lệ */
    private final char illegal[] = {'\"', '*', '<', '>', '?', '\\', '|', '/', ':', '%'};

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Hàm chuyển đổi tên sang tên tệp (dùng cho tệp hình ảnh) */
    public String convertNameToFileName(String name, String extension) {
        String nameStr = name;
        String fileName = "";
        if (!nameStr.isEmpty()) {
            for (char c : illegal) {
                nameStr = nameStr.replace(c, '_');
            }

            fileName = nameStr + "_" + randomString(4) + "." + extension;
        } else {
            fileName = randomString(32) + "." + extension;
        }
        return fileName;
    }

    /* Hàm chuyển đổi tến sang tên thư mục (dùng để lưu trữ tệp) */
    public String convertNameToFolderName(String name) {
        String nameStr = name;
        String fileName = "";
        if (!nameStr.isEmpty()) {
            for (char c : illegal) {
                nameStr = nameStr.replace(c, '_');
            }
        }
        fileName = nameStr;
        return fileName;
    }

    /* Hàm tạo chuỗi ngẫu nhiên */
    public String randomString(int sizeOfString) {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfString);
        for (int i = 0; i < sizeOfString; ++i) {
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }
        return sb.toString();
    }

    /* Hàm tạo thư mục làm việc trên máy */
    public void createFolderStorage(String folderWorking, String folderName) {
        File direct = new File(folderWorking + "/" +
                convertNameToFolderName(folderName));
        if (!direct.exists()) {
            File wallpaperDirectory = new File(folderWorking + "/" +
                    convertNameToFolderName(folderName) + "/");
            wallpaperDirectory.mkdirs();
        }
    }
}
