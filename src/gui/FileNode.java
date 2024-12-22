package gui;

import java.io.File;

public class FileNode {
    public File file;
    public FileNode(File file) { this.file = file; }

    @Override
    public String toString() {
        String name = file.getName();
        if (name.equals("")) { //in case of "/", etc..
            return file.getAbsolutePath();
        } else {
            return name;
        }
    }
}