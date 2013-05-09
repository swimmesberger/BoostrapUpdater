package org.fseek.thedeath.bootstrapupdater;

import org.fseek.thedeath.bootstrapupdater.models.Bootswatch;
import org.fseek.thedeath.bootstrapupdater.models.Theme;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Thedeath<www.fseek.org>
 */
public class BootstrapUpdater
{
    public static File mainFile = getMainFile();
    public static File cssDir;
    public static File jsDir;
    public static File imgDir;
    public static File themesDir;
    
    public static final String BOOTSWATCH = "http://api.bootswatch.com/";
    public static final String BOOTSTRAP = "http://twitter.github.io/bootstrap/assets/bootstrap.zip";
    
    public static final String[] bootstrapFiles = new String[]{"css/bootstrap.css", "css/bootstrap-responsive.css", "js/bootstrap.js", "img/glyphicons-halflings.png", "img/glyphicons-halflings-white.png"};
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        boolean bootstrapOnly = false;
        if(args.length < 4){
            System.out.println("Default paths are used: js=\"js/libs\" css=\"css\" themes=\"css/themes\" images=\"css/themes/img\" bootstrap_only=\"false\"");
            System.out.println("Usage for own paths: \"java -jar BootstrapUpdater.jar {themeDir} {cssDir] {jsDir} {imgDir} {bootstrap only}[true|false]\" without \"{}\"");
            if(args.length >= 1 && args[0].toLowerCase().equals("true")){
                bootstrapOnly = true;
            }
            initDefault();
        }else{
            init(args[0], args[1], args[2], args[3]);
            if(args.length >= 5 && args[4].toLowerCase().equals("true")){
                bootstrapOnly = true;
            }
        }
        initDirectories();
        if(bootstrapOnly == false){
            System.out.println("Copying bootswatch themes...");
            copyBootswatch();
        }
        System.out.println("Copying bootstrap...");
        copyBootstrap();
    }
    
    private static void initDefault(){
        File staticDir = new File(mainFile, "static");
        cssDir = new File(staticDir, "css");
        themesDir = new File(cssDir, "themes");
        imgDir = new File(themesDir, "img");
        jsDir = new File(staticDir.getAbsolutePath() + File.separator + "js" + File.separator + "libs");
    }
    
    private static void init(String themesDir, String cssDir, String jsDir, String imgDir){
        BootstrapUpdater.cssDir = new File(cssDir);
        BootstrapUpdater.themesDir = new File(themesDir);
        BootstrapUpdater.jsDir = new File(jsDir);
        BootstrapUpdater.imgDir = new File(imgDir);
    }
    
    private static void initDirectories(){
        themesDir.mkdirs();
        jsDir.mkdirs();
        cssDir.mkdirs();
        imgDir.mkdirs();
    }
    
    public static void copyBootswatch(){
        try
        {
            String json = IOUtils.toString(new URL(BOOTSWATCH));
            Bootswatch bw = new Gson().fromJson(json, Bootswatch.class);
            try{
                for(Theme t : bw.themes){
                    System.out.println("   Copying " + t.name);
                    File output = new File(themesDir, t.name);
                    output.mkdir();
                    File outputFile = new File(output, "bootstrap.css");
                    if(outputFile.exists()){
                        outputFile.delete();
                    }
                    FileUtils.copyURLToFile(new URL(t.css), outputFile);
                }
            }catch(IOException ex){
                System.out.println("Error while copying css file of bootswatch to local directory.");
                System.out.println("Detail: " + ex.getMessage());
            }
        } catch (IOException ex)
        {
            System.out.println("Bootswatch seems to be offline... or you don't have an internet connection.");
        }
    }
    
    public static void copyBootstrap(){
        try
        {
            
            // create the direcotry for the default bootstrap theme
            File bootstrapTheme = new File(themesDir.getAbsolutePath() + File.separator + "bootstrap");
            bootstrapTheme.mkdir();
            
            System.out.println("   Downloading archive...");
            // create a temp file to download the zip archive to
            File zipFile = File.createTempFile("bootstrap", "zip");
            FileUtils.copyURLToFile(new URL(BOOTSTRAP), zipFile);
            
            ZipFile zip = new ZipFile(zipFile);
            // iterate over the config in the header of this file -> so only the files defined their are used
            for(String file : bootstrapFiles){
                try{
                    ZipArchiveEntry zipEntry = zip.getEntry("bootstrap/" + file);
                    String zipFileFullName = zipEntry.getName();
                    String zipFileName = FilenameUtils.getName(zipFileFullName);
                    // css extension -> copy into css directory
                    if(FilenameUtils.isExtension(zipFileFullName, "css")){
                        // the main bootstrap file gets copied into the theme directory because I want to treat it as a seperate theme
                        if(zipFileName.toLowerCase().equals("bootstrap.css")){
                            copyArchiveFile(zip, zipEntry, new File(bootstrapTheme, zipFileName));
                        }else{
                            copyArchiveFile(zip, zipEntry, new File(cssDir, zipFileName));
                        }
                    // js extension -> copy into js directory
                    }else if(FilenameUtils.isExtension(zipFileFullName, "js")){
                        copyArchiveFile(zip, zipEntry, new File(jsDir, zipFileName));
                    // everything else gets copied into the img directory
                    }else{
                        copyArchiveFile(zip, zipEntry, new File(imgDir, zipFileName));
                    }
                }catch(IOException ex){
                    System.out.println("   " + file + " could not be copied.");
                }
            }
            zip.close();
            FileUtils.deleteQuietly(zipFile);
        } catch (IOException ex)
        {
            System.out.println("Bootstrap seems to be offline... or you don't have an internet connection.");
        }
    }
    
    private static void copyArchiveFile(ZipFile zip, ZipArchiveEntry entry, File file) throws IOException{
        if(file.exists()){
            file.delete();
        }
        System.out.println("   Copying "+file.getName()+"...");
        InputStream in = zip.getInputStream(entry);
        OutputStream out = new FileOutputStream(file);
        try{
            IOUtils.copy(in, out);
            out.flush();
            // Debug info
            //System.out.println("   Copied src=" + entry.getSize() + " dest=" + file.length());
        }finally{
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
    
    /*
     * This method detects the path to the executed .jar file
     */
    public static File getMainFile()
    {
        if(mainFile != null)return mainFile;
        try
        {
            String path = BootstrapUpdater.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File mainFileT = new File(decodedPath);
            String absolutePath = null;
            try
            {
                absolutePath = mainFileT.getCanonicalPath();
                if (absolutePath.contains(".jar"))
                {
                    int index = absolutePath.lastIndexOf(File.separator);
                    absolutePath = absolutePath.substring(0, index);
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                System.exit(1);
            }
            BootstrapUpdater.mainFile = new File(absolutePath);
            return BootstrapUpdater.mainFile;
        } 
        catch (UnsupportedEncodingException ex)
        {
            Logger.getLogger(BootstrapUpdater.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        }
        catch(Exception ex)
        {
            return new File(".");
        }
        return new File(".");
    }
}
