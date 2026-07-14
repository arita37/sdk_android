package com.vmodal.sdk.examples

import com.vmodal.sdk.Client
import com.vmodal.sdk.GoogleDriveFolderUploadResponse

fun importPublicDriveFolder(sdk: Client): GoogleDriveFolderUploadResponse =
    sdk.collections.uploadGoogleDriveFolder(
        googleDriveUrl = "https://drive.google.com/drive/folders/REPLACE_ME",
        groupName = "drive-import",
        mode = "vid_file",
        streamName = "astream",
    )
