package gui;

import java.io.File;

public class FileNode {
    public File file;
    public FileNode(File file) { this.file = file; }

    @Override
    public String toString() {
        String name = file.getName();
        return name.equals("") ? file.getAbsolutePath() : name; //in case of "/", etc..
    }
}