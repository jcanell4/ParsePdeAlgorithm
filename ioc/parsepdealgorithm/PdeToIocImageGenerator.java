/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ioc.parsepdealgorithm;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.app.Preferences;
import processing.app.SketchException;
import processing.mode.java.preproc.PdePreprocessor;

/**
 *
 * @author Josep Cañellas <jcanell4@ioc.cat>
 */
public class PdeToIocImageGenerator {
    private static final String PACKAGE_ARG = "-pkg";
    private static final String PRFERENCES_ARG = "-pref";
    private static final String IMPORT_LIST_FILE_ARG = "-import";
    private static final String  EDITOR_TABS_SIZE_ARG = "-size";
    private static final String  CLASS_NAME_ARG = "-cn";
    private static final String  OUT_DIR_ARG = "-outd";
    private static final String  PDE_FILE_ARG = "-pde";
    private static HashMap<String, String> hashArgs;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        String path;
        int ret=0;
        PdePreprocessor proc=null;
        FileWriter fout=null;
        FileInputStream fin=null;
        setArgs(args);
        try {
            if(hashArgs.containsKey(PRFERENCES_ARG)){
                fin = new FileInputStream(hashArgs.get(PRFERENCES_ARG));
                Preferences.load(fin);
            }else{
                setDefaultPreferences(!hashArgs.containsKey(IMPORT_LIST_FILE_ARG));
            }
            if(hashArgs.containsKey(IMPORT_LIST_FILE_ARG)){
                String list = read(hashArgs.get(IMPORT_LIST_FILE_ARG));
                Preferences.set("preproc.imports.list", list);
            }
            if(hashArgs.containsKey(EDITOR_TABS_SIZE_ARG)){
                int sizeVal = new Integer(hashArgs.get(EDITOR_TABS_SIZE_ARG));
                proc = new PdePreprocessor(hashArgs.get(CLASS_NAME_ARG), sizeVal);
            }else{
                proc = new PdePreprocessor(hashArgs.get(CLASS_NAME_ARG));
            }
            StringWriter out = new StringWriter();
            String prg = read(new File(hashArgs.get(PDE_FILE_ARG)));
            path = hashArgs.get(OUT_DIR_ARG);
//            if(hashArgs.containsKey(PACKAGE_ARG)){
//                out.write("package ");
//                out.write(hashArgs.get(PACKAGE_ARG));
//                out.write(";\n");
//                path += hashArgs.get(PACKAGE_ARG).replaceAll("\\.", "/")+"/";
//            }
            proc.write(out, prg/*, new String[] {"java.io"}*/);
            String classContent;
                             
            classContent = out.toString().replaceFirst("extends PApplet \\{"
                                                    , "extends ImageGenerator{");
            classContent = classContent.replaceFirst(
                    "static public void main\\(String\\[\\] passedArgs\\) "
                    + "\\{[\\s|.*\\s].*\\s*.*\\s*.*\\s*.*\\s*.*\\s*.*\\s*"
                    + ".*\\s*", "static public void main(String[] passedArgs)"
                    + "{}");
             
            classContent = classContent.replaceAll("import .*;\\s", "");
            
            String firstContent;
            if(hashArgs.containsKey(PACKAGE_ARG)){
                firstContent="package "+hashArgs.get(PACKAGE_ARG)+";\n\n";
                path += hashArgs.get(PACKAGE_ARG).replaceAll("\\.", "/")+"/";
            }else{
                firstContent="";
            }
            firstContent += "import processing.core.*;\nimport processing.data.*;\n" 
                     +"import processing.event.*;\nimport processing.opengl.*;\n"
                     +"import java.util.HashMap;\nimport java.util.ArrayList;\n\n";
            classContent=firstContent+classContent;

             
            File fpath = new File(path);
            if(!fpath.exists()){
                fpath.mkdirs();
            }
            fout = new FileWriter(path+hashArgs.get(CLASS_NAME_ARG)+".java");            
            fout.write(classContent);            
            out.flush();
            
        } catch (SketchException ex) {
            ret=-1;
            Logger.getLogger(PdeToIocImageGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RecognitionException ex) {
            ret=-2; //error de sintaxi. Instrucció no reconeguda
            Logger.getLogger(PdeToIocImageGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TokenStreamException ex) {
            ret=-3; //error de sintaxi. 
            Logger.getLogger(PdeToIocImageGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            ret=-4; //error de fitxer. 
            Logger.getLogger(PdeToIocImageGenerator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            ret=-5; //error de inesperat. 
            Logger.getLogger(PdeToIocImageGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            if(fin!=null){
                close(fin);
            }
            if(fout!=null){
                close(fout);
            }
        }
        System.exit(ret);
    }
    
    
    private static void setArgs(String[] args){
        //-pkg=ioc.wiki.processing -cn=PdeClass -pde=pdef.ped  
        //-pref preferences.txt -import
        hashArgs = new HashMap<>();
        for(int i=0; i<args.length; i++){
            if(args[i].indexOf('=')>-1){
                String[] pair = args[i].split("=");
                hashArgs.put(pair[0], pair[1]);
            }else{
                if(hasArgumentValue(args[0])){
                    hashArgs.put(args[i++], args[i]);
                }else{
                    hashArgs.put(args[i], "true");
                }
            }
        }
        if(!hashArgs.containsKey(CLASS_NAME_ARG)){
            if(hashArgs.containsKey(PDE_FILE_ARG)){
                String pdeFile = hashArgs.get(PDE_FILE_ARG);
                int endIndex = pdeFile.lastIndexOf(".pde");
                if(endIndex==-1){
                    endIndex=pdeFile.length();
                }
                String cn = hashArgs.get(PDE_FILE_ARG).substring(0, endIndex);
                cn = cn.toUpperCase().charAt(0) + cn.substring(1);
                hashArgs.put(CLASS_NAME_ARG, cn);
            }
        }
        if(hashArgs.containsKey(OUT_DIR_ARG)){
            String outDir = hashArgs.get(OUT_DIR_ARG);
            if(!outDir.endsWith("/")){
                outDir += "/";
                hashArgs.put(OUT_DIR_ARG, outDir);
            }
        }else{
            hashArgs.put(OUT_DIR_ARG, "");
        }
    }
    
    private static boolean hasArgumentValue(String arg){
        return true;
    }
    
    private static void setDefaultPreferences(boolean imports){
        Preferences.set("editor.tabs.size", "2");
        Preferences.set("preproc.save_build_files", "false");
        Preferences.set("preproc.color_datatype", "true");
        Preferences.set("preproc.web_colors", "true");
        Preferences.set("preproc.enhanced_casting", "true");
        Preferences.set("preproc.substitute_floats", "true");
        Preferences.set("preproc.substitute_unicode", "true");
        Preferences.set("preproc.output_parse_tree", "false");
        if(imports){
            Preferences.set("preproc.imports.list", "java.applet.*,"
                    + "java.awt.Dimension,java.awt.Frame,java.awt.event.MouseEvent,"
                    + "java.awt.event.KeyEvent,java.awt.event.FocusEvent,"
                    + "java.awt.Image,java.io.*,java.net.*,java.text.*,java.util.*,"
                    + "java.util.zip.*,java.util.regex.*");
        }
    }
    
    private static String read(String f) {
        return read(new File(f));
    }
    
    private static String read(final File f) {
        try {
          final FileInputStream fin = new FileInputStream(f);
          final InputStreamReader in = new InputStreamReader(fin, "UTF-8");
          try {
            final StringBuilder sb = new StringBuilder();
            final char[] buf = new char[1 << 12];
            int len;
            while ((len = in.read(buf)) != -1)
              sb.append(buf, 0, len);
            return normalize(sb);
          } finally {
            in.close();
          }
        } catch (Exception e) {
          throw new RuntimeException("Unexpected", e);
        }
    } 
   
    private static String normalize(final Object s) {
        return String.valueOf(s).replace("\r", "");
    }
    
    private static void close(Closeable cl){
        try {
            cl.close();
        } catch (IOException ex) {
            Logger.getLogger(PdeToIocImageGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
