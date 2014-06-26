package de.comlineag.snc.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * @author 		Mohammed Abbas
 * @category 	Helper Class
 * @version 	1.0
 * 
 * @description	this class is a way to serialize Java object as strings
 *
 */
public class ObjectUtil {

    private static Base64 base64 = new Base64();

    public static String serializeObjectToString(Object object) throws Exception {

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(arrayOutputStream);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);

        objectOutputStream.writeObject(object);

        objectOutputStream.flush();
        gzipOutputStream.close();
        arrayOutputStream.close();
        objectOutputStream.close();
        String objectString = new String(base64.encode(arrayOutputStream.toByteArray()));

        return objectString;
    }

    public static Object deserializeObjectFromString(String objectString) throws Exception {

        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream((byte[]) base64.decode(objectString));
        GZIPInputStream gzipInputStream = new GZIPInputStream(arrayInputStream);
        ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);

        Object object = objectInputStream.readObject();

        objectInputStream.close();
        gzipInputStream.close();
        arrayInputStream.close();

        return object;
    }
}