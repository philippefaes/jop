/*
 * Copyright (c) 2007,2008, Stefan Hepp
 *
 * This file is part of JOPtimizer.
 *
 * JOPtimizer is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * JOPtimizer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package joptimizer.config;

import com.jopdesign.libgraph.struct.AppConfig;
import org.apache.bcel.util.ClassPath;

import java.util.*;

/**
 * Main configuration container class.
 * TODO make 'Architecture'-conf-file option, load sys/default classnames, max-code/stack/locals,.. from prop-file
 *
 * @author Stefan Hepp, e0026640@student.tuwien.ac.at
 * @noinspection InconsistentJavaDoc
 */
public class JopConfig implements AppConfig {

    private Properties config;
    private Set rootClasses;
    private ClassPath classPath;
    private String mainClass;
    private ArchConfig archConfig;

    public static final String CONF_OUTPUTPATH = "o";

    public static final String CONF_ASSUME_DYNAMIC_LOADING = "assume-dynamic-loading";

    public static final String CONF_ASSUME_REFLECTION = "assume-reflection";

    public static final String CONF_ALLOW_INCOMPLETE_CODE = "allow-incomplete-code";

    public static final String CONF_ALLOW_LOADDEMAND = "allow-loaddemand";

    public static final String CONF_ARCH_CONFIG = "arch-config";

    public static final String CONF_IGNORE_PATH = "skip";

    public static final String CONF_IGNORE_ACTION_ERRORS = "ignore-errors";

    public JopConfig() {
        this.config = new Properties();
        this.classPath = new ClassPath(".");
        this.rootClasses = new HashSet();
        setArchConfig(null);
    }

    public JopConfig(Properties config) {
        this.config = config;
        this.classPath = new ClassPath(".");
        setArchConfig(getArchConfigFileName());
    }

    public JopConfig(Properties config, ClassPath classPath, String mainClass) {
        this.config = config;
        this.classPath = classPath;
        this.mainClass = mainClass;
        setArchConfig(getArchConfigFileName());
    }

    /**
     * Add all generic options for this configuration to a list.
     *
     * @param optionList a list where options as ArgOption will be added.
     */
    public static void createOptions(List optionList) {
        optionList.add(new StringOption(CONF_OUTPUTPATH,
                "Set default output path for all generated files.", "path"));
        optionList.add(new StringOption(CONF_ARCH_CONFIG,
                "Load an architecture configuration from a config file.", "file"));
        optionList.add(new BoolOption(CONF_ASSUME_DYNAMIC_LOADING,
                "Assume that dynamic class loading is used (disables some optimizations)."));
        optionList.add(new BoolOption(CONF_ASSUME_REFLECTION,
                "Assume that reflection is used (disables some optimizations)."));
        optionList.add(new BoolOption(CONF_ALLOW_INCOMPLETE_CODE,
                "Ignore missing classes. Some features will not work when this option is set."));
        optionList.add( new BoolOption( CONF_ALLOW_LOADDEMAND,
                "Allow class loading on demand. Disables automatic transitive hull loading."));
        optionList.add( new StringOption( CONF_IGNORE_PATH,
                "Comma-separated list of packages or classes which will not be loaded. Ignored if " +
                CONF_ALLOW_INCOMPLETE_CODE + " is not set.", "pkg"));
        optionList.add( new BoolOption(CONF_IGNORE_ACTION_ERRORS,
                "Continue if any action throws an error."));
    }

    public void setProperties(Properties config) {
        this.config.putAll(config);
    }

    public String getMainClassName() {
        return mainClass;
    }

    public void setMainClassName(String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * Get a set of all rootclass classnames as collection of strings.
     * @return a set of all rootclass classnames.
     */
    public Set getRootClasses() {
        return rootClasses;
    }

    /**
     * set the rootclass classnames as set of strings.
     * @param rootClasses all root classnames as strings.
     */
    public void setRootClasses(Set rootClasses) {
        this.rootClasses = rootClasses;
    }

    public ArchConfig getArchConfig() {
        return archConfig;
    }

    public String getArchConfigFileName() {
        return getOption(CONF_ARCH_CONFIG);
    }

    public void setArchConfig(String config) {
        this.config.put(CONF_ARCH_CONFIG, config);
        this.archConfig = new ArchConfig(config);
    }

    public String getMainMethodSignature() {
        return "main([Ljava/lang/String;)V";
    }

    public ClassPath getClassPath() {
        return classPath;
    }

    public void setClassPath(ClassPath classPath) {
        this.classPath = classPath;
    }

    public boolean isEnabled(String option) {
        return Boolean.valueOf(config.getProperty(option)).booleanValue();
    }

    public void setOption(String option, String value) {
        config.setProperty(option, value);
    }

    public String getOption(String option) {
        return config.getProperty(option);
    }

    public String getOption(String option, String defaultvalue) {
        return config.getProperty(option, defaultvalue);
    }

    /**
     * Get a map of all currently set options. <br>
     * Do not modify this map.
     * 
     * @return a map with the full option name as key and its value as string.
     */
    public Map getOptions() {
        return config;
    }

    public String getDefaultOutputPath() {
        return getOption(CONF_OUTPUTPATH, ".");
    }

    public boolean doAssumeDynamicLoading() {
        return isEnabled(CONF_ASSUME_DYNAMIC_LOADING);
    }

    public boolean doAssumeReflection() {
        return isEnabled(CONF_ASSUME_REFLECTION);
    }

    public boolean doLoadOnDemand() {
        return isEnabled(CONF_ALLOW_LOADDEMAND);
    }

    public boolean doIgnoreActionErrors() {
        return isEnabled(CONF_IGNORE_ACTION_ERRORS);
    }

    /**
     * Allow for missing classes. Some features will be disabled
     * if this option is set.
     *
     * @return true if missing classes should be ignored.
     */
    public boolean doAllowIncompleteCode() {
        return isEnabled(CONF_ALLOW_INCOMPLETE_CODE);
    }

    public boolean isNativeClassName(String className) {
        return archConfig.getNativeClassName().equals(className) ||
                "java.lang.Class".equals(className);
    }

    public String doExcludeClassName(String className) {

        if (isNativeClassName(className)) {
            return "Skipping native class {" + className + "}.";
        }

        if ( doAllowIncompleteCode() ) {
            // TODO check for jopConfig.ignore_path
        }

        return null;
    }

}
