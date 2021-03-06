package com.roily.root.demo.aboutfile.controller;

import com.roily.root.demo.aboutfile.service.FileInterface;
import com.roily.root.demo.common.common.VO.aboutfile.req.FileReqVo;
import com.roily.root.demo.common.common.VO.aboutfile.resp.FileRespVo;
import com.roily.root.demo.common.util.ResultVo;
import io.swagger.annotations.ApiModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @version 1.0.0
 * @Description 文件上传
 * @ClassName FileUpload.java
 * @author: RoilyFish
 * @date: 2022/5/27 23:45
 */
@ApiModel(value = "FileUpload", description = "文件上传")
@RestController
@Slf4j
public class FileUpload {

    @Resource
    FileInterface fileInterface;

    @RequestMapping("/upload")
    @ResponseBody
    public ResultVo<FileRespVo> upload(MultipartFile file) {

        return fileInterface.uploadFile(file);

    }

    @RequestMapping("/uploadbatch")
    @ResponseBody
    public ResultVo<List<FileRespVo>> uploadBatch(MultipartHttpServletRequest files) {

        List<MultipartFile> file = files.getFiles("file");
        assert file.size() > 0;

        return fileInterface.uploadFiles(file);

    }


}
