package jvn;

import java.io.*;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class JvnCoordLog {
    private static final String logsDirname = "logs" + File.separatorChar;
    private static final String objectsDirname = logsDirname + "objects" + File.separatorChar;
    private static final String namesDirname = logsDirname + "names" + File.separatorChar;

    private final File objectsDir = new File(objectsDirname);
    private final File namesDir = new File(namesDirname);
    private final HashMap<String, ObjectOutputStream> openedStreams = new HashMap<>();

    public JvnCoordLog() throws Exception {
        createDirectory(objectsDir);
        createDirectory(namesDir);
    }

    private static void createDirectory(File dir) throws FileSystemException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new FileSystemException("Failed to create \"" + dir.getName() +"\" directory.");
        }
    }


    private void log(Serializable data, String filename) throws Exception {
        if (!openedStreams.containsKey(filename)) {
            FileOutputStream fos = new FileOutputStream(filename);
            openedStreams.put(filename, new ObjectOutputStream(fos));
        }
        ObjectOutputStream oos = openedStreams.get(filename);
        oos.writeObject(data);
        oos.flush();
    }

    public void logObject(int joi, Serializable obj) {
        try {
            log(obj, objectsDirname + joi);
        } catch (Exception e) {
            System.err.println("Failed to log object " + joi + ".");
        }
    }

    public void logName(String jon, JvnObject jo) {
        try {
            log(jo, namesDirname + jon);
        } catch (Exception e) {
            System.err.println("Failed to log \"" + jon + "\".");
        }
    }


    private static Object restore(String filename) throws Exception {
        Object obj = null;
        FileInputStream fis = new FileInputStream(filename);
        ObjectInputStream ois = new ObjectInputStream(fis);

        try {
            while (true) {
                obj = ois.readObject();
            }
        } catch (EOFException e) {
            ois.close();
            fis.close();
        }
        return obj;
    }

    public int restoreObjects(
            HashMap<Integer, Serializable> objects,
            HashMap<Integer, Set<JvnRemoteServer>> readers) throws Exception {
        int objCount = 0;
        File[] files = objectsDir.listFiles();

        if (files == null) {
            throw new FileSystemException("Failed to create \"" + objectsDirname + "\" directory.");
        }
        for (File file : files) {
            Serializable obj = (Serializable) restore(objectsDirname + file.getName());
            int joi = Integer.parseInt(file.getName());
            objects.put(joi, obj);
            readers.put(joi, new HashSet<>());
            objCount++;
        }
        return objCount;
    }

    public void restoreNames(HashMap<String, JvnObject> names) throws Exception {
        File[] files = namesDir.listFiles();

        if (files == null) {
            throw new FileSystemException("Failed to create \"" + namesDirname + "\" directory.");
        }
        for (File file : files) {
            JvnObject jo = (JvnObject) restore(namesDirname + file.getName());
            names.put(file.getName(), jo);
        }
    }


    public static void deleteLogs(File dir) throws Exception {
        File[] files = dir.listFiles();

        if (files == null) {
            throw new FileSystemException("Failed to delete logs.");
        }
        for (File file : files) {
            file.delete();
        }
    }

    public void deleteAllLogs() throws Exception {
        deleteLogs(objectsDir);
        deleteLogs(namesDir);
    }
}
