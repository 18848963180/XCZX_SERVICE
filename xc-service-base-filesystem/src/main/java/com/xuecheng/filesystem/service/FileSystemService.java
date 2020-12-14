package com.xuecheng.filesystem.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.util.Map;

@Service
@Transactional
public class FileSystemService {

    @Autowired
    FileSystemRepository fileSystemRepository;

    @Value("xuecheng.fastdfs.tracker_servers")
    String tracker_servers;
    @Value("xuecheng.fastdfs.charset")
    String charset;
    @Value("xuecheng.fastdfs.connect_timeout_in_seconds")
    int connect_timeout_in_seconds;
    @Value("xuecheng.fastdfs.network_timeout_in_seconds")
    int network_timeout_in_seconds;

    public UploadFileResult upload(MultipartFile multipartFile, String filetag, String businesskey, String metadata) {
        //验证文件是否存在
        if (multipartFile == null) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
        }
        //上传文件返回文件id
        String fileId = this.uploadFdfs(multipartFile);
        //将文件信息存储到mongodb
        if (StringUtils.isEmpty(fileId)) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
        }
        FileSystem fs = new FileSystem();
        fs.setBusinesskey(businesskey);
        fs.setFileId(fileId);
        fs.setFilePath(fileId);
        fs.setFileType(multipartFile.getContentType());
        fs.setFileName(multipartFile.getOriginalFilename());
        fs.setFiletag(filetag);
        fs.setFileSize(multipartFile.getSize());
        if (StringUtils.isNotEmpty(metadata)) {
            fs.setMetadata(JSON.parseObject(metadata, Map.class));
        }
        fileSystemRepository.save(fs);
        return new UploadFileResult(CommonCode.SUCCESS, fs);
    }

    private String uploadFdfs(MultipartFile multipartFile) {
        try {
            //初始化文件服务器
            this.initFdfsConfig();
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer connection = trackerClient.getConnection();
            StorageServer storeStorage = trackerClient.getStoreStorage(connection);
            StorageClient1 storageClient1 = new StorageClient1(connection, storeStorage);
            String originalFilename = multipartFile.getOriginalFilename();
            String fen = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            String fileId = storageClient1.upload_appender_file1(multipartFile.getBytes(), fen, null);
            return fileId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initFdfsConfig() {

        try {
            ClientGlobal.initByTrackers(tracker_servers);
            ClientGlobal.setG_connect_timeout(connect_timeout_in_seconds);
            ClientGlobal.setG_network_timeout(network_timeout_in_seconds);
            ClientGlobal.setG_charset(charset);
        } catch (Exception e) {
            e.printStackTrace();
            ExceptionCast.cast(FileSystemCode.FS_INITFDFSERROR);
        }

    }
}
