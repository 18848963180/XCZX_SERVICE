package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {

    private final static Logger LOGGER = LoggerFactory.getLogger(MediaUploadService.class);

    @Autowired
    MediaFileRepository mediaFileRepository;
    //上传文件根目录
    @Value("${xc‐service‐manage‐media.upload‐location}")
    String uploadPath;

    @Value("${xc‐service‐manage‐media.mq.routingkey-media-video}")
    String routingkey_media_video;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 校验文件是否存在
     *
     * @param fileMd5
     * @param fileName
     * @param fileSize
     * @param mimetype
     * @param fileExt
     * @return
     */
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {

        String filePath = getFilePath(fileMd5, fileExt);
        //物理文件
        File file = new File(filePath);

        //数据库是否有记录
        Optional<MediaFile> fileOptional = mediaFileRepository.findById(fileMd5);

        if (file.exists() && fileOptional.isPresent()) {
            //说明文件存在
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }

        //以上都不存在 则说明文件不存在 可以创建目录
        boolean fileFold = this.createFileFold(fileMd5);
        if (fileFold) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_FAIL);
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 校验文件块是否存在
     *
     * @param fileMd5
     * @param chunk
     * @param chunkSize
     * @return
     */
    public CheckChunkResult checkchunk(String fileMd5, Integer chunk, Integer chunkSize) {
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File chunkFile = new File(chunkFileFolderPath + chunk);
        return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK, chunkFile.exists());
    }


    public ResponseResult uploadchunk(MultipartFile file, Integer chunk, String fileMd5) {
        if (file == null) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_ISNULL);
        }
        //创建块文件目录
        boolean fileFold = this.createChunkFileFolder(fileMd5);

        File chunkFile = new File(this.getChunkFileFolderPath(fileMd5) + chunk);

        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            inputStream = file.getInputStream();
            fileOutputStream = new FileOutputStream(chunkFile);
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("upload chunk file fail:{}", e.getMessage());
            ExceptionCast.cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }


    public ResponseResult mergechunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File chunkFilePath = new File(chunkFileFolderPath);
        if (!chunkFilePath.exists()) {
            chunkFilePath.mkdirs();
        }
        //合并的文件
        File mergeFile = new File(this.getFilePath(fileMd5, fileExt));
        if (mergeFile.exists()) {
            //如果以及存在则先删除
            mergeFile.delete();
        }
        boolean newFile = false;
        try {
            newFile = mergeFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("mergechunks..create mergeFile fail:{}", e.getMessage());
        }
        if (!newFile) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CREATEFAIL);
        }
        //获取块文件，此列表是已经排好序的列表
        List<File> chunkFiles = this.getChunkFiles(chunkFileFolderPath, chunkFilePath);
        //合并文件
        mergeFile = this.mergeFile(mergeFile, chunkFiles);

        //校验md5
        if (mergeFile == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //校验文件
        boolean checkResult = this.checkFileMd5(mergeFile, fileMd5);
        if (!checkResult) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }

        //将文件信息保存到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5 + "." + fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(getFileFolderRelativePath(fileMd5));
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        mediaFileRepository.save(mediaFile);
        sendProcessVideoMsg(mediaFile.getFileId());
        // mq发送消息 创建m3u8视频文件
        return new ResponseResult(CommonCode.SUCCESS);

    }

    private ResponseResult sendProcessVideoMsg(String fileId) {
        Optional<MediaFile> fileOptional = mediaFileRepository.findById(fileId);
        if (!fileOptional.isPresent()) {
            return new ResponseResult(CommonCode.FAIL);
        }
//        MediaFile mediaFile = fileOptional.get();
        String mediaIdJsonString = JSON.toJSONString(Collections.singletonMap("mediaId", fileId));
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK, routingkey_media_video, mediaIdJsonString);
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);

    }

    private boolean checkFileMd5(File mergeFile, String fileMd5) {
        if (mergeFile == null || StringUtils.isEmpty(fileMd5)) {
            return false;
        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(mergeFile);
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            return md5Hex.equalsIgnoreCase(md5Hex);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private File mergeFile(File mergeFile, List<File> chunkFiles) {

        try {
            RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
            byte[] b = new byte[1024];
            for (File chunkFile : chunkFiles) {
                RandomAccessFile r = new RandomAccessFile(chunkFile, "r");
                int len = -1;
                while ((len = r.read(b)) != -1) {
                    rw.write(b, 0, len);
                }
                r.close();
            }
            rw.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("merge file error:{}", e.getMessage());
            return null;
        }


        return mergeFile;
    }

    private List<File> getChunkFiles(String chunkFileFolderPath, File chunkFilePath) {
        File[] files = chunkFilePath.listFiles();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) > Integer.parseInt(o2.getName()) ? 1 : -1;
            }
        });
        return fileList;
    }


    /**
     * 根据文件md5得到文件路径
     * 规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     *
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     * @return 文件路径
     */
    private String getFilePath(String fileMd5, String fileExt) {
        return new StringBuffer()
                .append(uploadPath).append(this.getFileFolderRelativePath(fileMd5))
                .append(fileMd5).append(".").append(fileExt)
                .toString();
    }

    //得到文件目录相对路径，路径中去掉根目录
    private String getFileFolderRelativePath(String fileMd5) {
        return new StringBuffer().append(fileMd5, 0, 1)
                .append("/").append(fileMd5, 1, 2)
                .append("/").append(fileMd5).append("/").toString();
    }

    //得到文件所在目录
    private String getFileFolderPath(String fileMd5) {
        return uploadPath + this.getFileFolderRelativePath(fileMd5);
    }

    //得到块文件所在目录
    private String getChunkFileFolderPath(String fileMd5) {
        return getFileFolderPath(fileMd5) + "/" + "chunks" + "/";
    }

    private boolean createFileFold(String fileMd5) {
        //创建文件目录
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        File file = new File(fileFolderPath);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

    private boolean createChunkFileFolder(String fileMd5) {
        //创建文件目录
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        File file = new File(chunkFileFolderPath);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }
}
