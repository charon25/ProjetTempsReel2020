/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insa.kern.projet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pkern01
 */
public class Login {
    private String username;
    private String hashedPasswordString;

    public Login(String username, String password, boolean alreadyHashed) {
        try {
            this.username = username;
            if (alreadyHashed) {
                this.hashedPasswordString = password;
            } else {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashedPassword = digest.digest(password.getBytes(Charset.forName("UTF-8")));
                StringBuilder s = new StringBuilder();
                for (byte b : hashedPassword) {
                    s.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
                }
                this.hashedPasswordString = s.toString();
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getUsername() {
        return username;
    }

    
    public String getHashedPasswordString() {
        return hashedPasswordString;
    }

    public boolean isSameAs(Login login) {
        return this.username.equals(login.getUsername()) && this.hashedPasswordString.equals(login.getHashedPasswordString());
    }
    
}
