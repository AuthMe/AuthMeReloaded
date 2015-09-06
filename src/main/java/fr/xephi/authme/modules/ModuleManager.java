package fr.xephi.authme.modules;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;

public class ModuleManager implements Module {

    private AuthMe plugin;
    private ModuleManager instance;
    private List<Module> modules = new ArrayList<Module>();

    public ModuleManager(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "AuthMe Module Manager";
    }

    @Override
    public AuthMe getInstanceOfAuthMe() {
        return this.plugin;
    }

    @Override
    public Module getInstance() {
        if (this.instance == null)
            instance = new ModuleManager(AuthMe.getInstance());
        return instance;
    }

    @Override
    public ModuleType getType() {
        return (Module.ModuleType.MANAGER);
    }

    @Override
    public boolean load() {
        File dir = new File(plugin.getDataFolder() + File.separator + "modules");
        if (dir == null || !dir.exists() || !dir.isDirectory() || dir.listFiles() == null || dir.listFiles().length <= 0)
            return false;
        for (File pathToJar : dir.listFiles()) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(pathToJar);
                Enumeration<?> e = jarFile.entries();
                URL[] urls = { new URL("jar:file:" + pathToJar.getAbsolutePath() + "!/") };
                URLClassLoader cl = URLClassLoader.newInstance(urls);

                while (e.hasMoreElements()) {
                    JarEntry je = (JarEntry) e.nextElement();
                    if (je.isDirectory() || !je.getName().endsWith("Main.class")) {
                        continue;
                    }
                    String className = je.getName().substring(0, je.getName().length() - 6);
                    className = className.replace('/', '.');
                    Class<?> c = cl.loadClass(className);
                    Module mod = (Module) c.newInstance();
                    mod.load();
                    modules.add(mod);
                    break;

                }
            } catch (Exception ex) {
                ConsoleLogger.showError("Cannot load " + pathToJar.getName() + " jar file !");
            } finally {
                if (jarFile != null)
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
            }
        }
        return true;
    }

    @Override
    public boolean unload() {
        try {
            for (Module mod : modules) {
                mod.unload();
                modules.remove(mod);
            }
        } catch (Exception e) {
        }
        return true;
    }

}
