package com.phatvu1294.blkcapture;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveService {
    /*============================================================================================*/
    /* Các thành phần toàn cục */
    /*============================================================================================*/

    /* Biến thực thi Task vụ Google Drive */
    private final Executor executor = Executors.newSingleThreadExecutor();

    /* Biến Google Drive */
    private final Drive drive;

    /*============================================================================================*/
    /* Hàm */
    /*============================================================================================*/

    /* Khởi tạo class */
    public DriveService(Drive drive) {
        this.drive = drive;
    }

    /* Hàm tải tệp lên Google Drive */
    public String uploadFileToDrive(String folderId, String fileName,
                                    String filePath, String contentType) {
        String fileId = "";
        try {
            /* Tạo tệp meta Google Drive */
            File fileMetaData = new File();
            fileMetaData.setName(fileName);
            fileMetaData.setParents(Collections.singletonList(folderId));

            /* Nạp tệp IO vào tệp Google Drive */
            java.io.File file = new java.io.File(filePath);
            FileContent mediaContent = new FileContent(contentType, file);

            /* Tạo tệp Google Drive */
            File fileDrive = drive.files().create(fileMetaData, mediaContent)
                    .setFields("id, parent").execute();

            /* Lấy id của tệp */
            fileId = fileDrive.getId();
        } catch (IOException e) {
            e.printStackTrace();
            fileId = "";
        }

        /* Trả về id của tệp */
        return fileId;
    }

    /* Hàm tạo thư mục Google Drive */
    public String createFolderDrive(String folderName) {
        String folderId = "";
        try {
            /* Tạo tệp meta Google Drive */
            File fileMetadata = new File();
            fileMetadata.setName(folderName);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            /* Tạo tệp Google Drive */
            File fileDrive = drive.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            /* Lấy id của tệp */
            folderId = fileDrive.getId();
        } catch (IOException e) {
            e.printStackTrace();
            folderId = "";
        }

        /* Trả về id của tệp */
        return folderId;
    }

    /* Hàm lấy toàn bộ folder trên Google Drive */
    public ArrayList<String> getAllFolderDriveName() {
        ArrayList<String> folderNameList = new ArrayList<>();
        String pageToken = null;
        do {
            try {
                /* Lọc danh sách tệp */
                FileList result = drive.files().list()
                        .setQ("mimeType='application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();

                for (File folderName : result.getFiles()) {
                    folderNameList.add(folderName.getName());
                }

                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                e.printStackTrace();
                folderNameList = new ArrayList<>();
            }
        } while (pageToken != null);

        return folderNameList;
    }

    /* Hàm lấy toàn bộ danh sách tệp Google Drive */
    public ArrayList<String> getAllFileDriveName() {
        ArrayList<String> fileNameList = new ArrayList<>();
        String pageToken = null;
        do {
            try {
                /* Lọc danh sách tệp */
                FileList result = drive.files().list()
                        .setQ("mimeType='image/jpeg'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();

                for (File fileDrive : result.getFiles()) {
                    fileNameList.add(fileDrive.getName());
                }

                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                e.printStackTrace();
                fileNameList = new ArrayList<>();
            }
        } while (pageToken != null);

        return fileNameList;
    }

    /* Hàm kiểm tra sự tồn tại của tệp Google Drive */
    public String checkFileExists(String fileName) {
        String fileId = "";
        String pageToken = null;
        do {
            try {
                /* Lọc danh sách tệp */
                FileList result = drive.files().list()
                        .setQ("mimeType='image/jpeg'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();

                /* Tìm kiếm tệp trong danh sách vừa lọc */
                for (File fileDrive : result.getFiles()) {
                    /* Nếu tên có trong danh sách */
                    if (fileDrive.getName().equals(fileName)) {
                        /* Lấy id của tệp */
                        fileId = fileDrive.getId();
                    }
                }

                /* Chuuyển sáng page mới */
                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                e.printStackTrace();
                fileId = "";
            }
        } while (pageToken != null);

        /* Trả về id của tệp hoặc thư mục */
        return fileId;
    }

    /* Hàm kiểm tra sự tồn tại của thư mục Google Drive */
    public String checkFolderExists(String folderName) {
        String folderId = "";
        String pageToken = null;
        do {
            try {
                /* Lọc danh sách tệp */
                FileList result = drive.files().list()
                        .setQ("mimeType='application/vnd.google-apps.folder'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();

                /* Tìm kiếm tệp trong danh sách vừa lọc */
                for (File folderDrive : result.getFiles()) {
                    /* Nếu tên có trong danh sách */
                    if (folderDrive.getName().equals(folderName)) {
                        /* Lấy id của tệp */
                        folderId = folderDrive.getId();
                    }
                }

                /* Chuuyển sáng page mới */
                pageToken = result.getNextPageToken();
            } catch (IOException e) {
                e.printStackTrace();
                folderId = "";
            }
        } while (pageToken != null);

        /* Trả về id của tệp hoặc thư mục */
        return folderId;
    }

    /* Hàm đặt trạng thái chia sẻ cho tệp hoặc thư mục Google Drive */
    public String shareableFileOrFolderDrive(String fileFolderId) {
        String _fileFolderId = "";
        try {
            /* Khởi tạo quyền truy cập */
            Permission permission = new Permission();
            permission.setType("anyone");
            permission.setRole("reader");

            /* Tạo mới quyền truy cập */
            Permission permissionDrive = drive.permissions()
                    .create(fileFolderId, permission).setFields("id").execute();

            /* Lấy id của tệp hoặc thư mục */
            _fileFolderId = permissionDrive.getId();
        } catch (IOException e) {
            e.printStackTrace();
            _fileFolderId = "";
        }

        /* Trả về id của tệp hoặc thư mục */
        return _fileFolderId;
    }

    /*============================================================================================*/
    /* Hàm bất đồng bộ */
    /*============================================================================================*/

    /* Task tải nhiều tệp lên Google Drive */
    public Task<ArrayList<String>> uploadMultipleFileToDriveTask(String folderName,
                                                                 ArrayList<String> fileNameList,
                                                                 ArrayList<String> filePathList,
                                                                 String contentType) {
        /* Task Google Drive thực thi */
        return Tasks.call(executor, () -> {
            String folderId = "";
            String fileId = "";
            ArrayList<String> fileIdList = new ArrayList<>();

            /* Kiểm tra thư mục */
            folderId = checkFolderExists(folderName);

            /* Nếu thư mục không tồn tại */
            if (folderId.isEmpty()) {
                /* Tạo mới thư mục */
                folderId = createFolderDrive(folderName);
            }

            /* Tải từng tệp lên Google Drive */
            for (int i = 0; i < fileNameList.size(); i++) {
                /* Kiểm tra tệp */
                fileId = checkFileExists(fileNameList.get(i));

                /* Nếu tệp không tồn tại */
                if (fileId.isEmpty()) {
                    /* Tải tệp lên Google Drive */
                    fileId = uploadFileToDrive(folderId, fileNameList.get(i),
                            filePathList.get(i), contentType);
                }

                /* Thêm id tệp vào danh sách */
                fileIdList.add(fileId);
            }

            /* Chia sẻ thư mục */
            shareableFileOrFolderDrive(folderId);

            /* Trả về danh sách id của tệp */
            return fileIdList;
        });
    }
}
