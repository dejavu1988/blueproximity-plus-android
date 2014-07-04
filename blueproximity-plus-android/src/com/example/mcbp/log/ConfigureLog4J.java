package com.example.mcbp.log;

import org.apache.log4j.Level;

import com.example.mcbp.file.FileHelper;

import android.content.Context;
import android.provider.Settings.Secure;
import de.mindpipe.android.logging.log4j.LogConfigurator;

public class ConfigureLog4J {

	public static void configure(Context context) {
		FileHelper fh = new FileHelper();
		String root = fh.getDir();
		String uuid = Secure.getString(context.getContentResolver(),Secure.ANDROID_ID);
        final LogConfigurator logConfigurator = new LogConfigurator();
        String furi = root + "/" +"bplog_"+ uuid  + ".txt";
        logConfigurator.setFileName(furi);
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.DEBUG);
        logConfigurator.setUseFileAppender(true);
        logConfigurator.setFilePattern("%d %-5p [%c{2}]-[%L] %m%n");
        logConfigurator.setMaxFileSize(1024 * 1024 * 2);
        logConfigurator.setImmediateFlush(true);
        logConfigurator.configure();
	}
}
