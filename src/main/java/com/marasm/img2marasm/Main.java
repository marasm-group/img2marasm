package com.marasm.img2marasm;

import com.marasm.ppc.Variable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.PrintWriter;

public class Main {
    static PrintWriter writer=null;
    public static void main(String[] args) {
        if(args.length<2){System.out.println("Usage: img2marasm <image> <marasm>");System.exit(0);}
        String imgPath=args[0];
        String marasmPath=args[1];
        System.out.println("Converting " + imgPath + " to " + marasmPath);
        FastRGB img=null;
        try {
            writer= new PrintWriter(marasmPath, "UTF-8");
            File imgFile=new File(imgPath);
            String imgName=imgFile.getName();
            String imgLoadFunName="$load_"+imgName;
            printCmd("#json\n" +
                    "{\n" +
                    "    \"author\":\"img2marasm\",    \n" +
                    "    \"dependencies\":[],\n" +
                    "    \"init\":\""+imgLoadFunName+"\"\n" +
                    "}\n" +
                    "#end");
            printCmd(imgLoadFunName+" ;;");
            printCmd("gvar "+imgName);
            printCmd("mov "+imgName+" 0 ;; TODO: change this to desired address");
            BufferedImage bi=ImageIO.read(imgFile);
            bi=convert(bi,BufferedImage.TYPE_3BYTE_BGR);
            img=new FastRGB(bi);
            printCmd("var ptr");
            printCmd("mov ptr "+imgName);
            printCmd("store ptr "+img.width+" ; Width");
            printCmd("add ptr ptr 1");
            printCmd("store ptr "+img.height+" ; Height");
            printCmd("add ptr ptr 1");
            for(int y=0;y<img.height;y++)
            {
                for(int x=0;x<img.width;x++)
                {
                    int rgb=img.getRGB(x,y);
                    Variable v=new Variable(rgb);
                    printCmd("store ptr "+v+" ; R:"+(rgb>>16&0xFF)+" G:"+(rgb>>8&0xFF)+ " B:"+(rgb&0xFF)+" x:"+x+" y:"+y);
                    printCmd("add ptr ptr 1");
                }
            }
            printCmd("ret");
            printCmd("halt -1 ; stop further execution (if any)");
        } catch (Exception e) {
            e.printStackTrace();
            writer.close();
            System.exit(-1);
        }
        writer.close();
    }
    static void printCmd(String cmd)
    {
        //System.out.println(cmd);
        writer.println(cmd);
    }
    public static BufferedImage convert(BufferedImage src, int bufImgType) {
        BufferedImage img= new BufferedImage(src.getWidth(), src.getHeight(), bufImgType);
        Graphics2D g2d= img.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        return img;
    }
    public static class FastRGB
    {

        private int width;
        private int height;
        private boolean hasAlphaChannel;
        private int pixelLength;
        private byte[] pixels;

        FastRGB(BufferedImage image)
        {
            pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            width = image.getWidth();
            height = image.getHeight();
            hasAlphaChannel = image.getAlphaRaster() != null;
            pixelLength = 3;
            if (hasAlphaChannel) {pixelLength = 4;}
        }

        int getRGB(int x, int y)
        {
            int pos = (y * pixelLength * width) + (x * pixelLength);
            int argb = 0;//-16777216; // 255 alpha
            if (hasAlphaChannel){argb = (((int) pixels[pos++] & 0xff) << 24);} // alpha
            argb += ((int) pixels[pos++] & 0xff); // blue
            argb += (((int) pixels[pos++] & 0xff) << 8); // green
            argb += (((int) pixels[pos++] & 0xff) << 16); // red
            return argb;
        }
    }
}
