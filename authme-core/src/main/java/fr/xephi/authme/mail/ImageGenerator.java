package fr.xephi.authme.mail;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 */
public class ImageGenerator {

    private final String pass;

    /**
     * Constructor for ImageGenerator.
     *
     * @param pass String
     */
    public ImageGenerator(String pass) {
        this.pass = pass;
    }

    /**
     * Method generateImage.
     *
     * @return BufferedImage
     */
    public BufferedImage generateImage() {
        BufferedImage image = new BufferedImage(200, 60, BufferedImage.TYPE_BYTE_INDEXED);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, 200, 40);
        GradientPaint gradientPaint = new GradientPaint(10, 5, Color.WHITE, 20, 10, Color.WHITE, true);
        graphics.setPaint(gradientPaint);
        Font font = new Font("Comic Sans MS", Font.BOLD, 30);
        graphics.setFont(font);
        graphics.drawString(pass, 5, 30);
        graphics.dispose();
        image.flush();
        return image;
    }
}
