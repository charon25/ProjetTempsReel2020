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


// Classe qui permet de gérer des logins avec nom d'utilisateur et mots de passe hashés
public class Login {
    private String username; // Nom d'utilisateur
    private String hashedPasswordString; // Mot de passe hashé

    // Contructeur avec les paramètres suivants :
    // username : nom d'utilisateur (String)
    // password : mot de passe, hashé ou non (String)
    // alreadyHashed : indique si le mot de passe est déjà hashé (boolean)
    public Login(String username, String password, boolean alreadyHashed) {
        try {
            this.username = username;
            if (alreadyHashed) { // Si le mdp est déjà hashé, on ne le refait pas
                this.hashedPasswordString = password;
            } else { // Sinon, on utilise la méthode statique pour le hasher
                this.hashedPasswordString = getHashString(password);
            }
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // ACCESSEURS
    public String getUsername() {
        return username;
    }
    public String getHashedPasswordString() {
        return hashedPasswordString;
    }

    // Comparer deux logins et renvoie True si les deux ont même nom d'utilisateur et même mot de passe
    public boolean isSameAs(Login login) {
        return this.username.equals(login.getUsername()) && this.hashedPasswordString.equals(login.getHashedPasswordString());
    }

    // Méthode statique qui permet de hasher un mot de passe avec l'alhorithme SHA-256, qui renvoie un tableau de Byte qu'on convertit en String
    public static String getHashString(String str) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedPassword = digest.digest(str.getBytes(Charset.forName("UTF-8")));
        StringBuilder s = new StringBuilder();
        for (byte b : hashedPassword) {
            s.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return s.toString();
    }
    
}
