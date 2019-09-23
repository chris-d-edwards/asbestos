package gov.nist.asbestos.asbestosProxy.requests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Dirs {

    static List<File> listOfFiles(File root) {
        List<File> list = new ArrayList<>();

        File[] aList = root.listFiles();
        if (aList != null) {
            for (File file : aList) {
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                list.add(file);
            }
        }

        return list;
    }

    static List<File> listOfDirectories(File root) {
        List<File> list = new ArrayList<>();

        File[] aList = root.listFiles();
        if (aList != null) {
            for (File file : aList) {
                if (!file.isDirectory()) continue;
                if (file.getName().startsWith(".")) continue;
                if (file.getName().startsWith("_")) continue;
                list.add(file);
            }
        }

        return list;
    }

}
