package com.blade.mvc.multipart;

import com.blade.kit.ason.AsonIgnore;

import java.io.File;

/**
 * HTTP multipart/form-data Request
 *
 * @author <a href="mailto:biezhi.me@gmail.com" target="_blank">biezhi</a>
 * @since 1.5
 */
public class FileItem {

    private String name;
    private String fileName;
    private String contentType;
    private long length;

    @AsonIgnore
    private File file;

    public FileItem(String name, String fileName, String contentType, long length, File file) {
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.length = length;
        this.file = file;
    }

    public String name() {
        return name;
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }

    public long length() {
        return length;
    }

    public File file() {
        return file;
    }

    @Override
    public String toString() {
        long kb = length / 1024;
        return "FileItem(" +
                "name='" + name + '\'' +
                ", fileName='" + fileName + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + (kb < 1 ? 1 : kb) + "KB)";
    }
}