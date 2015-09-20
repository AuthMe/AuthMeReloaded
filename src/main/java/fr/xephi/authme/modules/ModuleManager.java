package fr.xephi.authme.modules;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleManager {

    private List<Module> modules = new ArrayList<>();

    public ModuleManager(AuthMe plugin) {
    }

    public boolean isModuleEnabled(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    public boolean isModuleEnabled(Module.ModuleType type) {
        for (Module m : modules) {
            if (m.getType() == type)
                return true;
        }
        return false;
    }

    public Module getModule(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name))
                return m;
        }
        return null;
    }

    public Module getModule(Module.ModuleType type) {
        for (Module m : modules) {
            if (m.getType() == type)
                return m;
        }
        return null;
    }

    public int loadModules() {
        File dir = Settings.MODULE_FOLDER;
        int count = 0;
        if (!dir.isDirectory()) {
            dir.mkdirs();
            return count;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return count;
        }
        for (File pathToJar : files) {
            JarFile jarFile = null;
            URLClassLoader cl = null;
            try {
                jarFile = new JarFile(pathToJar);
                URL[] urls = {new URL("jar:file:" + pathToJar.getAbsolutePath() + "!/")};
                cl = URLClassLoader.newInstance(urls);

                Enumeration<?> e = jarFile.entries();
                while (e.hasMoreElements()) {
                    JarEntry je = (JarEntry) e.nextElement();
                    if (je.isDirectory() || !je.getName().endsWith("Main.class")) {
                        continue;
                    }
                    String className = je.getName().substring(0, je.getName().length() - 6);
                    className = className.replace('/', '.');
                    Class<?> c = cl.loadClass(className);
                    if (!Module.class.isAssignableFrom(c)) {
                        continue;
                    }

                    Module mod = (Module) c.newInstance();
                    mod.load();
                    modules.add(mod);
                    count++;
                    break;
                }

            } catch (Exception ex) {
                ConsoleLogger.writeStackTrace(ex);
                ConsoleLogger.showError("Cannot load " + pathToJar.getName() + " jar file !");
            } finally {
                try {
                    if (jarFile != null) {
                        jarFile.close();
                    }
                    if (cl != null) {
                        cl.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return count;
    }

    public void unloadModule(String name) {
        Iterator<Module> it = modules.iterator();
        while (it.hasNext()) {
            Module m = it.next();
            if (m.getName().equalsIgnoreCase(name)) {
                m.unload();
                it.remove();
                return;
            }
        }
    }

    public void unloadModules() {
        Iterator<Module> it = modules.iterator();
        while (it.hasNext()) {
            it.next().unload();
            it.remove();
        }
    }

}
