package com.xuecheng.manage_media_process.mq;


import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    @Autowired
    MediaFileRepository mediaFileRepository;

    //ffmpeg绝对路径
    @Value("${xc‐service‐manage‐media.ffmpeg‐path}")
    String ffmpeg_path;
    //上传文件根目录
    @Value("${xc‐service‐manage‐media.upload‐location}")
    String serverPath;

    @RabbitListener(queues = "${queue-media-video-processor}",concurrency = "customContainerFactory")
    public void receiveMediaProcessTask(String msg) throws IOException {
        if (StringUtils.isEmpty(msg)) {
            return;
        }
        Map msgMap = JSON.parseObject(msg, Map.class);
        //获取媒资文件得id
        String mediaId = (String) msgMap.get("mediaId");

        Optional<MediaFile> mediaFileOptional = mediaFileRepository.findById(mediaId);

        if (!mediaFileOptional.isPresent()) {
            return;
        }
        //文件存在
        MediaFile mediaFile = mediaFileOptional.get();

        //如果不是avi 则不进行转换,直接设置状态保存即可
        String fileType = mediaFile.getFileType();
        if (fileType == null || fileType.equals("avi")) {
            //文件无需处理得状态
            mediaFile.setProcessStatus("303004");
            mediaFileRepository.save(mediaFile);
            return;
        } else {
            //设置处理中
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }
        //avi转mp4
        String video_path = serverPath + mediaFile.getFilePath() + mediaFile.getFileName();
        String mp4_name = mediaFile.getFileId() + ".mp4";
        String mp4folder_path = serverPath + mediaFile.getFilePath();
        Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil(ffmpeg_path, video_path, mp4_name, mp4folder_path);
        String mp4Result = mp4VideoUtil.generateMp4();
        if (mp4Result == null || mp4Result.equals("success")) {
            //转换过程出现异常
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(mp4Result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }
        //mp4转m3u8
        //生成m3u8
        video_path = serverPath + mediaFile.getFilePath() + mp4_name;//此地址为mp4的地址
        String m3u8_name = mediaFile.getFileId() + ".m3u8";
        String m3u8folder_path = serverPath + mediaFile.getFilePath() + "hls/";
        HlsVideoUtil hlsVideoUtil = new HlsVideoUtil(ffmpeg_path, video_path, m3u8_name, m3u8folder_path);
        String m3u8Result = hlsVideoUtil.generateM3u8();
        if (m3u8Result == null || m3u8Result.equals("success")) {
            //转换过程出现异常
            mediaFile.setProcessStatus("303003");
            MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(m3u8Result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
            mediaFileRepository.save(mediaFile);
            return;
        }

        //处理成功之后得代码
        //获取m3u8列表
        List<String> ts_list = hlsVideoUtil.get_ts_list();
        //更新处理状态为成功
        mediaFile.setProcessStatus("303002");//处理状态为处理成功
        MediaFileProcess_m3u8 mediaFileProcess_m3u8 = new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        //m3u8文件url
        mediaFile.setFileUrl(mediaFile.getFilePath() + "hls/" + m3u8_name);
        mediaFileRepository.save(mediaFile);
    }
}
