package com.image.upload.file;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/file")
@RestController
public class FileUploadController {

    private Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    /**
     * 单文件上传
     * @param file
     * @param request
     * @return
     */
    @RequestMapping(value = "/upload",method = RequestMethod.POST)
    public  String fileUpload(MultipartFile file, HttpServletRequest request){
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        String filePath = request.getSession().getServletContext().getRealPath("upload/");
       // String filePath = "D:\\work\\";
        try {
            uploadFile(file.getBytes(), filePath, fileName);
        } catch (Exception e) {
            e.getMessage();
        }
        return filePath+fileName;
    }

    /**
     * 将文件输入到指定的路径
     * @param file
     * @param filePath
     * @param fileName
     * @throws Exception
     */
    public  String uploadFile(byte[] file, String filePath, String fileName) throws Exception {
        File targetFile = new File(filePath);
        if(!targetFile.exists()){
            targetFile.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(filePath+fileName);
        out.write(file);
        out.flush();
        out.close();
        return filePath+fileName;
    }

    /**
     * 多文件上传
     * @param request
     * @return
     */
    @RequestMapping(value = "/handleFileUpload" ,method = RequestMethod.POST)
    public String handleFileUpload(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding("UTF-8");
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        List<String> list = new ArrayList<>();
        //pc端图片保存路径
        String filePath = request.getSession().getServletContext().getRealPath("upload/");
        BufferedOutputStream stream = null;
        for (int i = 0; i < files.size(); ++i) {
            MultipartFile multipartFile = files.get(i);
            if (!multipartFile.isEmpty()) {
                try {
                    byte[] bytes = multipartFile.getBytes();
                    //文件名
                    String fileName = multipartFile.getOriginalFilename();
                    String[] suffix = fileName.split("\\.");
                    boolean isTrue = checkImage(suffix[1]);
                    String url;
                    String phoneUrl = null;
                    if(isTrue == false){
                        url = uploadFile(bytes,filePath,fileName);
                    }else{
                        //手机端图片保存路径,图片保存两次，一次为pc端图片，一次为手机端图片，手机端图片需要将图片压缩
                        url = uploadFile(bytes,filePath,fileName);
                        String outputFileUrl = request.getSession().getServletContext().getRealPath("upload/phone/");
                        File targetFile = new File(outputFileUrl);
                        if (!targetFile.exists())targetFile.mkdirs();//判断路径是否存在，不存在则创建一个文件
                        phoneUrl = compressPic(filePath+fileName,120,120,outputFileUrl,fileName);
                    }
                    if(phoneUrl!=null)list.add(phoneUrl);
                    list.add(url);
                } catch (Exception e) {
                    stream = null;
                    return "第" + i + "行 => "+ e.getMessage()+"==="+e.getStackTrace();
                }
            } else {
                return null;
            }
        }
        return list.toString();
    }

    /**
     * 检验上传文件是否是图片
     * @param suffix 后缀
     * @return
     */
    public boolean checkImage(String suffix){
        // 声明图片后缀名数组
        String imageArray [] = {"bmp", "dib", "gif", "jfif", "jpe", "jpeg", "jpg", "png" ,"tif", "tiff", "ico"};
        for(int i = 0;i<imageArray.length;i++){
            if(imageArray[i].equalsIgnoreCase(suffix)){
                return true;
            }
        }
        return false;
    }

    /**
     * 图片存缩略图
     * @param fileUrl 文件路径
     * @param outputWidth 保存缩略图宽度
     * @param outputHeight 保存缩略图高度
     * @param outputFileUrl 输出保存的文件路径
     * @param outputFileName 输出保存的文件宽度
     * @return
     */
    public String compressPic(String fileUrl,int outputWidth,int outputHeight,String outputFileUrl,String outputFileName){
        File file = new File(fileUrl);
        try{
            Image img = ImageIO.read(file);
            // 判断图片格式是否正确
            if (img.getWidth(null) == -1) {
                return "no";
            } else {
                int newWidth;
                int newHeight;
                // 判断是否是等比缩放
                boolean proportion = true;
                if (proportion == true) {
                    // 为等比缩放计算输出的图片宽度及高度
                    double rate1 = ((double) img.getWidth(null)) / (double) outputWidth + 0.1;
                    double rate2 = ((double) img.getHeight(null)) / (double) outputHeight + 0.1;
                    // 根据缩放比率大的进行缩放控制
                    double rate = rate1 > rate2 ? rate1 : rate2;
                    newWidth = (int) (((double) img.getWidth(null)) / rate);
                    newHeight = (int) (((double) img.getHeight(null)) / rate);
                } else {
                    newWidth = outputWidth; // 输出的图片宽度
                    newHeight = outputHeight; // 输出的图片高度
                }
                BufferedImage tag = new BufferedImage((int) newWidth, (int) newHeight, BufferedImage.TYPE_INT_RGB);
                /*
                 * Image.SCALE_SMOOTH 的缩略算法
                 * 生成的图片质量比较好 但速度慢
                 */
                tag.getGraphics().drawImage(img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
                FileOutputStream out = new FileOutputStream(outputFileUrl + outputFileName);
                // JPEGImageEncoder可适用于其他图片类型的转换
                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
                encoder.encode(tag);
                out.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return outputFileUrl+outputFileName;

    }
}
