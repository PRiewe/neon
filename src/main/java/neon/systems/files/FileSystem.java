/*
 *	Neon, a roguelike engine.
 *	Copyright (C) 2011 - Maarten Driesen
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neon.systems.files;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import neon.util.trees.PathTree;

/*
 * VFS structure:
 * root
 * 	|- mod1
 * 	|- mod2
 * 	|- ...
 *
 * All immutable data is in modX. Everything that is generated during gameplay
 * goes into temp. Everything in temp is copied to saves when the game is
 * closed. In each savegame directory, the structure of data is replicated.
 *
 * When a file is requested: first check in temp, then check in save, then check
 * in mod. Classes don't need to know this is happening.
 * When a file is saved: put in temp. (possibly later make path with writable bit)
 * When game is saved: somehow retrieve modified files and put them in
 * save dir.
 *
 * file usage flow during game:
 * 1. new game:
 * 		- save everything in temp
 * 		- load first from temp, then from save, then from data
 * 		- game over: clear temp
 * 		- save game: create dir, modify saves.xml, cut-paste temp to dir
 * 2. load game:
 * 		- everything the same, only dir already created and don't modify saves.xml
 */
public class FileSystem {
  private HashMap<String, String> jars;
  private File temp;
  private PathTree<String, String> files;
  private HashMap<String, String> paths; // to keep track of absolute paths to a dir or jar

  public FileSystem() {
    this("temp");
  }

  public FileSystem(String temp) {
    files = new PathTree<String, String>();
    paths = new HashMap<String, String>();
    jars = new HashMap<String, String>();
    this.temp = new File(temp);
    clearTemp();
  }

  /*
   * Specific VFS methods
   */
  /**
   * Adds a directory or jar archive to this virtual file system
   *
   * @param path the path of the directory or jar file
   * @return the internal name of the added path
   * @throws IOException
   */
  public String mount(String path) throws IOException {
    // file separator issues
    String root = path.replace("/", File.separator);
    // the problem is that a / is used in neon.ini.xml, which confuses Windows

    // then load
    root = root.substring(0, root.lastIndexOf(File.separator) + 1);
    if (new File(path).isDirectory()) { // add directory
      String dir = addDirectory(path, root);
      paths.put(dir, path);
      return dir;
    } else if (new File(path).exists()) { // check if jar exists
      String dir = addArchive(path, root);
      jars.put(dir, path);
      return dir;
    } else {
      throw new IOException("Path does not exist: " + path);
    }
  }

  /**
   * Removes a mod from the file system.
   *
   * @param path the VFS path to the mod
   */
  public void removePath(String path) {
    paths.remove(path);
    jars.remove(path);
    files.remove(path);
  }

  /**
   * @param dir the directory to search
   * @return all files in the given directory
   */
  public Collection<String> listFiles(String... dir) {
    return files.list(dir);
  }

  private String addArchive(String path, String root) throws IOException {
    JarFile jar = new JarFile(new File(path));
    Enumeration<JarEntry> entries = jar.entries();
    String modID = jar.getManifest().getMainAttributes().getValue("Mod-ID");
    //			System.out.println(modID);
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if (!entry.isDirectory()) {
        String name = new String(entry.getName());
        // this apparently must use "/" because I'm in a jar, and not File.separator
        int separatorCount = name.length() - name.replace("/", "").length();
        String[] pathArray = new String[separatorCount + 2];
        pathArray[0] = modID;
        for (int i = 1; i < separatorCount + 1; i++) {
          pathArray[i] = name.substring(0, name.indexOf("/"));
          name = name.substring(name.indexOf("/") + 1);
        }
        pathArray[separatorCount + 1] = name;
        files.add(entry.getName(), pathArray);
      }
    }
    jar.close();
    return modID;
  }

  /*
   * Add directory and put all subdirs and files in tree. The absolute path is trimmed:
   * 'c:\games\neon\mod1' is added as 'mod1'
   */
  private String addDirectory(String path, String root) {
    File dir = new File(path);
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        addDirectory(file.getPath(), root);
      } else {
        String separator = File.separator;
        String relativePath = file.getPath().replace(root, "");
        int separatorCount = relativePath.length() - relativePath.replace(separator, "").length();
        String[] pathArray = new String[separatorCount + 1];
        for (int i = 0; i < separatorCount; i++) {
          pathArray[i] = relativePath.substring(0, relativePath.indexOf(separator));
          relativePath = relativePath.substring(relativePath.indexOf(separator) + 1);
        }
        pathArray[separatorCount] = relativePath;
        files.add(file.getPath(), pathArray);
      }
    }
    // messy return method
    return path.replace("/", File.separator).replace(root, "");
  }

  /**
   * Saves a file with the given path, using a translator.
   *
   * @param output the data that has to be saved
   * @param translator a translator
   * @param path the path of the file
   */
  public <T> void saveFile(T output, Translator<T> translator, String... path) {
    try {
      if (paths.containsKey(path[0])) {
        path[0] = paths.get(path[0]);
      }
      String fullPath = toString(path);
      File file = new File(fullPath);
      //			System.out.println("savefile: " + fullPath);
      if (!file.getParentFile().exists()) {
        makeDir(file.getParent());
      }
      file.createNewFile();
      FileOutputStream out = new FileOutputStream(file);
      translator.translate(output).writeTo(out);
      out.close();
    } catch (IOException e) {
      System.out.println("IOException in FileSystem.saveFile()");
    }
  }

  /**
   * @param translator
   * @param path
   * @return the resource file with the given name
   */
  public <T> T getFile(Translator<T> translator, String... path) {
    try {
      //			System.out.println(Arrays.deepToString(path));
      if (new File(temp.getPath() + toString(path)).exists()) {
        InputStream stream = new FileInputStream(temp.getPath() + toString(path));
        return translator.translate(stream);
      } else if (jars.containsKey(path[0])) { // path[0] is the name of the mod
        JarFile jar = new JarFile(new File(jars.get(path[0])));
        InputStream stream = jar.getInputStream(jar.getEntry(files.get(path)));
        T t = translator.translate(stream);
        jar.close();
        return t;
      } else {
        InputStream stream = new FileInputStream(files.get(path));
        return translator.translate(stream);
      }
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * @param file
   * @return whether this file exists or not
   */
  public boolean exists(String... file) {
    return (files.contains(file));
  }

  /**
   * Saves a resource file to the given path in the temp directory, using a translator.
   *
   * @param output the data that has to be saved
   * @param translator a translator
   * @param path the path of the file
   */
  public <T> void saveToTemp(T output, Translator<T> translator, String... path) {
    try {
      File file = new File(temp.getPath() + toString(path));
      if (!file.getParentFile().exists()) {
        makeDir(file.getParent());
      }
      file.createNewFile();
      FileOutputStream out = new FileOutputStream(file);
      translator.translate(output).writeTo(out);
      out.close();
    } catch (IOException e) {
      System.out.println("IOException in FileSystem.saveTemp()");
    }
  }

  private String toString(String... path) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < path.length; i++) {
      if(path[i] == null) {
        continue;
      }
      buffer.append(File.separator);
      buffer.append(path[i]);
    }
    return buffer.toString();
  }

  /*
   * General methods
   */
  private void makeDir(String path) throws IOException {
    File file = new File(path);
    if (!file.getParentFile().exists()) {
      makeDir(file.getParent());
    }
    file.mkdir();
  }

  /**
   * This method copies all files from the temp directory to the designated directory.
   *
   * @param destination the name of the directory to copy temp to
   */
  public void storeTemp(File destination) {
    if (destination.isDirectory()) {
      FileUtils.copy(temp.toPath(), destination.toPath());
    }
  }

  /**
   * Deletes a file.
   *
   * @param path the path of the file to delete.
   */
  public void delete(String path) {
    delete(new File(path));
  }

  private void clearTemp() {
    if (temp.exists()) {
      delete(temp);
    }
    temp.mkdir();
  }

  // delete only works for directories if they are empty
  private void delete(File file) {
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        delete(f);
      }
    }
    file.delete();
  }
}
